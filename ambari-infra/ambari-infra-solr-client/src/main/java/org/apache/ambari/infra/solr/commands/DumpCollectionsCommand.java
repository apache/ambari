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
package org.apache.ambari.infra.solr.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.ambari.infra.solr.AmbariSolrCloudClient;
import org.apache.ambari.infra.solr.domain.json.SolrCollection;
import org.apache.ambari.infra.solr.domain.json.SolrCoreData;
import org.apache.ambari.infra.solr.domain.json.SolrShard;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.SolrZooKeeper;
import org.apache.solr.common.cloud.ZkStateReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DumpCollectionsCommand extends AbstractZookeeperRetryCommand<String> {

  private static final Logger logger = LoggerFactory.getLogger(DumpCollectionsCommand.class);

  private final List<String> collections;

  public DumpCollectionsCommand(int maxRetries, int interval, List<String> collections) {
    super(maxRetries, interval);
    this.collections = collections;
  }

  @Override
  protected String executeZkCommand(AmbariSolrCloudClient client, SolrZkClient zkClient, SolrZooKeeper solrZooKeeper) throws Exception {
    Map<String, SolrCollection> collectionMap = new HashMap<>();
    if (!this.collections.isEmpty()) {
      for (String collection : this.collections) {
        SolrCollection solrCollection = new SolrCollection();
        CloudSolrClient solrClient = client.getSolrCloudClient();
        if (client.isIncludeDocNumber()) {
          long numberOfDocs = getNumberOfDocs(solrClient, collection);
          solrCollection.setNumberOfDocs(numberOfDocs);
        }
        Collection<Slice> slices = getSlices(solrClient, collection);
        Integer numShards = slices.size();
        Map<String, SolrShard> solrShardMap = new HashMap<>();
        Map<String, List<String>> leaderHostCoreMap = new HashMap<>();
        Map<String, SolrCoreData> leaderCoreDataMap = new HashMap<>();
        Map<String, List<String>> leaderShardCoreMap = new HashMap<>();
        Map<String, String> leaderCoreHostMap = new HashMap<>();
        for (Slice slice : slices) {
          SolrShard solrShard = new SolrShard();
          solrShard.setName(slice.getName());
          solrShard.setState(slice.getState());
          Collection<Replica> replicas = slice.getReplicas();
          Map<String, Replica> replicaMap = new HashMap<>();
          leaderShardCoreMap.put(slice.getName(), new ArrayList<>());
          for (Replica replica : replicas) {
            replicaMap.put(replica.getName(), replica);
            Replica.State state = replica.getState();
            if (Replica.State.ACTIVE.equals(state)
              && replica.getProperties().get("leader") != null && "true".equals(replica.getProperties().get("leader"))) {
              String coreName = replica.getCoreName();
              String hostName = getHostFromNodeName(replica.getNodeName());
              if (leaderHostCoreMap.containsKey(hostName)) {
                List<String> coresList = leaderHostCoreMap.get(hostName);
                coresList.add(coreName);
              } else {
                List<String> coreList = new ArrayList<>();
                coreList.add(coreName);
                leaderHostCoreMap.put(hostName, coreList);
              }
              Map<String, String> properties = new HashMap<>();
              properties.put("name", coreName);
              properties.put("coreNodeName", replica.getName());
              properties.put("shard", slice.getName());
              properties.put("collection", collection);
              properties.put("numShards", numShards.toString());
              properties.put("replicaType", replica.getType().name());
              SolrCoreData solrCoreData = new SolrCoreData(replica.getName(), hostName, properties);
              leaderCoreDataMap.put(coreName, solrCoreData);
              leaderShardCoreMap.get(slice.getName()).add(coreName);
              leaderCoreHostMap.put(coreName, hostName);
            }
          }
          solrShard.setReplicas(replicaMap);
          solrShardMap.put(slice.getName(), solrShard);
        }
        solrCollection.setShards(solrShardMap);
        solrCollection.setLeaderHostCoreMap(leaderHostCoreMap);
        solrCollection.setLeaderSolrCoreDataMap(leaderCoreDataMap);
        solrCollection.setLeaderShardsMap(leaderShardCoreMap);
        solrCollection.setLeaderCoreHostMap(leaderCoreHostMap);
        solrCollection.setName(collection);
        collectionMap.put(collection, solrCollection);
      }
    }
    ObjectMapper objectMapper = new ObjectMapper();
    final ObjectWriter objectWriter = objectMapper
      .writerWithDefaultPrettyPrinter();
    File file = new File(client.getOutput());
    if (!file.exists()) {
      file.createNewFile();
    }
    objectWriter.writeValue(file, collectionMap);
    return objectWriter.writeValueAsString(collectionMap);
  }

  private String getHostFromNodeName(String nodeName) {
    String[] splitted = nodeName.split(":");
    if (splitted.length > 0) {
      return splitted[0];
    } else {
      if (nodeName.endsWith("_solr")) {
        String[] splitted_ = nodeName.split("_");
        return splitted_[0];
      }
      return nodeName;
    }
  }

  private Collection<Slice> getSlices(CloudSolrClient solrClient, String collection) {
    ZkStateReader reader = solrClient.getZkStateReader();
    DocCollection docCollection = reader.getClusterState().getCollection(collection);
    return docCollection.getSlices();
  }

  private long getNumberOfDocs(CloudSolrClient solrClient, String collection) throws Exception {
    solrClient.setDefaultCollection(collection);
    SolrQuery q = new SolrQuery("*:*");
    q.setRows(0);
    return solrClient.query(q).getResults().getNumFound();
  }
}
