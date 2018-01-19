/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller;

import io.swagger.annotations.ApiModelProperty;

public class ServiceGroupDependencyResponse {

  private Long clusterId;
  private Long serviceGroupId;
  private Long dependencyClusterId;
  private Long dependencyGroupId;
  private Long dependencyId;
  private String clusterName;
  private String serviceGroupName;
  private String dependencyGroupName;
  private String dependencyClusterName;

  public ServiceGroupDependencyResponse(Long clusterId, String clusterName, Long serviceGroupId, String serviceGroupName,
                                        Long dependencyClusterId, String dependencyClusterName,
                                        Long dependencyGroupId, String dependencyGroupName, Long dependencyId) {
    this.clusterId = clusterId;
    this.serviceGroupId = serviceGroupId;
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.dependencyGroupId = dependencyGroupId;
    this.dependencyGroupName = dependencyGroupName;
    this.dependencyClusterId = dependencyClusterId;
    this.dependencyClusterName = dependencyClusterName;
    this.dependencyId = dependencyId;
  }

  /**
   * @return the clusterId
   */
  public Long getClusterId() {
    return clusterId;
  }

  /**
   * @param clusterId the clusterId to set
   */
  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * @return the clusterName
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * @param clusterName the clusterName to set
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * @return the service group Id
   */
  public Long getServiceGroupId() {
    return serviceGroupId;
  }

  /**
   * @param  serviceGroupId the service group Id
   */
  public void setServiceGroupId(Long serviceGroupId) {
    this.serviceGroupId = serviceGroupId;
  }

  /**
   * @return the service group name
   */
  public String getServiceGroupName() {
    return serviceGroupName;
  }

  /**
   * @param  serviceGroupName the service group name
   */
  public void setServiceGroupName(String serviceGroupName) {
    this.serviceGroupName = serviceGroupName;
  }

  public Long getDependencyGroupId() {
    return dependencyGroupId;
  }

  public void setDependencyGroupId(Long dependencyGroupId) {
    this.dependencyGroupId = dependencyGroupId;
  }

  public String getDependencyGroupName() {
    return dependencyGroupName;
  }

  public void setDependencyGroupName(String dependencyGroupName) {
    this.dependencyGroupName = dependencyGroupName;
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

  public Long getDependencyId() {
    return dependencyId;
  }

  public void setDependencyId(Long dependencyId) {
    this.dependencyId = dependencyId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ServiceGroupDependencyResponse)) return false;

    ServiceGroupDependencyResponse that = (ServiceGroupDependencyResponse) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (dependencyGroupId != null ? !dependencyGroupId.equals(that.dependencyGroupId) : that.dependencyGroupId != null)
      return false;
    if (serviceGroupId != null ? !serviceGroupId.equals(that.serviceGroupId) : that.serviceGroupId != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.hashCode() : 0;
    result = 31 * result + (serviceGroupId != null ? serviceGroupId.hashCode() : 0);
    result = 31 * result + (dependencyGroupId != null ? dependencyGroupId.hashCode() : 0);
    return result;
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface ServiceGroupDependencyResponseSwagger extends ApiModel {
    @ApiModelProperty(name = "ServiceGroupDependencyInfo")
    ServiceResponse getServiceGroupDependencyResponse();
  }

}
