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
package org.apache.ambari.logsearch.converter;

import static java.util.Collections.singletonList;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LOG_MESSAGE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.dao.SolrSchemaFieldDao;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public abstract class AbstractOperationHolderConverter <REQUEST_TYPE, QUERY_TYPE>
  extends AbstractConverterAware<REQUEST_TYPE, QUERY_TYPE> {

  @Inject
  private SolrSchemaFieldDao solrSchemaFieldDao;

  public List<String> splitValueAsList(String value, String separator) {
    return StringUtils.isNotEmpty(value) ? Splitter.on(separator).omitEmptyStrings().splitToList(value) : null;
  }

  public Query addEqualsFilterQuery(Query query, String field, String value) {
    return this.addEqualsFilterQuery(query, field, value, false);
  }

  public Query addEqualsFilterQuery(Query query, String field, String value, boolean negate) {
    if (StringUtils.isNotEmpty(value)) {
      addFilterQuery(query, new Criteria(field).is(value), negate);
    }
    return query;
  }

  public Query addContainsFilterQuery(Query query, String field, String value) {
    return this.addContainsFilterQuery(query, field, value, false);
  }

  public Query addContainsFilterQuery(Query query, String field, String value, boolean negate) {
    if (StringUtils.isNotEmpty(value)) {
      addFilterQuery(query, new Criteria(field).contains(value), negate);
    }
    return query;
  }

  public Query addInFilterQuery(Query query, String field, List<String> values) {
    return this.addInFilterQuery(query, field, values, false);
  }

  public Query addInFiltersIfNotNullAndEnabled(Query query, String value, String field, boolean condition) {
    if (value != null && condition) {
      List<String> values = value.length() == 0 ? singletonList("-1") : splitValueAsList(value, ",");
      addInFilterQuery(query, field, values);
    }
    return query;
  }

  public SolrQuery addInFiltersIfNotNullAndEnabled(SolrQuery query, String value, String field, boolean condition) {
    if (condition) {
      List<String> valuesList = value.length() == 0 ? singletonList("\\-1") : splitValueAsList(value, ",");
      if (valuesList.size() > 1) {
        query.addFilterQuery(String.format("%s:(%s)", field, StringUtils.join(valuesList, " OR ")));
      } else {
        query.addFilterQuery(String.format("%s:%s", field, valuesList.get(0)));
      }
    }
    return query;
  }

  public Query addInFilterQuery(Query query, String field, List<String> values, boolean negate) {
    if (CollectionUtils.isNotEmpty(values)) {
      String orQueryStr = StringUtils.join(values, " OR ");
      addFilterQuery(query, new Criteria(field).in(orQueryStr.split(" ")), negate);
    }
    return query;
  }

  public Query addRangeFilter(Query query, String field, String from, String to) {
    return this.addRangeFilter(query, field, from, to, false);
  }

  public Query addRangeFilter(Query query, String field, String from, String to, boolean negate) { // TODO use criteria.between without escaping
    String fromValue = StringUtils.defaultIfEmpty(from, "*");
    String toValue = StringUtils.defaultIfEmpty(to, "*");
    addFilterQuery(query, new SimpleStringCriteria(field + ":[" + fromValue +" TO "+ toValue + "]" ), negate);
    return query;
  }

  public void addFilterQuery(Query query, Criteria criteria, boolean negate) {
    if (negate) {
      criteria.not();
    }
    query.addFilterQuery(new SimpleFilterQuery(criteria));
  }

  public Query addIncludeFieldValues(Query query, String fieldValuesMapStr) {
    if (StringUtils.isNotEmpty(fieldValuesMapStr)) {
      List<Map<String, String>> criterias = new Gson().fromJson(fieldValuesMapStr,
        new TypeToken<List<HashMap<String, String>>>(){}.getType());
      for (Map<String, String> criteriaMap : criterias) {
        for (Map.Entry<String, String> fieldEntry : criteriaMap.entrySet()) {
          if (fieldEntry.getKey().equalsIgnoreCase(LOG_MESSAGE)) {
            addLogMessageFilter(query, fieldEntry.getValue(), false);
          } else {
            addFilterQuery(query, new Criteria(fieldEntry.getKey()).is(escapeNonLogMessageField(fieldEntry)), false);
          }
        }
      }
    }
    return query;
  }

  public SolrQuery addIncludeFieldValues(SolrQuery query, String fieldValuesMapStr) {
    if (StringUtils.isNotEmpty(fieldValuesMapStr)) {
      List<Map<String, String>> criterias = new Gson().fromJson(fieldValuesMapStr,
        new TypeToken<List<HashMap<String, String>>>(){}.getType());
      for (Map<String, String> criteriaMap : criterias) {
        for (Map.Entry<String, String> fieldEntry : criteriaMap.entrySet()) {
          if (fieldEntry.getKey().equalsIgnoreCase(LOG_MESSAGE)) {
            addLogMessageFilter(query, fieldEntry.getValue(), false);
          } else {
            query.addFilterQuery(String.format("%s:%s", fieldEntry.getKey(), escapeNonLogMessageField(fieldEntry)));
          }
        }
      }
    }
    return query;
  }

  public Query addExcludeFieldValues(Query query, String fieldValuesMapStr) {
    if (StringUtils.isNotEmpty(fieldValuesMapStr)) {
      List<Map<String, String>> criterias = new Gson().fromJson(fieldValuesMapStr,
        new TypeToken<List<HashMap<String, String>>>(){}.getType());
      for (Map<String, String> criteriaMap : criterias) {
        for (Map.Entry<String, String> fieldEntry : criteriaMap.entrySet()) {
          if (fieldEntry.getKey().equalsIgnoreCase(LOG_MESSAGE)) {
            addLogMessageFilter(query, fieldEntry.getValue(), true);
          } else {
            addFilterQuery(query, new Criteria(fieldEntry.getKey()).is(escapeNonLogMessageField(fieldEntry)), true);
          }
        }
      }
    }
    return query;
  }

  public SolrQuery addExcludeFieldValues(SolrQuery query, String fieldValuesMapStr) {
    if (StringUtils.isNotEmpty(fieldValuesMapStr)) {
      List<Map<String, String>> criterias = new Gson().fromJson(fieldValuesMapStr,
        new TypeToken<List<HashMap<String, String>>>(){}.getType());
      for (Map<String, String> criteriaMap : criterias) {
        for (Map.Entry<String, String> fieldEntry : criteriaMap.entrySet()) {
          if (fieldEntry.getKey().equalsIgnoreCase(LOG_MESSAGE)) {
            addLogMessageFilter(query, fieldEntry.getValue(), true);
          } else {
            query.addFilterQuery(String.format("-%s:%s", fieldEntry.getKey(), escapeNonLogMessageField(fieldEntry)));
          }
        }
      }
    }
    return query;
  }

  public SolrQuery addListFilterToSolrQuery(SolrQuery solrQuery, String fieldName, String fieldValue) {
    return SolrUtil.addListFilterToSolrQuery(solrQuery, fieldName, fieldValue);
  }

  public abstract LogType getLogType();

  private void addLogMessageFilter(Query query, String value, boolean negate) {
    value = value.trim();
    if (StringUtils.startsWith(value, "\"") && StringUtils.endsWith(value,"\"")) {
      value = String.format("\"%s\"", SolrUtil.escapeQueryChars(StringUtils.substring(value, 1, -1)));
      addFilterQuery(query, new Criteria(LOG_MESSAGE).expression(value), negate);
    }
    else if (isNotBlank(value)){
      addFilterQuery(query, new Criteria(LOG_MESSAGE).expression(SolrUtil.escapeQueryChars(value)), negate);
    }
  }

  private void addLogMessageFilter(SolrQuery query, String value, boolean negate) {
    String negateToken = negate ? "-" : "";
    value = value.trim();
    if (StringUtils.startsWith(value, "\"") && StringUtils.endsWith(value,"\"")) {
      value = String.format("\"%s\"", SolrUtil.escapeQueryChars(StringUtils.substring(value, 1, -1)));
      query.addFilterQuery(String.format("%s%s:%s", negateToken, LOG_MESSAGE, value));
    }
    else {
      query.addFilterQuery(String.format("%s%s:%s", negateToken, LOG_MESSAGE, SolrUtil.escapeQueryChars(value)));
    }
  }

  private String escapeNonLogMessageField(Map.Entry<String, String> fieldEntry) {
    Map<String, String> schemaFieldNameMap = solrSchemaFieldDao.getSchemaFieldNameMap(getLogType());
    Map<String, String> schemaFieldTypeMap = solrSchemaFieldDao.getSchemaFieldTypeMap(getLogType());
    String fieldType = schemaFieldNameMap.get(fieldEntry.getKey());
    String fieldTypeMetaData = schemaFieldTypeMap.get(fieldType);
    return SolrUtil.putWildCardByType(fieldEntry.getValue(), fieldType, fieldTypeMetaData);
  }
}
