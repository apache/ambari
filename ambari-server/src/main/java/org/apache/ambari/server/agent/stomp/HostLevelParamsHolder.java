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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandRepository;
import org.apache.ambari.server.agent.RecoveryConfig;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.agent.stomp.dto.HostLevelParamsCluster;
import org.apache.ambari.server.agent.stomp.dto.HostRepositories;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.events.ClusterComponentsRepoChangedEvent;
import org.apache.ambari.server.events.HostLevelParamsUpdateEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceComponentRecoveryChangedEvent;
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
    HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent = new HostLevelParamsUpdateEvent(hostId, hostLevelParamsClusters);
    return hostLevelParamsUpdateEvent;
  }

  @Override
  protected HostLevelParamsUpdateEvent handleUpdate(HostLevelParamsUpdateEvent current, HostLevelParamsUpdateEvent update) {
    HostLevelParamsUpdateEvent result = null;
    boolean changed = false;
    Map<String, HostLevelParamsCluster> mergedClusters = new HashMap<>();
    if (MapUtils.isNotEmpty(update.getHostLevelParamsClusters())) {
      // put from current all clusters absent in update
      for (Map.Entry<String, HostLevelParamsCluster> hostLevelParamsClusterEntry : current.getHostLevelParamsClusters().entrySet()) {
        String clusterId = hostLevelParamsClusterEntry.getKey();
        if (!update.getHostLevelParamsClusters().containsKey(clusterId)) {
          mergedClusters.put(clusterId, hostLevelParamsClusterEntry.getValue());
        }
      }
      // process clusters from update
      for (Map.Entry<String, HostLevelParamsCluster> hostLevelParamsClusterEntry : update.getHostLevelParamsClusters().entrySet()) {
        String clusterId = hostLevelParamsClusterEntry.getKey();
        if (current.getHostLevelParamsClusters().containsKey(clusterId)) {
          boolean clusterChanged = false;
          HostLevelParamsCluster updatedCluster = hostLevelParamsClusterEntry.getValue();
          HostLevelParamsCluster currentCluster = current.getHostLevelParamsClusters().get(clusterId);
          RecoveryConfig mergedRecoveryConfig;
          SortedMap<Long, CommandRepository> mergedRepositories;
          SortedMap<String, Long> mergedComponentRepos;
          if (!currentCluster.getRecoveryConfig().equals(updatedCluster.getRecoveryConfig())) {
            mergedRecoveryConfig = updatedCluster.getRecoveryConfig();
            clusterChanged = true;
          } else {
            mergedRecoveryConfig = currentCluster.getRecoveryConfig();
          }
          if (!currentCluster.getHostRepositories().getRepositories()
              .equals(updatedCluster.getHostRepositories().getRepositories())) {
            mergedRepositories = updatedCluster.getHostRepositories().getRepositories();
            clusterChanged = true;
          } else {
            mergedRepositories = currentCluster.getHostRepositories().getRepositories();
          }
          if (!currentCluster.getHostRepositories().getComponentRepos()
              .equals(updatedCluster.getHostRepositories().getComponentRepos())) {
            mergedComponentRepos = updatedCluster.getHostRepositories().getComponentRepos();
            clusterChanged = true;
          } else {
            mergedComponentRepos = currentCluster.getHostRepositories().getComponentRepos();
          }
          if (clusterChanged) {
            HostLevelParamsCluster mergedCluster = new HostLevelParamsCluster(
                new HostRepositories(mergedRepositories, mergedComponentRepos),
                mergedRecoveryConfig);
            mergedClusters.put(clusterId, mergedCluster);
            changed = true;
          } else {
            mergedClusters.put(clusterId, hostLevelParamsClusterEntry.getValue());
          }
        } else {
          mergedClusters.put(clusterId, hostLevelParamsClusterEntry.getValue());
          changed = true;
        }
      }
    }
    if (changed) {
      result = new HostLevelParamsUpdateEvent(current.getHostId(), mergedClusters);
    }
    return result;
  }

  @Override
  protected HostLevelParamsUpdateEvent getEmptyData() {
    return HostLevelParamsUpdateEvent.emptyUpdate();
  }

  @Subscribe
  public void onClusterComponentsRepoUpdate(ClusterComponentsRepoChangedEvent clusterComponentsRepoChangedEvent) throws AmbariException {
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
    HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent = new HostLevelParamsUpdateEvent(host.getHostId(),
        Long.toString(clusterId),
            new HostLevelParamsCluster(
                    m_ambariManagementController.get().retrieveHostRepositories(cluster, host),
                    recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), host.getHostName())));
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
