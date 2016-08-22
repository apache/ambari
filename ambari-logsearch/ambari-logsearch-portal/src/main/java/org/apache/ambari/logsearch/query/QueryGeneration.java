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
import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.ambari.logsearch.manager.MgrBase.LOG_TYPE;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.path.PathHierarchyTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.schema.TrieDoubleField;
import org.apache.solr.schema.TrieFloatField;
import org.apache.solr.schema.TrieLongField;
import org.springframework.stereotype.Component;

@Component
public class QueryGeneration extends QueryGenerationBase {

  private static Logger logger = Logger.getLogger(QueryGeneration.class);

  public SolrQuery commonServiceFilterQuery(SearchCriteria searchCriteria) {
    LOG_TYPE logType = LOG_TYPE.SERVICE;
    SolrQuery solrQuery = new SolrQuery();
    String treeParams = (String) searchCriteria.getParamValue("treeParams");
    String givenQuery = (String) searchCriteria.getParamValue("q");
    String level = (String) searchCriteria.getParamValue("level");
    String startTime = (String) searchCriteria.getParamValue("from");
    String endTime = (String) searchCriteria.getParamValue("to");
    String iMessage = (String) searchCriteria.getParamValue("iMessage");
    String eMessage = (String) searchCriteria.getParamValue("eMessage");
    String gEmessage = (String) searchCriteria.getParamValue("gEMessage");
    String selectedComp = (String) searchCriteria.getParamValue("selectComp");
    String bundleId = (String) searchCriteria
        .getParamValue(LogSearchConstants.BUNDLE_ID);
    String globalExcludeComp = (String) searchCriteria
        .getParamValue("gMustNot");
    String unselectedComp = (String) searchCriteria
        .getParamValue("unselectComp");
    String urlHostName = (String) searchCriteria.getParamValue("host_name");
    String urlComponentName = (String) searchCriteria
        .getParamValue("component_name");
    String file_name = (String) searchCriteria.getParamValue("file_name");
    String advQuery = (String) searchCriteria.getParamValue("advanceSearch");
    // build advance query
    if (!stringUtil.isEmpty(advQuery)) {
      String advQueryParameters[] = advQuery.split(Pattern.quote("}{"));
      SolrQuery advSolrQuery = new SolrQuery();
      for (String queryParam : advQueryParameters) {
        String params[] = queryParam.split(Pattern.quote("="));
        if (params != null && params.length > 1)
          advSolrQuery.setParam(params[0], params[1]);
      }
      // Building and adding levels to filters
      setFilterClauseWithFieldName(advSolrQuery, level,
          LogSearchConstants.SOLR_LEVEL, "", CONDITION.OR);

      // Adding Logtime to filters
      setSingleRangeFilter(advSolrQuery, LogSearchConstants.LOGTIME, startTime,
          endTime);

      // Building and adding exlcude components to filters
      setFilterClauseWithFieldName(advSolrQuery, unselectedComp,
          LogSearchConstants.SOLR_COMPONENT, LogSearchConstants.MINUS_OPERATOR,
          CONDITION.AND);

      // Building and adding exlcude components to filters
      setFilterClauseWithFieldName(advSolrQuery, selectedComp,
          LogSearchConstants.SOLR_COMPONENT, LogSearchConstants.NO_OPERATOR,
          CONDITION.OR);

      // Set Pagination
      setPagination(advSolrQuery, searchCriteria);

      return advSolrQuery;
    }

    setMainQuery(solrQuery, givenQuery);

    // Adding Logtime to filters
    setSingleRangeFilter(solrQuery, LogSearchConstants.LOGTIME, startTime,
        endTime);

    // String mainFilterQuery = buildQueryFromJSONCompHost(jsonHCNames,
    // selectedComp);

    // if (mainFilterQuery != null && !mainFilterQuery.equals(""))
    // solrQuery.addFilterQuery(mainFilterQuery);

    // add component filter
    addFilter(solrQuery, selectedComp, LogSearchConstants.SOLR_COMPONENT,
        CONDITION.OR);

    // add treeParams filter
    // hosts comma separated list
    addFilterQueryFromArray(solrQuery, treeParams,
        LogSearchConstants.SOLR_HOST, CONDITION.OR);

    // Building and adding levels to filters
    setFilterClauseWithFieldName(solrQuery, level,
        LogSearchConstants.SOLR_LEVEL, LogSearchConstants.NO_OPERATOR,
        CONDITION.OR);

    // Building and adding include string to filters
    setFilterClauseForSolrSearchableString(solrQuery, iMessage, CONDITION.OR,
        LogSearchConstants.NO_OPERATOR, LogSearchConstants.SOLR_KEY_LOG_MESSAGE);

    // Building and adding global exclude string to filters
    setFilterClauseForSolrSearchableString(solrQuery, gEmessage, CONDITION.AND,
        LogSearchConstants.MINUS_OPERATOR, LogSearchConstants.SOLR_KEY_LOG_MESSAGE);

    // Building and adding exclude string to filter
    setFilterClauseForSolrSearchableString(solrQuery, eMessage, CONDITION.AND,
        LogSearchConstants.MINUS_OPERATOR, LogSearchConstants.SOLR_KEY_LOG_MESSAGE);

    // Building and adding logfile to filters
    applyLogFileFilter(solrQuery, searchCriteria);

    // Building and adding exclude components to filters
    setFilterClauseWithFieldName(solrQuery, globalExcludeComp,
        LogSearchConstants.SOLR_COMPONENT, LogSearchConstants.MINUS_OPERATOR,
        CONDITION.AND);

    // Building and adding exlcude components to filters
    setFilterClauseWithFieldName(solrQuery, unselectedComp,
        LogSearchConstants.SOLR_COMPONENT, LogSearchConstants.MINUS_OPERATOR,
        CONDITION.AND);

    // Building and adding host names given url
    // setFilterClauseWithFieldName(solrQuery, urlHostName,
    // LogSearchConstants.SOLR_HOST,
    // "", "OR");
    urlHostName = solrUtil.escapeQueryChars(urlHostName);
    setSingleIncludeFilter(solrQuery, LogSearchConstants.SOLR_HOST, urlHostName);
    //
    // //Building and addding component names given url
    // setFilterClauseWithFieldName(solrQuery, urlComponents,
    // LogSearchConstants.SOLR_COMPONENT,
    // "", "OR");
    urlComponentName = solrUtil.escapeQueryChars(urlComponentName);
    setSingleIncludeFilter(solrQuery, LogSearchConstants.SOLR_COMPONENT,
        urlComponentName);

    // Set Pagination
    setPagination(solrQuery, searchCriteria);

    // SetSort type (by default Descending)
    setSortOrderDefaultServiceLog(solrQuery, searchCriteria);

    // Set Bundle Id
    setSingleIncludeFilter(solrQuery, LogSearchConstants.BUNDLE_ID, bundleId);

    // Set filename
    file_name = solrUtil.escapeQueryChars(file_name);
    setSingleIncludeFilter(solrQuery, LogSearchConstants.SOLR_PATH, file_name);
    // include query
    this.setUserSpecificFilter(searchCriteria, solrQuery,
        LogSearchConstants.INCLUDE_QUERY, LogSearchConstants.INCLUDE_QUERY,
        logType);
    // exclude query
    this.setUserSpecificFilter(searchCriteria, solrQuery,
        LogSearchConstants.EXCLUDE_QUERY, LogSearchConstants.EXCLUDE_QUERY,
        logType);
    return solrQuery;
  }

