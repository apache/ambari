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
package org.apache.ambari.logsearch.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;

public class SearchCriteria {
  private int startIndex = 0;
  private int maxRows = Integer.MAX_VALUE;
  private String sortBy = null;
  private String sortType = null;
  private int page = 0;

  private String globalStartTime = null;
  private String globalEndTime = null;

  private boolean getCount = true;
  private boolean isDistinct = false;
  private HashMap<String, Object> paramList = new HashMap<String, Object>();
  final private Set<String> nullParamList = new HashSet<String>();
  final private Set<String> notNullParamList = new HashSet<String>();

  private Map<String, Object> urlParamMap = new HashMap<String, Object>();

  public SearchCriteria(HttpServletRequest request) {
    try {
      if (request.getParameter("startIndex") != null && (!request.getParameter("startIndex").isEmpty())) {
        this.startIndex = new Integer(request.getParameter("startIndex"));
      }
      if (request.getParameter("page") != null && (!request.getParameter("page").isEmpty())) {
        this.page = new Integer(request.getParameter("page"));
      }
      if (request.getParameter("pageSize") != null && (!request.getParameter("pageSize").isEmpty())) {
        this.maxRows = new Integer(request.getParameter("pageSize"));
      } else {
        this.maxRows = PropertiesHelper.getIntProperty("db.maxResults", 50);
      }
    } catch (NumberFormatException e) {
      // do nothing
    }

    // Sort fields
    if (request.getParameter("sortBy") != null && (!request.getParameter("sortBy").isEmpty())) {
      this.sortBy = "" + request.getParameter("sortBy");
    }
    if (request.getParameter("sortType") != null && (!request.getParameter("sortType").isEmpty())) {
      this.sortType = "" + request.getParameter("sortType");
    }

    // url params
    if (request.getParameter("start_time") != null && (!request.getParameter("start_time").isEmpty())) {
      this.globalStartTime = "" + request.getParameter("start_time");
      this.urlParamMap.put("globalStartTime", request.getParameter("start_time"));
    }
    if (request.getParameter("end_time") != null && (!request.getParameter("end_time").isEmpty())) {
      this.globalEndTime = "" + request.getParameter("end_time");
      this.urlParamMap.put("globalEndTime", request.getParameter("end_time"));
    }
  }

  public SearchCriteria() {
    // Auto-generated constructor stub
  }

  /**
   * @return the startIndex
   */
  public int getStartIndex() {
    return startIndex;
  }

  /**
   * @param startIndex the startIndex to set
   */
  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  /**
   * @return the maxRows
   */
  public int getMaxRows() {
    return maxRows;
  }

  /**
   * @param maxRows the maxRows to set
   */
  public void setMaxRows(int maxRows) {
    this.maxRows = maxRows;
  }

  /**
   * @return the sortType
   */

  public String getSortType() {
    return sortType;
  }

  /**
   * @param sortType the sortType to set
   */

  public boolean isGetCount() {
    return getCount;
  }

  public void setGetCount(boolean getCount) {
    this.getCount = getCount;
  }

  /**
   * @return the paramList
   */
  public HashMap<String, Object> getParamList() {
    return paramList;
  }

  /**
   * @param paramList the paramList to set
   */
  public void setParamList(HashMap<String, Object> paramList) {
    this.paramList = paramList;
  }

  /**
   * @param request
   */
  public void addRequiredServiceLogsParams(HttpServletRequest request) {
    this.addParam("advanceSearch", StringEscapeUtils.unescapeXml(request.getParameter("advanceSearch")));
    this.addParam("q", request.getParameter("q"));
    this.addParam("treeParams", StringEscapeUtils.unescapeHtml(request.getParameter("treeParams")));
    this.addParam("level", request.getParameter("level"));
    this.addParam("gMustNot", request.getParameter("gMustNot"));
    this.addParam("from", request.getParameter("from"));
    this.addParam("to", request.getParameter("to"));
    this.addParam("selectComp", request.getParameter("mustBe"));
    this.addParam("unselectComp", request.getParameter("mustNot"));
    this.addParam("iMessage", StringEscapeUtils.unescapeXml(request.getParameter("iMessage")));
    this.addParam("gEMessage", StringEscapeUtils.unescapeXml(request.getParameter("gEMessage")));
    this.addParam("eMessage", StringEscapeUtils.unescapeXml(request.getParameter("eMessage")));
    this.addParam(LogSearchConstants.BUNDLE_ID, request.getParameter(LogSearchConstants.BUNDLE_ID));
    this.addParam("host_name", request.getParameter("host_name"));
    this.addParam("component_name", request.getParameter("component_name"));
    this.addParam("file_name", request.getParameter("file_name"));
    this.addParam("startDate", request.getParameter("start_time"));
    this.addParam("endDate", request.getParameter("end_time"));
    this.addParam("excludeQuery", StringEscapeUtils.unescapeXml(request.getParameter("excludeQuery")));
    this.addParam("includeQuery", StringEscapeUtils.unescapeXml(request.getParameter("includeQuery")));
  }

  /**
   * @param request
   */
  public void addRequiredAuditLogsParams(HttpServletRequest request) {
    this.addParam("q", request.getParameter("q"));
    this.addParam("columnQuery", StringEscapeUtils.unescapeXml(request.getParameter("columnQuery")));
    this.addParam("iMessage", StringEscapeUtils.unescapeXml(request.getParameter("iMessage")));
    this.addParam("gEMessage", StringEscapeUtils.unescapeXml(request.getParameter("gEMessage")));
    this.addParam("eMessage", StringEscapeUtils.unescapeXml(request.getParameter("eMessage")));
    this.addParam("includeString", request.getParameter("mustBe"));
    this.addParam("unselectComp", request.getParameter("mustNot"));
    this.addParam("excludeQuery", StringEscapeUtils.unescapeXml(request.getParameter("excludeQuery")));
    this.addParam("includeQuery", StringEscapeUtils.unescapeXml(request.getParameter("includeQuery")));
    this.addParam("startTime", request.getParameter("from"));
    this.addParam("endTime", request.getParameter("to"));
  }

  /**
   * @param string
   * @param caId
   */
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

  public void setNullParam(String name) {
    nullParamList.add(name);
  }

  public void setNotNullParam(String name) {
    notNullParamList.add(name);
  }

  public Object getParamValue(String name) {
    return paramList.get(name);
  }

  /**
   * @return the nullParamList
   */
  public Set<String> getNullParamList() {
    return nullParamList;
  }

  /**
   * @return the notNullParamList
   */
  public Set<String> getNotNullParamList() {
    return notNullParamList;
  }

  /**
   * @return the isDistinct
   */
  public boolean isDistinct() {
    return isDistinct;
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

  /**
   * @param isDistinct the isDistinct to set
   */
  public void setDistinct(boolean isDistinct) {
    this.isDistinct = isDistinct;
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