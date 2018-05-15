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
package org.apache.ambari.logsearch.handler;

import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.config.api.LogSearchConfigServer;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputSolrProperties;
import org.apache.ambari.logsearch.config.zookeeper.model.outputconfig.impl.OutputSolrPropertiesImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkStateReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.ROUTER_FIELD;

public class CreateCollectionHandler implements SolrZkRequestHandler<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(CreateCollectionHandler.class);

  private static final String MODIFY_COLLECTION_QUERY = "/admin/collections?action=MODIFYCOLLECTION&collection=%s&%s=%d";
  private static final String MAX_SHARDS_PER_NODE = "maxShardsPerNode";

  private final LogSearchConfigServer logSearchConfig;
  private final List<String> allCollectionList;

  public CreateCollectionHandler(LogSearchConfigServer logSearchConfig, List<String> allCollectionList) {
    this.logSearchConfig = logSearchConfig;
    this.allCollectionList = allCollectionList;
  }

  @Override
  public Boolean handle(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) throws Exception {
    if (solrPropsConfig.getLogType() != null) {
      OutputSolrProperties outputSolrProperties = new OutputSolrPropertiesImpl(solrPropsConfig.getCollection(),
          solrPropsConfig.getSplitInterval());
      logSearchConfig.saveOutputSolrProperties(solrPropsConfig.getLogType(), outputSolrProperties);
    }

    boolean result;
    if (solrPropsConfig.isSolrImplicitRouting()) {
      result = setupCollectionsWithImplicitRouting(solrClient, solrPropsConfig, this.allCollectionList);
    } else {
      result = createCollection(solrClient, solrPropsConfig, this.allCollectionList);
    }

    return result;
  }

  private boolean setupCollectionsWithImplicitRouting(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig, List<String> allCollectionList)
    throws Exception {
    LOG.info("setupCollectionsWithImplicitRouting(). collectionName=" + solrPropsConfig.getCollection()
      + ", numberOfShards=" + solrPropsConfig.getNumberOfShards());

    // Default is true, because if the collection and shard is already there, then it will return true
    boolean returnValue = true;

    List<String> shardsList = new ArrayList<>();
    for (int i = 0; i < solrPropsConfig.getNumberOfShards(); i++) {
      shardsList.add("shard" + i);
    }
    String shardsListStr = StringUtils.join(shardsList, ',');

    // Check if collection is already in zookeeper
    if (!allCollectionList.contains(solrPropsConfig.getCollection())) {
      LOG.info("Creating collection " + solrPropsConfig.getCollection() + ", shardsList=" + shardsList);
      CollectionAdminRequest.Create collectionCreateRequest = CollectionAdminRequest.createCollection(
          solrPropsConfig.getCollection(), solrPropsConfig.getConfigName(), solrPropsConfig.getNumberOfShards(),
          solrPropsConfig.getReplicationFactor());
      collectionCreateRequest.setRouterName("implicit");
      collectionCreateRequest.setShards(shardsListStr);
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
      Collection<Slice> slices = getSlices(solrClient, solrPropsConfig);
      Collection<String> existingShards = getShards(slices, solrPropsConfig);
      if (existingShards.size() < shardsList.size()) {
        try {
          updateMaximumNumberOfShardsPerCore(slices, solrPropsConfig);
        } catch (Throwable t) {
          returnValue = false;
          LOG.error(String.format("Exception during updating collection (%s)", t));
        }
      }
      for (String shard : shardsList) {
        if (!existingShards.contains(shard)) {
          try {
            LOG.info("Going to add Shard " + shard + " to collection " + solrPropsConfig.getCollection());
            CollectionAdminRequest.CreateShard createShardRequest =
                CollectionAdminRequest.createShard(solrPropsConfig.getCollection(), shard);
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

  private boolean createCollection(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig, List<String> allCollectionList)
      throws SolrServerException, IOException {

    if (allCollectionList.contains(solrPropsConfig.getCollection())) {
      LOG.info("Collection " + solrPropsConfig.getCollection() + " is already there. Won't create it");
      return true;
    }

    LOG.info("Creating collection " + solrPropsConfig.getCollection() + ", numberOfShards=" + solrPropsConfig.getNumberOfShards() +
      ", replicationFactor=" + solrPropsConfig.getReplicationFactor());

    CollectionAdminRequest.Create collectionCreateRequest = CollectionAdminRequest.createCollection(
        solrPropsConfig.getCollection(), solrPropsConfig.getConfigName(), solrPropsConfig.getNumberOfShards(),
        solrPropsConfig.getReplicationFactor());
    collectionCreateRequest.setMaxShardsPerNode(calculateMaxShardsPerNode(solrPropsConfig));
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

  private void updateMaximumNumberOfShardsPerCore(Collection<Slice> slices, SolrPropsConfig solrPropsConfig) throws IOException {
    String baseUrl = getRandomBaseUrl(slices);
    if (baseUrl != null) {
      CloseableHttpClient httpClient = HttpClientUtil.createClient(null);
      HttpGet request = new HttpGet(baseUrl + String.format(MODIFY_COLLECTION_QUERY,
        solrPropsConfig.getCollection(), MAX_SHARDS_PER_NODE, calculateMaxShardsPerNode(solrPropsConfig)));
      HttpResponse response = httpClient.execute(request);
      if (response.getStatusLine().getStatusCode() != Response.Status.OK.getStatusCode()) {
        throw new IllegalStateException(String.format("Cannot update collection (%s) - increase max number of nodes per core", solrPropsConfig.getCollection()));
      }
    } else {
      throw new IllegalStateException(String.format("Cannot get any core url for updating collection (%s)", solrPropsConfig.getCollection()));
    }
  }

  private Collection<Slice> getSlices(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) {
    ZkStateReader reader = solrClient.getZkStateReader();
    DocCollection collection = reader.getClusterState().getCollection(solrPropsConfig.getCollection());
    return collection.getSlices();
  }

  private Collection<String> getShards(Collection<Slice> slices, SolrPropsConfig solrPropsConfig) {
    Collection<String> list = new HashSet<>();
    for (Slice slice : slices) {
      for (Replica replica : slice.getReplicas()) {
        LOG.info("colName=" + solrPropsConfig.getCollection() + ", slice.name=" + slice.getName() + ", slice.state=" + slice.getState() +
          ", replica.core=" + replica.getStr("core") + ", replica.state=" + replica.getStr("state"));
        list.add(slice.getName());
      }
    }
    return list;
  }

  private String getRandomBaseUrl(Collection<Slice> slices) {
    String coreUrl = null;
    if (slices != null) {
      for (Slice slice : slices) {
        if (!slice.getReplicas().isEmpty()) {
          Replica replica = slice.getReplicas().iterator().next();
          coreUrl = replica.getStr("base_url");
          if (coreUrl != null) {
            break;
          }
        }
      }
    }
    return coreUrl;
  }

  private Integer calculateMaxShardsPerNode(SolrPropsConfig solrPropsConfig) {
    return solrPropsConfig.getReplicationFactor() * solrPropsConfig.getNumberOfShards();
  }

}
