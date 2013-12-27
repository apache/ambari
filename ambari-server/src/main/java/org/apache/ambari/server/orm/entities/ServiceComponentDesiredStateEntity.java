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

import org.apache.ambari.server.state.State;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.Collection;

import static org.apache.commons.lang.StringUtils.defaultString;

@javax.persistence.IdClass(ServiceComponentDesiredStateEntityPK.class)
@javax.persistence.Table(name = "servicecomponentdesiredstate")
@Entity
public class ServiceComponentDesiredStateEntity {

  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  @Id
  private Long clusterId;

  @Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  @Id
  private String serviceName;

  @Column(name = "component_name", nullable = false, insertable = true, updatable = true)
  @Id
  private String componentName;

  @Column(name = "desired_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(EnumType.STRING)
  private State desiredState = State.INIT;

  @Column(name = "desired_stack_version", insertable = true, updatable = true)
  @Basic
  private String desiredStackVersion = "";

  @ManyToOne
  @JoinColumns({@javax.persistence.JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false), @JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false)})
  private ClusterServiceEntity clusterServiceEntity;

  @OneToMany(mappedBy = "serviceComponentDesiredStateEntity", cascade = CascadeType.PERSIST)
  private Collection<HostComponentStateEntity> hostComponentStateEntities;

  @OneToMany(mappedBy = "serviceComponentDesiredStateEntity")
  private Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities;

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

  public String getDesiredStackVersion() {
    return defaultString(desiredStackVersion);
  }

  public void setDesiredStackVersion(String desiredStackVersion) {
    this.desiredStackVersion = desiredStackVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceComponentDesiredStateEntity that = (ServiceComponentDesiredStateEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) return false;
    if (desiredState != null ? !desiredState.equals(that.desiredState) : that.desiredState != null) return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
    if (desiredStackVersion != null ? !desiredStackVersion.equals(that.desiredStackVersion) : that.desiredStackVersion != null)
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.intValue() : 0;
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (desiredState != null ? desiredState.hashCode() : 0);
    result = 31 * result + (desiredStackVersion != null ? desiredStackVersion.hashCode() : 0);

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
