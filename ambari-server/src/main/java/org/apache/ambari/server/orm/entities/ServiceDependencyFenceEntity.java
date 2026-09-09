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
@Table(name = "service_dependency_fence")
@NamedQuery(name = "ServiceDependencyFenceEntity.findByConsumer", query =
    "SELECT fence FROM ServiceDependencyFenceEntity fence "
        + "WHERE fence.consumerClusterId=:clusterId "
        + "AND fence.consumerServiceName=:serviceName ORDER BY fence.bindingId")
public class ServiceDependencyFenceEntity {
  @Id
  @Column(name = "binding_id", length = 36, nullable = false, updatable = false)
  private String bindingId;

  @Column(name = "final_epoch", nullable = false, updatable = false)
  private Long finalEpoch;

  @Column(name = "immutable_spec_hash", length = 71, nullable = false, updatable = false)
  private String immutableSpecHash;

  @Column(name = "dependency_type", length = 32, nullable = false, updatable = false)
  private String dependencyType;

  @Column(name = "consumer_cluster_id", nullable = false, updatable = false)
  private Long consumerClusterId;

  @Column(name = "consumer_service_name", length = 255, nullable = false, updatable = false)
  private String consumerServiceName;

  @Column(name = "provider_cluster_id", nullable = false, updatable = false)
  private Long providerClusterId;

  @Column(name = "provider_service_name", length = 255, nullable = false, updatable = false)
  private String providerServiceName;

  @Column(name = "namespace_hash", length = 71, nullable = false, updatable = false)
  private String namespaceHash;

  @Column(name = "detach_operation_id", length = 36, updatable = false)
  private String detachOperationId;

  @Column(name = "detach_request_hash", length = 71, updatable = false)
  private String detachRequestHash;

  @Column(name = "detached_by_user_id", nullable = false, updatable = false)
  private Integer detachedByUserId;

  @Column(name = "detach_timestamp", nullable = false, updatable = false)
  private Long detachTimestamp;

  public String getBindingId() { return bindingId; }
  public void setBindingId(String value) { bindingId = value; }
  public Long getFinalEpoch() { return finalEpoch; }
  public void setFinalEpoch(Long value) { finalEpoch = value; }
  public String getImmutableSpecHash() { return immutableSpecHash; }
  public void setImmutableSpecHash(String value) { immutableSpecHash = value; }
  public String getDependencyType() { return dependencyType; }
  public void setDependencyType(String value) { dependencyType = value; }
  public Long getConsumerClusterId() { return consumerClusterId; }
  public void setConsumerClusterId(Long value) { consumerClusterId = value; }
  public String getConsumerServiceName() { return consumerServiceName; }
  public void setConsumerServiceName(String value) { consumerServiceName = value; }
  public Long getProviderClusterId() { return providerClusterId; }
  public void setProviderClusterId(Long value) { providerClusterId = value; }
  public String getProviderServiceName() { return providerServiceName; }
  public void setProviderServiceName(String value) { providerServiceName = value; }
  public String getNamespaceHash() { return namespaceHash; }
  public void setNamespaceHash(String value) { namespaceHash = value; }
  public String getDetachOperationId() { return detachOperationId; }
  public void setDetachOperationId(String value) { detachOperationId = value; }
  public String getDetachRequestHash() { return detachRequestHash; }
  public void setDetachRequestHash(String value) { detachRequestHash = value; }
  public Integer getDetachedByUserId() { return detachedByUserId; }
  public void setDetachedByUserId(Integer value) { detachedByUserId = value; }
  public Long getDetachTimestamp() { return detachTimestamp; }
  public void setDetachTimestamp(Long value) { detachTimestamp = value; }
}
