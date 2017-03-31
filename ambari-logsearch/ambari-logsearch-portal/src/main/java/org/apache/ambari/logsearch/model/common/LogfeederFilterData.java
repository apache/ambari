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
package org.apache.ambari.logsearch.model.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel
public class LogfeederFilterData {

  @ApiModelProperty
  private String label;

  @ApiModelProperty
  private List<String> hosts = new ArrayList<>();

  @ApiModelProperty
  private List<String> defaultLevels = new ArrayList<>();

  @ApiModelProperty
  private List<String> overrideLevels = new ArrayList<>();

  @ApiModelProperty
  private String expiryTime;

  public LogfeederFilterData() {
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public List<String> getHosts() {
    return hosts;
  }

  public void setHosts(List<String> hosts) {
    this.hosts = hosts;
  }

  public List<String> getDefaultLevels() {
    return defaultLevels;
  }

  public void setDefaultLevels(List<String> defaultLevels) {
    this.defaultLevels = defaultLevels;
  }

  public List<String> getOverrideLevels() {
    return overrideLevels;
  }

  public void setOverrideLevels(List<String> overrideLevels) {
    this.overrideLevels = overrideLevels;
  }

  public String getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(String expiryTime) {
    this.expiryTime = expiryTime;
  }
}
