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
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.SolrUserPropsConfig;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.LukeResponse.FieldInfo;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SolrSchemaFieldDao {

  private static final Logger LOG = LoggerFactory.getLogger(SolrSchemaFieldDao.class);

  private static final int SETUP_RETRY_SECOND = 30;
  private static final int SETUP_UPDATE_SECOND = 1 * 60; // 1 min
  
  private boolean populateFieldsThreadActive = false;

  private Map<String, String> schemaFieldNameMap = new HashMap<>();
  private Map<String, String> schemaFieldTypeMap = new HashMap<>();

  @Inject
  private SolrUserPropsConfig solrUserPropsConfig;

  public void populateSchemaFields(final CloudSolrClient solrClient, final SolrPropsConfig solrPropsConfig) {
    if (!populateFieldsThreadActive) {
      populateFieldsThreadActive = true;
      LOG.info("Creating thread to populated fields for collection=" + solrPropsConfig.getCollection());
      Thread fieldPopulationThread = new Thread("populated_fields_" + solrPropsConfig.getCollection()) {
        @Override
        public void run() {
          LOG.info("Started thread to get fields for collection=" + solrPropsConfig.getCollection());
          int retryCount = 0;
          while (true) {
            try {
              Thread.sleep(SETUP_RETRY_SECOND * 1000);
              retryCount++;
              boolean _result = _populateSchemaFields(solrClient, solrPropsConfig);
              if (_result) {
                LOG.info("Populate fields for collection " + solrPropsConfig.getCollection() + " is success, Update it after " +
                  SETUP_UPDATE_SECOND + " sec");
                Thread.sleep(SETUP_UPDATE_SECOND * 1000);
              }
            } catch (InterruptedException sleepInterrupted) {
              LOG.info("Sleep interrupted while populating fields for collection " + solrPropsConfig.getCollection());
              break;
            } catch (Exception ex) {
              LOG.error("Error while populating fields for collection " + solrPropsConfig.getCollection() + ", retryCount=" + retryCount);
            }
          }
          populateFieldsThreadActive = false;
          LOG.info("Exiting thread for populating fields. collection=" + solrPropsConfig.getCollection());
        }
      };
      fieldPopulationThread.setDaemon(true);
      fieldPopulationThread.start();
    }
  }

  /**
   * Called from the thread. Don't call this directly
   */
  private boolean _populateSchemaFields(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) {
    String historyCollection = solrUserPropsConfig.getCollection();
    if (solrClient != null && !solrPropsConfig.getCollection().equals(historyCollection)) {
      LukeResponse lukeResponse = null;
      SchemaResponse schemaResponse = null;
      try {
        LukeRequest lukeRequest = new LukeRequest();
        lukeRequest.setNumTerms(0);
        lukeResponse = lukeRequest.process(solrClient);
        
        SolrRequest<SchemaResponse> schemaRequest = new SchemaRequest();
        schemaRequest.setMethod(SolrRequest.METHOD.GET);
        schemaRequest.setPath("/schema");
        schemaResponse = schemaRequest.process(solrClient);
        
        LOG.debug("populateSchemaFields() collection=" + solrPropsConfig.getCollection() + ", luke=" + lukeResponse +
            ", schema= " + schemaResponse);
      } catch (SolrException | SolrServerException | IOException e) {
        LOG.error("Error occured while popuplating field. collection=" + solrPropsConfig.getCollection(), e);
      }

      if (lukeResponse != null && schemaResponse != null) {
        extractSchemaFieldsName(lukeResponse, schemaResponse);
        return true;
      }
    }
    return false;
  }

  private void extractSchemaFieldsName(LukeResponse lukeResponse, SchemaResponse schemaResponse) {
    try {
      HashMap<String, String> _schemaFieldNameMap = new HashMap<>();
      HashMap<String, String> _schemaFieldTypeMap = new HashMap<>();
      
      for (Entry<String, FieldInfo> e : lukeResponse.getFieldInfo().entrySet()) {
        String name = e.getKey();
        String type = e.getValue().getType();
        if (!name.contains("@") && !name.startsWith("_") && !name.contains("_md5") && !name.contains("_ms") &&
          !name.contains(LogSearchConstants.NGRAM_PREFIX) && !name.contains("tags") && !name.contains("_str")) {
          _schemaFieldNameMap.put(name, type);
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
        schemaFieldNameMap = _schemaFieldNameMap;
        schemaFieldTypeMap = _schemaFieldTypeMap;
      }
    } catch (Exception e) {
      LOG.error(e + "Credentials not specified in logsearch.properties " + MessageEnums.ERROR_SYSTEM);
    }
  }

  public synchronized Map<String, String> getSchemaFieldNameMap() {
    return schemaFieldNameMap;
  }

  public synchronized Map<String, String> getSchemaFieldTypeMap() {
    return schemaFieldTypeMap;
  }
}
