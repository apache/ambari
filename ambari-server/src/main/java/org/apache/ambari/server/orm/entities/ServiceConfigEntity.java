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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import java.util.List;

@Entity
@Table(name = "serviceconfig")
@TableGenerator(name = "service_config_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "value"
  , pkColumnValue = "service_config_id_seq"
  , initialValue = 1
  , allocationSize = 1
)
public class ServiceConfigEntity {
  @Id
  @Column(name = "service_config_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "service_config_id_generator")
  private Long serviceConfigId;

  @Basic
  @Column(name = "cluster_id", insertable = false, updatable = false, nullable = false)
  private Long clusterId;

  @Basic
  @Column(name = "service_name", nullable = false)
  private String serviceName;

  @Basic
  @Column(name = "version", nullable = false)
  private Long version;

  @Basic
  @Column(name = "create_timestamp", nullable = false)
  private Long createTimestamp = System.currentTimeMillis();

  @Basic
  @Column(name = "user_name")
  private String user = "_db";

  @Basic
  @Column(name = "note")
  private String note;

  @ManyToMany
  @JoinTable(
    name = "serviceconfigmapping",
    joinColumns = {@JoinColumn(name = "service_config_id", referencedColumnName = "service_config_id")},
    inverseJoinColumns = {@JoinColumn(name = "config_id", referencedColumnName = "config_id")}
  )
  private List<ClusterConfigEntity> clusterConfigEntities;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  private ClusterEntity clusterEntity;

  public Long getServiceConfigId() {
    return serviceConfigId;
  }

  public void setServiceConfigId(Long serviceConfigId) {
    this.serviceConfigId = serviceConfigId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public Long getCreateTimestamp() {
    return createTimestamp;
  }

  public void setCreateTimestamp(Long create_timestamp) {
    this.createTimestamp = create_timestamp;
  }

  public List<ClusterConfigEntity> getClusterConfigEntities() {
    return clusterConfigEntities;
  }

  public void setClusterConfigEntities(List<ClusterConfigEntity> clusterConfigEntities) {
    this.clusterConfigEntities = clusterConfigEntities;
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

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
