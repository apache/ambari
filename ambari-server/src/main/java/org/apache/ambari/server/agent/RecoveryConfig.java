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

package org.apache.ambari.server.agent;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Recovery config to be sent to the agent
 */
public class RecoveryConfig {

  @SerializedName("components")
  @JsonProperty("components")
  private List<RecoveryConfigComponent> enabledComponents;

  @SerializedName("topology_epoch")
  @JsonProperty("topology_epoch")
  private String topologyEpoch;

  @SerializedName("topology_version")
  @JsonProperty("topology_version")
  private long topologyVersion;

  @SerializedName("topology_complete")
  @JsonProperty("topology_complete")
  private boolean topologyComplete;

  public RecoveryConfig(List<RecoveryConfigComponent> enabledComponents) {
    this(enabledComponents, null, 0, true);
  }

  public RecoveryConfig(List<RecoveryConfigComponent> enabledComponents, String topologyEpoch,
      long topologyVersion, boolean topologyComplete) {
    this.enabledComponents = enabledComponents;
    this.topologyEpoch = topologyEpoch;
    this.topologyVersion = topologyVersion;
    this.topologyComplete = topologyComplete;
  }

  public List<RecoveryConfigComponent> getEnabledComponents() {
    return enabledComponents == null ? null : Collections.unmodifiableList(enabledComponents);
  }

  public String getTopologyEpoch() {
    return topologyEpoch;
  }

  public long getTopologyVersion() {
    return topologyVersion;
  }

  public boolean isTopologyComplete() {
    return topologyComplete;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RecoveryConfig that = (RecoveryConfig) o;

    if (topologyVersion != that.topologyVersion || topologyComplete != that.topologyComplete) {
      return false;
    }
    if (enabledComponents != null ? !enabledComponents.equals(that.enabledComponents) : that.enabledComponents != null) {
      return false;
    }
    return topologyEpoch != null ? topologyEpoch.equals(that.topologyEpoch) : that.topologyEpoch == null;
  }

  @Override
  public int hashCode() {
    int result = (enabledComponents != null ? enabledComponents.hashCode() : 0);
    result = 31 * result + (topologyEpoch != null ? topologyEpoch.hashCode() : 0);
    result = 31 * result + Long.hashCode(topologyVersion);
    result = 31 * result + (topologyComplete ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("RecoveryConfig{");
    buffer.append(", components=").append(enabledComponents);
    buffer.append(", topologyEpoch=").append(topologyEpoch);
    buffer.append(", topologyVersion=").append(topologyVersion);
    buffer.append(", topologyComplete=").append(topologyComplete);
    buffer.append('}');
    return buffer.toString();
  }
}
