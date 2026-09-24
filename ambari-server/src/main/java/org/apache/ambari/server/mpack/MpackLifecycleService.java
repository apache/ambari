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

import static org.apache.ambari.server.mpack.MpackLifecycleState.requiredHooks;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.mpack.MpackLifecycleState.Control;
import org.apache.ambari.server.mpack.MpackLifecycleState.HookReceipt;
import org.apache.ambari.server.mpack.MpackLifecycleState.HookState;
import org.apache.ambari.server.mpack.MpackLifecycleState.Operation;
import org.apache.ambari.server.mpack.MpackLifecycleState.Phase;
import org.apache.ambari.server.mpack.MpackLifecycleState.Plan;
import org.apache.ambari.server.mpack.MpackLifecycleState.Release;
import org.apache.ambari.server.orm.dao.MpackRecordDAO;
import org.apache.ambari.server.orm.entities.MpackRecordEntity.Kind;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.stack.StackManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** Owns acceptance, planning, publication and recovery of management pack mutations. */
@Singleton
public class MpackLifecycleService {
  @Inject private MpackCatalog catalog;
  @Inject private MpackArchiveStore archives;
  @Inject private MpackResources resources;
  @Inject private MpackBundles bundles;
  @Inject private MpackSnapshots snapshots;
  @Inject private MpackDefinitionLoader loader;
  @Inject private MpackPlanner planner;
  @Inject private MpackRuntime runtime;
  @Inject private MpackActivation activation;
  @Inject private MpackHookRunner hookRunner;
  @Inject private MpackTaskUsage taskUsage;
  @Inject private MpackServiceCatalog serviceCatalog;
  @Inject private Provider<ActionDBAccessor> actions;
  @Inject private Provider<org.apache.ambari.server.api.services.AmbariMetaInfo> metadata;
  @Inject private Provider<org.apache.ambari.server.state.Clusters> clusters;

  private final Set<String> resumedAfterRestart = new HashSet<>();
  private final java.util.concurrent.locks.ReentrantLock progress = new java.util.concurrent.locks.ReentrantLock();

  public Map<String, Object> upload(InputStream input, String digest) throws AuthorizationException {
    authorize();
    MpackArchiveStore.StoredArchive stored = archives.accept(input, digest);
    return bundles.inspect(stored);
  }

  public List<Map<String, String>> targets() throws AuthorizationException {
    authorize();
    Lock read = runtime.readLock();
    read.lock();
    try {
      runtime.requireExecutionReady();
      return metadata.get().getStackManager().getStacks().stream().filter(stack -> stack.isActive())
          .map(stack -> Map.of("stack_name", stack.getName(), "stack_version", stack.getVersion())).toList();
    } finally { read.unlock(); }
  }

  public Plan plan(MpackLifecycleState.Mutation mutation) throws AuthorizationException {
    return plan(mutation, null);
  }

  public Plan planServices(MpackServiceCatalog.Selection selection) throws AuthorizationException {
    authorize();
    MpackServiceCatalog.PreparedSelection prepared = serviceCatalog.prepare(selection);
    return plan(prepared.mutation(), prepared.deployment());
  }

  private Plan plan(MpackLifecycleState.Mutation mutation, MpackLifecycleState.Deployment deployment) throws AuthorizationException {
    authorize();
    Lock read = runtime.readLock();
    read.lock();
    try {
      runtime.requireExecutionReady();
      Plan plan = planner.plan(mutation, deployment);
      catalog.apply(List.of(MpackCatalog.change("plan/" + plan.id(), Kind.PLAN, MpackRecordDAO.ABSENT, plan)));
      return plan;
    } finally {
      read.unlock();
    }
  }

