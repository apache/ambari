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

import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

import com.google.inject.Singleton;

/**
 * Checks that all hosts have particular repository version. Hosts that are in
 * maintenance mode will be skipped and will not report a warning. Even if they
 * do not have the repo version, they will not be included in the upgrade
 * orchstration, so no warning is required.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.REPOSITORY_VERSION, required = true)
public class HostsRepositoryVersionCheck extends AbstractCheckDescriptor {

  static final String KEY_NO_REPO_VERSION = "no_repo_version";

  /**
   * Constructor.
   */
  public HostsRepositoryVersionCheck() {
    super(CheckDescription.HOSTS_REPOSITORY_VERSION);
  }

  @Override
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    return super.isApplicable(request) && request.getRepositoryVersion() != null;
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
      throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    final Map<String, Host> clusterHosts = clustersProvider.get().getHostsForCluster(clusterName);
    final StackId stackId = cluster.getDesiredStackVersion();

    for (Host host : clusterHosts.values()) {
      // hosts in MM will produce a warning if they do not have the repo version
      MaintenanceState maintenanceState = host.getMaintenanceState(cluster.getClusterId());
      if (maintenanceState != MaintenanceState.OFF) {
        continue;
      }

      if (null != request.getRepositoryVersion()) {
        boolean found = false;
        for (HostVersionEntity hve : hostVersionDaoProvider.get().findByHost(host.getHostName())) {

          if (hve.getRepositoryVersion().getVersion().equals(request.getRepositoryVersion())
              && hve.getState() == RepositoryVersionState.INSTALLED) {
            found = true;
            break;
          }
        }

        if (!found) {
          prerequisiteCheck.getFailedOn().add(host.getHostName());
        }
      } else {
        final RepositoryVersionEntity repositoryVersion = repositoryVersionDaoProvider.get().findByStackAndVersion(
            stackId, request.getRepositoryVersion());
        if (repositoryVersion == null) {
          prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
          prerequisiteCheck.setFailReason(
              getFailReason(KEY_NO_REPO_VERSION, prerequisiteCheck, request));
          prerequisiteCheck.getFailedOn().addAll(clusterHosts.keySet());
          return;
        }

        StackEntity repositoryStackEntity = repositoryVersion.getStack();
        StackId repositoryStackId = new StackId(repositoryStackEntity.getStackName(),
            repositoryStackEntity.getStackVersion());

        final HostVersionEntity hostVersion = hostVersionDaoProvider.get().findByClusterStackVersionAndHost(
            clusterName, repositoryStackId, repositoryVersion.getVersion(), host.getHostName());

        if (hostVersion == null || hostVersion.getState() != RepositoryVersionState.INSTALLED) {
          prerequisiteCheck.getFailedOn().add(host.getHostName());
        }
      }
    }

    if (!prerequisiteCheck.getFailedOn().isEmpty()) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(getFailReason(prerequisiteCheck, request));
    }
  }
}
