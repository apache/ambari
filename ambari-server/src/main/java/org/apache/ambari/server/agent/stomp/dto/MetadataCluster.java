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
package org.apache.ambari.server.agent.stomp.dto;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ambari.server.state.SecurityType;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MetadataCluster {
  @JsonProperty("status_commands_to_run")
  private Set<String> statusCommandsToRun = new HashSet<>();
  private SortedMap<String, MetadataServiceInfo> serviceLevelParams = new TreeMap<>();
  private SortedMap<String, String> clusterLevelParams = new TreeMap<>();

  private SortedMap<String, SortedMap<String,String>> agentConfigs = new TreeMap<>();

  public MetadataCluster(SecurityType securityType, SortedMap<String,MetadataServiceInfo> serviceLevelParams,
                         SortedMap<String, String> clusterLevelParams, SortedMap<String, SortedMap<String,String>> agentConfigs) {
    if (securityType != null) {
      this.statusCommandsToRun.add("STATUS");
      if (SecurityType.KERBEROS.equals(securityType)) {
        this.statusCommandsToRun.add("SECURITY_STATUS");
      }
    }
    this.serviceLevelParams = serviceLevelParams;
    this.clusterLevelParams = clusterLevelParams;
    this.agentConfigs = agentConfigs;
  }

  public static MetadataCluster emptyMetadataCluster() {
    return new MetadataCluster(null, null, null, null);
  }

  public Set<String> getStatusCommandsToRun() {
    return statusCommandsToRun;
  }

  public SortedMap<String, MetadataServiceInfo> getServiceLevelParams() {
    return serviceLevelParams;
  }

  public SortedMap<String, String> getClusterLevelParams() {
    return clusterLevelParams;
  }

  public void setClusterLevelParams(SortedMap<String, String> clusterLevelParams) {
    this.clusterLevelParams = clusterLevelParams;
  }

  public SortedMap<String, SortedMap<String, String>> getAgentConfigs() {
    return agentConfigs;
  }

  public void setAgentConfigs(SortedMap<String, SortedMap<String, String>> agentConfigs) {
    this.agentConfigs = agentConfigs;
  }

  public boolean updateServiceLevelParams(SortedMap<String, MetadataServiceInfo> update) {
    boolean changed = false;
    for (String key : update.keySet()) {
      if (!serviceLevelParams.containsKey(key) || !serviceLevelParams.get(key).equals(update.get(key))) {
        changed = true;
        break;
      }
    }
    if (changed) {
      serviceLevelParams.putAll(update);
    }
    return changed;
  }

  public boolean updateClusterLevelParams(SortedMap<String, String> update) {
    boolean changed = false;
    for (String key : update.keySet()) {
      if (!clusterLevelParams.containsKey(key) || !StringUtils.equals(clusterLevelParams.get(key), update.get(key))) {
        changed = true;
        break;
      }
    }
    if (changed) {
      clusterLevelParams.putAll(update);
    }
    return changed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MetadataCluster that = (MetadataCluster) o;

    return Objects.equals(statusCommandsToRun, that.statusCommandsToRun) &&
      Objects.equals(serviceLevelParams, that.serviceLevelParams) &&
      Objects.equals(clusterLevelParams, that.clusterLevelParams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statusCommandsToRun, serviceLevelParams, clusterLevelParams);
  }
}
