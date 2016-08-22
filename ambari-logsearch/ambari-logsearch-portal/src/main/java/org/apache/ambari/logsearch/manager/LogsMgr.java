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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.dao.ServiceLogsSolrDao;
import org.apache.ambari.logsearch.graph.GraphDataGenerator;
import org.apache.ambari.logsearch.query.QueryGenerationBase;
import org.apache.ambari.logsearch.util.BizUtil;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.FileUtil;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.ambari.logsearch.view.VBarDataList;
import org.apache.ambari.logsearch.view.VBarGraphData;
import org.apache.ambari.logsearch.view.VCount;
import org.apache.ambari.logsearch.view.VCountList;
import org.apache.ambari.logsearch.view.VGraphData;
import org.apache.ambari.logsearch.view.VGraphInfo;
import org.apache.ambari.logsearch.view.VGroupList;
import org.apache.ambari.logsearch.view.VNameValue;
import org.apache.ambari.logsearch.view.VNameValueList;
import org.apache.ambari.logsearch.view.VNode;
import org.apache.ambari.logsearch.view.VNodeList;
import org.apache.ambari.logsearch.view.VSolrLogList;
import org.apache.ambari.logsearch.view.VSummary;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component
public class LogsMgr extends MgrBase {
  private static Logger logger = Logger.getLogger(LogsMgr.class);

  public static List<String> cancelByDate = new CopyOnWriteArrayList<String>();

  public static Map<String, String> mapUniqueId = new ConcurrentHashMap<String, String>();

  public static enum CONDITION {
    OR, AND
  }

  @Autowired
  ServiceLogsSolrDao serviceLogsSolrDao;

  @Autowired
  BizUtil bizUtil;

  @Autowired
  FileUtil fileUtil;


  @Autowired
  GraphDataGenerator graphDataGenerator;


  public String searchLogs(SearchCriteria searchCriteria) {
    String keyword = (String) searchCriteria.getParamValue("keyword");
    String logId = (String) searchCriteria.getParamValue("sourceLogId");
    String lastPage = (String)  searchCriteria.getParamValue("isLastPage");
    Boolean isLastPage = Boolean.parseBoolean(lastPage);

    if (!stringUtil.isEmpty(keyword)) {
      try {
        return getPageByKeyword(searchCriteria);
      } catch (SolrException | SolrServerException e) {
        logger.error("Error while getting keyword=" + keyword, e);
        throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
            .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }
    } else if (!stringUtil.isEmpty(logId)) {
      try {
        return getPageByLogId(searchCriteria);
      } catch (SolrException e) {
        logger.error("Error while getting keyword=" + keyword, e);
        throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
            .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }
    } else if (isLastPage) {
      SolrQuery lastPageQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
      VSolrLogList collection = getLastPage(searchCriteria,LogSearchConstants.LOGTIME,serviceLogsSolrDao,lastPageQuery);
      if(collection == null){
        collection = new VSolrLogList();
      }
      return convertObjToString(collection);
    } else {
      SolrQuery solrQuery = queryGenerator
          .commonServiceFilterQuery(searchCriteria);

      solrQuery.setParam("event", "/service/logs");

      VSolrLogList collection = getLogAsPaginationProvided(solrQuery,
          serviceLogsSolrDao);
      return convertObjToString(collection);
    }
  }

  public String getHosts(SearchCriteria searchCriteria) {
    return getFields(searchCriteria, LogSearchConstants.SOLR_HOST);
  }

