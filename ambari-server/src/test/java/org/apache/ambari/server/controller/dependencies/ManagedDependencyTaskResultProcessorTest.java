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
package org.apache.ambari.server.controller.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand.Result;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand.ResultStatus;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CommandCompletion;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyOperationEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ManagedDependencyTaskResultProcessorTest {
  private static final UUID BINDING_ID =
      UUID.fromString("d3dbf29e-662b-476b-a36e-18f96d5b82d4");
  private static final UUID OPERATION_ID =
      UUID.fromString("c03447ff-e1c0-436b-bf60-bb89a293cc3d");
  private static final String HASH = "sha256:" + "b".repeat(64);

  private ServiceDependencyDAO dao;
  private ManagedDependencyOperationDispatcher dispatcher;
  private ManagedDependencyTaskResultProcessor processor;
  private ManagedDependencyCommand preparation;
  private ServiceDependencyHostResultEntity persisted;
  private HostRoleCommand task;
  private CommandReport report;

  @BeforeEach
  void setUp() {
    dao = mock(ServiceDependencyDAO.class);
    dispatcher = mock(ManagedDependencyOperationDispatcher.class);
    processor = new ManagedDependencyTaskResultProcessor(dao, dispatcher, mock(Clusters.class));
    preparation = ManagedDependencyCommand.prepareConsumer(snapshot(), OPERATION_ID, 4L, 41L,
        "hadoop_3_3_0_0_1-client", "3.3.0", HASH);
    persisted = ManagedDependencyOperationDispatcher.commandEntity(
        preparation, ManagedDependencyType.HDFS, 41L, "HBASE_MASTER");
    persisted.setState("DISPATCHED");
    persisted.setAmbariRequestId(101L);
    persisted.setAmbariStageId(2L);
    persisted.setAmbariTaskId(7L);
    when(dao.findHostResult(BINDING_ID.toString(), 1L, 4L, 41L, "HDFS",
        "PREPARE_HDFS_CONSUMER")).thenReturn(persisted);
    when(dao.findBinding(BINDING_ID.toString())).thenReturn(binding());
    when(dao.findOperation(OPERATION_ID.toString())).thenReturn(operation());
    ServiceDependencySnapshotEntity snapshotEntity = new ServiceDependencySnapshotEntity();
    snapshotEntity.setBindingId(BINDING_ID.toString());
    snapshotEntity.setSnapshotVersion(1L);
    snapshotEntity.setSnapshotJson(StageUtils.getGson().toJson(snapshot()));
    when(dao.findSnapshot(BINDING_ID.toString(), 1L)).thenReturn(snapshotEntity);

    ExecutionCommand execution = new ExecutionCommand();
    execution.setCommandParams(Map.of(ManagedDependencyRuntimePlanner.BUNDLE_PARAMETER,
        StageUtils.getGson().toJson(ManagedDependencyCommandBundle.of(
            41L, "hbase_mc_cb", HASH, List.of(preparation)))));
    ExecutionCommandWrapper wrapper = mock(ExecutionCommandWrapper.class);
    when(wrapper.getExecutionCommand()).thenReturn(execution);
    task = mock(HostRoleCommand.class);
    when(task.getExecutionCommandWrapper()).thenReturn(wrapper);
    when(task.getTaskId()).thenReturn(7L);
    when(task.getRequestId()).thenReturn(101L);
    when(task.getStageId()).thenReturn(2L);
    when(task.getHostId()).thenReturn(41L);
    when(task.getHostName()).thenReturn("hbase-a");
    when(task.getRole()).thenReturn(Role.HBASE_MASTER);
    when(task.getRoleCommand()).thenReturn(RoleCommand.INSTALL);

    Result result = successfulPreparationResult();
    report = new CommandReport();
    report.setTaskId(7L);
    report.setStatus("COMPLETED");
    report.setRole(Role.HBASE_MASTER.name());
    report.setRoleCommand(RoleCommand.INSTALL.name());
    report.setStructuredOut(StageUtils.getGson().toJson(Map.of(
        "managedDependencyPreparationResults", List.of(result))));
  }

  @Test
  void exactPreparationPersistsObservationAndVerifyIntentBeforeDispatch() {
    processor.process(report, "hbase-a", task);

    ArgumentCaptor<CommandCompletion> completion = ArgumentCaptor.forClass(CommandCompletion.class);
    verify(dao).completeCommand(any(), anyLong(), anyLong(), anyLong(), completion.capture());
    CommandCompletion value = completion.getValue();
    assertNotNull(value.preparationObservationId());
    assertEquals(preparation.envelope().immutableRequestHash(),
        value.preparationRequestHash());
    assertEquals("1.rpm", value.packageVersion());
    assertEquals("VERIFY_HDFS_CONSUMER", value.nextCommand().getCheckKind());
    ManagedDependencyCommand verification = StageUtils.getGson().fromJson(
        value.nextCommand().getCommandJson(), ManagedDependencyCommand.class);
    assertEquals(value.preparationObservationId(),
        verification.parameters().get("preparation.observation.id"));
    verify(dispatcher).dispatchSafely(value.nextCommand());
  }

  @Test
  void duplicateCallbackResumesOnlyThePersistedExactSuccessor() {
    processor.process(report, "hbase-a", task);
    ArgumentCaptor<CommandCompletion> completion = ArgumentCaptor.forClass(CommandCompletion.class);
    verify(dao).completeCommand(any(), anyLong(), anyLong(), anyLong(), completion.capture());
    ServiceDependencyHostResultEntity successor = completion.getValue().nextCommand();
    persisted.setResultHash(completion.getValue().resultHash());
    when(dao.findHostResult(BINDING_ID.toString(), 1L, 4L, 41L, "HDFS",
        "VERIFY_HDFS_CONSUMER")).thenReturn(successor);

    processor.process(report, "hbase-a", task);

    verify(dispatcher, times(2)).dispatchSafely(successor);
    verify(dao, times(1)).completeCommand(any(), anyLong(), anyLong(), anyLong(), any());
  }

  @Test
  void resultFromDifferentAuthenticatedHostCannotAdvanceBinding() {
    processor.process(report, "hbase-b", task);

    verify(dao, never()).completeCommand(any(), anyLong(), anyLong(), anyLong(), any());
    verify(dispatcher, never()).dispatchSafely(any());
  }

  @Test
  void malformedTerminalPreparationPersistsSanitizedRetryableFailure() {
    report.setStructuredOut("{}");

    processor.process(report, "hbase-a", task);

    verify(dao).failTaskCommands(101L, 2L, 7L,
        "DEPENDENCY_RESULT_INVALID",
        "The managed dependency task completed without valid structured evidence.");
    verify(dao, never()).completeCommand(any(), anyLong(), anyLong(), anyLong(), any());
  }

  @Test
  void credentialCompletionCannotAdvanceBeforeCurrentSecurePreparationPhase() {
    ActionManager actionManager = mock(ActionManager.class);
    ManagedDependencyTaskResultProcessor secureProcessor =
        new ManagedDependencyTaskResultProcessor(dao, dispatcher, mock(Clusters.class),
            () -> actionManager);

    assertThrows(org.apache.ambari.server.AmbariException.class,
        () -> secureProcessor.dispatchVerificationAfterCredentials(
            BINDING_ID.toString(), 4L, 41L, 501L, Set.of(601L)));

    verifyNoInteractions(actionManager);
    verify(dao, never()).planCommand(any());
    verify(dispatcher, never()).dispatchSafely(any());
  }

  @Test
  void colocatedNonCanonicalInstallTaskCannotPublishPreparationEvidence() {
    when(task.getRole()).thenReturn(Role.HBASE_REGIONSERVER);
    report.setRole(Role.HBASE_REGIONSERVER.name());

    processor.process(report, "hbase-a", task);

    verify(dao, never()).completeCommand(any(), anyLong(), anyLong(), anyLong(), any());
    verify(dao, never()).failTaskCommands(anyLong(), anyLong(), anyLong(), any(), any());
  }

  private Result successfulPreparationResult() {
    return new Result(preparation.name(), preparation.envelope(), ResultStatus.SUCCEEDED,
        new TreeMap<>(Map.ofEntries(
            Map.entry("applied.snapshot.fingerprint",
                preparation.parameters().get("snapshot.fingerprint")),
            Map.entry("client.config.fingerprint",
                preparation.parameters().get("client.config.fingerprint")),
            Map.entry("client.package.name", "hadoop_3_3_0_0_1-client"),
            Map.entry("client.package.version", "1.rpm"),
            Map.entry("client.software.kind", "HADOOP_CLIENT"),
            Map.entry("client.software.semantic.version", "3.3.0"),
            Map.entry("consumer.user", "hbase_mc_cb"),
            Map.entry("host.id", "41"),
            Map.entry("identity.fingerprint", HASH))), null, null);
  }

  private ServiceDependencyBindingEntity binding() {
    ServiceDependencyBindingEntity binding = new ServiceDependencyBindingEntity();
    binding.setBindingId(BINDING_ID.toString());
    binding.setConsumerClusterId(11L);
    binding.setConsumerServiceName("HBASE");
    binding.setProviderClusterId(22L);
    binding.setProviderServiceName("HDFS");
    binding.setDependencyType("HDFS");
    binding.setState("PROVISIONING");
    binding.setProvisioningPhase("CONSUMER_PREPARING");
    binding.setOperationEpoch(4L);
    binding.setDesiredSnapshotVersion(1L);
    binding.setActiveOperationId(OPERATION_ID.toString());
    return binding;
  }

  private ServiceDependencyOperationEntity operation() {
    ServiceDependencyOperationEntity operation = new ServiceDependencyOperationEntity();
    operation.setOperationId(OPERATION_ID.toString());
    operation.setBindingId(BINDING_ID.toString());
    operation.setOperationKind("CREATE");
    operation.setOperationEpoch(4L);
    operation.setTargetSnapshotVersion(1L);
    operation.setState("CONSUMER_PREPARING");
    return operation;
  }

  private ManagedDependencySnapshot snapshot() {
    ManagedDependencyVersion version = new ManagedDependencyVersion(
        "BIGTOP", "3.3.0", true, "3.3.0",
        new TreeMap<>(Map.of("distribution", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 31L, List.of());
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hbase_mc_cb", new TreeSet<>(), false, "hbase_mc_cb", true, "0700", false);
    return new ManagedDependencySnapshot(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION,
        BINDING_ID, 1L, ManagedDependencyType.HDFS,
        new ManagedDependencyServiceKey(22L, "HDFS"),
        ManagedDependencyNamespace.hdfs(BINDING_ID, "hdfs://provider"),
        version.compatibility(), ManagedDependencySecurityMode.INSECURE, identity,
        new TreeMap<>(Map.of("fs.defaultFS", "hdfs://provider")), new TreeMap<>(),
        new TreeMap<>(), HASH, HASH, HASH);
  }
}
