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

@IdClass(HostComponentMappingEntityPK.class)
@Table(name = "hostcomponentmapping", schema = "ambari", catalog = "")
@Entity
public class HostComponentMappingEntity {

  private String clusterName = "";

  @Column(name = "cluster_name", insertable = false, updatable = false)
  @Id
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  private String serviceName = "";

  @Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  @Id
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  private Integer hostComponentMappingId;

  @Column(name = "host_component_mapping_id")
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  public Integer getHostComponentMappingId() {
    return hostComponentMappingId;
  }

  public void setHostComponentMappingId(Integer hostComponentMappingId) {
    this.hostComponentMappingId = hostComponentMappingId;
  }

  private String hostComponentMappingSnapshot;

  @Column(name = "host_component_mapping_snapshot")
  @Basic
  public String getHostComponentMappingSnapshot() {
    return hostComponentMappingSnapshot;
  }

  public void setHostComponentMappingSnapshot(String hostComponentMappingSnapshot) {
    this.hostComponentMappingSnapshot = hostComponentMappingSnapshot;
  }

  ClusterServiceEntity clusterServiceEntity;

  @ManyToOne
  @JoinColumns(value = {@JoinColumn(name = "cluster_name", referencedColumnName = "cluster_name"), @JoinColumn(name = "service_name", referencedColumnName = "service_name")})
  public ClusterServiceEntity getClusterServiceEntity() {
    return clusterServiceEntity;
  }

  public void setClusterServiceEntity(ClusterServiceEntity clusterServiceEntity) {
    this.clusterServiceEntity = clusterServiceEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostComponentMappingEntity that = (HostComponentMappingEntity) o;

    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (hostComponentMappingId != null ? !hostComponentMappingId.equals(that.hostComponentMappingId) : that.hostComponentMappingId != null)
      return false;
    if (hostComponentMappingSnapshot != null ? !hostComponentMappingSnapshot.equals(that.hostComponentMappingSnapshot) : that.hostComponentMappingSnapshot != null)
      return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (hostComponentMappingId != null ? hostComponentMappingId.hashCode() : 0);
    result = 31 * result + (hostComponentMappingSnapshot != null ? hostComponentMappingSnapshot.hashCode() : 0);
    return result;
  }
}
