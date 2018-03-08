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


import java.util.Objects;

public class ServiceComponentRequest {

  private String clusterName; // REF
  private String serviceGroupName;
  private String serviceName; // GET/CREATE/UPDATE/DELETE
  private String componentName; // GET/CREATE/UPDATE/DELETE
  private String componentType;
  private String desiredState; // CREATE/UPDATE
  private String componentCategory;
  private String recoveryEnabled; // CREATE/UPDATE

  public ServiceComponentRequest(String clusterName, String serviceGroupName, String serviceName,
                                 String componentName, String componentType, String desiredState) {
    this(clusterName, serviceGroupName, serviceName, componentName, componentType, desiredState, null, null);
  }

  public ServiceComponentRequest(String clusterName, String serviceGroupName, String serviceName, String componentName,
                                 String componentType, String desiredState, String recoveryEnabled) {
    this(clusterName, serviceGroupName, serviceName, componentName, componentType, desiredState, recoveryEnabled, null);
  }

  public ServiceComponentRequest(String clusterName, String serviceGroupName,
                                 String serviceName, String componentName, String componentType,
                                 String desiredState, String recoveryEnabled,
                                 String componentCategory) {
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.serviceName = serviceName;
    this.componentName = componentName;
    this.componentType = componentType;
    this.desiredState = desiredState;
    this.recoveryEnabled = recoveryEnabled;
    this.componentCategory = componentCategory;
  }

  /**
   * @return the service group Name
   */
  public String getServiceGroupName() { return serviceGroupName; }

  /**
   * @param serviceGroupName the service group Name to set
   */
  public void setServiceGroupName(String serviceGroupName) { this.serviceGroupName = serviceGroupName; }

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
  public String getComponentName() { return componentName; }

  /**
   * @param componentName the componentName to set
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * @return the componentType
   */
  public String getComponentType() { return componentType; }

  /**
   * @param componentType the componentType to set
   */
  public void setComponentType(String componentType) {
    this.componentType = componentType;
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
   * @return recoveryEnabled
   */
  public String getRecoveryEnabled() {
    return recoveryEnabled;
  }

  /**
   * @param recoveryEnabled the recoveryEnabled value to set.
   */
  public void setRecoveryEnabled(String recoveryEnabled) {
    this.recoveryEnabled = recoveryEnabled;
  }

  public String getComponentCategory() {
    return componentCategory;
  }

  public void setComponentCategory(String componentCategory) {
    this.componentCategory = componentCategory;
  }

  @Override
  public String toString() {
    return String.format("[clusterName=%s, serviceGroupName=%s, serviceName=%s, componentName=%s, componentType=%s, " +
      "desiredState=%s, recoveryEnabled=%s, componentCategory=%s]", clusterName, serviceGroupName,
      serviceName, componentName, componentType, desiredState, recoveryEnabled, componentCategory);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ServiceComponentRequest other = (ServiceComponentRequest) obj;

    return Objects.equals(clusterName, other.clusterName) &&
      Objects.equals(serviceGroupName, other.serviceGroupName) &&
      Objects.equals(serviceName, other.serviceName) &&
      Objects.equals(componentCategory, other.componentCategory) &&
      Objects.equals(componentName, other.componentName) &&
      Objects.equals(desiredState, other.desiredState) &&
      Objects.equals(recoveryEnabled, other.recoveryEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterName, serviceGroupName, serviceName, componentCategory, componentName, desiredState, recoveryEnabled);
  }
}
