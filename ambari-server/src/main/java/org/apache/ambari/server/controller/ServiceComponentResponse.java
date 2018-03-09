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


import java.util.Map;

import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;

public class ServiceComponentResponse {

  private Long clusterId; // REF
  private String clusterName; // REF
  private Long serviceGroupId; // REF
  private String serviceGroupName; // REF
  private Long serviceId; // REF
  private String serviceName;
  private String serviceType;
  private Long componentId;
  private String componentName;
  private String componentType;
  private String displayName;
  private String desiredStackId;
  private String desiredState;
  private String category;
  private Map<String, Integer> serviceComponentStateCount;
  private boolean recoveryEnabled;
  private String desiredVersion;
  private RepositoryVersionState repoState;

  public ServiceComponentResponse(Long clusterId, String clusterName, Long serviceGroupId, String serviceGroupName,
                                  Long serviceId, String serviceName, String serviceType, Long componentId, String componentName,
                                  String componentType, StackId desiredStackId, String desiredState,
                                  Map<String, Integer> serviceComponentStateCount, boolean recoveryEnabled,
                                  String displayName, String desiredVersion, RepositoryVersionState repoState) {
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.serviceGroupId = serviceGroupId;
    this.serviceGroupName = serviceGroupName;
    this.serviceId = serviceId;
    this.serviceName = serviceName;
    this.serviceType = serviceType;
    this.componentId = componentId;
    this.componentName = componentName;
    this.componentType = componentType;
    this.displayName = displayName;
    this.desiredStackId = desiredStackId.getStackId();
    this.desiredState = desiredState;
    this.serviceComponentStateCount = serviceComponentStateCount;
    this.recoveryEnabled = recoveryEnabled;
    this.desiredVersion = desiredVersion;
    this.repoState = repoState;
  }

  /**
   * @return the serviceName
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @param serviceName the serviceName to set
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * @return the serviceName
   */
  public String getServiceType() { return serviceType; }

  /**
   * @param serviceType the serviceType to set
   */
  public void setServiceType(String serviceType) { this.serviceType = serviceType; }

  /**
   * @return the serviceId
   */
  public Long getServiceId() { return serviceId; }

  /**
   * @param serviceId the serviceId to set
   */
  public void setServiceId(Long serviceId) { this.serviceId = serviceId; }

  /**
   * @return the serviceGroupId
   */
  public Long getServiceGroupId() { return serviceGroupId; }

  /**
   * @param serviceGroupId the serviceGroupId to set
   */
  public void setServiceGroupId(Long serviceGroupId) { this.serviceGroupId = serviceGroupId; }

  /**
   * @return the serviceGroupName
   */
  public String getServiceGroupName() { return serviceGroupName; }

  /**
   * @param serviceGroupName the serviceGroupName to set
   */
  public void setServiceGroupName(String serviceGroupName) { this.serviceGroupName = serviceGroupName; }

  /**
   * @return the componentName
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   * @param componentName the componentName to set
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * @param componentId the componentId to set
   */
  public void setComponentName(Long componentId) {
    this.componentId = componentId;
  }

  /**
   * @return the componentType
   */
  public String getComponentType() {
    return componentType;
  }

  /**
   * @param componentType the componentType to set
   */
  public void setComponentType(String componentType) {
    this.componentType = componentType;
  }

  /**
   * @return the componentId
   */
  public Long getComponentId() {
    return componentId;
  }

  /**
   * @return the displayName
   */
  public String getDisplayName() {
    return displayName;
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
   * @return the desiredState
   */
  public String getDesiredState() {
    return desiredState;
  }

  /**
   * @param desiredState the desiredState to set
   */
  public void setDesiredState(String desiredState) {
    this.desiredState = desiredState;
  }

  /**
   * Gets the desired stack ID.
   *
   * @return the desiredStackVersion
   */
  public String getDesiredStackId() {
    return desiredStackId;
  }

  /**
   * Get the component category.
   *
   * @return the category
   */
  public String getCategory() {
    return category;
  }

  /**
   * Set the component category.
   *
   * @param category  the category
   */
  public void setCategory(String category) {
    this.category = category;
  }

  /**
   * Get the count of service component for each state
   * @return number of service component for each state
   */
  public Map<String, Integer> getServiceComponentStateCount() {
    return serviceComponentStateCount;
  }

  /**
   * Get a true or false value indicating if the service component is auto start enabled
   * @return true or false
   */
  public boolean isRecoveryEnabled() {
    return recoveryEnabled;
  }

  /**
   * Set a true or false value indicating whether the service component is auto start enabled
   * @param recoveryEnabled
   */
  public void setRecoveryEnabled(boolean recoveryEnabled) {
    this.recoveryEnabled = recoveryEnabled;
  }

  /**
   * @return the desired version of the component
   */
  public String getDesiredVersion() {
    return desiredVersion;
  }

  /**
   * @return the state of the repository against the desired version
   */
  public RepositoryVersionState getRepositoryState() {
    return repoState;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceComponentResponse that =
        (ServiceComponentResponse) o;

    if (clusterId != null ?
            !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }

    if (clusterName != null ?
        !clusterName.equals(that.clusterName) : that.clusterName != null) {
      return false;
    }

    if (serviceGroupId != null ?
            !serviceGroupId.equals(that.serviceGroupId) : that.serviceGroupId != null) {
      return false;
    }
    if (serviceGroupName != null ?
            !serviceGroupName.equals(that.serviceGroupName) : that.serviceGroupName != null) {
      return false;
    }
    if (serviceId != null ?
            !serviceId.equals(that.serviceId) : that.serviceId != null) {
      return false;
    }

    if (serviceName != null ?
        !serviceName.equals(that.serviceName) : that.serviceName != null) {
      return false;
    }

    if (serviceType != null ?
            !serviceType.equals(that.serviceType) : that.serviceType != null) {
      return false;
    }

    if (componentId != null ?
            !componentId.equals(that.componentId) : that.componentId != null) {
      return false;
    }

    if (componentName != null ?
        !componentName.equals(that.componentName) : that.componentName != null){
      return false;
    }

    if (componentType != null ?
            !componentType.equals(that.componentType) : that.componentType != null){
      return false;
    }

    if (displayName != null ?
            !displayName.equals(that.displayName) : that.displayName != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null? clusterId.intValue() : 0;
    result = 71 * result + (clusterName != null ? clusterName.hashCode() : 0);
    result = 71 * result + (serviceGroupId != null ? serviceGroupId.hashCode() : 0);
    result = 71 * result + (serviceGroupName != null ? serviceGroupName.hashCode() : 0);
    result = 71 * result + (serviceId != null ? serviceId.hashCode() : 0);
    result = 71 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 71 * result + (serviceType != null ? serviceType.hashCode() : 0);
    result = 71 * result + (componentId != null ? componentId.hashCode() : 0);
    result = 71 * result + (componentName != null ? componentName.hashCode():0);
    result = 71 * result + (componentType != null ? componentType.hashCode():0);
    result = 71 * result + (displayName != null ? displayName.hashCode():0);
    return result;
  }

}
