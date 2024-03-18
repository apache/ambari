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

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LSServerLogLevelFilter {

  @NotNull
  @ApiModelProperty
  private String label;
  
  @NotNull
  @ApiModelProperty
  private List<String> hosts;
  
  @NotNull
  @ApiModelProperty
  private List<String> defaultLevels;
  
  @ApiModelProperty
  private List<String> overrideLevels;
  
  @ApiModelProperty
  private Date expiryTime;

  public LSServerLogLevelFilter() {}

  public LSServerLogLevelFilter(LogLevelFilter logLevelFilter) {
    label = logLevelFilter.getLabel();
    hosts = logLevelFilter.getHosts();
    defaultLevels = logLevelFilter.getDefaultLevels();
    overrideLevels = logLevelFilter.getOverrideLevels();
    expiryTime = logLevelFilter.getExpiryTime();
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

  public Date getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(Date expiryTime) {
    this.expiryTime = expiryTime;
  }

  public LogLevelFilter convertToApi() {
    LogLevelFilter apiFilter = new LogLevelFilter();
    
    apiFilter.setLabel(label);
    apiFilter.setHosts(hosts);
    apiFilter.setDefaultLevels(defaultLevels);
    apiFilter.setOverrideLevels(overrideLevels);
    apiFilter.setExpiryTime(expiryTime);
    
    return apiFilter;
  }
}
