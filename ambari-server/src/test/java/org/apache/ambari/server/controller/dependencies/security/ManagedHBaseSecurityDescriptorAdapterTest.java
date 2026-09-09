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
package org.apache.ambari.server.controller.dependencies.security;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.KerberosHelperImpl;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Consumer;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ConsumerLifecycle;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.HdfsEndpoint;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Provider;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ZooKeeperEndpoint;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyErrorCode;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity.Plan;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySecurityMode;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySecurityValidationContext;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyServiceKey;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySnapshot;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySnapshotValidator;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySnapshotValidator.ValidationResult;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyVersion;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec.ManagedBindingSnapshotRef;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseSecurityDescriptorAdapter.Resolution;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.PolicySource;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.VariableReplacementHelper;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.jupiter.api.Test;

class ManagedHBaseSecurityDescriptorAdapterTest {
  private static final String REALM = "EXAMPLE.COM";
  private static final String STOCK_TEMPLATE = """
      [libdefaults]
        forwardable = true
        default_realm = {{realm}}
      """;
  private static final UUID HDFS_BINDING =
      UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID ZOOKEEPER_BINDING =
      UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final StackId STACK_ID = new StackId("BIGTOP-3.2.0");

  private final KerberosDescriptorFactory descriptorFactory = new KerberosDescriptorFactory();
  private final ManagedDependencySnapshotValidator validator =
      new ManagedDependencySnapshotValidator(true);

  @Test
  void oneCalculationBuildsBothContextsAndLeavesInputsUnchanged() throws Exception {
    CountingKerberosHelper helper = realHelper();
    ManagedHBaseSecurityDescriptorAdapter adapter =
        new ManagedHBaseSecurityDescriptorAdapter(helper);
    Cluster cluster = secureCluster(11L);
    Consumer consumer = consumer("SERVICE_PLAN", ConsumerLifecycle.DRAFT);
    KerberosDescriptor raw = compositeDescriptor();
    Map<String, Object> rawBefore = raw.toMap();
    Map<String, Map<String, String>> configurations = configurations();
    Map<String, Map<String, String>> configurationsBefore = mutableCopy(configurations);
    Map<String, Set<String>> services = managedOnlyServices();
    Map<String, Set<String>> servicesBefore = mutableServiceCopy(services);
    SortedMap<ManagedDependencyType, Provider> providers = providers(true, true);
    SortedMap<ManagedDependencyType, Provider> providersBefore = new TreeMap<>(providers);
    ManagedHBaseKerberosOverlaySpec spec = overlay(true, true);
    PolicySource hdfsPolicy = hdfsPolicy(REALM, false);

    Resolution resolution = adapter.resolve(cluster, consumer, raw, null,
        configurations, services, spec, providers, hdfsPolicy);

    assertEquals(1, helper.calculationCount);
    assertEquals(Set.of(ManagedDependencyType.HDFS, ManagedDependencyType.ZOOKEEPER),
        resolution.types());
    ManagedDependencySecurityValidationContext hdfs =
        resolution.contextFor(ManagedDependencyType.HDFS);
    ManagedDependencySecurityValidationContext zooKeeper =
        resolution.contextFor(ManagedDependencyType.ZOOKEEPER);
    assertSame(resolution.consumerCalculation().consumerLocalMapping(),
        hdfs.consumerMapping());
    assertSame(hdfs.consumerMapping(), zooKeeper.consumerMapping());
    assertSame(hdfsPolicy, hdfs.hdfsPolicySource());
    assertNull(zooKeeper.hdfsPolicySource());
    assertEquals(ManagedDependencyType.HDFS, hdfs.providerPolicy().type());
    assertEquals(ManagedDependencyType.ZOOKEEPER, zooKeeper.providerPolicy().type());
    assertTrue(snapshot(HDFS_BINDING, consumer, providers.get(ManagedDependencyType.HDFS),
        hdfs).isValid());
    assertTrue(snapshot(ZOOKEEPER_BINDING, consumer,
        providers.get(ManagedDependencyType.ZOOKEEPER), zooKeeper).isValid());
    assertEquals(rawBefore, raw.toMap());
    assertEquals(configurationsBefore, configurations);
    assertEquals(servicesBefore, services);
    assertEquals(providersBefore, providers);
    assertFalse(StageUtils.getGson().toJson(resolution).contains("RULE:"));
    assertFalse(resolution.toString().contains(hdfsPolicy.effectiveRules()));
  }

