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
import java.util.List;

import org.apache.ambari.logsearch.common.LogSearchContext;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.dao.UserConfigSolrDao;
import org.apache.ambari.logsearch.model.common.LogFeederDataMap;
import org.apache.ambari.logsearch.model.request.impl.UserConfigRequest;
import org.apache.ambari.logsearch.model.response.UserConfigData;
import org.apache.ambari.logsearch.model.response.UserConfigDataListResponse;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.core.convert.ConversionService;

import javax.inject.Inject;
import javax.inject.Named;

import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.ID;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.USER_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.VALUES;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.FILTER_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.ROW_TYPE;
import static org.apache.ambari.logsearch.solr.SolrConstants.UserConfigConstants.SHARE_NAME_LIST;

@Named
public class UserConfigManager extends JsonManagerBase {

  private static final Logger logger = Logger.getLogger(UserConfigManager.class);

  @Inject
  private UserConfigSolrDao userConfigSolrDao;
  @Inject
  private ConversionService conversionService;

  public String saveUserConfig(UserConfigData userConfig) {
    String filterName = userConfig.getFiltername();

    SolrInputDocument solrInputDoc = new SolrInputDocument();
    if (!isValid(userConfig)) {
      throw RESTErrorUtil.createRESTException("No FilterName Specified", MessageEnums.INVALID_INPUT_DATA);
    }

    if (isNotUnique(filterName)) {
      throw RESTErrorUtil.createRESTException( "Name '" + userConfig.getFiltername() + "' already exists", MessageEnums.INVALID_INPUT_DATA);
    }
    solrInputDoc.addField(ID, userConfig.getId());
    solrInputDoc.addField(USER_NAME, LogSearchContext.getCurrentUsername());
    solrInputDoc.addField(VALUES, userConfig.getValues());
    solrInputDoc.addField(FILTER_NAME, filterName);
    solrInputDoc.addField(ROW_TYPE, userConfig.getRowType());
    List<String> shareNameList = userConfig.getShareNameList();
    if (CollectionUtils.isNotEmpty(shareNameList)) {
      solrInputDoc.addField(SHARE_NAME_LIST, shareNameList);
    }

    try {
      userConfigSolrDao.addDocs(solrInputDoc);
      return convertObjToString(solrInputDoc);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error saving user config. solrDoc=" + solrInputDoc, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  private boolean isNotUnique(String filterName) {

    if (filterName != null) {
      SolrQuery solrQuery = new SolrQuery();
      filterName = SolrUtil.makeSearcableString(filterName);
      solrQuery.setQuery("*:*");
      solrQuery.addFilterQuery(FILTER_NAME + ":" + filterName);
      solrQuery.addFilterQuery(USER_NAME + ":" + LogSearchContext.getCurrentUsername());
      SolrUtil.setRowCount(solrQuery, 0);
      try {
        Long numFound = userConfigSolrDao.process(solrQuery).getResults().getNumFound();
        if (numFound > 0) {
          return true;
        }
      } catch (SolrException e) {
        logger.error("Error while checking if userConfig is unique.", e);
      }
    }
    return false;
  }

  private boolean isValid(UserConfigData vHistory) {
    return StringUtils.isNotBlank(vHistory.getFiltername())
        && StringUtils.isNotBlank(vHistory.getRowType())
        && StringUtils.isNotBlank(vHistory.getValues());
  }

  public void deleteUserConfig(String id) {
    try {
      userConfigSolrDao.deleteUserConfig(id);
    } catch (SolrException | SolrServerException | IOException e) {
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  @SuppressWarnings("unchecked")
  public UserConfigDataListResponse getUserConfig(UserConfigRequest request) {
    UserConfigDataListResponse response = new UserConfigDataListResponse();
    String rowType = request.getRowType();
    if (StringUtils.isBlank(rowType)) {
      throw RESTErrorUtil.createRESTException("row type was not specified", MessageEnums.INVALID_INPUT_DATA);
    }

    SolrQuery userConfigQuery = conversionService.convert(request, SolrQuery.class);
    userConfigQuery.addFilterQuery(String.format("%s:%s OR %s:%s", USER_NAME, LogSearchContext.getCurrentUsername(),
        SHARE_NAME_LIST, LogSearchContext.getCurrentUsername()));
    SolrDocumentList solrList = userConfigSolrDao.process(userConfigQuery).getResults();

    Collection<UserConfigData> configList = new ArrayList<>();

    for (SolrDocument solrDoc : solrList) {
      UserConfigData userConfig = new UserConfigData();
      userConfig.setFiltername("" + solrDoc.get(FILTER_NAME));
      userConfig.setId("" + solrDoc.get(ID));
      userConfig.setValues("" + solrDoc.get(VALUES));
      userConfig.setRowType("" + solrDoc.get(ROW_TYPE));
      try {
        List<String> shareNameList = (List<String>) solrDoc.get(SHARE_NAME_LIST);
        userConfig.setShareNameList(shareNameList);
      } catch (Exception e) {
        // do nothing
      }

      userConfig.setUserName("" + solrDoc.get(USER_NAME));

      configList.add(userConfig);
    }

    response.setName("historyList");
    response.setUserConfigList(configList);

    response.setStartIndex(Integer.parseInt(request.getStartIndex()));
    response.setPageSize(Integer.parseInt(request.getPageSize()));

    response.setTotalCount((long) solrList.getNumFound());

    return response;

  }

  // ////////////////////////////LEVEL FILTER/////////////////////////////////////

  public LogFeederDataMap getUserFilter() {
    LogFeederDataMap userFilter;
    try {
      userFilter = userConfigSolrDao.getUserFilter();
    } catch (SolrServerException | IOException e) {
      logger.error(e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
    return userFilter;
  }

  public LogFeederDataMap saveUserFiter(LogFeederDataMap logfeederFilters) {
    try {
      userConfigSolrDao.saveUserFilter(logfeederFilters);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("user config not able to save", e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
    return getUserFilter();
  }

  public List<String> getAllUserName() {
    List<String> userList = new ArrayList<String>();
    try {
      SolrQuery userListQuery = new SolrQuery();
      userListQuery.setQuery("*:*");
      SolrUtil.setFacetField(userListQuery, USER_NAME);
      QueryResponse queryResponse = userConfigSolrDao.process(userListQuery);
      if (queryResponse == null) {
        return userList;
      }
      List<Count> counList = queryResponse.getFacetField(USER_NAME).getValues();
      for (Count cnt : counList) {
        String userName = cnt.getName();
        userList.add(userName);
      }
    } catch (SolrException e) {
      logger.warn("Error getting all users.", e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
    return userList;
  }
}
