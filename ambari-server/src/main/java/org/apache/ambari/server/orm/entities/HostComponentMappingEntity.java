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

@javax.persistence.IdClass(HostComponentMappingEntityPK.class)
@javax.persistence.Table(name = "hostcomponentmapping", schema = "ambari", catalog = "")
@Entity
@SequenceGenerator(name = "ambari.hostcomponentmapping_host_component_mapping_id_seq", allocationSize = 1)
public class HostComponentMappingEntity {
  private Long clusterId;

  @javax.persistence.Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  @Id
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  private String serviceName = "";

  @javax.persistence.Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  @Id
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  private Integer hostComponentMappingId;

  @javax.persistence.Column(name = "host_component_mapping_id", nullable = false, insertable = true, updatable = true, length = 10)
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ambari.hostcomponentmapping_host_component_mapping_id_seq")
  public Integer getHostComponentMappingId() {
    return hostComponentMappingId;
  }

  public void setHostComponentMappingId(Integer hostComponentMappingId) {
    this.hostComponentMappingId = hostComponentMappingId;
  }

  private String hostComponentMappingSnapshot;

  @javax.persistence.Column(name = "host_component_mapping_snapshot", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getHostComponentMappingSnapshot() {
    return hostComponentMappingSnapshot;
  }

  public void setHostComponentMappingSnapshot(String hostComponentMappingSnapshot) {
    this.hostComponentMappingSnapshot = hostComponentMappingSnapshot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostComponentMappingEntity that = (HostComponentMappingEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (hostComponentMappingId != null ? !hostComponentMappingId.equals(that.hostComponentMappingId) : that.hostComponentMappingId != null) return false;
    if (hostComponentMappingSnapshot != null ? !hostComponentMappingSnapshot.equals(that.hostComponentMappingSnapshot) : that.hostComponentMappingSnapshot != null)
      return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId !=null ? clusterId.intValue() : 0;
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (hostComponentMappingId != null ? hostComponentMappingId.hashCode() : 0);
    result = 31 * result + (hostComponentMappingSnapshot != null ? hostComponentMappingSnapshot.hashCode() : 0);
    return result;
  }

  private ClusterServiceEntity clusterServiceEntity;

  @ManyToOne
  @javax.persistence.JoinColumns({@javax.persistence.JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false), @javax.persistence.JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false)})
  public ClusterServiceEntity getClusterServiceEntity() {
    return clusterServiceEntity;
  }

  public void setClusterServiceEntity(ClusterServiceEntity clusterServiceEntity) {
    this.clusterServiceEntity = clusterServiceEntity;
  }
}
