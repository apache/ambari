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

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.schema.TrieDoubleField;
import org.apache.solr.schema.TrieFloatField;
import org.apache.solr.schema.TrieIntField;
import org.apache.solr.schema.TrieLongField;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

public class SolrUtil {
  private SolrUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static String setField(String fieldName, String value) {
    if (value == null || value.trim().length() == 0) {
      return "";
    }
    return fieldName + ":" + value.trim().toLowerCase(Locale.ENGLISH);
  }

  public static String inList(String fieldName, int[] values) {
    if (ArrayUtils.isEmpty(values)) {
      return "";
    }
    String expr = "";
    // Add the filter queries
    for (int i : values) {
      expr += i + " ";
    }
    if (values.length == 0) {
      return fieldName + ":" + expr;
    } else {
      return fieldName + ":(" + expr + ")";
    }
  }

  public static String inList(Collection<Long> values) {
    if (CollectionUtils.isEmpty(values)) {
      return "";
    }
    String expr = "";
    for (Long value : values) {
      expr += value.toString() + " ";
    }

    if (values.isEmpty()) {
      return expr.trim();
    } else {
      return "(" + expr.trim() + ")";
    }

  }

  public static String orList(String fieldName, String[] valueList, String wildCard) {
    if (ArrayUtils.isEmpty(valueList)) {
      return "";
    }
    
    if (StringUtils.isBlank(wildCard)) {
      wildCard = "";
    }
    
    StringBuilder expr = new StringBuilder();
    int count = -1;
    for (String value : valueList) {
      count++;
      if (count > 0) {
        expr.append(" OR ");
      }
      
      expr.append( fieldName + ":"+ wildCard + value + wildCard);

    }
    if (valueList.length == 0) {
      return expr.toString();
    } else {
      return "(" + expr + ")";
    }

  }

  public static String andList(String fieldName, String[] valueList, String wildCard) {
    if (ArrayUtils.isEmpty(valueList)) {
      return "";
    }
    
    if (StringUtils.isBlank(wildCard)) {
      wildCard = "";
    }
    
    StringBuilder expr = new StringBuilder();
    int count = -1;
    for (String value : valueList) {
      count++;
      if (count > 0) {
        expr.append(" AND ");
      }
      
      expr.append( fieldName + ":"+ wildCard + value + wildCard);

    }
    if (valueList.length == 0) {
      return expr.toString();
    } else {
      return "(" + expr + ")";
    }

  }

