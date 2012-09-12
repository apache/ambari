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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.*;
import java.util.List;

@Table(name = "clusters", schema = "ambari", catalog = "")
@Entity
public class ClusterEntity {

  private static final Log log = LogFactory.getLog(ClusterEntity.class);

  private String clusterName;

  @Column(name = "cluster_name")
  @Id
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  private String desiredClusterState = "";

  @Column(name = "desired_cluster_state", nullable = false)
  @Basic
  public String getDesiredClusterState() {
    return desiredClusterState;
  }

  public void setDesiredClusterState(String desiredClusterState) {
    this.desiredClusterState = desiredClusterState;
  }

  private String clusterInfo = "";

  @Column(name = "cluster_info", nullable = false)
  @Basic
  public String getClusterInfo() {
    return clusterInfo;
  }

  public void setClusterInfo(String clusterInfo) {
    this.clusterInfo = clusterInfo;
  }

  List<HostEntity> hostEntities;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "clusterEntity")
  public List<HostEntity> getHostEntities() {
    return hostEntities;
  }

  public void setHostEntities(List<HostEntity> hostEntities) {
    this.hostEntities = hostEntities;
  }

  List<ClusterServiceEntity> clusterServiceEntities;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "clusterEntity")
  public List<ClusterServiceEntity> getClusterServiceEntities() {
    return clusterServiceEntities;
  }

  public void setClusterServiceEntities(List<ClusterServiceEntity> clusterServiceEntities) {
    this.clusterServiceEntities = clusterServiceEntities;
  }

  List<ActionStatusEntity> actionStatusEntities;

  @OneToMany(mappedBy = "clusterEntity")
  public List<ActionStatusEntity> getActionStatusEntities() {
    return actionStatusEntities;
  }

  public void setActionStatusEntities(List<ActionStatusEntity> actionStatusEntities) {
    this.actionStatusEntities = actionStatusEntities;
  }

  List<ComponentHostDesiredStateEntity> componentHostDesiredStateEntities;

  @OneToMany(mappedBy = "clusterEntity")
  public List<ComponentHostDesiredStateEntity> getComponentHostDesiredStateEntities() {
    return componentHostDesiredStateEntities;
  }

  public void setComponentHostDesiredStateEntities(List<ComponentHostDesiredStateEntity> componentHostDesiredStateEntities) {
    this.componentHostDesiredStateEntities = componentHostDesiredStateEntities;
  }

  private ClusterStateEntity clusterStateEntity;

  @OneToOne(mappedBy = "clusterEntity")
  public ClusterStateEntity getClusterStateEntity() {
    return clusterStateEntity;
  }

  public void setClusterStateEntity(ClusterStateEntity clusterStateEntity) {
    this.clusterStateEntity = clusterStateEntity;
  }

  private List<HostStateEntity> hostStateEntities;

  @OneToMany(mappedBy = "clusterEntity")
  public List<HostStateEntity> getHostStateEntities() {
    return hostStateEntities;
  }

  public void setHostStateEntities(List<HostStateEntity> hostStateEntities) {
    this.hostStateEntities = hostStateEntities;
  }

  private List<HostComponentStateEntity> hostComponentStateEntities;

  @OneToMany(mappedBy = "clusterEntity")
  public List<HostComponentStateEntity> getHostComponentStateEntities() {
    return hostComponentStateEntities;
  }

  public void setHostComponentStateEntities(List<HostComponentStateEntity> hostComponentStateEntities) {
    this.hostComponentStateEntities = hostComponentStateEntities;
  }

  private List<ServiceComponentStateEntity> serviceComponentStateEntities;

  @OneToMany(mappedBy = "clusterEntity")
  public List<ServiceComponentStateEntity> getServiceComponentStateEntities() {
    return serviceComponentStateEntities;
  }

  public void setServiceComponentStateEntities(List<ServiceComponentStateEntity> serviceComponentStateEntities) {
    this.serviceComponentStateEntities = serviceComponentStateEntities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClusterEntity that = (ClusterEntity) o;

    if (clusterInfo != null ? !clusterInfo.equals(that.clusterInfo) : that.clusterInfo != null) return false;
    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (desiredClusterState != null ? !desiredClusterState.equals(that.desiredClusterState) : that.desiredClusterState != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (desiredClusterState != null ? desiredClusterState.hashCode() : 0);
    result = 31 * result + (clusterInfo != null ? clusterInfo.hashCode() : 0);
    return result;
  }

}