  public String getFields(SearchCriteria searchCriteria,String field){

    SolrQuery solrQuery = new SolrQuery();
    VGroupList collection = new VGroupList();
    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.setFacetField(solrQuery,
        field);
    queryGenerator.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if(response == null){
        return convertObjToString(collection);
      }
      FacetField facetField = response
        .getFacetField(field);
      if (facetField == null){
        return convertObjToString(collection);
      }
      List<Count> fieldList = facetField.getValues();
      if (fieldList == null){
        return convertObjToString(collection);
      }
      SolrDocumentList docList = response.getResults();
      if(docList == null){
        return convertObjToString(collection);
      }
      String temp = "";
      for (Count cnt : fieldList) {
        SolrDocument solrDoc = new SolrDocument();
        temp = cnt.getName();
        solrDoc.put(field, temp);
        docList.add(solrDoc);
      }

      collection.setGroupDocuments(docList);
      if(!docList.isEmpty()){
        collection.setStartIndex((int) docList.getStart());
        collection.setTotalCount(docList.getNumFound());
      }
      return convertObjToString(collection);
    } catch (IOException | SolrServerException | SolrException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }

  }

  public String getComponents(SearchCriteria searchCriteria) {
    return getFields(searchCriteria, LogSearchConstants.SOLR_COMPONENT);
  }

  public String getAggregatedInfo(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    String hierarchy = "host,type,level";
    VGraphInfo graphInfo = new VGraphInfo();
    try {
      queryGenerator.setMainQuery(solrQuery, null);
      queryGenerator.setFacetPivot(solrQuery, 1, hierarchy);
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if (response == null) {
        return convertObjToString(graphInfo);
      }

      List<List<PivotField>> hirarchicalPivotField = new ArrayList<List<PivotField>>();
      List<VGraphData> dataList = new ArrayList<VGraphData>();
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

      return convertObjToString(graphInfo);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public List<VGraphData> buidGraphData(List<PivotField> pivotFields) {
    List<VGraphData> logList = new ArrayList<VGraphData>();
    if (pivotFields != null) {
      for (PivotField pivotField : pivotFields) {
        if (pivotField != null) {
          VGraphData logLevel = new VGraphData();
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

  public VCountList getFieldCount(SearchCriteria searchCriteria, String field){
    VCountList collection = new VCountList();
    List<VCount> vCounts = new ArrayList<VCount>();
    SolrQuery solrQuery = new SolrQuery();
    queryGenerator.setMainQuery(solrQuery, null);
    if(field == null){
      return collection;
    }
    queryGenerator.setFacetField(solrQuery, field);
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
          VCount vCount = new VCount();
          vCount.setName(cnt.getName());
          vCount.setCount(cnt.getCount());
          vCounts.add(vCount);
        }
      }

    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }

    collection.setCounts(vCounts);
    return collection;
  }

  public VCountList getLogLevelCount(SearchCriteria searchCriteria) {
    return getFieldCount(searchCriteria, LogSearchConstants.SOLR_LEVEL);
  }

  public VCountList getComponentsCount(SearchCriteria searchCriteria) {
    return getFieldCount(searchCriteria, LogSearchConstants.SOLR_COMPONENT);
  }

  public VCountList getHostsCount(SearchCriteria searchCriteria) {
    return getFieldCount(searchCriteria, LogSearchConstants.SOLR_HOST);
  }

  public List<VNode> buidTreeData(List<PivotField> pivotFields,
                                  List<PivotField> pivotFieldHost, SolrQuery query,
                                  String firstPriority, String secondPriority) {
    List<VNode> extensionTree = new ArrayList<VNode>();
    String hostQuery = null;
    if (pivotFields != null) {
      // For Host
      for (PivotField pivotHost : pivotFields) {
        if (pivotHost != null) {
          VNode hostNode = new VNode();
          String name = (pivotHost.getValue() == null ? "" : ""+ pivotHost.getValue());
          String value = "" + pivotHost.getCount();
          if(!stringUtil.isEmpty(name)){
            hostNode.setName(name);
          }
          if(!stringUtil.isEmpty(value)){
            hostNode.setValue(value);
          }
          if(!stringUtil.isEmpty(firstPriority)){
            hostNode.setType(firstPriority);
          }

          hostNode.setParent(true);
          hostNode.setRoot(true);
          PivotField hostPivot = null;
          for (PivotField searchHost : pivotFieldHost) {
            if (!stringUtil.isEmpty(hostNode.getName())
                && hostNode.getName().equals(searchHost.getValue())) {
              hostPivot = searchHost;
              break;
            }
          }
          List<PivotField> pivotLevelHost = hostPivot.getPivot();
          if (pivotLevelHost != null) {
            Collection<VNameValue> logLevelCount = new ArrayList<VNameValue>();
            for (PivotField pivotLevel : pivotLevelHost) {
              if (pivotLevel != null) {
                VNameValue vnameValue = new VNameValue();
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
            Collection<VNode> componentNodes = new ArrayList<VNode>();
            for (PivotField pivotComp : pivotComponents) {
              if (pivotComp != null) {
                VNode compNode = new VNode();
                String compName = (pivotComp.getValue() == null ? "" : ""
                    + pivotComp.getValue());
                compNode.setName(compName);
                if (!stringUtil.isEmpty(secondPriority)) {
                  compNode.setType(secondPriority);
                }
                compNode.setValue("" + pivotComp.getCount());
                compNode.setParent(false);
                compNode.setRoot(false);
                List<PivotField> pivotLevels = pivotComp.getPivot();
                if (pivotLevels != null) {
                  Collection<VNameValue> logLevelCount = new ArrayList<VNameValue>();
                  for (PivotField pivotLevel : pivotLevels) {
                    if (pivotLevel != null) {
                      VNameValue vnameValue = new VNameValue();
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

  public VNodeList getTreeExtension(SearchCriteria searchCriteria) {
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
    if (!stringUtil.isEmpty(hostName)){
      solrQuery.addFilterQuery(LogSearchConstants.SOLR_HOST + ":*"
        + hostName + "*");
    }
    String firstHirarchy = "host,type,level";
    String secondHirarchy = "host,level";
    VNodeList list = new VNodeList();
    try {

      queryGenerator.setFacetPivot(solrQuery, 1, firstHirarchy,
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
      List<VNode> dataList = buidTreeData(firstHirarchicalPivotFields,
        secondHirarchicalPivotFields, solrQuery,
        LogSearchConstants.HOST, LogSearchConstants.COMPONENT);

      list.setvNodeList(dataList);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }

    return list;
  }

  public String getHostListByComponent(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    solrQuery.setParam("event", "/service/hosts/component");

    VNodeList list = new VNodeList();
    if (searchCriteria.getSortBy() == null) {
      searchCriteria.setSortBy(LogSearchConstants.SOLR_HOST);
      searchCriteria.setSortType(SolrQuery.ORDER.asc.toString());
    }
    queryGenerator.setFilterFacetSort(solrQuery, searchCriteria);
    String componentName = ""
      + ((searchCriteria.getParamValue("componentName") == null) ? ""
      : searchCriteria.getParamValue("componentName"));
    if (!stringUtil.isEmpty(componentName)){
      solrQuery.addFilterQuery(LogSearchConstants.SOLR_COMPONENT + ":"
        + componentName);
    } else {
      return convertObjToString(list);
    }

    String firstHirarchy = "type,host,level";
    String secondHirarchy = "type,level";

    try {
      queryGenerator.setFacetPivot(solrQuery, 1, firstHirarchy,
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
        return convertObjToString(list);
      }

      List<VNode> dataList = buidTreeData(
        firstHirarchicalPivotFields.get(0),
        secondHirarchicalPivotFields.get(0), solrQuery,
        LogSearchConstants.COMPONENT, LogSearchConstants.HOST);
      if(dataList == null){
        return convertObjToString(list);
      }

      list.setvNodeList(dataList);
      return convertObjToString(list);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public VNameValueList getLogsLevelCount(SearchCriteria sc) {
    VNameValueList nameValueList = new VNameValueList();
    SolrQuery query = queryGenerator.commonServiceFilterQuery(sc);
    query.setParam("event", "/service/logs/levels/counts/namevalues");
    List<VNameValue> logsCounts = getLogLevelFacets(query);
    nameValueList.setVNameValues(logsCounts);

    return nameValueList;
  }

  public List<VNameValue> getLogLevelFacets(SolrQuery query) {
    String defalutValue = "0";
    HashMap<String, String> map = new HashMap<String, String>();
    List<VNameValue> logsCounts = new ArrayList<VNameValue>();
    try {
      queryGenerator.setFacetField(query, LogSearchConstants.SOLR_LEVEL);
      List<Count> logLevelCounts = getFacetCounts(query,
          LogSearchConstants.SOLR_LEVEL);
      if (logLevelCounts == null) {
        return logsCounts;
      }
      for (Count count : logLevelCounts) {
        map.put(count.getName().toUpperCase(), "" + count.getCount());
      }
      for (String level : LogSearchConstants.SUPPORTED_LOG_LEVEL) {
        VNameValue nameValue = new VNameValue();
        String value = map.get(level);
        if (stringUtil.isEmpty(value)) {
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

  public String getPageByKeyword(SearchCriteria searchCriteria)
    throws SolrServerException {
    String defaultChoice = "0";

    String key = (String) searchCriteria.getParamValue("keyword");
    if(stringUtil.isEmpty(key)){
      throw restErrorUtil.createRESTException("Keyword was not given",
          MessageEnums.DATA_NOT_FOUND);
    }

    String keyword = solrUtil.escapeForStandardTokenizer(key);

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
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList docList = queryResponse.getResults();
        if(docList ==null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocument solrDoc = docList.get(0);

        Date logDate = (Date) solrDoc.get(LogSearchConstants.LOGTIME);
        if(logDate == null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        nextPageLogTime = dateUtil
          .convertDateWithMillisecondsToSolrDate(logDate);
        nextPageLogID = ""
          + solrDoc.get(LogSearchConstants.ID);

        if (stringUtil.isEmpty(nextPageLogID)){
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
        queryGenerator.setFl(listRemoveIds, LogSearchConstants.ID);
        queryResponse = serviceLogsSolrDao.process(
            listRemoveIds);
        if(queryResponse == null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList docListIds = queryResponse.getResults();
        if(docListIds ==null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
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
        if (!stringUtil.isEmpty(filterQueryListIds)){
          logTimeThroughRangeQuery.setFilterQueries(filterQueryListIds);
        }

        String sortByType = searchCriteria.getSortType();

        if (!stringUtil.isEmpty(sortByType) && sortByType
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
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList documentList = queryResponse.getResults();
        if(documentList ==null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocument solrDocument = new SolrDocument();
        if (!documentList.isEmpty()){
          solrDocument = documentList.get(0);
        }

        Date keywordLogDate = (Date) solrDocument.get(LogSearchConstants.LOGTIME);
        if(keywordLogDate == null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        String originalKeywordDate = dateUtil
          .convertDateWithMillisecondsToSolrDate(keywordLogDate);
        String keywordId = "" + solrDocument.get(LogSearchConstants.ID);

        // Getting Range Count from StartTime To Keyword Log Time
        SolrQuery rangeLogQuery = nextPageLogTimeQuery.getCopy();
        rangeLogQuery.remove("start");
        rangeLogQuery.remove("rows");

        if (!stringUtil.isEmpty(sortByType) && sortByType
          .equalsIgnoreCase(LogSearchConstants.ASCENDING_ORDER)) {
          keywordLogDate = dateUtil.addMilliSecondsToDate(keywordLogDate, 1);
          String keywordDateTime = dateUtil
            .convertDateWithMillisecondsToSolrDate(keywordLogDate);
          queryGenerator.setSingleRangeFilter(rangeLogQuery,
            LogSearchConstants.LOGTIME, startTime,
            keywordDateTime);
        } else {
          keywordLogDate = dateUtil.addMilliSecondsToDate(keywordLogDate, -1);
          String keywordDateTime = dateUtil
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
          queryGenerator.setFl(sameIdQuery, LogSearchConstants.ID);
          SolrDocumentList sameQueryDocList = serviceLogsSolrDao.process(sameIdQuery)
            .getResults();
          for (SolrDocument solrDocumenent : sameQueryDocList) {
            String id = (String) solrDocumenent
              .getFieldValue(LogSearchConstants.ID);
            countNumberLogs++;

            if (stringUtil.isEmpty(id) && id.equals(keywordId)){
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
        VSolrLogList vSolrLogList = getLogAsPaginationProvided(logIdQuery, serviceLogsSolrDao);
        return convertObjToString(vSolrLogList);

      } catch (Exception e) {
        //do nothing
      }

    } else {
      try {
        int currentPageNumber = searchCriteria.getPage();
        int maxRows = searchCriteria.getMaxRows();

        if (currentPageNumber == 0) {
          throw restErrorUtil.createRESTException("This is first Page Not",
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
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList docList = queryResponse.getResults();
        if(docList ==null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        SolrDocument solrDoc = docList.get(0);

        Date logDate = (Date) solrDoc.get(LogSearchConstants.LOGTIME);
        String sortByType = searchCriteria.getSortType();
        lastLogsLogTime = dateUtil
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
        queryGenerator.setFl(listRemoveIds, LogSearchConstants.ID);
        queryResponse = serviceLogsSolrDao.process(
            lastLogTime);
        if(queryResponse == null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList docListIds = queryResponse.getResults();
        if(docListIds == null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
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
        if (!stringUtil.isEmpty(filterQueryListIds)){
          logTimeThroughRangeQuery.setFilterQueries(filterQueryListIds);
        }

        if (!stringUtil.isEmpty(sortByType) && sortByType
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
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }

        SolrDocumentList documentList = queryResponse.getResults();
        if(documentList == null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        SolrDocument solrDocument = new SolrDocument();
        if (!documentList.isEmpty()){
          solrDocument = documentList.get(0);
        }

        Date keywordLogDate = (Date) solrDocument.get(LogSearchConstants.LOGTIME);
        if(keywordLogDate == null){
          throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
              MessageEnums.ERROR_SYSTEM);
        }
        String originalKeywordDate = dateUtil
          .convertDateWithMillisecondsToSolrDate(keywordLogDate);
        String keywordId = "" + solrDocument.get(LogSearchConstants.ID);

        // Getting Range Count from StartTime To Keyword Log Time
        SolrQuery rangeLogQuery = lastLogTime.getCopy();
        rangeLogQuery.remove("start");
        rangeLogQuery.remove("rows");

        if (!stringUtil.isEmpty(sortByType) && sortByType
          .equalsIgnoreCase(LogSearchConstants.ASCENDING_ORDER)) {
       //   keywordLogDate = dateUtil.addMilliSecondsToDate(keywordLogDate, 1);
          String keywordDateTime = dateUtil
            .convertDateWithMillisecondsToSolrDate(keywordLogDate);
          queryGenerator.setSingleRangeFilter(rangeLogQuery,
            LogSearchConstants.LOGTIME, startTime,
            keywordDateTime);


        } else {
     //     keywordLogDate = dateUtil.addMilliSecondsToDate(keywordLogDate, -1);
          String keywordDateTime = dateUtil
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
          queryGenerator.setFl(sameIdQuery, LogSearchConstants.ID);
          SolrDocumentList sameQueryDocList = serviceLogsSolrDao.process(sameIdQuery)
            .getResults();
          for (SolrDocument solrDocumenent : sameQueryDocList) {
            if (solrDocumenent != null) {
              String id = (String) solrDocumenent
                  .getFieldValue(LogSearchConstants.ID);
              countNumberLogs++;
              if ( stringUtil.isEmpty(id) && id.equals(keywordId)) {
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
        VSolrLogList vSolrLogList = getLogAsPaginationProvided(logIdQuery, serviceLogsSolrDao);
        return convertObjToString(vSolrLogList);
      } catch (Exception e) {
        //do nothing
      }

    }
    throw restErrorUtil.createRESTException("The keyword "+"\""+key+"\""+" was not found",
        MessageEnums.ERROR_SYSTEM);
  }

  private String getPageByLogId(SearchCriteria searchCriteria) {
    VSolrLogList vSolrLogList = new VSolrLogList();
    String endLogTime = (String) searchCriteria.getParamValue("to");
    if(stringUtil.isEmpty(endLogTime)){
      return convertObjToString(vSolrLogList);
    }
    long startIndex = 0l;

    String logId = (String) searchCriteria.getParamValue("sourceLogId");
    if(stringUtil.isEmpty(logId)){
      return convertObjToString(vSolrLogList);
    }
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);

    String endTimeMinusOneMilli = "";
    String logTime = "";
    try {

      SolrQuery logTimeByIdQuery = new SolrQuery();
      queryGenerator.setMainQuery(logTimeByIdQuery, null);
      queryGenerator.setSingleIncludeFilter(logTimeByIdQuery,
          LogSearchConstants.ID, logId);
      queryGenerator.setRowCount(solrQuery, 1);

      QueryResponse queryResponse = serviceLogsSolrDao
          .process(logTimeByIdQuery);

      if(queryResponse == null){
        return convertObjToString(new VSolrLogList());
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
        logTime = dateUtil.convertDateWithMillisecondsToSolrDate(dateOfLogId);
        Date endDate = dateUtil.addMilliSecondsToDate(dateOfLogId, 1);
        endTimeMinusOneMilli = (String) dateUtil
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
      queryGenerator.setRowCount(solrQuery, 0);
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
        if (!stringUtil.isEmpty(id)) {
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
      vSolrLogList = getLogAsPaginationProvided(logIdQuery,
          serviceLogsSolrDao);
      return convertObjToString(vSolrLogList);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
    }

    throw restErrorUtil.createRESTException("LogId not Found",
        MessageEnums.ERROR_SYSTEM);
  }

  @SuppressWarnings("unchecked")
  public List<VNameValue> getHistogramCounts(SolrQuery solrQuery,
                                             String from, String to, String unit) {
    List<VNameValue> logsCounts = new ArrayList<VNameValue>();
    try {

      queryGenerator.setFacetRange(solrQuery, LogSearchConstants.LOGTIME,
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
        VNameValue nameValue = new VNameValue();
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
  public String getHistogramData(SearchCriteria searchCriteria) {
    String deafalutValue = "0";
    VBarDataList dataList = new VBarDataList();
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    solrQuery.set("event", "/audit/logs/histogram");
    String from = getFrom((String) searchCriteria.getParamValue("from"));
    String to = getTo((String) searchCriteria.getParamValue("to"));
    String unit = getUnit((String) searchCriteria.getParamValue("unit"));

    List<VBarGraphData> histogramData = new ArrayList<VBarGraphData>();

    String jsonHistogramQuery = queryGenerator
      .buildJSONFacetTermTimeRangeQuery(
        LogSearchConstants.SOLR_LEVEL,
        LogSearchConstants.LOGTIME, from, to, unit).replace(
        "\\", "");

    try {
      queryGenerator.setJSONFacet(solrQuery, jsonHistogramQuery);
      queryGenerator.setRowCount(solrQuery,Integer.parseInt(deafalutValue));
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if (response == null){
        return convertObjToString(dataList);
      }
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
        .getResponse().get("facets");

      if (jsonFacetResponse == null
        || jsonFacetResponse.toString().equals("{count=0}")){
        return convertObjToString(dataList);
      }

      extractValuesFromBuckets(jsonFacetResponse, "x", "y", histogramData);

      Collection<VNameValue> vNameValues = new ArrayList<VNameValue>();
      List<VBarGraphData> graphDatas = new ArrayList<VBarGraphData>();
      for (String level : LogSearchConstants.SUPPORTED_LOG_LEVEL) {
        boolean isLevelPresent = false;
        VBarGraphData vData1 = null;
        for (VBarGraphData vData2 : histogramData) {
          String name = vData2.getName();
          if (level.contains(name)) {
            isLevelPresent = true;
            vData1 = vData2;
            break;
          }
          if (vNameValues.isEmpty()) {
            Collection<VNameValue> vNameValues2 = vData2
              .getDataCount();
            for (VNameValue value : vNameValues2) {
              VNameValue value2 = new VNameValue();
              value2.setValue(deafalutValue);
              value2.setName(value.getName());
              vNameValues.add(value2);
            }
          }
        }
        if (!isLevelPresent) {
          VBarGraphData vBarGraphData = new VBarGraphData();
          vBarGraphData.setName(level);
          vBarGraphData.setDataCounts(vNameValues);
          graphDatas.add(vBarGraphData);
        } else {
          graphDatas.add(vData1);
        }
      }

      dataList.setGraphData(graphDatas);
      return convertObjToString(dataList);

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);

    }
  }

  public void arrangeLevel(String level,
                           List<VBarGraphData> histogramDataLocal,
                           List<VBarGraphData> histogramData) {
    for (VBarGraphData histData : histogramData) {
      if (histData != null && level.equals(histData.getName())) {
        histogramDataLocal.add(histData);
      }
    }
  }

  public String cancelFindRequestByDate(String uniqueId) {
    if (stringUtil.isEmpty(uniqueId)) {
      logger.error("Unique id is Empty");
      throw restErrorUtil.createRESTException("Unique id is Empty",
        MessageEnums.DATA_NOT_FOUND);
    }

    if (cancelByDate.remove(uniqueId)) {
      mapUniqueId.remove(uniqueId);
      return "Cancel Request Successfully Procssed ";
    }
    return "Cancel Request Unable to Process";
  }

  public boolean cancelRequest(String uniqueId) {
    if (stringUtil.isEmpty(uniqueId)) {
      logger.error("Unique id is Empty");
      throw restErrorUtil.createRESTException("Unique id is Empty",
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

    if(stringUtil.isEmpty(utcOffset)){
      utcOffset = "0";
    }

    if (!dateUtil.isDateValid(from) || !dateUtil.isDateValid(to)) {
      logger.error("Not valid date format. Valid format should be"
          + LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z);
      throw restErrorUtil.createRESTException("Not valid date format. Valid format should be"
          + LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z,
          MessageEnums.INVALID_INPUT_DATA);

    } else {
      from = from.replace("T", " ");
      from = from.replace(".", ",");

      to = to.replace("T", " ");
      to = to.replace(".", ",");

      to = dateUtil.addOffsetToDate(to, Long.parseLong(utcOffset),
          "yyyy-MM-dd HH:mm:ss,SSS");
      from = dateUtil.addOffsetToDate(from, Long.parseLong(utcOffset),
          "yyyy-MM-dd HH:mm:ss,SSS");
    }

    String fileName = dateUtil.getCurrentDateInString();
    if (searchCriteria.getParamValue("hostLogFile") != null
      && searchCriteria.getParamValue("compLogFile") != null) {
      fileName = searchCriteria.getParamValue("hostLogFile") + "_"
        + searchCriteria.getParamValue("compLogFile");
    }

    String textToSave = "";
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if (response == null) {
        throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
            .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }
      SolrDocumentList docList = response.getResults();
      if (docList == null) {
        throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
            .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }

      VSummary vsummary = bizUtil.buildSummaryForLogFile(docList);
      vsummary.setFormat(format);
      vsummary.setFrom(from);
      vsummary.setTo(to);

      String includeString = (String) searchCriteria.getParamValue("iMessage");
      if (stringUtil.isEmpty(includeString)) {
        includeString = "";
      }

      String include[] = includeString.split(LogSearchConstants.I_E_SEPRATOR);

      for (String inc : include) {
        includeString = includeString + ",\"" + inc + "\"";
      }
      includeString = includeString.replaceFirst(",", "");
      if (!stringUtil.isEmpty(includeString)) {
        vsummary.setIncludeString(includeString);
      }

      String excludeString = null;
      boolean isNormalExcluded = false;

      excludeString = (String) searchCriteria.getParamValue("eMessage");
      if (stringUtil.isEmpty(excludeString)) {
        excludeString = "";
      }

      String exclude[] = excludeString.split(LogSearchConstants.I_E_SEPRATOR);
      for (String exc : exclude) {
        excludeString = excludeString + ",\"" + exc + "\"";
      }

      excludeString = excludeString.replaceFirst(",", "");
      if (!stringUtil.isEmpty(excludeString)) {
        vsummary.setExcludeString(excludeString);
        isNormalExcluded = true;
      }

      String globalExcludeString = (String) searchCriteria
          .getParamValue("gEMessage");
      if (stringUtil.isEmpty(globalExcludeString)) {
        globalExcludeString = "";
      }

      String globalExclude[] = globalExcludeString
          .split(LogSearchConstants.I_E_SEPRATOR);

      for (String exc : globalExclude) {
        excludeString = excludeString + ",\"" + exc + "\"";
      }

      if (!stringUtil.isEmpty(excludeString)) {
        if (!isNormalExcluded) {
          excludeString = excludeString.replaceFirst(",", "");
        }
        vsummary.setExcludeString(excludeString);
      }

      for (SolrDocument solrDoc : docList) {

        Date logTimeDateObj = (Date) solrDoc.get(LogSearchConstants.LOGTIME);
        if(logTimeDateObj != null){
        String logTime = dateUtil.convertSolrDateToNormalDateFormat(
            logTimeDateObj.getTime(), Long.parseLong(utcOffset));
        solrDoc.remove(LogSearchConstants.LOGTIME);
        solrDoc.addField(LogSearchConstants.LOGTIME, logTime);
        }
      }

      if (format.toLowerCase(Locale.ENGLISH).equals(".txt")) {
        textToSave = bizUtil.convertObjectToNormalText(docList);
      } else if (format.toLowerCase(Locale.ENGLISH).equals(".json")) {
        textToSave = convertObjToString(docList);
      } else {
        throw restErrorUtil.createRESTException(
            "unsoported format either should be json or text",
            MessageEnums.ERROR_SYSTEM);
      }
      return fileUtil.saveToFile(textToSave, fileName, vsummary);

    } catch (SolrException | SolrServerException | IOException
      | ParseException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public String getComponentListWithLevelCounts(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    solrQuery.setParam("event", "/service/logs/components/level/counts");

    if (searchCriteria.getSortBy() == null) {
      searchCriteria.setSortBy(LogSearchConstants.SOLR_COMPONENT);
      searchCriteria.setSortType(SolrQuery.ORDER.asc.toString());
    }
    queryGenerator.setFilterFacetSort(solrQuery, searchCriteria);
    String componentLevelHirachy = "type,level";
    VNodeList list = new VNodeList();
    try {

      queryGenerator.setFacetPivot(solrQuery, 1, componentLevelHirachy);

      QueryResponse response = serviceLogsSolrDao.process(solrQuery);

      List<List<PivotField>> listPivotField = new ArrayList<List<PivotField>>();
      NamedList<List<PivotField>> namedList = response.getFacetPivot();
      if (namedList != null) {
        listPivotField = namedList.getAll(componentLevelHirachy);
      }
      List<PivotField> secondHirarchicalPivotFields = null;
      if (listPivotField == null || listPivotField.isEmpty()) {
        return convertObjToString(list);
      } else {
        secondHirarchicalPivotFields = listPivotField.get(0);
      }
      List<VNode> datatList = new ArrayList<VNode>();
      for (PivotField singlePivotField : secondHirarchicalPivotFields) {
        if (singlePivotField != null) {
          VNode comp = new VNode();
          comp.setName("" + singlePivotField.getValue());
          List<PivotField> levelList = singlePivotField.getPivot();
          List<VNameValue> levelCountList = new ArrayList<VNameValue>();
          comp.setLogLevelCount(levelCountList);
          if(levelList != null){
          for (PivotField levelPivot : levelList) {
		  VNameValue level = new VNameValue();
		  level.setName(("" + levelPivot.getValue()).toUpperCase());
		  level.setValue("" + levelPivot.getCount());
		  levelCountList.add(level);
		}
          }
          datatList.add(comp);
        }
      }
      list.setvNodeList(datatList);
      return convertObjToString(list);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e.getMessage() + "SolrQuery"+solrQuery);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public String getExtremeDatesForBundelId(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = new SolrQuery();
    VNameValueList nameValueList = new VNameValueList();
    try {
      String bundelId = (String) searchCriteria
        .getParamValue(LogSearchConstants.BUNDLE_ID);
      if(stringUtil.isEmpty(bundelId)){
        bundelId = "";
      }

      queryGenerator.setSingleIncludeFilter(solrQuery,
        LogSearchConstants.BUNDLE_ID, bundelId);

      queryGenerator.setMainQuery(solrQuery, null);
      solrQuery.setSort(LogSearchConstants.LOGTIME, SolrQuery.ORDER.asc);
      queryGenerator.setRowCount(solrQuery, 1);

      List<VNameValue> vNameValues = new ArrayList<VNameValue>();
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);

      if(response == null){
        return convertObjToString(nameValueList);
      }

      SolrDocumentList solrDocList = response.getResults();
      if(solrDocList == null){
        return convertObjToString(nameValueList);
      }
      for (SolrDocument solrDoc : solrDocList) {

        Date logTimeAsc = (Date) solrDoc
          .getFieldValue(LogSearchConstants.LOGTIME);
        if (logTimeAsc != null) {
          VNameValue nameValue = new VNameValue();
          nameValue.setName("From");
          nameValue.setValue("" + logTimeAsc.getTime());
          vNameValues.add(nameValue);
        }
      }

      solrQuery.clear();
      queryGenerator.setMainQuery(solrQuery, null);
      queryGenerator.setSingleIncludeFilter(solrQuery,
        LogSearchConstants.BUNDLE_ID, bundelId);
      solrQuery.setSort(LogSearchConstants.LOGTIME, SolrQuery.ORDER.desc);
      queryGenerator.setRowCount(solrQuery, 1);

      solrDocList.clear();
      response = serviceLogsSolrDao.process(solrQuery);

      solrDocList = response.getResults();
      for (SolrDocument solrDoc : solrDocList) {
        if (solrDoc != null) {
          Date logTimeDesc = (Date) solrDoc
              .getFieldValue(LogSearchConstants.LOGTIME);

          if (logTimeDesc != null) {
            VNameValue nameValue = new VNameValue();
            nameValue.setName("To");
            nameValue.setValue("" + logTimeDesc.getTime());
            vNameValues.add(nameValue);
          }
        }
      }
      nameValueList.setVNameValues(vNameValues);


    } catch (SolrServerException | SolrException | IOException e) {
      logger.error(e.getMessage() + "SolrQuery"+solrQuery);
      nameValueList=new VNameValueList();
    }
    return convertObjToString(nameValueList);
  }

  protected VGroupList getSolrGroupList(SolrQuery query)
      throws SolrServerException, IOException, SolrException {
    VGroupList collection = new VGroupList();
    QueryResponse response = serviceLogsSolrDao.process(query);
    if (response == null) {
      return collection;
    }
    SolrDocumentList docList = response.getResults();
    if (docList != null) {
      collection.setGroupDocuments(docList);
      collection.setStartIndex((int) docList.getStart());
      collection.setTotalCount(docList.getNumFound());
    }

    return collection;
  }

  public String getServiceLogsFieldsName() {
    String fieldsNameStrArry[] = PropertiesUtil
      .getPropertyStringList("logsearch.service.logs.fields");
    if (fieldsNameStrArry.length > 0) {

      List<String> uiFieldNames = new ArrayList<String>();
      String temp = null;
      for (String field : fieldsNameStrArry) {
        temp = ConfigUtil.serviceLogsColumnMapping.get(field
            + LogSearchConstants.SOLR_SUFFIX);
        if (temp == null){
          uiFieldNames.add(field);
        }else{
          uiFieldNames.add(temp);
        }
      }
      return convertObjToString(uiFieldNames);

    }
    throw restErrorUtil.createRESTException(
      "No field name found in property file",
      MessageEnums.DATA_NOT_FOUND);

  }

  public String getServiceLogsSchemaFieldsName() {

    List<String> fieldNames = new ArrayList<String>();
    String excludeArray[] = PropertiesUtil
        .getPropertyStringList("logsearch.solr.service.logs.exclude.columnlist");

    HashMap<String, String> uiFieldColumnMapping = new LinkedHashMap<String, String>();
    ConfigUtil.getSchemaFieldsName(excludeArray, fieldNames,serviceLogsSolrDao);

    for (String fieldName : fieldNames) {
      String uiField = ConfigUtil.serviceLogsColumnMapping.get(fieldName
          + LogSearchConstants.SOLR_SUFFIX);
      if (uiField != null) {
        uiFieldColumnMapping.put(fieldName, uiField);
      } else {
        uiFieldColumnMapping.put(fieldName, fieldName);
      }
    }

    HashMap<String, String> uiFieldColumnMappingSorted = new LinkedHashMap<String, String>();
    uiFieldColumnMappingSorted.put(LogSearchConstants.SOLR_LOG_MESSAGE, LogSearchConstants.SOLR_LOG_MESSAGE);

    Iterator<Entry<String, String>> it = bizUtil
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
    String innerField, List<VBarGraphData> histogramData) {
    NamedList<Object> stack = (NamedList<Object>) jsonFacetResponse
      .get(outerField);
    ArrayList<Object> stackBuckets = (ArrayList<Object>) stack
      .get("buckets");
    for (Object temp : stackBuckets) {
      VBarGraphData vBarGraphData = new VBarGraphData();

      SimpleOrderedMap<Object> level = (SimpleOrderedMap<Object>) temp;
      String name = ((String) level.getVal(0)).toUpperCase();
      vBarGraphData.setName(name);

      Collection<VNameValue> vNameValues = new ArrayList<VNameValue>();
      vBarGraphData.setDataCounts(vNameValues);
      ArrayList<Object> levelBuckets = (ArrayList<Object>) ((NamedList<Object>) level
        .get(innerField)).get("buckets");
      for (Object temp1 : levelBuckets) {
        SimpleOrderedMap<Object> countValue = (SimpleOrderedMap<Object>) temp1;
        String value = dateUtil
          .convertDateWithMillisecondsToSolrDate((Date) countValue
            .getVal(0));

        String count = "" + countValue.getVal(1);
        VNameValue vNameValue = new VNameValue();
        vNameValue.setName(value);
        vNameValue.setValue(count);
        vNameValues.add(vNameValue);
      }
      histogramData.add(vBarGraphData);
    }
  }

  public String getAnyGraphData(SearchCriteria searchCriteria) {
    searchCriteria.addParam("fieldTime", LogSearchConstants.LOGTIME);
    SolrQuery solrQuery = queryGenerator.commonServiceFilterQuery(searchCriteria);
    VBarDataList result = graphDataGenerator.getAnyGraphData(searchCriteria,
        serviceLogsSolrDao, solrQuery);
    if (result == null) {
      result = new VBarDataList();
    }
    return convertObjToString(result);

  }

  public String getAfterBeforeLogs(SearchCriteria searchCriteria) {
    VSolrLogList vSolrLogList = new VSolrLogList();
    SolrDocumentList docList = null;
    String id = (String) searchCriteria
      .getParamValue(LogSearchConstants.ID);
    if (stringUtil.isEmpty(id)) {
      return convertObjToString(vSolrLogList);

    }
    String maxRows = "";

    maxRows = (String) searchCriteria.getParamValue("numberRows");
    if (stringUtil.isEmpty(maxRows)){
      maxRows = ""+maxRows;
    }
    String scrollType = (String) searchCriteria.getParamValue("scrollType");
    if(stringUtil.isEmpty(scrollType)){
      scrollType = "";
    }

    String logTime = null;
    String sequenceId = null;
    try {
      SolrQuery solrQuery = new SolrQuery();
      queryGenerator.setMainQuery(solrQuery,
        queryGenerator.buildFilterQuery(LogSearchConstants.ID, id));
      queryGenerator.setRowCount(solrQuery, 1);
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if(response == null){
        return convertObjToString(vSolrLogList);
      }
      docList = response.getResults();
      if (docList != null && !docList.isEmpty()) {
        Date date = (Date) docList.get(0).getFieldValue(
          LogSearchConstants.LOGTIME);
        logTime = dateUtil.convertDateWithMillisecondsToSolrDate(date);
        sequenceId = ""
          + docList.get(0).getFieldValue(
          LogSearchConstants.SEQUNCE_ID);
      }
      if (stringUtil.isEmpty(logTime)) {
        return convertObjToString(vSolrLogList);
      }
    } catch (SolrServerException | SolrException | IOException e) {
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
    if (LogSearchConstants.SCROLL_TYPE_BEFORE.equals(scrollType)) {
      vSolrLogList = whenScrollUp(searchCriteria, logTime,
        sequenceId, maxRows);

      SolrDocumentList solrDocList = new SolrDocumentList();
      for (SolrDocument solrDoc : vSolrLogList.getList()) {
        solrDocList.add(solrDoc);
      }
      vSolrLogList.setSolrDocuments(solrDocList);
        return convertObjToString(vSolrLogList);

    } else if (LogSearchConstants.SCROLL_TYPE_AFTER.equals(scrollType)) {
      SolrDocumentList solrDocList = new SolrDocumentList();
      vSolrLogList = new VSolrLogList();
      for (SolrDocument solrDoc : whenScrollDown(searchCriteria, logTime,
          sequenceId, maxRows).getList()) {
        solrDocList.add(solrDoc);
      }
      vSolrLogList.setSolrDocuments(solrDocList);
      return convertObjToString(vSolrLogList);

    } else {
      vSolrLogList = new VSolrLogList();
      SolrDocumentList initial = new SolrDocumentList();
      SolrDocumentList before = whenScrollUp(searchCriteria, logTime,
        sequenceId, maxRows).getList();
      SolrDocumentList after = whenScrollDown(searchCriteria, logTime,
        sequenceId, maxRows).getList();
			if (before != null && !before.isEmpty()) {
				for (SolrDocument solrDoc : Lists.reverse(before)) {
					initial.add(solrDoc);
				}
			}

      initial.add(docList.get(0));
      if (after != null && !after.isEmpty()){
        for (SolrDocument solrDoc : after) {
          initial.add(solrDoc);
	      }
      }

      vSolrLogList.setSolrDocuments(initial);

        return convertObjToString(vSolrLogList);

    }
  }

  private VSolrLogList whenScrollUp(SearchCriteria searchCriteria,
                                    String logTime, String sequenceId, String maxRows) {
    SolrQuery solrQuery = new SolrQuery();
    queryGenerator.setMainQuery(solrQuery, null);
    /*queryGenerator.setSingleExcludeFilter(solrQuery,
        LogSearchConstants.SEQUNCE_ID, sequenceId);*/
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
    queryGenerator.setRowCount(solrQuery, Integer.parseInt(maxRows));
    String order1 = LogSearchConstants.LOGTIME + " "
      + LogSearchConstants.DESCENDING_ORDER;
    String order2 = LogSearchConstants.SEQUNCE_ID + " "
      + LogSearchConstants.DESCENDING_ORDER;
    List<String> sortOrder = new ArrayList<String>();
    sortOrder.add(order1);
    sortOrder.add(order2);
    searchCriteria.addParam(LogSearchConstants.SORT, sortOrder);
    queryGenerator.setMultipleSortOrder(solrQuery, searchCriteria);

    return getLogAsPaginationProvided(solrQuery, serviceLogsSolrDao);
  }

  private VSolrLogList whenScrollDown(SearchCriteria searchCriteria,
                                      String logTime, String sequenceId, String maxRows) {
    SolrQuery solrQuery = new SolrQuery();
    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.applyLogFileFilter(solrQuery, searchCriteria);

    /*queryGenerator.setSingleExcludeFilter(solrQuery,
        LogSearchConstants.SEQUNCE_ID, sequenceId);*/
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
    queryGenerator.setRowCount(solrQuery, Integer.parseInt(maxRows));

    String order1 = LogSearchConstants.LOGTIME + " "
      + LogSearchConstants.ASCENDING_ORDER;
    String order2 = LogSearchConstants.SEQUNCE_ID + " "
      + LogSearchConstants.ASCENDING_ORDER;
    List<String> sortOrder = new ArrayList<String>();
    sortOrder.add(order1);
    sortOrder.add(order2);
    searchCriteria.addParam(LogSearchConstants.SORT, sortOrder);
    queryGenerator.setMultipleSortOrder(solrQuery, searchCriteria);

    return getLogAsPaginationProvided(solrQuery, serviceLogsSolrDao);
  }

  @Scheduled(cron = "${logsearch.solr.warming.cron}")
  public void warmingSolrServer(){
    logger.info("solr warming triggered.");
    SolrQuery solrQuery = new SolrQuery();
    TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");
    GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
    utc.setTimeInMillis(new Date().getTime());
    utc.set(GregorianCalendar.HOUR, 0);
    utc.set(GregorianCalendar.MINUTE, 0);
    utc.set(GregorianCalendar.MILLISECOND, 001);
    utc.set(GregorianCalendar.SECOND, 0);
    dateUtil.convertDateWithMillisecondsToSolrDate(utc.getTime());
    String from = dateUtil.convertDateWithMillisecondsToSolrDate(utc.getTime());
    utc.set(Calendar.MILLISECOND, 999);
    utc.set(Calendar.SECOND, 59);
    utc.set(Calendar.MINUTE, 59);
    utc.set(Calendar.HOUR, 23);
    String to = dateUtil.convertDateWithMillisecondsToSolrDate(utc.getTime());
    queryGenerator.setSingleRangeFilter(solrQuery,
        LogSearchConstants.LOGTIME, from,to);
    String level = LogSearchConstants.FATAL+","+LogSearchConstants.ERROR+","+LogSearchConstants.WARN;
    queryGenerator.setFilterClauseWithFieldName(solrQuery, level,
        LogSearchConstants.SOLR_LEVEL, "", QueryGenerationBase.CONDITION.OR);
    try {
      serviceLogsSolrDao.process(solrQuery);
    } catch (SolrServerException | IOException e) {
      logger.error("Error while warming solr server",e);
    }
  }


}
