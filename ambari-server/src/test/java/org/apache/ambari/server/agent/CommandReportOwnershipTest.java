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
package org.apache.ambari.server.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapperFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyTaskResultProcessor;
import org.apache.ambari.server.events.ActionFinalReportReceivedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.inject.Injector;

class CommandReportOwnershipTest {
  private ActionDBAccessor db;
  private ActionManager manager;
  private Clusters clusters;
  private HeartbeatProcessor processor;
  private HostRoleCommand task;
  private CommandReport report;
  private Stage stage;

  @BeforeEach
  void setUp() {
    db = mock(ActionDBAccessor.class);
    manager = new ActionManager(db, null, null);
    clusters = mock(Clusters.class);
    processor = new HeartbeatProcessor(clusters, manager, null, mock(Injector.class));
    processor.ambariEventPublisher = mock(AmbariEventPublisher.class);
    processor.kerberosKeytabPrincipalDAO = mock(KerberosKeytabPrincipalDAO.class);
    processor.managedDependencyTaskResultProcessor = mock(ManagedDependencyTaskResultProcessor.class);
    processor.gson = StageUtils.getGson();
    stage = mock(Stage.class);
    when(stage.getClusterId()).thenReturn(2L);
    when(db.getStage("101-3")).thenReturn(stage);
    task = persistedTask("host-b", "2", Role.KERBEROS_CLIENT, RoleCommand.CUSTOM_COMMAND,
        "KERBEROS", "SET_KEYTAB");
    when(db.getTasks(any())).thenReturn(List.of(task));
    report = report("2", Role.KERBEROS_CLIENT, RoleCommand.CUSTOM_COMMAND, "KERBEROS", "SET_KEYTAB");
  }

