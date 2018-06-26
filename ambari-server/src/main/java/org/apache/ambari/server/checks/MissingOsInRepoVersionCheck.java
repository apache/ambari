/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.checks;

import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.state.MaintenanceState.OFF;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

import com.google.inject.Singleton;

/**
 * This checks if the source and target version has an entry for each OS type in the cluster.
 */
@Singleton
@UpgradeCheck(
  group = UpgradeCheckGroup.REPOSITORY_VERSION,
  required = { UpgradeType.NON_ROLLING, UpgradeType.ROLLING })
public class MissingOsInRepoVersionCheck extends AbstractCheckDescriptor {
  public static final String SOURCE_OS = "source_os";
  public static final String TARGET_OS = "target_os";

  public MissingOsInRepoVersionCheck() {
    super(CheckDescription.MISSING_OS_IN_REPO_VERSION);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    Set<String> osFamiliesInCluster = osFamiliesInCluster(cluster(prerequisiteCheck));
    if (!targetOsFamilies(request).containsAll(osFamiliesInCluster)) {
      prerequisiteCheck.setFailReason(getFailReason(TARGET_OS, prerequisiteCheck, request));
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailedOn(new LinkedHashSet<>(osFamiliesInCluster));
    } else if (!sourceOsFamilies(request).containsAll(osFamiliesInCluster)) {
      prerequisiteCheck.setFailReason(getFailReason(SOURCE_OS, prerequisiteCheck, request));
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailedOn(new LinkedHashSet<>(osFamiliesInCluster));
    }
  }

  private Cluster cluster(PrerequisiteCheck prerequisiteCheck) throws AmbariException {
    return clustersProvider.get().getCluster(prerequisiteCheck.getClusterName());
  }

  /**
   * @return set of each os family in the cluster, excluding hosts which are in maintenance state
   */
  private Set<String> osFamiliesInCluster(Cluster cluster) {
    return cluster.getHosts().stream()
      .filter(host -> host.getMaintenanceState(cluster.getClusterId()) == OFF)
      .map(Host::getOsFamily)
      .collect(toSet());
  }

  /**
   * @return set of each os family in the source stack
   */
  private Set<String> sourceOsFamilies(PrereqCheckRequest request) throws AmbariException {
    return ambariMetaInfo.get().getStack(request.getSourceStackId()).getRepositoriesByOs().keySet();
  }

  /**
   * @return set of each os family in the target repository
   */
  private Set<String> targetOsFamilies(PrereqCheckRequest request) {
    return request
      .getTargetRepositoryVersion()
      .getRepoOsEntities()
      .stream()
      .map(RepoOsEntity::getFamily)
      .collect(toSet());
  }
}
