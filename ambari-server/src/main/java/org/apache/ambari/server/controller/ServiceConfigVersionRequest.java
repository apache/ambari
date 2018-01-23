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


import java.util.List;

public class ServiceConfigVersionRequest {
  private String clusterName;
  private String serviceName;
  private String serviceGroupName;
  private Long version;
  private Long createTime;
  private Long applyTime;
  private String userName;
  private String note;
  private Boolean isCurrent;
  private List<ConfigurationRequest> configs;
  private String stackId;

  public ServiceConfigVersionRequest() {
  }

  public ServiceConfigVersionRequest(String clusterName, String serviceGroupName, String serviceName, Long version, Long createTime, Long applyTime, String userName, Boolean isCurrent, String note, String stackId) {
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.serviceName = serviceName;
    this.version = version;
    this.createTime = createTime;
    this.applyTime = applyTime;
    this.userName = userName;
    this.isCurrent = isCurrent;
    this.note = note;
    this.stackId = stackId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  public Long getApplyTime() {
    return applyTime;
  }

  public void setApplyTime(Long applyTime) {
    this.applyTime = applyTime;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
  }

  public String getServiceGroupName() {
    return serviceGroupName;
  }

  public void setServiceGroupName(String serviceGroupName) {
    this.serviceGroupName = serviceGroupName;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public void setConfigs(List<ConfigurationRequest> configRequests) {
    configs = configRequests;
  }

  public List<ConfigurationRequest> getConfigs() {
    return configs;
  }

  @Override
  public String toString() {
    return "ServiceConfigVersionRequest{" +
        "clusterName='" + clusterName + '\'' +
        ", serviceGroupName='" + serviceGroupName + '\'' +
        ", serviceName='" + serviceName + '\'' +
        ", version=" + version +
        ", createTime=" + createTime +
        ", applyTime=" + applyTime +
        ", userName='" + userName + '\'' +
        ", note='" + note + '\'' +
        ", isCurrent=" + isCurrent +
        '}';
  }
}
