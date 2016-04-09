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

package org.apache.ambari.logsearch.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.ambari.logsearch.query.QueryGeneration;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.StringUtil;
import org.apache.ambari.logsearch.view.VBarDataList;
import org.apache.ambari.logsearch.view.VBarGraphData;
import org.apache.ambari.logsearch.view.VNameValue;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GraphDataGnerator extends GraphDataGeneratorBase {

  @Autowired
  StringUtil stringUtil;

  @Autowired
  QueryGeneration queryGenerator;

  @Autowired
  RESTErrorUtil restErrorUtil;

  @Autowired
  DateUtil dateUtil;

  private static Logger logger = Logger.getLogger(GraphDataGnerator.class);

  public String getAnyGraphData(SearchCriteria searchCriteria,
                                SolrDaoBase solrDaoBase, SolrQuery solrQuery) {
    // X axis credentials
    String xAxisField = (String) searchCriteria.getParamValue("xAxis");
    String stackField = (String) searchCriteria.getParamValue("stackBy");
    String from = (String) searchCriteria.getParamValue("from");
    String to = (String) searchCriteria.getParamValue("to");
    String unit = (String) searchCriteria.getParamValue("unit");
    String suffix = (String) searchCriteria.getParamValue("suffix");
    String typeXAxis = ConfigUtil.schemaFieldsName.get(xAxisField + suffix);
    typeXAxis = (stringUtil.isEmpty(typeXAxis)) ? "string" : typeXAxis;

    // Y axis credentials
    String yAxisField = (String) searchCriteria.getParamValue("yAxis");

    searchCriteria.addParam("type", typeXAxis);
    String fieldTime = (String) searchCriteria.getParamValue("feildTime");

    int garphType = getGraphType(searchCriteria);

    switch (garphType) {
      case 1:
        return normalGraph(xAxisField, yAxisField, from, to, solrDaoBase,
          typeXAxis, fieldTime, solrQuery);
      case 2:
        return rangeNonStackGraph(xAxisField, yAxisField, from, to, unit,
          solrDaoBase, typeXAxis, fieldTime, solrQuery);
      case 3:
        return nonRangeStackGraph(xAxisField, yAxisField, stackField, from,
          to, solrDaoBase, typeXAxis, fieldTime, solrQuery);
      case 4:
        return rangeStackGraph(xAxisField, yAxisField, stackField, from,
          to, unit, solrDaoBase, typeXAxis, fieldTime, solrQuery);
      default:
        return null;
    }
  }

  private int getGraphType(SearchCriteria searchCriteria) {
    // X axis credentials
    String xAxisField = (String) searchCriteria.getParamValue("xAxis");
    String stackField = (String) searchCriteria.getParamValue("stackBy");
    String from = (String) searchCriteria.getParamValue("from");
    String to = (String) searchCriteria.getParamValue("to");
    String xType = (String) searchCriteria.getParamValue("type");

    if (xType == null)
      return 0;

    // Y axis credentials
    String yAxisField = (String) searchCriteria.getParamValue("yAxis");
    if (stringUtil.isEmpty(xAxisField) || stringUtil.isEmpty(yAxisField)) {
    }
    // Normal Graph Type
    else if (stringUtil.isEmpty(stackField) && !stringUtil.isEmpty(to)
      && !stringUtil.isEmpty(from)
      && !(xType.contains("date") || xType.contains("time")))
      return 1;
      // Range(Non-Stack) Graph Type
    else if (stringUtil.isEmpty(stackField) && !stringUtil.isEmpty(to)
      && !stringUtil.isEmpty(from)
      && (xType.contains("date") || xType.contains("time")))
      return 2;
      // Non-Range Stack Graph Type
    else if (!stringUtil.isEmpty(stackField) && !stringUtil.isEmpty(to)
      && !stringUtil.isEmpty(from)
      && !(xType.contains("date") || xType.contains("time")))
      return 3;
      // Range Stack GraphType
    else if (!stringUtil.isEmpty(stackField) && !stringUtil.isEmpty(to)
      && !stringUtil.isEmpty(from)
      && (xType.contains("date") || xType.contains("time")))
      return 4;

    return 0;
  }

  @SuppressWarnings("unchecked")
  private String normalGraph(String xAxisField, String yAxisField,
                             String from, String to, SolrDaoBase solrDaoBase, String typeXAxis,
                             String fieldTime, SolrQuery solrQuery) {
    VBarDataList dataList = new VBarDataList();
    Collection<VBarGraphData> vBarGraphDatas = new ArrayList<VBarGraphData>();
    VBarGraphData vBarGraphData = new VBarGraphData();
    Collection<VNameValue> vNameValues = new ArrayList<VNameValue>();

    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.setSingleIncludeFilter(solrQuery, fieldTime, "[" + from
      + " TO " + to + "]");
    if (typeXAxis.contains("string")
      || typeXAxis.contains("key_lower_case")
      || typeXAxis.contains("text")) {
      queryGenerator.setFacetField(solrQuery, xAxisField);
      try {
        QueryResponse response = solrDaoBase.process(solrQuery);
        Long count = response.getResults().getNumFound();

        if (response != null && count > 0) {
          FacetField facetField = response.getFacetField(xAxisField);
          if (facetField != null) {
            List<Count> countValues = facetField.getValues();
            for (Count cnt : countValues) {
              VNameValue vNameValue = new VNameValue();
              vNameValue.setName(cnt.getName());
              vNameValue.setValue("" + cnt.getCount());
              vNameValues.add(vNameValue);
            }
            vBarGraphData.setDataCounts(vNameValues);
            vBarGraphData.setName(xAxisField);
            vBarGraphDatas.add(vBarGraphData);
            dataList.setGraphData(vBarGraphDatas);
          }
        }
        return convertObjToString(dataList);
      } catch (SolrException | SolrServerException | IOException e) {

      }
    } else {
      queryGenerator.setRowCount(solrQuery, 0);
      String yAxis = yAxisField.contains("count") ? "sum" : yAxisField;
      String jsonQuery = queryGenerator
        .buildJSONFacetAggregatedFuncitonQuery(yAxis,
          xAxisField);
      queryGenerator.setJSONFacet(solrQuery, jsonQuery);
      try {
        QueryResponse response = solrDaoBase.process(solrQuery);

        SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
          .getResponse().get("facets");
        if (jsonFacetResponse.toString().equals("{count=0}"))
          return convertObjToString(dataList);
        VNameValue value = new VNameValue();
        String sum = jsonFacetResponse.getVal(1).toString();
        value.setName(xAxisField);
        value.setValue(sum.substring(0, sum.indexOf(".")));

        vNameValues.add(value);
        vBarGraphData.setDataCounts(vNameValues);
        vBarGraphData.setName(xAxisField);
        vBarGraphDatas.add(vBarGraphData);
        dataList.setGraphData(vBarGraphDatas);
        return convertObjToString(dataList);
      } catch (SolrException | SolrServerException | IOException e) {

      }
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  private String nonRangeStackGraph(String xAxisField, String yAxisField,
                                    String stackField, String from, String to, SolrDaoBase solrDaoBase,
                                    String typeXAxis, String fieldTime, SolrQuery solrQuery) {
    VBarDataList dataList = new VBarDataList();
    Collection<VBarGraphData> vGraphData = new ArrayList<VBarGraphData>();

    String mainQuery = queryGenerator.buildInclusiveRangeFilterQuery(
      fieldTime, from, to);
    queryGenerator.setMainQuery(solrQuery, mainQuery);
    queryGenerator.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
    String jsonQuery = "";

    if (isTypeNumber(typeXAxis)) {
      String function = (yAxisField.contains("count")) ? "sum"
        : yAxisField;
      jsonQuery = queryGenerator.buidlJSONFacetRangeQueryForNumber(
        stackField, xAxisField, function);
    } else {
      jsonQuery = queryGenerator.buildJsonFacetTermsRangeQuery(
        stackField, xAxisField);
    }

    try {
      queryGenerator.setJSONFacet(solrQuery, jsonQuery);
      dataList.setGraphData(vGraphData);

      QueryResponse response = solrDaoBase.process(solrQuery);
      if (response == null) {
        response = new QueryResponse();
      }
      Long count = response.getResults().getNumFound();
      if (count <= 0)
        return convertObjToString(dataList);

      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
        .getResponse().get("facets");
      if (jsonFacetResponse == null
        || jsonFacetResponse.toString().equals("{count=0}")) {
        return convertObjToString(dataList);
      }

      extractNonRangeStackValuesFromBucket(jsonFacetResponse, stackField, vGraphData,
        typeXAxis);

      if (LogSearchConstants.SOLR_LEVEL.equalsIgnoreCase(stackField)
        && LogSearchConstants.SOLR_LEVEL
        .equalsIgnoreCase(xAxisField)) {
        Collection<VBarGraphData> levelVGraphData = dataList.getGraphData();
        List<String> logLevels = new ArrayList<String>();

        logLevels.add(LogSearchConstants.FATAL);
        logLevels.add(LogSearchConstants.ERROR);
        logLevels.add(LogSearchConstants.WARN);
        logLevels.add(LogSearchConstants.INFO);
        logLevels.add(LogSearchConstants.DEBUG);
        logLevels.add(LogSearchConstants.TRACE);

        for (VBarGraphData garphData : levelVGraphData) {
          Collection<VNameValue> valueList = garphData.getDataCount();
          Collection<VNameValue> valueListSorted = new ArrayList<VNameValue>();
          for (String level : logLevels) {
            String val = "0";
            for (VNameValue value : valueList) {
              if (value.getName().equalsIgnoreCase(level)) {
                val = value.getValue();
                break;
              }
            }
            VNameValue v1 = new VNameValue();
            v1.setName(level.toUpperCase());
            v1.setValue(val);
            valueListSorted.add(v1);
          }
          garphData.setDataCounts(valueListSorted);
        }
      }

      return convertObjToString(dataList);
    } catch (SolrException | IOException | SolrServerException e) {
      logger.error(e);
      throw restErrorUtil.createRESTException(e.getMessage(),
        MessageEnums.DATA_NOT_FOUND);
    }

  }

  @SuppressWarnings("unchecked")
  private String rangeNonStackGraph(String xAxisField, String yAxisField,
                                    String from, String to, String unit, SolrDaoBase solrDaoBase,
                                    String typeXAxis, String fieldTime, SolrQuery solrQuery) {
    VBarDataList dataList = new VBarDataList();
    Collection<VBarGraphData> vBarGraphDatas = new ArrayList<VBarGraphData>();
    VBarGraphData vBarGraphData = new VBarGraphData();
    Collection<VNameValue> vNameValues = new ArrayList<VNameValue>();

    queryGenerator.setMainQuery(solrQuery, null);

    if (isTypeNumber(typeXAxis)) {
      queryGenerator.setSingleRangeFilter(solrQuery, fieldTime, from, to);
      return normalGraph(xAxisField, yAxisField, from, to, solrDaoBase,
        typeXAxis, fieldTime, solrQuery);
    } else {
      try {
        queryGenerator.setFacetRange(solrQuery, xAxisField, from, to,
          unit);
        QueryResponse response = solrDaoBase.process(solrQuery);
        if (response == null)
          response = new QueryResponse();
        Long count = response.getResults().getNumFound();
        if (count > 0) {

          @SuppressWarnings("rawtypes")
          List<RangeFacet> rangeFacet = response.getFacetRanges();
          if (rangeFacet == null)
            return convertObjToString(dataList);

          List<RangeFacet.Count> listCount = rangeFacet.get(0)
            .getCounts();
          if (listCount != null) {
            for (RangeFacet.Count cnt : listCount) {
              VNameValue vNameValue = new VNameValue();
              vNameValue.setName(cnt.getValue());
              vNameValue.setValue("" + cnt.getCount());
              vNameValues.add(vNameValue);
            }
            vBarGraphData.setDataCounts(vNameValues);
            vBarGraphDatas.add(vBarGraphData);
            vBarGraphData.setName(xAxisField);
            dataList.setGraphData(vBarGraphDatas);
          }
        }
        return convertObjToString(dataList);
      } catch (SolrException | SolrServerException | IOException e) {

      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private String rangeStackGraph(String xAxisField, String yAxisField,
                                 String stackField, String from, String to, String unit,
                                 SolrDaoBase solrDaoBase, String typeXAxis, String fieldTime, SolrQuery solrQuery) {
    VBarDataList dataList = new VBarDataList();
    List<VBarGraphData> histogramData = new ArrayList<VBarGraphData>();

    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);

    String jsonHistogramQuery = queryGenerator
      .buildJSONFacetTermTimeRangeQuery(stackField, xAxisField, from,
        to, unit).replace("\\", "");

    try {
      solrQuery.set("json.facet", jsonHistogramQuery);
      queryGenerator.setRowCount(solrQuery, 0);
      QueryResponse response = solrDaoBase.process(solrQuery);
      if (response == null)
        response = new QueryResponse();

      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
        .getResponse().get("facets");

      if (jsonFacetResponse == null
        || jsonFacetResponse.toString().equals("{count=0}"))
        return convertObjToString(dataList);

      extractRangeStackValuesFromBucket(jsonFacetResponse, "x", "y", histogramData);

      dataList.setGraphData(histogramData);
      return convertObjToString(dataList);

    } catch (SolrException | IOException | SolrServerException e) {
    }

    return null;
  }
}
