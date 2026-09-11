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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.server.controller.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.ambari.server.controller.AuthToLocalBuilder;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity.Plan;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosDescriptorOverlay.SealedConfigurations;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosLivePlan;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec.ManagedBindingSnapshotRef;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingProof;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerPatternInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier.ConsumerInput;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.SecurityType;
import org.junit.jupiter.api.Test;

class PersistedManagedHBaseKerberosLivePlanProviderTest {
  private static final long CONSUMER_CLUSTER_ID = 41L;
  private static final String REALM = "EXAMPLE.COM";
  private static final UUID HDFS_BINDING =
      UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID ZOOKEEPER_BINDING =
      UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final String CONSUMER_FINGERPRINT = hash('a');

  @Test
  void noBindingsKeepsTheUnboundClusterPathAndReadsInsideTheClusterLock() {
    ReadFixture fixture = fixture(Optional.empty(), SecurityType.NONE, null);

    assertTrue(fixture.provider().findApprovedLivePlan(fixture.cluster()).isEmpty());

    assertFalse(fixture.inReadLock().get());
    verify(fixture.dao()).findApprovedLivePlanSnapshots(CONSUMER_CLUSTER_ID);
  }

  @Test
  void coherentInsecurePlanKeepsExistingUnsecuredBehavior() {
    ManagedDependencySnapshot insecure = insecureSnapshot(
        ManagedDependencyType.HDFS, HDFS_BINDING);
    ReadFixture fixture = fixture(Optional.of(List.of(insecure)), SecurityType.NONE, null);

    assertTrue(fixture.provider().findApprovedLivePlan(fixture.cluster()).isEmpty());
  }

  @Test
  void legacyInsecureSnapshotKeepsExistingUnsecuredBehavior() {
    ManagedDependencySnapshot current = insecureSnapshot(
        ManagedDependencyType.HDFS, HDFS_BINDING);
    ManagedDependencySnapshot legacy = new ManagedDependencySnapshot(
        ManagedDependencySnapshot.LEGACY_INSECURE_SCHEMA_VERSION,
        current.bindingId(), current.snapshotVersion(), current.type(),
        current.providerService(), current.namespace(), current.providerVersion(),
        current.securityMode(), current.consumerIdentity(), current.coreSite(),
        current.hdfsSite(), current.zooKeeperClient(), current.consumerFingerprint(),
        current.providerFingerprint(), current.snapshotFingerprint());
    ReadFixture fixture = fixture(Optional.of(List.of(legacy)), SecurityType.NONE, null);

    assertTrue(fixture.provider().findApprovedLivePlan(fixture.cluster()).isEmpty());
  }

  @Test
  void reconstructsOneBindingSecurePlanFromTypedProofs() {
    ManagedHBaseConsumerLocalMapping mapping = consumerMapping(1L, REALM, false);
    ManagedDependencySnapshot hdfs = secureSnapshot(
        ManagedDependencyType.HDFS, HDFS_BINDING, mapping, CONSUMER_FINGERPRINT);
    ReadFixture fixture = fixture(Optional.of(List.of(hdfs)), SecurityType.KERBEROS, REALM);

    ManagedHBaseKerberosLivePlan plan = fixture.provider()
        .findApprovedLivePlan(fixture.cluster()).orElseThrow();

    assertTrue(plan.overlaySpec().hasManagedHdfs());
    assertFalse(plan.overlaySpec().hasManagedZooKeeper());
    assertEquals(mapping.profileFingerprint(),
        plan.approvedConsumerMappingProfileFingerprint());
    assertEquals(mapping.identityPlanFingerprint(), plan.overlaySpec().identityPlanFingerprint());
  }

