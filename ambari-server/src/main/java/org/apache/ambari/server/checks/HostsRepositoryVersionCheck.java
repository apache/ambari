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
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

/**
 * Checks that all hosts have particular repository version.
 */
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
    return request.getRepositoryVersion() != null;
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    final Map<String, Host> clusterHosts = clustersProvider.get().getHostsForCluster(clusterName);
    final StackId stackId = cluster.getDesiredStackVersion();
    for (Map.Entry<String, Host> hostEntry : clusterHosts.entrySet()) {
      final Host host = hostEntry.getValue();
      if (host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.OFF) {
        final RepositoryVersionEntity repositoryVersion = repositoryVersionDaoProvider.get().findByStackAndVersion(stackId.getStackId(), request.getRepositoryVersion());
        if (repositoryVersion == null) {
          prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
          prerequisiteCheck.setFailReason(getFailReason(KEY_NO_REPO_VERSION, prerequisiteCheck, request));
          prerequisiteCheck.getFailedOn().addAll(clusterHosts.keySet());
          return;
        }
        final HostVersionEntity hostVersion = hostVersionDaoProvider.get().findByClusterStackVersionAndHost(clusterName, repositoryVersion.getStack(), repositoryVersion.getVersion(), host.getHostName());
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
