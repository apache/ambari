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
package org.apache.ambari.server.actionmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.annotations.TransactionalLock;
import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.annotations.TransactionalLock.LockType;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.OperationStatusAuditEvent;
import org.apache.ambari.server.audit.event.TaskStatusAuditEvent;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.RequestFinishedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.RequestScheduleDAO;
import org.apache.ambari.server.orm.dao.RoleSuccessCriteriaDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
import org.apache.ambari.server.orm.entities.RoleSuccessCriteriaEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.utils.LoopBody;
import org.apache.ambari.server.utils.Parallel;
import org.apache.ambari.server.utils.ParallelLoopResult;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;

@Singleton
public class ActionDBAccessorImpl implements ActionDBAccessor {
  private static final Logger LOG = LoggerFactory.getLogger(ActionDBAccessorImpl.class);

  private long requestId;

  @Inject
  ClusterDAO clusterDAO;

  @Inject
  HostDAO hostDAO;

  @Inject
  RequestDAO requestDAO;

  @Inject
  StageDAO stageDAO;

  @Inject
  HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  ExecutionCommandDAO executionCommandDAO;

  @Inject
  RoleSuccessCriteriaDAO roleSuccessCriteriaDAO;

  @Inject
  StageFactory stageFactory;

  @Inject
  RequestFactory requestFactory;

  @Inject
  HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  Clusters clusters;

  @Inject
  RequestScheduleDAO requestScheduleDAO;

  @Inject
  Configuration configuration;

  @Inject
  AmbariEventPublisher ambariEventPublisher;

  @Inject
  AuditLogger auditLogger;

  /**
   * Cache for auditlog. It stores a {@link RequestDetails} object for every requests.
   * {@link RequestDetails} contains the previous status of the request and a map for tasks.
   * A task has a {@link RequestDetails.Component} key and the value of that is the previous status of the task.
   * {@link RequestDetails.Component} contains the component name and the host name
   */
  private Cache<Long,RequestDetails> auditlogRequestCache = CacheBuilder.newBuilder().expireAfterAccess(60, TimeUnit.MINUTES).concurrencyLevel(4).build();

  private Cache<Long, HostRoleCommand> hostRoleCommandCache;
  private long cacheLimit; //may be exceeded to store tasks from one request

  @Inject
  public ActionDBAccessorImpl(@Named("executionCommandCacheSize") long cacheLimit,
                              AmbariEventPublisher eventPublisher) {

    this.cacheLimit = cacheLimit;
    hostRoleCommandCache = CacheBuilder.newBuilder().
        expireAfterAccess(5, TimeUnit.MINUTES).
        build();

    eventPublisher.register(this);
  }

