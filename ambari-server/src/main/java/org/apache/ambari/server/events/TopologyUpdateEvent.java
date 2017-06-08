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

import org.apache.ambari.server.agent.stomp.dto.Hashable;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopologyUpdateEvent extends AmbariUpdateEvent implements Hashable {
  @JsonProperty("clusters")
  private TreeMap<String, TopologyCluster> clusters;

  private String hash;

  private EventType eventType;

  public TopologyUpdateEvent(TreeMap<String, TopologyCluster> clusters, EventType eventType) {
    super(Type.TOPOLOGY);
    this.clusters = clusters;
    this.eventType = eventType;
  }

  public TreeMap<String, TopologyCluster> getClusters() {
    return clusters;
  }

  public void setClusters(TreeMap<String, TopologyCluster> clusters) {
    this.clusters = clusters;
  }

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(EventType eventType) {
    this.eventType = eventType;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public static TopologyUpdateEvent emptyUpdate() {
    return new TopologyUpdateEvent(null, null);
  }

  public enum EventType {
    CREATE,
    DELETE,
    UPDATE
  }
}
