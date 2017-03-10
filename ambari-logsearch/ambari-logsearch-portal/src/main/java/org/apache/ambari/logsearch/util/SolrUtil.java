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

package org.apache.ambari.logsearch.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.path.PathHierarchyTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.schema.TrieDoubleField;
import org.apache.solr.schema.TrieFloatField;
import org.apache.solr.schema.TrieIntField;
import org.apache.solr.schema.TrieLongField;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

public class SolrUtil {
  private SolrUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Copied from Solr ClientUtils.escapeQueryChars and removed escaping *
   */
  public static String escapeQueryChars(String s) {
    StringBuilder sb = new StringBuilder();
    if (s != null) {
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        int ic = (int) c;
        if (ic == 10) {
          sb.append('\\');
          sb.append((char) 13);
        }
        // Note: Remove || c == '*'
        // These characters are part of the query syntax and must be escaped
        if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '('
          || c == ')' || c == ':' || c == '^' || c == '[' || c == ']'
          || c == '\"' || c == '{' || c == '}' || c == '~' || c == '?'
          || c == '|' || c == '&' || c == ';' || c == '/'
          || Character.isWhitespace(c)) {
          sb.append('\\');
        }
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public static String escapeForStandardTokenizer(String search) {
    if (search == null) {
      return null;
    }
    String newSearch = escapeQueryChars(search.trim());
    if (StringUtils.containsWhitespace(newSearch)) {
      newSearch = "\"" + newSearch + "\"";
    }

    return newSearch;
  }

  private static String makeSolrSearchStringWithoutAsterisk(String search) {
    String newString = search.trim();
    String newSearch = newString.replaceAll("(?=[]\\[+&|!(){}^\"~=/$@%?:.\\\\])", "\\\\");
    newSearch = newSearch.replace("\n", "*");
    newSearch = newSearch.replace("\t", "*");
    newSearch = newSearch.replace("\r", "*");
    newSearch = newSearch.replace(" ", "\\ ");
    newSearch = newSearch.replace("**", "*");
    newSearch = newSearch.replace("***", "*");
    return newSearch;
  }

  public static String makeSearcableString(String search) {
    if (StringUtils.isBlank(search)) {
      return "";
    }
    String newSearch = search.replaceAll("[\\t\\n\\r]", " ");
    newSearch = newSearch.replaceAll("(?=[]\\[+&|!(){}^~=$/@%?:.\\\\-])", "\\\\");

    return newSearch.replace(" ", "\\ ");
  }

  public static void removeDoubleOrTripleEscapeFromFilters(SolrQuery solrQuery) {
    String[] filterQueries = solrQuery.getFilterQueries();
    List<String> newArray = new ArrayList<>();
    if (filterQueries != null && filterQueries.length > 0) {
      for (String filterQuery : filterQueries) {
        newArray.add(filterQuery.replaceAll("\\\\\\\\\\\\|\\\\\\\\", "\\\\"));
      }
    }
    solrQuery.setFilterQueries(newArray.toArray(new String[0]));
  }
  

  private static boolean isSolrFieldNumber(Map<String, Object> fieldTypeInfoMap) {
    if (MapUtils.isEmpty(fieldTypeInfoMap)) {
      return false;
    }
    String fieldTypeClassName = (String) fieldTypeInfoMap.get("class");
    return fieldTypeClassName.equalsIgnoreCase(TrieIntField.class.getSimpleName()) ||
           fieldTypeClassName.equalsIgnoreCase(TrieDoubleField.class.getSimpleName()) ||
           fieldTypeClassName.equalsIgnoreCase(TrieFloatField.class.getSimpleName()) ||
           fieldTypeClassName.equalsIgnoreCase(TrieLongField.class.getSimpleName());
  }

  public static String putWildCardByType(String str, String fieldType, String fieldTypeMetaData) {
    Map<String, Object> fieldTypeInfoMap = getFieldTypeInfoMap(fieldTypeMetaData);
    if (StringUtils.isNotBlank(fieldType)) {
      if (isSolrFieldNumber(fieldTypeInfoMap)) {
        String value = putEscapeCharacterForNumber(str, fieldTypeInfoMap);
        if (StringUtils.isNotBlank(value)) {
          return value;
        } else {
          return null;
        }
      } else if (checkTokenizer(StandardTokenizerFactory.class, fieldTypeInfoMap)) {
        return escapeForStandardTokenizer(str);
      } else if (checkTokenizer(KeywordTokenizerFactory.class, fieldTypeInfoMap) || "string".equalsIgnoreCase(fieldType)) {
        return makeSolrSearchStringWithoutAsterisk(str);
      } else if (checkTokenizer(PathHierarchyTokenizerFactory.class, fieldTypeInfoMap)) {
        return str;
      } else {
        return escapeQueryChars(str);
      }
    }
    return str;
  }

  private static String putEscapeCharacterForNumber(String str, Map<String, Object> fieldTypeInfoMap) {
    if (StringUtils.isNotEmpty(str)) {
      str = str.replace("*", "");
    }
    String escapeCharSting = parseInputValueAsPerFieldType(str, fieldTypeInfoMap);
    if (escapeCharSting == null || escapeCharSting.isEmpty()) {
      return null;
    }
    escapeCharSting = escapeCharSting.replace("-", "\\-");
    return escapeCharSting;
  }

  private static String parseInputValueAsPerFieldType(String str, Map<String, Object> fieldTypeInfoMap) {
    try {
      String className = (String) fieldTypeInfoMap.get("class");
      if (className.equalsIgnoreCase(TrieDoubleField.class.getSimpleName())) {
        return "" + Double.parseDouble(str);
      } else if (className.equalsIgnoreCase(TrieFloatField.class.getSimpleName())) {
        return "" + Float.parseFloat(str);
      } else if (className.equalsIgnoreCase(TrieLongField.class.getSimpleName())) {
        return "" + Long.parseLong(str);
      } else {
        return "" + Integer.parseInt(str);
      }
    } catch (Exception e) {
      return null;
    }
  }

  public static SolrQuery addListFilterToSolrQuery(SolrQuery solrQuery, String fieldName, String fieldValue) {
    if (org.apache.commons.lang.StringUtils.isNotEmpty(fieldValue)) {
      List<String> clusters = Splitter.on(",").splitToList(fieldValue);
      if (clusters.size() > 1) {
        solrQuery.addFilterQuery(String.format("%s:(%s)", fieldName, org.apache.commons.lang.StringUtils.join(clusters, " OR ")));
      } else {
        solrQuery.addFilterQuery(String.format("%s:%s", fieldName, clusters.get(0)));
      }
    }
    return solrQuery;
  }
  
  private static Map<String, Object> getFieldTypeInfoMap(String fieldTypeMetaData) {
    HashMap<String, Object> fieldTypeMap = JSONUtil.jsonToMapObject(fieldTypeMetaData);
    if (fieldTypeMap == null) {
      return new HashMap<>();
    }
    String classname = (String) fieldTypeMap.get("class");
    if (StringUtils.isNotBlank(classname)) {
      classname = classname.replace("solr.", "");
      fieldTypeMap.put("class", classname);
    }
    return fieldTypeMap;
  }
  
  //=============================================================================================================

  public static void setFacetField(SolrQuery solrQuery, String facetField) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_FIELD, facetField);
    setFacetLimit(solrQuery, -1);
  }

  public static void setFacetSort(SolrQuery solrQuery, String sortType) {
    solrQuery.setFacet(true);
    solrQuery.setFacetSort(sortType);
  }

  public static void setFacetPivot(SolrQuery solrQuery, int mincount, String... hirarchy) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_PIVOT, hirarchy);
    solrQuery.set(LogSearchConstants.FACET_PIVOT_MINCOUNT, mincount);
    setFacetLimit(solrQuery, -1);
  }

  private static void setFacetLimit(SolrQuery solrQuery, int limit) {
    solrQuery.set("facet.limit", limit);
  }

  public static void setRowCount(SolrQuery solrQuery, int rows) {
    if (rows > 0) {
      solrQuery.setRows(rows);
    } else {
      solrQuery.setRows(0);
      solrQuery.remove(LogSearchConstants.SORT);
    }
  }

  @SuppressWarnings("unchecked")
  private static boolean checkTokenizer(Class<? extends TokenizerFactory> tokenizerFactoryClass, Map<String, Object> fieldTypeInfoMap) {
    HashMap<String, Object> analyzer = (HashMap<String, Object>) fieldTypeInfoMap.get("analyzer");
    HashMap<String, Object> tokenizerMap = (HashMap<String, Object>)MapUtils.getObject(analyzer, "tokenizer");
    if (tokenizerMap != null) {
      String tokenizerClass = (String) tokenizerMap.get("class");
      if (StringUtils.isNotEmpty(tokenizerClass)) {
        tokenizerClass = tokenizerClass.replace("solr.", "");
        return tokenizerClass.equalsIgnoreCase(tokenizerFactoryClass.getSimpleName());
      }
    }
    
    return false;
  }
  
}