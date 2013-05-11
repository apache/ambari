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

import org.apache.ambari.server.state.HostState;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;

import static org.apache.commons.lang.StringUtils.defaultString;

@javax.persistence.Table(name = "hoststate")
@Entity
public class HostStateEntity {
  
  @javax.persistence.Column(name = "host_name", nullable = false, insertable = false, updatable = false)
  @Id
  private String hostName;

  @Column(name = "available_mem", nullable = false, insertable = true, updatable = true)
  @Basic
  private Long availableMem = 0L;

  @javax.persistence.Column(name = "time_in_state", nullable = false, insertable = true, updatable = true)
  @Basic
  private Long timeInState = 0L;

  @Column(name = "health_status", insertable = true, updatable = true)
  @Basic
  private String healthStatus;

  @Column(name = "agent_version", insertable = true, updatable = true)
  @Basic
  private String agentVersion = "";

  @Column(name = "current_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private HostState currentState = HostState.INIT;

  @OneToOne
  @JoinColumn(name = "host_name", referencedColumnName = "host_name", nullable = false)
  private HostEntity hostEntity;

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public Long getAvailableMem() {
    return availableMem;
  }

  public void setAvailableMem(Long availableMem) {
    this.availableMem = availableMem;
  }

  public Long getTimeInState() {
    return timeInState;
  }

  public void setTimeInState(Long timeInState) {
    this.timeInState = timeInState;
  }

  public String getHealthStatus() {
    return healthStatus;
  }

  public void setHealthStatus(String healthStatus) {
    this.healthStatus = healthStatus;
  }

  public String getAgentVersion() {
    return defaultString(agentVersion);
  }

  public void setAgentVersion(String agentVersion) {
    this.agentVersion = agentVersion;
  }

  public HostState getCurrentState() {
    return currentState;
  }

  public void setCurrentState(HostState currentState) {
    this.currentState = currentState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostStateEntity that = (HostStateEntity) o;

    if (availableMem != null ? !availableMem.equals(that.availableMem) : that.availableMem != null) return false;
    if (timeInState != null ? !timeInState.equals(that.timeInState) : that.timeInState!= null) return false;
    if (agentVersion != null ? !agentVersion.equals(that.agentVersion) : that.agentVersion != null) return false;
    if (currentState != null ? !currentState.equals(that.currentState) : that.currentState != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = hostName != null ? hostName.hashCode() : 0;
    result = 31 * result + (availableMem != null ? availableMem.intValue() : 0);
    result = 31 * result + (timeInState != null ? timeInState.intValue() : 0);
    result = 31 * result + (agentVersion != null ? agentVersion.hashCode() : 0);
    result = 31 * result + (currentState != null ? currentState.hashCode() : 0);
    return result;
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

}
