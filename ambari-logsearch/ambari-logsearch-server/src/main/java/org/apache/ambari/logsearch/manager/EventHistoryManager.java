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
import org.apache.ambari.logsearch.dao.EventHistorySolrDao;
import org.apache.ambari.logsearch.model.request.impl.EventHistoryRequest;
import org.apache.ambari.logsearch.model.response.EventHistoryData;
import org.apache.ambari.logsearch.model.response.EventHistoryDataListResponse;
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

import static org.apache.ambari.logsearch.solr.SolrConstants.EventHistoryConstants.ID;
import static org.apache.ambari.logsearch.solr.SolrConstants.EventHistoryConstants.USER_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.EventHistoryConstants.VALUES;
import static org.apache.ambari.logsearch.solr.SolrConstants.EventHistoryConstants.FILTER_NAME;
import static org.apache.ambari.logsearch.solr.SolrConstants.EventHistoryConstants.ROW_TYPE;
import static org.apache.ambari.logsearch.solr.SolrConstants.EventHistoryConstants.SHARE_NAME_LIST;

@Named
public class EventHistoryManager extends JsonManagerBase {

  private static final Logger logger = Logger.getLogger(EventHistoryManager.class);

  @Inject
  private EventHistorySolrDao eventHistorySolrDao;
  @Inject
  private ConversionService conversionService;

  public String saveEvent(EventHistoryData eventHistoryData) {
    String filterName = eventHistoryData.getFiltername();

    SolrInputDocument solrInputDoc = new SolrInputDocument();
    if (!isValid(eventHistoryData)) {
      throw RESTErrorUtil.createRESTException("No FilterName Specified", MessageEnums.INVALID_INPUT_DATA);
    }

    if (isNotUnique(filterName)) {
      throw RESTErrorUtil.createRESTException( "Name '" + eventHistoryData.getFiltername() + "' already exists", MessageEnums.INVALID_INPUT_DATA);
    }
    solrInputDoc.addField(ID, eventHistoryData.getId());
    solrInputDoc.addField(USER_NAME, LogSearchContext.getCurrentUsername());
    solrInputDoc.addField(VALUES, eventHistoryData.getValues());
    solrInputDoc.addField(FILTER_NAME, filterName);
    solrInputDoc.addField(ROW_TYPE, eventHistoryData.getRowType());
    List<String> shareNameList = eventHistoryData.getShareNameList();
    if (CollectionUtils.isNotEmpty(shareNameList)) {
      solrInputDoc.addField(SHARE_NAME_LIST, shareNameList);
    }

    try {
      eventHistorySolrDao.addDocs(solrInputDoc);
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
        Long numFound = eventHistorySolrDao.process(solrQuery).getResults().getNumFound();
        if (numFound > 0) {
          return true;
        }
      } catch (SolrException e) {
        logger.error("Error while checking if event history data is unique.", e);
      }
    }
    return false;
  }

  private boolean isValid(EventHistoryData vHistory) {
    return StringUtils.isNotBlank(vHistory.getFiltername())
        && StringUtils.isNotBlank(vHistory.getRowType())
        && StringUtils.isNotBlank(vHistory.getValues());
  }

  public void deleteEvent(String id) {
    try {
      eventHistorySolrDao.deleteEventHistoryData(id);
    } catch (SolrException | SolrServerException | IOException e) {
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  @SuppressWarnings("unchecked")
  public EventHistoryDataListResponse getEventHistory(EventHistoryRequest request) {
    EventHistoryDataListResponse response = new EventHistoryDataListResponse();
    String rowType = request.getRowType();
    if (StringUtils.isBlank(rowType)) {
      throw RESTErrorUtil.createRESTException("row type was not specified", MessageEnums.INVALID_INPUT_DATA);
    }

    SolrQuery evemtHistoryQuery = conversionService.convert(request, SolrQuery.class);
    evemtHistoryQuery.addFilterQuery(String.format("%s:%s OR %s:%s", USER_NAME, LogSearchContext.getCurrentUsername(),
        SHARE_NAME_LIST, LogSearchContext.getCurrentUsername()));
    SolrDocumentList solrList = eventHistorySolrDao.process(evemtHistoryQuery).getResults();

    Collection<EventHistoryData> configList = new ArrayList<>();

    for (SolrDocument solrDoc : solrList) {
      EventHistoryData eventHistoryData = new EventHistoryData();
      eventHistoryData.setFiltername("" + solrDoc.get(FILTER_NAME));
      eventHistoryData.setId("" + solrDoc.get(ID));
      eventHistoryData.setValues("" + solrDoc.get(VALUES));
      eventHistoryData.setRowType("" + solrDoc.get(ROW_TYPE));
      try {
        List<String> shareNameList = (List<String>) solrDoc.get(SHARE_NAME_LIST);
        eventHistoryData.setShareNameList(shareNameList);
      } catch (Exception e) {
        // do nothing
      }

      eventHistoryData.setUserName("" + solrDoc.get(USER_NAME));

      configList.add(eventHistoryData);
    }

    response.setName("historyList");
    response.setEventHistoryDataList(configList);

    response.setStartIndex(Integer.parseInt(request.getStartIndex()));
    response.setPageSize(Integer.parseInt(request.getPageSize()));

    response.setTotalCount((long) solrList.getNumFound());

    return response;

  }

  public List<String> getAllUserName() {
    List<String> userList = new ArrayList<String>();
    try {
      SolrQuery userListQuery = new SolrQuery();
      userListQuery.setQuery("*:*");
      SolrUtil.setFacetField(userListQuery, USER_NAME);
      QueryResponse queryResponse = eventHistorySolrDao.process(userListQuery);
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
