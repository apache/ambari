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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/** Pure guards for client installation and HBase daemon start/restart. */
public final class ManagedDependencyReadinessPolicy {
  public static final String NAMENODE_RPC_CHECK = "NAMENODE_RPC";
  public static final String DATANODE_READ_WRITE_CHECK = "DATANODE_READ_WRITE";
  public static final String ZOOKEEPER_QUORUM_CHECK = "ZOOKEEPER_QUORUM";
  public static final String ZOOKEEPER_PRIVATE_ZNODE_CHECK = "ZOOKEEPER_PRIVATE_ZNODE";

  public Decision canInstall(ManagedDependencySnapshot desiredSnapshot,
      SnapshotApproval approval, ProviderPreparation preparation, BindingPhase phase) {
    if (desiredSnapshot == null || approval == null || !approval.approved()
        || !approval.bindingId().equals(desiredSnapshot.bindingId())
        || approval.snapshotVersion() != desiredSnapshot.snapshotVersion()
        || !approval.snapshotFingerprint().equals(desiredSnapshot.snapshotFingerprint())) {
      return Decision.reject(ManagedDependencyErrorCode.DEPENDENCY_SNAPSHOT_NOT_APPROVED,
          "Client installation requires the durable approval for the exact current snapshot.");
    }
    if (preparation == null
        || !preparation.bindingId().equals(desiredSnapshot.bindingId())
        || preparation.snapshotVersion() != desiredSnapshot.snapshotVersion()
        || !preparation.snapshotFingerprint().equals(desiredSnapshot.snapshotFingerprint())
        || !preparation.providerFingerprint().equals(desiredSnapshot.providerFingerprint())) {
      return Decision.reject(ManagedDependencyErrorCode.DEPENDENCY_PROVIDER_NOT_READY,
          "Client installation requires provider preparation evidence for the exact current snapshot.");
    }
    boolean reconciliationInstall = desiredSnapshot.type() == ManagedDependencyType.ZOOKEEPER
        && phase == BindingPhase.ZOOKEEPER_HANDOFF_RECONCILING
        && preparation.handoffReconciliationRequired();
    if (!preparation.prepared() && !reconciliationInstall) {
      return Decision.reject(ManagedDependencyErrorCode.DEPENDENCY_PROVIDER_NOT_READY,
          "Client installation requires provider preparation or an exact ZooKeeper handoff reconciliation.");
    }
    if (phase == null || !Set.of(BindingPhase.PROVIDER_PREPARED,
        BindingPhase.ZOOKEEPER_HANDOFF_RECONCILING, BindingPhase.CONSUMER_VERIFYING,
        BindingPhase.READY).contains(phase)) {
      return Decision.reject(ManagedDependencyErrorCode.DEPENDENCY_PROVIDER_NOT_READY,
          "Client installation is blocked while the binding is stale, retiring, or not provider-prepared.");
    }
    return Decision.allow();
  }

  public Decision canStartOrRestart(ManagedDependencySnapshot desiredSnapshot,
      SnapshotApproval approval, ProviderPreparation preparation, BindingPhase phase, long targetHostId,
      Set<Long> requiredHostIds, Map<Long, HostEvidence> evidenceByHost,
      String expectedPackageVersion, String expectedClientConfigFingerprint,
      String expectedIdentityFingerprint) {
    Decision installDecision = canInstall(desiredSnapshot, approval, preparation, phase);
    if (!installDecision.allowed()) {
      return installDecision;
    }
    if (phase != BindingPhase.READY) {
      return Decision.reject(ManagedDependencyErrorCode.DEPENDENCY_CONSUMER_NOT_READY,
          "HBase start and restart require a READY binding with current evidence from every daemon host.");
    }
    if (requiredHostIds == null || requiredHostIds.isEmpty()
        || !requiredHostIds.contains(targetHostId)) {
      return Decision.reject(ManagedDependencyErrorCode.DEPENDENCY_CONSUMER_NOT_READY,
          "The target must be a current HBase daemon host in the approved topology.");
    }
    Set<String> requiredChecks = desiredSnapshot.type() == ManagedDependencyType.HDFS
        ? Set.of(NAMENODE_RPC_CHECK, DATANODE_READ_WRITE_CHECK)
        : Set.of(ZOOKEEPER_QUORUM_CHECK, ZOOKEEPER_PRIVATE_ZNODE_CHECK);
    for (Long hostId : requiredHostIds) {
      HostEvidence evidence = evidenceByHost == null ? null : evidenceByHost.get(hostId);
      if (evidence == null
          || evidence.hostId() != hostId
          || !evidence.bindingId().equals(desiredSnapshot.bindingId())
          || evidence.snapshotVersion() != desiredSnapshot.snapshotVersion()
          || !evidence.snapshotFingerprint().equals(desiredSnapshot.snapshotFingerprint())
          || !evidence.packageVersion().equals(expectedPackageVersion)
          || !evidence.clientConfigFingerprint().equals(expectedClientConfigFingerprint)
          || !evidence.identityFingerprint().equals(expectedIdentityFingerprint)
          || !evidence.passedChecks().containsAll(requiredChecks)) {
        return Decision.reject(ManagedDependencyErrorCode.DEPENDENCY_CONSUMER_NOT_READY,
            "Every current HBase daemon host needs current client, identity, and connectivity evidence; host "
                + hostId + " is missing or stale.");
      }
    }
    return Decision.allow();
  }

