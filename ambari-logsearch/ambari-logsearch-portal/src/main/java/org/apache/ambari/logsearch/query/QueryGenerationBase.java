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

package org.apache.ambari.logsearch.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.QueryBase;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.ambari.logsearch.util.StringUtil;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class QueryGenerationBase extends QueryBase {

  static Logger logger = Logger.getLogger(QueryGenerationBase.class);

  @Autowired
  SolrUtil solrUtil;

  @Autowired
  StringUtil stringUtil;

  @Autowired
  JSONUtil jsonUtil;

  // SetMethods to apply to the query
  public void setFilterClauseForSolrSearchableString(SolrQuery solrQuery,
                                                     String commaSepratedString, String booleanOperator, String opr,
                                                     String messageField) {
    String operator = opr;
    String filterQuery = "";
    if (commaSepratedString != null && !commaSepratedString.isEmpty()) {

      String queryMsg = "";
      operator = operator == null ? "" : operator;
      String[] msgList = commaSepratedString
        .split(LogSearchConstants.I_E_SEPRATOR);
      int count = 0;
      for (String temp : msgList) {
        count += 1;

        queryMsg = queryMsg + " " + operator + messageField + ":"
          + solrUtil.makeSolrSearchString(temp);
        if (msgList.length > count)
          queryMsg = queryMsg + " " + booleanOperator + " ";
      }
      filterQuery = queryMsg;
      solrQuery.addFilterQuery(filterQuery);
      logger.debug("Filter added :- " + filterQuery);
    }
  }

  public void setFilterClauseWithFieldName(SolrQuery solrQuery,
                                           String commaSepratedString, String field, String operator,
                                           String condition) {
    if (commaSepratedString != null && !commaSepratedString.isEmpty()) {
      String[] arrayOfSepratedString = commaSepratedString.split(",");
      String filterQuery;
      if ("OR".equals(condition))
        filterQuery = solrUtil.orList(operator + field,
          arrayOfSepratedString, "");
      else
        filterQuery = solrUtil.andList(operator + field,
          arrayOfSepratedString, "");
      solrQuery.addFilterQuery(filterQuery);
      logger.debug("Filter added :- " + filterQuery);
    }
  }

  public void setSortOrderDefaultServiceLog(SolrQuery solrQuery,
                                            SearchCriteria searchCriteria) {
    List<SolrQuery.SortClause> defaultSort = new ArrayList<SolrQuery.SortClause>();
    if (searchCriteria.getSortBy() != null
      && (!searchCriteria.getSortBy().isEmpty())) {
      ORDER order = SolrQuery.ORDER.asc;
      if (searchCriteria.getSortType() != null
        && (!searchCriteria.getSortType().isEmpty())
        && !searchCriteria.getSortType().equalsIgnoreCase(
        order.toString())) {
        order = SolrQuery.ORDER.desc;
      }
      SolrQuery.SortClause logtimeSortClause = SolrQuery.SortClause
        .create(searchCriteria.getSortBy(), order);
      defaultSort.add(logtimeSortClause);
    } else {
      // by default sorting by logtime and sequence number in
      // Descending order
      SolrQuery.SortClause logtimeSortClause = SolrQuery.SortClause
        .create(LogSearchConstants.LOGTIME, SolrQuery.ORDER.desc);

      defaultSort.add(logtimeSortClause);

    }
    SolrQuery.SortClause sequenceNumberSortClause = SolrQuery.SortClause
      .create(LogSearchConstants.SEQUNCE_ID, SolrQuery.ORDER.desc);
    defaultSort.add(sequenceNumberSortClause);
    solrQuery.setSorts(defaultSort);
    logger.debug("Sort Order :-" + defaultSort);
  }

  public void setFilterFacetSort(SolrQuery solrQuery,
                                 SearchCriteria searchCriteria) {
    if (searchCriteria.getSortBy() != null
      && (!searchCriteria.getSortBy().isEmpty())) {
      solrQuery.setFacetSort(searchCriteria.getSortBy());
      logger.info("Sorted By :- " + searchCriteria.getSortBy());
    }
  }

  public void setSingleSortOrder(SolrQuery solrQuery,
                                 SearchCriteria searchCriteria) {
    List<SolrQuery.SortClause> sort = new ArrayList<SolrQuery.SortClause>();
    if (searchCriteria.getSortBy() != null
      && (!searchCriteria.getSortBy().isEmpty())) {
      ORDER order = SolrQuery.ORDER.asc;
      if (searchCriteria.getSortType() != null
        && (!searchCriteria.getSortType().isEmpty())
        && !searchCriteria.getSortType().equalsIgnoreCase(
        order.toString())) {
        order = SolrQuery.ORDER.desc;
      }
      SolrQuery.SortClause sortOrder = SolrQuery.SortClause.create(
        searchCriteria.getSortBy(), order);
      sort.add(sortOrder);
      solrQuery.setSorts(sort);
      logger.debug("Sort Order :-" + sort);
    }
  }

  // Search Criteria has parameter "sort" from it can get list of Sort Order
  // Example of list can be [logtime desc,seq_num desc]
  @SuppressWarnings("unchecked")
  public void setMultipleSortOrder(SolrQuery solrQuery,
                                   SearchCriteria searchCriteria) {
    List<SolrQuery.SortClause> sort = new ArrayList<SolrQuery.SortClause>();
    List<String> sortList = (List<String>) searchCriteria
      .getParamValue("sort");
    for (String sortOrder : sortList) {
      String sortByAndOrder[] = sortOrder.split(" ");
      ORDER order = sortByAndOrder[1].contains("asc") ? SolrQuery.ORDER.asc
        : SolrQuery.ORDER.desc;
      SolrQuery.SortClause sortOrder2 = SolrQuery.SortClause.create(
        sortByAndOrder[0], order);
      sort.add(sortOrder2);
      logger.debug("Sort Order :-" + sort);
    }
    solrQuery.setSorts(sort);
  }

  public void setSingleIncludeFilter(SolrQuery solrQuery, String filterType,
                                     String filterValue) {
    if (filterType != null && !filterType.isEmpty() && filterValue != null
      && !filterValue.isEmpty()) {
      String filterQuery = buildFilterQuery(filterType, filterValue);
      solrQuery.addFilterQuery(filterQuery);
      logger.debug("Filter added :- " + filterQuery);
    }
  }

  public void setSingleExcludeFilter(SolrQuery solrQuery, String filterType,
                                     String filterValue) {
    if (filterType != null && !filterType.isEmpty() && filterValue != null
      && !filterValue.isEmpty()) {
      String filterQuery = "-"
        + buildFilterQuery(filterType, filterValue);
      solrQuery.addFilterQuery(filterQuery);
      logger.debug("Filter added :- " + filterQuery);
    }
  }

  public void setSingleRangeFilter(SolrQuery solrQuery, String filterType,
                                   String filterFromValue, String filterToValue) {
    if (filterType != null && !filterType.isEmpty()
      && filterFromValue != null && !filterFromValue.isEmpty()
      && filterToValue != null && !filterToValue.isEmpty()) {
      String filterQuery = buildInclusiveRangeFilterQuery(filterType,
        filterFromValue, filterToValue);
      solrQuery.addFilterQuery(filterQuery);
      logger.debug("Filter added :- " + filterQuery);
    }
  }

  public void setPagination(SolrQuery solrQuery, SearchCriteria searchCriteria) {
    Integer startIndex = null;
    Integer maxRows = null;
    try {
      startIndex = (Integer) searchCriteria.getStartIndex();
      setStart(solrQuery, startIndex);
    } catch (ClassCastException e) {
      setStart(solrQuery, 0);
    }
    try {
      maxRows = (Integer) searchCriteria.getMaxRows();
      setRowCount(solrQuery, maxRows);
    } catch (ClassCastException e) {
      setRowCount(solrQuery, 10);
    }

    if (startIndex != null && maxRows != null)
      logger.info("Pagination was set from " + startIndex.intValue()
        + " to " + maxRows.intValue());
  }

  public void setSingleORFilter(SolrQuery solrQuery, String filterName1, String value1, String filterName2, String value2) {
    String filterQuery = filterName1 + ":" + value1 + " OR " + filterName2 + ":" + value2;
    solrQuery.setFilterQueries(filterQuery);
  }

  // BuildMethods to prepare a particular format as required for solr
  public String buildInclusiveRangeFilterQuery(String filterType,
                                               String filterFromValue, String filterToValue) {
    String filterQuery = filterType + ":[" + filterFromValue + " TO "
      + filterToValue + "]";
    logger.info("Build Filter was :- " + filterQuery);
    return filterQuery;
  }

  public String buildExclusiveRangeFilterQuery(String filterType,
                                               String filterFromValue, String filterToValue) {
    String filterQuery = filterType + ":{" + filterFromValue + " TO "
      + filterToValue + "}";
    logger.info("Build Filter was :- " + filterQuery);
    return filterQuery;
  }

  public String buildFilterQuery(String filterType, String filterValue) {
    String filterQuery = filterType + ":" + filterValue;
    logger.info("Build Filter Query was :- " + filterQuery);
    return filterQuery;
  }

  public String buildQueryFromJSONCompHost(String jsonHCNames,
                                           String selectedComponent) {
    String queryHostComponent = "";
    // Building and adding exclude string to filters
    String selectedCompQuery = "";

    if (selectedComponent != null && !selectedComponent.equals("")) {
      String[] selectedComponents = selectedComponent.split(",");
      selectedCompQuery = solrUtil.orList(LogSearchConstants.SOLR_COMPONENT, selectedComponents);

    }

    // Building Query of Host and Components from given json
    if (jsonHCNames != null && !jsonHCNames.equals("")
      && !jsonHCNames.equals("[]")) {

      try {
        JSONArray jarray = new JSONArray(jsonHCNames);
        int flagHost = 0;
        int flagComp;
        int count;
        for (int i = 0; i < jarray.length(); i++) {
          if (flagHost == 1)
            queryHostComponent = queryHostComponent + " OR ";
          JSONObject jsonObject = jarray.getJSONObject(i);
          String host = jsonObject.getString("h");
          queryHostComponent = queryHostComponent + "( host:" + host;
          List<String> components = JSONUtil.JSONToList(jsonObject
            .getJSONArray("c"));
          if (components.isEmpty())
            queryHostComponent = queryHostComponent + " AND ";

          flagComp = 0;
          count = 0;
          for (String comp : components) {
            if (flagComp == 0)
              queryHostComponent = queryHostComponent + " ( ";
            count += 1;
            queryHostComponent = queryHostComponent + " "
              + " type:" + comp;
            if (components.size() <= count)
              queryHostComponent = queryHostComponent + " ) ";
            else
              queryHostComponent = queryHostComponent + " OR ";
            flagComp = 1;
          }
          queryHostComponent = queryHostComponent + " ) ";
          flagHost = 1;
        }
      } catch (JSONException e) {
        logger.error(e);
      }
    }
    if (selectedCompQuery != null && !selectedCompQuery.equals("")) {
      if (queryHostComponent == null || queryHostComponent.equals(""))
        queryHostComponent = selectedCompQuery;
      else
        queryHostComponent = queryHostComponent + " OR "
          + selectedCompQuery;
    }
    return queryHostComponent;
  }

  // JSON BuildMethods

  /**
   * @param function , xAxisField
   * @return jsonString
   */
  public String buildJSONFacetAggregatedFuncitonQuery(String function,
                                                      String xAxisField) {
    return "{x:'" + function + "(" + xAxisField + ")'}";
  }

  /**
   * @param fieldName , fieldTime, from, to, unit
   * @return jsonString
   * @hierarchy Term, Time Range
   */
  public String buildJSONFacetTermTimeRangeQuery(String fieldName,
                                                 String fieldTime, String from, String to, String unit) {
    String query = "{";
    query += "x" + ":{type:terms,field:" + fieldName
      + ",facet:{y:{type:range,field:" + fieldTime + ",start:\""
      + from + "\",end:\"" + to + "\",gap:\"" + unit + "\"}}}";
    query += "}";
    logger.info("Build JSONQuery is :- " + query);
    return query;
  }

  /**
   * @param stackField , xAxisField
   * @return jsonString
   * @hierarchy Term, Range
   */
  public String buildJsonFacetTermsRangeQuery(String stackField,
                                              String xAxisField) {
    String jsonQuery = "{ " + stackField + ": { type: terms,field:"
      + stackField + "," + "facet: {   x: { type: terms, field:"
      + xAxisField + ",mincount:0,sort:{index:asc}}}}}";
    logger.info("Build JSONQuery is :- " + jsonQuery);
    return jsonQuery;
  }

  /**
   * @param stackField , xAxisField, function
   * @return
   * @hierarchy Term, Range
   */
  public String buidlJSONFacetRangeQueryForNumber(String stackField,
                                                  String xAxisField, String function) {
    String jsonQuery = "{ " + stackField + ": { type: terms,field:"
      + stackField + "," + "facet: {   x:'" + function + "("
      + xAxisField + ")'}}}}";
    logger.info("Build JSONQuery is :- " + jsonQuery);
    return jsonQuery;
  }

  /**
   * @param stackField , xAxisField, function
   * @return
   * @hierarchy Query, T
   */
  public String buidlJSONFacetRangeQueryForSuggestion(
    String originalFieldName, String valueToSuggest) {
    String jsonQuery = "{y:{type:query,query:\"" + originalFieldName + ":"
      + valueToSuggest + "\",facet:{x:{type:terms,field:"
      + originalFieldName + "}}}}";
    logger.info("Build JSONQuery is :- " + jsonQuery);
    return jsonQuery;
  }
}
