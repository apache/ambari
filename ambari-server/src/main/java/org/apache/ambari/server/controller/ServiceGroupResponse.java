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

import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

public class ServiceGroupResponse {

  private Long clusterId;
  private Long serviceGroupId;
  private String clusterName;
  private String serviceGroupName;
  private String version;

  public ServiceGroupResponse(Long clusterId, String clusterName, Long serviceGroupId, String serviceGroupName, String version) {
    this.clusterId = clusterId;
    this.serviceGroupId = serviceGroupId;
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.version = version;

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

  /**
   * @return the servicegroup version (stackName-stackVersion)
   */
  public String getVersion() {
    return version;
  }

  /**
   * @param version the servicegroup version (stackName-stackVersion)
   */
  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceGroupResponse other = (ServiceGroupResponse) o;

    return Objects.equals(clusterId, other.clusterId) && Objects.equals(clusterName, other.clusterName) && Objects.equals(serviceGroupName, other.serviceGroupName) && Objects.equals(version, other.version);
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface ServiceGroupResponseSwagger extends ApiModel {
    @ApiModelProperty(name = "ServiceGroupInfo")
    ServiceGroupResponse getServiceGroupResponse();
  }

}