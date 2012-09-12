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

@Table(name = "hoststate", schema = "ambari", catalog = "")
@Entity
public class HostStateEntity {

  private String clusterName;

  @Column(name = "cluster_name", insertable = false, updatable = false)
  @Basic
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  private String hostName;

  @Column(name = "host_name", insertable = false, updatable = false)
  @Id
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  private Integer lastHeartbeatTime = 0;

  @Column(name = "last_heartbeat_time")
  @Basic
  public Integer getLastHeartbeatTime() {
    return lastHeartbeatTime;
  }

  public void setLastHeartbeatTime(Integer lastHeartbeatTime) {
    this.lastHeartbeatTime = lastHeartbeatTime;
  }

  private String agentVersion = "";

  @Column(name = "agent_version")
  @Basic
  public String getAgentVersion() {
    return agentVersion;
  }

  public void setAgentVersion(String agentVersion) {
    this.agentVersion = agentVersion;
  }

  private String currentState;

  @Column(name = "current_state")
  @Basic
  public String getCurrentState() {
    return currentState;
  }

  public void setCurrentState(String currentState) {
    this.currentState = currentState;
  }

  private HostEntity hostEntity;

  @OneToOne
  @JoinColumn(name = "host_name")
  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostStateEntity that = (HostStateEntity) o;

    if (agentVersion != null ? !agentVersion.equals(that.agentVersion) : that.agentVersion != null) return false;
    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (currentState != null ? !currentState.equals(that.currentState) : that.currentState != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (lastHeartbeatTime != null ? !lastHeartbeatTime.equals(that.lastHeartbeatTime) : that.lastHeartbeatTime != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (lastHeartbeatTime != null ? lastHeartbeatTime.hashCode() : 0);
    result = 31 * result + (agentVersion != null ? agentVersion.hashCode() : 0);
    result = 31 * result + (currentState != null ? currentState.hashCode() : 0);
    return result;
  }
}
