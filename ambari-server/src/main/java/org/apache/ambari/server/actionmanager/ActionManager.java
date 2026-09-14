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
package org.apache.ambari.server.actionmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.CommandUtils;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * This class acts as the interface for action manager with other components.
 */
@Singleton
public class ActionManager {
  private static final Logger LOG = LoggerFactory.getLogger(ActionManager.class);
  private final ActionScheduler scheduler;
  private final ActionDBAccessor db;
  private final AtomicLong requestCounter;
  private final RequestFactory requestFactory;
  private static TopologyManager topologyManager;


  /**
   * Guice-injected Constructor.
   *
   * @param db
   * @param requestFactory
   * @param scheduler
   */
  @Inject
  public ActionManager(ActionDBAccessor db, RequestFactory requestFactory,
      ActionScheduler scheduler) {
    this.db = db;
    this.requestFactory = requestFactory;
    this.scheduler = scheduler;

    requestCounter = new AtomicLong(db.getLastPersistedRequestIdWhenInitialized());
  }

  public void start() {
    LOG.info("Starting scheduler thread");
    scheduler.start();
  }

  public void shutdown() {
    scheduler.stop();
  }

  public void sendActions(List<Stage> stages, String clusterHostInfo, ExecuteActionRequest actionRequest) throws AmbariException {
    Request request = requestFactory.createNewFromStages(stages, clusterHostInfo, actionRequest);
    request.setUserName(AuthorizationHelper.getAuthenticatedName());
    sendActions(request, actionRequest);
  }

  public void sendActions(Request request, ExecuteActionRequest executeActionRequest) throws AmbariException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Persisting Request into DB: {}", request);

