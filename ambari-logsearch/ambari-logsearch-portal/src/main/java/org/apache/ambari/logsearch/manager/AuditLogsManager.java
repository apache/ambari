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

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.ManageStartEndTime;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.conf.SolrAuditLogPropsConfig;
import org.apache.ambari.logsearch.dao.AuditSolrDao;
import org.apache.ambari.logsearch.graph.GraphDataGenerator;
import org.apache.ambari.logsearch.model.response.AuditLogResponse;
import org.apache.ambari.logsearch.model.response.BarGraphData;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.response.GroupListResponse;
import org.apache.ambari.logsearch.model.response.LogData;
import org.apache.ambari.logsearch.model.response.LogSearchResponse;
import org.apache.ambari.logsearch.model.response.NameValueData;
import org.apache.ambari.logsearch.model.response.NameValueDataListResponse;
import org.apache.ambari.logsearch.query.model.AnyGraphSearchCriteria;
import org.apache.ambari.logsearch.query.model.AuditBarGraphSearchCriteria;
import org.apache.ambari.logsearch.query.model.CommonSearchCriteria;
import org.apache.ambari.logsearch.query.model.FieldAuditBarGraphSearchCriteria;
import org.apache.ambari.logsearch.query.model.FieldAuditLogSearchCriteria;
import org.apache.ambari.logsearch.query.model.UserExportSearchCriteria;
import org.apache.ambari.logsearch.solr.model.SolrAuditLogData;
import org.apache.ambari.logsearch.solr.model.SolrComponentTypeLogData;
import org.apache.ambari.logsearch.util.BizUtil;
import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.ambari.logsearch.view.VResponse;
import org.apache.ambari.logsearch.query.model.AuditLogSearchCriteria;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.stereotype.Component;

@Component
public class AuditLogsManager extends ManagerBase<SolrAuditLogData, AuditLogResponse> {
  private static final Logger logger = Logger.getLogger(AuditLogsManager.class);

  @Inject
  private AuditSolrDao auditSolrDao;
  @Inject
  private GraphDataGenerator graphDataGenerator;
  @Inject
  private SolrAuditLogPropsConfig solrAuditLogPropsConfig;

