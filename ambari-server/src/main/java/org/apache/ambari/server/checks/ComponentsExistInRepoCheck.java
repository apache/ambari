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
import java.util.LinkedHashSet;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.serveraction.upgrades.DeleteUnsupportedServicesAndComponents;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentSupport;
import org.apache.ambari.server.state.UpgradeHelper;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.ClusterGrouping;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.ServerActionTask;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link ComponentsExistInRepoCheck} is used to determine if any of the
 * components scheduled for upgrade do not exist in the target repository or
 * stack.
 */
@Singleton
@UpgradeCheck(
  group = UpgradeCheckGroup.INFORMATIONAL_WARNING,
  required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING })
public class ComponentsExistInRepoCheck extends AbstractCheckDescriptor {
  public static final String AUTO_REMOVE = "auto_remove";
  public static final String MANUAL_REMOVE = "manual_remove";
  @Inject
  ServiceComponentSupport serviceComponentSupport;
  @Inject
  UpgradeHelper upgradeHelper;

  public ComponentsExistInRepoCheck() {
    super(CheckDescription.COMPONENTS_EXIST_IN_TARGET_REPO);
  }

  @Override
  public void perform(PrerequisiteCheck check, PrereqCheckRequest request) throws AmbariException {
    Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    String stackName = request.getTargetRepositoryVersion().getStackName();
    String stackVersion = request.getTargetRepositoryVersion().getStackVersion();
    Collection<String> allUnsupported = serviceComponentSupport.allUnsupported(cluster, stackName, stackVersion);
    report(check, request, allUnsupported);
  }

  private void report(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request, Collection<String> allUnsupported) throws AmbariException {
    if (allUnsupported.isEmpty()) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.PASS);
      return;
    }
    prerequisiteCheck.setFailedOn(new LinkedHashSet<>(allUnsupported));
    if (hasDeleteUnsupportedServicesAction(upgradePack(request))) {
      prerequisiteCheck.setStatus(PrereqCheckStatus.WARNING);
      prerequisiteCheck.setFailReason(getFailReason(AUTO_REMOVE, prerequisiteCheck, request));
    } else {
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(getFailReason(MANUAL_REMOVE, prerequisiteCheck, request));
    }
  }

  private UpgradePack upgradePack(PrereqCheckRequest request) throws AmbariException {
    return upgradeHelper.suggestUpgradePack(
      request.getClusterName(),
      request.getSourceStackId(),
      request.getTargetRepositoryVersion().getStackId(),
      Direction.UPGRADE,
      request.getUpgradeType(),
      null);
  }

  private boolean hasDeleteUnsupportedServicesAction(UpgradePack upgradePack) {
    return upgradePack.getAllGroups().stream()
      .filter(ClusterGrouping.class::isInstance)
      .flatMap(group -> ((ClusterGrouping) group).executionStages.stream())
      .map(executeStage -> executeStage.task)
      .anyMatch(this::isDeleteUnsupportedTask);
  }

  private boolean isDeleteUnsupportedTask(Task task) {
    return task instanceof ServerActionTask
      && DeleteUnsupportedServicesAndComponents.class.getName().equals(((ServerActionTask)task).getImplementationClass());
  }
}
