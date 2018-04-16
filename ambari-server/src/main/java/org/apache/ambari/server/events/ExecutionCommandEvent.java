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
package org.apache.ambari.server.events;

import java.util.TreeMap;

import org.apache.ambari.server.agent.stomp.dto.ExecutionCommandsCluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event to send execution commands to agent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionCommandEvent extends STOMPHostEvent {

  /**
   * Host id with agent execution commands will be send to.
   */
  private Long hostId;

  /**
   *
   */
  @JsonProperty("requiredConfigTimestamp")
  private Long requiredConfigTimestamp;

  /**
   * Execution commands grouped by cluster id.
   */
  @JsonProperty("clusters")
  private TreeMap<String, ExecutionCommandsCluster> clusters;

  public ExecutionCommandEvent(TreeMap<String, ExecutionCommandsCluster> clusters) {
    super(Type.COMMAND);
    this.clusters = clusters;
  }

  public TreeMap<String, ExecutionCommandsCluster> getClusters() {
    return clusters;
  }

  public void setClusters(TreeMap<String, ExecutionCommandsCluster> clusters) {
    this.clusters = clusters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExecutionCommandEvent that = (ExecutionCommandEvent) o;

    if (hostId != null ? !hostId.equals(that.hostId) : that.hostId != null) return false;
    return clusters != null ? clusters.equals(that.clusters) : that.clusters == null;
  }

  @Override
  public int hashCode() {
    int result = hostId != null ? hostId.hashCode() : 0;
    result = 31 * result + (clusters != null ? clusters.hashCode() : 0);
    return result;
  }

  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  @Override
  public Long getHostId() {
    return hostId;
  }

  public Long getRequiredConfigTimestamp() {
    return requiredConfigTimestamp;
  }

  public void setRequiredConfigTimestamp(Long requiredConfigTimestamp) {
    this.requiredConfigTimestamp = requiredConfigTimestamp;
  }
}
