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

import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;

import javax.persistence.*;

import static org.apache.commons.lang.StringUtils.defaultString;

@IdClass(HostComponentStateEntityPK.class)
@Table(name = "hostcomponentstate")
@Entity
@NamedQueries({
    @NamedQuery(name = "HostComponentStateEntity.findAll", query = "SELECT hcs from HostComponentStateEntity hcs"),
    @NamedQuery(name = "HostComponentStateEntity.findByHost", query =
        "SELECT hcs from HostComponentStateEntity hcs WHERE hcs.hostEntity.hostName=:hostName"),
})
public class HostComponentStateEntity {

  @Id
  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Id
  @Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  private String serviceName;

  @Id
  @Column(name = "host_id", nullable=false, insertable = false, updatable = false)
  private Long hostId;

  @Id
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

  @Basic
  @Column(name = "current_stack_version", nullable = false, insertable = true, updatable = true)
  private String currentStackVersion;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumns({
      @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false),
      @JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false),
      @JoinColumn(name = "component_name", referencedColumnName = "component_name", nullable = false)})
  private ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity;

  @ManyToOne
  @JoinColumn(name = "host_id", referencedColumnName = "host_id", nullable = false)
  private HostEntity hostEntity;

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
    return this.hostEntity.getHostName();
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

  public String getCurrentStackVersion() {
    return currentStackVersion;
  }

  public void setCurrentStackVersion(String currentStackVersion) {
    this.currentStackVersion = currentStackVersion;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostComponentStateEntity that = (HostComponentStateEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) return false;
    if (currentStackVersion != null ? !currentStackVersion.equals(that.currentStackVersion) : that.currentStackVersion != null)
      return false;
    if (currentState != null ? !currentState.equals(that.currentState) : that.currentState != null) return false;
    if (upgradeState != null ? !upgradeState.equals(that.upgradeState) : that.upgradeState != null) return false;
    if (hostEntity != null ? !hostEntity.equals(that.hostEntity) : that.hostEntity != null) return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
    if (version != null ? !version.equals(that.version) : that.version != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.intValue() : 0;
    result = 31 * result + (hostEntity != null ? hostEntity.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (currentState != null ? currentState.hashCode() : 0);
    result = 31 * result + (upgradeState != null ? upgradeState.hashCode() : 0);
    result = 31 * result + (currentStackVersion != null ? currentStackVersion.hashCode() : 0);
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
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

}
