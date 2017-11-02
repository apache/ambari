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
  private String clusterName;
  private String serviceGroupName;
  private String dependencyGroupName;
  private String dependencyClusterName;

  public ServiceGroupDependencyResponse(Long clusterId, String clusterName, Long serviceGroupId, String serviceGroupName,
                                        Long dependencyClusterId, String dependencyClusterName,
                                        Long dependencyGroupId, String dependencyGroupName) {
    this.clusterId = clusterId;
    this.serviceGroupId = serviceGroupId;
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.dependencyGroupId = dependencyGroupId;
    this.dependencyGroupName = dependencyGroupName;
    this.dependencyClusterId = dependencyClusterId;
    this.dependencyClusterName = dependencyClusterName;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceGroupResponse that = (ServiceGroupResponse) o;

    if (clusterId != null ?
            !clusterId.equals(this.clusterId) : this.clusterId != null) {
      return false;
    }
    if (clusterName != null ?
            !clusterName.equals(this.clusterName) : this.clusterName != null) {
      return false;
    }
    if (serviceGroupName != null ?
            !serviceGroupName.equals(this.serviceGroupName) : this.serviceGroupName != null) {
      return false;
    }

    return true;
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface ServiceGroupDependencyResponseSwagger extends ApiModel {
    @ApiModelProperty(name = "ServiceGroupDependencyInfo")
    ServiceResponse getServiceGroupDependencyResponse();
  }

}
