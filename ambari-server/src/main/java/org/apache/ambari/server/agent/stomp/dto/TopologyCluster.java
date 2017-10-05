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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.commons.collections.SetUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TopologyCluster {
  @JsonProperty("components")
  private Set<TopologyComponent> topologyComponents = new HashSet<>();

  @JsonProperty("hosts")
  private Set<TopologyHost> topologyHosts = new HashSet<>();

  public TopologyCluster() {
  }

  public TopologyCluster(Set<TopologyComponent> topologyComponents, Set<TopologyHost> topologyHosts) {
    this.topologyComponents = topologyComponents;
    this.topologyHosts = topologyHosts;
  }

  public void update(Set<TopologyComponent> componentsToUpdate, Set<TopologyHost> hostsToUpdate,
                     TopologyUpdateEvent.EventType eventType) {
    for (TopologyComponent componentToUpdate : componentsToUpdate) {
      boolean updated = false;
      for (Iterator<TopologyComponent> iter = getTopologyComponents().iterator(); iter.hasNext() && !updated; ) {
        TopologyComponent existsComponent = iter.next();
        if (existsComponent.equals(componentToUpdate)) {
          if (eventType.equals(TopologyUpdateEvent.EventType.DELETE)) {
            if (SetUtils.isEqualSet(existsComponent.getHostIds(), componentToUpdate.getHostIds())) {
              iter.remove();
            } else {
              existsComponent.removeComponent(componentToUpdate);
            }
          } else {
            existsComponent.updateComponent(componentToUpdate);
          }
          updated = true;
        }
      }
      if (!updated && eventType.equals(TopologyUpdateEvent.EventType.UPDATE)) {
        getTopologyComponents().add(componentToUpdate);
      }
    }
    for (TopologyHost hostToUpdate : hostsToUpdate) {
      boolean updated = false;
      for (Iterator<TopologyHost> iter = getTopologyHosts().iterator(); iter.hasNext() && !updated; ) {
        TopologyHost existsHost = iter.next();
        if (existsHost.equals(hostToUpdate)) {
          if (eventType.equals(TopologyUpdateEvent.EventType.DELETE)) {
            iter.remove();
          } else {
            existsHost.updateHost(hostToUpdate);
          }
          updated = true;
        }
      }
      if (!updated && eventType.equals(TopologyUpdateEvent.EventType.UPDATE)) {
        getTopologyHosts().add(hostToUpdate);
      }
    }
  }

  public Set<TopologyComponent> getTopologyComponents() {
    return topologyComponents;
  }

  public void setTopologyComponents(Set<TopologyComponent> topologyComponents) {
    this.topologyComponents = topologyComponents;
  }

  public Set<TopologyHost> getTopologyHosts() {
    return topologyHosts;
  }

  public Set<TopologyHost> deepCopyTopologyHosts() {
    return topologyHosts;
  }

  public TopologyCluster deepCopyCluster() {
    Set<TopologyComponent> copiedComponents = new HashSet<>();
    for (TopologyComponent topologyComponent : topologyComponents) {
      copiedComponents.add(topologyComponent.deepCopy());
    }
    Set<TopologyHost> copiedHosts = new HashSet<>();
    for (TopologyHost topologyHost : topologyHosts) {
      copiedHosts.add(topologyHost.deepCopy());
    }
    return new TopologyCluster(copiedComponents, copiedHosts);
  }

  public void setTopologyHosts(Set<TopologyHost> topologyHosts) {
    this.topologyHosts = topologyHosts;
  }

  public void addTopologyHost(TopologyHost topologyHost) {
    topologyHosts.add(topologyHost);
  }

  public void addTopologyComponent(TopologyComponent topologyComponent) {
    topologyComponents.add(topologyComponent);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyCluster that = (TopologyCluster) o;

    if (topologyComponents != null ? !topologyComponents.equals(that.topologyComponents) : that.topologyComponents != null)
      return false;
    return topologyHosts != null ? topologyHosts.equals(that.topologyHosts) : that.topologyHosts == null;
  }

  @Override
  public int hashCode() {
    int result = topologyComponents != null ? topologyComponents.hashCode() : 0;
    result = 31 * result + (topologyHosts != null ? topologyHosts.hashCode() : 0);
    return result;
  }
}
