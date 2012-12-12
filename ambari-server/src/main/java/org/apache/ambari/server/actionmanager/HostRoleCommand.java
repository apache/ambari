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
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;

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
  private String hostName;
  private final Role role;
  private HostRoleStatus status = HostRoleStatus.PENDING;
  private String stdout = "";
  private String stderr = "";
  private int exitCode = 999; //Default is unknown
  private final ServiceComponentHostEvent event;
  private long startTime = -1;
  private long lastAttemptTime = -1;
  private short attemptCount = 0;

  private ExecutionCommand executionCommand;

  public HostRoleCommand(String host, Role role,
                         ServiceComponentHostEvent event) {
    this.hostName = host;
    this.role = role;
    this.event = event;
  }

  @AssistedInject
  public HostRoleCommand(@Assisted HostRoleCommandEntity hostRoleCommandEntity, Injector injector) {
    taskId = hostRoleCommandEntity.getTaskId();
    stageId = hostRoleCommandEntity.getStage().getStageId();
    this.hostName = hostRoleCommandEntity.getHostName();
    role = hostRoleCommandEntity.getRole();
    status = hostRoleCommandEntity.getStatus();
    stdout = hostRoleCommandEntity.getStdOut();
    stderr = hostRoleCommandEntity.getStdError();
    exitCode = hostRoleCommandEntity.getExitcode();
    startTime = hostRoleCommandEntity.getStartTime();
    lastAttemptTime = hostRoleCommandEntity.getLastAttemptTime();
    attemptCount = hostRoleCommandEntity.getAttemptCount();

    try {
      log.info(hostRoleCommandEntity.getEvent());
      event = StageUtils.fromJson(hostRoleCommandEntity.getEvent(), ServiceComponentHostEvent.class);
      executionCommand = StageUtils.stringToExecutionCommand(hostRoleCommandEntity.getExecutionCommand().getCommand());
    } catch (IOException e) {
      throw new RuntimeException("Unable to parse JSON string", e);
    }

  }

  HostRoleCommandEntity constructNewPersistenceEntity() {
    HostRoleCommandEntity hostRoleCommandEntity = new HostRoleCommandEntity();
    hostRoleCommandEntity.setHostName(hostName);
    hostRoleCommandEntity.setRole(role);
    hostRoleCommandEntity.setStatus(status);
    hostRoleCommandEntity.setStdError(stderr);
    hostRoleCommandEntity.setExitcode(exitCode);
    hostRoleCommandEntity.setStdOut(stdout);
    hostRoleCommandEntity.setStartTime(startTime);
    hostRoleCommandEntity.setLastAttemptTime(lastAttemptTime);
    hostRoleCommandEntity.setAttemptCount(attemptCount);

    try {
      hostRoleCommandEntity.setEvent(StageUtils.jaxbToString(event));
      ExecutionCommandEntity executionCommandEntity = new ExecutionCommandEntity();
      executionCommandEntity.setCommand(StageUtils.jaxbToString(executionCommand));
      executionCommandEntity.setHostRoleCommand(hostRoleCommandEntity);
      hostRoleCommandEntity.setExecutionCommand(executionCommandEntity);
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return hostRoleCommandEntity;
  }

  ExecutionCommandEntity constructExecutionCommandEntity(){
    try {
      ExecutionCommandEntity executionCommandEntity = new ExecutionCommandEntity();
      executionCommandEntity.setCommand(StageUtils.jaxbToString(executionCommand));
      return executionCommandEntity;
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public long getTaskId() {
    return taskId;
  }

  public void setTaskId(long taskId) {
      this.taskId = taskId;
      executionCommand.setTaskId(taskId);
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

  public ServiceComponentHostEvent getEvent() {
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

  public ExecutionCommand getExecutionCommand() {
    return executionCommand;
  }

  public void setExecutionCommand(ExecutionCommand executionCommand) {
    this.executionCommand = executionCommand;
  }

  public long getStageId() {
    return stageId;
  }

  public void setStageId(long stageId) {
    this.stageId = stageId;
  }

  @Override
  public int hashCode() {
    return role.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof HostRoleCommand)) {
      return false;
    }
    HostRoleCommand o = (HostRoleCommand) other;
    return this.role.equals(o.role);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("HostRoleCommand State:\n");
    builder.append("  TaskId: " + taskId + "\n");
    builder.append("  Role: " + role + "\n");
    builder.append("  Status: " + status + "\n");
    builder.append("  Event: " + event + "\n");
    builder.append("  stdout: " + stdout + "\n");
    builder.append("  stderr: " + stderr + "\n");
    builder.append("  exitcode: " + exitCode + "\n");
    builder.append("  Start time: " + startTime + "\n");
    builder.append("  Last attempt time: " + lastAttemptTime + "\n");
    builder.append("  attempt count: " + attemptCount + "\n");
    return builder.toString();
  }
}
