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
package org.apache.ambari.server.agent.stomp.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.events.AlertDefinitionEventType;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertCluster {

  private static final Logger LOG = LoggerFactory.getLogger(AlertCluster.class);

  private final Map<Long, AlertDefinition> alertDefinitions;
  private final String hostName;
  private Integer staleIntervalMultiplier;

  public AlertCluster(AlertDefinition alertDefinition, String hostName, Integer staleIntervalMultiplier) {
    this(Collections.singletonMap(alertDefinition.getDefinitionId(), alertDefinition), hostName, staleIntervalMultiplier);
  }

  public AlertCluster(Map<Long, AlertDefinition> alertDefinitions, String hostName, Integer staleIntervalMultiplier) {
    this.alertDefinitions = new HashMap<>(alertDefinitions);
    this.hostName = hostName;
    this.staleIntervalMultiplier = staleIntervalMultiplier;
  }

  public AlertCluster(AlertDefinition alertDefinition, String hostName) {
    this(Collections.singletonMap(alertDefinition.getDefinitionId(), alertDefinition), hostName, null);
  }

  public AlertCluster(Map<Long, AlertDefinition> alertDefinitions, String hostName) {
    this.alertDefinitions = new HashMap<>(alertDefinitions);
    this.hostName = hostName;
    this.staleIntervalMultiplier = null;
  }

  @JsonProperty("staleIntervalMultiplier")
  public Integer getStaleIntervalMultiplier() {
    return staleIntervalMultiplier;
  }

  @JsonProperty("alertDefinitions")
  public Collection<AlertDefinition> getAlertDefinitions() {
    return alertDefinitions.values();
  }

  @JsonProperty("hostName")
  public String getHostName() {
    return hostName;
  }

  public boolean handleUpdate(AlertDefinitionEventType eventType, AlertCluster update) {
    boolean changed = false;

    switch (eventType) {
      case CREATE:
        // FIXME should clear map first?
      case UPDATE:
        changed = !alertDefinitions.keySet().containsAll(update.alertDefinitions.keySet());
        if (changed) {
          alertDefinitions.putAll(update.alertDefinitions);
        } else {
          for (Map.Entry<Long, AlertDefinition> e : update.alertDefinitions.entrySet()) {
            Long definitionId = e.getKey();
            AlertDefinition newDefinition = e.getValue();
            AlertDefinition oldDefinition = alertDefinitions.put(definitionId, newDefinition);
            changed = changed || !oldDefinition.deeplyEquals(newDefinition);
          }
        }
        if (update.getStaleIntervalMultiplier() != null
            && !update.getStaleIntervalMultiplier().equals(staleIntervalMultiplier)) {
          staleIntervalMultiplier = update.getStaleIntervalMultiplier();
          changed = true;
        }
        LOG.debug("Handled {} of {} alerts, changed = {}", eventType, update.alertDefinitions.size(), changed);
        break;
      case DELETE:
        changed = alertDefinitions.keySet().removeAll(update.alertDefinitions.keySet());
        LOG.debug("Handled {} of {} alerts", eventType, update.alertDefinitions.size());
        break;
      default:
        LOG.warn("Unhandled event type {}", eventType);
        break;
    }

    return changed;
  }
}
