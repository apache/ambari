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
package org.apache.ambari.server.serveraction.upgrades;

import com.google.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryVersionState;
import  org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostSummary;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Action that represents finalizing the Upgrade by completing any database changes.
 */
public class FinalizeUpgradeAction extends AbstractServerAction {


  /**
   * The Cluster that this ServerAction implementation is executing on
   */
  @Inject
  private Clusters clusters = null;

  @Inject
  private ClusterVersionDAO clusterVersionDAO;

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private HostComponentStateDAO hostComponentStateDAO;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Override
  public CommandReport execute(
      ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    Set<RepositoryVersionState> allowedStates = new HashSet<RepositoryVersionState>();
    allowedStates.add(RepositoryVersionState.UPGRADED);

    StringBuffer outSB = new StringBuffer();
    StringBuffer errSB = new StringBuffer();

    try {
      String version = this.getExecutionCommand().getCommandParams().get("version");
      String clusterName = this.getExecutionCommand().getClusterName();
      outSB.append(MessageFormat.format("Begin finalizing the upgrade of cluster {0} to version {1}\n", clusterName, version));

      Cluster cluster = clusters.getCluster(clusterName);

      StackId stack = cluster.getCurrentStackVersion();
      String stackId = stack.getStackId();
      ClusterVersionEntity upgradingClusterVersion = clusterVersionDAO.findByClusterAndStackAndVersion(clusterName,
          stackId, version);

      if (upgradingClusterVersion == null) {
        throw new AmbariException(String.format("Cluster stack version %s not found", version));
      }

      // Validate that all of the hosts with a version in the cluster have the version being upgraded to, and it is in an allowed state.
      List<HostVersionEntity> hostVersions = hostVersionDAO.findByClusterStackAndVersion(clusterName, stackId, version);

      // Will include hosts whose state is UPGRADED, and potentially INSTALLED
      Set<HostVersionEntity> hostsWithAllowedVersion = new HashSet<HostVersionEntity>();
      Set<HostVersionEntity> hostsWithoutCorrectVersionState = new HashSet<HostVersionEntity>();
      Set<String> hostsToUpdate = new HashSet<String>();
      // If true, then the cluster version is still in UPGRADING and allowed to transition to UPGRADED, and then CURRENT
      boolean atLeastOneHostInInstalledState = false;

      // It is important to only iterate over the hosts with a version, as opposed to all hosts, since some hosts
      // may only have components that do not advertise a version, such as AMS.
      for (HostVersionEntity hostVersion : hostVersions) {
        boolean isStateCorrect = false;

        if (allowedStates.contains(hostVersion.getState())) {
          isStateCorrect = true;
        } else {
          if (hostVersion.getState() == RepositoryVersionState.INSTALLED) {
            // It is possible that the host version has a state of INSTALLED and it never changed if the host only has
            // components that do not advertise a version.
            HostEntity host = hostVersion.getHostEntity();
            ServiceComponentHostSummary hostSummary = new ServiceComponentHostSummary(ambariMetaInfo, host, stack);
            if (hostSummary.haveAllComponentsFinishedAdvertisingVersion()){
              isStateCorrect = true;
              atLeastOneHostInInstalledState = true;
            }
          }
        }

        if (isStateCorrect) {
          hostsWithAllowedVersion.add(hostVersion);
          hostsToUpdate.add(hostVersion.getHostName());
        } else {
          hostsWithoutCorrectVersionState.add(hostVersion);
        }
      }

      if (hostsWithoutCorrectVersionState.size() > 0) {
        String message = String.format("The following %d host(s) have not been upgraded to version %s. " +
                "Please install and upgrade the Stack Version on those hosts and try again.\nHosts: %s\n",
            hostsWithoutCorrectVersionState.size(),
            version,
            StringUtils.join(hostsWithoutCorrectVersionState, ", "));
        outSB.append(message);
        throw new AmbariException(message);
      }

      // Allow the cluster version to transition from UPGRADING to CURRENT
      if (atLeastOneHostInInstalledState) {
        cluster.transitionClusterVersion(stackId, version, RepositoryVersionState.UPGRADED);
        upgradingClusterVersion = clusterVersionDAO.findByClusterAndStackAndVersion(clusterName,
            stackId, version);
      }

      if (!allowedStates.contains(upgradingClusterVersion.getState())) {
        throw new AmbariException(String.format("The cluster stack version state %s is not allowed to transition directly into %s",
            upgradingClusterVersion.getState(), RepositoryVersionState.CURRENT.toString()));
      }

      outSB.append(String.format("Will finalize the upgraded state of host components in %d host(s).\n", hostsWithAllowedVersion.size()));

      for (HostVersionEntity hostVersion : hostsWithAllowedVersion) {
        Collection<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findByHost(hostVersion.getHostName());
        for (HostComponentStateEntity hostComponentStateEntity: hostComponentStates) {
          hostComponentStateEntity.setUpgradeState(UpgradeState.NONE);
          hostComponentStateDAO.merge(hostComponentStateEntity);
        }
      }

      outSB.append(String.format("Will finalize the version for %d host(s).\n", hostsWithAllowedVersion.size()));

      // Impacts all hosts that have a version
      cluster.mapHostVersions(hostsToUpdate, upgradingClusterVersion, RepositoryVersionState.CURRENT);

      outSB.append(String.format("Will finalize the version for cluster %s.\n", clusterName));
      cluster.transitionClusterVersion(stackId, version, RepositoryVersionState.CURRENT);

      outSB.append("Upgrade was successful!\n");
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outSB.toString(), errSB.toString());
    } catch (Exception e) {
      errSB.append(e.getMessage());
      return createCommandReport(-1, HostRoleStatus.FAILED, "{}", outSB.toString(), errSB.toString());
    }
  }
}
