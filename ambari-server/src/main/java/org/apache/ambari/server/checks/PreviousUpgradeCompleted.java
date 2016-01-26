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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.commons.lang.StringUtils;

import javax.inject.Provider;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks if Install Packages needs to be re-run
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.DEFAULT, order = 4.0f, required = true)
public class PreviousUpgradeCompleted extends AbstractCheckDescriptor {

  /**
   * If this ever changes, we will need to keep the historic name.
   */
  public static final String FINALIZE_ACTION_CLASS_NAME = "org.apache.ambari.server.serveraction.upgrades.FinalizeUpgradeAction";
  public static final String SET_CURRENT_COMMAND = "ambari-server set-current --cluster-name=$CLUSTERNAME --version-display-name=$VERSION_NAME";

  @Inject
  Provider<RequestDAO> requestDaoProvider;

  @Inject
  Provider<HostRoleCommandDAO> hostRoleCommandDaoProvider;

  /**
   * Constructor.
   */
  public PreviousUpgradeCompleted() {
    super(CheckDescription.PREVIOUS_UPGRADE_COMPLETED);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);

    String errorMessage = null;

    List<UpgradeEntity> upgrades= upgradeDaoProvider.get().findAll();
    if (upgrades != null) {
      Long lastStartTime = 0L;
      UpgradeEntity mostRecentUpgrade = null;
      UpgradeEntity correspondingDowngrade = null;
      for (UpgradeEntity upgrade : upgrades) {
        // Find the most recent upgrade for this cluster
        if (upgrade.getClusterId() == cluster.getClusterId() && upgrade.getDirection() == Direction.UPGRADE) {
          Long requestId = upgrade.getRequestId();
          RequestEntity upgradeRequest = requestDaoProvider.get().findByPK(requestId);
          if (upgradeRequest != null && upgradeRequest.getStartTime() > lastStartTime) {
            mostRecentUpgrade = upgrade;
            lastStartTime = upgradeRequest.getStartTime();
          }
        }
      }

      // Check for the corresponding downgrade.
      if (mostRecentUpgrade != null) {
        for (UpgradeEntity downgrade : upgrades) {
          // Surprisingly, a Downgrade's from and to version are identical.
          if (downgrade.getClusterId() == cluster.getClusterId() && downgrade.getDirection() == Direction.DOWNGRADE &&
              downgrade.getFromVersion().equals(mostRecentUpgrade.getFromVersion())) {
            correspondingDowngrade = downgrade;
            break;
          }
        }

        // If it has no downgrade, then the "Save Cluster State" step should have COMPLETED.
        if (correspondingDowngrade == null) {
          // Should have only 1 element.
          List<HostRoleCommandEntity> finalizeCommandList = hostRoleCommandDaoProvider.get().
              findSortedCommandsByRequestIdAndCustomCommandName(mostRecentUpgrade.getRequestId(), FINALIZE_ACTION_CLASS_NAME);
  
          // If the action is not COMPLETED, then something went wrong.
          if (finalizeCommandList != null) {
            for (HostRoleCommandEntity command : finalizeCommandList) {
              if (command.getStatus() != HostRoleStatus.COMPLETED) {
                errorMessage = MessageFormat.format("Upgrade attempt (id: {0}, request id: {1}, from version: {2}, " +
                    "to version: {3}) did not complete task with id {4} since its state is {5} instead of COMPLETED.",
                    mostRecentUpgrade.getId(), mostRecentUpgrade.getRequestId(), mostRecentUpgrade.getFromVersion(),
                    mostRecentUpgrade.getToVersion(), command.getTaskId(), command.getStatus());
                errorMessage += " Please ensure that you called:\n" + SET_CURRENT_COMMAND;
                errorMessage += MessageFormat.format("\nFurther, change the status of host_role_command with " +
                    "id {0} to COMPLETED", mostRecentUpgrade.getId());
                break;
              }
            }
          }
        }
      }
    }

    if (null != errorMessage) {
      LinkedHashSet<String> failedOn = new LinkedHashSet<String>();
      failedOn.add(cluster.getClusterName());
      prerequisiteCheck.setFailedOn(failedOn);
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(errorMessage);
    }
  }
}