  public Operation accept(String planId, String idempotencyKey) throws AuthorizationException {
    authorize();
    int ownerId = AuthorizationHelper.getAuthenticatedId();
    String ownerName = AuthorizationHelper.getAuthenticatedName();
    Plan plan = catalog.plan(planId).value();
    String requestDigest = MpackJson.digest(MpackJson.tree(Map.of("plan_id", plan.id(), "plan_digest", plan.digest())));
    MpackCatalog.Versioned<MpackLifecycleState.Idempotency> acceptedBefore = catalog.idempotency(ownerId, idempotencyKey);
    if (acceptedBefore != null) {
      if (!requestDigest.equals(acceptedBefore.value().requestDigest())) {
        throw new MpackException(MpackException.Code.IDEMPOTENCY_CONFLICT, "This submission identifies different inputs");
      }
      return catalog.operation(acceptedBefore.value().operationId()).value();
    }
    MpackSnapshots.Snapshot candidate = snapshots.load(plan.candidateSnapshot());
    MpackSnapshots.Snapshot original = snapshots.load(plan.previousSnapshot());
    StackManager candidateDefinitions = loader.resolve(candidate);
    StackManager originalDefinitions = original.id().equals(candidate.id()) ? candidateDefinitions : loader.resolve(original);
    MpackSnapshots.Snapshot capturedBuiltin = catalog.control() == null ? snapshots.captureBuiltins(loader.existingBindings()) : null;
    Lock write = runtime.writeLock();
    write.lock();
    try {
      MpackCatalog.Versioned<MpackLifecycleState.Idempotency> previous = catalog.idempotency(ownerId, idempotencyKey);
      if (previous != null) {
        if (!requestDigest.equals(previous.value().requestDigest()) || ownerId != previous.value().ownerId()) {
          throw new MpackException(MpackException.Code.IDEMPOTENCY_CONFLICT,
              "The idempotency key already identifies different lifecycle inputs");
        }
        return catalog.operation(previous.value().operationId()).value();
      }
      MpackCatalog.Versioned<Control> current = catalog.control();
      if (current == null) {
        MpackSnapshots.Snapshot builtin = capturedBuiltin;
        if (plan.catalogRevision() != MpackRecordDAO.ABSENT || !builtin.id().equals(plan.previousSnapshot())) {
          throw new MpackException(MpackException.Code.STALE_PLAN, "Distribution definitions changed after preview");
        }
        current = new MpackCatalog.Versioned<>(MpackCatalog.CONTROL, MpackRecordDAO.ABSENT,
            new Control(1, builtin.id(), builtin.id(), null, 0, List.of()));
      }
      if (current.value().pendingOperation() != null) {
        throw new MpackException(MpackException.Code.OPERATION_CONFLICT, "Another lifecycle operation is unfinished");
      }
      if (runtime.pendingOperation() != null) {
        throw new MpackException(MpackException.Code.OPERATION_CONFLICT, "The previous publication is being reconciled");
      }
      if (plan.expiresAt() < System.currentTimeMillis() || plan.catalogRevision() != current.revision()
          || !plan.previousSnapshot().equals(current.value().effectiveSnapshot())) {
        throw new MpackException(MpackException.Code.STALE_PLAN, "Create a new preview against the current catalog");
      }
      MpackPlanner.Impact impact = planner.impact(original, candidate, originalDefinitions, candidateDefinitions);
      if (!new HashSet<>(plan.affectedClusters()).containsAll(impact.clusters())
          || (impact.inUseDefinitions() && !plan.mutation().maintenance())) {
        throw new MpackException(MpackException.Code.STALE_PLAN, "The operation's impact changed after preview");
      }
      String id = UUID.randomUUID().toString();
      long now = System.currentTimeMillis();
      long generation = Math.addExact(current.value().generation(), 1);
      Operation operation = new Operation(1, id, plan.id(), plan.digest(), ownerId, ownerName, Phase.ACCEPTED,
          now, now, generation, null, Map.of(), List.of(), null, Map.of(), null);
      Control accepted = new Control(1, current.value().builtinSnapshot(), current.value().effectiveSnapshot(),
          id, generation, current.value().activeReleases());
      catalog.apply(List.of(
          MpackCatalog.change(MpackCatalog.CONTROL, Kind.CONTROL, current.revision(), accepted),
          MpackCatalog.change("operation/" + id, Kind.OPERATION, MpackRecordDAO.ABSENT, operation),
          MpackCatalog.change(MpackCatalog.idempotencyKey(ownerId, idempotencyKey), Kind.IDEMPOTENCY,
              MpackRecordDAO.ABSENT, new MpackLifecycleState.Idempotency(1, ownerId, requestDigest, id))));
      runtime.reserve(id, plan.affectedDefinitions());
      return operation;
    } finally {
      write.unlock();
    }
  }

  public List<Release> releases() throws AuthorizationException {
    authorize();
    return catalog.releases().stream().map(MpackCatalog.Versioned::value).toList();
  }

  public List<Operation> operations() throws AuthorizationException {
    authorize();
    return catalog.operations().stream().map(MpackCatalog.Versioned::value).toList();
  }

