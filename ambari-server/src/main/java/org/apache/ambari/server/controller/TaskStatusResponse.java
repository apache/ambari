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

public class TaskStatusResponse extends ShortTaskStatus {
  private long requestId;
  private int exitCode;
  private String stderr;
  private String stdout;
  private String structuredOut;
  private String commandDetail;
  private long startTime;
  private Long endTime;
  private short attemptCount;

  public TaskStatusResponse() {
  }

  public TaskStatusResponse(long requestId,
                            int taskId, long stageId, String hostName, String role, String command, String status,
                            int exitCode, String stderr, String stdout, long startTime, short attemptCount,
                            String commandDetail, String customCommandName, String outputLog, String errorLog) {
    super(taskId, stageId, hostName, role, command, status, customCommandName, outputLog, errorLog);
    this.requestId = requestId;
    this.exitCode = exitCode;
    this.stderr = stderr;
    this.stdout = stdout;
    this.startTime = startTime;
    this.attemptCount = attemptCount;
    this.commandDetail = commandDetail;
  }

  public TaskStatusResponse(HostRoleCommand hostRoleCommand) {
    super(hostRoleCommand);
    this.requestId = hostRoleCommand.getRequestId();
    this.exitCode = hostRoleCommand.getExitCode();
    this.stderr = hostRoleCommand.getStderr();
    this.stdout = hostRoleCommand.getStdout();
    this.startTime = hostRoleCommand.getStartTime();
    this.attemptCount = hostRoleCommand.getAttemptCount();
    this.structuredOut = hostRoleCommand.getStructuredOut();
    this.endTime = hostRoleCommand.getEndTime();
    this.commandDetail = hostRoleCommand.getCommandDetail();
  }

  public long getRequestId() {
    return requestId;
  }

  public void setRequestId(long requestId) {
    this.requestId = requestId;
  }

  public int getExitCode() {
    return exitCode;
  }

  public void setExitCode(int exitCode) {
    this.exitCode = exitCode;
  }

  public String getStderr() {
    return stderr;
  }

  public void setStderr(String stderr) {
    this.stderr = stderr;
  }

  public String getStdout() {
    return stdout;
  }

  public void setStdout(String stdout) {
    this.stdout = stdout;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public short getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(short attemptCount) {
    this.attemptCount = attemptCount;
  }

  public String getStructuredOut() {
    return structuredOut;
  }

  public void setStructuredOut(String structuredOut) {
    this.structuredOut = structuredOut;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public String getCommandDetail() {
    return commandDetail;
  }

  public void setCommandDetail(String commandDetail) {
    this.commandDetail = commandDetail;
  }

  @Override
  public String toString() {
      return super.toString();
  }
}
