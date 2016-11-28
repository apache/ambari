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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity that maps to a cluster config mapping.
 */
@Table(name = "clusterconfigmapping")
@Entity
@IdClass(ClusterConfigMappingEntityPK.class)
public class ClusterConfigMappingEntity {

  @Id
  @Column(name = "cluster_id", insertable = false, updatable = false, nullable = false)
  private Long clusterId;

  @Id
  @Column(name = "type_name", insertable = true, updatable = false, nullable = false)
  private String typeName;

  @Id
  @Column(name = "create_timestamp", insertable = true, updatable = false, nullable = false)
  private Long createTimestamp;

  @Column(name = "version_tag", insertable = true, updatable = false, nullable = false)
  private String tag;

  @Column(name = "selected", insertable = true, updatable = true, nullable = false)
  private int selectedInd = 0;

  @Column(name = "user_name", insertable = true, updatable = true, nullable = false)
  private String user;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  private ClusterEntity clusterEntity;

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long id) {
    clusterId = id;
  }

  public String getType() {
    return typeName;
  }

  public void setType(String type) {
    typeName = type;
  }

  public Long getCreateTimestamp() {
    return createTimestamp;
  }

  public void setCreateTimestamp(Long timestamp) {
    createTimestamp = timestamp;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String version) {
    tag = version;
  }

  public int isSelected() {
    return selectedInd;
  }

  public void setSelected(int selected) {
    selectedInd = selected;
  }

  /**
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * @param userName the user
   */
  public void setUser(String userName) {
    user = userName;
  }

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity entity) {
    clusterEntity = entity;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((clusterId == null) ? 0 : clusterId.hashCode());
    result = prime * result + ((createTimestamp == null) ? 0 : createTimestamp.hashCode());
    result = prime * result + ((tag == null) ? 0 : tag.hashCode());
    result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    ClusterConfigMappingEntity other = (ClusterConfigMappingEntity) obj;
    if (clusterId == null) {
      if (other.clusterId != null) {
        return false;
      }
    } else if (!clusterId.equals(other.clusterId)) {
      return false;
    }

    if (createTimestamp == null) {
      if (other.createTimestamp != null) {
        return false;
      }
    } else if (!createTimestamp.equals(other.createTimestamp)) {
      return false;
    }

    if (tag == null) {
      if (other.tag != null) {
        return false;
      }
    } else if (!tag.equals(other.tag)) {
      return false;
    }

    if (typeName == null) {
      if (other.typeName != null) {
        return false;
      }
    } else if (!typeName.equals(other.typeName)) {
      return false;
    }

    return true;
  }
}