  public Operation operation(String id) throws AuthorizationException {
    authorize();
    return catalog.operation(id).value();
  }

  public record DeploymentHandoff(int schemaVersion, String operationId, String planId,
      String effectiveSnapshot, MpackLifecycleState.Deployment deployment, String clusterName) { }

  public DeploymentHandoff deployment(String id) throws AuthorizationException {
    authorize();
    Operation operation = catalog.operation(id).value();
    Plan plan = catalog.plan(operation.planId()).value();
    if (operation.phase() != Phase.SUCCEEDED || plan.deployment() == null) {
      throw new MpackException(MpackException.Code.UNSUPPORTED_OPERATION, "This operation has no completed service selection");
    }
    MpackSnapshots.Snapshot original = snapshots.load(plan.candidateSnapshot());
    StackManager originalDefinitions = loader.resolve(original);
    Lock read = runtime.readLock();
    read.lock();
    try {
      MpackLifecycleState.Deployment selected = plan.deployment();
      org.apache.ambari.server.state.StackId stack = new org.apache.ambari.server.state.StackId(selected.stackName(), selected.stackVersion());
      for (String service : selected.serviceNames()) {
        runtime.requireServiceReady(stack, service);
        if (runtime.snapshot() == null || !planner.executionIdentity(original, originalDefinitions, stack, service).equals(
            planner.executionIdentity(runtime.snapshot(), metadata.get().getStackManager(), stack, service))) {
          throw new MpackException(MpackException.Code.STALE_PLAN, "The selected definitions changed; select services again");
        }
      }
      String name = null;
      if (selected.clusterId() != null) {
        org.apache.ambari.server.state.Cluster cluster = clusters.get().getClusterById(selected.clusterId());
        if (!cluster.getDesiredStackVersion().equals(stack)) {
          throw new MpackException(MpackException.Code.INVALID_TARGET, "The destination context changed");
        }
        name = cluster.getClusterName();
      }
      return new DeploymentHandoff(1, id, plan.id(), operation.effectiveSnapshot(), selected, name);
    } catch (org.apache.ambari.server.AmbariException e) {
      throw new MpackException(MpackException.Code.INVALID_TARGET, "The destination cluster is unavailable");
    } finally { read.unlock(); }
  }

  public List<Map<String, Object>> members(String id) throws AuthorizationException {
    authorize();
    Operation operation = catalog.operation(id).value();
    Plan plan = catalog.plan(operation.planId()).value();
    List<Map<String, Object>> result = new ArrayList<>();
    for (Release release : operationReleases(plan)) {
      Set<String> required = requiredHooks(plan, release);
      Map<String, HookReceipt> observations = new HashMap<>();
      operation.hooks().forEach((key, receipt) -> {
        if (receipt.archiveDigest().equals(release.archiveDigest())) {
          observations.put(key, receipt);
        }
      });
      String phase;
      if (observations.values().stream().anyMatch(receipt -> receipt.state() == HookState.UNKNOWN)) {
        phase = "RECOVERY_REQUIRED";
      } else if (observations.values().stream().anyMatch(receipt -> receipt.state() == HookState.FAILED)) {
        phase = "FAILED";
      } else if (plan.candidateSnapshot().equals(operation.effectiveSnapshot())
          && observations.keySet().containsAll(required)
          && observations.values().stream().allMatch(receipt -> receipt.state() == HookState.APPLIED)) {
        phase = "SUCCEEDED";
      } else if (operation.phase() == Phase.RECOVERY_REQUIRED) {
        phase = "BLOCKED";
      } else {
        phase = operation.phase().name();
      }
      String memberId = MpackJson.digest(MpackJson.tree(Map.of("parent_id", id,
          "release_id", release.id(), "archive_digest", release.archiveDigest())));
      result.add(Map.of("schema_version", 1, "id", memberId, "parent_id", id,
          "release_id", release.id(), "archive_digest", release.archiveDigest(), "phase", phase,
          "hook_receipts", observations));
    }
    return List.copyOf(result);
  }

