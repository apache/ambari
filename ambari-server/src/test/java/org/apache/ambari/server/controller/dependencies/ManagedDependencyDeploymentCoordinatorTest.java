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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.ambari.server.actionmanager.*;
import org.apache.ambari.server.controller.*;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDeploymentCoordinator.Target;
import org.apache.ambari.server.orm.dao.*;
import org.apache.ambari.server.orm.entities.*;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.*;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/** Exercises the real reconciler; persistence/actuator boundaries are mocked here and tested in H2 separately. */
class ManagedDependencyDeploymentCoordinatorTest {
  private final UUID id = UUID.fromString("00000000-0000-4000-8000-000000000091");
  private final ServiceDependencyDeploymentDAO deployments = mock(ServiceDependencyDeploymentDAO.class);
  private final ServiceDependencyDAO dependencies = mock(ServiceDependencyDAO.class);
  private final Clusters clusters = mock(Clusters.class);
  private final Users users = mock(Users.class);
  private final ActionManager actions = mock(ActionManager.class);
  private final AmbariManagementController controller = mock(AmbariManagementController.class);
  private final ResourceProviderFactory providers = mock(ResourceProviderFactory.class);
  private final ManagedServiceDependencyCoordinator bindings = mock(ManagedServiceDependencyCoordinator.class);
  private final Cluster consumer = mock(Cluster.class);
  private final ServiceComponentHost host = mock(ServiceComponentHost.class);
  private final ServiceDependencyBindingEntity binding = new ServiceDependencyBindingEntity();
  private ServiceDependencyDeploymentEntity row;
  private ManagedDependencyDeploymentCoordinator engine;

  @BeforeEach
  void setUp() throws Exception {
    var auth = TestAuthenticationFactory.createAdministrator(7, "alice");
    SecurityContextHolder.getContext().setAuthentication(auth);
    var owner = new UserEntity(); owner.setUserId(7); owner.setUserName("alice"); owner.setActive(true);
    User user = mock(User.class); when(user.getUserId()).thenReturn(7); when(user.getUserName()).thenReturn("alice");
    when(user.isActive()).thenReturn(true);
    when(users.getUserEntity(7)).thenReturn(owner); when(users.getUser(owner)).thenReturn(user);
    when(users.getUserAuthorities(owner)).thenReturn(auth.getAuthorities().stream().map(AmbariGrantedAuthority.class::cast).toList());
    when(consumer.getClusterId()).thenReturn(11L); when(consumer.getResourceId()).thenReturn(101L);
    when(consumer.getClusterName()).thenReturn("consumer");
    when(consumer.executeUnderWriteLockUntilTransactionCompletion(any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(0)).get());
    when(clusters.getCluster("consumer")).thenReturn(consumer); when(clusters.getClusterById(11L)).thenReturn(consumer);
    Cluster provider = mock(Cluster.class); when(provider.getClusterId()).thenReturn(22L);
    when(provider.executeUnderWriteLockUntilTransactionCompletion(any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(0)).get());
    when(clusters.getClusterById(22L)).thenReturn(provider);
    Service service = mock(Service.class); ServiceComponent component = mock(ServiceComponent.class);
    Host realHost = mock(Host.class); when(realHost.getHostId()).thenReturn(41L);
    when(host.getHost()).thenReturn(realHost); when(host.getState()).thenReturn(State.INSTALLED);
    when(consumer.getService("HBASE")).thenReturn(service);
    when(service.getServiceComponent("HBASE_MASTER")).thenReturn(component);
    when(component.getServiceComponentHost("consumer-host")).thenReturn(host);
    binding.setBindingId(UUID.randomUUID().toString()); binding.setConsumerClusterId(11L);
    binding.setProviderClusterId(22L); binding.setDesiredSnapshotVersion(1L); binding.setOperationEpoch(1L);
    binding.setState("PROVISIONING"); binding.setProviderPreparationHash("prepared");
    when(dependencies.findByConsumer(11L, "HBASE")).thenReturn(List.of(binding));
    when(bindings.list("consumer", "HBASE")).thenReturn(List.of(Map.of("ownership", "managed",
        "capabilities", Map.of("install_or_configure_allowed", true))));
    when(deployments.find(id.toString())).thenAnswer(call -> row);
    when(deployments.create(any())).thenAnswer(call -> row = call.getArgument(0));
    when(deployments.active(0, 64)).thenAnswer(call -> row == null ? List.of() : List.of(row));
    when(deployments.mutate(eq(id.toString()), any())).thenAnswer(call -> {
      ((ServiceDependencyDeploymentDAO.Mutation) call.getArgument(1)).apply(row); return row;
    });
    engine = newEngine();
    engine.launch("consumer", id, List.of(new Target("HBASE", "HBASE_MASTER", "consumer-host", 41L)), false);
  }

