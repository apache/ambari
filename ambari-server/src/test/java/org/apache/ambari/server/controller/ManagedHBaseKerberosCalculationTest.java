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
package org.apache.ambari.server.controller;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.controller.KerberosHelper.KerberosDescriptorType;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosCalculation;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosDescriptorOverlay;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosLivePlan;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosLivePlanProvider;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec.ManagedBindingSnapshotRef;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.VariableReplacementHelper;
import org.junit.jupiter.api.Test;

class ManagedHBaseKerberosCalculationTest {
  private static final String REALM = "EXAMPLE.COM";
  private static final String USER = "hbase_mc_0123456789abcdefabcd";
  private static final String USER_B = "hbase_mc_fedcba9876543210abcd";
  private static final String PLAN_FINGERPRINT =
      "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
  private static final String PLAN_FINGERPRINT_B =
      "sha256:abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
  private static final UUID HDFS_BINDING =
      UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID HDFS_BINDING_B =
      UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final StackId STACK_ID = new StackId("BIGTOP-3.2.0");

  private final KerberosDescriptorFactory descriptorFactory = new KerberosDescriptorFactory();

  @Test
  void prospectiveCalculationUsesNormalRulesWithoutHostsOrLivePlanRecursion()
      throws Exception {
    KerberosDescriptor raw = compositeDescriptor();
    Map<String, Object> rawBefore = raw.toMap();
    Cluster cluster = secureCluster("consumer-a", 11L);
    KerberosHelperImpl helper = helper(
        ignored -> {
          throw new AssertionError("prospective calculation must not resolve a live binding");
        }, null);
    Map<String, Map<String, String>> configurations = configurations();
    Map<String, Set<String>> plannedServices = plannedServices();

    ManagedHBaseKerberosCalculation planned =
        helper.calculateManagedHBaseKerberosConfiguration(cluster, raw, null,
            configurations, plannedServices, spec(), false);
    ManagedHBaseKerberosCalculation materialized =
        helper.calculateManagedHBaseKerberosConfiguration(cluster,
            planned.detachedDescriptor(), null, configurations, plannedServices,
            spec(), false);

    assertEquals(rawBefore, raw.toMap());
    assertEquals(planned.consumerLocalMapping(), materialized.consumerLocalMapping());
    assertEquals(REALM, planned.realm());
    assertEquals(USER, planned.effectiveShortUser());
    assertEquals(USER + "/_HOST@" + REALM, planned.rolePrincipalPattern());
    assertEquals(USER + "@" + REALM, planned.headlessPrincipal());
    assertEquals("ambari-qa@" + REALM, planned.smokePrincipal());
    assertTrue(planned.consumerLocalMapping().canonicalRules().contains("/hdfs/"));
    assertTrue(planned.consumerLocalMapping().canonicalRules().contains("/" + USER + "/"));
    assertNull(planned.detachedDescriptor().getService("HBASE")
        .getComponent("HBASE_MASTER").getIdentity("hbase_hbase_master_hdfs"));
    assertEquals("hdfs://local-ns", planned.configurations().get("core-site")
        .get("fs.defaultFS"));
    assertThrows(UnsupportedOperationException.class,
        () -> planned.configurations().get("core-site").put("changed", "value"));

    KerberosDescriptor firstDetached = planned.detachedDescriptor();
    firstDetached.getService("HBASE").getComponent("HBASE_MASTER")
        .getIdentity("hbase_master_hbase").getPrincipalDescriptor().setValue("changed");
    assertNotEquals("changed", planned.detachedDescriptor().getService("HBASE")
        .getComponent("HBASE_MASTER").getIdentity("hbase_master_hbase")
        .getPrincipalDescriptor().getValue());
    assertFalse(planned.toString().contains(
        planned.consumerLocalMapping().canonicalRules()));
  }

