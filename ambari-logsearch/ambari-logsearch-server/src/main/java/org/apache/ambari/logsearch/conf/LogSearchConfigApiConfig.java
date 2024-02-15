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

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogSearchConfigApiConfig {

  @LogSearchPropertyDescription(
    name = "logsearch.config.api.enabled",
    description = "Enable config API feature and shipperconfig API endpoints.",
    examples = {"false"},
    defaultValue = "true",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.config.api.enabled:true}")
  private boolean configApiEnabled;

  @LogSearchPropertyDescription(
    name = "logsearch.config.api.filter.solr.enabled",
    description = "Use solr as a log level filter storage",
    examples = {"true"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.config.api.filter.solr.enabled:false}")
  public boolean solrFilterStorage;

  @LogSearchPropertyDescription(
    name = "logsearch.config.api.filter.zk-only.enabled",
    description = "Use zookeeper as a log level filter storage",
    examples = {"true"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.config.api.filter.zk.enabled:false}")
  public boolean zkFilterStorage;

  public boolean isConfigApiEnabled() {
    return configApiEnabled;
  }

  public void setConfigApiEnabled(boolean configApiEnabled) {
    this.configApiEnabled = configApiEnabled;
  }

  public boolean isSolrFilterStorage() {
    return this.solrFilterStorage;
  }

  public void setSolrFilterStorage(boolean solrFilterStorage) {
    this.solrFilterStorage = solrFilterStorage;
  }

  public boolean isZkFilterStorage() {
    return zkFilterStorage;
  }

  public void setZkFilterStorage(boolean zkFilterStorage) {
    this.zkFilterStorage = zkFilterStorage;
  }
}
