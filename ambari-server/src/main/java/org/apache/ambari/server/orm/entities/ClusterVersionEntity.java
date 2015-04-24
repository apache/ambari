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

import static org.apache.commons.lang.StringUtils.defaultString;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.state.RepositoryVersionState;

@Table(name = "cluster_version")
@Entity
@TableGenerator(name = "cluster_version_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "cluster_version_id_seq"
    , initialValue = 0
)
@NamedQueries({
    @NamedQuery(name = "clusterVersionByClusterAndStackAndVersion", query =
        "SELECT clusterVersion FROM ClusterVersionEntity clusterVersion JOIN clusterVersion.clusterEntity cluster " +
        "WHERE cluster.clusterName=:clusterName AND clusterVersion.repositoryVersion.stack.stackName=:stackName AND clusterVersion.repositoryVersion.stack.stackVersion=:stackVersion AND clusterVersion.repositoryVersion.version=:version"),
    @NamedQuery(name = "clusterVersionByClusterAndState", query =
        "SELECT clusterVersion FROM ClusterVersionEntity clusterVersion JOIN clusterVersion.clusterEntity cluster " +
        "WHERE cluster.clusterName=:clusterName AND clusterVersion.state=:state"),
    @NamedQuery(name = "clusterVersionByCluster", query =
        "SELECT clusterVersion FROM ClusterVersionEntity clusterVersion JOIN clusterVersion.clusterEntity cluster " +
        "WHERE cluster.clusterName=:clusterName"),
    @NamedQuery(name = "clusterVersionByStackVersion", query = "SELECT clusterVersion FROM ClusterVersionEntity clusterVersion WHERE clusterVersion.repositoryVersion.stack.stackName=:stackName AND clusterVersion.repositoryVersion.stack.stackVersion=:stackVersion AND clusterVersion.repositoryVersion.version=:version"),
})
public class ClusterVersionEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "cluster_version_id_generator")
  private Long id;

  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false)
  private Long clusterId;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  private ClusterEntity clusterEntity;

  @ManyToOne
  @JoinColumn(name = "repo_version_id", referencedColumnName = "repo_version_id", nullable = false)
  private RepositoryVersionEntity repositoryVersion;

  @Column(name = "state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private RepositoryVersionState state = RepositoryVersionState.CURRENT;

  @Basic
  @Column(name = "start_time", nullable = false, insertable = true, updatable = true)
  private Long startTime = System.currentTimeMillis();

  @Basic
  @Column(name = "end_time", insertable = true, updatable = true)
  private Long endTime;

  @Basic
  @Column(name = "user_name", insertable = true, updatable = true)
  private String userName = "";

  /**
   * Empty constructor primarily used by unit tests.
   */
  public ClusterVersionEntity() {
  }

  /**
   * Full constructor that doesn't have the endTime
   * @param cluster Cluster entity
   * @param repositoryVersion repository version
   * @param state Cluster version state
   * @param startTime Time the cluster version reached its first state
   * @param userName User who performed the action
   */
  public ClusterVersionEntity(ClusterEntity cluster, RepositoryVersionEntity repositoryVersion, RepositoryVersionState state, long startTime, String userName) {
    clusterId = cluster.getClusterId();
    this.repositoryVersion = repositoryVersion;
    clusterEntity = cluster;
    this.state = state;
    this.startTime = startTime;
    this.userName = userName;
  }

  /**
   * Full constructor that does have the endTime
   * @param cluster Cluster entity
   * @param repositoryVersion repository version
   * @param state Cluster version state
   * @param startTime Time the cluster version reached its first state
   * @param endTime Time the cluster version finalized its state
   * @param userName User who performed the action
   */
  public ClusterVersionEntity(ClusterEntity cluster, RepositoryVersionEntity repositoryVersion, RepositoryVersionState state, long startTime, long endTime, String userName) {
    this(cluster, repositoryVersion, state, startTime, userName);
    this.endTime = endTime;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  public RepositoryVersionState getState() {
    return state;
  }

  public void setState(RepositoryVersionState state) {
    this.state = state;
  }

  public Long getStartTime() { return startTime; }

  public void setStartTime(Long startTime) { this.startTime = startTime; }

  public Long getEndTime() { return endTime; }

  public void setEndTime(Long endTime) { this.endTime = endTime; }

  public String getUserName() { return defaultString(userName); }

  public void setUserName(String userName) { this.userName = userName; }

  public void setRepositoryVersion(RepositoryVersionEntity repositoryVersion) {
    this.repositoryVersion = repositoryVersion;
  }

  public RepositoryVersionEntity getRepositoryVersion() {
    return repositoryVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClusterVersionEntity that = (ClusterVersionEntity) o;

    if (id != that.id
        || clusterId != that.clusterId
        || !repositoryVersion.equals(that.repositoryVersion)
        || !state.equals(that.state)
        || !startTime.equals(that.startTime)
        || !endTime.equals(that.endTime)
        || !userName.equals(that.userName)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = id !=null ? id.intValue() : 0;
    result = 31 * result + (clusterId != null ? clusterId.hashCode() : 0);
    result = 31 * result + (repositoryVersion != null ? repositoryVersion.hashCode() : 0);
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    result = 31 * result + (userName != null ? userName.hashCode() : 0);
    return result;
  }
}