  public Decision canStop(BindingPhase phase) {
    return phase == null
        ? Decision.reject(ManagedDependencyErrorCode.INVALID_CONSUMER_DESCRIPTOR,
            "A binding phase is required to evaluate consumer stop.")
        : Decision.allow();
  }

  public enum BindingPhase {
    PREVIEWED,
    PROVIDER_PREPARING,
    PROVIDER_PREPARED,
    ZOOKEEPER_HANDOFF_RECONCILING,
    CONSUMER_VERIFYING,
    READY,
    STALE,
    FAILED,
    DETACHING,
    FENCING_UNCERTAIN,
    RETIRED,
    DETACHED,
    TOMBSTONED
  }

  public record ProviderPreparation(
      UUID bindingId,
      long snapshotVersion,
      String snapshotFingerprint,
      String providerFingerprint,
      long actionHostId,
      boolean prepared,
      boolean handoffReconciliationRequired) {
    public ProviderPreparation {
      bindingId = Objects.requireNonNull(bindingId, "bindingId");
      if (snapshotVersion <= 0 || actionHostId <= 0) {
        throw new IllegalArgumentException("snapshotVersion and actionHostId must be positive");
      }
      snapshotFingerprint = requireNonBlank(snapshotFingerprint, "snapshotFingerprint");
      providerFingerprint = requireNonBlank(providerFingerprint, "providerFingerprint");
      if (prepared && handoffReconciliationRequired) {
        throw new IllegalArgumentException(
            "preparation cannot be prepared while awaiting ZooKeeper handoff reconciliation");
      }
    }

    public ProviderPreparation(UUID bindingId, long snapshotVersion,
        String snapshotFingerprint, String providerFingerprint, long actionHostId,
        boolean prepared) {
      this(bindingId, snapshotVersion, snapshotFingerprint, providerFingerprint,
          actionHostId, prepared, false);
    }
  }

  public record SnapshotApproval(
      UUID bindingId,
      long snapshotVersion,
      String snapshotFingerprint,
      boolean approved) {
    public SnapshotApproval {
      bindingId = Objects.requireNonNull(bindingId, "bindingId");
      if (snapshotVersion <= 0) {
        throw new IllegalArgumentException("snapshotVersion must be positive");
      }
      snapshotFingerprint = requireNonBlank(snapshotFingerprint, "snapshotFingerprint");
    }
  }

  public record HostEvidence(
      long hostId,
      UUID bindingId,
      long snapshotVersion,
      String snapshotFingerprint,
      String packageVersion,
      String clientConfigFingerprint,
      String identityFingerprint,
      SortedSet<String> passedChecks) {
    public HostEvidence {
      if (hostId <= 0 || snapshotVersion <= 0) {
        throw new IllegalArgumentException("hostId and snapshotVersion must be positive");
      }
      bindingId = Objects.requireNonNull(bindingId, "bindingId");
      snapshotFingerprint = requireNonBlank(snapshotFingerprint, "snapshotFingerprint");
      packageVersion = requireNonBlank(packageVersion, "packageVersion");
      clientConfigFingerprint = requireNonBlank(clientConfigFingerprint, "clientConfigFingerprint");
      identityFingerprint = requireNonBlank(identityFingerprint, "identityFingerprint");
      passedChecks = passedChecks == null
          ? Collections.emptySortedSet()
          : Collections.unmodifiableSortedSet(new TreeSet<>(passedChecks));
    }
  }

  public record Decision(boolean allowed, ManagedDependencyErrorCode errorCode, String message) {
    public Decision {
      if (allowed && (errorCode != null || message != null)
          || !allowed && (errorCode == null || message == null || message.isBlank())) {
        throw new IllegalArgumentException("allowed decisions have no error; rejected decisions require one");
      }
    }

    public static Decision allow() {
      return new Decision(true, null, null);
    }

    public static Decision reject(ManagedDependencyErrorCode code, String message) {
      return new Decision(false, Objects.requireNonNull(code, "code"), message);
    }
  }

  private static String requireNonBlank(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
