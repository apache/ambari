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
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.UpgradeContextFactory;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

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
  public static final String COMMAND_PARAM_UPGRADE_PACK = "upgrade_pack";

  /**
   * The Cluster that this ServerAction implementation is executing on.
   */
  @Inject
  private Clusters clusters;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  /**
   * Used for building {@link UpgradeContext} instances.
   */
  @Inject
  UpgradeContextFactory m_upgradeContextFactory;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {
    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    UpgradeContext context = m_upgradeContextFactory.create(cluster,
        cluster.getUpgradeInProgress());


    // invalidate any cached effective ID
    cluster.invalidateUpgradeEffectiveVersion();

    return updateDesiredStack(cluster, context);
  }

  /**
   * Set the cluster's Desired Stack Id during an upgrade.
   *
   * @param context
   *          the upgrade context (not {@code null}).
   * @return the command report to return
   */
  private CommandReport updateDesiredStack(Cluster cluster, UpgradeContext context)
      throws AmbariException, InterruptedException {

    Direction direction = context.getDirection();
    RepositoryVersionEntity fromRepositoryVersion = context.getSourceRepositoryVersion();
    RepositoryVersionEntity toRepositoryVersion = context.getTargetRepositoryVersion();

    String clusterName = cluster.getClusterName();
    StringBuilder out = new StringBuilder();
    StringBuilder err = new StringBuilder();

    try {
      StackId currentClusterStackId = cluster.getCurrentStackVersion();
      out.append(String.format("%s %s from %s-%s to %s-%s\n",
          direction.getVerb(true), clusterName,
          fromRepositoryVersion.getStackId().getStackName(),
          fromRepositoryVersion.getVersion(),
          toRepositoryVersion.getStackId().getStackName(),
          toRepositoryVersion.getVersion()));

      out.append(String.format(
          "Checking if can update the desired stack to %s. The cluster's current stack is %s\n",
          toRepositoryVersion.getStackId(), currentClusterStackId.getStackId()));

      // Ensure that the target stack id exist
      StackId targetStackId = toRepositoryVersion.getStackId();
      StackInfo desiredClusterStackInfo = ambariMetaInfo.getStack(targetStackId.getStackName(), targetStackId.getStackVersion());
      if (null == desiredClusterStackInfo) {
        String message = String.format("Invalid target stack of \n", targetStackId.getStackId());
        err.append(message);
        out.append(message);
        return createCommandReport(-1, HostRoleStatus.FAILED, "{}", out.toString(), err.toString());
      }

      // Ensure that the current Stack Id coincides with the parameter that the user passed in.
      StackId originalStackId = context.getOriginalStackId();
      if (!currentClusterStackId.equals(originalStackId)) {
        String message = String.format(
            "The current cluster stack of %s doesn't match the original upgrade stack of %s",
            currentClusterStackId, originalStackId);

        err.append(message);
        out.append(message);
        return createCommandReport(-1, HostRoleStatus.FAILED, "{}", out.toString(), err.toString());
      }

      // Check for a no-op
      if (currentClusterStackId.equals(targetStackId)) {
        String message = String.format("Success! The cluster's desired stack was already set to %s\n", targetStackId.getStackId());
        out.append(message);
        return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", out.toString(), err.toString());
      }

      // Create Create new configurations that are a merge between the current stack and the desired stack
      // Also updates the desired stack version.
      UpgradeResourceProvider upgradeResourceProvider = new UpgradeResourceProvider(AmbariServer.getController());
      upgradeResourceProvider.applyStackAndProcessConfigurations(context);
      String message = String.format("Success! Set cluster's %s desired stack to %s.\n", clusterName, targetStackId.getStackId());
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
