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

import java.util.Collection;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.agent.stomp.dto.HostLevelParamsCluster;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.events.ClusterComponentsRepoChangedEvent;
import org.apache.ambari.server.events.HostLevelParamsUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;

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
    TreeMap<String, HostLevelParamsCluster> hostLevelParamsClusters = new TreeMap<>();
    Host host = clusters.getHostById(hostId);
    for (Cluster cl : clusters.getClustersForHost(host.getHostName())) {
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

  protected boolean handleUpdate(HostLevelParamsUpdateEvent update) throws AmbariException {
    //TODO implement update host level params process
    setData(update, update.getHostId());
    return true;
  }

  @Override
  protected HostLevelParamsUpdateEvent getEmptyData() {
    return HostLevelParamsUpdateEvent.emptyUpdate();
  }

  @Subscribe
  public void onClusterComponentsRepoUpdate(ClusterComponentsRepoChangedEvent clusterComponentsRepoChangedEvent) throws AmbariException {
    Long clusterId = clusterComponentsRepoChangedEvent.getClusterId();

    Cluster cluster = clusters.getCluster(clusterId);
    Collection<Host> hosts = clusters.getCluster(clusterId).getHosts();
    for (Host host : hosts) {
      HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent = new HostLevelParamsUpdateEvent(Long.toString(clusterId),
          new HostLevelParamsCluster(
            m_ambariManagementController.get().retrieveHostRepositories(cluster, host),
            recoveryConfigHelper.getRecoveryConfig(cluster.getClusterName(), host.getHostName())));
      hostLevelParamsUpdateEvent.setHostId(host.getHostId());
      updateData(hostLevelParamsUpdateEvent);
    }
  }
}