  @Test
  void reconstructsImmutableTwoBindingPlanWithProspectiveOverlaySemantics() {
    ManagedHBaseConsumerLocalMapping firstMapping = consumerMapping(1L, REALM, false);
    ManagedHBaseConsumerLocalMapping secondMapping = consumerMapping(1L, REALM, false);
    List<ManagedDependencySnapshot> snapshots = new ArrayList<>(List.of(
        secureSnapshot(ManagedDependencyType.ZOOKEEPER, ZOOKEEPER_BINDING,
            secondMapping, CONSUMER_FINGERPRINT),
        secureSnapshot(ManagedDependencyType.HDFS, HDFS_BINDING,
            firstMapping, CONSUMER_FINGERPRINT)));
    ReadFixture fixture = fixture(Optional.of(snapshots), SecurityType.KERBEROS, REALM);

    ManagedHBaseKerberosLivePlan plan = fixture.provider()
        .findApprovedLivePlan(fixture.cluster()).orElseThrow();
    snapshots.clear();

    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> expectedBindings = new TreeMap<>();
    expectedBindings.put(ManagedDependencyType.HDFS,
        new ManagedBindingSnapshotRef(ManagedDependencyType.HDFS, HDFS_BINDING));
    expectedBindings.put(ManagedDependencyType.ZOOKEEPER,
        new ManagedBindingSnapshotRef(ManagedDependencyType.ZOOKEEPER, ZOOKEEPER_BINDING));
    ManagedHBaseKerberosOverlaySpec expected = ManagedHBaseKerberosOverlaySpec.create(
        REALM, "hbase_mc_c1", "ambari-qa-plan@" + REALM, "ambari-qa",
        Plan.forExistingCluster(1L).planFingerprint(), expectedBindings);
    assertEquals(expected, plan.overlaySpec());
    assertEquals(firstMapping.profileFingerprint(),
        plan.approvedConsumerMappingProfileFingerprint());
    assertThrows(UnsupportedOperationException.class,
        () -> plan.overlaySpec().bindings().clear());
  }

  @Test
  void rejectsMixedSecurityAndConsumerSecurityTransitions() {
    ManagedHBaseConsumerLocalMapping mapping = consumerMapping(1L, REALM, false);
    ManagedDependencySnapshot secure = secureSnapshot(
        ManagedDependencyType.HDFS, HDFS_BINDING, mapping, CONSUMER_FINGERPRINT);
    ManagedDependencySnapshot insecure = insecureSnapshot(
        ManagedDependencyType.ZOOKEEPER, ZOOKEEPER_BINDING);

    assertCode("DEPENDENCY_SECURITY_MISMATCH", fixture(
        Optional.of(List.of(secure, insecure)), SecurityType.KERBEROS, REALM));
    assertCode("DEPENDENCY_SECURITY_MISMATCH", fixture(
        Optional.of(List.of(secure)), SecurityType.NONE, null));
    assertCode("DEPENDENCY_SECURITY_MISMATCH", fixture(
        Optional.of(List.of(insecure)), SecurityType.KERBEROS, REALM));
  }

  @Test
  void rejectsDifferentConsumerUsersAndMappingProfiles() {
    ManagedHBaseConsumerLocalMapping first = consumerMapping(1L, REALM, false);
    ManagedHBaseConsumerLocalMapping otherUser = consumerMapping(2L, REALM, false);
    ManagedHBaseConsumerLocalMapping otherProfile = consumerMapping(1L, REALM, true);
    ManagedDependencySnapshot hdfs = secureSnapshot(
        ManagedDependencyType.HDFS, HDFS_BINDING, first, CONSUMER_FINGERPRINT);

    assertCode("DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED", fixture(Optional.of(List.of(
        hdfs, secureSnapshot(ManagedDependencyType.ZOOKEEPER, ZOOKEEPER_BINDING,
            otherUser, hash('b')))), SecurityType.KERBEROS, REALM));
    assertCode("DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED", fixture(Optional.of(List.of(
        hdfs, secureSnapshot(ManagedDependencyType.ZOOKEEPER, ZOOKEEPER_BINDING,
            otherProfile, CONSUMER_FINGERPRINT))), SecurityType.KERBEROS, REALM));
  }

  @Test
  void rejectsDifferentRealmDuplicateTypeAndBindingUuidReuse() {
    ManagedHBaseConsumerLocalMapping mapping = consumerMapping(1L, REALM, false);
    ManagedHBaseConsumerLocalMapping otherRealm =
        consumerMapping(1L, "OTHER.EXAMPLE.COM", false);
    ManagedDependencySnapshot hdfs = secureSnapshot(
        ManagedDependencyType.HDFS, HDFS_BINDING, mapping, CONSUMER_FINGERPRINT);
    ManagedDependencySnapshot secondHdfs = secureSnapshot(
        ManagedDependencyType.HDFS, ZOOKEEPER_BINDING, mapping, CONSUMER_FINGERPRINT);
    ManagedDependencySnapshot zooKeeperWithReusedId = secureSnapshot(
        ManagedDependencyType.ZOOKEEPER, HDFS_BINDING, mapping, CONSUMER_FINGERPRINT);

    assertCode("DEPENDENCY_SECURITY_MISMATCH", fixture(
        Optional.of(List.of(hdfs)), SecurityType.KERBEROS, "OTHER.EXAMPLE.COM"));
    assertCode("DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED", fixture(Optional.of(List.of(
        hdfs, secureSnapshot(ManagedDependencyType.ZOOKEEPER, ZOOKEEPER_BINDING,
            otherRealm, CONSUMER_FINGERPRINT))), SecurityType.KERBEROS, REALM));
    assertCode("DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED", fixture(
        Optional.of(List.of(hdfs, secondHdfs)), SecurityType.KERBEROS, REALM));
    assertCode("DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED", fixture(
        Optional.of(List.of(hdfs, zooKeeperWithReusedId)), SecurityType.KERBEROS, REALM));
  }

