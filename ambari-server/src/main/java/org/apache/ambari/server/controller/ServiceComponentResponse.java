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


public class ServiceComponentResponse {

  private Long clusterId; // REF

  private String clusterName; // REF

  private String serviceName;

  private String componentName;

  private String displayName;

  private String desiredStackVersion;

  private String desiredState;

  private String category;

  private int totalCount;

  private int startedCount;

  private int installedCount;

  public ServiceComponentResponse(Long clusterId, String clusterName,
                                  String serviceName,
                                  String componentName,
                                  String desiredStackVersion,
                                  String desiredState,
                                  int totalCount,
                                  int startedCount,
                                  int installedCount, String displayName) {
    super();
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.componentName = componentName;
    this.displayName = displayName;
    this.desiredStackVersion = desiredStackVersion;
    this.desiredState = desiredState;
    this.totalCount = totalCount;
    this.startedCount = startedCount;
    this.installedCount = installedCount;
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
   * Get the number of started SCH's
   * @return number of started SCH's
   */
  public int getStartedCount() {
    return startedCount;
  }

  /**
   * Set the number of started SCH's
   * @param startedCount
   */
  public void setStartedCount(int startedCount) {
    this.startedCount = startedCount;
  }

  /**
   * Get the number of installed SCH's
   * @return number of installed SCH's
   */
  public int getInstalledCount() {
    return installedCount;
  }

  /**
   * Set the number of installed SCH's
   * @param installedCount
   */
  public void setInstalledCount(int installedCount) {
    this.installedCount = installedCount;
  }

  /**
   * Get the total number of SCH's
   * @return
   */
  public int getTotalCount() {
    return totalCount;
  }

  /**
   * Set the total number of SCH's
   * @param totalCount
   */
  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
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
