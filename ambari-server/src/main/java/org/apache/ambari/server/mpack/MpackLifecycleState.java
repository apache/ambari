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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.stack.StackResolutionContext;

/** Versioned persistent contracts. An operation outcome is distinct from activation. */
public final class MpackLifecycleState {
  public enum Action {
    IMPORT, ENABLE, INSTALL, UPDATE, BIND, UNBIND, UNINSTALL
  }

  public enum Phase {
    ACCEPTED, PREPARING, WAITING_MAINTENANCE, WAITING_RESTART, PUBLISHING,
    SUCCEEDED, FAILED, CANCELLING, CANCELLED, RECOVERY_REQUIRED
  }

  public enum HookState {
    RUNNING, APPLIED, FAILED, UNKNOWN
  }

  public enum EffectState {
    APPLIED, NOT_APPLIED, UNKNOWN
  }

  public record Control(int schemaVersion, String builtinSnapshot, String effectiveSnapshot,
      String pendingOperation, long generation, List<String> activeReleases) {
    public Control {
      schema(schemaVersion);
      MpackManifest.requireDigest(builtinSnapshot);
      MpackManifest.requireDigest(effectiveSnapshot);
      if (pendingOperation != null) {
        identifier(pendingOperation);
      }
      if (generation < 0) {
        throw invalid("Invalid publication generation");
      }
      activeReleases = List.copyOf(activeReleases);
    }
  }

