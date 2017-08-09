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

import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

@Configuration
public class SolrEventHistoryPropsConfig extends SolrConnectionPropsConfig {

  @Value("${logsearch.solr.collection.history:history}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.collection.history",
    description = "Name of Log Search event history collection.",
    examples = {"history"},
    defaultValue = "history",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String collection;

  @Value("${logsearch.history.split.interval.mins:none}")
  @LogSearchPropertyDescription(
    name = "logsearch.history.split.interval.mins",
    description = "Will create multiple collections and use alias. (not supported right now, use implicit routingif the value is not none)",
    examples = {"none", "15"},
    defaultValue = "none",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String splitInterval;

  @Value("${logsearch.solr.history.config.name:history}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.history.config.name",
    description = "Solr configuration name of the event history collection.",
    examples = {"history"},
    defaultValue = "history",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String configName;

  @Value("${logsearch.collection.history.numshards:1}")
  @LogSearchPropertyDescription(
    name = "logsearch.collection.history.numshards",
    description = "Number of Solr shards for event history collection (bootstrapping).",
    examples = {"2"},
    defaultValue = "1",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Integer numberOfShards;

  @Value("${logsearch.collection.history.replication.factor:2}")
  @LogSearchPropertyDescription(
    name = "logsearch.collection.history.replication.factor",
    description = "Solr replication factor for event history collection (bootstrapping).",
    examples = {"3"},
    defaultValue = "2",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Integer replicationFactor;

  @Value("${logsearch.schema.fields.populate.interval.mins:1}")
  @LogSearchPropertyDescription(
    name = "logsearch.schema.fields.populate.interval.mins",
    description = "Interval in minutes for populating schema fiels for event history collections.",
    examples = {"10"},
    defaultValue = "1",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Integer populateIntervalMins;
  
  @Override
  public String getCollection() {
    return collection;
  }

  @Override
  public void setCollection(String collection) {
    this.collection = collection;
  }

  @Override
  public String getSplitInterval() {
    return splitInterval;
  }

  @Override
  public void setSplitInterval(String splitInterval) {
    this.splitInterval = splitInterval;
  }

  @Override
  public String getConfigName() {
    return configName;
  }

  @Override
  public void setConfigName(String configName) {
    this.configName = configName;
  }

  @Override
  public Integer getNumberOfShards() {
    return numberOfShards;
  }

  @Override
  public void setNumberOfShards(Integer numberOfShards) {
    this.numberOfShards = numberOfShards;
  }

  @Override
  public Integer getReplicationFactor() {
    return replicationFactor;
  }

  @Override
  public void setReplicationFactor(Integer replicationFactor) {
    this.replicationFactor = replicationFactor;
  }
  

  public Integer getPopulateIntervalMins() {
    return populateIntervalMins;
  }
  
  void setPopulateIntervalMins(Integer populateIntervalMins) {
    this.populateIntervalMins = populateIntervalMins;
  }

  @Override
  public String getLogType() {
    return null;
  }
}
