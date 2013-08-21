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

import java.util.Set;

/**
 * Used for create Cluster
 */
public class ClusterRequest {

  private Long clusterId; // for GET

  private String clusterName; // for GET/CREATE/UPDATE

  private String stackVersion; // for CREATE/UPDATE

  Set<String> hostNames; // CREATE/UPDATE
  
  private ConfigurationRequest config = null;

  public ClusterRequest(Long clusterId, String clusterName,
      String stackVersion, Set<String> hostNames) {
    super();
    this.clusterId = clusterId;
    this.clusterName = clusterName;
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
   * Sets the config request (if any)
   * @param configRequest
   */
  public void setDesiredConfig(ConfigurationRequest configRequest) {
    config = configRequest;
  }
  
  /**
   * Gets any configuration-based request (if any).
   * @return the configuration request, or <code>null</code> if none is set.
   */
  public ConfigurationRequest getDesiredConfig() {
    return config;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{"
        + " clusterName=" + clusterName
        + ", clusterId=" + clusterId
        + ", stackVersion=" + stackVersion
        + ", hosts=[");
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
  

}