  public void applyLogFileFilter(SolrQuery solrQuery,
      SearchCriteria searchCriteria) {
    String hostLogFile = (String) searchCriteria.getParamValue("hostLogFile");
    String compLogFile = (String) searchCriteria.getParamValue("compLogFile");
    String givenQuery = (String) searchCriteria.getParamValue("q");
    String logfileQuery = "";
    if (!stringUtil.isEmpty(hostLogFile) && !stringUtil.isEmpty(compLogFile)) {
      logfileQuery = LogSearchConstants.SOLR_HOST + ":" + hostLogFile + " "
          + CONDITION.AND + " " + LogSearchConstants.SOLR_COMPONENT + ":"
          + compLogFile;
      if (!stringUtil.isEmpty(givenQuery)) {
        logfileQuery = "(" + givenQuery + ") " + CONDITION.AND + " ("
            + logfileQuery + ")";
      }
      if (!stringUtil.isEmpty(logfileQuery)) {
        solrQuery.addFilterQuery(logfileQuery);
      }
    }
  }

  public void setUserSpecificFilter(SearchCriteria searchCriteria,
      SolrQuery solrQuery, String paramName, String operation, LOG_TYPE logType) {
    String queryString = (String) searchCriteria.getParamValue(paramName);
    String columnQuery = (String) searchCriteria
        .getParamValue(LogSearchConstants.COLUMN_QUERY);
    if (stringUtil.isEmpty(queryString)) {
      queryString = null;
    }
    // if (!stringUtil.isEmpty(queryString) && "[]".equals(queryString)) {
    // queryString = null;
    // }
    if (!stringUtil.isEmpty(columnQuery) && stringUtil.isEmpty(queryString)
        && !paramName.equals(LogSearchConstants.EXCLUDE_QUERY)) {
      queryString = columnQuery;
    }
    List<String> conditionQuries = new ArrayList<String>();
    List<String> referalConditionQuries = new ArrayList<String>();
    List<String> elments = new ArrayList<String>();
    // convert json to list of hashmap
    List<HashMap<String, Object>> queryList = jsonUtil
        .jsonToMapObjectList(queryString);
    // null and size check
    if (queryList != null && queryList.size() > 0) {
      if (!stringUtil.isEmpty(columnQuery) && !columnQuery.equals(queryString)
          && !paramName.equals(LogSearchConstants.EXCLUDE_QUERY)) {
        List<HashMap<String, Object>> columnQueryList = jsonUtil
            .jsonToMapObjectList(columnQuery);
        if (columnQueryList != null && columnQueryList.size() > 0) {
          queryList.addAll(columnQueryList);
        }
      }
      for (HashMap<String, Object> columnListMap : queryList) {
        String orQuery = "";
        StringBuilder field = new StringBuilder();
        if (columnListMap != null) {
          for (String key : columnListMap.keySet()) {
            if (!stringUtil.isEmpty(key)) {
              String originalKey = getOriginalKey(key, logType);
              String value = getOriginalValue(originalKey,
                  "" + columnListMap.get(key));
              orQuery = putWildCardByType(value, originalKey, logType);
              if (stringUtil.isEmpty(orQuery)) {
                logger.debug("Removing invalid filter for key :"+originalKey +" and value :" +value );
                continue;
              }
              boolean isSame = false;
              if (elments.contains(key)) {
                isSame = true;
              }
              if (isSame && !operation.equals(LogSearchConstants.EXCLUDE_QUERY)) {
                for (String tempCondition : conditionQuries) {
                  if (tempCondition.contains(originalKey)) {
                    String newCondtion = tempCondition + " "
                        + CONDITION.OR.name() + " " + orQuery;
                    referalConditionQuries.remove(tempCondition);
                    referalConditionQuries.add(newCondtion);
                  }
                }
                conditionQuries.removeAll(conditionQuries);
                conditionQuries.addAll(referalConditionQuries);
              } else {
                conditionQuries.add(orQuery.toString());
                referalConditionQuries.add(orQuery.toString());
              }
              field.append(key);
              elments.add(field.toString());
            }
          }
        }
      }
    }
    if (!referalConditionQuries.isEmpty() && !stringUtil.isEmpty(operation)) {
      if (operation.equals(LogSearchConstants.INCLUDE_QUERY)
          || operation.equals(LogSearchConstants.COLUMN_QUERY)) {
        for (String filter : referalConditionQuries) {
          if (!stringUtil.isEmpty(filter)) {
            solrQuery.addFilterQuery(filter);
          }
        }
      } else if (operation.equals(LogSearchConstants.EXCLUDE_QUERY)) {
        for (String filter : referalConditionQuries) {
          if (!stringUtil.isEmpty(filter)) {
            filter = LogSearchConstants.MINUS_OPERATOR + filter;
            solrQuery.addFilterQuery(filter);
          }
        }
      }
    }
  }

