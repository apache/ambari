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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ambari.logsearch.common.LogSearchContext;
import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.SolrEventHistoryPropsConfig;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.configurer.SolrCollectionConfigurer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

import org.apache.log4j.Logger;
import org.springframework.data.solr.core.SolrTemplate;

@Named
public class EventHistorySolrDao extends SolrDaoBase {

  private static final Logger LOG = Logger.getLogger(EventHistorySolrDao.class);

  private static final Logger LOG_PERFORMANCE = Logger.getLogger("org.apache.ambari.logsearch.performance");

  @Inject
  private SolrEventHistoryPropsConfig solrEventHistoryPropsConfig;

  @Inject
  @Named("eventHistorySolrTemplate")
  private SolrTemplate eventHistorySolrTemplate;

  @Inject
  @Named("solrEventHistoryState")
  private SolrCollectionState solrEventHistoryState;

  public EventHistorySolrDao() {
    super(LogType.SERVICE);
  }

  @Override
  public SolrTemplate getSolrTemplate() {
    return eventHistorySolrTemplate;
  }

  @Override
  public void setSolrTemplate(SolrTemplate solrTemplate) {
    this.eventHistorySolrTemplate = solrTemplate;
  }

  @PostConstruct
  public void postConstructor() {
    String solrUrl = solrEventHistoryPropsConfig.getSolrUrl();
    String zkConnectString = solrEventHistoryPropsConfig.getZkConnectString();
    String collection = solrEventHistoryPropsConfig.getCollection();

    try {
      new SolrCollectionConfigurer(this, false).start();
    } catch (Exception e) {
      LOG.error("error while connecting to Solr for history logs : solrUrl=" + solrUrl + ", zkConnectString=" + zkConnectString +
          ", collection=" + collection, e);
    }
  }

  public void deleteEventHistoryData(String id) throws SolrException, SolrServerException, IOException {
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

  @Override
  public SolrCollectionState getSolrCollectionState() {
    return solrEventHistoryState;
  }

  @Override
  public SolrPropsConfig getSolrPropsConfig() {
    return solrEventHistoryPropsConfig;
  }
}
