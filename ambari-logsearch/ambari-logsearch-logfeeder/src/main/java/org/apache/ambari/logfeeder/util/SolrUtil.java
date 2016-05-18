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
package org.apache.ambari.logfeeder.util;

import java.io.IOException;
import java.util.HashMap;

import org.apache.ambari.logfeeder.LogFeederUtil;
import org.apache.ambari.logfeeder.logconfig.LogFeederConstants;
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
import org.apache.solr.common.SolrInputDocument;

public class SolrUtil {

  private static Logger logger = Logger.getLogger(SolrUtil.class);

  private static SolrUtil instance = null;
  SolrClient solrClient = null;
  CloudSolrClient solrClouldClient = null;

  boolean isSolrCloud = true;
  String solrDetail = "";
  String collectionName = null;

  private SolrUtil() throws Exception {
    String url = LogFeederUtil.getStringProperty("logfeeder.solr.url");
    String zkHosts = LogFeederUtil.getStringProperty("logfeeder.solr.zkhosts");
    String collection = LogFeederUtil.getStringProperty("logfeeder.solr.core.history", "history");
    connectToSolr(url, zkHosts, collection);
  }

  public static SolrUtil getInstance() {
    if (instance == null) {
      synchronized (SolrUtil.class) {
        if (instance == null) {
          try {
            instance = new SolrUtil();
          } catch (Exception e) {
            final String LOG_MESSAGE_KEY = SolrUtil.class
                .getSimpleName() + "_SOLR_UTIL";
              LogFeederUtil.logErrorMessageByInterval(
                LOG_MESSAGE_KEY,
                "Error constructing solrUtil", e, logger,
                Level.WARN);
          }
        }
      }
    }
    return instance;
  }

  public SolrClient connectToSolr(String url, String zkHosts,
                                  String collection) throws Exception {
    this.collectionName = collection;
    solrDetail = "zkHosts=" + zkHosts + ", collection=" + collection
      + ", url=" + url;

    logger.info("connectToSolr() " + solrDetail);
    if (collection == null || collection.isEmpty()) {
      throw new Exception("For solr, collection name is mandatory. "
        + solrDetail);
    }
    if (zkHosts != null && !zkHosts.isEmpty()) {
      solrDetail = "zkHosts=" + zkHosts + ", collection=" + collection;
      logger.info("Using zookeepr. " + solrDetail);
      solrClouldClient = new CloudSolrClient(zkHosts);
      solrClouldClient.setDefaultCollection(collection);
      solrClient = solrClouldClient;
      int waitDurationMS = 3 * 60 * 1000;
      checkSolrStatus(waitDurationMS);
    } else {
      if (url == null || url.trim().isEmpty()) {
        throw new Exception("Both zkHosts and URL are empty. zkHosts="
          + zkHosts + ", collection=" + collection + ", url="
          + url);
      }
      solrDetail = "collection=" + collection + ", url=" + url;
      String collectionURL = url + "/" + collection;
      logger.info("Connecting to  solr : " + collectionURL);
      solrClient = new HttpSolrClient(collectionURL);

    }
    return solrClient;
  }

  /**
   * @param waitDurationMS
   * @return
   */
  public boolean checkSolrStatus(int waitDurationMS) {
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
          logger.error("Con't connect to Solr. solrDetail=" + solrDetail, ex);
        }
        if (response != null && response.getStatus() == 0) {
          logger.info("Solr getCollections() is success. solr=" + solrDetail);
          status = true;
          break;
        }
        if (System.currentTimeMillis() - beginTimeMS > waitDurationMS) {
          logger.error("Solr is not reachable even after "
            + (System.currentTimeMillis() - beginTimeMS)
            + " ms. If you are using alias, then you might have to restart LogSearch after Solr is up and running. solr="
            + solrDetail + ", response=" + response);
          break;
        } else {
          logger.warn("Solr is not not reachable yet. getCollections() attempt count=" + pingCount
            + ". Will sleep for " + waitIntervalMS + " ms and try again." + " solr=" + solrDetail
            + ", response=" + response);

        }
        Thread.sleep(waitIntervalMS);
      }
    } catch (Throwable t) {
      logger.error("Seems Solr is not up. solrDetail=" + solrDetail);
    }
    return status;
  }

  /**
   * @param solrQuery
   * @return
   * @throws SolrServerException
   * @throws IOException
   * @throws SolrException
   */
  public QueryResponse process(SolrQuery solrQuery) throws SolrServerException, IOException, SolrException {
    if (solrClient != null) {
      QueryResponse queryResponse = solrClient.query(solrQuery, METHOD.POST);
      return queryResponse;
    } else {
      logger.error("solrClient can't be null");
      return null;
    }
  }

  /**
   * @return
   */
  public HashMap<String, Object> getConfigDoc() {
    HashMap<String, Object> configMap = new HashMap<String, Object>();
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    String fq = LogFeederConstants.ROW_TYPE + ":" + LogFeederConstants.NAME;
    solrQuery.setFilterQueries(fq);
    try {
      QueryResponse response = process(solrQuery);
      if (response != null) {
        SolrDocumentList documentList = response.getResults();
        if (documentList != null && documentList.size() > 0) {
          SolrDocument configDoc = documentList.get(0);
          String configJson = LogFeederUtil.getGson().toJson(configDoc);
          configMap = (HashMap<String, Object>) LogFeederUtil
              .toJSONObject(configJson);
        }
      }
    } catch (Exception e) {
      logger.error("Error getting config", e);
    }
    return configMap;
  }

  /**
   * @param solrInputDocument
   * @throws SolrServerException
   * @throws IOException
   */
  public void addDoc(SolrInputDocument solrInputDocument) throws SolrServerException, IOException {
    solrClient.add(solrInputDocument);
    solrClient.commit();
  }

}