      if (executeActionRequest != null) {
        LOG.debug("In response to request: {}", request);
      }
    }
    db.persistActions(request);
    scheduler.awake();
  }

  public List<Request> getRequests(Collection<Long> requestIds) {
    List<Request> requests =  db.getRequests(requestIds);
    requests.addAll(topologyManager.getRequests(requestIds));

    return requests;
  }

  public List<Stage> getRequestStatus(long requestId) {
    return db.getAllStages(requestId);
  }

  public Stage getAction(long requestId, long stageId) {
    return db.getStage(StageUtils.getActionId(requestId, stageId));
  }

  /**
   * Get all actions(stages) for a request.
   *
   * @param requestId the request id
   * @return list of all stages associated with the given request id
   */
  public List<Stage> getActions(long requestId) {
    return db.getAllStages(requestId);
  }

  public HostRoleCommand getTaskById(long taskId) {
    return db.getTask(taskId);
  }

  /**
   * Persists command reports into the db
   * @param reports command reports
   * @param commands a list of commands that correspond to reports list (it should be
   * a 1 to 1 matching). We use this list to avoid fetching commands from the DB
   * twice
   */
  public void processTaskResponse(String hostname, List<CommandReport> reports,
                                  Map<Long, HostRoleCommand> commands) {
    if (reports == null) {
      return;
    }

    reports = getValidTaskReports(hostname, reports, commands);
    reports.sort(Comparator.comparingLong(CommandReport::getTaskId));
    List<CommandReport> reportsToProcess = new ArrayList<>();
    //persist the action response into the db.
    for (CommandReport report : reports) {
      HostRoleCommand command = commands.get(report.getTaskId());
      if (LOG.isDebugEnabled()) {
        LOG.debug("Processing command report : {}", report);
      }
      if (command == null) {
        LOG.warn("The task " + report.getTaskId()
            + " is invalid");
        continue;
      }
      if (! command.getStatus().equals(HostRoleStatus.IN_PROGRESS)
          && ! command.getStatus().equals(HostRoleStatus.QUEUED)
          && ! command.getStatus().equals(HostRoleStatus.ABORTED)) {
        LOG.warn("The task " + command.getTaskId()
            + " is not in progress, ignoring update");
        continue;
      }
      reportsToProcess.add(report);
    }

    if (!reportsToProcess.isEmpty()) {
      db.updateHostRoleStates(reportsToProcess);
    }
  }

  /**
   * Validates agent-supplied identities before reports reach any state machine.
   * The hostname must come from the authenticated agent session. Task, stage and
   * execution-command identities come from persisted server commands, not from
   * the report's cluster envelope. The returned list does not modify the input.
   */
  public List<CommandReport> getValidTaskReports(String hostname, List<CommandReport> reports,
      Map<Long, HostRoleCommand> commands) {
    List<CommandReport> accepted = new ArrayList<>();
    if (reports == null) {
      return accepted;
    }
    for (CommandReport report : reports) {
      if (report == null) {
        continue;
      }
      HostRoleCommand command = commands.get(report.getTaskId());
      if (isValidTaskReport(hostname, report, command)) {
        accepted.add(report);
      } else {
        // Do not log untrusted output or serialized execution commands (secrets).
        LOG.warn("Rejected agent report for task {}: identity or status does not match the persisted task",
            report.getTaskId());
      }
    }
    return accepted;
  }

  private boolean isValidTaskReport(String hostname, CommandReport report, HostRoleCommand command) {
    if (command == null || hostname == null || !hostname.equals(command.getHostName())
        || command.getTaskId() != report.getTaskId()
        || command.getRole() == null || !command.getRole().name().equals(report.getRole())
        || command.getRoleCommand() == null
        || !command.getRoleCommand().name().equals(report.getRoleCommand())
        || !StageUtils.getActionId(command.getRequestId(), command.getStageId()).equals(report.getActionId())) {
      return false;
    }
    try {
      HostRoleStatus status = HostRoleStatus.valueOf(report.getStatus());
      long clusterId = Long.parseLong(report.getClusterId());
      Stage stage = getAction(command.getRequestId(), command.getStageId());
      if (stage == null || stage.getClusterId() != clusterId) {
        return false;
      }
      // Read the stored command without expanding or refreshing configurations.
      // This also works when a task is loaded after a server restart.
      ExecutionCommand execution = StageUtils.getGson().fromJson(
          command.getExecutionCommandWrapper().getJson(), ExecutionCommand.class);
      if (execution == null || !Long.toString(clusterId).equals(execution.getClusterId())
          || !Objects.equals(agentServiceName(execution.getServiceName()),
              agentServiceName(report.getServiceName()))) {
        return false;
      }
      String customCommand = execution.getCommandParams() == null ? null
          : execution.getCommandParams().get("custom_command");
      // The agent adds customCommand to execution results, not progress templates.
      return (report.getCustomCommand() == null && status != HostRoleStatus.COMPLETED)
          || Objects.equals(customCommand, report.getCustomCommand());
    } catch (RuntimeException ex) {
      // Missing/corrupt persisted identity and malformed input both fail closed.
      // Continue validating siblings; never emit configuration or report secrets.
      return false;
    }
  }

  private static String agentServiceName(String serviceName) {
    // ActionQueue uses the literal "null" for commands with no service scope.
    return serviceName == null ? "null" : serviceName;
  }

  /**
   * Find if the command report is for an in progress command
   * @param report
   * @return
   */
  public boolean isInProgressCommand(CommandReport report) {
    HostRoleCommand command = db.getTask(report.getTaskId());
    if (command == null) {
      LOG.warn("The task " + report.getTaskId() + " is invalid");
      return false;
    }
    return command.getStatus().equals(HostRoleStatus.IN_PROGRESS)
      || command.getStatus().equals(HostRoleStatus.QUEUED);
  }

  public void handleLostHost(String host) {
    //Do nothing, the task will timeout anyway.
    //The actions can be failed faster as an optimization
    //if action timeout happens to be much larger than
    //heartbeat timeout.
  }

  public long getNextRequestId() {
    return requestCounter.incrementAndGet();
  }

  public List<HostRoleCommand> getRequestTasks(long requestId) {
    return db.getRequestTasks(requestId);
  }

  public List<HostRoleCommand> getAllTasksByRequestIds(Collection<Long> requestIds) {
    return db.getAllTasksByRequestIds(requestIds);
  }

  public Collection<HostRoleCommand> getTasks(Collection<Long> taskIds) {
    return db.getTasks(taskIds);
  }

  public Map<Long, HostRoleCommand> getTasksMap(Collection<Long> taskIds) {
    return CommandUtils.convertToTaskIdCommandMap(getTasks(taskIds));
  }

  /**
   * Get first or last maxResults requests that are in the specified status
   *
   * @param status
   *          Desired request status
   * @param maxResults
   *          maximal number of returned id's
   * @param ascOrder
   *          defines sorting order for database query result
   * @return First or last maxResults request id's if ascOrder is true or false,
   *         respectively
   */
  public List<Long> getRequestsByStatus(RequestStatus status, int maxResults, boolean ascOrder) {
    List<Long> requests = db.getRequestsByStatus(status, maxResults, ascOrder);

    for (Request logicalRequest : topologyManager.getRequests(Collections.emptySet())) {
      //todo: Request.getStatus() returns HostRoleStatus and we are comparing to RequestStatus
      //todo: for now just compare the names as RequestStatus names are a subset of HostRoleStatus names
      HostRoleStatus logicalRequestStatus = logicalRequest.getStatus();
      if (status == null || (logicalRequestStatus != null
          && logicalRequest.getStatus().name().equals(status.name()))) {
        requests.add(logicalRequest.getRequestId());
      }
    }
    return requests;
  }

  public Map<Long, String> getRequestContext(List<Long> requestIds) {
    return db.getRequestContext(requestIds);
  }

  public String getRequestContext(long requestId) {
    return db.getRequestContext(requestId);
  }

  public void cancelRequest(long requestId, String reason) {
    scheduler.scheduleCancellingRequest(requestId, reason);
    scheduler.awake();
  }

  //todo: proper static injection
  public static void setTopologyManager(TopologyManager topologyManager) {
    ActionManager.topologyManager = topologyManager;
  }

  public void resubmitTasks(List<Long> taskIds) {
    db.resubmitTasks(taskIds);
  }

}
