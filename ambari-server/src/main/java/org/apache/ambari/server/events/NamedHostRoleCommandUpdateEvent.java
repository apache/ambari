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

package org.apache.ambari.server.events;

import org.apache.ambari.server.actionmanager.HostRoleStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamedHostRoleCommandUpdateEvent extends AmbariUpdateEvent {

  private Long id;
  private Long requestId;
  private String hostName;
  private Long endTime;
  private HostRoleStatus status;
  private String errorLog;
  private String outLog;
  private String stderr;
  private String stdout;

  public NamedHostRoleCommandUpdateEvent(Long id, Long requestId, String hostName, Long endTime, HostRoleStatus status, String errorLog, String outLog, String stderr, String stdout) {
    super(Type.NAMEDHOSTCOMPONENT);
    this.id = id;
    this.requestId = requestId;
    this.hostName = hostName;
    this.endTime = endTime;
    this.status = status;
    this.errorLog = errorLog;
    this.outLog = outLog;
    this.stderr = stderr;
    this.stdout = stdout;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  public String getErrorLog() {
    return errorLog;
  }

  public void setErrorLog(String errorLog) {
    this.errorLog = errorLog;
  }

  public String getOutLog() {
    return outLog;
  }

  public void setOutLog(String outLog) {
    this.outLog = outLog;
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

  @Override
  public String getDestination() {
    return super.getDestination() + getId();
  }
}
