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

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.state.State;

@Entity
@Table(
    name = "servicecomponentdesiredstate",
    uniqueConstraints = @UniqueConstraint(
        name = "unq_scdesiredstate_name",
        columnNames = { "component_name", "service_name", "cluster_id" }) )
@TableGenerator(
    name = "servicecomponentdesiredstate_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "servicecomponentdesiredstate_id_seq",
    initialValue = 0)
@NamedQueries({
 @NamedQuery(
    name = "ServiceComponentDesiredStateEntity.findByName",
    query = "SELECT scds FROM ServiceComponentDesiredStateEntity scds WHERE scds.clusterId = :clusterId AND scds.serviceName = :serviceName AND scds.componentName = :componentName") })
public class ServiceComponentDesiredStateEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(
      strategy = GenerationType.TABLE,
      generator = "servicecomponentdesiredstate_id_generator")
  private Long id;

  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  private String serviceName;

  @Column(name = "component_name", nullable = false, insertable = true, updatable = true)
  private String componentName;

  @Column(name = "desired_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(EnumType.STRING)
  private State desiredState = State.INIT;

  @Column(name = "recovery_enabled", nullable = false, insertable = true, updatable = true)
  private Integer recoveryEnabled = 0;

  /**
   * Unidirectional one-to-one association to {@link StackEntity}
   */
  @OneToOne
  @JoinColumn(name = "desired_stack_id", unique = false, nullable = false, insertable = true, updatable = true)
  private StackEntity desiredStack;

  /**
   * Version string that should be followed by instances
   * of component on hosts. Includes both stack version and build
   */
  @Column(name = "desired_version", nullable = false, insertable = true, updatable = true)
  private String desiredVersion = State.UNKNOWN.toString();

  @ManyToOne
  @JoinColumns({@javax.persistence.JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false), @JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false)})
  private ClusterServiceEntity clusterServiceEntity;

  @OneToMany(mappedBy = "serviceComponentDesiredStateEntity")
  private Collection<HostComponentStateEntity> hostComponentStateEntities;

  @OneToMany(mappedBy = "serviceComponentDesiredStateEntity")
  private Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities;

  /**
   * All of the upgrades and downgrades which have occurred for this component.
   * Can be {@code null} for none.
   */
  @OneToMany(
      mappedBy = "m_serviceComponentDesiredStateEntity",
      cascade = { CascadeType.ALL })
  private Collection<ServiceComponentHistoryEntity> serviceComponentHistory;

  @OneToMany(mappedBy = "m_serviceComponentDesiredStateEntity", cascade = { CascadeType.ALL })
  private Collection<ServiceComponentVersionEntity> serviceComponentVersion;

  public Long getId() {
    return id;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public State getDesiredState() {
    return desiredState;
  }

  public void setDesiredState(State desiredState) {
    this.desiredState = desiredState;
  }

  public StackEntity getDesiredStack() {
    return desiredStack;
  }

  public void setDesiredStack(StackEntity desiredStack) {
    this.desiredStack = desiredStack;
  }

  public String getDesiredVersion() {
    return desiredVersion;
  }

  public void setDesiredVersion(String desiredVersion) {
    this.desiredVersion = desiredVersion;
  }

  /**
   * Adds a historical entry for the version of this service component. New
   * entries are automatically created when this entity is merged via a
   * {@link CascadeType#MERGE}.
   *
   * @param historicalEntry
   *          the entry to add.
   */
  public void addHistory(ServiceComponentHistoryEntity historicalEntry) {
    if (null == serviceComponentHistory) {
      serviceComponentHistory = new ArrayList<>();
    }

    serviceComponentHistory.add(historicalEntry);
    historicalEntry.setServiceComponentDesiredState(this);
  }

  /**
   * Gets the history of this component's upgrades and downgrades.
   *
   * @return the component history, or {@code null} if none.
   */
  public Collection<ServiceComponentHistoryEntity> getHistory() {
    return serviceComponentHistory;
  }


  /**
   * @param versionEntry the version to add
   */
  public void addVersion(ServiceComponentVersionEntity versionEntry) {
    if (null == serviceComponentVersion) {
      serviceComponentVersion = new ArrayList<>();
    }

    serviceComponentVersion.add(versionEntry);
    versionEntry.setServiceComponentDesiredState(this);
  }

  /**
   * @return the collection of versions for the component
   */
  public Collection<ServiceComponentVersionEntity> getVersions() {
    return serviceComponentVersion;
  }


  public boolean isRecoveryEnabled() {
    return recoveryEnabled != 0;
  }

  public void setRecoveryEnabled(boolean recoveryEnabled) {
    this.recoveryEnabled = (recoveryEnabled == false) ? 0 : 1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceComponentDesiredStateEntity that = (ServiceComponentDesiredStateEntity) o;

    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) {
      return false;
    }
    if (desiredState != null ? !desiredState.equals(that.desiredState) : that.desiredState != null) {
      return false;
    }
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) {
      return false;
    }
    if (desiredStack != null ? !desiredStack.equals(that.desiredStack)
        : that.desiredStack != null) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (clusterId != null ? clusterId.hashCode() : 0);
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (desiredState != null ? desiredState.hashCode() : 0);
    result = 31 * result + (desiredStack != null ? desiredStack.hashCode() : 0);

    return result;
  }

  public ClusterServiceEntity getClusterServiceEntity() {
    return clusterServiceEntity;
  }

  public void setClusterServiceEntity(ClusterServiceEntity clusterServiceEntity) {
    this.clusterServiceEntity = clusterServiceEntity;
  }

  public Collection<HostComponentStateEntity> getHostComponentStateEntities() {
    return hostComponentStateEntities;
  }

  public void setHostComponentStateEntities(Collection<HostComponentStateEntity> hostComponentStateEntities) {
    this.hostComponentStateEntities = hostComponentStateEntities;
  }

  public Collection<HostComponentDesiredStateEntity> getHostComponentDesiredStateEntities() {
    return hostComponentDesiredStateEntities;
  }

  public void setHostComponentDesiredStateEntities(Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities) {
    this.hostComponentDesiredStateEntities = hostComponentDesiredStateEntities;
  }

}
