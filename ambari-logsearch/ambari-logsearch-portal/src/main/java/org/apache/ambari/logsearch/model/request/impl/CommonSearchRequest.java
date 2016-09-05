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
package org.apache.ambari.logsearch.model.request.impl;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.CommonSearchParamDefinition;
import org.apache.ambari.logsearch.model.request.SearchRequest;

import javax.ws.rs.QueryParam;

public class CommonSearchRequest implements SearchRequest, CommonSearchParamDefinition {

  @QueryParam(LogSearchConstants.REQUEST_PARAM_START_INDEX)
  private String startIndex;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_PAGE)
  private String page;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_PAGE_SIZE)
  private String pageSize;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_SORT_BY)
  private String sortBy;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_SORT_TYPE)
  private String sortType;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_START_TIME)
  private String startTime;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_END_TIME)
  private String endTime;

  @Override
  public String getStartIndex() {
    return startIndex;
  }

  @Override
  public void setStartIndex(String startIndex) {
    this.startIndex = startIndex;
  }

  @Override
  public String getPage() {
    return page;
  }

  @Override
  public void setPage(String page) {
    this.page = page;
  }

  @Override
  public String getPageSize() {
    return pageSize;
  }

  @Override
  public void setPageSize(String pageSize) {
    this.pageSize = pageSize;
  }

  @Override
  public String getSortBy() {
    return sortBy;
  }

  @Override
  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  @Override
  public String getSortType() {
    return sortType;
  }

  @Override
  public void setSortType(String sortType) {
    this.sortType = sortType;
  }

  @Override
  public String getStartTime() {
    return startTime;
  }

  @Override
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  @Override
  public String getEndTime() {
    return endTime;
  }

  @Override
  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }
}
