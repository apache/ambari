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
import org.apache.ambari.logsearch.model.response.BarGraphData;
import org.apache.ambari.logsearch.model.response.BarGraphDataListResponse;
import org.apache.ambari.logsearch.model.response.NameValueData;
import org.apache.ambari.logsearch.query.model.SearchCriteria;
import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.ambari.logsearch.query.QueryGeneration;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class GraphDataGenerator extends GraphDataGeneratorBase {

  private static final Logger logger = Logger.getLogger(GraphDataGenerator.class);

  @Inject
  private QueryGeneration queryGenerator;

  public BarGraphDataListResponse getAnyGraphData(SearchCriteria searchCriteria, SolrDaoBase solrDaoBase, SolrQuery solrQuery) {
    // X axis credentials
    String xAxisField = (String) searchCriteria.getParamValue("xAxis");
    String stackField = (String) searchCriteria.getParamValue("stackBy");
    String from = (String) searchCriteria.getParamValue("from");
    String to = (String) searchCriteria.getParamValue("to");
    String unit = (String) searchCriteria.getParamValue("unit");
    String typeXAxis = solrDaoBase.schemaFieldNameMap.get(xAxisField);
    typeXAxis = (StringUtils.isBlank(typeXAxis)) ? "string" : typeXAxis;

    // Y axis credentials
    String yAxisField = (String) searchCriteria.getParamValue("yAxis");
    // add updated typeXAxis as a type parameter
    searchCriteria.addParam("type", typeXAxis);
    String fieldTime = (String) searchCriteria.getParamValue("fieldTime");
    // decide graph type based on user request parameter
    GraphType garphType = getGraphType(searchCriteria);
    switch (garphType) {
    case NORMAL_GRAPH:
      return normalGraph(xAxisField, yAxisField, from, to, solrDaoBase, typeXAxis, fieldTime, solrQuery);
    case RANGE_NON_STACK_GRAPH:
      return rangeNonStackGraph(xAxisField, yAxisField, from, to, unit, solrDaoBase, typeXAxis, fieldTime, solrQuery);
    case NON_RANGE_STACK_GRAPH:
      return nonRangeStackGraph(xAxisField, yAxisField, stackField, from, to, solrDaoBase, typeXAxis, fieldTime, solrQuery);
    case RANGE_STACK_GRAPH:
      return rangeStackGraph(xAxisField, stackField, from, to, unit, solrDaoBase, solrQuery);
    default:
      logger.warn("Invalid graph type :" + garphType.name());
      return null;
    }
  }

  private GraphType getGraphType(SearchCriteria searchCriteria) {
    // default graph type is unknown
    GraphType graphType = GraphType.UNKNOWN;
    // X axis credentials
    String xAxisField = (String) searchCriteria.getParamValue("xAxis");
    String stackField = (String) searchCriteria.getParamValue("stackBy");
    String from = (String) searchCriteria.getParamValue("from");
    String to = (String) searchCriteria.getParamValue("to");
    String xType = (String) searchCriteria.getParamValue("type");
    if (xType != null) {
      // Y axis credentials
      String yAxisField = (String) searchCriteria.getParamValue("yAxis");
      if (StringUtils.isBlank(xAxisField) || StringUtils.isBlank(yAxisField)) {
        graphType = GraphType.UNKNOWN;
      } else if (StringUtils.isBlank(stackField) && !StringUtils.isBlank(to) && !StringUtils.isBlank(from)
          && !(xType.contains("date") || xType.contains("time"))) {
        graphType = GraphType.NORMAL_GRAPH;
      } else if (StringUtils.isBlank(stackField) && !StringUtils.isBlank(to) && !StringUtils.isBlank(from)
          && (xType.contains("date") || xType.contains("time"))) {
        graphType = GraphType.RANGE_NON_STACK_GRAPH;
      } else if (!StringUtils.isBlank(stackField) && !StringUtils.isBlank(to) && !StringUtils.isBlank(from)
          && !(xType.contains("date") || xType.contains("time"))) {
        graphType = GraphType.NON_RANGE_STACK_GRAPH;
      } else if (!StringUtils.isBlank(stackField) && !StringUtils.isBlank(to) && !StringUtils.isBlank(from)
          && (xType.contains("date") || xType.contains("time"))) {
        graphType = GraphType.RANGE_STACK_GRAPH;
      }
    }
    return graphType;
  }

  @SuppressWarnings("unchecked")
  private BarGraphDataListResponse normalGraph(String xAxisField, String yAxisField, String from, String to, SolrDaoBase solrDaoBase,
      String typeXAxis, String fieldTime, SolrQuery solrQuery) {
    BarGraphDataListResponse dataList = new BarGraphDataListResponse();
    Collection<BarGraphData> vBarGraphDatas = new ArrayList<BarGraphData>();
    BarGraphData vBarGraphData = new BarGraphData();
    Collection<NameValueData> vNameValues = new ArrayList<NameValueData>();
    SolrUtil.setMainQuery(solrQuery, null);
    queryGenerator.setSingleIncludeFilter(solrQuery, fieldTime, "[" + from + " TO " + to + "]");
    if (typeXAxis.contains("string") || typeXAxis.contains("key_lower_case") || typeXAxis.contains("text")) {
      SolrUtil.setFacetField(solrQuery, xAxisField);
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
                    NameValueData vNameValue = new NameValueData();
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
          Collection<NameValueData> sortedVNameValues = new ArrayList<NameValueData>();
          for (String level : LogSearchConstants.SUPPORTED_LOG_LEVEL) {
            NameValueData value = new NameValueData();
            value.setName(level);
            String val = "0";
            for (NameValueData valueLevel : vNameValues) {
              if (valueLevel.getName().equalsIgnoreCase(level)) {
                val = valueLevel.getValue();
                break;
              }
            }
            value.setValue(val);
            sortedVNameValues.add(value);
          }
          vBarGraphData.setDataCount(sortedVNameValues);
        } else {
          vBarGraphData.setDataCount(vNameValues);
        }
        return dataList;
      } catch (SolrException | SolrServerException | IOException e) {
        String query = solrQuery != null ? solrQuery.toQueryString() : "";
        logger.error("Got exception for solr query :" + query, e.getCause());
      }
    } else {
      SolrUtil.setRowCount(solrQuery, 0);
      String yAxis = yAxisField.contains("count") ? "sum" : yAxisField;
      String jsonQuery = queryGenerator.buildJSONFacetAggregatedFuncitonQuery(yAxis, xAxisField);
      SolrUtil.setJSONFacet(solrQuery, jsonQuery);
      try {
        QueryResponse response = solrDaoBase.process(solrQuery);
        SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response.getResponse().get("facets");
        if (jsonFacetResponse.toString().equals("{count=0}")) {
          return dataList;
        }
        NameValueData value = new NameValueData();
        String sum = (String) jsonFacetResponse.getVal(1);
        value.setName(xAxisField);
        value.setValue(sum != null ? sum.substring(0, sum.indexOf(".")) : "");
        vNameValues.add(value);
        vBarGraphData.setDataCount(vNameValues);
        vBarGraphData.setName(xAxisField);
        vBarGraphDatas.add(vBarGraphData);
        dataList.setGraphData(vBarGraphDatas);
        return dataList;
      } catch (SolrException | SolrServerException | IOException e) {
        String query = solrQuery != null ? solrQuery.toQueryString() : "";
        logger.error("Got exception for solr query :" + query, e.getCause());
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private BarGraphDataListResponse nonRangeStackGraph(String xAxisField, String yAxisField, String stackField, String from, String to,
      SolrDaoBase solrDaoBase, String typeXAxis, String fieldTime, SolrQuery solrQuery) {
    BarGraphDataListResponse dataList = new BarGraphDataListResponse();
    Collection<BarGraphData> vGraphData = new ArrayList<BarGraphData>();
    String mainQuery = queryGenerator.buildInclusiveRangeFilterQuery(fieldTime, from, to);
    SolrUtil.setMainQuery(solrQuery, mainQuery);
    SolrUtil.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
    String jsonQuery = "";
    if (SolrUtil.isSolrFieldNumber(typeXAxis,solrDaoBase)) {
      String function = (yAxisField.contains("count")) ? "sum" : yAxisField;
      jsonQuery = queryGenerator.buidlJSONFacetRangeQueryForNumber(stackField, xAxisField, function);
    } else {
      jsonQuery = queryGenerator.buildJsonFacetTermsRangeQuery(stackField, xAxisField);
    }
    try {
      SolrUtil.setJSONFacet(solrQuery, jsonQuery);
      dataList.setGraphData(vGraphData);
      QueryResponse response = solrDaoBase.process(solrQuery);
      if (response == null) {
        response = new QueryResponse();
      }
      Long count = response.getResults().getNumFound();
      if (count <= 0) {
        return dataList;
      }
      SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response.getResponse().get("facets");
      if (jsonFacetResponse == null || jsonFacetResponse.toString().equals("{count=0}")) {
        return dataList;
      }
      extractNonRangeStackValuesFromBucket(jsonFacetResponse, stackField, vGraphData, typeXAxis);
      if (LogSearchConstants.SOLR_LEVEL.equalsIgnoreCase(stackField) && LogSearchConstants.SOLR_LEVEL.equalsIgnoreCase(xAxisField)) {
        Collection<BarGraphData> levelVGraphData = dataList.getGraphData();
        for (BarGraphData graphData : levelVGraphData) {
          Collection<NameValueData> valueList = graphData.getDataCount();
          Collection<NameValueData> valueListSorted = new ArrayList<NameValueData>();
          for (String level : LogSearchConstants.SUPPORTED_LOG_LEVEL) {
            String val = "0";
            for (NameValueData value : valueList) {
              if (value.getName().equalsIgnoreCase(level)) {
                val = value.getValue();
                break;
              }
            }
            NameValueData v1 = new NameValueData();
            v1.setName(level.toUpperCase());
            v1.setValue(val);
            valueListSorted.add(v1);
          }
          graphData.setDataCount(valueListSorted);
        }
      }
      return dataList;
    } catch (SolrException | IOException | SolrServerException e) {
      String query = solrQuery != null ? solrQuery.toQueryString() : "";
      logger.error("Got exception for solr query :" + query, e.getCause());
      throw RESTErrorUtil.createRESTException(MessageEnums.DATA_NOT_FOUND.getMessage().getMessage(), MessageEnums.DATA_NOT_FOUND);
    }
  }

  @SuppressWarnings("unchecked")
  private BarGraphDataListResponse rangeNonStackGraph(String xAxisField, String yAxisField, String from, String to, String unit,
      SolrDaoBase solrDaoBase, String typeXAxis, String fieldTime, SolrQuery solrQuery) {
    BarGraphDataListResponse dataList = new BarGraphDataListResponse();
    Collection<BarGraphData> vBarGraphDatas = new ArrayList<BarGraphData>();
    BarGraphData vBarGraphData = new BarGraphData();
    Collection<NameValueData> vNameValues = new ArrayList<NameValueData>();
    SolrUtil.setMainQuery(solrQuery, null);
    if (SolrUtil.isSolrFieldNumber(typeXAxis,solrDaoBase)) {
      queryGenerator.setSingleRangeFilter(solrQuery, fieldTime, from, to);
      return normalGraph(xAxisField, yAxisField, from, to, solrDaoBase, typeXAxis, fieldTime, solrQuery);
    } else {
      try {
        SolrUtil.setFacetRange(solrQuery, xAxisField, from, to, unit);
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
                  NameValueData vNameValue = new NameValueData();
                  vNameValue.setName(cnt.getValue());
                  vNameValue.setValue("" + cnt.getCount());
                  vNameValues.add(vNameValue);
                }
                vBarGraphData.setDataCount(vNameValues);
                vBarGraphDatas.add(vBarGraphData);
                vBarGraphData.setName(xAxisField);
                dataList.setGraphData(vBarGraphDatas);
              }
            }
          }
        }
        return dataList;
      } catch (SolrException | SolrServerException | IOException e) {
        logger.error("Got exception for solr query :" + solrQuery, e.getCause());
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private BarGraphDataListResponse rangeStackGraph(String xAxisField, String stackField, String from, String to, String unit,
      SolrDaoBase solrDaoBase, SolrQuery solrQuery) {
    BarGraphDataListResponse dataList = new BarGraphDataListResponse();
    List<BarGraphData> histogramData = new ArrayList<BarGraphData>();
    SolrUtil.setMainQuery(solrQuery, null);
    SolrUtil.setFacetSort(solrQuery, LogSearchConstants.FACET_INDEX);
    String jsonHistogramQuery =
        queryGenerator.buildJSONFacetTermTimeRangeQuery(stackField, xAxisField, from, to, unit).replace("\\", "");
    try {
      solrQuery.set("json.facet", jsonHistogramQuery);
      SolrUtil.setRowCount(solrQuery, 0);
      QueryResponse response = solrDaoBase.process(solrQuery);
      if (response != null) {
        SimpleOrderedMap<Object> jsonFacetResponse = (SimpleOrderedMap<Object>) response.getResponse().get("facets");
        if (jsonFacetResponse == null || jsonFacetResponse.toString().equals("{count=0}")) {
          return dataList;
        }
        extractRangeStackValuesFromBucket(jsonFacetResponse, "x", "y", histogramData);
        dataList.setGraphData(histogramData);
      }
      return dataList;
    } catch (SolrException | IOException | SolrServerException e) {
      logger.error("Got exception for solr query :" + solrQuery, e.getCause());
    }
    return null;
  }
}
