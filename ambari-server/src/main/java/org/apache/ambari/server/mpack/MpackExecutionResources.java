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
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Captures execution inputs once, before a task is persisted or dispatched. */
@Singleton
public class MpackExecutionResources {
  public static final String SNAPSHOT_ID = "mpack_definition_snapshot";
  public static final String RESOURCE_REFERENCES = "mpack_resource_references";
  public static final String EXECUTION_ID = "mpack_execution_id";
  public static final String PINNED_HOOKS = "mpack_hooks_folder";
  public static final String RESOURCE_CONTRACT = "mpack_resource_contract";
  public static final String RESOURCE_PROTOCOL = "MPACK_RESOURCES_V1";
  public static final String DEFINITION_IDENTITY = "mpack_definition_identity";

  private final MpackRuntime runtime;
  private final Configuration configuration;

  @Inject private com.google.inject.Provider<org.apache.ambari.server.state.Clusters> clusters;
  @Inject private com.google.inject.Provider<org.apache.ambari.server.api.services.AmbariMetaInfo> metadata;
  @Inject private com.google.inject.Provider<MpackSnapshots> snapshots;
  @Inject private com.google.inject.Provider<MpackPlanner> planner;

  public static final java.util.Set<String> PINNED_KEYS = java.util.Set.of(SNAPSHOT_ID,
      RESOURCE_REFERENCES, EXECUTION_ID, PINNED_HOOKS, RESOURCE_CONTRACT, "service_package_folder",
      ExecutionCommand.KeyNames.RESOURCE_ARCHIVE_DIGESTS, DEFINITION_IDENTITY);

  @Inject
  public MpackExecutionResources(MpackRuntime runtime, Configuration configuration) {
    this.runtime = runtime;
    this.configuration = configuration;
  }

  public void pin(ExecutionCommand command) {
    Lock read = runtime.readLock();
    if (!read.tryLock()) {
      throw new MpackException(MpackException.Code.OPERATION_CONFLICT, "Definition publication is in progress");
    }
    try {
      requireReady(command);
      MpackSnapshots.Snapshot snapshot = runtime.snapshot();
      if (snapshot == null) {
        return;
      }
      verifyAgentProtocol(command);
      Map<String, String> params = command.getCommandParams() == null
          ? new TreeMap<>() : new TreeMap<>(command.getCommandParams());
      if (params.containsKey(SNAPSHOT_ID)) {
        if (!snapshot.id().equals(params.get(SNAPSHOT_ID))) {
          String identity = definitionIdentity(command, snapshot);
          if (identity == null || !identity.equals(params.get(DEFINITION_IDENTITY))) {
            throw new MpackException(MpackException.Code.STALE_PLAN,
                "The task's service definitions changed before it could be accepted");
          }
          verify(params, snapshots.get().load(params.get(SNAPSHOT_ID)));
          return;
        }
        verify(params, snapshot);
        pinServicePath(command, params);
        command.setCommandParams(params);
        return;
      }
      params.putAll(parameters(snapshot));
      params.put(EXECUTION_ID, UUID.randomUUID().toString());
      pinServicePath(command, params);
      command.setCommandParams(params);
    } finally {
      read.unlock();
    }
  }

  public void requireReady(ExecutionCommand command) {
    try {
      if (command.getClusterName() != null) {
        org.apache.ambari.server.state.Cluster cluster = clusters.get().getCluster(command.getClusterName());
        org.apache.ambari.server.state.StackId stack = command.getServiceName() == null
            ? cluster.getDesiredStackVersion() : cluster.getService(command.getServiceName()).getDesiredStackId();
        runtime.requireServiceReady(stack, command.getServiceName());
      }
    } catch (org.apache.ambari.server.AmbariException e) {
      throw new MpackException(MpackException.Code.INVALID_TARGET, "Task definition context is unavailable");
    }
  }

  private String definitionIdentity(ExecutionCommand command, MpackSnapshots.Snapshot snapshot) {
    if (command.getClusterName() == null) return null;
    try {
      org.apache.ambari.server.state.Cluster cluster = clusters.get().getCluster(command.getClusterName());
      org.apache.ambari.server.state.StackId stack = command.getServiceName() == null
          ? cluster.getDesiredStackVersion() : cluster.getService(command.getServiceName()).getDesiredStackId();
      return planner.get().executionIdentity(snapshot, metadata.get().getStackManager(), stack, command.getServiceName());
    } catch (org.apache.ambari.server.AmbariException e) {
      throw new MpackException(MpackException.Code.INVALID_TARGET, "Task definition context is unavailable");
    }
  }

