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

@IdClass(org.apache.ambari.server.orm.entities.StageEntityPK.class)
@Table(name = "stage", schema = "ambari", catalog = "")
@Entity
public class StageEntity {
  private Long clusterId;

  @Column(name = "cluster_id", insertable = false, updatable = false, nullable = false)
  @Basic
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  private Long requestId;

  @Column(name = "request_id")
  @Id
  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  private Long stageId = 0L;

  @Column(name = "stage_id", nullable = false)
  @Id
  public Long getStageId() {
    return stageId;
  }

  public void setStageId(Long stageId) {
    this.stageId = stageId;
  }

  private String logInfo = "";

  @Column(name = "log_info", nullable = false)
  @Basic
  public String getLogInfo() {
    return logInfo;
  }

  public void setLogInfo(String logInfo) {
    this.logInfo = logInfo;
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

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.hashCode() : 0;
    result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
    result = 31 * result + (stageId != null ? stageId.hashCode() : 0);
    result = 31 * result + (logInfo != null ? logInfo.hashCode() : 0);
    return result;
  }

  private ClusterEntity cluster;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id")
  public ClusterEntity getCluster() {
    return cluster;
  }

  public void setCluster(ClusterEntity cluster) {
    this.cluster = cluster;
  }

  private Collection<HostRoleCommandEntity> hostRoleCommands;

  @OneToMany(mappedBy = "stage")
  public Collection<HostRoleCommandEntity> getHostRoleCommands() {
    return hostRoleCommands;
  }

  public void setHostRoleCommands(Collection<HostRoleCommandEntity> hostRoleCommands) {
    this.hostRoleCommands = hostRoleCommands;
  }

  private Collection<RoleSuccessCriteriaEntity> roleSuccessCriterias;

  @OneToMany(mappedBy = "stage")
  public Collection<RoleSuccessCriteriaEntity> getRoleSuccessCriterias() {
    return roleSuccessCriterias;
  }

  public void setRoleSuccessCriterias(Collection<RoleSuccessCriteriaEntity> roleSuccessCriterias) {
    this.roleSuccessCriterias = roleSuccessCriterias;
  }
}
