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
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Action that represents updating the Desired Stack Id during the middle of a stack upgrade (typically NonRolling).
 * In a {@link org.apache.ambari.server.state.stack.upgrade.UpgradeType#NON_ROLLING}, the effective Stack Id is
 * actually changed half-way through calculating the Actions, and this serves to update the database to make it
 * evident to the user at which point it changed.
 */
public class UpdateDesiredStackAction extends AbstractServerAction {

  /**
   * The original "current" stack of the cluster before the upgrade started.
   * This is the same regardless of whether the current direction is
   * {@link org.apache.ambari.server.state.stack.upgrade.Direction#UPGRADE} or {@link org.apache.ambari.server.state.stack.upgrade.Direction#DOWNGRADE}.
   */
  public static final String ORIGINAL_STACK_KEY = "original_stack";

  /**
   * The target upgrade stack before the upgrade started. This is the same
   * regardless of whether the current direction is {@link org.apache.ambari.server.state.stack.upgrade.Direction#UPGRADE} or
   * {@link org.apache.ambari.server.state.stack.upgrade.Direction#DOWNGRADE}.
   */
  public static final String TARGET_STACK_KEY = "target_stack";

  /**
   * The Cluster that this ServerAction implementation is executing on.
   */
  @Inject
  private Clusters clusters;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {
    Map<String, String> commandParams = getExecutionCommand().getCommandParams();

    StackId originalStackId = new StackId(commandParams.get(ORIGINAL_STACK_KEY));
    StackId targetStackId = new StackId(commandParams.get(TARGET_STACK_KEY));
    String clusterName = getExecutionCommand().getClusterName();

    return updateDesiredStack(clusterName, originalStackId, targetStackId);
  }

  /**
   * Set the cluster's Desired Stack Id during an upgrade.
   *
   * @param clusterName the name of the cluster the action is meant for
   * @paran originalStackId the stack Id of the cluster before the upgrade.
   * @paran targetStackId the stack Id that was desired for this upgrade.
   * @return the command report to return
   */
  private CommandReport updateDesiredStack(String clusterName, StackId originalStackId, StackId targetStackId)
      throws AmbariException, InterruptedException {
    StringBuilder out = new StringBuilder();
    StringBuilder err = new StringBuilder();

    try {
      Cluster cluster = clusters.getCluster(clusterName);
      StackId currentClusterStackId = cluster.getCurrentStackVersion();

      out.append(String.format("Checking if can update the Desired Stack Id to %s. The cluster's current Stack Id is %s\n", targetStackId.getStackId(), currentClusterStackId.getStackId()));

      // Ensure that the target stack id exist
      StackInfo desiredClusterStackInfo = ambariMetaInfo.getStack(targetStackId.getStackName(), targetStackId.getStackVersion());
      if (null == desiredClusterStackInfo) {
        String message = String.format("Parameter %s has an invalid value: %s. That Stack Id does not exist.\n",
            TARGET_STACK_KEY, targetStackId.getStackId());
        err.append(message);
        out.append(message);
        return createCommandReport(-1, HostRoleStatus.FAILED, "{}", out.toString(), err.toString());
      }

      // Ensure that the current Stack Id coincides with the parameter that the user passed in.
      if (!currentClusterStackId.equals(originalStackId)) {
        String message = String.format("Parameter %s has invalid value: %s. " +
            "The cluster is currently on stack %s, " + currentClusterStackId.getStackId() +
            ", yet the parameter to this function indicates a different value.\n", ORIGINAL_STACK_KEY, targetStackId.getStackId(), currentClusterStackId.getStackId());
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

      cluster.setDesiredStackVersion(targetStackId, true);
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
