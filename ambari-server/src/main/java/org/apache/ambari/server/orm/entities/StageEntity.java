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

import javax.persistence.*;
import java.util.Collection;

import static org.apache.commons.lang.StringUtils.defaultString;

@IdClass(org.apache.ambari.server.orm.entities.StageEntityPK.class)
@Table(name = "stage")
@Entity
public class StageEntity {

  @Column(name = "cluster_id", updatable = false, nullable = false)
  @Basic
  private Long clusterId = Long.valueOf(-1L);

  @Column(name = "request_id", insertable = false, updatable = false, nullable = false)
  @Id
  private Long requestId;

  @Column(name = "stage_id", nullable = false)
  @Id
  private Long stageId = 0L;

  @Column(name = "log_info")
  @Basic
  private String logInfo = "";

  @Column(name = "request_context")
  @Basic
  private String requestContext = "";
  
  @Column(name = "cluster_host_info")
  @Basic
  private byte[] clusterHostInfo;
 
  @Column(name = "command_params")
  @Basic
  private byte[] commandParamsStage;

  @Column(name = "host_params")
  @Basic
  private byte[] hostParamsStage;

  @ManyToOne
  @JoinColumn(name = "request_id", referencedColumnName = "request_id", nullable = false)
  private RequestEntity request;
  

  @OneToMany(mappedBy = "stage", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
  private Collection<HostRoleCommandEntity> hostRoleCommands;

  @OneToMany(mappedBy = "stage", cascade = CascadeType.REMOVE)
  private Collection<RoleSuccessCriteriaEntity> roleSuccessCriterias;

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
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

  public String getLogInfo() {
    return defaultString(logInfo);
  }

  public void setLogInfo(String logInfo) {
    this.logInfo = logInfo;
  }

  public String getRequestContext() {
    return defaultString(requestContext);
  }

  public String getClusterHostInfo() {
    return clusterHostInfo == null ? new String() : new String(clusterHostInfo);
  }

  public void setClusterHostInfo(String clusterHostInfo) {
    this.clusterHostInfo = clusterHostInfo.getBytes();
  }
 
  public String getCommandParamsStage() {
    return commandParamsStage == null ? new String() : new String(commandParamsStage);
  }

  public void setCommandParamsStage(String commandParamsStage) {
    this.commandParamsStage = commandParamsStage.getBytes();
  }

  public String getHostParamsStage() {
    return hostParamsStage == null ? new String() : new String(hostParamsStage);
  }

  public void setHostParamsStage(String hostParamsStage) {
    this.hostParamsStage = hostParamsStage.getBytes();
  }

  public void setRequestContext(String requestContext) {
    if (requestContext != null) {
      this.requestContext = requestContext;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StageEntity that = (StageEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (logInfo != null ? !logInfo.equals(that.logInfo) : that.logInfo != null) return false;
    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;
    if (stageId != null ? !stageId.equals(that.stageId) : that.stageId != null) return false;
    if (clusterHostInfo != null ? !clusterHostInfo.equals(that.clusterHostInfo) : that.clusterHostInfo != null) return false;
    if (commandParamsStage != null ? !commandParamsStage.equals(that.commandParamsStage) : that.commandParamsStage != null) return false;
    if (hostParamsStage != null ? !hostParamsStage.equals(that.hostParamsStage) : that.hostParamsStage != null) return false;
    return !(requestContext != null ? !requestContext.equals(that.requestContext) : that.requestContext != null);

  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.hashCode() : 0;
    result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
    result = 31 * result + (stageId != null ? stageId.hashCode() : 0);
    result = 31 * result + (logInfo != null ? logInfo.hashCode() : 0);
    result = 31 * result + (clusterHostInfo != null ? clusterHostInfo.hashCode() : 0);
    result = 31 * result + (commandParamsStage != null ? commandParamsStage.hashCode() : 0);
    result = 31 * result + (hostParamsStage != null ? hostParamsStage.hashCode() : 0);
    result = 31 * result + (requestContext != null ? requestContext.hashCode() : 0);
    return result;
  }

  public Collection<HostRoleCommandEntity> getHostRoleCommands() {
    return hostRoleCommands;
  }

  public void setHostRoleCommands(Collection<HostRoleCommandEntity> hostRoleCommands) {
    this.hostRoleCommands = hostRoleCommands;
  }

  public Collection<RoleSuccessCriteriaEntity> getRoleSuccessCriterias() {
    return roleSuccessCriterias;
  }

  public void setRoleSuccessCriterias(Collection<RoleSuccessCriteriaEntity> roleSuccessCriterias) {
    this.roleSuccessCriterias = roleSuccessCriterias;
  }

  public RequestEntity getRequest() {
    return request;
  }

  public void setRequest(RequestEntity request) {
    this.request = request;
  }
}
