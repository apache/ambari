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

import org.apache.ambari.logsearch.manager.MgrBase.LOG_TYPE;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class UserConfigSolrDao extends SolrDaoBase {

  static private Logger logger = Logger.getLogger(UserConfigSolrDao.class);

  public UserConfigSolrDao() {
    super(LOG_TYPE.SERVICE);
  }

  @PostConstruct
  public void postConstructor() {

    String solrUrl = PropertiesUtil.getProperty("solr.url");
    String zkHosts = PropertiesUtil.getProperty("solr.zkhosts");
    String collection = PropertiesUtil.getProperty("solr.core.history",
      "history");
    String configName = PropertiesUtil.getProperty(
      "solr.history.config_name", "history");
    int replicationFactor = PropertiesUtil.getIntProperty(
      "solr.history.replication_factor", 2);
    String splitInterval = "none";
    int numberOfShards = 1;

    try {
      connectToSolr(solrUrl, zkHosts, collection);
      setupCollections(splitInterval, configName, numberOfShards,
        replicationFactor);

    } catch (Exception e) {
      logger.error(
        "error while connecting to Solr for history logs : solrUrl="
          + solrUrl + ", zkHosts=" + zkHosts
          + ", collection=" + collection, e);
    }
  }

}
