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

@Table(name = "host_version")
@Entity
@TableGenerator(name = "host_version_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "host_version_id_seq"
    , initialValue = 0
    , allocationSize = 1
)
@NamedQueries({
    @NamedQuery(name = "hostVersionByClusterAndStackAndVersion", query =
        "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN ClusterEntity cluster " +
            "WHERE cluster.clusterName=:clusterName AND hostVersion.repositoryVersion.stack=:stack AND hostVersion.repositoryVersion.version=:version"),

    @NamedQuery(name = "hostVersionByClusterAndHostname", query =
        "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN ClusterEntity cluster " +
            "WHERE cluster.clusterName=:clusterName AND hostVersion.hostName=:hostName"),

    @NamedQuery(name = "hostVersionByHostname", query =
        "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host " +
            "WHERE hostVersion.hostName=:hostName"),

    @NamedQuery(name = "hostVersionByClusterHostnameAndState", query =
        "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN ClusterEntity cluster " +
            "WHERE cluster.clusterName=:clusterName AND hostVersion.hostName=:hostName AND hostVersion.state=:state"),

    @NamedQuery(name = "hostVersionByClusterStackVersionAndHostname", query =
        "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN ClusterEntity cluster " +
            "WHERE cluster.clusterName=:clusterName AND hostVersion.repositoryVersion.stack=:stack AND hostVersion.repositoryVersion.version=:version AND " +
            "hostVersion.hostName=:hostName"),
})
public class HostVersionEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "host_version_id_generator")
  private Long id;

  @Column(name = "host_name", nullable = false, insertable = false, updatable = false)
  private String hostName;

  @ManyToOne
  @JoinColumn(name = "repo_version_id", referencedColumnName = "repo_version_id", nullable = false)
  private RepositoryVersionEntity repositoryVersion;

  @ManyToOne
  @JoinColumn(name = "host_name", referencedColumnName = "host_name", nullable = false)
  private HostEntity hostEntity;

  @Column(name = "state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private RepositoryVersionState state;

  /**
   * Empty constructor is needed by unit tests.
   */
  public HostVersionEntity() {
  }

  /**
   * When using this constructor, you should also call setHostEntity(). Otherwise
   * you will have persistence errors when persisting the instance.
   */
  public HostVersionEntity(String hostName, RepositoryVersionEntity repositoryVersion, RepositoryVersionState state) {
    this.hostName = hostName;
    this.repositoryVersion = repositoryVersion;
    this.state = state;
  }

  /**
   * This constructor is mainly used by the unit tests in order to construct an object without the id.
   */
  public HostVersionEntity(HostVersionEntity other) {
    this.hostName = other.hostName;
    this.repositoryVersion = other.repositoryVersion;
    this.state = other.state;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  public RepositoryVersionState getState() {
    return state;
  }

  public void setState(RepositoryVersionState state) {
    this.state = state;
  }

  public RepositoryVersionEntity getRepositoryVersion() {
    return repositoryVersion;
  }

  public void setRepositoryVersion(RepositoryVersionEntity repositoryVersion) {
    this.repositoryVersion = repositoryVersion;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((hostEntity == null) ? 0 : hostEntity.hashCode());
    result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((repositoryVersion == null) ? 0 : repositoryVersion.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;

    HostVersionEntity other = (HostVersionEntity) obj;
    if (hostEntity == null) {
      if (other.hostEntity != null) return false;
    } else if (!hostEntity.equals(other.hostEntity)) return false;
    if (hostName == null) {
      if (other.hostName != null) return false;
    } else if (!hostName.equals(other.hostName)) return false;
    if (id == null) {
      if (other.id != null) return false;
    } else if (!id.equals(other.id)) return false;
    if (repositoryVersion == null) {
      if (other.repositoryVersion != null) return false;
    } else if (!repositoryVersion.equals(other.repositoryVersion)) return false;
    if (state != other.state) return false;
    return true;
  }

}
