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
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.VERSION;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.Direction;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action that represents updating the Desired Stack Id during the middle of a stack upgrade (typically NonRolling).
 * In a {@link org.apache.ambari.server.state.stack.upgrade.UpgradeType#NON_ROLLING}, the effective Stack Id is
 * actually changed half-way through calculating the Actions, and this serves to update the database to make it
 * evident to the user at which point it changed.
 */
public class UpdateDesiredStackAction extends AbstractServerAction {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpdateDesiredStackAction.class);

  public static final String COMMAND_PARAM_VERSION = VERSION;
  public static final String COMMAND_DOWNGRADE_FROM_VERSION = "downgrade_from_version";
  public static final String COMMAND_PARAM_DIRECTION = "upgrade_direction";
  public static final String COMMAND_PARAM_UPGRADE_PACK = "upgrade_pack";

  /**
   * The original "current" stack of the cluster before the upgrade started.
   * This is the same regardless of whether the current direction is
   * {@link Direction#UPGRADE} or {@link Direction#DOWNGRADE}.
   */
  public static final String COMMAND_PARAM_ORIGINAL_STACK = "original_stack";

  /**
   * The target upgrade stack before the upgrade started. This is the same
   * regardless of whether the current direction is {@link Direction#UPGRADE} or
   * {@link Direction#DOWNGRADE}.
   */
  public static final String COMMAND_PARAM_TARGET_STACK = "target_stack";

  /**
   * The Cluster that this ServerAction implementation is executing on.
   */
  @Inject
  private Clusters clusters;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  /**
   * The Ambari configuration.
   */
  @Inject
  private Configuration m_configuration;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {
    Map<String, String> commandParams = getExecutionCommand().getCommandParams();

    StackId originalStackId = new StackId(commandParams.get(COMMAND_PARAM_ORIGINAL_STACK));
    StackId targetStackId = new StackId(commandParams.get(COMMAND_PARAM_TARGET_STACK));
    Direction direction = Direction.UPGRADE;
    if(commandParams.containsKey(COMMAND_PARAM_DIRECTION)
        && "downgrade".equals(commandParams.get(COMMAND_PARAM_DIRECTION).toLowerCase())) {
      direction = Direction.DOWNGRADE;
    }
    String version = commandParams.get(COMMAND_PARAM_VERSION);
    String upgradePackName = commandParams.get(COMMAND_PARAM_UPGRADE_PACK);
    String clusterName = getExecutionCommand().getClusterName();
    UpgradePack upgradePack = ambariMetaInfo.getUpgradePacks(originalStackId.getStackName(), originalStackId.getStackVersion()).get(upgradePackName);

    Map<String, String> roleParams = getExecutionCommand().getRoleParams();

    // Make a best attempt at setting the username
    String userName;
    if (roleParams != null && roleParams.containsKey(ServerAction.ACTION_USER_NAME)) {
      userName = roleParams.get(ServerAction.ACTION_USER_NAME);
    } else {
      userName = m_configuration.getAnonymousAuditName();
      LOG.warn(String.format("Did not receive role parameter %s, will save configs using anonymous username %s", ServerAction.ACTION_USER_NAME, userName));
    }

    return updateDesiredStack(clusterName, originalStackId, targetStackId, version, direction, upgradePack, userName);
  }

  /**
   * Set the cluster's Desired Stack Id during an upgrade.
   *
   * @param clusterName the name of the cluster the action is meant for
   * @param originalStackId the stack Id of the cluster before the upgrade.
   * @param targetStackId the stack Id that was desired for this upgrade.
   * @param direction direction, either upgrade or downgrade
   * @param upgradePack Upgrade Pack to use
   * @param userName username performing the action
   * @return the command report to return
   */
  private CommandReport updateDesiredStack(
      String clusterName, StackId originalStackId, StackId targetStackId,
      String version, Direction direction, UpgradePack upgradePack, String userName)
      throws AmbariException, InterruptedException {
    StringBuilder out = new StringBuilder();
    StringBuilder err = new StringBuilder();

    try {
      Cluster cluster = clusters.getCluster(clusterName);
      StackId currentClusterStackId = cluster.getCurrentStackVersion();
      out.append(String.format("Params: %s %s %s %s %s %s\n",
          clusterName, originalStackId.getStackId(), targetStackId.getStackId(), version, direction.getText(false), upgradePack.getName()));

      out.append(String.format("Checking if can update the Desired Stack Id to %s. The cluster's current Stack Id is %s\n", targetStackId.getStackId(), currentClusterStackId.getStackId()));

      // Ensure that the target stack id exist
      StackInfo desiredClusterStackInfo = ambariMetaInfo.getStack(targetStackId.getStackName(), targetStackId.getStackVersion());
      if (null == desiredClusterStackInfo) {
        String message = String.format("Parameter %s has an invalid value: %s. That Stack Id does not exist.\n",
            COMMAND_PARAM_TARGET_STACK, targetStackId.getStackId());
        err.append(message);
        out.append(message);
        return createCommandReport(-1, HostRoleStatus.FAILED, "{}", out.toString(), err.toString());
      }

      // Ensure that the current Stack Id coincides with the parameter that the user passed in.
      if (!currentClusterStackId.equals(originalStackId)) {
        String message = String.format("Parameter %s has invalid value: %s. " +
            "The cluster is currently on stack %s, " + currentClusterStackId.getStackId() +
            ", yet the parameter to this function indicates a different value.\n", COMMAND_PARAM_ORIGINAL_STACK, originalStackId.getStackId(), currentClusterStackId.getStackId());
        err.append(message);
        out.append(message);
        return createCommandReport(-1, HostRoleStatus.FAILED, "{}", out.toString(), err.toString());
      }

      // Check for a no-op
      if (currentClusterStackId.equals(targetStackId)) {
        String message = String.format("Success! The cluster's Desired Stack Id was already set to %s\n", targetStackId.getStackId());
        out.append(message);
        return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", out.toString(), err.toString());
      }

      // Create Create new configurations that are a merge between the current stack and the desired stack
      // Also updates the desired stack version.
      UpgradeResourceProvider upgradeResourceProvider = new UpgradeResourceProvider(AmbariServer.getController());
      upgradeResourceProvider.applyStackAndProcessConfigurations(targetStackId.getStackName(), cluster, version, direction, upgradePack, userName);
      String message = String.format("Success! Set cluster's %s Desired Stack Id to %s.\n", clusterName, targetStackId.getStackId());
      out.append(message);

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", out.toString(), err.toString());
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      err.append(sw.toString());

      return createCommandReport(-1, HostRoleStatus.FAILED, "{}", out.toString(), err.toString());
    }
  }
}
