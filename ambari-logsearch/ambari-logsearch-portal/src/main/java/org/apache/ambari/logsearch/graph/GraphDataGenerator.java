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
public class GraphDataGenerator extends GraphDataGeneratorBase {

  @Autowired
  StringUtil stringUtil;

  @Autowired
  QueryGeneration queryGenerator;

  @Autowired
  RESTErrorUtil restErrorUtil;

  @Autowired
  DateUtil dateUtil;

  private static Logger logger = Logger.getLogger(GraphDataGenerator.class);

  /**
   *
   * @param searchCriteria
   * @param solrDaoBase
   * @param solrQuery
   * @return
   */
  public VBarDataList getAnyGraphData(SearchCriteria searchCriteria,
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
    // add updated typeXAxis as a type parameter
    searchCriteria.addParam("type", typeXAxis);
    String fieldTime = (String) searchCriteria.getParamValue("fieldTime");
    // decide graph type based on user request parameter
    GRAPH_TYPE garphType = getGraphType(searchCriteria);
    switch (garphType) {
    case NORMAL_GRAPH:
      return normalGraph(xAxisField, yAxisField, from, to, solrDaoBase,
          typeXAxis, fieldTime, solrQuery);
    case RANGE_NON_STACK_GRAPH:
      return rangeNonStackGraph(xAxisField, yAxisField, from, to, unit,
          solrDaoBase, typeXAxis, fieldTime, solrQuery);
    case NON_RANGE_STACK_GRAPH:
      return nonRangeStackGraph(xAxisField, yAxisField, stackField, from, to,
          solrDaoBase, typeXAxis, fieldTime, solrQuery);
    case RANGE_STACK_GRAPH:
      return rangeStackGraph(xAxisField, yAxisField, stackField, from, to,
          unit, solrDaoBase, typeXAxis, fieldTime, solrQuery);
    default:
      logger.warn("Invalid graph type :" + garphType.name());
      return null;
    }
  }

  private GRAPH_TYPE getGraphType(SearchCriteria searchCriteria) {
    // default graph type is unknown
    GRAPH_TYPE graphType = GRAPH_TYPE.UNKNOWN;
    // X axis credentials
    String xAxisField = (String) searchCriteria.getParamValue("xAxis");
    String stackField = (String) searchCriteria.getParamValue("stackBy");
    String from = (String) searchCriteria.getParamValue("from");
    String to = (String) searchCriteria.getParamValue("to");
    String xType = (String) searchCriteria.getParamValue("type");
    if (xType != null) {
      // Y axis credentials
      String yAxisField = (String) searchCriteria.getParamValue("yAxis");
      if (stringUtil.isEmpty(xAxisField) || stringUtil.isEmpty(yAxisField)) {
        graphType = GRAPH_TYPE.UNKNOWN;
      } else if (stringUtil.isEmpty(stackField) && !stringUtil.isEmpty(to)
          && !stringUtil.isEmpty(from)
          && !(xType.contains("date") || xType.contains("time"))) {
        // Normal Graph Type
        graphType = GRAPH_TYPE.NORMAL_GRAPH;
      } else if (stringUtil.isEmpty(stackField) && !stringUtil.isEmpty(to)
          && !stringUtil.isEmpty(from)
          && (xType.contains("date") || xType.contains("time"))) {
        // Range(Non-Stack) Graph Type
        graphType = GRAPH_TYPE.RANGE_NON_STACK_GRAPH;
      } else if (!stringUtil.isEmpty(stackField) && !stringUtil.isEmpty(to)
          && !stringUtil.isEmpty(from)
          && !(xType.contains("date") || xType.contains("time"))) {
        // Non-Range Stack Graph Type
        graphType = GRAPH_TYPE.NON_RANGE_STACK_GRAPH;
      } else if (!stringUtil.isEmpty(stackField) && !stringUtil.isEmpty(to)
          && !stringUtil.isEmpty(from)
          && (xType.contains("date") || xType.contains("time"))) {
        // Range Stack GraphType
        graphType = GRAPH_TYPE.RANGE_STACK_GRAPH;
      }
    }
    return graphType;
  }

