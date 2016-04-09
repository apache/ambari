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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.dao.ServiceLogsSolrDao;
import org.apache.ambari.logsearch.graph.GraphDataGnerator;
import org.apache.ambari.logsearch.query.QueryGeneration;
import org.apache.ambari.logsearch.util.BizUtil;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.DateUtil;
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
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component
public class LogsMgr extends MgrBase {
  private static Logger logger = Logger.getLogger(LogsMgr.class);

  public static List<String> cancelByDate = new CopyOnWriteArrayList<String>();

  public static Map<String, String> mapUniqueId = new ConcurrentHashMap<String, String>();

  @Autowired
  ServiceLogsSolrDao serviceLogsSolrDao;

  @Autowired
  BizUtil bizUtil;

  @Autowired
  QueryGeneration queryGenerator;

  @Autowired
  FileUtil fileUtil;

  @Autowired
  DateUtil dateUtil;


  @Autowired
  GraphDataGnerator graphDataGnerator;


  public String searchLogs(SearchCriteria searchCriteria) {
    String keyword = (String) searchCriteria.getParamValue("keyword");
    if (!stringUtil.isEmpty(keyword))
      try {
        return getPageByKeyword(searchCriteria);
      } catch (SolrException | SolrServerException e) {
        logger.error("Error while getting keyword=" + keyword, e);
      }
    String logId = (String) searchCriteria.getParamValue("sourceLogId");
    if (!stringUtil.isEmpty(logId))
      try {
        return getPageByLogId(searchCriteria);
      } catch (SolrException e) {
        logger.error("Error while getting keyword=" + keyword, e);
      }
    SolrQuery solrQuery = queryGenerator.commonFilterQuery(searchCriteria);

    solrQuery.setParam("event", "/solr/logs_search");
    try {
      VSolrLogList collection = getLogAsPaginationProvided(solrQuery, serviceLogsSolrDao);
      return convertObjToString(collection);
    } catch (SolrException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  public String getHosts(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = new SolrQuery();
    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.setFacetField(solrQuery, LogSearchConstants.SOLR_HOST);
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      FacetField hostFacetField = response
        .getFacetField(LogSearchConstants.SOLR_HOST);
      if (hostFacetField == null)
        return convertObjToString(new SolrDocumentList());
      List<Count> hostList = hostFacetField.getValues();
      if (hostList == null)
        return convertObjToString(new SolrDocumentList());
      SolrDocumentList docList = response.getResults();
      String hostName = "";
      for (Count host : hostList) {
        SolrDocument solrDoc = new SolrDocument();
        hostName = host.getName();
        solrDoc.put(LogSearchConstants.SOLR_HOST, hostName);
        docList.add(solrDoc);
      }

      VGroupList collection = new VGroupList(docList);
      collection.setStartIndex((int) docList.getStart());
      collection.setTotalCount(docList.getNumFound());
      return convertObjToString(collection);
    } catch (IOException | SolrServerException | SolrException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  public VGroupList getComponentList(SearchCriteria searchCriteria) {
    SolrQuery query = new SolrQuery();
    query.setParam("event", "/audit/getLiveLogsCount");
    queryGenerator.setMainQuery(query, null);

    queryGenerator.setGroupField(query, LogSearchConstants.SOLR_COMPONENT,
      searchCriteria.getMaxRows());

    searchCriteria.setSortBy(LogSearchConstants.SOLR_COMPONENT);
    queryGenerator.setSortOrderDefaultServiceLog(query, searchCriteria);
    try {
      return this.getSolrGroupList(query);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + query, e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  public String getComponents(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = new SolrQuery();
    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.setFacetField(solrQuery,
      LogSearchConstants.SOLR_COMPONENT);
    queryGenerator.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      FacetField hostFacetField = response
        .getFacetField(LogSearchConstants.SOLR_COMPONENT);
      if (hostFacetField == null)
        return convertObjToString(new SolrDocumentList());
      List<Count> componenttList = hostFacetField.getValues();
      if (componenttList == null)
        return convertObjToString(new SolrDocumentList());
      SolrDocumentList docList = response.getResults();
      String hostName = "";
      for (Count component : componenttList) {
        SolrDocument solrDoc = new SolrDocument();
        hostName = component.getName();
        solrDoc.put(LogSearchConstants.SOLR_COMPONENT, hostName);
        docList.add(solrDoc);
      }

      VGroupList collection = new VGroupList(docList);
      collection.setStartIndex((int) docList.getStart());
      collection.setTotalCount(docList.getNumFound());
      return convertObjToString(collection);
    } catch (IOException | SolrServerException | SolrException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  public String getAggregatedInfo(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonFilterQuery(searchCriteria);
    String hierarchy = "host,type,level";
    try {
      queryGenerator.setMainQuery(solrQuery, null);
      queryGenerator.setFacetPivot(solrQuery, 1, hierarchy);
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);

      List<List<PivotField>> hirarchicalPivotField = new ArrayList<List<PivotField>>();
      List<VGraphData> dataList = new ArrayList<VGraphData>();
      NamedList<List<PivotField>> namedList = response.getFacetPivot();
      if (namedList != null) {
        hirarchicalPivotField = namedList.getAll(hierarchy);
      }
      if (!hirarchicalPivotField.isEmpty())
        dataList = buidGraphData(hirarchicalPivotField.get(0));
      VGraphInfo graphInfo = new VGraphInfo();
      graphInfo.setGraphData(dataList);
      return convertObjToString(graphInfo);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  public List<VGraphData> buidGraphData(List<PivotField> pivotFields) {
    List<VGraphData> logList = new ArrayList<VGraphData>();
    if (pivotFields != null) {
      for (PivotField pivotField : pivotFields) {
        VGraphData logLevel = new VGraphData();
        logLevel.setName("" + pivotField.getValue());
        logLevel.setCount(Long.valueOf(pivotField.getCount()));
        if (pivotField.getPivot() != null)
          logLevel.setDataList(buidGraphData(pivotField.getPivot()));
        logList.add(logLevel);
      }
    }
    return logList;
  }

  public VCountList getLogLevelCount(SearchCriteria searchCriteria) {
    VCountList collection = new VCountList();
    List<VCount> vCounts = new ArrayList<VCount>();
    SolrQuery solrQuery = new SolrQuery();
    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.setFacetField(solrQuery, LogSearchConstants.SOLR_LEVEL);
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      FacetField hostFacetField = response
        .getFacetField(LogSearchConstants.SOLR_LEVEL);
      if (hostFacetField == null)
        return collection;
      List<Count> levelList = hostFacetField.getValues();

      for (Count level : levelList) {
        VCount vCount = new VCount();
        vCount.setName(level.getName());
        vCount.setCount(level.getCount());
        vCounts.add(vCount);
      }

    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }

    collection.setCounts(vCounts);
    return collection;
  }

  public VCountList getComponenetsCount(SearchCriteria searchCriteria) {
    VCountList collection = new VCountList();
    List<VCount> vCounts = new ArrayList<VCount>();
    SolrQuery solrQuery = new SolrQuery();
    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.setFacetField(solrQuery,
      LogSearchConstants.SOLR_COMPONENT);
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      FacetField hostFacetField = response
        .getFacetField(LogSearchConstants.SOLR_COMPONENT);
      if (hostFacetField == null)
        return collection;
      List<Count> componentList = hostFacetField.getValues();

      for (Count component : componentList) {
        VCount vCount = new VCount();
        vCount.setName(component.getName());
        vCount.setCount(component.getCount());
        vCounts.add(vCount);
      }

    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }

    collection.setCounts(vCounts);
    return collection;
  }

  public VCountList getHostsCount(SearchCriteria searchCriteria) {
    VCountList collection = new VCountList();
    List<VCount> vCounts = new ArrayList<VCount>();
    SolrQuery solrQuery = new SolrQuery();
    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.setFacetField(solrQuery, LogSearchConstants.SOLR_HOST);
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      FacetField hostFacetField = response
        .getFacetField(LogSearchConstants.SOLR_HOST);
      if (hostFacetField == null)
        return collection;
      List<Count> hostList = hostFacetField.getValues();

      for (Count host : hostList) {
        VCount vCount = new VCount();
        vCount.setName(host.getName());
        vCount.setCount(host.getCount());
        vCounts.add(vCount);
      }

    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }

    collection.setCounts(vCounts);
    return collection;
  }

  public List<VNode> buidTreeData(List<PivotField> pivotFields,
                                  List<PivotField> pivotFieldHost, SolrQuery query,
                                  String firstPriority, String secondPriority) {
    List<VNode> extensionTree = new ArrayList<VNode>();
    String hostQuery = null;
    if (pivotFields != null) {
      // For Host
      for (PivotField pivotHost : pivotFields) {
        VNode hostNode = new VNode();
        hostNode.setName("" + pivotHost.getValue());
        hostNode.setValue("" + pivotHost.getCount());
        hostNode.setType(firstPriority);
        hostNode.setParent(true);
        hostNode.setRoot(true);
        PivotField hostPivot = null;
        for (PivotField searchHost : pivotFieldHost) {
          if (hostNode.getName().equals(searchHost.getValue())) {
            hostPivot = searchHost;
            break;
          }
        }
        List<PivotField> pivotLevelHost = hostPivot.getPivot();
        if (pivotLevelHost != null) {
          Collection<VNameValue> logLevelCount = new ArrayList<VNameValue>();
          for (PivotField pivotLevel : pivotLevelHost) {
            VNameValue vnameValue = new VNameValue();
            vnameValue.setName(((String) pivotLevel.getValue())
              .toUpperCase());

            vnameValue.setValue("" + pivotLevel.getCount());
            logLevelCount.add(vnameValue);

          }
          hostNode.setLogLevelCount(logLevelCount);
        }

        query.addFilterQuery(hostQuery);
        List<PivotField> pivotComponents = pivotHost.getPivot();
        // For Components
        if (pivotComponents != null) {
          Collection<VNode> componentNodes = new ArrayList<VNode>();
          for (PivotField pivotComp : pivotComponents) {
            VNode compNode = new VNode();
            compNode.setName("" + pivotComp.getValue());
            compNode.setType(secondPriority);
            compNode.setValue("" + pivotComp.getCount());
            compNode.setParent(false);
            compNode.setRoot(false);
            List<PivotField> pivotLevels = pivotComp.getPivot();
            if (pivotLevels != null) {
              Collection<VNameValue> logLevelCount = new ArrayList<VNameValue>();
              for (PivotField pivotLevel : pivotLevels) {
                VNameValue vnameValue = new VNameValue();
                vnameValue.setName(((String) pivotLevel
                  .getValue()).toUpperCase());

                vnameValue.setValue("" + pivotLevel.getCount());
                logLevelCount.add(vnameValue);

              }
              compNode.setLogLevelCount(logLevelCount);
            }
            componentNodes.add(compNode);
          }
          hostNode.setChilds(componentNodes);
        }
        extensionTree.add(hostNode);
      }
    }

    return extensionTree;
  }

  public VNodeList getTreeExtension(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonFilterQuery(searchCriteria);
    solrQuery.setParam("event", "/getTreeExtension");

    if (searchCriteria.getSortBy() == null) {
      searchCriteria.setSortBy(LogSearchConstants.SOLR_HOST);
      searchCriteria.setSortType(SolrQuery.ORDER.asc.toString());
    }
    queryGenerator.setFilterFacetSort(solrQuery, searchCriteria);
    String hostName = ""
      + ((searchCriteria.getParamValue("hostName") == null) ? ""
      : searchCriteria.getParamValue("hostName"));
    if (!"".equals(hostName))
      solrQuery.addFilterQuery(LogSearchConstants.SOLR_HOST + ":*"
        + hostName + "*");
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
    }

    return list;
  }

  public String getHostListByComponent(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonFilterQuery(searchCriteria);
    solrQuery.setParam("event", "/getHostListByComponent");

    if (searchCriteria.getSortBy() == null) {
      searchCriteria.setSortBy(LogSearchConstants.SOLR_HOST);
      searchCriteria.setSortType(SolrQuery.ORDER.asc.toString());
    }
    queryGenerator.setFilterFacetSort(solrQuery, searchCriteria);
    String componentName = ""
      + ((searchCriteria.getParamValue("componentName") == null) ? ""
      : searchCriteria.getParamValue("componentName"));
    if (!"".equals(componentName))
      solrQuery.addFilterQuery(LogSearchConstants.SOLR_COMPONENT + ":"
        + componentName);
    else
      try {
        return convertObjToString(new VNodeList());
      } catch (IOException e1) {
        logger.error(e1);
      }
    String firstHirarchy = "type,host,level";
    String secondHirarchy = "type,level";
    VNodeList list = new VNodeList();
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
        return convertObjToString(new ArrayList<VNode>());
      }

      List<VNode> dataList = buidTreeData(
        firstHirarchicalPivotFields.get(0),
        secondHirarchicalPivotFields.get(0), solrQuery,
        LogSearchConstants.COMPONENT, LogSearchConstants.HOST);

      list.setvNodeList(dataList);
      return convertObjToString(list);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  public VNameValueList getLogsLevelCount(SearchCriteria sc) {
    VNameValueList nameValueList = new VNameValueList();
    SolrQuery query = queryGenerator.commonFilterQuery(sc);
    query.setParam("event", "/getLogLevelCounts");
    List<VNameValue> logsCounts = getLogLevelFacets(query);
    nameValueList.setVNameValues(logsCounts);

    return nameValueList;
  }

  public List<VNameValue> getLogLevelFacets(SolrQuery query) {
    HashMap<String, String> map = new HashMap<String, String>();
    List<VNameValue> logsCounts = new ArrayList<VNameValue>();
    try {

      queryGenerator.setFacetField(query, LogSearchConstants.SOLR_LEVEL);

      List<Count> logLevelCounts = getFacetCounts(query,
        LogSearchConstants.SOLR_LEVEL);
      for (Count count : logLevelCounts) {
        map.put(count.getName().toUpperCase(), "" + count.getCount());
      }
      String level = LogSearchConstants.FATAL;
      VNameValue nameValue = null;

      String value = map.get(level);
      if (value == null || value.equals(""))
        value = "0";
      nameValue = new VNameValue();
      nameValue.setName(level);
      nameValue.setValue(value);
      logsCounts.add(nameValue);

      level = LogSearchConstants.ERROR;

      value = map.get(level);
      if (value == null || value.equals(""))
        value = "0";
      nameValue = new VNameValue();
      nameValue.setName(level);
      nameValue.setValue(value);
      logsCounts.add(nameValue);

      level = LogSearchConstants.WARN;

      value = map.get(level);
      if (value == null || value.equals(""))
        value = "0";
      nameValue = new VNameValue();
      nameValue.setName(level);
      nameValue.setValue(value);
      logsCounts.add(nameValue);

      level = LogSearchConstants.INFO;

      value = map.get(level);
      if (value == null || value.equals(""))
        value = "0";
      nameValue = new VNameValue();
      nameValue.setName(level);
      nameValue.setValue(value);
      logsCounts.add(nameValue);

      level = LogSearchConstants.DEBUG;

      value = map.get(level);
      if (value == null || value.equals(""))
        value = "0";
      nameValue = new VNameValue();
      nameValue.setName(level);
      nameValue.setValue(value);
      logsCounts.add(nameValue);

      level = LogSearchConstants.TRACE;

      value = map.get(level);
      if (value == null || value.equals(""))
        value = "0";
      nameValue = new VNameValue();
      nameValue.setName(level);
      nameValue.setValue(value);
      logsCounts.add(nameValue);

    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + query, e);
    }
    return logsCounts;
  }

  // Get Facet Count According to FacetFeild
  public List<Count> getFacetCounts(SolrQuery solrQuery, String facetField)
    throws SolrServerException, IOException, SolrException {

    QueryResponse response = serviceLogsSolrDao.process(solrQuery);

    FacetField field = response.getFacetField(facetField);
    if (field == null) {
      return new ArrayList<FacetField.Count>();
    }
    return field.getValues();
  }

  public String getPageByKeyword(SearchCriteria searchCriteria)
    throws SolrServerException {

    String keyword = solrUtil.makeSolrSearchString((String) searchCriteria
      .getParamValue("keyword"));

    String keyType = (String) searchCriteria.getParamValue("keywordType");

    if (!(boolean) "0".equals(keyType)) {
      try {
        int currentPageNumber = searchCriteria.getPage();
        int maxRows = searchCriteria.getMaxRows();
        String nextPageLogID = "";

        int lastLogIndexNumber = ((currentPageNumber + 1)
          * maxRows);
        String nextPageLogTime = "";


        // Next Page Start Time Calculation
        SolrQuery nextPageLogTimeQuery = queryGenerator
          .commonFilterQuery(searchCriteria);
        nextPageLogTimeQuery.remove("start");
        nextPageLogTimeQuery.remove("rows");
        nextPageLogTimeQuery.setStart(lastLogIndexNumber);
        nextPageLogTimeQuery.setRows(1);

        SolrDocumentList docList = serviceLogsSolrDao.process(
          nextPageLogTimeQuery).getResults();
        SolrDocument solrDoc = docList.get(0);

        Date logDate = (Date) solrDoc.get(LogSearchConstants.LOGTIME);
        nextPageLogTime = dateUtil
          .convertDateWithMillisecondsToSolrDate(logDate);
        nextPageLogID = ""
          + solrDoc.get(LogSearchConstants.ID);

        if (stringUtil.isEmpty(nextPageLogID))
          nextPageLogID = "0";

        String filterQueryListIds = "";
        // Remove the same Time Ids
        SolrQuery listRemoveIds = queryGenerator
          .commonFilterQuery(searchCriteria);
        listRemoveIds.remove("start");
        listRemoveIds.remove("rows");
        queryGenerator.setSingleIncludeFilter(listRemoveIds,
          LogSearchConstants.LOGTIME, "\"" + nextPageLogTime + "\"");
        queryGenerator.setSingleExcludeFilter(listRemoveIds,
          LogSearchConstants.ID, nextPageLogID);
        listRemoveIds.set("fl", LogSearchConstants.ID);
        SolrDocumentList docListIds = serviceLogsSolrDao.process(
          listRemoveIds).getResults();
        boolean isFirst = true;
        for (SolrDocument solrDocId : docListIds) {
          String id = "" + solrDocId.get(LogSearchConstants.ID);
          if (isFirst) {
            filterQueryListIds += "-" + LogSearchConstants.ID + ":" + id;
            isFirst = false;
          } else {
            filterQueryListIds += " AND " + "-" + LogSearchConstants.ID + ":" + id;
          }
        }

        // Keyword Sequence Number Calculation
        String endTime = (String) searchCriteria.getParamValue("to");
        String startTime = (String) searchCriteria
          .getParamValue("from");
        SolrQuery logTimeThroughRangeQuery = queryGenerator
          .commonFilterQuery(searchCriteria);
        logTimeThroughRangeQuery.remove("start");
        logTimeThroughRangeQuery.remove("rows");
        logTimeThroughRangeQuery.setRows(1);
        if (!stringUtil.isEmpty(filterQueryListIds))
          logTimeThroughRangeQuery.setFilterQueries(filterQueryListIds);


        String sortByType = searchCriteria.getSortType();

        if (!stringUtil.isEmpty(sortByType) && sortByType
          .equalsIgnoreCase(LogSearchConstants.ASCENDING_ORDER)) {
          
          /*sequenceNumber =""+( Integer.parseInt(sequenceNumber) - 1);*/
          /*queryGenerator.setSingleRangeFilter(
              logTimeThroughRangeQuery,
              LogSearchConstants.SEQUNCE_ID, "*",sequenceNumber);*/
          queryGenerator.setSingleRangeFilter(logTimeThroughRangeQuery,
            LogSearchConstants.LOGTIME, nextPageLogTime,
            endTime);
          logTimeThroughRangeQuery.set("sort",
            LogSearchConstants.LOGTIME + " "
              + LogSearchConstants.ASCENDING_ORDER);

        } else {
          /*sequenceNumber =""+( Integer.parseInt(sequenceNumber) + 1);*/
          /*queryGenerator.setSingleRangeFilter(
              logTimeThroughRangeQuery,
              LogSearchConstants.SEQUNCE_ID, sequenceNumber, "*");*/
          queryGenerator.setSingleRangeFilter(logTimeThroughRangeQuery,
            LogSearchConstants.LOGTIME, startTime,
            nextPageLogTime);
          logTimeThroughRangeQuery.set("sort",
            LogSearchConstants.LOGTIME + " "
              + LogSearchConstants.DESCENDING_ORDER);
        }
        queryGenerator.setSingleIncludeFilter(logTimeThroughRangeQuery,
          LogSearchConstants.SOLR_LOG_MESSAGE, keyword);


        SolrDocumentList documentList = serviceLogsSolrDao.process(
          logTimeThroughRangeQuery).getResults();

        SolrDocument solrDocument = new SolrDocument();
        if (!documentList.isEmpty())
          solrDocument = documentList.get(0);
        /*String keywordLogSequenceNumber = ""+ solrDocument.get(LogSearchConstants.SEQUNCE_ID);*/
        Date keywordLogDate = (Date) solrDocument.get(LogSearchConstants.LOGTIME);
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
          /*queryGenerator
          .setSingleRangeFilter(rangeLogQuery,
              LogSearchConstants.SEQUNCE_ID,"*", keywordLogSequenceNumber);*/


        } else {
          /*queryGenerator
          .setSingleRangeFilter(rangeLogQuery,
              LogSearchConstants.SEQUNCE_ID, keywordLogSequenceNumber,
              "*"); */
          keywordLogDate = dateUtil.addMilliSecondsToDate(keywordLogDate, -1);
          String keywordDateTime = dateUtil
            .convertDateWithMillisecondsToSolrDate(keywordLogDate);
          queryGenerator.setSingleRangeFilter(rangeLogQuery,
            LogSearchConstants.LOGTIME, keywordDateTime,
            endTime);
        }


        long countNumberLogs = countQuery(rangeLogQuery) - 1;
        
        /*// Delete Duplicate entries
        SolrQuery duplicatesLogQuery = nextPageLogTimeQuery.getCopy();
        duplicatesLogQuery.remove("start");
        duplicatesLogQuery.remove("rows");
        queryGenerator.setSingleIncludeFilter(duplicatesLogQuery,
            LogSearchConstants.LOGTIME, "\"" + keywordLogTime
                + "\"");

        countNumberLogs = countNumberLogs
            - countQuery(duplicatesLogQuery);*/

        //Adding numbers on 


        try {
          SolrQuery sameIdQuery = queryGenerator
            .commonFilterQuery(searchCriteria);
          queryGenerator.setSingleIncludeFilter(sameIdQuery,
            LogSearchConstants.LOGTIME, "\"" + originalKeywordDate + "\"");
          sameIdQuery.set("fl", LogSearchConstants.ID);
          SolrDocumentList sameQueryDocList = serviceLogsSolrDao.process(sameIdQuery)
            .getResults();
          for (SolrDocument solrDocumenent : sameQueryDocList) {
            String id = (String) solrDocumenent
              .getFieldValue(LogSearchConstants.ID);
            countNumberLogs++;
            if (id.equals(keywordId))
              break;
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
        String sequenceNumber = "";

        if (currentPageNumber == 0) {
          throw restErrorUtil.createRESTException("This is first Page Not",
            MessageEnums.ERROR_SYSTEM);
        }

        int firstLogCurrentPage = (currentPageNumber * maxRows);
        String lastLogsLogTime = "";

        // Next Page Start Time Calculation
        SolrQuery lastLogTime = queryGenerator
          .commonFilterQuery(searchCriteria);
        lastLogTime.remove("start");
        lastLogTime.remove("rows");

        lastLogTime.setStart(firstLogCurrentPage);
        lastLogTime.setRows(1);

        SolrDocumentList docList = serviceLogsSolrDao.process(
          lastLogTime).getResults();
        SolrDocument solrDoc = docList.get(0);

        Date logDate = (Date) solrDoc.get(LogSearchConstants.LOGTIME);
        String sortByType = searchCriteria.getSortType();
        lastLogsLogTime = dateUtil
          .convertDateWithMillisecondsToSolrDate(logDate);
        String lastLogsLogId = ""
          + solrDoc.get(LogSearchConstants.SEQUNCE_ID);
        if (stringUtil.isEmpty(sequenceNumber))
          sequenceNumber = "0";


        String filterQueryListIds = "";
        // Remove the same Time Ids
        SolrQuery listRemoveIds = queryGenerator
          .commonFilterQuery(searchCriteria);
        listRemoveIds.remove("start");
        listRemoveIds.remove("rows");
        queryGenerator.setSingleIncludeFilter(listRemoveIds,
          LogSearchConstants.LOGTIME, "\"" + lastLogsLogTime + "\"");
        queryGenerator.setSingleExcludeFilter(listRemoveIds,
          LogSearchConstants.ID, lastLogsLogId);
        listRemoveIds.set("fl", LogSearchConstants.ID);
        SolrDocumentList docListIds = serviceLogsSolrDao.process(
          listRemoveIds).getResults();
        boolean isFirst = true;
        for (SolrDocument solrDocId : docListIds) {
          String id = "" + solrDocId.get(LogSearchConstants.ID);
          if (isFirst) {
            filterQueryListIds += "-" + LogSearchConstants.ID + ":" + id;
            isFirst = false;
          } else {
            filterQueryListIds += " AND " + "-" + LogSearchConstants.ID + ":" + id;
          }
        }


        // Keyword LogTime Calculation
        String endTime = (String) searchCriteria.getParamValue("to");
        String startTime = (String) searchCriteria
          .getParamValue("from");
        SolrQuery logTimeThroughRangeQuery = queryGenerator
          .commonFilterQuery(searchCriteria);
        logTimeThroughRangeQuery.remove("start");
        logTimeThroughRangeQuery.remove("rows");
        logTimeThroughRangeQuery.setRows(1);
        queryGenerator.setSingleExcludeFilter(logTimeThroughRangeQuery,
          LogSearchConstants.ID, lastLogsLogId);
        if (!stringUtil.isEmpty(filterQueryListIds))
          logTimeThroughRangeQuery.setFilterQueries(filterQueryListIds);

        if (!stringUtil.isEmpty(sortByType) && sortByType
          .equalsIgnoreCase(LogSearchConstants.ASCENDING_ORDER)) {

          sequenceNumber = ""
            + (Integer.parseInt(sequenceNumber) - 1);
          logTimeThroughRangeQuery.remove("sort");
          logTimeThroughRangeQuery.set("sort",
            LogSearchConstants.LOGTIME + " "
              + LogSearchConstants.DESCENDING_ORDER);
          
          /*queryGenerator.setSingleRangeFilter(
              logTimeThroughRangeQuery,
              LogSearchConstants.SEQUNCE_ID,"*", sequenceNumber);*/
          queryGenerator.setSingleRangeFilter(
            logTimeThroughRangeQuery,
            LogSearchConstants.LOGTIME, startTime,
            lastLogsLogTime);

        } else {
          sequenceNumber = "" + (Integer.parseInt(sequenceNumber) + 1);

          logTimeThroughRangeQuery.remove("sort");
          logTimeThroughRangeQuery.set("sort",
            LogSearchConstants.LOGTIME + " "
              + LogSearchConstants.ASCENDING_ORDER);
          
          /*queryGenerator.setSingleRangeFilter(
              logTimeThroughRangeQuery,
              LogSearchConstants.SEQUNCE_ID, sequenceNumber,"*");*/
          queryGenerator.setSingleRangeFilter(logTimeThroughRangeQuery,
            LogSearchConstants.LOGTIME, lastLogsLogTime, endTime);
        }
        queryGenerator.setSingleIncludeFilter(logTimeThroughRangeQuery,
          LogSearchConstants.SOLR_LOG_MESSAGE, keyword);


        SolrDocumentList documentList = serviceLogsSolrDao.process(
          logTimeThroughRangeQuery).getResults();
        SolrDocument solrDocument = new SolrDocument();
        if (!documentList.isEmpty())
          solrDocument = documentList.get(0);

        
        /*String keywordLogSequenceNumber = ""+ solrDocument.get(LogSearchConstants.SEQUNCE_ID);*/
        Date keywordLogDate = (Date) solrDocument.get(LogSearchConstants.LOGTIME);
        String originalKeywordDate = dateUtil
          .convertDateWithMillisecondsToSolrDate(keywordLogDate);
        String keywordId = "" + solrDocument.get(LogSearchConstants.ID);

        // Getting Range Count from StartTime To Keyword Log Time
        SolrQuery rangeLogQuery = lastLogTime.getCopy();
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
          /*queryGenerator
          .setSingleRangeFilter(rangeLogQuery,
              LogSearchConstants.SEQUNCE_ID,"*", keywordLogSequenceNumber);*/


        } else {
          /*queryGenerator
          .setSingleRangeFilter(rangeLogQuery,
              LogSearchConstants.SEQUNCE_ID, keywordLogSequenceNumber,
              "*"); */
          keywordLogDate = dateUtil.addMilliSecondsToDate(keywordLogDate, -1);
          String keywordDateTime = dateUtil
            .convertDateWithMillisecondsToSolrDate(keywordLogDate);
          queryGenerator.setSingleRangeFilter(rangeLogQuery,
            LogSearchConstants.LOGTIME, keywordDateTime,
            endTime);
        }


        long countNumberLogs = countQuery(rangeLogQuery) - 1;
        
        /*// Delete Duplicate entries
        SolrQuery duplicatesLogQuery = nextPageLogTimeQuery.getCopy();
        duplicatesLogQuery.remove("start");
        duplicatesLogQuery.remove("rows");
        queryGenerator.setSingleIncludeFilter(duplicatesLogQuery,
            LogSearchConstants.LOGTIME, "\"" + keywordLogTime
                + "\"");

        countNumberLogs = countNumberLogs
            - countQuery(duplicatesLogQuery);*/

        //Adding numbers on 


        try {
          SolrQuery sameIdQuery = queryGenerator
            .commonFilterQuery(searchCriteria);
          queryGenerator.setSingleIncludeFilter(sameIdQuery,
            LogSearchConstants.LOGTIME, "\"" + originalKeywordDate + "\"");
          sameIdQuery.set("fl", LogSearchConstants.ID);
          SolrDocumentList sameQueryDocList = serviceLogsSolrDao.process(sameIdQuery)
            .getResults();
          for (SolrDocument solrDocumenent : sameQueryDocList) {
            String id = (String) solrDocumenent
              .getFieldValue(LogSearchConstants.ID);
            countNumberLogs++;
            if (id.equals(keywordId))
              break;
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
    throw restErrorUtil.createRESTException("keyword not found",
      MessageEnums.ERROR_SYSTEM);
  }

  public String getPageByKeyword1(SearchCriteria searchCriteria)
    throws SolrServerException {

    SolrQuery query = queryGenerator.commonFilterQuery(searchCriteria);
    String keyword = solrUtil.makeSearcableString((String) searchCriteria
      .getParamValue("keyword"));
    String uniqueId = (String) searchCriteria.getParamValue("token");
    if (uniqueId != null && !uniqueId.equals(""))
      cancelByDate.add(uniqueId);
    Long numberPages = 0l;
    int currentPageNumber = searchCriteria.getPage();
    try {
      numberPages = countQuery(query) / searchCriteria.getMaxRows();
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
    }
    if ((boolean) searchCriteria.getParamValue("keywordType").equals("0")) {
      for (int i = currentPageNumber - 1; i >= 0
        && !cancelRequest(uniqueId); i--) {
        mapUniqueId.put(uniqueId, "" + i);
        query.remove("rows");
        query.remove("start");
        query.setStart(i * searchCriteria.getMaxRows());
        query.setRows(searchCriteria.getMaxRows());
        VSolrLogList vSolrLogList = getLogAsPaginationProvided(query, serviceLogsSolrDao);
        SolrDocumentList documentList = vSolrLogList.getList();
        for (SolrDocument solrDoc : documentList) {
          String log_message = solrUtil
            .makeSearcableString((String) solrDoc
              .getFieldValue(LogSearchConstants.SOLR_LOG_MESSAGE));
          if (log_message != null
            && log_message
            .toLowerCase(Locale.ENGLISH)
            .contains(
              keyword.toLowerCase(Locale.ENGLISH))) {
            cancelByDate.remove(uniqueId);
            try {
              return convertObjToString(vSolrLogList);
            } catch (IOException e) {
              logger.error(e);
            }
          }
        }
      }

    } else {
      for (int i = currentPageNumber + 1; i <= numberPages
        && !cancelRequest(uniqueId); i++) {
        mapUniqueId.put(uniqueId, "" + i);
        query.remove("rows");
        query.remove("start");
        query.setStart(i * searchCriteria.getMaxRows());
        query.setRows(searchCriteria.getMaxRows());
        VSolrLogList vSolrLogList = getLogAsPaginationProvided(query, serviceLogsSolrDao);
        SolrDocumentList solrDocumentList = vSolrLogList.getList();
        for (SolrDocument solrDocument : solrDocumentList) {
          String logMessage = solrUtil
            .makeSearcableString((String) solrDocument
              .getFieldValue(LogSearchConstants.SOLR_LOG_MESSAGE));
          if (logMessage != null
            && logMessage.toLowerCase(Locale.ENGLISH).contains(
            keyword.toLowerCase(Locale.ENGLISH))) {
            cancelByDate.remove(uniqueId);
            try {
              return convertObjToString(vSolrLogList);
            } catch (SolrException | IOException e) {
              logger.error(e);
            }
          }
        }
      }
    }
    throw restErrorUtil.createRESTException("keyword not found",
      MessageEnums.ERROR_SYSTEM);
  }

  private String getPageByLogId(SearchCriteria searchCriteria) {
    String endLogTime = (String) searchCriteria.getParamValue("to");
    long startIndex = 0l;

    String logId = (String) searchCriteria.getParamValue("sourceLogId");
    SolrQuery solrQuery = queryGenerator.commonFilterQuery(searchCriteria);

    String endTimeMinusOneMilli = "";
    String logTime = "";
    try {

      SolrQuery logTimeByIdQuery = new SolrQuery();
      queryGenerator.setMainQuery(logTimeByIdQuery, null);
      queryGenerator.setSingleIncludeFilter(logTimeByIdQuery,
        LogSearchConstants.ID, logId);
      queryGenerator.setRowCount(solrQuery, 1);

      SolrDocumentList docList = serviceLogsSolrDao.process(
        logTimeByIdQuery).getResults();
      Date dateOfLogId = (Date) docList.get(0).get(
        LogSearchConstants.LOGTIME);

      logTime = dateUtil
        .convertDateWithMillisecondsToSolrDate(dateOfLogId);
      Date endDate = dateUtil.addMilliSecondsToDate(dateOfLogId, 1);
      endTimeMinusOneMilli = (String) dateUtil
        .convertDateWithMillisecondsToSolrDate(endDate);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
    }

    try {
      solrQuery.remove(LogSearchConstants.ID);
      solrQuery.remove(LogSearchConstants.LOGTIME);
      queryGenerator.setSingleRangeFilter(solrQuery,
        LogSearchConstants.LOGTIME, endTimeMinusOneMilli,
        endLogTime);
      queryGenerator.setRowCount(solrQuery, 0);
      startIndex = countQuery(solrQuery);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
    }

    try {
      SolrQuery sameIdQuery = queryGenerator
        .commonFilterQuery(searchCriteria);
      queryGenerator.setSingleIncludeFilter(sameIdQuery,
        LogSearchConstants.LOGTIME, "\"" + logTime + "\"");
      sameIdQuery.set("fl", LogSearchConstants.ID);
      SolrDocumentList docList = serviceLogsSolrDao.process(sameIdQuery)
        .getResults();
      for (SolrDocument solrDocumenent : docList) {
        String id = (String) solrDocumenent
          .getFieldValue(LogSearchConstants.ID);
        startIndex++;
        if (id.equals(logId))
          break;
      }

      SolrQuery logIdQuery = queryGenerator
        .commonFilterQuery(searchCriteria);
      logIdQuery.remove("rows");
      logIdQuery.remove("start");
      int start = (int) ((startIndex / searchCriteria.getMaxRows()) * searchCriteria
        .getMaxRows());
      logIdQuery.setStart(start);
      logIdQuery.setRows(searchCriteria.getMaxRows());
      VSolrLogList vSolrLogList = getLogAsPaginationProvided(logIdQuery, serviceLogsSolrDao);
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

      List<RangeFacet.Count> logLevelCounts;

      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      @SuppressWarnings("rawtypes")
      List<RangeFacet> rangeFacet = response.getFacetRanges();

      if (rangeFacet == null) {
        return new ArrayList<VNameValue>();

      }
      logLevelCounts = rangeFacet.get(0).getCounts();
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
    VBarDataList dataList = new VBarDataList();
    SolrQuery solrQuery = queryGenerator.commonFilterQuery(searchCriteria);
    solrQuery.set("event", "/getHistogramData");
    String from = (String) searchCriteria.getParamValue("from");
    String to = (String) searchCriteria.getParamValue("to");
    String unit = (String) searchCriteria.getParamValue("unit");

    List<VBarGraphData> histogramData = new ArrayList<VBarGraphData>();
    List<String> logLevels = ConfigUtil.logLevels;

    String jsonHistogramQuery = queryGenerator
      .buildJSONFacetTermTimeRangeQuery(
        LogSearchConstants.SOLR_LEVEL,
        LogSearchConstants.LOGTIME, from, to, unit).replace(
        "\\", "");

    try {
      queryGenerator.setJSONFacet(solrQuery, jsonHistogramQuery);
      queryGenerator.setRowCount(solrQuery, 0);
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      if (response == null)
        response = new QueryResponse();

      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
        .getResponse().get("facets");

      if (jsonFacetResponse == null
        || jsonFacetResponse.toString().equals("{count=0}"))
        return convertObjToString(dataList);

      extractValuesFromBuckets(jsonFacetResponse, "x", "y", histogramData);

      Collection<VNameValue> vNameValues = new ArrayList<VNameValue>();
      List<VBarGraphData> graphDatas = new ArrayList<VBarGraphData>();
      for (String level : logLevels) {
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
              value2.setValue("0");
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
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);

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

  public String cancelFindRequestByDate(HttpServletRequest request) {
    String uniqueId = null;

    uniqueId = (String) request.getParameter("token");
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
    for (String date : cancelByDate) {
      if (uniqueId.equalsIgnoreCase(date))
        return false;
    }
    return true;
  }

  public Response exportToTextFile(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonFilterQuery(searchCriteria);
    String from = (String) searchCriteria.getParamValue("from");
    from = from.replace("T", " ");
    from = from.replace(".", ",");
    String to = (String) searchCriteria.getParamValue("to");
    to = to.replace("T", " ");
    to = to.replace(".", ",");

    String utcOffset = (String) searchCriteria.getParamValue("utcOffset");
    to = dateUtil.addOffsetToDate(to, Long.parseLong(utcOffset),
      "yyyy-MM-dd HH:mm:ss,SSS");
    from = dateUtil.addOffsetToDate(from, Long.parseLong(utcOffset),
      "yyyy-MM-dd HH:mm:ss,SSS");

    String fileName = dateUtil.getCurrentDateInString();
    if (searchCriteria.getParamValue("hostLogFile") != null
      && searchCriteria.getParamValue("compLogFile") != null) {
      fileName = searchCriteria.getParamValue("hostLogFile") + "_"
        + searchCriteria.getParamValue("compLogFile");
    }
    String format = (String) searchCriteria.getParamValue("format");
    format = "text".equalsIgnoreCase(format) && format != null ? ".txt"
      : ".json";
    String textToSave = "";
    try {
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
      SolrDocumentList docList = response.getResults();
      VSummary vsummary = bizUtil.buildSummaryForLogFile(docList);
      vsummary.setFormat(format);
      vsummary.setFrom(from);
      vsummary.setTo(to);

      try {
        String include[] = ((String) searchCriteria
          .getParamValue("iMessage"))
          .split(LogSearchConstants.I_E_SEPRATOR);
        String includeString = "";
        for (String inc : include) {
          includeString = includeString + ",\"" + inc + "\"";
        }
        includeString = includeString.replaceFirst(",", "");
        if (!stringUtil.isEmpty(includeString)) {
          vsummary.setIncludeString(includeString);
        }
      } catch (Exception e) {
        // do nothing
      }

      String excludeString = "";
      boolean isNormalExcluded = false;
      try {
        String exclude[] = ((String) searchCriteria
          .getParamValue("eMessage"))
          .split(LogSearchConstants.I_E_SEPRATOR);
        for (String exc : exclude) {
          excludeString = excludeString + ",\"" + exc + "\"";
        }
        excludeString = excludeString.replaceFirst(",", "");
        if (!stringUtil.isEmpty(excludeString)) {
          vsummary.setExcludeString(excludeString);
          isNormalExcluded = true;
        }
      } catch (Exception ne) {
        // do nothing
      }
      try {

        String globalExclude[] = ((String) searchCriteria
          .getParamValue("gEMessage"))
          .split(LogSearchConstants.I_E_SEPRATOR);

        for (String exc : globalExclude) {
          excludeString = excludeString + ",\"" + exc + "\"";
        }

        if (!stringUtil.isEmpty(excludeString)) {
          if (!isNormalExcluded)
            excludeString = excludeString.replaceFirst(",", "");
          vsummary.setExcludeString(excludeString);
        }
      } catch (Exception ne) {
        // do nothing
      }

      for (SolrDocument solrDoc : docList) {

        Date logTimeDateObj = (Date) solrDoc
          .get(LogSearchConstants.LOGTIME);

        String logTime = dateUtil.convertSolrDateToNormalDateFormat(
          logTimeDateObj.getTime(), Long.parseLong(utcOffset));
        solrDoc.remove(LogSearchConstants.LOGTIME);
        solrDoc.addField(LogSearchConstants.LOGTIME, logTime);
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
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  public String getComponentListWithLevelCounts(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonFilterQuery(searchCriteria);
    solrQuery.setParam("event", "/getComponentListWithLevelCounts");

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
        VNode comp = new VNode();
        comp.setName("" + singlePivotField.getValue());
        List<PivotField> levelList = singlePivotField.getPivot();
        List<VNameValue> levelCountList = new ArrayList<VNameValue>();
        comp.setLogLevelCount(levelCountList);
        for (PivotField levelPivot : levelList) {
          VNameValue level = new VNameValue();
          level.setName(("" + levelPivot.getValue()).toUpperCase());
          level.setValue("" + levelPivot.getCount());
          levelCountList.add(level);
        }
        datatList.add(comp);
      }
      list.setvNodeList(datatList);
      return convertObjToString(list);
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  public String getExtremeDatesForBundelId(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = new SolrQuery();
    try {
      String bundelId = (String) searchCriteria
        .getParamValue(LogSearchConstants.BUNDLE_ID);

      queryGenerator.setSingleIncludeFilter(solrQuery,
        LogSearchConstants.BUNDLE_ID, bundelId);

      queryGenerator.setMainQuery(solrQuery, null);
      solrQuery.setSort(LogSearchConstants.LOGTIME, SolrQuery.ORDER.asc);
      queryGenerator.setRowCount(solrQuery, 1);
      VNameValueList nameValueList = new VNameValueList();
      List<VNameValue> vNameValues = new ArrayList<VNameValue>();
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);

      SolrDocumentList solrDocList = response.getResults();
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
        Date logTimeDesc = (Date) solrDoc
          .getFieldValue(LogSearchConstants.LOGTIME);

        if (logTimeDesc != null) {
          VNameValue nameValue = new VNameValue();
          nameValue.setName("To");
          nameValue.setValue("" + logTimeDesc.getTime());
          vNameValues.add(nameValue);
        }
      }
      nameValueList.setVNameValues(vNameValues);
      return convertObjToString(nameValueList);

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error(e);
      try {
        return convertObjToString(new VNameValueList());
      } catch (IOException e1) {
        throw restErrorUtil.createRESTException(e1.getMessage(),
          MessageEnums.DATA_NOT_FOUND);
      }
    }
  }

  protected VGroupList getSolrGroupList(SolrQuery query)
    throws SolrServerException, IOException, SolrException {
    QueryResponse response = serviceLogsSolrDao.process(query);
    SolrDocumentList docList = response.getResults();
    VGroupList collection = new VGroupList(docList);
    collection.setStartIndex((int) docList.getStart());
    collection.setTotalCount(docList.getNumFound());
    return collection;
  }

  public Long countQuery(SolrQuery query) throws SolrException,
    SolrServerException, IOException {
    query.setRows(0);
    QueryResponse response = serviceLogsSolrDao.process(query);
    SolrDocumentList docList = response.getResults();
    return docList.getNumFound();
  }

  public String getServiceLogsFieldsName() {
    String fieldsNameStrArry[] = PropertiesUtil
      .getPropertyStringList("solr.servicelogs.fields");
    if (fieldsNameStrArry.length > 0) {
      try {
        List<String> uiFieldNames = new ArrayList<String>();
        String temp = null;
        for (String field : fieldsNameStrArry) {
          temp = ConfigUtil.serviceLogsColumnMapping.get(field
            + LogSearchConstants.SOLR_SUFFIX);
          if (temp == null)
            uiFieldNames.add(field);
          else
            uiFieldNames.add(temp);
        }
        return convertObjToString(uiFieldNames);
      } catch (IOException e) {
        logger.error("converting object to json failed", e);
      }
    }
    throw restErrorUtil.createRESTException(
      "No field name found in property file",
      MessageEnums.DATA_NOT_FOUND);

  }

  public String getServiceLogsSchemaFieldsName() {

    List<String> fieldNames = new ArrayList<String>();
    String suffix = PropertiesUtil.getProperty("solr.core.logs");
    String excludeArray[] = PropertiesUtil
      .getPropertyStringList("servicelogs.exclude.columnlist");

    HashMap<String, String> uiFieldColumnMapping = new LinkedHashMap<String, String>();
    ConfigUtil.getSchemaFieldsName(suffix, excludeArray, fieldNames);

    for (String fieldName : fieldNames) {
      String uiField = ConfigUtil.serviceLogsColumnMapping.get(fieldName
        + LogSearchConstants.SOLR_SUFFIX);
      if (uiField != null) {
        uiFieldColumnMapping.put(fieldName, uiField);
      } else {
        uiFieldColumnMapping.put(fieldName, fieldName);
      }
    }

    try {
      HashMap<String, String> uiFieldColumnMappingSorted = new LinkedHashMap<String, String>();
      uiFieldColumnMappingSorted.put(LogSearchConstants.SOLR_LOG_MESSAGE,
        "");

      Iterator<Entry<String, String>> it = bizUtil
        .sortHashMapByValuesD(uiFieldColumnMapping).entrySet()
        .iterator();
      while (it.hasNext()) {
        @SuppressWarnings("rawtypes")
        Map.Entry pair = (Map.Entry) it.next();
        uiFieldColumnMappingSorted.put("" + pair.getKey(),
          "" + pair.getValue());
        it.remove();
      }

      return convertObjToString(uiFieldColumnMappingSorted);
    } catch (IOException e) {
      logger.error(e);
    }
    throw restErrorUtil.createRESTException(
      "Cache is Empty for FieldsName", MessageEnums.DATA_NOT_FOUND);
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

  public String getCurrentPageOfKeywordSearch(String requestDate) {
    if (stringUtil.isEmpty(requestDate)) {
      logger.error("Unique id is Empty");
      throw restErrorUtil.createRESTException("Unique id is Empty",
        MessageEnums.DATA_NOT_FOUND);
    }
    String pageNumber = mapUniqueId.get(requestDate);
    if (stringUtil.isEmpty(pageNumber)) {
      logger.error("Not able to find Page Number");
      throw restErrorUtil.createRESTException("Page Number not found",
        MessageEnums.DATA_NOT_FOUND);
    }
    return pageNumber;
  }

  public String getAnyGraphData(SearchCriteria searchCriteria) {
    searchCriteria.addParam("feildTime", LogSearchConstants.LOGTIME);
    String suffix = PropertiesUtil.getProperty("solr.core.logs");
    searchCriteria.addParam("suffix", suffix);
    SolrQuery solrQuery = queryGenerator.commonFilterQuery(searchCriteria);
    String result = graphDataGnerator.getAnyGraphData(searchCriteria,
      serviceLogsSolrDao, solrQuery);
    if (result != null)
      return result;
    try {
      return convertObjToString(new VBarDataList());
    } catch (IOException e) {
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.ERROR_SYSTEM);
    }
  }

  public String getAfterBeforeLogs(SearchCriteria searchCriteria) {
    SolrDocumentList docList = null;
    String id = (String) searchCriteria
      .getParamValue(LogSearchConstants.ID);
    if (stringUtil.isEmpty(id)) {
      try {
        return convertObjToString(new VSolrLogList());
      } catch (IOException e) {
        throw restErrorUtil.createRESTException(e.getMessage(),
          MessageEnums.ERROR_SYSTEM);
      }
    }
    String maxRows = "";

    maxRows = (String) searchCriteria.getParamValue("numberRows");
    if (stringUtil.isEmpty(maxRows))
      maxRows = "10";
    String scrollType = (String) searchCriteria.getParamValue("scrollType");
    String logTime = null;
    String sequenceId = null;
    try {
      SolrQuery solrQuery = new SolrQuery();
      queryGenerator.setMainQuery(solrQuery,
        queryGenerator.buildFilterQuery(LogSearchConstants.ID, id));
      queryGenerator.setRowCount(solrQuery, 1);
      QueryResponse response = serviceLogsSolrDao.process(solrQuery);
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
        return convertObjToString(new VSolrLogList());
      }
    } catch (SolrServerException | SolrException | IOException e) {
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.DATA_NOT_FOUND);
    }
    if (LogSearchConstants.SCROLL_TYPE_BEFORE.equals(scrollType)) {
      VSolrLogList vSolrLogList = whenScrollUp(searchCriteria, logTime,
        sequenceId, maxRows);

      SolrDocumentList solrDocList = new SolrDocumentList();
      for (SolrDocument solrDoc : vSolrLogList.getList()) {
        solrDocList.add(solrDoc);
      }
      vSolrLogList.setSolrDocuments(solrDocList);
      try {
        return convertObjToString(vSolrLogList);
      } catch (IOException e) {
        // do nothing
      }
    } else if (LogSearchConstants.SCROLL_TYPE_AFTER.equals(scrollType)) {
      SolrDocumentList solrDocList = new SolrDocumentList();
      VSolrLogList vSolrLogList = new VSolrLogList();
      for (SolrDocument solrDoc : whenScrollDown(searchCriteria, logTime,
        sequenceId, maxRows).getList()) {
        solrDocList.add(solrDoc);
      }
      vSolrLogList.setSolrDocuments(solrDocList);
      try {
        return convertObjToString(vSolrLogList);
      } catch (IOException e) {
        // do nothing
      }

    } else {
      VSolrLogList vSolrLogList = new VSolrLogList();
      SolrDocumentList initial = new SolrDocumentList();
      SolrDocumentList before = whenScrollUp(searchCriteria, logTime,
        sequenceId, maxRows).getList();
      SolrDocumentList after = whenScrollDown(searchCriteria, logTime,
        sequenceId, maxRows).getList();
      if (before == null || before.isEmpty())
        before = new SolrDocumentList();
      for (SolrDocument solrDoc : Lists.reverse(before)) {
        initial.add(solrDoc);
      }
      initial.add(docList.get(0));
      if (after == null || after.isEmpty())
        after = new SolrDocumentList();
      for (SolrDocument solrDoc : after) {
        initial.add(solrDoc);
      }
      vSolrLogList.setSolrDocuments(initial);
      try {
        return convertObjToString(vSolrLogList);
      } catch (IOException e) {
        // do nothing
      }
    }
    try {
      return convertObjToString(new VSolrLogList());
    } catch (IOException e) {
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.DATA_NOT_FOUND);
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
      + LogSearchConstants.ASCENDING_ORDER;
    List<String> sortOrder = new ArrayList<String>();
    sortOrder.add(order1);
    sortOrder.add(order2);
    searchCriteria.addParam("sort", sortOrder);
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
    searchCriteria.addParam("sort", sortOrder);
    queryGenerator.setMultipleSortOrder(solrQuery, searchCriteria);

    return getLogAsPaginationProvided(solrQuery, serviceLogsSolrDao);
  }

  @SuppressWarnings("unchecked")
  public String getSuggestions(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = new SolrQuery();
    queryGenerator.setMainQuery(solrQuery, null);
    String field = "" + searchCriteria.getParamValue("fieldName");
    String originalFieldName = ConfigUtil.serviceLogsColumnMapping
      .get(field + LogSearchConstants.UI_SUFFIX);
    if (originalFieldName == null)
      originalFieldName = field;
    originalFieldName = LogSearchConstants.NGRAM_SUFFIX + originalFieldName;
    String value = "" + searchCriteria.getParamValue("valueToSuggest");
    String jsonQuery = queryGenerator
      .buidlJSONFacetRangeQueryForSuggestion(originalFieldName, value);
    try {
      List<String> valueList = new ArrayList<String>();
      queryGenerator.setJSONFacet(solrQuery, jsonQuery);
      QueryResponse queryResponse = serviceLogsSolrDao.process(solrQuery);
      if (queryResponse == null)
        return convertObjToString(valueList);
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) queryResponse
        .getResponse().get("facets");
      if (jsonFacetResponse == null
        || jsonFacetResponse.equals("{count=0}"))
        return convertObjToString(valueList);
      SimpleOrderedMap<Object> stack = (SimpleOrderedMap<Object>) jsonFacetResponse
        .get("y");
      if (stack == null || stack.equals("{count=0}"))
        return convertObjToString(valueList);
      SimpleOrderedMap<Object> buckets = (SimpleOrderedMap<Object>) stack
        .get("x");
      if (buckets == null || buckets.equals("{count=0}"))
        return convertObjToString(valueList);
      ArrayList<Object> bucketList = (ArrayList<Object>) buckets
        .get("buckets");
      for (Object object : bucketList) {
        SimpleOrderedMap<Object> simpleOrderdMap = (SimpleOrderedMap<Object>) object;
        String val = "" + simpleOrderdMap.getVal(0);
        valueList.add(val);
      }
      return convertObjToString(valueList);
    } catch (SolrException | SolrServerException | IOException e) {
      try {
        return convertObjToString(new ArrayList<String>());
      } catch (IOException e1) {
        throw restErrorUtil.createRESTException(e.getMessage(),
          MessageEnums.ERROR_SYSTEM);
      }
    }
  }
}