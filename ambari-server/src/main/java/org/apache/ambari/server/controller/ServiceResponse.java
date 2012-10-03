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
import java.util.Map;

public class ServiceResponse {

  private Long clusterId; // REF

  private String clusterName; // REF

  private List<PerServiceResponse> services;
  
  public ServiceResponse(Long clusterId, String clusterName,
      List<PerServiceResponse> services) {
    super();
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.services = services;
  }

  public static class PerServiceResponse {
    
    private String serviceName;
    
    private String currentStackVersion;
    
    // Config type -> version mapping
    private Map<String, String> configVersions; // CREATE/UPDATE

    public PerServiceResponse(String serviceName, String currentStackVersion,
        Map<String, String> configVersions) {
      super();
      this.serviceName = serviceName;
      this.currentStackVersion = currentStackVersion;
      this.configVersions = configVersions;
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
     * @return the currentStackVersion
     */
    public String getCurrentStackVersion() {
      return currentStackVersion;
    }

    /**
     * @param currentStackVersion the currentStackVersion to set
     */
    public void setCurrentStackVersion(String currentStackVersion) {
      this.currentStackVersion = currentStackVersion;
    }

    /**
     * @return the configVersions
     */
    public Map<String, String> getConfigVersions() {
      return configVersions;
    }

    /**
     * @param configVersions the configVersions to set
     */
    public void setConfigVersions(Map<String, String> configVersions) {
      this.configVersions = configVersions;
    }
    
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
   * @return the services
   */
  public List<PerServiceResponse> getServices() {
    return services;
  }

  /**
   * @param services the services to set
   */
  public void setServices(List<PerServiceResponse> services) {
    this.services = services;
  }
}
