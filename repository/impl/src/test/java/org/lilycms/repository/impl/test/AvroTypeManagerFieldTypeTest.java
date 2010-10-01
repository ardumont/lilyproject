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
package org.lilycms.repository.impl.test;



import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.avro.ipc.HttpServer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.lilycms.repository.api.BlobStoreAccessFactory;
import org.lilycms.repository.api.Repository;
import org.lilycms.repository.api.TypeManager;
import org.lilycms.repository.avro.AvroConverter;
import org.lilycms.repository.avro.AvroLily;
import org.lilycms.repository.avro.AvroLilyImpl;
import org.lilycms.repository.avro.LilySpecificResponder;
import org.lilycms.repository.impl.DFSBlobStoreAccess;
import org.lilycms.repository.impl.HBaseRepository;
import org.lilycms.repository.impl.HBaseTableUtil;
import org.lilycms.repository.impl.HBaseTypeManager;
import org.lilycms.repository.impl.IdGeneratorImpl;
import org.lilycms.repository.impl.RemoteRepository;
import org.lilycms.repository.impl.RemoteTypeManager;
import org.lilycms.repository.impl.SizeBasedBlobStoreAccessFactory;
import org.lilycms.rowlog.api.RowLog;
import org.lilycms.rowlog.api.RowLogException;
import org.lilycms.rowlog.api.RowLogShard;
import org.lilycms.rowlog.impl.RowLogImpl;
import org.lilycms.rowlog.impl.RowLogShardImpl;
import org.lilycms.testfw.HBaseProxy;
import org.lilycms.testfw.TestHelper;

public class AvroTypeManagerFieldTypeTest extends AbstractTypeManagerFieldTypeTest {
    private static HBaseRepository serverRepository;
    private static RowLog wal;

    private final static HBaseProxy HBASE_PROXY = new HBaseProxy();
    private static Configuration configuration;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TestHelper.setupLogging();
        HBASE_PROXY.start();
        IdGeneratorImpl idGenerator = new IdGeneratorImpl();
        configuration = HBASE_PROXY.getConf();
        TypeManager serverTypeManager = new HBaseTypeManager(idGenerator, configuration);
        DFSBlobStoreAccess dfsBlobStoreAccess = new DFSBlobStoreAccess(HBASE_PROXY.getBlobFS(), new Path("/lily/blobs"));
        BlobStoreAccessFactory blobStoreAccessFactory = new SizeBasedBlobStoreAccessFactory(dfsBlobStoreAccess);
        
        serverRepository = new HBaseRepository(serverTypeManager, idGenerator, blobStoreAccessFactory, wal, configuration);
        
        AvroConverter serverConverter = new AvroConverter();
        serverConverter.setRepository(serverRepository);
        HttpServer lilyServer = new HttpServer(
                new LilySpecificResponder(AvroLily.class, new AvroLilyImpl(serverRepository, serverConverter),
                        serverConverter), 0);
        AvroConverter remoteConverter = new AvroConverter();
        typeManager = new RemoteTypeManager(new InetSocketAddress(lilyServer.getPort()),
                remoteConverter, idGenerator);
        Repository repository = new RemoteRepository(new InetSocketAddress(lilyServer.getPort()),
                remoteConverter, (RemoteTypeManager)typeManager, idGenerator, blobStoreAccessFactory);
        remoteConverter.setRepository(repository);

    }
    
    protected static void setupWal() throws IOException, RowLogException {
        wal = new RowLogImpl("WAL", HBaseTableUtil.getRecordTable(configuration), HBaseTableUtil.WAL_PAYLOAD_COLUMN_FAMILY, HBaseTableUtil.WAL_COLUMN_FAMILY, 10000L, true, configuration);
        RowLogShard walShard = new RowLogShardImpl("WS1", configuration, wal, 100);
        wal.registerShard(walShard);
    }
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        serverRepository.stop();
        HBASE_PROXY.stop();
    }


    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

}
