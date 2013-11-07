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

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.DesiredConfig;

public class ClusterResponse {

  private final Long clusterId;

  private final String clusterName;

  private final Set<String> hostNames;

  private final String desiredStackVersion;

  private Map<String, DesiredConfig> desiredConfigs;
  
  public ClusterResponse(Long clusterId, String clusterName,
      Set<String> hostNames, String desiredStackVersion) {
    super();
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.hostNames = hostNames;
    this.desiredStackVersion = desiredStackVersion;
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{"
        + " clusterName=" + clusterName
        + ", clusterId=" + clusterId
        + ", desiredStackVersion=" + desiredStackVersion
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

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

}
