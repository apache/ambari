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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.ClusterHealthReport;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.State;

public class ClusterResponse {

  private final Long clusterId;

  private final String clusterName;

  private final Set<String> hostNames;

  private final String desiredStackVersion;

  private Map<String, DesiredConfig> desiredConfigs;

  private Map<String, Collection<ServiceConfigVersionResponse>> desiredServiceConfigVersions;

  private String provisioningState;

  /**
   * The cluster's security.
   * <p/>
   * See {@link org.apache.ambari.server.state.SecurityType} for relevant values.
   */
  private String securityType;

  private Integer totalHosts;

  private ClusterHealthReport clusterHealthReport;

  private Map<String, String> credentialStoreServiceProperties = null;

  public ClusterResponse(Long clusterId, String clusterName,
                         State provisioningState, SecurityType securityType, Set<String> hostNames, Integer totalHosts,
                         String desiredStackVersion, ClusterHealthReport clusterHealthReport) {

    super();
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.hostNames = hostNames;
    this.totalHosts = totalHosts;
    this.desiredStackVersion = desiredStackVersion;
    this.clusterHealthReport = clusterHealthReport;

    if (null != provisioningState) {
      this.provisioningState = provisioningState.name();
    }

    if (null == securityType) {
      this.securityType = SecurityType.NONE.name();
    } else {
      this.securityType = securityType.name();
    }
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
   * @return the host names
   */
  public Set<String> getHostNames() {
    return hostNames;
  }

  /**
   * Gets whether the cluster is still initializing or has finished with its
   * deployment requests.
   *
   * @return either {@code INIT} or {@code INSTALLED}, never {@code null}.
   */
  public String getProvisioningState() {
    return provisioningState;
  }

  /**
   * Gets the cluster's security type.
   * <p/>
   * See {@link org.apache.ambari.server.state.SecurityType} for relevant values.
   *
   * @return the cluster's security type
   */
  public String getSecurityType() {
    return securityType;
  }

  /**
   * Sets the cluster's security type.
   * <p/>
   * See {@link org.apache.ambari.server.state.SecurityType} for relevant values.
   *
   * @param securityType a String declaring the cluster's security type
   */
  public void setSecurityType(String securityType) {
    this.securityType = securityType;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{"
        + " clusterName=" + clusterName
        + ", clusterId=" + clusterId
        + ", provisioningState=" + provisioningState
        + ", desiredStackVersion=" + desiredStackVersion
        + ", totalHosts=" + totalHosts
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
    sb.append("]"
        + ", clusterHealthReport= " + clusterHealthReport
        + "}");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClusterResponse that = (ClusterResponse) o;

    if (clusterId != null ?
        !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }
    if (clusterName != null ?
        !clusterName.equals(that.clusterName) : that.clusterName != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.intValue() : 0;
    result = 71 * result + (clusterName != null ? clusterName.hashCode() : 0);
    return result;
  }

  /**
   * @return the desiredStackVersion
   */
  public String getDesiredStackVersion() {
    return desiredStackVersion;
  }

  /**
   * @param configs
   */
  public void setDesiredConfigs(Map<String, DesiredConfig> configs) {
    desiredConfigs = configs;
  }

  /**
   * @return the desired configs
   */
  public Map<String, DesiredConfig> getDesiredConfigs() {
    return desiredConfigs;
  }

  /**
   * @return total number of hosts in the cluster
   */
  public Integer getTotalHosts() {
    return totalHosts;
  }

  /**
   * @return cluster health report
   */
  public ClusterHealthReport getClusterHealthReport() {
    return clusterHealthReport;
  }

  public Map<String, Collection<ServiceConfigVersionResponse>> getDesiredServiceConfigVersions() {
    return desiredServiceConfigVersions;
  }

  public void setDesiredServiceConfigVersions(Map<String, Collection<ServiceConfigVersionResponse>> desiredServiceConfigVersions) {
    this.desiredServiceConfigVersions = desiredServiceConfigVersions;
  }

  public void setCredentialStoreServiceProperties(Map<String, String> credentialServiceProperties) {
    this.credentialStoreServiceProperties = credentialServiceProperties;
  }

  public Map<String, String> getCredentialStoreServiceProperties() {
    return credentialStoreServiceProperties;
  }
}
