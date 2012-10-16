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

public class ActionStatusEntityPK implements Serializable {
  private Long clusterId;

  @Id
  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = true, length = 10)
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  private String hostName;

  @Id
  @Column(name = "host_name", nullable = false, insertable = true, updatable = true)
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  private String role;

  @Id
  @Column(name = "role", nullable = false, insertable = true, updatable = true)
  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  private Integer requestId;

  @Id
  @Column(name = "request_id", nullable = false, insertable = true, updatable = true, length = 10)
  public Integer getRequestId() {
    return requestId;
  }

  public void setRequestId(Integer requestId) {
    this.requestId = requestId;
  }

  private Integer stageId;

  @Id
  @Column(name = "stage_id", nullable = false, insertable = true, updatable = true, length = 10)
  public Integer getStageId() {
    return stageId;
  }

  public void setStageId(Integer stageId) {
    this.stageId = stageId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ActionStatusEntityPK that = (ActionStatusEntityPK) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;
    if (stageId != null ? !stageId.equals(that.stageId) : that.stageId != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (role != null ? !role.equals(that.role) : that.role != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.intValue() : 0;
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (role != null ? role.hashCode() : 0);
    result = 31 * result + (requestId !=null ? requestId : 0);
    result = 31 * result + (stageId !=null ? stageId : 0);
    return result;
  }
}
