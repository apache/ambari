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

import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.agent.stomp.dto.HostLevelParamsCluster;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.events.HostLevelParamsUpdateEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceComponentRecoveryChangedEvent;
import org.apache.ambari.server.events.ServiceGroupMpackChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.collections.MapUtils;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class HostLevelParamsHolder extends AgentHostDataHolder<HostLevelParamsUpdateEvent> {

  @Inject
  private RecoveryConfigHelper recoveryConfigHelper;

  @Inject
  private Clusters clusters;

  @Inject
  private Provider<AmbariManagementController> m_ambariManagementController;

  @Inject
  public HostLevelParamsHolder(AmbariEventPublisher ambariEventPublisher) {
    ambariEventPublisher.register(this);
  }

  @Override
  public HostLevelParamsUpdateEvent getCurrentData(Long hostId) throws AmbariException {
    return getCurrentDataExcludeCluster(hostId, null);
  }

  public HostLevelParamsUpdateEvent getCurrentDataExcludeCluster(Long hostId, Long clusterId) throws AmbariException {
    TreeMap<String, HostLevelParamsCluster> hostLevelParamsClusters = new TreeMap<>();
    Host host = clusters.getHostById(hostId);
    for (Cluster cl : clusters.getClustersForHost(host.getHostName())) {
      if (clusterId != null && cl.getClusterId() == clusterId) {
        continue;
      }
      HostLevelParamsCluster hostLevelParamsCluster = new HostLevelParamsCluster(
          m_ambariManagementController.get().retrieveHostRepositories(cl, host),
          recoveryConfigHelper.getRecoveryConfig(cl.getClusterName(), host.getHostName()));

      hostLevelParamsClusters.put(Long.toString(cl.getClusterId()),
          hostLevelParamsCluster);
    }
    HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent = new HostLevelParamsUpdateEvent(hostLevelParamsClusters);
    hostLevelParamsUpdateEvent.setHostId(hostId);
    return hostLevelParamsUpdateEvent;
  }


  protected boolean handleUpdate(HostLevelParamsUpdateEvent update) {
    boolean changed = false;
    if (MapUtils.isNotEmpty(update.getHostLevelParamsClusters())) {
      Long hostId = update.getHostId();
      for (Map.Entry<String, HostLevelParamsCluster> hostLevelParamsClusterEntry : update.getHostLevelParamsClusters().entrySet()) {
        HostLevelParamsCluster updatedCluster = hostLevelParamsClusterEntry.getValue();
        String clusterId = hostLevelParamsClusterEntry.getKey();
        Map<String, HostLevelParamsCluster> clusters = getData().get(hostId).getHostLevelParamsClusters();
        if (clusters.containsKey(clusterId)) {
          HostLevelParamsCluster cluster = clusters.get(clusterId);
          if (!cluster.getRecoveryConfig().equals(updatedCluster.getRecoveryConfig())) {
            cluster.setRecoveryConfig(updatedCluster.getRecoveryConfig());
            changed = true;
          }
          if (!cluster.getHostRepositories().getRepositories()
              .equals(updatedCluster.getHostRepositories().getRepositories())) {
            cluster.getHostRepositories().setRepositories(updatedCluster.getHostRepositories().getRepositories());
            changed = true;
          }
          if (!cluster.getHostRepositories().getComponentRepos()
              .equals(updatedCluster.getHostRepositories().getComponentRepos())) {
            cluster.getHostRepositories().setComponentRepos(updatedCluster.getHostRepositories().getComponentRepos());
            changed = true;
          }
        } else {
          clusters.put(clusterId, updatedCluster);
          changed = true;
        }
      }
    }
    return changed;
  }

  @Override
  protected HostLevelParamsUpdateEvent getEmptyData() {
    return HostLevelParamsUpdateEvent.emptyUpdate();
  }

  @Subscribe
  public void onUpgradeDesiredMpackChange(ServiceGroupMpackChangedEvent clusterComponentsRepoChangedEvent) throws AmbariException {
    Long clusterId = clusterComponentsRepoChangedEvent.getClusterId();

    Cluster cluster = clusters.getCluster(clusterComponentsRepoChangedEvent.getClusterId());
    for (Host host : cluster.getHosts()) {
      updateDataOfHost(clusterComponentsRepoChangedEvent.getClusterId(), cluster, host);
    }
  }

  @Subscribe
  public void onServiceComponentRecoveryChanged(ServiceComponentRecoveryChangedEvent event) throws AmbariException {
    long clusterId = event.getClusterId();
    Cluster cluster = clusters.getCluster(clusterId);
    for (ServiceComponentHost host : cluster.getServiceComponentHosts(event.getServiceName(), event.getComponentName())) {
      updateDataOfHost(clusterId, cluster, host.getHost());
    }
  }

  private void updateDataOfHost(long clusterId, Cluster cluster, Host host) throws AmbariException {
    HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent = new HostLevelParamsUpdateEvent(Long.toString(clusterId),
            new HostLevelParamsCluster(
                    m_ambariManagementController.get().retrieveHostRepositories(cluster, host),
                    recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), host.getHostName())));
    hostLevelParamsUpdateEvent.setHostId(host.getHostId());
    updateData(hostLevelParamsUpdateEvent);
  }

  @Subscribe
  public void onMaintenanceModeChanged(MaintenanceModeEvent event) throws AmbariException {
    long clusterId = event.getClusterId();
    Cluster cluster = clusters.getCluster(clusterId);
    if (event.getHost() != null || event.getServiceComponentHost() != null) {
      Host host = event.getHost() != null ? event.getHost() : event.getServiceComponentHost().getHost();
      updateDataOfHost(clusterId, cluster, host);
    }
    else if (event.getService() != null) {
      for (String hostName : event.getService().getServiceHosts()) {
        updateDataOfHost(clusterId, cluster, cluster.getHost(hostName));
      }
    }
  }
}
