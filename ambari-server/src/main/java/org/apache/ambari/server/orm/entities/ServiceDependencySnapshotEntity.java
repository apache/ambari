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
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@IdClass(ServiceDependencySnapshotEntityPK.class)
@Table(name = "service_dependency_snapshot")
public class ServiceDependencySnapshotEntity {
  @Id
  @Column(name = "binding_id", length = 36, nullable = false, updatable = false)
  private String bindingId;

  @Id
  @Column(name = "snapshot_version", nullable = false, updatable = false)
  private Long snapshotVersion;

  @Column(name = "schema_version", nullable = false, updatable = false)
  private Integer schemaVersion;

  @Column(name = "consumer_fingerprint", length = 71, nullable = false, updatable = false)
  private String consumerFingerprint;

  @Column(name = "provider_fingerprint", length = 71, nullable = false, updatable = false)
  private String providerFingerprint;

  @Column(name = "provider_display_name", length = 255, nullable = false, updatable = false)
  private String providerDisplayName;

  @Column(name = "consumer_service_version", length = 128, nullable = false, updatable = false)
  private String consumerServiceVersion;

  @Column(name = "snapshot_fingerprint", length = 71, nullable = false, updatable = false)
  private String snapshotFingerprint;

  @Column(name = "client_features_hash", length = 71, nullable = false, updatable = false)
  private String clientFeaturesHash;

  @Column(name = "security_policy_hash", length = 71, nullable = false, updatable = false)
  private String securityPolicyHash;

  @Lob
  @Basic
  @Column(name = "snapshot_json", nullable = false, updatable = false)
  private String snapshotJson;

  @Column(name = "created_by_user_id", nullable = false, updatable = false)
  private Integer createdByUserId;

  @Column(name = "create_timestamp", nullable = false, updatable = false)
  private Long createTimestamp;

  public String getBindingId() { return bindingId; }
  public void setBindingId(String value) { bindingId = value; }
  public Long getSnapshotVersion() { return snapshotVersion; }
  public void setSnapshotVersion(Long value) { snapshotVersion = value; }
  public Integer getSchemaVersion() { return schemaVersion; }
  public void setSchemaVersion(Integer value) { schemaVersion = value; }
  public String getConsumerFingerprint() { return consumerFingerprint; }
  public void setConsumerFingerprint(String value) { consumerFingerprint = value; }
  public String getProviderFingerprint() { return providerFingerprint; }
  public void setProviderFingerprint(String value) { providerFingerprint = value; }
  public String getProviderDisplayName() { return providerDisplayName; }
  public void setProviderDisplayName(String value) { providerDisplayName = value; }
  public String getConsumerServiceVersion() { return consumerServiceVersion; }
  public void setConsumerServiceVersion(String value) { consumerServiceVersion = value; }
  public String getSnapshotFingerprint() { return snapshotFingerprint; }
  public void setSnapshotFingerprint(String value) { snapshotFingerprint = value; }
  public String getClientFeaturesHash() { return clientFeaturesHash; }
  public void setClientFeaturesHash(String value) { clientFeaturesHash = value; }
  public String getSecurityPolicyHash() { return securityPolicyHash; }
  public void setSecurityPolicyHash(String value) { securityPolicyHash = value; }
  public String getSnapshotJson() { return snapshotJson; }
  public void setSnapshotJson(String value) { snapshotJson = value; }
  public Integer getCreatedByUserId() { return createdByUserId; }
  public void setCreatedByUserId(Integer value) { createdByUserId = value; }
  public Long getCreateTimestamp() { return createTimestamp; }
  public void setCreateTimestamp(Long value) { createTimestamp = value; }
}
