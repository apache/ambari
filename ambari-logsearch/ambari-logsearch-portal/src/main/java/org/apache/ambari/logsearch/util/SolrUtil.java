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
import org.apache.commons.lang.StringUtils;

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
    String newString = search.trim();
    String newSearch = escapeQueryChars(newString);
    boolean isSingleWord = true;
    for (int i = 0; i < search.length(); i++) {
      if (Character.isWhitespace(search.charAt(i))) {
        isSingleWord = false;
      }
    }
    if (!isSingleWord) {
      newSearch = "\"" + newSearch + "\"";
    }

    return newSearch;
  }

  private static String escapeForKeyTokenizer(String search) {
    if (search.startsWith("*") && search.endsWith("*") && StringUtils.isNotBlank(search)) {
      // Remove the * from both the sides
      if (search.length() > 1) {
        search = search.substring(1, search.length() - 1);
      } else {
        //search string have only * 
        search="";
      }
    }
    search = escapeQueryChars(search);

    return "*" + search + "*";
  }

  /**
   * This is a special case scenario to handle log_message for wild card
   * scenarios
   */
  public static String escapeForLogMessage(String search) {
    if (search.startsWith("*") && search.endsWith("*")) {
      search = escapeForKeyTokenizer(search);
    } else {
      // Use whitespace index
      search = escapeForStandardTokenizer(search);
    }
    return search;
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
  

  private static boolean isSolrFieldNumber(String fieldType, Map<String, String> schemaFieldsMap) {
    if (StringUtils.isBlank(fieldType)) {
      return false;
    } else {
      HashMap<String, Object> typeInfoMap = getFieldTypeInfoMap(fieldType, schemaFieldsMap);
      if (MapUtils.isEmpty(typeInfoMap)) {
        return false;
      }
      String fieldTypeClassName = (String) typeInfoMap.get("class");
      return fieldTypeClassName.equalsIgnoreCase(TrieIntField.class.getSimpleName()) ||
             fieldTypeClassName.equalsIgnoreCase(TrieDoubleField.class.getSimpleName()) ||
             fieldTypeClassName.equalsIgnoreCase(TrieFloatField.class.getSimpleName()) ||
             fieldTypeClassName.equalsIgnoreCase(TrieLongField.class.getSimpleName());
    }
  }

  public static String putWildCardByType(String str, String key, Map<String, String> schemaFieldsMap) {
    String fieldType = schemaFieldsMap.get(key);
    if (StringUtils.isNotBlank(fieldType)) {
      if (isSolrFieldNumber(fieldType, schemaFieldsMap)) {
        String value = putEscapeCharacterForNumber(str, fieldType, schemaFieldsMap);
        if (StringUtils.isNotBlank(value)) {
          return value;
        } else {
          return null;
        }
      } else if (checkTokenizer(fieldType, StandardTokenizerFactory.class, schemaFieldsMap)) {
        return escapeForStandardTokenizer(str);
      } else if (checkTokenizer(fieldType, KeywordTokenizerFactory.class, schemaFieldsMap)|| "string".equalsIgnoreCase(fieldType)) {
        return makeSolrSearchStringWithoutAsterisk(str);
      } else if (checkTokenizer(fieldType, PathHierarchyTokenizerFactory.class, schemaFieldsMap)) {
        return str;
      }
    }
    return str;
  }

  private static String putEscapeCharacterForNumber(String str,String fieldType, Map<String, String> schemaFieldsMap) {
    if (StringUtils.isNotEmpty(str)) {
      str = str.replace("*", "");
    }
    String escapeCharSting = parseInputValueAsPerFieldType(str,fieldType, schemaFieldsMap);
    if (escapeCharSting == null || escapeCharSting.isEmpty()) {
      return null;
    }
    escapeCharSting = escapeCharSting.replace("-", "\\-");
    return escapeCharSting;
  }

  private static String parseInputValueAsPerFieldType(String str,String fieldType, Map<String, String> schemaFieldsMap) {
    try {
      HashMap<String, Object> fieldTypeInfoMap = SolrUtil.getFieldTypeInfoMap(fieldType, schemaFieldsMap);
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
  
  private static HashMap<String, Object> getFieldTypeInfoMap(String fieldType, Map<String, String> schemaFieldsTypeMap) {
    String fieldTypeMetaData = schemaFieldsTypeMap.get(fieldType);
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
  private static boolean checkTokenizer(String fieldType, Class<? extends TokenizerFactory> tokenizerFactoryClass,
      Map<String, String> schemaFieldsMap) {
    HashMap<String, Object> fieldTypeMap = SolrUtil.getFieldTypeInfoMap(fieldType ,schemaFieldsMap);
    HashMap<String, Object> analyzer = (HashMap<String, Object>) fieldTypeMap.get("analyzer");
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