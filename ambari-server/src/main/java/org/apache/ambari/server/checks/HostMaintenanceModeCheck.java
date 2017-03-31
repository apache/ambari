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

import java.util.Collection;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

import com.google.inject.Singleton;

/**
 * The {@link HostMaintenanceModeCheck} is used to provide a warning if any
 * hosts are in maintenance mode. Hosts in MM will be exluded from the upgrade.
 * <p/>
 * This check will return {@link PrereqCheckStatus#WARNING} for any hosts in
 * maintenance mode.
 *
 * @see HostsHeartbeatCheck
 */
@Singleton
@UpgradeCheck(
    group = UpgradeCheckGroup.MAINTENANCE_MODE,
    order = 7.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class HostMaintenanceModeCheck extends AbstractCheckDescriptor {

  public static final String KEY_CANNOT_START_HOST_ORDERED = "cannot_upgrade_mm_hosts";

  /**
   * Constructor.
   */
  public HostMaintenanceModeCheck() {
    super(CheckDescription.HOSTS_MAINTENANCE_MODE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
      throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    Collection<Host> hosts = cluster.getHosts();

    // see if any hosts in the cluster are in MM
    for (Host host : hosts) {
      MaintenanceState maintenanceState = host.getMaintenanceState(cluster.getClusterId());
      if (maintenanceState != MaintenanceState.OFF) {
        prerequisiteCheck.getFailedOn().add(host.getHostName());
      }
    }

    // for any host in MM, produce a warning
    if (!prerequisiteCheck.getFailedOn().isEmpty()) {
      PrereqCheckStatus status = request.getUpgradeType() == UpgradeType.HOST_ORDERED ?
          PrereqCheckStatus.FAIL : PrereqCheckStatus.WARNING;
      prerequisiteCheck.setStatus(status);

      String failReason = request.getUpgradeType() == UpgradeType.HOST_ORDERED ?
          getFailReason(KEY_CANNOT_START_HOST_ORDERED, prerequisiteCheck, request) :
          getFailReason(prerequisiteCheck, request);

      prerequisiteCheck.setFailReason(failReason);
    }
  }
}
