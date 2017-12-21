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


public class ServiceDependencyResponse {

  private Long clusterId;
  private Long serviceId;
  private Long serviceGroupId;
  private Long dependencyClusterId;
  private Long dependencyServiceId;
  private Long dependencyServiceGroupId;
  private Long dependencyId;
  private String clusterName;
  private String serviceName;
  private String serviceGroupName;
  private String dependencyServiceName;
  private String dependencyClusterName;
  private String dependencyServiceGroupName;

  public ServiceDependencyResponse(Long clusterId, String clusterName, Long dependencyClusterId, String dependencyClusterName,
                                   Long dependencyServiceGroupId, String dependencyServiceGroupName, Long dependencyServiceId,
                                   String dependencyServiceName, Long serviceGroupId, String serviceGroupName,
                                   Long serviceId, String serviceName, Long dependencyId) {
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.dependencyClusterId = dependencyClusterId;
    this.dependencyClusterName = dependencyClusterName;
    this.dependencyServiceGroupId = dependencyServiceGroupId;
    this.dependencyServiceGroupName = dependencyServiceGroupName;
    this.dependencyServiceId = dependencyServiceId;
    this.dependencyServiceName = dependencyServiceName;
    this.serviceGroupId = serviceGroupId;
    this.serviceGroupName = serviceGroupName;
    this.serviceId = serviceId;
    this.serviceName = serviceName;
    this.dependencyId = dependencyId;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public Long getDependencyClusterId() {
    return dependencyClusterId;
  }

  public void setDependencyClusterId(Long dependencyClusterId) {
    this.dependencyClusterId = dependencyClusterId;
  }

  public String getDependencyClusterName() {
    return dependencyClusterName;
  }

  public void setDependencyClusterName(String dependencyClusterName) {
    this.dependencyClusterName = dependencyClusterName;
  }

  public Long getDependencyServiceGroupId() {
    return dependencyServiceGroupId;
  }

  public void setDependencyServiceGroupId(Long dependencyServiceGroupId) {
    this.dependencyServiceGroupId = dependencyServiceGroupId;
  }

  public String getDependencyServiceGroupName() {
    return dependencyServiceGroupName;
  }

  public void setDependencyServiceGroupName(String dependencyServiceGroupName) {
    this.dependencyServiceGroupName = dependencyServiceGroupName;
  }

  public Long getDependencyServiceId() {
    return dependencyServiceId;
  }

  public void setDependencyServiceId(Long dependencyServiceId) {
    this.dependencyServiceId = dependencyServiceId;
  }

  public String getDependencyServiceName() {
    return dependencyServiceName;
  }

  public void setDependencyServiceName(String dependencyServiceName) {
    this.dependencyServiceName = dependencyServiceName;
  }

  public Long getServiceGroupId() {
    return serviceGroupId;
  }

  public void setServiceGroupId(Long serviceGroupId) {
    this.serviceGroupId = serviceGroupId;
  }

  public String getServiceGroupName() {
    return serviceGroupName;
  }

  public void setServiceGroupName(String serviceGroupName) {
    this.serviceGroupName = serviceGroupName;
  }

  public Long getServiceId() {
    return serviceId;
  }

  public void setServiceId(Long serviceId) {
    this.serviceId = serviceId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public Long getDependencyId() {
    return dependencyId;
  }

  public void setDependencyId(Long dependencyId) {
    this.dependencyId = dependencyId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ServiceDependencyResponse)) return false;

    ServiceDependencyResponse that = (ServiceDependencyResponse) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (dependencyClusterId != null ? !dependencyClusterId.equals(that.dependencyClusterId) : that.dependencyClusterId != null)
      return false;
    if (dependencyClusterName != null ? !dependencyClusterName.equals(that.dependencyClusterName) : that.dependencyClusterName != null)
      return false;
    if (dependencyServiceGroupId != null ? !dependencyServiceGroupId.equals(that.dependencyServiceGroupId) : that.dependencyServiceGroupId != null)
      return false;
    if (dependencyServiceGroupName != null ? !dependencyServiceGroupName.equals(that.dependencyServiceGroupName) : that.dependencyServiceGroupName != null)
      return false;
    if (dependencyServiceId != null ? !dependencyServiceId.equals(that.dependencyServiceId) : that.dependencyServiceId != null)
      return false;
    if (dependencyServiceName != null ? !dependencyServiceName.equals(that.dependencyServiceName) : that.dependencyServiceName != null)
      return false;
    if (serviceGroupId != null ? !serviceGroupId.equals(that.serviceGroupId) : that.serviceGroupId != null)
      return false;
    if (serviceGroupName != null ? !serviceGroupName.equals(that.serviceGroupName) : that.serviceGroupName != null)
      return false;
    if (serviceId != null ? !serviceId.equals(that.serviceId) : that.serviceId != null) return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.hashCode() : 0;
    result = 31 * result + (serviceId != null ? serviceId.hashCode() : 0);
    result = 31 * result + (serviceGroupId != null ? serviceGroupId.hashCode() : 0);
    result = 31 * result + (dependencyClusterId != null ? dependencyClusterId.hashCode() : 0);
    result = 31 * result + (dependencyServiceId != null ? dependencyServiceId.hashCode() : 0);
    result = 31 * result + (dependencyServiceGroupId != null ? dependencyServiceGroupId.hashCode() : 0);
    result = 31 * result + (clusterName != null ? clusterName.hashCode() : 0);
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (serviceGroupName != null ? serviceGroupName.hashCode() : 0);
    result = 31 * result + (dependencyServiceName != null ? dependencyServiceName.hashCode() : 0);
    result = 31 * result + (dependencyClusterName != null ? dependencyClusterName.hashCode() : 0);
    result = 31 * result + (dependencyServiceGroupName != null ? dependencyServiceGroupName.hashCode() : 0);
    return result;
  }
}