  @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

  @Test void providerUpdateBeforeFirstInstallRetainsOldApprovalAndResumesSameDeployment() {
    request("FAILED", "INSTALL", null, 11L, HostRoleStatus.FAILED);
    when(host.getState()).thenReturn(State.INIT);
    binding.setDesiredSnapshotVersion(2L);
    binding.setOperationEpoch(2L);
    binding.setSnapshotApproval("APPROVED");
    assertTrue((Boolean) engine.get("consumer", id).get("retry_allowed"));
    UUID retryId = UUID.randomUUID();
    engine.retry("consumer", id, retryId);
    assertEquals("NEW", row.getState());
    assertTrue(row.getPlanJson().contains("\"snapshotVersion\":1"));
    assertTrue(row.getProgressJson().contains("\"approvedBindings\":[{\"bindingId\":\"" + binding.getBindingId()
        + "\",\"snapshotVersion\":2"));
    assertTrue(row.getProgressJson().contains("\"snapshotVersion\":1"));
    String retained = row.getProgressJson();
    newEngine().retry("consumer", id, retryId);
    assertEquals(retained, row.getProgressJson());
    verifyNoInteractions(providers, controller);
  }

  @Test void onlyUninstalledBindingApprovalChangesCanResolveWithoutRequestLineage() {
    request("UNRESOLVED", "INSTALL", null, 11L, HostRoleStatus.FAILED);
    when(host.getState()).thenReturn(State.INIT);
    binding.setDesiredSnapshotVersion(2L);
    binding.setSnapshotApproval("APPROVED");
    row.setProgressJson(row.getProgressJson().replace("\"phase\":\"INSTALL\"",
        "\"phase\":\"INSTALL\",\"failureCode\":\"DEPLOYMENT_REQUEST_LINEAGE_MISSING\""));
    assertFalse((Boolean) engine.get("consumer", id).get("retry_allowed"));
    row.setProgressJson(row.getProgressJson().replace("DEPLOYMENT_REQUEST_LINEAGE_MISSING",
        "DEPLOYMENT_BINDING_LINEAGE_CHANGED"));
    assertTrue((Boolean) engine.get("consumer", id).get("retry_allowed"));
    engine.retry("consumer", id, UUID.randomUUID());
    assertEquals("NEW", row.getState());
    assertTrue(row.getProgressJson().contains("\"snapshotVersion\":2"));
  }

  @Test void updatedApprovalCannotReplaceAnInstallationWithMissingCurrentRequestLineage() {
    request("FAILED", "INSTALL", null, 11L, HostRoleStatus.FAILED);
    when(host.getState()).thenReturn(State.INIT);
    binding.setDesiredSnapshotVersion(2L);
    binding.setSnapshotApproval("APPROVED");
    row.setProgressJson(row.getProgressJson().replace("\"history\":[]",
        "\"history\":[{\"attemptId\":\"" + id + "\",\"phase\":\"INSTALL\",\"requestId\":101,\"targets\":[]}]"));
    assertFalse((Boolean) engine.get("consumer", id).get("retry_allowed"));
    assertThrows(ManagedDependencyIntegrationException.class, () -> engine.retry("consumer", id, UUID.randomUUID()));
    assertTrue(row.getPlanJson().contains("\"snapshotVersion\":1"));
  }

  private ManagedDependencyDeploymentCoordinator newEngine() {
    var metadata = new org.apache.ambari.server.metadata.ActionMetadata();
    metadata.addServiceCheckAction("HBASE");
    metadata.addServiceCheckAction("ZOOKEEPER");
    return new ManagedDependencyDeploymentCoordinator(deployments, dependencies, clusters, users,
        () -> actions, () -> controller, providers, () -> bindings, metadata);
  }