  private void verifyAgentProtocol(ExecutionCommand command) {
    if (command.getHostname() == null || command.getHostname().isEmpty()
        || org.apache.ambari.server.Role.AMBARI_SERVER_ACTION.name().equals(command.getRole())) {
      return;
    }
    try {
      org.apache.ambari.server.agent.AgentEnv environment = clusters.get().getHost(command.getHostname()).getLastAgentEnv();
      if (environment == null || !java.util.Arrays.asList(environment.getResourceProtocols()).contains(RESOURCE_PROTOCOL)) {
        throw new MpackException(MpackException.Code.UNSUPPORTED_OPERATION,
            "The target Agent must advertise the immutable resource protocol before accepting tasks",
            Map.of("host", command.getHostname(), "required_protocol", RESOURCE_PROTOCOL));
      }
    } catch (org.apache.ambari.server.AmbariException e) {
      throw new MpackException(MpackException.Code.INVALID_TARGET, "Task Agent is not registered");
    }
  }

  private void pinServicePath(ExecutionCommand command, Map<String, String> params) {
    String identity = definitionIdentity(command, runtime.snapshot());
    if (identity != null) params.put(DEFINITION_IDENTITY, identity);
    if (command.getServiceName() == null || command.getClusterName() == null) {
      return;
    }
    try {
      org.apache.ambari.server.state.Service service = clusters.get().getCluster(command.getClusterName())
          .getService(command.getServiceName());
      org.apache.ambari.server.state.StackId stack = service.getDesiredStackId();
      String path = metadata.get().getService(stack.getStackName(), stack.getStackVersion(), service.getName())
          .getServicePackageFolder();
      if (path != null) {
        params.put("service_package_folder", resolveCurrentResource(path));
      }
      String hooks = metadata.get().getStack(stack.getStackName(), stack.getStackVersion()).getHooksFolder();
      if (hooks == null) {
        hooks = configuration.getProperty(Configuration.HOOKS_FOLDER);
      }
      String hookResource = hooks.isEmpty() ? "" : resolveCurrentResource(hooks);
      params.put(PINNED_HOOKS, hookResource);
      Map<String, String> clusterParameters = new TreeMap<>(command.getClusterLevelParams() == null
          ? Map.of() : command.getClusterLevelParams());
      clusterParameters.put("hooks_folder", hookResource);
      command.setClusterLevelParams(clusterParameters);
    } catch (org.apache.ambari.server.AmbariException e) {
      throw new MpackException(MpackException.Code.INVALID_TARGET, "Task service definition is unavailable");
    }
  }

  public String resolveCurrentResource(String path) {
    MpackSnapshots.Snapshot snapshot = runtime.snapshot();
    if (snapshot == null) {
      return path;
    }
    String prefix = "mpacks/" + snapshot.id() + "/";
    String resolved = path.startsWith(prefix) ? path : prefix + path;
    if (!snapshot.archiveDigests().containsKey(resolved)) {
      throw new MpackException(MpackException.Code.INVALID_TARGET, "Execution resource is absent from the definition snapshot");
    }
    return resolved;
  }

  public Map<String, String> metadataParameters() {
    Lock read = runtime.readLock();
    read.lock();
    try {
      MpackSnapshots.Snapshot snapshot = runtime.snapshot();
      return snapshot == null ? Map.of() : parameters(snapshot);
    } finally {
      read.unlock();
    }
  }

  private Map<String, String> parameters(MpackSnapshots.Snapshot snapshot) {
    Map<String, String> references = new TreeMap<>();
    String prefix = "mpacks/" + snapshot.id() + "/";
    snapshot.archiveDigests().keySet().forEach(path -> references.put(path.substring(prefix.length()), path));
    return Map.of(SNAPSHOT_ID, snapshot.id(),
        RESOURCE_CONTRACT, RESOURCE_PROTOCOL,
        RESOURCE_REFERENCES, MpackJson.canonical(MpackJson.tree(references)),
        ExecutionCommand.KeyNames.RESOURCE_ARCHIVE_DIGESTS,
        MpackJson.canonical(MpackJson.tree(snapshot.archiveDigests())));
  }

  private void verify(Map<String, String> params, MpackSnapshots.Snapshot snapshot) {
    if (params.get(EXECUTION_ID) == null || !RESOURCE_PROTOCOL.equals(params.get(RESOURCE_CONTRACT))
        || !MpackJson.tree(snapshot.archiveDigests()).equals(
            MpackJson.read(params.get(ExecutionCommand.KeyNames.RESOURCE_ARCHIVE_DIGESTS)))) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT,
          "Task resources do not match the selected definition snapshot");
    }
    String expected = parameters(snapshot).get(RESOURCE_REFERENCES);
    if (!MpackJson.read(expected).equals(MpackJson.read(params.get(RESOURCE_REFERENCES)))) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT,
          "Task resource paths do not match the selected definition snapshot");
    }
  }
}
