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

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.conf.SolrEventHistoryPropsConfig;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.LukeResponse.FieldInfo;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.common.util.NamedList;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

public class SolrSchemaFieldDao {

  private static final Logger LOG = LoggerFactory.getLogger(SolrSchemaFieldDao.class);

  private static final int RETRY_SECOND = 30;

  @Inject
  private ServiceLogsSolrDao serviceLogsSolrDao;

  @Inject
  private AuditSolrDao auditSolrDao;
  
  @Inject
  private SolrEventHistoryPropsConfig solrEventHistoryPropsConfig;
  
  private int retryCount;
  private int skipCount;
  
  private Map<String, String> serviceSchemaFieldNameMap = new HashMap<>();
  private Map<String, String> serviceSchemaFieldTypeMap = new HashMap<>();
  private Map<String, String> auditSchemaFieldNameMap = new HashMap<>();
  private Map<String, String> auditSchemaFieldTypeMap = new HashMap<>();
  
  @Scheduled(fixedDelay = RETRY_SECOND * 1000)
  public void populateAllSchemaFields() {
    if (skipCount > 0) {
      skipCount--;
      return;
    }
    if (serviceLogsSolrDao.getSolrCollectionState().isSolrCollectionReady()) {
      CloudSolrClient serviceSolrClient = (CloudSolrClient) serviceLogsSolrDao.getSolrTemplate().getSolrClient();
      populateSchemaFields(serviceSolrClient, serviceSchemaFieldNameMap, serviceSchemaFieldTypeMap);
    }
    if (auditSolrDao.getSolrCollectionState().isSolrCollectionReady()) {
      CloudSolrClient auditSolrClient = (CloudSolrClient) auditSolrDao.getSolrTemplate().getSolrClient();
      populateSchemaFields(auditSolrClient, auditSchemaFieldNameMap, auditSchemaFieldTypeMap);
    }
  }

  private void populateSchemaFields(CloudSolrClient solrClient, Map<String, String> schemaFieldNameMap,
      Map<String, String> schemaFieldTypeMap) {
    if (solrClient != null) {
      LOG.debug("Started thread to get fields for collection=" + solrClient.getDefaultCollection());
      List<LukeResponse> lukeResponses = null;
      SchemaResponse schemaResponse = null;
      try {
        lukeResponses = getLukeResponsesForCores(solrClient);

        SolrRequest<SchemaResponse> schemaRequest = new SchemaRequest();
        schemaRequest.setMethod(SolrRequest.METHOD.GET);
        schemaRequest.setPath("/schema");
        schemaResponse = schemaRequest.process(solrClient);
        
        LOG.debug("populateSchemaFields() collection=" + solrClient.getDefaultCollection() + ", luke=" + lukeResponses +
            ", schema= " + schemaResponse);
      } catch (SolrException | SolrServerException | IOException e) {
        LOG.error("Error occured while popuplating field. collection=" + solrClient.getDefaultCollection(), e);
      }

      if (schemaResponse != null) {
        extractSchemaFieldsName(lukeResponses, schemaResponse, schemaFieldNameMap, schemaFieldTypeMap);
        LOG.debug("Populate fields for collection " + solrClient.getDefaultCollection()+ " was successful, next update it after " +
            solrEventHistoryPropsConfig.getPopulateIntervalMins() + " minutes");
        retryCount = 0;
        skipCount = (solrEventHistoryPropsConfig.getPopulateIntervalMins() * 60) / RETRY_SECOND - 1;
      }
      else {
        retryCount++;
        LOG.error("Error while populating fields for collection " + solrClient.getDefaultCollection() + ", retryCount=" + retryCount);
      }
    }
  }
  
  private static final String LUKE_REQUEST_URL_SUFFIX = "admin/luke?numTerms=0&wt=javabin&version=2";
  