  private void request(String state, String phase, Long requestId, long ownerCluster, HostRoleStatus status) {
    row.setState(state);
    row.setProgressJson(StageUtils.getGson().toJson(Map.of("attemptId", id.toString(), "phase", phase,
        "history", List.of(), "requestId", requestId == null ? 0L : requestId)));
    if (requestId == null) row.setProgressJson(row.getProgressJson().replace("\"requestId\":0", "\"requestId\":null"));
    if ("CHECKS".equals(phase) && requestId != null) {
      row.setProgressJson(row.getProgressJson().replace("\"history\":[]",
          "\"history\":[{\"attemptId\":\"" + id + "\",\"phase\":\"CHECKS\",\"requestId\":"
              + requestId + ",\"targets\":[{\"serviceName\":\"HBASE\",\"componentName\":\"HBASE_MASTER\","
              + "\"hostName\":\"consumer-host\",\"hostId\":41}]}]"));
    }
    Request request = mock(Request.class); when(request.getClusterId()).thenReturn(ownerCluster);
    when(actions.getRequests(List.of(requestId == null ? 0L : requestId))).thenReturn(List.of(request));
    HostRoleCommand task = mock(HostRoleCommand.class); when(task.getStatus()).thenReturn(status);
    when(actions.getRequestTasks(requestId == null ? 0L : requestId)).thenReturn(List.of(task));
  }

  @Test void installationCompletionWaitsForBindingVerificationAcrossRestart() {
    request("INSTALLING", "INSTALL", 101L, 11L, HostRoleStatus.COMPLETED);
    engine.recoverOutstanding();
    assertEquals("WAIT_DEPENDENCIES", row.getState());
    newEngine().recoverOutstanding();
    assertEquals("WAIT_DEPENDENCIES", row.getState());
    verifyNoInteractions(providers, controller);
  }

  @Test void missingRequestOrTaskLineageCannotBecomeSuccess() {
    request("INSTALLING", "INSTALL", null, 11L, HostRoleStatus.COMPLETED);
    engine.recoverOutstanding(); assertEquals("UNRESOLVED", row.getState());
    request("INSTALLING", "INSTALL", 101L, 11L, HostRoleStatus.COMPLETED);
    when(actions.getRequestTasks(101L)).thenReturn(List.of());
    engine.recoverOutstanding(); assertEquals("UNRESOLVED", row.getState());
    assertFalse((Boolean) engine.get("consumer", id).get("completed"));
    verifyNoInteractions(providers, controller);
  }

  @Test void anotherClusterRequestIsRejectedBeforeReadingTasks() {
    request("INSTALLING", "INSTALL", 101L, 22L, HostRoleStatus.COMPLETED);
    engine.recoverOutstanding(); assertEquals("UNRESOLVED", row.getState());
    verify(actions, never()).getRequestTasks(101L);
  }

  @Test void failedInstallationIsRetriedOnceWithTheSameAttemptAfterLostResponse() throws Exception {
    request("INSTALLING", "INSTALL", 101L, 11L, HostRoleStatus.FAILED);
    engine.recoverOutstanding(); assertEquals("FAILED", row.getState());
    UUID attempt = UUID.randomUUID(); engine.retry("consumer", id, attempt);
    newEngine().retry("consumer", id, attempt);
    assertEquals("NEW", row.getState());
    verify(bindings, times(1)).retryInstallation("consumer", 101L, attempt);
  }

  @Test void serviceCheckCompletionRequiresRealTargetState() {
    request("CHECKING", "CHECKS", 103L, 11L, HostRoleStatus.COMPLETED);
    engine.recoverOutstanding(); assertEquals("FAILED", row.getState());
    request("CHECKING", "CHECKS", 103L, 11L, HostRoleStatus.COMPLETED);
    when(host.getState()).thenReturn(State.STARTED);
    newEngine().recoverOutstanding(); assertEquals("COMPLETE", row.getState());
    assertTrue((Boolean) engine.get("consumer", id).get("completed"));
  }

  @Test void completedStartPublishesTheRegisteredServiceCheck() throws Exception {
    request("STARTING", "START", 102L, 11L, HostRoleStatus.COMPLETED);
    when(host.getState()).thenReturn(State.STARTED);
    var response = mock(org.apache.ambari.server.controller.RequestStatusResponse.class);
    when(response.getRequestId()).thenReturn(103L);
    when(response.getTasks()).thenReturn(List.of(mock(org.apache.ambari.server.controller.ShortTaskStatus.class)));
    when(controller.createAction(any(), any())).thenReturn(response);
    engine.recoverOutstanding();
    var request = org.mockito.ArgumentCaptor.forClass(org.apache.ambari.server.controller.ExecuteActionRequest.class);
    verify(controller).createAction(request.capture(), any());
    assertEquals("HBASE_SERVICE_CHECK", request.getValue().getCommandName());
    assertEquals("HBASE", request.getValue().getResourceFilters().get(0).getServiceName());
    assertEquals("CHECKING", row.getState());
    assertTrue(row.getProgressJson().contains("\"requestId\":103"));
  }