  @Test
  void concurrentProspectiveCalculationsRemainClusterAndIdentityScoped()
      throws Exception {
    KerberosDescriptor raw = compositeDescriptor();
    Map<String, Object> rawBefore = raw.toMap();
    KerberosHelperImpl helper = helper(
        ignored -> {
          throw new AssertionError("prospective calculation must not resolve a live binding");
        }, null);
    ManagedHBaseKerberosOverlaySpec firstSpec = spec();
    ManagedHBaseKerberosOverlaySpec secondSpec = spec(USER_B, HDFS_BINDING_B,
        PLAN_FINGERPRINT_B);
    List<Callable<ManagedHBaseKerberosCalculation>> tasks = List.of(
        () -> helper.calculateManagedHBaseKerberosConfiguration(
            secureCluster("consumer-concurrent-a", 31L), raw, null, configurations(),
            plannedServices(), firstSpec, false),
        () -> helper.calculateManagedHBaseKerberosConfiguration(
            secureCluster("consumer-concurrent-b", 32L), raw, null, configurations(),
            plannedServices(), secondSpec, false));
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Future<ManagedHBaseKerberosCalculation>> results = executor.invokeAll(tasks);
      ManagedHBaseKerberosCalculation first = results.get(0).get();
      ManagedHBaseKerberosCalculation second = results.get(1).get();

      assertEquals(USER, first.effectiveShortUser());
      assertEquals(USER_B, second.effectiveShortUser());
      assertNotEquals(first.mappingProfileFingerprint(),
          second.mappingProfileFingerprint());
      assertTrue(first.consumerLocalMapping().canonicalRules().contains("/" + USER + "/"));
      assertTrue(second.consumerLocalMapping().canonicalRules()
          .contains("/" + USER_B + "/"));
      assertEquals(rawBefore, raw.toMap());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void ordinaryPostAdvisorAuthToLocalBoundaryResealsAndProvesManagedValues()
      throws Exception {
    Cluster cluster = secureCluster("consumer-advisor", 15L);
    ManagedHBaseKerberosLivePlan livePlan = approvedLivePlan(cluster,
        managedOnlyServices());
    KerberosHelperImpl helper = helper(ignored -> Optional.of(livePlan), null);
    KerberosDescriptor managedDescriptor = new ManagedHBaseKerberosDescriptorOverlay()
        .applyCopy(compositeDescriptor(), null, spec());
    Map<String, Map<String, String>> updates = new TreeMap<>();
    updates.put("hbase-env", new TreeMap<>(Map.of("hbase_user", "hbase")));
    updates.put("hbase-site", new TreeMap<>(Map.of(
        "hbase.master.kerberos.principal", "hbase/_HOST@" + REALM,
        "hbase.superuser", "hbase")));

    helper.setAuthToLocalRules(cluster, managedDescriptor, REALM, managedOnlyServices(),
        configurations(), updates, false);

    assertEquals(USER, updates.get("hbase-env").get("hbase_user"));
    assertEquals(USER + "/_HOST@" + REALM,
        updates.get("hbase-site").get("hbase.master.kerberos.principal"));
    assertEquals(USER, updates.get("hbase-site").get("hbase.superuser"));
    assertTrue(updates.get("core-site").get("hadoop.security.auth_to_local")
        .contains("/" + USER + "/"));
  }

  @Test
  void publicAdvisorBoundaryRestoresControlledValuesAndRejectsTheirDeletion()
      throws Exception {
    Cluster cluster = secureCluster("consumer-advisor-output", 18L, true);
    ManagedHBaseKerberosCalculation approved = approvedCalculation(cluster,
        managedOnlyServices());
    ManagedHBaseKerberosLivePlan livePlan = new ManagedHBaseKerberosLivePlan(spec(),
        approved.mappingProfileFingerprint());
    KerberosHelperImpl helper = helper(ignored -> Optional.of(livePlan), null);
    RecommendationResponse response = advisorResponse(Map.of(
        "hbase-env", Map.of("hbase_user", "hbase"),
        "hbase-site", Map.of(
            "hbase.master.kerberos.principal", "hbase/_HOST@" + REALM,
            "advisor-only-property", "applied")),
        Map.of("hbase-site", Set.of("hbase.superuser")));
    setAdvisor(helper, response);
    Map<String, Map<String, String>> existing = configurations();
    existing.put("hbase-site", new TreeMap<>(Map.of("hbase.superuser", USER)));
    Map<String, Map<String, String>> updates = new TreeMap<>();
    updates.put("core-site", new TreeMap<>(Map.of(
        "hadoop.security.auth_to_local", approved.consumerLocalMapping().canonicalRules())));
    updates.put("hbase-env", new TreeMap<>(Map.of("hbase_user", USER)));
    updates.put("hbase-site", new TreeMap<>(Map.of(
        "hbase.master.kerberos.principal", USER + "/_HOST@" + REALM)));
    Map<String, Set<String>> removals = new TreeMap<>();

    Map<String, Map<String, String>> result = helper.applyStackAdvisorUpdates(cluster,
        managedOnlyServices().keySet(), existing, updates, new TreeMap<>(), removals, true);

    assertEquals(USER, result.get("hbase-env").get("hbase_user"));
    assertEquals(USER + "/_HOST@" + REALM,
        result.get("hbase-site").get("hbase.master.kerberos.principal"));
    assertEquals(USER, result.get("hbase-site").get("hbase.superuser"));
    assertEquals("applied", result.get("hbase-site").get("advisor-only-property"));
    assertFalse(removals.containsKey("hbase-site"));
  }

  @Test
  void publicAdvisorBoundaryRejectsChangedConsumerRules()
      throws Exception {
    Cluster cluster = secureCluster("consumer-advisor-rules", 19L, true);
    ManagedHBaseKerberosCalculation approved = approvedCalculation(cluster,
        managedOnlyServices());
    ManagedHBaseKerberosLivePlan livePlan = new ManagedHBaseKerberosLivePlan(spec(),
        approved.mappingProfileFingerprint());
    KerberosHelperImpl helper = helper(ignored -> Optional.of(livePlan), null);
    String changedRules = approvedCalculation(cluster, plannedServices())
        .consumerLocalMapping().canonicalRules();
    assertNotEquals(approved.consumerLocalMapping().canonicalRules(), changedRules);
    setAdvisor(helper, advisorResponse(Map.of("core-site", Map.of(
        "hadoop.security.auth_to_local", changedRules)), Map.of()));
    Map<String, Map<String, String>> updates = new TreeMap<>();
    updates.put("core-site", new TreeMap<>(Map.of(
        "hadoop.security.auth_to_local", approved.consumerLocalMapping().canonicalRules())));

    ManagedDependencyIntegrationException failure = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> helper.applyStackAdvisorUpdates(cluster, managedOnlyServices().keySet(),
            configurations(), updates, new TreeMap<>(), new TreeMap<>(), true));

    assertEquals("DEPENDENCY_KERBEROS_LIVE_MAPPING_MISMATCH", failure.getCode());
  }

