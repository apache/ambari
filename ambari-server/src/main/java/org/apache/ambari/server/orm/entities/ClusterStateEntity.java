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

@javax.persistence.Table(name = "clusterstate", schema = "ambari", catalog = "")
@Entity
public class ClusterStateEntity {

  private String clusterName;

  @javax.persistence.Column(name = "cluster_name", insertable = false, updatable = false)
  @Id
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  private String currentClusterState = "";

  @javax.persistence.Column(name = "current_cluster_state", nullable = false)
  @Basic
  public String getCurrentClusterState() {
    return currentClusterState;
  }

  public void setCurrentClusterState(String currentClusterState) {
    this.currentClusterState = currentClusterState;
  }

  private ClusterEntity clusterEntity;

  @OneToOne
  @JoinColumn(name = "cluster_name")
  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClusterStateEntity that = (ClusterStateEntity) o;

    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (currentClusterState != null ? !currentClusterState.equals(that.currentClusterState) : that.currentClusterState != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (currentClusterState != null ? currentClusterState.hashCode() : 0);
    return result;
  }
}
