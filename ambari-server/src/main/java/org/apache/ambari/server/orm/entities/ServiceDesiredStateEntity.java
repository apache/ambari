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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.State;

@javax.persistence.Table(name = "servicedesiredstate")
@NamedQueries({
  @NamedQuery(name = "ServiceDesiredStateByServiceId", query =
    "SELECT serviceDesiredState " +
    "FROM ServiceDesiredStateEntity serviceDesiredState " +
    "WHERE serviceDesiredState.serviceId=:serviceId ")
})

@Entity
public class ServiceDesiredStateEntity {

  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = false, length = 10)
  private Long clusterId;

  @Column(name = "service_group_id", nullable = false, insertable = true, updatable = false, length = 10)
  private Long serviceGroupId;

  @Column(name = "service_id", nullable = false, insertable = false, updatable = false, length = 10)
  @Id
  private Long serviceId;

  @Column(name = "desired_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private State desiredState = State.INIT;

  @Basic
  @Column(name = "desired_host_role_mapping", nullable = false, insertable = true, updatable = true, length = 10)
  private int desiredHostRoleMapping = 0;

  @Column(name = "maintenance_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private MaintenanceState maintenanceState = MaintenanceState.OFF;

  @Column(name = "credential_store_enabled", nullable = false, insertable = true, updatable = true)
  private short credentialStoreEnabled = 0;

  @OneToOne
  @JoinColumn(name = "service_id", referencedColumnName = "id", nullable = false)
  private ClusterServiceEntity clusterServiceEntity;

  public Long getClusterId() {
    return clusterId;
  }

  public void setServiceGroupId(Long serviceGroupId) {
    this.serviceGroupId = serviceGroupId;
  }

  public Long getServiceGroupId() {
    return serviceGroupId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public Long getServiceId() {
    return serviceId;
  }

  public void setServiceId(Long serviceId) {
    this.serviceId = serviceId;
  }

  public State getDesiredState() {
    return desiredState;
  }

  public void setDesiredState(State desiredState) {
    this.desiredState = desiredState;
  }

  public int getDesiredHostRoleMapping() {
    return desiredHostRoleMapping;
  }

  public void setDesiredHostRoleMapping(int desiredHostRoleMapping) {
    this.desiredHostRoleMapping = desiredHostRoleMapping;
  }

  public MaintenanceState getMaintenanceState() {
    return maintenanceState;
  }

  public void setMaintenanceState(MaintenanceState state) {
    maintenanceState = state;
  }

  /**
   * Gets a value indicating if credential store use is enabled or not.
   *
   * @return true or false
   */
  public boolean isCredentialStoreEnabled() {
    return credentialStoreEnabled != 0;
  }

  /**
   * Sets a value indicating if credential store use is enabled or not.
   *
   * @param credentialStoreEnabled
   */
  public void setCredentialStoreEnabled(boolean credentialStoreEnabled) {
    this.credentialStoreEnabled = (short)((credentialStoreEnabled == false) ? 0 : 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceDesiredStateEntity that = (ServiceDesiredStateEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }

    if (serviceGroupId != null ? !serviceGroupId.equals(that.serviceGroupId) : that.serviceGroupId != null) {
      return false;
    }

    if (serviceId != null ? !serviceId.equals(that.serviceId) : that.serviceId != null) {
      return false;
    }

    if (desiredState != null ? !desiredState.equals(that.desiredState) : that.desiredState != null) {
      return false;
    }

    if (desiredHostRoleMapping != that.desiredHostRoleMapping) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.intValue() : 0;
    result = 31 * result + (serviceGroupId != null ? serviceGroupId.hashCode() : 0);
    result = 31 * result + (serviceId != null ? serviceId.hashCode() : 0);
    result = 31 * result + (desiredState != null ? desiredState.hashCode() : 0);
    result = 31 * result + desiredHostRoleMapping;
    return result;
  }

  public ClusterServiceEntity getClusterServiceEntity() {
    return clusterServiceEntity;
  }

  public void setClusterServiceEntity(ClusterServiceEntity clusterServiceEntity) {
    this.clusterServiceEntity = clusterServiceEntity;
  }
}