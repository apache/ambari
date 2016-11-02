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


public class ServiceResponse {

  private Long clusterId;
  private String clusterName;
  private String serviceName;
  private String desiredStackVersion;
  private String desiredState;
  private String maintenanceState;
  private boolean credentialStoreSupported;
  private boolean credentialStoreEnabled;

  public ServiceResponse(Long clusterId, String clusterName,
                         String serviceName,
                         String desiredStackVersion, String desiredState,
                         boolean credentialStoreSupported, boolean credentialStoreEnabled) {
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.setDesiredStackVersion(desiredStackVersion);
    this.setDesiredState(desiredState);
    this.credentialStoreSupported = credentialStoreSupported;
    this.credentialStoreEnabled = credentialStoreEnabled;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceResponse that = (ServiceResponse) o;

    if (clusterId != null ?
        !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }
    if (clusterName != null ?
        !clusterName.equals(that.clusterName) : that.clusterName != null) {
      return false;
    }
    if (serviceName != null ?
        !serviceName.equals(that.serviceName) : that.serviceName != null) {
      return false;
    }

    return true;
  }
  
  public void setMaintenanceState(String state) {
    maintenanceState = state;
  }
  
  public String getMaintenanceState() {
    return maintenanceState;
  }

  /**
   * Get a true or false value indicating if the service supports
   * credential store use or not.
   *
   * @return true or false
   */
  public boolean isCredentialStoreSupported() {
    return credentialStoreSupported;
  }

  /**
   * Set a true or false value indicating whether the service
   * supports credential store or not.
   *
   * @param credentialStoreSupported
   */
  public void setCredentialStoreSupported(boolean credentialStoreSupported) {
    this.credentialStoreSupported = credentialStoreSupported;
  }

  /**
   * Get a true or false value indicating if the service is enabled
   * for credential store use or not.
   *
   * @return true or false
   */
  public boolean isCredentialStoreEnabled() {
    return credentialStoreEnabled;
  }

  /**
   * Set a true or false value indicating whether the service is
   * enabled for credential store use or not.
   *
   * @param credentialStoreEnabled
   */
  public void setCredentialStoreEnabled(boolean credentialStoreEnabled) {
    this.credentialStoreEnabled = credentialStoreEnabled;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null? clusterId.intValue() : 0;
    result = 71 * result + (clusterName != null ? clusterName.hashCode() : 0);
    result = 71 * result + (serviceName != null ? serviceName.hashCode() : 0);
    return result;
  }

}
