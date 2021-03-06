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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lilyproject.rowlog.api.RowLogMessage;
import org.lilyproject.rowlog.api.RowLogMessageListenerMapping;
import org.lilyproject.rowlog.api.RowLogSubscription;

public class RowLogLocalEndToEndTest extends AbstractRowLogEndToEndTest {

    Log log = LogFactory.getLog(getClass());
    private ValidationMessageListener validationListener2;
    long t0 = 0;

    @Before
    public void setUp() throws Exception {
        System.out.println(">>RowLogLocalEndToEndTest#"+name.getMethodName());
        validationListener = new ValidationMessageListener("VML1", subscriptionId, rowLog);
        RowLogMessageListenerMapping.INSTANCE.put(subscriptionId , validationListener);
        rowLogConfigurationManager.addSubscription(rowLog.getId(), subscriptionId,  RowLogSubscription.Type.VM, 1);
        waitForSubscription(rowLog, subscriptionId);
        rowLogConfigurationManager.addListener(rowLog.getId(), subscriptionId, "listener1");
        t0 = System.currentTimeMillis();
    }

    @After
    public void tearDown() throws Exception {
        rowLogConfigurationManager.removeListener(rowLog.getId(), subscriptionId, "listener1");
        rowLogConfigurationManager.removeSubscription(rowLog.getId(), subscriptionId);
    }

    @Test(timeout=270000)
    public void testMultipleSubscriptions() throws Exception {
        String subscriptionId2 = "Subscription2";
        validationListener2 = new ValidationMessageListener("VML2", subscriptionId2, rowLog);
        RowLogMessageListenerMapping.INSTANCE.put(subscriptionId2, validationListener2);
        rowLogConfigurationManager.addSubscription(rowLog.getId(), subscriptionId2, RowLogSubscription.Type.VM, 2);
        waitForSubscription(rowLog, subscriptionId2); // Avoid putting messages on the rowlog before all subscriptions are setup
        rowLogConfigurationManager.addListener(rowLog.getId(), subscriptionId2, "Listener2");
        validationListener.expectMessages(10);
        validationListener2.expectMessages(10);
        RowLogMessage message;
        for (long seqnr = 0; seqnr < 2; seqnr++) {
            for (int rownr = 20; rownr < 25; rownr++) {
                byte[] data = Bytes.toBytes(rownr);
                data = Bytes.add(data, Bytes.toBytes(seqnr));
                message = rowLog.putMessage(Bytes.toBytes("row" + rownr), data, null, null);
                validationListener.expectMessage(message);
                validationListener2.expectMessage(message);
            }
        }
        processor.start();
        validationListener.waitUntilMessagesConsumed(120000);
        validationListener2.waitUntilMessagesConsumed(120000);
        processor.stop();
        validationListener2.validate();
        rowLogConfigurationManager.removeListener(rowLog.getId(), subscriptionId2, "Listener2");
        rowLogConfigurationManager.removeSubscription(rowLog.getId(), subscriptionId2);
        validationListener.validate();
    }

    @Test(timeout=270000)
    public void testMultipleSubscriptionsOrder() throws Exception {
        String subscriptionId2 = "Subscription2";
        validationListener2 = new ValidationMessageListener("VML2", subscriptionId2, rowLog);
        RowLogMessageListenerMapping.INSTANCE.put(subscriptionId2, validationListener2);
        rowLogConfigurationManager.addSubscription(rowLog.getId(), subscriptionId2, RowLogSubscription.Type.VM, 0);
        waitForSubscription(rowLog, subscriptionId2);
        rowLogConfigurationManager.addListener(rowLog.getId(), subscriptionId2, "Listener2");
        int rownr = 222;
        byte[] data = Bytes.toBytes(222);
        data = Bytes.add(data, Bytes.toBytes(0));
        RowLogMessage message = rowLog.putMessage(Bytes.toBytes("row" + rownr), data, null, null);
        validationListener.expectMessages(1);
        validationListener.expectMessage(message);
        
        validationListener2.messagesToFail.add(message);
        validationListener2.expectMessage(message, 2);
        validationListener2.expectMessages(2);

        processor.start();
        validationListener.waitUntilMessagesConsumed(120000);
        validationListener2.waitUntilMessagesConsumed(120000);
        processor.stop();
        validationListener2.validate();
     // Assert the message was not processed by subscription1 (last in order) before subscription2 (first in order) processed it
        //TODO
        rowLogConfigurationManager.removeListener(rowLog.getId(), subscriptionId2, "Listener2");
        rowLogConfigurationManager.removeSubscription(rowLog.getId(), subscriptionId2);
    } 
}