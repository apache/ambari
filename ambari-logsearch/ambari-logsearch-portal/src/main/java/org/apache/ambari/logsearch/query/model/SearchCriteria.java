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
package org.apache.ambari.logsearch.query.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ambari.logsearch.common.PropertiesHelper;

public class SearchCriteria {
  private int startIndex = 0;
  private int maxRows = Integer.MAX_VALUE;
  private String sortBy = null;
  private String sortType = null;
  private int page = 0;

  private String globalStartTime = null;
  private String globalEndTime = null;

  private HashMap<String, Object> paramList = new HashMap<String, Object>();

  private Map<String, Object> urlParamMap = new HashMap<String, Object>();

  public SearchCriteria() {
    // Auto-generated constructor stub
  }

  public int getStartIndex() {
    return startIndex;
  }

  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public int getMaxRows() {
    return maxRows;
  }

  public void setMaxRows(int maxRows) {
    this.maxRows = maxRows;
  }

  public String getSortType() {
    return sortType;
  }


  public void addParam(String name, Object value) {
    String solrValue = PropertiesHelper.getProperty(name);
    if (solrValue == null || solrValue.isEmpty()) {
      paramList.put(name, value);
    } else {
      try {
        String propertyFieldMappings[] = solrValue.split(",");
        HashMap<String, String> propertyFieldValue = new HashMap<String, String>();
        for (String temp : propertyFieldMappings) {
          String arrayValue[] = temp.split(":");
          propertyFieldValue.put(arrayValue[0].toLowerCase(Locale.ENGLISH), arrayValue[1].toLowerCase(Locale.ENGLISH));
        }
        String originalValue = propertyFieldValue.get(value.toString().toLowerCase(Locale.ENGLISH));
        if (originalValue != null && !originalValue.isEmpty())
          paramList.put(name, originalValue);

      } catch (Exception e) {
        //do nothing
      }
    }
  }

  public Object getParamValue(String name) {
    return paramList.get(name);
  }

  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public void setSortType(String sortType) {
    this.sortType = sortType;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public String getGlobalStartTime() {
    return globalStartTime;
  }

  public void setGlobalStartTime(String globalStartTime) {
    this.globalStartTime = globalStartTime;
  }

  public String getGlobalEndTime() {
    return globalEndTime;
  }

  public void setGlobalEndTime(String globalEndTime) {
    this.globalEndTime = globalEndTime;
  }

  public Map<String, Object> getUrlParamMap() {
    return urlParamMap;
  }

  public void setUrlParamMap(Map<String, Object> urlParamMap) {
    this.urlParamMap = urlParamMap;
  }

}