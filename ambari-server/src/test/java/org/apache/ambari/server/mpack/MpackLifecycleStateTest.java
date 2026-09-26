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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class MpackLifecycleStateTest {
  private static final String OPERATION = "12345678-1234-1234-1234-123456789abc";
  private static final String PLAN = "23456789-1234-1234-1234-123456789abc";
  private static final String DIGEST = "a".repeat(64);

  private MpackLifecycleState.Operation operation(Map<String, MpackLifecycleState.HookReceipt> hooks,
      List<MpackLifecycleState.HookReceipt> history, MpackLifecycleState.Phase phase) {
    return new MpackLifecycleState.Operation(1, OPERATION, PLAN, DIGEST, 1, "admin", phase,
        100, 100, 1, DIGEST, hooks, history, null, Map.of(), null);
  }

  private MpackLifecycleState.HookReceipt receipt(String owner, int attempt,
      MpackLifecycleState.HookState state, MpackLifecycleState.EffectState effect) {
    return new MpackLifecycleState.HookReceipt(1, owner, DIGEST, DIGEST, "after-install",
        attempt, state, effect, Map.of("resource_identity", "reference"));
  }

  @Test
  public void scopePreservesPersistedOrderForPlanDigestAcrossRestarts() {
    for (List<String> order : List.of(List.of("kyuubi-env", "kyuubi-defaults"),
        List.of("kyuubi-defaults", "kyuubi-env"))) {
      String json = "{\"stack_name\":\"BIGTOP\",\"stack_version\":\"3.3.0\","
          + "\"service_name\":\"KYUUBI\",\"config_types\":" + MpackJson.tree(order) + "}";
      MpackScope decoded = MpackJson.decode(json, MpackScope.class);
      assertEquals(order, List.copyOf(decoded.configTypes()));
      assertEquals(MpackJson.digest(MpackJson.read(json)), MpackJson.digest(MpackJson.tree(decoded)));
      assertThrows(UnsupportedOperationException.class, () -> decoded.configTypes().add("foreign"));
      LinkedHashSet<String> source = new LinkedHashSet<>(order);
      MpackScope scope = new MpackScope("BIGTOP", "3.3.0", "KYUUBI", source);
      source.clear();
      assertEquals(order, List.copyOf(scope.configTypes()));
    }
  }

  @Test
  public void roundTripsVersionedRecordsWithoutTypeCoercion() {
    MpackLifecycleState.Operation value = operation(Map.of(), List.of(), MpackLifecycleState.Phase.SUCCEEDED);
    assertEquals(value, MpackJson.decode(MpackJson.canonical(MpackJson.tree(value)), MpackLifecycleState.Operation.class));
    String text = MpackJson.canonical(MpackJson.tree(value)).replace("\"generation\":1", "\"generation\":\"1\"");
    assertThrows(MpackException.class, () -> MpackJson.decode(text, MpackLifecycleState.Operation.class));
    assertThrows(MpackException.class,
        () -> MpackJson.decode("{\"schema_version\":1}", MpackLifecycleState.Operation.class));
  }

  @Test
  public void rejectsForeignHooksAndUnknownEffectsAsSuccess() {
    MpackLifecycleState.HookReceipt foreign = receipt(UUID.randomUUID().toString(), 1,
        MpackLifecycleState.HookState.APPLIED, MpackLifecycleState.EffectState.APPLIED);
    assertThrows(MpackException.class, () -> operation(Map.of(DIGEST + "/after-install", foreign),
        List.of(), MpackLifecycleState.Phase.SUCCEEDED));
    assertThrows(MpackException.class, () -> receipt(OPERATION, 1,
        MpackLifecycleState.HookState.APPLIED, MpackLifecycleState.EffectState.UNKNOWN));
  }

  @Test
  public void retainsFailedAttemptAndRejectsDuplicateOrFutureHistory() {
    MpackLifecycleState.HookReceipt failed = receipt(OPERATION, 1,
        MpackLifecycleState.HookState.FAILED, MpackLifecycleState.EffectState.NOT_APPLIED);
    MpackLifecycleState.HookReceipt applied = receipt(OPERATION, 2,
        MpackLifecycleState.HookState.APPLIED, MpackLifecycleState.EffectState.APPLIED);
    MpackLifecycleState.Operation value = operation(Map.of(DIGEST + "/after-install", applied),
        List.of(failed), MpackLifecycleState.Phase.SUCCEEDED);
    assertEquals(1, value.hookHistory().size());
    assertEquals(2, value.hooks().values().iterator().next().attempt());
    assertThrows(MpackException.class, () -> operation(Map.of(DIGEST + "/after-install", failed),
        List.of(applied), MpackLifecycleState.Phase.RECOVERY_REQUIRED));
    assertThrows(MpackException.class, () -> operation(Map.of(), List.of(failed, failed),
        MpackLifecycleState.Phase.RECOVERY_REQUIRED));
  }

  @Test
  public void rejectsIncompleteActionInputs() {
    assertThrows(MpackException.class, () -> new MpackLifecycleState.Mutation(1,
        MpackLifecycleState.Action.UPDATE, List.of(DIGEST), List.of(), List.of(), true, false));
    assertThrows(MpackException.class, () -> new MpackLifecycleState.Mutation(1,
        MpackLifecycleState.Action.BIND, List.of(), List.of("nginx/1.0"), List.of(), true, false));
    assertThrows(MpackException.class, () -> new MpackLifecycleState.Mutation(1,
        MpackLifecycleState.Action.UNINSTALL, List.of(DIGEST), List.of("nginx/1.0"), List.of(), true, false));
  }

  @Test
  public void removingAnInactiveImportDoesNotExecuteUninstallHooks() {
    var manifest = MpackManifestTest.manifest();
    manifest.putArray("hooks").addObject().put("name", "before-uninstall").put("type", "shell")
        .put("script", "hooks/remove.sh").put("timeout_seconds", 10).put("idempotent", false);
    var release = new MpackLifecycleState.Release(1, "nginx/1.0.0.0", DIGEST, manifest.toString(), List.of(), Map.of(), true);
    var mutation = new MpackLifecycleState.Mutation(1, MpackLifecycleState.Action.UNINSTALL,
        List.of(), List.of(release.id()), List.of(), true, false);
    var inactive = new MpackLifecycleState.Plan(1, PLAN, DIGEST, 0, 100, mutation, DIGEST, DIGEST,
        List.of(), List.of(), List.of(), false, false);
    assertTrue(MpackLifecycleState.requiredHooks(inactive, release).isEmpty());
    var active = new MpackLifecycleState.Plan(1, PLAN, DIGEST, 0, 100, mutation, DIGEST, DIGEST,
        List.of(), List.of(), List.of(), false, false, List.of(), List.of(), List.of(release.id()), null);
    assertEquals(java.util.Set.of(DIGEST + "/before-uninstall"), MpackLifecycleState.requiredHooks(active, release));
  }

  @Test
  public void publicationBarrierRequiresItsOwnerAndDoesNotAffectQueries() {
    MpackRuntime runtime = new MpackRuntime();
    runtime.writeLock().lock();
    try {
      runtime.reserve(OPERATION, List.of(new MpackScope("BASE", "1.0", "DATABASE", java.util.Set.of())));
      assertThrows(MpackException.class, () -> runtime.release(PLAN));
    } finally {
      runtime.writeLock().unlock();
    }
    runtime.requireExecutionReady();
    assertThrows(MpackException.class, () -> runtime.requireServiceReady(
        new org.apache.ambari.server.state.StackId("BASE", "1.0"), "DATABASE"));
    runtime.readLock().lock();
    try {
      assertEquals(OPERATION, runtime.pendingOperation());
    } finally {
      runtime.readLock().unlock();
    }
    runtime.writeLock().lock();
    try {
      runtime.release(OPERATION);
    } finally {
      runtime.writeLock().unlock();
    }
    runtime.requireExecutionReady();
    assertTrue(operation(Map.of(), List.of(), MpackLifecycleState.Phase.CANCELLED).terminal());
  }
}
