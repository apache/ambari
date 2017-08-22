/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events;

import java.util.Objects;
import java.util.SortedMap;

import org.apache.ambari.server.agent.stomp.dto.ClusterConfigs;
import org.apache.ambari.server.agent.stomp.dto.Hashable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains info about configs update for one host. This update will be sent to single host only.
 * Host can be identified by AgentConfigsUpdateEvent#hostName.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentConfigsUpdateEvent extends AmbariHostUpdateEvent implements Hashable {

  /**
   * Actual version hash.
   */
  private String hash;

  /**
   * Host identifier.
   */
  private String hostName;

  /**
   * Configs grouped by cluster id as keys.
   */
  @JsonProperty("clusters")
  private final SortedMap<String, ClusterConfigs> clustersConfigs;

  public AgentConfigsUpdateEvent(SortedMap<String, ClusterConfigs> clustersConfigs) {
    super(Type.AGENT_CONFIGS);
    this.clustersConfigs = clustersConfigs;
  }

  @Override
  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public static AgentConfigsUpdateEvent emptyUpdate() {
    return new AgentConfigsUpdateEvent(null);
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

    AgentConfigsUpdateEvent that = (AgentConfigsUpdateEvent) o;

    return Objects.equals(hostName, that.hostName) &&
      Objects.equals(clustersConfigs, that.clustersConfigs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostName, clustersConfigs);
  }
}
