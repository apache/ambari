/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.dao;

import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkStateReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Component
public class SolrCollectionDao {

  private static final Logger LOG = LoggerFactory.getLogger(SolrCollectionDao.class);

  private static final String ROUTER_FIELD = "_router_field_";
  private static final int SETUP_RETRY_SECOND = 30;

  /**
   * This will try to get the collections from the Solr. Ping doesn't work if
   * collection is not given
   */
  public boolean checkSolrStatus(CloudSolrClient cloudSolrClient) {
    int waitDurationMS = 3 * 60 * 1000;
    boolean status = false;
    try {
      long beginTimeMS = System.currentTimeMillis();
      long waitIntervalMS = 2000;
      int pingCount = 0;
      while (true) {
        pingCount++;
        try {
          List<String> collectionList = getCollections(cloudSolrClient);
          if (collectionList != null) {
            LOG.info("checkSolrStatus(): Solr getCollections() is success. collectionList=" + collectionList);
            status = true;
            break;
          }
        } catch (Exception ex) {
          LOG.error("Error while doing Solr check", ex);
        }
        if (System.currentTimeMillis() - beginTimeMS > waitDurationMS) {
          LOG.error("Solr is not reachable even after " + (System.currentTimeMillis() - beginTimeMS) + " ms. " +
            "If you are using alias, then you might have to restart LogSearch after Solr is up and running.");
          break;
        } else {
          LOG.warn("Solr is not not reachable yet. getCollections() attempt count=" + pingCount + ". " +
            "Will sleep for " + waitIntervalMS + " ms and try again.");
        }
        Thread.sleep(waitIntervalMS);

      }
    } catch (Throwable t) {
      LOG.error("Seems Solr is not up.");
    }
    return status;
  }

  public void setupCollections(final CloudSolrClient solrClient, final SolrPropsConfig solrPropsConfig) throws Exception {
    boolean setupStatus = createCollectionsIfNeeded(solrClient, solrPropsConfig);
    LOG.info("Setup status for " + solrPropsConfig.getCollection() + " is " + setupStatus);
    if (!setupStatus) {
      // Start a background thread to do setup
      Thread setupThread = new Thread("setup_collection_" + solrPropsConfig.getCollection()) {
        @Override
        public void run() {
          LOG.info("Started monitoring thread to check availability of Solr server. collection=" + solrPropsConfig.getCollection());
          int retryCount = 0;
          while (true) {
            try {
              Thread.sleep(SETUP_RETRY_SECOND * 1000);
              retryCount++;
              boolean setupStatus = createCollectionsIfNeeded(solrClient, solrPropsConfig);
              if (setupStatus) {
                LOG.info("Setup for collection " + solrPropsConfig.getCollection() + " is successful. Exiting setup retry thread");
                break;
              }
            } catch (InterruptedException sleepInterrupted) {
              LOG.info("Sleep interrupted while setting up collection " + solrPropsConfig.getCollection());
              break;
            } catch (Exception e) {
              LOG.error("Error setting up collection=" + solrPropsConfig.getCollection(), e);
            }
            LOG.error("Error setting collection. collection=" + solrPropsConfig.getCollection() + ", retryCount=" + retryCount);
          }
        }
      };
      setupThread.setDaemon(true);
      setupThread.start();
    }
  }