  @Inject
  void init() {
    requestId = stageDAO.getLastRequestId();
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#getStage(java.lang.String)
   */
  @Override
  public Stage getStage(String actionId) {
    StageEntity stageEntity = stageDAO.findByActionId(actionId);
    return stageEntity == null ? null : stageFactory.createExisting(stageEntity);
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#getAllStages(java.lang.String)
   */
  @Override
  public List<Stage> getAllStages(long requestId) {
    List<StageEntity> stageEntities = stageDAO.findByRequestId(requestId);
    List<Stage> stages = new ArrayList<>(stageEntities.size());
    for (StageEntity stageEntity : stageEntities ){
      stages.add(stageFactory.createExisting(stageEntity));
    }

    return stages;
  }

  @Override
  public RequestEntity getRequestEntity(long requestId) {
    return requestDAO.findByPK(requestId);
  }

  @Override
  public Request getRequest(long requestId) {
    RequestEntity requestEntity = getRequestEntity(requestId);
    if (requestEntity != null) {
      return requestFactory.createExisting(requestEntity);
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void abortOperation(long requestId) {
    long now = System.currentTimeMillis();

    endRequest(requestId);

    // only request commands which actually need to be aborted; requesting all
    // commands here can cause OOM problems during large requests like upgrades
    List<HostRoleCommandEntity> commands = hostRoleCommandDAO.findByRequestIdAndStatuses(requestId,
        HostRoleStatus.SCHEDULED_STATES);

    for (HostRoleCommandEntity command : commands) {
      command.setStatus(HostRoleStatus.ABORTED);
      command.setEndTime(now);
      LOG.info("Aborting command. Hostname " + command.getHostName()
          + " role " + command.getRole()
          + " requestId " + command.getRequestId()
          + " taskId " + command.getTaskId()
          + " stageId " + command.getStageId());

      auditLog(command, requestId);
    }

    // no need to merge if there's nothing to merge
    if (!commands.isEmpty()) {
      hostRoleCommandDAO.mergeAll(commands);
    }
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#timeoutHostRole(long, long, org.apache.ambari.server.Role)
   */
  @Override
  public void timeoutHostRole(String host, long requestId, long stageId,
                              String role) {
    timeoutHostRole(host, requestId, stageId, role, false);
  }

  @Override
  public void timeoutHostRole(String host, long requestId, long stageId,
                              String role, boolean skipSupported) {
    long now = System.currentTimeMillis();
    List<HostRoleCommandEntity> commands =
            hostRoleCommandDAO.findByHostRole(host, requestId, stageId, role);
    for (HostRoleCommandEntity command : commands) {
      if (skipSupported) {
        command.setStatus(HostRoleStatus.SKIPPED_FAILED);
      } else {
        command.setStatus(command.isRetryAllowed() ? HostRoleStatus.HOLDING_TIMEDOUT : HostRoleStatus.TIMEDOUT);
      }

      command.setEndTime(now);

      auditLog(command, requestId);
    }

    // no need to merge if there's nothing to merge
    if (!commands.isEmpty()) {
      hostRoleCommandDAO.mergeAll(commands);
    }

    endRequestIfCompleted(requestId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Experimental(feature = ExperimentalFeature.PARALLEL_PROCESSING)
  public List<Stage> getStagesInProgressForRequest(Long requestId) {
    List<StageEntity> stageEntities = stageDAO.findByRequestIdAndCommandStatuses(requestId, HostRoleStatus.IN_PROGRESS_STATUSES);
    return getStagesForEntities(stageEntities);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Experimental(feature = ExperimentalFeature.PARALLEL_PROCESSING)
  public List<Stage> getStagesInProgress() {
    List<StageEntity> stageEntities = stageDAO.findByCommandStatuses(
      HostRoleStatus.IN_PROGRESS_STATUSES);
    return getStagesForEntities(stageEntities);
  }

  @Experimental(feature = ExperimentalFeature.PARALLEL_PROCESSING)
  private List<Stage> getStagesForEntities(List<StageEntity> stageEntities) {
    // experimentally enable parallel stage processing
    @Experimental(feature = ExperimentalFeature.PARALLEL_PROCESSING)
    boolean useConcurrentStageProcessing = configuration.isExperimentalConcurrentStageProcessingEnabled();
    if (useConcurrentStageProcessing) {
      ParallelLoopResult<Stage> loopResult = Parallel.forLoop(stageEntities,
          new LoopBody<StageEntity, Stage>() {
            @Override
            public Stage run(StageEntity stageEntity) {
              return stageFactory.createExisting(stageEntity);
            }
          });
      if (loopResult.getIsCompleted()) {
        return loopResult.getResult();
      } else {
        // Fetch any missing results sequentially
        List<Stage> stages = loopResult.getResult();
        for (int i = 0; i < stages.size(); i++) {
          if (stages.get(i) == null) {
            stages.set(i, stageFactory.createExisting(stageEntities.get(i)));
          }
        }
        return stages;
      }
    } else {
      List<Stage> stages = new ArrayList<>(stageEntities.size());
      for (StageEntity stageEntity : stageEntities) {
        stages.add(stageFactory.createExisting(stageEntity));
      }
      return stages;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getCommandsInProgressCount() {
    Number count = hostRoleCommandDAO.getCountByStatus(HostRoleStatus.IN_PROGRESS_STATUSES);
    if (null == count) {
      return 0;
    }

    return count.intValue();
  }

  @Override
  @Transactional
  @TransactionalLock(lockArea = LockArea.HRC_STATUS_CACHE, lockType = LockType.WRITE)
  public void persistActions(Request request) throws AmbariException {

    RequestEntity requestEntity = request.constructNewPersistenceEntity();

    Long clusterId = -1L;
    ClusterEntity clusterEntity = clusterDAO.findById(request.getClusterId());
    if (clusterEntity != null) {
      clusterId = clusterEntity.getClusterId();
    }

    requestEntity.setClusterId(clusterId);
    requestDAO.create(requestEntity);

    //TODO wire request to cluster
    List<StageEntity> stageEntities = new ArrayList<StageEntity>(request.getStages().size());

    addRequestToAuditlogCache(request);

    for (Stage stage : request.getStages()) {
      StageEntity stageEntity = stage.constructNewPersistenceEntity();
      stageEntities.add(stageEntity);
      stageEntity.setClusterId(clusterId);
      stageEntity.setRequest(requestEntity);
      stageDAO.create(stageEntity);

      List<HostRoleCommand> orderedHostRoleCommands = stage.getOrderedHostRoleCommands();

      for (HostRoleCommand hostRoleCommand : orderedHostRoleCommands) {
        HostRoleCommandEntity hostRoleCommandEntity = hostRoleCommand.constructNewPersistenceEntity();
        hostRoleCommandEntity.setStage(stageEntity);
        hostRoleCommandDAO.create(hostRoleCommandEntity);

        assert hostRoleCommandEntity.getTaskId() != null;
        hostRoleCommand.setTaskId(hostRoleCommandEntity.getTaskId());

        String prefix = "";
        String output = "output-" + hostRoleCommandEntity.getTaskId() + ".txt";
        String error = "errors-" + hostRoleCommandEntity.getTaskId() + ".txt";

        HostEntity hostEntity = null;
        if (null != hostRoleCommandEntity.getHostId()) {
          hostEntity = hostDAO.findById(hostRoleCommandEntity.getHostId());
          if (hostEntity == null) {
            String msg = String.format("Host %s doesn't exist in database", hostRoleCommandEntity.getHostName());
            LOG.error(msg);
            throw new AmbariException(msg);
          }

          hostRoleCommandEntity.setHostEntity(hostEntity);

          try {
            // Get the in-memory host object and its prefix to construct the output and error log paths.
            Host hostObject = clusters.getHost(hostEntity.getHostName());

            if (!StringUtils.isBlank(hostObject.getPrefix())) {
              prefix = hostObject.getPrefix();
              if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
              }
            }
          } catch (AmbariException e) {
            LOG.warn("Exception in getting prefix for host and setting output and error log files.  Using no prefix");
          }
        }

        hostRoleCommand.setOutputLog(prefix + output);
        hostRoleCommand.setErrorLog(prefix + error);
        hostRoleCommandEntity.setOutputLog(hostRoleCommand.getOutputLog());
        hostRoleCommandEntity.setErrorLog(hostRoleCommand.getErrorLog());

        ExecutionCommandEntity executionCommandEntity = hostRoleCommand.constructExecutionCommandEntity();
        executionCommandEntity.setHostRoleCommand(hostRoleCommandEntity);

        executionCommandEntity.setTaskId(hostRoleCommandEntity.getTaskId());
        hostRoleCommandEntity.setExecutionCommand(executionCommandEntity);

        executionCommandDAO.create(hostRoleCommandEntity.getExecutionCommand());
        hostRoleCommandEntity = hostRoleCommandDAO.merge(hostRoleCommandEntity);

        if (null != hostEntity) {
          hostEntity = hostDAO.merge(hostEntity);
        }
      }

      for (RoleSuccessCriteriaEntity roleSuccessCriteriaEntity : stageEntity.getRoleSuccessCriterias()) {
        roleSuccessCriteriaDAO.create(roleSuccessCriteriaEntity);
      }

      stageEntity = stageDAO.merge(stageEntity);
    }

    requestEntity.setStages(stageEntities);
    requestDAO.merge(requestEntity);
  }

  @Override
  @Transactional
  public void startRequest(long requestId) {
    RequestEntity requestEntity = getRequestEntity(requestId);
    if (requestEntity != null && requestEntity.getStartTime() == -1L) {
      requestEntity.setStartTime(System.currentTimeMillis());
      requestDAO.merge(requestEntity);
    }
  }

  @Override
  @Transactional
  public void endRequest(long requestId) {
    RequestEntity requestEntity = getRequestEntity(requestId);
    if (requestEntity != null && requestEntity.getEndTime() == -1L) {
      requestEntity.setEndTime(System.currentTimeMillis());
      requestDAO.merge(requestEntity);
      ambariEventPublisher.publish(new RequestFinishedEvent(requestEntity.getClusterId(), requestId));
    }
  }

  public void endRequestIfCompleted(long requestId) {
    if (requestDAO.isAllTasksCompleted(requestId)) {
      endRequest(requestId);
    }
  }

  @Override
  @Transactional
  public void setSourceScheduleForRequest(long requestId, long scheduleId) {
    RequestEntity requestEntity = requestDAO.findByPK(requestId);
    if (requestEntity != null) {
      RequestScheduleEntity scheduleEntity = requestScheduleDAO.findById(scheduleId);
      if (scheduleEntity != null) {
        requestEntity.setRequestScheduleEntity(scheduleEntity);
        //we may want to break entity graph here for perf purposes (when list size is too large)
        scheduleEntity.getRequestEntities().add(requestEntity);

        requestDAO.merge(requestEntity);
        requestScheduleDAO.merge(scheduleEntity);

      } else {
        String message = String.format("Request Schedule with id=%s not found", scheduleId);
        LOG.error(message);
        throw new RuntimeException(message);
      }

    } else {
      String message = String.format("Request with id=%s not found", scheduleId);
      LOG.error(message);
      throw new RuntimeException(message);
    }
  }

  @Override
  public void updateHostRoleStates(Collection<CommandReport> reports) {
    Map<Long, CommandReport> taskReports = new HashMap<Long, CommandReport>();
    for (CommandReport report : reports) {
      taskReports.put(report.getTaskId(), report);
    }

    long now = System.currentTimeMillis();

    List<Long> requestsToCheck = new ArrayList<Long>();
    List<Long> abortedCommandUpdates = new ArrayList<Long>();

    List<HostRoleCommandEntity> commandEntities = hostRoleCommandDAO.findByPKs(taskReports.keySet());
    for (HostRoleCommandEntity commandEntity : commandEntities) {
      CommandReport report = taskReports.get(commandEntity.getTaskId());

      boolean statusChanged = false;

      switch (commandEntity.getStatus()) {
        case ABORTED:
          // We don't want to overwrite statuses for ABORTED tasks with
          // statuses that have been received from the agent after aborting task
          abortedCommandUpdates.add(commandEntity.getTaskId());
          break;
        default:
          HostRoleStatus status = HostRoleStatus.valueOf(report.getStatus());
          // if FAILED and marked for holding then set status = HOLDING_FAILED
          if (status == HostRoleStatus.FAILED && commandEntity.isRetryAllowed()) {
            status = HostRoleStatus.HOLDING_FAILED;

            // tasks can be marked as skipped when they fail
            if (commandEntity.isFailureAutoSkipped()) {
              status = HostRoleStatus.SKIPPED_FAILED;
            }
          }

          commandEntity.setStatus(status);
          statusChanged = true;
          break;
      }

      commandEntity.setStdOut(report.getStdOut().getBytes());
      commandEntity.setStdError(report.getStdErr().getBytes());
      commandEntity.setStructuredOut(report.getStructuredOut() == null ? null :
        report.getStructuredOut().getBytes());
      commandEntity.setExitcode(report.getExitCode());

      if (HostRoleStatus.getCompletedStates().contains(commandEntity.getStatus())) {
        commandEntity.setEndTime(now);

        String actionId = report.getActionId();
        long[] requestStageIds = StageUtils.getRequestStage(actionId);
        long requestId = requestStageIds[0];
        long stageId = requestStageIds[1];
        if(statusChanged) {
          auditLog(commandEntity, requestId);
        }
        if (requestDAO.getLastStageId(requestId).equals(stageId)) {
          requestsToCheck.add(requestId);
        }
      }
    }

    // no need to merge if there's nothing to merge
    if (!commandEntities.isEmpty()) {
      hostRoleCommandDAO.mergeAll(commandEntities);
    }

    // Invalidate cache because of updates to ABORTED commands
    hostRoleCommandCache.invalidateAll(abortedCommandUpdates);

    for (Long requestId : requestsToCheck) {
      endRequestIfCompleted(requestId);
    }
  }

  @Override
  public void updateHostRoleState(String hostname, long requestId,
                                  long stageId, String role, CommandReport report) {
    boolean checkRequest = false;
    if (LOG.isDebugEnabled()) {
      LOG.debug("Update HostRoleState: "
        + "HostName " + hostname + " requestId " + requestId + " stageId "
        + stageId + " role " + role + " report " + report);
    }

    long now = System.currentTimeMillis();
    List<HostRoleCommandEntity> commands = hostRoleCommandDAO.findByHostRole(
      hostname, requestId, stageId, role);

    for (HostRoleCommandEntity command : commands) {
      HostRoleStatus status = HostRoleStatus.valueOf(report.getStatus());

      // if FAILED and marked for holding then set status = HOLDING_FAILED
      if (status == HostRoleStatus.FAILED && command.isRetryAllowed()) {
        status = HostRoleStatus.HOLDING_FAILED;

        // tasks can be marked as skipped when they fail
        if (command.isFailureAutoSkipped()) {
          status = HostRoleStatus.SKIPPED_FAILED;
        }
      }

      command.setStatus(status);
      command.setStdOut(report.getStdOut().getBytes());
      command.setStdError(report.getStdErr().getBytes());
      command.setStructuredOut(report.getStructuredOut() == null ? null :
        report.getStructuredOut().getBytes());

      if (HostRoleStatus.getCompletedStates().contains(command.getStatus())) {
        command.setEndTime(now);
        if (requestDAO.getLastStageId(requestId).equals(stageId)) {
          checkRequest = true;
        }
      }
      command.setExitcode(report.getExitCode());

      auditLog(command, requestId);
    }

    // no need to merge if there's nothing to merge
    if (!commands.isEmpty()) {
      hostRoleCommandDAO.mergeAll(commands);
    }

    if (checkRequest) {
      endRequestIfCompleted(requestId);
    }
  }

  @Override
  public void abortHostRole(String host, long requestId, long stageId, String role) {
    String reason = String.format("On host %s role %s in invalid state.", host, role);
    abortHostRole(host, requestId, stageId, role, reason);
  }

  @Override
  public void abortHostRole(String host, long requestId, long stageId, String role, String reason) {
    CommandReport report = new CommandReport();
    report.setExitCode(999);
    report.setStdErr(reason);
    report.setStdOut("");
    report.setStatus("ABORTED");
    updateHostRoleState(host, requestId, stageId, role, report);
  }

  @Override
  public long getLastPersistedRequestIdWhenInitialized() {
    return requestId;
  }


  @Override
  @Transactional
  public void bulkHostRoleScheduled(Stage s, List<ExecutionCommand> commands) {
    for (ExecutionCommand command : commands) {
      hostRoleScheduled(s, command.getHostname(), command.getRole());
    }
  }

  @Override
  @Transactional
  public void bulkAbortHostRole(Stage s, Map<ExecutionCommand, String> commands) {
    for (ExecutionCommand command : commands.keySet()) {
      String reason = String.format("On host %s role %s in invalid state.\n%s",
              command.getHostname(), command.getRole(), commands.get(command));
      abortHostRole(command.getHostname(), s.getRequestId(), s.getStageId(), command.getRole(), reason);
    }
  }

  @Override
  @Transactional
  public void hostRoleScheduled(Stage s, String hostname, String roleStr) {
    HostRoleCommand hostRoleCommand = s.getHostRoleCommand(hostname, roleStr);
    HostRoleCommandEntity entity = hostRoleCommandDAO.findByPK(hostRoleCommand.getTaskId());
    if (entity != null) {
      entity.setStartTime(hostRoleCommand.getStartTime());
      if (entity.getOriginalStartTime() == null || entity.getOriginalStartTime() == -1) {
        entity.setOriginalStartTime(System.currentTimeMillis());
      }
      entity.setLastAttemptTime(hostRoleCommand.getLastAttemptTime());
      entity.setStatus(hostRoleCommand.getStatus());
      entity.setAttemptCount(hostRoleCommand.getAttemptCount());

      auditLog(entity, s.getRequestId());

      hostRoleCommandDAO.merge(entity);
    } else {
      throw new RuntimeException("HostRoleCommand is not persisted, cannot update:\n" + hostRoleCommand);
    }
  }

  @Override
  public List<HostRoleCommand> getRequestTasks(long requestId) {
    return getTasks(hostRoleCommandDAO.findTaskIdsByRequest(requestId));
  }

  @Override
  public List<HostRoleCommand> getAllTasksByRequestIds(Collection<Long> requestIds) {
    if (requestIds.isEmpty()) {
      return Collections.emptyList();
    }

    return getTasks(
        hostRoleCommandDAO.findTaskIdsByRequestIds(requestIds)
    );
  }

  @Override
  public List<HostRoleCommand> getTasks(Collection<Long> taskIds) {
    if (taskIds.isEmpty()) {
      return Collections.emptyList();
    }

    List<HostRoleCommand> commands = new ArrayList<HostRoleCommand>();

    Map<Long, HostRoleCommand> cached = hostRoleCommandCache.getAllPresent(taskIds);
    commands.addAll(cached.values());

    List<Long> absent = new ArrayList<Long>();
    absent.addAll(taskIds);
    absent.removeAll(cached.keySet());

    if (!absent.isEmpty()) {
      boolean allowStore = hostRoleCommandCache.size() <= cacheLimit;

      for (HostRoleCommandEntity commandEntity : hostRoleCommandDAO.findByPKs(absent)) {
        HostRoleCommand hostRoleCommand = hostRoleCommandFactory.createExisting(commandEntity);
        commands.add(hostRoleCommand);
        if (allowStore) {
          switch (hostRoleCommand.getStatus()) {
            case ABORTED:
            case COMPLETED:
            case TIMEDOUT:
            case FAILED:
              hostRoleCommandCache.put(hostRoleCommand.getTaskId(), hostRoleCommand);
              break;
          }
        }
      }
    }
    Collections.sort(commands, new Comparator<HostRoleCommand>() {
      @Override
      public int compare(HostRoleCommand o1, HostRoleCommand o2) {
        return (int) (o1.getTaskId()-o2.getTaskId());
      }
    });
    return commands;
  }

  @Override
  public List<HostRoleCommand> getTasksByHostRoleAndStatus(String hostname, String role, HostRoleStatus status) {
    return getTasks(hostRoleCommandDAO.findTaskIdsByHostRoleAndStatus(hostname, role, status));
  }

  @Override
  public List<HostRoleCommand> getTasksByRoleAndStatus(String role, HostRoleStatus status) {
    return getTasks(hostRoleCommandDAO.findTaskIdsByRoleAndStatus(role, status));
  }

  @Override
  public HostRoleCommand getTask(long taskId) {
    HostRoleCommandEntity commandEntity = hostRoleCommandDAO.findByPK((int) taskId);
    if (commandEntity == null) {
      return null;
    }
    return hostRoleCommandFactory.createExisting(commandEntity);
  }

  @Override
  public List<Long> getRequestsByStatus(RequestStatus status, int maxResults,
    boolean ascOrder) {

    if (null == status) {
      return requestDAO.findAllRequestIds(maxResults, ascOrder);
    }

    EnumSet<HostRoleStatus> taskStatuses = null;
    switch( status ){
      case IN_PROGRESS:
        taskStatuses = HostRoleStatus.IN_PROGRESS_STATUSES;
        break;
      case FAILED:
        taskStatuses = HostRoleStatus.FAILED_STATUSES;
        break;
      case COMPLETED:
        // !!! COMPLETED is special as all tasks in the request must be
        // completed
        return hostRoleCommandDAO.getCompletedRequests(maxResults, ascOrder);
    }

    return hostRoleCommandDAO.getRequestsByTaskStatus(taskStatuses, maxResults,
      ascOrder);
  }

  @Override
  public Map<Long, String> getRequestContext(List<Long> requestIds) {
    return stageDAO.findRequestContext(requestIds);
  }

  @Override
  public String getRequestContext(long requestId) {
    return stageDAO.findRequestContext(requestId);
  }

  @Override
  public List<Request> getRequests(Collection<Long> requestIds) {
    List<RequestEntity> requestEntities = requestDAO.findByPks(requestIds);
    List<Request> requests = new ArrayList<Request>(requestEntities.size());
    for (RequestEntity requestEntity : requestEntities) {
      requests.add(requestFactory.createExisting(requestEntity));
    }
    return requests;
  }

  @Override
  public void resubmitTasks(List<Long> taskIds) {
    List<HostRoleCommandEntity> tasks = hostRoleCommandDAO.findByPKs(taskIds);
    for (HostRoleCommandEntity task : tasks) {
      task.setStatus(HostRoleStatus.PENDING);
      // TODO HACK, shouldn't reset start time.
      // Because it expects -1, RetryActionMonitor.java also had to set it to -1.
      task.setStartTime(-1L);
      task.setEndTime(-1L);

      auditLog(task, task.getRequestId());
    }

    // no need to merge if there's nothing to merge
    if (!tasks.isEmpty()) {
      hostRoleCommandDAO.mergeAll(tasks);
    }

    hostRoleCommandCache.invalidateAll(taskIds);
  }

  /**
   * Invalidate cached HostRoleCommands if a host is deleted.
   * @param event @HostRemovedEvent
   */
  @Subscribe
  public void invalidateCommandCacheOnHostRemove(HostsRemovedEvent event) {
    LOG.info("Invalidating HRC cache after receiveing {}", event);
    hostRoleCommandCache.invalidateAll();
  }

  /**
   * Updates auditlog cache and returns the status of the latest task for the given component on the given host.
   * @param commandEntity new entity with the new status. It also holds the component and the hostname
   * @param requestId
   * @return
   */
  private HostRoleStatus updateAuditlogCache(HostRoleCommandEntity commandEntity, Long requestId) {

    RequestDetails details = auditlogRequestCache.getIfPresent(requestId);
    if(details == null) {
      return null;
    }

    RequestDetails.Component component = new RequestDetails.Component(commandEntity.getRole(), commandEntity.getHostName());

    HostRoleStatus lastTaskStatus = null;
    if(details.getTasks().containsKey(component)) {
      lastTaskStatus = details.getTasks().get(component);
    }
    details.getTasks().put(component, commandEntity.getStatus());
    return lastTaskStatus;
  }

  /**
   * Adds request to auditlog cache
   * @param request
   */
  private void addRequestToAuditlogCache(Request request) {
    if(!auditLogger.isEnabled()) {
      return;
    }
    if(auditlogRequestCache.getIfPresent(request.getRequestId()) == null) {
      int numberOfTasks = 0;
      for (Stage stage : request.getStages()) {
        numberOfTasks += stage.getOrderedHostRoleCommands().size();
      }
      RequestDetails requestDetails = new RequestDetails();
      requestDetails.setNumberOfTasks(numberOfTasks);
      requestDetails.setUserName(AuthorizationHelper.getAuthenticatedName());
      auditlogRequestCache.put(request.getRequestId(), requestDetails);
    }
  }

  /**
   * AuditLog operation status change
   * @param requestId
   */
  private void auditLog(HostRoleCommandEntity commandEntity, Long requestId) {
    if(!auditLogger.isEnabled()) {
      return;
    }

    if(requestId != null) {
      HostRoleStatus lastTaskStatus = updateAuditlogCache(commandEntity, requestId);

      // details must not be null
      RequestDetails details = auditlogRequestCache.getIfPresent(requestId);
      if (details != null) {
        HostRoleStatus calculatedStatus = calculateStatus(requestId, details.getNumberOfTasks());

        if (details.getLastStatus() != calculatedStatus) {
          RequestEntity request = requestDAO.findByPK(requestId);
          String context = request != null ? request.getRequestContext() : null;
          AuditEvent auditEvent = OperationStatusAuditEvent.builder()
            .withRequestId(String.valueOf(requestId))
            .withStatus(String.valueOf(calculatedStatus))
            .withRequestContext(context)
            .withUserName(details.getUserName())
            .withTimestamp(System.currentTimeMillis())
            .build();
          auditLogger.log(auditEvent);

          details.setLastStatus(calculatedStatus);
        }
      }
      logTask(commandEntity, requestId, lastTaskStatus);
    }
  }

  /**
   * Calculates summary status for the given request
   * @param requestId
   * @return
   */
  private HostRoleStatus calculateStatus(Long requestId, int numberOfTasks) {
    RequestDetails details = auditlogRequestCache.getIfPresent(requestId);
    if(details == null) {
      return HostRoleStatus.QUEUED;
    }
    Collection<HostRoleStatus> taskStatuses = details.getTaskStatuses();
    return CalculatedStatus.calculateSummaryStatusOfStage(CalculatedStatus.calculateStatusCounts(taskStatuses), numberOfTasks, false);
  }

  /**
   * Logs task status change
   * @param commandEntity
   * @param requestId
   * @param lastTaskStatus
   */
  private void logTask(HostRoleCommandEntity commandEntity, Long requestId, HostRoleStatus lastTaskStatus) {

    RequestDetails.Component component = new RequestDetails.Component(commandEntity.getRole(), commandEntity.getHostName());
    RequestDetails details = auditlogRequestCache.getIfPresent(requestId);
    if(details == null) {
      return;
    }

    HostRoleStatus cachedStatus = details.getTasks().get(component);

    if(lastTaskStatus == null || cachedStatus != lastTaskStatus ) {
      AuditEvent taskEvent = TaskStatusAuditEvent.builder()
        .withTaskId(String.valueOf(commandEntity.getTaskId()))
        .withHostName(commandEntity.getHostName())
        .withUserName(details.getUserName())
        .withOperation(commandEntity.getRoleCommand().toString() + " " + commandEntity.getRole().toString())
        .withDetails(commandEntity.getCommandDetail())
        .withStatus(commandEntity.getStatus().toString())
        .withRequestId(String.valueOf(requestId))
        .withTimestamp(System.currentTimeMillis())
        .build();

      auditLogger.log(taskEvent);
    }
  }

  /**
   * The purpose of this nested class is to store details about the request: lastStatus and the list of tasks
   */
  private static class RequestDetails {
    /**
     * Last summary status of the request
     */
    HostRoleStatus lastStatus = null;

    /**
     * The number of tasks that can be found in the request
     */
    int numberOfTasks = 0;

    /**
     * The user who issued the request
     */
    String userName;

    /**
     * Component and the status of the belonging task
     * Size of this container might be less than {@link RequestDetails#numberOfTasks}
     */
    Map<Component, HostRoleStatus> tasks = new HashMap<>();

    public HostRoleStatus getLastStatus() {
      return lastStatus;
    }

    public void setLastStatus(HostRoleStatus lastStatus) {
      this.lastStatus = lastStatus;
    }

    public int getNumberOfTasks() {
      return numberOfTasks;
    }

    public void setNumberOfTasks(int numberOfTasks) {
      this.numberOfTasks = numberOfTasks;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public Map<Component, HostRoleStatus> getTasks() {
      return tasks;
    }

    /**
     * Returns the list of statuses from {@link RequestDetails#tasks}
     * @return
     */
    public Collection<HostRoleStatus> getTaskStatuses() {
      return getTasks().values();
    }

    /**
     * This nested class is the key for the {@link RequestDetails#tasks} map
     */
    static class Component {
      /**
       * Component name
       */
      private final Role role;
      /**
       * Host name
       */
      private final String hostName;
      Component(Role role, String hostName) {
        this.role = role;
        this.hostName = hostName;
      }

      public Role getRole() {
        return role;
      }

      public String getHostName() {
        return hostName;
      }

      /**
       * Hash code generation
       * @return
       */
      @Override
      public final int hashCode() {
        int hash = 7;
        String roleStr = role == null ? "null" : role.toString();
        String hostNameStr = hostName == null ? "null" : hostName;
        String str = roleStr.concat(hostNameStr);
        for (int i = 0; i < str.length(); i++) {
          hash = hash*31 + str.charAt(i);
        }
        return hash;
      }

      /**
       * Two components are equal if their component name and host name are the same
       * @param other
       * @return
       */
      @Override
      public final boolean equals(final Object other) {
        if(other instanceof Component) {
          Component comp = (Component) other;
          return Objects.equals(comp.role, role) && Objects.equals(comp.hostName, hostName);
        }

        return false;
      }
    }
  }
}