  public record Release(int schemaVersion, String id, String archiveDigest, String manifestJson,
      List<MpackResources.Contribution> contributions,
      Map<String, MpackDependencies.Reference> dependencies, boolean installed) {
    public Release {
      schema(schemaVersion);
      MpackManifest.requireDigest(archiveDigest);
      MpackManifest manifest = MpackManifest.parse(manifestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      if (!manifest.identity().equals(id)) {
        throw invalid("Release identity does not match its manifest");
      }
      contributions = List.copyOf(contributions);
      dependencies = Map.copyOf(dependencies);
      if (contributions.stream().anyMatch(value -> !archiveDigest.equals(value.archiveDigest()))) {
        throw invalid("Release contributions belong to another archive");
      }
    }

    public MpackManifest manifest() {
      return MpackManifest.parse(manifestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public MpackResources.PreparedPack prepared() {
      return new MpackResources.PreparedPack(manifest(), archiveDigest, contributions,
          MpackJson.digest(MpackJson.tree(contributions)));
    }
  }

  public record Mutation(int schemaVersion, Action action, List<String> archiveDigests,
      List<String> releaseIds, List<StackResolutionContext.Binding> bindings,
      boolean activate, boolean maintenance) {
    public Mutation {
      schema(schemaVersion);
      Objects.requireNonNull(action);
      archiveDigests = List.copyOf(archiveDigests);
      releaseIds = List.copyOf(releaseIds);
      bindings = List.copyOf(bindings);
      archiveDigests.forEach(MpackManifest::requireDigest);
      if (archiveDigests.stream().distinct().count() != archiveDigests.size()
          || releaseIds.stream().distinct().count() != releaseIds.size()) {
        throw invalid("Duplicate mutation input");
      }
      switch (action) {
        case IMPORT -> {
          if (archiveDigests.isEmpty() || !releaseIds.isEmpty() || !bindings.isEmpty() || activate) {
            throw invalid("Import requires archives without activation or bindings");
          }
        }
        case ENABLE -> {
          if (!archiveDigests.isEmpty() || releaseIds.isEmpty() || !activate) {
            throw invalid("Enable requires imported releases and activation");
          }
        }
        case INSTALL -> {
          if (archiveDigests.isEmpty() || !releaseIds.isEmpty()) {
            throw invalid("Install requires archives and no replacement release IDs");
          }
        }
        case UPDATE -> {
          if (archiveDigests.isEmpty() || releaseIds.isEmpty() || !activate) {
            throw invalid("Update requires archives, replaced releases, and activation");
          }
        }
        case BIND, UNBIND -> {
          if (!archiveDigests.isEmpty() || releaseIds.isEmpty() || bindings.isEmpty() || !activate) {
            throw invalid("Binding changes require installed releases, targets, and activation");
          }
        }
        case UNINSTALL -> {
          if (!archiveDigests.isEmpty() || releaseIds.isEmpty() || !bindings.isEmpty()) {
            throw invalid("Uninstall requires exact release IDs without new archives or bindings");
          }
        }
        default -> throw invalid("Unknown lifecycle action");
      }
      if (!activate && !bindings.isEmpty()) {
        throw invalid("Bindings require explicit activation");
      }
    }
  }

  public record Plan(int schemaVersion, String id, String digest, long catalogRevision,
      long expiresAt, Mutation mutation, String previousSnapshot, String candidateSnapshot,
      List<Release> releases, List<String> activeReleases, List<String> affectedClusters,
      boolean maintenanceRequired, boolean restartRequired, List<MpackScope> affectedDefinitions,
      List<String> initializingReleases, List<String> retiringReleases, Deployment deployment) {
    public Plan(int schemaVersion, String id, String digest, long catalogRevision,
        long expiresAt, Mutation mutation, String previousSnapshot, String candidateSnapshot,
        List<Release> releases, List<String> activeReleases, List<String> affectedClusters,
        boolean maintenanceRequired, boolean restartRequired) {
      this(schemaVersion, id, digest, catalogRevision, expiresAt, mutation, previousSnapshot,
          candidateSnapshot, releases, activeReleases, affectedClusters, maintenanceRequired,
          restartRequired, List.of(), List.of(), List.of(), null);
    }
    public Plan {
      schema(schemaVersion);
      identifier(id);
      MpackManifest.requireDigest(digest);
      MpackManifest.requireDigest(previousSnapshot);
      MpackManifest.requireDigest(candidateSnapshot);
      if (catalogRevision < org.apache.ambari.server.orm.dao.MpackRecordDAO.ABSENT || expiresAt <= 0) {
        throw invalid("Invalid plan revision or expiry");
      }
      Objects.requireNonNull(mutation);
      releases = List.copyOf(releases);
      activeReleases = List.copyOf(activeReleases);
      affectedClusters = List.copyOf(affectedClusters);
      affectedDefinitions = List.copyOf(affectedDefinitions);
      initializingReleases = List.copyOf(initializingReleases);
      retiringReleases = List.copyOf(retiringReleases);
    }
  }

  public record Deployment(String stackName, String stackVersion, Long clusterId, List<String> serviceNames,
      List<String> serviceIds) {
    public Deployment {
      MpackManifest.requireName(stackName);
      MpackManifest.requireVersion(stackVersion);
      if (clusterId != null && clusterId < 1) throw invalid("A positive target cluster identity is required");
      serviceNames = List.copyOf(serviceNames);
      if (serviceNames.isEmpty()) throw invalid("Select at least one service");
      serviceNames.forEach(MpackManifest::requireName);
      serviceIds = List.copyOf(serviceIds);
      if (serviceIds.size() != serviceNames.size()) throw invalid("Service selection identities are incomplete");
      serviceIds.forEach(MpackManifest::requireDigest);
    }
  }

  public record HookReceipt(int schemaVersion, String operationId, String planDigest,
      String archiveDigest, String phase, int attempt, HookState state, EffectState effectState,
      Map<String, Object> observations) {
    public HookReceipt {
      schema(schemaVersion);
      identifier(operationId);
      MpackManifest.requireDigest(planDigest);
      MpackManifest.requireDigest(archiveDigest);
      if (phase == null || phase.isEmpty() || state == null || effectState == null || attempt < 1) {
        throw invalid("Missing hook phase or outcome");
      }
      observations = Map.copyOf(observations);
      if (effectState != EffectState.UNKNOWN && observations.isEmpty()) {
        throw invalid("Known hook effects require structured observations");
      }
      if (state == HookState.APPLIED && (observations.isEmpty() || effectState == EffectState.UNKNOWN)) {
        throw invalid("An applied hook requires structured observations");
      }
    }
  }

  public record Operation(int schemaVersion, String id, String planId, String planDigest,
      int ownerId, String ownerName, Phase phase, long createdAt, long updatedAt,
      long generation, String effectiveSnapshot, Map<String, HookReceipt> hooks,
      List<HookReceipt> hookHistory, MpackException.Code errorCode, Map<String, Object> errorDetails, String parentId) {
    public Operation {
      schema(schemaVersion);
      identifier(id);
      identifier(planId);
      MpackManifest.requireDigest(planDigest);
      Objects.requireNonNull(ownerName);
      Objects.requireNonNull(phase);
      if (createdAt <= 0 || updatedAt < createdAt || generation < 0) {
        throw invalid("Invalid operation timestamps or generation");
      }
      if (effectiveSnapshot != null) {
        MpackManifest.requireDigest(effectiveSnapshot);
      }
      if (parentId != null) {
        identifier(parentId);
      }
      hooks = Map.copyOf(hooks);
      hookHistory = List.copyOf(hookHistory);
      errorDetails = Map.copyOf(errorDetails);
      if (phase == Phase.SUCCEEDED && (effectiveSnapshot == null || errorCode != null
          || hooks.values().stream().anyMatch(value -> value.state() != HookState.APPLIED))) {
        throw invalid("Successful operations require verified publication and resolved hooks");
      }
      hooks.forEach((key, receipt) -> {
        if (!id.equals(receipt.operationId()) || !planDigest.equals(receipt.planDigest())) {
          throw invalid("Hook receipt belongs to another operation");
        }
        if (!key.equals(receipt.archiveDigest() + "/" + receipt.phase())) {
          throw invalid("Hook receipt does not match its resource and phase key");
        }
      });
      Set<String> historicalAttempts = new HashSet<>();
      Map<String, HookReceipt> currentHooks = hooks;
      hookHistory.forEach(receipt -> {
        if (!id.equals(receipt.operationId()) || !planDigest.equals(receipt.planDigest())) {
          throw invalid("Historical hook receipt belongs to another operation");
        }
        String key = receipt.archiveDigest() + "/" + receipt.phase();
        if (!historicalAttempts.add(key + "/" + receipt.attempt())
            || (currentHooks.containsKey(key) && currentHooks.get(key).attempt() <= receipt.attempt())) {
          throw invalid("Hook attempt history is duplicated or newer than the current observation");
        }
      });
    }

    public boolean terminal() {
      return phase == Phase.SUCCEEDED || phase == Phase.FAILED || phase == Phase.CANCELLED;
    }

    public Operation transition(Phase next, String effective, MpackException.Code error,
        Map<String, Object> details) {
      return new Operation(schemaVersion, id, planId, planDigest, ownerId, ownerName, next,
          createdAt, System.currentTimeMillis(), generation, effective, hooks, hookHistory, error, details, parentId);
    }
  }

  public record Idempotency(int schemaVersion, int ownerId, String requestDigest, String operationId) {
    public Idempotency {
      schema(schemaVersion);
      MpackManifest.requireDigest(requestDigest);
      identifier(operationId);
    }
  }

  private MpackLifecycleState() {
  }

  public static Set<String> requiredHooks(Plan plan, Release release) {
    if (plan.mutation().action() == Action.IMPORT
        || (plan.mutation().action() == Action.INSTALL && !plan.mutation().activate())) return Set.of();
    if (plan.mutation().action() == Action.ENABLE && !plan.initializingReleases().contains(release.id())) return Set.of();
    if (plan.mutation().action() == Action.UNINSTALL && !plan.retiringReleases().contains(release.id())) return Set.of();
    String suffix = switch (plan.mutation().action()) {
      case ENABLE, INSTALL -> "install";
      case UPDATE -> "upgrade";
      case UNINSTALL -> "uninstall";
      default -> "binding";
    };
    Set<String> result = new HashSet<>();
    for (MpackManifest.Hook hook : release.manifest().hooks()) {
      if (hook.phase().equals("before-" + suffix) || hook.phase().equals("after-" + suffix)) {
        result.add(MpackHookRunner.key(release, hook));
      }
    }
    return result;
  }

  private static void schema(int version) {
    if (version != 1) {
      throw new MpackException(MpackException.Code.UNSUPPORTED_SCHEMA, "Unsupported lifecycle record schema");
    }
  }

  private static void identifier(String id) {
    if (id == null || !UUID.fromString(id).toString().equals(id)) {
      throw invalid("A canonical operation or plan UUID is required");
    }
  }

  private static MpackException invalid(String message) {
    return new MpackException(MpackException.Code.INVALID_RECEIPT, message);
  }
}
