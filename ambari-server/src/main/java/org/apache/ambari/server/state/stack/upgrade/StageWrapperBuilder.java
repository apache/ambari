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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.serveraction.upgrades.AutoSkipFailedSummaryAction;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;

/**
 * Defines how to build stages for an Upgrade or Downgrade.
 */
public abstract class StageWrapperBuilder {

  /**
   * The message for the task which checks for skipped failures.
   */
  private static final String AUTO_SKIPPED_TASK_SUMMARY = "Pauses the upgrade if there were failed steps that were automatically skipped.";

  /**
   * The upgrade/downgrade grouping that the builder is for.
   */
  protected final Grouping m_grouping;

  /**
   * Constructor.
   *
   * @param grouping
   *          the upgrade/downgrade grouping (not {@code null}).
   */
  protected StageWrapperBuilder(Grouping grouping) {
    m_grouping = grouping;
  }

  /**
   * Adds a processing component that will be built into stage wrappers.
   *
   * @param ctx
   *          the upgrade context
   * @param hostsType
   *          the hosts, along with their type
   * @param service
   *          the service name
   * @param clientOnly
   *          whether the service is client only, no service checks
   * @param pc
   *          the AffectedComponent derived from the upgrade pack
   * @param params
   *          additional parameters
   */
  public abstract void add(UpgradeContext upgradeContext, HostsType hostsType, String service,
      boolean clientOnly, ProcessingComponent pc, Map<String, String> params);

  /**
   * Builds the stage wrappers, including any pre- and post-procesing that needs
   * to be performed.
   *
   * @param upgradeContext
   *          the upgrade context (not {@code null}).
   * @return a list of stages, never {@code null}
   */
  public final List<StageWrapper> build(UpgradeContext upgradeContext) {
    List<StageWrapper> stageWrappers = beforeBuild(upgradeContext);
    stageWrappers = build(upgradeContext, stageWrappers);
    stageWrappers = afterBuild(upgradeContext, stageWrappers);
    return stageWrappers;
  }

  /**
   * Performs any pre-processing that needs to be performed on the list of stage
   * wrappers.
   *
   * @param upgradeContext
   *          the upgrade context (not {@code null}).
   * @return the initial list of stage wrappers, or an empty list (never
   *         {@code null}).
   */
  protected List<StageWrapper> beforeBuild(UpgradeContext upgradeContext) {
    List<StageWrapper> stageWrappers = new ArrayList<>(100);
    return stageWrappers;
  }

  /**
   * Builds the stage wrappers.
   *
   * @param upgradeContext
   *          the upgrade context (not {@code null}).
   * @param stageWrappers
   *          the list of stage wrappers created by
   *          {@link #beforeBuild(UpgradeContext)}.
   * @return the stage wrapper list, (never {@code null})
   */
  public abstract List<StageWrapper> build(UpgradeContext upgradeContext,
      List<StageWrapper> stageWrappers);

  /**
   * Performs any post-processing that needs to be performed on the list of
   * stage wrappers.
   *
   * @param upgradeContext
   *          the upgrade context (not {@code null}).
   * @param stageWrappers
   *          the list of stage wrappers created by
   *          {@link #build(UpgradeContext, List)}.
   * @return the post-processed list of stage wrappers (never {@code null})
   */
  protected List<StageWrapper> afterBuild(UpgradeContext upgradeContext,
      List<StageWrapper> stageWrappers) {

    if (stageWrappers.isEmpty()) {
      return stageWrappers;
    }

    // we only want to insert the auto skip summary if the group is skippable
    // and the upgrade context says to auto skip failures
    final boolean autoSkipFailures;
    if (m_grouping instanceof ServiceCheckGrouping) {
      autoSkipFailures = upgradeContext.isServiceCheckFailureAutoSkipped();
    } else {
      autoSkipFailures = upgradeContext.isComponentFailureAutoSkipped();
    }

    if (m_grouping.supportsAutoSkipOnFailure && m_grouping.skippable && autoSkipFailures) {
      ServerActionTask skippedFailedCheck = new ServerActionTask();
      skippedFailedCheck.implClass = AutoSkipFailedSummaryAction.class.getName();
      skippedFailedCheck.summary = AUTO_SKIPPED_TASK_SUMMARY;

      TaskWrapper skippedFailedTaskWrapper = new TaskWrapper(null, null,
          Collections.<String> emptySet(), skippedFailedCheck);

      StageWrapper skippedFailedStageWrapper = new StageWrapper(
          StageWrapper.Type.SERVER_SIDE_ACTION, "Verifying Skipped Failures",
          skippedFailedTaskWrapper);

      stageWrappers.add(skippedFailedStageWrapper);
    }

    return stageWrappers;
  }

  /**
   * Consistently formats a string.
   * @param prefix
   * @param component
   * @param batchNum
   * @param totalBatches
   * @return the prepared string
   */
  protected String getStageText(String prefix, String component, Set<String> hosts, int batchNum, int totalBatches) {
    String stageText = getStageText(prefix, component, hosts);
    String batchText = 1 == totalBatches? "" : String.format(" (Batch %s of %s)", batchNum, totalBatches);
    return stageText + batchText;
  }

  /**
   * Consistently formats a string.
   * @param prefix
   * @param component
   * @param hosts
   * @return the prepared string
   */
  protected String getStageText(String prefix, String component, Set<String> hosts) {
    return String.format("%s %s on %s%s",
        prefix,
        component,
        1 == hosts.size() ? hosts.iterator().next() : Integer.valueOf(hosts.size()),
        1 == hosts.size() ? "" : " hosts");
  }

  /**
   * Determine the list of tasks given these rules
   * <ul>
   *   <li>When performing an upgrade, only use upgrade tasks</li>
   *   <li>When performing a downgrade, use the downgrade tasks if they are defined</li>
   *   <li>When performing a downgrade, but no downgrade tasks exist, reuse the upgrade tasks</li>
   * </ul>
   * @param forUpgrade  {@code true} if resolving for an upgrade, {@code false} for downgrade
   * @param preTasks    {@code true} if loading pre-upgrade or pre-downgrade
   * @param pc          the processing component holding task definitions
   * @return A collection, potentially empty, of the tasks to run, which may contain either
   * pre or post tasks if they exist, and the order depends on whether it's an upgrade or downgrade.
   */
  protected List<Task> resolveTasks(boolean forUpgrade, boolean preTasks, ProcessingComponent pc) {
    if (null == pc) {
      return Collections.emptyList();
    }

    if (forUpgrade) {
      return preTasks ? pc.preTasks : pc.postTasks;
    } else {
      return preTasks ?
        (null == pc.preDowngradeTasks ? pc.preTasks : pc.preDowngradeTasks) :
        (null == pc.postDowngradeTasks ? pc.postTasks : pc.postDowngradeTasks);
    }
  }
}
