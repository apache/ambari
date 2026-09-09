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
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "service_dependency_binding", uniqueConstraints = @UniqueConstraint(
    name = "uq_svc_dep_consumer_type",
    columnNames = {"consumer_cluster_id", "consumer_service_name", "dependency_type"}))
@NamedQueries({
    @NamedQuery(name = "ServiceDependencyBindingEntity.findByConsumer", query =
        "SELECT binding FROM ServiceDependencyBindingEntity binding "
            + "WHERE binding.consumerClusterId=:clusterId AND binding.consumerServiceName=:serviceName "
            + "ORDER BY binding.dependencyType"),
    @NamedQuery(name = "ServiceDependencyBindingEntity.findByConsumerAndType", query =
        "SELECT binding FROM ServiceDependencyBindingEntity binding "
            + "WHERE binding.consumerClusterId=:clusterId AND binding.consumerServiceName=:serviceName "
            + "AND binding.dependencyType=:dependencyType"),
    @NamedQuery(name = "ServiceDependencyBindingEntity.findByProvider", query =
        "SELECT binding FROM ServiceDependencyBindingEntity binding "
            + "WHERE binding.providerClusterId=:clusterId AND binding.providerServiceName=:serviceName "
            + "ORDER BY binding.bindingId")
})
public class ServiceDependencyBindingEntity {
  @Id
  @Column(name = "binding_id", length = 36, nullable = false, updatable = false)
  private String bindingId;

  @Column(name = "consumer_cluster_id", nullable = false, updatable = false)
  private Long consumerClusterId;

  @Column(name = "consumer_service_name", length = 255, nullable = false, updatable = false)
  private String consumerServiceName;

  @Column(name = "provider_cluster_id", nullable = false, updatable = false)
  private Long providerClusterId;

  @Column(name = "provider_service_name", length = 255, nullable = false, updatable = false)
  private String providerServiceName;

  @Column(name = "dependency_type", length = 32, nullable = false, updatable = false)
  private String dependencyType;

  @Column(name = "state", length = 32, nullable = false)
  private String state;

  @Column(name = "provisioning_phase", length = 64)
  private String provisioningPhase;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long rowVersion = 0L;

  @Column(name = "operation_epoch", nullable = false)
  private Long operationEpoch = 1L;

  @Column(name = "desired_snapshot_version", nullable = false)
  private Long desiredSnapshotVersion;

  @Column(name = "snapshot_approval", length = 32, nullable = false)
  private String snapshotApproval;

  @Column(name = "provider_preparation_hash", length = 71)
  private String providerPreparationHash;

  @Column(name = "applied_snapshot_version")
  private Long appliedSnapshotVersion;

  @Column(name = "provider_fingerprint", length = 71, nullable = false)
  private String providerFingerprint;

  @Column(name = "applied_provider_fingerprint", length = 71)
  private String appliedProviderFingerprint;

  @Column(name = "namespace_root", length = 2048)
  private String namespaceRoot;

  @Column(name = "namespace_wal", length = 2048)
  private String namespaceWal;

  @Column(name = "namespace_znode", length = 1024)
  private String namespaceZnode;

  @Column(name = "action_host_id")
  private Long actionHostId;

  @Column(name = "active_operation_id", length = 36, nullable = false)
  private String activeOperationId;

  @Column(name = "active_request_id")
  private Long activeRequestId;

  @Column(name = "failure_code", length = 128)
  private String failureCode;

  @Column(name = "failure_phase", length = 64)
  private String failurePhase;

  @Column(name = "failure_message", length = 1024)
  private String failureMessage;

  @Column(name = "failure_retryable", nullable = false)
  private Boolean failureRetryable = false;

  @Column(name = "created_by_user_id", nullable = false, updatable = false)
  private Integer createdByUserId;

  @Column(name = "updated_by_user_id", nullable = false)
  private Integer updatedByUserId;

  @Column(name = "create_timestamp", nullable = false, updatable = false)
  private Long createTimestamp;

  @Column(name = "update_timestamp", nullable = false)
  private Long updateTimestamp;

