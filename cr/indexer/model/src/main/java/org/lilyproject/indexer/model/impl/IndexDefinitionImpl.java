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
package org.lilyproject.indexer.model.impl;

import org.lilyproject.indexer.model.api.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IndexDefinitionImpl implements IndexDefinition {
    private String name;
    private IndexGeneralState generalState = IndexGeneralState.ACTIVE;
    private IndexBatchBuildState buildState = IndexBatchBuildState.INACTIVE;
    private IndexUpdateState updateState = IndexUpdateState.SUBSCRIBE_AND_LISTEN;
    private String queueSubscriptionId;
    private byte[] configuration;
    private byte[] shardingConfiguration;
    private byte[] defaultBatchIndexConfiguration;
    private byte[] batchIndexConfiguration;
    private Map<String, String> solrShards = Collections.emptyMap();
    private int zkDataVersion = -1;
    private BatchBuildInfo lastBatchBuildInfo;
    private ActiveBatchBuildInfo activeBatchBuildInfo;
    private boolean immutable;

    public IndexDefinitionImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IndexGeneralState getGeneralState() {
        return generalState;
    }

    @Override
    public void setGeneralState(IndexGeneralState state) {
        checkIfMutable();
        this.generalState = state;
    }

    @Override
    public IndexBatchBuildState getBatchBuildState() {
        return buildState;
    }

    @Override
    public void setBatchBuildState(IndexBatchBuildState state) {
        checkIfMutable();
        this.buildState = state;
    }

    @Override
    public IndexUpdateState getUpdateState() {
        return updateState;
    }

    @Override
    public void setUpdateState(IndexUpdateState state) {
        checkIfMutable();
        this.updateState = state;
    }

    @Override
    public String getQueueSubscriptionId() {
        return queueSubscriptionId;
    }

    @Override
    public void setQueueSubscriptionId(String queueSubscriptionId) {
        checkIfMutable();
        this.queueSubscriptionId = queueSubscriptionId;
    }

    @Override
    public byte[] getConfiguration() {
        // Note that while one could modify the returned byte array, it is very unlikely to do this
        // by accident, and we assume cooperating users.
        return configuration;
    }

    @Override
    public void setConfiguration(byte[] configuration) {
        this.configuration = configuration;
    }

    @Override
    public byte[] getShardingConfiguration() {
        return shardingConfiguration;
    }

    @Override
    public void setShardingConfiguration(byte[] shardingConfiguration) {
        this.shardingConfiguration = shardingConfiguration;
    }

    @Override
    public Map<String, String> getSolrShards() {
        return new HashMap<String, String>(solrShards);
    }

    @Override
    public void setSolrShards(Map<String, String> shards) {
        this.solrShards = new HashMap<String, String>(shards);
    }

    @Override
    public int getZkDataVersion() {
        return zkDataVersion;
    }

    public void setZkDataVersion(int zkDataVersion) {
        checkIfMutable();
        this.zkDataVersion = zkDataVersion;
    }

    @Override
    public BatchBuildInfo getLastBatchBuildInfo() {
        return lastBatchBuildInfo;
    }

    @Override
    public void setLastBatchBuildInfo(BatchBuildInfo info) {
        checkIfMutable();
        this.lastBatchBuildInfo = info;
    }

    @Override
    public ActiveBatchBuildInfo getActiveBatchBuildInfo() {
        return activeBatchBuildInfo;
    }

    @Override
    public void setActiveBatchBuildInfo(ActiveBatchBuildInfo info) {
        checkIfMutable();
        this.activeBatchBuildInfo = info;
    }

    public void makeImmutable() {
        this.immutable = true;
        if (lastBatchBuildInfo != null)
            lastBatchBuildInfo.makeImmutable();
        if (activeBatchBuildInfo != null)
            activeBatchBuildInfo.makeImmutable();
    }

    private void checkIfMutable() {
        if (immutable)
            throw new RuntimeException("This IndexDefinition is immutable");
    }

    public byte[] getDefaultBatchIndexConfiguration() {
        return defaultBatchIndexConfiguration;
    }

    public void setDefaultBatchIndexConfiguration(byte[] defaultBatchIndexConfiguration) {
        this.defaultBatchIndexConfiguration = defaultBatchIndexConfiguration;
    }

    public byte[] getBatchIndexConfiguration() {
        return batchIndexConfiguration;
    }

    public void setBatchIndexConfiguration(byte[] batchIndexConfiguration) {
        this.batchIndexConfiguration = batchIndexConfiguration;
    }
}
