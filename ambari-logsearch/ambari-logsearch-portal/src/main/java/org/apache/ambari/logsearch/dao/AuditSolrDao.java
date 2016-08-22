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

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.PostConstruct;

import org.apache.ambari.logsearch.manager.MgrBase.LogType;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class AuditSolrDao extends SolrDaoBase {

  static private Logger logger = Logger.getLogger(AuditSolrDao.class);
  
  public AuditSolrDao() {
    super(LogType.AUDIT);
  }

  @PostConstruct
  public void postConstructor() {
    String solrUrl = PropertiesUtil.getProperty("logsearch.solr.audit.logs.url");
    String zkConnectString = PropertiesUtil.getProperty("logsearch.solr.audit.logs.zk_connect_string");
    String collection = PropertiesUtil.getProperty("logsearch.solr.collection.audit.logs", "audit_logs");
    String aliasNameIn = PropertiesUtil.getProperty("logsearch.solr.audit.logs.alias.name", "audit_logs_alias");
    String rangerAuditCollection = PropertiesUtil.getProperty("logsearch.ranger.audit.logs.collection.name");
    String splitInterval = PropertiesUtil.getProperty("logsearch.audit.logs.split.interval.mins", "none");
    String configName = PropertiesUtil.getProperty("logsearch.solr.audit.logs.config.name", "audit_logs");
    int numberOfShards = PropertiesUtil.getIntProperty("logsearch.collection.audit.logs.numshards", 1);
    int replicationFactor = PropertiesUtil.getIntProperty("logsearch.collection.audit.logs.replication.factor", 1);

    try {
      connectToSolr(solrUrl, zkConnectString, collection);
      
      boolean createAlias = (aliasNameIn != null && !StringUtils.isBlank(rangerAuditCollection));
      boolean needToPopulateSchemaField = !createAlias;
      
      setupCollections(splitInterval, configName, numberOfShards, replicationFactor, needToPopulateSchemaField);
      
      if (createAlias) {
        Collection<String> collectionsIn = Arrays.asList(collection, rangerAuditCollection.trim());
        setupAlias(aliasNameIn, collectionsIn);
      }
    } catch (Exception e) {
      logger.error("Error while connecting to Solr for audit logs : solrUrl=" + solrUrl + ", zkConnectString=" +
          zkConnectString + ", collection=" + collection, e);
    }
  }
}