  public Operation retryFailedHooks(String id) throws AuthorizationException {
    authorize();
    Lock write = runtime.writeLock();
    write.lock();
    try {
      MpackCatalog.Versioned<Operation> current = catalog.operation(id);
      requireOwner(current.value());
      if (current.value().phase() != Phase.RECOVERY_REQUIRED) {
        throw new MpackException(MpackException.Code.UNSUPPORTED_OPERATION, "Only failed work can be retried");
      }
      Plan plan = catalog.plan(current.value().planId()).value();
      Map<String, HookReceipt> remaining = new HashMap<>(current.value().hooks());
      List<HookReceipt> history = new ArrayList<>(current.value().hookHistory());
      boolean retried = false;
      for (Release release : hookReleases(plan)) {
        for (MpackManifest.Hook hook : release.manifest().hooks()) {
          String key = MpackHookRunner.key(release, hook);
          HookReceipt receipt = remaining.get(key);
          if (receipt == null || receipt.state() == HookState.APPLIED) {
            continue;
          }
          if (!hook.idempotent() || receipt.state() != HookState.FAILED
              || receipt.effectState() != MpackLifecycleState.EffectState.NOT_APPLIED) {
            throw new MpackException(MpackException.Code.RECOVERY_REQUIRED,
                "Resolve uncertain effects before retrying the failed item", Map.of("hook", key));
          }
          history.add(receipt);
          remaining.remove(key);
          retried = true;
        }
      }
      if (!retried) {
        throw new MpackException(MpackException.Code.UNSUPPORTED_OPERATION, "No failed hook is eligible for retry");
      }
      Operation value = current.value();
      Operation retry = new Operation(1, id, value.planId(), value.planDigest(), value.ownerId(), value.ownerName(),
          Phase.PREPARING, value.createdAt(), System.currentTimeMillis(), value.generation(),
          value.effectiveSnapshot(), remaining, history, null, Map.of(), value.parentId());
      update(current, retry);
      return retry;
    } finally {
      write.unlock();
    }
  }

  public Operation cancel(String id) throws AuthorizationException {
    authorize();
    return cancelAccepted(id, AuthorizationHelper.getAuthenticatedId());
  }

  private Operation cancelAccepted(String id, int requestedBy) {
    Operation known = catalog.operation(id).value();
    if (known.phase() == Phase.CANCELLED) return known;
    requireOwner(known);
    MpackSnapshots.Snapshot effective = snapshots.load(catalog.control().value().effectiveSnapshot());
    StackManager retained = loader.resolve(effective);
    Lock write = runtime.writeLock();
    write.lock();
    try {
      MpackCatalog.Versioned<Operation> current = catalog.operation(id);
      if (current.value().phase() == Phase.CANCELLED) return current.value();
      requireOwner(current.value());
      if (current.value().hooks().values().stream().anyMatch(receipt ->
          receipt.effectState() != MpackLifecycleState.EffectState.NOT_APPLIED)) {
        throw new MpackException(MpackException.Code.RECOVERY_REQUIRED,
            "An operation with applied or uncertain external effects cannot be cancelled");
      }
      if (current.value().phase() != Phase.CANCELLING) {
        update(current, current.value().transition(Phase.CANCELLING, current.value().effectiveSnapshot(), null,
            Map.of("cancelled_by", requestedBy)));
        current = catalog.operation(id);
      }
      MpackCatalog.Versioned<Control> control = catalog.control();
      if (!effective.id().equals(control.value().effectiveSnapshot())) {
        throw new MpackException(MpackException.Code.OPERATION_CONFLICT, "Publication advanced while cancellation was being prepared; retry cancellation");
      }
      runtime.gate(id);
      boolean unchangedBaseline = runtime.hasReadableView() && runtime.snapshot() == null
          && control.value().activeReleases().isEmpty() && effective.id().equals(control.value().builtinSnapshot());
      if (!unchangedBaseline) activation.publishRuntime(id, effective, retained);
      Operation cancelled = current.value().transition(Phase.CANCELLED, current.value().effectiveSnapshot(), null,
          Map.of("cancelled_by", current.value().errorDetails().get("cancelled_by"),
              "retained_snapshot", effective.id(),
              "publication_retained", effective.id().equals(current.value().effectiveSnapshot())));
      if (!unchangedBaseline) activation.announce();
      catalog.apply(List.of(MpackCatalog.change(current.key(), Kind.OPERATION, current.revision(), cancelled),
          MpackCatalog.change(MpackCatalog.CONTROL, Kind.CONTROL, control.revision(),
              new Control(1, control.value().builtinSnapshot(), effective.id(), null,
                  control.value().generation(), control.value().activeReleases()))));
      runtime.release(id);
      return cancelled;
    } catch (AmbariException e) {
      throw new MpackException(MpackException.Code.RECOVERY_REQUIRED, "Unable to verify the retained definition set");
    } finally {
      write.unlock();
    }
  }

