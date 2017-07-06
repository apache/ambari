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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Collection;

@ApiModel
public class EventHistoryDataListResponse extends SearchResponse{

  @ApiModelProperty
  private String name;

  @ApiModelProperty
  private Collection<EventHistoryData> eventHistoryDataList;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Collection<EventHistoryData> getEventHistoryDataList() {
    return eventHistoryDataList;
  }

  public void setEventHistoryDataList(Collection<EventHistoryData> eventHistoryDataList) {
    this.eventHistoryDataList = eventHistoryDataList;
  }

  @Override
  public int getListSize() {
    return eventHistoryDataList != null ? eventHistoryDataList.size() : 0;
  }
}
