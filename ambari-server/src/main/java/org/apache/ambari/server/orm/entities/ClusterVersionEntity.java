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

import org.apache.ambari.server.state.ClusterVersionState;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.TableGenerator;
import javax.persistence.Table;

import static org.apache.commons.lang.StringUtils.defaultString;

@Table(name = "cluster_version")
@Entity
@TableGenerator(name = "cluster_version_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "cluster_version_id_seq"
    , initialValue = 0
    , allocationSize = 1
)
@NamedQueries({
    @NamedQuery(name = "clusterVersionByClusterAndStackAndVersion", query =
        "SELECT clusterVersion FROM ClusterVersionEntity clusterVersion JOIN clusterVersion.clusterEntity cluster " +
        "WHERE cluster.clusterName=:clusterName AND clusterVersion.stack=:stack AND clusterVersion.version=:version"),
    @NamedQuery(name = "clusterVersionByClusterAndState", query =
        "SELECT clusterVersion FROM ClusterVersionEntity clusterVersion JOIN clusterVersion.clusterEntity cluster " +
        "WHERE cluster.clusterName=:clusterName AND clusterVersion.state=:state"),
    @NamedQuery(name = "clusterVersionByStackVersion",
        query = "SELECT clusterVersion FROM ClusterVersionEntity clusterVersion WHERE clusterVersion.stack=:stack AND clusterVersion.version=:version"),
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

  @Basic
  @Column(name = "stack", nullable = false, insertable = true, updatable = true)
  private String stack = "";

  @Basic
  @Column(name = "version", nullable = false, insertable = true, updatable = true)
  private String version = "";

  @Column(name = "state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private ClusterVersionState state = ClusterVersionState.CURRENT;

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
   * @param stack Stack name (e.g., HDP)
   * @param version Stack version (e.g., 2.2.0.0-995)
   * @param state Cluster version state
   * @param startTime Time the cluster version reached its first state
   * @param userName User who performed the action
   */
  public ClusterVersionEntity(ClusterEntity cluster, String stack, String version, ClusterVersionState state, long startTime, String userName) {
    this.clusterId = cluster.getClusterId();
    this.clusterEntity = cluster;
    this.stack = stack;
    this.version = version;
    this.state = state;
    this.startTime = startTime;
    this.userName = userName;
  }

  /**
   * Full constructor that does have the endTime
   * @param cluster Cluster entity
   * @param stack Stack name (e.g., HDP)
   * @param version Stack version (e.g., 2.2.0.0-995)
   * @param state Cluster version state
   * @param startTime Time the cluster version reached its first state
   * @param endTime Time the cluster version finalized its state
   * @param userName User who performed the action
   */
  public ClusterVersionEntity(ClusterEntity cluster, String stack, String version, ClusterVersionState state, long startTime, long endTime, String userName) {
    this(cluster, stack, version, state, startTime, userName);
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

  public String getStack() {
    return defaultString(stack);
  }

  public void setStack(String stack) {
    this.stack = stack;
  }

  public String getVersion() {
    return defaultString(version);
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public ClusterVersionState getState() {
    return state;
  }

  public void setState(ClusterVersionState state) {
    this.state = state;
  }

  public Long getStartTime() { return startTime; }

  public void setStartTime(Long startTime) { this.startTime = startTime; }

  public Long getEndTime() { return endTime; }

  public void setEndTime(Long endTime) { this.endTime = endTime; }

  public String getUserName() { return defaultString(userName); }

  public void setUserName(String userName) { this.userName = userName; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClusterVersionEntity that = (ClusterVersionEntity) o;

    if (this.id != that.id || this.clusterId != that.clusterId || !this.stack.equals(that.stack)
        || !this.version.equals(that.version) || !this.state.equals(that.state)
        || !this.startTime.equals(that.startTime) || !this.endTime.equals(that.endTime)
        || !this.userName.equals(that.userName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id !=null ? id.intValue() : 0;
    result = 31 * result + (clusterId != null ? clusterId.hashCode() : 0);
    result = 31 * result + (stack != null ? stack.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (endTime != null ? stack.hashCode() : 0);
    result = 31 * result + (userName != null ? userName.hashCode() : 0);
    return result;
  }
}
