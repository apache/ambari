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
package org.apache.ambari.server.api.stomp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ambari.server.agent.stomp.dto.AlertCluster;
import org.apache.ambari.server.agent.stomp.dto.AlertGroupUpdate;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;
import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;
import org.apache.ambari.server.api.stomp.ApiStompAuthorizationService.EventAccess;
import org.apache.ambari.server.events.AlertDefinitionsUIUpdateEvent;
import org.apache.ambari.server.events.AlertGroupsUpdateEvent;
import org.apache.ambari.server.events.AlertUpdateEvent;
import org.apache.ambari.server.events.ConfigsUpdateEvent;
import org.apache.ambari.server.events.HostComponentUpdate;
import org.apache.ambari.server.events.HostComponentsUpdateEvent;
import org.apache.ambari.server.events.HostUpdateEvent;
import org.apache.ambari.server.events.NamedTaskUpdateEvent;
import org.apache.ambari.server.events.RequestUpdateEvent;
import org.apache.ambari.server.events.STOMPEvent;
import org.apache.ambari.server.events.ServiceUpdateEvent;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.events.UpgradeUpdateEvent;
import org.springframework.security.core.Authentication;

public class ApiStompEventProjector {
  private final ApiStompAuthorizationService authorizationService;

  public ApiStompEventProjector(ApiStompAuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  public Optional<STOMPEvent> project(STOMPEvent event, String destination, Authentication authentication) {
    if (event == null || destination == null || authentication == null
        || !ApiStompDestinations.destinationFor(event).filter(destination::equals).isPresent()) {
      return Optional.empty();
    }

    switch (event.getType()) {
      case ALERT:
        return event instanceof AlertUpdateEvent
            ? projectAlerts((AlertUpdateEvent) event, authentication) : Optional.empty();
      case ALERT_GROUP:
        return event instanceof AlertGroupsUpdateEvent
            ? projectAlertGroups((AlertGroupsUpdateEvent) event, authentication) : Optional.empty();
      case METADATA:
        return Optional.empty();
      case UI_TOPOLOGY:
        return event instanceof TopologyUpdateEvent
            ? projectTopology((TopologyUpdateEvent) event, authentication) : Optional.empty();
      case CONFIGS:
        return event instanceof ConfigsUpdateEvent
            ? projectConfigs((ConfigsUpdateEvent) event, authentication) : Optional.empty();
      case HOSTCOMPONENT:
        return event instanceof HostComponentsUpdateEvent
            ? projectHostComponents((HostComponentsUpdateEvent) event, authentication) : Optional.empty();
      case NAMEDTASK:
        return event instanceof NamedTaskUpdateEvent
            ? projectNamedTask((NamedTaskUpdateEvent) event, destination, authentication) : Optional.empty();
      case REQUEST:
        return event instanceof RequestUpdateEvent
            ? projectRequest((RequestUpdateEvent) event, authentication) : Optional.empty();
      case SERVICE:
        return event instanceof ServiceUpdateEvent
            ? projectService((ServiceUpdateEvent) event, authentication) : Optional.empty();
      case HOST:
        return event instanceof HostUpdateEvent
            ? projectHost((HostUpdateEvent) event, authentication) : Optional.empty();
      case UI_ALERT_DEFINITIONS:
        return event instanceof AlertDefinitionsUIUpdateEvent
            ? projectAlertDefinitions((AlertDefinitionsUIUpdateEvent) event, authentication) : Optional.empty();
      case UPGRADE:
        return event instanceof UpgradeUpdateEvent
            ? projectUpgrade((UpgradeUpdateEvent) event, authentication) : Optional.empty();
      default:
        return Optional.empty();
    }
  }

  private Optional<STOMPEvent> projectAlerts(AlertUpdateEvent event, Authentication authentication) {
    if (event.getSummaries() == null) {
      return Optional.empty();
    }
    Map<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> summaries = new HashMap<>();
    for (Map.Entry<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> entry
        : event.getSummaries().entrySet()) {
      if (authorizationService.isAuthorized(authentication, entry.getKey(), EventAccess.ALERT)) {
        summaries.put(entry.getKey(), entry.getValue());
      }
    }
    return summaries.isEmpty() ? Optional.empty() : Optional.of(new AlertUpdateEvent(summaries));
  }

  private Optional<STOMPEvent> projectAlertGroups(AlertGroupsUpdateEvent event, Authentication authentication) {
    if (event.getGroups() == null) {
      return Optional.empty();
    }
    List<AlertGroupUpdate> groups = new ArrayList<>();
    for (AlertGroupUpdate group : event.getGroups()) {
      if (group != null && authorizationService.isAuthorized(
          authentication, group.getClusterId(), EventAccess.ALERT)) {
        groups.add(group);
      }
    }
    return groups.isEmpty()
        ? Optional.empty()
        : Optional.of(new AlertGroupsUpdateEvent(groups, event.getUpdateType()));
  }

  private Optional<STOMPEvent> projectTopology(TopologyUpdateEvent event, Authentication authentication) {
    if (event.getClusters() == null) {
      return Optional.empty();
    }
    SortedMap<String, TopologyCluster> clusters = new TreeMap<>();
    for (Map.Entry<String, TopologyCluster> entry : event.getClusters().entrySet()) {
      Long clusterId = parseClusterId(entry.getKey());
      if (clusterId != null && entry.getValue() != null
          && authorizationService.isAuthorized(authentication, clusterId, EventAccess.CLUSTER)) {
        clusters.put(entry.getKey(), entry.getValue().deepCopyCluster());
      }
    }
    if (clusters.isEmpty()) {
      return Optional.empty();
    }
    TopologyUpdateEvent projected = new TopologyUpdateEvent(clusters, event.getEventType());
    return Optional.of(projected);
  }

  private Optional<STOMPEvent> projectConfigs(ConfigsUpdateEvent event, Authentication authentication) {
    if (event.getClusterId() == null
        || !authorizationService.isAuthorized(authentication, event.getClusterId(), EventAccess.CONFIG)) {
      return Optional.empty();
    }
    if (event.getConfigs() != null) {
      for (ConfigsUpdateEvent.ClusterConfig config : event.getConfigs()) {
        if (config == null || !event.getClusterId().equals(config.getClusterId())) {
          return Optional.empty();
        }
      }
    }
    return Optional.of(event);
  }

  private Optional<STOMPEvent> projectHostComponents(HostComponentsUpdateEvent event,
                                                      Authentication authentication) {
    if (event.getHostComponentUpdates() == null) {
      return Optional.empty();
    }
    List<HostComponentUpdate> updates = new ArrayList<>();
    for (HostComponentUpdate update : event.getHostComponentUpdates()) {
      if (update != null && authorizationService.isAuthorized(
          authentication, update.getClusterId(), EventAccess.SERVICE)) {
        updates.add(update);
      }
    }
    return updates.isEmpty() ? Optional.empty() : Optional.of(new HostComponentsUpdateEvent(updates));
  }

  private Optional<STOMPEvent> projectNamedTask(NamedTaskUpdateEvent event, String destination,
                                                 Authentication authentication) {
    Optional<Long> destinationTaskId = ApiStompDestinations.taskId(destination);
    return destinationTaskId.filter(event.getId()::equals).isPresent()
        && authorizationService.canViewTask(authentication, event.getId(), event.getRequestId())
        ? Optional.of(event) : Optional.empty();
  }

  private Optional<STOMPEvent> projectRequest(RequestUpdateEvent event, Authentication authentication) {
    if (event.getRequestId() == null || !requestTasksMatch(event)
        || !authorizationService.canViewRequest(authentication, event.getRequestId(), event.getClusterName())) {
      return Optional.empty();
    }
    return Optional.of(event);
  }

  private Optional<STOMPEvent> projectService(ServiceUpdateEvent event, Authentication authentication) {
    return authorizationService.isAuthorized(authentication, event.getClusterName(), EventAccess.SERVICE)
        ? Optional.of(event) : Optional.empty();
  }

  private Optional<STOMPEvent> projectHost(HostUpdateEvent event, Authentication authentication) {
    return authorizationService.isAuthorized(authentication, event.getClusterName(), EventAccess.CLUSTER)
        ? Optional.of(event) : Optional.empty();
  }

  private Optional<STOMPEvent> projectAlertDefinitions(AlertDefinitionsUIUpdateEvent event,
                                                        Authentication authentication) {
    if (event.getClusters() == null) {
      return Optional.empty();
    }
    Map<Long, AlertCluster> clusters = new HashMap<>();
    for (Map.Entry<Long, AlertCluster> entry : event.getClusters().entrySet()) {
      if (authorizationService.isAuthorized(authentication, entry.getKey(), EventAccess.ALERT)) {
        clusters.put(entry.getKey(), entry.getValue());
      }
    }
    return clusters.isEmpty()
        ? Optional.empty()
        : Optional.of(new AlertDefinitionsUIUpdateEvent(event.getEventType(), clusters));
  }

  private Optional<STOMPEvent> projectUpgrade(UpgradeUpdateEvent event, Authentication authentication) {
    return authorizationService.canViewUpgrade(authentication, event.getClusterId(), event.getRequestId())
        ? Optional.of(event) : Optional.empty();
  }

  private boolean requestTasksMatch(RequestUpdateEvent event) {
    if (event.getHostRoleCommands() == null) {
      return true;
    }
    for (RequestUpdateEvent.HostRoleCommand task : event.getHostRoleCommands()) {
      if (task == null || !event.getRequestId().equals(task.getRequestId())) {
        return false;
      }
    }
    return true;
  }

  private Long parseClusterId(String clusterId) {
    try {
      return Long.valueOf(clusterId);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
