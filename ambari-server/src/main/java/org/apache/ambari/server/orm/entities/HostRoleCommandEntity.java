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

package org.apache.ambari.server.orm.entities;

import static org.apache.commons.lang.StringUtils.defaultString;

import java.util.Arrays;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.commons.lang.ArrayUtils;

@Table(name = "host_role_command")
@Entity
@TableGenerator(name = "host_role_command_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "host_role_command_id_seq"
    , initialValue = 1
    , allocationSize = 50
)

public class HostRoleCommandEntity {

  private static int MAX_COMMAND_DETAIL_LENGTH = 250;

  @Column(name = "task_id")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "host_role_command_id_generator")
  private Long taskId;

  @Column(name = "request_id", insertable = false, updatable = false, nullable = false)
  @Basic
  private Long requestId;

  @Column(name = "stage_id", insertable = false, updatable = false, nullable = false)
  @Basic
  private Long stageId;

  @Column(name = "host_name", insertable = false, updatable = false, nullable = false)
  @Basic
  private String hostName;

  @Column(name = "role")
  private String role;

  @Column(name = "event", length = 32000)
  @Basic
  @Lob
  private String event = "";

  @Column(name = "exitcode", nullable = false)
  @Basic
  private Integer exitcode = 0;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private HostRoleStatus status;

  @Column(name = "std_error")
  @Lob
  @Basic
  private byte[] stdError = new byte[0];

  @Column(name = "std_out")
  @Lob
  @Basic
  private byte[] stdOut = new byte[0];

  @Column(name = "output_log")
  @Basic
  private String outputLog = null;

  @Column(name = "error_log")
  @Basic
  private String errorLog = null;


  @Column(name = "structured_out")
  @Lob
  @Basic
  private byte[] structuredOut = new byte[0];

  @Basic
  @Column(name = "start_time", nullable = false)
  private Long startTime = -1L;

  @Basic
  @Column(name = "end_time", nullable = false)
  private Long endTime = -1L;

  @Basic
  @Column(name = "last_attempt_time", nullable = false)
  private Long lastAttemptTime = -1L;

  @Basic
  @Column(name = "attempt_count", nullable = false)
  private Short attemptCount = 0;

  // This is really command type as well as name
  @Column(name = "role_command")
  @Enumerated(EnumType.STRING)
  private RoleCommand roleCommand;

  // A readable description of the command
  @Column(name = "command_detail")
  @Basic
  private String commandDetail;

  // When command type id CUSTOM_COMMAND and CUSTOM_ACTION this is the name
  @Column(name = "custom_command_name")
  @Basic
  private String customCommandName;

  @OneToOne(mappedBy = "hostRoleCommand", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
  private ExecutionCommandEntity executionCommand;

  @ManyToOne(cascade = {CascadeType.MERGE})
  @JoinColumns({@JoinColumn(name = "request_id", referencedColumnName = "request_id", nullable = false), @JoinColumn(name = "stage_id", referencedColumnName = "stage_id", nullable = false)})
  private StageEntity stage;

  @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
  @JoinColumn(name = "host_name", referencedColumnName = "host_name", nullable = false)
  private HostEntity host;

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public Long getStageId() {
    return stageId;
  }

  public void setStageId(Long stageId) {
    this.stageId = stageId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public Role getRole() {
    return Role.valueOf(this.role);
  }

  public void setRole(Role role) {
    this.role = role.name();
  }

  public String getEvent() {
    return defaultString(event);
  }

  public void setEvent(String event) {
    this.event = event;
  }

  public Integer getExitcode() {
    return exitcode;
  }

  public void setExitcode(Integer exitcode) {
    this.exitcode = exitcode;
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  public byte[] getStdError() {
    return ArrayUtils.nullToEmpty(stdError);
  }

  public void setStdError(byte[] stdError) {
    this.stdError = stdError;
  }

  public byte[] getStdOut() {
    return ArrayUtils.nullToEmpty(stdOut);
  }

  public void setStdOut(byte[] stdOut) {
    this.stdOut = stdOut;
  }

  public String getOutputLog() { return outputLog; }

  public void setOutputLog(String outputLog) { this.outputLog = outputLog; }

  public String getErrorLog() { return errorLog; }

  public void setErrorLog(String errorLog) { this.errorLog = errorLog; }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Long getLastAttemptTime() {
    return lastAttemptTime;
  }

  public void setLastAttemptTime(Long lastAttemptTime) {
    this.lastAttemptTime = lastAttemptTime;
  }

  public Short getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(Short attemptCount) {
    this.attemptCount = attemptCount;
  }

  public RoleCommand getRoleCommand() {
    return roleCommand;
  }

  public void setRoleCommand(RoleCommand roleCommand) {
    this.roleCommand = roleCommand;
  }

  public byte[] getStructuredOut() {
    return structuredOut;
  }

  public void setStructuredOut(byte[] structuredOut) {
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
    String truncatedCommandDetail = commandDetail;
    if (commandDetail != null) {
      if (commandDetail.length() > MAX_COMMAND_DETAIL_LENGTH) {
        truncatedCommandDetail = commandDetail.substring(0, MAX_COMMAND_DETAIL_LENGTH) + "...";
      }
    }
    this.commandDetail = truncatedCommandDetail;
  }

  public String getCustomCommandName() {
    return customCommandName;
  }

  public void setCustomCommandName(String customCommandName) {
    this.customCommandName = customCommandName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostRoleCommandEntity that = (HostRoleCommandEntity) o;

    if (attemptCount != null ? !attemptCount.equals(that.attemptCount) : that.attemptCount != null) return false;
    if (event != null ? !event.equals(that.event) : that.event != null) return false;
    if (exitcode != null ? !exitcode.equals(that.exitcode) : that.exitcode != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (lastAttemptTime != null ? !lastAttemptTime.equals(that.lastAttemptTime) : that.lastAttemptTime != null)
      return false;
    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;
    if (role != null ? !role.equals(that.role) : that.role != null) return false;
    if (stageId != null ? !stageId.equals(that.stageId) : that.stageId != null) return false;
    if (startTime != null ? !startTime.equals(that.startTime) : that.startTime != null) return false;
    if (status != null ? !status.equals(that.status) : that.status != null) return false;
    if (stdError != null ? !Arrays.equals(stdError, that.stdError) : that.stdError != null) return false;
    if (stdOut != null ? !Arrays.equals(stdOut, that.stdOut) : that.stdOut != null) return false;
    if (outputLog != null ? !outputLog.equals(that.outputLog) : that.outputLog != null) return false;
    if (errorLog != null ? !errorLog.equals(that.errorLog) : that.errorLog != null) return false;
    if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
    if (structuredOut != null ? !Arrays.equals(structuredOut, that.structuredOut) : that.structuredOut != null) return false;
    if (endTime != null ? !endTime.equals(that.endTime) : that.endTime != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = taskId != null ? taskId.hashCode() : 0;
    result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
    result = 31 * result + (stageId != null ? stageId.hashCode() : 0);
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (role != null ? role.hashCode() : 0);
    result = 31 * result + (event != null ? event.hashCode() : 0);
    result = 31 * result + (exitcode != null ? exitcode.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (stdError != null ? Arrays.hashCode(stdError) : 0);
    result = 31 * result + (stdOut != null ? Arrays.hashCode(stdOut) : 0);
    result = 31 * result + (outputLog != null ? outputLog.hashCode() : 0);
    result = 31 * result + (errorLog != null ? errorLog.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (lastAttemptTime != null ? lastAttemptTime.hashCode() : 0);
    result = 31 * result + (attemptCount != null ? attemptCount.hashCode() : 0);
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    result = 31 * result + (structuredOut != null ? Arrays.hashCode(structuredOut) : 0);
    return result;
  }

  public ExecutionCommandEntity getExecutionCommand() {
    return executionCommand;
  }

  public void setExecutionCommand(ExecutionCommandEntity executionCommandsByTaskId) {
    this.executionCommand = executionCommandsByTaskId;
  }

  public StageEntity getStage() {
    return stage;
  }

  public void setStage(StageEntity stage) {
    this.stage = stage;
  }

  public HostEntity getHost() {
    return host;
  }

  public void setHost(HostEntity host) {
    this.host = host;
  }
}
