/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events;

import org.apache.ambari.server.actionmanager.HostRoleStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Single host role command update info. This update will be sent to all subscribed recipients.
 */
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NamedHostRoleCommandUpdateEvent that = (NamedHostRoleCommandUpdateEvent) o;

    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (endTime != null ? !endTime.equals(that.endTime) : that.endTime != null) return false;
    if (status != that.status) return false;
    if (errorLog != null ? !errorLog.equals(that.errorLog) : that.errorLog != null) return false;
    if (outLog != null ? !outLog.equals(that.outLog) : that.outLog != null) return false;
    if (stderr != null ? !stderr.equals(that.stderr) : that.stderr != null) return false;
    return stdout != null ? stdout.equals(that.stdout) : that.stdout == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (errorLog != null ? errorLog.hashCode() : 0);
    result = 31 * result + (outLog != null ? outLog.hashCode() : 0);
    result = 31 * result + (stderr != null ? stderr.hashCode() : 0);
    result = 31 * result + (stdout != null ? stdout.hashCode() : 0);
    return result;
  }
}
