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
import javax.inject.Named;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.LogSearchContext;
import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.conf.SolrUserPropsConfig;
import org.apache.ambari.logsearch.model.common.LogFeederDataMap;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
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

import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.log4j.Logger;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.util.CollectionUtils;

import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.ID;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.USER_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.VALUES;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.FILTER_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.ROW_TYPE;

@Named
public class UserConfigSolrDao extends SolrDaoBase {

  private static final Logger LOG = Logger.getLogger(UserConfigSolrDao.class);

  private static final Logger LOG_PERFORMANCE = Logger.getLogger("org.apache.ambari.logsearch.performance");

  @Inject
  private SolrUserPropsConfig solrUserConfig;

  @Inject
  private SolrCollectionDao solrCollectionDao;

  @Inject
  @Named("userConfigSolrFieldDao")
  private SolrSchemaFieldDao solrSchemaFieldDao;

  @Inject
  @Named("userConfigSolrTemplate")
  private SolrTemplate userConfigSolrTemplate;

  public UserConfigSolrDao() {
    super(LogType.SERVICE);
  }

  @Override
  public SolrTemplate getSolrTemplate() {
    return userConfigSolrTemplate;
  }

  @PostConstruct
  public void postConstructor() {
    String solrUrl = solrUserConfig.getSolrUrl();
    String zkConnectString = solrUserConfig.getZkConnectString();
    String collection = solrUserConfig.getCollection();

    try {
      solrCollectionDao.checkSolrStatus(getSolrClient());
      solrCollectionDao.setupCollections(getSolrClient(), solrUserConfig);
      solrSchemaFieldDao.populateSchemaFields(getSolrClient(), solrUserConfig);
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

  public void saveUserFilter(LogFeederDataMap logfeederFilterWrapper) throws SolrException, SolrServerException, IOException {
    String filterName = LogSearchConstants.LOGFEEDER_FILTER_NAME;
    String json = JSONUtil.objToJson(logfeederFilterWrapper);
    SolrInputDocument configDocument = new SolrInputDocument();
    configDocument.addField(ID, logfeederFilterWrapper.getId());
    configDocument.addField(ROW_TYPE, filterName);
    configDocument.addField(VALUES, json);
    configDocument.addField(USER_NAME, filterName);
    configDocument.addField(FILTER_NAME, filterName);
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
  public LogFeederDataMap getUserFilter() throws SolrServerException, IOException {

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    String fq = ROW_TYPE + ":" + LogSearchConstants.LOGFEEDER_FILTER_NAME;
    solrQuery.setFilterQueries(fq);

    QueryResponse response = process(solrQuery);
    SolrDocumentList documentList = response.getResults();
    LogFeederDataMap logfeederDataMap = null;
    if (!CollectionUtils.isEmpty(documentList)) {
      SolrDocument configDoc = documentList.get(0);
      String configJson = JSONUtil.objToJson(configDoc);
      HashMap<String, Object> configMap = JSONUtil.jsonToMapObject(configJson);
      String json = (String) configMap.get(VALUES);
      logfeederDataMap = (LogFeederDataMap) JSONUtil.jsonToObj(json, LogFeederDataMap.class);
      logfeederDataMap.setId("" + configDoc.get(ID));

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
        logfeederDataMap = (LogFeederDataMap) JSONUtil.jsonToObj(jsonValue.toString(), LogFeederDataMap.class);
        logfeederDataMap.setId(""+new Date().getTime());
        saveUserFilter(logfeederDataMap);

      } catch (JsonParseException | JSONException je) {
        LOG.error("Error parsing JSON. key=" + key + ", componentArray=" + componentArray, je);
        logfeederDataMap = new LogFeederDataMap();
      }
    }
    return logfeederDataMap;
  }

  @Override
  public SolrSchemaFieldDao getSolrSchemaFieldDao() {
    return solrSchemaFieldDao;
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
