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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.actionmanager.HostRoleCommand;

public class ShortTaskStatus {
  protected long taskId;
  protected long stageId;
  protected String hostName;
  protected String role;
  protected String command;
  protected String status;

  public ShortTaskStatus() {
  }

  public ShortTaskStatus(int taskId, long stageId, String hostName, String role, String command, String status) {
    this.taskId = taskId;
    this.stageId = stageId;
    this.hostName = hostName;
    this.role = role;
    this.command = command;
    this.status = status;
  }

  public ShortTaskStatus(HostRoleCommand hostRoleCommand) {
    this.taskId = hostRoleCommand.getTaskId();
    this.stageId = hostRoleCommand.getStageId();
    this.command = hostRoleCommand.getRoleCommand().toString();
    this.hostName = hostRoleCommand.getHostName();
    this.role = hostRoleCommand.getRole().toString();
    this.status = hostRoleCommand.getStatus().toString();
  }

  public long getTaskId() {
    return taskId;
  }

  public void setTaskId(long taskId) {
    this.taskId = taskId;
  }

  public long getStageId() {
    return stageId;
  }

  public void setStageId(long stageId) {
    this.stageId = stageId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ShortTaskStatusDump "
        + ", stageId=" + stageId
        + ", taskId=" + taskId
        + ", hostname=" + hostName
        + ", role=" + role
        + ", command=" + command
        + ", status=" + status);
    return sb.toString();
  }

}
