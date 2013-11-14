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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

/**
 * This class encapsulates the information for an task on a host for a
 * particular role which action manager needs. It doesn't capture actual
 * command and parameters, but just the stuff enough for action manager to
 * track the request.
 * For the actual command refer {@link HostAction#commandToHost}
 */
public class HostRoleCommand {
  private static final Logger log = LoggerFactory.getLogger(HostRoleCommand.class);

  private long taskId = -1;
  private long stageId = -1;
  private long requestId = -1;
  private String hostName;
  private final Role role;
  private HostRoleStatus status = HostRoleStatus.PENDING;
  private String stdout = "";
  private String stderr = "";
  private int exitCode = 999; //Default is unknown
  private final ServiceComponentHostEventWrapper event;
  private long startTime = -1;
  private long lastAttemptTime = -1;
  private short attemptCount = 0;
  private RoleCommand roleCommand;

  private ExecutionCommandWrapper executionCommandWrapper;

  private ExecutionCommandDAO executionCommandDAO;

  public HostRoleCommand(String host, Role role,
      ServiceComponentHostEvent event, RoleCommand command) {
    this.hostName = host;
    this.role = role;
    this.event = new ServiceComponentHostEventWrapper(event);
    this.roleCommand = command;
  }

  @AssistedInject
  public HostRoleCommand(@Assisted HostRoleCommandEntity hostRoleCommandEntity, Injector injector) {
    taskId = hostRoleCommandEntity.getTaskId();
    stageId = hostRoleCommandEntity.getStage().getStageId();
    requestId = hostRoleCommandEntity.getStage().getRequestId();
    this.hostName = hostRoleCommandEntity.getHostName();
    role = hostRoleCommandEntity.getRole();
    status = hostRoleCommandEntity.getStatus();
    stdout = hostRoleCommandEntity.getStdOut() != null ? new String(hostRoleCommandEntity.getStdOut()) : "";
    stderr = hostRoleCommandEntity.getStdError() != null ? new String(hostRoleCommandEntity.getStdError()) : "";
    exitCode = hostRoleCommandEntity.getExitcode();
    startTime = hostRoleCommandEntity.getStartTime();
    lastAttemptTime = hostRoleCommandEntity.getLastAttemptTime();
    attemptCount = hostRoleCommandEntity.getAttemptCount();
    roleCommand = hostRoleCommandEntity.getRoleCommand();
    event = new ServiceComponentHostEventWrapper(hostRoleCommandEntity.getEvent());
    //make use of lazy loading

    executionCommandDAO = injector.getInstance(ExecutionCommandDAO.class);
  }

  HostRoleCommandEntity constructNewPersistenceEntity() {
    HostRoleCommandEntity hostRoleCommandEntity = new HostRoleCommandEntity();
    hostRoleCommandEntity.setHostName(hostName);
    hostRoleCommandEntity.setRole(role);
    hostRoleCommandEntity.setStatus(status);
    hostRoleCommandEntity.setStdError(stderr.getBytes());
    hostRoleCommandEntity.setExitcode(exitCode);
    hostRoleCommandEntity.setStdOut(stdout.getBytes());
    hostRoleCommandEntity.setStartTime(startTime);
    hostRoleCommandEntity.setLastAttemptTime(lastAttemptTime);
    hostRoleCommandEntity.setAttemptCount(attemptCount);
    hostRoleCommandEntity.setRoleCommand(roleCommand);

    hostRoleCommandEntity.setEvent(event.getEventJson());

    return hostRoleCommandEntity;
  }

  ExecutionCommandEntity constructExecutionCommandEntity() {
    ExecutionCommandEntity executionCommandEntity = new ExecutionCommandEntity();
    executionCommandEntity.setCommand(executionCommandWrapper.getJson().getBytes());
    return executionCommandEntity;
  }

  public long getTaskId() {
    return taskId;
  }

  public void setTaskId(long taskId) {
    if (this.taskId != -1) {
      throw new RuntimeException("Attempt to set taskId again, not allowed");
    }
    this.taskId = taskId;
    executionCommandWrapper.getExecutionCommand().setTaskId(taskId);
    //Need to invalidate json because taskId is updated.
    executionCommandWrapper.invalidateJson();
  }

  public String getHostName() {
    return hostName;
  }

  public Role getRole() {
    return role;
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public ServiceComponentHostEventWrapper getEvent() {
    return event;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  public String getStdout() {
    return stdout;
  }

  public void setStdout(String stdout) {
    this.stdout = stdout;
  }

  public String getStderr() {
    return stderr;
  }

  public void setStderr(String stderr) {
    this.stderr = stderr;
  }

  public int getExitCode() {
    return exitCode;
  }

  public void setExitCode(int exitCode) {
    this.exitCode = exitCode;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getLastAttemptTime() {
    return lastAttemptTime;
  }

  public void setLastAttemptTime(long lastAttemptTime) {
    this.lastAttemptTime = lastAttemptTime;
  }

  public short getAttemptCount() {
    return attemptCount;
  }

  public void incrementAttemptCount() {
    this.attemptCount++;
  }

  public ExecutionCommandWrapper getExecutionCommandWrapper() {
    if (taskId != -1 && executionCommandWrapper == null) {
      ExecutionCommandEntity commandEntity = executionCommandDAO.findByPK(taskId);
      if (commandEntity == null) {
        throw new RuntimeException("Invalid DB state, broken one-to-one relation for taskId=" + taskId);
      }
      executionCommandWrapper = new ExecutionCommandWrapper(new String(
          commandEntity.getCommand()
      ));
    }

    return executionCommandWrapper;
  }

  public void setExecutionCommandWrapper(ExecutionCommandWrapper executionCommandWrapper) {
    this.executionCommandWrapper = executionCommandWrapper;
  }

  public RoleCommand getRoleCommand() {
    return roleCommand;
  }

  public void setRoleCommand(RoleCommand roleCommand) {
    this.roleCommand = roleCommand;
  }

  public long getStageId() {
    return stageId;
  }

  public long getRequestId() {
    return requestId;
  }

  @Override
  public int hashCode() {
    return (hostName.toString() + role.toString() + roleCommand.toString())
        .hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof HostRoleCommand)) {
      return false;
    }
    HostRoleCommand o = (HostRoleCommand) other;
    return (this.role.equals(o.role) && this.hostName.equals(o.hostName) && this.roleCommand
        .equals(o.roleCommand));
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("HostRoleCommand State:\n");
    builder.append("  TaskId: ").append(taskId).append("\n");
    builder.append("  Role: ").append(role).append("\n");
    builder.append("  Status: ").append(status).append("\n");
    builder.append("  Event: ").append(event).append("\n");
    builder.append("  stdout: ").append(stdout).append("\n");
    builder.append("  stderr: ").append(stderr).append("\n");
    builder.append("  exitcode: ").append(exitCode).append("\n");
    builder.append("  Start time: ").append(startTime).append("\n");
    builder.append("  Last attempt time: ").append(lastAttemptTime).append("\n");
    builder.append("  attempt count: ").append(attemptCount).append("\n");
    return builder.toString();
  }
}
