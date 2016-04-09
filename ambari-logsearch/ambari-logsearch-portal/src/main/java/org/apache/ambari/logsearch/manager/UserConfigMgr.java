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

package org.apache.ambari.logsearch.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.dao.UserConfigSolrDao;
import org.apache.ambari.logsearch.query.QueryGeneration;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.ambari.logsearch.util.StringUtil;
import org.apache.ambari.logsearch.view.VLogfeederFilterWrapper;
import org.apache.ambari.logsearch.view.VUserConfig;
import org.apache.ambari.logsearch.view.VUserConfigList;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonParseException;

@Component
public class UserConfigMgr extends MgrBase {
  static Logger logger = Logger.getLogger(UserConfigMgr.class);

  @Autowired
  UserConfigSolrDao userConfigSolrDao;

  @Autowired
  SolrUtil solrUtil;

  @Autowired
  RESTErrorUtil restErrorUtil;

  @Autowired
  QueryGeneration queryGenerator;

  @Autowired
  StringUtil stringUtil;

  public String saveUserConfig(VUserConfig vHistory) {

    SolrInputDocument solrInputDoc = new SolrInputDocument();
    if (!isValid(vHistory)) {
      throw restErrorUtil.createRESTException("No FilterName Specified",
        MessageEnums.INVALID_INPUT_DATA);
    }

    if (isNotUnique(vHistory) && !vHistory.isOverwrite()) {
      throw restErrorUtil.createRESTException(
        "Name '" + vHistory.getFilterName() + "' already exists",
        MessageEnums.INVALID_INPUT_DATA);
    }

    solrInputDoc.addField(LogSearchConstants.ID, vHistory.getId());
    solrInputDoc.addField(LogSearchConstants.USER_NAME,
      vHistory.getUserName());
    solrInputDoc.addField(LogSearchConstants.VALUES, vHistory.getValues());
    solrInputDoc.addField(LogSearchConstants.FILTER_NAME,
      vHistory.getFilterName());
    solrInputDoc.addField(LogSearchConstants.ROW_TYPE,
      vHistory.getRowType());
    List<String> shareNameList = vHistory.getShareNameList();
    if (shareNameList != null && !shareNameList.isEmpty())
      solrInputDoc.addField(LogSearchConstants.SHARE_NAME_LIST, shareNameList);

    try {
      userConfigSolrDao.addDocs(solrInputDoc);
      return convertObjToString(solrInputDoc);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  private boolean isNotUnique(VUserConfig vHistory) {
    String filterName = vHistory.getFilterName();
    String rowType = vHistory.getRowType();

    if (filterName != null && rowType != null) {
      SolrQuery solrQuery = new SolrQuery();
      filterName = solrUtil.makeSearcableString(filterName);
      solrQuery.setQuery(LogSearchConstants.COMPOSITE_KEY + ":"
        + filterName + "-" + rowType);
      queryGenerator.setRowCount(solrQuery, 0);
      try {
        Long numFound = userConfigSolrDao.process(solrQuery)
          .getResults().getNumFound();
        if (numFound > 0)
          return true;
      } catch (SolrException | SolrServerException | IOException e) {
        logger.error(e);
      }
    }
    return false;
  }

  private boolean isValid(VUserConfig vHistory) {

    return !stringUtil.isEmpty(vHistory.getFilterName())
      && !stringUtil.isEmpty(vHistory.getRowType())
      && !stringUtil.isEmpty(vHistory.getUserName())
      && !stringUtil.isEmpty(vHistory.getValues());
  }

  public void deleteUserConfig(String id) {
    try {
      userConfigSolrDao.removeDoc("id:" + id);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  @SuppressWarnings("unchecked")
  public String getUserConfig(SearchCriteria searchCriteria) {

    SolrDocumentList solrList = new SolrDocumentList();
    VUserConfigList userConfigList = new VUserConfigList();

    String rowType = (String) searchCriteria
      .getParamValue(LogSearchConstants.ROW_TYPE);
    if (stringUtil.isEmpty(rowType)) {
      throw restErrorUtil.createRESTException(
        "row type was not specified",
        MessageEnums.INVALID_INPUT_DATA);
    }

    String userName = (String) searchCriteria
      .getParamValue(LogSearchConstants.USER_NAME);
    if (stringUtil.isEmpty(userName)) {
      throw restErrorUtil.createRESTException(
        "user name was not specified",
        MessageEnums.INVALID_INPUT_DATA);
    }
    String filterName = (String) searchCriteria
      .getParamValue(LogSearchConstants.FILTER_NAME);
    filterName = stringUtil.isEmpty(filterName) ? "*" : "*" + filterName
      + "*";

    try {

      SolrQuery userConfigQuery = new SolrQuery();
      queryGenerator.setMainQuery(userConfigQuery, null);
      queryGenerator.setPagination(userConfigQuery, searchCriteria);
      queryGenerator.setSingleIncludeFilter(userConfigQuery,
        LogSearchConstants.ROW_TYPE, rowType);
      queryGenerator.setSingleORFilter(userConfigQuery,
        LogSearchConstants.USER_NAME, userName,
        LogSearchConstants.SHARE_NAME_LIST, userName);
      queryGenerator.setSingleIncludeFilter(userConfigQuery,
        LogSearchConstants.FILTER_NAME, filterName);

      if (stringUtil.isEmpty(searchCriteria.getSortBy())
        || searchCriteria.getSortBy().equals("historyName"))
        searchCriteria
          .setSortBy(LogSearchConstants.FILTER_NAME);
      if (stringUtil.isEmpty(searchCriteria.getSortType()))
        searchCriteria.setSortType("" + SolrQuery.ORDER.asc);

      queryGenerator.setSingleSortOrder(userConfigQuery, searchCriteria);
      solrList = userConfigSolrDao.process(userConfigQuery).getResults();

      Collection<VUserConfig> configList = new ArrayList<VUserConfig>();

      for (SolrDocument solrDoc : solrList) {
        VUserConfig userConfig = new VUserConfig();
        userConfig.setFilterName(""
          + solrDoc.get(LogSearchConstants.FILTER_NAME));
        userConfig.setId("" + solrDoc.get(LogSearchConstants.ID));
        userConfig.setValues("" + solrDoc.get(LogSearchConstants.VALUES));
        userConfig.setRowType(""
          + solrDoc.get(LogSearchConstants.ROW_TYPE));
        try {
          List<String> shareNameList = (List<String>) solrDoc
            .get(LogSearchConstants.SHARE_NAME_LIST);
          userConfig.setShareNameList(shareNameList);
        } catch (Exception e) {
          // do nothing
        }

        userConfig.setUserName(""
          + solrDoc.get(LogSearchConstants.USER_NAME));

        configList.add(userConfig);
      }

      userConfigList.setName("historyList");
      userConfigList.setUserConfigList(configList);

      userConfigList.setStartIndex(searchCriteria.getStartIndex());
      userConfigList.setPageSize((int) searchCriteria.getMaxRows());

      userConfigList.setTotalCount((long) solrList.getNumFound());
      userConfigList
        .setResultSize((int) (configList.size() - searchCriteria
          .getStartIndex()));
    } catch (SolrException | SolrServerException | IOException e) {
      // do nothing
    }
    try {
      return convertObjToString(userConfigList);
    } catch (IOException e) {
      return "";
    }
  }

  public String updateUserConfig(VUserConfig vuserConfig) {
    String id = "" + vuserConfig.getId();
    deleteUserConfig(id);
    return saveUserConfig(vuserConfig);
  }

  // ////////////////////////////LEVEL
  // FILTER/////////////////////////////////////

  /**
   * @return
   */
  @SuppressWarnings("unchecked")
  public String getUserFilter() {
    String filterName = LogSearchConstants.LOGFEEDER_FILTER_NAME;
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    String fq = LogSearchConstants.ROW_TYPE + ":" + filterName;
    solrQuery.setFilterQueries(fq);
    try {
      QueryResponse response = userConfigSolrDao.process(solrQuery);
      SolrDocumentList documentList = response.getResults();
      VLogfeederFilterWrapper logfeederFilterWrapper = null;
      if (documentList != null && documentList.size() > 0) {
        SolrDocument configDoc = documentList.get(0);
        String configJson = jsonUtil.objToJson(configDoc);
        HashMap<String, Object> configMap = (HashMap<String, Object>) jsonUtil.jsonToMapObject(configJson);
        String json = (String) configMap.get(LogSearchConstants.VALUES);
        logfeederFilterWrapper = (VLogfeederFilterWrapper) jsonUtil.jsonToObj(json, VLogfeederFilterWrapper.class);
        logfeederFilterWrapper.setId("" + configDoc.get(LogSearchConstants.ID));

      } else {
        String hadoopServiceString = getHadoopServiceConfigJSON();
        try {

          JSONObject componentList = new JSONObject();
          JSONObject jsonValue = new JSONObject();

          JSONObject hadoopServiceJsonObject = new JSONObject(hadoopServiceString)
            .getJSONObject("service");
          Iterator<String> hadoopSerivceKeys = hadoopServiceJsonObject
            .keys();
          while (hadoopSerivceKeys.hasNext()) {
            String key = hadoopSerivceKeys.next();
            JSONArray componentArray = hadoopServiceJsonObject
              .getJSONObject(key).getJSONArray("components");
            for (int i = 0; i < componentArray.length(); i++) {
              JSONObject compJsonObject = (JSONObject) componentArray
                .get(i);
              String componentName = compJsonObject
                .getString("name");
              JSONObject innerContent = new JSONObject();
              innerContent.put("label", componentName);
              innerContent.put("hosts", new JSONArray());
              innerContent.put("defaultLevels", new JSONArray());
              componentList.put(componentName, innerContent);
            }
          }
          jsonValue.put("filter", componentList);
          return saveUserFiter(jsonValue.toString());

        } catch (JsonParseException | JSONException je) {
          logger.error(je);
          logfeederFilterWrapper = new VLogfeederFilterWrapper();
        }
      }
      return convertObjToString(logfeederFilterWrapper);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(e.getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  /**
   * Creating filter for logfeeder
   *
   * @param String
   * @return
   */
  public String saveUserFiter(String json) {
    VLogfeederFilterWrapper logfeederFilterWrapper = (VLogfeederFilterWrapper) jsonUtil.jsonToObj(json,
      VLogfeederFilterWrapper.class);
    if (logfeederFilterWrapper == null) {
      logger.error("filter json is not a valid :" + json);
      throw restErrorUtil.createRESTException("Invalid filter json", MessageEnums.ERROR_SYSTEM);
    }
    String id = logfeederFilterWrapper.getId();
    if (!stringUtil.isEmpty(id)) {
      deleteUserConfig(id);
    }
    String filterName = LogSearchConstants.LOGFEEDER_FILTER_NAME;
    json = jsonUtil.objToJson(logfeederFilterWrapper);
    SolrInputDocument conifgDocument = new SolrInputDocument();
    conifgDocument.addField(LogSearchConstants.ID, new Date().getTime());
    conifgDocument.addField(LogSearchConstants.ROW_TYPE, filterName);
    conifgDocument.addField(LogSearchConstants.VALUES, json);
    conifgDocument.addField(LogSearchConstants.USER_NAME, filterName);
    conifgDocument.addField(LogSearchConstants.FILTER_NAME, filterName);
    try {
      userConfigSolrDao.addDocs(conifgDocument);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(e.getMessage(), MessageEnums.ERROR_SYSTEM);
    }
    return getUserFilter();
  }

  public String getAllUserName() {
    List<String> userList = new ArrayList<String>();
    try {
      SolrQuery userListQuery = new SolrQuery();
      queryGenerator.setMainQuery(userListQuery, null);
      queryGenerator.setFacetField(userListQuery,
        LogSearchConstants.USER_NAME);
      QueryResponse queryResponse = userConfigSolrDao
        .process(userListQuery);
      if (queryResponse == null)
        return convertObjToString(userList);
      List<Count> counList = queryResponse.getFacetField(
        LogSearchConstants.USER_NAME).getValues();
      for (Count cnt : counList) {
        String userName = cnt.getName();
        userList.add(userName);
      }
    } catch (SolrException | SolrServerException | IOException e) {
      // do nothing
    }

    try {
      return convertObjToString(userList);
    } catch (IOException e) {
      return "";
    }
  }

}
