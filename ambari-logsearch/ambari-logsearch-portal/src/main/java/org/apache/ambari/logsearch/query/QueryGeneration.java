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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.SearchCriteria;
import org.apache.ambari.logsearch.util.BizUtil;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.ambari.logsearch.util.StringUtil;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QueryGeneration extends QueryGenerationBase {

  static Logger logger = Logger.getLogger(QueryGeneration.class);

  @Autowired
  SolrUtil solrUtil;

  @Autowired
  RESTErrorUtil restErrorUtil;

  @Autowired
  BizUtil bizUtil;

  @Autowired
  DateUtil dateUtil;

  @Autowired
  StringUtil stringUtil;

  @Autowired
  JSONUtil jsonUtil;

  public SolrQuery commonFilterQuery(SearchCriteria searchCriteria) {

    SolrQuery solrQuery = new SolrQuery();

    String jsonHCNames = (String) searchCriteria
      .getParamValue("treeParams");
    String givenQuery = (String) searchCriteria.getParamValue("q");
    String level = (String) searchCriteria.getParamValue("level");

    String startTime = (String) searchCriteria.getParamValue("from");
    String endTime = (String) searchCriteria.getParamValue("to");
    String iMessage = (String) searchCriteria.getParamValue("iMessage");
    String eMessage = (String) searchCriteria.getParamValue("eMessage");
    String gEmessage = (String) searchCriteria.getParamValue("gEMessage");
    String selectedComp = (String) searchCriteria
      .getParamValue("selectComp");
    String bundleId = (String) searchCriteria
      .getParamValue(LogSearchConstants.BUNDLE_ID);
    String globalExcludeComp = (String) searchCriteria
      .getParamValue("gMustNot");
    String unselectedComp = (String) searchCriteria
      .getParamValue("unselectComp");
    String urlHostName = (String) searchCriteria.getParamValue("host_name");
    String urlComponents = (String) searchCriteria.getParamValue("components_name");

    String advQuery = (String) searchCriteria.getParamValue("advanceSearch");
    if (!stringUtil.isEmpty(advQuery)) {
      String advQueryParameters[] = advQuery.split(Pattern.quote("}{"));
      SolrQuery advSolrQuery = new SolrQuery();

      for (String queryParam : advQueryParameters) {
        String params[] = queryParam.split(Pattern.quote("="));
        advSolrQuery.setParam(params[0], params[1]);
      }

      // Building and adding levels to filters
      setFilterClauseWithFieldName(advSolrQuery, level,
        LogSearchConstants.SOLR_LEVEL, "", "OR");

      // Adding Logtime to filters
      setSingleRangeFilter(advSolrQuery, LogSearchConstants.LOGTIME,
        startTime, endTime);

      // Building and adding exlcude components to filters
      setFilterClauseWithFieldName(advSolrQuery, unselectedComp,
        LogSearchConstants.SOLR_COMPONENT,
        LogSearchConstants.MINUS_OPERATOR, "AND");

      // Building and adding exlcude components to filters
      setFilterClauseWithFieldName(advSolrQuery, selectedComp,
        LogSearchConstants.SOLR_COMPONENT,
        LogSearchConstants.NO_OPERATOR, "OR");

      // Set Pagination
      setPagination(advSolrQuery, searchCriteria);

      return advSolrQuery;
    }

    setMainQuery(solrQuery, givenQuery);

    // Adding Logtime to filters
    setSingleRangeFilter(solrQuery, LogSearchConstants.LOGTIME, startTime,
      endTime);

    String mainFilterQuery = buildQueryFromJSONCompHost(jsonHCNames, selectedComp);

    if (mainFilterQuery != null && !mainFilterQuery.equals(""))
      solrQuery.addFilterQuery(mainFilterQuery);

    // Building and adding levels to filters
    setFilterClauseWithFieldName(solrQuery, level, LogSearchConstants.SOLR_LEVEL, "", "OR");

    // Building and adding include string to filters
    setFilterClauseForSolrSearchableString(solrQuery, iMessage, "OR", "",
      LogSearchConstants.SOLR_LOG_MESSAGE);

    // Building and adding global exclude string to filters
    setFilterClauseForSolrSearchableString(solrQuery, gEmessage, "AND",
      LogSearchConstants.MINUS_OPERATOR,
      LogSearchConstants.SOLR_LOG_MESSAGE);

    // Building and adding exclude string to filter
    setFilterClauseForSolrSearchableString(solrQuery, eMessage, "AND",
      LogSearchConstants.MINUS_OPERATOR,
      LogSearchConstants.SOLR_LOG_MESSAGE);

    // Building and adding logfile to filters
    applyLogFileFilter(solrQuery, searchCriteria);

    // Building and adding exclude components to filters
    setFilterClauseWithFieldName(solrQuery, globalExcludeComp,
      LogSearchConstants.SOLR_COMPONENT,
      LogSearchConstants.MINUS_OPERATOR, "AND");

    // Building and adding exlcude components to filters
    setFilterClauseWithFieldName(solrQuery, unselectedComp,
      LogSearchConstants.SOLR_COMPONENT,
      LogSearchConstants.MINUS_OPERATOR, "AND");

    //Building and addding host names given url
    setFilterClauseWithFieldName(solrQuery, urlHostName,
      LogSearchConstants.SOLR_HOST,
      "", "OR");

    //Building and addding component names given url
    setFilterClauseWithFieldName(solrQuery, urlComponents,
      LogSearchConstants.SOLR_COMPONENT,
      "", "OR");

    // Set Pagination
    setPagination(solrQuery, searchCriteria);

    // SetSort type (by default Descending)
    setSortOrderDefaultServiceLog(solrQuery, searchCriteria);

    // Set Bundle Id
    setSingleIncludeFilter(solrQuery, LogSearchConstants.BUNDLE_ID, bundleId);

    this.setUserSpecificFilter(searchCriteria, solrQuery,
      LogSearchConstants.INCLUDE_QUERY,
      LogSearchConstants.INCLUDE_QUERY);

    this.setUserSpecificFilter(searchCriteria, solrQuery,
      LogSearchConstants.EXCLUDE_QUERY,
      LogSearchConstants.EXCLUDE_QUERY);
    return solrQuery;
  }

  public void applyLogFileFilter(SolrQuery solrQuery,
                                 SearchCriteria searchCriteria) {
    String hostLogFile = (String) searchCriteria
      .getParamValue("hostLogFile");
    String compLogFile = (String) searchCriteria
      .getParamValue("compLogFile");
    String givenQuery = (String) searchCriteria.getParamValue("q");
    String logfileQuery = "";
    if (hostLogFile != null && !hostLogFile.equals("")
      && compLogFile != null && !compLogFile.equals("")) {
      logfileQuery = "host:" + hostLogFile + " AND type:" + compLogFile;
      if (givenQuery != null && !givenQuery.equals(""))
        logfileQuery = "(" + givenQuery + ") AND (" + logfileQuery
          + ")";
      solrQuery.addFilterQuery(logfileQuery);
    }
  }

  public void setUserSpecificFilter(SearchCriteria searchCriteria,
                                    SolrQuery solrQuery, String paramName, String operation) {

    String queryString = (String) searchCriteria.getParamValue(paramName);
    String columnQuery = (String) searchCriteria
      .getParamValue(LogSearchConstants.COLUMN_QUERY);
    if (!stringUtil.isEmpty(queryString) && "[]".equals(queryString))
      queryString = null;
    if (!stringUtil.isEmpty(columnQuery) && stringUtil.isEmpty(queryString)
      && !paramName.equals(LogSearchConstants.EXCLUDE_QUERY))
      queryString = columnQuery;
    List<String> conditionQuries = new ArrayList<String>();
    List<String> referalConditionQuries = new ArrayList<String>();
    List<String> elments = new ArrayList<String>();
    if (!stringUtil.isEmpty(queryString)) {
      List<HashMap<String, Object>> queryList = jsonUtil
        .jsonToMapObjectList(queryString);
      if (!stringUtil.isEmpty(columnQuery)
        && !columnQuery.equals(queryString) && !paramName.equals(LogSearchConstants.EXCLUDE_QUERY)) {
        List<HashMap<String, Object>> columnQueryList = jsonUtil
          .jsonToMapObjectList(columnQuery);
        queryList.addAll(columnQueryList);
      }

      for (HashMap<String, Object> columnListMap : queryList) {
        String orQuery = "";
        String field = "";
        for (String key : columnListMap.keySet()) {
          String originalKey = getOriginalKey(key);
          String value = getOriginalValue(originalKey, ""
            + columnListMap.get(key));
          orQuery = originalKey + ":"
            + putWildCardByType(value, originalKey);

          boolean isSame = false;
          for (String temp : elments) {
            if (key.equals(temp))
              isSame = true;
          }
          if (isSame
            && !operation
            .equals(LogSearchConstants.EXCLUDE_QUERY)) {
            for (String tempCondition : conditionQuries) {
              if (tempCondition.contains(originalKey)) {
                String newCondtion = tempCondition + " OR "
                  + orQuery;
                referalConditionQuries.remove(tempCondition);
                referalConditionQuries.add(newCondtion);
              }
            }
            conditionQuries.removeAll(conditionQuries);
            conditionQuries.addAll(referalConditionQuries);
          } else {
            conditionQuries.add(orQuery);
            referalConditionQuries.add(orQuery);
          }

          field = key;
          elments.add(field);
        }

      }
    }
    if (!referalConditionQuries.isEmpty()) {
      if (operation.equals(LogSearchConstants.INCLUDE_QUERY)
        || operation.equals(LogSearchConstants.COLUMN_QUERY)) {
        for (String filter : referalConditionQuries)
          solrQuery.addFilterQuery(filter);
      } else if (operation.equals(LogSearchConstants.EXCLUDE_QUERY)) {

        for (String filter : referalConditionQuries) {
          filter = "-" + filter;
          solrQuery.addFilterQuery(filter);
        }
      }
    }
  }

  public SolrQuery commonAuditFilterQuery(SearchCriteria searchCriteria) {

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");

    String startTime = (String) searchCriteria.getParamValue("startTime");
    String endTime = (String) searchCriteria.getParamValue("endTime");
    String selectedComp = (String) searchCriteria
      .getParamValue("includeString");

    this.setFilterClauseWithFieldName(solrQuery, selectedComp,
      LogSearchConstants.AUDIT_COMPONENT,
      LogSearchConstants.NO_OPERATOR, "OR");

    String globalExcludeComp = (String) searchCriteria
      .getParamValue("gMustNot");

    this.setUserSpecificFilter(searchCriteria, solrQuery,
      LogSearchConstants.INCLUDE_QUERY,
      LogSearchConstants.INCLUDE_QUERY);

    this.setUserSpecificFilter(searchCriteria, solrQuery,
      LogSearchConstants.EXCLUDE_QUERY,
      LogSearchConstants.EXCLUDE_QUERY);

    String unselectedComp = (String) searchCriteria
      .getParamValue("unselectComp");

    this.setFilterClauseWithFieldName(solrQuery, globalExcludeComp,
      LogSearchConstants.AUDIT_COMPONENT,
      LogSearchConstants.MINUS_OPERATOR, "AND");

    // Building and adding exlcude components to filters
    this.setFilterClauseWithFieldName(solrQuery, unselectedComp,
      LogSearchConstants.AUDIT_COMPONENT,
      LogSearchConstants.MINUS_OPERATOR, "AND");

    // Adding Logtime to filters
    this.setSingleRangeFilter(solrQuery, LogSearchConstants.AUDIT_EVTTIME,
      startTime, endTime);

    this.setPagination(solrQuery, searchCriteria);
    try {
      if (searchCriteria.getSortBy().isEmpty()) {
        searchCriteria.setSortBy(LogSearchConstants.AUDIT_EVTTIME);
        searchCriteria.setSortType(SolrQuery.ORDER.desc.toString());
      }
    } catch (Exception e) {
      searchCriteria.setSortBy(LogSearchConstants.AUDIT_EVTTIME);
      searchCriteria.setSortType(SolrQuery.ORDER.desc.toString());
    }

    this.setSortOrderDefaultServiceLog(solrQuery, searchCriteria);
    return solrQuery;
  }

  private String putWildCardByType(String str, String key) {

    String auditSuffix = PropertiesUtil
      .getProperty("auditlog.solr.core.logs");
    String serviceLogs = PropertiesUtil.getProperty("solr.core.logs");

    String type = ConfigUtil.schemaFieldsName.get(key + auditSuffix);
    if (type == null)
      type = ConfigUtil.schemaFieldsName.get(key + serviceLogs);
    if (type == null)
      return "*" + str + "*";
    if ("text_std_token_lower_case".equalsIgnoreCase(type))
      return giveSplittedStringQuery(str);
    if ("key_lower_case".equalsIgnoreCase(type)
      || "string".equalsIgnoreCase(type))
      //return solrUtil.makeSolrSearchString(str);
      return solrUtil.makeSolrSearchStringWithoutAsterisk(str);
    if ("ip_address".equalsIgnoreCase(type))
      return str;
    return putEscapeCharacterForNumber(str);
  }

  private String giveSplittedStringQuery(String str) {
    try {
      String splittedString[] = str
        .split("/|-|@|&|^|%|$|#|!|~|:|;|\\*|\\+");
      String newStr = "(";
      int cnt = 0;
      for (String normalString : splittedString) {
        cnt++;
        if (!normalString.isEmpty()) {
          newStr += "*" + normalString + "*";
        }
        if (!normalString.isEmpty() && cnt < splittedString.length)
          newStr += " AND ";
      }
      newStr += ")";
      return newStr;
    } catch (Exception e) {
      return "*" + str + "*";
    }
  }

  private String putEscapeCharacterForNumber(String str) {
    String escapeCharSting = "" + returnDefaultIfValueNotNumber(str);
    escapeCharSting = str.replace("-", "\\-");
    return escapeCharSting;
  }

  private String returnDefaultIfValueNotNumber(String str) {
    try {
      return "" + Integer.parseInt(str);
    } catch (Exception e) {
      return "0";
    }
  }

  private String getOriginalValue(String name, String value) {
    String solrValue = PropertiesUtil.getProperty(name);

    try {
      String propertyFieldMappings[] = solrValue.split(",");
      HashMap<String, String> propertyFieldValue = new HashMap<String, String>();
      for (String temp : propertyFieldMappings) {
        String arrayValue[] = temp.split(":");
        propertyFieldValue.put(arrayValue[0].toLowerCase(Locale.ENGLISH),
          arrayValue[1].toLowerCase(Locale.ENGLISH));
      }
      String originalValue = propertyFieldValue.get(value.toLowerCase(Locale.ENGLISH));
      if (originalValue != null && !originalValue.isEmpty())
        return originalValue;

    } catch (Exception e) {
      // do nothing
    }
    return value;

  }

  private String getOriginalKey(String key) {
    String originalServiceKey = ConfigUtil.serviceLogsColumnMapping.get(key
      + LogSearchConstants.UI_SUFFIX);
    String originalAuditKey = ConfigUtil.auditLogsColumnMapping.get(key
      + LogSearchConstants.UI_SUFFIX);
    if (originalAuditKey != null && originalServiceKey == null) {
      return originalAuditKey;
    }
    if (originalServiceKey != null && originalAuditKey == null) {
      return originalServiceKey;
    }
    if (originalAuditKey != null && originalServiceKey != null) {
      return originalServiceKey;
    }
    return key;
  }
}