  @Test
  void propagatesTheDaosInvalidPlanDecisionWithoutFallback() {
    ManagedDependencyIntegrationException expected =
        new ManagedDependencyIntegrationException(409,
            "DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED", "stale approved snapshot");
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    when(dao.findApprovedLivePlanSnapshots(CONSUMER_CLUSTER_ID)).thenThrow(expected);
    Cluster cluster = cluster(SecurityType.KERBEROS, REALM, new AtomicBoolean());
    PersistedManagedHBaseKerberosLivePlanProvider provider =
        new PersistedManagedHBaseKerberosLivePlanProvider(dao);

    assertSame(expected, assertThrows(ManagedDependencyIntegrationException.class,
        () -> provider.findApprovedLivePlan(cluster)));
  }

  private void assertCode(String code, ReadFixture fixture) {
    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> fixture.provider().findApprovedLivePlan(fixture.cluster()));
    assertEquals(code, error.getCode());
  }

  private ReadFixture fixture(Optional<List<ManagedDependencySnapshot>> decision,
      SecurityType securityType, String realm) {
    AtomicBoolean inReadLock = new AtomicBoolean();
    Cluster cluster = cluster(securityType, realm, inReadLock);
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    when(dao.findApprovedLivePlanSnapshots(CONSUMER_CLUSTER_ID)).thenAnswer(invocation -> {
      assertTrue(inReadLock.get(), "DAO read must be covered by the consumer cluster read lock");
      return decision;
    });
    return new ReadFixture(cluster, dao,
        new PersistedManagedHBaseKerberosLivePlanProvider(dao), inReadLock);
  }

  @SuppressWarnings("unchecked")
  private Cluster cluster(SecurityType securityType, String realm,
      AtomicBoolean inReadLock) {
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterId()).thenReturn(CONSUMER_CLUSTER_ID);
    when(cluster.getSecurityType()).thenReturn(securityType);
    if (realm != null) {
      Config kerberosEnv = mock(Config.class);
      when(kerberosEnv.getProperties()).thenReturn(Map.of("realm", realm));
      when(cluster.getDesiredConfigByType("kerberos-env")).thenReturn(kerberosEnv);
    }
    when(cluster.executeUnderReadLock(any())).thenAnswer(invocation -> {
      assertTrue(inReadLock.compareAndSet(false, true));
      try {
        return ((Supplier<Object>) invocation.getArgument(0)).get();
      } finally {
        assertTrue(inReadLock.compareAndSet(true, false));
      }
    });
    return cluster;
  }

  private ManagedDependencySnapshot insecureSnapshot(ManagedDependencyType type,
      UUID bindingId) {
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hbase_mc_c1", new TreeSet<>(), false, "hbase_mc_c1", true, "0700", false);
    return snapshot(type, bindingId, ManagedDependencySecurityMode.INSECURE, identity,
        null, null, null, CONSUMER_FINGERPRINT);
  }

  private ManagedDependencySnapshot secureSnapshot(ManagedDependencyType type,
      UUID bindingId, ManagedHBaseConsumerLocalMapping mapping, String consumerFingerprint) {
    String user = mapping.proof().effectiveShortUser();
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        user, new TreeSet<>(Set.of(mapping.rolePrincipalPattern())), false,
        user, true, "0700", true);
    String policyFingerprint = type == ManagedDependencyType.HDFS ? hash('6') : hash('7');
    ManagedDependencyProviderSecurityProof provider =
        new ManagedDependencyProviderSecurityProof(
            ManagedDependencyProviderSecurityProof.SCHEMA_VERSION, type,
            type == ManagedDependencyType.HDFS
                ? ManagedDependencySecurityPolicyKind.HDFS_AUTH_TO_LOCAL
                : ManagedDependencySecurityPolicyKind.ZOOKEEPER_SASL_ID,
            mapping.realm(), policyFingerprint);
    String principalFingerprint = type == ManagedDependencyType.HDFS
        ? new ManagedHdfsAuthToLocalVerifier().fingerprintConsumerPattern(
            new ConsumerPatternInput(mapping.realm(), user, mapping.rolePrincipalPattern()))
        : new ManagedZooKeeperSaslPolicyVerifier().fingerprintConsumer(
            new ConsumerInput(mapping.realm(), user, mapping.rolePrincipalPattern(), user));
    ManagedDependencyPairSecurityProof pair = new ManagedDependencyPairSecurityProof(
        ManagedDependencyPairSecurityProof.SCHEMA_VERSION, type, provider.kind(),
        policyFingerprint, principalFingerprint, user, hash('8'));
    return snapshot(type, bindingId, ManagedDependencySecurityMode.KERBEROS, identity,
        mapping, provider, pair, consumerFingerprint);
  }

  private ManagedDependencySnapshot snapshot(ManagedDependencyType type, UUID bindingId,
      ManagedDependencySecurityMode mode, ManagedDependencyIdentity identity,
      ManagedHBaseConsumerLocalMapping mapping,
      ManagedDependencyProviderSecurityProof provider,
      ManagedDependencyPairSecurityProof pair, String consumerFingerprint) {
    ManagedDependencyNamespace namespace = type == ManagedDependencyType.HDFS
        ? ManagedDependencyNamespace.hdfs(bindingId, "hdfs://nameservice")
        : ManagedDependencyNamespace.zooKeeper(bindingId);
    ManagedDependencyVersion.Compatibility version = new ManagedDependencyVersion.Compatibility(
        "BIGTOP", "3.3.0", true, type.getProviderServiceName() + "-3.3.0",
        new TreeMap<>(Map.of("distribution", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")));
    return new ManagedDependencySnapshot(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION,
        bindingId, 7L, type,
        new ManagedDependencyServiceKey(
            type == ManagedDependencyType.HDFS ? 22L : 23L,
            type.getProviderServiceName()),
        namespace, version, mode, identity,
        type == ManagedDependencyType.HDFS
            ? new TreeMap<>(Map.of("fs.defaultFS", "hdfs://nameservice"))
            : new TreeMap<>(),
        new TreeMap<>(), type == ManagedDependencyType.ZOOKEEPER
            ? new TreeMap<>(Map.of("hbase.zookeeper.quorum", "zk.example.test"))
            : new TreeMap<>(),
        consumerFingerprint, hash('b'),
        type == ManagedDependencyType.HDFS ? hash('c') : hash('d'),
        mapping, provider, pair);
  }

  private ManagedHBaseConsumerLocalMapping consumerMapping(long identityClusterId,
      String realm, boolean includeUnrelatedRule) {
    Plan identityPlan = Plan.forExistingCluster(identityClusterId);
    String user = identityPlan.plannedShortUser();
    String smokePrincipal = "ambari-qa-plan@" + realm;
    AuthToLocalBuilder builder = new AuthToLocalBuilder(realm, "", false);
    builder.addRule(user + "/_HOST@" + realm, user);
    builder.addRule(user + "@" + realm, user);
    builder.addRule(smokePrincipal, "ambari-qa");
    if (includeUnrelatedRule) {
      builder.addRule("auxiliary@" + realm, "auxiliary");
    }
    String rules = builder.generate();
    ManagedHdfsAuthToLocalVerifier verifier = new ManagedHdfsAuthToLocalVerifier();
    ConsumerLocalMappingProof proof = verifier.proveConsumerLocalMappings(rules,
        new ConsumerLocalMappingInput(realm, user, user + "/_HOST@" + realm,
            user + "@" + realm, smokePrincipal, "ambari-qa"));
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindings = new TreeMap<>();
    bindings.put(ManagedDependencyType.HDFS,
        new ManagedBindingSnapshotRef(ManagedDependencyType.HDFS, HDFS_BINDING));
    bindings.put(ManagedDependencyType.ZOOKEEPER,
        new ManagedBindingSnapshotRef(ManagedDependencyType.ZOOKEEPER, ZOOKEEPER_BINDING));
    ManagedHBaseKerberosOverlaySpec spec = ManagedHBaseKerberosOverlaySpec.create(
        realm, user, smokePrincipal, "ambari-qa", identityPlan.planFingerprint(), bindings);
    return ManagedHBaseConsumerLocalMapping.create(spec,
        new SealedConfigurations(Map.of(
            "core-site", Map.of("hadoop.security.auth_to_local", rules)), proof));
  }

  private static String hash(char value) {
    return "sha256:" + String.valueOf(value).repeat(64);
  }

  private record ReadFixture(
      Cluster cluster,
      ServiceDependencyDAO dao,
      PersistedManagedHBaseKerberosLivePlanProvider provider,
      AtomicBoolean inReadLock) {
  }
}
