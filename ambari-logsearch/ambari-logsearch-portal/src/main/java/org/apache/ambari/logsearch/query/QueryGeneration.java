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

import org.apache.ambari.logsearch.common.ConfigHelper;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.PropertiesHelper;
import org.apache.ambari.logsearch.conf.SolrAuditLogConfig;
import org.apache.ambari.logsearch.conf.SolrServiceLogConfig;
import org.apache.ambari.logsearch.query.model.AuditLogSearchCriteria;
import org.apache.ambari.logsearch.query.model.CommonSearchCriteria;
import org.apache.ambari.logsearch.query.model.CommonServiceLogSearchCriteria;
import org.apache.ambari.logsearch.query.model.SearchCriteria;
import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.ambari.logsearch.manager.ManagerBase.LogType;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
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

import javax.inject.Inject;

@Component
public class QueryGeneration extends QueryGenerationBase {

  private static Logger logger = Logger.getLogger(QueryGeneration.class);

  @Inject
  private SolrServiceLogConfig solrServiceLogConfig;

  @Inject
  private SolrAuditLogConfig solrAuditLogConfig;

  public SolrQuery commonServiceFilterQuery(CommonServiceLogSearchCriteria searchCriteria) {
    LogType logType = LogType.SERVICE;
    SolrQuery solrQuery = new SolrQuery();
    String advQuery = (String) searchCriteria.getParamValue("advanceSearch"); // TODO: check these are used from the UI or not
    String gEmessage = (String) searchCriteria.getParamValue("gEMessage");
    String globalExcludeComp = (String) searchCriteria.getParamValue("gMustNot");
    String unselectedComp = (String) searchCriteria.getParamValue("unselectComp");

    String treeParams = searchCriteria.getTreeParams();
    String givenQuery = (String) searchCriteria.getParamValue("q");
    String level = searchCriteria.getLevel();
    String startTime = searchCriteria.getFrom();
    String endTime = searchCriteria.getTo();
    String iMessage = searchCriteria.getIncludeMessage();
    String eMessage = searchCriteria.getExcludeMessage();
    String selectedComp = searchCriteria.getSelectComp();
    String bundleId = searchCriteria.getBundleId();
    String urlHostName = searchCriteria.getHostName();
    String urlComponentName = searchCriteria.getComponentName();
    String file_name = searchCriteria.getFileName();
    // build advance query
    if (!StringUtils.isBlank(advQuery)) {
      String advQueryParameters[] = advQuery.split(Pattern.quote("}{"));
      SolrQuery advSolrQuery = new SolrQuery();
      for (String queryParam : advQueryParameters) {
        String params[] = queryParam.split(Pattern.quote("="));
        if (params.length > 1)
          advSolrQuery.setParam(params[0], params[1]);
      }
      setFilterClauseWithFieldName(advSolrQuery, level, LogSearchConstants.SOLR_LEVEL, "", Condition.OR);
      setSingleRangeFilter(advSolrQuery, LogSearchConstants.LOGTIME, startTime, endTime);
      setFilterClauseWithFieldName(advSolrQuery, unselectedComp, LogSearchConstants.SOLR_COMPONENT, LogSearchConstants.MINUS_OPERATOR,
          Condition.AND);
      setFilterClauseWithFieldName(advSolrQuery, selectedComp, LogSearchConstants.SOLR_COMPONENT, LogSearchConstants.NO_OPERATOR,
          Condition.OR);

      setPagination(advSolrQuery, searchCriteria);

      return advSolrQuery;
    }

    SolrUtil.setMainQuery(solrQuery, givenQuery);

    setSingleRangeFilter(solrQuery, LogSearchConstants.LOGTIME, startTime, endTime);
    addFilter(solrQuery, selectedComp, LogSearchConstants.SOLR_COMPONENT, Condition.OR);
    addFilterQueryFromArray(solrQuery, treeParams, LogSearchConstants.SOLR_HOST, Condition.OR);

    setFilterClauseWithFieldName(solrQuery, level, LogSearchConstants.SOLR_LEVEL, LogSearchConstants.NO_OPERATOR, Condition.OR);

    setFilterClauseForSolrSearchableString(solrQuery, iMessage, Condition.OR, LogSearchConstants.NO_OPERATOR, LogSearchConstants.SOLR_KEY_LOG_MESSAGE);
    setFilterClauseForSolrSearchableString(solrQuery, gEmessage, Condition.AND, LogSearchConstants.MINUS_OPERATOR, LogSearchConstants.SOLR_KEY_LOG_MESSAGE);
    setFilterClauseForSolrSearchableString(solrQuery, eMessage, Condition.AND, LogSearchConstants.MINUS_OPERATOR, LogSearchConstants.SOLR_KEY_LOG_MESSAGE);

    applyLogFileFilter(solrQuery, searchCriteria);

    setFilterClauseWithFieldName(solrQuery, globalExcludeComp, LogSearchConstants.SOLR_COMPONENT, LogSearchConstants.MINUS_OPERATOR, Condition.AND);
    setFilterClauseWithFieldName(solrQuery, unselectedComp, LogSearchConstants.SOLR_COMPONENT, LogSearchConstants.MINUS_OPERATOR, Condition.AND);

    urlHostName = SolrUtil.escapeQueryChars(urlHostName);
    setSingleIncludeFilter(solrQuery, LogSearchConstants.SOLR_HOST, urlHostName);
    urlComponentName = SolrUtil.escapeQueryChars(urlComponentName);
    setSingleIncludeFilter(solrQuery, LogSearchConstants.SOLR_COMPONENT, urlComponentName);

    setPagination(solrQuery, searchCriteria);
    setSortOrderDefaultServiceLog(solrQuery, searchCriteria);
    setSingleIncludeFilter(solrQuery, LogSearchConstants.BUNDLE_ID, bundleId);
    file_name = SolrUtil.escapeQueryChars(file_name);
    setSingleIncludeFilter(solrQuery, LogSearchConstants.SOLR_PATH, file_name);
    setUserSpecificFilter(searchCriteria, solrQuery, LogSearchConstants.INCLUDE_QUERY, LogSearchConstants.INCLUDE_QUERY, logType);
    setUserSpecificFilter(searchCriteria, solrQuery, LogSearchConstants.EXCLUDE_QUERY, LogSearchConstants.EXCLUDE_QUERY, logType);
    
    return solrQuery;
  }

