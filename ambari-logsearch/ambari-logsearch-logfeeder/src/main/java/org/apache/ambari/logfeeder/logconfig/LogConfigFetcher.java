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
package org.apache.ambari.logfeeder.logconfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;

public class LogConfigFetcher {
  private static final Logger LOG = Logger.getLogger(LogConfigFetcher.class);
  
  private static LogConfigFetcher instance;
  public synchronized static LogConfigFetcher getInstance() {
    if (instance == null) {
      try {
        instance = new LogConfigFetcher();
      } catch (Exception e) {
        String logMessageKey = LogConfigFetcher.class.getSimpleName() + "_SOLR_UTIL";
              LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Error constructing solrUtil", e, LOG, Level.WARN);
      }
    }
    return instance;
  }

  private SolrClient solrClient;

  private String solrDetail = "";

  public LogConfigFetcher() throws Exception {
    String url = LogFeederUtil.getStringProperty("logfeeder.solr.url");
    String zkConnectString = LogFeederUtil.getStringProperty("logfeeder.solr.zk_connect_string");
    String collection = LogFeederUtil.getStringProperty("logfeeder.solr.core.config.name", "history");
    connectToSolr(url, zkConnectString, collection);
  }

  private SolrClient connectToSolr(String url, String zkConnectString, String collection) throws Exception {
    solrDetail = "zkConnectString=" + zkConnectString + ", collection=" + collection + ", url=" + url;

    LOG.info("connectToSolr() " + solrDetail);
    if (StringUtils.isEmpty(collection)) {
      throw new Exception("For solr, collection name is mandatory. " + solrDetail);
    }
    
    if (StringUtils.isEmpty(zkConnectString) && StringUtils.isBlank(url))
      throw new Exception("Both zkConnectString and URL are empty. zkConnectString=" + zkConnectString + ", collection=" +
          collection + ", url=" + url);
    
    if (StringUtils.isNotEmpty(zkConnectString)) {
      solrDetail = "zkConnectString=" + zkConnectString + ", collection=" + collection;
      LOG.info("Using zookeepr. " + solrDetail);
      CloudSolrClient solrClouldClient = new CloudSolrClient(zkConnectString);
      solrClouldClient.setDefaultCollection(collection);
      solrClient = solrClouldClient;
      checkSolrStatus(3 * 60 * 1000);
    } else {
      solrDetail = "collection=" + collection + ", url=" + url;
      String collectionURL = url + "/" + collection;
      LOG.info("Connecting to  solr : " + collectionURL);
      solrClient = new HttpSolrClient(collectionURL);
    }
    return solrClient;
  }

  private boolean checkSolrStatus(int waitDurationMS) {
    boolean status = false;
    try {
      long beginTimeMS = System.currentTimeMillis();
      long waitIntervalMS = 2000;
      int pingCount = 0;
      while (true) {
        pingCount++;
        CollectionAdminResponse response = null;
        try {
          CollectionAdminRequest.List colListReq = new CollectionAdminRequest.List();
          response = colListReq.process(solrClient);
        } catch (Exception ex) {
          LOG.error("Con't connect to Solr. solrDetail=" + solrDetail, ex);
        }
        if (response != null && response.getStatus() == 0) {
          LOG.info("Solr getCollections() is success. solr=" + solrDetail);
          status = true;
          break;
        }
        if (System.currentTimeMillis() - beginTimeMS > waitDurationMS) {
          LOG.error("Solr is not reachable even after " + (System.currentTimeMillis() - beginTimeMS)
            + " ms. If you are using alias, then you might have to restart LogSearch after Solr is up and running. solr="
            + solrDetail + ", response=" + response);
          break;
        } else {
          LOG.warn("Solr is not reachable yet. getCollections() attempt count=" + pingCount + ". Will sleep for " +
              waitIntervalMS + " ms and try again." + " solr=" + solrDetail + ", response=" + response);
        }
        Thread.sleep(waitIntervalMS);
      }
    } catch (Throwable t) {
      LOG.error("Seems Solr is not up. solrDetail=" + solrDetail, t);
    }
    return status;
  }

  public Map<String, Object> getConfigDoc() {
    HashMap<String, Object> configMap = new HashMap<String, Object>();
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    String fq = LogFeederConstants.ROW_TYPE + ":" + LogFeederConstants.LOGFEEDER_FILTER_NAME;
    solrQuery.setFilterQueries(fq);
    try {
      QueryResponse response = process(solrQuery);
      if (response != null) {
        SolrDocumentList documentList = response.getResults();
        if (CollectionUtils.isNotEmpty(documentList)) {
          SolrDocument configDoc = documentList.get(0);
          String configJson = LogFeederUtil.getGson().toJson(configDoc);
          configMap = (HashMap<String, Object>) LogFeederUtil.toJSONObject(configJson);
        }
      }
    } catch (Exception e) {
      String logMessageKey = this.getClass().getSimpleName() + "_FETCH_FILTER_CONFIG_ERROR";
      LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Error getting filter config from solr", e, LOG, Level.ERROR);
    }
    return configMap;
  }

  private QueryResponse process(SolrQuery solrQuery) throws SolrServerException, IOException, SolrException {
    if (solrClient != null) {
      QueryResponse queryResponse = solrClient.query(solrQuery, METHOD.POST);
      return queryResponse;
    } else {
      LOG.error("solrClient can't be null");
      return null;
    }
  }
}
