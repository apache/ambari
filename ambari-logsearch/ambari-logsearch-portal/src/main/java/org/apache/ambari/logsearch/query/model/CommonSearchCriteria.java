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

public class CommonSearchCriteria extends SearchCriteria {
  private int startIndex = 0;
  private int maxRows = Integer.MAX_VALUE;
  private String sortBy = null;
  private String sortType = null;
  private int page = 0;

  private String globalStartTime = null;
  private String globalEndTime = null;

  @Override
  public int getStartIndex() {
    return startIndex;
  }

  @Override
  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  @Override
  public int getMaxRows() {
    return maxRows;
  }

  @Override
  public void setMaxRows(int maxRows) {
    this.maxRows = maxRows;
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
  public String getSortBy() {
    return sortBy;
  }

  @Override
  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  @Override
  public int getPage() {
    return page;
  }

  @Override
  public void setPage(int page) {
    this.page = page;
  }

  @Override
  public String getGlobalStartTime() {
    return globalStartTime;
  }

  @Override
  public void setGlobalStartTime(String globalStartTime) {
    this.globalStartTime = globalStartTime;
  }

  @Override
  public String getGlobalEndTime() {
    return globalEndTime;
  }

  @Override
  public void setGlobalEndTime(String globalEndTime) {
    this.globalEndTime = globalEndTime;
  }
}
