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

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModelProperty;

public class ServiceGroupResponse {

  private Long clusterId;
  private Long serviceGroupId;
  private String clusterName;
  private String serviceGroupName;
  private String stackId;

  public ServiceGroupResponse(Long clusterId, String clusterName, Long serviceGroupId, String serviceGroupName, String stackId) {
    this.clusterId = clusterId;
    this.serviceGroupId = serviceGroupId;
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.stackId = stackId;

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
   * @return the stack ID (stack name - stack Id)
   */
  public String getStackId() {
    return stackId;
  }

  /**
   * @param stackId the stack Id
   */
  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceGroupResponse that = (ServiceGroupResponse) o;

    if (clusterId != null ?
      !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }
    if (clusterName != null ?
      !clusterName.equals(that.clusterName) : that.clusterName != null) {
      return false;
    }
    if (serviceGroupName != null ?
      !serviceGroupName.equals(that.serviceGroupName) : that.serviceGroupName != null) {
      return false;
    }

    if (stackId != null ?
        !stackId.equals(that.stackId) : that.stackId != null) {
      return false;
    }

    return true;
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface ServiceGroupResponseSwagger extends ApiModel {
    @ApiModelProperty(name = "ServiceGroupInfo")
    ServiceGroupResponse getServiceGroupResponse();
  }

}