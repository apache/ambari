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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.ManageStartEndTime;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.dao.AuditSolrDao;
import org.apache.ambari.logsearch.graph.GraphDataGenerator;
import org.apache.ambari.logsearch.util.BizUtil;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.StringUtil;
import org.apache.ambari.logsearch.view.VBarDataList;
import org.apache.ambari.logsearch.view.VBarGraphData;
import org.apache.ambari.logsearch.view.VGroupList;
import org.apache.ambari.logsearch.view.VNameValue;
import org.apache.ambari.logsearch.view.VNameValueList;
import org.apache.ambari.logsearch.view.VResponse;
import org.apache.ambari.logsearch.view.VSolrLogList;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuditMgr extends MgrBase {
  static Logger logger = Logger.getLogger(AuditMgr.class);

  @Autowired
  AuditSolrDao auditSolrDao;

  @Autowired
  RESTErrorUtil restErrorUtil;

  @Autowired
  JSONUtil jsonUtil;

  @Autowired
  StringUtil stringUtil;

  @Autowired
  BizUtil bizUtil;

  @Autowired
  DateUtil dateUtil;

  @Autowired
  GraphDataGenerator graphDataGenerator;

  public String getLogs(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);
    VSolrLogList collection = getLogAsPaginationProvided(solrQuery,
        auditSolrDao);
    return convertObjToString(collection);

  }

  public SolrDocumentList getComponents(SearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator
      .commonAuditFilterQuery(searchCriteria);
    SolrDocumentList docList = new SolrDocumentList();
    try {
      queryGenerator.setFacetField(solrQuery,
        LogSearchConstants.AUDIT_COMPONENT);
      queryGenerator.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
      List<FacetField> facetFields = null;
      List<Count> componentsCount = new ArrayList<Count>();
      FacetField facetField = null;

      QueryResponse queryResponse = auditSolrDao.process(solrQuery);
      if (queryResponse == null) {
        return docList;
      }

      facetFields = queryResponse.getFacetFields();
      if (facetFields == null) {
        return docList;
      }
      if (!facetFields.isEmpty()) {
        facetField = facetFields.get(0);
      }
      if (facetField != null) {
        componentsCount = facetField.getValues();
      }
    
      for (Count compnonet : componentsCount) {
        SolrDocument solrDocument = new SolrDocument();
        solrDocument.addField("type", compnonet.getName());
        docList.add(solrDocument);
      }
      return docList;
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public String getAuditComponents(SearchCriteria searchCriteria) {
    VGroupList vGroupList = new VGroupList();
    SolrDocumentList docList = getComponents(searchCriteria);

    vGroupList.setGroupDocuments(docList);
    return convertObjToString(vGroupList);
  }

  @SuppressWarnings("unchecked")
  public String getAuditLineGraphData(SearchCriteria searchCriteria) {
    VBarDataList dataList = new VBarDataList();
    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);

    String from = getFrom((String) searchCriteria.getParamValue("startTime"));
    String to = getTo((String) searchCriteria.getParamValue("endTime"));
    String unit = getUnit((String) searchCriteria.getParamValue("unit"));
    
    

    List<VBarGraphData> histogramData = new ArrayList<VBarGraphData>();
    String jsonHistogramQuery = queryGenerator.buildJSONFacetTermTimeRangeQuery(
      LogSearchConstants.AUDIT_COMPONENT,
      LogSearchConstants.AUDIT_EVTTIME, from, to, unit).replace("\\",
      "");

    try {
      queryGenerator.setJSONFacet(solrQuery, jsonHistogramQuery);
      queryGenerator.setRowCount(solrQuery, 0);
      QueryResponse response = auditSolrDao.process(solrQuery);
      if (response == null){
        return convertObjToString(dataList);
      }
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
        .getResponse().get("facets");

      if (jsonFacetResponse == null
        || jsonFacetResponse.toString().equals("{count=0}")){
        return convertObjToString(dataList);
      }

      extractValuesFromBucket(jsonFacetResponse, "x", "y",
        histogramData);

      dataList.setGraphData(histogramData);
      return convertObjToString(dataList);

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);

    }
  }

  public String getTopAuditFieldCount(SearchCriteria searchCriteria) {
    int topCounts = 10;
    Integer top = (Integer) searchCriteria.getParamValue("top");
    String facetField = (String) searchCriteria.getParamValue("field");
    if (top == null){
      top = new Integer(topCounts);
    }
    SolrQuery solrQuery = queryGenerator
      .commonAuditFilterQuery(searchCriteria);
    try {

      List<VNameValue> nameValues = new ArrayList<VNameValue>();

      VNameValueList nameValueList = new VNameValueList(nameValues);

      queryGenerator.setFacetField(solrQuery, facetField);
      queryGenerator.setFacetSort(solrQuery, LogSearchConstants.FACET_COUNT);
      queryGenerator.setFacetLimit(solrQuery, top.intValue());

      List<Count> countList = new ArrayList<FacetField.Count>();
      QueryResponse queryResponse = auditSolrDao.process(solrQuery);
      if(queryResponse == null){
        return convertObjToString(nameValueList);
      }
      
      if (queryResponse.getFacetField(facetField) != null) {
        FacetField queryFacetField = queryResponse
          .getFacetField(facetField);
        if (queryFacetField != null) {
          countList = queryFacetField.getValues();
        }
      }

      for (Count cnt : countList) {
        VNameValue nameValue = new VNameValue();
        nameValue.setName(cnt.getName());

        nameValue.setValue("" + cnt.getCount());
        nameValues.add(nameValue);
      }
      return convertObjToString(nameValueList);

    } catch (SolrException | IOException | SolrServerException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public String getLiveLogCounts() {
    VNameValueList nameValueList = new VNameValueList();
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setParam("event", "/getLiveLogsCount");
    try {
      String startDate = dateUtil
        .convertGivenDateFormatToSolrDateFormat(ManageStartEndTime.startDate);

      String endDate = dateUtil
        .convertGivenDateFormatToSolrDateFormat(ManageStartEndTime.endDate);

      queryGenerator.setMainQuery(solrQuery, null);
      queryGenerator.setFacetRange(solrQuery,
        LogSearchConstants.AUDIT_EVTTIME, startDate, endDate,
        "+2MINUTE");
      List<RangeFacet.Count> listCount;

      QueryResponse response = auditSolrDao.process(solrQuery);
 
      List<RangeFacet> rangeFacet = response.getFacetRanges();
      if (rangeFacet == null){
        return convertObjToString(nameValueList);
      }
      RangeFacet range=rangeFacet.get(0);
      
      if(range == null){
        return convertObjToString(nameValueList);
      }
      
      listCount = range.getCounts();

      List<VNameValue> nameValues = new ArrayList<VNameValue>();
      int count = 0;
      for (RangeFacet.Count cnt : listCount) {
        VNameValue nameValue = new VNameValue();
        nameValue.setName("" + count);
        nameValue.setValue("" + cnt.getCount());
        nameValues.add(nameValue);
        count++;
      }
      nameValueList.setVNameValues(nameValues);
      return convertObjToString(nameValueList);

    } catch (SolrException | SolrServerException | ParseException
      | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public String topTenUsers(SearchCriteria searchCriteria) {

    String jsonUserQuery = "{Users:{type:terms, field:reqUser, facet:{ Repo:{ type:terms, field:repo, facet:{eventCount:\"sum(event_count)\"}}}}}";
    SolrQuery solrQuery = queryGenerator
      .commonAuditFilterQuery(searchCriteria);
    queryGenerator.setJSONFacet(solrQuery, jsonUserQuery);
    queryGenerator.setRowCount(solrQuery, 0);
    try {
      VBarDataList vBarDataList = new VBarDataList();
      QueryResponse queryResponse = auditSolrDao.process(solrQuery);
      if(queryResponse == null){
        return convertObjToString(vBarDataList);
      }

      NamedList<Object> namedList = queryResponse.getResponse();
      
      if (namedList == null) {
        return convertObjToString(vBarDataList);
      }

      @SuppressWarnings("unchecked")
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) namedList
        .get("facets");
      if(jsonFacetResponse == null){
        return convertObjToString(vBarDataList);
      }
      if(jsonFacetResponse.toString().equals("{count=0}")){
        return convertObjToString(vBarDataList);
      }
      vBarDataList = bizUtil.buildSummaryForTopCounts(jsonFacetResponse,"Repo","Users");
      return convertObjToString(vBarDataList);

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error("Error during solrQuery=" + e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public String topTenResources(SearchCriteria searchCriteria) {

    String jsonUserQuery = "{Users:{type:terms,field:resource,facet:{Repo:{type:terms,field:repo,facet:{eventCount:\"sum(event_count)\"}}}}}";
    SolrQuery solrQuery = queryGenerator
      .commonAuditFilterQuery(searchCriteria);
    queryGenerator.setJSONFacet(solrQuery, jsonUserQuery);
    queryGenerator.setRowCount(solrQuery, 0);
    try {
      VBarDataList vBarDataList = new VBarDataList();
      QueryResponse queryResponse = auditSolrDao.process(solrQuery);
      if(queryResponse == null){
        return convertObjToString(vBarDataList);
      }

      NamedList<Object> namedList = queryResponse.getResponse();
      if (namedList == null) {
        return convertObjToString(vBarDataList);
      }

      @SuppressWarnings("unchecked")
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) namedList
        .get("facets");

      vBarDataList = bizUtil.buildSummaryForTopCounts(jsonFacetResponse,"Repo","Users");
      return convertObjToString(vBarDataList);

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  @SuppressWarnings("unchecked")
  public String getRequestUserLineGraph(SearchCriteria searchCriteria) {

    String from = getFrom((String) searchCriteria.getParamValue("startTime"));
    String to = getTo((String) searchCriteria.getParamValue("endTime"));
    String unit = getUnit((String) searchCriteria.getParamValue("unit"));
    
    SolrQuery solrQuery = queryGenerator
      .commonAuditFilterQuery(searchCriteria);

    VBarDataList dataList = new VBarDataList();
    List<VBarGraphData> histogramData = new ArrayList<VBarGraphData>();

    queryGenerator.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);

    String jsonHistogramQuery = queryGenerator
      .buildJSONFacetTermTimeRangeQuery(
        LogSearchConstants.AUDIT_REQUEST_USER,
        LogSearchConstants.AUDIT_EVTTIME, from, to, unit)
      .replace("\\", "");

    try {
      queryGenerator.setJSONFacet(solrQuery, jsonHistogramQuery);
      queryGenerator.setRowCount(solrQuery, 0);
      QueryResponse response = auditSolrDao.process(solrQuery);
      if (response == null){
        return convertObjToString(dataList);
      }
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
        .getResponse().get("facets");

      if (jsonFacetResponse == null
        || jsonFacetResponse.toString().equals("{count=0}")){
        return convertObjToString(dataList);
      }
      extractValuesFromBucket(jsonFacetResponse, "x", "y", histogramData);

      dataList.setGraphData(histogramData);
      return convertObjToString(dataList);

    } catch (SolrException | IOException | SolrServerException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }

  }

  public SolrDocumentList getRequestUser(SearchCriteria searchCriteria) {
    SolrDocumentList docList = new SolrDocumentList();
    SolrQuery solrQuery = queryGenerator
      .commonAuditFilterQuery(searchCriteria);
    try {
      queryGenerator.setFacetField(solrQuery,
        LogSearchConstants.AUDIT_REQUEST_USER);
      queryGenerator.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
      List<FacetField> facetFields = null;
      List<Count> componentsCount = new ArrayList<Count>();
      FacetField facetField = null;

      QueryResponse queryResponse = auditSolrDao.process(solrQuery);
      if (queryResponse == null) {
        return docList;
      }

      facetFields = queryResponse.getFacetFields();
      if (facetFields == null) {
        return docList;
      }
      if (!facetFields.isEmpty()) {
        facetField = facetFields.get(0);
      }
      if (facetField != null) {
        componentsCount = facetField.getValues();
      }
     
      for (Count compnonet : componentsCount) {
        SolrDocument solrDocument = new SolrDocument();
        solrDocument.addField("type", compnonet.getName());
        docList.add(solrDocument);
      }
      return docList;
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public String getAuditLogsSchemaFieldsName() {
    String suffix = PropertiesUtil.getProperty("auditlog.solr.core.logs");
    String excludeArray[] = PropertiesUtil
        .getPropertyStringList("auditlog.exclude.columnlist");
    List<String> fieldNames = new ArrayList<String>();
    HashMap<String, String> uiFieldColumnMapping = new HashMap<String, String>();
    ConfigUtil.getSchemaFieldsName(suffix, excludeArray, fieldNames);

    for (String fieldName : fieldNames) {
      String uiField = ConfigUtil.auditLogsColumnMapping.get(fieldName
          + LogSearchConstants.SOLR_SUFFIX);
      if (uiField == null) {
        uiFieldColumnMapping.put(fieldName, fieldName);
      } else {
        uiFieldColumnMapping.put(fieldName, uiField);
      }
    }

    uiFieldColumnMapping = bizUtil.sortHashMapByValues(uiFieldColumnMapping);
    return convertObjToString(uiFieldColumnMapping);

  }

  public String getAnyGraphData(SearchCriteria searchCriteria) {
    searchCriteria.addParam("fieldTime", LogSearchConstants.AUDIT_EVTTIME);
    String suffix = PropertiesUtil.getProperty("auditlog.solr.core.logs");
    searchCriteria.addParam("suffix", suffix);
    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);
    VBarDataList result = graphDataGenerator.getAnyGraphData(searchCriteria,
        auditSolrDao, solrQuery);
    if (result == null) {
      result = new VBarDataList();
    }

    return convertObjToString(result);

  }

  @SuppressWarnings("unchecked")
  public void extractValuesFromBucket(
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

  @SuppressWarnings({"unchecked", "resource"})
  public Response exportUserTableToTextFile(SearchCriteria searchCriteria) {
    String jsonUserQuery = "{ Users: { type: terms, field: reqUser, facet:  {Repo: {  type: terms, field: repo, facet: {  eventCount: \"sum(event_count)\"}}}},x:{ type: terms,field: resource, facet: {y: {  type: terms, field: repo,facet: {  eventCount: \"sum(event_count)\"}}}}}";

    SolrQuery solrQuery = queryGenerator
      .commonAuditFilterQuery(searchCriteria);
    String startTime = (String) searchCriteria.getParamValue("startTime");
    String endTime = (String) searchCriteria.getParamValue("endTime");

    startTime = startTime == null ? "" : startTime;
    endTime = endTime == null ? "" : "_" + endTime;

    queryGenerator.setJSONFacet(solrQuery, jsonUserQuery);
    queryGenerator.setRowCount(solrQuery, 0);

    String dataFormat = (String) searchCriteria.getParamValue("format");
    try {
      QueryResponse queryResponse = auditSolrDao.process(solrQuery);
      if(queryResponse == null){
        VResponse response = new VResponse();
        response.setMsgDesc("Query was not able to execute "+solrQuery);
        throw restErrorUtil.createRESTException(response);
      }

      NamedList<Object> namedList = queryResponse.getResponse();
      if(namedList == null){
        VResponse response = new VResponse();
        response.setMsgDesc("Query was not able to execute "+solrQuery);
        throw restErrorUtil.createRESTException(response);
      }
      VBarDataList vBarUserDataList = new VBarDataList();
      VBarDataList vBarResourceDataList = new VBarDataList();

      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) namedList
        .get("facets");
      vBarUserDataList = bizUtil
        .buildSummaryForTopCounts(jsonFacetResponse,"Repo","Users");
      vBarResourceDataList = bizUtil
        .buildSummaryForTopCounts(jsonFacetResponse,"y","x");
      String data = "";
      String summary = "";
      if ("text".equals(dataFormat)) {
        int users = 0;
        int resources = 0;
        summary += "\n\n\n\n";
        data += addBlank("Users") + "Components/Access" + "\n";
        data += "--------------------------------------------------------------------------\n";
        Collection<VBarGraphData> tableUserData = vBarUserDataList
          .getGraphData();
        for (VBarGraphData graphData : tableUserData) {
          String userName = graphData.getName();
          String largeUserName = "";

          if (userName.length() > 45) {
            largeUserName = userName.substring(0, 45);
            data += addBlank(largeUserName);
          } else
            data += addBlank(userName);

          Collection<VNameValue> vnameValueList = graphData
            .getDataCount();
          int count = 0;
          String blank = "";
          for (VNameValue vNameValue : vnameValueList) {
            data += blank + vNameValue.getName() + " "
              + vNameValue.getValue() + "\n";
            if (count == 0)
              blank = addBlank(blank);
            count++;

          }
          while (largeUserName.length() > 0) {
            data += largeUserName.substring(0, 45) + "\n";
          }

          users += 1;
        }
        data += "\n\n\n\n\n\n";
        data += addBlank("Resources") + "Components/Access" + "\n";
        data += "--------------------------------------------------------------------------\n";
        Collection<VBarGraphData> tableResourceData = vBarResourceDataList
          .getGraphData();
        for (VBarGraphData graphData : tableResourceData) {
          String resourceName = graphData.getName();
          String largeResourceName = resourceName;
          if (largeResourceName.length() > 45) {
            resourceName = largeResourceName.substring(0, 45);
            largeResourceName = largeResourceName.substring(45, largeResourceName.length());
          } else {
            largeResourceName = "";
          }

          //resourceName = resourceName.replaceAll("(.{45})", resourceName.substring(0, 45)+"\n");
          data += addBlank(resourceName);
          Collection<VNameValue> vnameValueList = graphData
            .getDataCount();
          int count = 0;
          String blank = "";
          for (VNameValue vNameValue : vnameValueList) {
            data += blank + vNameValue.getName() + " "
              + vNameValue.getValue() + "\n";
            if (count == 0)
              blank = addBlank(blank);
            count++;
          }
          String tempLargeResourceName = largeResourceName;
          while (largeResourceName.length() > 45) {
            largeResourceName = tempLargeResourceName.substring(0, 45);
            tempLargeResourceName = tempLargeResourceName.substring(45, tempLargeResourceName.length());
            data += largeResourceName + "\n";
          }
          if (largeResourceName.length() < 45 && !largeResourceName.isEmpty()) {
            data += largeResourceName + "\n";
          }
          resources += 1;
        }
        String header = "--------------------------------SUMMARY-----------------------------------\n";
        summary = header + "Users  = " + users + "\nResources  = " + resources + "\n" + summary;
        data = summary + data;
      } else {
        data = "{" + convertObjToString(vBarUserDataList) + "," + convertObjToString(vBarResourceDataList) + "}";
        dataFormat = "json";
      }
      String fileName = "Users_Resource" + startTime + endTime
        + ".";
      File file = File.createTempFile(fileName, dataFormat);

      FileOutputStream fis = new FileOutputStream(file);
      fis.write(data.getBytes());
      return Response
        .ok(file, MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition",
          "attachment;filename=" + fileName + dataFormat)
        .build();

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error("Error during solrQuery=" + e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  private String addBlank(String field) {
    int blanks = 50;
    int strSize = field.length();
    String fieldWithBlank = field;
    for (int i = 0; i < blanks - strSize; i++) {
      fieldWithBlank += " ";
    }
    return fieldWithBlank;
  }

  public String getServiceLoad(SearchCriteria searchCriteria) {
    VBarDataList dataList = new VBarDataList();
    Collection<VBarGraphData> vaDatas = new ArrayList<VBarGraphData>();
    dataList.setGraphData(vaDatas);

    SolrQuery serivceLoadQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);

    try {
      queryGenerator.setFacetField(serivceLoadQuery,
        LogSearchConstants.AUDIT_COMPONENT);
      QueryResponse serviceLoadResponse = auditSolrDao
        .process(serivceLoadQuery);
      if (serviceLoadResponse == null){
        return convertObjToString(dataList);
      }
      FacetField serviceFacetField =serviceLoadResponse.getFacetField(
          LogSearchConstants.AUDIT_COMPONENT);
      if(serviceFacetField == null){
        return convertObjToString(dataList);
      }
      
      List<Count> serviceLoadFacets = serviceFacetField.getValues();
      if(serviceLoadFacets == null){
        return convertObjToString(dataList);
      }
      for (Count cnt : serviceLoadFacets) {
        List<VNameValue> valueList = new ArrayList<VNameValue>();
        VBarGraphData vBarGraphData = new VBarGraphData();
        vaDatas.add(vBarGraphData);
        VNameValue vNameValue = new VNameValue();
        vNameValue.setName(cnt.getName());
        vBarGraphData.setName(cnt.getName().toUpperCase());
        vNameValue.setValue("" + cnt.getCount());
        valueList.add(vNameValue);
        vBarGraphData.setDataCounts(valueList);
      }


      return convertObjToString(dataList);

    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + e);
      throw restErrorUtil.createRESTException(MessageEnums.SOLR_ERROR
          .getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
    
  /*  
    String preDefinedJSON = getHadoopServiceConfigJSON();
    try {
      JSONObject serviceJSON = new JSONObject(preDefinedJSON).getJSONObject("service");
      HashMap<String, Object> serviceMap = jsonUtil.jsonToMapObject(serviceJSON.toString());
      Iterator<Entry<String, Object>> serviceMapIterator= serviceMap.entrySet().iterator();
      List<VNameValue> newValueList = new ArrayList<VNameValue>();
      for (VNameValue vNameValue : valueList) {
        String name=vNameValue.getName();
        while (serviceMapIterator.hasNext()) {
          Map.Entry<String, Object> tempMap = serviceMapIterator
              .next();
          
          String keyName = tempMap.getKey();
          
          JSONObject valueObj = new JSONObject(tempMap.toString().replace(keyName+"=", ""));
          if(name.contains(keyName.toLowerCase())){
            vNameValue.setName(valueObj.getString("label"));
            break;
          }
          JSONArray componentsArray = valueObj.getJSONArray("components");
          
          for(int i =0;i< componentsArray.length();i++){
            JSONObject jObj = componentsArray.getJSONObject(i);
            String jsonName = jObj.getString("name");
            if(name.contains(jsonName.toLowerCase())){
              vNameValue.setName(valueObj.getString("label"));
              break;
            }
          }
          
        }
        if(newValueList.isEmpty()){
          newValueList.add(vNameValue);
        }else{
          boolean isMatch = false;
          for(VNameValue vValue: newValueList){
            if(vValue.getName().equalsIgnoreCase(vNameValue.getName())){
              isMatch =true;
              Integer cnt1 = Integer.parseInt(vValue.getValue());
              Integer cnt2 = Integer.parseInt(vNameValue.getValue());
              vValue.setValue((cnt1+cnt2)+"");
            }
          }
          if(!isMatch)
            newValueList.add(vNameValue);
        }
      }
      vBarGraphData.setDataCounts(newValueList);
      vBarGraphData.setName("ServiceList");
      return convertObjToString(dataList);
      
    } catch (Exception e) {
      throw restErrorUtil.createRESTException(e.getMessage(),
          MessageEnums.ERROR_SYSTEM);
    }*/
  }
}
