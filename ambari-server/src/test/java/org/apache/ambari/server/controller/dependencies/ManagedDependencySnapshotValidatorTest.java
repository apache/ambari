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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.ambari.server.controller.AuthToLocalBuilder;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Consumer;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ConsumerLifecycle;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.HdfsEndpoint;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Provider;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ZooKeeperEndpoint;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySnapshotValidator.ValidationResult;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity.Allocation;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity.Plan;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosDescriptorOverlay.SealedConfigurations;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec.ManagedBindingSnapshotRef;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingProof;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.Policy;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.PolicySource;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ManagedDependencySnapshotValidatorTest {
  private static final UUID BINDING_ID = UUID.fromString("cd6de25d-a556-41f3-90d8-bcec840c27f3");
  private static final String FAILOVER_PROVIDER =
      "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider";
  private static final String REALM = "EXAMPLE.COM";
  private static final String STOCK_KRB5_TEMPLATE = """
      [libdefaults]
        default_realm = {{realm}}
      """;

  @Test
  void fingerprintIgnoresDraftAndClusterProvenance() {
    ManagedDependencySnapshotValidator validator = new ManagedDependencySnapshotValidator(false);
    UUID draftId = UUID.fromString("c835202c-4a5b-4fb3-a962-286d6572bf97");
    Plan plan = Plan.forCreationDraft(17L, draftId);
    ManagedDependencyIdentity identity = insecureIdentity(plan);
    Consumer draft = consumer("DRAFT", draftId, null, null, ConsumerLifecycle.DRAFT, identity, plan);
    Consumer service = consumer("SERVICE", null, 41L, "renamed-cluster",
        ConsumerLifecycle.INIT_UNINSTALLED, identity, plan);

    assertEquals(draft, service);
    assertEquals(validator.consumerFingerprint(draft), validator.consumerFingerprint(service));
  }

  @Test
  void acceptsMirroredResolvedRepositoryAndDefensivelyCopiesInputs() throws Exception {
    TreeMap<String, String> coreSite = new TreeMap<>(Map.of(
        "fs.defaultFS", "hdfs://nn.example.test:8020",
        "hadoop.security.authentication", "simple",
        "hadoop.security.credential.provider.path", "jceks://provider-secret"));
    TreeSet<String> required = new TreeSet<>(Set.of("fs.defaultFS"));
    Provider provider = ordinaryProvider(coreSite, required, new TreeSet<>());
    coreSite.put("fs.defaultFS", "hdfs://mutated.example.test:9000");
    required.add("dfs.namenode.keytab.file");

    ValidationResult result = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, insecureConsumer(), provider, List.of());

    assertTrue(result.isValid());
    ManagedDependencySnapshot snapshot = result.snapshot().orElseThrow();
    assertEquals("hdfs://nn.example.test:8020", snapshot.coreSite().get("fs.defaultFS"));
    assertFalse(snapshot.coreSite().containsKey("hadoop.security.credential.provider.path"));
    assertThrows(UnsupportedOperationException.class,
        () -> snapshot.coreSite().put("fs.defaultFS", "hdfs://other:8020"));

    String serializedSnapshot = new ObjectMapper().writeValueAsString(snapshot);
    assertFalse(serializedSnapshot.contains("repositoryRowId"));
    assertFalse(serializedSnapshot.contains("mirrorUrls"));
    assertFalse(serializedSnapshot.contains("https://mirror-b.example/repository"));
    assertFalse(serializedSnapshot.contains("jceks://provider-secret"));
  }

  @Test
  void supportsOrdinaryNameNodeAndPrivateRootWalNamespaces() {
    ManagedDependencySnapshot snapshot = validator(false).validate(BINDING_ID, 7,
        ManagedDependencyType.HDFS, insecureConsumer(), ordinaryProvider(), List.of())
        .snapshot().orElseThrow();

    assertEquals("hdfs://nn.example.test:8020", snapshot.coreSite().get("fs.defaultFS"));
    assertEquals("hdfs://nn.example.test:8020/apps/ambari-managed/hbase/" + BINDING_ID + "/root",
        snapshot.namespace().rootUri());
    assertEquals("hdfs://nn.example.test:8020/apps/ambari-managed/hbase/" + BINDING_ID + "/wal",
        snapshot.namespace().walUri());
  }

  @Test
  void retainsStandardLogicalHaResolution() {
    ValidationResult result = validator(false).validate(BINDING_ID, 2,
        ManagedDependencyType.HDFS, insecureConsumer(), haProvider(), List.of());

    assertTrue(result.isValid());
    SortedMap<String, String> hdfsSite = result.snapshot().orElseThrow().hdfsSite();
    assertEquals("analytics", result.snapshot().orElseThrow().coreSite().get("fs.defaultFS")
        .substring("hdfs://".length()));
    assertEquals("nn1,nn2", hdfsSite.get("dfs.ha.namenodes.analytics"));
    assertEquals("nn1.example.test:8020", hdfsSite.get("dfs.namenode.rpc-address.analytics.nn1"));
    assertEquals("nn2.example.test:8020", hdfsSite.get("dfs.namenode.rpc-address.analytics.nn2"));
    assertEquals(FAILOVER_PROVIDER, hdfsSite.get("dfs.client.failover.proxy.provider.analytics"));
  }

  @Test
  void rejectsUnsupportedTopologyNamespaceAndRequiredClientProperties() {
    Provider observer = provider(ManagedDependencyType.HDFS,
        new HdfsEndpoint("hdfs://nn.example.test:8020", false, "", new TreeMap<>(), "", false, true),
        null, Map.of("fs.defaultFS", "hdfs://nn.example.test:8020"), Map.of(), Map.of(),
        new TreeSet<>(Set.of("fs.defaultFS")), new TreeSet<>());
    assertError(observer, ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED);

    Provider secretRequired = ordinaryProvider(new TreeMap<>(Map.of(
        "fs.defaultFS", "hdfs://nn.example.test:8020",
        "dfs.namenode.keytab.file", "/etc/security/keytabs/nn.service.keytab")),
        new TreeSet<>(Set.of("fs.defaultFS", "dfs.namenode.keytab.file")), new TreeSet<>());
    assertError(secretRequired, ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED);

    ManagedDependencyNamespace ancestor = new ManagedDependencyNamespace(
        "hdfs://nn.example.test:8020/apps/ambari-managed/hbase",
        "hdfs://nn.example.test:8020/other-wal", "");
    ValidationResult collision = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, insecureConsumer(), ordinaryProvider(), List.of(ancestor));
    assertTrue(collision.hasError(ManagedDependencyErrorCode.DEPENDENCY_NAMESPACE_CONFLICT));
  }

  @Test
  void rejectsMissingFeatureAndReportsUnsupportedProviderFeature() {
    Provider missingProperty = ordinaryProvider(new TreeMap<>(Map.of(
        "fs.defaultFS", "hdfs://nn.example.test:8020")),
        new TreeSet<>(Set.of("dfs.client.socket-timeout")), new TreeSet<>());
    assertError(missingProperty, ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED);

    Provider unsupported = ordinaryProvider(new TreeMap<>(Map.of(
        "fs.defaultFS", "hdfs://nn.example.test:8020")),
        new TreeSet<>(Set.of("fs.defaultFS")),
        new TreeSet<>(Set.of("CUSTOM_FAILOVER_PROVIDER")));
    assertError(unsupported, ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED);
  }

  @Test
  void rejectsMalformedOrUnboundedClientValues() {
    Provider provider = ordinaryProvider(new TreeMap<>(Map.of(
        "fs.defaultFS", "hdfs://nn.example.test:8020",
        "hadoop.security.authorization", "yes",
        "ipc.client.connect.max.retries", "100000")),
        new TreeSet<>(Set.of("fs.defaultFS")), new TreeSet<>());

    assertError(provider, ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR);
  }

  @Test
  void acceptsPinnedBigtopLocalAndWebDefaultsWithoutExportingThem() {
    Map<String, String> pinnedDefaults = Map.of(
        "dfs.client.read.shortcircuit", "true",
        "dfs.domain.socket.path", "/var/lib/hadoop-hdfs/dn_socket",
        "dfs.http.policy", "HTTP_ONLY",
        "dfs.https.port", "50470",
        "dfs.permissions.enabled", "true");
    Provider ordinary = provider(ManagedDependencyType.HDFS,
        new HdfsEndpoint("hdfs://nn.example.test:8020", false, "", new TreeMap<>(), "", false, false),
        null, Map.of("fs.defaultFS", "hdfs://nn.example.test:8020"), pinnedDefaults, Map.of(),
        new TreeSet<>(Set.of("fs.defaultFS")), new TreeSet<>());
    TreeMap<String, String> haDefaults = new TreeMap<>(pinnedDefaults);
    haDefaults.put("dfs.nameservices", "analytics");
    Provider ha = provider(ManagedDependencyType.HDFS,
        new HdfsEndpoint("hdfs://analytics", true, "analytics", Map.of(
            "nn1", "nn1.example.test:8020", "nn2", "nn2.example.test:8020"),
            FAILOVER_PROVIDER, false, false),
        null, Map.of("fs.defaultFS", "hdfs://analytics"), haDefaults, Map.of(),
        new TreeSet<>(Set.of("fs.defaultFS", "dfs.client.failover.proxy.provider.analytics")),
        new TreeSet<>());

    ManagedDependencySnapshot ordinarySnapshot = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, insecureConsumer(), ordinary, List.of()).snapshot().orElseThrow();
    ManagedDependencySnapshot haSnapshot = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, insecureConsumer(), ha, List.of()).snapshot().orElseThrow();

    assertFalse(ordinarySnapshot.hdfsSite().containsKey("dfs.client.read.shortcircuit"));
    assertFalse(ordinarySnapshot.hdfsSite().containsKey("dfs.domain.socket.path"));
    assertFalse(ordinarySnapshot.hdfsSite().containsKey("dfs.http.policy"));
    assertFalse(ordinarySnapshot.hdfsSite().containsKey("dfs.https.port"));
    assertFalse(ordinarySnapshot.hdfsSite().containsKey("dfs.permissions.enabled"));
    assertEquals("analytics", haSnapshot.hdfsSite().get("dfs.nameservices"));
    assertEquals(FAILOVER_PROVIDER,
        haSnapshot.hdfsSite().get("dfs.client.failover.proxy.provider.analytics"));
  }

  @Test
  void rejectsProviderWithHdfsPermissionEnforcementDisabled() {
    Provider provider = provider(ManagedDependencyType.HDFS,
        new HdfsEndpoint("hdfs://nn.example.test:8020", false, "", new TreeMap<>(), "", false, false),
        null, Map.of("fs.defaultFS", "hdfs://nn.example.test:8020"),
        Map.of("dfs.permissions.enabled", "false"), Map.of(),
        new TreeSet<>(Set.of("fs.defaultFS")), new TreeSet<>());

    assertError(provider, ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED);
  }

  @Test
  void detectsEffectiveUnsupportedFeaturesFromResolvedProviderConfig() {
    Provider provider = provider(ManagedDependencyType.HDFS,
        new HdfsEndpoint("hdfs://nn.example.test:8020", false, "", new TreeMap<>(), "", false, false),
        null, Map.of(
            "fs.defaultFS", "hdfs://nn.example.test:8020",
            "hadoop.security.authentication.provider", "com.example.CustomProvider"),
        Map.of("dfs.encryption.key.provider.uri", "kms://https@kms.example.test:9600/kms"), Map.of(),
        new TreeSet<>(Set.of("fs.defaultFS")), new TreeSet<>());

    assertError(provider, ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED);

    Provider secureZooKeeperTransport = provider(ManagedDependencyType.ZOOKEEPER, null,
        new ZooKeeperEndpoint(List.of("zk1.example.test"), 2181, false, "", false, List.of()),
        Map.of(), Map.of(), Map.of("zookeeper.client.secure", "true"),
        new TreeSet<>(Set.of("hbase.zookeeper.quorum")), new TreeSet<>());
    ValidationResult zooKeeperResult = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.ZOOKEEPER, insecureConsumer(), secureZooKeeperTransport, List.of());
    assertTrue(zooKeeperResult.hasError(ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED));

    Provider requiredShortCircuit = ordinaryProvider(new TreeMap<>(Map.of(
        "fs.defaultFS", "hdfs://nn.example.test:8020",
        "dfs.client.read.shortcircuit", "true")),
        new TreeSet<>(Set.of("fs.defaultFS", "dfs.client.read.shortcircuit")), new TreeSet<>());
    assertError(requiredShortCircuit, ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED);
  }

  @Test
  void rejectsUnsupportedResolvedStack() {
    Provider ordinary = ordinaryProvider();
    ManagedDependencyVersion wrongStack = new ManagedDependencyVersion(
        "BIGTOP", "3.2.0", true, "HDFS-3.2.0",
        new TreeMap<>(Map.of("repository.version", "3.2.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 71L, List.of("https://mirror.example/repo"));
    Provider unsupported = new Provider(ordinary.serviceKey(), ordinary.type(), wrongStack,
        true, true, ordinary.securityMode(), ordinary.realm(), ordinary.identity(),
        ordinary.hdfsEndpoint(), null, ordinary.coreSite(), ordinary.hdfsSite(), Map.of(),
        ordinary.requiredClientProperties(), new TreeSet<>());

    assertError(unsupported, ManagedDependencyErrorCode.DEPENDENCY_VERSION_UNSUPPORTED);
  }

  @Test
  void rejectsSharedHadoopGroupModeEvenForInitialInsecureLane() {
    Plan plan = Plan.forExistingCluster(1L);
    ManagedDependencyIdentity sharedGroup = new ManagedDependencyIdentity(
        plan.plannedShortUser(), new TreeSet<>(), false, "hadoop", false, "0750", false);
    Consumer consumer = consumer("SERVICE", null, 1L, "consumer-a",
        ConsumerLifecycle.INIT_UNINSTALLED, sharedGroup, plan);

    ValidationResult result = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, consumer, ordinaryProvider(), List.of());

    assertTrue(result.hasError(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED));
  }

  @Test
  void rejectsInstalledLocalHbaseAndIdentityCollision() {
    Plan plan = Plan.forExistingCluster(1L);
    ManagedDependencyIdentity identity = insecureIdentity(plan);
    Consumer installedLocal = consumer("SERVICE", null, 1L, "consumer-a",
        ConsumerLifecycle.INSTALLED_LOCAL, identity, plan);
    ValidationResult installed = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, installedLocal, ordinaryProvider(), List.of());
    assertTrue(installed.hasError(ManagedDependencyErrorCode.DEPENDENCY_LOCAL_DATA_MIGRATION_UNSUPPORTED));

    Consumer fresh = consumer("SERVICE", null, 1L, "consumer-a",
        ConsumerLifecycle.INIT_UNINSTALLED, identity, plan);
    Allocation anotherOwner = new Allocation(identity.effectiveShortUser(),
        Plan.forExistingCluster(2L).planFingerprint());
    ValidationResult collision = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, fresh, ordinaryProvider(), List.of(),
        List.of(anotherOwner), null);
    assertTrue(collision.hasError(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED));

    Allocation sameConsumer = new Allocation(identity.effectiveShortUser(), plan.planFingerprint());
    ValidationResult reusedBySameConsumer = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, fresh, ordinaryProvider(), List.of(),
        List.of(sameConsumer), null);
    assertTrue(reusedBySameConsumer.isValid());
  }

  @Test
  void managedUpdateRetainsBindingNamespaceProviderAndIdentity() {
    ManagedDependencySnapshot current = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, insecureConsumer(), ordinaryProvider(), List.of())
        .snapshot().orElseThrow();
    Plan plan = Plan.forExistingCluster(1L);
    Consumer update = consumer("SERVICE", null, 1L, "consumer-renamed",
        ConsumerLifecycle.MANAGED_UPDATE, insecureIdentity(plan), plan);

    ValidationResult accepted = validator(false).validate(BINDING_ID, 2,
        ManagedDependencyType.HDFS, update, ordinaryProvider(), List.of(current.namespace()),
        List.of(), current);
    assertTrue(accepted.isValid());

    ValidationResult replacement = validator(false).validate(UUID.randomUUID(), 2,
        ManagedDependencyType.HDFS, update, ordinaryProvider(), List.of(current.namespace()),
        List.of(), current);
    assertTrue(replacement.hasError(ManagedDependencyErrorCode.INVALID_CONSUMER_DESCRIPTOR));
  }

  @Test
  void rejectsMixedCrossRealmAndInterimSameRealmSecurity() {
    Consumer secureConsumer = secureConsumer(uniqueSecureIdentity(), "EXAMPLE.COM");
    Provider insecureProvider = ordinaryProvider();
    ValidationResult mixed = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, secureConsumer, insecureProvider, List.of());
    assertTrue(mixed.hasError(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH));

    Provider crossRealm = secureHdfsProvider(uniqueSecureIdentity(), "OTHER.EXAMPLE");
    ValidationResult cross = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, secureConsumer, crossRealm, List.of());
    assertTrue(cross.hasError(ManagedDependencyErrorCode.CROSS_REALM_NOT_SUPPORTED));

    Provider sameRealm = secureHdfsProvider(uniqueSecureIdentity(), "EXAMPLE.COM");
    ValidationResult interim = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, secureConsumer, sameRealm, List.of());
    assertTrue(interim.hasError(ManagedDependencyErrorCode.SAME_REALM_BINDING_NOT_AVAILABLE));

    TreeMap<String, String> unsafeCoreSite = new TreeMap<>(sameRealm.coreSite());
    unsafeCoreSite.put("ipc.client.fallback-to-simple-auth-allowed", "true");
    Provider fallback = new Provider(sameRealm.serviceKey(), sameRealm.type(), sameRealm.version(),
        true, true, sameRealm.securityMode(), sameRealm.realm(), sameRealm.identity(),
        sameRealm.hdfsEndpoint(), null, unsafeCoreSite, sameRealm.hdfsSite(), Map.of(),
        sameRealm.requiredClientProperties(), new TreeSet<>());
    ValidationResult unsafe = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, secureConsumer, fallback, List.of());
    assertTrue(unsafe.hasError(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH));
  }

  @Test
  void secureLaneRejectsSharedHbaseIdentityAndSharedHadoopGroup() {
    ManagedDependencyIdentity sharedIdentity = new ManagedDependencyIdentity(
        "hbase", new TreeSet<>(Set.of("hbase/_HOST@EXAMPLE.COM")), false,
        "hadoop", false, "0750", true);
    ValidationResult result = validator(true).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, secureConsumer(sharedIdentity, "EXAMPLE.COM"),
        secureHdfsProvider(sharedIdentity, "EXAMPLE.COM"), List.of());

    assertTrue(result.hasError(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED));
  }

  @Test
  void reservesIndependentZooKeeperRootAndRequiresParentAclCapability() {
    ManagedDependencyIdentity secureIdentity = uniqueSecureIdentity();
    Provider providerWithoutParentAcl = secureZooKeeperProvider(secureIdentity, false, List.of());
    ValidationResult missingAcl = validator(true).validate(BINDING_ID, 1,
        ManagedDependencyType.ZOOKEEPER, secureConsumer(secureIdentity, "EXAMPLE.COM"),
        providerWithoutParentAcl, List.of());
    assertTrue(missingAcl.hasError(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED));

    Provider conflictingParent = secureZooKeeperProvider(secureIdentity, true,
        List.of("/ambari-managed-hbase/existing-app"));
    ValidationResult conflict = validator(true).validate(BINDING_ID, 1,
        ManagedDependencyType.ZOOKEEPER, secureConsumer(secureIdentity, "EXAMPLE.COM"),
        conflictingParent, List.of());
    assertTrue(conflict.hasError(ManagedDependencyErrorCode.DEPENDENCY_NAMESPACE_CONFLICT));

    Provider provider = secureZooKeeperProvider(secureIdentity, true, List.of("/hbase-unmanaged"));
    ManagedDependencySnapshot snapshot = validator(true).validate(BINDING_ID, 1,
        ManagedDependencyType.ZOOKEEPER, secureConsumer(secureIdentity, "EXAMPLE.COM"),
        provider, List.of(), List.of(), null,
        zooKeeperSecurityContext(secureIdentity, provider)).snapshot().orElseThrow();
    assertEquals("/ambari-managed-hbase/" + BINDING_ID + "/hbase",
        snapshot.namespace().znode());

    ZooKeeperEndpoint unsupportedMapping = new ZooKeeperEndpoint(
        provider.zooKeeperEndpoint().quorumHosts(), provider.zooKeeperEndpoint().clientPort(),
        true, "zookeeper", true, List.of("/hbase-unmanaged"), true, false, "DEFAULT");
    Provider unsafeMapping = new Provider(provider.serviceKey(), provider.type(), provider.version(),
        true, true, provider.securityMode(), provider.realm(), provider.identity(), null,
        unsupportedMapping, provider.coreSite(), provider.hdfsSite(), provider.zooKeeperClient(),
        provider.requiredClientProperties(), provider.unsupportedFeatures());
    ValidationResult mappingResult = validator(true).validate(BINDING_ID, 1,
        ManagedDependencyType.ZOOKEEPER, secureConsumer(secureIdentity, "EXAMPLE.COM"),
        unsafeMapping, List.of());
    assertTrue(mappingResult.hasError(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED));
  }

  @Test
  void secureHdfsRequiresAndPersistsExactTypedProofsWithoutProviderRules() throws Exception {
    ManagedDependencyIdentity identity = uniqueSecureIdentity();
    Consumer consumer = secureConsumer(identity, REALM);
    Provider provider = secureHdfsProvider(identity, REALM);
    ManagedDependencySecurityValidationContext context = hdfsSecurityContext(identity);

    ValidationResult missing = validator(true).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, consumer, provider, List.of());
    assertTrue(missing.hasError(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_PROOF_MISSING));

    ManagedDependencySnapshot snapshot = validator(true).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, consumer, provider, List.of(), List.of(), null, context)
        .snapshot().orElseThrow();
    String json = new ObjectMapper().writeValueAsString(snapshot);

    assertEquals(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION, snapshot.schemaVersion());
    assertEquals(context.consumerMapping(), snapshot.consumerLocalMapping());
    assertEquals(context.providerPolicy(), snapshot.providerSecurity());
    assertEquals(identity.effectiveShortUser(), snapshot.pairSecurity().expectedAuthorizationId());
    assertFalse(json.contains(STOCK_KRB5_TEMPLATE));
    assertFalse(json.contains("https://mirror-b.example/repository"));
    assertFalse(json.contains("krb5ConfTemplate"));

    ManagedDependencyPairSecurityProof wrongPrincipal = new ManagedDependencyPairSecurityProof(
        snapshot.pairSecurity().schemaVersion(), snapshot.type(), snapshot.pairSecurity().kind(),
        snapshot.pairSecurity().providerPolicyFingerprint(), "sha256:" + "0".repeat(64),
        snapshot.pairSecurity().expectedAuthorizationId(),
        snapshot.pairSecurity().proofFingerprint());
    assertThrows(IllegalArgumentException.class,
        () -> new ManagedDependencySnapshot(snapshot.schemaVersion(), snapshot.bindingId(),
            snapshot.snapshotVersion(), snapshot.type(), snapshot.providerService(),
            snapshot.namespace(), snapshot.providerVersion(), snapshot.securityMode(),
            snapshot.consumerIdentity(), snapshot.coreSite(), snapshot.hdfsSite(),
            snapshot.zooKeeperClient(), snapshot.consumerFingerprint(),
            snapshot.providerFingerprint(), snapshot.snapshotFingerprint(),
            snapshot.consumerLocalMapping(), snapshot.providerSecurity(), wrongPrincipal));
  }

  @Test
  void secureLaneRejectsLegacyBooleanEvenWithTypedProofs() {
    ManagedDependencyIdentity current = uniqueSecureIdentity();
    ManagedDependencyIdentity legacyBoolean = new ManagedDependencyIdentity(
        current.effectiveShortUser(), current.principalPatterns(), true,
        current.filesystemGroup(), current.distinctConsumerGroup(), current.directoryMode(),
        current.privateZooKeeperAcl());

    ValidationResult result = validator(true).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, secureConsumer(legacyBoolean, REALM),
        secureHdfsProvider(legacyBoolean, REALM), List.of(), List.of(), null,
        hdfsSecurityContext(legacyBoolean));

    assertTrue(result.hasError(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED));
  }

  @Test
  void secureFingerprintIsProvenanceIndependentAndChangesWithMapping() {
    ManagedDependencyIdentity identity = uniqueSecureIdentity();
    Plan plan = Plan.forExistingCluster(1L);
    Consumer planned = new Consumer("SERVICE_PLAN", null, 1L, "before", "HBASE",
        ConsumerLifecycle.DRAFT, version("HBASE", 11L, "https://mirror-a.example/repo"),
        ManagedDependencySecurityMode.KERBEROS, REALM, identity, plan);
    Consumer live = new Consumer("SERVICE", null, 1L, "after", "HBASE",
        ConsumerLifecycle.INIT_UNINSTALLED,
        version("HBASE", 11L, "https://mirror-a.example/repo"),
        ManagedDependencySecurityMode.KERBEROS, REALM, identity, plan);
    ManagedHBaseConsumerLocalMapping mapping = consumerMapping(identity, ManagedDependencyType.HDFS);
    Provider provider = secureHdfsProvider(identity, REALM);
    ManagedDependencySecurityValidationContext context = hdfsSecurityContext(identity, mapping);

    ManagedDependencySnapshot plannedSnapshot = validator(true).validate(BINDING_ID, 9,
        ManagedDependencyType.HDFS, planned, provider, List.of(), List.of(), null, context)
        .snapshot().orElseThrow();
    ManagedDependencySnapshot liveSnapshot = validator(true).validate(BINDING_ID, 9,
        ManagedDependencyType.HDFS, live, provider, List.of(), List.of(), null, context)
        .snapshot().orElseThrow();

    assertEquals(validator(true).consumerFingerprint(planned, mapping),
        validator(true).consumerFingerprint(live, mapping));
    assertEquals(plannedSnapshot.snapshotFingerprint(), liveSnapshot.snapshotFingerprint());

    ManagedHBaseConsumerLocalMapping changedMapping = consumerMapping(
        identity, ManagedDependencyType.HDFS, true);
    ManagedDependencySnapshot changedSnapshot = validator(true).validate(BINDING_ID, 9,
        ManagedDependencyType.HDFS, live, provider, List.of(), List.of(), null,
        hdfsSecurityContext(identity, changedMapping)).snapshot().orElseThrow();
    assertNotEquals(liveSnapshot.consumerFingerprint(), changedSnapshot.consumerFingerprint());
    assertNotEquals(liveSnapshot.snapshotFingerprint(), changedSnapshot.snapshotFingerprint());
  }

  @Test
  void schemaOneAcceptsOnlyInsecureSnapshots() {
    ManagedDependencySnapshot current = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, insecureConsumer(), ordinaryProvider(), List.of())
        .snapshot().orElseThrow();
    ManagedDependencySnapshot legacy = new ManagedDependencySnapshot(
        ManagedDependencySnapshot.LEGACY_INSECURE_SCHEMA_VERSION, current.bindingId(),
        current.snapshotVersion(), current.type(), current.providerService(), current.namespace(),
        current.providerVersion(), current.securityMode(), current.consumerIdentity(),
        current.coreSite(), current.hdfsSite(), current.zooKeeperClient(),
        current.consumerFingerprint(), current.providerFingerprint(), current.snapshotFingerprint());
    assertTrue(legacy.isLegacyInsecure());
    String serialized = StageUtils.getGson().toJson(legacy);
    assertTrue(StageUtils.getGson().fromJson(serialized, ManagedDependencySnapshot.class)
        .isLegacyInsecure());

    ManagedDependencyIntegrationException secureError = assertThrows(
        ManagedDependencyIntegrationException.class, () -> new ManagedDependencySnapshot(
            ManagedDependencySnapshot.LEGACY_INSECURE_SCHEMA_VERSION, current.bindingId(),
            current.snapshotVersion(), current.type(), current.providerService(), current.namespace(),
            current.providerVersion(), ManagedDependencySecurityMode.KERBEROS,
            uniqueSecureIdentity(), current.coreSite(), current.hdfsSite(), current.zooKeeperClient(),
            current.consumerFingerprint(), current.providerFingerprint(),
            current.snapshotFingerprint()));
    assertEquals(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED.name(),
        secureError.getCode());
  }

  private void assertError(Provider provider, ManagedDependencyErrorCode errorCode) {
    ValidationResult result = validator(false).validate(BINDING_ID, 1,
        ManagedDependencyType.HDFS, insecureConsumer(), provider, List.of());
    assertTrue(result.hasError(errorCode), () -> "Expected " + errorCode + " but got " + result.issues());
  }

  private ManagedDependencySnapshotValidator validator(boolean sameRealmSecureEnabled) {
    return new ManagedDependencySnapshotValidator(sameRealmSecureEnabled);
  }

  private Consumer insecureConsumer() {
    Plan plan = Plan.forExistingCluster(1L);
    return consumer("SERVICE", null, 1L, "consumer-a",
        ConsumerLifecycle.INIT_UNINSTALLED, insecureIdentity(plan), plan);
  }

  private Consumer consumer(String scope, UUID draftId, Long clusterId, String clusterName,
      ConsumerLifecycle lifecycle, ManagedDependencyIdentity identity, Plan identityPlan) {
    return new Consumer(scope, draftId, clusterId, clusterName, "HBASE", lifecycle,
        version("HBASE", 11L, "https://mirror-a.example/repo"),
        ManagedDependencySecurityMode.INSECURE, "", identity, identityPlan);
  }

  private Consumer secureConsumer(ManagedDependencyIdentity identity, String realm) {
    return new Consumer("SERVICE", null, 1L, "consumer-a", "HBASE",
        ConsumerLifecycle.INIT_UNINSTALLED, version("HBASE", 11L,
        "https://mirror-a.example/repo"), ManagedDependencySecurityMode.KERBEROS,
        realm, identity, Plan.forExistingCluster(1L));
  }

  private Provider ordinaryProvider() {
    return ordinaryProvider(new TreeMap<>(Map.of(
        "fs.defaultFS", "hdfs://nn.example.test:8020",
        "hadoop.security.authentication", "simple")),
        new TreeSet<>(Set.of("fs.defaultFS")), new TreeSet<>());
  }

  private Provider ordinaryProvider(TreeMap<String, String> coreSite,
      TreeSet<String> required, TreeSet<String> unsupported) {
    return provider(ManagedDependencyType.HDFS,
        new HdfsEndpoint("hdfs://nn.example.test:8020", false, "", new TreeMap<>(), "", false, false),
        null, coreSite, Map.of("dfs.client.use.datanode.hostname", "true"), Map.of(),
        required, unsupported);
  }

  private Provider haProvider() {
    TreeMap<String, String> addresses = new TreeMap<>(Map.of(
        "nn1", "nn1.example.test:8020", "nn2", "nn2.example.test:8020"));
    return provider(ManagedDependencyType.HDFS,
        new HdfsEndpoint("hdfs://analytics", true, "analytics", addresses,
            FAILOVER_PROVIDER, false, false),
        null, Map.of("fs.defaultFS", "hdfs://analytics"), Map.of(), Map.of(),
        new TreeSet<>(Set.of("fs.defaultFS", "dfs.client.failover.proxy.provider.analytics")),
        new TreeSet<>());
  }

  private Provider secureHdfsProvider(ManagedDependencyIdentity identity, String realm) {
    Provider insecure = ordinaryProvider();
    TreeMap<String, String> coreSite = new TreeMap<>(insecure.coreSite());
    coreSite.put("hadoop.security.authentication", "kerberos");
    coreSite.put("ipc.client.fallback-to-simple-auth-allowed", "false");
    TreeMap<String, String> hdfsSite = new TreeMap<>(insecure.hdfsSite());
    hdfsSite.put("dfs.namenode.kerberos.principal", "nn/_HOST@" + realm);
    hdfsSite.put("dfs.datanode.kerberos.principal", "dn/_HOST@" + realm);
    return new Provider(insecure.serviceKey(), insecure.type(), insecure.version(), true, true,
        ManagedDependencySecurityMode.KERBEROS, realm, identity, insecure.hdfsEndpoint(), null,
        coreSite, hdfsSite, Map.of(), insecure.requiredClientProperties(), new TreeSet<>());
  }

  private Provider secureZooKeeperProvider(ManagedDependencyIdentity identity,
      boolean parentAclSupported, List<String> applicationParents) {
    return new Provider(new ManagedDependencyServiceKey(2L, "ZOOKEEPER"),
        ManagedDependencyType.ZOOKEEPER, version("ZOOKEEPER", 99L, "https://mirror-b.example/repo"),
        true, true, ManagedDependencySecurityMode.KERBEROS, "EXAMPLE.COM", identity, null,
        new ZooKeeperEndpoint(List.of("zk1.example.test", "zk2.example.test"), 2181,
            true, "zookeeper", parentAclSupported, applicationParents,
            true, true, "DEFAULT"),
        Map.of(), Map.of(), Map.of(
            "zookeeper.sasl.client", "true",
            "zookeeper.sasl.client.username", "zookeeper",
            "zookeeper.sasl.clientconfig", "Client"),
        new TreeSet<>(Set.of("hbase.zookeeper.quorum")), new TreeSet<>());
  }

  private Provider provider(ManagedDependencyType type, HdfsEndpoint hdfsEndpoint,
      ZooKeeperEndpoint zooKeeperEndpoint, Map<String, String> coreSite,
      Map<String, String> hdfsSite, Map<String, String> zooKeeperClient,
      SortedSet<String> required, SortedSet<String> unsupported) {
    return new Provider(new ManagedDependencyServiceKey(2L, type.getProviderServiceName()), type,
        version(type.getProviderServiceName(), 73L, "https://mirror-b.example/repository"),
        true, true, ManagedDependencySecurityMode.INSECURE, "", null,
        hdfsEndpoint, zooKeeperEndpoint, coreSite, hdfsSite, zooKeeperClient, required, unsupported);
  }

  private ManagedDependencyVersion version(String service, long rowId, String mirror) {
    return new ManagedDependencyVersion("BIGTOP", "3.3.0", true, service + "-3.3.0",
        new TreeMap<>(Map.of("repository.version", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), rowId, new ArrayList<>(List.of(mirror)));
  }

  private ManagedDependencyIdentity insecureIdentity() {
    return insecureIdentity(Plan.forExistingCluster(1L));
  }

  private ManagedDependencyIdentity insecureIdentity(Plan plan) {
    return new ManagedDependencyIdentity(plan.plannedShortUser(), new TreeSet<>(), false,
        plan.plannedShortUser(), true, "0700", false);
  }

  private ManagedDependencyIdentity uniqueSecureIdentity() {
    Plan plan = Plan.forExistingCluster(1L);
    return new ManagedDependencyIdentity(plan.plannedShortUser(),
        new TreeSet<>(Set.of(plan.plannedShortUser() + "/_HOST@EXAMPLE.COM")), false,
        plan.plannedShortUser(), true, "0700", true);
  }

  private ManagedDependencySecurityValidationContext hdfsSecurityContext(
      ManagedDependencyIdentity identity) {
    return hdfsSecurityContext(identity,
        consumerMapping(identity, ManagedDependencyType.HDFS));
  }

  private ManagedDependencySecurityValidationContext hdfsSecurityContext(
      ManagedDependencyIdentity identity, ManagedHBaseConsumerLocalMapping mapping) {
    ManagedHdfsAuthToLocalVerifier verifier = new ManagedHdfsAuthToLocalVerifier();
    String providerRules = new AuthToLocalBuilder(REALM, "", false).generate();
    PolicySource source = new PolicySource(providerRules, "", true, REALM, true, null,
        "/etc", STOCK_KRB5_TEMPLATE, verifier.fingerprintTemplate(STOCK_KRB5_TEMPLATE));
    Policy policy = verifier.inspectPolicy(source);
    return new ManagedDependencySecurityValidationContext(
        mapping, ManagedDependencyProviderSecurityProof.forHdfs(policy), source);
  }

  private ManagedDependencySecurityValidationContext zooKeeperSecurityContext(
      ManagedDependencyIdentity identity, Provider provider) {
    ZooKeeperEndpoint endpoint = provider.zooKeeperEndpoint();
    ManagedZooKeeperSaslPolicyVerifier verifier = new ManagedZooKeeperSaslPolicyVerifier();
    ManagedZooKeeperSaslPolicyVerifier.Policy policy = verifier.inspectPolicy(
        new ManagedZooKeeperSaslPolicyVerifier.PolicySource(provider.realm(),
            endpoint.saslEnabled(), endpoint.saslServiceName(),
            endpoint.managedParentAclSupported(), endpoint.kerberosRemoveHostFromPrincipal(),
            endpoint.kerberosRemoveRealmFromPrincipal(), endpoint.kerberosAuthToLocalRules()));
    return new ManagedDependencySecurityValidationContext(
        consumerMapping(identity, ManagedDependencyType.ZOOKEEPER),
        ManagedDependencyProviderSecurityProof.forZooKeeper(policy), null);
  }

  private ManagedHBaseConsumerLocalMapping consumerMapping(
      ManagedDependencyIdentity identity, ManagedDependencyType type) {
    return consumerMapping(identity, type, false);
  }

  private ManagedHBaseConsumerLocalMapping consumerMapping(
      ManagedDependencyIdentity identity, ManagedDependencyType type,
      boolean includeUnrelatedRule) {
    String user = identity.effectiveShortUser();
    String smokePrincipal = "ambari-qa-plan@" + REALM;
    String smokeUser = "ambari-qa";
    AuthToLocalBuilder builder = new AuthToLocalBuilder(REALM, "", false);
    builder.addRule(user + "/_HOST@" + REALM, user);
    builder.addRule(user + "@" + REALM, user);
    builder.addRule(smokePrincipal, smokeUser);
    if (includeUnrelatedRule) {
      builder.addRule("auxiliary@" + REALM, "auxiliary");
    }
    String rules = builder.generate();
    ManagedHdfsAuthToLocalVerifier verifier = new ManagedHdfsAuthToLocalVerifier();
    ConsumerLocalMappingProof proof = verifier.proveConsumerLocalMappings(rules,
        new ConsumerLocalMappingInput(REALM, user, user + "/_HOST@" + REALM,
            user + "@" + REALM, smokePrincipal, smokeUser));
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindings = new TreeMap<>();
    bindings.put(type, new ManagedBindingSnapshotRef(type, BINDING_ID));
    ManagedHBaseKerberosOverlaySpec spec = ManagedHBaseKerberosOverlaySpec.create(
        REALM, user, smokePrincipal, smokeUser,
        Plan.forExistingCluster(1L).planFingerprint(), bindings);
    return ManagedHBaseConsumerLocalMapping.create(spec,
        new SealedConfigurations(Map.of(
            "core-site", Map.of("hadoop.security.auth_to_local", rules)), proof));
  }
}
