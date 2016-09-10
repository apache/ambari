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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.ambari.logsearch.common.LogSearchContext;
import org.apache.ambari.logsearch.conf.SolrUserPropsConfig;
import org.apache.ambari.logsearch.view.VLogfeederFilterWrapper;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import com.google.gson.JsonParseException;

import org.apache.ambari.logsearch.manager.ManagerBase.LogType;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class UserConfigSolrDao extends SolrDaoBase {

  private static final Logger LOG = Logger.getLogger(UserConfigSolrDao.class);

  private static final Logger LOG_PERFORMANCE = Logger.getLogger("org.apache.ambari.logsearch.performance");

  @Inject
  private SolrUserPropsConfig solrUserConfig;

  @Inject
  private SolrCollectionDao solrCollectionDao;

  @Inject
  private SolrSchemaFieldDao solrSchemaFieldDao;

  @Inject
  @Qualifier("userConfigSolrTemplate")
  private SolrTemplate userConfigSolrTemplate;

  public UserConfigSolrDao() {
    super(LogType.SERVICE);
  }

  @Override
  public CloudSolrClient getSolrClient() {
    return (CloudSolrClient) userConfigSolrTemplate.getSolrClient();
  }


  @PostConstruct
  public void postConstructor() {
    String solrUrl = solrUserConfig.getSolrUrl();
    String zkConnectString = solrUserConfig.getZkConnectString();
    String collection = solrUserConfig.getCollection();

    try {
      solrCollectionDao.checkSolrStatus(getSolrClient());
      solrCollectionDao.setupCollections(getSolrClient(), solrUserConfig);
      solrSchemaFieldDao.populateSchemaFields(getSolrClient(), solrUserConfig, this);
      intializeLogFeederFilter();

    } catch (Exception e) {
      LOG.error("error while connecting to Solr for history logs : solrUrl=" + solrUrl + ", zkConnectString=" + zkConnectString +
          ", collection=" + collection, e);
    }
  }

  private void intializeLogFeederFilter() {
    try {
      getUserFilter();
    } catch (SolrServerException | IOException e) {
      LOG.error("not able to save logfeeder filter while initialization", e);
    }
  }

  public void saveUserFilter(VLogfeederFilterWrapper logfeederFilterWrapper) throws SolrException, SolrServerException, IOException {
    String filterName = LogSearchConstants.LOGFEEDER_FILTER_NAME;
    String json = JSONUtil.objToJson(logfeederFilterWrapper);
    SolrInputDocument configDocument = new SolrInputDocument();
    configDocument.addField(LogSearchConstants.ID, logfeederFilterWrapper.getId());
    configDocument.addField(LogSearchConstants.ROW_TYPE, filterName);
    configDocument.addField(LogSearchConstants.VALUES, json);
    configDocument.addField(LogSearchConstants.USER_NAME, filterName);
    configDocument.addField(LogSearchConstants.FILTER_NAME, filterName);
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

	@SuppressWarnings("unchecked")
  public VLogfeederFilterWrapper getUserFilter() throws SolrServerException, IOException {

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    String fq = LogSearchConstants.ROW_TYPE + ":" + LogSearchConstants.LOGFEEDER_FILTER_NAME;
    solrQuery.setFilterQueries(fq);

    QueryResponse response = process(solrQuery);
    SolrDocumentList documentList = response.getResults();
    VLogfeederFilterWrapper logfeederFilterWrapper = null;
    if (!CollectionUtils.isEmpty(documentList)) {
      SolrDocument configDoc = documentList.get(0);
      String configJson = JSONUtil.objToJson(configDoc);
      HashMap<String, Object> configMap = JSONUtil.jsonToMapObject(configJson);
      String json = (String) configMap.get(LogSearchConstants.VALUES);
      logfeederFilterWrapper = (VLogfeederFilterWrapper) JSONUtil.jsonToObj(json, VLogfeederFilterWrapper.class);
      logfeederFilterWrapper.setId("" + configDoc.get(LogSearchConstants.ID));

    } else {
      List<String> logfeederDefaultLevels = solrUserConfig.getLogLevels();
      JSONArray levelJsonArray = new JSONArray(logfeederDefaultLevels);

      String hadoopServiceString = getHadoopServiceConfigJSON();
      String key = null;
      JSONArray componentArray = null;
      try {
        JSONObject componentList = new JSONObject();
        JSONObject jsonValue = new JSONObject();

        JSONObject hadoopServiceJsonObject = new JSONObject(hadoopServiceString).getJSONObject("service");
        Iterator<String> hadoopSerivceKeys = hadoopServiceJsonObject.keys();
        while (hadoopSerivceKeys.hasNext()) {
          key = hadoopSerivceKeys.next();
          componentArray = hadoopServiceJsonObject.getJSONObject(key).getJSONArray("components");
          for (int i = 0; i < componentArray.length(); i++) {
            JSONObject compJsonObject = (JSONObject) componentArray.get(i);
            String componentName = compJsonObject.getString("name");
            JSONObject innerContent = new JSONObject();
            innerContent.put("label", componentName);
            innerContent.put("hosts", new JSONArray());
            innerContent.put("defaultLevels", levelJsonArray);
            componentList.put(componentName, innerContent);
          }
        }
        jsonValue.put("filter", componentList);
        logfeederFilterWrapper = (VLogfeederFilterWrapper) JSONUtil.jsonToObj(jsonValue.toString(), VLogfeederFilterWrapper.class);
        logfeederFilterWrapper.setId(""+new Date().getTime());
        saveUserFilter(logfeederFilterWrapper);

      } catch (JsonParseException | JSONException je) {
        LOG.error("Error parsing JSON. key=" + key + ", componentArray=" + componentArray, je);
        logfeederFilterWrapper = new VLogfeederFilterWrapper();
      }
    }
    return logfeederFilterWrapper;
  }

  private String getHadoopServiceConfigJSON() {
    StringBuilder result = new StringBuilder("");

    // Get file from resources folder
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("HadoopServiceConfig.json").getFile());

    try (Scanner scanner = new Scanner(file)) {

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        result.append(line).append("\n");
      }

      scanner.close();

    } catch (IOException e) {
      LOG.error("Unable to read HadoopServiceConfig.json", e);
    }

    String hadoopServiceConfig = result.toString();
    if (JSONUtil.isJSONValid(hadoopServiceConfig)) {
      return hadoopServiceConfig;
    }
    return null;
  }
}