  @Test
  void planAndLiveDescriptorsProduceTheSameMappingAndSnapshot() throws Exception {
    ManagedHBaseSecurityDescriptorAdapter adapter =
        new ManagedHBaseSecurityDescriptorAdapter(realHelper());
    Cluster cluster = secureCluster(11L);
    Consumer planned = consumer("SERVICE_PLAN", ConsumerLifecycle.DRAFT);
    Consumer live = consumer("SERVICE", ConsumerLifecycle.INIT_UNINSTALLED);
    SortedMap<ManagedDependencyType, Provider> providers = providers(true, false);
    Provider provider = providers.get(ManagedDependencyType.HDFS);
    ManagedHBaseKerberosOverlaySpec spec = overlay(true, false);
    PolicySource policy = hdfsPolicy(REALM, false);

    Resolution plannedResolution = adapter.resolve(cluster, planned, compositeDescriptor(), null,
        configurations(), managedOnlyServices(), spec, providers, policy);
    Resolution liveResolution = adapter.resolve(cluster, live, compositeDescriptor(), null,
        configurations(), managedOnlyServices(), spec, providers, policy);
    ManagedDependencySnapshot plannedSnapshot = snapshot(HDFS_BINDING, planned, provider,
        plannedResolution.contextFor(ManagedDependencyType.HDFS)).snapshot().orElseThrow();
    ManagedDependencySnapshot liveSnapshot = snapshot(HDFS_BINDING, live, provider,
        liveResolution.contextFor(ManagedDependencyType.HDFS)).snapshot().orElseThrow();

    assertEquals(plannedResolution.consumerCalculation().consumerLocalMapping(),
        liveResolution.consumerCalculation().consumerLocalMapping());
    assertEquals(plannedSnapshot.consumerFingerprint(), liveSnapshot.consumerFingerprint());
    assertEquals(plannedSnapshot.snapshotFingerprint(), liveSnapshot.snapshotFingerprint());
  }

  @Test
  void authoritativeLocalCounterpartControlsOnlyConsumerRulesAndAdminReference()
      throws Exception {
    ManagedHBaseSecurityDescriptorAdapter adapter =
        new ManagedHBaseSecurityDescriptorAdapter(realHelper());
    Cluster cluster = secureCluster(11L);
    Consumer consumer = consumer("SERVICE_PLAN", ConsumerLifecycle.DRAFT);

    Resolution managedZooKeeper = adapter.resolve(cluster, consumer, compositeDescriptor(), null,
        configurations(), servicesWithLocalHdfs(), overlay(false, true), providers(false, true),
        null);
    assertTrue(managedZooKeeper.consumerCalculation().consumerLocalMapping().canonicalRules()
        .contains("/hdfs/"));
    assertEquals("/HDFS/NAMENODE/hdfs",
        managedZooKeeper.consumerCalculation().detachedDescriptor().getService("HBASE")
            .getComponent("HBASE_MASTER").getIdentity("hbase_hbase_master_hdfs")
            .getReference());

    Resolution managedHdfs = adapter.resolve(cluster, consumer, compositeDescriptor(), null,
        configurations(), managedOnlyServices(), overlay(true, false), providers(true, false),
        hdfsPolicy(REALM, false));
    assertFalse(managedHdfs.consumerCalculation().consumerLocalMapping().canonicalRules()
        .contains("/hdfs/"));
    assertNull(managedHdfs.consumerCalculation().detachedDescriptor().getService("HBASE")
        .getComponent("HBASE_MASTER").getIdentity("hbase_hbase_master_hdfs"));
  }

  @Test
  void rawUserConflictFailsWithoutMutatingDescriptors() throws Exception {
    ManagedHBaseSecurityDescriptorAdapter adapter =
        new ManagedHBaseSecurityDescriptorAdapter(realHelper());
    KerberosDescriptor raw = compositeDescriptor();
    KerberosDescriptor user = descriptorFactory.createInstance("""
        {"configurations":[{
          "hbase-site":{"hbase.superuser":"shared_hbase"}
        }]}
        """);
    Map<String, Object> rawBefore = raw.toMap();
    Map<String, Object> userBefore = user.toMap();

    ManagedDependencyIntegrationException failure = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> adapter.resolve(secureCluster(11L),
            consumer("SERVICE_PLAN", ConsumerLifecycle.DRAFT), raw, user,
            configurations(), managedOnlyServices(), overlay(true, false),
            providers(true, false), hdfsPolicy(REALM, false)));