  public Map<String, Object> bindings() throws AuthorizationException {
    authorize();
    MpackCatalog.Versioned<Control> current = catalog.control();
    if (current == null) {
      return Map.of("schema_version", 1, "items", loader.existingBindings(), "managed", false);
    }
    return Map.of("schema_version", 1, "items", snapshots.load(current.value().effectiveSnapshot()).bindings(),
        "managed", true, "revision", current.revision(), "effective_snapshot", current.value().effectiveSnapshot());
  }

  public Operation recover(String id) throws AuthorizationException {
    authorize();
    Lock write = runtime.writeLock();
    write.lock();
    try {
      MpackCatalog.Versioned<Operation> operation = catalog.operation(id);
      if (operation.value().phase() != Phase.RECOVERY_REQUIRED) {
        throw new MpackException(MpackException.Code.UNSUPPORTED_OPERATION,
            "Only an unresolved operation can request receipt reconciliation");
      }
      requireOwner(operation.value());
      Plan plan = catalog.plan(operation.value().planId()).value();
      Map<String, HookReceipt> observed = new HashMap<>(operation.value().hooks());
      boolean applied = true;
      for (Release release : hookReleases(plan)) {
        for (MpackManifest.Hook hook : release.manifest().hooks()) {
          String key = MpackHookRunner.key(release, hook);
          HookReceipt previous = observed.get(key);
          if (previous != null && previous.state() != HookState.APPLIED) {
            HookReceipt receipt;
            write.unlock();
            try { receipt = hookRunner.reconcile(operation.value(), release, hook); }
            finally { write.lock(); }
            requireOwner(catalog.operation(id).value());
            if (catalog.operation(id).revision() != operation.revision()) {
              throw new MpackException(MpackException.Code.OPERATION_CONFLICT, "Recovery observations changed concurrently; reconcile again");
            }
            observed.put(key, receipt);
            applied &= receipt.state() == HookState.APPLIED;
          }
        }
      }
      Operation resumed = withHooks(operation.value(), observed).transition(
          applied ? Phase.PREPARING : Phase.RECOVERY_REQUIRED,
          operation.value().effectiveSnapshot(), applied ? null : MpackException.Code.RECOVERY_REQUIRED,
          applied ? Map.of() : Map.of("reason", "HOOK_OBSERVATIONS_RECONCILED"));
      catalog.apply(List.of(MpackCatalog.change(operation.key(), Kind.OPERATION, operation.revision(), resumed)));
      return resumed;
    } finally {
      write.unlock();
    }
  }

  /** Called once by the server worker after a real process startup. */
  public void recoverOnStartup() {
    Lock write = runtime.writeLock();
    write.lock();
    try {
      MpackCatalog.Versioned<Control> control = catalog.control();
      if (control != null && control.value().pendingOperation() != null) {
        Operation operation = catalog.operation(control.value().pendingOperation()).value();
        if (operation.phase() == Phase.WAITING_RESTART) {
          resumedAfterRestart.add(operation.id());
        }
      }
    } finally {
      write.unlock();
    }
  }

