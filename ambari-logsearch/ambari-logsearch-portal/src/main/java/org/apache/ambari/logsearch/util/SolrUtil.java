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
import java.util.Locale;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class SolrUtil {
  static final Logger logger = Logger.getLogger("org.apache.ambari.logsearch");

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
   * @param fuzzyStr
   * @param string
   * @param searchList
   * @return
   */
  public String orList(String fieldName, String[] valueList, String fuzzyStr) {
    if (valueList == null || valueList.length == 0) {
      return "";
    }
    String expr = "";
    int count = -1;
    for (String value : valueList) {
      count++;
      if (count > 0) {
        expr += " OR ";
      }
      expr += fieldName + ":*" + value + "*";

    }
    if (valueList.length == 0) {
      return expr;
    } else {
      return "(" + expr + ")";
    }

  }
  
  /**
   * @param fuzzyStr
   * @param string
   * @param searchList
   * @return
   */
  public String orList(String fieldName, String[] valueList) {
    if (valueList == null || valueList.length == 0) {
      return "";
    }
    String expr = "";
    int count = -1;
    for (String value : valueList) {
      count++;
      if (count > 0) {
        expr += " OR ";
      }
      expr += fieldName + ":" + value;

    }
    if (valueList.length == 0) {
      return expr;
    } else {
      return "(" + expr + ")";
    }

  }
  
  

  /**
   * @param fuzzyStr
   * @param string
   * @param searchList
   * @return
   */
  public String andList(String fieldName, String[] valueList, String fuzzyStr) {
    if (valueList == null || valueList.length == 0) {
      return "";
    }
    String expr = "";
    int count = -1;
    for (String value : valueList) {
      count++;
      if (count > 0) {
        expr += " AND ";
      }
      expr += fieldName + ":*" + value + "*";
    }
    if (valueList.length == 0) {
      return expr;
    } else {
      return "(" + expr + ")";
    }

  }

  public String makeSolrSearchString(String search) {
    String newString = search.trim();
    String newSearch = newString.replaceAll(
        "(?=[]\\[+&|!(){}^~*=$@%?:.\\\\])", "\\\\");
    newSearch = newSearch.replace("\n", "*");
    newSearch = newSearch.replace("\t", "*");
    newSearch = newSearch.replace("\r", "*");
    newSearch = newSearch.replace(" ", "\\ ");
    newSearch = newSearch.replace("**", "*");
    newSearch = newSearch.replace("***", "*");
    return "*" + newSearch + "*";
  }
  
  public String makeSolrSearchStringWithoutAsterisk(String search) {
    String newString = search.trim();
    String newSearch = newString.replaceAll(
        "(?=[]\\[+&|!(){}^\"~=$@%?:.\\\\])", "\\\\");
    newSearch = newSearch.replace("\n", "*");
    newSearch = newSearch.replace("\t", "*");
    newSearch = newSearch.replace("\r", "*");
    newSearch = newSearch.replace(" ", "\\ ");
    newSearch = newSearch.replace("**", "*");
    newSearch = newSearch.replace("***", "*");
    return newSearch;
  }

  public String makeSearcableString(String search) {
    if(search == null || search.isEmpty())
      return "";
    String newSearch = search.replaceAll("[\\t\\n\\r]", " ");
    newSearch = newSearch.replaceAll(
        "(?=[]\\[+&|!(){}^~*=$/@%?:.\\\\-])", "\\\\");

    return newSearch.replace(" ", "\\ ");
  }

}