  @Test
  void evaluatedCompositePrunesUnrelatedConditionalIdentityAndRejectsManagedDrift()
      throws Exception {
    Cluster cluster = secureCluster("consumer-when", 20L);
    ManagedHBaseKerberosCalculation approved = approvedCalculation(cluster,
        managedOnlyServices());
    ManagedHBaseKerberosLivePlan livePlan = new ManagedHBaseKerberosLivePlan(spec(),
        approved.mappingProfileFingerprint());
    KerberosDescriptor cached = compositeDescriptor();
    cached.update(descriptorFactory.createInstance("""
        {"identities":[{
          "name":"conditional-pruned",
          "when":{"contains":["services","NOT_INSTALLED"]},
          "principal":{
            "value":"conditional@${realm}",
            "type":"user",
            "local_username":"conditional"
          }
        }]}
        """));
    KerberosHelperImpl helper = helper(ignored -> Optional.of(livePlan), cached);
    setConfigurationSource(helper, cluster, null, approved.configurations());

    KerberosDescriptor evaluated = helper.getKerberosDescriptor(
        KerberosDescriptorType.COMPOSITE, cluster, true, null, false, null, null);

    assertNull(evaluated.getIdentity("conditional-pruned"));
    assertEquals(USER + "/_HOST@" + REALM,
        evaluated.getService("HBASE").getComponent("HBASE_MASTER")
            .getIdentity("hbase_master_hbase").getPrincipalDescriptor().getValue());
    helper.calculateConfigurations(cluster, null, evaluated, false, false, null);

    evaluated.getService("HBASE").getComponent("HBASE_MASTER")
        .getIdentity("hbase_master_hbase").getPrincipalDescriptor()
        .setValue("hbase/_HOST@${realm}");
    ManagedDependencyIntegrationException failure = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> helper.calculateConfigurations(cluster, null, evaluated,
            false, false, null));
    assertEquals("DEPENDENCY_KERBEROS_LIVE_DESCRIPTOR_MISMATCH", failure.getCode());
  }

  @Test
  void ordinaryConfigurationAndCredentialDescriptorPathsUseTheApprovedLivePlan()
      throws Exception {
    KerberosDescriptor cachedStackDescriptor = compositeDescriptor();
    Map<String, Object> cachedBefore = cachedStackDescriptor.toMap();
    Cluster cluster = secureCluster("consumer-live", 12L);
    ManagedHBaseKerberosLivePlan livePlan = approvedLivePlan(cluster,
        managedOnlyServices());
    KerberosHelperImpl helper = helper(
        ignored -> Optional.of(livePlan), cachedStackDescriptor);

    Map<String, Map<String, String>> updates = helper.getServiceConfigurationUpdates(
        cluster, configurations(), managedOnlyServices(), null, null, true, false);
    KerberosDescriptor credentialDescriptor = helper.getKerberosDescriptor(cluster, false);
    AmbariManagementController managementController = niceMock(
        AmbariManagementController.class);
    ConfigHelper configHelper = niceMock(ConfigHelper.class);
    expect(configHelper.calculateExistingConfigurations(managementController, cluster,
        "master.example.com", null)).andReturn(mutableCopy(
            approvedCalculation(cluster, managedOnlyServices()).configurations()));
    replay(managementController, configHelper);
    setField(helper, "ambariManagementController", managementController);
    setField(helper, "configHelper", configHelper);
    Map<String, Map<String, String>> credentialConfigurations =
        helper.calculateConfigurations(cluster, "master.example.com", credentialDescriptor,
            false, false, null);

    assertEquals(USER + "/_HOST@" + REALM,
        updates.get("hbase-site").get("hbase.master.kerberos.principal"));
    assertFalse(updates.containsKey("hadoop-env"));
    assertEquals(USER + "/_HOST@" + REALM,
        credentialDescriptor.getService("HBASE").getComponent("HBASE_MASTER")
            .getIdentity("hbase_master_hbase").getPrincipalDescriptor().getValue());
    assertNull(credentialDescriptor.getService("HBASE").getComponent("HBASE_MASTER")
        .getIdentity("hbase_hbase_master_hdfs"));
    assertEquals(livePlan.approvedConsumerMappingProfileFingerprint(),
        ManagedHBaseConsumerLocalMapping.create(spec(),
            new ManagedHBaseKerberosDescriptorOverlay()
                .sealCalculatedConfigurations(credentialConfigurations, spec()))
            .profileFingerprint());
    assertEquals(cachedBefore, cachedStackDescriptor.toMap());
  }

  @Test
  void credentialConfigurationRejectsRulesOutsideApprovedSnapshotLineage()
      throws Exception {
    Cluster cluster = secureCluster("consumer-credential-stale", 17L);
    ManagedHBaseKerberosLivePlan livePlan = approvedLivePlan(cluster,
        managedOnlyServices());
    KerberosHelperImpl helper = helper(ignored -> Optional.of(livePlan),
        compositeDescriptor());
    KerberosDescriptor managedDescriptor = helper.getKerberosDescriptor(cluster, false);
    AmbariManagementController managementController = niceMock(
        AmbariManagementController.class);
    ConfigHelper configHelper = niceMock(ConfigHelper.class);
    expect(configHelper.calculateExistingConfigurations(managementController, cluster,
        "master.example.com", null)).andReturn(mutableCopy(
            approvedCalculation(cluster, plannedServices()).configurations()));
    replay(managementController, configHelper);
    setField(helper, "ambariManagementController", managementController);
    setField(helper, "configHelper", configHelper);

    ManagedDependencyIntegrationException failure = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> helper.calculateConfigurations(cluster, "master.example.com",
            managedDescriptor, false, false, null));

    assertEquals("DEPENDENCY_KERBEROS_LIVE_MAPPING_MISMATCH", failure.getCode());
  }

  @Test
  void ordinaryCalculationRejectsRulesOutsideApprovedSnapshotLineage()
      throws Exception {
    KerberosDescriptor cachedStackDescriptor = compositeDescriptor();
    Cluster cluster = secureCluster("consumer-mismatch", 16L);
    ManagedHBaseKerberosLivePlan mismatched = new ManagedHBaseKerberosLivePlan(spec(),
        "sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    KerberosHelperImpl helper = helper(ignored -> Optional.of(mismatched),
        cachedStackDescriptor);

    ManagedDependencyIntegrationException failure = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> helper.getServiceConfigurationUpdates(cluster, configurations(),
            Map.of("HBASE", Set.of("HBASE_MASTER", "HBASE_REGIONSERVER")),
            null, null, true, false));

    assertEquals("DEPENDENCY_KERBEROS_LIVE_MAPPING_MISMATCH", failure.getCode());
  }

  @Test
  void invalidActiveLivePlanFailsClosedWhileRawPlanningOverloadStaysUnhooked()
      throws Exception {
    KerberosDescriptor cachedStackDescriptor = compositeDescriptor();
    Cluster cluster = secureCluster("consumer-stale", 13L);
    KerberosHelperImpl helper = helper(
        ignored -> {
          throw new AmbariException("active managed HBase plan is stale");
        }, cachedStackDescriptor);

    AmbariException failure = assertThrows(AmbariException.class,
        () -> helper.getKerberosDescriptor(cluster, false));
    assertTrue(failure.getMessage().contains("stale"));

    KerberosDescriptor raw = helper.getKerberosDescriptor(
        KerberosDescriptorType.COMPOSITE, cluster, STACK_ID, false, null);
    assertEquals("hbase/_HOST@${realm}", raw.getService("HBASE")
        .getComponent("HBASE_MASTER").getIdentity("hbase_master_hbase")
        .getPrincipalDescriptor().getValue());
  }

  @Test
  void emptyLivePlanPreservesUnboundHBaseDescriptor() throws Exception {
    KerberosDescriptor cachedStackDescriptor = compositeDescriptor();
    Cluster cluster = secureCluster("consumer-local", 14L);
    KerberosHelperImpl helper = helper(ignored -> Optional.empty(),
        cachedStackDescriptor);

    KerberosDescriptor result = helper.getKerberosDescriptor(cluster, false);

    assertEquals("hbase/_HOST@${realm}", result.getService("HBASE")
        .getComponent("HBASE_MASTER").getIdentity("hbase_master_hbase")
        .getPrincipalDescriptor().getValue());
    assertEquals("/HDFS/NAMENODE/hdfs", result.getService("HBASE")
        .getComponent("HBASE_MASTER").getIdentity("hbase_hbase_master_hdfs")
        .getReference());
  }

  private KerberosHelperImpl helper(ManagedHBaseKerberosLivePlanProvider livePlanProvider,
      KerberosDescriptor stackDescriptor) throws Exception {
    KerberosHelperImpl helper = new KerberosHelperImpl();
    setField(helper, "kerberosDescriptorFactory", descriptorFactory);
    setField(helper, "variableReplacementHelper", new VariableReplacementHelper());
    setField(helper, "managedHBaseKerberosLivePlanProvider", livePlanProvider);
    if (stackDescriptor != null) {
      AmbariMetaInfo metaInfo = niceMock(AmbariMetaInfo.class);
      expect(metaInfo.getKerberosDescriptor("BIGTOP", "3.2.0", false))
          .andStubReturn(stackDescriptor);
      replay(metaInfo);
      setField(helper, "ambariMetaInfo", metaInfo);

      ArtifactDAO artifactDAO = niceMock(ArtifactDAO.class);
      replay(artifactDAO);
      setField(helper, "artifactDAO", artifactDAO);
    }
    return helper;
  }

  private Cluster secureCluster(String name, long id) {
    return secureCluster(name, id, false);
  }

  private Cluster secureCluster(String name, long id, boolean withAdvisorHost) {
    Config krb5Conf = niceMock(Config.class);
    expect(krb5Conf.getProperties()).andStubReturn(Map.of("realm", REALM));
    replay(krb5Conf);
    Config kerberosEnv = niceMock(Config.class);
    expect(kerberosEnv.getProperties()).andStubReturn(
        configurations().get("kerberos-env"));
    replay(kerberosEnv);

    Cluster cluster = niceMock(Cluster.class);
    expect(cluster.getDesiredConfigByType("krb5-conf")).andStubReturn(krb5Conf);
    expect(cluster.getDesiredConfigByType("kerberos-env")).andStubReturn(kerberosEnv);
    expect(cluster.getSecurityType()).andStubReturn(SecurityType.KERBEROS);
    expect(cluster.getClusterName()).andStubReturn(name);
    expect(cluster.getClusterId()).andStubReturn(id);
    expect(cluster.getDesiredStackVersion()).andStubReturn(STACK_ID);
    Service hbase = niceMock(Service.class);
    expect(hbase.getDesiredStackId()).andStubReturn(STACK_ID);
    replay(hbase);
    expect(cluster.getServices()).andStubReturn(Map.of("HBASE", hbase));
    if (withAdvisorHost) {
      Host host = niceMock(Host.class);
      expect(host.getHostName()).andStubReturn("master.example.com");
      replay(host);
      expect(cluster.getHosts()).andStubReturn(List.of(host));
      expect(cluster.getServiceComponentHostMap(null, Set.of("HBASE")))
          .andStubReturn(Map.of());
    }
    replay(cluster);
    return cluster;
  }

  private void setAdvisor(KerberosHelperImpl helper, RecommendationResponse response)
      throws Exception {
    StackAdvisorHelper advisor = niceMock(StackAdvisorHelper.class);
    expect(advisor.recommend(anyObject(StackAdvisorRequest.class))).andReturn(response);
    replay(advisor);
    setField(helper, "stackAdvisorHelper", advisor);
  }

  private RecommendationResponse advisorResponse(
      Map<String, Map<String, String>> properties,
      Map<String, Set<String>> deletions) {
    Map<String, RecommendationResponse.BlueprintConfigurations> configurations =
        new TreeMap<>();
    Set<String> types = new TreeSet<>(properties.keySet());
    types.addAll(deletions.keySet());
    for (String type : types) {
      RecommendationResponse.BlueprintConfigurations configuration =
          new RecommendationResponse.BlueprintConfigurations();
      configuration.setProperties(properties.getOrDefault(type, Map.of()));
      Map<String, ValueAttributesInfo> attributes = new TreeMap<>();
      for (String property : deletions.getOrDefault(type, Set.of())) {
        ValueAttributesInfo delete = new ValueAttributesInfo();
        delete.setDelete("true");
        attributes.put(property, delete);
      }
      configuration.setPropertyAttributes(attributes.isEmpty() ? null : attributes);
      configurations.put(type, configuration);
    }
    RecommendationResponse.Blueprint blueprint = new RecommendationResponse.Blueprint();
    blueprint.setConfigurations(configurations);
    RecommendationResponse.Recommendation recommendation =
        new RecommendationResponse.Recommendation();
    recommendation.setBlueprint(blueprint);
    RecommendationResponse response = new RecommendationResponse();
    response.setRecommendations(recommendation);
    return response;
  }

  private void setConfigurationSource(KerberosHelperImpl helper, Cluster cluster,
      String hostname, Map<String, Map<String, String>> configurations) throws Exception {
    AmbariManagementController managementController = niceMock(
        AmbariManagementController.class);
    ConfigHelper configHelper = niceMock(ConfigHelper.class);
    expect(configHelper.calculateExistingConfigurations(managementController, cluster,
        hostname, null)).andStubReturn(mutableCopy(configurations));
    replay(managementController, configHelper);
    setField(helper, "ambariManagementController", managementController);
    setField(helper, "configHelper", configHelper);
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

  private Map<String, Set<String>> plannedServices() {
    Map<String, Set<String>> services = new TreeMap<>();
    services.put("HDFS", Set.of("NAMENODE", "DATANODE"));
    services.put("HBASE", Set.of("HBASE_MASTER", "HBASE_REGIONSERVER",
        "HBASE_THRIFT"));
    return services;
  }

  private Map<String, Set<String>> managedOnlyServices() {
    return Map.of("HBASE", Set.of("HBASE_MASTER", "HBASE_REGIONSERVER",
        "HBASE_THRIFT"));
  }

  private ManagedHBaseKerberosOverlaySpec spec() {
    return spec(USER, HDFS_BINDING, PLAN_FINGERPRINT);
  }

  private ManagedHBaseKerberosOverlaySpec spec(String user, UUID bindingId,
      String identityPlanFingerprint) {
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindings = new TreeMap<>();
    bindings.put(ManagedDependencyType.HDFS,
        new ManagedBindingSnapshotRef(ManagedDependencyType.HDFS, bindingId));
    return ManagedHBaseKerberosOverlaySpec.create(REALM, user,
        "ambari-qa@" + REALM, "ambari-qa", identityPlanFingerprint, bindings);
  }

  private ManagedHBaseKerberosLivePlan approvedLivePlan(Cluster cluster,
      Map<String, Set<String>> services) throws Exception {
    ManagedHBaseKerberosCalculation calculation = approvedCalculation(cluster, services);
    return new ManagedHBaseKerberosLivePlan(spec(),
        calculation.mappingProfileFingerprint());
  }

  private ManagedHBaseKerberosCalculation approvedCalculation(Cluster cluster,
      Map<String, Set<String>> services) throws Exception {
    KerberosHelperImpl prospective = helper(
        ignored -> {
          throw new AssertionError("candidate calculation must not resolve a live binding");
        }, null);
    return prospective.calculateManagedHBaseKerberosConfiguration(cluster,
        compositeDescriptor(), null, configurations(), services, spec(), false);
  }

  private Map<String, Map<String, String>> mutableCopy(
      Map<String, Map<String, String>> source) {
    Map<String, Map<String, String>> copy = new TreeMap<>();
    source.forEach((type, properties) -> copy.put(type, new TreeMap<>(properties)));
    return copy;
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

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = KerberosHelperImpl.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
