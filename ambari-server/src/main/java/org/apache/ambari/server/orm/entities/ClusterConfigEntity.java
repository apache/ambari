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

import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "clusterconfig",
  uniqueConstraints = {@UniqueConstraint(name = "UQ_config_type_tag", columnNames = {"cluster_id", "type_name", "version_tag"}),
    @UniqueConstraint(name = "UQ_config_type_version", columnNames = {"cluster_id", "type_name", "version"})})
@TableGenerator(name = "config_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "config_id_seq"
  , initialValue = 1
)
@NamedQueries({
    @NamedQuery(name = "ClusterConfigEntity.findNextConfigVersion", query = "SELECT COALESCE(MAX(clusterConfig.version),0) + 1 as nextVersion FROM ClusterConfigEntity clusterConfig WHERE clusterConfig.type=:configType AND clusterConfig.clusterId=:clusterId"),
    @NamedQuery(name = "ClusterConfigEntity.findAllConfigsByStack", query = "SELECT clusterConfig FROM ClusterConfigEntity clusterConfig WHERE clusterConfig.clusterId=:clusterId AND clusterConfig.stack=:stack"),
    @NamedQuery(name = "ClusterConfigEntity.findLatestConfigsByStack", query = "SELECT clusterConfig FROM ClusterConfigEntity clusterConfig WHERE clusterConfig.clusterId=:clusterId AND clusterConfig.timestamp = (SELECT MAX(clusterConfig2.timestamp) FROM ClusterConfigEntity clusterConfig2 WHERE clusterConfig2.clusterId=:clusterId AND clusterConfig2.stack=:stack AND clusterConfig2.type = clusterConfig.type)"),
    @NamedQuery(name = "ClusterConfigEntity.findClusterConfigMappingsByStack",
      query = "SELECT mapping FROM ClusterConfigMappingEntity mapping " +
        "JOIN ClusterConfigEntity config ON mapping.typeName = config.type AND mapping.tag = config.tag " +
        "WHERE mapping.clusterId = :clusterId AND config.stack = :stack")
})
public class ClusterConfigEntity {

  @Id
  @Column(name = "config_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "config_id_generator")
  private Long configId;

  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Column(name = "type_name")
  private String type;

  @Column(name = "version")
  private Long version;

  @Column(name = "version_tag")
  private String tag;

  @Basic(fetch = FetchType.LAZY)
  @Column(name = "config_data", nullable = false, insertable = true)
  @Lob
  private String configJson;

  @Basic(fetch = FetchType.LAZY)
  @Column(name = "config_attributes", nullable = true, insertable = true)
  @Lob
  private String configAttributesJson;

  @Column(name = "create_timestamp", nullable = false, insertable = true, updatable = false)
  private long timestamp;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  private ClusterEntity clusterEntity;

  @OneToMany(mappedBy = "clusterConfigEntity")
  private Collection<ConfigGroupConfigMappingEntity> configGroupConfigMappingEntities;

  @ManyToMany(mappedBy = "clusterConfigEntities")
  private Collection<ServiceConfigEntity> serviceConfigEntities;

  /**
   * Unidirectional one-to-one association to {@link StackEntity}
   */
  @OneToOne
  @JoinColumn(name = "stack_id", unique = false, nullable = false, insertable = true, updatable = true)
  private StackEntity stack;

  public Long getConfigId() {
    return configId;
  }

  public void setConfigId(Long configId) {
    this.configId = configId;
  }

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

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
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
    configJson = data;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long stamp) {
    timestamp = stamp;
  }

  public String getAttributes() {
    return configAttributesJson;
  }

  public void setAttributes(String attributes) {
    configAttributesJson = attributes;
  }

  /**
   * Gets the cluster configuration's stack.
   *
   * @return the stack.
   */
  public StackEntity getStack() {
    return stack;
  }

  /**
   * Sets the cluster configuration's stack.
   *
   * @param stack
   *          the stack to set for the cluster config (not {@code null}).
   */
  public void setStack(StackEntity stack) {
    this.stack = stack;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClusterConfigEntity that = (ClusterConfigEntity) o;

    if (configId != null ? !configId.equals(that.configId) : that.configId != null) {
      return false;
    }

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }

    if (type != null ? !type.equals(that.type) : that.type != null) {
      return false;
    }

    if (tag != null ? !tag.equals(that.tag) : that.tag != null) {
      return false;
    }

    if (stack != null ? !stack.equals(that.stack) : that.stack != null) {
      return false;
    }

    return true;

  }

  @Override
  public int hashCode() {
    int result = configId != null ? configId.hashCode() : 0;
    result = 31 * result + (clusterId != null ? clusterId.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (tag != null ? tag.hashCode() : 0);
    result = 31 * result + (stack != null ? stack.hashCode() : 0);

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


  public Collection<ServiceConfigEntity> getServiceConfigEntities() {
    return serviceConfigEntities;
  }

  public void setServiceConfigEntities(Collection<ServiceConfigEntity> serviceConfigEntities) {
    this.serviceConfigEntities = serviceConfigEntities;
  }
}
