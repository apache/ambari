/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller;


import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.List;

public class ServiceConfigVersionResponse {
  private String clusterName;
  private String serviceName;
  private Long version;
  private Long createTime;
  private Long groupId;
  private String groupName;
  private String userName;
  private String note;
  private Boolean isCurrent = false;
  private List<ConfigurationResponse> configurations;
  private List<String> hosts;

  @JsonProperty("service_name")
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @JsonProperty("service_config_version")
  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  @JsonProperty("createtime")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  @JsonProperty("user")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  @JsonProperty("cluster_name")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public List<ConfigurationResponse> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(List<ConfigurationResponse> configurations) {
    this.configurations = configurations;
  }

  @JsonProperty("service_config_version_note")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public List<String> getHosts() {
    return hosts;
  }

  @JsonProperty("hosts")
  public void setHosts(List<String> hosts) {
    this.hosts = hosts;
  }

  @JsonProperty("group_name")
  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  @JsonProperty("group_id")
  public Long getGroupId() {
    return groupId;
  }

  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }

  @JsonProperty("is_current")
  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
  }
}

