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

import java.util.Collection;

import org.apache.ambari.server.state.State;

import javax.persistence.*;

@javax.persistence.IdClass(HostComponentStateEntityPK.class)
@javax.persistence.Table(name = "hostcomponentstate", schema = "ambari", catalog = "")
@Entity
public class HostComponentStateEntity {
  private Long clusterId;

  @javax.persistence.Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  @Id
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  private String serviceName;

  @javax.persistence.Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  @Id
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  private String hostName = "";

  @javax.persistence.Column(name = "host_name", nullable = false, insertable = false, updatable = false)
  @Id
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  private String componentName;

  @javax.persistence.Column(name = "component_name", nullable = false, insertable = false, updatable = false)
  @Id
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  private State currentState = State.INIT;

  @javax.persistence.Column(name = "current_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  public State getCurrentState() {
    return currentState;
  }

  public void setCurrentState(State currentState) {
    this.currentState = currentState;
  }

  private String currentStackVersion;

  @javax.persistence.Column(name = "current_stack_version", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getCurrentStackVersion() {
    return currentStackVersion;
  }

  public void setCurrentStackVersion(String currentStackVersion) {
    this.currentStackVersion = currentStackVersion;
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
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.intValue() : 0;
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (currentState != null ? currentState.hashCode() : 0);
    result = 31 * result + (currentStackVersion != null ? currentStackVersion.hashCode() : 0);
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    return result;
  }

  private ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false),
      @JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false),
      @JoinColumn(name = "component_name", referencedColumnName = "component_name", nullable = false)})
  public ServiceComponentDesiredStateEntity getServiceComponentDesiredStateEntity() {
    return serviceComponentDesiredStateEntity;
  }

  public void setServiceComponentDesiredStateEntity(ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    this.serviceComponentDesiredStateEntity = serviceComponentDesiredStateEntity;
  }

  private HostEntity hostEntity;

  @ManyToOne
  @JoinColumn(name = "host_name", referencedColumnName = "host_name", nullable = false)
  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  private Collection<HostComponentConfigMappingEntity> configMappingEntities;
  @OneToMany(mappedBy = "hostComponentStateEntity", cascade = CascadeType.ALL)
  public Collection<HostComponentConfigMappingEntity> getHostComponentConfigMappingEntities() {
    return configMappingEntities;
  }

  public void setHostComponentConfigMappingEntities(Collection<HostComponentConfigMappingEntity> entities) {
    configMappingEntities = entities;
  }



}
