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
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@IdClass(ServiceDependencyHostResultEntityPK.class)
@Table(name = "service_dependency_host_result")
@NamedQueries({
    @NamedQuery(name = "ServiceDependencyHostResultEntity.findOutstanding", query =
        "SELECT result FROM ServiceDependencyHostResultEntity result, ServiceDependencyBindingEntity binding "
            + "WHERE result.bindingId=binding.bindingId AND result.operationId=binding.activeOperationId "
            + "AND result.operationEpoch=binding.operationEpoch AND result.state IN ('INTENT', 'SCHEDULING', 'DISPATCHED') "
            + "ORDER BY result.checkTimestamp, result.bindingId, result.hostId"),
    @NamedQuery(name = "ServiceDependencyHostResultEntity.findByTask", query =
        "SELECT result FROM ServiceDependencyHostResultEntity result "
            + "WHERE result.ambariTaskId=:taskId")
})
public class ServiceDependencyHostResultEntity {
  @Id
  @Column(name = "binding_id", length = 36, nullable = false, updatable = false)
  private String bindingId;

  @Id
  @Column(name = "snapshot_version", nullable = false, updatable = false)
  private Long snapshotVersion;

  @Id
  @Column(name = "host_id", nullable = false, updatable = false)
  private Long hostId;

  @Id
  @Column(name = "dependency_type", length = 32, nullable = false, updatable = false)
  private String dependencyType;

  @Id
  @Column(name = "check_kind", length = 64, nullable = false, updatable = false)
  private String checkKind;

  @Id
  @Column(name = "operation_epoch", nullable = false, updatable = false)
  private Long operationEpoch;

  @Column(name = "operation_id", length = 36, nullable = false, updatable = false)
  private String operationId;

  @Column(name = "component_name", length = 255, updatable = false)
  private String componentName;

  @Column(name = "command_request_hash", length = 71, nullable = false, updatable = false)
  private String commandRequestHash;

  @Lob
  @Column(name = "command_json", nullable = false, updatable = false)
  private String commandJson;

  @Lob
  @Column(name = "credential_plan_json")
  private String credentialPlanJson;

  @Column(name = "ambari_request_id")
  private Long ambariRequestId;

  @Column(name = "ambari_stage_id")
  private Long ambariStageId;

  @Column(name = "ambari_task_id")
  private Long ambariTaskId;

  @Column(name = "required_package_hash", length = 71, nullable = false)
  private String requiredPackageHash;

  @Column(name = "observed_package_hash", length = 71)
  private String observedPackageHash;

  @Column(name = "rendered_config_hash", length = 71)
  private String renderedConfigHash;

  @Column(name = "identity_fingerprint", length = 71)
  private String identityFingerprint;

  @Lob
  @Column(name = "result_json")
  private String resultJson;

  @Column(name = "result_hash", length = 71)
  private String resultHash;

  @Column(name = "preparation_observation_id", length = 36)
  private String preparationObservationId;

  @Column(name = "preparation_request_hash", length = 71)
  private String preparationRequestHash;

  @Column(name = "preparation_observation_fingerprint", length = 71)
  private String preparationObservationFingerprint;

  @Column(name = "package_name", length = 128)
  private String packageName;

  @Column(name = "package_version", length = 512)
  private String packageVersion;

  @Column(name = "client_software_version", length = 128)
  private String clientSoftwareVersion;

  @Column(name = "state", length = 32, nullable = false)
  private String state;

  @Column(name = "check_timestamp", nullable = false)
  private Long checkTimestamp;

  @Column(name = "failure_code", length = 128)
  private String failureCode;

  @Column(name = "failure_message", length = 1024)
  private String failureMessage;

  public String getBindingId() { return bindingId; }
  public void setBindingId(String value) { bindingId = value; }
  public Long getSnapshotVersion() { return snapshotVersion; }
  public void setSnapshotVersion(Long value) { snapshotVersion = value; }
  public Long getHostId() { return hostId; }
  public void setHostId(Long value) { hostId = value; }
  public String getDependencyType() { return dependencyType; }
  public void setDependencyType(String value) { dependencyType = value; }
  public String getCheckKind() { return checkKind; }
  public void setCheckKind(String value) { checkKind = value; }
  public Long getOperationEpoch() { return operationEpoch; }
  public void setOperationEpoch(Long value) { operationEpoch = value; }
  public String getOperationId() { return operationId; }
  public void setOperationId(String value) { operationId = value; }
  public String getComponentName() { return componentName; }
  public void setComponentName(String value) { componentName = value; }
  public String getCommandRequestHash() { return commandRequestHash; }
  public void setCommandRequestHash(String value) { commandRequestHash = value; }
  public String getCredentialPlanJson() { return credentialPlanJson; }
  public void setCredentialPlanJson(String value) { credentialPlanJson = value; }
  public String getCommandJson() { return commandJson; }
  public void setCommandJson(String value) { commandJson = value; }
  public Long getAmbariRequestId() { return ambariRequestId; }
  public void setAmbariRequestId(Long value) { ambariRequestId = value; }
  public Long getAmbariStageId() { return ambariStageId; }
  public void setAmbariStageId(Long value) { ambariStageId = value; }
  public Long getAmbariTaskId() { return ambariTaskId; }
  public void setAmbariTaskId(Long value) { ambariTaskId = value; }
  public String getRequiredPackageHash() { return requiredPackageHash; }
  public void setRequiredPackageHash(String value) { requiredPackageHash = value; }
  public String getObservedPackageHash() { return observedPackageHash; }
  public void setObservedPackageHash(String value) { observedPackageHash = value; }
  public String getRenderedConfigHash() { return renderedConfigHash; }
  public void setRenderedConfigHash(String value) { renderedConfigHash = value; }
  public String getIdentityFingerprint() { return identityFingerprint; }
  public void setIdentityFingerprint(String value) { identityFingerprint = value; }
  public String getResultJson() { return resultJson; }
  public void setResultJson(String value) { resultJson = value; }
  public String getResultHash() { return resultHash; }
  public void setResultHash(String value) { resultHash = value; }
  public String getPreparationObservationId() { return preparationObservationId; }
  public void setPreparationObservationId(String value) { preparationObservationId = value; }
  public String getPreparationRequestHash() { return preparationRequestHash; }
  public void setPreparationRequestHash(String value) { preparationRequestHash = value; }
  public String getPreparationObservationFingerprint() { return preparationObservationFingerprint; }
  public void setPreparationObservationFingerprint(String value) { preparationObservationFingerprint = value; }
  public String getPackageName() { return packageName; }
  public void setPackageName(String value) { packageName = value; }
  public String getPackageVersion() { return packageVersion; }
  public void setPackageVersion(String value) { packageVersion = value; }
  public String getClientSoftwareVersion() { return clientSoftwareVersion; }
  public void setClientSoftwareVersion(String value) { clientSoftwareVersion = value; }
  public String getState() { return state; }
  public void setState(String value) { state = value; }
  public Long getCheckTimestamp() { return checkTimestamp; }
  public void setCheckTimestamp(Long value) { checkTimestamp = value; }
  public String getFailureCode() { return failureCode; }
  public void setFailureCode(String value) { failureCode = value; }
  public String getFailureMessage() { return failureMessage; }
  public void setFailureMessage(String value) { failureMessage = value; }
}