  @SuppressWarnings("unchecked")
  private VBarDataList normalGraph(String xAxisField, String yAxisField, String from,
      String to, SolrDaoBase solrDaoBase, String typeXAxis, String fieldTime,
      SolrQuery solrQuery) {
    VBarDataList dataList = new VBarDataList();
    Collection<VBarGraphData> vBarGraphDatas = new ArrayList<VBarGraphData>();
    VBarGraphData vBarGraphData = new VBarGraphData();
    Collection<VNameValue> vNameValues = new ArrayList<VNameValue>();
    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.setSingleIncludeFilter(solrQuery, fieldTime, "[" + from
        + " TO " + to + "]");
    if (typeXAxis.contains("string") || typeXAxis.contains("key_lower_case")
        || typeXAxis.contains("text")) {
      queryGenerator.setFacetField(solrQuery, xAxisField);
      try {
        QueryResponse response = solrDaoBase.process(solrQuery);
        if (response != null && response.getResults() != null) {
          long count = response.getResults().getNumFound();
          if (count > 0) {
            FacetField facetField = response.getFacetField(xAxisField);
            if (facetField != null) {
              List<Count> countValues = facetField.getValues();
              if (countValues != null) {
                for (Count countValue : countValues) {
                  if (countValue != null) {
                    VNameValue vNameValue = new VNameValue();
                    vNameValue.setName(countValue.getName());
                    vNameValue.setValue("" + countValue.getCount());
                    vNameValues.add(vNameValue);
                  }
                }
              }
              vBarGraphData.setName(xAxisField);
              vBarGraphDatas.add(vBarGraphData);
              dataList.setGraphData(vBarGraphDatas);
            }
          }
        }
        if (xAxisField.equalsIgnoreCase(LogSearchConstants.SOLR_LEVEL)) {
          Collection<VNameValue> sortedVNameValues = new ArrayList<VNameValue>();
          for (String level : LogSearchConstants.SUPPORTED_LOG_LEVEL) {
            VNameValue value = new VNameValue();
            value.setName(level);
            String val = "0";
            for (VNameValue valueLevel : vNameValues) {
              if (valueLevel.getName().equalsIgnoreCase(level)) {
                val = valueLevel.getValue();
                break;
              }
            }
            value.setValue(val);
            sortedVNameValues.add(value);
          }
          vBarGraphData.setDataCounts(sortedVNameValues);
        } else {
          vBarGraphData.setDataCounts(vNameValues);
        }
        return dataList;
      } catch (SolrException | SolrServerException | IOException e) {
        String query = solrQuery != null ? solrQuery.toQueryString() : "";
        logger.error("Got exception for solr query :" + query,
            e.getCause());
      }
    } else {
      queryGenerator.setRowCount(solrQuery, 0);
      String yAxis = yAxisField.contains("count") ? "sum" : yAxisField;
      String jsonQuery = queryGenerator.buildJSONFacetAggregatedFuncitonQuery(
          yAxis, xAxisField);
      queryGenerator.setJSONFacet(solrQuery, jsonQuery);
      try {
        QueryResponse response = solrDaoBase.process(solrQuery);
        SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
            .getResponse().get("facets");
        if (jsonFacetResponse.toString().equals("{count=0}")){
          return dataList;
        }
        VNameValue value = new VNameValue();
        String sum = (String) jsonFacetResponse.getVal(1);
        value.setName(xAxisField);
        value.setValue(sum != null ? sum.substring(0, sum.indexOf(".")) : "");
        vNameValues.add(value);
        vBarGraphData.setDataCounts(vNameValues);
        vBarGraphData.setName(xAxisField);
        vBarGraphDatas.add(vBarGraphData);
        dataList.setGraphData(vBarGraphDatas);
        return dataList;
      } catch (SolrException | SolrServerException | IOException e) {
        String query = solrQuery != null ? solrQuery.toQueryString() : "";
        logger.error("Got exception for solr query :" + query,
            e.getCause());
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private VBarDataList nonRangeStackGraph(String xAxisField, String yAxisField,
      String stackField, String from, String to, SolrDaoBase solrDaoBase,
      String typeXAxis, String fieldTime, SolrQuery solrQuery) {
    VBarDataList dataList = new VBarDataList();
    Collection<VBarGraphData> vGraphData = new ArrayList<VBarGraphData>();
    String mainQuery = queryGenerator.buildInclusiveRangeFilterQuery(fieldTime,
        from, to);
    queryGenerator.setMainQuery(solrQuery, mainQuery);
    queryGenerator.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
    String jsonQuery = "";
    if (isTypeNumber(typeXAxis)) {
      String function = (yAxisField.contains("count")) ? "sum" : yAxisField;
      jsonQuery = queryGenerator.buidlJSONFacetRangeQueryForNumber(stackField,
          xAxisField, function);
    } else {
      jsonQuery = queryGenerator.buildJsonFacetTermsRangeQuery(stackField,
          xAxisField);
    }
    try {
      queryGenerator.setJSONFacet(solrQuery, jsonQuery);
      dataList.setGraphData(vGraphData);
      QueryResponse response = solrDaoBase.process(solrQuery);
      if (response == null) {
        response = new QueryResponse();
      }
      Long count = response.getResults().getNumFound();
      if (count <= 0) {
        return dataList;
      }
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
          .getResponse().get("facets");
      if (jsonFacetResponse == null
          || jsonFacetResponse.toString().equals("{count=0}")) {
        return dataList;
      }
      extractNonRangeStackValuesFromBucket(jsonFacetResponse, stackField,
          vGraphData, typeXAxis);
      if (LogSearchConstants.SOLR_LEVEL.equalsIgnoreCase(stackField)
          && LogSearchConstants.SOLR_LEVEL.equalsIgnoreCase(xAxisField)) {
        Collection<VBarGraphData> levelVGraphData = dataList.getGraphData();
        for (VBarGraphData garphData : levelVGraphData) {
          Collection<VNameValue> valueList = garphData.getDataCount();
          Collection<VNameValue> valueListSorted = new ArrayList<VNameValue>();
          for (String level : LogSearchConstants.SUPPORTED_LOG_LEVEL) {
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
      return dataList;
    } catch (SolrException | IOException | SolrServerException e) {
      String query = solrQuery != null ? solrQuery.toQueryString() : "";
      logger.error("Got exception for solr query :" + query,
          e.getCause());
      throw restErrorUtil.createRESTException(MessageEnums.DATA_NOT_FOUND
          .getMessage().getMessage(), MessageEnums.DATA_NOT_FOUND);
    }
  }

  @SuppressWarnings("unchecked")
  private VBarDataList rangeNonStackGraph(String xAxisField, String yAxisField,
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
        queryGenerator.setFacetRange(solrQuery, xAxisField, from, to, unit);
        QueryResponse response = solrDaoBase.process(solrQuery);
        if (response != null) {
          Long count = response.getResults().getNumFound();
          if (count > 0) {
            @SuppressWarnings("rawtypes")
            List<RangeFacet> rangeFacet = response.getFacetRanges();
            if (rangeFacet != null && rangeFacet.size() > 0) {
              List<RangeFacet.Count> listCount = rangeFacet.get(0).getCounts();
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
          }
        }
        return dataList;
      } catch (SolrException | SolrServerException | IOException e) {
        logger.error("Got exception for solr query :" + solrQuery,
            e.getCause());
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private VBarDataList rangeStackGraph(String xAxisField, String yAxisField,
      String stackField, String from, String to, String unit,
      SolrDaoBase solrDaoBase, String typeXAxis, String fieldTime,
      SolrQuery solrQuery) {
    VBarDataList dataList = new VBarDataList();
    List<VBarGraphData> histogramData = new ArrayList<VBarGraphData>();
    queryGenerator.setMainQuery(solrQuery, null);
    queryGenerator.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
    String jsonHistogramQuery = queryGenerator
        .buildJSONFacetTermTimeRangeQuery(stackField, xAxisField, from, to,
            unit).replace("\\", "");
    try {
      solrQuery.set("json.facet", jsonHistogramQuery);
      queryGenerator.setRowCount(solrQuery, 0);
      QueryResponse response = solrDaoBase.process(solrQuery);
      if (response != null) {
        SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response
            .getResponse().get("facets");
        if (jsonFacetResponse == null
            || jsonFacetResponse.toString().equals("{count=0}")) {
          // return
          return dataList;
        }
        extractRangeStackValuesFromBucket(jsonFacetResponse, "x", "y",
            histogramData);
        dataList.setGraphData(histogramData);
      }
      return dataList;
    } catch (SolrException | IOException | SolrServerException e) {
      logger.error("Got exception for solr query :" + solrQuery,
          e.getCause());
    }
    return null;
  }
}
