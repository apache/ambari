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

@javax.persistence.Table(name = "clusters", schema = "ambari", catalog = "")
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
@SequenceGenerator(name = "ambari.clusters_cluster_id_seq", allocationSize = 1)
public class ClusterEntity {
  private Long clusterId;

  @javax.persistence.Column(name = "cluster_id", nullable = false, insertable = true, updatable = true)
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ambari.clusters_cluster_id_seq")
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  private String clusterName;

  @javax.persistence.Column(name = "cluster_name", nullable = false, insertable = true,
          updatable = true, unique = true, length = 100)
  @Basic
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  private String desiredClusterState = "";

  @javax.persistence.Column(name = "desired_cluster_state", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getDesiredClusterState() {
    return desiredClusterState;
  }

  public void setDesiredClusterState(String desiredClusterState) {
    this.desiredClusterState = desiredClusterState;
  }

  private String clusterInfo = "";

  @javax.persistence.Column(name = "cluster_info", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getClusterInfo() {
    return clusterInfo;
  }

  public void setClusterInfo(String clusterInfo) {
    this.clusterInfo = clusterInfo;
  }

  private String desiredStackVersion = "";

  @javax.persistence.Column(name = "desired_stack_version", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getDesiredStackVersion() {
    return desiredStackVersion;
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

  private Collection<ClusterServiceEntity> clusterServiceEntities;

  @OneToMany(mappedBy = "clusterEntity")
  public Collection<ClusterServiceEntity> getClusterServiceEntities() {
    return clusterServiceEntities;
  }

  public void setClusterServiceEntities(Collection<ClusterServiceEntity> clusterServiceEntities) {
    this.clusterServiceEntities = clusterServiceEntities;
  }

  private ClusterStateEntity clusterStateEntity;

  @OneToOne(mappedBy = "clusterEntity")
  public ClusterStateEntity getClusterStateEntity() {
    return clusterStateEntity;
  }

  public void setClusterStateEntity(ClusterStateEntity clusterStateEntity) {
    this.clusterStateEntity = clusterStateEntity;
  }

  private Collection<HostEntity> hostEntities;

  @ManyToMany(mappedBy = "clusterEntities")
  public Collection<HostEntity> getHostEntities() {
    return hostEntities;
  }

  public void setHostEntities(Collection<HostEntity> hostEntities) {
    this.hostEntities = hostEntities;
  }

  private Collection<StageEntity> stages;

  @OneToMany(mappedBy = "cluster")
  public Collection<StageEntity> getStages() {
    return stages;
  }

  public void setStages(Collection<StageEntity> stages) {
    this.stages = stages;
  }
  
  private Collection<ClusterConfigEntity> configEntities;
  @OneToMany(mappedBy = "clusterEntity")
  public Collection<ClusterConfigEntity> getClusterConfigEntities() {
    return configEntities;
  }
  
  public void setClusterConfigEntities(Collection<ClusterConfigEntity> entities) {
    configEntities = entities;
  }
}