    assertEquals("DEPENDENCY_KERBEROS_OVERRIDE_CONFLICT", failure.getCode());
    assertEquals(rawBefore, raw.toMap());
    assertEquals(userBefore, user.toMap());
  }

  @Test
  void policyAndConsumerPlanDriftChangeApprovedFingerprints() throws Exception {
    ManagedHBaseSecurityDescriptorAdapter adapter =
        new ManagedHBaseSecurityDescriptorAdapter(realHelper());
    Cluster cluster = secureCluster(11L);
    Consumer consumer = consumer("SERVICE_PLAN", ConsumerLifecycle.DRAFT);
    Provider provider = providers(true, false).get(ManagedDependencyType.HDFS);
    SortedMap<ManagedDependencyType, Provider> selected = providers(true, false);
    ManagedHBaseKerberosOverlaySpec spec = overlay(true, false);

    Resolution original = adapter.resolve(cluster, consumer, compositeDescriptor(), null,
        configurations(), managedOnlyServices(), spec, selected,
        hdfsPolicy(REALM, false));
    Resolution providerDrift = adapter.resolve(cluster, consumer, compositeDescriptor(), null,
        configurations(), managedOnlyServices(), spec, selected,
        hdfsPolicy(REALM, true));
    Resolution consumerDrift = adapter.resolve(cluster, consumer, compositeDescriptor(), null,
        configurations(), servicesWithLocalHdfs(), spec, selected,
        hdfsPolicy(REALM, false));
    ManagedDependencySnapshot originalSnapshot = snapshot(HDFS_BINDING, consumer, provider,
        original.contextFor(ManagedDependencyType.HDFS)).snapshot().orElseThrow();
    ManagedDependencySnapshot providerDriftSnapshot = snapshot(HDFS_BINDING, consumer, provider,
        providerDrift.contextFor(ManagedDependencyType.HDFS)).snapshot().orElseThrow();
    ManagedDependencySnapshot consumerDriftSnapshot = snapshot(HDFS_BINDING, consumer, provider,
        consumerDrift.contextFor(ManagedDependencyType.HDFS)).snapshot().orElseThrow();

    assertNotEquals(originalSnapshot.providerFingerprint(),
        providerDriftSnapshot.providerFingerprint());
    assertNotEquals(originalSnapshot.snapshotFingerprint(),
        providerDriftSnapshot.snapshotFingerprint());
    assertNotEquals(originalSnapshot.consumerFingerprint(),
        consumerDriftSnapshot.consumerFingerprint());
    assertNotEquals(originalSnapshot.snapshotFingerprint(),
        consumerDriftSnapshot.snapshotFingerprint());
  }

  @Test
  void mixedAndCrossRealmProvidersFailBeforeConsumerCalculation() throws Exception {
    CountingKerberosHelper helper = realHelper();
    ManagedHBaseSecurityDescriptorAdapter adapter =
        new ManagedHBaseSecurityDescriptorAdapter(helper);
    Cluster cluster = secureCluster(11L);
    Consumer consumer = consumer("SERVICE_PLAN", ConsumerLifecycle.DRAFT);
    ManagedHBaseKerberosOverlaySpec spec = overlay(true, false);

    ManagedDependencyIntegrationException mixed = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> adapter.resolve(cluster, consumer, compositeDescriptor(), null,
            configurations(), managedOnlyServices(), spec,
            MapBuilder.providers(ManagedDependencyType.HDFS, insecureHdfsProvider()),
            hdfsPolicy(REALM, false)));
    ManagedDependencyIntegrationException crossRealm = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> adapter.resolve(cluster, consumer, compositeDescriptor(), null,
            configurations(), managedOnlyServices(), spec,
            MapBuilder.providers(ManagedDependencyType.HDFS, secureHdfsProvider("OTHER.COM")),
            hdfsPolicy("OTHER.COM", false)));

    assertEquals(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH.name(),
        mixed.getCode());
    assertEquals(ManagedDependencyErrorCode.CROSS_REALM_NOT_SUPPORTED.name(),
        crossRealm.getCode());
    assertEquals(0, helper.calculationCount);
  }

  @Test
  void hdfsPolicyRealmDifferentFromTypedProviderFailsInTheValidator() throws Exception {
    ManagedHBaseSecurityDescriptorAdapter adapter =
        new ManagedHBaseSecurityDescriptorAdapter(realHelper());
    Cluster cluster = secureCluster(11L);
    Consumer consumer = consumer("SERVICE_PLAN", ConsumerLifecycle.DRAFT);
    Provider provider = secureHdfsProvider(REALM);
    Resolution resolution = adapter.resolve(cluster, consumer, compositeDescriptor(), null,
        configurations(), managedOnlyServices(), overlay(true, false),
        MapBuilder.providers(ManagedDependencyType.HDFS, provider),
        hdfsPolicy("OTHER.COM", false));

    ValidationResult result = snapshot(HDFS_BINDING, consumer, provider,
        resolution.contextFor(ManagedDependencyType.HDFS));

    assertFalse(result.isValid());
    assertTrue(result.hasError(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED));
  }

  private ValidationResult snapshot(UUID bindingId, Consumer consumer, Provider provider,
      ManagedDependencySecurityValidationContext context) {
    return validator.validate(bindingId, 1, provider.type(), consumer, provider,
        List.of(), List.of(), null, context);
  }

  private CountingKerberosHelper realHelper() throws Exception {
    CountingKerberosHelper helper = new CountingKerberosHelper();
    setField(helper, "kerberosDescriptorFactory", descriptorFactory);
    setField(helper, "variableReplacementHelper", new VariableReplacementHelper());
    setField(helper, "managedHBaseKerberosLivePlanProvider",
        (ManagedHBaseKerberosLivePlanProvider) ignored -> {
          throw new AssertionError("prospective calculation must not resolve a live binding");
        });
    return helper;
  }

  private Cluster secureCluster(long id) {
    Config krb5Conf = niceMock(Config.class);
    expect(krb5Conf.getProperties()).andStubReturn(Map.of("realm", REALM));
    replay(krb5Conf);
    Config kerberosEnv = niceMock(Config.class);
    expect(kerberosEnv.getProperties()).andStubReturn(configurations().get("kerberos-env"));
    replay(kerberosEnv);
    Service hbase = niceMock(Service.class);
    expect(hbase.getDesiredStackId()).andStubReturn(STACK_ID);
    replay(hbase);

    Cluster cluster = niceMock(Cluster.class);
    expect(cluster.getDesiredConfigByType("krb5-conf")).andStubReturn(krb5Conf);
    expect(cluster.getDesiredConfigByType("kerberos-env")).andStubReturn(kerberosEnv);
    expect(cluster.getSecurityType()).andStubReturn(SecurityType.KERBEROS);
    expect(cluster.getClusterName()).andStubReturn("consumer-a");
    expect(cluster.getClusterId()).andStubReturn(id);
    expect(cluster.getDesiredStackVersion()).andStubReturn(STACK_ID);
    expect(cluster.getServices()).andStubReturn(Map.of("HBASE", hbase));
    expect(cluster.getHosts()).andStubReturn(List.of());
    replay(cluster);
    return cluster;
  }

  private Consumer consumer(String source, ConsumerLifecycle lifecycle) {
    Plan plan = Plan.forExistingCluster(11L);
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        plan.plannedShortUser(),
        new TreeSet<>(Set.of(plan.plannedShortUser() + "/_HOST@" + REALM)),
        false, plan.plannedShortUser(), true, "0700", true);
    return new Consumer(source, null, 11L,
        "SERVICE".equals(source) ? "renamed-consumer" : "consumer-a",
        "HBASE", lifecycle, version("HBASE", 11L),
        ManagedDependencySecurityMode.KERBEROS, REALM, identity, plan);
  }

  private ManagedHBaseKerberosOverlaySpec overlay(boolean hdfs, boolean zooKeeper) {
    Plan plan = Plan.forExistingCluster(11L);
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindings = new TreeMap<>();
    if (hdfs) {
      bindings.put(ManagedDependencyType.HDFS,
          new ManagedBindingSnapshotRef(ManagedDependencyType.HDFS, HDFS_BINDING));
    }
    if (zooKeeper) {
      bindings.put(ManagedDependencyType.ZOOKEEPER,
          new ManagedBindingSnapshotRef(ManagedDependencyType.ZOOKEEPER, ZOOKEEPER_BINDING));
    }
    return ManagedHBaseKerberosOverlaySpec.create(REALM, plan.plannedShortUser(),
        "ambari-qa@" + REALM, "ambari-qa", plan.planFingerprint(), bindings);
  }

  private SortedMap<ManagedDependencyType, Provider> providers(boolean hdfs,
      boolean zooKeeper) {
    SortedMap<ManagedDependencyType, Provider> providers = new TreeMap<>();
    if (hdfs) {
      providers.put(ManagedDependencyType.HDFS, secureHdfsProvider(REALM));
    }
    if (zooKeeper) {
      providers.put(ManagedDependencyType.ZOOKEEPER, secureZooKeeperProvider(REALM));
    }
    return providers;
  }

  private Provider secureHdfsProvider(String realm) {
    Map<String, String> coreSite = new TreeMap<>(Map.of(
        "fs.defaultFS", "hdfs://nn.example.test:8020",
        "hadoop.security.authentication", "kerberos",
        "ipc.client.fallback-to-simple-auth-allowed", "false"));
    Map<String, String> hdfsSite = new TreeMap<>(Map.of(
        "dfs.client.use.datanode.hostname", "true",
        "dfs.namenode.kerberos.principal", "nn/_HOST@" + realm,
        "dfs.datanode.kerberos.principal", "dn/_HOST@" + realm));
    return new Provider(new ManagedDependencyServiceKey(22L, "HDFS"),
        ManagedDependencyType.HDFS, version("HDFS", 22L), true, true,
        ManagedDependencySecurityMode.KERBEROS, realm, providerIdentity("hdfs", realm),
        new HdfsEndpoint("hdfs://nn.example.test:8020", false, "", new TreeMap<>(),
            "", false, false), null, coreSite, hdfsSite, Map.of(),
        new TreeSet<>(Set.of("fs.defaultFS", "dfs.client.use.datanode.hostname")),
        new TreeSet<>());
  }

  private Provider insecureHdfsProvider() {
    Provider secure = secureHdfsProvider(REALM);
    Map<String, String> coreSite = new TreeMap<>(secure.coreSite());
    coreSite.put("hadoop.security.authentication", "simple");
    coreSite.remove("ipc.client.fallback-to-simple-auth-allowed");
    return new Provider(secure.serviceKey(), secure.type(), secure.version(), true, true,
        ManagedDependencySecurityMode.INSECURE, "", null, secure.hdfsEndpoint(), null,
        coreSite, secure.hdfsSite(), Map.of(), secure.requiredClientProperties(),
        new TreeSet<>());
  }

  private Provider secureZooKeeperProvider(String realm) {
    List<String> hosts = List.of("zk1.example.test", "zk2.example.test");
    Map<String, String> client = new TreeMap<>(Map.of(
        "hbase.zookeeper.quorum", String.join(",", hosts),
        "hbase.zookeeper.property.clientPort", "2181",
        "zookeeper.sasl.client", "true",
        "zookeeper.sasl.client.username", "zookeeper",
        "zookeeper.sasl.clientconfig", "Client"));
    return new Provider(new ManagedDependencyServiceKey(33L, "ZOOKEEPER"),
        ManagedDependencyType.ZOOKEEPER, version("ZOOKEEPER", 33L), true, true,
        ManagedDependencySecurityMode.KERBEROS, realm,
        providerIdentity("zookeeper", realm), null,
        new ZooKeeperEndpoint(hosts, 2181, true, "zookeeper", true,
            List.of("/hbase-secure"), true, true, "DEFAULT"),
        Map.of(), Map.of(), client, new TreeSet<>(client.keySet()), new TreeSet<>());
  }

  private ManagedDependencyIdentity providerIdentity(String user, String realm) {
    return new ManagedDependencyIdentity(user,
        new TreeSet<>(Set.of(user + "/_HOST@" + realm)), false, "hadoop", false,
        "0700", false);
  }

  private ManagedDependencyVersion version(String service, long repositoryRowId) {
    return new ManagedDependencyVersion("BIGTOP", "3.3.0", true,
        service + "-3.3.0", new TreeMap<>(Map.of("repository.version", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), repositoryRowId,
        new ArrayList<>(List.of("https://mirror.example/repository")));
  }

  private PolicySource hdfsPolicy(String realm, boolean extraRule) {
    Plan plan = Plan.forExistingCluster(11L);
    org.apache.ambari.server.state.kerberos.AuthToLocalBuilder rules =
        new org.apache.ambari.server.state.kerberos.AuthToLocalBuilder(realm, "", false);
    rules.addRule(plan.plannedShortUser() + "/_HOST@" + realm,
        plan.plannedShortUser());
    if (extraRule) {
      rules.addRule("auxiliary/_HOST@" + realm, "auxiliary");
    }
    ManagedHdfsAuthToLocalVerifier verifier = new ManagedHdfsAuthToLocalVerifier();
    return new PolicySource(rules.generate(), "", true, realm, true, null, "/etc",
        STOCK_TEMPLATE, verifier.fingerprintTemplate(STOCK_TEMPLATE));
  }

  private Map<String, Map<String, String>> configurations() {
    Map<String, Map<String, String>> configurations = new TreeMap<>();
    configurations.put("", new TreeMap<>(Map.of("principal_suffix", "")));
    configurations.put(KerberosHelper.CLUSTER_HOST_INFO, new TreeMap<>());
    configurations.put("cluster-env", new TreeMap<>(Map.of(
        "smokeuser", "ambari-qa", "user_group", "hadoop")));
    configurations.put("hadoop-env", new TreeMap<>(Map.of("hdfs_user", "hdfs")));
    configurations.put("kerberos-env", new TreeMap<>(Map.of(
        KerberosHelper.DEFAULT_REALM, REALM,
        KerberosHelper.KDC_TYPE, "mit-kdc",
        KerberosHelper.MANAGE_IDENTITIES, "true",
        KerberosHelper.MANAGE_AUTH_TO_LOCAL_RULES, "true",
        KerberosHelper.CASE_INSENSITIVE_USERNAME_RULES, "false",
        KerberosHelper.CREATE_AMBARI_PRINCIPAL, "false")));
    configurations.put("core-site", new TreeMap<>(Map.of(
        "fs.defaultFS", "hdfs://local-ns")));
    return configurations;
  }

  private Map<String, Set<String>> managedOnlyServices() {
    return new TreeMap<>(Map.of("HBASE", new TreeSet<>(Set.of(
        "HBASE_MASTER", "HBASE_REGIONSERVER", "HBASE_THRIFT"))));
  }

  private Map<String, Set<String>> servicesWithLocalHdfs() {
    Map<String, Set<String>> services = managedOnlyServices();
    services.put("HDFS", new TreeSet<>(Set.of("NAMENODE", "DATANODE")));
    return services;
  }

  private KerberosDescriptor compositeDescriptor() throws IOException, AmbariException {
    KerberosDescriptor root = load("kerberos/test_kerberos_descriptor_simple.json");
    root.update(load("stacks/BIGTOP/3.2.0/services/HBASE/kerberos.json"));
    return root;
  }

  private KerberosDescriptor load(String resource) throws IOException, AmbariException {
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
      if (input == null) {
        throw new IOException("missing test resource " + resource);
      }
      return descriptorFactory.createInstance(
          new String(input.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  private Map<String, Map<String, String>> mutableCopy(
      Map<String, Map<String, String>> source) {
    Map<String, Map<String, String>> copy = new TreeMap<>();
    source.forEach((type, properties) -> copy.put(type, new TreeMap<>(properties)));
    return copy;
  }

  private Map<String, Set<String>> mutableServiceCopy(Map<String, Set<String>> source) {
    Map<String, Set<String>> copy = new TreeMap<>();
    source.forEach((service, components) -> copy.put(service, new TreeSet<>(components)));
    return copy;
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = KerberosHelperImpl.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static final class CountingKerberosHelper extends KerberosHelperImpl {
    private int calculationCount;

    @Override
    public ManagedHBaseKerberosCalculation calculateManagedHBaseKerberosConfiguration(
        Cluster cluster, KerberosDescriptor rawEffectiveComposite,
        @Nullable KerberosDescriptor rawUserDescriptor,
        Map<String, Map<String, String>> existingConfigurations,
        Map<String, Set<String>> plannedServices,
        ManagedHBaseKerberosOverlaySpec overlaySpec,
        boolean applyStackAdvisorUpdates)
        throws KerberosInvalidConfigurationException, AmbariException {
      calculationCount++;
      return super.calculateManagedHBaseKerberosConfiguration(cluster, rawEffectiveComposite,
          rawUserDescriptor, existingConfigurations, plannedServices, overlaySpec,
          applyStackAdvisorUpdates);
    }
  }

  private static final class MapBuilder {
    private MapBuilder() {
    }

    private static SortedMap<ManagedDependencyType, Provider> providers(
        ManagedDependencyType type, Provider provider) {
      SortedMap<ManagedDependencyType, Provider> result = new TreeMap<>();
      result.put(type, provider);
      return result;
    }
  }
}
