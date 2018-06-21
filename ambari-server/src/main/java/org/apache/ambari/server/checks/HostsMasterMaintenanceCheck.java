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
package org.apache.ambari.server.checks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.UpgradePlanDetailEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ModuleComponent;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.Mpack.ModuleComponentVersionChange;
import org.apache.ambari.server.state.Mpack.MpackChangeSummary;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang3.StringUtils;

import com.google.inject.Singleton;

/**
 * Checks that all hosts in maintenance state do not have master components.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.MAINTENANCE_MODE,
    order = 5.0f,
    required = { UpgradeType.ROLLING, UpgradeType.EXPRESS, UpgradeType.HOST_ORDERED })
public class HostsMasterMaintenanceCheck extends ClusterCheck {

  /**
   * Constructor.
   */
  public HostsMasterMaintenanceCheck() {
    super(CheckDescription.HOSTS_MASTER_MAINTENANCE);
  }

  @Override
  public UpgradeCheckResult perform(PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    final Set<String> hostsWithMasterComponent = new HashSet<>();

    UpgradeCheckResult result = new UpgradeCheckResult(this);

    for (UpgradePlanDetailEntity upgradeDetail : request.getUpgradePlan().getDetails()) {
      ServiceGroup serviceGroup = cluster.getServiceGroup(upgradeDetail.getServiceGroupId());
      Mpack sourceMpack = ambariMetaInfo.get().getMpack(serviceGroup.getMpackId());
      Mpack targetMpack = ambariMetaInfo.get().getMpack(upgradeDetail.getMpackTargetId());

      MpackChangeSummary changeSummary = sourceMpack.getChangeSummary(targetMpack);
      for( ModuleComponentVersionChange componentChanges : changeSummary.getComponentVersionChanges()) {
        ModuleComponent moduleComponent = componentChanges.getSource();

        for (Service service: serviceGroup.getServices()) {
          for (ServiceComponent serviceComponent: service.getServiceComponents().values()) {
            if (serviceComponent.isMasterComponent() && StringUtils.equals(moduleComponent.getName(), serviceComponent.getName())) {
              hostsWithMasterComponent.addAll(serviceComponent.getServiceComponentHosts().keySet());
            }
          }
        }
      }
    }

    final Map<String, Host> clusterHosts = clustersProvider.get().getHostsForCluster(clusterName);
    for (Map.Entry<String, Host> hostEntry : clusterHosts.entrySet()) {
      final Host host = hostEntry.getValue();
      if (host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.ON && hostsWithMasterComponent.contains(host.getHostName())) {
        result.getFailedOn().add(host.getHostName());

        result.getFailedDetail().add(
            new HostDetail(host.getHostId(), host.getHostName()));
      }
    }

    if (!result.getFailedOn().isEmpty()) {
      result.setStatus(PrereqCheckStatus.FAIL);
      result.setFailReason(getFailReason(result, request));
    }

    return result;
  }
}