  @Test void serviceChecksResumeByExactServiceReceiptAcrossCoordinatorRestart() throws Exception {
    request("STARTING", "START", 102L, 11L, HostRoleStatus.COMPLETED);
    when(host.getState()).thenReturn(State.STARTED);
    Service zk = mock(Service.class);
    ServiceComponent zkComponent = mock(ServiceComponent.class);
    when(consumer.getService("ZOOKEEPER")).thenReturn(zk);
    when(zk.getServiceComponent("ZOOKEEPER_SERVER")).thenReturn(zkComponent);
    when(zkComponent.getServiceComponentHost("consumer-host")).thenReturn(host);
    var plan = com.google.gson.JsonParser.parseString(row.getPlanJson()).getAsJsonObject();
    plan.getAsJsonArray("targets").add(com.google.gson.JsonParser.parseString(StageUtils.getGson().toJson(
        new Target("ZOOKEEPER", "ZOOKEEPER_SERVER", "consumer-host", 41L))));
    row.setPlanJson(plan.toString());
    var first = mock(org.apache.ambari.server.controller.RequestStatusResponse.class);
    var second = mock(org.apache.ambari.server.controller.RequestStatusResponse.class);
    when(first.getRequestId()).thenReturn(103L); when(second.getRequestId()).thenReturn(104L);
    var task = mock(org.apache.ambari.server.controller.ShortTaskStatus.class);
    when(first.getTasks()).thenReturn(List.of(task)); when(second.getTasks()).thenReturn(List.of(task));
    when(controller.createAction(any(), any())).thenReturn(first, second);
    engine.recoverOutstanding();
    for (long requestId : List.of(103L, 104L)) {
      Request owned = mock(Request.class); when(owned.getClusterId()).thenReturn(11L);
      when(actions.getRequests(List.of(requestId))).thenReturn(List.of(owned));
      HostRoleCommand completed = mock(HostRoleCommand.class);
      when(completed.getStatus()).thenReturn(HostRoleStatus.COMPLETED);
      when(actions.getRequestTasks(requestId)).thenReturn(List.of(completed));
      newEngine().recoverOutstanding();
    }
    var requests = org.mockito.ArgumentCaptor.forClass(org.apache.ambari.server.controller.ExecuteActionRequest.class);
    verify(controller, times(2)).createAction(requests.capture(), any());
    assertEquals(List.of("HBASE_SERVICE_CHECK", "ZOOKEEPER_QUORUM_SERVICE_CHECK"),
        requests.getAllValues().stream().map(org.apache.ambari.server.controller.ExecuteActionRequest::getCommandName).toList());
    assertEquals("COMPLETE", row.getState());
    assertTrue(row.getProgressJson().contains("\"requestId\":103"));
    assertTrue(row.getProgressJson().contains("\"requestId\":104"));
  }

  @Test void missingServiceCheckHistoryCannotBeReconstructedFromCurrentRequest() {
    request("CHECKING", "CHECKS", 103L, 11L, HostRoleStatus.COMPLETED);
    var progress = com.google.gson.JsonParser.parseString(row.getProgressJson()).getAsJsonObject();
    progress.add("history", new com.google.gson.JsonArray());
    row.setProgressJson(progress.toString());
    when(host.getState()).thenReturn(State.STARTED);
    engine.recoverOutstanding();
    assertEquals("UNRESOLVED", row.getState());
    verifyNoInteractions(controller);
  }

  @Test void failedRetryPublicationRetainsTheOriginalInstallationReceipt() throws Exception {
    request("FAILED", "INSTALL", null, 11L, HostRoleStatus.FAILED);
    row.setProgressJson(row.getProgressJson().replace("\"history\":[]",
        "\"history\":[{\"attemptId\":\"" + id + "\",\"phase\":\"INSTALL\",\"requestId\":101,\"targets\":[]}]"));
    UUID attempt = UUID.randomUUID();
    engine.retry("consumer", id, attempt);
    newEngine().retry("consumer", id, attempt);
    verify(bindings, times(1)).retryInstallation("consumer", 101L, attempt);
    assertTrue(row.getProgressJson().contains("\"requestId\":101"));
    assertEquals("NEW", row.getState());
  }

