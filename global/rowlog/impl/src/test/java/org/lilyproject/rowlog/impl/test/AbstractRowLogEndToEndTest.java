/*
 * Copyright 2010 Outerthought bvba
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lilyproject.rowlog.impl.test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.*;
import org.junit.rules.TestName;
import org.lilyproject.rowlog.api.*;
import org.lilyproject.rowlog.impl.*;
import org.lilyproject.hadooptestfw.HBaseProxy;
import org.lilyproject.hadooptestfw.TestHelper;
import org.lilyproject.util.hbase.HBaseTableFactoryImpl;
import org.lilyproject.util.io.Closer;
import org.lilyproject.util.zookeeper.ZkUtil;
import org.lilyproject.util.zookeeper.ZooKeeperItf;

import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class AbstractRowLogEndToEndTest {
    protected static HBaseProxy HBASE_PROXY;
    protected static RowLog rowLog;
    protected static RowLogProcessor processor;
    protected static RowLogConfigurationManagerImpl rowLogConfigurationManager;
    protected String subscriptionId = "Subscription1";
    protected ValidationMessageListener validationListener;
    private static Configuration configuration;
    protected static ZooKeeperItf zooKeeper;

    @Rule public TestName name = new TestName();
    private static HTableInterface rowTable;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TestHelper.setupLogging();
        HBASE_PROXY = new HBaseProxy();
        HBASE_PROXY.start();
        configuration = HBASE_PROXY.getConf();
        rowTable = RowLogTableUtil.getRowTable(configuration);
        // Using a large ZooKeeper timeout, seems to help the build to succeed on Hudson (not sure if this is
        // the problem or the symptom, but HBase's Sleeper thread also reports it slept to long, so it appears
        // to be JVM-level).
        zooKeeper = ZkUtil.connect(HBASE_PROXY.getZkConnectString(), 120000);
        rowLogConfigurationManager = new RowLogConfigurationManagerImpl(zooKeeper);
        // The orphanedMessageDelay is smaller than usual on purpose, since some tests wait on this cleanup
        rowLogConfigurationManager.addRowLog("EndToEndRowLog", new RowLogConfig(true, true, 100L, 0L, 5000L, 5000L, 100));
        rowLog = new RowLogImpl("EndToEndRowLog", rowTable, RowLogTableUtil.ROWLOG_COLUMN_FAMILY,
                (byte)1, rowLogConfigurationManager, null, new RowLogHashShardRouter());
        RowLogShardSetup.setupShards(1, rowLog, new HBaseTableFactoryImpl(configuration));
        processor = new RowLogProcessorImpl(rowLog, rowLogConfigurationManager, configuration);
    }    
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        Closer.close(processor);
        Closer.close(rowLog);
        Closer.close(rowLogConfigurationManager);
        Closer.close(zooKeeper);
        HBASE_PROXY.stop();
    }
    
    @Test(timeout=150000)
    public void testSingleMessage() throws Exception {
        RowLogMessage message = rowLog.putMessage(Bytes.toBytes("row1"), null, null, null);
        validationListener.expectMessage(message);
        validationListener.expectMessages(1);
        processor.start();
        validationListener.waitUntilMessagesConsumed(120000);
        // Sleep to allow processor to finish message processing (messageDone marking)
        Thread.sleep(2000);
        processor.stop();
        validationListener.validate();
    }

    @Test(timeout=150000)
    public void testRemovalFromShardFailed() throws Exception {
        RowLogMessage message = rowLog.putMessage(Bytes.toBytes("row1"), null, null, null);
        validationListener.expectMessage(message);
        validationListener.expectMessages(1);
        processor.start();
        validationListener.waitUntilMessagesConsumed(120000);
        // Sleep to allow processor to finish message processing (messageDone marking)
        Thread.sleep(2000);
        processor.stop();
        validationListener.validate();

        List<RowLogShard> shards = rowLog.getShardList().getShards();
        assertEquals(1, shards.size());
        RowLogShard shard = shards.get(0);

        shard.putMessage(message);
        Assert.assertFalse(shard.next(subscriptionId, 20).isEmpty());
        processor.start();
        Thread.sleep(10000); // Give processor some time to cleanup the message
        processor.stop();
        Assert.assertTrue("The message should have been cleaned up since it was already processed",
                shard.next(subscriptionId, 20).isEmpty());
    }

    @Test(timeout=150000)
    public void testAtomicMessage() throws Exception {
        byte[] rowKey = Bytes.toBytes("row1");
        Put put = new Put(rowKey);
        put.add(RowLogTableUtil.DATA_COLUMN_FAMILY, Bytes.toBytes("column1"), Bytes.toBytes("aValue"));
        RowLogMessage message = rowLog.putMessage(rowKey, Bytes.toBytes("data"), Bytes.toBytes("payLoad"), put);
        rowTable.put(put);
        validationListener.expectMessage(message);
        validationListener.expectMessages(1);
        processor.start();
        validationListener.waitUntilMessagesConsumed(120000);
        // Sleep to allow processor to finish message processing (messageDone marking)
        Thread.sleep(2000);
        processor.stop();
        validationListener.validate();
    }

    @Test(timeout=150000)
    public void testAtomicMessageFailingPut() throws Exception {
        byte[] rowKey = Bytes.toBytes("row1");
        Put put = new Put(rowKey);
        put.add(RowLogTableUtil.DATA_COLUMN_FAMILY, Bytes.toBytes("column1"), Bytes.toBytes("aValue"));
        RowLogMessage message1 = rowLog.putMessage(rowKey, Bytes.toBytes("data"), Bytes.toBytes("payLoad"), put);
        // Don't perform rowTable.put(put) to simulate a failing put
        Put put2 = new Put(rowKey);
        put2.add(RowLogTableUtil.DATA_COLUMN_FAMILY, Bytes.toBytes("column1"), Bytes.toBytes("aValue2"));
        RowLogMessage message2 = rowLog.putMessage(rowKey, Bytes.toBytes("data2"), Bytes.toBytes("payLoad2"), put2);
        rowTable.put(put2);
        validationListener.expectMessage(message2); // We expect message1 to be discarded
        validationListener.expectMessages(1);
        processor.start();
        validationListener.waitUntilMessagesConsumed(120000);
        // Sleep to allow processor to finish message processing (messageDone marking)
        Thread.sleep(2000);
        processor.stop();
        validationListener.validate();
    }

    @Test(timeout=150000)
    public void testSingleMessageProcessorStartsFirst() throws Exception {
        validationListener.expectMessages(1);
        processor.start();
        System.out.println(">>RowLogEndToEndTest#"+name.getMethodName()+": processor started");
        RowLogMessage message = rowLog.putMessage(Bytes.toBytes("row2"), null, null, null);
        validationListener.expectMessage(message);
        System.out.println(">>RowLogEndToEndTest#"+name.getMethodName()+": waiting for message to be processed");
        validationListener.waitUntilMessagesConsumed(120000);
        System.out.println(">>RowLogEndToEndTest#"+name.getMethodName()+": message processed");
        // Sleep to allow processor to finish message processing (messageDone marking)
        Thread.sleep(2000);
        processor.stop();
        System.out.println(">>RowLogEndToEndTest#"+name.getMethodName()+": processor stopped");
        validationListener.validate();
    }

    @Test(timeout=150000)
    public void testMultipleMessagesSameRow() throws Exception {
        RowLogMessage message;
        validationListener.expectMessages(10);
        for (int i = 0; i < 10; i++) {
            byte[] rowKey = Bytes.toBytes("row3");
            message = rowLog.putMessage(rowKey, null, "aPayload".getBytes(), null);
            validationListener.expectMessage(message);
        }
        processor.start();
        validationListener.waitUntilMessagesConsumed(120000);
        // Sleep to allow processor to finish message processing (messageDone marking)
        Thread.sleep(2000);
        processor.stop();
        validationListener.validate();
    }

    @Test(timeout=150000)
    public void testMultipleMessagesMultipleRows() throws Exception {
        RowLogMessage message;
        validationListener.expectMessages(25);
        for (long seqnr = 0L; seqnr < 5; seqnr++) {
            for (int rownr = 10; rownr < 15; rownr++) {
                byte[] data = Bytes.toBytes(rownr);
                data = Bytes.add(data, Bytes.toBytes(seqnr));
                message = rowLog.putMessage(Bytes.toBytes("row" + rownr), data, null, null);
                validationListener.expectMessage(message);
            }
        }
        processor.start();
        validationListener.waitUntilMessagesConsumed(120000);
        // Sleep to allow processor to finish message processing (messageDone marking)
        Thread.sleep(2000);
        processor.stop();
        validationListener.validate();
    }

    public static void waitForSubscription(RowLog rowLog, String subscriptionId) throws InterruptedException {
        boolean subscriptionKnown = false;
        int timeOut = 10000;
        long waitUntil = System.currentTimeMillis() + 10000;
        while (!subscriptionKnown && System.currentTimeMillis() < waitUntil) {
            if (rowLog.getSubscriptionIds().contains(subscriptionId)) {
                subscriptionKnown = true;
                break;
            }
            Thread.sleep(10);
        }
        Assert.assertTrue("Subscription '" + subscriptionId + "' not known to rowlog within timeout " + timeOut + "ms",
                subscriptionKnown);
    }
}
