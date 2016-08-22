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
import java.util.Iterator;
import java.util.List;
import org.apache.ambari.logsearch.common.LogsearchContextUtil;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.manager.MgrBase.LOG_TYPE;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.StringUtil;
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

public abstract class SolrDaoBase {
  static private Logger logger = Logger.getLogger(SolrDaoBase.class);
  
  public HashMap<String, String> schemaFieldsNameMap = new HashMap<String, String>();
  public HashMap<String, String> schemaFieldTypeMap = new HashMap<String, String>();
  
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
  
  String aliasName = null;
  Collection<String> aliasCollectionList = new ArrayList<String>();

  private SolrClient solrClient = null;
  CloudSolrClient solrClouldClient = null;

  boolean isSolrCloud = true;
  String solrDetail = "";

  boolean isSolrInitialized = false;

  private boolean setup_status = false;

  private boolean populateFieldsThreadActive = false;

  int SETUP_RETRY_SECOND = 30;
  int SETUP_UPDATE_SECOND = 10*60; //10 min
  int ALIAS_SETUP_RETRY_SECOND = 30*60; //30 minutes
  
  private boolean isZkConnectString=false;//by default its false
  
  //set logtype
  public SolrDaoBase(LOG_TYPE logType) {
    this.logType = logType;
  }

