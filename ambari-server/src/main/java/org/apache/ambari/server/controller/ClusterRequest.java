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

import java.util.List;
import java.util.Set;

/**
 * Used for create Cluster
 */
public class ClusterRequest {

  private Long clusterId; // for GET

  private String clusterName; // for GET/CREATE/UPDATE

  private String stackVersion; // for CREATE/UPDATE

  private String provisioningState; // for GET/CREATE/UPDATE
  
  Set<String> hostNames; // CREATE/UPDATE
  
  private List<ConfigurationRequest> configs = null;

  private ServiceConfigVersionRequest serviceConfigVersionRequest = null;

  public ClusterRequest(Long clusterId, String clusterName, 
      String stackVersion, Set<String> hostNames) {
    this(clusterId, clusterName, null, stackVersion, hostNames);
  }  
  
  public ClusterRequest(Long clusterId, String clusterName, 
      String provisioningState, String stackVersion, Set<String> hostNames) {
    super();
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.provisioningState = provisioningState;
    this.stackVersion = stackVersion;
    this.hostNames = hostNames;    
  }
  
  /**
   * @return the clusterId
   */
  public Long getClusterId() {
    return clusterId;
  }

  /**
   * @return the clusterName
   */
  public String getClusterName() {
    return clusterName;
  }
  
  /**
   * Gets whether the cluster is still initializing or has finished with its
   * deployment requests.
   * 
   * @return either {@code INIT} or {@code INSTALLED} or {@code null} if not set
   *         on the request.
   */
  public String getProvisioningState(){
    return provisioningState;
  }
  
  /**
   * Sets whether the cluster is still initializing or has finished with its
   * deployment requests.
   * 
   * @param provisioningState
   *          either {@code INIT} or {@code INSTALLED}, or {@code null} if not
   *          set on the request.
   */
  public void setProvisioningState(String provisioningState) {
    this.provisioningState = provisioningState;
  }

  /**
   * @return the stackVersion
   */
  public String getStackVersion() {
    return stackVersion;
  }

  /**
   * @param clusterId the clusterId to set
   */
  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * @param clusterName the clusterName to set
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * @param stackVersion the stackVersion to set
   */
  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  public Set<String> getHostNames() {
    return hostNames;
  }

  public void setHostNames(Set<String> hostNames) {
    this.hostNames = hostNames;
  }
  
  /**
   * Sets the configs requests (if any)
   * @param configRequest
   */
  public void setDesiredConfig(List<ConfigurationRequest> configRequest) {
    configs = configRequest;
  }
  
  /**
   * Gets any configuration-based request (if any).
   * @return the list of configuration requests,
   * or <code>null</code> if none is set.
   */
  public List<ConfigurationRequest> getDesiredConfig() {
    return configs;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{" + " clusterName=").append(clusterName)
        .append(", clusterId=").append(clusterId)
        .append(", provisioningState=").append(provisioningState)
        .append(", stackVersion=").append(stackVersion)
        .append(", desired_scv=").append(serviceConfigVersionRequest)
        .append(", hosts=[");
    if (hostNames != null) {
      int i = 0;
      for (String hostName : hostNames) {
        if (i != 0) {
          sb.append(",");
        }
        ++i;
        sb.append(hostName);
      }
    }
    sb.append("] }");
    return sb.toString();
  }


  public ServiceConfigVersionRequest getServiceConfigVersionRequest() {
    return serviceConfigVersionRequest;
  }

  public void setServiceConfigVersionRequest(ServiceConfigVersionRequest serviceConfigVersionRequest) {
    this.serviceConfigVersionRequest = serviceConfigVersionRequest;
  }
}
