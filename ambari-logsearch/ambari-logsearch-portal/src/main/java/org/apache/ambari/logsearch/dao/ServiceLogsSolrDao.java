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

import org.apache.ambari.logsearch.manager.MgrBase.LogType;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class ServiceLogsSolrDao extends SolrDaoBase {

  private static final Logger logger = Logger.getLogger(ServiceLogsSolrDao.class);
  
  public ServiceLogsSolrDao() {
    super(LogType.SERVICE);
  }

  @PostConstruct
  public void postConstructor() {
    logger.info("postConstructor() called.");
    String solrUrl = PropertiesUtil.getProperty("logsearch.solr.url");
    String zkConnectString = PropertiesUtil.getProperty("logsearch.solr.zk_connect_string");
    String collection = PropertiesUtil.getProperty("logsearch.solr.collection.service.logs", "hadoop_logs");
    String splitInterval = PropertiesUtil.getProperty("logsearch.service.logs.split.interval.mins", "none");
    String configName = PropertiesUtil.getProperty("logsearch.solr.service.logs.config.name", "hadoop_logs");
    int numberOfShards = PropertiesUtil.getIntProperty("logsearch.collection.service.logs.numshards", 1);
    int replicationFactor = PropertiesUtil.getIntProperty("logsearch.collection.service.logs.replication.factor", 1);

    try {
      connectToSolr(solrUrl, zkConnectString, collection);
      setupCollections(splitInterval, configName, numberOfShards, replicationFactor, true);
    } catch (Exception e) {
      logger.error("error while connecting to Solr for service logs : solrUrl=" + solrUrl + ", zkConnectString=" +
          zkConnectString + ", collection=" + collection, e);
    }
  }
}
