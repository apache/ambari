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
import org.apache.zookeeper.data.ACL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

@Configuration
public class SolrAuditLogPropsConfig implements SolrPropsConfig {

  @Value("${logsearch.solr.audit.logs.url:}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.audit.logs.url",
    description = "URL of Solr (non cloud mode) - currently unsupported.",
    examples = {"localhost1:8868"},
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String solrUrl;

  @Value("${logsearch.solr.audit.logs.zk_connect_string:}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.audit.logs.zk_connect_string",
    description = "Zookeeper connection string for Solr (used for audit log collection).",
    examples = {"localhost1:2181,localhost2:2181/mysolr_znode"},
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String zkConnectString;

  @Value("${logsearch.solr.collection.audit.logs:audit_logs}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.collection.audit.logs",
    description = "Name of Log Search audit collection.",
    examples = {"audit_logs"},
    defaultValue = "audit_logs",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String collection;

  @Value("${logsearch.ranger.audit.logs.collection.name:}")
  @LogSearchPropertyDescription(
    name = "logsearch.ranger.audit.logs.collection.name",
    description = "Name of Ranger audit collections (can be used if ranger audits managed by the same Solr which is used for Log Search).",
    examples = {"ranger_audits"},
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String rangerCollection;

  @Value("${logsearch.solr.audit.logs.config.name:audit_logs}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.audit.logs.config.name",
    description = "Solr configuration name of the audit collection.",
    examples = {"audit_logs"},
    defaultValue = "audit_logs",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String configName;

  @Value("${logsearch.solr.audit.logs.alias.name:audit_logs_alias}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.audit.logs.alias.name",
    description = "Alias name for audit log collection (can be used for Log Search audit collection and ranger collection as well).",
    examples = {"audit_logs_alias"},
    defaultValue = "audit_logs_alias",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String aliasNameIn;

  @Value("${logsearch.audit.logs.split.interval.mins:none}")
  @LogSearchPropertyDescription(
    name = "logsearch.audit.logs.split.interval.mins",
    description = "Will create multiple collections and use alias. (not supported right now, use implicit routingif the value is not none)",
    examples = {"none", "15"},
    defaultValue = "none",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String splitInterval;

  @Value("${logsearch.collection.audit.logs.numshards:1}")
  @LogSearchPropertyDescription(
    name = "logsearch.collection.audit.logs.numshards",
    description = "Number of Solr shards for audit collection (bootstrapping).",
    examples = {"2"},
    defaultValue = "1",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Integer numberOfShards;

  @Value("${logsearch.collection.audit.logs.replication.factor:1}")
  @LogSearchPropertyDescription(
    name = "logsearch.collection.audit.logs.replication.factor",
    description = "Solr replication factor for audit collection (bootstrapping).",
    examples = {"2"},
    defaultValue = "1",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Integer replicationFactor;

  @Value("#{ACLPropertiesSplitter.parseAcls('${logsearch.solr.audit.logs.zk.acls:}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.audit.logs.zk.acls",
    description = "List of Zookeeper ACLs for Log Search audit collection (Log Search and Solr must be able to read/write collection details)",
    examples = {"world:anyone:r,sasl:solr:cdrwa,sasl:logsearch:cdrwa"},
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<ACL> zkAcls;

  @Value("${logsearch.solr.audit.logs.config_set.folder:/usr/lib/ambari-logsearch-portal/conf/solr_configsets}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.audit.logs.config_set.folder",
    description = "Location of Log Search audit collection configs for Solr.",
    examples = {"/usr/lib/ambari-logsearch-portal/conf/solr_configsets"},
    defaultValue = "/usr/lib/ambari-logsearch-portal/conf/solr_configsets",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String configSetFolder;

  @LogSearchPropertyDescription(
    name = "logsearch.solr.implicit.routing",
    description = "Use implicit routing for Solr Collections.",
    examples = {"true"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.solr.implicit.routing:false}")
  private boolean solrImplicitRouting;

  @Override
  public String getSolrUrl() {
    return solrUrl;
  }

  @Override
  public void setSolrUrl(String solrUrl) {
    this.solrUrl = solrUrl;
  }

  @Override
  public String getCollection() {
    return collection;
  }

  @Override
  public void setCollection(String collection) {
    this.collection = collection;
  }

  @Override
  public String getZkConnectString() {
    return zkConnectString;
  }

  @Override
  public void setZkConnectString(String zkConnectString) {
    this.zkConnectString = zkConnectString;
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

  @Override
  public String getSplitInterval() {
    return splitInterval;
  }

  @Override
  public void setSplitInterval(String splitInterval) {
    this.splitInterval = splitInterval;
  }

  @Override
  public List<ACL> getZkAcls() {
    return zkAcls;
  }

  @Override
  public void setZkAcls(List<ACL> zkAcls) {
    this.zkAcls = zkAcls;
  }

  @Override
  public String getConfigSetFolder() {
    return configSetFolder;
  }

  @Override
  public void setConfigSetFolder(String configSetFolder) {
    this.configSetFolder = configSetFolder;
  }

  public String getRangerCollection() {
    return rangerCollection;
  }

  public void setRangerCollection(String rangerCollection) {
    this.rangerCollection = rangerCollection;
  }

  public String getAliasNameIn() {
    return aliasNameIn;
  }

  public void setAliasNameIn(String aliasNameIn) {
    this.aliasNameIn = aliasNameIn;
  }

  @Override
  public boolean isSolrImplicitRouting() {
    return solrImplicitRouting;
  }

  @Override
  public void setSolrImplicitRouting(boolean solrImplicitRouting) {
    this.solrImplicitRouting = solrImplicitRouting;
  }

  @Override
  public String getLogType() {
    return "audit";
  }
}
