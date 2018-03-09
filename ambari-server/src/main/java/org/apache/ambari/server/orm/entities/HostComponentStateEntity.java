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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;

import com.google.common.base.Objects;

@Entity
@Table(
    name = "hostcomponentstate",
    uniqueConstraints = @UniqueConstraint(
                name = "UQ_hostcomponentstate_name",
                columnNames = { "component_name", "service_id" , "host_id", "service_group_id", "cluster_id" }) )
@TableGenerator(
    name = "hostcomponentstate_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "hostcomponentstate_id_seq",
    initialValue = 0)
@NamedQueries({
    @NamedQuery(
        name = "HostComponentStateEntity.findAll",
        query = "SELECT hcs from HostComponentStateEntity hcs"),
    @NamedQuery(
        name = "HostComponentStateEntity.findByHost",
        query = "SELECT hcs from HostComponentStateEntity hcs WHERE hcs.hostEntity.hostName=:hostName"),
    @NamedQuery(
        name = "HostComponentStateEntity.findByService",
        query = "SELECT hcs from HostComponentStateEntity  hcs WHERE hcs.clusterId=:clusterId " +
                "AND hcs.serviceGroupId=:serviceGroupId AND hcs.serviceId=:serviceId " ),
    @NamedQuery(
        name = "HostComponentStateEntity.findByServiceAndComponent",
        query = "SELECT hcs from HostComponentStateEntity hcs WHERE hcs.clusterId=:clusterId " +
                "AND hcs.serviceGroupId=:serviceGroupId AND hcs.serviceId=:serviceId " +
                "AND hcs.componentName=:componentName"),
    @NamedQuery(
        name = "HostComponentStateEntity.findByServiceComponentAndHost",
        query = "SELECT hcs from HostComponentStateEntity hcs WHERE hcs.clusterId=:clusterId " +
                "AND hcs.serviceGroupId=:serviceGroupId AND hcs.serviceId=:serviceId " +
                "AND hcs.componentName=:componentName AND hcs.hostEntity.hostName=:hostName"),
    @NamedQuery(
        name = "HostComponentStateEntity.findByServiceAndComponentAndNotVersion",
        query = "SELECT hcs from HostComponentStateEntity hcs WHERE hcs.clusterId=:clusterId " +
                "AND hcs.serviceGroupId=:serviceGroupId AND hcs.serviceId=:serviceId " +
                "AND hcs.componentName=:componentName " +
                "AND hcs.version != :version"),
    @NamedQuery(
        name = "HostComponentStateEntity.findByIndex",
        query = "SELECT hcs from HostComponentStateEntity hcs WHERE hcs.id=:id") })
public class HostComponentStateEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "hostcomponentstate_id_generator")
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  private Long id;

  @Column(name = "host_component_desired_state_id", nullable = false, insertable = false, updatable = false)
  private Long hostComponentDesiredStateId;

  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Column(name = "service_group_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long serviceGroupId;

  @Column(name = "service_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long serviceId;

  @Column(name = "host_id", nullable = false, insertable = false, updatable = false)
  private Long hostId;

  @Column(name = "component_name", nullable = false, insertable = false, updatable = false)
  private String componentName;

  @Column(name = "component_type", nullable = false, insertable = false, updatable = false)
  private String componentType;

  /**
   * Version reported by host component during last status update.
   */
  @Column(name = "version", nullable = false, insertable = true, updatable = true)
  private String version = State.UNKNOWN.toString();

  @Enumerated(value = EnumType.STRING)
  @Column(name = "current_state", nullable = false, insertable = true, updatable = true)
  private State currentState = State.INIT;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "upgrade_state", nullable = false, insertable = true, updatable = true)
  private UpgradeState upgradeState = UpgradeState.NONE;

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

  @OneToOne
  @JoinColumn(name = "host_component_desired_state_id", referencedColumnName = "id", nullable = false)
  private HostComponentDesiredStateEntity hostComponentDesiredStateEntity;

  public Long getId() {
    return id;
  }

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
    return this.serviceId;
  }

  public void setServiceId(Long serviceId) {
    this.serviceId = serviceId;
  }

  public String getHostName() {
    return hostEntity.getHostName();
  }

  public Long getHostId() {
    return hostEntity != null ? hostEntity.getHostId() : null;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public Long getHostComponentDesiredStateId() {
    return hostComponentDesiredStateEntity != null ? hostComponentDesiredStateEntity.getId() : null;
  }

  public void setComponentId(Long componentId) {
    this.id = componentId;
  }

  public Long getComponentId() {
    return id;
  }

  public void setComponentType(String componentType) {
    this.componentType = componentType;
  }

  public String getComponentType() {
    return componentType;
  }

  public State getCurrentState() {
    return currentState;
  }

  public void setCurrentState(State currentState) {
    this.currentState = currentState;
  }

  public UpgradeState getUpgradeState() {
    return upgradeState;
  }

  public void setUpgradeState(UpgradeState upgradeState) {
    this.upgradeState = upgradeState;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HostComponentStateEntity that = (HostComponentStateEntity) o;

    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }

    if (serviceGroupId != null ? !serviceGroupId.equals(that.serviceGroupId) : that.serviceGroupId != null) {
      return false;
    }

    if (serviceId != null ? !serviceId.equals(that.serviceId) : that.serviceId != null) {
      return false;
    }

    if (componentName != null ? !componentName.equals(that.componentName)
        : that.componentName != null) {
      return false;
    }

    if (componentType != null ? !componentType.equals(that.componentType)
            : that.componentType != null) {
      return false;
    }

    if (currentState != null ? !currentState.equals(that.currentState)
        : that.currentState != null) {
      return false;
    }

    if (upgradeState != null ? !upgradeState.equals(that.upgradeState)
        : that.upgradeState != null) {
      return false;
    }

    if (hostEntity != null ? !hostEntity.equals(that.hostEntity) : that.hostEntity != null) {
      return false;
    }

    if (hostComponentDesiredStateEntity != null ? !hostComponentDesiredStateEntity.equals(that.hostComponentDesiredStateEntity) : that.hostComponentDesiredStateEntity != null) {
      return false;
    }

    if (version != null ? !version.equals(that.version) : that.version != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.intValue() : 0;
    result = 31 * result + (clusterId != null ? clusterId.intValue() : 0);
    result = 31 * result + (serviceGroupId != null ? serviceGroupId.intValue() : 0);
    result = 31 * result + (serviceId != null ? serviceId.intValue() : 0);
    result = 31 * result + (hostEntity != null ? hostEntity.hashCode() : 0);
    result = 31 * result + (hostComponentDesiredStateEntity != null ? hostComponentDesiredStateEntity.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (componentType != null ? componentType.hashCode() : 0);
    result = 31 * result + (currentState != null ? currentState.hashCode() : 0);
    result = 31 * result + (upgradeState != null ? upgradeState.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
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

  public HostComponentDesiredStateEntity getHostComponentDesiredStateEntity() {
    return hostComponentDesiredStateEntity;
  }

  public void setHostComponentDesiredStateEntity(HostComponentDesiredStateEntity hostComponentDesiredStateEntity) {
    this.hostComponentDesiredStateEntity = hostComponentDesiredStateEntity;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("clusterId", clusterId).add("serviceGroupId", serviceGroupId).add(
      "serviceId", serviceId).add("componentId", id).add("componentName", componentName).add
            ("componentType", componentType).add("hostId", hostId).add("state", currentState).toString();
  }

}