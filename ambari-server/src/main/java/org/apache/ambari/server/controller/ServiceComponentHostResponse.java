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

import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.HostConfig;

import java.util.Map;

public class ServiceComponentHostResponse {

  private String clusterName; // REF

  private String serviceName;

  private String componentName;

  private String hostname;

  // type -> desired config
  private Map<String, HostConfig> actualConfigs;

  private String liveState;

  private String stackVersion;

  private String desiredStackVersion;

  private String desiredState;
  
  private boolean staleConfig = false;


  public ServiceComponentHostResponse(String clusterName, String serviceName,
                                      String componentName, String hostname,
                                      String liveState,
                                      String stackVersion,
                                      String desiredState, String desiredStackVersion) {
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.componentName = componentName;
    this.hostname = hostname;
    this.liveState = liveState;
    this.stackVersion = stackVersion;
    this.desiredState = desiredState;
    this.desiredStackVersion = desiredStackVersion;
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
   * @return the stackVersion
   */
  public String getStackVersion() {
    return stackVersion;
  }

  /**
   * @param stackVersion the stackVersion to set
   */
  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceComponentHostResponse that =
        (ServiceComponentHostResponse) o;

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
    if (hostname != null ?
        !hostname.equals(that.hostname) : that.hostname != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 71 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 71 * result + (componentName != null ? componentName.hashCode():0);
    result = 71 * result + (hostname != null ? hostname.hashCode() : 0);
    return result;
  }

  /**
   * @param configs the actual configs
   */
  public void setActualConfigs(Map<String, HostConfig> configs) {
    actualConfigs = configs;
  }
  
  /**
   * @return the actual configs
   */
  public Map<String, HostConfig> getActualConfigs() {
    return actualConfigs;
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

}
