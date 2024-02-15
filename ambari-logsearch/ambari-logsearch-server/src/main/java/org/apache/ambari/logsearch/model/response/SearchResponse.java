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
package org.apache.ambari.logsearch.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SearchResponse {
  /**
   * Start index for the result
   */
  private int startIndex;
  /**
   * Page size used for the result
   */
  private int pageSize;
  /**
   * Total records in the database for the given search conditions
   */
  private long totalCount;
  /**
   * Number of rows returned for the search condition
   */
  private int resultSize;
  /**
   * Sort type. Either desc or asc
   */
  private String sortType;
  /**
   * Comma seperated list of the fields for sorting
   */
  private String sortBy;

  private long queryTimeMS = System.currentTimeMillis();

  public int getStartIndex() {
    return startIndex;
  }

  public int getPageSize() {
    return pageSize;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public int getResultSize() {
    return resultSize;
  }

  public String getSortType() {
    return sortType;
  }

  public String getSortBy() {
    return sortBy;
  }

  public long getQueryTimeMS() {
    return queryTimeMS;
  }

  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  public void setResultSize(int resultSize) {
    this.resultSize = resultSize;
  }

  public void setSortType(String sortType) {
    this.sortType = sortType;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public void setQueryTimeMS(long queryTimeMS) {
    this.queryTimeMS = queryTimeMS;
  }

  public abstract int getListSize();

}
