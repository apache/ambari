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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.ambari.logsearch.common.ConfigHelper;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.PropertiesHelper;
import org.apache.ambari.logsearch.conf.SolrServiceLogConfig;
import org.apache.ambari.logsearch.dao.ServiceLogsSolrDao;
import org.apache.ambari.logsearch.graph.GraphDataGenerator;
import org.apache.ambari.logsearch.model.response.BarGraphData;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.response.CountData;
import org.apache.ambari.logsearch.model.response.CountDataListResponse;
import org.apache.ambari.logsearch.model.response.GraphData;
import org.apache.ambari.logsearch.model.response.GraphDataListResponse;
import org.apache.ambari.logsearch.model.response.GroupListResponse;
import org.apache.ambari.logsearch.model.response.LogData;
import org.apache.ambari.logsearch.model.response.LogListResponse;
import org.apache.ambari.logsearch.model.response.LogSearchResponse;
import org.apache.ambari.logsearch.model.response.NameValueData;
import org.apache.ambari.logsearch.model.response.NameValueDataListResponse;
import org.apache.ambari.logsearch.model.response.NodeData;
import org.apache.ambari.logsearch.model.response.NodeListResponse;
import org.apache.ambari.logsearch.model.response.ServiceLogData;
import org.apache.ambari.logsearch.model.response.ServiceLogResponse;
import org.apache.ambari.logsearch.query.QueryGenerationBase;
import org.apache.ambari.logsearch.solr.model.SolrComponentTypeLogData;
import org.apache.ambari.logsearch.solr.model.SolrHostLogData;
import org.apache.ambari.logsearch.solr.model.SolrServiceLogData;
import org.apache.ambari.logsearch.util.BizUtil;
import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.util.FileUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.ambari.logsearch.view.VSummary;
import org.apache.ambari.logsearch.query.model.SearchCriteria;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component
public class ServiceLogsManager extends ManagerBase<SolrServiceLogData, ServiceLogResponse> {

  private static final Logger logger = Logger.getLogger(ServiceLogsManager.class);

  private static List<String> cancelByDate = new CopyOnWriteArrayList<String>();

  private static Map<String, String> mapUniqueId = new ConcurrentHashMap<String, String>();

  private enum CONDITION {
    OR, AND
  }

  @Inject
  private ServiceLogsSolrDao serviceLogsSolrDao;
  @Inject
  private GraphDataGenerator graphDataGenerator;
  @Inject
  private SolrServiceLogConfig solrServiceLogConfig;

  public ServiceLogResponse searchLogs(SearchCriteria searchCriteria) {
    String keyword = (String) searchCriteria.getParamValue("keyword");
    String logId = (String) searchCriteria.getParamValue("sourceLogId");
    Boolean isLastPage = (Boolean) searchCriteria.getParamValue("isLastPage");

    if (!StringUtils.isBlank(keyword)) {
      try {
        return (ServiceLogResponse) getPageByKeyword(searchCriteria);
      } catch (SolrException | SolrServerException e) {
        logger.error("Error while getting keyword=" + keyword, e);
        throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
            .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }
    } else if (!StringUtils.isBlank(logId)) {
      try {
        return (ServiceLogResponse) getPageByLogId(searchCriteria);
      } catch (SolrException e) {
        logger.error("Error while getting keyword=" + keyword, e);
        throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
            .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }
    } else if (isLastPage) {
      SolrQuery lastPageQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
      ServiceLogResponse logResponse = getLastPage(searchCriteria,LogSearchConstants.LOGTIME,serviceLogsSolrDao,lastPageQuery);
      if(logResponse == null){
        logResponse = new ServiceLogResponse();
      }
      return logResponse;
    } else {
      SolrQuery solrQuery = queryGenerator
          .commonServiceFilterQuery(searchCriteria);

      solrQuery.setParam("event", "/service/logs");

      return getLogAsPaginationProvided(solrQuery,
          serviceLogsSolrDao);
    }
  }

  public GroupListResponse getHosts() {
    return getFields(LogSearchConstants.SOLR_HOST, SolrHostLogData.class);
  }
  
  private <T extends LogData> GroupListResponse getFields(String field, Class<T> clazz) {

    SolrQuery solrQuery = new SolrQuery();
    GroupListResponse collection = new GroupListResponse();
    SolrUtil.setMainQuery(solrQuery, null);
    SolrUtil.setFacetField(solrQuery,
        field);
    SolrUtil.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if(response == null){
        return collection;
      }
      FacetField facetField = response
        .getFacetField(field);
      if (facetField == null){
        return collection;
      }
      List<Count> fieldList = facetField.getValues();
      if (fieldList == null){
        return collection;
      }
      SolrDocumentList docList = response.getResults();
      if(docList == null){
        return collection;
      }
      List<LogData> groupList = getLogDataListByFieldType(clazz, response, fieldList);

      collection.setGroupList(groupList);
      if(!docList.isEmpty()){
        collection.setStartIndex((int) docList.getStart());
        collection.setTotalCount(docList.getNumFound());
      }
      return collection;
    } catch (IOException | SolrServerException | SolrException e) {
      logger.error(e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }

  }

  public GroupListResponse getComponents() {
    return getFields(LogSearchConstants.SOLR_COMPONENT, SolrComponentTypeLogData.class);
  }

