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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.ambari.logsearch.common.LogSearchContext;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.manager.MgrBase.LogType;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
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

import com.google.common.annotations.VisibleForTesting;

public abstract class SolrDaoBase {
  private static final Logger logger = Logger.getLogger(SolrDaoBase.class);
  private static final Logger logPerformance = Logger.getLogger("org.apache.ambari.logsearch.performance");
  
  public HashMap<String, String> schemaFieldsNameMap = new HashMap<String, String>();
  public HashMap<String, String> schemaFieldTypeMap = new HashMap<String, String>();

  private static final String ROUTER_FIELD = "_router_field_";
  
  private static final int SETUP_RETRY_SECOND = 30;
  private static final int SETUP_UPDATE_SECOND = 10*60; //10 min
  private static final int ALIAS_SETUP_RETRY_SECOND = 30*60;

  private LogType logType;

  @Autowired
  protected JSONUtil jsonUtil;
  @Autowired
  protected RESTErrorUtil restErrorUtil;

  @VisibleForTesting
  protected String collectionName = null;
  @VisibleForTesting
  protected SolrClient solrClient = null;
  @VisibleForTesting
  protected CloudSolrClient solrClouldClient = null;
  @VisibleForTesting
  protected boolean isZkConnectString = false;
  
  private String solrDetail = "";

  private boolean populateFieldsThreadActive = false;
  
  protected SolrDaoBase(LogType logType) {
    this.logType = logType;
  }

  protected SolrClient connectToSolr(String url, String zkConnectString, String collection) throws Exception {
    this.collectionName = collection;
    solrDetail = "zkConnectString=" + zkConnectString + ", collection=" + collection + ", url=" + url;

    logger.info("connectToSolr() " + solrDetail);
    if (StringUtils.isBlank(collection)) {
      throw new Exception("For solr, collection name is mandatory. " + solrDetail);
    }
    
    setupSecurity();
    
    if (solrClient != null) {
      return solrClient;
    }
      
    if (!StringUtils.isBlank(zkConnectString)) {
      isZkConnectString = true;
      solrDetail = "zkConnectString=" + zkConnectString + ", collection=" + collection;
      logger.info("Using zookeeper. " + solrDetail);
      solrClouldClient = new CloudSolrClient(zkConnectString);
      solrClouldClient.setDefaultCollection(collection);
      solrClient = solrClouldClient;
      int waitDurationMS = 3 * 60 * 1000;
      checkSolrStatus(waitDurationMS);
    } else {
      if (StringUtils.isBlank(url)) {
        throw new Exception("Both zkConnectString and URL are empty. zkConnectString=" + zkConnectString + ", " +
            "collection=" + collection + ", url=" + url);
      }
      solrDetail = "collection=" + collection + ", url=" + url;
      String collectionURL = url + "/" + collection;
      logger.info("Connecting to  solr : " + collectionURL);
      solrClient = new HttpSolrClient(collectionURL);
    }
    return solrClient;
  }
  
