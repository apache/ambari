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
package org.apache.ambari.server.agent.stomp.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.ambari.server.state.SecurityType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MetadataCluster {
  @JsonProperty("status_commands_to_run")
  private List<String> statusCommandsToRun = new ArrayList<>();
  private TreeMap<String, MetadataServiceInfo> serviceLevelParams;
  private TreeMap<String, String> clusterLevelParams;

  public MetadataCluster(SecurityType securityType, TreeMap<String,MetadataServiceInfo> serviceLevelParams,
                         TreeMap<String, String> clusterLevelParams) {
    this.statusCommandsToRun.add("STATUS");
    if (SecurityType.KERBEROS.equals(securityType)) {
      this.statusCommandsToRun.add("SECURITY_STATUS");
    }
    this.serviceLevelParams = serviceLevelParams;
    this.clusterLevelParams = clusterLevelParams;
  }

  public List<String> getStatusCommandsToRun() {
    return statusCommandsToRun;
  }

  public void setStatusCommandsToRun(List<String> statusCommandsToRun) {
    this.statusCommandsToRun = statusCommandsToRun;
  }

  public TreeMap<String, MetadataServiceInfo> getServiceLevelParams() {
    return serviceLevelParams;
  }

  public void setServiceLevelParams(TreeMap<String, MetadataServiceInfo> serviceLevelParams) {
    this.serviceLevelParams = serviceLevelParams;
  }

  public TreeMap<String, String> getClusterLevelParams() {
    return clusterLevelParams;
  }

  public void setClusterLevelParams(TreeMap<String, String> clusterLevelParams) {
    this.clusterLevelParams = clusterLevelParams;
  }
}
