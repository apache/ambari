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

import javax.annotation.PostConstruct;

import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class AuditSolrDao extends SolrDaoBase {

  static private Logger logger = Logger.getLogger(AuditSolrDao.class);

  @PostConstruct
  public void postConstructor() {
    String solrUrl = PropertiesUtil.getProperty("auditlog.solr.url");
    String zkHosts = PropertiesUtil.getProperty("auditlog.solr.zkhosts");
    String collection = PropertiesUtil.getProperty(
      "auditlog.solr.core.logs", "audit_logs");
    String splitInterval = PropertiesUtil.getProperty(
      "solr.audit_logs.split_interval_mins", "none");
    String configName = PropertiesUtil.getProperty(
      "solr.audit_logs.config_name", "audit_logs");
    int numberOfShards = PropertiesUtil.getIntProperty(
      "solr.audit_logs.shards", 1);
    int replicationFactor = PropertiesUtil.getIntProperty(
      "solr.audit_logs.replication_factor", 1);

    try {
      connectToSolr(solrUrl, zkHosts, collection);
      setupCollections(splitInterval, configName, numberOfShards,
        replicationFactor);
    } catch (Exception e) {
      logger.error(
        "Error while connecting to Solr for audit logs : solrUrl="
          + solrUrl + ", zkHosts=" + zkHosts
          + ", collection=" + collection, e);
    }
  }

}
