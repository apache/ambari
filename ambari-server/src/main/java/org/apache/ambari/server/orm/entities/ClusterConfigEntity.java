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
@Table(name = "clusterconfig")
@Entity
public class ClusterConfigEntity {

  @Id
  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Id
  @Column(name = "type_name")
  private String type;

  @Id
  @Column(name = "version_tag")
  private String tag;

  @Basic(fetch=FetchType.LAZY)
  @Column(name = "config_data", nullable = false, insertable = true, updatable = false, length=32000)
  @Lob
  private String configJson;

  @Column(name = "create_timestamp", nullable=false, insertable=true, updatable=false)
  private long timestamp;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  private ClusterEntity clusterEntity;

  @OneToMany(mappedBy = "clusterConfigEntity")
  private Collection<ConfigGroupConfigMappingEntity> configGroupConfigMappingEntities;

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }
  
  public String getType() {
    return type;
  }
  
  public void setType(String typeName) {
    type = typeName;
  }
  
  public String getTag() {
    return tag;
  }
  
  public void setTag(String versionTag) {
    tag = versionTag;
  }

  public String getData() {
    return configJson;
  }

  public void setData(String data) {
    this.configJson = data;
  }
  
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

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  public Collection<ConfigGroupConfigMappingEntity> getConfigGroupConfigMappingEntities() {
    return configGroupConfigMappingEntities;
  }

  public void setConfigGroupConfigMappingEntities(Collection<ConfigGroupConfigMappingEntity> configGroupConfigMappingEntities) {
    this.configGroupConfigMappingEntities = configGroupConfigMappingEntities;
  }
}