  public GraphDataListResponse getAggregatedInfo(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    String hierarchy = "host,type,level";
    GraphDataListResponse graphInfo = new GraphDataListResponse();
    try {
      SolrUtil.setMainQuery(solrQuery, null);
      SolrUtil.setFacetPivot(solrQuery, 1, hierarchy);
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if (response == null) {
        return graphInfo;
      }

      List<List<PivotField>> hirarchicalPivotField = new ArrayList<List<PivotField>>();
      List<GraphData> dataList = new ArrayList<>();
      NamedList<List<PivotField>> namedList = response.getFacetPivot();
      if (namedList != null) {
        hirarchicalPivotField = namedList.getAll(hierarchy);
      }
      if (!hirarchicalPivotField.isEmpty()) {
        dataList = buidGraphData(hirarchicalPivotField.get(0));
      }
      if (!dataList.isEmpty()) {
        graphInfo.setGraphData(dataList);
      }

      return graphInfo;
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public List<GraphData> buidGraphData(List<PivotField> pivotFields) {
    List<GraphData> logList = new ArrayList<>();
    if (pivotFields != null) {
      for (PivotField pivotField : pivotFields) {
        if (pivotField != null) {
          GraphData logLevel = new GraphData();
          logLevel.setName("" + pivotField.getValue());
          logLevel.setCount(Long.valueOf(pivotField.getCount()));
          if (pivotField.getPivot() != null) {
            logLevel.setDataList(buidGraphData(pivotField.getPivot()));
          }
          logList.add(logLevel);
        }
      }
    }
    return logList;
  }

  public CountDataListResponse getFieldCount(String field){
    CountDataListResponse collection = new CountDataListResponse();
    List<CountData> vCounts = new ArrayList<>();
    SolrQuery solrQuery = new SolrQuery();
    SolrUtil.setMainQuery(solrQuery, null);
    if(field == null){
      return collection;
    }
    SolrUtil.setFacetField(solrQuery, field);
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if (response == null){
        return collection;
      }
      FacetField facetFields = response.getFacetField(field);
      if (facetFields == null){
        return collection;
      }
      List<Count> fieldList = facetFields.getValues();

      if(fieldList == null){
        return collection;
      }

      for (Count cnt : fieldList) {
        if (cnt != null) {
          CountData vCount = new CountData();
          vCount.setName(cnt.getName());
          vCount.setCount(cnt.getCount());
          vCounts.add(vCount);
        }
      }

    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }

    collection.setvCounts(vCounts);
    return collection;
  }
  
  public CountDataListResponse getLogLevelCount() {
    return getFieldCount(LogSearchConstants.SOLR_LEVEL);
  }

  public CountDataListResponse getComponentsCount() {
    return getFieldCount(LogSearchConstants.SOLR_COMPONENT);
  }

  public CountDataListResponse getHostsCount() {
    return getFieldCount(LogSearchConstants.SOLR_HOST);
  }

  public List<NodeData> buidTreeData(List<PivotField> pivotFields,
                                     List<PivotField> pivotFieldHost, SolrQuery query,
                                     String firstPriority, String secondPriority) {
    List<NodeData> extensionTree = new ArrayList<>();
    String hostQuery = null;
    if (pivotFields != null) {
      // For Host
      for (PivotField pivotHost : pivotFields) {
        if (pivotHost != null) {
          NodeData hostNode = new NodeData();
          String name = (pivotHost.getValue() == null ? "" : ""+ pivotHost.getValue());
          String value = "" + pivotHost.getCount();
          if(!StringUtils.isBlank(name)){
            hostNode.setName(name);
          }
          if(!StringUtils.isBlank(value)){
            hostNode.setValue(value);
          }
          if(!StringUtils.isBlank(firstPriority)){
            hostNode.setType(firstPriority);
          }

          hostNode.setParent(true);
          hostNode.setRoot(true);
          PivotField hostPivot = null;
          for (PivotField searchHost : pivotFieldHost) {
            if (!StringUtils.isBlank(hostNode.getName())
                && hostNode.getName().equals(searchHost.getValue())) {
              hostPivot = searchHost;
              break;
            }
          }
          List<PivotField> pivotLevelHost = hostPivot.getPivot();
          if (pivotLevelHost != null) {
            Collection<NameValueData> logLevelCount = new ArrayList<>();
            for (PivotField pivotLevel : pivotLevelHost) {
              if (pivotLevel != null) {
                NameValueData vnameValue = new NameValueData();
                String levelName = (pivotLevel.getValue() == null ? "" : ""
                    + pivotLevel.getValue());
                vnameValue.setName(levelName.toUpperCase());
                vnameValue.setValue("" + pivotLevel.getCount());
                logLevelCount.add(vnameValue);
              }
            }
            hostNode.setLogLevelCount(logLevelCount);
          }

          query.addFilterQuery(hostQuery);
          List<PivotField> pivotComponents = pivotHost.getPivot();
          // For Components
          if (pivotComponents != null) {
            Collection<NodeData> componentNodes = new ArrayList<>();
            for (PivotField pivotComp : pivotComponents) {
              if (pivotComp != null) {
                NodeData compNode = new NodeData();
                String compName = (pivotComp.getValue() == null ? "" : ""
                    + pivotComp.getValue());
                compNode.setName(compName);
                if (!StringUtils.isBlank(secondPriority)) {
                  compNode.setType(secondPriority);
                }
                compNode.setValue("" + pivotComp.getCount());
                compNode.setParent(false);
                compNode.setRoot(false);
                List<PivotField> pivotLevels = pivotComp.getPivot();
                if (pivotLevels != null) {
                  Collection<NameValueData> logLevelCount = new ArrayList<>();
                  for (PivotField pivotLevel : pivotLevels) {
                    if (pivotLevel != null) {
                      NameValueData vnameValue = new NameValueData();
                      String compLevel = pivotLevel.getValue() == null ? ""
                          : "" + pivotLevel.getValue();
                      vnameValue.setName((compLevel).toUpperCase());

                      vnameValue.setValue("" + pivotLevel.getCount());
                      logLevelCount.add(vnameValue);
                    }
                  }
                  compNode.setLogLevelCount(logLevelCount);
                }
                componentNodes.add(compNode);
              }}
            hostNode.setChilds(componentNodes);
          }
          extensionTree.add(hostNode);
        }}
    }

    return extensionTree;
  }

  public NodeListResponse getTreeExtension(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    solrQuery.setParam("event", "/getTreeExtension");

    if (searchCriteria.getSortBy() == null) {
      searchCriteria.setSortBy(LogSearchConstants.SOLR_HOST);
      searchCriteria.setSortType(SolrQuery.ORDER.asc.toString());
    }
    queryGenerator.setFilterFacetSort(solrQuery, searchCriteria);
    String hostName = ""
      + ((searchCriteria.getParamValue("hostName") == null) ? ""
      : searchCriteria.getParamValue("hostName"));
    if (!StringUtils.isBlank(hostName)){
      solrQuery.addFilterQuery(LogSearchConstants.SOLR_HOST + ":*"
        + hostName + "*");
    }
    String firstHirarchy = "host,type,level";
    String secondHirarchy = "host,level";
    NodeListResponse list = new NodeListResponse();
    try {

      SolrUtil.setFacetPivot(solrQuery, 1, firstHirarchy,
        secondHirarchy);

      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      List<List<PivotField>> listFirstHirarchicalPivotFields = new ArrayList<List<PivotField>>();
      NamedList<List<PivotField>> firstNamedList = response
        .getFacetPivot();
      if (firstNamedList != null) {
        listFirstHirarchicalPivotFields = firstNamedList
          .getAll(firstHirarchy);
      }
      List<List<PivotField>> listSecondHirarchicalPivotFields = new ArrayList<List<PivotField>>();
      NamedList<List<PivotField>> secondNamedList = response
        .getFacetPivot();
      if (secondNamedList != null) {
        listSecondHirarchicalPivotFields = secondNamedList
          .getAll(secondHirarchy);
      }
      List<PivotField> firstHirarchicalPivotFields = new ArrayList<PivotField>();
      List<PivotField> secondHirarchicalPivotFields = new ArrayList<PivotField>();
      if (!listFirstHirarchicalPivotFields.isEmpty()) {
        firstHirarchicalPivotFields = listFirstHirarchicalPivotFields
          .get(0);
      }
      if (!listSecondHirarchicalPivotFields.isEmpty()) {
        secondHirarchicalPivotFields = listSecondHirarchicalPivotFields
          .get(0);
      }
      List<NodeData> dataList = buidTreeData(firstHirarchicalPivotFields,
        secondHirarchicalPivotFields, solrQuery,
        LogSearchConstants.HOST, LogSearchConstants.COMPONENT);

      list.setvNodeList(dataList);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }

    return list;
  }

  public NodeListResponse getHostListByComponent(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    solrQuery.setParam("event", "/service/hosts/components");

    NodeListResponse list = new NodeListResponse();
    if (searchCriteria.getSortBy() == null) {
      searchCriteria.setSortBy(LogSearchConstants.SOLR_HOST);
      searchCriteria.setSortType(SolrQuery.ORDER.asc.toString());
    }
    queryGenerator.setFilterFacetSort(solrQuery, searchCriteria);
    String componentName = ""
      + ((searchCriteria.getParamValue("componentName") == null) ? ""
      : searchCriteria.getParamValue("componentName"));
    if (!StringUtils.isBlank(componentName)){
      solrQuery.addFilterQuery(LogSearchConstants.SOLR_COMPONENT + ":"
        + componentName);
    } else {
      return list;
    }

    String firstHirarchy = "type,host,level";
    String secondHirarchy = "type,level";

    try {
      SolrUtil.setFacetPivot(solrQuery, 1, firstHirarchy,
        secondHirarchy);
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      List<List<PivotField>> firstHirarchicalPivotFields = null;
      List<List<PivotField>> secondHirarchicalPivotFields = null;
      NamedList<List<PivotField>> firstNamedList = response
        .getFacetPivot();
      if (firstNamedList != null) {
        firstHirarchicalPivotFields = firstNamedList
          .getAll(firstHirarchy);
        secondHirarchicalPivotFields = firstNamedList
          .getAll(secondHirarchy);
      }

      if (firstHirarchicalPivotFields == null
        || secondHirarchicalPivotFields == null) {
        return list;
      }

      List<NodeData> dataList = buidTreeData(
        firstHirarchicalPivotFields.get(0),
        secondHirarchicalPivotFields.get(0), solrQuery,
        LogSearchConstants.COMPONENT, LogSearchConstants.HOST);
      if(dataList == null){
        return list;
      }

      list.setvNodeList(dataList);
      return list;
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public NameValueDataListResponse getLogsLevelCount(SearchCriteria sc) {
    NameValueDataListResponse nameValueList = new NameValueDataListResponse();
    SolrQuery query = queryGenerator.commonServiceFilterQuery(sc);
    query.setParam("event", "/service/logs/levels/counts/namevalues");
    List<NameValueData> logsCounts = getLogLevelFacets(query);
    nameValueList.setvNameValues(logsCounts);

    return nameValueList;
  }

  public List<NameValueData> getLogLevelFacets(SolrQuery query) {
    String defalutValue = "0";
    HashMap<String, String> map = new HashMap<String, String>();
    List<NameValueData> logsCounts = new ArrayList<>();
    try {
      SolrUtil.setFacetField(query, LogSearchConstants.SOLR_LEVEL);
      List<Count> logLevelCounts = getFacetCounts(query,
          LogSearchConstants.SOLR_LEVEL);
      if (logLevelCounts == null) {
        return logsCounts;
      }
      for (Count count : logLevelCounts) {
        map.put(count.getName().toUpperCase(), "" + count.getCount());
      }
      for (String level : LogSearchConstants.SUPPORTED_LOG_LEVEL) {
        NameValueData nameValue = new NameValueData();
        String value = map.get(level);
        if (StringUtils.isBlank(value)) {
          value = defalutValue;
        }
        nameValue.setName(level);
        nameValue.setValue(value);
        logsCounts.add(nameValue);
      }
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + query, e);
    }
    return logsCounts;
  }

  // Get Facet Count According to FacetFeild
  public List<Count> getFacetCounts(SolrQuery solrQuery, String facetField)
    throws SolrServerException, IOException, SolrException {
    List<Count> list = new ArrayList<FacetField.Count>();

    QueryResponse response = serviceLogsSolrDao.process(solrQuery);
    if(response == null){
      return list;
    }

    FacetField field = response.getFacetField(facetField);
    if (field == null) {
      return list;
    }
    list = field.getValues();


    return list;
  }

  public LogListResponse getPageByKeyword(SearchCriteria searchCriteria)
    throws SolrServerException {
    String defaultChoice = "0";

    String key = (String) searchCriteria.getParamValue("keyword");
    if(StringUtils.isBlank(key)){
      throw RESTErrorUtil.createRESTException("Keyword was not given",
          MessageEnums.DATA_NOT_FOUND);
    }

    String keyword = SolrUtil.escapeForStandardTokenizer(key);

    if(keyword.startsWith("\"") && keyword.endsWith("\"")){
      keyword = keyword.substring(1);
      keyword = keyword.substring(0, keyword.length()-1);
    }
    keyword = "*" + keyword + "*";


    String keyType = (String) searchCriteria.getParamValue("keywordType");
    QueryResponse queryResponse = null;

    if (!defaultChoice.equals(keyType)) {
      try {
        int currentPageNumber = searchCriteria.getPage();
        int maxRows = searchCriteria.getMaxRows();
        String nextPageLogID = "";

        int lastLogIndexNumber = ((currentPageNumber + 1)
          * maxRows);
        String nextPageLogTime = "";


        // Next Page Start Time Calculation
        SolrQuery nextPageLogTimeQuery = queryGenerator
          .commonServiceFilterQuery(searchCriteria);
        nextPageLogTimeQuery.remove("start");
        nextPageLogTimeQuery.remove("rows");
        nextPageLogTimeQuery.setStart(lastLogIndexNumber);
        nextPageLogTimeQuery.setRows(1);

        queryResponse = serviceLogsSolrDao.process(
            nextPageLogTimeQuery);
        if(queryResponse == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList docList = queryResponse.getResults();
        if(docList ==null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocument solrDoc = docList.get(0);

        Date logDate = (Date) solrDoc.get(LogSearchConstants.LOGTIME);
        if(logDate == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        nextPageLogTime = DateUtil
          .convertDateWithMillisecondsToSolrDate(logDate);
        nextPageLogID = ""
          + solrDoc.get(LogSearchConstants.ID);

        if (StringUtils.isBlank(nextPageLogID)){
          nextPageLogID = "0";
        }

        String filterQueryListIds = "";
        // Remove the same Time Ids
        SolrQuery listRemoveIds = queryGenerator
          .commonServiceFilterQuery(searchCriteria);
        listRemoveIds.remove("start");
        listRemoveIds.remove("rows");
        queryGenerator.setSingleIncludeFilter(listRemoveIds,
          LogSearchConstants.LOGTIME, "\"" + nextPageLogTime + "\"");
        queryGenerator.setSingleExcludeFilter(listRemoveIds,
          LogSearchConstants.ID, nextPageLogID);
        SolrUtil.setFl(listRemoveIds, LogSearchConstants.ID);
        queryResponse = serviceLogsSolrDao.process(
            listRemoveIds);
        if(queryResponse == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList docListIds = queryResponse.getResults();
        if(docListIds ==null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        boolean isFirst = true;
        for (SolrDocument solrDocId :  docListIds ) {
          String id = "" + solrDocId.get(LogSearchConstants.ID);
          if (isFirst) {
            filterQueryListIds += LogSearchConstants.MINUS_OPERATOR + LogSearchConstants.ID + ":" + id;
            isFirst = false;
          } else {
            filterQueryListIds += " "+CONDITION.AND+" " + LogSearchConstants.MINUS_OPERATOR + LogSearchConstants.ID + ":" + id;
          }
        }

        // Keyword Sequence Number Calculation
        String endTime = (String) searchCriteria.getParamValue("to");
        String startTime = (String) searchCriteria
          .getParamValue("from");
        SolrQuery logTimeThroughRangeQuery = queryGenerator
          .commonServiceFilterQuery(searchCriteria);
        logTimeThroughRangeQuery.remove("start");
        logTimeThroughRangeQuery.remove("rows");
        logTimeThroughRangeQuery.setRows(1);
        if (!StringUtils.isBlank(filterQueryListIds)){
          logTimeThroughRangeQuery.setFilterQueries(filterQueryListIds);
        }

        String sortByType = searchCriteria.getSortType();

        if (!StringUtils.isBlank(sortByType) && sortByType
          .equalsIgnoreCase(LogSearchConstants.ASCENDING_ORDER)) {

          queryGenerator.setSingleRangeFilter(logTimeThroughRangeQuery,
            LogSearchConstants.LOGTIME, nextPageLogTime,
            endTime);
          logTimeThroughRangeQuery.set(LogSearchConstants.SORT,
            LogSearchConstants.LOGTIME + " "
              + LogSearchConstants.ASCENDING_ORDER);

        } else {

          queryGenerator.setSingleRangeFilter(logTimeThroughRangeQuery,
            LogSearchConstants.LOGTIME, startTime,
            nextPageLogTime);
          logTimeThroughRangeQuery.set(LogSearchConstants.SORT,
            LogSearchConstants.LOGTIME + " "
              + LogSearchConstants.DESCENDING_ORDER);
        }
        queryGenerator.setSingleIncludeFilter(logTimeThroughRangeQuery,
          LogSearchConstants.SOLR_KEY_LOG_MESSAGE, keyword);


        queryResponse = serviceLogsSolrDao.process(
            logTimeThroughRangeQuery);
        if(queryResponse == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList documentList = queryResponse.getResults();
        if(documentList ==null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocument solrDocument = new SolrDocument();
        if (!documentList.isEmpty()){
          solrDocument = documentList.get(0);
        }

        Date keywordLogDate = (Date) solrDocument.get(LogSearchConstants.LOGTIME);
        if(keywordLogDate == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        String originalKeywordDate = DateUtil
          .convertDateWithMillisecondsToSolrDate(keywordLogDate);
        String keywordId = "" + solrDocument.get(LogSearchConstants.ID);

        // Getting Range Count from StartTime To Keyword Log Time
        SolrQuery rangeLogQuery = nextPageLogTimeQuery.getCopy();
        rangeLogQuery.remove("start");
        rangeLogQuery.remove("rows");

        if (!StringUtils.isBlank(sortByType) && sortByType
          .equalsIgnoreCase(LogSearchConstants.ASCENDING_ORDER)) {
          keywordLogDate = DateUtils.addMilliseconds(keywordLogDate, 1);
          String keywordDateTime = DateUtil
            .convertDateWithMillisecondsToSolrDate(keywordLogDate);
          queryGenerator.setSingleRangeFilter(rangeLogQuery,
            LogSearchConstants.LOGTIME, startTime,
            keywordDateTime);
        } else {
          keywordLogDate = DateUtils.addMilliseconds(keywordLogDate, -1);
          String keywordDateTime = DateUtil
            .convertDateWithMillisecondsToSolrDate(keywordLogDate);
          queryGenerator.setSingleRangeFilter(rangeLogQuery,
            LogSearchConstants.LOGTIME, keywordDateTime,
            endTime);
        }


        long countNumberLogs = countQuery(rangeLogQuery,serviceLogsSolrDao) - 1;


        //Adding numbers on


        try {
          SolrQuery sameIdQuery = queryGenerator
            .commonServiceFilterQuery(searchCriteria);
          queryGenerator.setSingleIncludeFilter(sameIdQuery,
            LogSearchConstants.LOGTIME, "\"" + originalKeywordDate + "\"");
          SolrUtil.setFl(sameIdQuery, LogSearchConstants.ID);
          SolrDocumentList sameQueryDocList = serviceLogsSolrDao.process(sameIdQuery)
            .getResults();
          for (SolrDocument solrDocumenent : sameQueryDocList) {
            String id = (String) solrDocumenent
              .getFieldValue(LogSearchConstants.ID);
            countNumberLogs++;
           
            if (StringUtils.isBlank(id) && id.equals(keywordId)){
              break;
            }
          }
        } catch (SolrException | SolrServerException | IOException e) {
          logger.error(e);
        }

        int start = (int) ((countNumberLogs / maxRows) * maxRows);
        SolrQuery logIdQuery = nextPageLogTimeQuery.getCopy();
        rangeLogQuery.remove("start");
        rangeLogQuery.remove("rows");
        logIdQuery.setStart(start);
        logIdQuery.setRows(searchCriteria.getMaxRows());
        LogListResponse logResponse = getLogAsPaginationProvided(logIdQuery, serviceLogsSolrDao);
        return logResponse;

      } catch (Exception e) {
        //do nothing
      }

    } else {
      try {
        int currentPageNumber = searchCriteria.getPage();
        int maxRows = searchCriteria.getMaxRows();

        if (currentPageNumber == 0) {
          throw RESTErrorUtil.createRESTException("This is first Page Not",
            MessageEnums.DATA_NOT_FOUND);
        }

        int firstLogCurrentPage = (currentPageNumber * maxRows);
        String lastLogsLogTime = "";

        // Next Page Start Time Calculation
        SolrQuery lastLogTime = queryGenerator
          .commonServiceFilterQuery(searchCriteria);
        lastLogTime.remove("start");
        lastLogTime.remove("rows");

        lastLogTime.setStart(firstLogCurrentPage);
        lastLogTime.setRows(1);

        queryResponse = serviceLogsSolrDao.process(
            lastLogTime);
        if(queryResponse == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList docList = queryResponse.getResults();
        if(docList ==null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        SolrDocument solrDoc = docList.get(0);

        Date logDate = (Date) solrDoc.get(LogSearchConstants.LOGTIME);
        String sortByType = searchCriteria.getSortType();
        lastLogsLogTime = DateUtil
          .convertDateWithMillisecondsToSolrDate(logDate);
        String lastLogsLogId = ""
          + solrDoc.get(LogSearchConstants.ID);


        String filterQueryListIds = "";
        // Remove the same Time Ids
        SolrQuery listRemoveIds = queryGenerator
          .commonServiceFilterQuery(searchCriteria);
        listRemoveIds.remove("start");
        listRemoveIds.remove("rows");
        queryGenerator.setSingleIncludeFilter(listRemoveIds,
          LogSearchConstants.LOGTIME, "\"" + lastLogsLogTime + "\"");
        queryGenerator.setSingleExcludeFilter(listRemoveIds,
          LogSearchConstants.ID, lastLogsLogId);
        SolrUtil.setFl(listRemoveIds, LogSearchConstants.ID);
        queryResponse = serviceLogsSolrDao.process(
            lastLogTime);
        if(queryResponse == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList docListIds = queryResponse.getResults();
        if(docListIds == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        boolean isFirst = true;
        for (SolrDocument solrDocId : docListIds) {
          if (solrDocId != null) {
            String id = "" + solrDocId.get(LogSearchConstants.ID);
            if (isFirst) {
              filterQueryListIds += LogSearchConstants.MINUS_OPERATOR + LogSearchConstants.ID + ":" + id;
              isFirst = false;
            } else {
              filterQueryListIds += " "+CONDITION.AND+" " + LogSearchConstants.MINUS_OPERATOR + LogSearchConstants.ID + ":"
                  + id;
            }
          }
        }


        // Keyword LogTime Calculation
        String endTime = (String) searchCriteria.getParamValue("to");
        String startTime = (String) searchCriteria
          .getParamValue("from");
        SolrQuery logTimeThroughRangeQuery = queryGenerator
          .commonServiceFilterQuery(searchCriteria);
        logTimeThroughRangeQuery.remove("start");
        logTimeThroughRangeQuery.remove("rows");
        logTimeThroughRangeQuery.setRows(1);
        queryGenerator.setSingleExcludeFilter(logTimeThroughRangeQuery,
          LogSearchConstants.ID, lastLogsLogId);
        if (!StringUtils.isBlank(filterQueryListIds)){
          logTimeThroughRangeQuery.setFilterQueries(filterQueryListIds);
        }

        if (!StringUtils.isBlank(sortByType) && sortByType
          .equalsIgnoreCase(LogSearchConstants.ASCENDING_ORDER)) {

          logTimeThroughRangeQuery.remove(LogSearchConstants.SORT);
          logTimeThroughRangeQuery.set(LogSearchConstants.SORT,
            LogSearchConstants.LOGTIME + " "
              + LogSearchConstants.DESCENDING_ORDER);


          queryGenerator.setSingleRangeFilter(
            logTimeThroughRangeQuery,
            LogSearchConstants.LOGTIME, startTime,
            lastLogsLogTime);

        } else {

          logTimeThroughRangeQuery.remove(LogSearchConstants.SORT);
          logTimeThroughRangeQuery.set(LogSearchConstants.SORT,
            LogSearchConstants.LOGTIME + " "
              + LogSearchConstants.ASCENDING_ORDER);


          queryGenerator.setSingleRangeFilter(logTimeThroughRangeQuery,
            LogSearchConstants.LOGTIME, lastLogsLogTime, endTime);
        }
        queryGenerator.setSingleIncludeFilter(logTimeThroughRangeQuery,
          LogSearchConstants.SOLR_KEY_LOG_MESSAGE, keyword);


        queryResponse = serviceLogsSolrDao.process(
            logTimeThroughRangeQuery);
        if(queryResponse == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList documentList = queryResponse.getResults();
        if(documentList == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        SolrDocument solrDocument = new SolrDocument();
        if (!documentList.isEmpty()){
          solrDocument = documentList.get(0);
        }

        Date keywordLogDate = (Date) solrDocument.get(LogSearchConstants.LOGTIME);
        if(keywordLogDate == null){
          throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        String originalKeywordDate = DateUtil
          .convertDateWithMillisecondsToSolrDate(keywordLogDate);
        String keywordId = "" + solrDocument.get(LogSearchConstants.ID);

        // Getting Range Count from StartTime To Keyword Log Time
        SolrQuery rangeLogQuery = lastLogTime.getCopy();
        rangeLogQuery.remove("start");
        rangeLogQuery.remove("rows");

        if (!StringUtils.isBlank(sortByType) && sortByType
          .equalsIgnoreCase(LogSearchConstants.ASCENDING_ORDER)) {
       //   keywordLogDate = DateUtil.addMilliSecondsToDate(keywordLogDate, 1);
          String keywordDateTime = DateUtil
            .convertDateWithMillisecondsToSolrDate(keywordLogDate);
          queryGenerator.setSingleRangeFilter(rangeLogQuery,
            LogSearchConstants.LOGTIME, startTime,
            keywordDateTime);


        } else {
     //     keywordLogDate = DateUtil.addMilliSecondsToDate(keywordLogDate, -1);
          String keywordDateTime = DateUtil
            .convertDateWithMillisecondsToSolrDate(keywordLogDate);
          queryGenerator.setSingleRangeFilter(rangeLogQuery,
            LogSearchConstants.LOGTIME, keywordDateTime,
            endTime);
        }


        long countNumberLogs = countQuery(rangeLogQuery,serviceLogsSolrDao) - 1;

        //Adding numbers on
        try {
          SolrQuery sameIdQuery = queryGenerator
            .commonServiceFilterQuery(searchCriteria);
          queryGenerator.setSingleIncludeFilter(sameIdQuery,
            LogSearchConstants.LOGTIME, "\"" + originalKeywordDate + "\"");
          SolrUtil.setFl(sameIdQuery, LogSearchConstants.ID);
          SolrDocumentList sameQueryDocList = serviceLogsSolrDao.process(sameIdQuery)
            .getResults();
          for (SolrDocument solrDocumenent : sameQueryDocList) {
            if (solrDocumenent != null) {
              String id = (String) solrDocumenent
                  .getFieldValue(LogSearchConstants.ID);
              countNumberLogs++;
              if ( StringUtils.isBlank(id) && id.equals(keywordId)) {
                break;
              }
            }
          }
        } catch (SolrException | SolrServerException | IOException e) {
          logger.error(e);
        }
        int start = (int) ((countNumberLogs / maxRows) * maxRows);

        SolrQuery logIdQuery = lastLogTime.getCopy();
        rangeLogQuery.remove("start");
        rangeLogQuery.remove("rows");
        logIdQuery.setStart(start);
        logIdQuery.setRows(searchCriteria.getMaxRows());
        return getLogAsPaginationProvided(logIdQuery, serviceLogsSolrDao);
      } catch (Exception e) {
        //do nothing
      }

    }
    throw RESTErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
        MessageEnums.ERROR_SYSTEM);
  }

  private LogSearchResponse getPageByLogId(SearchCriteria searchCriteria) {
    LogSearchResponse logResponse = new ServiceLogResponse();
    String endLogTime = (String) searchCriteria.getParamValue("to");
    if(StringUtils.isBlank(endLogTime)){
      return logResponse;
    }
    long startIndex = 0l;

    String logId = (String) searchCriteria.getParamValue("sourceLogId");
    if(StringUtils.isBlank(logId)){
      return logResponse;
    }
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);

    String endTimeMinusOneMilli = "";
    String logTime = "";
    try {

      SolrQuery logTimeByIdQuery = new SolrQuery();
      SolrUtil.setMainQuery(logTimeByIdQuery, null);
      queryGenerator.setSingleIncludeFilter(logTimeByIdQuery,
          LogSearchConstants.ID, logId);
      SolrUtil.setRowCount(solrQuery, 1);

      QueryResponse queryResponse = serviceLogsSolrDao
          .process(logTimeByIdQuery);

      if(queryResponse == null){
        return new ServiceLogResponse();
      }

      SolrDocumentList docList = queryResponse.getResults();
      Date dateOfLogId = null;
      if (docList != null && !docList.isEmpty()) {
        SolrDocument dateLogIdDoc = docList.get(0);
        if(dateLogIdDoc != null){
          dateOfLogId = (Date) dateLogIdDoc.get(LogSearchConstants.LOGTIME);
        }
      }

      if (dateOfLogId != null) {
        logTime = DateUtil.convertDateWithMillisecondsToSolrDate(dateOfLogId);
        Date endDate = DateUtils.addMilliseconds(dateOfLogId, 1);
        endTimeMinusOneMilli = (String) DateUtil
            .convertDateWithMillisecondsToSolrDate(endDate);
      }

    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
    }

    try {
      solrQuery.remove(LogSearchConstants.ID);
      solrQuery.remove(LogSearchConstants.LOGTIME);
      queryGenerator.setSingleRangeFilter(solrQuery,
          LogSearchConstants.LOGTIME, endTimeMinusOneMilli, endLogTime);
      SolrUtil.setRowCount(solrQuery, 0);
      startIndex = countQuery(solrQuery,serviceLogsSolrDao);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
    }

    try {
      SolrQuery sameIdQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
      queryGenerator.setSingleIncludeFilter(sameIdQuery,
          LogSearchConstants.LOGTIME, "\"" + logTime + "\"");
      sameIdQuery.set("fl", LogSearchConstants.ID);

      QueryResponse sameIdResponse = serviceLogsSolrDao.process(sameIdQuery);
      SolrDocumentList docList = sameIdResponse.getResults();

      for (SolrDocument solrDocumenent : docList) {
        String id = (String) solrDocumenent
            .getFieldValue(LogSearchConstants.ID);
        startIndex++;
        if (!StringUtils.isBlank(id)) {
          if (id.equals(logId)) {
            break;
          }
        }
      }

      SolrQuery logIdQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
      logIdQuery.remove("rows");
      logIdQuery.remove("start");
      int start = (int) ((startIndex / searchCriteria.getMaxRows()) * searchCriteria
          .getMaxRows());
      logIdQuery.setStart(start);
      logIdQuery.setRows(searchCriteria.getMaxRows());
      logResponse = getLogAsPaginationProvided(logIdQuery,
          serviceLogsSolrDao);
      return logResponse;
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
    }

    throw RESTErrorUtil.createRESTException("LogId not Found",
        MessageEnums.ERROR_SYSTEM);
  }

  @SuppressWarnings("unchecked")
  public List<NameValueData> getHistogramCounts(SolrQuery solrQuery,
                                             String from, String to, String unit) {
    List<NameValueData> logsCounts = new ArrayList<>();
    try {

      SolrUtil.setFacetRange(solrQuery, LogSearchConstants.LOGTIME,
        from, to, unit);

      List<RangeFacet.Count> logLevelCounts = null;

      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if(response == null){
        return logsCounts;
      }
      @SuppressWarnings("rawtypes")
      List<RangeFacet> rangeFacetList = response.getFacetRanges();
      if (rangeFacetList == null) {
        return logsCounts;

      }

      @SuppressWarnings("rawtypes")
      RangeFacet rangeFacet=rangeFacetList.get(0);
      if (rangeFacet == null) {
        return logsCounts;
      }
      logLevelCounts = rangeFacet.getCounts();

      if(logLevelCounts == null){
        return logsCounts;
      }
      for (RangeFacet.Count logCount : logLevelCounts) {
        NameValueData nameValue = new NameValueData();
        nameValue.setName(logCount.getValue());
        nameValue.setValue("" + logCount.getCount());
        logsCounts.add(nameValue);
      }
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
    }
    return logsCounts;
  }

  public List<Count> getFacetCountsByDate(SolrQuery solrQuery,
                                          String facetField) throws SolrServerException, IOException,
    SolrException {

    QueryResponse response = serviceLogsSolrDao.process(solrQuery);

    FacetField field = response.getFacetDate(facetField);
    return field.getValues();
  }

  @SuppressWarnings("unchecked")
  public BarGraphDataListResponse getHistogramData(SearchCriteria searchCriteria) {
    String deafalutValue = "0";
    BarGraphDataListResponse dataList = new BarGraphDataListResponse();
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    solrQuery.set("event", "/audit/logs/histogram");
    String from = getFrom((String) searchCriteria.getParamValue("from"));
    String to = getTo((String) searchCriteria.getParamValue("to"));
    String unit = getUnit((String) searchCriteria.getParamValue("unit"));

    List<BarGraphData> histogramData = new ArrayList<>();

    String jsonHistogramQuery = queryGenerator
      .buildJSONFacetTermTimeRangeQuery(
        LogSearchConstants.SOLR_LEVEL,
        LogSearchConstants.LOGTIME, from, to, unit).replace(
        "\\", "");

    try {
      SolrUtil.setJSONFacet(solrQuery, jsonHistogramQuery);
      SolrUtil.setRowCount(solrQuery,Integer.parseInt(deafalutValue));
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if (response == null){
        return dataList;
      }
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
        .getResponse().get("facets");

      if (jsonFacetResponse == null
        || jsonFacetResponse.toString().equals("{count=0}")){
        return dataList;
      }

      extractValuesFromBuckets(jsonFacetResponse, "x", "y", histogramData);

      Collection<NameValueData> vNameValues = new ArrayList<NameValueData>();
      List<BarGraphData> graphDatas = new ArrayList<BarGraphData>();
      for (String level : LogSearchConstants.SUPPORTED_LOG_LEVEL) {
        boolean isLevelPresent = false;
        BarGraphData vData1 = null;
        for (BarGraphData vData2 : histogramData) {
          String name = vData2.getName();
          if (level.contains(name)) {
            isLevelPresent = true;
            vData1 = vData2;
            break;
          }
          if (vNameValues.isEmpty()) {
            Collection<NameValueData> vNameValues2 = vData2
              .getDataCount();
            for (NameValueData value : vNameValues2) {
              NameValueData value2 = new NameValueData();
              value2.setValue(deafalutValue);
              value2.setName(value.getName());
              vNameValues.add(value2);
            }
          }
        }
        if (!isLevelPresent) {
          BarGraphData vBarGraphData = new BarGraphData();
          vBarGraphData.setName(level);
          vBarGraphData.setDataCount(vNameValues);
          graphDatas.add(vBarGraphData);
        } else {
          graphDatas.add(vData1);
        }
      }

      dataList.setGraphData(graphDatas);
      return dataList;

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error(e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);

    }
  }

  public void arrangeLevel(String level,
                           List<BarGraphData> histogramDataLocal,
                           List<BarGraphData> histogramData) {
    for (BarGraphData histData : histogramData) {
      if (histData != null && level.equals(histData.getName())) {
        histogramDataLocal.add(histData);
      }
    }
  }

  public String cancelFindRequestByDate(String uniqueId) {
    if (StringUtils.isEmpty(uniqueId)) {
      logger.error("Unique id is Empty");
      throw RESTErrorUtil.createRESTException("Unique id is Empty",
        MessageEnums.DATA_NOT_FOUND);
    }

    if (cancelByDate.remove(uniqueId)) {
      mapUniqueId.remove(uniqueId);
      return "Cancel Request Successfully Procssed ";
    }
    return "Cancel Request Unable to Process";
  }

  public boolean cancelRequest(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      logger.error("Unique id is Empty");
      throw RESTErrorUtil.createRESTException("Unique id is Empty",
        MessageEnums.DATA_NOT_FOUND);
    }
    for (String date : cancelByDate) {
      if (uniqueId.equalsIgnoreCase(date)){
        return false;
      }
    }
    return true;
  }

  public Response exportToTextFile(SearchCriteria searchCriteria) {
    String defaultFormat = "text";
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    String from = (String) searchCriteria.getParamValue("from");
    String to = (String) searchCriteria.getParamValue("to");
    String utcOffset = (String) searchCriteria.getParamValue("utcOffset");
    String format = (String) searchCriteria.getParamValue("format");

    format = defaultFormat.equalsIgnoreCase(format) && format != null ? ".txt"
        : ".json";
    
    if(StringUtils.isBlank(utcOffset)){
      utcOffset = "0";
    }

    if (!DateUtil.isDateValid(from) || !DateUtil.isDateValid(to)) {
      logger.error("Not valid date format. Valid format should be"
          + LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z);
      throw RESTErrorUtil.createRESTException("Not valid date format. Valid format should be"
          + LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z,
          MessageEnums.INVALID_INPUT_DATA);

    } else {
      from = from.replace("T", " ");
      from = from.replace(".", ",");

      to = to.replace("T", " ");
      to = to.replace(".", ",");

      to = DateUtil.addOffsetToDate(to, Long.parseLong(utcOffset),
          "yyyy-MM-dd HH:mm:ss,SSS");
      from = DateUtil.addOffsetToDate(from, Long.parseLong(utcOffset),
          "yyyy-MM-dd HH:mm:ss,SSS");
    }

    String fileName = DateUtil.getCurrentDateInString();
    if (searchCriteria.getParamValue("hostLogFile") != null
      && searchCriteria.getParamValue("compLogFile") != null) {
      fileName = searchCriteria.getParamValue("hostLogFile") + "_"
        + searchCriteria.getParamValue("compLogFile");
    }

    String textToSave = "";
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if (response == null) {
        throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
            .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }
      SolrDocumentList docList = response.getResults();
      if (docList == null) {
        throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
            .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }

      VSummary vsummary = BizUtil.buildSummaryForLogFile(docList);
      vsummary.setFormat(format);
      vsummary.setFrom(from);
      vsummary.setTo(to);

      String includeString = (String) searchCriteria.getParamValue("iMessage");
      if (StringUtils.isBlank(includeString)) {
        includeString = "";
      }

      String include[] = includeString.split(LogSearchConstants.I_E_SEPRATOR);

      for (String inc : include) {
        includeString = includeString + ",\"" + inc + "\"";
      }
      includeString = includeString.replaceFirst(",", "");
      if (!StringUtils.isBlank(includeString)) {
        vsummary.setIncludeString(includeString);
      }

      String excludeString = null;
      boolean isNormalExcluded = false;

      excludeString = (String) searchCriteria.getParamValue("eMessage");
      if (StringUtils.isBlank(excludeString)) {
        excludeString = "";
      }

      String exclude[] = excludeString.split(LogSearchConstants.I_E_SEPRATOR);
      for (String exc : exclude) {
        excludeString = excludeString + ",\"" + exc + "\"";
      }

      excludeString = excludeString.replaceFirst(",", "");
      if (!StringUtils.isBlank(excludeString)) {
        vsummary.setExcludeString(excludeString);
        isNormalExcluded = true;
      }

      String globalExcludeString = (String) searchCriteria
          .getParamValue("gEMessage");
      if (StringUtils.isBlank(globalExcludeString)) {
        globalExcludeString = "";
      }

      String globalExclude[] = globalExcludeString
          .split(LogSearchConstants.I_E_SEPRATOR);

      for (String exc : globalExclude) {
        excludeString = excludeString + ",\"" + exc + "\"";
      }

      if (!StringUtils.isBlank(excludeString)) {
        if (!isNormalExcluded) {
          excludeString = excludeString.replaceFirst(",", "");
        }
        vsummary.setExcludeString(excludeString);
      }

      for (SolrDocument solrDoc : docList) {

        Date logTimeDateObj = (Date) solrDoc.get(LogSearchConstants.LOGTIME);
        if(logTimeDateObj != null){
        String logTime = DateUtil.convertSolrDateToNormalDateFormat(
            logTimeDateObj.getTime(), Long.parseLong(utcOffset));
        solrDoc.remove(LogSearchConstants.LOGTIME);
        solrDoc.addField(LogSearchConstants.LOGTIME, logTime);
        }
      }

      if (format.toLowerCase(Locale.ENGLISH).equals(".txt")) {
        textToSave = BizUtil.convertObjectToNormalText(docList);
      } else if (format.toLowerCase(Locale.ENGLISH).equals(".json")) {
        textToSave = convertObjToString(docList);
      } else {
        throw RESTErrorUtil.createRESTException(
            "unsoported format either should be json or text",
            MessageEnums.ERROR_SYSTEM);
      }
      return FileUtil.saveToFile(textToSave, fileName, vsummary);

    } catch (SolrException | SolrServerException | IOException
      | ParseException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public NodeListResponse getComponentListWithLevelCounts(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    solrQuery.setParam("event", "/service/logs/components/levels/counts");

    if (searchCriteria.getSortBy() == null) {
      searchCriteria.setSortBy(LogSearchConstants.SOLR_COMPONENT);
      searchCriteria.setSortType(SolrQuery.ORDER.asc.toString());
    }
    queryGenerator.setFilterFacetSort(solrQuery, searchCriteria);
    String componentLevelHirachy = "type,level";
    NodeListResponse list = new NodeListResponse();
    try {

      SolrUtil.setFacetPivot(solrQuery, 1, componentLevelHirachy);

      QueryResponse response = serviceLogsSolrDao.process(solrQuery);

      List<List<PivotField>> listPivotField = new ArrayList<List<PivotField>>();
      NamedList<List<PivotField>> namedList = response.getFacetPivot();
      if (namedList != null) {
        listPivotField = namedList.getAll(componentLevelHirachy);
      }
      List<PivotField> secondHirarchicalPivotFields = null;
      if (listPivotField == null || listPivotField.isEmpty()) {
        return list;
      } else {
        secondHirarchicalPivotFields = listPivotField.get(0);
      }
      List<NodeData> datatList = new ArrayList<>();
      for (PivotField singlePivotField : secondHirarchicalPivotFields) {
        if (singlePivotField != null) {
          NodeData comp = new NodeData();
          comp.setName("" + singlePivotField.getValue());
          List<PivotField> levelList = singlePivotField.getPivot();
          List<NameValueData> levelCountList = new ArrayList<>();
          comp.setLogLevelCount(levelCountList);
          if(levelList != null){
            for (PivotField levelPivot : levelList) {
              NameValueData level = new NameValueData();
              level.setName(("" + levelPivot.getValue()).toUpperCase());
              level.setValue("" + levelPivot.getCount());
              levelCountList.add(level);
            }
          }
          datatList.add(comp);
        }
      }
      list.setvNodeList(datatList);
      return list;
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e.getMessage() + "SolrQuery"+solrQuery);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public NameValueDataListResponse getExtremeDatesForBundelId(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = new SolrQuery();
    NameValueDataListResponse nameValueList = new NameValueDataListResponse();
    try {
      String bundelId = (String) searchCriteria
        .getParamValue(LogSearchConstants.BUNDLE_ID);
      if(StringUtils.isBlank(bundelId)){
        bundelId = "";
      }

      queryGenerator.setSingleIncludeFilter(solrQuery,
        LogSearchConstants.BUNDLE_ID, bundelId);

      SolrUtil.setMainQuery(solrQuery, null);
      solrQuery.setSort(LogSearchConstants.LOGTIME, SolrQuery.ORDER.asc);
      SolrUtil.setRowCount(solrQuery, 1);

      List<NameValueData> vNameValues = new ArrayList<>();
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);

      if(response == null){
        return nameValueList;
      }

      SolrDocumentList solrDocList = response.getResults();
      if(solrDocList == null){
        return nameValueList;
      }
      for (SolrDocument solrDoc : solrDocList) {

        Date logTimeAsc = (Date) solrDoc
          .getFieldValue(LogSearchConstants.LOGTIME);
        if (logTimeAsc != null) {
          NameValueData nameValue = new NameValueData();
          nameValue.setName("From");
          nameValue.setValue("" + logTimeAsc.getTime());
          vNameValues.add(nameValue);
        }
      }

      solrQuery.clear();
      SolrUtil.setMainQuery(solrQuery, null);
      queryGenerator.setSingleIncludeFilter(solrQuery,
        LogSearchConstants.BUNDLE_ID, bundelId);
      solrQuery.setSort(LogSearchConstants.LOGTIME, SolrQuery.ORDER.desc);
      SolrUtil.setRowCount(solrQuery, 1);

      solrDocList.clear();
      response = serviceLogsSolrDao.process(solrQuery);

      solrDocList = response.getResults();
      for (SolrDocument solrDoc : solrDocList) {
        if (solrDoc != null) {
          Date logTimeDesc = (Date) solrDoc
              .getFieldValue(LogSearchConstants.LOGTIME);

          if (logTimeDesc != null) {
            NameValueData nameValue = new NameValueData();
            nameValue.setName("To");
            nameValue.setValue("" + logTimeDesc.getTime());
            vNameValues.add(nameValue);
          }
        }
      }
      nameValueList.setvNameValues(vNameValues);


    } catch (SolrServerException | SolrException | IOException e) {
      logger.error(e.getMessage() + "SolrQuery"+solrQuery);
      nameValueList = new NameValueDataListResponse();
    }
    return nameValueList;
  }

  public String getServiceLogsFieldsName() {
    List<String> fieldsNames = solrServiceLogConfig.getFields();
    if (fieldsNames.size() > 0) {

      List<String> uiFieldNames = new ArrayList<String>();
      String temp = null;
      for (String field : fieldsNames) {
        temp = solrServiceLogConfig.getSolrAndUiColumns().get(field + LogSearchConstants.SOLR_SUFFIX);
        if (temp == null){
          uiFieldNames.add(field);
        }else{
          uiFieldNames.add(temp);
        }
      }
      return convertObjToString(uiFieldNames);

    }
    throw RESTErrorUtil.createRESTException(
      "No field name found in property file",
      MessageEnums.DATA_NOT_FOUND);

  }

  public String getServiceLogsSchemaFieldsName() {

    List<String> fieldNames = new ArrayList<String>();
    String excludeArray[] = Arrays.copyOf(solrServiceLogConfig.getExcludeColumnList().toArray(),
      solrServiceLogConfig.getExcludeColumnList().size(), String[].class);

    HashMap<String, String> uiFieldColumnMapping = new LinkedHashMap<String, String>();
    ConfigHelper.getSchemaFieldsName(excludeArray, fieldNames,serviceLogsSolrDao);

    for (String fieldName : fieldNames) {
      String uiField = solrServiceLogConfig.getSolrAndUiColumns().get(fieldName + LogSearchConstants.SOLR_SUFFIX);
      if (uiField != null) {
        uiFieldColumnMapping.put(fieldName, uiField);
      } else {
        uiFieldColumnMapping.put(fieldName, fieldName);
      }
    }

    HashMap<String, String> uiFieldColumnMappingSorted = new LinkedHashMap<String, String>();
    uiFieldColumnMappingSorted.put(LogSearchConstants.SOLR_LOG_MESSAGE, LogSearchConstants.SOLR_LOG_MESSAGE);

    Iterator<Entry<String, String>> it = BizUtil
        .sortHashMapByValues(uiFieldColumnMapping).entrySet().iterator();
    while (it.hasNext()) {
      @SuppressWarnings("rawtypes")
      Map.Entry pair = (Map.Entry) it.next();
      uiFieldColumnMappingSorted.put("" + pair.getKey(), "" + pair.getValue());
    }

    return convertObjToString(uiFieldColumnMappingSorted);

  }

  @SuppressWarnings("unchecked")
  public void extractValuesFromBuckets(
    SimpleOrderedMap<Object> jsonFacetResponse, String outerField,
    String innerField, List<BarGraphData> histogramData) {
    NamedList<Object> stack = (NamedList<Object>) jsonFacetResponse
      .get(outerField);
    ArrayList<Object> stackBuckets = (ArrayList<Object>) stack
      .get("buckets");
    for (Object temp : stackBuckets) {
      BarGraphData vBarGraphData = new BarGraphData();

      SimpleOrderedMap<Object> level = (SimpleOrderedMap<Object>) temp;
      String name = ((String) level.getVal(0)).toUpperCase();
      vBarGraphData.setName(name);

      Collection<NameValueData> vNameValues = new ArrayList<NameValueData>();
      vBarGraphData.setDataCount(vNameValues);
      ArrayList<Object> levelBuckets = (ArrayList<Object>) ((NamedList<Object>) level
        .get(innerField)).get("buckets");
      for (Object temp1 : levelBuckets) {
        SimpleOrderedMap<Object> countValue = (SimpleOrderedMap<Object>) temp1;
        String value = DateUtil
          .convertDateWithMillisecondsToSolrDate((Date) countValue
            .getVal(0));

        String count = "" + countValue.getVal(1);
        NameValueData vNameValue = new NameValueData();
        vNameValue.setName(value);
        vNameValue.setValue(count);
        vNameValues.add(vNameValue);
      }
      histogramData.add(vBarGraphData);
    }
  }

  public BarGraphDataListResponse getAnyGraphData(SearchCriteria searchCriteria) {
    searchCriteria.addParam("fieldTime", LogSearchConstants.LOGTIME);
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    BarGraphDataListResponse result = graphDataGenerator.getAnyGraphData(searchCriteria, serviceLogsSolrDao, solrQuery);
    if (result == null) {
      result = new BarGraphDataListResponse();
    }
    return result;

  }

  public ServiceLogResponse getAfterBeforeLogs(SearchCriteria searchCriteria) {
    ServiceLogResponse logResponse = new ServiceLogResponse();
    List<SolrServiceLogData> docList = null;
    String id = (String) searchCriteria
      .getParamValue(LogSearchConstants.ID);
    if (StringUtils.isBlank(id)) {
      return logResponse;

    }
    String maxRows = "";

    maxRows = (String) searchCriteria.getParamValue("numberRows");
    if (StringUtils.isBlank(maxRows)){
      maxRows = ""+maxRows;
    }
    String scrollType = (String) searchCriteria.getParamValue("scrollType");
    if(StringUtils.isBlank(scrollType)){
      scrollType = "";
    }

    String logTime = null;
    String sequenceId = null;
    try {
      SolrQuery solrQuery = new SolrQuery();
      SolrUtil.setMainQuery(solrQuery,
        queryGenerator.buildFilterQuery(LogSearchConstants.ID, id));
      SolrUtil.setRowCount(solrQuery, 1);
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if(response == null){
        return logResponse;
      }
      docList = convertToSolrBeans(response);
      if (docList != null && !docList.isEmpty()) {
        Date date = docList.get(0).getLogTime();
        logTime = DateUtil.convertDateWithMillisecondsToSolrDate(date);
        sequenceId = ""
          + docList.get(0).getSeqNum();
      }
      if (StringUtils.isBlank(logTime)) {
        return logResponse;
      }
    } catch (SolrServerException | SolrException | IOException e) {
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
    if (LogSearchConstants.SCROLL_TYPE_BEFORE.equals(scrollType)) {
      logResponse = whenScrollUp(searchCriteria, logTime,
        sequenceId, maxRows);

      List<ServiceLogData> solrDocList = new ArrayList<>();
      for (ServiceLogData solrDoc : logResponse.getLogList()) {
        solrDocList.add(solrDoc);
      }
      logResponse.setLogList(solrDocList);
        return logResponse;

    } else if (LogSearchConstants.SCROLL_TYPE_AFTER.equals(scrollType)) {
      List<ServiceLogData> solrDocList = new ArrayList<>();
      logResponse = new ServiceLogResponse();
      for (ServiceLogData solrDoc : whenScrollDown(searchCriteria, logTime,
          sequenceId, maxRows).getLogList()) {
        solrDocList.add(solrDoc);
      }
      logResponse.setLogList(solrDocList);
      return logResponse;

    } else {
      logResponse = new ServiceLogResponse();
      List<ServiceLogData> initial = new ArrayList<>();
      List<ServiceLogData> before = whenScrollUp(searchCriteria, logTime,
        sequenceId, maxRows).getLogList();
      List<ServiceLogData> after = whenScrollDown(searchCriteria, logTime,
        sequenceId, maxRows).getLogList();
      if (before != null && !before.isEmpty()) {
        for (ServiceLogData solrDoc : Lists.reverse(before)) {
          initial.add(solrDoc);
        }
      }

      initial.add(docList.get(0));
      if (after != null && !after.isEmpty()) {
        for (ServiceLogData solrDoc : after) {
          initial.add(solrDoc);
        }
      }

      logResponse.setLogList(initial);

      return logResponse;

    }
  }

  private ServiceLogResponse whenScrollUp(SearchCriteria searchCriteria,
                                          String logTime, String sequenceId, String maxRows) {
    SolrQuery solrQuery = new SolrQuery();
    SolrUtil.setMainQuery(solrQuery, null);
    try {
      int seq_num = Integer.parseInt(sequenceId) - 1;
      sequenceId = "" + seq_num;
    } catch (Exception e) {

    }
    queryGenerator.setSingleRangeFilter(
      solrQuery,
      LogSearchConstants.SEQUNCE_ID, "*", sequenceId);

    queryGenerator.applyLogFileFilter(solrQuery, searchCriteria);

    queryGenerator.setSingleRangeFilter(solrQuery,
      LogSearchConstants.LOGTIME, "*", logTime);
    SolrUtil.setRowCount(solrQuery, Integer.parseInt(maxRows));
    String order1 = LogSearchConstants.LOGTIME + " "
      + LogSearchConstants.DESCENDING_ORDER;
    String order2 = LogSearchConstants.SEQUNCE_ID + " "
      + LogSearchConstants.DESCENDING_ORDER;
    List<String> sortOrder = new ArrayList<String>();
    sortOrder.add(order1);
    sortOrder.add(order2);
    searchCriteria.addParam(LogSearchConstants.SORT, sortOrder);
    queryGenerator.setMultipleSortOrder(solrQuery, searchCriteria);

    return (ServiceLogResponse) getLogAsPaginationProvided(solrQuery, serviceLogsSolrDao);
  }

  private ServiceLogResponse whenScrollDown(SearchCriteria searchCriteria,
                                            String logTime, String sequenceId, String maxRows) {
    SolrQuery solrQuery = new SolrQuery();
    SolrUtil.setMainQuery(solrQuery, null);
    queryGenerator.applyLogFileFilter(solrQuery, searchCriteria);

    try {
      int seq_num = Integer.parseInt(sequenceId) + 1;
      sequenceId = "" + seq_num;
    } catch (Exception e) {

    }
    queryGenerator.setSingleRangeFilter(
      solrQuery,
      LogSearchConstants.SEQUNCE_ID, sequenceId, "*");
    queryGenerator.setSingleRangeFilter(solrQuery,
      LogSearchConstants.LOGTIME, logTime, "*");
    SolrUtil.setRowCount(solrQuery, Integer.parseInt(maxRows));

    String order1 = LogSearchConstants.LOGTIME + " "
      + LogSearchConstants.ASCENDING_ORDER;
    String order2 = LogSearchConstants.SEQUNCE_ID + " "
      + LogSearchConstants.ASCENDING_ORDER;
    List<String> sortOrder = new ArrayList<String>();
    sortOrder.add(order1);
    sortOrder.add(order2);
    searchCriteria.addParam(LogSearchConstants.SORT, sortOrder);
    queryGenerator.setMultipleSortOrder(solrQuery, searchCriteria);

    return (ServiceLogResponse) getLogAsPaginationProvided(solrQuery, serviceLogsSolrDao);
  }

  @Scheduled(cron = "${logsearch.solr.warming.cron}")
  public void warmingSolrServer(){
    logger.info("solr warming triggered.");
    SolrQuery solrQuery = new SolrQuery();
    TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");
    GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
    utc.setTimeInMillis(new Date().getTime());
    utc.set(Calendar.HOUR, 0);
    utc.set(Calendar.MINUTE, 0);
    utc.set(Calendar.MILLISECOND, 001);
    utc.set(Calendar.SECOND, 0);
    DateUtil.convertDateWithMillisecondsToSolrDate(utc.getTime());
    String from = DateUtil.convertDateWithMillisecondsToSolrDate(utc.getTime());
    utc.set(Calendar.MILLISECOND, 999);
    utc.set(Calendar.SECOND, 59);
    utc.set(Calendar.MINUTE, 59);
    utc.set(Calendar.HOUR, 23);
    String to = DateUtil.convertDateWithMillisecondsToSolrDate(utc.getTime());
    queryGenerator.setSingleRangeFilter(solrQuery,
        LogSearchConstants.LOGTIME, from,to);
    String level = LogSearchConstants.FATAL+","+LogSearchConstants.ERROR+","+LogSearchConstants.WARN;
    queryGenerator.setFilterClauseWithFieldName(solrQuery, level,
        LogSearchConstants.SOLR_LEVEL, "", QueryGenerationBase.Condition.OR);
    try {
      serviceLogsSolrDao.process(solrQuery);
    } catch (SolrServerException | IOException e) {
      logger.error("Error while warming solr server",e);
    }
  }

  @Override
  protected List<SolrServiceLogData> convertToSolrBeans(QueryResponse response) {
    return response.getBeans(SolrServiceLogData.class);
  }

  @Override
  protected ServiceLogResponse createLogSearchResponse() {
    return new ServiceLogResponse();
  }

  private List<LogData> getLogDataListByFieldType(Class clazz, QueryResponse response, List<Count> fieldList) {
    List<LogData> groupList = getComponentBeans(clazz, response);
    String temp = "";
    for (Count cnt : fieldList) {
      LogData logData = createNewFieldByType(clazz, cnt, temp);
      groupList.add(logData);
    }
    return groupList;
  }

  private <T extends LogData> List<LogData> getComponentBeans(Class<T> clazz, QueryResponse response) {
    if (clazz.isAssignableFrom(SolrHostLogData.class) || clazz.isAssignableFrom(SolrComponentTypeLogData.class)) {
      return (List<LogData>) response.getBeans(clazz);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private <T extends LogData> LogData createNewFieldByType(Class<T> clazz, Count count, String temp) {
    temp = count.getName();
    LogData result = null;
    if (clazz.isAssignableFrom(SolrHostLogData.class)) {
      SolrHostLogData fieldData = new SolrHostLogData();
      fieldData.setHost(temp);
      result = fieldData;
    } else if (clazz.isAssignableFrom(SolrComponentTypeLogData.class)) {
      SolrComponentTypeLogData fieldData = new SolrComponentTypeLogData();
      fieldData.setType(temp);
      result = fieldData;
    }
    if (result != null) {
      return result;
    }
    throw new UnsupportedOperationException();
  }

}
