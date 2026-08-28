/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.util.Map;

import org.apache.ambari.server.agent.stomp.dto.Hashable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Complete Prometheus telemetry configuration for one Agent host.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelemetryUpdateEvent extends STOMPHostEvent implements Hashable {
  private String hash;
  private final Long hostId;
  private final ObjectNode assignment;
  private final Map<String, JsonNode> profiles;

  public TelemetryUpdateEvent(Long hostId, ObjectNode assignment, Map<String, JsonNode> profiles) {
    super(Type.TELEMETRY);
    this.hostId = hostId;
    this.assignment = assignment;
    this.profiles = profiles;
  }

  @Override
  public String getHash() {
    return hash;
  }

  @Override
  public void setHash(String hash) {
    this.hash = hash;
  }

  @Override
  public Long getHostId() {
    return hostId;
  }

  public ObjectNode getAssignment() {
    return assignment;
  }

  public Map<String, JsonNode> getProfiles() {
    return profiles;
  }

  public static TelemetryUpdateEvent emptyUpdate() {
    return new TelemetryUpdateEvent(null, null, null);
  }
}
