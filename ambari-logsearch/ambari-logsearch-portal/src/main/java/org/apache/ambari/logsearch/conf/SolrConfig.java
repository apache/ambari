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
package org.apache.ambari.logsearch.conf;

public interface SolrConfig {
  String getSolrUrl();

  void setSolrUrl(String solrUrl);

  String getZkConnectString();

  void setZkConnectString(String zkConnectString);

  String getCollection();

  void setCollection(String collection);

  String getConfigName();

  void setConfigName(String configName);

  Integer getNumberOfShards();

  void setNumberOfShards(Integer numberOfShards);

  Integer getReplicationFactor();

  void setReplicationFactor(Integer replicationFactor);

  String getSplitInterval();

  void setSplitInterval(String splitInterval);

}
