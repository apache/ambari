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

import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang.builder.EqualsBuilder;

import com.google.common.base.Objects;

import io.swagger.annotations.ApiModelProperty;

public class ServiceGroupResponse {

  private Long clusterId;
  private Long serviceGroupId;
  private String clusterName;
  private String serviceGroupName;
  private Long mpackId;
  private StackId stackId;

  public ServiceGroupResponse(Long clusterId, String clusterName, Long mpackId, StackId stackId,
      Long serviceGroupId, String serviceGroupName) {
    this.clusterId = clusterId;
    this.serviceGroupId = serviceGroupId;
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.mpackId = mpackId;
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
   * @return the servicegroup stackId (stackName-stackVersion)
   */
  public StackId getStackId() {
    return stackId;
  }

  /**
   * @param version
   *          the servicegroup stackId (stackName-stackVersion)
   */
  public void setStackId(StackId stackId) {
    this.stackId = stackId;
  }

  /**
   * Gets the MpackID for this service group.
   *
   * @return the mpackId
   */
  public Long getMpackId() {
    return mpackId;
  }

  /**
   * Sets the Mpack ID for this service group.
   *
   * @param mpackId
   *          the mpackId to set
   */
  public void setMpackId(Long mpackId) {
    this.mpackId = mpackId;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    ServiceGroupResponse that = (ServiceGroupResponse) object;
    EqualsBuilder equalsBuilder = new EqualsBuilder();

    equalsBuilder.append(clusterId, that.clusterId);
    equalsBuilder.append(clusterName, that.clusterName);
    equalsBuilder.append(mpackId, that.mpackId);
    equalsBuilder.append(stackId, that.stackId);
    equalsBuilder.append(serviceGroupId, that.serviceGroupId);
    equalsBuilder.append(serviceGroupName, that.serviceGroupName);
    return equalsBuilder.isEquals();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(clusterId, clusterName, mpackId, stackId, serviceGroupId,
        serviceGroupName);
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface ServiceGroupResponseSwagger extends ApiModel {
    @ApiModelProperty(name = "ServiceGroupInfo")
    ServiceGroupResponse getServiceGroupResponse();
  }

}