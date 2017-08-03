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
package org.apache.ambari.server.agent.stomp;

import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.agent.stomp.dto.HostLevelParamsCluster;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.HostLevelParamsUpdateEvent;
import org.apache.ambari.server.events.publishers.StateUpdateEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HostLevelParamsHolder extends AgentHostDataHolder<HostLevelParamsUpdateEvent> {

  @Inject
  private RecoveryConfigHelper recoveryConfigHelper;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private Clusters clusters;

  private StateUpdateEventPublisher stateUpdateEventPublisher;

  @Inject
  public HostLevelParamsHolder(StateUpdateEventPublisher stateUpdateEventPublisher) {
    this.stateUpdateEventPublisher = stateUpdateEventPublisher;
    stateUpdateEventPublisher.register(this);
  }

  @Override
  public HostLevelParamsUpdateEvent getCurrentData(String hostName) throws AmbariException {
    TreeMap<String, HostLevelParamsCluster> hostLevelParamsClusters = new TreeMap<>();
    for (Cluster cl : clusters.getClustersForHost(hostName)) {
      Host host = clusters.getHost(hostName);
      HostLevelParamsCluster hostLevelParamsCluster = new HostLevelParamsCluster(
          ambariMetaInfo.getRepoInfo(cl, host),
          recoveryConfigHelper.getRecoveryConfig(cl.getClusterName(), hostName));

      hostLevelParamsClusters.put(Long.toString(cl.getClusterId()),
          hostLevelParamsCluster);
    }
    HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent = new HostLevelParamsUpdateEvent(hostLevelParamsClusters);
    return hostLevelParamsUpdateEvent;
  }

  public void updateData(HostLevelParamsUpdateEvent update) throws AmbariException {
    //TODO implement update host level params process
    setData(update, update.getHostName());
    regenerateHash(update.getHostName());
    update.setHash(getData(update.getHostName()).getHash());
    stateUpdateEventPublisher.publish(update);
  }

  @Override
  protected HostLevelParamsUpdateEvent getEmptyData() {
    return HostLevelParamsUpdateEvent.emptyUpdate();
  }
}
