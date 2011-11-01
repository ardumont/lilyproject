package org.lilyproject.tools.rowlogvisualizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.joda.time.LocalDateTime;
import org.lilyproject.cli.BaseZkCliTool;
import org.lilyproject.repository.api.IdGenerator;
import org.lilyproject.repository.api.RecordId;
import org.lilyproject.repository.impl.IdGeneratorImpl;
import org.lilyproject.rowlog.impl.SubscriptionExecutionState;
import org.lilyproject.util.hbase.LilyHBaseSchema;

import java.nio.ByteBuffer;
import java.util.List;

public class RowLogVisualizer extends BaseZkCliTool {

    private static final byte[] MESSAGES_CF = Bytes.toBytes("messages");
    private static final byte[] MESSAGE_COLUMN = Bytes.toBytes("msg");

    private static final byte PL_BYTE = (byte)1;
    private static final byte ES_BYTE = (byte)2;

    private byte[] executionStatePrefix;
    private byte[] payloadPrefix;

    @Override
    protected String getCmdName() {
        return "lily-show-rowlog";
    }

    @Override
    public List<Option> getOptions() {
        List<Option> options = super.getOptions();

        return options;
    }

    public static void main(String[] args) {
        new RowLogVisualizer().start(args);
    }

    @Override
    public int run(CommandLine cmd) throws Exception {
        int result =  super.run(cmd);
        if (result != 0)
            return result;

        Configuration hbaseConf = HBaseConfiguration.create();
        hbaseConf.set("hbase.zookeeper.quorum", zkConnectionString);

        HTableInterface rowlogTable = new HTable(hbaseConf, "rowlog-mq");
        HTableInterface recordsTable = new HTable(hbaseConf, "record");

        IdGenerator idGenerator = new IdGeneratorImpl();

        // General parameters
        byte rowLogId = LilyHBaseSchema.RecordColumn.MQ_PREFIX;
        this.executionStatePrefix = new byte[] {rowLogId, ES_BYTE};
        this.payloadPrefix = new byte[] {rowLogId, PL_BYTE};
        byte[] rowLogColumnFamily = LilyHBaseSchema.RecordCf.ROWLOG.bytes;

        //
        Scan scan = new Scan();
        scan.setCacheBlocks(false);
        scan.setCaching(1000);
        scan.addColumn(MESSAGES_CF, MESSAGE_COLUMN);

        ResultScanner scanner = rowlogTable.getScanner(scan);
        Result rowlogRow;
        int counter = 0;
        while ((rowlogRow = scanner.next()) != null) {
            counter++;
            byte[] rowkey = rowlogRow.getRow();

            long hbaseTimestamp = rowlogRow.getColumnLatest(MESSAGES_CF, MESSAGE_COLUMN).getTimestamp();

            // First byte it the shard key (the 'region selector')
            rowkey = Bytes.tail(rowkey, rowkey.length - 1);

            // Find out where the subscription name ends
            int endOfPrefixPos = -1;
            for (int i = 0; i < rowkey.length; i++) {
                if (rowkey[i] == (byte)0) {
                    endOfPrefixPos = i;
                    break;
                }
            }

            String subscriptionName = new String(Bytes.head(rowkey, endOfPrefixPos + 1));

            // Copied from RowLogShardImpl.decodeMessage
            byte[] messageId = Bytes.tail(rowkey, rowkey.length - (endOfPrefixPos + 1));
            long timestamp = Bytes.toLong(messageId);
            long seqNr = Bytes.toLong(messageId, Bytes.SIZEOF_LONG);
            byte[] recordRowkey = Bytes.tail(messageId, messageId.length - (2 * Bytes.SIZEOF_LONG));

            // Read the execution state
            byte[] executionStateQualifier = executionStateQualifier(seqNr, timestamp);
            Get get = new Get(recordRowkey);
            get.addColumn(rowLogColumnFamily, executionStateQualifier);
            Result esResult = recordsTable.get(get);
            SubscriptionExecutionState execState = null;
            if (!esResult.isEmpty()) {
                byte[] esData = esResult.getValue(rowLogColumnFamily, executionStateQualifier);
                execState = SubscriptionExecutionState.fromBytes(esData);
            }

            // Read the payload
            byte[] payloadQualifier = payloadQualifier(seqNr, timestamp);
            get = new Get(recordRowkey);
            get.addColumn(rowLogColumnFamily, payloadQualifier);
            Result plResult = recordsTable.get(get);
            byte[] payload = plResult.getValue(rowLogColumnFamily, payloadQualifier);

            // Print info
            RecordId recordId = idGenerator.fromBytes(recordRowkey);
            System.out.println("-------------------------------------------------------------------------");
            System.out.println("   Subscription: " + subscriptionName);
            System.out.println("         Record: " + recordId);
            System.out.println("      Timestamp: " + new LocalDateTime(timestamp) + " - " + timestamp);
            System.out.println("HBase timestamp: " + new LocalDateTime(hbaseTimestamp) + " - " + hbaseTimestamp);
            System.out.println("          Seqnr: " + seqNr);
            System.out.println();
            if (execState == null) {
                System.out.println("No execution state found.");
            } else {
                System.out.println("Execution state. (timestamp = " + new LocalDateTime(execState.getTimestamp()) + ")");
                for (String subscriptionId : execState.getSubscriptionIds()) {
                    System.out.println("  - Subscription: " + subscriptionId + ", state = " + execState.getState(subscriptionId));
                }
            }
            System.out.println();
            if (payload == null) {
                System.out.println("No payload found.");
            } else {
                System.out.println("Payload: " + new String(payload, "UTF-8"));
            }
            System.out.println();
        }

        System.out.println();
        System.out.println("Total number of entries: " + counter);

        scanner.close();

        return 0;
    }

    // Copied from RowLogImpl
    private byte[] executionStateQualifier(long seqnr, long timestamp) {
        ByteBuffer buffer = ByteBuffer.allocate(2 + 8 + 8); // executionState-prefix + seqnr + timestamp
        buffer.put(executionStatePrefix);
        buffer.putLong(seqnr);
        buffer.putLong(timestamp);
        return buffer.array();
    }

    // Copied from RowLogImpl
    private byte[] payloadQualifier(long seqnr, long timestamp) {
        ByteBuffer buffer = ByteBuffer.allocate(2 + 8 + 8); // payload-prefix + seqnr + timestamp
        buffer.put(payloadPrefix);
        buffer.putLong(seqnr);
        buffer.putLong(timestamp);
        return buffer.array();
    }
}