  public SolrQuery commonAuditFilterQuery(SearchCriteria searchCriteria) {
    LOG_TYPE logType = LOG_TYPE.AUDIT;
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");
    String startTime = (String) searchCriteria.getParamValue("startTime");
    String endTime = (String) searchCriteria.getParamValue("endTime");
    String selectedComp = (String) searchCriteria
        .getParamValue("includeString");
    this.setFilterClauseWithFieldName(solrQuery, selectedComp,
        LogSearchConstants.AUDIT_COMPONENT, LogSearchConstants.NO_OPERATOR,
        CONDITION.OR);
    String globalExcludeComp = (String) searchCriteria
        .getParamValue("gMustNot");
    this.setUserSpecificFilter(searchCriteria, solrQuery,
        LogSearchConstants.INCLUDE_QUERY, LogSearchConstants.INCLUDE_QUERY,
        logType);
    this.setUserSpecificFilter(searchCriteria, solrQuery,
        LogSearchConstants.EXCLUDE_QUERY, LogSearchConstants.EXCLUDE_QUERY,
        logType);
    String unselectedComp = (String) searchCriteria
        .getParamValue("unselectComp");
    this.setFilterClauseWithFieldName(solrQuery, globalExcludeComp,
        LogSearchConstants.AUDIT_COMPONENT, LogSearchConstants.MINUS_OPERATOR,
        CONDITION.AND);
    // Building and adding exlcude components to filters
    this.setFilterClauseWithFieldName(solrQuery, unselectedComp,
        LogSearchConstants.AUDIT_COMPONENT, LogSearchConstants.MINUS_OPERATOR,
        CONDITION.AND);
    // Adding Logtime to filters
    this.setSingleRangeFilter(solrQuery, LogSearchConstants.AUDIT_EVTTIME,
        startTime, endTime);
    this.setPagination(solrQuery, searchCriteria);
    try {
      if (searchCriteria.getSortBy() == null
          || searchCriteria.getSortBy().isEmpty()) {
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

  private String putWildCardByType(String str, String key, LOG_TYPE logType) {
    String fieldType;
    SolrDaoBase solrDaoBase = null;
    switch (logType) {
    case AUDIT:
      fieldType = auditSolrDao.schemaFieldsNameMap.get(key);
      solrDaoBase = auditSolrDao;
      break;
    case SERVICE:
      fieldType = serviceLogsSolrDao.schemaFieldsNameMap.get(key);
      solrDaoBase = serviceLogsSolrDao;
      if (key.equalsIgnoreCase(LogSearchConstants.SOLR_LOG_MESSAGE)) {
        return solrUtil.escapeForLogMessage(key, str);
      }
      break;
    default:
      // set as null
      logger.error("Invalid logtype :" + logType);
      fieldType = null;
    }
    if (!stringUtil.isEmpty(fieldType)) {
      if (solrUtil.isSolrFieldNumber(fieldType, solrDaoBase)) {
        String value = putEscapeCharacterForNumber(str, fieldType,solrDaoBase);
        if (!stringUtil.isEmpty(value)) {
          return key + ":" + value;
        } else {
          return null;
        }
      } else if (checkTokenizer(fieldType, StandardTokenizerFactory.class,solrDaoBase)) {
        return key + ":" + solrUtil.escapeForStandardTokenizer(str);
      } else if (checkTokenizer(fieldType, KeywordTokenizerFactory.class,solrDaoBase)|| "string".equalsIgnoreCase(fieldType)) {
        return key + ":" + solrUtil.makeSolrSearchStringWithoutAsterisk(str);
      } else if (checkTokenizer(fieldType, PathHierarchyTokenizerFactory.class,solrDaoBase)) {
        return key + ":" + str;
      }
    }
   return key + ":" + "*" + str + "*";
  }

  private String putEscapeCharacterForNumber(String str,String fieldType,SolrDaoBase solrDaoBase) {
    if (!stringUtil.isEmpty(str)) {
      str = str.replace("*", "");
    }
    String escapeCharSting = parseInputValueAsPerFieldType(str,fieldType,solrDaoBase);
    if (escapeCharSting == null || escapeCharSting.isEmpty()) {
      return null;
    }
    escapeCharSting = escapeCharSting.replace("-", "\\-");
    return escapeCharSting;
  }

  private String parseInputValueAsPerFieldType(String str,String fieldType,SolrDaoBase solrDaoBase ) {
    try {
      HashMap<String, Object> fieldTypeInfoMap= solrUtil.getFieldTypeInfoMap(fieldType,solrDaoBase);
      String className = (String) fieldTypeInfoMap.get("class");
      if( className.equalsIgnoreCase(TrieDoubleField.class.getSimpleName())){
        return ""+ Double.parseDouble(str);
      }else if(className.equalsIgnoreCase(TrieFloatField.class.getSimpleName())){
        return ""+ Float.parseFloat(str);
      }else if(className.equalsIgnoreCase(TrieLongField.class.getSimpleName())){
        return ""+ Long.parseLong(str);
      }else {
        return "" + Integer.parseInt(str);
      }
    } catch (Exception e) {
      logger.debug("Invaid input str: " + str + " For fieldType :" + fieldType);
      return null;
    }
  }

  private String getOriginalValue(String name, String value) {
    String solrValue = PropertiesUtil.getProperty(name);
    if (stringUtil.isEmpty(solrValue)) {
      return value;
    }
    try {
      String propertyFieldMappings[] = solrValue
          .split(LogSearchConstants.LIST_SEPARATOR);
      if (propertyFieldMappings != null && propertyFieldMappings.length > 0) {
        HashMap<String, String> propertyFieldValue = new HashMap<String, String>();
        for (String temp : propertyFieldMappings) {
          if (!stringUtil.isEmpty(temp)) {
            String arrayValue[] = temp.split(":");
            if (arrayValue.length > 1) {
              propertyFieldValue.put(arrayValue[0].toLowerCase(Locale.ENGLISH),
                  arrayValue[1].toLowerCase(Locale.ENGLISH));
            } else {
              logger.warn("array length is less than required length 1");
            }
          }
        }
        String originalValue = propertyFieldValue.get(value
            .toLowerCase(Locale.ENGLISH));
        if (!stringUtil.isEmpty(originalValue)) {
          return originalValue;
        }
      }
    } catch (Exception e) {
      // do nothing
    }
    return value;
  }

  private String getOriginalKey(String key, LOG_TYPE logType) {
    String originalKey;
    switch (logType) {
    case AUDIT:
      originalKey = ConfigUtil.auditLogsColumnMapping.get(key
          + LogSearchConstants.UI_SUFFIX);
      break;
    case SERVICE:
      originalKey = ConfigUtil.serviceLogsColumnMapping.get(key
          + LogSearchConstants.UI_SUFFIX);
      break;
    default:
      originalKey = null;
      // set as null
    }
    if (stringUtil.isEmpty(originalKey)) {
      // return default values
      return key;
    }
    return originalKey;
  }
  
  public boolean checkTokenizer(String fieldType,Class tokenizerFactoryClass,SolrDaoBase solrDaoBase) {
    HashMap<String, Object> fieldTypeMap = solrUtil.getFieldTypeInfoMap(fieldType,solrDaoBase);
    HashMap<String, Object> analyzer = (HashMap<String, Object>) fieldTypeMap
        .get("analyzer");
    if (analyzer != null) {
      HashMap<String, Object> tokenizerMap = (HashMap<String, Object>) analyzer
          .get("tokenizer");
      if (tokenizerMap != null) {
        String tokenizerClass = (String) tokenizerMap.get("class");
        if (!StringUtils.isEmpty(tokenizerClass)) {
          tokenizerClass =tokenizerClass.replace("solr.", "");
          if (tokenizerClass.equalsIgnoreCase(tokenizerFactoryClass
              .getSimpleName())) {
            return true;
          }
        }
      }
    }
    return false;
  }
}