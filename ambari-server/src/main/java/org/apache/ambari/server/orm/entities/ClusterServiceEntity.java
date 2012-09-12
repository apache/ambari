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
import java.util.List;

@IdClass(ClusterServiceEntityPK.class)
@Table(name = "clusterservices", schema = "ambari", catalog = "")
@Entity
public class ClusterServiceEntity {

  private String clusterName;

  @Column(name = "cluster_name", nullable = false, insertable = false, updatable = false)
  @Id
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  private String serviceName;

  @Column(name = "service_name")
  @Id
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  private Integer serviceEnabled = 0;

  @Column(name = "service_enabled", nullable = false)
  @Basic
  public Integer getServiceEnabled() {
    return serviceEnabled;
  }

  public void setServiceEnabled(Integer serviceEnabled) {
    this.serviceEnabled = serviceEnabled;
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

  private List<ServiceConfigEntity> serviceConfigEntities;

  @OneToMany(mappedBy = "clusterServiceEntity")
  public List<ServiceConfigEntity> getServiceConfigEntities() {
    return serviceConfigEntities;
  }

  public void setServiceConfigEntities(List<ServiceConfigEntity> serviceConfigEntities) {
    this.serviceConfigEntities = serviceConfigEntities;
  }

  private List<ServiceComponentConfigEntity> serviceComponentConfigEntities;

  @OneToMany(mappedBy = "clusterServiceEntity")
  public List<ServiceComponentConfigEntity> getServiceComponentConfigEntities() {
    return serviceComponentConfigEntities;
  }

  public void setServiceComponentConfigEntities(List<ServiceComponentConfigEntity> serviceComponentConfigEntities) {
    this.serviceComponentConfigEntities = serviceComponentConfigEntities;
  }

  private List<ServiceComponentHostConfigEntity> serviceComponentHostConfigEntities;

  @OneToMany(mappedBy = "clusterServiceEntity")
  public List<ServiceComponentHostConfigEntity> getServiceComponentHostConfigEntities() {
    return serviceComponentHostConfigEntities;
  }

  public void setServiceComponentHostConfigEntities(List<ServiceComponentHostConfigEntity> serviceComponentHostConfigEntities) {
    this.serviceComponentHostConfigEntities = serviceComponentHostConfigEntities;
  }

  private List<ServiceDesiredStateEntity> serviceDesiredStateEntities;

  @OneToMany(mappedBy = "clusterServiceEntity")
  public List<ServiceDesiredStateEntity> getServiceDesiredStateEntities() {
    return serviceDesiredStateEntities;
  }

  public void setServiceDesiredStateEntities(List<ServiceDesiredStateEntity> serviceDesiredStateEntities) {
    this.serviceDesiredStateEntities = serviceDesiredStateEntities;
  }

  private List<HostComponentMappingEntity> hostComponentMappingEntities;

  @OneToMany(mappedBy = "clusterServiceEntity")
  public List<HostComponentMappingEntity> getHostComponentMappingEntities() {
    return hostComponentMappingEntities;
  }

  public void setHostComponentMappingEntities(List<HostComponentMappingEntity> hostComponentMappingEntities) {
    this.hostComponentMappingEntities = hostComponentMappingEntities;
  }

  private List<ServiceStateEntity> serviceStateEntities;

  @OneToMany(mappedBy = "clusterServiceEntity")
  public List<ServiceStateEntity> getServiceStateEntities() {
    return serviceStateEntities;
  }

  public void setServiceStateEntities(List<ServiceStateEntity> serviceStateEntities) {
    this.serviceStateEntities = serviceStateEntities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClusterServiceEntity that = (ClusterServiceEntity) o;

    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (serviceEnabled != null ? !serviceEnabled.equals(that.serviceEnabled) : that.serviceEnabled != null)
      return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (serviceEnabled != null ? serviceEnabled.hashCode() : 0);
    return result;
  }
}
