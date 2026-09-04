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
package org.apache.ambari.server.agent.stomp;

import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.ClusterConfigChangedEvent;
import org.apache.ambari.server.events.HostRegisteredEvent;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.ServiceComponentInstalledEvent;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.StackUpgradeFinishEvent;
import org.apache.ambari.server.events.TelemetryUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Maintains and publishes host-specific telemetry assignments.
 */
@Singleton
public class TelemetryHolder extends AgentHostDataHolder<TelemetryUpdateEvent> {
  private static final Logger LOG = LoggerFactory.getLogger(TelemetryHolder.class);

  private final TelemetryAssignmentCompiler compiler;
  private final Provider<Clusters> clusters;

  @Inject
  public TelemetryHolder(TelemetryAssignmentCompiler compiler, Provider<Clusters> clusters,
      AmbariEventPublisher eventPublisher) {
    this.compiler = compiler;
    this.clusters = clusters;
    eventPublisher.register(this);
  }

  @Override
  protected TelemetryUpdateEvent getCurrentData(Long hostId) throws AmbariException {
    return compiler.compile(hostId);
  }

  @Override
  protected TelemetryUpdateEvent getCurrentData(Long hostId,
      Map<Long, Map<String, DesiredConfig>> cachedClustersDesiredConfigs) throws AmbariException {
    return getCurrentData(hostId);
  }

  @Override
  protected TelemetryUpdateEvent handleUpdate(TelemetryUpdateEvent current,
      TelemetryUpdateEvent update) {
    return update;
  }

  @Override
  protected TelemetryUpdateEvent getEmptyData() {
    return TelemetryUpdateEvent.emptyUpdate();
  }

  @Subscribe
  public void onClusterConfigChanged(ClusterConfigChangedEvent event) {
    try {
      Cluster cluster = clusters.get().getCluster(event.getClusterName());
      for (Host host : cluster.getHosts()) {
        refreshHost(host.getHostId());
      }
    } catch (AmbariException e) {
      LOG.error("Unable to refresh telemetry after a configuration change", e);
    }
  }

  @Subscribe
  public void onComponentInstalled(ServiceComponentInstalledEvent event) {
    refreshHost(event.getHostName());
  }

  @Subscribe
  public void onComponentUninstalled(ServiceComponentUninstalledEvent event) {
    refreshHost(event.getHostName());
  }

  @Subscribe
  public void onHostRegistered(HostRegisteredEvent event) {
    refreshHost(event.getHostId());
  }

  @Subscribe
  public void onHostsRemoved(HostsRemovedEvent event) {
    for (Long hostId : event.getHostIds()) {
      onHostRemoved(hostId);
    }
  }

  @Subscribe
  public void onStackUpgradeFinished(StackUpgradeFinishEvent event) {
    refreshCluster(event.getCluster());
  }

  private void refreshCluster(Cluster cluster) {
    for (Host host : cluster.getHosts()) {
      refreshHost(host.getHostId());
    }
  }

  private void refreshHost(String hostName) {
    try {
      refreshHost(clusters.get().getHost(hostName).getHostId());
    } catch (AmbariException e) {
      LOG.error("Unable to resolve host {} for telemetry refresh", hostName, e);
    }
  }

  private void refreshHost(Long hostId) {
    try {
      updateData(compiler.compile(hostId));
    } catch (AmbariException e) {
      LOG.error("Unable to refresh telemetry for host {}", hostId, e);
    }
  }
}