  /**
   * Copied from Solr ClientUtils.escapeQueryChars and removed escaping *
   */
  public static String escapeQueryChars(String s) {
    StringBuilder sb = new StringBuilder();
    int prev = 0;
    if (s != null) {
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        int ic = (int)c;
        if( ic == 10 ) {
          if( prev != 13) {
            //Let's insert \r
            sb.append('\\');
            sb.append((char)13);
          }
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

  private static String escapeForWhiteSpaceTokenizer(String search) {
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
    if (search.startsWith("*") && search.endsWith("*") && !StringUtils.isBlank(search)) {
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
  public static String escapeForLogMessage(String field, String search) {
    if (search.startsWith("*") && search.endsWith("*")) {
      field = LogSearchConstants.SOLR_KEY_LOG_MESSAGE;
      search = escapeForKeyTokenizer(search);
    } else {
      // Use whitespace index
      field = LogSearchConstants.SOLR_LOG_MESSAGE;
      search = escapeForWhiteSpaceTokenizer(search);
    }
    return field + ":" + search;
  }

  public static String makeSolrSearchString(String search) {
    String newString = search.trim();
    String newSearch = newString.replaceAll("(?=[]\\[+&|!(){},:\"^~/=$@%?:.\\\\])", "\\\\");
    newSearch = newSearch.replace("\n", "*");
    newSearch = newSearch.replace("\t", "*");
    newSearch = newSearch.replace("\r", "*");
    newSearch = newSearch.replace("**", "*");
    newSearch = newSearch.replace("***", "*");
    return "*" + newSearch + "*";
  }

  public static String makeSolrSearchStringWithoutAsterisk(String search) {
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
  

  public static boolean isSolrFieldNumber(String fieldType,SolrDaoBase solrDaoBase) {
    if (StringUtils.isBlank(fieldType)) {
      return false;
    } else {
      HashMap<String, Object> typeInfoMap = getFieldTypeInfoMap(fieldType,solrDaoBase);
      if (typeInfoMap == null || typeInfoMap.isEmpty()) {
        return false;
      }
      String fieldTypeClassName = (String) typeInfoMap.get("class");
      if (fieldTypeClassName.equalsIgnoreCase(TrieIntField.class.getSimpleName())) {
        return true;
      }
      if (fieldTypeClassName.equalsIgnoreCase(TrieDoubleField.class.getSimpleName())) {
        return true;
      }
      if (fieldTypeClassName.equalsIgnoreCase(TrieFloatField.class.getSimpleName())) {
        return true;
      }
      if (fieldTypeClassName.equalsIgnoreCase(TrieLongField.class.getSimpleName())) {
        return true;
      }
      return false;
    }
  }
  
  public static HashMap<String, Object> getFieldTypeInfoMap(String fieldType,SolrDaoBase solrDaoBase) {
    String fieldTypeMetaData = solrDaoBase.schemaFieldTypeMap.get(fieldType);
    HashMap<String, Object> fieldTypeMap = JSONUtil.jsonToMapObject(fieldTypeMetaData);
    if (fieldTypeMap == null) {
      return new HashMap<String, Object>();
    }
    String classname = (String) fieldTypeMap.get("class");
    if (!StringUtils.isBlank(classname)) {
      classname = classname.replace("solr.", "");
      fieldTypeMap.put("class", classname);
    }
    return fieldTypeMap;
  }
  
  //=============================================================================================================
  
  //Solr Facet Methods
  public static void setFacetField(SolrQuery solrQuery, String facetField) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_FIELD, facetField);
    setFacetLimit(solrQuery, -1);
  }

  public static void setJSONFacet(SolrQuery solrQuery, String jsonQuery) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_JSON_FIELD, jsonQuery);
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

  public static void setFacetDate(SolrQuery solrQuery, String facetField, String from, String to, String unit) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_DATE, facetField);
    solrQuery.set(LogSearchConstants.FACET_DATE_START, from);
    solrQuery.set(LogSearchConstants.FACET_DATE_END, to);
    solrQuery.set(LogSearchConstants.FACET_DATE_GAP, unit);
    solrQuery.set(LogSearchConstants.FACET_MINCOUNT, 0);
    setFacetLimit(solrQuery, -1);
  }

  public static void setFacetRange(SolrQuery solrQuery, String facetField, String from, String to, String unit) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_RANGE, facetField);
    solrQuery.set(LogSearchConstants.FACET_RANGE_START, from);
    solrQuery.set(LogSearchConstants.FACET_RANGE_END, to);
    solrQuery.set(LogSearchConstants.FACET_RANGE_GAP, unit);
    solrQuery.set(LogSearchConstants.FACET_MINCOUNT, 0);
    setFacetLimit(solrQuery, -1);
  }

  public static void setFacetLimit(SolrQuery solrQuery, int limit) {
    solrQuery.set("facet.limit", limit);
  }

  //Solr Group Mehtods
  public static void setGroupField(SolrQuery solrQuery, String groupField, int rows) {
    solrQuery.set(LogSearchConstants.FACET_GROUP, true);
    solrQuery.set(LogSearchConstants.FACET_GROUP_FIELD, groupField);
    solrQuery.set(LogSearchConstants.FACET_GROUP_MAIN, true);
    setRowCount(solrQuery, rows);
  }

  //Main Query
  public static void setMainQuery(SolrQuery solrQuery, String query) {
    String defalultQuery = "*:*";
    if (StringUtils.isBlank(query)){
      solrQuery.setQuery(defalultQuery);
    }else{
      solrQuery.setQuery(query);
    }
  }

  public static void setStart(SolrQuery solrQuery, int start) {
    int defaultStart = 0;
    if (start > defaultStart) {
      solrQuery.setStart(start);
    } else {
      solrQuery.setStart(defaultStart);
    }
  }

  //Set Number of Rows
  public static void setRowCount(SolrQuery solrQuery, int rows) {
    if (rows > 0) {
      solrQuery.setRows(rows);
    } else {
      solrQuery.setRows(0);
      solrQuery.remove(LogSearchConstants.SORT);
    }
  }

  //Solr Facet Methods
  public static void setFacetFieldWithMincount(SolrQuery solrQuery, String facetField, int minCount) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_FIELD, facetField);
    solrQuery.set(LogSearchConstants.FACET_MINCOUNT, minCount);
    setFacetLimit(solrQuery, -1);
  }
  
  public static void setFl(SolrQuery solrQuery,String field){
    solrQuery.set(LogSearchConstants.FL, field);
  }
  
}