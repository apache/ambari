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

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;

import javax.persistence.*;

@Table(name = "host_role_command", schema = "ambari", catalog = "")
@Entity
@Cacheable(false)
@SequenceGenerator(name = "ambari.host_role_command_task_id_seq", allocationSize = 1)
public class HostRoleCommandEntity {
  private Long taskId;

  @Column(name = "task_id")
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ambari.host_role_command_task_id_seq")
  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  private Long requestId;

  @Column(name = "request_id", insertable = false, updatable = false, nullable = false)
  @Basic
  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  private Long stageId;

  @Column(name = "stage_id", insertable = false, updatable = false, nullable = false)
  @Basic
  public Long getStageId() {
    return stageId;
  }

  public void setStageId(Long stageId) {
    this.stageId = stageId;
  }

  private String hostName;

  @Column(name = "host_name", insertable = false, updatable = false, nullable = false)
  @Basic
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  private Role role;

  @Column(name = "role")
  @Enumerated(EnumType.STRING)
  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  private String event = "";

  @Column(name = "event", nullable = false, length = 32000)
  @Basic
  public String getEvent() {
    return event;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  private Integer exitcode = 0;

  @Column(name = "exitcode", nullable = false)
  @Basic
  public Integer getExitcode() {
    return exitcode;
  }

  public void setExitcode(Integer exitcode) {
    this.exitcode = exitcode;
  }

  private HostRoleStatus status;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  public HostRoleStatus getStatus() {
    return status;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  private byte[] stdError = new byte[0];

  @Column(name = "std_error", nullable = false)
  @Lob
  @Basic
  public byte[] getStdError() {
    return stdError;
  }

  public void setStdError(byte[] stdError) {
    this.stdError = stdError;
  }

  private byte[] stdOut = new byte[0];

  @Column(name = "std_out", nullable = false)
  @Lob
  @Basic
  public byte[] getStdOut() {
    return stdOut;
  }

  public void setStdOut(byte[] stdOut) {
    this.stdOut = stdOut;
  }

  private Long startTime = -1L;

  @Column(name = "start_time", nullable = false)
  @Basic
  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  private Long lastAttemptTime = -1L;

  @Column(name = "last_attempt_time", nullable = false)
  @Basic
  public Long getLastAttemptTime() {
    return lastAttemptTime;
  }

  public void setLastAttemptTime(Long lastAttemptTime) {
    this.lastAttemptTime = lastAttemptTime;
  }

  private Short attemptCount = 0;

  @Column(name = "attempt_count", nullable = false)
  @Basic
  public Short getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(Short attemptCount) {
    this.attemptCount = attemptCount;
  }

  private RoleCommand roleCommand;

  @Column(name = "role_command")
  @Enumerated(EnumType.STRING)
  public RoleCommand getRoleCommand() {
    return roleCommand;
  }

  public void setRoleCommand(RoleCommand roleCommand) {
    this.roleCommand = roleCommand;
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
    if (stdError != null ? !stdError.equals(that.stdError) : that.stdError != null) return false;
    if (stdOut != null ? !stdOut.equals(that.stdOut) : that.stdOut != null) return false;
    if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;

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
    result = 31 * result + (stdError != null ? stdError.hashCode() : 0);
    result = 31 * result + (stdOut != null ? stdOut.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (lastAttemptTime != null ? lastAttemptTime.hashCode() : 0);
    result = 31 * result + (attemptCount != null ? attemptCount.hashCode() : 0);
    return result;
  }

  private ExecutionCommandEntity executionCommand;

  @OneToOne(mappedBy = "hostRoleCommand")
  public ExecutionCommandEntity getExecutionCommand() {
    return executionCommand;
  }

  public void setExecutionCommand(ExecutionCommandEntity executionCommandsByTaskId) {
    this.executionCommand = executionCommandsByTaskId;
  }

  private StageEntity stage;

  @ManyToOne
  @JoinColumns({@JoinColumn(name = "request_id", referencedColumnName = "request_id", nullable = false), @JoinColumn(name = "stage_id", referencedColumnName = "stage_id", nullable = false)})
  public StageEntity getStage() {
    return stage;
  }

  public void setStage(StageEntity stage) {
    this.stage = stage;
  }

  private HostEntity host;

  @ManyToOne
  @JoinColumn(name = "host_name", referencedColumnName = "host_name", nullable = false)
  public HostEntity getHost() {
    return host;
  }

  public void setHost(HostEntity host) {
    this.host = host;
  }
}