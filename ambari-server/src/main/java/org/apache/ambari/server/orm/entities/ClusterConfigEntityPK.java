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
import java.io.Serializable;

@SuppressWarnings("serial")
public class ClusterConfigEntityPK implements Serializable {
  private Long clusterId;

  @Id
  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = true, length = 10)
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  private String type;
  @Id
  @Column(name = "type_name", nullable = false, insertable = true, updatable = false)
  public String getType() {
    return type;
  }

  public void setType(String typeName) {
    type = typeName;
  }

  private String tag;
  @Id
  @Column(name="version_tag", nullable = false, insertable = true, updatable = false)
  public String getTag() {
    return tag;
  }

  public void setTag(String configTag) {
    tag = configTag;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClusterConfigEntityPK that = (ClusterConfigEntityPK) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (tag != null ? !tag.equals(that.tag) : that.tag != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId !=null ? clusterId.intValue() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (tag != null ? tag.hashCode() : 0);
    return result;
  }
}
