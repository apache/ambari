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
package org.apache.ambari.server.mpack;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.inject.Singleton;

/** Runtime publication barrier; durable intent remains owned by the lifecycle records. */
@Singleton
public class MpackRuntime {
  @com.google.inject.Inject private com.google.inject.Provider<org.apache.ambari.server.state.Clusters> clusters;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
  private volatile MpackSnapshots.Snapshot snapshot;
  private volatile String pendingOperation;
  private volatile java.util.List<MpackScope> scopes = java.util.List.of();
  private volatile boolean readable = true;

  public Lock readLock() {
    return lock.readLock();
  }

  public Lock writeLock() {
    return lock.writeLock();
  }

  public MpackSnapshots.Snapshot snapshot() {
    return snapshot;
  }

  public String pendingOperation() {
    return pendingOperation;
  }

  public boolean hasReadableView() {
    return readable;
  }

  public void gate(String operationId) {
    requireWriter();
    if (pendingOperation != null && !pendingOperation.equals(operationId)) {
      throw new MpackException(MpackException.Code.OPERATION_CONFLICT,
          "Another management pack operation owns the publication barrier",
          Map.of("operation_id", pendingOperation));
    }
    pendingOperation = operationId;
  }

  public void reserve(String operationId, java.util.List<MpackScope> affected) {
    gate(operationId);
    scopes = java.util.List.copyOf(affected);
  }

  public java.util.List<MpackScope> scopes() {
    return scopes;
  }

  public boolean conflicts(org.apache.ambari.server.state.StackId stack, String service) {
    return pendingOperation != null && scopes.stream().anyMatch(scope -> scope.contains(stack, service));
  }

  public void requireServiceReady(org.apache.ambari.server.state.StackId stack, String service) {
    if (!lock.isWriteLockedByCurrentThread() && conflicts(stack, service)) {
      throw new MpackException(MpackException.Code.MAINTENANCE_REQUIRED,
          "This service definition has a pending management pack operation",
          Map.of("operation_id", pendingOperation, "stack", stack.getStackId(),
              "service", service == null ? "" : service));
    }
  }

  public void requireConfigurationReady(org.apache.ambari.server.state.StackId stack, String type) {
    if (!lock.isWriteLockedByCurrentThread() && pendingOperation != null && scopes.stream().anyMatch(scope ->
        scope.contains(stack, null) && (scope.serviceName() == null || scope.configTypes().contains(type)))) {
      throw new MpackException(MpackException.Code.MAINTENANCE_REQUIRED,
          "This configuration is used by a definition awaiting publication",
          Map.of("operation_id", pendingOperation, "config_type", type));
    }
  }

  public void markUnreadable() {
    requireWriter();
    readable = false;
  }

  public void requireReadable(org.apache.ambari.server.state.StackId stack, String service) {
    if (!readable) {
      requireServiceReady(stack, service);
    }
  }

  public void requireResourceReadable(Map<org.apache.ambari.server.controller.spi.Resource.Type, String> keys) {
    if (readable) return;
    org.apache.ambari.server.controller.spi.Resource.Type clusterType = org.apache.ambari.server.controller.spi.Resource.Type.Cluster;
    try {
      if (keys.get(clusterType) != null) {
        org.apache.ambari.server.state.Cluster cluster = clusters.get().getCluster(keys.get(clusterType));
        requireReadable(cluster.getDesiredStackVersion(),
            keys.get(org.apache.ambari.server.controller.spi.Resource.Type.Service));
      } else if (keys.get(org.apache.ambari.server.controller.spi.Resource.Type.Stack) != null
          && keys.get(org.apache.ambari.server.controller.spi.Resource.Type.StackVersion) != null) {
        requireReadable(new org.apache.ambari.server.state.StackId(
            keys.get(org.apache.ambari.server.controller.spi.Resource.Type.Stack),
            keys.get(org.apache.ambari.server.controller.spi.Resource.Type.StackVersion)),
            keys.get(org.apache.ambari.server.controller.spi.Resource.Type.StackService));
      } else {
        requireExecutionReady();
      }
    } catch (org.apache.ambari.server.AmbariException e) {
      throw new MpackException(MpackException.Code.INVALID_TARGET, "The definition context is unavailable");
    }
  }

  public void requireResourceMutation(Map<org.apache.ambari.server.controller.spi.Resource.Type, String> keys,
      Map<String, Object> properties) {
    if (pendingOperation == null) return;
    String name = keys.get(org.apache.ambari.server.controller.spi.Resource.Type.Cluster);
    if (name == null) return;
    try {
      org.apache.ambari.server.state.Cluster cluster = clusters.get().getCluster(name);
      String service = keys.get(org.apache.ambari.server.controller.spi.Resource.Type.Service);
      if (service == null && properties.get("ServiceInfo/service_name") instanceof String selected) service = selected;
      if (service != null) {
        org.apache.ambari.server.state.Service existing = cluster.getServices().get(service);
        requireServiceReady(existing == null ? cluster.getDesiredStackVersion() : existing.getDesiredStackId(), service);
      }
      String component = keys.get(org.apache.ambari.server.controller.spi.Resource.Type.HostComponent);
      if (component == null && properties.get("HostRoles/component_name") instanceof String selected) component = selected;
      if (component != null) {
        for (org.apache.ambari.server.state.Service existing : cluster.getServices().values()) {
          if (existing.getServiceComponents().containsKey(component)) requireServiceReady(existing.getDesiredStackId(), existing.getName());
        }
      }
    } catch (org.apache.ambari.server.AmbariException e) {
      throw new MpackException(MpackException.Code.INVALID_TARGET, "The mutation context is unavailable");
    }
  }

  public void publish(String operationId, MpackSnapshots.Snapshot published) {
    requireWriter();
    if (!operationId.equals(pendingOperation)) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT,
          "Publication does not belong to the operation holding the barrier");
    }
    snapshot = published;
    readable = true;
  }

  public void restore(MpackSnapshots.Snapshot effective, String interruptedOperation) {
    requireWriter();
    snapshot = effective;
    pendingOperation = interruptedOperation;
    readable = true;
  }

  public void release(String operationId) {
    requireWriter();
    if (!operationId.equals(pendingOperation)) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT,
          "Only the publication owner may release its barrier");
    }
    pendingOperation = null;
    scopes = java.util.List.of();
    readable = true;
  }

  public void requireExecutionReady() {
    if (!readable && !lock.isWriteLockedByCurrentThread()) {
      throw new MpackException(MpackException.Code.MAINTENANCE_REQUIRED,
          "Definition publication is waiting for completion or recovery",
          Map.of("operation_id", pendingOperation));
    }
  }

  public void requirePublicationLock() {
    requireWriter();
  }

  public void requireLegacyMutationAllowed() {
    throw new MpackException(MpackException.Code.UNSUPPORTED_OPERATION,
        "Use the management pack lifecycle API to change managed definitions or bindings");
  }

  private void requireWriter() {
    if (!lock.isWriteLockedByCurrentThread()) {
      throw new IllegalStateException("Definition publication requires the mutation barrier");
    }
  }
}
