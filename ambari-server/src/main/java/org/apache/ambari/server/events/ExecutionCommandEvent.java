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
public class ExecutionCommandEvent extends AmbariHostUpdateEvent {

  /**
   * Host name with agent execution commands will be send to.
   */
  private String hostName;

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

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @Override
  public String getHostName() {
    return hostName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExecutionCommandEvent that = (ExecutionCommandEvent) o;

    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    return clusters != null ? clusters.equals(that.clusters) : that.clusters == null;
  }

  @Override
  public int hashCode() {
    int result = hostName != null ? hostName.hashCode() : 0;
    result = 31 * result + (clusters != null ? clusters.hashCode() : 0);
    return result;
  }
}
