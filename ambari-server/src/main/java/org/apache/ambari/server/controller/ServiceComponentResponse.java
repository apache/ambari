/**
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

public class ServiceComponentResponse {

  private Long clusterId; // REF

  private String clusterName; // REF

  private String serviceName;

  private String componentName;

  private String displayName;

  private String desiredStackVersion;

  private String desiredState;

  private String category;

  Map<String, Integer> serviceComponentStateCount;

  private boolean recoveryEnabled;

  public ServiceComponentResponse(Long clusterId, String clusterName,
                                  String serviceName,
                                  String componentName,
                                  String desiredStackVersion,
                                  String desiredState,
                                  Map<String, Integer> serviceComponentStateCount,
                                  boolean recoveryEnabled,
                                  String displayName) {
    super();
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.componentName = componentName;
    this.displayName = displayName;
    this.desiredStackVersion = desiredStackVersion;
    this.desiredState = desiredState;
    this.serviceComponentStateCount = serviceComponentStateCount;
    this.recoveryEnabled = recoveryEnabled;
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
   * @return the desiredStackVersion
   */
  public String getDesiredStackVersion() {
    return desiredStackVersion;
  }

  /**
   * @param desiredStackVersion the desiredStackVersion to set
   */
  public void setDesiredStackVersion(String desiredStackVersion) {
    this.desiredStackVersion = desiredStackVersion;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceComponentResponse that =
        (ServiceComponentResponse) o;

    if (clusterName != null ?
        !clusterName.equals(that.clusterName) : that.clusterName != null) {
      return false;
    }
    if (serviceName != null ?
        !serviceName.equals(that.serviceName) : that.serviceName != null) {
      return false;
    }
    if (componentName != null ?
        !componentName.equals(that.componentName) : that.componentName != null){
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null? clusterId.intValue() : 0;
    result = 71 * result + (clusterName != null ? clusterName.hashCode() : 0);
    result = 71 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 71 * result + (componentName != null ? componentName.hashCode():0);
    return result;
  }

}
