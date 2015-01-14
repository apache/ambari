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
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  @Override
  public CommandReport execute(
      ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    Set<RepositoryVersionState> allowedStates = new HashSet<RepositoryVersionState>();
    // TODO Rolling Upgrade, hack, should only allow UPGRADED.
    allowedStates.add(RepositoryVersionState.INSTALLED);
    allowedStates.add(RepositoryVersionState.UPGRADING);
    allowedStates.add(RepositoryVersionState.UPGRADED);

    StringBuffer outSB = new StringBuffer();
    StringBuffer errSB = new StringBuffer();

    try {
      String version = this.getExecutionCommand().getCommandParams().get("version");
      String clusterName = this.getExecutionCommand().getClusterName();
      outSB.append(MessageFormat.format("Begin finalizing the upgrade of cluster {0} to version {1}\n", clusterName, version));

      Cluster cluster = clusters.getCluster(clusterName);

      String stackId = cluster.getCurrentStackVersion().getStackId();
      ClusterVersionEntity upgradingClusterVersion = clusterVersionDAO.findByClusterAndStackAndVersion(clusterName,
          stackId, version);

      if (upgradingClusterVersion == null) {
        throw new AmbariException(String.format("Cluster stack version %s not found", version));
      }

      if (! allowedStates.contains(upgradingClusterVersion.getState())) {
        throw new AmbariException(String.format("The cluster stack version state %s is not allowed to transition directly into %s",
            upgradingClusterVersion.getState(), RepositoryVersionState.CURRENT.toString()));
      }

      // Validate that all of the hosts in the cluster have the version being upgraded to, and it is in an allowed state.
      Map<String, Host> hosts = clusters.getHostsForCluster(clusterName);
      List<HostVersionEntity> hostVersions = hostVersionDAO.findByClusterStackAndVersion(clusterName, stackId, version);

      Set<String> hostWithAllowedVersion = new HashSet<String>();
      for (HostVersionEntity hostVersion : hostVersions) {
        if (allowedStates.contains(hostVersion.getState())) {
          hostWithAllowedVersion.add(hostVersion.getHostName());
        }
      }

      Set<String> hostsWithoutCorrectVersionState = new HashSet<String>();
      for (String host : hosts.keySet()) {
        if (!hostWithAllowedVersion.contains(host)) {
          hostsWithoutCorrectVersionState.add(host);
        }
      }

      if (hostsWithoutCorrectVersionState.size() > 0) {
        throw new AmbariException(String.format("The following host(s) have not been upgraded to version %s. " +
                "Please install and upgrade the Stack Version on those hosts and try again.",
            version,
            StringUtils.join(hostsWithoutCorrectVersionState, ", ")));
      }

      outSB.append(String.format("Will finalize the version for %d host(s).\n", hosts.keySet().size()));
      cluster.mapHostVersions(hosts.keySet(), upgradingClusterVersion, RepositoryVersionState.CURRENT);

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