  /** Advances durable accepted work independently of an HTTP client. */
  public void advance() {
    MpackCatalog.Versioned<Control> control = catalog.control();
    if (control == null || !progress.tryLock()) {
      return;
    }
    Lock write = runtime.writeLock();
    String id = control.value().pendingOperation();
    try {
      if (id == null) {
        reconcileCompleted(control);
        return;
      }
      Operation operation = catalog.operation(id).value();
      if (operation.phase() == Phase.CANCELLING) {
        cancelAccepted(id, operation.ownerId());
        return;
      }
      if (operation.terminal() || operation.phase() == Phase.RECOVERY_REQUIRED) {
        return;
      }
      Plan plan = catalog.plan(operation.planId()).value();
      if (!operation.planDigest().equals(plan.digest())) {
        throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Operation plan identity has changed");
      }
      write.lock();
      try {
        requireOwner(catalog.operation(id).value());
        runtime.reserve(id, plan.affectedDefinitions());
      } finally {
        write.unlock();
      }
      List<Map<String, Object>> blockers = taskUsage.blockers(plan.affectedDefinitions());
      if (!blockers.isEmpty()) {
        waiting(id, Phase.WAITING_MAINTENANCE, Map.of("reason", "AFFECTED_TASKS", "blockers", blockers));
        return;
      }
      MpackSnapshots.Snapshot candidate = snapshots.load(plan.candidateSnapshot());
      StackManager resolved = loader.resolve(candidate);
      MpackPlanner.Impact impact = planner.impact(snapshots.load(plan.previousSnapshot()), candidate, resolved);
      if (!new HashSet<>(plan.affectedClusters()).containsAll(impact.clusters())
          || (impact.inUseDefinitions() && !plan.mutation().maintenance())) {
        throw new MpackException(MpackException.Code.STALE_PLAN, "Accepted operation impact has changed");
      }
      if ((plan.restartRequired() || impact.restartRequired()) && !resumedAfterRestart.contains(id)) {
        waiting(id, Phase.WAITING_RESTART, Map.of());
        return;
      }
      write.lock();
      try {
        requireOwner(catalog.operation(id).value());
        MpackCatalog.Versioned<Operation> current = catalog.operation(id);
        if (current.value().phase() == Phase.CANCELLING) return;
        List<Map<String, Object>> admittedTasks = taskUsage.blockers(plan.affectedDefinitions());
        if (!admittedTasks.isEmpty()) {
          waiting(id, Phase.WAITING_MAINTENANCE, Map.of("reason", "AFFECTED_TASKS", "blockers", admittedTasks));
          return;
        }
        update(current, current.value().transition(Phase.PREPARING, current.value().effectiveSnapshot(), null, Map.of()));
        runHooks(id, plan, true);
        List<Map<String, Object>> remainingTasks = taskUsage.blockers(plan.affectedDefinitions());
        if (!remainingTasks.isEmpty()) {
          waiting(id, Phase.WAITING_MAINTENANCE, Map.of("reason", "AFFECTED_TASKS", "blockers", remainingTasks));
          return;
        }
        boolean catalogOnly = plan.previousSnapshot().equals(plan.candidateSnapshot());
        if (catalogOnly) {
          activation.commitCatalogOnly(plan, catalog.operation(id));
        } else if (!candidate.id().equals(catalog.operation(id).value().effectiveSnapshot())) {
          activation.activate(plan, catalog.operation(id), resolved);
        } else if (runtime.snapshot() == null || !candidate.id().equals(runtime.snapshot().id())) {
          activation.publishRuntime(id, candidate, resolved);
        }
        if (!catalogOnly) activation.announce();
        runHooks(id, plan, false);
        complete(id);
      } finally {
        write.unlock();
      }
    } catch (MpackException e) {
      if (id == null) throw e;
      write.lock();
      try { unresolved(id, e.getCode(), e.getDetails()); } finally { write.unlock(); }
    } catch (AmbariException | RuntimeException e) {
      if (id == null) throw new MpackException(MpackException.Code.RECOVERY_REQUIRED, "Completion reconciliation remains unresolved");
      write.lock();
      try {
        unresolved(id, MpackException.Code.RECOVERY_REQUIRED, Map.of("reason", "PUBLICATION_OR_STORAGE_FAILURE"));
      } finally { write.unlock(); }
    } finally {
      progress.unlock();
    }
  }

