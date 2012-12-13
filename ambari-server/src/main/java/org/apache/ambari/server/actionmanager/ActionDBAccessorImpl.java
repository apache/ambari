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

import java.util.*;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RoleSuccessCriteriaDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RoleSuccessCriteriaEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ActionDBAccessorImpl implements ActionDBAccessor {
  private static final Logger LOG = LoggerFactory.getLogger(ActionDBAccessorImpl.class);

  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private HostDAO hostDAO;
  @Inject
  private StageDAO stageDAO;
  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;
  @Inject
  private ExecutionCommandDAO executionCommandDAO;
  @Inject
  private RoleSuccessCriteriaDAO roleSuccessCriteriaDAO;
  @Inject
  private StageFactory stageFactory;
  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;
  @Inject
  private Clusters clusters;

  private final long requestId;

  @Inject
  public ActionDBAccessorImpl(Injector injector) {
    injector.injectMembers(this);
    requestId = stageDAO.getLastRequestId();


  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#getAction(java.lang.String)
   */
  @Override
  public Stage getAction(String actionId) {
    return stageFactory.createExisting(actionId);
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#getAllStages(java.lang.String)
   */
  @Override
  public List<Stage> getAllStages(long requestId) {
    List<Stage> stages = new ArrayList<Stage>();
    for (StageEntity stageEntity : stageDAO.findByRequestId(requestId)) {
      stages.add(stageFactory.createExisting(stageEntity));
    }
    return stages;
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#abortOperation(long)
   */
  @Override
  public void abortOperation(long requestId) {
    Collection<HostRoleStatus> sourceStatuses =
        Arrays.asList(HostRoleStatus.QUEUED, HostRoleStatus.IN_PROGRESS,
            HostRoleStatus.PENDING);
    int result = hostRoleCommandDAO.updateStatusByRequestId(requestId,
        HostRoleStatus.ABORTED, sourceStatuses);
    LOG.info("Aborted {} commands " + result);
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#timeoutHostRole(long, long, org.apache.ambari.server.Role)
   */
  @Override
  @Transactional
  public void timeoutHostRole(String host, long requestId, long stageId,
      Role role) {
    List<HostRoleCommandEntity> commands =
        hostRoleCommandDAO.findByHostRole(host, requestId, stageId, role);
    for (HostRoleCommandEntity command : commands) {
      command.setStatus(HostRoleStatus.TIMEDOUT);
      hostRoleCommandDAO.merge(command);
    }
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#getPendingStages()
   */
  @Override
  public List<Stage> getStagesInProgress() {
    List<Stage> stages = new ArrayList<Stage>();
    List<HostRoleStatus> statuses =
        Arrays.asList(HostRoleStatus.QUEUED, HostRoleStatus.IN_PROGRESS,
            HostRoleStatus.PENDING);
    for (StageEntity stageEntity : stageDAO.findByCommandStatuses(statuses)) {
      stages.add(stageFactory.createExisting(stageEntity));
    }
    return stages;
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#persistActions(java.util.List)
   */
  @Override
  @Transactional
  public void persistActions(List<Stage> stages) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding stages to DB, stageCount=" + stages.size());
    }
    for (Stage stage : stages) {
      StageEntity stageEntity = stage.constructNewPersistenceEntity();
      Cluster cluster;
      try {
        cluster = clusters.getCluster(stage.getClusterName());
      } catch (AmbariException e) {
        throw new RuntimeException(e);
      }
      ClusterEntity clusterEntity = clusterDAO.findById(cluster.getClusterId());

      stageEntity.setCluster(clusterEntity);
      clusterEntity.getStages().add(stageEntity);

      for (HostRoleCommand hostRoleCommand : stage.getOrderedHostRoleCommands()) {
        HostRoleCommandEntity hostRoleCommandEntity = hostRoleCommand.constructNewPersistenceEntity();
        stageEntity.getHostRoleCommands().add(hostRoleCommandEntity);
        hostRoleCommandEntity.setStage(stageEntity);

        HostEntity hostEntity = hostDAO.findByName(hostRoleCommandEntity.getHostName());
        if (hostEntity == null) {
          LOG.error("Host {} doesn't exists in database" + hostRoleCommandEntity.getHostName());
          throw new RuntimeException("Host '"+hostRoleCommandEntity.getHostName()+"' doesn't exists in database");
        }
        hostEntity.getHostRoleCommandEntities().add(hostRoleCommandEntity);
        hostRoleCommandEntity.setHost(hostEntity);
        hostRoleCommandDAO.create(hostRoleCommandEntity);

        assert hostRoleCommandEntity.getTaskId() != null;

        hostRoleCommand.setTaskId(hostRoleCommandEntity.getTaskId());
        ExecutionCommandEntity executionCommandEntity = hostRoleCommand.constructExecutionCommandEntity();
        executionCommandEntity.setHostRoleCommand(hostRoleCommandEntity);
        hostRoleCommandEntity.setExecutionCommand(executionCommandEntity);

        executionCommandDAO.create(hostRoleCommandEntity.getExecutionCommand());
        hostRoleCommandDAO.merge(hostRoleCommandEntity);
        hostDAO.merge(hostEntity);
      }

      for (RoleSuccessCriteriaEntity roleSuccessCriteriaEntity : stageEntity.getRoleSuccessCriterias()) {
        roleSuccessCriteriaDAO.create(roleSuccessCriteriaEntity);
      }

      stageDAO.create(stageEntity);
      clusterDAO.merge(clusterEntity);
    }
  }

  @Override
  @Transactional
  public void updateHostRoleState(String hostname, long requestId,
      long stageId, String role, CommandReport report) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Update HostRoleState: "
          + "HostName " + hostname + " requestId " + requestId + " stageId "
          + stageId + " role " + role + " report " + report);
    }
    List<HostRoleCommandEntity> commands = hostRoleCommandDAO.findByHostRole(
        hostname, requestId, stageId, Role.valueOf(role));
    for (HostRoleCommandEntity command : commands) {
      command.setStatus(HostRoleStatus.valueOf(report.getStatus()));
      command.setStdOut(report.getStdOut().getBytes());
      command.setStdError(report.getStdErr().getBytes());
      command.setExitcode(report.getExitCode());
      hostRoleCommandDAO.merge(command);
    }
  }

  @Override
  public void abortHostRole(String host, long requestId, long stageId, Role role) {
    CommandReport report = new CommandReport();
    report.setExitCode(999);
    report.setStdErr("Host Role in invalid state");
    report.setStdOut("");
    report.setStatus("ABORTED");
    updateHostRoleState(host, requestId, stageId, role.toString(), report);
  }

  @Override
  public long getLastPersistedRequestIdWhenInitialized() {
    return requestId;
  }

  @Override
  @Transactional
  public void hostRoleScheduled(Stage s, String hostname, String roleStr) {
    HostRoleCommand hostRoleCommand = s.getHostRoleCommand(hostname, roleStr);
    HostRoleCommandEntity entity = hostRoleCommandDAO.findByPK(hostRoleCommand.getTaskId());
    if (entity != null) {
      entity.setStartTime(hostRoleCommand.getStartTime());
      entity.setLastAttemptTime(hostRoleCommand.getLastAttemptTime());
      entity.setStatus(hostRoleCommand.getStatus());
      entity.setAttemptCount(hostRoleCommand.getAttemptCount());
      hostRoleCommandDAO.merge(entity);
    } else {
      throw new RuntimeException("HostRoleCommand is not persisted, cannot update:\n" + hostRoleCommand);
    }

  }

  @Override
  public List<HostRoleCommand> getRequestTasks(long requestId) {
    List<HostRoleCommand> tasks = new ArrayList<HostRoleCommand>();
    for (HostRoleCommandEntity hostRoleCommandEntity : hostRoleCommandDAO.findByRequest(requestId)) {
      tasks.add(hostRoleCommandFactory.createExisting(hostRoleCommandEntity));
    }
    return tasks;
  }

  @Override
  public List<HostRoleCommand> getAllTasksByRequestIds(Collection<Long> requestIds) {
    if (requestIds.isEmpty()) {
      return Collections.emptyList();
    }
    List<HostRoleCommand> tasks = new ArrayList<HostRoleCommand>();
    for (HostRoleCommandEntity hostRoleCommandEntity : hostRoleCommandDAO.findByRequestIds(requestIds)) {
      tasks.add(hostRoleCommandFactory.createExisting(hostRoleCommandEntity));
    }
    return tasks;
  }

  @Override
  public List<HostRoleCommand> getTasksByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds) {
    if (!requestIds.isEmpty() && !taskIds.isEmpty()) {
      List<HostRoleCommand> tasks = new ArrayList<HostRoleCommand>();
      for (HostRoleCommandEntity hostRoleCommandEntity : hostRoleCommandDAO.findByRequestAndTaskIds(requestIds, taskIds)) {
        tasks.add(hostRoleCommandFactory.createExisting(hostRoleCommandEntity));
      }
      return tasks;
    }else if (requestIds.isEmpty()) {
      return getTasks(taskIds);
    }else if (taskIds.isEmpty()) {
      return getAllTasksByRequestIds(requestIds);
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public List<HostRoleCommand> getTasks(Collection<Long> taskIds) {
    if (taskIds.isEmpty()) {
      return Collections.emptyList();
    }
    List<HostRoleCommand> commands = new ArrayList<HostRoleCommand>();
    for (HostRoleCommandEntity commandEntity : hostRoleCommandDAO.findByPKs(taskIds)) {
      commands.add(hostRoleCommandFactory.createExisting(commandEntity));
    }
    return commands;
  }

  @Override
  public List<Stage> getStagesByHostRoleStatus(Set<HostRoleStatus> statuses) {
    List<Stage> stages = new ArrayList<Stage>();
    for (StageEntity stageEntity : stageDAO.findByCommandStatuses(statuses)) {
      stages.add(stageFactory.createExisting(stageEntity));
    }
    return stages;
  }

  @Override
  public List<Long> getRequests() {
    return hostRoleCommandDAO.getRequests();
  }

  public HostRoleCommand getTask(long taskId) {
    HostRoleCommandEntity commandEntity = hostRoleCommandDAO.findByPK((int)taskId);
    if (commandEntity == null) {
      return null;
    }
    return hostRoleCommandFactory.createExisting(commandEntity);
  }

  @Override
  public List<Long> getRequestsByStatus(RequestStatus status) {
    boolean match = true;
    Set<HostRoleStatus> statuses = new HashSet<HostRoleStatus>();
    if (status == RequestStatus.IN_PROGRESS) {
      statuses.addAll( Arrays.asList(HostRoleStatus.PENDING,
          HostRoleStatus.IN_PROGRESS, HostRoleStatus.QUEUED));
    } else if (status == RequestStatus.COMPLETED) {
      match = false;
      statuses.addAll( Arrays.asList(HostRoleStatus.PENDING,
          HostRoleStatus.IN_PROGRESS, HostRoleStatus.QUEUED,
          HostRoleStatus.ABORTED, HostRoleStatus.FAILED,
          HostRoleStatus.FAILED, HostRoleStatus.TIMEDOUT));
    } else if (status == RequestStatus.FAILED) {
      statuses.addAll( Arrays.asList(HostRoleStatus.ABORTED,
          HostRoleStatus.FAILED, HostRoleStatus.FAILED,
          HostRoleStatus.TIMEDOUT));
    }
    return hostRoleCommandDAO.getRequestsByTaskStatus(statuses, match);
  }
}