  private boolean createCollectionsIfNeeded(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) {
    boolean result = false;
    try {
      List<String> allCollectionList = getCollections(solrClient);
      if (solrPropsConfig.getSplitInterval().equalsIgnoreCase("none")) {
        result = createCollection(solrClient, solrPropsConfig, allCollectionList);
      } else {
        result = setupCollectionsWithImplicitRouting(solrClient, solrPropsConfig, allCollectionList);
      }
    } catch (Exception ex) {
      LOG.error("Error creating collection. collectionName=" + solrPropsConfig.getCollection(), ex);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public List<String> getCollections(CloudSolrClient solrClient) throws SolrServerException,
    IOException {
    try {
      CollectionAdminRequest.List colListReq = new CollectionAdminRequest.List();
      CollectionAdminResponse response = colListReq.process(solrClient);
      if (response.getStatus() != 0) {
        LOG.error("Error getting collection list from solr.  response=" + response);
        return null;
      }
      return (List<String>) response.getResponse().get("collections");
    } catch (SolrException e) {
      LOG.error("getCollections() operation failed", e);
      return new ArrayList<>();
    }
  }

  private boolean setupCollectionsWithImplicitRouting(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig, List<String> allCollectionList)
    throws Exception {
    LOG.info("setupCollectionsWithImplicitRouting(). collectionName=" + solrPropsConfig.getCollection()
      + ", numberOfShards=" + solrPropsConfig.getNumberOfShards());

    // Default is true, because if the collection and shard is already there, then it will return true
    boolean returnValue = true;

    List<String> shardsList = new ArrayList<String>();
    for (int i = 0; i < solrPropsConfig.getNumberOfShards(); i++) {
      shardsList.add("shard" + i);
    }
    String shardsListStr = StringUtils.join(shardsList, ',');

    // Check if collection is already in zookeeper
    if (!allCollectionList.contains(solrPropsConfig.getCollection())) {
      LOG.info("Creating collection " + solrPropsConfig.getCollection() + ", shardsList=" + shardsList);
      CollectionAdminRequest.Create collectionCreateRequest = new CollectionAdminRequest.Create();
      collectionCreateRequest.setCollectionName(solrPropsConfig.getCollection());
      collectionCreateRequest.setRouterName("implicit");
      collectionCreateRequest.setShards(shardsListStr);
      collectionCreateRequest.setNumShards(solrPropsConfig.getNumberOfShards());
      collectionCreateRequest.setReplicationFactor(solrPropsConfig.getReplicationFactor());
      collectionCreateRequest.setConfigName(solrPropsConfig.getConfigName());
      collectionCreateRequest.setRouterField(ROUTER_FIELD);
      collectionCreateRequest.setMaxShardsPerNode(solrPropsConfig.getReplicationFactor() * solrPropsConfig.getNumberOfShards());

      CollectionAdminResponse createResponse = collectionCreateRequest.process(solrClient);
      if (createResponse.getStatus() != 0) {
        returnValue = false;
        LOG.error("Error creating collection. collectionName=" + solrPropsConfig.getCollection()
          + ", shardsList=" + shardsList +", response=" + createResponse);
      } else {
        LOG.info("Created collection " + solrPropsConfig.getCollection() + ", shardsList=" + shardsList);
      }
    } else {
      LOG.info("Collection " + solrPropsConfig.getCollection() + " is already there. Will check whether it has the required shards");
      Collection<String> existingShards = getShards(solrClient, solrPropsConfig);
      for (String shard : shardsList) {
        if (!existingShards.contains(shard)) {
          try {
            LOG.info("Going to add Shard " + shard + " to collection " + solrPropsConfig.getCollection());
            CollectionAdminRequest.CreateShard createShardRequest = new CollectionAdminRequest.CreateShard();
            createShardRequest.setCollectionName(solrPropsConfig.getCollection());
            createShardRequest.setShardName(shard);
            CollectionAdminResponse response = createShardRequest.process(solrClient);
            if (response.getStatus() != 0) {
              LOG.error("Error creating shard " + shard + " in collection " + solrPropsConfig.getCollection() + ", response=" + response);
              returnValue = false;
              break;
            } else {
              LOG.info("Successfully created shard " + shard + " in collection " + solrPropsConfig.getCollection());
            }
          } catch (Throwable t) {
            LOG.error("Error creating shard " + shard + " in collection " + solrPropsConfig.getCollection(), t);
            returnValue = false;
            break;
          }
        }
      }
    }
    return returnValue;
  }

  private Collection<String> getShards(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) {
    Collection<String> list = new HashSet<>();
    ZkStateReader reader = solrClient.getZkStateReader();
    Collection<Slice> slices = reader.getClusterState().getSlices(solrPropsConfig.getCollection());
    for (Slice slice : slices) {
      for (Replica replica : slice.getReplicas()) {
        LOG.info("colName=" + solrPropsConfig.getCollection() + ", slice.name=" + slice.getName() + ", slice.state=" + slice.getState() +
          ", replica.core=" + replica.getStr("core") + ", replica.state=" + replica.getStr("state"));
        list.add(slice.getName());
      }
    }
    return list;
  }

  private boolean createCollection(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig, List<String> allCollectionList) throws SolrServerException, IOException {

    if (allCollectionList.contains(solrPropsConfig.getCollection())) {
      LOG.info("Collection " + solrPropsConfig.getCollection() + " is already there. Won't create it");
      return true;
    }

    LOG.info("Creating collection " + solrPropsConfig.getCollection() + ", numberOfShards=" + solrPropsConfig.getNumberOfShards() +
      ", replicationFactor=" + solrPropsConfig.getReplicationFactor());

    CollectionAdminRequest.Create collectionCreateRequest = new CollectionAdminRequest.Create();
    collectionCreateRequest.setCollectionName(solrPropsConfig.getCollection());
    collectionCreateRequest.setNumShards(solrPropsConfig.getNumberOfShards());
    collectionCreateRequest.setReplicationFactor(solrPropsConfig.getReplicationFactor());
    collectionCreateRequest.setConfigName(solrPropsConfig.getConfigName());
    collectionCreateRequest.setMaxShardsPerNode(solrPropsConfig.getReplicationFactor() * solrPropsConfig.getNumberOfShards());
    CollectionAdminResponse createResponse = collectionCreateRequest.process(solrClient);
    if (createResponse.getStatus() != 0) {
      LOG.error("Error creating collection. collectionName=" + solrPropsConfig.getCollection() + ", response=" + createResponse);
      return false;
    } else {
      LOG.info("Created collection " + solrPropsConfig.getCollection() + ", numberOfShards=" + solrPropsConfig.getNumberOfShards() +
        ", replicationFactor=" + solrPropsConfig.getReplicationFactor());
      return true;
    }
  }
}
