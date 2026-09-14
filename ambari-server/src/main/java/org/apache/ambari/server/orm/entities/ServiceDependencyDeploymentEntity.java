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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Durable consumer deployment intent; binding operations retain their own lifecycle. */
@Entity
@Table(name = "service_dependency_deployment")
public class ServiceDependencyDeploymentEntity {
  @Id
  @Column(name = "deployment_id", length = 36, nullable = false, updatable = false)
  private String deploymentId;

  @Column(name = "cluster_id", nullable = false, updatable = false)
  private Long clusterId;

  @Column(name = "owner_user_id", nullable = false, updatable = false)
  private Integer ownerUserId;

  @Lob
  @Column(name = "plan_json", nullable = false, updatable = false)
  private String planJson;

  @Lob
  @Column(name = "progress_json", nullable = false)
  private String progressJson;

  @Column(name = "state", length = 32, nullable = false)
  private String state;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long rowVersion;

  @Column(name = "create_timestamp", nullable = false, updatable = false)
  private Long createTimestamp;

  @Column(name = "update_timestamp", nullable = false)
  private Long updateTimestamp;

  public String getState() { return state; }
  public void setState(String value) { state = value; }
  public String getDeploymentId() { return deploymentId; }
  public void setDeploymentId(String value) { deploymentId = value; }
  public Long getClusterId() { return clusterId; }
  public void setClusterId(Long value) { clusterId = value; }
  public Integer getOwnerUserId() { return ownerUserId; }
  public void setOwnerUserId(Integer value) { ownerUserId = value; }
  public String getPlanJson() { return planJson; }
  public void setPlanJson(String value) { planJson = value; }
  public String getProgressJson() { return progressJson; }
  public void setProgressJson(String value) { progressJson = value; }
  public Long getRowVersion() { return rowVersion; }
  public void setRowVersion(Long value) { rowVersion = value; }
  public Long getCreateTimestamp() { return createTimestamp; }
  public void setCreateTimestamp(Long value) { createTimestamp = value; }
  public Long getUpdateTimestamp() { return updateTimestamp; }
  public void setUpdateTimestamp(Long value) { updateTimestamp = value; }
}
