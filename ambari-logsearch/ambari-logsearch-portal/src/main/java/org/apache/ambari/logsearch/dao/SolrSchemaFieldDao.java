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
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Scope(value = "prototype")
public class SolrSchemaFieldDao {

  private static final Logger LOG = LoggerFactory.getLogger(SolrSchemaFieldDao.class);

  private static final int SETUP_RETRY_SECOND = 30;
  private static final int SETUP_UPDATE_SECOND = 10 * 60; // 10 min

  private boolean populateFieldsThreadActive = false;

  @Inject
  private SolrUserPropsConfig solrUserPropsConfig;

  public void populateSchemaFields(final CloudSolrClient solrClient, final SolrPropsConfig solrPropsConfig,
                                   final SolrDaoBase solrDao) {
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
              boolean _result = _populateSchemaFields(solrClient, solrPropsConfig, solrDao);
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
  private boolean _populateSchemaFields(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig, SolrDaoBase solrDao) {
    SolrRequest<SchemaResponse> request = new SchemaRequest();
    request.setMethod(SolrRequest.METHOD.GET);
    request.setPath("/schema");
    String historyCollection = solrUserPropsConfig.getCollection();
    if (solrClient != null && !solrPropsConfig.getCollection().equals(historyCollection)) {
      NamedList<Object> namedList = null;
      try {
        namedList = solrClient.request(request);
        LOG.debug("populateSchemaFields() collection=" + solrPropsConfig.getCollection() + ", fields=" + namedList);
      } catch (SolrException | SolrServerException | IOException e) {
        LOG.error("Error occured while popuplating field. collection=" + solrPropsConfig.getCollection(), e);
      }

      if (namedList != null) {
        extractSchemaFieldsName(namedList.toString(), solrDao.schemaFieldNameMap, solrDao.schemaFieldTypeMap);
        return true;
      }
    }
    return false;
  }

  public void extractSchemaFieldsName(String responseString,
                                      final Map<String, String> schemaFieldsNameMap,
                                      final Map<String, String> schemaFieldTypeMap) {
    try {
      JSONObject jsonObject = new JSONObject(responseString);
      JSONObject schemajsonObject = jsonObject.getJSONObject("schema");
      JSONArray jsonArrayList = schemajsonObject.getJSONArray("fields");
      JSONArray fieldTypeJsonArray = schemajsonObject
        .getJSONArray("fieldTypes");
      if (jsonArrayList == null) {
        return;
      }
      if (fieldTypeJsonArray == null) {
        return;
      }
      HashMap<String, String> _schemaFieldTypeMap = new HashMap<>();
      HashMap<String, String> _schemaFieldsNameMap = new HashMap<String, String>();
      for (int i = 0; i < fieldTypeJsonArray.length(); i++) {
        JSONObject typeObject = fieldTypeJsonArray.getJSONObject(i);
        String name = typeObject.getString("name");
        String fieldTypeJson = typeObject.toString();
        _schemaFieldTypeMap.put(name, fieldTypeJson);
      }
      for (int i = 0; i < jsonArrayList.length(); i++) {
        JSONObject explrObject = jsonArrayList.getJSONObject(i);
        String name = explrObject.getString("name");
        String type = explrObject.getString("type");
        if (!name.contains("@") && !name.startsWith("_") && !name.contains("_md5") && !name.contains("_ms") &&
          !name.contains(LogSearchConstants.NGRAM_SUFFIX) && !name.contains("tags") && !name.contains("_str")) {
          _schemaFieldsNameMap.put(name, type);
        }
      }
      schemaFieldsNameMap.clear();
      schemaFieldTypeMap.clear();
      schemaFieldsNameMap.putAll(_schemaFieldsNameMap);
      schemaFieldTypeMap.putAll(_schemaFieldTypeMap);
    } catch (Exception e) {
      LOG.error(e + "Credentials not specified in logsearch.properties " + MessageEnums.ERROR_SYSTEM);
    }
  }
}
