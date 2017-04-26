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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ambari.logsearch.common.HadoopServiceConfigHelper;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.LogSearchContext;
import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.SolrUserPropsConfig;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.configurer.LogfeederFilterConfigurer;
import org.apache.ambari.logsearch.configurer.SolrCollectionConfigurer;
import org.apache.ambari.logsearch.model.common.LogFeederDataMap;
import org.apache.ambari.logsearch.model.common.LogfeederFilterData;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.data.solr.core.SolrTemplate;

import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.ID;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.USER_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.VALUES;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.FILTER_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.ROW_TYPE;

@Named
public class UserConfigSolrDao extends SolrDaoBase {

  private static final Logger LOG = Logger.getLogger(UserConfigSolrDao.class);

  private static final Logger LOG_PERFORMANCE = Logger.getLogger("org.apache.ambari.logsearch.performance");

  @Inject
  private SolrUserPropsConfig solrUserConfig;

  @Inject
  @Named("userConfigSolrTemplate")
  private SolrTemplate userConfigSolrTemplate;

  @Inject
  @Named("solrUserConfigState")
  private SolrCollectionState solrUserConfigState;

  public UserConfigSolrDao() {
    super(LogType.SERVICE);
  }

  @Override
  public SolrTemplate getSolrTemplate() {
    return userConfigSolrTemplate;
  }

  @Override
  public void setSolrTemplate(SolrTemplate solrTemplate) {
    this.userConfigSolrTemplate = solrTemplate;
  }

  @PostConstruct
  public void postConstructor() {
    String solrUrl = solrUserConfig.getSolrUrl();
    String zkConnectString = solrUserConfig.getZkConnectString();
    String collection = solrUserConfig.getCollection();

    try {
      new SolrCollectionConfigurer(this, false).start();
      new LogfeederFilterConfigurer(this).start();
    } catch (Exception e) {
      LOG.error("error while connecting to Solr for history logs : solrUrl=" + solrUrl + ", zkConnectString=" + zkConnectString +
          ", collection=" + collection, e);
    }
  }

  public void saveUserFilter(LogFeederDataMap logfeederFilterWrapper) throws SolrException, SolrServerException, IOException {
    String filterName = LogSearchConstants.LOGFEEDER_FILTER_NAME;
    String json = JSONUtil.objToJson(logfeederFilterWrapper);
    SolrInputDocument configDocument = new SolrInputDocument();
    configDocument.addField(ID, logfeederFilterWrapper.getId());
    configDocument.addField(ROW_TYPE, filterName);
    configDocument.addField(VALUES, json);
    configDocument.addField(USER_NAME, filterName);
    configDocument.addField(FILTER_NAME, filterName);
    addDocs(configDocument);
  }

  public void deleteUserConfig(String id) throws SolrException, SolrServerException, IOException {
    removeDoc("id:" + id);
  }

  public UpdateResponse addDocs(SolrInputDocument doc) throws SolrServerException, IOException, SolrException {
    UpdateResponse updateResoponse = getSolrClient().add(doc);
    LOG_PERFORMANCE.info("\n Username :- " + LogSearchContext.getCurrentUsername() +
      " Update Time Execution :- " + updateResoponse.getQTime() + " Total Time Elapsed is :- " + updateResoponse.getElapsedTime());
    getSolrClient().commit();
    return updateResoponse;
  }

  public UpdateResponse removeDoc(String query) throws SolrServerException, IOException, SolrException {
    UpdateResponse updateResoponse = getSolrClient().deleteByQuery(query);
    getSolrClient().commit();
    LOG_PERFORMANCE.info("\n Username :- " + LogSearchContext.getCurrentUsername() +
      " Remove Time Execution :- " + updateResoponse.getQTime() + " Total Time Elapsed is :- " + updateResoponse.getElapsedTime());
    return updateResoponse;
  }

  public LogFeederDataMap getUserFilter() throws SolrServerException, IOException {
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    solrQuery.setFilterQueries(ROW_TYPE + ":" + LogSearchConstants.LOGFEEDER_FILTER_NAME);

    QueryResponse response = process(solrQuery);
    SolrDocumentList documentList = response.getResults();
    LogFeederDataMap logfeederDataMap = null;
    if (CollectionUtils.isNotEmpty(documentList)) {
      SolrDocument configDoc = documentList.get(0);
      String json = (String) configDoc.get(VALUES);
      logfeederDataMap = (LogFeederDataMap) JSONUtil.jsonToObj(json, LogFeederDataMap.class);
      logfeederDataMap.setId("" + configDoc.get(ID));
    } else {
      logfeederDataMap = new LogFeederDataMap();
      logfeederDataMap.setFilter(new TreeMap<String, LogfeederFilterData>());
      logfeederDataMap.setId(Long.toString(System.currentTimeMillis()));
    }
    
    addMissingFilters(logfeederDataMap);
    
    return logfeederDataMap;
  }

  private void addMissingFilters(LogFeederDataMap logfeederDataMap) throws SolrServerException, IOException {
    Set<String> logIds = HadoopServiceConfigHelper.getAllLogIds();
    if (logIds != null) {
      List<String> logfeederDefaultLevels = solrUserConfig.getLogLevels();
      
      boolean modified = false;
      for (String logId : logIds) {
        if (!logfeederDataMap.getFilter().containsKey(logId)) {
          LogfeederFilterData logfeederFilterData = new LogfeederFilterData();
          logfeederFilterData.setLabel(logId);
          logfeederFilterData.setDefaultLevels(logfeederDefaultLevels);
          logfeederDataMap.getFilter().put(logId, logfeederFilterData);
          modified = true;
        }
      }
      
      if (modified) {
        saveUserFilter(logfeederDataMap);
      }
    }
  }

  @Override
  public SolrCollectionState getSolrCollectionState() {
    return solrUserConfigState;
  }

  @Override
  public SolrPropsConfig getSolrPropsConfig() {
    return solrUserConfig;
  }
}
