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

import org.apache.ambari.server.state.StackId;

import io.swagger.annotations.ApiModelProperty;

public class ServiceResponse {

  private Long clusterId;
  private String clusterName;
  private Long serviceGroupId;
  private String serviceGroupName;
  private Long serviceId;
  private String serviceName;
  private String serviceType;
  private StackId desiredStackId;
  private String desiredState;
  private String maintenanceState;
  private final boolean hidden;
  private boolean credentialStoreSupported;
  private boolean credentialStoreEnabled;
  private final boolean ssoIntegrationSupported;
  private final boolean ssoIntegrationDesired;
  private final boolean ssoIntegrationEnabled;

  public ServiceResponse(Long clusterId, String clusterName, Long serviceGroupId,
      String serviceGroupName, Long serviceId, String serviceName, String serviceType,
      StackId desiredStackId, String desiredRepositoryVersion, String desiredState,
      boolean credentialStoreSupported, boolean credentialStoreEnabled,
      boolean ssoIntegrationSupported, boolean ssoIntegrationDesired,
      boolean ssoIntegrationEnabled, boolean hidden) {
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.serviceGroupId = serviceGroupId;
    this.serviceGroupName = serviceGroupName;
    this.serviceId = serviceId;
    this.serviceName = serviceName;
    this.serviceType = serviceType;
    this.desiredStackId = desiredStackId;
    this.ssoIntegrationSupported = ssoIntegrationSupported;
    this.ssoIntegrationDesired = ssoIntegrationDesired;
    this.ssoIntegrationEnabled = ssoIntegrationEnabled;
    setDesiredState(desiredState);
    this.credentialStoreSupported = credentialStoreSupported;
    this.credentialStoreEnabled = credentialStoreEnabled;
    this.hidden = hidden;
  }

  /**
   * @return the serviceName
   */
  @ApiModelProperty(name = "service_name")
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
  @ApiModelProperty(hidden = true)
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
  @ApiModelProperty(name = "cluster_name")
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
   * @return the serviceGroupId
   */
  public Long getServiceGroupId() { return serviceGroupId; }

  /**
   * @param serviceGroupId the serviceGroupId to set
   */
  public void setServiceGroupId(Long serviceGroupId) { this.serviceGroupId = serviceGroupId; }

  /**
   * @return the service group name
   */
  public String getServiceGroupName() { return serviceGroupName; }

  /**
   * @param  serviceGroupName the service group name
   */
  public void setServiceGroupName(String serviceGroupName) { this.serviceGroupName = serviceGroupName; }

  /**
   * @return the serviceId
   */
  public Long getServiceId() { return serviceId; }

  /**
   * @param serviceId the serviceId to set
   */
  public void setServiceId(Long serviceId) { this.serviceId = serviceId; }

  /**
   * @return the serviceType (i.e. stack service name)
   */
  public String getServiceType() { return serviceType; }

  /**
   * @param serviceType the serviceType (i.e. stack service name) to set
   */
  public void setServiceType(String serviceType) { this.serviceType = serviceType; }

  /**
   * @return the desiredState
   */
  @ApiModelProperty(name = "state")
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
   * @return the desired stack ID.
   */
  @ApiModelProperty(hidden = true)
  public String getDesiredStackId() {
    return desiredStackId.getStackId();

  }

  @ApiModelProperty(name = "hidden")
  public boolean isHidden() {
    return hidden;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceResponse that = (ServiceResponse) o;

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

    return true;
  }

  public void setMaintenanceState(String state) {
    maintenanceState = state;
  }

  @ApiModelProperty(name = "maintenance_state")
  public String getMaintenanceState() {
    return maintenanceState;
  }

  /**
   * Get a true or false value indicating if the service supports
   * credential store use or not.
   *
   * @return true or false
   */
  @ApiModelProperty(name = "credential_store_supported")
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
  @ApiModelProperty(name = "credential_store_enabled")
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
    result = 71 * result + (serviceGroupId != null ? serviceGroupId.hashCode() : 0);
    result = 71 * result + (serviceGroupName != null ? serviceGroupName.hashCode() : 0);
    result = 71 * result + (serviceId != null ? serviceId.hashCode() : 0);
    result = 71 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 71 * result + (serviceType != null ? serviceType.hashCode() : 0);
    return result;
  }

  /**
   * Indicates if this service supports single sign-on integration.
   */
  @ApiModelProperty(name = "sso_integration_supported")
  public boolean isSsoIntegrationSupported() {
    return ssoIntegrationSupported;
  }

  /**
   * Indicates whether the service is chosen for SSO integration or not
   */
  @ApiModelProperty(name = "sso_integration_desired")
  public boolean isSsoIntegrationDesired() {
    return ssoIntegrationDesired;
  }

  /**
   * Indicates whether the service is configured for SSO integration or not
   */
  @ApiModelProperty(name = "sso_integration_enabled")
  public boolean isSsoIntegrationEnabled() {
    return ssoIntegrationEnabled;
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface ServiceResponseSwagger extends ApiModel {
    @ApiModelProperty(name = "ServiceInfo")
    ServiceResponse getServiceResponse();
  }
}