  public void applyLogFileFilter(SolrQuery solrQuery, SearchCriteria searchCriteria) {
    String hostLogFile = (String) searchCriteria.getParamValue("hostLogFile");
    String compLogFile = (String) searchCriteria.getParamValue("compLogFile");
    String givenQuery = (String) searchCriteria.getParamValue("q");
    String logfileQuery = "";
    if (!StringUtils.isBlank(hostLogFile) && !StringUtils.isBlank(compLogFile)) {
      logfileQuery = LogSearchConstants.SOLR_HOST + ":" + hostLogFile + " " + Condition.AND + " " +
          LogSearchConstants.SOLR_COMPONENT + ":" + compLogFile;
      if (!StringUtils.isBlank(givenQuery)) {
        logfileQuery = "(" + givenQuery + ") " + Condition.AND + " (" + logfileQuery + ")";
      }
      if (!StringUtils.isBlank(logfileQuery)) {
        solrQuery.addFilterQuery(logfileQuery);
      }
    }
  }

  private void setUserSpecificFilter(SearchCriteria searchCriteria, SolrQuery solrQuery, String paramName, String operation,
      LogType logType) {
    String queryString = (String) searchCriteria.getParamValue(paramName);
    String columnQuery = (String) searchCriteria.getParamValue(LogSearchConstants.COLUMN_QUERY);
    if (StringUtils.isBlank(queryString)) {
      queryString = null;
    }
    if (!StringUtils.isBlank(columnQuery) && StringUtils.isBlank(queryString) && !paramName.equals(LogSearchConstants.EXCLUDE_QUERY)) {
      queryString = columnQuery;
    }
    List<String> conditionQuries = new ArrayList<String>();
    List<String> referalConditionQuries = new ArrayList<String>();
    List<String> elments = new ArrayList<String>();
    List<HashMap<String, Object>> queryList = JSONUtil.jsonToMapObjectList(queryString);
    if (queryList != null && queryList.size() > 0) {
      if (!StringUtils.isBlank(columnQuery) && !columnQuery.equals(queryString) && !paramName.equals(LogSearchConstants.EXCLUDE_QUERY)) {
        List<HashMap<String, Object>> columnQueryList = JSONUtil.jsonToMapObjectList(columnQuery);
        if (columnQueryList != null && columnQueryList.size() > 0) {
          queryList.addAll(columnQueryList);
        }
      }
      for (HashMap<String, Object> columnListMap : queryList) {
        String orQuery = "";
        StringBuilder field = new StringBuilder();
        if (columnListMap != null) {
          for (String key : columnListMap.keySet()) {
            if (!StringUtils.isBlank(key)) {
              String originalKey = getOriginalKey(key, logType);
              String value = getOriginalValue(originalKey, "" + columnListMap.get(key));
              orQuery = putWildCardByType(value, originalKey, logType);
              if (StringUtils.isBlank(orQuery)) {
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
                    String newCondtion = tempCondition + " " + Condition.OR.name() + " " + orQuery;
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
    if (!referalConditionQuries.isEmpty() && !StringUtils.isBlank(operation)) {
      if (operation.equals(LogSearchConstants.INCLUDE_QUERY) || operation.equals(LogSearchConstants.COLUMN_QUERY)) {
        for (String filter : referalConditionQuries) {
          if (!StringUtils.isBlank(filter)) {
            solrQuery.addFilterQuery(filter);
          }
        }
      } else if (operation.equals(LogSearchConstants.EXCLUDE_QUERY)) {
        for (String filter : referalConditionQuries) {
          if (!StringUtils.isBlank(filter)) {
            filter = LogSearchConstants.MINUS_OPERATOR + filter;
            solrQuery.addFilterQuery(filter);
          }
        }
      }
    }
  }

  public SolrQuery commonAuditFilterQuery(CommonSearchCriteria searchCriteria) {
    LogType logType = LogType.AUDIT;
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*");

    String globalExcludeComp = (String) searchCriteria.getParamValue("gMustNot"); // TODO: check this are used from UI or not
    String unselectedComp = (String) searchCriteria.getParamValue("unselectComp");

    String startTime = searchCriteria.getStartTime();
    String endTime = searchCriteria.getEndTime();
    String selectedComp = searchCriteria.getMustBe();
    setFilterClauseWithFieldName(solrQuery, selectedComp, LogSearchConstants.AUDIT_COMPONENT, LogSearchConstants.NO_OPERATOR, Condition.OR);
    setUserSpecificFilter(searchCriteria, solrQuery, LogSearchConstants.INCLUDE_QUERY, LogSearchConstants.INCLUDE_QUERY, logType);
    setUserSpecificFilter(searchCriteria, solrQuery, LogSearchConstants.EXCLUDE_QUERY, LogSearchConstants.EXCLUDE_QUERY, logType);
    setFilterClauseWithFieldName(solrQuery, globalExcludeComp, LogSearchConstants.AUDIT_COMPONENT, LogSearchConstants.MINUS_OPERATOR, Condition.AND);
    setFilterClauseWithFieldName(solrQuery, unselectedComp, LogSearchConstants.AUDIT_COMPONENT, LogSearchConstants.MINUS_OPERATOR, Condition.AND);
    setSingleRangeFilter(solrQuery, LogSearchConstants.AUDIT_EVTTIME, startTime, endTime);
    setPagination(solrQuery, searchCriteria);
    try {
      if (searchCriteria.getSortBy() == null || searchCriteria.getSortBy().isEmpty()) {
        searchCriteria.setSortBy(LogSearchConstants.AUDIT_EVTTIME);
        searchCriteria.setSortType(SolrQuery.ORDER.desc.toString());
      }
    } catch (Exception e) {
      searchCriteria.setSortBy(LogSearchConstants.AUDIT_EVTTIME);
      searchCriteria.setSortType(SolrQuery.ORDER.desc.toString());
    }
    setSortOrderDefaultServiceLog(solrQuery, searchCriteria);
    return solrQuery;
  }

  private String putWildCardByType(String str, String key, LogType logType) {
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
        return SolrUtil.escapeForLogMessage(key, str);
      }
      break;
    default:
      // set as null
      logger.error("Invalid logtype :" + logType);
      fieldType = null;
    }
    if (!StringUtils.isBlank(fieldType)) {
      if (SolrUtil.isSolrFieldNumber(fieldType, solrDaoBase)) {
        String value = putEscapeCharacterForNumber(str, fieldType,solrDaoBase);
        if (!StringUtils.isBlank(value)) {
          return key + ":" + value;
        } else {
          return null;
        }
      } else if (checkTokenizer(fieldType, StandardTokenizerFactory.class,solrDaoBase)) {
        return key + ":" + SolrUtil.escapeForStandardTokenizer(str);
      } else if (checkTokenizer(fieldType, KeywordTokenizerFactory.class,solrDaoBase)|| "string".equalsIgnoreCase(fieldType)) {
        return key + ":" + SolrUtil.makeSolrSearchStringWithoutAsterisk(str);
      } else if (checkTokenizer(fieldType, PathHierarchyTokenizerFactory.class,solrDaoBase)) {
        return key + ":" + str;
      }
    }
   return key + ":" + "*" + str + "*";
  }

  private String putEscapeCharacterForNumber(String str,String fieldType,SolrDaoBase solrDaoBase) {
    if (!StringUtils.isBlank(str)) {
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
      HashMap<String, Object> fieldTypeInfoMap= SolrUtil.getFieldTypeInfoMap(fieldType,solrDaoBase);
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
    String solrValue = PropertiesHelper.getProperty(name);
    if (StringUtils.isBlank(solrValue)) {
      return value;
    }
    try {
      String propertyFieldMappings[] = solrValue.split(LogSearchConstants.LIST_SEPARATOR);
      if (propertyFieldMappings != null && propertyFieldMappings.length > 0) {
        HashMap<String, String> propertyFieldValue = new HashMap<String, String>();
        for (String temp : propertyFieldMappings) {
          if (!StringUtils.isBlank(temp)) {
            String arrayValue[] = temp.split(":");
            if (arrayValue.length > 1) {
              propertyFieldValue.put(arrayValue[0].toLowerCase(Locale.ENGLISH), arrayValue[1].toLowerCase(Locale.ENGLISH));
            } else {
              logger.warn("array length is less than required length 1");
            }
          }
        }
        String originalValue = propertyFieldValue.get(value.toLowerCase(Locale.ENGLISH));
        if (!StringUtils.isBlank(originalValue)) {
          return originalValue;
        }
      }
    } catch (Exception e) {
      // do nothing
    }
    return value;
  }

  private String getOriginalKey(String key, LogType logType) {
    String originalKey;
    switch (logType) {
    case AUDIT:
      originalKey = solrAuditLogConfig.getSolrAndUiColumns().get(key + LogSearchConstants.UI_SUFFIX);
      break;
    case SERVICE:
      originalKey = solrServiceLogConfig.getSolrAndUiColumns().get(key + LogSearchConstants.UI_SUFFIX);
      break;
    default:
      originalKey = null;
    }
    if (StringUtils.isBlank(originalKey)) {
      return key;
    }
    return originalKey;
  }
  
  private boolean checkTokenizer(String fieldType, Class tokenizerFactoryClass, SolrDaoBase solrDaoBase) {
    HashMap<String, Object> fieldTypeMap = SolrUtil.getFieldTypeInfoMap(fieldType,solrDaoBase);
    HashMap<String, Object> analyzer = (HashMap<String, Object>) fieldTypeMap.get("analyzer");
    if (analyzer != null) {
      HashMap<String, Object> tokenizerMap = (HashMap<String, Object>) analyzer.get("tokenizer");
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
