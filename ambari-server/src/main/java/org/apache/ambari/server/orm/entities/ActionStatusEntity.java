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

package org.apache.ambari.server.orm.entities;

import javax.persistence.*;
import java.util.Arrays;

@javax.persistence.IdClass(ActionStatusEntityPK.class)
@javax.persistence.Table(name = "actionstatus", schema = "ambari", catalog = "")
@Entity
public class ActionStatusEntity {

  private String clusterName = "";

  @javax.persistence.Column(name = "cluster_name", nullable = true, insertable = false, updatable = false)
  @Id
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  private String hostName = "";

  @javax.persistence.Column(name = "host_name", nullable = false, insertable = false, updatable = false)
  @Id
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  private String role = "";

  @javax.persistence.Column(name = "role", nullable = false)
  @Id
  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  private Integer requestId = 0;

  @javax.persistence.Column(name = "request_id", nullable = false)
  @Id
  public Integer getRequestId() {
    return requestId;
  }

  public void setRequestId(Integer requestId) {
    this.requestId = requestId;
  }

  private Integer stageId = 0;

  @javax.persistence.Column(name = "stage_id", nullable = false)
  @Id
  public Integer getStageId() {
    return stageId;
  }

  public void setStageId(Integer stageId) {
    this.stageId = stageId;
  }

  private String event = "";

  @javax.persistence.Column(name = "event", nullable = false)
  @Basic
  public String getEvent() {
    return event;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  private Integer taskId = 0;

  @javax.persistence.Column(name = "task_id", nullable = false)
  @Basic
  public Integer getTaskId() {
    return taskId;
  }

  public void setTaskId(Integer taskId) {
    this.taskId = taskId;
  }

  private String status = "";

  @javax.persistence.Column(name = "status", nullable = false)
  @Basic
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  private String logInfo;

  @javax.persistence.Column(name = "log_info")
  @Basic
  public String getLogInfo() {
    return logInfo;
  }

  public void setLogInfo(String logInfo) {
    this.logInfo = logInfo;
  }

  private byte[] continueCriteria;

  @javax.persistence.Column(name = "continue_criteria")
  @Basic
  public byte[] getContinueCriteria() {
    return continueCriteria;
  }

  public void setContinueCriteria(byte[] continueCriteria) {
    this.continueCriteria = continueCriteria;
  }

  private ClusterEntity clusterEntity;

  @ManyToOne
  @JoinColumn(name = "cluster_name")
  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  private HostEntity hostEntity;

  @ManyToOne
  @JoinColumn(name = "host_name")
  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ActionStatusEntity that = (ActionStatusEntity) o;

    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (!Arrays.equals(continueCriteria, that.continueCriteria)) return false;
    if (event != null ? !event.equals(that.event) : that.event != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (logInfo != null ? !logInfo.equals(that.logInfo) : that.logInfo != null) return false;
    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;
    if (role != null ? !role.equals(that.role) : that.role != null) return false;
    if (stageId != null ? !stageId.equals(that.stageId) : that.stageId != null) return false;
    if (status != null ? !status.equals(that.status) : that.status != null) return false;
    if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (role != null ? role.hashCode() : 0);
    result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
    result = 31 * result + (stageId != null ? stageId.hashCode() : 0);
    result = 31 * result + (event != null ? event.hashCode() : 0);
    result = 31 * result + (taskId != null ? taskId.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (logInfo != null ? logInfo.hashCode() : 0);
    result = 31 * result + (continueCriteria != null ? Arrays.hashCode(continueCriteria) : 0);
    return result;
  }
}