  /**
   * This will try to get the collections from the Solr. Ping doesn't work if
   * collection is not given
   */
  protected boolean checkSolrStatus(int waitDurationMS) {
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
            logger.info("checkSolrStatus(): Solr getCollections() is success. solr=" + solrDetail + ", collectionList=" + collectionList);
            status = true;
            break;
          }
        } catch (Exception ex) {
          logger.error("Error while doing Solr check", ex);
        }
        if (System.currentTimeMillis() - beginTimeMS > waitDurationMS) {
          logger.error("Solr is not reachable even after " + (System.currentTimeMillis() - beginTimeMS) + " ms. " +
              "If you are using alias, then you might have to restart LogSearch after Solr is up and running. solr=" + solrDetail);
          break;
        } else {
          logger.warn("Solr is not not reachable yet. getCollections() attempt count=" + pingCount + ". " +
              "Will sleep for " + waitIntervalMS + " ms and try again." + " solr=" + solrDetail);
        }
        Thread.sleep(waitIntervalMS);

      }
    } catch (Throwable t) {
      logger.error("Seems Solr is not up. solrDetail=" + solrDetail);
    }
    return status;
  }

  protected void setupAlias(final String aliasNameIn, final Collection<String> collectionListIn ) throws Exception {
    if (aliasNameIn == null || collectionListIn == null || collectionListIn.size() == 0 || solrClouldClient == null) {
      logger.info("Will not create alias " + aliasNameIn + " for " +
          (collectionListIn == null ? null: collectionListIn.toString()) + ", solrCloudClient=" + solrClouldClient);
      return;
    }
    
    logger.info("setupAlias " + aliasNameIn + " for " + (collectionListIn == null ? null: collectionListIn.toString()));
    // Start a background thread to do setup
    Thread setupThread = new Thread("setup_alias_" + aliasNameIn) {
      @Override
      public void run() {
        logger.info("Started monitoring thread to check availability of Solr server. alias=" + aliasNameIn +
            ", collections=" + collectionListIn.toString());
        int retryCount = 0;
        while (true) {
          try {
            int count = createAlias(aliasNameIn, collectionListIn);
            if (count > 0) {
              solrClouldClient.setDefaultCollection(aliasNameIn);
              if (count == collectionListIn.size()) {
                logger.info("Setup for alias " + aliasNameIn + " is successful. Exiting setup retry thread. " +
                    "Collections=" + collectionListIn);
                populateSchemaFields();
                break;
              }
            } else {
              logger.warn("Not able to create alias=" + aliasNameIn + ", retryCount=" + retryCount);
            }
          } catch (Exception e) {
            logger.error("Error setting up alias=" + aliasNameIn, e);
          }
          try {
            Thread.sleep(ALIAS_SETUP_RETRY_SECOND * 1000);
          } catch (InterruptedException sleepInterrupted) {
            logger.info("Sleep interrupted while setting up alias " + aliasNameIn);
            break;
          }
          retryCount++;
        }
      }
    };
    setupThread.setDaemon(true);
    setupThread.start();
  }
  
  private int createAlias(String aliasNameIn, Collection<String> collectionListIn) throws SolrServerException, IOException {
    List<String> collectionToAdd = getCollections();
    collectionToAdd.retainAll(collectionListIn);
    
    String collectionsCSV = null;
    if (!collectionToAdd.isEmpty()) {
      collectionsCSV = StringUtils.join(collectionToAdd, ',');
      CollectionAdminRequest.CreateAlias aliasCreateRequest = new CollectionAdminRequest.CreateAlias(); 
      aliasCreateRequest.setAliasName(aliasNameIn);
      aliasCreateRequest.setAliasedCollections(collectionsCSV);
      CollectionAdminResponse createResponse = aliasCreateRequest.process(solrClouldClient);
      if (createResponse.getStatus() != 0) {
        logger.error("Error creating alias. alias=" + aliasNameIn + ", collectionList=" + collectionsCSV +
            ", solrDetail=" + solrDetail + ", response=" + createResponse);
        return 0;
      }
    } 
    if ( collectionToAdd.size() == collectionListIn.size()) {
      logger.info("Created alias for all collections. alias=" + aliasNameIn + ", collectionsCSV=" + collectionsCSV +
          ", solrDetail=" + solrDetail);
    } else {
      logger.info("Created alias for " + collectionToAdd.size() + " out of " + collectionListIn.size() + " collections. " +
          "alias=" + aliasNameIn + ", collectionsCSV=" + collectionsCSV + ", solrDetail=" + solrDetail);
    }
    return collectionToAdd.size();
  }

  protected void setupCollections(final String splitInterval, final String configName, final int numberOfShards,
      final int replicationFactor, boolean needToPopulateSchemaField) throws Exception {
    if (isZkConnectString) {
      boolean setupStatus = createCollectionsIfNeeded(splitInterval, configName, numberOfShards, replicationFactor);
      logger.info("Setup status for " + collectionName + " is " + setupStatus);
      if (!setupStatus) {
        // Start a background thread to do setup
        Thread setupThread = new Thread("setup_collection_" + collectionName) {
          @Override
          public void run() {
            logger.info("Started monitoring thread to check availability of Solr server. collection=" + collectionName);
            int retryCount = 0;
            while (true) {
              try {
                Thread.sleep(SETUP_RETRY_SECOND * 1000);
                retryCount++;
                boolean setupStatus = createCollectionsIfNeeded(splitInterval, configName, numberOfShards, replicationFactor);
                if (setupStatus) {
                  logger.info("Setup for collection " + collectionName + " is successful. Exiting setup retry thread");
                  break;
                }
              } catch (InterruptedException sleepInterrupted) {
                logger.info("Sleep interrupted while setting up collection " + collectionName);
                break;
              } catch (Exception e) {
                logger.error("Error setting up collection=" + collectionName, e);
              }
              logger.error("Error setting collection. collection=" + collectionName + ", retryCount=" + retryCount);
            }
          }
        };
        setupThread.setDaemon(true);
        setupThread.start();
      }
    }
    
    if (needToPopulateSchemaField){
      populateSchemaFields();
    }
  }

  private boolean createCollectionsIfNeeded(String splitInterval, String configName, int numberOfShards, int replicationFactor) {
    boolean result = false;
    try {
      List<String> allCollectionList = getCollections();
      if (splitInterval.equalsIgnoreCase("none")) {
        result = createCollection(configName, numberOfShards, replicationFactor, allCollectionList);
      } else {
        result = setupCollectionsWithImplicitRouting(configName, numberOfShards, replicationFactor, allCollectionList);
      }
    } catch (Exception ex) {
      logger.error("Error creating collection. collectionName=" + collectionName, ex);
    }
    return result;
  }

  private List<String> getCollections() throws SolrServerException,
    IOException {
    try {
      CollectionAdminRequest.List colListReq = new CollectionAdminRequest.List();
      CollectionAdminResponse response = colListReq.process(solrClient);
      if (response.getStatus() != 0) {
        logger.error("Error getting collection list from solr.  response=" + response);
        return null;
      }

      @SuppressWarnings("unchecked")
      List<String> allCollectionList = (List<String>) response.getResponse().get("collections");
      return allCollectionList;
    } catch (SolrException e) {
      logger.error(e);
      return new ArrayList<String>();
    }
  }

  private boolean setupCollectionsWithImplicitRouting(String configName, int numberOfShards, int replicationFactor,
                                                     List<String> allCollectionList) throws Exception {
    logger.info("setupCollectionsWithImplicitRouting(). collectionName=" + collectionName + ", numberOfShards=" + numberOfShards);

    // Default is true, because if the collection and shard is already there, then it will return true
    boolean returnValue = true;
    
    List<String> shardsList = new ArrayList<String>();
    for (int i = 0; i < numberOfShards; i++) {
      shardsList.add("shard" + i);
    }
    String shardsListStr = StringUtils.join(shardsList, ',');

    // Check if collection is already in zookeeper
    if (!allCollectionList.contains(collectionName)) {
      logger.info("Creating collection " + collectionName + ", shardsList=" + shardsList + ", solrDetail=" + solrDetail);
      CollectionAdminRequest.Create collectionCreateRequest = new CollectionAdminRequest.Create();
      collectionCreateRequest.setCollectionName(collectionName);
      collectionCreateRequest.setRouterName("implicit");
      collectionCreateRequest.setShards(shardsListStr);
      collectionCreateRequest.setNumShards(numberOfShards);
      collectionCreateRequest.setReplicationFactor(replicationFactor);
      collectionCreateRequest.setConfigName(configName);
      collectionCreateRequest.setRouterField(ROUTER_FIELD);
      collectionCreateRequest.setMaxShardsPerNode(replicationFactor * numberOfShards);

      CollectionAdminResponse createResponse = collectionCreateRequest.process(solrClient);
      if (createResponse.getStatus() != 0) {
        returnValue = false;
        logger.error("Error creating collection. collectionName=" + collectionName + ", shardsList=" + shardsList +
            ", solrDetail=" + solrDetail + ", response=" + createResponse);
      } else {
        logger.info("Created collection " + collectionName + ", shardsList=" + shardsList + ", solrDetail=" + solrDetail);
      }
    } else {
      logger.info("Collection " + collectionName + " is already there. Will check whether it has the required shards");
      Collection<String> existingShards = getShards();
      for (String shard : shardsList) {
        if (!existingShards.contains(shard)) {
          try {
            logger.info("Going to add Shard " + shard + " to collection " + collectionName);
            CollectionAdminRequest.CreateShard createShardRequest = new CollectionAdminRequest.CreateShard();
            createShardRequest.setCollectionName(collectionName);
            createShardRequest.setShardName(shard);
            CollectionAdminResponse response = createShardRequest.process(solrClient);
            if (response.getStatus() != 0) {
              logger.error("Error creating shard " + shard + " in collection " + collectionName + ", response=" + response +
                  ", solrDetail=" + solrDetail);
              returnValue = false;
              break;
            } else {
              logger.info("Successfully created shard " + shard + " in collection " + collectionName);
            }
          } catch (Throwable t) {
            logger.error("Error creating shard " + shard + " in collection " + collectionName + ", solrDetail=" + solrDetail, t);
            returnValue = false;
            break;
          }
        }
      }
    }
    return returnValue;
  }

  private Collection<String> getShards() {
    Collection<String> list = new HashSet<String>();

    if (solrClouldClient == null) {
      logger.error("getShards(). Only supporting in SolrCloud mode");
      return list;
    }

    ZkStateReader reader = solrClouldClient.getZkStateReader();
    Collection<Slice> slices = reader.getClusterState().getSlices(collectionName);
    for (Slice slice : slices) {
      for (Replica replica : slice.getReplicas()) {
        logger.info("colName=" + collectionName + ", slice.name=" + slice.getName() + ", slice.state=" + slice.getState() +
            ", replica.core=" + replica.getStr("core") + ", replica.state=" + replica.getStr("state"));
        list.add(slice.getName());
      }
    }
    return list;
  }

  private boolean createCollection(String configName, int numberOfShards, int replicationFactor,
                                  List<String> allCollectionList) throws SolrServerException, IOException {
    if (allCollectionList.contains(collectionName)) {
      logger.info("Collection " + collectionName + " is already there. Won't create it");
      return true;
    }

    logger.info("Creating collection " + collectionName + ", numberOfShards=" + numberOfShards +
        ", replicationFactor=" + replicationFactor + ", solrDetail=" + solrDetail);

    CollectionAdminRequest.Create collectionCreateRequest = new CollectionAdminRequest.Create();
    collectionCreateRequest.setCollectionName(collectionName);
    collectionCreateRequest.setNumShards(numberOfShards);
    collectionCreateRequest.setReplicationFactor(replicationFactor);
    collectionCreateRequest.setConfigName(configName);
    collectionCreateRequest.setMaxShardsPerNode(replicationFactor * numberOfShards);
    CollectionAdminResponse createResponse = collectionCreateRequest.process(solrClient);
    if (createResponse.getStatus() != 0) {
      logger.error("Error creating collection. collectionName=" + collectionName + ", solrDetail=" + solrDetail + ", response=" +
    createResponse);
      return false;
    } else {
      logger.info("Created collection " + collectionName + ", numberOfShards=" + numberOfShards +
          ", replicationFactor=" + replicationFactor + ", solrDetail=" + solrDetail);
      return true;
    }
  }

  public QueryResponse process(SolrQuery solrQuery) throws SolrServerException, IOException {
    if (solrClient != null) {
      String event = solrQuery.get("event");
      solrQuery.remove("event");
      QueryResponse queryResponse = solrClient.query(solrQuery, METHOD.POST);

      if (event != null && !"/audit/logs/live/count".equalsIgnoreCase(event)) {
        logPerformance.info("\n Username :- " + LogSearchContext.getCurrentUsername() + " Event :- " + event + " SolrQuery :- " +
            solrQuery + "\nQuery Time Execution :- " + queryResponse.getQTime() + " Total Time Elapsed is :- " +
            queryResponse.getElapsedTime());
      }
      return queryResponse;
    } else {
      throw restErrorUtil.createRESTException("Solr configuration improper for " + logType.getLabel() +" logs",
          MessageEnums.ERROR_SYSTEM);
    }
  }

  public UpdateResponse addDocs(SolrInputDocument doc) throws SolrServerException, IOException, SolrException {
    UpdateResponse updateResoponse = solrClient.add(doc);
    logPerformance.info("\n Username :- " + LogSearchContext.getCurrentUsername() +
        " Update Time Execution :- " + updateResoponse.getQTime() + " Total Time Elapsed is :- " + updateResoponse.getElapsedTime());
    solrClient.commit();
    return updateResoponse;
  }

  public UpdateResponse removeDoc(String query) throws SolrServerException, IOException, SolrException {
    UpdateResponse updateResoponse = solrClient.deleteByQuery(query);
    solrClient.commit();
    logPerformance.info("\n Username :- " + LogSearchContext.getCurrentUsername() +
        " Remove Time Execution :- " + updateResoponse.getQTime() + " Total Time Elapsed is :- " + updateResoponse.getElapsedTime());
    return updateResoponse;
  }

  private void setupSecurity() {
    String jaasFile = PropertiesUtil.getProperty("logsearch.solr.jaas.file", "/etc/security/keytabs/logsearch_solr.service.keytab");
    boolean securityEnabled = PropertiesUtil.getBooleanProperty("logsearch.solr.kerberos.enable", false);
    if (securityEnabled) {
      System.setProperty("java.security.auth.login.config", jaasFile);
      HttpClientUtil.setConfigurer(new Krb5HttpClientConfigurer());
      logger.info("setupSecurity() called for kerberos configuration, jaas file: " + jaasFile);
    }
  }

  private void populateSchemaFields() {
    if (!populateFieldsThreadActive) {
      populateFieldsThreadActive = true;
      logger.info("Creating thread to populated fields for collection=" + collectionName);
      Thread fieldPopulationThread = new Thread("populated_fields_" + collectionName) {
        @Override
        public void run() {
          logger.info("Started thread to get fields for collection=" + collectionName);
          int retryCount = 0;
          while (true) {
            try {
              Thread.sleep(SETUP_RETRY_SECOND * 1000);
              retryCount++;
              boolean _result = _populateSchemaFields();
              if (_result) {
                logger.info("Populate fields for collection " + collectionName + " is success, Update it after " +
                    SETUP_UPDATE_SECOND + " sec");
                Thread.sleep(SETUP_UPDATE_SECOND * 1000);
              }
            } catch (InterruptedException sleepInterrupted) {
              logger.info("Sleep interrupted while populating fields for collection " + collectionName);
              break;
            } catch (Exception ex) {
              logger.error("Error while populating fields for collection " + collectionName + ", retryCount=" + retryCount);
            }
          }
          populateFieldsThreadActive = false;
          logger.info("Exiting thread for populating fields. collection=" + collectionName);
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
    request.setPath("/schema");
    String historyCollection = PropertiesUtil.getProperty("logsearch.solr.collection.history","history");
    if (solrClient != null && !collectionName.equals(historyCollection)) {
      NamedList<Object> namedList = null;
      try {
        namedList = solrClient.request(request);
        logger.info("populateSchemaFields() collection=" + collectionName + ", fields=" + namedList);
      } catch (SolrException | SolrServerException | IOException e) {
        logger.error("Error occured while popuplating field. collection=" + collectionName, e);
      }
      
      if (namedList != null) {
        ConfigUtil.extractSchemaFieldsName(namedList.toString(), schemaFieldsNameMap,schemaFieldTypeMap);
        return true;
      }
    }
    return false;
  }
}
