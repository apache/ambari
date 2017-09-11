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
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

import com.google.inject.Singleton;

/**
 * Checks that all hosts in maintenance state do not have master components.
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.MAINTENANCE_MODE,
    order = 5.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class HostsMasterMaintenanceCheck extends AbstractCheckDescriptor {

  static final String KEY_NO_UPGRADE_NAME = "no_upgrade_name";
  static final String KEY_NO_UPGRADE_PACK = "no_upgrade_pack";

  /**
   * Constructor.
   */
  public HostsMasterMaintenanceCheck() {
    super(CheckDescription.HOSTS_MASTER_MAINTENANCE);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    final StackId stackId = request.getSourceStackId();
    final Set<String> hostsWithMasterComponent = new HashSet<>();

    // TODO AMBARI-12698, need to pass the upgrade pack to use in the request, or at least the type.
    RepositoryVersionEntity repositoryVersion = request.getTargetRepositoryVersion();

    final String upgradePackName = repositoryVersionHelper.get().getUpgradePackageName(
        stackId.getStackName(), stackId.getStackVersion(), repositoryVersion.getVersion(), null);

    if (upgradePackName == null) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      String fail = getFailReason(KEY_NO_UPGRADE_NAME, prerequisiteCheck, request);
      prerequisiteCheck.setFailReason(String.format(fail, stackId.getStackName(), stackId.getStackVersion()));
      return;
    }

    final UpgradePack upgradePack = ambariMetaInfo.get().getUpgradePacks(stackId.getStackName(), stackId.getStackVersion()).get(upgradePackName);
    if (upgradePack == null) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      String fail = getFailReason(KEY_NO_UPGRADE_PACK, prerequisiteCheck, request);
      prerequisiteCheck.setFailReason(String.format(fail, upgradePackName));
      return;
    }

    final Set<String> componentsFromUpgradePack = new HashSet<>();
    for (Map<String, ProcessingComponent> task: upgradePack.getTasks().values()) {
      componentsFromUpgradePack.addAll(task.keySet());
    }

    for (Service service: cluster.getServices().values()) {
      for (ServiceComponent serviceComponent: service.getServiceComponents().values()) {
        if (serviceComponent.isMasterComponent() && componentsFromUpgradePack.contains(serviceComponent.getName())) {
          hostsWithMasterComponent.addAll(serviceComponent.getServiceComponentHosts().keySet());
        }
      }
    }

    final Map<String, Host> clusterHosts = clustersProvider.get().getHostsForCluster(clusterName);
    for (Map.Entry<String, Host> hostEntry : clusterHosts.entrySet()) {
      final Host host = hostEntry.getValue();
      if (host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.ON && hostsWithMasterComponent.contains(host.getHostName())) {
        prerequisiteCheck.getFailedOn().add(host.getHostName());
      }
    }

    if (!prerequisiteCheck.getFailedOn().isEmpty()) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
    }
  }
}
