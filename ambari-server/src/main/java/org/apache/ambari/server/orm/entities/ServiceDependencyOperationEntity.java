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
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_dependency_operation")
@NamedQuery(name = "ServiceDependencyOperationEntity.findByBinding", query =
    "SELECT operation FROM ServiceDependencyOperationEntity operation "
        + "WHERE operation.bindingId=:bindingId ORDER BY operation.operationEpoch")
public class ServiceDependencyOperationEntity {
  @Id
  @Column(name = "operation_id", length = 36, nullable = false, updatable = false)
  private String operationId;

  @Column(name = "binding_id", length = 36, nullable = false, updatable = false)
  private String bindingId;

  @Column(name = "operation_kind", length = 32, nullable = false, updatable = false)
  private String operationKind;

  @Column(name = "operation_epoch", nullable = false, updatable = false)
  private Long operationEpoch;

  @Column(name = "target_snapshot_version", nullable = false, updatable = false)
  private Long targetSnapshotVersion;

  @Column(name = "request_hash", length = 71, nullable = false, updatable = false)
  private String requestHash;

  @Column(name = "state", length = 32, nullable = false)
  private String state;

  @Column(name = "ambari_request_id")
  private Long ambariRequestId;

  @Column(name = "failure_code", length = 128)
  private String failureCode;

  @Column(name = "failure_message", length = 1024)
  private String failureMessage;

  @Column(name = "create_timestamp", nullable = false, updatable = false)
  private Long createTimestamp;

  @Column(name = "update_timestamp", nullable = false)
  private Long updateTimestamp;

  public String getOperationId() { return operationId; }
  public void setOperationId(String value) { operationId = value; }
  public String getBindingId() { return bindingId; }
  public void setBindingId(String value) { bindingId = value; }
  public String getOperationKind() { return operationKind; }
  public void setOperationKind(String value) { operationKind = value; }
  public Long getOperationEpoch() { return operationEpoch; }
  public void setOperationEpoch(Long value) { operationEpoch = value; }
  public Long getTargetSnapshotVersion() { return targetSnapshotVersion; }
  public void setTargetSnapshotVersion(Long value) { targetSnapshotVersion = value; }
  public String getRequestHash() { return requestHash; }
  public void setRequestHash(String value) { requestHash = value; }
  public String getState() { return state; }
  public void setState(String value) { state = value; }
  public Long getAmbariRequestId() { return ambariRequestId; }
  public void setAmbariRequestId(Long value) { ambariRequestId = value; }
  public String getFailureCode() { return failureCode; }
  public void setFailureCode(String value) { failureCode = value; }
  public String getFailureMessage() { return failureMessage; }
  public void setFailureMessage(String value) { failureMessage = value; }
  public Long getCreateTimestamp() { return createTimestamp; }
  public void setCreateTimestamp(Long value) { createTimestamp = value; }
  public Long getUpdateTimestamp() { return updateTimestamp; }
  public void setUpdateTimestamp(Long value) { updateTimestamp = value; }
}
