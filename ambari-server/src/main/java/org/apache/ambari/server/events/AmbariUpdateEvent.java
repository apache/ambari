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

import java.beans.Transient;

public abstract class AmbariUpdateEvent {
  protected final Type type;

  public AmbariUpdateEvent(Type type) {
    this.type = type;
  }

  @Transient
  public Type getType() {
    return type;
  }

  @Transient
  public String getDestination() {
    return type.getDestination();
  }

  @Transient
  public String getMetricName() {
    return type.getMetricName();
  }

  public enum Type {
    ALERT("/events/alerts", "events.alerts"),
    METADATA("/events/metadata", "events.metadata"),
    TOPOLOGY("/events/topology", "events.topology_update"),
    AGENT_CONFIGS("/events/configs", "events.agent.configs"),
    CONFIGS("/events/configs", "events.configs"),
    HOSTCOMPONENT("/events/hostcomponents", "events.hostcomponents"),
    NAMEDHOSTCOMPONENT("/events/tasks/", "events.hostrolecommands.named"),
    REQUEST("/events/requests", "events.requests"),
    NAMEDREQUEST("/events/requests", "events.requests.named"),
    COMMAND("/user/commands", "events.commands");

    private String destination;
    private String metricName;

    Type(String destination, String metricName) {
      this.destination = destination;
      this.metricName = metricName;
    }

    public String getDestination() {
      return destination;
    }

    public String getMetricName() {
      return metricName;
    }
  }
}
