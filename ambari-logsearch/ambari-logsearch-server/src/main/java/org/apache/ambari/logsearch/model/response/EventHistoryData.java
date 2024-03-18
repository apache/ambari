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

import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.List;

public class EventHistoryData {

  @ApiModelProperty
  private String id;

  @ApiModelProperty
  private String userName;

  @ApiModelProperty
  private String filtername;

  @ApiModelProperty
  private String values;

  @ApiModelProperty
  private List<String> shareNameList;

  @ApiModelProperty
  private String rowType;

  public EventHistoryData() {
    id = String.valueOf(new Date().getTime());
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getFiltername() {
    return filtername;
  }

  public void setFiltername(String filtername) {
    this.filtername = filtername;
  }

  public List<String> getShareNameList() {
    return shareNameList;
  }

  public void setShareNameList(List<String> shareNameList) {
    this.shareNameList = shareNameList;
  }

  public String getValues() {
    return values;
  }

  public void setValues(String values) {
    this.values = values;
  }

  public String getRowType() {
    return rowType;
  }

  public void setRowType(String rowType) {
    this.rowType = rowType;
  }
}