  private void reconcileCompleted(MpackCatalog.Versioned<Control> control) throws AmbariException {
    String id = runtime.pendingOperation();
    if (id == null) return;
    Operation completed = catalog.operation(id).value();
    if (!completed.terminal() || completed.generation() != control.value().generation()) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Runtime reservation has no matching terminal operation");
    }
    MpackSnapshots.Snapshot effective = snapshots.load(control.value().effectiveSnapshot());
    snapshots.verify(effective);
    StackManager restored = loader.resolve(effective);
    Lock write = runtime.writeLock();
    write.lock();
    try {
      MpackCatalog.Versioned<Control> current = catalog.control();
      if (current.revision() != control.revision() || current.value().pendingOperation() != null
          || !id.equals(runtime.pendingOperation())) return;
      boolean unmanagedBaseline = runtime.hasReadableView() && runtime.snapshot() == null && control.value().activeReleases().isEmpty()
          && effective.id().equals(control.value().builtinSnapshot());
      if (!unmanagedBaseline && (runtime.snapshot() == null || !effective.id().equals(runtime.snapshot().id())
          || !effective.id().equals(metadata.get().getStackManager().getDefinitionSnapshotId()))) {
        activation.publishRuntime(id, effective, restored);
        activation.announce();
      }
      runtime.release(id);
    } finally { write.unlock(); }
  }

  private void waiting(String id, Phase phase, Map<String, Object> details) {
    Lock write = runtime.writeLock();
    write.lock();
    try {
      MpackCatalog.Versioned<Operation> current = catalog.operation(id);
      requireOwner(current.value());
      if (current.value().phase() == Phase.CANCELLING) return;
      update(current, current.value().transition(phase, current.value().effectiveSnapshot(), null, details));
    } finally { write.unlock(); }
  }

  public static void authorize() throws AuthorizationException {
    AuthorizationHelper.verifyAuthorization(ResourceType.AMBARI, null,
        EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS));
    org.springframework.security.core.Authentication authentication = AuthorizationHelper.getAuthentication();
    boolean administrator = authentication != null && authentication.getAuthorities().stream()
        .filter(authority -> authority instanceof org.apache.ambari.server.security.authorization.AmbariGrantedAuthority)
        .map(authority -> ((org.apache.ambari.server.security.authorization.AmbariGrantedAuthority) authority)
            .getPrivilegeEntity().getPermission())
        .anyMatch(permission -> permission != null &&
            org.apache.ambari.server.orm.entities.PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME
                .equals(permission.getPermissionName()));
    if (!administrator || AuthorizationHelper.getAuthenticatedId() < 0) {
      throw new AuthorizationException("Management pack administration requires an authenticated Ambari administrator");
    }
  }

  private void runHooks(String id, Plan plan, boolean before) {
    if (plan.mutation().action() == MpackLifecycleState.Action.IMPORT
        || (plan.mutation().action() == MpackLifecycleState.Action.INSTALL && !plan.mutation().activate())) return;
    String phase = (before ? "before-" : "after-") + switch (plan.mutation().action()) {
      case ENABLE, INSTALL -> "install";
      case UPDATE -> "upgrade";
      case UNINSTALL -> "uninstall";
      default -> "binding";
    };
    for (Release release : hookReleases(plan)) {
      for (MpackManifest.Hook hook : release.manifest().hooks()) {
        if (!phase.equals(hook.phase())) {
          continue;
        }
        MpackCatalog.Versioned<Operation> operation = catalog.operation(id);
        String key = MpackHookRunner.key(release, hook);
        HookReceipt previous = operation.value().hooks().get(key);
        if (previous != null) {
          if (previous.state() == HookState.APPLIED) {
            continue;
          }
          throw new MpackException(MpackException.Code.RECOVERY_REQUIRED,
              "An interrupted hook requires explicit receipt reconciliation", Map.of("hook", key));
        }
        Map<String, HookReceipt> receipts = new HashMap<>(operation.value().hooks());
        receipts.put(key, new HookReceipt(1, id, plan.digest(), release.archiveDigest(), phase,
            MpackHookRunner.attempt(operation.value(), release, hook), HookState.RUNNING,
            MpackLifecycleState.EffectState.UNKNOWN, Map.of()));
        update(operation, withHooks(operation.value(), receipts));
        HookReceipt observed;
        runtime.writeLock().unlock();
        try {
          observed = hookRunner.run(operation.value(), release, hook);
        } finally {
          runtime.writeLock().lock();
        }
        requireOwner(catalog.operation(id).value());
        MpackCatalog.Versioned<Operation> running = catalog.operation(id);
        receipts.put(key, observed);
        update(running, withHooks(running.value(), receipts));
        if (observed.state() != HookState.APPLIED) {
          throw new MpackException(MpackException.Code.RECOVERY_REQUIRED,
              "Hook execution did not produce a verified applied result", Map.of("hook", key, "state", observed.state()));
        }
      }
    }
  }

  private List<Release> hookReleases(Plan plan) {
    if (plan.mutation().action() == MpackLifecycleState.Action.UNINSTALL) {
      List<Release> ordered = new ArrayList<>(orderedReleases(operationReleases(plan).stream()
          .filter(release -> plan.retiringReleases().contains(release.id())).toList()));
      java.util.Collections.reverse(ordered);
      return ordered;
    }
    if (plan.mutation().action() == MpackLifecycleState.Action.ENABLE) {
      return orderedReleases(plan.releases().stream().filter(release -> plan.initializingReleases().contains(release.id())).toList());
    }
    return orderedReleases(plan.releases().stream().filter(release -> plan.mutation().archiveDigests().contains(release.archiveDigest())).toList());
  }

  private List<Release> operationReleases(Plan plan) {
    if (plan.mutation().action() == MpackLifecycleState.Action.UNINSTALL) {
      return plan.mutation().releaseIds().stream().map(id -> catalog.release(id).value()).toList();
    }
    return plan.releases().stream().filter(release -> plan.mutation().action() == MpackLifecycleState.Action.ENABLE
        ? plan.mutation().releaseIds().contains(release.id())
        : plan.mutation().archiveDigests().contains(release.archiveDigest())).toList();
  }

  static List<Release> orderedReleases(List<Release> releases) {
    Map<String, Release> available = new java.util.TreeMap<>();
    releases.forEach(release -> available.put(release.id(), release));
    List<Release> result = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    for (String id : available.keySet()) orderRelease(id, available, visited, new HashSet<>(), result);
    return List.copyOf(result);
  }

  private static void orderRelease(String id, Map<String, Release> available, Set<String> visited,
      Set<String> visiting, List<Release> result) {
    if (visited.contains(id)) return;
    if (!visiting.add(id)) throw new MpackException(MpackException.Code.DEPENDENCY_CYCLE, "Hook providers form a dependency cycle");
    Release release = available.get(id);
    for (MpackDependencies.Reference provider : release.dependencies().values()) {
      if (available.containsKey(provider.release())) orderRelease(provider.release(), available, visited, visiting, result);
    }
    visiting.remove(id);
    visited.add(id);
    result.add(release);
  }

  private void complete(String id) {
    MpackCatalog.Versioned<Operation> operation = catalog.operation(id);
    MpackCatalog.Versioned<Control> control = catalog.control();
    requireOwner(operation.value());
    Plan plan = catalog.plan(operation.value().planId()).value();
    Set<String> required = new HashSet<>();
    hookReleases(plan).forEach(release -> required.addAll(requiredHooks(plan, release)));
    if (!operation.value().hooks().keySet().equals(required)
        || operation.value().hooks().values().stream().anyMatch(receipt -> receipt.state() != HookState.APPLIED)) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Required hook observations are incomplete");
    }
    boolean unmanagedBaseline = runtime.hasReadableView() && runtime.snapshot() == null && control.value().activeReleases().isEmpty()
        && control.value().effectiveSnapshot().equals(control.value().builtinSnapshot());
    if (!unmanagedBaseline && (runtime.snapshot() == null || !runtime.snapshot().id().equals(control.value().effectiveSnapshot()))) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Runtime and durable activation differ");
    }
    Control completed = new Control(1, control.value().builtinSnapshot(), control.value().effectiveSnapshot(),
        null, control.value().generation(), control.value().activeReleases());
    catalog.apply(List.of(MpackCatalog.change(MpackCatalog.CONTROL, Kind.CONTROL, control.revision(), completed),
        MpackCatalog.change(operation.key(), Kind.OPERATION, operation.revision(),
            operation.value().transition(Phase.SUCCEEDED, control.value().effectiveSnapshot(), null, Map.of()))));
    runtime.release(id);
  }

  private void requireOwner(Operation operation) {
    MpackCatalog.Versioned<Control> current = catalog.control();
    if (current == null || !operation.id().equals(current.value().pendingOperation())
        || operation.generation() != current.value().generation()) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Operation no longer owns the catalog generation");
    }
  }

  private void unresolved(String id, MpackException.Code code, Map<String, Object> details) {
    MpackCatalog.Versioned<Operation> current = catalog.operation(id);
    if (!current.value().terminal()) {
      Map<String, Object> observations = new HashMap<>(current.value().errorDetails());
      observations.putAll(details);
      update(current, current.value().transition(current.value().phase() == Phase.CANCELLING
              ? Phase.CANCELLING : Phase.RECOVERY_REQUIRED,
          current.value().effectiveSnapshot(), code, observations));
    }
  }

  private void update(MpackCatalog.Versioned<Operation> previous, Operation next) {
    catalog.apply(List.of(MpackCatalog.change(previous.key(), Kind.OPERATION, previous.revision(), next)));
  }

  private static Operation withHooks(Operation operation, Map<String, HookReceipt> receipts) {
    return new Operation(1, operation.id(), operation.planId(), operation.planDigest(), operation.ownerId(),
        operation.ownerName(), operation.phase(), operation.createdAt(), System.currentTimeMillis(), operation.generation(),
        operation.effectiveSnapshot(), receipts, operation.hookHistory(), operation.errorCode(), operation.errorDetails(), operation.parentId());
  }
}