  public SolrClient connectToSolr(String url, String zkConnectString,
                                  String collection) throws Exception {
    this.collectionName = collection;
    solrDetail = "zkConnectString=" + zkConnectString + ", collection=" + collection
      + ", url=" + url;

    logger.info("connectToSolr() " + solrDetail);
    if (stringUtil.isEmpty(collection)) {
      throw new Exception("For solr, collection name is mandatory. "
        + solrDetail);
    }
    setupSecurity();
    if (!stringUtil.isEmpty(zkConnectString)) {
      isZkConnectString=true;
      solrDetail = "zkConnectString=" + zkConnectString + ", collection=" + collection;
      logger.info("Using zookeepr. " + solrDetail);
      solrClouldClient = new CloudSolrClient(zkConnectString);
      solrClouldClient.setDefaultCollection(collection);
      solrClient = solrClouldClient;
      int waitDurationMS = 3 * 60 * 1000;
      checkSolrStatus(waitDurationMS);
    } else {
      if (stringUtil.isEmpty(url)) {
        throw new Exception("Both zkConnectString and URL are empty. zkConnectString="
          + zkConnectString + ", collection=" + collection + ", url="
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

  public void setupAlias(final String aliasNameIn, final Collection<String> collectionListIn ) throws Exception {
    if( aliasNameIn == null || collectionListIn== null || collectionListIn.size() == 0 || solrClouldClient == null) {
      logger.info("Will not create alias " + aliasNameIn + " for "
        + (collectionListIn==null?null: collectionListIn.toString()) + ", solrCloudClient=" + solrClouldClient);
      return;
    }
    logger.info("setupAlias " + aliasNameIn + " for " + (collectionListIn==null?null: collectionListIn.toString()));
    aliasName = aliasNameIn;
    aliasCollectionList = collectionListIn;

    // Start a background thread to do setup
    Thread setupThread = new Thread("setup_alias_" + aliasNameIn) {
      @Override
      public void run() {
        logger.info("Started monitoring thread to check availability of Solr server. alias="
            + aliasNameIn + ", collections=" + collectionListIn.toString());
        int retryCount = 0;
        while (true) {
          try {
            int count = createAlias(aliasNameIn,collectionListIn);
            if (count > 0) {
              solrClouldClient.setDefaultCollection(aliasNameIn);
              if( count == collectionListIn.size()) {
                logger.info("Setup for alias " + aliasNameIn
                    + " is successful. Exiting setup retry thread. Collections=" + collectionListIn);
                populateSchemaFields();
                break;
              }
            } else {
              logger.warn("Not able to create alias="
                  + aliasNameIn + ", retryCount=" + retryCount);
            }
          } catch (Exception e) {
            logger.error("Error setting up alias=" + aliasNameIn, e);
          }
          try {
            Thread.sleep(ALIAS_SETUP_RETRY_SECOND * 1000);
          } catch (InterruptedException sleepInterrupted) {
            logger.info("Sleep interrupted while setting up alias "
                + aliasNameIn);
            break;
          }
          retryCount++;
        }
      }
    };
    setupThread.setDaemon(true);
    setupThread.start();     
  }
  
  /**
   * @param aliasNameIn
   * @param collectionListIn
   * @return
   * @throws IOException 
   * @throws SolrServerException 
   */
  protected int createAlias(String aliasNameIn,
      Collection<String> collectionListIn) throws SolrServerException, IOException {
    List<String> collections = getCollections();
    List<String> collectionToAdd = new ArrayList<String>();
    for (String col : collections) {
      if( collectionListIn.contains(col)) {
        collectionToAdd.add(col);
      }
    }
    String collectionsCSV = null;
    if( collectionToAdd.size() > 0 ) {
      for (String col : collectionToAdd) {
        if(collectionsCSV == null) {
          collectionsCSV = col;
        } else {
          collectionsCSV = collectionsCSV + ","  + col;
        }
      }
      CollectionAdminRequest.CreateAlias aliasCreateRequest = new CollectionAdminRequest.CreateAlias(); 
      aliasCreateRequest.setAliasName(aliasNameIn);
      aliasCreateRequest.setAliasedCollections(collectionsCSV);
      CollectionAdminResponse createResponse = aliasCreateRequest.process(solrClouldClient);
      if (createResponse.getStatus() != 0) {
        logger.error("Error creating alias. alias="
        + aliasNameIn + ", collectionList=" + collectionsCSV
        + ", solrDetail=" + solrDetail + ", response="
        + createResponse);
        return 0;
      }
    } 
    if( collectionToAdd.size() == collectionListIn.size()) {
      logger.info("Created alias for all collections. alias=" + aliasNameIn + ", collectionsCSV="
          + collectionsCSV + ", solrDetail=" + solrDetail);        
    } else {
      logger.info("Created alias for " + collectionToAdd.size() + " out of " + 
          + collectionListIn.size() + " collections. alias=" + aliasNameIn 
          + ", collectionsCSV=" + collectionsCSV + ", solrDetail=" + solrDetail);
    }
    return collectionToAdd.size();
  }

  public void setupCollections(final String splitMode, final String configName,
      final int numberOfShards, final int replicationFactor,boolean needToPopulateSchemaField) throws Exception {
    if (isZkConnectString) {
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
                Thread.sleep(SETUP_RETRY_SECOND * 1000);
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
    if(needToPopulateSchemaField){
      populateSchemaFields();
    }
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
          configName, numberOfShards, replicationFactor, allCollectionList);
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
                                                     String configName, int numberOfShards, int replicationFactor,
                                                     List<String> allCollectionList) throws Exception {
    logger.info("setupCollectionsWithImplicitRouting(). collectionName="
      + collectionName + ", numberOfShards=" + numberOfShards);
    return createCollectionWithImplicitRoute(collectionName, configName,
      numberOfShards, replicationFactor, allCollectionList);
  }

  public boolean createCollectionWithImplicitRoute(String colName,
                                                   String configName, int numberOfShards, int replicationFactor,
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

      if (event != null && !"/audit/logs/live/count".equalsIgnoreCase(event)) {
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
                logger.info("Populate fields for collection " + collectionName
                    + " is success, Update it after " + SETUP_UPDATE_SECOND
                    + " sec");
                Thread.sleep(SETUP_UPDATE_SECOND * 1000);
              }
            } catch (InterruptedException sleepInterrupted) {
              logger
                  .info("Sleep interrupted while populating fields for collection "
                      + collectionName);
              break;
            } catch (Exception ex) {
              logger.error("Error while populating fields for collection "
                  + collectionName + ", retryCount=" + retryCount);
            }
          }
          populateFieldsThreadActive = false;
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
    request.setPath("/schema");
    String historyCollection = PropertiesUtil.getProperty("logsearch.solr.collection.history","history");
    if (solrClient != null && !collectionName.equals(historyCollection)) {
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
          schemaFieldsNameMap,schemaFieldTypeMap);
        return true;
      }
    }
    return false;
  }
}