  @Test void failedBindingBeforeInstallationRetriesPreparationBeforePublishingInstall() {
    binding.setState("FAILED");
    binding.setRowVersion(3L);
    when(bindings.list("consumer", "HBASE")).thenReturn(List.of(Map.of(
        "binding_id", binding.getBindingId(), "capabilities", Map.of("retry_allowed", true))));
    engine.recoverOutstanding();
    assertEquals("FAILED", row.getState());
    assertEquals("INSTALL", engine.get("consumer", id).get("phase"));
    assertTrue((Boolean) engine.get("consumer", id).get("retry_allowed"));
    UUID attempt = UUID.randomUUID();
    engine.retry("consumer", id, attempt);
    newEngine().retry("consumer", id, attempt);
    assertEquals("NEW", row.getState());
    verify(bindings, times(1)).retry(eq("consumer"), eq("HBASE"),
        eq(UUID.fromString(binding.getBindingId())), any());
    verify(bindings, never()).retryInstallation(anyString(), anyLong(), any());
    verifyNoInteractions(providers, controller);
  }

  @Test void providerFailureWithoutPreparationCannotAdvertiseOrExecuteConsumerRetry() {
    binding.setState("FAILED");
    binding.setProviderPreparationHash(null);
    engine.recoverOutstanding();
    assertEquals("FAILED", row.getState());
    assertFalse((Boolean) engine.get("consumer", id).get("retry_allowed"));
    assertThrows(ManagedDependencyIntegrationException.class,
        () -> engine.retry("consumer", id, UUID.randomUUID()));
    verify(bindings, never()).retry(anyString(), anyString(), any(), any());
    verifyNoInteractions(providers, controller);
  }

  @Test void staleBindingBeforeInstallationFailsWithoutWaitingForeverOrRetrying() {
    binding.setState("STALE");
    engine.recoverOutstanding();
    assertEquals("FAILED", row.getState());
    assertFalse((Boolean) engine.get("consumer", id).get("retry_allowed"));
    verifyNoInteractions(providers, controller);
  }

  @Test void lostLaunchResponseReplaysItsIdAndCrossClusterReadIsNotFound() throws Exception {
    newEngine().launch("consumer", id, List.of(new Target("HBASE", "HBASE_MASTER", "consumer-host", 41L)), false);
    verify(deployments, times(1)).create(any());
    Cluster other = mock(Cluster.class); when(other.getResourceId()).thenReturn(202L); when(other.getClusterId()).thenReturn(22L);
    when(clusters.getCluster("provider")).thenReturn(other);
    assertEquals(404, assertThrows(ManagedDependencyIntegrationException.class,
        () -> engine.get("provider", id)).getStatus());
  }

  @Test void permissionsForAnotherClusterCannotReadOrRetryTheDeployment() {
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator(7, "alice", 202L));
    assertEquals(403, assertThrows(ManagedDependencyIntegrationException.class,
        () -> engine.get("consumer", id)).getStatus());
    assertEquals(403, assertThrows(ManagedDependencyIntegrationException.class,
        () -> engine.retry("consumer", id, UUID.randomUUID())).getStatus());
  }

  @Test void anotherAuthorizedUserCanReadButCannotMutateTheOwnerDeployment() {
    row.setState("FAILED");
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator(8, "bob", 101L));
    assertFalse((Boolean) engine.get("consumer", id).get("retry_allowed"));
    assertEquals(404, assertThrows(ManagedDependencyIntegrationException.class,
        () -> engine.retry("consumer", id, UUID.randomUUID())).getStatus());
  }

  @Test void revokedMutationPermissionsRemoveTheOwnersRetryCapability() {
    row.setOwnerUserId(1);
    row.setState("FAILED");
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterUser("alice", 101L));
    assertFalse((Boolean) engine.get("consumer", id).get("retry_allowed"));
    assertEquals(403, assertThrows(ManagedDependencyIntegrationException.class,
        () -> engine.retry("consumer", id, UUID.randomUUID())).getStatus());
  }

  @Test void inactiveOwnerCannotPublishDuringRecoveryAndCallerAuthenticationIsRestored() {
    users.getUserEntity(7).setActive(false);
    var caller = SecurityContextHolder.getContext().getAuthentication();
    engine.recoverOutstanding();
    assertEquals("FAILED", row.getState());
    assertSame(caller, SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(providers, controller);
  }
}
