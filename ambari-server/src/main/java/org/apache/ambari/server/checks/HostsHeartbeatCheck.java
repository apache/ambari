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
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

import com.google.inject.Singleton;

/**
 * Checks that all hosts are heartbeating with the Ambari Server. If there is a
 * host which is not heartbeating, then it must be in maintenance mode to
 * prevent a failure of this check.
 * <p/>
 * Hosts that are in maintenance mode will be added to a warning that they will
 * not be included in the upgrade.
 * <p/>
 * This check will return {@link PrereqCheckStatus#FAIL} if there are hosts not
 * heartbeating and not in maintenance mode. Otherwise, it will return
 * {@link PrereqCheckStatus#WARNING} for any hosts in maintenance mode.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.LIVELINESS, order = 1.0f, required = true)
public class HostsHeartbeatCheck extends AbstractCheckDescriptor {

  static final String KEY_HOSTS_IN_MM_WARNING = "key.hosts.in.mm.warning";

  /**
   * Constructor.
   */
  public HostsHeartbeatCheck() {
    super(CheckDescription.HOSTS_HEARTBEAT);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
      throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    Collection<Host> hosts = cluster.getHosts();

    for (Host host : hosts) {
      HealthStatus hostHealth = host.getHealthStatus().getHealthStatus();
      MaintenanceState maintenanceState = host.getMaintenanceState(cluster.getClusterId());
      switch (hostHealth) {
        case UNHEALTHY:
        case UNKNOWN:
          if (maintenanceState == MaintenanceState.OFF) {
            prerequisiteCheck.getFailedOn().add(host.getHostName());
          }
          break;
        default:
          break;

      }
    }

    // for any hosts unhealthy and NOT in MM mode, fail this check
    if (!prerequisiteCheck.getFailedOn().isEmpty()) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
      return;
    }

    // no failues so far, check to see if any hosts are in MM so that this check
    // will produce a warning
    for (Host host : hosts) {
      MaintenanceState maintenanceState = host.getMaintenanceState(cluster.getClusterId());
      if (maintenanceState != MaintenanceState.OFF) {
        prerequisiteCheck.getFailedOn().add(host.getHostName());
      }
    }

    if (!prerequisiteCheck.getFailedOn().isEmpty()) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
      prerequisiteCheck.setFailReason(
          getFailReason(KEY_HOSTS_IN_MM_WARNING, prerequisiteCheck, request));
    }
  }
}
