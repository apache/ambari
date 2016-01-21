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

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Singleton;

/**
 * Checks if Install Packages needs to be re-run
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.DEFAULT, order = 3.0f, required = true)
public class InstallPackagesCheck extends AbstractCheckDescriptor {

  /**
   * Constructor.
   */
  public InstallPackagesCheck() {
    super(CheckDescription.INSTALL_PACKAGES_CHECK);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    final StackId targetStackId = request.getTargetStackId();
    final String stackName = targetStackId.getStackName();
    final String repoVersion = request.getRepositoryVersion();

    final RepositoryVersionEntity rve = repositoryVersionDaoProvider.get().findByStackNameAndVersion(stackName, request.getRepositoryVersion());
    if (StringUtils.isBlank(rve.getVersion()) || !rve.getVersion().matches("^\\d+(\\.\\d+)*\\-\\d+$")) {
      String message = MessageFormat.format("The Repository Version {0} for Stack {1} must contain a \"-\" followed by a build number. " +
          "Make sure that another registered repository does not have the same repo URL or " +
          "shares the same build number. Next, try reinstalling the Repository Version.", rve.getVersion(), rve.getStackVersion());
      prerequisiteCheck.getFailedOn().add("Repository Version " + rve.getVersion());
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(message);
      return;
    }

    final ClusterVersionEntity clusterVersion = clusterVersionDAOProvider.get().findByClusterAndStackAndVersion(
        clusterName, targetStackId, repoVersion);
    final Set<String> failedHosts = new HashSet<String>();

    for (Host host : cluster.getHosts()) {
      if (host.getMaintenanceState(cluster.getClusterId()) != MaintenanceState.ON) {
        for (HostVersionEntity hve : hostVersionDaoProvider.get().findByHost(host.getHostName())) {
          if (hve.getRepositoryVersion().getVersion().equals(request.getRepositoryVersion())
              && hve.getState() == RepositoryVersionState.INSTALL_FAILED) {
            failedHosts.add(host.getHostName());
          }
        }
      }
    }

    if (!failedHosts.isEmpty()) {
      String message = MessageFormat.format("Hosts in cluster [{0},{1},{2},{3}] are in INSTALL_FAILED state because " +
              "Install Packages had failed. Please re-run Install Packages, if necessary place following hosts " +
              "in Maintenance mode: {4}", cluster.getClusterName(), targetStackId.getStackName(),
          targetStackId.getStackVersion(), repoVersion, StringUtils.join(failedHosts, ", "));
      prerequisiteCheck.setFailedOn(new LinkedHashSet<String>(failedHosts));
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(message);
    } else if (clusterVersion.getState() == RepositoryVersionState.INSTALL_FAILED) {
      String message = MessageFormat.format("Cluster [{0},{1},{2},{3}] is in INSTALL_FAILED state because " +
              "Install Packages failed. Please re-run Install Packages even if you placed the failed hosts " +
              "in Maintenance mode.", cluster.getClusterName(), targetStackId.getStackName(),
          targetStackId.getStackVersion(), repoVersion);
      LinkedHashSet<String> failedOn = new LinkedHashSet<String>();
      failedOn.add(cluster.getClusterName());
      prerequisiteCheck.setFailedOn(failedOn);
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(message);
    }
  }
}
