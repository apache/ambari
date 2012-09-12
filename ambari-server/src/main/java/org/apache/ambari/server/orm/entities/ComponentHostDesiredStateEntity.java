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

import javax.persistence.*;

@javax.persistence.IdClass(ComponentHostDesiredStateEntityPK.class)
@javax.persistence.Table(name = "componenthostdesiredstate", schema = "ambari", catalog = "")
@Entity
public class ComponentHostDesiredStateEntity {

  private String clusterName = "";

  @javax.persistence.Column(name = "cluster_name", insertable = false, updatable = false)
  @Id
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
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

  private String componentName = "";

  @javax.persistence.Column(name = "component_name", nullable = false)
  @Id
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  private String desiredState = "";

  @javax.persistence.Column(name = "desired_state", nullable = false)
  @Basic
  public String getDesiredState() {
    return desiredState;
  }

  public void setDesiredState(String desiredState) {
    this.desiredState = desiredState;
  }

  private String desiredConfigVersion = "";

  @javax.persistence.Column(name = "desired_config_version", nullable = false)
  @Basic
  public String getDesiredConfigVersion() {
    return desiredConfigVersion;
  }

  public void setDesiredConfigVersion(String desiredConfigVersion) {
    this.desiredConfigVersion = desiredConfigVersion;
  }

  private String desiredStackVersion = "";

  @javax.persistence.Column(name = "desired_stack_version", nullable = false)
  @Basic
  public String getDesiredStackVersion() {
    return desiredStackVersion;
  }

  public void setDesiredStackVersion(String desiredStackVersion) {
    this.desiredStackVersion = desiredStackVersion;
  }

  private ClusterEntity clusterEntity;

  @ManyToOne
  @JoinColumn(name = "cluster_name")
  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  private HostEntity hostEntity;

  @ManyToOne
  @JoinColumn(name = "host_name")
  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ComponentHostDesiredStateEntity that = (ComponentHostDesiredStateEntity) o;

    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) return false;
    if (desiredConfigVersion != null ? !desiredConfigVersion.equals(that.desiredConfigVersion) : that.desiredConfigVersion != null)
      return false;
    if (desiredStackVersion != null ? !desiredStackVersion.equals(that.desiredStackVersion) : that.desiredStackVersion != null)
      return false;
    if (desiredState != null ? !desiredState.equals(that.desiredState) : that.desiredState != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (desiredState != null ? desiredState.hashCode() : 0);
    result = 31 * result + (desiredConfigVersion != null ? desiredConfigVersion.hashCode() : 0);
    result = 31 * result + (desiredStackVersion != null ? desiredStackVersion.hashCode() : 0);
    return result;
  }
}
