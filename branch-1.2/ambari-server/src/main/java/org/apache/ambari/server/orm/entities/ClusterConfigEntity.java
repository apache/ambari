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
import java.util.Collection;

@IdClass(ClusterConfigEntityPK.class)
@Table(name = "clusterconfig", schema = "ambari", catalog = "")
@Entity
public class ClusterConfigEntity {
  private Long clusterId;
  private String configJson;
  private String type;
  private String tag;
  private long timestamp;
  private Collection<HostComponentConfigMappingEntity> hostComponentConfigMappingEntities;
  private Collection<ServiceConfigMappingEntity> serviceConfigMappingEntities;
  private Collection<HostComponentDesiredConfigMappingEntity> hostComponentDesiredConfigMappingEntities;
  private Collection<ComponentConfigMappingEntity> componentConfigMappingEntities;
  
  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  @Id
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }
  
  @Column(name = "type_name")
  @Id
  public String getType() {
    return type;
  }
  
  public void setType(String typeName) {
    type = typeName;
  }
  
  @Column(name = "version_tag")
  @Id
  public String getTag() {
    return tag;
  }
  
  public void setTag(String versionTag) {
    tag = versionTag;
  }

  @Column(name = "config_data", nullable = false, insertable = true, updatable = false, length=32000)
  @Basic(fetch=FetchType.LAZY)
  public String getData() {
    return configJson;
  }

  public void setData(String data) {
    this.configJson = data;
  }
  
  @Column(name = "create_timestamp", nullable=false, insertable=true, updatable=false)
  public long getTimestamp() {
    return timestamp;
  }
  
  public void setTimestamp(long stamp) {
    timestamp = stamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClusterConfigEntity that = (ClusterConfigEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (configJson != null ? !configJson.equals(that.configJson) : that.configJson != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId !=null ? clusterId.intValue() : 0;
    result = 31 * result + (configJson != null ? configJson.hashCode() : 0);
    return result;
  }

  private ClusterEntity clusterEntity;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  @OneToMany(mappedBy = "clusterConfigEntity")
  public Collection<HostComponentConfigMappingEntity> getHostComponentConfigMappingEntities() {
    return hostComponentConfigMappingEntities;
  }

  public void setHostComponentConfigMappingEntities(Collection<HostComponentConfigMappingEntity> hostComponentConfigMappingEntities) {
    this.hostComponentConfigMappingEntities = hostComponentConfigMappingEntities;
  }

  @OneToMany(mappedBy = "clusterConfigEntity")
  public Collection<ServiceConfigMappingEntity> getServiceConfigMappingEntities() {
    return serviceConfigMappingEntities;
  }

  public void setServiceConfigMappingEntities(Collection<ServiceConfigMappingEntity> serviceConfigMappingEntities) {
    this.serviceConfigMappingEntities = serviceConfigMappingEntities;
  }

  @OneToMany(mappedBy = "clusterConfigEntity")
  public Collection<HostComponentDesiredConfigMappingEntity> getHostComponentDesiredConfigMappingEntities() {
    return hostComponentDesiredConfigMappingEntities;
  }

  public void setHostComponentDesiredConfigMappingEntities(Collection<HostComponentDesiredConfigMappingEntity> hostComponentDesiredConfigMappingEntities) {
    this.hostComponentDesiredConfigMappingEntities = hostComponentDesiredConfigMappingEntities;
  }

  @OneToMany(mappedBy = "clusterConfigEntity")
  public Collection<ComponentConfigMappingEntity> getComponentConfigMappingEntities() {
    return componentConfigMappingEntities;
  }

  public void setComponentConfigMappingEntities(Collection<ComponentConfigMappingEntity> componentConfigMappingEntities) {
    this.componentConfigMappingEntities = componentConfigMappingEntities;
  }
}
