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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;

@Entity
@Table(name = "hostcomponentstate")
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
        query = "SELECT hcs from HostComponentStateEntity hcs WHERE hcs.serviceName=:serviceName"),
    @NamedQuery(
        name = "HostComponentStateEntity.findByServiceAndComponent",
        query = "SELECT hcs from HostComponentStateEntity hcs WHERE hcs.serviceName=:serviceName AND hcs.componentName=:componentName"),
    @NamedQuery(
        name = "HostComponentStateEntity.findByServiceComponentAndHost",
        query = "SELECT hcs from HostComponentStateEntity hcs WHERE hcs.serviceName=:serviceName AND hcs.componentName=:componentName AND hcs.hostEntity.hostName=:hostName"),
    @NamedQuery(
        name = "HostComponentStateEntity.findByIndex",
        query = "SELECT hcs from HostComponentStateEntity hcs WHERE hcs.clusterId=:clusterId AND hcs.serviceName=:serviceName AND hcs.componentName=:componentName AND hcs.hostId=:hostId") })
public class HostComponentStateEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "hostcomponentstate_id_generator")
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  private Long id;

  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  private String serviceName;

  @Column(name = "host_id", nullable = false, insertable = false, updatable = false)
  private Long hostId;

  @Column(name = "component_name", nullable = false, insertable = false, updatable = false)
  private String componentName;

  @Column(name = "version", nullable = false, insertable = true, updatable = true)
  private String version = "UNKNOWN";

  @Enumerated(value = EnumType.STRING)
  @Column(name = "current_state", nullable = false, insertable = true, updatable = true)
  private State currentState = State.INIT;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "upgrade_state", nullable = false, insertable = true, updatable = true)
  private UpgradeState upgradeState = UpgradeState.NONE;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "security_state", nullable = false, insertable = true, updatable = true)
  private SecurityState securityState = SecurityState.UNSECURED;

  /**
   * Unidirectional one-to-one association to {@link StackEntity}
   */
  @OneToOne
  @JoinColumn(name = "current_stack_id", unique = false, nullable = false, insertable = true, updatable = true)
  private StackEntity currentStack;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false),
      @JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false),
      @JoinColumn(name = "component_name", referencedColumnName = "component_name", nullable = false) })
  private ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity;

  @ManyToOne
  @JoinColumn(name = "host_id", referencedColumnName = "host_id", nullable = false)
  private HostEntity hostEntity;

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

  public State getCurrentState() {
    return currentState;
  }

  public void setCurrentState(State currentState) {
    this.currentState = currentState;
  }

  public SecurityState getSecurityState() {
    return securityState;
  }

  public void setSecurityState(SecurityState securityState) {
    this.securityState = securityState;
  }

  public UpgradeState getUpgradeState() {
    return upgradeState;
  }

  public void setUpgradeState(UpgradeState upgradeState) {
    this.upgradeState = upgradeState;
  }

  public StackEntity getCurrentStack() {
    return currentStack;
  }

  public void setCurrentStack(StackEntity currentStack) {
    this.currentStack = currentStack;
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

    if (componentName != null ? !componentName.equals(that.componentName)
        : that.componentName != null) {
      return false;
    }

    if (currentStack != null ? !currentStack.equals(that.currentStack)
        : that.currentStack != null) {
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

    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) {
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
    result = 31 * result + (hostEntity != null ? hostEntity.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (currentState != null ? currentState.hashCode() : 0);
    result = 31 * result + (upgradeState != null ? upgradeState.hashCode() : 0);
    result = 31 * result + (currentStack != null ? currentStack.hashCode() : 0);
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  public ServiceComponentDesiredStateEntity getServiceComponentDesiredStateEntity() {
    return serviceComponentDesiredStateEntity;
  }

  public void setServiceComponentDesiredStateEntity(
      ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    this.serviceComponentDesiredStateEntity = serviceComponentDesiredStateEntity;
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

}
