/*
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Entity that represents a host config mapping and override.
 */
@Table(name = "hostconfigmapping")
@Entity
@IdClass(HostConfigMappingEntityPK.class)
@NamedQueries({
    @NamedQuery(name = "HostConfigMappingEntity.findAll",
        query = "SELECT entity FROM HostConfigMappingEntity entity"),
    @NamedQuery(name = "HostConfigMappingEntity.findByHostId",
        query = "SELECT entity FROM HostConfigMappingEntity entity WHERE entity.hostId = :hostId")
})
public class HostConfigMappingEntity {

  @Id
  @Column(name = "cluster_id", insertable = false, updatable = false, nullable = false)
  private Long clusterId;

  @Id
  @Column(name = "host_id", insertable = true, updatable = false, nullable=false)
  private Long hostId;

  @Id
  @Column(name = "type_name", insertable = true, updatable = false, nullable = false)
  private String type;

  @Id
  @Column(name = "create_timestamp", insertable = true, updatable = false, nullable = false)
  private Long createTimestamp;

  @Column(name = "version_tag", insertable = true, updatable = false, nullable = false)
  private String versionTag;

  @Column(name = "service_id", insertable = false, updatable = false, nullable = false)
  private Long serviceId;

  @Column(name = "service_group_id", insertable = false, updatable = false, nullable = false)
  private Long serviceGroupId;

  @ManyToOne
  @JoinColumns(
      {
          @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false),
          @JoinColumn(name = "service_group_id", referencedColumnName = "service_group_id", nullable = false),
          @JoinColumn(name = "service_id", referencedColumnName = "id", nullable = false)
      })
  private ClusterServiceEntity clusterServiceEntity;

  @Column(name = "selected", insertable = true, updatable = true, nullable = false)
  private int selected = 0;
  
  @Column(name = "user_name", insertable = true, updatable = true, nullable = false)
  private String user = null;

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long id) {
    clusterId = id;
  }

  public Long getHostId() {
    return hostId;
  }

  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Long getCreateTimestamp() {
    return createTimestamp;
  }

  public void setCreateTimestamp(Long timestamp) {
    createTimestamp = timestamp;
  }

  public String getVersion() {
    return versionTag;
  }

  public void setVersion(String version) {
    versionTag = version;
  }

  public int isSelected() {
    return selected;
  }

  public void setSelected(int selected) {
    this.selected = selected;
  }

  /**
   * @return the user
   */
  public String getUser() {
    return user;
  }
  
  /**
   * @param userName the user
   */
  public void setUser(String userName) {
    user = userName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostConfigMappingEntity that = (HostConfigMappingEntity) o;

    if (selected != that.selected) return false;
    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (createTimestamp != null ? !createTimestamp.equals(that.createTimestamp) : that.createTimestamp != null)
      return false;
    if (hostId != null ? !hostId.equals(that.hostId) : that.hostId != null) return false;
    if (serviceGroupId != null ? !serviceGroupId.equals(that.serviceGroupId) : that.serviceGroupId != null) return false;
    if (serviceId != null ? !serviceId.equals(that.serviceId) : that.serviceId != null) return false;
    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (user != null ? !user.equals(that.user) : that.user != null) return false;
    if (versionTag != null ? !versionTag.equals(that.versionTag) : that.versionTag != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.hashCode() : 0;
    result = 31 * result + (hostId != null ? hostId.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (createTimestamp != null ? createTimestamp.hashCode() : 0);
    result = 31 * result + (versionTag != null ? versionTag.hashCode() : 0);
    result = 31 * result + (serviceGroupId != null ? serviceGroupId.hashCode() : 0);
    result = 31 * result + (serviceId != null ? serviceId.hashCode() : 0);
    result = 31 * result + selected;
    result = 31 * result + (user != null ? user.hashCode() : 0);
    return result;
  }

  public Long getServiceId() {
    return serviceId;
  }

  public void setServiceId(Long serviceId) {
    this.serviceId = serviceId;
  }

  public Long getServiceGroupId() {
    return serviceGroupId;
  }

  public void setServiceGroupId(Long serviceGroupId) {
    this.serviceGroupId = serviceGroupId;
  }

  public ClusterServiceEntity getClusterServiceEntity() {
    return clusterServiceEntity;
  }

  public void setClusterServiceEntity(ClusterServiceEntity clusterServiceEntity) {
    this.clusterServiceEntity = clusterServiceEntity;
  }
}
