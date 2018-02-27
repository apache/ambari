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

import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.UpgradeState;

public class ServiceComponentHostResponse {

  private Long clusterId; // REF
  private String clusterName; // REF
  private Long serviceGroupId;
  private String serviceGroupName;
  private Long serviceId;
  private String serviceName;
  private String serviceType;
  private Long hostComponentId;
  private String componentName;
  private String componentType;
  private String displayName;
  private String publicHostname;
  private String hostname;
  // type -> desired config
  private Map<String, HostConfig> actualConfigs;
  private String liveState;
  private String version;
  private String desiredStackVersion;
  private String desiredRepositoryVersion;
  private String desiredState;
  private boolean staleConfig = false;
  private boolean reloadConfig = false;
  private String adminState = null;
  private String maintenanceState = null;
  private UpgradeState upgradeState = UpgradeState.NONE;

  public ServiceComponentHostResponse(Long clusterId, String clusterName, Long serviceGroupId, String serviceGroupName,
                                      Long serviceId, String serviceName, String serviceType, Long hostComponentId,
                                      String componentName, String componentType, String displayName, String hostname,
                                      String publicHostname, String liveState, String version, String desiredState,
                                      String desiredStackVersion, String desiredRepositoryVersion,
                                      HostComponentAdminState adminState) {
    this.clusterId = clusterId;
    this.serviceGroupId = serviceGroupId;
    this.serviceGroupName = serviceGroupName;
    this.serviceId = serviceId;
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.serviceType = serviceType;
    this.hostComponentId = hostComponentId;
    this.componentName = componentName;
    this.componentType = componentType;
    this.displayName = displayName;
    this.hostname = hostname;
    this.publicHostname = publicHostname;
    this.liveState = liveState;
    this.version = version;
    this.desiredState = desiredState;
    this.desiredStackVersion = desiredStackVersion;
    this.desiredRepositoryVersion = desiredRepositoryVersion;
    if (adminState != null) {
      this.adminState = adminState.name();
    }
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
   * @return the serviceGroupName
   */
  public String getServiceGroupName() { return serviceGroupName; }

  /**
   * @param serviceGroupName the serviceGroupName to set
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
   * @return the hostComponentId
   */
  public Long getHostComponentId() { return hostComponentId; }

  /**
   * @param hostComponentId the hostComponentId to set
   */
  public void sethostComponentId(Long hostComponentId) { this.hostComponentId = hostComponentId; }

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
   * @return the componentName
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   * @return the componentType
   */
  public String getComponentType() {
    return componentType;
  }

  /**
   * @param componentName the componentName to set
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * @param componentType the componentType to set
   */
  public void setComponentType(String componentType) {
    this.componentType = componentType;
  }

  /**
   * @return the displayName
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * @return the hostname
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * @param hostname the hostname to set
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /**
   * @return the public hostname
   */
  public String getPublicHostname() {
    return publicHostname;
  }

  /**
   * @param publicHostname the public hostname to set
   */
  public void setPublicHostname(String publicHostname) {
    this.publicHostname = publicHostname;
  }

  /**
   * @return the liveState
   */
  public String getLiveState() {
    return liveState;
  }

  /**
   * @param liveState the liveState to set
   */
  public void setLiveState(String liveState) {
    this.liveState = liveState;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
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
   * Gets the desired repository of the component.
   *
   * @return the desired repository.
   */
  public String getDesiredRepositoryVersion() {
    return desiredRepositoryVersion;
  }

  /**
   * @return the clusterId
   */
  public Long getClusterId() { return clusterId; }

  /**
   * @param clusterId the clusterId to set
   */
  public void setClusterId(Long clusterId) { this.clusterId = clusterId; }

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
   * @return the admin state of the host component
   */
  public String getAdminState() {
    return adminState;
  }

  /**
   * @param adminState of the host component
   */
  public void setAdminState(String adminState) {
    this.adminState = adminState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceComponentHostResponse that =
        (ServiceComponentHostResponse) o;

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

    if (componentName != null ?
        !componentName.equals(that.componentName) : that.componentName != null) {
      return false;
    }

    if (componentType != null ?
            !componentType.equals(that.componentType) : that.componentType != null) {
      return false;
    }

    if (displayName != null ?
            !displayName.equals(that.displayName) : that.displayName != null) {
      return false;
    }

    if (hostname != null ?
        !hostname.equals(that.hostname) : that.hostname != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null? clusterId.intValue() : 0;
    result = clusterName != null ? clusterName.hashCode() : 0;
    result = 71 * result + (serviceGroupId != null ? serviceGroupId.hashCode() : 0);
    result = 71 * result + (serviceGroupName != null ? serviceGroupName.hashCode() : 0);
    result = 71 * result + (serviceId != null ? serviceId.hashCode() : 0);
    result = 71 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 71 * result + (serviceType != null ? serviceType.hashCode() : 0);
    result = 71 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 71 * result + (componentType != null ? componentType.hashCode() : 0);
    result = 71 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 71 * result + (hostname != null ? hostname.hashCode() : 0);
    return result;
  }

  /**
   * @return the actual configs
   */
  public Map<String, HostConfig> getActualConfigs() {
    return actualConfigs;
  }

  /**
   * @param configs the actual configs
   */
  public void setActualConfigs(Map<String, HostConfig> configs) {
    actualConfigs = configs;
  }

  /**
   * @return if the configs are stale
   */
  public boolean isStaleConfig() {
    return staleConfig;
  }

  /**
   * @param stale
   */
  public void setStaleConfig(boolean stale) {
    staleConfig = stale;
  }

  /**
   * @return true if configs are reloadable without RESTART command
   */
  public boolean isReloadConfig() {
    return reloadConfig;
  }

  /**
   * @param reloadConfig
   */
  public void setReloadConfig(boolean reloadConfig) {
    this.reloadConfig = reloadConfig;
  }

  /**
   * @return the maintenance state
   */
  public String getMaintenanceState() {
    return maintenanceState;
  }

  /**
   * @param state the maintenance state
   */
  public void setMaintenanceState(String state) {
    maintenanceState = state;
  }

  /**
   * @param state  the upgrade state
   */
  public void setUpgradeState(UpgradeState state) {
    upgradeState = state;
  }

  /**
   * @return the upgrade state
   */
  public UpgradeState getUpgradeState() {
    return upgradeState;
  }

}
