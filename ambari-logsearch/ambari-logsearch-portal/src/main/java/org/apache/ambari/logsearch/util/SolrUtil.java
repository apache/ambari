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
import org.apache.log4j.Logger;
import org.apache.solr.schema.TrieDoubleField;
import org.apache.solr.schema.TrieFloatField;
import org.apache.solr.schema.TrieIntField;
import org.apache.solr.schema.TrieLongField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SolrUtil {
  static final Logger logger = Logger.getLogger("org.apache.ambari.logsearch");
  
  @Autowired
  StringUtil stringUtil;

  @Autowired
  JSONUtil jsonUtil;

  public String setField(String fieldName, String value) {
    if (value == null || value.trim().length() == 0) {
      return "";
    }
    return fieldName + ":" + value.trim().toLowerCase(Locale.ENGLISH);
  }

  /**
   * @param string
   * @param myClassTypes
   * @return
   */
  public String inList(String fieldName, int[] values) {
    if (values == null || values.length == 0) {
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

  /**
   * @param familyUserIdSet
   * @return
   */
  public String inList(Collection<Long> values) {
    if (values == null || values.isEmpty()) {
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

  /**
   * @param wildCard
   * @param string
   * @param searchList
   * @return
   */
  public String orList(String fieldName, String[] valueList, String wildCard) {
    if (valueList == null || valueList.length == 0) {
      return "";
    }
    
    if(stringUtil.isEmpty(wildCard)){
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

  /**
   * @param wildCard
   * @param string
   * @param searchList
   * @return
   */
  public String andList(String fieldName, String[] valueList, String wildCard) {
    if (valueList == null || valueList.length == 0) {
      return "";
    }
    
    if(stringUtil.isEmpty(wildCard)){
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
   * 
   * @param s
   * @return
   */
  public String escapeQueryChars(String s) {
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

  public String escapeForWhiteSpaceTokenizer(String search) {
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

  public String escapeForStandardTokenizer(String search) {
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

  public String escapeForKeyTokenizer(String search) {
    if (search.startsWith("*") && search.endsWith("*")
        && !stringUtil.isEmpty(search)) {
      // Remove the * from both the sides
      if (search.length() > 1) {
        search = search.substring(1, search.length() - 1);
      }else{
        //search string have only * 
        search="";
      }
    }
    // Escape the string
    search = escapeQueryChars(search);

    // Add the *
    return "*" + search + "*";
  }

  /**
   * This is a special case scenario to handle log_message for wild card
   * scenarios
   * 
   * @param search
   * @return
   */
  public String escapeForLogMessage(String field, String search) {
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

  public String makeSolrSearchString(String search) {
    String newString = search.trim();
    String newSearch = newString.replaceAll(
        "(?=[]\\[+&|!(){},:\"^~/=$@%?:.\\\\])", "\\\\");
    newSearch = newSearch.replace("\n", "*");
    newSearch = newSearch.replace("\t", "*");
    newSearch = newSearch.replace("\r", "*");
    newSearch = newSearch.replace("**", "*");
    newSearch = newSearch.replace("***", "*");
    return "*" + newSearch + "*";
  }

  public String makeSolrSearchStringWithoutAsterisk(String search) {
    String newString = search.trim();
    String newSearch = newString.replaceAll(
        "(?=[]\\[+&|!(){}^\"~=/$@%?:.\\\\])", "\\\\");
    newSearch = newSearch.replace("\n", "*");
    newSearch = newSearch.replace("\t", "*");
    newSearch = newSearch.replace("\r", "*");
    newSearch = newSearch.replace(" ", "\\ ");
    newSearch = newSearch.replace("**", "*");
    newSearch = newSearch.replace("***", "*");
    return newSearch;
  }

  public String makeSearcableString(String search) {
    if (search == null || search.isEmpty()){
      return "";
    }
    String newSearch = search.replaceAll("[\\t\\n\\r]", " ");
    newSearch = newSearch.replaceAll("(?=[]\\[+&|!(){}^~=$/@%?:.\\\\-])",
        "\\\\");

    return newSearch.replace(" ", "\\ ");
  }
  

  public boolean isSolrFieldNumber(String fieldType,SolrDaoBase solrDaoBase) {
    if (stringUtil.isEmpty(fieldType)) {
      return false;
    } else {
      HashMap<String, Object> typeInfoMap = getFieldTypeInfoMap(fieldType,solrDaoBase);
      if (typeInfoMap == null || typeInfoMap.isEmpty()) {
        return false;
      }
      String fieldTypeClassName = (String) typeInfoMap.get("class");
      if (fieldTypeClassName.equalsIgnoreCase(TrieIntField.class
          .getSimpleName())) {
        return true;
      }
      if (fieldTypeClassName.equalsIgnoreCase(TrieDoubleField.class
          .getSimpleName())) {
        return true;
      }
      if (fieldTypeClassName.equalsIgnoreCase(TrieFloatField.class
          .getSimpleName())) {
        return true;
      }
      if (fieldTypeClassName.equalsIgnoreCase(TrieLongField.class
          .getSimpleName())) {
        return true;
      }
      return false;
    }
  }
  
  public HashMap<String, Object> getFieldTypeInfoMap(String fieldType,SolrDaoBase solrDaoBase) {
    String fieldTypeMetaData = solrDaoBase.schemaFieldTypeMap.get(fieldType);
    HashMap<String, Object> fieldTypeMap = jsonUtil
        .jsonToMapObject(fieldTypeMetaData);
    if (fieldTypeMap == null) {
      return new HashMap<String, Object>();
    }
    String classname = (String) fieldTypeMap.get("class");
    if (!stringUtil.isEmpty(classname)) {
      classname = classname.replace("solr.", "");
      fieldTypeMap.put("class", classname);
    }
    return fieldTypeMap;
  }
}