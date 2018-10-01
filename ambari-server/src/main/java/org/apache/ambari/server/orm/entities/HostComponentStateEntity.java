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

import com.google.common.base.MoreObjects;
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

  @Column(name = "host_comp_desired_state_id", nullable = false, insertable = false, updatable = false)
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
   * The mpack version reported by the host component during the last status
   * update. Components are associated with more than a single version; they
   * have an mpack version and then their specific component version.
   * </p>
   *
   * <pre>
   * /usr/vendor/mpacks/my-mpack/1.0.0-b450/some_component -> /usr/vendor/modules/some_component/3.4.0.0-b42
   * </pre>
   *
   * In this example, the version of the mpack can change, but still technically
   * point to the same component version. This is why both are tracked.
   *
   * @see #version
   */
  @Column(name = "mpack_version", nullable = false, insertable = true, updatable = true)
  private String mpackVersion = State.UNKNOWN.toString();

  /**
   * The component version reported by the host component during the last status
   * update.
   *
   * @see #mpackVersion
   */
  @Column(name = "version", nullable = false, insertable = true, updatable = true)
  private String version = State.UNKNOWN.toString();

  @Enumerated(value = EnumType.STRING)
  @Column(name = "current_state", nullable = false, insertable = true, updatable = true)
  private State currentState = State.INIT;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "last_live_state", nullable = true, insertable = true, updatable = true)
  private State lastLiveState = State.INIT;

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
  @JoinColumn(name = "host_comp_desired_state_id", referencedColumnName = "id", nullable = false)
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
    return serviceId;
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
    id = componentId;
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

  public State getLastLiveState() {
    return lastLiveState;
  }

  public void setLastLiveState(State lastLiveState) {
    this.lastLiveState = lastLiveState;
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

  /**
   * Gets the version of the mpack which was reported for this host component.
   *
   * @return the mpack version reporting for this component, or
   *         {@link State#UNKNOWN}.
   */
  public String getMpackVersion() {
    return mpackVersion;
  }

  /**
   * Sets the version of the mpack which was reported for this host component.
   *
   * @param mpackVersion
   *          the version to set, or {@link State#UNKNOWN}.
   */
  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
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
    return Objects.equal(id, that.id) && Objects.equal(clusterId, that.clusterId)
        && Objects.equal(serviceGroupId, that.serviceGroupId)
        && Objects.equal(serviceId, that.serviceId)
        && Objects.equal(componentName, that.componentName)
        && Objects.equal(componentType, that.componentType)
        && Objects.equal(currentState, that.currentState)
        && Objects.equal(lastLiveState, that.lastLiveState)
        && Objects.equal(upgradeState, that.upgradeState)
        && Objects.equal(hostEntity, that.hostEntity)
        && Objects.equal(hostComponentDesiredStateEntity, that.hostComponentDesiredStateEntity)
        && Objects.equal(mpackVersion, that.version) && Objects.equal(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, clusterId, serviceGroupId, serviceId, hostEntity,
        hostComponentDesiredStateEntity, componentName, componentType, currentState, lastLiveState,
        upgradeState, mpackVersion, version);
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
    return MoreObjects.toStringHelper(this)
        .add("clusterId", clusterId)
        .add("serviceGroupId", serviceGroupId)
        .add("serviceId", serviceId)
        .add("componentId", id)
        .add("componentName", componentName)
        .add("componentType", componentType)
        .add("hostId", hostId)
        .add("state", currentState)
        .add("mpackVersion", mpackVersion)
        .add("version", version).toString();
  }
}
