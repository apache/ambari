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
import org.apache.ambari.logsearch.dao.AuditSolrDao;
import org.apache.ambari.logsearch.dao.ServiceLogsSolrDao;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.QueryBase;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public abstract class QueryGenerationBase extends QueryBase {

  private static final Logger logger = Logger.getLogger(QueryGenerationBase.class);

  @Autowired
  protected SolrUtil solrUtil;
  
  @Autowired
  protected AuditSolrDao auditSolrDao;
  
  @Autowired
  protected ServiceLogsSolrDao serviceLogsSolrDao;
  
  @Autowired
  protected JSONUtil jsonUtil;

  public static enum Condition {
    OR, AND
  }

  // SetMethods to apply to the query
  protected void setFilterClauseForSolrSearchableString(SolrQuery solrQuery, String commaSepratedString, Condition condition,
      String operator, String messageField) {
    String filterQuery = "";
    if (!StringUtils.isBlank(commaSepratedString)) {
      StringBuilder queryMsg = new StringBuilder();
      operator = (operator == null ? LogSearchConstants.NO_OPERATOR : operator);
      String[] msgList = commaSepratedString.split(LogSearchConstants.I_E_SEPRATOR);
      int count = 0;
      for (String temp : msgList) {
        count += 1;
        if (LogSearchConstants.SOLR_LOG_MESSAGE.equalsIgnoreCase(messageField)) {
          queryMsg.append(" " + operator + solrUtil.escapeForLogMessage(messageField, temp));
        } else {
          temp = solrUtil.escapeForStandardTokenizer(temp);
          if(temp.startsWith("\"") && temp.endsWith("\"")){
            temp = temp.substring(1);
            temp = temp.substring(0, temp.length()-1);
          }
          temp = "*" + temp + "*";
          queryMsg.append(" " + operator + messageField + ":" + temp);
        }
        if (msgList.length > count){
          queryMsg.append(" " + condition.name() + " ");
        }
      }
      filterQuery = queryMsg.toString();
      solrQuery.addFilterQuery(filterQuery);
      logger.debug("Filter added :- " + filterQuery);
    }
  }

  public void setFilterClauseWithFieldName(SolrQuery solrQuery, String commaSepratedString, String field, String operator,
      Condition condition) {
    if (!StringUtils.isBlank(commaSepratedString)) {
      String[] arrayOfSepratedString = commaSepratedString.split(LogSearchConstants.LIST_SEPARATOR);
      String filterQuery = null;
      if (Condition.OR.equals(condition)) {
        filterQuery = solrUtil.orList(operator + field, arrayOfSepratedString,"");
      } else if (Condition.AND.equals(condition)) {
        filterQuery = solrUtil.andList(operator + field, arrayOfSepratedString,"");
      }else{
        logger.warn("Not a valid condition :" + condition.name());
      }
      //add
      if (!StringUtils.isBlank(filterQuery)){
        solrQuery.addFilterQuery(filterQuery);
        logger.debug("Filter added :- " + filterQuery);
      }

    }
  }

  public void setSortOrderDefaultServiceLog(SolrQuery solrQuery, SearchCriteria searchCriteria) {
    List<SolrQuery.SortClause> defaultSort = new ArrayList<SolrQuery.SortClause>();
    if (!StringUtils.isBlank(searchCriteria.getSortBy())) {
      ORDER order = SolrQuery.ORDER.asc;
      if (!order.toString().equalsIgnoreCase(searchCriteria.getSortType())) {
        order = SolrQuery.ORDER.desc;
      }
      SolrQuery.SortClause logtimeSortClause = SolrQuery.SortClause.create(searchCriteria.getSortBy(), order);
      defaultSort.add(logtimeSortClause);
    } else {
      // by default sorting by logtime and sequence number in Descending order
      SolrQuery.SortClause logtimeSortClause = SolrQuery.SortClause.create(LogSearchConstants.LOGTIME, SolrQuery.ORDER.desc);
      defaultSort.add(logtimeSortClause);

    }
    SolrQuery.SortClause sequenceNumberSortClause = SolrQuery.SortClause.create(LogSearchConstants.SEQUNCE_ID, SolrQuery.ORDER.desc);
    defaultSort.add(sequenceNumberSortClause);
    solrQuery.setSorts(defaultSort);
    logger.debug("Sort Order :-" + defaultSort);
  }

  public void setFilterFacetSort(SolrQuery solrQuery, SearchCriteria searchCriteria) {
    if (!StringUtils.isBlank(searchCriteria.getSortBy())) {
      solrQuery.setFacetSort(searchCriteria.getSortBy());
      logger.info("Sorted By :- " + searchCriteria.getSortBy());
    }
  }

  public void setSingleSortOrder(SolrQuery solrQuery, SearchCriteria searchCriteria) {
    List<SolrQuery.SortClause> sort = new ArrayList<SolrQuery.SortClause>();
    if (!StringUtils.isBlank(searchCriteria.getSortBy())) {
      ORDER order = SolrQuery.ORDER.asc;
      if (!order.toString().equalsIgnoreCase(searchCriteria.getSortType())) {
        order = SolrQuery.ORDER.desc;
      }
      SolrQuery.SortClause sortOrder = SolrQuery.SortClause.create(searchCriteria.getSortBy(), order);
      sort.add(sortOrder);
      solrQuery.setSorts(sort);
      logger.debug("Sort Order :-" + sort);
    }
  }

  // Search Criteria has parameter "sort" from it can get list of Sort Order
  // Example of list can be [logtime desc,seq_num desc]
  @SuppressWarnings("unchecked")
  public void setMultipleSortOrder(SolrQuery solrQuery, SearchCriteria searchCriteria) {
    List<SolrQuery.SortClause> sort = new ArrayList<SolrQuery.SortClause>();
    List<String> sortList = (List<String>) searchCriteria.getParamValue("sort");
    if (sortList != null) {
      for (String sortOrder : sortList) {
        if (!StringUtils.isBlank(sortOrder)) {
          String sortByAndOrder[] = sortOrder.split(" ");
          if (sortByAndOrder.length > 1) {
            ORDER order = sortByAndOrder[1].contains("asc") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
            SolrQuery.SortClause solrSortClause = SolrQuery.SortClause.create(sortByAndOrder[0], order);
            sort.add(solrSortClause);
            logger.debug("Sort Order :-" + sort);
          } else {
            logger.warn("Not a valid sort Clause " + sortOrder);
          }
        }
      }
      solrQuery.setSorts(sort);
    }
  }

  public void setSingleIncludeFilter(SolrQuery solrQuery, String filterType, String filterValue) {
    if (!StringUtils.isBlank(filterType) && !StringUtils.isBlank(filterValue)) {
      String filterQuery = buildFilterQuery(filterType, filterValue);
      solrQuery.addFilterQuery(filterQuery);
      logger.debug("Filter added :- " + filterQuery);
    }
  }

  public void setSingleExcludeFilter(SolrQuery solrQuery, String filterType, String filterValue) {
    if (!StringUtils.isBlank(filterValue) && !StringUtils.isBlank(filterType)) {
      String filterQuery = LogSearchConstants.MINUS_OPERATOR + buildFilterQuery(filterType, filterValue);
      solrQuery.addFilterQuery(filterQuery);
      logger.debug("Filter added :- " + filterQuery);
    }
  }

  public void setSingleRangeFilter(SolrQuery solrQuery, String filterType, String filterFromValue, String filterToValue) {
    if (!StringUtils.isBlank(filterToValue) && !StringUtils.isBlank(filterType) && !StringUtils.isBlank(filterFromValue)) {
      String filterQuery = buildInclusiveRangeFilterQuery(filterType, filterFromValue, filterToValue);
      if (!StringUtils.isBlank(filterQuery)) {
        solrQuery.addFilterQuery(filterQuery);
        logger.debug("Filter added :- " + filterQuery);
      }
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
      logger.info("Pagination was set from " + startIndex.intValue() + " to " + maxRows.intValue());
  }

  public void setSingleORFilter(SolrQuery solrQuery, String filterName1, String value1, String filterName2, String value2) {
    String filterQuery = filterName1 + ":" + value1 + " " + Condition.OR.name() + " " + filterName2 + ":" + value2;
    solrQuery.setFilterQueries(filterQuery);
  }

  // BuildMethods to prepare a particular format as required for solr
  public String buildInclusiveRangeFilterQuery(String filterType, String filterFromValue, String filterToValue) {
    String filterQuery = filterType + ":[" + filterFromValue + " TO " + filterToValue + "]";
    logger.info("Build Filter was :- " + filterQuery);
    return filterQuery;
  }

  public String buildFilterQuery(String filterType, String filterValue) {
    String filterQuery = filterType + ":" + filterValue;
    logger.info("Build Filter Query was :- " + filterQuery);
    return filterQuery;
  }

  public String buildJSONFacetAggregatedFuncitonQuery(String function, String xAxisField) {
    return "{x:'" + function + "(" + xAxisField + ")'}";
  }

  public String buildJSONFacetTermTimeRangeQuery(String fieldName, String fieldTime, String from, String to, String unit) {
    String query = "{";
    query += "x" + ":{type:terms,field:" + fieldName + ",facet:{y:{type:range,field:" + fieldTime + ",start:\"" + from + "\",end:\"" + to + "\",gap:\"" + unit + "\"}}}";
    query += "}";
    logger.info("Build JSONQuery is :- " + query);
    return query;
  }

  public String buildJsonFacetTermsRangeQuery(String stackField, String xAxisField) {
    String jsonQuery = "{ " + stackField + ": { type: terms,field:" + stackField + "," + "facet: {   x: { type: terms, field:" + xAxisField + ",mincount:0,sort:{index:asc}}}}}";
    logger.info("Build JSONQuery is :- " + jsonQuery);
    return jsonQuery;
  }

  public String buidlJSONFacetRangeQueryForNumber(String stackField, String xAxisField, String function) {
    String jsonQuery = "{ " + stackField + ": { type: terms,field:" + stackField + "," + "facet: {   x:'" + function + "(" + xAxisField + ")'}}}}";
    logger.info("Build JSONQuery is :- " + jsonQuery);
    return jsonQuery;
  }

  private String buildListQuery(String paramValue, String solrFieldName, Condition condition) {
    if (!StringUtils.isBlank(paramValue)) {
      String[] values = paramValue.split(LogSearchConstants.LIST_SEPARATOR);
      switch (condition) {
      case OR:
        return solrUtil.orList(solrFieldName, values,"");
      case AND:
        return solrUtil.andList(solrFieldName, values, "");
      default:
        logger.error("Invalid condition " + condition.name());
      }
    }
    return "";
  }

  protected void addFilterQueryFromArray(SolrQuery solrQuery, String jsonArrStr, String solrFieldName, Condition condition) {
    if (!StringUtils.isBlank(jsonArrStr) && condition != null && solrQuery != null) {
      Gson gson = new Gson();
      String[] arr = null;
      try {
        arr = gson.fromJson(jsonArrStr, String[].class);
      } catch (Exception exception) {
        logger.error("Invaild json array:" + jsonArrStr);
        return;
      }
      String query;;
      switch (condition) {
      case OR:
        query = solrUtil.orList(solrFieldName, arr,"");
        break;
      case AND:
        query = solrUtil.andList(solrFieldName, arr, "");
        break;
      default:
        query=null;
        logger.error("Invalid condition :" + condition.name());
      }
      if (!StringUtils.isBlank(query)) {
        solrQuery.addFilterQuery(query);
      }
    }
  }

  protected void addFilter(SolrQuery solrQuery, String paramValue, String solrFieldName, Condition condition) {
    String filterQuery = buildListQuery(paramValue, solrFieldName, condition);
    if (!StringUtils.isBlank(filterQuery)) {
      if (solrQuery != null) {
        solrQuery.addFilterQuery(filterQuery);
      }
    }
  }
}
