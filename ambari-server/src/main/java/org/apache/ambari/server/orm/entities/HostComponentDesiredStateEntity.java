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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.State;

@javax.persistence.IdClass(HostComponentDesiredStateEntityPK.class)
@javax.persistence.Table(name = "hostcomponentdesiredstate")
@Entity
public class HostComponentDesiredStateEntity {

  @Id
  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Id
  @Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  private String serviceName;

  @Id
  @Column(name = "host_name", insertable = false, updatable = false)
  private String hostName = "";

  @Id
  @Column(name = "component_name", insertable = false, updatable = false)
  private String componentName = "";

  @Basic
  @Column(name = "desired_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private State desiredState = State.INIT;

  @Basic
  @Column(name = "desired_stack_version", insertable = true, updatable = true)
  private String desiredStackVersion = "";

  @Enumerated(value = EnumType.STRING)
  @Column(name = "admin_state", nullable = true, insertable = true, updatable = true)
  private HostComponentAdminState adminState;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumns({
      @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false),
      @JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false),
      @JoinColumn(name = "component_name", referencedColumnName = "component_name", nullable = false)})
  private ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity;

  @ManyToOne
  @JoinColumn(name = "host_name", referencedColumnName = "host_name", nullable = false)
  private HostEntity hostEntity;
  
  @Enumerated(value = EnumType.STRING)
  @Column(name="maintenance_state", nullable = false, insertable = true, updatable = true)
  private MaintenanceState maintenanceState = MaintenanceState.OFF;

  @Basic
  @Column(name = "restart_required", insertable = true, updatable = true, nullable = false)
  private Integer restartRequired = 0;

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
    return defaultString(hostName);
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getComponentName() {
    return defaultString(componentName);
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

  public String getDesiredStackVersion() {
    return defaultString(desiredStackVersion);
  }

  public void setDesiredStackVersion(String desiredStackVersion) {
    this.desiredStackVersion = desiredStackVersion;
  }


  public HostComponentAdminState getAdminState() {
    return adminState;
  }

  public void setAdminState(HostComponentAdminState attribute) {
    this.adminState = attribute;
  }
  
  public MaintenanceState getMaintenanceState() {
    return maintenanceState;
  }
  
  public void setMaintenanceState(MaintenanceState state) {
    maintenanceState = state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostComponentDesiredStateEntity that = (HostComponentDesiredStateEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) return false;
    if (desiredStackVersion != null ? !desiredStackVersion.equals(that.desiredStackVersion) : that.desiredStackVersion != null)
      return false;
    if (desiredState != null ? !desiredState.equals(that.desiredState) : that.desiredState != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.intValue() : 0;
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (desiredState != null ? desiredState.hashCode() : 0);
    result = 31 * result + (desiredStackVersion != null ? desiredStackVersion.hashCode() : 0);
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
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
}
