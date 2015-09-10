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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.ServiceComponentHostEventWrapper;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * The {@link AutoSkipFailedSummaryAction} is used to check if any
 * {@link HostRoleCommand}s were skipped automatically after they failed during
 * an upgrade. This will be automatically marked as
 * {@link HostRoleStatus#COMPLETED} if there are no skipped failures. Otherwise
 * it will be placed into {@link HostRoleStatus#HOLDING}.
 */
public class AutoSkipFailedSummaryAction extends AbstractServerAction {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AutoSkipFailedSummaryAction.class);

  /**
   * The standard output template message.
   */
  private static final String FAILURE_STD_OUT_TEMPLATE = "There were {0} skipped failure(s) that must be addressed before you can proceed. Please resolve each failure before continuing with the upgrade.";

  /**
   * ...
   */
  private static final String MIDDLE_ELLIPSIZE_MARKER = "\n\u2026\n";

  /**
   * Used to lookup the {@link UpgradeGroupEntity}.
   */
  @Inject
  private UpgradeDAO m_upgradeDAO;

  /**
   * Used to lookup the tasks that need to be checked for
   * {@link HostRoleStatus#SKIPPED_FAILED}.
   */
  @Inject
  private HostRoleCommandDAO m_hostRoleCommandDAO;

  /**
   * Used for writing structured out.
   */
  @Inject
  private Gson m_gson;

  /**
   * A mapping of host -> Map<key,info> for each failure.
   */
  private Map<String, Map<String, Object>> m_structuredFailures = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    HostRoleCommand hostRoleCommand = getHostRoleCommand();
    long requestId = hostRoleCommand.getRequestId();
    long stageId = hostRoleCommand.getStageId();

    // use the host role command to get to the parent upgrade group
    UpgradeItemEntity upgradeItem = m_upgradeDAO.findUpgradeItemByRequestAndStage(requestId,stageId);
    UpgradeGroupEntity upgradeGroup = upgradeItem.getGroupEntity();

    // find all of the stages in this group
    long upgradeGroupId = upgradeGroup.getId();
    UpgradeGroupEntity upgradeGroupEntity = m_upgradeDAO.findUpgradeGroup(upgradeGroupId);
    List<UpgradeItemEntity> groupUpgradeItems = upgradeGroupEntity.getItems();
    TreeSet<Long> stageIds = new TreeSet<>();
    for (UpgradeItemEntity groupUpgradeItem : groupUpgradeItems) {
      stageIds.add(groupUpgradeItem.getStageId());
    }

    // for every stage, find all tasks that have been SKIPPED_FAILED - we use a
    // bit of trickery here since within any given request, the stage ID are
    // always sequential. This allows us to make a simple query instead of some
    // overly complex IN or NESTED SELECT query
    long minStageId = stageIds.first();
    long maxStageId = stageIds.last();

    List<HostRoleCommandEntity> skippedTasks = m_hostRoleCommandDAO.findByStatusBetweenStages(
        hostRoleCommand.getRequestId(),
        HostRoleStatus.SKIPPED_FAILED, minStageId, maxStageId);

    if (skippedTasks.isEmpty()) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
          "There were no skipped failures", null);
    }

    StringBuilder buffer = new StringBuilder("The following steps failed and were automatically skipped:\n");

    for (HostRoleCommandEntity skippedTask : skippedTasks) {
      try{
        ServiceComponentHostEventWrapper eventWrapper = new ServiceComponentHostEventWrapper(
            skippedTask.getEvent());

        ServiceComponentHostEvent event = eventWrapper.getEvent();

        String hostName = skippedTask.getHostName();
        if(null != hostName){
          Map<String, Object> failures = m_structuredFailures.get(hostName);
          if( null == failures ){
            failures = new HashMap<>();
            m_structuredFailures.put(hostName, failures);
          }

          failures.put("id", skippedTask.getTaskId());
          failures.put("exit_code", skippedTask.getExitcode());
          failures.put("output_log", skippedTask.getOutputLog());
          failures.put("error_log", skippedTask.getErrorLog());

          String stdOut = StringUtils.abbreviateMiddle(new String(skippedTask.getStdOut()),
              MIDDLE_ELLIPSIZE_MARKER, 1000);

          String stderr = StringUtils.abbreviateMiddle(new String(skippedTask.getStdError()),
              MIDDLE_ELLIPSIZE_MARKER, 1000);

          failures.put("stdout", stdOut);
          failures.put("stderr", stderr);
        }

        buffer.append(event.getServiceComponentName());
        if (null != event.getHostName()) {
          buffer.append(" on ");
          buffer.append(event.getHostName());
        }

        buffer.append(": ");
        buffer.append(skippedTask.getCommandDetail());
        buffer.append("\n");
      } catch (Exception exception) {
        LOG.warn("Unable to extract failure information for {}", skippedTask);
        buffer.append(": ");
        buffer.append(skippedTask);
      }
    }

    String structuredOutput = m_gson.toJson(m_structuredFailures);
    String standardOutput = MessageFormat.format(FAILURE_STD_OUT_TEMPLATE, skippedTasks.size());
    String standardError = buffer.toString();

    return createCommandReport(0, HostRoleStatus.HOLDING, structuredOutput, standardOutput,
        standardError);
  }
}
