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

import static org.apache.commons.lang.StringUtils.defaultString;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.State;

import com.google.common.base.Objects;


@Entity
@Table(
  name = "hostcomponentdesiredstate",
  uniqueConstraints = @UniqueConstraint(
    name = "UQ_hcdesiredstate_name",
    columnNames = { "component_name", "service_id", "service_group_id", "cluster_id" }) )
@TableGenerator(
  name = "hostcomponentdesiredstate_id_generator",
  table = "ambari_sequences",
  pkColumnName = "sequence_name",
  valueColumnName = "sequence_value",
  pkColumnValue = "hostcomponentdesiredstate_id_seq",
  initialValue = 0)
@NamedQueries({
    @NamedQuery(name = "HostComponentDesiredStateEntity.findAll", query = "SELECT hcds from HostComponentDesiredStateEntity hcds"),

  @NamedQuery(name = "HostComponentDesiredStateEntity.findByServiceAndComponent", query =
    "SELECT hcds from HostComponentDesiredStateEntity hcds WHERE hcds.clusterId=:clusterId AND hcds.serviceGroupId=:serviceGroupId AND hcds.serviceId=:serviceId AND hcds.componentName=:componentName"),

  @NamedQuery(name = "HostComponentDesiredStateEntity.findByServiceComponentAndHost", query =
    "SELECT hcds from HostComponentDesiredStateEntity hcds WHERE hcds.clusterId=:clusterId AND hcds.serviceGroupId=:serviceGroupId AND hcds.serviceId=:serviceId AND hcds.componentName=:componentName AND hcds.hostEntity.hostName=:hostName"),

  @NamedQuery(name = "HostComponentDesiredStateEntity.findByIndex", query =
    "SELECT hcds from HostComponentDesiredStateEntity hcds WHERE hcds.id=:id")
})
public class HostComponentDesiredStateEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "hostcomponentdesiredstate_id_generator")
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  private Long id;


  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Column(name = "service_group_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long serviceGroupId;

  @Column(name = "service_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long serviceId;

  @Column(name = "host_id", nullable = false, insertable = false, updatable = false)
  private Long hostId;

  @Column(name = "component_name", insertable = false, updatable = false)
  private String componentName = "";

  @Column(name = "component_type", insertable = false, updatable = false)
  private String componentType = "";

  @Basic
  @Column(name = "desired_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private State desiredState = State.INIT;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "admin_state", nullable = true, insertable = true, updatable = true)
  private HostComponentAdminState adminState;

  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false),
    @JoinColumn(name = "service_group_id", referencedColumnName = "service_group_id", nullable = false),
    @JoinColumn(name = "service_id", referencedColumnName = "service_id", nullable = false),
    @JoinColumn(name = "component_name", referencedColumnName = "component_name", nullable = false),
    @JoinColumn(name = "component_type", referencedColumnName = "component_type", nullable = false) })
  private ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity;

  @ManyToOne
  @JoinColumn(name = "host_id", referencedColumnName = "host_id", nullable = false)
  private HostEntity hostEntity;

  @Enumerated(value = EnumType.STRING)
  @Column(name="maintenance_state", nullable = false, insertable = true, updatable = true)
  private MaintenanceState maintenanceState = MaintenanceState.OFF;

  @Basic
  @Column(name = "restart_required", insertable = true, updatable = true, nullable = false)
  private Integer restartRequired = 0;

  public Long getId() { return id; }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public Long getServiceGroupId() {
    return serviceGroupId;
  }

  public void setServiceGroupId(Long serviceGroupId) {
    this.serviceGroupId = serviceGroupId;
  }

  public Long getServiceId() { return serviceId; }

  public void setServiceId(Long serviceId) { this.serviceId = serviceId; }

  public Long getHostId() {
    return hostEntity != null ? hostEntity.getHostId() : null;
  }

  public String getComponentName() {
    return defaultString(componentName);
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public String getComponentType() {
    return defaultString(componentType);
  }

  public void setComponentType(String componentType) {
    this.componentType = componentType;
  }

  public State getDesiredState() {
    return desiredState;
  }

  public void setDesiredState(State desiredState) {
    this.desiredState = desiredState;
  }

  public HostComponentAdminState getAdminState() {
    return adminState;
  }

  public void setAdminState(HostComponentAdminState attribute) {
    adminState = attribute;
  }

  public MaintenanceState getMaintenanceState() {
    return maintenanceState;
  }

  public void setMaintenanceState(MaintenanceState state) {
    maintenanceState = state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HostComponentDesiredStateEntity that = (HostComponentDesiredStateEntity) o;

    if (!Objects.equal(id, that.id)) {
      return false;
    }

    if (!Objects.equal(clusterId, that.clusterId)) {
      return false;
    }

    if (!Objects.equal(serviceGroupId, that.serviceGroupId)) {
      return false;
    }

    if (!Objects.equal(serviceId, that.serviceId)) {
      return false;
    }

    if (!Objects.equal(componentName, that.componentName)) {
      return false;
    }

    if (!Objects.equal(componentType, that.componentType)) {
      return false;
    }

    if (!Objects.equal(desiredState, that.desiredState)) {
      return false;
    }

    if (!Objects.equal(hostEntity, that.hostEntity)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (clusterId != null ? clusterId.hashCode() : 0);
    result = 31 * result + (serviceGroupId != null ? serviceGroupId.hashCode() : 0);
    result = 31 * result + (serviceId != null ? serviceId.hashCode() : 0);
    result = 31 * result + (hostEntity != null ? hostEntity.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (componentType != null ? componentType.hashCode() : 0);
    result = 31 * result + (desiredState != null ? desiredState.hashCode() : 0);
    return result;
  }


  public ServiceComponentDesiredStateEntity getServiceComponentDesiredStateEntity() {
    return serviceComponentDesiredStateEntity;
  }

  public void setServiceComponentDesiredStateEntity(ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    this.serviceComponentDesiredStateEntity = serviceComponentDesiredStateEntity;
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  public boolean isRestartRequired() {
    return restartRequired == 0 ? false : true;
  }

  public void setRestartRequired(boolean restartRequired) {
    this.restartRequired = (restartRequired == false ? 0 : 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("clusterId", clusterId).add(
      "serviceGroupId", serviceGroupId).add("serviceId", serviceId).add("componentName",
      componentName).add("componentType", componentType).add("hostId", hostId).add("desiredState",
      desiredState).toString();
  }
}