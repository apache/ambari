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

import java.beans.Transient;

/**
 * Update data from server side, will be sent as STOMP message to recipients from all hosts.
 */
public abstract class AmbariUpdateEvent {

  /**
   * Update type.
   */
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
    HOSTLEVELPARAMS("/host_level_params", "events.hostlevelparams"),
    UI_TOPOLOGY("/events/ui_topologies", "events.topology_update"),
    AGENT_TOPOLOGY("/events/topologies", "events.topology_update"),
    AGENT_CONFIGS("/configs", "events.agent.configs"),
    CONFIGS("/events/configs", "events.configs"),
    HOSTCOMPONENT("/events/hostcomponents", "events.hostcomponents"),
    NAMEDHOSTCOMPONENT("/events/tasks/", "events.hostrolecommands.named"),
    REQUEST("/events/requests", "events.requests"),
    SERVICE("/events/services", "events.services"),
    HOST("/events/hosts", "events.hosts"),
    ALERT_DEFINITIONS("/alert_definitions", "events.alert_definitions"),
    COMMAND("/commands", "events.commands");

    /**
     * Destination is used for delivery message to recipients.
     */
    private String destination;

    /**
     * Is used to collect info about event appearing frequency.
     */
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
