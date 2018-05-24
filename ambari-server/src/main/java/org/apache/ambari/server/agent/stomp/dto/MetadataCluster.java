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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.state.SecurityType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MetadataCluster {
  private final static Lock LOCK = new ReentrantLock();

  @JsonProperty("status_commands_to_run")
  private final Set<String> statusCommandsToRun;
  private final SortedMap<String, MetadataServiceInfo> serviceLevelParams;
  private final SortedMap<String, String> clusterLevelParams;
  private final SortedMap<String, SortedMap<String,String>> agentConfigs;

  public MetadataCluster(SecurityType securityType, SortedMap<String,MetadataServiceInfo> serviceLevelParams,
                         SortedMap<String, String> clusterLevelParams, SortedMap<String, SortedMap<String,String>> agentConfigs) {
    this.statusCommandsToRun  = new HashSet<>();
    if (securityType != null) {
      this.statusCommandsToRun.add("STATUS");
      if (SecurityType.KERBEROS.equals(securityType)) {
        this.statusCommandsToRun.add("SECURITY_STATUS");
      }
    }
    this.serviceLevelParams = serviceLevelParams == null ? new TreeMap<>() : serviceLevelParams;
    this.clusterLevelParams = clusterLevelParams == null ? new TreeMap<>() : clusterLevelParams;
    this.agentConfigs = agentConfigs == null ? new TreeMap<>() : agentConfigs;
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

  public SortedMap<String, SortedMap<String, String>> getAgentConfigs() {
    return agentConfigs;
  }

  public boolean updateServiceLevelParams(SortedMap<String, MetadataServiceInfo> update) {
    return updateMapIfNeeded(serviceLevelParams, update);
  }

  public boolean updateClusterLevelParams(SortedMap<String, String> update) {
    return updateMapIfNeeded(clusterLevelParams, update);
  }

  private <T> boolean updateMapIfNeeded(Map<String, T> mapToBeUpdated, Map<String, T> updatedMap) {
    try {
      LOCK.lock();
      final boolean changed = !Objects.equals(mapToBeUpdated, updatedMap);

      if (changed) {
        mapToBeUpdated.clear();
        mapToBeUpdated.putAll(updatedMap);
      }
      return changed;
    } finally {
      LOCK.unlock();
    }
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
