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
package org.lilyproject.util.hbase;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTableInterface;
import static org.lilyproject.util.hbase.LilyHBaseSchema.*;

public class HBaseTableUtil {    

    public static HTableInterface getRecordTable(Configuration configuration) throws IOException {
        HBaseAdmin admin = new HBaseAdmin(configuration);
        try {
            admin.getTableDescriptor(Table.RECORD.bytes);
        } catch (TableNotFoundException e) {
            try {
                HTableDescriptor tableDescriptor = new HTableDescriptor(Table.RECORD.bytes);
                tableDescriptor.addFamily(new HColumnDescriptor(RecordCf.NON_VERSIONED_SYSTEM.bytes));
                tableDescriptor.addFamily(new HColumnDescriptor(RecordCf.VERSIONED_SYSTEM.bytes, HConstants.ALL_VERSIONS, "none", false, true, HConstants.FOREVER, HColumnDescriptor.DEFAULT_BLOOMFILTER));
                tableDescriptor.addFamily(new HColumnDescriptor(RecordCf.NON_VERSIONED.bytes));
                tableDescriptor.addFamily(new HColumnDescriptor(RecordCf.VERSIONED.bytes, HConstants.ALL_VERSIONS, "none", false, true, HConstants.FOREVER, HColumnDescriptor.DEFAULT_BLOOMFILTER));
                tableDescriptor.addFamily(new HColumnDescriptor(RecordCf.VERSIONED_MUTABLE.bytes, HConstants.ALL_VERSIONS, "none", false, true, HConstants.FOREVER, HColumnDescriptor.DEFAULT_BLOOMFILTER));
                tableDescriptor.addFamily(new HColumnDescriptor(RecordCf.WAL_PAYLOAD.bytes));
                tableDescriptor.addFamily(new HColumnDescriptor(RecordCf.WAL_STATE.bytes));
                tableDescriptor.addFamily(new HColumnDescriptor(RecordCf.MQ_PAYLOAD.bytes));
                tableDescriptor.addFamily(new HColumnDescriptor(RecordCf.MQ_STATE.bytes));
                admin.createTable(tableDescriptor);
            } catch (TableExistsException e2) {
                // Likely table is created by another process
            }
        }

        return new LocalHTable(configuration, Table.RECORD.bytes);
    }

    public static HTableInterface getTypeTable(Configuration configuration) throws IOException {
        HBaseAdmin admin = new HBaseAdmin(configuration);

        try {
            admin.getTableDescriptor(Table.TYPE.bytes);
        } catch (TableNotFoundException e) {
            try {
                HTableDescriptor tableDescriptor = new HTableDescriptor(Table.TYPE.bytes);
                tableDescriptor.addFamily(new HColumnDescriptor(TypeCf.DATA.bytes));
                tableDescriptor.addFamily(new HColumnDescriptor(TypeCf.FIELDTYPE_ENTRY.bytes, HConstants.ALL_VERSIONS,
                        "none", false, true, HConstants.FOREVER, HColumnDescriptor.DEFAULT_BLOOMFILTER));
                tableDescriptor.addFamily(new HColumnDescriptor(TypeCf.MIXIN.bytes, HConstants.ALL_VERSIONS, "none",
                        false, true, HConstants.FOREVER, HColumnDescriptor.DEFAULT_BLOOMFILTER));
                admin.createTable(tableDescriptor);
            } catch (TableExistsException e2) {
                // Likely table is created by another process
            }
        }

        return new LocalHTable(configuration, Table.TYPE.bytes);
    }
}