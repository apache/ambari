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

import com.google.inject.Injector;
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

/**
 * This class encapsulates the information for an task on a host for a
 * particular role which action manager needs. It doesn't capture actual
 * command and parameters, but just the stuff enough for action manager to
 * track the request.
 */
public class HostRoleCommand {
  private static final Logger log = LoggerFactory.getLogger(HostRoleCommand.class);
  private final Role role;
  private final ServiceComponentHostEventWrapper event;
  private long taskId = -1;
  private long stageId = -1;
  private long requestId = -1;
  private String hostName;
  private HostRoleStatus status = HostRoleStatus.PENDING;
  private String stdout = "";
  private String stderr = "";
  public String outputLog = null;
  public String errorLog = null;
  private String structuredOut = "";
  private int exitCode = 999; //Default is unknown
  private long startTime = -1;
  private long endTime = -1;
  private long lastAttemptTime = -1;
  private short attemptCount = 0;
  private RoleCommand roleCommand;
  private String commandDetail;
  private String customCommandName;
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
    outputLog = hostRoleCommandEntity.getOutputLog();
    errorLog = hostRoleCommandEntity.getErrorLog();
    structuredOut = hostRoleCommandEntity.getStructuredOut() != null ? new String(hostRoleCommandEntity.getStructuredOut()) : "";
    exitCode = hostRoleCommandEntity.getExitcode();
    startTime = hostRoleCommandEntity.getStartTime();
    endTime = hostRoleCommandEntity.getEndTime() != null ? hostRoleCommandEntity.getEndTime() : -1L;
    lastAttemptTime = hostRoleCommandEntity.getLastAttemptTime();
    attemptCount = hostRoleCommandEntity.getAttemptCount();
    roleCommand = hostRoleCommandEntity.getRoleCommand();
    event = new ServiceComponentHostEventWrapper(hostRoleCommandEntity.getEvent());
    commandDetail = hostRoleCommandEntity.getCommandDetail();
    customCommandName = hostRoleCommandEntity.getCustomCommandName();
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
    hostRoleCommandEntity.setStructuredOut(structuredOut.getBytes());
    hostRoleCommandEntity.setStartTime(startTime);
    hostRoleCommandEntity.setEndTime(endTime);
    hostRoleCommandEntity.setLastAttemptTime(lastAttemptTime);
    hostRoleCommandEntity.setAttemptCount(attemptCount);
    hostRoleCommandEntity.setRoleCommand(roleCommand);
    hostRoleCommandEntity.setCommandDetail(commandDetail);
    hostRoleCommandEntity.setCustomCommandName(customCommandName);

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

  public String getCommandDetail() {
    return commandDetail;
  }

  public void setCommandDetail(String commandDetail) {
    this.commandDetail = commandDetail;
  }

  public String getCustomCommandName() {
    return customCommandName;
  }

  public void setCustomCommandName(String customCommandName) {
    this.customCommandName = customCommandName;
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  public ServiceComponentHostEventWrapper getEvent() {
    return event;
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

  public String getOutputLog() { return outputLog; }

  public void setOutputLog(String outputLog)  {
    this.outputLog = outputLog;
  }

  public String getErrorLog() { return errorLog; }

  public void setErrorLog(String errorLog) {
      this.errorLog = errorLog;
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

  public String getStructuredOut() {
    return structuredOut;
  }

  public void setStructuredOut(String structuredOut) {
    this.structuredOut = structuredOut;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
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
    builder.append("  Output log: ").append(outputLog).append("\n");
    builder.append("  Error log: ").append(errorLog).append("\n");
    builder.append("  stdout: ").append(stdout).append("\n");
    builder.append("  stderr: ").append(stderr).append("\n");
    builder.append("  exitcode: ").append(exitCode).append("\n");
    builder.append("  Start time: ").append(startTime).append("\n");
    builder.append("  Last attempt time: ").append(lastAttemptTime).append("\n");
    builder.append("  attempt count: ").append(attemptCount).append("\n");
    return builder.toString();
  }
}
