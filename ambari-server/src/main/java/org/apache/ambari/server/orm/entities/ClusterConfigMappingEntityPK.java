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
import javax.persistence.Id;

/**
 * PK class for cluster config mappings.
 */
public class ClusterConfigMappingEntityPK {
  private Long clusterId;
  private String typeName;
  private Long createTimestamp;
  
  @Id
  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = true, length = 10)
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  @Id
  @Column(name = "type_name", nullable = false, insertable = true, updatable = false)
  public String getType() {
    return typeName;
  }

  public void setType(String type) {
    typeName = type;
  }

  @Id
  @Column(name = "create_timestamp", nullable = false, insertable = true, updatable = false)
  public Long getCreateTimestamp() {
    return createTimestamp;
  }

  public void setCreateTimestamp(Long timestamp) {
    createTimestamp = timestamp;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClusterConfigMappingEntityPK that = (ClusterConfigMappingEntityPK) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (typeName != null ? !typeName.equals(that.typeName) : that.typeName != null) return false;
    if (createTimestamp != null ? !createTimestamp.equals (that.createTimestamp) : that.createTimestamp != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId !=null ? clusterId.intValue() : 0;
    result = 31 * result + (typeName != null ? typeName.hashCode() : 0);
    result = 31 * result + createTimestamp.intValue();
    return result;
  }
}
