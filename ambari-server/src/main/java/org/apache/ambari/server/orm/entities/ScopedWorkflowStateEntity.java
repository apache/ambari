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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "scoped_workflow_state")
@NamedQuery(name = "ScopedWorkflowStateEntity.findActiveCreationDrafts", query =
    "SELECT state FROM ScopedWorkflowStateEntity state " +
        "WHERE state.ownerUserId=:ownerUserId AND state.workflow='CLUSTER_CREATE' " +
        "AND state.scopeKey LIKE :scopePrefix ORDER BY state.scopeKey")
public class ScopedWorkflowStateEntity {
  @Id
  @Column(name = "scope_key", length = 255, nullable = false, updatable = false)
  private String scopeKey;

  @Column(name = "revision", nullable = false)
  private Long revision;

  @Column(name = "owner_user_id")
  private Integer ownerUserId;

  @Column(name = "owner_name", length = 255)
  private String ownerName;

  @Column(name = "created_cluster_id")
  private Long createdClusterId;

  @Column(name = "workflow", length = 64, nullable = false)
  private String workflow;

  @Column(name = "phase", length = 128, nullable = false)
  private String phase;

  @Lob
  @Basic
  @Column(name = "payload", nullable = false)
  private String payload;

  public String getScopeKey() {
    return scopeKey;
  }

  public void setScopeKey(String scopeKey) {
    this.scopeKey = scopeKey;
  }

  public Long getRevision() {
    return revision;
  }

  public void setRevision(Long revision) {
    this.revision = revision;
  }

  public Integer getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Integer ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public Long getCreatedClusterId() {
    return createdClusterId;
  }

  public void setCreatedClusterId(Long createdClusterId) {
    this.createdClusterId = createdClusterId;
  }

  public String getWorkflow() {
    return workflow;
  }

  public void setWorkflow(String workflow) {
    this.workflow = workflow;
  }

  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }
}
