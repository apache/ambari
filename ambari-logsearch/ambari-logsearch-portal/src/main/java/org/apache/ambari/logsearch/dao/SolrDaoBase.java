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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.ambari.logsearch.common.LogsearchContextUtil;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.manager.MgrBase.LOG_TYPE;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.StringUtil;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.util.NamedList;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SolrDaoBase {
  static private Logger logger = Logger.getLogger(SolrDaoBase.class);

  private static Logger logPerformance = Logger
    .getLogger("org.apache.ambari.logsearch.performance");

  private static final String ROUTER_FIELD = "_router_field_";
 
  protected LOG_TYPE logType;

  @Autowired
  StringUtil stringUtil;

  @Autowired
  JSONUtil jsonUtil;
  
  @Autowired
  RESTErrorUtil restErrorUtil;

  String collectionName = null;
  // List<String> collectionList = new ArrayList<String>();

  private SolrClient solrClient = null;
  CloudSolrClient solrClouldClient = null;

  boolean isSolrCloud = true;
  String solrDetail = "";

  boolean isSolrInitialized = false;

  private boolean setup_status = false;
  private boolean populateFieldsThreadActive = false;

  int SETUP_RETRY_SECOND = 30;
  
  private boolean isZkhost=false;//by default its false
  
  //set logtype
  public SolrDaoBase(LOG_TYPE logType) {
    this.logType = logType;
  }

  public SolrClient connectToSolr(String url, String zkHosts,
                                  String collection) throws Exception {
    this.collectionName = collection;
    solrDetail = "zkHosts=" + zkHosts + ", collection=" + collection
      + ", url=" + url;

    logger.info("connectToSolr() " + solrDetail);
    if (stringUtil.isEmpty(collection)) {
      throw new Exception("For solr, collection name is mandatory. "
        + solrDetail);
    }
    if (!stringUtil.isEmpty(zkHosts)) {
      isZkhost=true;
      solrDetail = "zkHosts=" + zkHosts + ", collection=" + collection;
      logger.info("Using zookeepr. " + solrDetail);
      solrClouldClient = new CloudSolrClient(zkHosts);
      solrClouldClient.setDefaultCollection(collection);
      solrClient = solrClouldClient;
      int waitDurationMS = 3 * 60 * 1000;
      checkSolrStatus(waitDurationMS);
    } else {
      if (stringUtil.isEmpty(url)) {
        throw new Exception("Both zkHosts and URL are empty. zkHosts="
          + zkHosts + ", collection=" + collection + ", url="
          + url);
      }
      solrDetail = "collection=" + collection + ", url=" + url;
      String collectionURL = url + "/" + collection;
      logger.info("Connecting to  solr : " + collectionURL);
      solrClient = new HttpSolrClient(collectionURL);

    }
    // populateSchemaFields(collection);
    return solrClient;
  }

  public SolrClient getSolrClient() {
    return solrClient;
  }

  /**
   * This will try to get the collections from the Solr. Ping doesn't work if
   * collection is not given
   *
   * @param waitDurationMS
   */
  public boolean checkSolrStatus(int waitDurationMS) {
    boolean status = false;
    try {
      long beginTimeMS = System.currentTimeMillis();
      long waitIntervalMS = 2000;
      int pingCount = 0;
      while (true) {
        pingCount++;
        try {
          List<String> collectionList = getCollections();
          if (collectionList != null) {
            logger.info("checkSolrStatus(): Solr getCollections() is success. solr="
              + solrDetail
              + ", collectionList="
              + collectionList);
            status = true;
            break;
          }
        } catch (Exception ex) {
          logger.error("Error while doing Solr check", ex);
        }
        if (System.currentTimeMillis() - beginTimeMS > waitDurationMS) {
          logger.error("Solr is not reachable even after "
            + (System.currentTimeMillis() - beginTimeMS)
            + " ms. If you are using alias, then you might have to restart LogSearch after Solr is up and running. solr="
            + solrDetail);
          break;
        } else {
          logger.warn("Solr is not not reachable yet. getCollections() attempt count="
            + pingCount
            + ". Will sleep for "
            + waitIntervalMS
            + " ms and try again." + " solr=" + solrDetail);
        }
        Thread.sleep(waitIntervalMS);

      }
    } catch (Throwable t) {
      logger.error("Seems Solr is not up. solrDetail=" + solrDetail);
    }
    return status;
  }

  public void setupCollections(final String splitMode, final String configName,
      final int numberOfShards, final int replicationFactor) throws Exception {
    if (isZkhost) {
      setup_status = createCollectionsIfNeeded(splitMode, configName,
          numberOfShards, replicationFactor);
      logger.info("Setup status for " + collectionName + " is " + setup_status);
      if (!setup_status) {
        // Start a background thread to do setup
        Thread setupThread = new Thread("setup_collection_" + collectionName) {
          @Override
          public void run() {
            logger
                .info("Started monitoring thread to check availability of Solr server. collection="
                    + collectionName);
            int retryCount = 0;
            while (true) {
              try {
                Thread.sleep(SETUP_RETRY_SECOND);
                retryCount++;
                setup_status = createCollectionsIfNeeded(splitMode, configName,
                    numberOfShards, replicationFactor);
                if (setup_status) {
                  logger.info("Setup for collection " + collectionName
                      + " is successful. Exiting setup retry thread");
                  break;
                }
              } catch (InterruptedException sleepInterrupted) {
                logger.info("Sleep interrupted while setting up collection "
                    + collectionName);
                break;
              } catch (Exception e) {
                logger
                    .error("Error setting up collection=" + collectionName, e);
              }
              logger.error("Error setting collection. collection="
                  + collectionName + ", retryCount=" + retryCount);
            }
          }
        };
        setupThread.setDaemon(true);
        setupThread.start();
      }
    }
    populateSchemaFields();
  }

  public boolean createCollectionsIfNeeded(final String splitMode,
                                           final String configName, final int numberOfShards,
                                           final int replicationFactor) {
    boolean result = false;
    try {
      List<String> allCollectionList = getCollections();
      if (splitMode.equalsIgnoreCase("none")) {
        // Just create regular collection
        result = createCollection(collectionName, configName,
          numberOfShards, replicationFactor, allCollectionList);
      } else {
        result = setupCollectionsWithImplicitRouting(splitMode,
          configName, numberOfShards, allCollectionList);
      }
    } catch (Exception ex) {
      logger.error("Error creating collection. collectionName="
        + collectionName, ex);
    }
    return result;
  }

  public List<String> getCollections() throws SolrServerException,
    IOException {
    try {
      CollectionAdminRequest.List colListReq = new CollectionAdminRequest.List();
      CollectionAdminResponse response = colListReq.process(solrClient);
      if (response.getStatus() != 0) {
        logger.error("Error getting collection list from solr.  response="
          + response);
        return null;
      }

      @SuppressWarnings("unchecked")
      List<String> allCollectionList = (List<String>) response
        .getResponse().get("collections");
      return allCollectionList;
    } catch (SolrException e) {
      logger.error(e);
      return new ArrayList<String>();
    }
  }

  public boolean setupCollectionsWithImplicitRouting(String splitMode,
                                                     String configName, int numberOfShards,
                                                     List<String> allCollectionList) throws Exception {
    logger.info("setupCollectionsWithImplicitRouting(). collectionName="
      + collectionName + ", numberOfShards=" + numberOfShards);
    return createCollectionWithImplicitRoute(collectionName, configName,
      numberOfShards, allCollectionList);
  }

  public boolean createCollectionWithImplicitRoute(String colName,
                                                   String configName, int numberOfShards,
                                                   List<String> allCollectionList) throws SolrServerException,
    IOException {

    // Default is true, because if the collection and shard is already
    // there, then it will return true
    boolean returnValue = true;
    String shardsListStr = "";
    List<String> shardsList = new ArrayList<String>();
    for (int i = 0; i < numberOfShards; i++) {
      if (i != 0) {
        shardsListStr += ",";
      }
      String shard = "shard" + i;
      shardsListStr += shard;
      shardsList.add(shard);
    }

    // Check if collection is already in zookeeper
    if (!allCollectionList.contains(colName)) {
      logger.info("Creating collection " + colName + ", shardsList="
        + shardsList + ", solrDetail=" + solrDetail);
      int replicationFactor = 1;
      CollectionAdminRequest.Create collectionCreateRequest = new CollectionAdminRequest.Create();
      collectionCreateRequest.setCollectionName(colName);
      collectionCreateRequest.setRouterName("implicit");
      collectionCreateRequest.setShards(shardsListStr);
      collectionCreateRequest.setMaxShardsPerNode(numberOfShards);
      collectionCreateRequest.setReplicationFactor(replicationFactor);
      collectionCreateRequest.setConfigName(configName);
      collectionCreateRequest.setRouterField(ROUTER_FIELD);
      collectionCreateRequest.setMaxShardsPerNode(replicationFactor
        * numberOfShards);

      CollectionAdminResponse createResponse = collectionCreateRequest
        .process(solrClient);
      if (createResponse.getStatus() != 0) {
        returnValue = false;
        logger.error("Error creating collection. collectionName="
          + colName + ", shardsList=" + shardsList
          + ", solrDetail=" + solrDetail + ", response="
          + createResponse);
      } else {
        logger.info("Created collection " + colName + ", shardsList="
          + shardsList + ", solrDetail=" + solrDetail);
      }
    } else {
      logger.info("Collection "
        + colName
        + " is already there. Will check whether it has the required shards");
      Collection<String> existingShards = getShards();
      for (String shard : shardsList) {
        if (!existingShards.contains(shard)) {
          try {
            logger.info("Going to add Shard " + shard
              + " to collection " + collectionName);
            CollectionAdminRequest.CreateShard createShardRequest = new CollectionAdminRequest.CreateShard();
            createShardRequest.setCollectionName(collectionName);
            createShardRequest.setShardName(shard);
            CollectionAdminResponse response = createShardRequest
              .process(solrClient);
            if (response.getStatus() != 0) {
              logger.error("Error creating shard " + shard
                + " in collection " + collectionName
                + ", response=" + response
                + ", solrDetail=" + solrDetail);
              returnValue = false;
              break;
            } else {
              logger.info("Successfully created shard " + shard
                + " in collection " + collectionName);
            }
          } catch (Throwable t) {
            logger.error("Error creating shard " + shard
              + " in collection " + collectionName
              + ", solrDetail=" + solrDetail, t);
            returnValue = false;
            break;
          }
        }
      }
    }
    return returnValue;
  }

  public Collection<String> getShards() {
    Collection<String> list = new HashSet<String>();

    if (solrClouldClient == null) {
      logger.error("getShards(). Only supporting in SolrCloud mode");
      return list;
    }

    ZkStateReader reader = solrClouldClient.getZkStateReader();
    Collection<Slice> slices = reader.getClusterState().getSlices(
      collectionName);
    Iterator<Slice> iter = slices.iterator();

    while (iter.hasNext()) {
      Slice slice = iter.next();
      for (Replica replica : slice.getReplicas()) {
        logger.info("colName=" + collectionName + ", slice.name="
          + slice.getName() + ", slice.state=" + slice.getState()
          + ", replica.core=" + replica.getStr("core")
          + ", replica.state=" + replica.getStr("state"));
        list.add(slice.getName());
      }
    }
    return list;
  }

  public boolean createCollection(String colName, String configName,
                                  int numberOfShards, int replicationFactor,
                                  List<String> allCollectionList) throws SolrServerException,
    IOException {
    // Check if collection is already in zookeeper
    if (allCollectionList.contains(colName)) {
      logger.info("Collection " + colName
        + " is already there. Won't create it");
      return true;
    }

    logger.info("Creating collection " + colName + ", numberOfShards="
      + numberOfShards + ", replicationFactor=" + replicationFactor
      + ", solrDetail=" + solrDetail);

    CollectionAdminRequest.Create collectionCreateRequest = new CollectionAdminRequest.Create();
    collectionCreateRequest.setCollectionName(colName);
    collectionCreateRequest.setNumShards(numberOfShards);
    collectionCreateRequest.setReplicationFactor(replicationFactor);
    collectionCreateRequest.setConfigName(configName);
    collectionCreateRequest.setMaxShardsPerNode(replicationFactor
      * numberOfShards);
    CollectionAdminResponse createResponse = collectionCreateRequest
      .process(solrClient);
    if (createResponse.getStatus() != 0) {
      logger.error("Error creating collection. collectionName=" + colName
        + ", solrDetail=" + solrDetail + ", response="
        + createResponse);
      return false;
    } else {
      logger.info("Created collection " + colName + ", numberOfShards="
        + numberOfShards + ", replicationFactor="
        + replicationFactor + ", solrDetail=" + solrDetail);
      return true;
    }
  }

  public QueryResponse process(SolrQuery solrQuery)
    throws SolrServerException, IOException {
    if (solrClient != null) {
      String event = solrQuery.get("event");
      solrQuery.remove("event");
      QueryResponse queryResponse = solrClient.query(solrQuery,
        METHOD.POST);

      if (event != null && !"/getLiveLogsCount".equalsIgnoreCase(event)) {
        logPerformance.info("\n Username :- "
          + LogsearchContextUtil.getCurrentUsername()
          + " Event :- " + event + " SolrQuery :- " + solrQuery
          + "\nQuery Time Execution :- "
          + queryResponse.getQTime()
          + " Total Time Elapsed is :- "
          + queryResponse.getElapsedTime());
      }
      return queryResponse;
    } else {
      throw restErrorUtil.createRESTException(
          "Solr configuration improper for " + logType.getLabel() +" logs",
          MessageEnums.ERROR_SYSTEM);
    }
  }

  public UpdateResponse addDocs(SolrInputDocument doc)
    throws SolrServerException, IOException, SolrException {
    UpdateResponse updateResoponse = solrClient.add(doc);
    logPerformance.info("\n Username :- "
      + LogsearchContextUtil.getCurrentUsername()
      + " Update Time Execution :- " + updateResoponse.getQTime()
      + " Total Time Elapsed is :- "
      + updateResoponse.getElapsedTime());
    solrClient.commit();
    return updateResoponse;
  }

  public UpdateResponse removeDoc(String query) throws SolrServerException,
    IOException, SolrException {
    UpdateResponse updateResoponse = solrClient.deleteByQuery(query);
    solrClient.commit();
    logPerformance.info("\n Username :- "
      + LogsearchContextUtil.getCurrentUsername()
      + " Remove Time Execution :- " + updateResoponse.getQTime()
      + " Total Time Elapsed is :- "
      + updateResoponse.getElapsedTime());
    return updateResoponse;
  }

  private void populateSchemaFields() {
    boolean result = _populateSchemaFields();
    if (!result && !populateFieldsThreadActive) {
      populateFieldsThreadActive = true;
      logger.info("Creating thread to populated fields for collection="
        + collectionName);
      Thread fieldPopulationThread = new Thread("populated_fields_"
        + collectionName) {
        @Override
        public void run() {
          logger.info("Started thread to get fields for collection="
            + collectionName);
          int retryCount = 0;
          while (true) {
            try {
              Thread.sleep(SETUP_RETRY_SECOND * 1000);
              retryCount++;
              boolean _result = _populateSchemaFields();
              if (_result) {
                logger.info("Populate fields for collection "
                  + collectionName + " is success");
                break;
              }
            } catch (InterruptedException sleepInterrupted) {
              logger.info("Sleep interrupted while populating fields for collection "
                + collectionName);
              break;
            } catch (Exception ex) {
              logger.error("Error while populating fields for collection "
                + collectionName
                + ", retryCount="
                + retryCount);
            } finally {
              populateFieldsThreadActive = false;
            }
          }
          logger.info("Exiting thread for populating fields. collection="
            + collectionName);
        }

      };
      fieldPopulationThread.setDaemon(true);
      fieldPopulationThread.start();
    }
  }

  /**
   * Called from the thread. Don't call this directly
   */
  private boolean _populateSchemaFields() {
    SolrRequest<SchemaResponse> request = new SchemaRequest();
    request.setMethod(METHOD.GET);
    request.setPath("/schema/fields");
    if (solrClient != null) {
      NamedList<Object> namedList = null;
      try {
        namedList = solrClient.request(request);
        logger.info("populateSchemaFields() collection="
          + collectionName + ", fields=" + namedList);
      } catch (SolrException | SolrServerException | IOException e) {
        logger.error(
          "Error occured while popuplating field. collection="
            + collectionName, e);
      }
      if (namedList != null) {
        ConfigUtil.extractSchemaFieldsName(namedList.toString(),
          collectionName);
        return true;
      }
    }
    return false;
  }
}
