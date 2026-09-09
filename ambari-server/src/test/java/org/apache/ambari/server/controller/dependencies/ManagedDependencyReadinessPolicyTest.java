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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyReadinessPolicy.BindingPhase;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyReadinessPolicy.HostEvidence;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyReadinessPolicy.ProviderPreparation;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyReadinessPolicy.SnapshotApproval;
import org.junit.jupiter.api.Test;

class ManagedDependencyReadinessPolicyTest {
  private static final UUID BINDING_ID = UUID.fromString("2e4cf87b-e94f-435e-bc56-f98be0c36b39");
  private static final String SNAPSHOT_HASH =
      "sha256:6b60a968f701c2b8b3107879078968033d6f2d3d3f7f8343ee026e2d8759f93c";
  private static final String IDENTITY_HASH =
      "sha256:b73227ce67a73828f65c233da48a2c32b43c06e0bfbe9ac4906ff41b8c4cf04b";
  private static final String CONSUMER_HASH =
      "sha256:4237ab34c95a454996a6ad936cc6561c4494931151a7615174974c3941c9d132";
  private static final String CLIENT_CONFIG_HASH =
      "sha256:9d494a6b03b9caf55abff5aab1275dc82701da09dc97d30398a80d521e38e5f5";
  private static final String PROVIDER_HASH =
      "sha256:4da955fc4af6012e80fd223382230959993ece79f56d001a31e2d9e0ca12c793";

  private final ManagedDependencyReadinessPolicy policy = new ManagedDependencyReadinessPolicy();

  @Test
  void currentApprovalAndPreparationAllowLateHostInstallAfterProviderPreparation() {
    ManagedDependencySnapshot snapshot = snapshot();
    SnapshotApproval approval = approval(snapshot);
    ProviderPreparation preparation = preparation(snapshot);

    assertTrue(policy.canInstall(snapshot, approval, preparation, BindingPhase.PROVIDER_PREPARED).allowed());
    assertTrue(policy.canInstall(snapshot, approval, preparation, BindingPhase.CONSUMER_VERIFYING).allowed());
    assertTrue(policy.canInstall(snapshot, approval, preparation, BindingPhase.READY).allowed());
    assertFalse(policy.canInstall(snapshot, approval, preparation, BindingPhase.STALE).allowed());
    assertFalse(policy.canInstall(snapshot, approval, preparation, BindingPhase.DETACHING).allowed());
    assertFalse(policy.canInstall(snapshot, approval, preparation, BindingPhase.FENCING_UNCERTAIN).allowed());
    assertFalse(policy.canInstall(snapshot, approval, preparation, BindingPhase.RETIRED).allowed());
    assertFalse(policy.canInstall(snapshot, approval, preparation, null).allowed());
  }

  @Test
  void staleOrUnapprovedSnapshotCannotInstall() {
    ManagedDependencySnapshot snapshot = snapshot();
    SnapshotApproval stale = new SnapshotApproval(BINDING_ID, 6, SNAPSHOT_HASH, true);
    SnapshotApproval unapproved = new SnapshotApproval(BINDING_ID, 7, SNAPSHOT_HASH, false);

    assertFalse(policy.canInstall(snapshot, stale, preparation(snapshot), BindingPhase.READY).allowed());
    assertFalse(policy.canInstall(snapshot, unapproved, preparation(snapshot),
        BindingPhase.CONSUMER_VERIFYING).allowed());
    assertFalse(policy.canInstall(snapshot, approval(snapshot), null, BindingPhase.READY).allowed());
  }

  @Test
  void exactZooKeeperHandoffReconciliationAllowsInstallButNeverStart() {
    ManagedDependencySnapshot snapshot = snapshot(ManagedDependencyType.ZOOKEEPER);
    ProviderPreparation reconciliation = new ProviderPreparation(
        snapshot.bindingId(), snapshot.snapshotVersion(), snapshot.snapshotFingerprint(),
        snapshot.providerFingerprint(), 21L, false, true);

    assertTrue(policy.canInstall(snapshot, approval(snapshot), reconciliation,
        BindingPhase.ZOOKEEPER_HANDOFF_RECONCILING).allowed());
    assertFalse(policy.canInstall(snapshot(), approval(snapshot()), reconciliation,
        BindingPhase.ZOOKEEPER_HANDOFF_RECONCILING).allowed());
    assertFalse(policy.canStartOrRestart(snapshot, approval(snapshot), reconciliation,
        BindingPhase.ZOOKEEPER_HANDOFF_RECONCILING, 11L, Set.of(11L), Map.of(),
        "2.4.13-1", CLIENT_CONFIG_HASH, IDENTITY_HASH).allowed());
  }

  @Test
  void startRequiresCurrentEvidenceFromEveryDaemonHost() {
    ManagedDependencySnapshot snapshot = snapshot();
    Map<Long, HostEvidence> evidence = Map.of(11L, evidence(11L, 7), 12L, evidence(12L, 7));

    assertTrue(policy.canStartOrRestart(snapshot, approval(snapshot), preparation(snapshot), BindingPhase.READY,
        11L, Set.of(11L, 12L), evidence, "3.3.0-1", CLIENT_CONFIG_HASH,
        IDENTITY_HASH).allowed());

    assertFalse(policy.canStartOrRestart(snapshot, approval(snapshot), preparation(snapshot),
        BindingPhase.CONSUMER_VERIFYING, 11L, Set.of(11L, 12L), evidence,
        "3.3.0-1", CLIENT_CONFIG_HASH, IDENTITY_HASH).allowed());
    assertFalse(policy.canStartOrRestart(snapshot, approval(snapshot), preparation(snapshot), BindingPhase.READY,
        13L, Set.of(11L, 12L), evidence, "3.3.0-1", CLIENT_CONFIG_HASH,
        IDENTITY_HASH).allowed());
    assertFalse(policy.canStartOrRestart(snapshot, approval(snapshot), preparation(snapshot), BindingPhase.READY,
        11L, Set.of(11L, 12L), Map.of(11L, evidence(11L, 7)),
        "3.3.0-1", CLIENT_CONFIG_HASH, IDENTITY_HASH).allowed());
  }

