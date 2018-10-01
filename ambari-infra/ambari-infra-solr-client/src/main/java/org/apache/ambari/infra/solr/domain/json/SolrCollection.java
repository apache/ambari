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
package org.apache.ambari.infra.solr.domain.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrCollection {
  private String name;
  private long numberOfDocs = -1;
  private Map<String, SolrShard> shards = new HashMap<>();
  private Map<String, List<String>> leaderHostCoreMap = new HashMap<>();
  private Map<String, SolrCoreData> leaderSolrCoreDataMap = new HashMap<>();
  private Map<String, List<String>> leaderShardsMap = new HashMap<>();
  private Map<String, String> leaderCoreHostMap = new HashMap<>();

  public long getNumberOfDocs() {
    return numberOfDocs;
  }

  public void setNumberOfDocs(long numberOfDocs) {
    this.numberOfDocs = numberOfDocs;
  }

  public Map<String, SolrShard> getShards() {
    return shards;
  }

  public void setShards(Map<String, SolrShard> shards) {
    this.shards = shards;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, List<String>> getLeaderHostCoreMap() {
    return leaderHostCoreMap;
  }

  public void setLeaderHostCoreMap(Map<String, List<String>> leaderHostCoreMap) {
    this.leaderHostCoreMap = leaderHostCoreMap;
  }

  public Map<String, SolrCoreData> getLeaderSolrCoreDataMap() {
    return leaderSolrCoreDataMap;
  }

  public void setLeaderSolrCoreDataMap(Map<String, SolrCoreData> leaderSolrCoreDataMap) {
    this.leaderSolrCoreDataMap = leaderSolrCoreDataMap;
  }

  public Map<String, List<String>> getLeaderShardsMap() {
    return leaderShardsMap;
  }

  public void setLeaderShardsMap(Map<String, List<String>> leaderShardsMap) {
    this.leaderShardsMap = leaderShardsMap;
  }

  public Map<String, String> getLeaderCoreHostMap() {
    return leaderCoreHostMap;
  }

  public void setLeaderCoreHostMap(Map<String, String> leaderCoreHostMap) {
    this.leaderCoreHostMap = leaderCoreHostMap;
  }
}
