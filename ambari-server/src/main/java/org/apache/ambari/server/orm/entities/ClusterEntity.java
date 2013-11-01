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

import static org.apache.commons.lang.StringUtils.defaultString;

@Table(name = "clusters")
@NamedQueries({
    @NamedQuery(name = "clusterByName", query =
        "SELECT cluster " +
            "FROM ClusterEntity cluster " +
            "WHERE cluster.clusterName=:clusterName"),
    @NamedQuery(name = "allClusters", query =
        "SELECT clusters " +
            "FROM ClusterEntity clusters")
})
@Entity
@TableGenerator(name = "cluster_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "value"
    , pkColumnValue = "cluster_id_seq"
    , initialValue = 1
    , allocationSize = 1
)
public class ClusterEntity {

  @Id
  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = true)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "cluster_id_generator")
  private Long clusterId;

  @Basic
  @Column(name = "cluster_name", nullable = false, insertable = true,
      updatable = true, unique = true, length = 100)
  private String clusterName;

  @Basic
  @Column(name = "desired_cluster_state", insertable = true, updatable = true)
  private String desiredClusterState = "";

  @Basic
  @Column(name = "cluster_info", insertable = true, updatable = true)
  private String clusterInfo = "";

  @Basic
  @Column(name = "desired_stack_version", insertable = true, updatable = true)
  private String desiredStackVersion = "";

  @OneToMany(mappedBy = "clusterEntity")
  private Collection<ClusterServiceEntity> clusterServiceEntities;

  @OneToOne(mappedBy = "clusterEntity", cascade = CascadeType.REMOVE)
  private ClusterStateEntity clusterStateEntity;

  @ManyToMany(mappedBy = "clusterEntities")
  private Collection<HostEntity> hostEntities;

  @OneToMany(mappedBy = "cluster", cascade = {CascadeType.REMOVE, CascadeType.REFRESH})
  private Collection<StageEntity> stages;

  @OneToMany(mappedBy = "clusterEntity", cascade = CascadeType.ALL)
  private Collection<ClusterConfigEntity> configEntities;

  @OneToMany(mappedBy = "clusterEntity", cascade = CascadeType.ALL)
  private Collection<ClusterConfigMappingEntity> configMappingEntities;

  @OneToMany(mappedBy = "clusterEntity", cascade = CascadeType.ALL)
  private Collection<ConfigGroupEntity> configGroupEntities;

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getDesiredClusterState() {
    return defaultString(desiredClusterState);
  }

  public void setDesiredClusterState(String desiredClusterState) {
    this.desiredClusterState = desiredClusterState;
  }

  public String getClusterInfo() {
    return defaultString(clusterInfo);
  }

  public void setClusterInfo(String clusterInfo) {
    this.clusterInfo = clusterInfo;
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

    ClusterEntity that = (ClusterEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (clusterInfo != null ? !clusterInfo.equals(that.clusterInfo) : that.clusterInfo != null) return false;
    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (desiredClusterState != null ? !desiredClusterState.equals(that.desiredClusterState) : that.desiredClusterState != null)
      return false;
    if (desiredStackVersion != null ? !desiredStackVersion.equals(that.desiredStackVersion) : that.desiredStackVersion != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.intValue() : 0;
    result = 31 * result + (clusterName != null ? clusterName.hashCode() : 0);
    result = 31 * result + (desiredClusterState != null ? desiredClusterState.hashCode() : 0);
    result = 31 * result + (clusterInfo != null ? clusterInfo.hashCode() : 0);
    result = 31 * result + (desiredStackVersion != null ? desiredStackVersion.hashCode() : 0);
    return result;
  }

  public Collection<ClusterServiceEntity> getClusterServiceEntities() {
    return clusterServiceEntities;
  }

  public void setClusterServiceEntities(Collection<ClusterServiceEntity> clusterServiceEntities) {
    this.clusterServiceEntities = clusterServiceEntities;
  }

  public ClusterStateEntity getClusterStateEntity() {
    return clusterStateEntity;
  }

  public void setClusterStateEntity(ClusterStateEntity clusterStateEntity) {
    this.clusterStateEntity = clusterStateEntity;
  }

  public Collection<HostEntity> getHostEntities() {
    return hostEntities;
  }

  public void setHostEntities(Collection<HostEntity> hostEntities) {
    this.hostEntities = hostEntities;
  }

  public Collection<StageEntity> getStages() {
    return stages;
  }

  public void setStages(Collection<StageEntity> stages) {
    this.stages = stages;
  }

  public Collection<ClusterConfigEntity> getClusterConfigEntities() {
    return configEntities;
  }

  public void setClusterConfigEntities(Collection<ClusterConfigEntity> entities) {
    configEntities = entities;
  }

  public Collection<ClusterConfigMappingEntity> getConfigMappingEntities() {
    return configMappingEntities;
  }
  
  public void setConfigMappingEntities(Collection<ClusterConfigMappingEntity> entities) {
    configMappingEntities = entities;
  }

  public Collection<ConfigGroupEntity> getConfigGroupEntities() {
    return configGroupEntities;
  }

  public void setConfigGroupEntities(Collection<ConfigGroupEntity> configGroupEntities) {
    this.configGroupEntities = configGroupEntities;
  }
}
