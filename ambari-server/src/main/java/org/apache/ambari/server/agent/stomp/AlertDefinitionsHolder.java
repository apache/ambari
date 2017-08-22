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
package org.apache.ambari.server.agent.stomp;

import static org.apache.ambari.server.events.AlertDefinitionsUpdateEvent.EventType.CREATE;
import static org.apache.ambari.server.events.AlertDefinitionsUpdateEvent.EventType.DELETE;
import static org.apache.ambari.server.events.AlertDefinitionsUpdateEvent.EventType.UPDATE;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.AlertCluster;
import org.apache.ambari.server.events.AlertDefinitionChangedEvent;
import org.apache.ambari.server.events.AlertDefinitionDeleteEvent;
import org.apache.ambari.server.events.AlertDefinitionRegistrationEvent;
import org.apache.ambari.server.events.AlertDefinitionsUpdateEvent;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.ServiceComponentInstalledEvent;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

@Singleton
public class AlertDefinitionsHolder extends AgentHostDataHolder<AlertDefinitionsUpdateEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(AlertDefinitionsHolder.class);

  @Inject
  private Provider<AlertDefinitionHash> helper;

  @Inject
  private Provider<Clusters> clusters;

  @Inject
  public AlertDefinitionsHolder(AmbariEventPublisher eventPublisher) {
    eventPublisher.register(this);
  }

  @Override
  protected AlertDefinitionsUpdateEvent getCurrentData(String hostName) throws AmbariException {
    Map<Long, AlertCluster> result = new TreeMap<>();
    Map<Long, Map<Long, AlertDefinition>> alertDefinitions = helper.get().getAlertDefinitions(hostName);
    long count = 0;
    for (Map.Entry<Long, Map<Long, AlertDefinition>> e : alertDefinitions.entrySet()) {
      Long clusterId = e.getKey();
      Map<Long, AlertDefinition> definitionMap = e.getValue();
      result.put(clusterId, new AlertCluster(definitionMap, hostName));
      count += definitionMap.size();
    }
    LOG.info("Loaded {} alert definitions for {} clusters for host {}", count, result.size(), hostName);
    return new AlertDefinitionsUpdateEvent(CREATE, result, hostName);
  }

  @Override
  protected AlertDefinitionsUpdateEvent getEmptyData() {
    return AlertDefinitionsUpdateEvent.emptyEvent();
  }

  @Override
  protected boolean handleUpdate(AlertDefinitionsUpdateEvent update) throws AmbariException {
    Map<Long, AlertCluster> updateClusters = update.getClusters();
    if (updateClusters.isEmpty()) {
      return false;
    }

    String hostName = update.getHostName();
    boolean changed = false;
    Map<Long, AlertCluster> existingClusters = getData(hostName).getClusters();

    switch (update.getEventType()) {
      case UPDATE:
      case DELETE:
        if (!existingClusters.keySet().containsAll(updateClusters.keySet())) {
          throw new AmbariException("Unknown clusters in update");
        }
        for (Map.Entry<Long, AlertCluster> e : updateClusters.entrySet()) {
          changed |= existingClusters.get(e.getKey()).handleUpdate(update.getEventType(), e.getValue());
        }
        LOG.debug("Handled {} of alerts for {} cluster(s) on host {}, changed = {}", update.getEventType(), updateClusters.size(), hostName, changed);
        break;
      case CREATE:
        if (!updateClusters.isEmpty()) {
          if (!Sets.intersection(existingClusters.keySet(), updateClusters.keySet()).isEmpty()) {
            throw new AmbariException("Existing clusters in create");
          }
          existingClusters.putAll(updateClusters);
          LOG.debug("Handled {} of alerts for {} cluster(s)", update.getEventType(), updateClusters.size());
          changed = true;
        }
        break;
      default:
        LOG.warn("Unhandled event type {}", update.getEventType());
        break;
    }

    return changed;
  }

  @Subscribe
  public void onAlertDefinitionRegistered(AlertDefinitionRegistrationEvent event) {
    handleSingleDefinitionChange(UPDATE, event.getDefinition());
  }

  @Subscribe
  public void onAlertDefinitionChanged(AlertDefinitionChangedEvent event) {
    handleSingleDefinitionChange(UPDATE, event.getDefinition());
  }

  @Subscribe
  public void onAlertDefinitionDeleted(AlertDefinitionDeleteEvent event) {
    handleSingleDefinitionChange(DELETE, event.getDefinition());
  }

  @Subscribe
  public void onServiceComponentInstalled(ServiceComponentInstalledEvent event) {
    String hostName = event.getHostName();
    String serviceName = event.getServiceName();
    String componentName = event.getComponentName();

    Map<Long, AlertDefinition> definitions = helper.get().findByServiceComponent(event.getClusterId(), serviceName, componentName);

    if (event.isMasterComponent()) {
      try {
        Cluster cluster = clusters.get().getClusterById(event.getClusterId());
        if (cluster.getService(serviceName).getServiceComponents().get(componentName).getServiceComponentHosts().containsKey(hostName)) {
          definitions.putAll(helper.get().findByServiceMaster(event.getClusterId(), serviceName));
        }
      } catch (AmbariException e) {
        String msg = String.format("Failed to get alert definitions for master component %s/%s", serviceName, componentName);
        LOG.warn(msg, e);
      }
    }

    Map<Long, AlertCluster> map = Collections.singletonMap(event.getClusterId(), new AlertCluster(definitions, hostName));
    safelyUpdateData(new AlertDefinitionsUpdateEvent(UPDATE, map, hostName));
  }

  @Subscribe
  public void onServiceComponentUninstalled(ServiceComponentUninstalledEvent event) {
    String hostName = event.getHostName();
    Map<Long, AlertDefinition> definitions = helper.get().findByServiceComponent(event.getClusterId(), event.getServiceName(), event.getComponentName());
    if (event.isMasterComponent()) {
      definitions.putAll(helper.get().findByServiceMaster(event.getClusterId(), event.getServiceName()));
    }
    Map<Long, AlertCluster> map = Collections.singletonMap(event.getClusterId(), new AlertCluster(definitions, hostName));
    safelyUpdateData(new AlertDefinitionsUpdateEvent(DELETE, map, hostName));
  }

  @Subscribe
  public void onHostsRemoved(HostsRemovedEvent event) {
    for (String hostName : event.getHostNames()) {
      onHostRemoved(hostName);
    }
  }

  private void safelyUpdateData(AlertDefinitionsUpdateEvent event) {
    try {
      updateData(event);
    } catch (AmbariException e) {
      LOG.warn(String.format("Failed to %s alert definitions for host %s", event.getEventType(), event.getHostName()), e);
    }
  }

  private void safelyResetData(String hostName) {
    try {
      resetData(hostName);
    } catch (AmbariException e) {
      LOG.warn(String.format("Failed to reset alert definitions for host %s", hostName), e);
    }
  }

  private void handleSingleDefinitionChange(AlertDefinitionsUpdateEvent.EventType eventType, AlertDefinition alertDefinition) {
    LOG.info("{} alert definition '{}'", eventType, alertDefinition);
    Set<String> hosts = helper.get().invalidateHosts(alertDefinition);
    for (String hostName : hosts) {
      Map<Long, AlertCluster> update = Collections.singletonMap(alertDefinition.getClusterId(), new AlertCluster(alertDefinition, hostName));
      AlertDefinitionsUpdateEvent event = new AlertDefinitionsUpdateEvent(eventType, update, hostName);
      safelyUpdateData(event);
    }
  }

}
