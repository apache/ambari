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

import java.util.Collection;

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

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.State;

@Entity
@Table(
    name = "servicecomponentdesiredstate",
    uniqueConstraints = @UniqueConstraint(
        name = "unq_scdesiredstate_name",
        columnNames = { "component_name", "service_id" , "service_group_id", "cluster_id" }) )
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
    query = "SELECT scds FROM ServiceComponentDesiredStateEntity scds WHERE scds.clusterId = :clusterId " +
      "AND scds.serviceGroupId = :serviceGroupId " +
      "AND scds.serviceId = :serviceId " +
      "AND scds.componentName = :componentName " +
      "AND scds.componentType = :componentType" ),
  @NamedQuery(
    name = "ServiceComponentDesiredStateEntity.findById",
    query = "SELECT scds FROM ServiceComponentDesiredStateEntity scds WHERE scds.id = :id" )
})

public class ServiceComponentDesiredStateEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(
      strategy = GenerationType.TABLE,
      generator = "servicecomponentdesiredstate_id_generator")
  private Long id;

  @Column(name = "component_name", nullable = false, insertable = true, updatable = true)
  private String componentName;

  @Column(name = "component_type", nullable = false, insertable = true, updatable = true)
  private String componentType;

  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Column(name = "service_group_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long serviceGroupId;

  @Column(name = "service_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long serviceId;

  @Column(name = "desired_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(EnumType.STRING)
  private State desiredState = State.INIT;

  @Column(name = "recovery_enabled", nullable = false, insertable = true, updatable = true)
  private Integer recoveryEnabled = 0;

  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  @Column(name = "repo_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(EnumType.STRING)
  private RepositoryVersionState repoState = RepositoryVersionState.NOT_REQUIRED;

  /**
   * Unidirectional one-to-one association to {@link RepositoryVersionEntity}
   */
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  @OneToOne
  @JoinColumn(
      name = "desired_repo_version_id",
      unique = false,
      nullable = false,
      insertable = true,
      updatable = true)
  private RepositoryVersionEntity desiredRepositoryVersion;

  @ManyToOne
  @JoinColumns(
    {
      @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false),
      @JoinColumn(name = "service_group_id", referencedColumnName = "service_group_id", nullable = false),
      @JoinColumn(name = "service_id", referencedColumnName = "id", nullable = false)
    })
  private ClusterServiceEntity clusterServiceEntity;

  @OneToMany(mappedBy = "serviceComponentDesiredStateEntity")
  private Collection<HostComponentStateEntity> hostComponentStateEntities;

  @OneToMany(mappedBy = "serviceComponentDesiredStateEntity")
  private Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities;

  public Long getId() {
    return id;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public Long getServiceGroupId() { return serviceGroupId; }

  public void setServiceGroupId(Long serviceGroupId) { this.serviceGroupId = serviceGroupId; }

  public Long getServiceId() { return serviceId; }

  public void setServiceId(Long serviceId) { this.serviceId = serviceId; }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public String getComponentType() {
    return componentType;
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

  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  public RepositoryVersionEntity getDesiredRepositoryVersion() {
    return desiredRepositoryVersion;
  }

  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  public void setDesiredRepositoryVersion(RepositoryVersionEntity desiredRepositoryVersion) {
    this.desiredRepositoryVersion = desiredRepositoryVersion;
  }

  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  public StackEntity getDesiredStack() {
    return desiredRepositoryVersion.getStack();
  }

  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  public String getDesiredVersion() {
    return desiredRepositoryVersion.getVersion();
  }

  public boolean isRecoveryEnabled() {
    return recoveryEnabled != 0;
  }

  public void setRecoveryEnabled(boolean recoveryEnabled) {
    this.recoveryEnabled = (recoveryEnabled == false) ? 0 : 1;
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

    ServiceComponentDesiredStateEntity that = (ServiceComponentDesiredStateEntity) o;

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
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) {
      return false;
    }
    if (componentType != null ? !componentType.equals(that.componentType) : that.componentType != null) {
      return false;
    }
    if (desiredState != null ? !desiredState.equals(that.desiredState) : that.desiredState != null) {
      return false;
    }
    if (desiredRepositoryVersion != null ? !desiredRepositoryVersion.equals(that.desiredRepositoryVersion)
      : that.desiredRepositoryVersion != null) {
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
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (componentType != null ? componentType.hashCode() : 0);
    result = 31 * result + (desiredState != null ? desiredState.hashCode() : 0);
    result = 31 * result + (desiredRepositoryVersion != null ? desiredRepositoryVersion.hashCode() : 0);

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

  /**
   * @param state the repository state for {@link #getDesiredVersion()}
   */
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  public void setRepositoryState(RepositoryVersionState state) {
    repoState = state;
  }

  /**
   * @return the state of the repository for {@link #getDesiredVersion()}
   */
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  public RepositoryVersionState getRepositoryState() {
    return repoState;
  }

}
