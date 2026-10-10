/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.mpack.MpackLifecycleState.Control;
import org.apache.ambari.server.mpack.MpackLifecycleState.Operation;
import org.apache.ambari.server.mpack.MpackLifecycleState.Phase;
import org.apache.ambari.server.mpack.MpackLifecycleState.Plan;
import org.apache.ambari.server.orm.dao.MpackRecordDAO;
import org.apache.ambari.server.orm.entities.MpackRecordEntity.Kind;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Provider;

public class MpackLifecycleServiceTest {
  private static final String PREVIOUS = "a".repeat(64);
  private static final String CANDIDATE = "b".repeat(64);
  private MpackLifecycleService service;
  private MpackCatalog catalog;
  private MpackSnapshots snapshots;
  private MpackDefinitionLoader loader;
  private MpackPlanner planner;
  private MpackActivation activation;
  private ActionDBAccessor actions;
  private MpackTaskUsage taskUsage;
  private MpackRuntime runtime;
  private Plan plan;

  @Before
  public void setUp() throws Exception {
    org.springframework.security.core.Authentication authentication = TestAuthenticationFactory.createAdministrator();
    authentication.getAuthorities().forEach(authority ->
        ((org.apache.ambari.server.security.authorization.AmbariGrantedAuthority) authority)
            .getPrivilegeEntity().getPermission().setPermissionName(
                org.apache.ambari.server.orm.entities.PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    service = new MpackLifecycleService();
    catalog = mock(MpackCatalog.class);
    snapshots = mock(MpackSnapshots.class);
    loader = mock(MpackDefinitionLoader.class);
    planner = mock(MpackPlanner.class);
    activation = mock(MpackActivation.class);
    actions = mock(ActionDBAccessor.class);
    taskUsage = mock(MpackTaskUsage.class);
    when(taskUsage.blockers(anyList())).thenReturn(List.of());
    runtime = new MpackRuntime();
    inject("catalog", catalog);
    inject("snapshots", snapshots);
    inject("loader", loader);
    inject("planner", planner);
    inject("activation", activation);
    inject("actions", (Provider<ActionDBAccessor>) () -> actions);
    inject("runtime", runtime);
    inject("taskUsage", taskUsage);
    lastTransition[0] = null;
    MpackLifecycleState.Mutation mutation = new MpackLifecycleState.Mutation(1,
        MpackLifecycleState.Action.INSTALL, List.of("c".repeat(64)), List.of(), List.of(), true, false);
    plan = new Plan(1, UUID.randomUUID().toString(), "d".repeat(64), 0,
        System.currentTimeMillis() + 60000, mutation, PREVIOUS, CANDIDATE,
        List.of(), List.of(), List.of(), false, false);
    when(catalog.plan(plan.id())).thenReturn(new MpackCatalog.Versioned<>("plan/" + plan.id(), 0, plan));
    when(catalog.control()).thenReturn(new MpackCatalog.Versioned<>(MpackCatalog.CONTROL, 0,
        new Control(1, PREVIOUS, PREVIOUS, null, 0, List.of())));
    when(snapshots.load(PREVIOUS)).thenReturn(snapshot(PREVIOUS));
    when(snapshots.load(CANDIDATE)).thenReturn(snapshot(CANDIDATE));
    when(planner.impact(eq(snapshot(PREVIOUS)), eq(snapshot(CANDIDATE)), eq(null)))
        .thenReturn(new MpackPlanner.Impact(List.of(), false, false));
    when(planner.impact(eq(snapshot(PREVIOUS)), eq(snapshot(CANDIDATE)), eq(null), eq(null)))
        .thenReturn(new MpackPlanner.Impact(List.of(), false, false));
  }

  @After
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void clusterAdministratorCannotSubmitGlobalDefinitionChanges() {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator());
    assertEquals(MpackException.Code.FORBIDDEN, assertThrows(MpackException.class,
        () -> service.accept(plan.id(), "submission-1")).getCode());
    verify(catalog, never()).apply(anyList());
    assertNull(runtime.pendingOperation());
  }

  @Test
  public void acceptancePersistsOperationAndIdempotencyBeforeGating() throws Exception {
    doAnswer(invocation -> {
      assertNull(runtime.pendingOperation());
      List<MpackRecordDAO.Change> changes = invocation.getArgument(0);
      assertEquals(3, changes.size());
      assertEquals(List.of(Kind.CONTROL, Kind.OPERATION, Kind.IDEMPOTENCY),
          changes.stream().map(MpackRecordDAO.Change::kind).toList());
      return null;
    }).when(catalog).apply(anyList());

    Operation accepted = service.accept(plan.id(), "submission-1");
    assertEquals(Phase.ACCEPTED, accepted.phase());
    assertEquals(accepted.id(), runtime.pendingOperation());
    assertEquals(plan.digest(), accepted.planDigest());
    assertEquals(1, accepted.generation());
  }

  @Test
  public void failedAcceptanceTransactionDoesNotOwnRuntimeBarrier() {
    doThrow(new IllegalStateException("Simulated transaction failure")).when(catalog).apply(anyList());
    assertThrows(IllegalStateException.class, () -> service.accept(plan.id(), "submission-1"));
    assertNull(runtime.pendingOperation());
    assertNull(runtime.snapshot());
  }

  @Test
  public void repeatedIdempotencyReturnsOriginalOperationWithoutNewWrites() throws Exception {
    Operation original = operation(Phase.ACCEPTED);
    String request = MpackJson.digest(MpackJson.tree(Map.of("plan_id", plan.id(), "plan_digest", plan.digest())));
    when(catalog.idempotency(1, "submission-1")).thenReturn(new MpackCatalog.Versioned<>("idempotency/test", 0,
        new MpackLifecycleState.Idempotency(1, 1, request, original.id())));
    when(catalog.operation(original.id())).thenReturn(versioned(original));
    assertEquals(original, service.accept(plan.id(), "submission-1"));
    verify(catalog, never()).apply(anyList());
  }

  @Test
  public void stalePlanIsNotAccepted() {
    when(catalog.control()).thenReturn(new MpackCatalog.Versioned<>(MpackCatalog.CONTROL, 1,
        new Control(1, PREVIOUS, PREVIOUS, null, 0, List.of())));
    MpackException error = assertThrows(MpackException.class, () -> service.accept(plan.id(), "submission-1"));
    assertEquals(MpackException.Code.STALE_PLAN, error.getCode());
    verify(catalog, never()).apply(anyList());
    assertNull(runtime.pendingOperation());
  }

  @Test
  public void durableWorkerWaitsForExistingTasksWithoutPublication() throws Exception {
    Operation accepted = operation(Phase.ACCEPTED);
    pending(accepted);
    when(taskUsage.blockers(anyList())).thenReturn(List.of(Map.of("task_id", 31L, "status", "HOLDING")));
    service.advance();
    assertEquals(Phase.WAITING_MAINTENANCE, lastTransition[0].phase());
    assertEquals(accepted.id(), runtime.pendingOperation());
    verify(activation, never()).announce();
    verify(snapshots, never()).load(anyString());
  }

  @Test
  public void cancellationNotificationFailureRetainsRecoverableOwnership() throws Exception {
    Operation accepted = operation(Phase.ACCEPTED);
    pending(accepted);
    runtime.writeLock().lock();
    try { runtime.restore(snapshot(PREVIOUS), accepted.id()); } finally { runtime.writeLock().unlock(); }
    doThrow(new org.apache.ambari.server.AmbariException("notification unavailable")).when(activation).announce();
    assertThrows(MpackException.class, () -> service.cancel(accepted.id()));
    assertEquals(Phase.CANCELLING, lastTransition[0].phase());
    assertEquals(accepted.id(), runtime.pendingOperation());
    org.mockito.Mockito.doNothing().when(activation).announce();
    service.advance();
    assertEquals(Phase.CANCELLED, lastTransition[0].phase());
    assertNull(runtime.pendingOperation());
  }

  @Test
  public void terminalDatabaseResultRepairsAnOrphanedRuntimeReservation() throws Exception {
    Operation finished = operation(Phase.ACCEPTED).transition(Phase.SUCCEEDED, CANDIDATE, null, Map.of());
    when(catalog.operation(finished.id())).thenReturn(versioned(finished));
    when(catalog.control()).thenReturn(new MpackCatalog.Versioned<>(MpackCatalog.CONTROL, 3,
        new Control(1, PREVIOUS, CANDIDATE, null, finished.generation(), List.of())));
    var meta = mock(org.apache.ambari.server.api.services.AmbariMetaInfo.class);
    var manager = mock(org.apache.ambari.server.stack.StackManager.class);
    when(manager.getDefinitionSnapshotId()).thenReturn(CANDIDATE); when(meta.getStackManager()).thenReturn(manager);
    inject("metadata", (Provider<org.apache.ambari.server.api.services.AmbariMetaInfo>) () -> meta);
    runtime.writeLock().lock();
    try { runtime.restore(snapshot(CANDIDATE), finished.id()); } finally { runtime.writeLock().unlock(); }
    service.advance();
    assertNull(runtime.pendingOperation());
    verify(snapshots).verify(snapshot(CANDIDATE));
    verify(activation, never()).activate(any(), any(), any());
  }

  @Test
  public void reconciledFailureWithoutEffectsAllowsExplicitRetry() throws Exception {
    com.fasterxml.jackson.databind.node.ObjectNode manifest = MpackManifestTest.manifest();
    manifest.putArray("hooks").addObject().put("name", "before-install").put("type", "python")
        .put("script", "hooks/install.py").put("timeout_seconds", 10).put("idempotent", true)
        .put("scope", "DEFINITIONS");
    MpackLifecycleState.Release release = new MpackLifecycleState.Release(1, "nginx/1.0.0.0",
        "c".repeat(64), manifest.toString(), List.of(), Map.of(), true);
    plan = new Plan(1, plan.id(), plan.digest(), plan.catalogRevision(), plan.expiresAt(), plan.mutation(),
        plan.previousSnapshot(), plan.candidateSnapshot(), List.of(release), List.of(release.id()), List.of(), false, false);
    when(catalog.plan(plan.id())).thenReturn(new MpackCatalog.Versioned<>("plan/" + plan.id(), 0, plan));
    Operation base = operation(Phase.RECOVERY_REQUIRED);
    String key = release.archiveDigest() + "/before-install";
    MpackLifecycleState.HookReceipt running = new MpackLifecycleState.HookReceipt(1, base.id(), plan.digest(),
        release.archiveDigest(), "before-install", 1, MpackLifecycleState.HookState.RUNNING,
        MpackLifecycleState.EffectState.UNKNOWN, Map.of());
    Operation interrupted = new Operation(1, base.id(), plan.id(), plan.digest(), 1, "admin", base.phase(),
        base.createdAt(), base.updatedAt(), 1, null, Map.of(key, running), List.of(),
        MpackException.Code.RECOVERY_REQUIRED, Map.of(), null);
    pending(interrupted);
    MpackHookRunner runner = mock(MpackHookRunner.class);
    inject("hookRunner", runner);
    MpackLifecycleState.HookReceipt failed = new MpackLifecycleState.HookReceipt(1, base.id(), plan.digest(),
        release.archiveDigest(), "before-install", 1, MpackLifecycleState.HookState.FAILED,
        MpackLifecycleState.EffectState.NOT_APPLIED, Map.of("resource_exists", false));
    when(runner.reconcile(any(), any(), any())).thenReturn(failed);
    Operation observed = service.recover(base.id());
    assertEquals(Phase.RECOVERY_REQUIRED, observed.phase());
    assertEquals(failed, observed.hooks().get(key));
    Operation retry = service.retryFailedHooks(base.id());
    assertEquals(Phase.PREPARING, retry.phase());
    assertEquals(List.of(failed), retry.hookHistory());
    assertTrue(retry.hooks().isEmpty());
  }

  @Test
  public void importCompletesWithoutHooksOrGlobalTaskDrain() throws Exception {
    com.fasterxml.jackson.databind.node.ObjectNode manifest = MpackManifestTest.manifest();
    manifest.putArray("hooks").addObject().put("name", "before-install").put("type", "shell")
        .put("script", "hooks/install.sh").put("timeout_seconds", 10).put("idempotent", false);
    var release = new MpackLifecycleState.Release(1, "nginx/1.0.0.0", "c".repeat(64),
        manifest.toString(), List.of(), Map.of(), true);
    var mutation = new MpackLifecycleState.Mutation(1, MpackLifecycleState.Action.IMPORT,
        List.of(release.archiveDigest()), List.of(), List.of(), false, false);
    plan = new Plan(1, plan.id(), plan.digest(), 0, plan.expiresAt(), mutation, PREVIOUS, PREVIOUS,
        List.of(release), List.of(), List.of(), false, false);
    when(catalog.plan(plan.id())).thenReturn(new MpackCatalog.Versioned<>("plan/" + plan.id(), 0, plan));
    Operation accepted = operation(Phase.ACCEPTED); pending(accepted);
    when(planner.impact(eq(snapshot(PREVIOUS)), eq(snapshot(PREVIOUS)), eq(null)))
        .thenReturn(new MpackPlanner.Impact(List.of(), false, false));
    MpackHookRunner runner = mock(MpackHookRunner.class); inject("hookRunner", runner);
    doAnswer(call -> {
      when(catalog.control()).thenReturn(new MpackCatalog.Versioned<>(MpackCatalog.CONTROL, 2,
          new Control(1, PREVIOUS, PREVIOUS, accepted.id(), 1, List.of())));
      return null;
    }).when(activation).commitCatalogOnly(any(), any());
    service.advance();
    assertEquals(Phase.SUCCEEDED, lastTransition[0].phase());
    assertNull(runtime.pendingOperation());
    assertNull(runtime.snapshot());
    verify(activation, never()).activate(any(), any(), any());
    verify(runner, never()).run(any(), any(), any());
    verify(actions, never()).getCommandsInProgressCount();
  }

  @Test
  public void invalidSnapshotPreservesExplicitRecoveryAndBarrier() throws Exception {
    Operation accepted = operation(Phase.ACCEPTED);
    pending(accepted);
    when(snapshots.load(CANDIDATE)).thenThrow(new MpackException(MpackException.Code.DIGEST_MISMATCH,
        "Simulated altered snapshot"));
    service.advance();
    assertEquals(Phase.RECOVERY_REQUIRED, lastTransition[0].phase());
    assertEquals(MpackException.Code.DIGEST_MISMATCH, lastTransition[0].errorCode());
    assertEquals(accepted.id(), runtime.pendingOperation());
    assertNull(runtime.snapshot());
    verify(activation, never()).announce();
  }

  private final Operation[] lastTransition = new Operation[1];

  private void pending(Operation operation) {
    when(catalog.control()).thenReturn(new MpackCatalog.Versioned<>(MpackCatalog.CONTROL, 1,
        new Control(1, PREVIOUS, PREVIOUS, operation.id(), 1, List.of())));
    when(catalog.operation(operation.id())).thenAnswer(invocation ->
        versioned(lastTransition[0] == null ? operation : lastTransition[0]));
    doAnswer(invocation -> {
      List<MpackRecordDAO.Change> changes = invocation.getArgument(0);
      changes.stream().filter(change -> change.kind() == Kind.OPERATION).forEach(change ->
          lastTransition[0] = MpackJson.decode(change.payload(), Operation.class));
      return null;
    }).when(catalog).apply(anyList());
  }

  private Operation operation(Phase phase) {
    long now = System.currentTimeMillis();
    return new Operation(1, UUID.randomUUID().toString(), plan.id(), plan.digest(), 1, "admin", phase,
        now, now, 1, null, Map.of(), List.of(), null, Map.of(), null);
  }

  private MpackCatalog.Versioned<Operation> versioned(Operation operation) {
    return new MpackCatalog.Versioned<>("operation/" + operation.id(), 0, operation);
  }

  private MpackSnapshots.Snapshot snapshot(String id) {
    return new MpackSnapshots.Snapshot(1, id, null, List.of(), List.of(), Map.of(), Map.of());
  }

  private void inject(String name, Object value) throws Exception {
    Field field = MpackLifecycleService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(service, value);
  }
}