  public AuditLogResponse getLogs(AuditLogSearchCriteria searchCriteria) {
    Boolean isLastPage = searchCriteria.isLastPage();
    if (isLastPage) {
      SolrQuery lastPageQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);
      LogSearchResponse logResponse = getLastPage(searchCriteria, LogSearchConstants.AUDIT_EVTTIME, auditSolrDao, lastPageQuery);
      if (logResponse == null) {
        logResponse = new AuditLogResponse();
      }
      return (AuditLogResponse) logResponse;
    }
    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);
    return getLogAsPaginationProvided(solrQuery, auditSolrDao);
  }

  private List<LogData> getComponents(CommonSearchCriteria searchCriteria) {
    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);
    List<LogData> docList = new ArrayList<>();
    try {
      SolrUtil.setFacetField(solrQuery, LogSearchConstants.AUDIT_COMPONENT);
      SolrUtil.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
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

      for (Count component : componentsCount) {
        SolrComponentTypeLogData logData = new SolrComponentTypeLogData();
        logData.setType(component.getName());
        docList.add(logData);
      }
      return docList;
    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public GroupListResponse getAuditComponents(CommonSearchCriteria searchCriteria) {
    GroupListResponse componentResponse = new GroupListResponse();
    List<LogData> docList = getComponents(searchCriteria);
    componentResponse.setGroupList(docList);
    return componentResponse;
  }

  @SuppressWarnings("unchecked")
  public BarGraphDataListResponse getAuditBarGraphData(AuditBarGraphSearchCriteria searchCriteria) {
    BarGraphDataListResponse dataList = new BarGraphDataListResponse();
    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);

    String from = getFrom(searchCriteria.getStartTime());
    String to = getTo(searchCriteria.getEndTime());
    String unit = getUnit(searchCriteria.getUnit());

    List<BarGraphData> histogramData = new ArrayList<BarGraphData>();
    String jsonHistogramQuery = queryGenerator.buildJSONFacetTermTimeRangeQuery(LogSearchConstants.AUDIT_COMPONENT,
      LogSearchConstants.AUDIT_EVTTIME, from, to, unit).replace("\\", "");

    return getGraphResponse(dataList, solrQuery, histogramData, jsonHistogramQuery);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public NameValueDataListResponse getLiveLogCounts() {
    NameValueDataListResponse nameValueList = new NameValueDataListResponse();
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setParam("event", "/audit/logs/live/count");
    try {
      Date[] timeRange = ManageStartEndTime.getStartEndTime();
      String startDate = DateUtil.convertGivenDateFormatToSolrDateFormat(timeRange[0]);
      String endDate = DateUtil.convertGivenDateFormatToSolrDateFormat(timeRange[1]);

      SolrUtil.setMainQuery(solrQuery, null);
      SolrUtil.setFacetRange(solrQuery, LogSearchConstants.AUDIT_EVTTIME, startDate, endDate, "+2MINUTE");
      List<RangeFacet.Count> listCount;

      QueryResponse response = auditSolrDao.process(solrQuery);

      List<RangeFacet> rangeFacet = response.getFacetRanges();
      if (rangeFacet == null) {
        return nameValueList;
      }
      RangeFacet range = rangeFacet.get(0);

      if (range == null) {
        return nameValueList;
      }

      listCount = range.getCounts();

      List<NameValueData> nameValues = new ArrayList<>();
      int count = 0;
      for (RangeFacet.Count cnt : listCount) {
        NameValueData nameValue = new NameValueData();
        nameValue.setName("" + count);
        nameValue.setValue("" + cnt.getCount());
        nameValues.add(nameValue);
        count++;
      }
      nameValueList.setvNameValues(nameValues);
      return nameValueList;

    } catch (SolrException | SolrServerException | ParseException
      | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public BarGraphDataListResponse topTenUsers(FieldAuditBarGraphSearchCriteria searchCriteria) {

    String jsonUserQuery =
      "{Users:{type:terms, field:reqUser, facet:{ Repo:{ type:terms, field:repo, facet:{eventCount:\"sum(event_count)\"}}}}}";
    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);
    SolrUtil.setJSONFacet(solrQuery, jsonUserQuery);
    SolrUtil.setRowCount(solrQuery, 0);
    try {
      BarGraphDataListResponse barGraphDataListResponse = new BarGraphDataListResponse();
      QueryResponse queryResponse = auditSolrDao.process(solrQuery);
      if (queryResponse == null) {
        return barGraphDataListResponse;
      }

      NamedList<Object> namedList = queryResponse.getResponse();

      if (namedList == null) {
        return barGraphDataListResponse;
      }

      @SuppressWarnings("unchecked")
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) namedList.get("facets");
      if (jsonFacetResponse == null) {
        return barGraphDataListResponse;
      }
      if (jsonFacetResponse.toString().equals("{count=0}")) {
        return barGraphDataListResponse;
      }
      barGraphDataListResponse = BizUtil.buildSummaryForTopCounts(jsonFacetResponse, "Repo", "Users");
      return barGraphDataListResponse;

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error("Error during solrQuery=" + e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  public BarGraphDataListResponse topTenResources(FieldAuditLogSearchCriteria searchCriteria) {

    String jsonUserQuery =
      "{Users:{type:terms,field:resource,facet:{Repo:{type:terms,field:repo,facet:{eventCount:\"sum(event_count)\"}}}}}";
    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);
    SolrUtil.setJSONFacet(solrQuery, jsonUserQuery);
    SolrUtil.setRowCount(solrQuery, 0);
    try {
      BarGraphDataListResponse barGraphDataListResponse = new BarGraphDataListResponse();
      QueryResponse queryResponse = auditSolrDao.process(solrQuery);
      if (queryResponse == null) {
        return barGraphDataListResponse;
      }

      NamedList<Object> namedList = queryResponse.getResponse();
      if (namedList == null) {
        return barGraphDataListResponse;
      }

      @SuppressWarnings("unchecked")
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) namedList.get("facets");

      barGraphDataListResponse = BizUtil.buildSummaryForTopCounts(jsonFacetResponse, "Repo", "Users");
      return barGraphDataListResponse;

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error("Error during solrQuery=" + solrQuery, e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  @SuppressWarnings("unchecked")
  public BarGraphDataListResponse getRequestUserLineGraph(FieldAuditBarGraphSearchCriteria searchCriteria) {

    String from = getFrom(searchCriteria.getStartTime());
    String to = getTo(searchCriteria.getEndTime());
    String unit = getUnit(searchCriteria.getUnit());

    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);

    BarGraphDataListResponse dataList = new BarGraphDataListResponse();
    List<BarGraphData> histogramData = new ArrayList<BarGraphData>();

    SolrUtil.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);

    String jsonHistogramQuery = queryGenerator.buildJSONFacetTermTimeRangeQuery(LogSearchConstants.AUDIT_REQUEST_USER,
      LogSearchConstants.AUDIT_EVTTIME, from, to, unit).replace("\\", "");

    return getGraphResponse(dataList, solrQuery, histogramData, jsonHistogramQuery);
  }

  public String getAuditLogsSchemaFieldsName() {
    return convertObjToString(auditSolrDao.schemaFieldNameMap);
  }

  public BarGraphDataListResponse getAnyGraphData(AnyGraphSearchCriteria searchCriteria) {
    searchCriteria.addParam("fieldTime", LogSearchConstants.AUDIT_EVTTIME);
    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);
    BarGraphDataListResponse result = graphDataGenerator.getAnyGraphData(searchCriteria, auditSolrDao, solrQuery);
    if (result == null) {
      result = new BarGraphDataListResponse();
    }
    return result;

  }

  @SuppressWarnings({"unchecked"})
  public Response exportUserTableToTextFile(UserExportSearchCriteria searchCriteria) {
    String jsonUserQuery =
      "{ Users: { type: terms, field: reqUser, facet:  {Repo: {  type: terms, field: repo, facet: {  eventCount: \"sum(event_count)\"}}}},x:{ type: terms,field: resource, facet: {y: {  type: terms, field: repo,facet: {  eventCount: \"sum(event_count)\"}}}}}";

    SolrQuery solrQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);
    String startTime = searchCriteria.getStartTime();
    String endTime = searchCriteria.getEndTime();

    startTime = startTime == null ? "" : startTime;
    endTime = endTime == null ? "" : "_" + endTime;

    SolrUtil.setJSONFacet(solrQuery, jsonUserQuery);
    SolrUtil.setRowCount(solrQuery, 0);

    String dataFormat = searchCriteria.getFormat();

    FileOutputStream fis = null;
    try {
      QueryResponse queryResponse = auditSolrDao.process(solrQuery);
      if (queryResponse == null) {
        VResponse response = new VResponse();
        response.setMsgDesc("Query was not able to execute " + solrQuery);
        throw RESTErrorUtil.createRESTException(response);
      }

      NamedList<Object> namedList = queryResponse.getResponse();
      if (namedList == null) {
        VResponse response = new VResponse();
        response.setMsgDesc("Query was not able to execute " + solrQuery);
        throw RESTErrorUtil.createRESTException(response);
      }
      BarGraphDataListResponse vBarUserDataList = new BarGraphDataListResponse();
      BarGraphDataListResponse vBarResourceDataList = new BarGraphDataListResponse();

      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) namedList.get("facets");
      vBarUserDataList = BizUtil.buildSummaryForTopCounts(jsonFacetResponse, "Repo", "Users");
      vBarResourceDataList = BizUtil.buildSummaryForTopCounts(jsonFacetResponse, "y", "x");
      String data = "";
      String summary = "";
      if ("text".equals(dataFormat)) {
        int users = 0;
        int resources = 0;
        summary += "\n\n\n\n";
        data += addBlank("Users") + "Components/Access" + "\n";
        data += "--------------------------------------------------------------------------\n";
        Collection<BarGraphData> tableUserData = vBarUserDataList.getGraphData();
        for (BarGraphData graphData : tableUserData) {
          String userName = graphData.getName();
          String largeUserName = "";

          if (userName.length() > 45) {
            largeUserName = userName.substring(0, 45);
            data += addBlank(largeUserName);
          } else
            data += addBlank(userName);

          Collection<NameValueData> vnameValueList = graphData.getDataCount();
          data = appendNameValueData(data, vnameValueList);
          while (largeUserName.length() > 0) {
            data += largeUserName.substring(0, 45) + "\n";
          }

          users += 1;
        }
        data += "\n\n\n\n\n\n";
        data += addBlank("Resources") + "Components/Access" + "\n";
        data += "--------------------------------------------------------------------------\n";
        Collection<BarGraphData> tableResourceData = vBarResourceDataList.getGraphData();
        for (BarGraphData graphData : tableResourceData) {
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
          Collection<NameValueData> vnameValueList = graphData.getDataCount();
          data = appendNameValueData(data, vnameValueList);
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
      String fileName = "Users_Resource" + startTime + endTime + ".";
      File file = File.createTempFile(fileName, dataFormat);

      fis = new FileOutputStream(file);
      fis.write(data.getBytes());
      return Response
        .ok(file, MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment;filename=" + fileName + dataFormat)
        .build();

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error("Error during solrQuery=" + e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
        }
      }
    }
  }

  private BarGraphDataListResponse getGraphResponse(BarGraphDataListResponse dataList, SolrQuery solrQuery,
                                                    List<BarGraphData> histogramData, String jsonHistogramQuery) {
    try {
      SolrUtil.setJSONFacet(solrQuery, jsonHistogramQuery);
      SolrUtil.setRowCount(solrQuery, 0);
      QueryResponse response = auditSolrDao.process(solrQuery);
      if (response == null) {
        return dataList;
      }
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response.getResponse().get("facets");

      if (jsonFacetResponse == null || jsonFacetResponse.toString().equals("{count=0}")) {
        return dataList;
      }

      auditSolrDao.extractValuesFromBuckets(jsonFacetResponse, "x", "y", histogramData);

      dataList.setGraphData(histogramData);
      return dataList;

    } catch (SolrServerException | SolrException | IOException e) {
      logger.error(e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);

    }
  }

  private String appendNameValueData(String data, Collection<NameValueData> vnameValueList) {
    int count = 0;
    String blank = "";
    for (NameValueData vNameValue : vnameValueList) {
      data += blank + vNameValue.getName() + " " + vNameValue.getValue() + "\n";
      if (count == 0)
        blank = addBlank(blank);
      count++;
    }
    return data;
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

  public BarGraphDataListResponse getServiceLoad(CommonSearchCriteria searchCriteria) {
    BarGraphDataListResponse dataList = new BarGraphDataListResponse();
    Collection<BarGraphData> vaDatas = new ArrayList<BarGraphData>();
    dataList.setGraphData(vaDatas);

    SolrQuery serivceLoadQuery = queryGenerator.commonAuditFilterQuery(searchCriteria);

    try {
      SolrUtil.setFacetField(serivceLoadQuery, LogSearchConstants.AUDIT_COMPONENT);
      QueryResponse serviceLoadResponse = auditSolrDao.process(serivceLoadQuery);
      if (serviceLoadResponse == null) {
        return dataList;
      }
      FacetField serviceFacetField = serviceLoadResponse.getFacetField(LogSearchConstants.AUDIT_COMPONENT);
      if (serviceFacetField == null) {
        return dataList;
      }

      List<Count> serviceLoadFacets = serviceFacetField.getValues();
      if (serviceLoadFacets == null) {
        return dataList;
      }
      for (Count cnt : serviceLoadFacets) {
        List<NameValueData> valueList = new ArrayList<NameValueData>();
        BarGraphData vBarGraphData = new BarGraphData();
        vaDatas.add(vBarGraphData);
        NameValueData vNameValue = new NameValueData();
        vNameValue.setName(cnt.getName());
        vBarGraphData.setName(cnt.getName().toUpperCase());
        vNameValue.setValue("" + cnt.getCount());
        valueList.add(vNameValue);
        vBarGraphData.setDataCount(valueList);
      }

      return dataList;

    } catch (SolrException | SolrServerException | IOException e) {
      logger.error("Error during solrQuery=" + e);
      throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
    }
  }

  @Override
  protected List<SolrAuditLogData> convertToSolrBeans(QueryResponse response) {
    return response.getBeans(SolrAuditLogData.class);
  }

  @Override
  protected AuditLogResponse createLogSearchResponse() {
    return new AuditLogResponse();
  }
}