  @Test
  void staleHostOrMissingDataNodeCheckCannotStart() {
    ManagedDependencySnapshot snapshot = snapshot();
    HostEvidence stale = evidence(11L, 6);
    HostEvidence missingDataNode = new HostEvidence(11L, BINDING_ID, 7, SNAPSHOT_HASH,
        "3.3.0-1", CLIENT_CONFIG_HASH, IDENTITY_HASH,
        new TreeSet<>(Set.of(ManagedDependencyReadinessPolicy.NAMENODE_RPC_CHECK)));

    assertFalse(policy.canStartOrRestart(snapshot, approval(snapshot), preparation(snapshot), BindingPhase.READY,
        11L, Set.of(11L), Map.of(11L, stale), "3.3.0-1", CLIENT_CONFIG_HASH,
        IDENTITY_HASH).allowed());
    assertFalse(policy.canStartOrRestart(snapshot, approval(snapshot), preparation(snapshot), BindingPhase.READY,
        11L, Set.of(11L), Map.of(11L, missingDataNode), "3.3.0-1",
        CLIENT_CONFIG_HASH, IDENTITY_HASH).allowed());

    HostEvidence wrongClientConfig = new HostEvidence(11L, BINDING_ID, 7, SNAPSHOT_HASH,
        "3.3.0-1", SNAPSHOT_HASH, IDENTITY_HASH,
        new TreeSet<>(Set.of(ManagedDependencyReadinessPolicy.NAMENODE_RPC_CHECK,
            ManagedDependencyReadinessPolicy.DATANODE_READ_WRITE_CHECK)));
    assertFalse(policy.canStartOrRestart(snapshot, approval(snapshot), preparation(snapshot), BindingPhase.READY,
        11L, Set.of(11L), Map.of(11L, wrongClientConfig), "3.3.0-1",
        CLIENT_CONFIG_HASH, IDENTITY_HASH).allowed());
  }

  @Test
  void staleAndFencingStatesStillAllowConsumerStop() {
    assertTrue(policy.canStop(BindingPhase.STALE).allowed());
    assertTrue(policy.canStop(BindingPhase.FENCING_UNCERTAIN).allowed());
  }

  private SnapshotApproval approval(ManagedDependencySnapshot snapshot) {
    return new SnapshotApproval(snapshot.bindingId(), snapshot.snapshotVersion(),
        snapshot.snapshotFingerprint(), true);
  }

  private ProviderPreparation preparation(ManagedDependencySnapshot snapshot) {
    return new ProviderPreparation(snapshot.bindingId(), snapshot.snapshotVersion(),
        snapshot.snapshotFingerprint(), snapshot.providerFingerprint(), 21L, true);
  }

  private HostEvidence evidence(long hostId, long snapshotVersion) {
    return new HostEvidence(hostId, BINDING_ID, snapshotVersion, SNAPSHOT_HASH,
        "3.3.0-1", CLIENT_CONFIG_HASH, IDENTITY_HASH,
        new TreeSet<>(Set.of(ManagedDependencyReadinessPolicy.NAMENODE_RPC_CHECK,
            ManagedDependencyReadinessPolicy.DATANODE_READ_WRITE_CHECK)));
  }

  private ManagedDependencySnapshot snapshot() {
    return snapshot(ManagedDependencyType.HDFS);
  }

  private ManagedDependencySnapshot snapshot(ManagedDependencyType type) {
    ManagedDependencyVersion version = new ManagedDependencyVersion(
        "BIGTOP", "3.3.0", true, type.getProviderServiceName() + "-3.3.0",
        new TreeMap<>(Map.of("repository.version", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 18L, List.of("https://mirror.example/repo"));
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hbase_mc_c1", new TreeSet<>(), false, "hbase_mc_c1", true, "0700", false);
    return new ManagedDependencySnapshot(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION,
        BINDING_ID, 7, type,
        new ManagedDependencyServiceKey(2L, type.getProviderServiceName()),
        type == ManagedDependencyType.HDFS
            ? ManagedDependencyNamespace.hdfs(BINDING_ID, "hdfs://nn.example.test:8020")
            : ManagedDependencyNamespace.zooKeeper(BINDING_ID),
        version.compatibility(), ManagedDependencySecurityMode.INSECURE, identity,
        type == ManagedDependencyType.HDFS
            ? new TreeMap<>(Map.of("fs.defaultFS", "hdfs://nn.example.test:8020"))
            : new TreeMap<>(),
        new TreeMap<>(), type == ManagedDependencyType.ZOOKEEPER
            ? new TreeMap<>(Map.of(
                "hbase.zookeeper.property.clientPort", "2181",
                "hbase.zookeeper.quorum", "zk1.example.test",
                "zookeeper.znode.parent", ManagedDependencyNamespace.zooKeeper(BINDING_ID).znode()))
            : new TreeMap<>(), CONSUMER_HASH, PROVIDER_HASH, SNAPSHOT_HASH);
  }
}