  @PrePersist
  protected void onCreate() {
    long now = System.currentTimeMillis();
    if (createTimestamp == null) {
      createTimestamp = now;
    }
    updateTimestamp = createTimestamp;
  }

  @PreUpdate
  protected void onUpdate() {
    updateTimestamp = System.currentTimeMillis();
  }

  public String getBindingId() { return bindingId; }
  public void setBindingId(String bindingId) { this.bindingId = bindingId; }
  public Long getConsumerClusterId() { return consumerClusterId; }
  public void setConsumerClusterId(Long value) { consumerClusterId = value; }
  public String getConsumerServiceName() { return consumerServiceName; }
  public void setConsumerServiceName(String value) { consumerServiceName = value; }
  public Long getProviderClusterId() { return providerClusterId; }
  public void setProviderClusterId(Long value) { providerClusterId = value; }
  public String getProviderServiceName() { return providerServiceName; }
  public void setProviderServiceName(String value) { providerServiceName = value; }
  public String getDependencyType() { return dependencyType; }
  public void setDependencyType(String value) { dependencyType = value; }
  public String getState() { return state; }
  public void setState(String value) { state = value; }
  public String getProvisioningPhase() { return provisioningPhase; }
  public void setProvisioningPhase(String value) { provisioningPhase = value; }
  public Long getRowVersion() { return rowVersion; }
  public void setRowVersion(Long value) { rowVersion = value; }
  public Long getOperationEpoch() { return operationEpoch; }
  public void setOperationEpoch(Long value) { operationEpoch = value; }
  public Long getDesiredSnapshotVersion() { return desiredSnapshotVersion; }
  public void setDesiredSnapshotVersion(Long value) { desiredSnapshotVersion = value; }
  public String getSnapshotApproval() { return snapshotApproval; }
  public void setSnapshotApproval(String value) { snapshotApproval = value; }
  public String getProviderPreparationHash() { return providerPreparationHash; }
  public void setProviderPreparationHash(String value) { providerPreparationHash = value; }
  public Long getAppliedSnapshotVersion() { return appliedSnapshotVersion; }
  public void setAppliedSnapshotVersion(Long value) { appliedSnapshotVersion = value; }
  public String getProviderFingerprint() { return providerFingerprint; }
  public void setProviderFingerprint(String value) { providerFingerprint = value; }
  public String getAppliedProviderFingerprint() { return appliedProviderFingerprint; }
  public void setAppliedProviderFingerprint(String value) { appliedProviderFingerprint = value; }
  public String getNamespaceRoot() { return namespaceRoot; }
  public void setNamespaceRoot(String value) { namespaceRoot = value; }
  public String getNamespaceWal() { return namespaceWal; }
  public void setNamespaceWal(String value) { namespaceWal = value; }
  public String getNamespaceZnode() { return namespaceZnode; }
  public void setNamespaceZnode(String value) { namespaceZnode = value; }
  public Long getActionHostId() { return actionHostId; }
  public void setActionHostId(Long value) { actionHostId = value; }
  public String getActiveOperationId() { return activeOperationId; }
  public void setActiveOperationId(String value) { activeOperationId = value; }
  public Long getActiveRequestId() { return activeRequestId; }
  public void setActiveRequestId(Long value) { activeRequestId = value; }
  public String getFailureCode() { return failureCode; }
  public void setFailureCode(String value) { failureCode = value; }
  public String getFailurePhase() { return failurePhase; }
  public void setFailurePhase(String value) { failurePhase = value; }
  public String getFailureMessage() { return failureMessage; }
  public void setFailureMessage(String value) { failureMessage = value; }
  public Boolean getFailureRetryable() { return failureRetryable; }
  public void setFailureRetryable(Boolean value) { failureRetryable = value; }
  public Integer getCreatedByUserId() { return createdByUserId; }
  public void setCreatedByUserId(Integer value) { createdByUserId = value; }
  public Integer getUpdatedByUserId() { return updatedByUserId; }
  public void setUpdatedByUserId(Integer value) { updatedByUserId = value; }
  public Long getCreateTimestamp() { return createTimestamp; }
  public Long getUpdateTimestamp() { return updateTimestamp; }
  public void setUpdateTimestamp(Long value) { updateTimestamp = value; }
}