  // Reconstruct the real task and execution wrapper from persistence-shaped data.
  // Do not stub the identity-validation method or deserialize via live configs.
  private HostRoleCommand persistedTask(String hostname, String clusterId, Role role,
      RoleCommand roleCommand, String service, String custom) {
    HostEntity host = new HostEntity();
    host.setHostId(22L);
    host.setHostName(hostname);
    HostRoleCommandEntity entity = new HostRoleCommandEntity();
    entity.setTaskId(987L);
    entity.setRequestId(101L);
    entity.setStageId(3L);
    entity.setHostEntity(host);
    entity.setRole(role);
    entity.setRoleCommand(roleCommand);
    entity.setStatus(HostRoleStatus.IN_PROGRESS);

    ExecutionCommand execution = new ExecutionCommand();
    execution.setClusterId(clusterId);
    execution.setServiceName(service);
    execution.setCommandParams(custom == null ? Map.of() : Map.of("custom_command", custom));
    String json = StageUtils.getGson().toJson(execution);
    ExecutionCommandEntity storedExecution = new ExecutionCommandEntity();
    storedExecution.setCommand(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    ExecutionCommandDAO executionDAO = mock(ExecutionCommandDAO.class);
    when(executionDAO.findByPK(987L)).thenReturn(storedExecution);
    ExecutionCommandWrapperFactory wrappers = mock(ExecutionCommandWrapperFactory.class);
    when(wrappers.createFromJson(json)).thenReturn(new ExecutionCommandWrapper(json));
    return new HostRoleCommand(entity, null, executionDAO, wrappers);
  }

  private CommandReport report(String clusterId, Role role, RoleCommand command,
      String service, String custom) {
    CommandReport value = new CommandReport();
    value.setTaskId(987L);
    value.setActionId("101-3");
    value.setClusterId(clusterId);
    value.setRole(role.name());
    value.setRoleCommand(command.name());
    value.setServiceName(service);
    value.setCustomCommand(custom);
    value.setStatus("COMPLETED");
    value.setStructuredOut("{\"keytabs\":{\"identity\":\"/etc/security/keytabs/hbase.service.keytab\"}}");
    return value;
  }

  @Test
  void foreignHostCannotWriteTaskStateThroughActionManager() {
    manager.processTaskResponse("host-a", List.of(report), Map.of(987L, task));
    verify(db, org.mockito.Mockito.never()).updateHostRoleStates(any());
  }

  @Test
  void foreignHostCannotReachHeartbeatSideEffects() throws Exception {
    assertTrue(processor.processCommandReports(List.of(report), "host-a", 1L).isEmpty());
    assertNoReportEffects();
  }

  static Stream<Consumer<CommandReport>> invalidIdentities() {
    return Stream.of(
        value -> value.setTaskId(999L),
        value -> value.setClusterId("1"),
        value -> value.setClusterId("invalid"),
        value -> value.setClusterId(null),
        value -> value.setActionId("102-3"),
        value -> value.setActionId("101-4"),
        value -> value.setRole(Role.HBASE_MASTER.name()),
        value -> value.setRoleCommand(RoleCommand.ACTIONEXECUTE.name()),
        value -> value.setServiceName("HBASE"),
        value -> value.setCustomCommand("CHECK_KEYTABS"),
        value -> value.setCustomCommand(null),
        value -> value.setStatus("invalid"),
        value -> value.setStatus(null));
  }

  @ParameterizedTest
  @MethodSource("invalidIdentities")
  void forgedOrMalformedReportsCannotReachAnySideEffect(Consumer<CommandReport> corrupt) throws Exception {
    corrupt.accept(report);
    assertTrue(processor.processCommandReports(List.of(report), "host-b", 1L).isEmpty());
    assertNoReportEffects();
  }

  @Test
  void actionCompletionFromForeignHostCannotPublishFinalEvent() throws Exception {
    task = persistedTask("host-b", "2", Role.INSTALL_PACKAGES, RoleCommand.ACTIONEXECUTE, null, null);
    when(db.getTasks(any())).thenReturn(List.of(task));
    report = report("2", Role.INSTALL_PACKAGES, RoleCommand.ACTIONEXECUTE, "null", null);
    assertTrue(processor.processCommandReports(List.of(report), "host-a", 1L).isEmpty());
    assertNoReportEffects();
  }

  @Test
  void mixedBatchRetainsOwnedCredentialCompletion() throws Exception {
    Host host = mock(Host.class);
    when(host.getHostId()).thenReturn(22L);
    when(clusters.getHost("host-b")).thenReturn(host);
    KerberosKeytabPrincipalEntity keytab = new KerberosKeytabPrincipalEntity();
    when(processor.kerberosKeytabPrincipalDAO.findByHostAndKeytab(
        22L, "/etc/security/keytabs/hbase.service.keytab")).thenReturn(List.of(keytab));
    CommandReport foreign = report("1", Role.KERBEROS_CLIENT, RoleCommand.CUSTOM_COMMAND,
        "KERBEROS", "SET_KEYTAB");
    List<CommandReport> input = new ArrayList<>();
    input.add(foreign);
    input.add(null);
    input.add(report);

    assertEquals(List.of(report), processor.processCommandReports(input, "host-b", 1L));
    assertEquals(3, input.size());
    verify(processor.kerberosKeytabPrincipalDAO).merge(keytab);
    assertTrue(keytab.isDistributed());
    verify(db).updateHostRoleStates(List.of(report));
    verify(processor.managedDependencyTaskResultProcessor).process(report, "host-b", task);
    verifyNoInteractions(processor.ambariEventPublisher);
  }

  @Test
  void globalTaskCompletionRetainsItsAssignedHostAndStage() throws Exception {
    task = persistedTask("host-b", "-1", Role.INSTALL_PACKAGES, RoleCommand.ACTIONEXECUTE, null, null);
    when(db.getTasks(any())).thenReturn(List.of(task));
    when(stage.getClusterId()).thenReturn(-1L);
    when(clusters.getHost("host-b")).thenReturn(mock(Host.class));
    report = report("-1", Role.INSTALL_PACKAGES, RoleCommand.ACTIONEXECUTE, "null", null);

    assertEquals(List.of(report), processor.processCommandReports(List.of(report), "host-b", 1L));
    verify(processor.ambariEventPublisher).publish(any(ActionFinalReportReceivedEvent.class));
    verify(db).updateHostRoleStates(List.of(report));
    verifyNoInteractions(processor.kerberosKeytabPrincipalDAO);
  }

  @Test
  void progressTemplateDoesNotRequireCustomCommandResult() {
    report.setStatus("IN_PROGRESS");
    report.setCustomCommand(null);
    manager.processTaskResponse("host-b", List.of(report), Map.of(987L, task));
    verify(db).updateHostRoleStates(List.of(report));
  }

  @Test
  void missingStageFailsClosed() throws Exception {
    when(db.getStage("101-3")).thenReturn(null);
    assertTrue(processor.processCommandReports(List.of(report), "host-b", 1L).isEmpty());
    assertNoReportEffects();
  }

  private void assertNoReportEffects() {
    verify(db, org.mockito.Mockito.never()).updateHostRoleStates(any());
    verifyNoInteractions(clusters, processor.ambariEventPublisher,
        processor.kerberosKeytabPrincipalDAO, processor.managedDependencyTaskResultProcessor);
    assertEquals(HostRoleStatus.IN_PROGRESS, task.getStatus());
    assertEquals(-1L, task.getStartTime());
  }
}
