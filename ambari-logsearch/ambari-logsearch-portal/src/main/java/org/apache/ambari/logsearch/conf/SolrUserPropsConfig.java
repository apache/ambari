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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SolrUserPropsConfig extends SolrConnectionPropsConfig {

  @Value("${logsearch.solr.collection.history:history}")
  private String collection;

  @Value("${logsearch.history.split.interval.mins:none}")
  private String splitInterval;

  @Value("${logsearch.solr.history.config.name:history}")
  private String configName;

  @Value("${logsearch.collection.history.numshards:1}")
  private Integer numberOfShards;

  @Value("${logsearch.collection.history.replication.factor:2}")
  private Integer replicationFactor;

  @Value("#{'${logsearch.logfeeder.include.default.level:FATAL,ERROR,WARN,INFO,DEBUG,TRACE,UNKNOWN}'.split(',')}")
  private List<String> logLevels;

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

  public List<String> getLogLevels() {
    return logLevels;
  }

  public void setLogLevels(List<String> logLevels) {
    this.logLevels = logLevels;
  }
}
