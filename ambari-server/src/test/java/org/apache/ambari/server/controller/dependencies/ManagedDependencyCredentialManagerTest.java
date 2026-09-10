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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.*;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.*;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManagedDependencyCredentialManagerTest {
  private final String hash = "sha256:" + "a".repeat(64);
  private final ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
  private final Clusters clusters = mock(Clusters.class);
  private final ActionManager actions = mock(ActionManager.class);
  private final ManagedDependencyOperationDispatcher dispatcher = mock(ManagedDependencyOperationDispatcher.class);
  private ManagedDependencyCredentialManager manager;
  private ManagedDependencyTaskResultProcessor processor;
  private ServiceDependencyHostResultEntity preparation;
  private HostRoleCommand credential;
  private ExecutionCommand credentialExecution;
  private String principal;

  @BeforeEach
  void prepareActualProducerAndConsumer() throws Exception {
    var snapshot = new ManagedDependencyCommandTest().secureSnapshot(ManagedDependencyType.HDFS, UUID.randomUUID(), false);
    UUID operationId = UUID.randomUUID();
    var command = ManagedDependencyCommand.prepareConsumer(snapshot, operationId, 4L, 41L,
        "hadoop_3_3_0_0_1-client", "3.3.0", hash);
    preparation = ManagedDependencyOperationDispatcher.commandEntity(command, ManagedDependencyType.HDFS, 41L, "HBASE_MASTER");
    preparation.setAmbariRequestId(101L); preparation.setAmbariStageId(1L); preparation.setAmbariTaskId(7L);
    preparation.setState("SUCCEEDED"); preparation.setPreparationObservationId(UUID.randomUUID().toString());
    preparation.setPreparationObservationFingerprint(hash); preparation.setPackageName("hadoop_3_3_0_0_1-client");
    preparation.setPackageVersion("3.3.0-1"); preparation.setClientSoftwareVersion("3.3.0");
    preparation.setIdentityFingerprint(hash); preparation.setObservedPackageHash(hash); preparation.setRenderedConfigHash(hash);
    preparation.setResultHash(hash);
    ServiceDependencyBindingEntity binding = new ServiceDependencyBindingEntity();
    binding.setBindingId(snapshot.bindingId().toString()); binding.setConsumerClusterId(11L);
    binding.setDesiredSnapshotVersion(snapshot.snapshotVersion()); binding.setOperationEpoch(4L);
    binding.setActiveOperationId(operationId.toString()); binding.setDependencyType("HDFS");
    binding.setState("PROVISIONING"); binding.setProvisioningPhase("CONSUMER_CREDENTIALS_REQUIRED");
    when(dao.findBinding(binding.getBindingId())).thenReturn(binding);
    when(dao.findHostResult(binding.getBindingId(), snapshot.snapshotVersion(), 4L, 41L, "HDFS", "PREPARE_HDFS_CONSUMER"))
        .thenReturn(preparation);
    ServiceDependencySnapshotEntity storedSnapshot = new ServiceDependencySnapshotEntity();
    storedSnapshot.setSnapshotJson(StageUtils.getGson().toJson(snapshot));
    when(dao.findSnapshot(binding.getBindingId(), snapshot.snapshotVersion())).thenReturn(storedSnapshot);
    when(dao.findPreparationsByRequest(101L)).thenReturn(List.of(preparation));
    doAnswer(call -> { preparation.setCredentialPlanJson(call.getArgument(2)); return null; })
        .when(dao).recordCredentialPlan(any(), nullable(String.class), anyString());
    Cluster cluster = mock(Cluster.class); Service hbase = mock(Service.class); ServiceComponent master = mock(ServiceComponent.class);
    Config hbaseConfig = mock(Config.class);
    when(clusters.getClusterById(11L)).thenReturn(cluster); when(cluster.getService("HBASE")).thenReturn(hbase);
    when(cluster.getDesiredConfigByType("hbase-site")).thenReturn(hbaseConfig);
    when(hbaseConfig.getProperties()).thenReturn(Map.of("hbase.master.keytab.file", "/etc/security/keytabs/hbase.keytab"));
    when(hbase.getServiceComponents()).thenReturn(Map.of("HBASE_MASTER", master)); when(master.getName()).thenReturn("HBASE_MASTER");
    when(master.getServiceComponentHosts()).thenReturn(Map.of("host-a", mock(ServiceComponentHost.class)));
    HostRoleCommand install = task(7L, 1L, Role.HBASE_MASTER, RoleCommand.INSTALL, new ExecutionCommand());
    credentialExecution = new ExecutionCommand(); credentialExecution.setTaskId(8L); credentialExecution.setRequestAndStage(101L, 2L);
    credentialExecution.setCommandParams(Map.of("custom_command", "SET_KEYTAB"));
    credential = task(8L, 2L, Role.KERBEROS_CLIENT, RoleCommand.CUSTOM_COMMAND, credentialExecution);
    when(credential.getCustomCommandName()).thenReturn("SET_KEYTAB");
    when(actions.getTaskById(8L)).thenReturn(credential); when(actions.getRequestTasks(101L)).thenReturn(List.of(install, credential));
    processor = new ManagedDependencyTaskResultProcessor(dao, dispatcher, clusters, () -> actions);
    manager = new ManagedDependencyCredentialManager(dao, clusters, () -> actions, () -> processor);
    var field = ManagedDependencyTaskResultProcessor.class.getDeclaredField("credentialManager"); field.setAccessible(true); field.set(processor, manager);
    manager.associate(101L, List.of(install, credential));
    principal = snapshot.consumerIdentity().principalPatterns().first().replace("_HOST", "host-a");
    credentialExecution.setKerberosCommandParams(List.of(Map.of("principal", principal,
        "keytab_file_path", "/etc/security/keytabs/hbase.keytab")));
    when(credential.getStructuredOut()).thenReturn(StageUtils.getGson().toJson(Map.of("keytabIdentities",
        List.of(Map.of("principal", principal, "keytab", "/etc/security/keytabs/hbase.keytab")))));
  }

  private HostRoleCommand task(long id, long stage, Role role, RoleCommand command, ExecutionCommand execution) {
    HostRoleCommand task = mock(HostRoleCommand.class); ExecutionCommandWrapper wrapper = mock(ExecutionCommandWrapper.class);
    execution.setClusterId("11"); when(wrapper.getExecutionCommand()).thenReturn(execution);
    when(task.getExecutionCommandWrapper()).thenReturn(wrapper); when(task.getTaskId()).thenReturn(id);
    when(task.getStageId()).thenReturn(stage); when(task.getRequestId()).thenReturn(101L); when(task.getHostId()).thenReturn(41L);
    when(task.getHostName()).thenReturn("host-a"); when(task.getRole()).thenReturn(role); when(task.getRoleCommand()).thenReturn(command);
    when(task.getStatus()).thenReturn(HostRoleStatus.COMPLETED); return task;
  }

  @Test
  void productionTerminalCallbackPublishesVerificationFromFrozenCredentialPlan() {
    manager.beforeSend(credentialExecution, 41L);
    CommandReport report = new CommandReport(); report.setTaskId(8L); report.setStatus("COMPLETED");
    processor.process(report, "host-a", credential);
    var next = org.mockito.ArgumentCaptor.forClass(ServiceDependencyHostResultEntity.class);
    verify(dao).planCommand(next.capture());
    assertEquals("VERIFY_HDFS_CONSUMER", next.getValue().getCheckKind());
    assertEquals(preparation.getOperationEpoch(), next.getValue().getOperationEpoch());
    assertEquals(preparation.getOperationId(), next.getValue().getOperationId());
    verify(dispatcher).dispatchSafely(next.getValue());
  }

  @Test
  void restartReconcilesTheSamePersistedProducerWithoutScanningOtherRequests() {
    manager.beforeSend(credentialExecution, 41L);
    new ManagedDependencyCredentialManager(dao, clusters, () -> actions, () -> processor).recover(preparation);
    verify(dao).planCommand(any());
    verify(actions, never()).getRequests(any());
  }

  @Test
  void successfulTaskWithWrongIdentityFailsInsteadOfAdvancing() {
    manager.beforeSend(credentialExecution, 41L);
    when(credential.getStructuredOut()).thenReturn("{\"keytabs\":{\"wrong@EXAMPLE.COM\":\"/etc/security/keytabs/hbase.keytab\"}}");
    manager.recover(preparation);
    verify(dao).failCredentials(any()); verify(dao, never()).planCommand(any());
  }

  @Test
  void completionWithoutProducerTransmissionCannotAdvance() {
    manager.recover(preparation);
    verify(dao).failCredentials(any()); verify(dao, never()).planCommand(any());
  }
}
