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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.stack.upgrade.UpgradeCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeCheckStatus;
import org.apache.ambari.server.state.stack.upgrade.UpgradeCheckType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Manages pre-upgrade checks.
 */
@Singleton
public class UpgradeChecks {

  /**
   * Log.
   */
  private static Logger LOG = LoggerFactory.getLogger(UpgradeChecks.class);

  /**
   * List of all possible upgrade checks.
   */
  private static final List<UpgradeCheckDescriptor> UPGRADE_CHECK_REGISTRY = new ArrayList<UpgradeCheckDescriptor>();

  @Inject
  Provider<Clusters> clustersProvider;

  /**
   * Constructor. Fills upgrade check registry upon creation.
   */
  public UpgradeChecks() {
    UPGRADE_CHECK_REGISTRY.add(new UpgradeCheckDescriptor() {

      @Override
      public UpgradeCheck perform(String clusterName) {
        final UpgradeCheck upgradeCheck = new UpgradeCheck(
            "SERVICES_MAINTENANCE_MODE",
            "All services must not be in Maintenance Mode",
            UpgradeCheckType.SERVICE,
            clusterName);

        try {
          final Cluster cluster = clustersProvider.get().getCluster(clusterName);
          for (Map.Entry<String, Service> serviceEntry: cluster.getServices().entrySet()) {
            final Service service = serviceEntry.getValue();
            if (service.getMaintenanceState() == MaintenanceState.ON) {
              upgradeCheck.getFailedOn().add(service.getName());
            }
          }
          if (upgradeCheck.getFailedOn().isEmpty()) {
            upgradeCheck.setStatus(UpgradeCheckStatus.PASS);
          } else {
            upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
            upgradeCheck.setFailReason("Some services are in Maintenance Mode");
          }
        } catch (AmbariException ex) {
          LOG.error("Pre-upgrade check " + upgradeCheck.getId() + " failed", ex);
          upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
          upgradeCheck.setFailReason("Unexpected server error happened");
        }

        return upgradeCheck;
      }
    });

    UPGRADE_CHECK_REGISTRY.add(new UpgradeCheckDescriptor() {

      @Override
      public UpgradeCheck perform(String clusterName) {
        final UpgradeCheck upgradeCheck = new UpgradeCheck(
            "HOST_HEARTBEAT",
            "All hosts must be heartbeating with the server unless they are in Maintenance Mode",
            UpgradeCheckType.HOST,
            clusterName);

        try {
          final Map<String, Host> clusterHosts = clustersProvider.get().getHostsForCluster(clusterName);
          for (Map.Entry<String, Host> hostEntry: clusterHosts.entrySet()) {
            final Host host = hostEntry.getValue();
            if (host.getHealthStatus().getHealthStatus() == HealthStatus.UNKNOWN) {
              upgradeCheck.getFailedOn().add(host.getHostName());
            }
          }
          if (upgradeCheck.getFailedOn().isEmpty()) {
            upgradeCheck.setStatus(UpgradeCheckStatus.PASS);
          } else {
            upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
            upgradeCheck.setFailReason("Some hosts are not heartbeating with the server");
          }
        } catch (AmbariException ex) {
          LOG.error("Pre-upgrade check " + upgradeCheck.getId() + " failed", ex);
          upgradeCheck.setStatus(UpgradeCheckStatus.FAIL);
          upgradeCheck.setFailReason("Unexpected server error happened");
        }

        return upgradeCheck;
      }
    });
  }

  public List<UpgradeCheck> performAll(String clusterName) {
    final List<UpgradeCheck> upgradeCheckResults = new ArrayList<UpgradeCheck>();
    for (UpgradeCheckDescriptor upgradeCheck: UPGRADE_CHECK_REGISTRY) {
      if (upgradeCheck.isApplicable()) {
        upgradeCheckResults.add(upgradeCheck.perform(clusterName));
      }
    }
    return upgradeCheckResults;
  }

  /**
   * Describes upgrade check.
   */
  private abstract class UpgradeCheckDescriptor {

    /**
     * By default returns true.
     *
     * @return true if check should be performed
     */
    public boolean isApplicable() {
      return true;
    }

    /**
     * Executes check against given cluster.
     *
     * @param clusterName cluster name
     * @return the results of the check
     */
    public abstract UpgradeCheck perform(String clusterName);
  }

}