  @SuppressWarnings("unchecked")
  private List<LukeResponse> getLukeResponsesForCores(CloudSolrClient solrClient) {
    ZkStateReader zkStateReader = solrClient.getZkStateReader();
    Collection<Slice> activeSlices = zkStateReader.getClusterState().getCollection(solrClient.getDefaultCollection()).getActiveSlices();
    
    List<LukeResponse> lukeResponses = new ArrayList<>();
    for (Slice slice : activeSlices) {
      for (Replica replica : slice.getReplicas()) {
        try (CloseableHttpClient httpClient = HttpClientUtil.createClient(null)) {
          HttpGet request = new HttpGet(replica.getCoreUrl() + LUKE_REQUEST_URL_SUFFIX);
          HttpResponse response = httpClient.execute(request);
          @SuppressWarnings("resource") // JavaBinCodec implements Closeable, yet it can't be closed if it is used for unmarshalling only
          NamedList<Object> lukeData = (NamedList<Object>) new JavaBinCodec().unmarshal(response.getEntity().getContent());
          LukeResponse lukeResponse = new LukeResponse();
          lukeResponse.setResponse(lukeData);
          lukeResponses.add(lukeResponse);
        } catch (IOException e) {
          LOG.error("Exception during getting luke responses", e);
        }
      }
    }
    return lukeResponses;
  }

  private void extractSchemaFieldsName(List<LukeResponse> lukeResponses, SchemaResponse schemaResponse,
      Map<String, String> schemaFieldNameMap, Map<String, String> schemaFieldTypeMap) {
    try {
      HashMap<String, String> _schemaFieldNameMap = new HashMap<>();
      HashMap<String, String> _schemaFieldTypeMap = new HashMap<>();
      
      for (LukeResponse lukeResponse : lukeResponses) {
        for (Entry<String, FieldInfo> e : lukeResponse.getFieldInfo().entrySet()) {
          String name = e.getKey();
          String type = e.getValue().getType();
          if (!name.contains("@") && !name.startsWith("_") && !name.contains("_md5") && !name.contains("_ms") &&
              !name.contains(LogSearchConstants.NGRAM_PREFIX) && !name.contains("tags") && !name.contains("_str")) {
            _schemaFieldNameMap.put(name, type);
          }
        }
      }
      
      List<FieldTypeDefinition> fieldTypes = schemaResponse.getSchemaRepresentation().getFieldTypes();
      for (FieldTypeDefinition fieldType : fieldTypes) {
        Map<String, Object> fieldAttributes = fieldType.getAttributes();
        String name = (String) fieldAttributes.get("name");
        String fieldTypeJson = new JSONObject(fieldAttributes).toString();
        _schemaFieldTypeMap.put(name, fieldTypeJson);
      }
      
      List<Map<String, Object>> fields = schemaResponse.getSchemaRepresentation().getFields();
      for (Map<String, Object> field : fields) {
        String name = (String) field.get("name");
        String type = (String) field.get("type");
        if (!name.contains("@") && !name.startsWith("_") && !name.contains("_md5") && !name.contains("_ms") &&
          !name.contains(LogSearchConstants.NGRAM_PREFIX) && !name.contains("tags") && !name.contains("_str")) {
          _schemaFieldNameMap.put(name, type);
        }
      }
      
      if (_schemaFieldNameMap.isEmpty() || _schemaFieldTypeMap.isEmpty()) {
        return;
      }
      
      synchronized (this) {
        schemaFieldNameMap.clear();
        schemaFieldNameMap.putAll(_schemaFieldNameMap);
        schemaFieldTypeMap.clear();
        schemaFieldTypeMap.putAll(_schemaFieldTypeMap);
      }
    } catch (Exception e) {
      LOG.error(e + "Credentials not specified in logsearch.properties " + MessageEnums.ERROR_SYSTEM);
    }
  }

  public Map<String, String> getSchemaFieldNameMap(LogType logType) {
    return LogType.AUDIT == logType ? auditSchemaFieldNameMap : serviceSchemaFieldNameMap;
  }

  public Map<String, String> getSchemaFieldTypeMap(LogType logType) {
    return LogType.AUDIT == logType ? auditSchemaFieldTypeMap : serviceSchemaFieldTypeMap;
  }
}
