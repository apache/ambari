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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.PersistKeyValueImpl;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.ScopedWorkflowState;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.KerberosHelperImpl;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Consumer;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ConsumerLifecycle;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Provider;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity.Plan;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosCalculation;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosLivePlanProvider;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseSecurityDescriptorAdapter;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseSecurityDescriptorAdapter.Resolution;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.PolicySource;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ClientConfigFileDefinition;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.VariableReplacementHelper;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.junit.jupiter.api.Test;

class ManagedDependencyDescriptorResolverTest {
  @org.junit.jupiter.api.BeforeEach
  void initializeCommandContext() {
    org.apache.ambari.server.utils.StageUtils.setTopologyManager(mock(org.apache.ambari.server.topology.TopologyManager.class));
    org.apache.ambari.server.utils.StageUtils.setConfiguration(mock(org.apache.ambari.server.configuration.Configuration.class));
  }

  private static final String STOCK_TEMPLATE_RESOURCE =
      "/stacks/BIGTOP/3.2.0/services/KERBEROS/properties/krb5_conf.j2";
  private static final String STOCK_TEMPLATE = loadStockTemplate();

  private final ManagedDependencyDescriptorResolver resolver =
      new ManagedDependencyDescriptorResolver(null, null, null, null, null);

  @Test
  void constructingResolverDoesNotConstructCommandControllerOrKerberosAdapter() {
    var injector = com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule() {
      @Override
      protected void configure() {
        bind(Clusters.class).toProvider(() -> mock(Clusters.class));
        bind(AmbariMetaInfo.class).toProvider(() -> mock(AmbariMetaInfo.class));
        bind(RepositoryVersionDAO.class).toProvider(() -> mock(RepositoryVersionDAO.class));
        bind(ServiceDependencyDAO.class).toProvider(() -> mock(ServiceDependencyDAO.class));
        bind(PersistKeyValueImpl.class).toProvider(() -> mock(PersistKeyValueImpl.class));
        bind(AmbariManagementController.class).toProvider(() -> {
          throw new AssertionError("Controller must be resolved only during security calculation");
        });
        bind(ManagedHBaseSecurityDescriptorAdapter.class).toProvider(() -> {
          throw new AssertionError("Kerberos adapter must be resolved only during security calculation");
        });
      }
    });
    org.junit.jupiter.api.Assertions.assertNotNull(
        injector.getInstance(ManagedDependencyDescriptorResolver.class));
  }

  @Test
  void freshInitConsumerMayHaveUnknownPackageVersionButInstalledConsumerMayNot() {
    RepositoryVersionEntity repository = mock(RepositoryVersionEntity.class);
    when(repository.getId()).thenReturn(31L);
    when(repository.getParentId()).thenReturn(null);
    when(repository.getVersion()).thenReturn("3.3.0-1");
    ServiceComponentHost host = mock(ServiceComponentHost.class);
    when(host.getUpgradeState()).thenReturn(UpgradeState.NONE);
    when(host.getState()).thenReturn(State.INIT);
    when(host.getDesiredState()).thenReturn(State.INIT);
    when(host.getVersion()).thenReturn(State.UNKNOWN.name());
    ServiceComponent component = mock(ServiceComponent.class);
    when(component.getDesiredRepositoryVersion()).thenReturn(repository);
    when(component.isVersionAdvertised()).thenReturn(true);
    when(component.getServiceComponentHosts()).thenReturn(Map.of("consumer.example.test", host));
    Service service = mock(Service.class);
    when(service.getServiceComponents()).thenReturn(Map.of("HBASE_CLIENT", component));

    assertDoesNotThrow(() -> resolver.validateActiveComponentVersions(service, repository, false));
    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> resolver.validateActiveComponentVersions(service, repository, true));
    assertEquals("DEPENDENCY_VERSION_UNSUPPORTED", error.getCode());
  }

  @Test
  void configGroupRejectsProviderSecurityOverrideAndAllowsUnrelatedProperty() {
    Host host = mock(Host.class);
    when(host.getHostName()).thenReturn("provider.example.test");
    Config config = mock(Config.class);
    when(config.getType()).thenReturn("zoo.cfg");
    when(config.getProperties()).thenReturn(Map.of("security.auth_to_local", "RULE:[1:$1]"));
    ConfigGroup group = mock(ConfigGroup.class);
    when(group.getHosts()).thenReturn(Map.of(7L, host));
    when(group.getConfigurations()).thenReturn(Map.of("zoo.cfg", config));
    Cluster cluster = mock(Cluster.class);
    when(cluster.getConfigGroups()).thenReturn(Map.of(3L, group));
    ServiceComponent component = mock(ServiceComponent.class);
    when(component.getServiceComponentHosts()).thenReturn(
        Map.of("provider.example.test", mock(ServiceComponentHost.class)));
    Service service = mock(Service.class);
    when(service.getServiceComponents()).thenReturn(Map.of("ZOOKEEPER_SERVER", component));

    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> resolver.validateProviderOverrides(cluster, service, ManagedDependencyType.ZOOKEEPER));
    assertEquals("DEPENDENCY_CONFIG_OVERRIDE_UNSUPPORTED", error.getCode());

    when(config.getProperties()).thenReturn(Map.of("autopurge.purgeInterval", "48"));
    assertDoesNotThrow(
        () -> resolver.validateProviderOverrides(cluster, service, ManagedDependencyType.ZOOKEEPER));
  }

  @Test
  void hdfsPolicySourceReadsCurrentFactsAndHashesActiveStackTemplate() throws Exception {
    Clusters clusters = mock(Clusters.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    Cluster cluster = mock(Cluster.class);
    Service hdfs = mock(Service.class);
    StackId stack = new StackId("BIGTOP", "3.2.0");
    when(cluster.getClusterId()).thenReturn(41L);
    when(cluster.getDesiredStackVersion()).thenReturn(stack);
    when(cluster.getCurrentStackVersion()).thenReturn(stack);
    when(cluster.getSecurityType()).thenReturn(SecurityType.KERBEROS);
    when(cluster.getService("HDFS")).thenReturn(hdfs);
    when(cluster.getConfigGroups()).thenReturn(Map.of());
    when(clusters.getClusterById(41L)).thenReturn(cluster);
    doReturn(config(Map.of(
        "hadoop.security.auth_to_local", "RULE:[1:$1@$0](hbase@EXAMPLE.COM)s/.*/hbase/",
        "hadoop.security.auth_to_local.mechanism", "hadoop"))).when(cluster).getDesiredConfigByType("core-site");
    doReturn(config(Map.of(
        "realm", "EXAMPLE.COM", "manage_auth_to_local", "true"))).when(cluster).getDesiredConfigByType("kerberos-env");
    doReturn(config(Map.of(
        "manage_krb5_conf", "true", "realm", "EXAMPLE.COM", "conf_dir", "/etc",
        "content", STOCK_TEMPLATE))).when(cluster).getDesiredConfigByType("krb5-conf");
    when(metaInfo.getServiceProperties("BIGTOP", "3.2.0", "KERBEROS"))
        .thenReturn(Set.of(stockTemplateProperty()));

    ManagedDependencyDescriptorResolver scopedResolver = new ManagedDependencyDescriptorResolver(
        clusters, metaInfo, null, null, null);
    PolicySource source = scopedResolver.resolveHdfsPolicySource(cluster, hdfsProvider(41L));

    assertEquals("RULE:[1:$1@$0](hbase@EXAMPLE.COM)s/.*/hbase/", source.effectiveRules());
    assertEquals("hadoop", source.configuredMechanism());
    assertTrue(source.ambariManagesRules());
    assertTrue(source.ambariManagesKrb5Conf());
    assertEquals("EXAMPLE.COM", source.kerberosEnvRealm());
    assertEquals("EXAMPLE.COM", source.krb5ConfRealm());
    assertEquals("/etc", source.krb5ConfDirectory());
    assertEquals(new ManagedHdfsAuthToLocalVerifier().fingerprintTemplate(STOCK_TEMPLATE),
        source.expectedStockKrb5ConfTemplateFingerprint());
  }

  @Test
  void hdfsPolicySourcePreservesAbsentRealmAndRejectsMalformedManagementFlags() throws Exception {
    Clusters clusters = mock(Clusters.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    Cluster cluster = mock(Cluster.class);
    Service hdfs = mock(Service.class);
    StackId stack = new StackId("BIGTOP", "3.2.0");
    when(cluster.getClusterId()).thenReturn(42L);
    when(cluster.getDesiredStackVersion()).thenReturn(stack);
    when(cluster.getCurrentStackVersion()).thenReturn(stack);
    when(cluster.getSecurityType()).thenReturn(SecurityType.KERBEROS);
    when(cluster.getService("HDFS")).thenReturn(hdfs);
    when(cluster.getConfigGroups()).thenReturn(Map.of());
    doReturn(config(Map.of())).when(cluster).getDesiredConfigByType("core-site");
    doReturn(config(Map.of(
        "realm", "EXAMPLE.COM"))).when(cluster).getDesiredConfigByType("kerberos-env");
    doReturn(config(Map.of(
        "content", STOCK_TEMPLATE))).when(cluster).getDesiredConfigByType("krb5-conf");
    when(metaInfo.getServiceProperties("BIGTOP", "3.2.0", "KERBEROS"))
        .thenReturn(Set.of(stockTemplateProperty()));

    ManagedDependencyDescriptorResolver scopedResolver = new ManagedDependencyDescriptorResolver(
        clusters, metaInfo, null, null, null);
    PolicySource source = scopedResolver.resolveHdfsPolicySource(cluster, hdfsProvider(42L));
    assertNull(source.krb5ConfRealm());
    assertEquals("/etc", source.krb5ConfDirectory());

    doReturn(config(Map.of(
        "content", STOCK_TEMPLATE, "realm", ""))).when(cluster).getDesiredConfigByType("krb5-conf");
    PolicySource blankRealm = scopedResolver.resolveHdfsPolicySource(cluster, hdfsProvider(42L));
    assertEquals("", blankRealm.krb5ConfRealm());

    doReturn(config(Map.of(
        "realm", "EXAMPLE.COM", "manage_auth_to_local", "sometimes"))).when(cluster).getDesiredConfigByType("kerberos-env");
    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> scopedResolver.resolveHdfsPolicySource(cluster, hdfsProvider(42L)));
    assertEquals("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN", error.getCode());
  }

  @Test
  void hdfsProviderHostOverridesCoverCompletePolicySource() {
    for (String[] property : new String[][] {
        {"core-site", "hadoop.security.auth_to_local"},
        {"core-site", "hadoop.security.auth_to_local.mechanism"},
        {"kerberos-env", "manage_auth_to_local"},
        {"kerberos-env", "realm"},
        {"krb5-conf", "manage_krb5_conf"},
        {"krb5-conf", "realm"},
        {"krb5-conf", "conf_dir"},
        {"krb5-conf", "content"}}) {
      assertTrue(resolver.isExportedOverride(ManagedDependencyType.HDFS,
          property[0], property[1]), property[0] + "/" + property[1]);
    }
    assertFalse(resolver.isExportedOverride(ManagedDependencyType.HDFS,
        "krb5-conf", "force_tcp"));
  }

  @Test
  void bigtopKrb5MetadataUsesTheFileBackedStockTemplate() {
    String metadata = loadResource(
        "/stacks/BIGTOP/3.2.0/services/KERBEROS/configuration/krb5-conf.xml");
    assertTrue(metadata.contains("<name>content</name>"));
    assertTrue(metadata.contains("<property-type>VALUE_FROM_PROPERTY_FILE</property-type>"));
    assertTrue(metadata.contains("<property-file-name>krb5_conf.j2</property-file-name>"));
  }

  @Test
  void preservesUnsupportedZooKeeperTransportAndAuthenticationFacts() {
    Set<String> unsupported = resolver.unsupportedZooKeeperFeatures(Map.of(
        "secureClientPort", "2281",
        "authProvider.1", "example.CustomAuthenticationProvider",
        "requireClientAuthScheme", "digest"), true);

    assertTrue(unsupported.contains("zookeeper-tls:secureClientPort"));
    assertTrue(unsupported.contains("zookeeper-custom-auth-provider:authProvider.1"));
    assertTrue(unsupported.contains("zookeeper-custom-client-auth:digest"));
    assertTrue(resolver.unsupportedZooKeeperFeatures(Map.of(
        "authProvider.1", "org.apache.zookeeper.server.auth.SASLAuthenticationProvider",
        "requireClientAuthScheme", "sasl"), true).isEmpty());
    assertFalse(resolver.unsupportedZooKeeperFeatures(Map.of(
        "authProvider.1", "org.apache.zookeeper.server.auth.SASLAuthenticationProvider"),
        false).isEmpty());
  }

  @Test
  void addServicePlanWithoutHbaseMatchesFreshLiveConsumerFingerprint() throws Exception {
    Clusters clusters = mock(Clusters.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    RepositoryVersionDAO repositoryVersionDAO = mock(RepositoryVersionDAO.class);
    ServiceDependencyDAO dependencyDAO = mock(ServiceDependencyDAO.class);
    PersistKeyValueImpl persistKeyValue = mock(PersistKeyValueImpl.class);
    ManagedDependencyDescriptorResolver scopedResolver = new ManagedDependencyDescriptorResolver(
        clusters, metaInfo, repositoryVersionDAO, dependencyDAO, persistKeyValue);

    long clusterId = 27L;
    StackId stack = new StackId("BIGTOP", "3.3.0");
    Cluster cluster = mock(Cluster.class);
    ClusterEntity clusterEntity = new ClusterEntity();
    Map<String, Service> services = new HashMap<>();
    Service existing = mock(Service.class);
    Service hbase = mock(Service.class);
    RepositoryVersionEntity repository = mock(RepositoryVersionEntity.class);
    when(clusters.getClusterById(clusterId)).thenReturn(cluster);
    when(cluster.getClusterId()).thenReturn(clusterId);
    when(cluster.getClusterName()).thenReturn("consumer-a");
    when(cluster.getClusterEntity()).thenReturn(clusterEntity);
    when(cluster.getDesiredStackVersion()).thenReturn(stack);
    when(cluster.getCurrentStackVersion()).thenReturn(stack);
    when(cluster.getSecurityType()).thenReturn(SecurityType.NONE);
    when(cluster.getServices()).thenReturn(services);
    when(cluster.getService("HBASE")).thenReturn(hbase);
    when(existing.getDesiredRepositoryVersion()).thenReturn(repository);
    when(hbase.getDesiredRepositoryVersion()).thenReturn(repository);
    when(hbase.getDesiredState()).thenReturn(State.INIT);
    when(hbase.getServiceComponents()).thenReturn(Map.of());
    when(repository.getId()).thenReturn(31L);
    when(repository.getParentId()).thenReturn(null);
    when(repository.getVersion()).thenReturn("3.3.0-1");
    when(repository.isResolved()).thenReturn(true);
    when(repositoryVersionDAO.findByPK(31L)).thenReturn(repository);
    when(dependencyDAO.findByConsumer(clusterId, "HBASE")).thenReturn(java.util.List.of());
    services.put("HDFS", existing);

    ServiceInfo hbaseInfo = mock(ServiceInfo.class);
    ComponentInfo hbaseClient = mock(ComponentInfo.class);
    when(metaInfo.getService("BIGTOP", "3.3.0", "HBASE")).thenReturn(hbaseInfo);
    when(hbaseInfo.getVersion()).thenReturn("2.4.17");
    when(hbaseInfo.getComponentByName("HBASE_CLIENT")).thenReturn(hbaseClient);
    when(hbaseClient.isClient()).thenReturn(true);
    when(hbaseClient.isVersionAdvertised()).thenReturn(true);
    when(hbaseClient.getCommandScript()).thenReturn(mock(CommandScriptDefinition.class));
    when(hbaseClient.getClientConfigFiles()).thenReturn(
        java.util.List.of(mock(ClientConfigFileDefinition.class)));
    ScopedWorkflowState workflow = new ScopedWorkflowState(9, "alice", "ADD_SERVICE",
        "SERVICES", Map.of("ADD_SERVICE", Map.of("addServiceSteps", Map.of(
            "SERVICES", Map.of("data", Map.of("services", Map.of(
                "HBASE", Map.of("selected", true))))))));
    when(persistKeyValue.getActiveOwnedClusterWorkflowState(
        clusterId, "ADD_SERVICE", 9)).thenReturn(workflow);

    ManagedDependencyDescriptor.Consumer plan = scopedResolver.resolveServicePlan(clusterId, 9);
    services.put("HBASE", hbase);
    ManagedDependencyDescriptor.Consumer live = scopedResolver.resolveService(
        new ManagedDependencyServiceKey(clusterId, "HBASE"));

    ManagedDependencySnapshotValidator validator = new ManagedDependencySnapshotValidator(false);
    assertEquals("SERVICE_PLAN", plan.sourceScope());
    assertEquals(ManagedDependencyDescriptor.ConsumerLifecycle.DRAFT, plan.lifecycle());
    assertEquals(validator.consumerFingerprint(plan), validator.consumerFingerprint(live));
  }

  @Test
  void completeSelectionIsImmutableAndRejectsDuplicateBindingIds() {
    UUID hdfsBinding = UUID.fromString("10000000-0000-0000-0000-000000000001");
    UUID zooKeeperBinding = UUID.fromString("f0000000-0000-0000-0000-000000000002");
    SortedMap<ManagedDependencyType, ManagedDependencyDescriptorResolver.BindingSelection> input =
        new TreeMap<>();
    input.put(ManagedDependencyType.HDFS,
        new ManagedDependencyDescriptorResolver.BindingSelection(hdfsBinding,
            new ManagedDependencyServiceKey(41L, "HDFS")));
    input.put(ManagedDependencyType.ZOOKEEPER,
        new ManagedDependencyDescriptorResolver.BindingSelection(zooKeeperBinding,
            new ManagedDependencyServiceKey(42L, "ZOOKEEPER")));

    ManagedDependencyDescriptorResolver.CompleteSelection selection =
        new ManagedDependencyDescriptorResolver.CompleteSelection(input);
    input.clear();
    assertEquals(Set.of(ManagedDependencyType.HDFS, ManagedDependencyType.ZOOKEEPER),
        selection.bindings().keySet());
    assertEquals(hdfsBinding, selection.bindings().get(ManagedDependencyType.HDFS).bindingId());
    assertThrows(IllegalArgumentException.class, () ->
        new ManagedDependencyDescriptorResolver.CompleteSelection(new TreeMap<>(Map.of(
            ManagedDependencyType.HDFS,
            new ManagedDependencyDescriptorResolver.BindingSelection(hdfsBinding,
                new ManagedDependencyServiceKey(41L, "HDFS")),
            ManagedDependencyType.ZOOKEEPER,
            new ManagedDependencyDescriptorResolver.BindingSelection(hdfsBinding,
                new ManagedDependencyServiceKey(42L, "ZOOKEEPER"))))));
  }

  @Test
  void completeProducerUsesRealAdapterForTwoProvidersAndNewPlanService() throws Exception {
    ProducerFixture fixture = new ProducerFixture(true);
    Consumer planned = fixture.consumer("SERVICE_PLAN", ConsumerLifecycle.DRAFT);
    Consumer live = fixture.consumer("SERVICE", ConsumerLifecycle.INIT_UNINSTALLED);

    Resolution plannedResolution = fixture.resolver.resolveSecureSelection(
        fixture.consumerCluster, planned, fixture.selection(), 7L);
    assertEquals(Set.of("HBASE", "HDFS", "ZOOKEEPER"),
        fixture.helper.lastPlannedServices.keySet());
    assertEquals(Set.of("ZOOKEEPER_CLIENT", "ZOOKEEPER_SERVER"),
        fixture.helper.lastPlannedServices.get("ZOOKEEPER"));
    assertEquals(Set.of("NAMENODE", "DATANODE"),
        fixture.helper.lastPlannedServices.get("HDFS"));
    assertEquals(Set.of(ManagedDependencyType.HDFS, ManagedDependencyType.ZOOKEEPER),
        plannedResolution.types());
    assertEquals("ambari-qa@EXAMPLE.COM", plannedResolution.consumerCalculation().smokePrincipal());

    fixture.consumerServices.put("HBASE", fixture.hbase);
    fixture.consumerServices.put("ZOOKEEPER", fixture.zookeeper);
    Resolution liveResolution = fixture.resolver.resolveSecureSelection(
        fixture.consumerCluster, live, fixture.selection(), null);

    assertEquals(plannedResolution.consumerCalculation().consumerLocalMapping(),
        liveResolution.consumerCalculation().consumerLocalMapping());
    assertEquals(Set.of("HBASE", "HDFS", "ZOOKEEPER"),
        fixture.helper.lastPlannedServices.keySet());
    assertEquals(plannedResolution.types(), liveResolution.types());
  }

  @Test
  void completeProducerRetainsExistingLocalCounterpartForSingleProvider() throws Exception {
    ProducerFixture fixture = new ProducerFixture(false);
    Consumer planned = fixture.consumer("SERVICE_PLAN", ConsumerLifecycle.DRAFT);
    Consumer live = fixture.consumer("SERVICE", ConsumerLifecycle.INIT_UNINSTALLED);

    Resolution plannedResolution = fixture.resolver.resolveSecureSelection(
        fixture.consumerCluster, planned, fixture.selection(), 7L);
    assertEquals(Set.of("HBASE", "HDFS"), fixture.helper.lastPlannedServices.keySet());
    assertEquals(Set.of("NAMENODE", "DATANODE"),
        fixture.helper.lastPlannedServices.get("HDFS"));
    assertEquals(Set.of(ManagedDependencyType.HDFS), plannedResolution.types());

    fixture.consumerServices.put("HBASE", fixture.hbase);
    Resolution liveResolution = fixture.resolver.resolveSecureSelection(
        fixture.consumerCluster, live, fixture.selection(), null);

    assertEquals(plannedResolution.consumerCalculation().consumerLocalMapping(),
        liveResolution.consumerCalculation().consumerLocalMapping());
  }

  @Test
  void servicePlanProducerRequiresTheExactOwnedCheckpointRevision() throws Exception {
    ProducerFixture fixture = new ProducerFixture(false);
    Consumer planned = fixture.consumer("SERVICE_PLAN", ConsumerLifecycle.DRAFT);

    ManagedDependencyIntegrationException missing = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> fixture.resolver.resolveSecureSelection(
            fixture.consumerCluster, planned, fixture.selection(), null));
    assertEquals("WORKFLOW_VERSION_CONFLICT", missing.getCode());

    ManagedDependencyIntegrationException stale = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> fixture.resolver.resolveSecureSelection(
            fixture.consumerCluster, planned, fixture.selection(), 8L));
    assertEquals("WORKFLOW_VERSION_CONFLICT", stale.getCode());
  }

  @Test
  void typeAwareLiveResolverClassifiesBothCreationOrdersAndInstalledLocal() throws Exception {
    Clusters clusters = mock(Clusters.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    RepositoryVersionDAO repositoryVersionDAO = mock(RepositoryVersionDAO.class);
    ServiceDependencyDAO dependencyDAO = mock(ServiceDependencyDAO.class);
    PersistKeyValueImpl persistKeyValue = mock(PersistKeyValueImpl.class);
    ServiceInfo hbaseInfo = lifecycleHbaseInfo();
    when(metaInfo.getService("BIGTOP", "3.2.0", "HBASE")).thenReturn(hbaseInfo);

    Cluster hdfsFirst = lifecycleCluster(51L, State.INIT, State.INIT);
    Cluster zooKeeperFirst = lifecycleCluster(52L, State.INIT, State.INIT);
    Cluster installed = lifecycleCluster(53L, State.STARTED, State.STARTED);
    when(clusters.getClusterById(51L)).thenReturn(hdfsFirst);
    when(clusters.getClusterById(52L)).thenReturn(zooKeeperFirst);
    when(clusters.getClusterById(53L)).thenReturn(installed);

    ServiceDependencyBindingEntity hdfsBinding = mock(ServiceDependencyBindingEntity.class);
    when(hdfsBinding.getDependencyType()).thenReturn("HDFS");
    ServiceDependencyBindingEntity zooKeeperBinding = mock(ServiceDependencyBindingEntity.class);
    when(zooKeeperBinding.getDependencyType()).thenReturn("ZOOKEEPER");
    when(dependencyDAO.findByConsumer(51L, "HBASE")).thenReturn(List.of(hdfsBinding));
    when(dependencyDAO.findByConsumer(52L, "HBASE")).thenReturn(List.of(zooKeeperBinding));
    when(dependencyDAO.findByConsumer(53L, "HBASE")).thenReturn(List.of());

    ManagedDependencyDescriptorResolver scopedResolver = new ManagedDependencyDescriptorResolver(
        clusters, metaInfo, repositoryVersionDAO, dependencyDAO, persistKeyValue);
    assertEquals(ConsumerLifecycle.MANAGED_UPDATE,
        scopedResolver.resolveService(new ManagedDependencyServiceKey(51L, "HBASE"),
            ManagedDependencyType.HDFS).lifecycle());
    assertEquals(ConsumerLifecycle.INIT_UNINSTALLED,
        scopedResolver.resolveService(new ManagedDependencyServiceKey(51L, "HBASE"),
            ManagedDependencyType.ZOOKEEPER).lifecycle());
    assertEquals(ConsumerLifecycle.MANAGED_UPDATE,
        scopedResolver.resolveService(new ManagedDependencyServiceKey(52L, "HBASE"),
            ManagedDependencyType.ZOOKEEPER).lifecycle());
    assertEquals(ConsumerLifecycle.INIT_UNINSTALLED,
        scopedResolver.resolveService(new ManagedDependencyServiceKey(52L, "HBASE"),
            ManagedDependencyType.HDFS).lifecycle());
    Consumer installedLocal = scopedResolver.resolveService(
        new ManagedDependencyServiceKey(53L, "HBASE"), ManagedDependencyType.ZOOKEEPER);
    assertEquals(ConsumerLifecycle.INSTALLED_LOCAL, installedLocal.lifecycle());
    assertTrue(new ManagedDependencySnapshotValidator(true).validate(
        UUID.fromString("33333333-3333-4333-8333-333333333333"), 1,
        ManagedDependencyType.ZOOKEEPER, installedLocal, zooKeeperProvider(53L), List.of())
        .hasError(ManagedDependencyErrorCode.DEPENDENCY_LOCAL_DATA_MIGRATION_UNSUPPORTED));
  }

  private ServiceInfo lifecycleHbaseInfo() {
    ServiceInfo info = mock(ServiceInfo.class);
    when(info.getVersion()).thenReturn("2.4.17");
    ComponentInfo client = mock(ComponentInfo.class);
    when(info.getComponentByName("HBASE_CLIENT")).thenReturn(client);
    when(client.isClient()).thenReturn(true);
    when(client.isVersionAdvertised()).thenReturn(true);
    when(client.getCommandScript()).thenReturn(mock(CommandScriptDefinition.class));
    when(client.getClientConfigFiles()).thenReturn(
        List.of(mock(ClientConfigFileDefinition.class)));
    return info;
  }

  private Cluster lifecycleCluster(long clusterId, State serviceState, State hostState) throws Exception {
    RepositoryVersionEntity repository = mock(RepositoryVersionEntity.class);
    when(repository.getId()).thenReturn(1L);
    when(repository.getVersion()).thenReturn("3.2.0-1");
    when(repository.isResolved()).thenReturn(true);
    ServiceComponentHost host = mock(ServiceComponentHost.class);
    when(host.getUpgradeState()).thenReturn(UpgradeState.NONE);
    when(host.getState()).thenReturn(hostState);
    when(host.getDesiredState()).thenReturn(hostState);
    when(host.getVersion()).thenReturn("3.2.0-1");
    ServiceComponent client = mock(ServiceComponent.class);
    when(client.getDesiredRepositoryVersion()).thenReturn(repository);
    when(client.isVersionAdvertised()).thenReturn(true);
    when(client.isClientComponent()).thenReturn(true);
    when(client.getServiceComponentHosts()).thenReturn(Map.of("hbase.example.test", host));
    Service hbase = mock(Service.class);
    when(hbase.getDesiredRepositoryVersion()).thenReturn(repository);
    when(hbase.getDesiredState()).thenReturn(serviceState);
    when(hbase.getServiceComponents()).thenReturn(Map.of("HBASE_CLIENT", client));
    StackId stack = new StackId("BIGTOP", "3.2.0");
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterId()).thenReturn(clusterId);
    when(cluster.getClusterName()).thenReturn("consumer-" + clusterId);
    when(cluster.getClusterEntity()).thenReturn(new ClusterEntity());
    when(cluster.getDesiredStackVersion()).thenReturn(stack);
    when(cluster.getCurrentStackVersion()).thenReturn(stack);
    when(cluster.getUpgradeInProgress()).thenReturn(null);
    when(cluster.getSecurityType()).thenReturn(SecurityType.KERBEROS);
    when(cluster.getService("HBASE")).thenReturn(hbase);
    when(cluster.getServices()).thenReturn(Map.of("HBASE", hbase));
    doReturn(config(Map.of(
        "realm", "EXAMPLE.COM"))).when(cluster).getDesiredConfigByType("kerberos-env");
    return cluster;
  }

  private KerberosDescriptor compositeDescriptor() throws Exception {
    KerberosDescriptorFactory descriptorFactory = new KerberosDescriptorFactory();
    KerberosDescriptor root = descriptorFactory.createInstance(loadResource(
        "/kerberos/test_kerberos_descriptor_simple.json"));
    root.update(descriptorFactory.createInstance(loadResource(
        "/stacks/BIGTOP/3.2.0/services/HBASE/kerberos.json")));
    root.update(descriptorFactory.createInstance(loadResource(
        "/stacks/BIGTOP/3.2.0/services/ZOOKEEPER/kerberos.json")));
    return root;
  }

  private static Provider zooKeeperProvider(long clusterId) {
    ManagedDependencyVersion version = new ManagedDependencyVersion(
        "BIGTOP", "3.2.0", true, "3.2.0",
        new TreeMap<>(Map.of("distribution", "3.2.0-1")), new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 1L, List.of());
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "zookeeper", new TreeSet<>(), false, "", false, "0700", false);
    ManagedDependencyDescriptor.ZooKeeperEndpoint endpoint =
        new ManagedDependencyDescriptor.ZooKeeperEndpoint(
            List.of("zk.example.test"), 2181, true, "zookeeper", true,
            List.of("/hbase"), true, true, "DEFAULT");
    return new Provider(new ManagedDependencyServiceKey(clusterId, "ZOOKEEPER"),
        ManagedDependencyType.ZOOKEEPER, version, true, true,
        ManagedDependencySecurityMode.KERBEROS, "EXAMPLE.COM", identity, null, endpoint,
        Map.of(), Map.of(), new TreeMap<>(Map.of("hbase.zookeeper.quorum", "zk.example.test")),
        Set.of("hbase.zookeeper.quorum"), Set.of());
  }

  final class ProducerFixture {
    private static final long CONSUMER_CLUSTER_ID = 11L;
    private static final long PROVIDER_CLUSTER_ID = 22L;
    private static final long ZOOKEEPER_CLUSTER_ID = 33L;
    private static final String REALM = "EXAMPLE.COM";
    private final UUID hdfsBinding =
        UUID.fromString("11111111-1111-4111-8111-111111111111");
    private final UUID zooKeeperBinding =
        UUID.fromString("22222222-2222-4222-8222-222222222222");

    private final boolean includeZooKeeper;
    private final StackId stack;
    final Map<String, Service> consumerServices = new TreeMap<>();
    final Service hbase = mock(Service.class);
    final Service zookeeper = mock(Service.class);
    final Cluster consumerCluster = mock(Cluster.class);
    final Cluster providerCluster;
    final Cluster zooKeeperProvider;
    final RecordingKerberosHelper helper = new RecordingKerberosHelper();
    final ManagedDependencyDescriptorResolver resolver;

    ProducerFixture(boolean includeZooKeeper) throws Exception {
      this(includeZooKeeper, "3.2.0");
    }

    ProducerFixture(boolean includeZooKeeper, String stackVersion) throws Exception {
      this.stack = new StackId("BIGTOP", stackVersion);
      this.includeZooKeeper = includeZooKeeper;
      AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
      ServiceInfo hbaseInfo = serviceInfo("HBASE", "2.4.17",
          "HBASE_CLIENT", "HBASE_MASTER", "HBASE_REGIONSERVER", "HBASE_THRIFT");
      ServiceInfo hdfsInfo = serviceInfo("HDFS", stack.getStackVersion(),
          "HDFS_CLIENT", "NAMENODE", "DATANODE");
      ServiceInfo zooKeeperInfo = serviceInfo("ZOOKEEPER", stack.getStackVersion(),
          "ZOOKEEPER_CLIENT", "ZOOKEEPER_SERVER");
      KerberosDescriptor descriptor = compositeDescriptor();
      when(metaInfo.getService("BIGTOP", stack.getStackVersion(), "HBASE")).thenReturn(hbaseInfo);
      when(metaInfo.getService("BIGTOP", stack.getStackVersion(), "HDFS")).thenReturn(hdfsInfo);
      when(metaInfo.getService("BIGTOP", stack.getStackVersion(), "ZOOKEEPER")).thenReturn(zooKeeperInfo);
      when(metaInfo.getKerberosDescriptor("BIGTOP", stack.getStackVersion(), false)).thenReturn(descriptor);
      when(metaInfo.getServiceProperties("BIGTOP", stack.getStackVersion(), "KERBEROS"))
          .thenReturn(Set.of(stockTemplateProperty()));

      configureHelper(metaInfo);
      providerCluster = providerCluster();
      zooKeeperProvider = zooKeeperProvider();
      Clusters clusters = mock(Clusters.class);
      when(clusters.getClusterById(CONSUMER_CLUSTER_ID)).thenReturn(consumerCluster);
      when(clusters.getCluster("consumer-a")).thenReturn(consumerCluster);
      when(clusters.getClusterById(PROVIDER_CLUSTER_ID)).thenReturn(providerCluster);
      when(clusters.getClusterById(ZOOKEEPER_CLUSTER_ID)).thenReturn(zooKeeperProvider);
      configureConsumerCluster();

      AmbariManagementController controller = mock(AmbariManagementController.class);
      ConfigHelper configHelper = mock(ConfigHelper.class);
      when(controller.getKerberosHelper()).thenReturn(helper);
      when(controller.getConfigHelper()).thenReturn(configHelper);
      when(configHelper.calculateExistingConfigurations(controller, consumerCluster, null, null))
          .thenReturn(consumerConfigurations());

      PersistKeyValueImpl persistKeyValue = mock(PersistKeyValueImpl.class);
      when(persistKeyValue.getActiveOwnedClusterWorkflowState(
          CONSUMER_CLUSTER_ID, "ADD_SERVICE", 7L)).thenReturn(servicePlanCheckpoint());
      ManagedHBaseSecurityDescriptorAdapter adapter =
          new ManagedHBaseSecurityDescriptorAdapter(helper);
      resolver = new ManagedDependencyDescriptorResolver(clusters, metaInfo, null, null,
          persistKeyValue, () -> controller, () -> adapter);
    }

    private void configureHelper(AmbariMetaInfo metaInfo) throws Exception {
      helper.setField("ambariMetaInfo", metaInfo);
      helper.setField("artifactDAO", mock(ArtifactDAO.class));
      helper.setField("kerberosDescriptorFactory", new KerberosDescriptorFactory());
      helper.setField("variableReplacementHelper", new VariableReplacementHelper());
      helper.setField("managedHBaseKerberosLivePlanProvider",
          (ManagedHBaseKerberosLivePlanProvider) ignored -> {
            throw new AssertionError("producer calculation must not resolve a live binding");
          });
    }

    private Cluster providerCluster() throws Exception {
      RepositoryVersionEntity repository = mock(RepositoryVersionEntity.class);
      when(repository.getId()).thenReturn(1L);
      when(repository.getVersion()).thenReturn((stack.getStackVersion() + "-1"));
      when(repository.isResolved()).thenReturn(true);
      ServiceComponentHost host = mock(ServiceComponentHost.class);
      when(host.getUpgradeState()).thenReturn(UpgradeState.NONE);
      when(host.getState()).thenReturn(State.INSTALLED);
      when(host.getDesiredState()).thenReturn(State.INSTALLED);
      when(host.getVersion()).thenReturn((stack.getStackVersion() + "-1"));
      ServiceComponent client = mock(ServiceComponent.class);
      when(client.getDesiredRepositoryVersion()).thenReturn(repository);
      when(client.isVersionAdvertised()).thenReturn(true);
      when(client.isClientComponent()).thenReturn(true);
      when(client.getServiceComponentHosts()).thenReturn(Map.of("nn.example.test", host));
      Service hdfs = mock(Service.class);
      when(hdfs.getDesiredRepositoryVersion()).thenReturn(repository);
      when(hdfs.getDesiredState()).thenReturn(State.STARTED);
      when(hdfs.getServiceComponents()).thenReturn(Map.of("HDFS_CLIENT", client));
      Cluster provider = mock(Cluster.class);
      when(provider.getClusterId()).thenReturn(PROVIDER_CLUSTER_ID);
      when(provider.getClusterName()).thenReturn("hdfs-provider");
      when(provider.getResourceId()).thenReturn(202L);
      when(provider.getDesiredStackVersion()).thenReturn(stack);
      when(provider.getCurrentStackVersion()).thenReturn(stack);
      when(provider.getUpgradeInProgress()).thenReturn(null);
      when(provider.getSecurityType()).thenReturn(SecurityType.KERBEROS);
      when(provider.getService("HDFS")).thenReturn(hdfs);
      when(provider.getServices()).thenReturn(Map.of("HDFS", hdfs));
      when(provider.getConfigGroups()).thenReturn(Map.of());
      doReturn(config(Map.of(
          "fs.defaultFS", "hdfs://nn.example.test:8020",
          "hadoop.security.authentication", "kerberos",
          "hadoop.security.auth_to_local", "DEFAULT"))).when(provider).getDesiredConfigByType("core-site");
      doReturn(config(Map.of("dfs.namenode.kerberos.principal", "nn/_HOST@" + REALM,
          "dfs.datanode.kerberos.principal", "dn/_HOST@" + REALM))).when(provider).getDesiredConfigByType("hdfs-site");
      doReturn(config(Map.of(
          "realm", REALM, "manage_auth_to_local", "true"))).when(provider).getDesiredConfigByType("kerberos-env");
      doReturn(config(Map.of(
          "manage_krb5_conf", "true", "realm", REALM, "conf_dir", "/etc",
          "content", STOCK_TEMPLATE))).when(provider).getDesiredConfigByType("krb5-conf");
      return provider;
    }

    private Cluster zooKeeperProvider() throws Exception {
      RepositoryVersionEntity repository = mock(RepositoryVersionEntity.class);
      when(repository.getId()).thenReturn(1L);
      when(repository.getVersion()).thenReturn((stack.getStackVersion() + "-1"));
      when(repository.isResolved()).thenReturn(true);
      ServiceComponentHost host = mock(ServiceComponentHost.class);
      when(host.getUpgradeState()).thenReturn(UpgradeState.NONE);
      when(host.getState()).thenReturn(State.STARTED);
      when(host.getDesiredState()).thenReturn(State.STARTED);
      when(host.getVersion()).thenReturn((stack.getStackVersion() + "-1"));
      ServiceComponent server = mock(ServiceComponent.class);
      when(server.getName()).thenReturn("ZOOKEEPER_SERVER");
      when(server.getDesiredRepositoryVersion()).thenReturn(repository);
      when(server.isVersionAdvertised()).thenReturn(true);
      when(server.isClientComponent()).thenReturn(false);
      when(server.getServiceComponentHosts()).thenReturn(Map.of("zk.example.test", host));
      Service zookeeperService = mock(Service.class);
      when(zookeeperService.getDesiredRepositoryVersion()).thenReturn(repository);
      when(zookeeperService.getDesiredState()).thenReturn(State.STARTED);
      when(zookeeperService.getServiceComponents()).thenReturn(
          Map.of("ZOOKEEPER_SERVER", server));
      Cluster provider = mock(Cluster.class);
      when(provider.getClusterId()).thenReturn(ZOOKEEPER_CLUSTER_ID);
      when(provider.getClusterName()).thenReturn("zookeeper-provider");
      when(provider.getResourceId()).thenReturn(303L);
      when(provider.getDesiredStackVersion()).thenReturn(stack);
      when(provider.getCurrentStackVersion()).thenReturn(stack);
      when(provider.getUpgradeInProgress()).thenReturn(null);
      when(provider.getSecurityType()).thenReturn(SecurityType.KERBEROS);
      when(provider.getService("ZOOKEEPER")).thenReturn(zookeeperService);
      when(provider.getServices()).thenReturn(Map.of("ZOOKEEPER", zookeeperService));
      when(provider.getConfigGroups()).thenReturn(Map.of());
      doReturn(config(Map.of(
          "clientPort", "2181",
          "kerberos.removeHostFromPrincipal", "true",
          "kerberos.removeRealmFromPrincipal", "true",
          "security.auth_to_local", "DEFAULT",
          "authProvider.1",
              "org.apache.zookeeper.server.auth.SASLAuthenticationProvider"))).when(provider).getDesiredConfigByType("zoo.cfg");
      doReturn(config(Map.of(
          "zookeeper.znode.parent", "/hbase"))).when(provider).getDesiredConfigByType("hbase-site");
      doReturn(config(Map.of(
          "zookeeper_principal_name", "zookeeper/_HOST@" + REALM))).when(provider).getDesiredConfigByType("zookeeper-env");
      doReturn(config(Map.of(
          "realm", REALM))).when(provider).getDesiredConfigByType("kerberos-env");
      return provider;
    }

    private void configureConsumerCluster() {
      Service localHdfs = mock(Service.class);
      ServiceComponent namenode = mock(ServiceComponent.class);
      ServiceComponent datanode = mock(ServiceComponent.class);
      when(localHdfs.getServiceComponents()).thenReturn(Map.of(
          "NAMENODE", namenode, "DATANODE", datanode));
      consumerServices.put("HDFS", localHdfs);
      when(hbase.getServiceComponents()).thenReturn(Map.of(
          "HBASE_CLIENT", mock(ServiceComponent.class),
          "HBASE_MASTER", mock(ServiceComponent.class),
          "HBASE_REGIONSERVER", mock(ServiceComponent.class),
          "HBASE_THRIFT", mock(ServiceComponent.class)));
      when(zookeeper.getServiceComponents()).thenReturn(Map.of(
          "ZOOKEEPER_CLIENT", mock(ServiceComponent.class),
          "ZOOKEEPER_SERVER", mock(ServiceComponent.class)));
      for (Service service : List.of(localHdfs, hbase, zookeeper)) {
        service.getServiceComponents().forEach((name, component) -> {
          when(component.getName()).thenReturn(name);
          when(component.isClientComponent()).thenReturn(name.endsWith("_CLIENT"));
        });
      }
      when(consumerCluster.getClusterId()).thenReturn(CONSUMER_CLUSTER_ID);
      when(consumerCluster.getResourceId()).thenReturn(101L);
      when(consumerCluster.getClusterName()).thenReturn("consumer-a");
      when(consumerCluster.getDesiredStackVersion()).thenReturn(stack);
      when(consumerCluster.getCurrentStackVersion()).thenReturn(stack);
      when(consumerCluster.getUpgradeInProgress()).thenReturn(null);
      when(consumerCluster.getSecurityType()).thenReturn(SecurityType.KERBEROS);
      when(consumerCluster.getServices()).thenReturn(consumerServices);
      when(consumerCluster.getHosts()).thenReturn(List.of());
      doReturn(config(Map.of(
          "realm", REALM))).when(consumerCluster).getDesiredConfigByType("krb5-conf");
      doReturn(config(
          consumerConfigurations().get("kerberos-env"))).when(consumerCluster).getDesiredConfigByType("kerberos-env");
    }

    private ServiceInfo serviceInfo(String serviceName, String version, String clientName,
        String... componentNames) {
      ServiceInfo info = mock(ServiceInfo.class);
      when(info.getVersion()).thenReturn(version);
      ComponentInfo client = component(clientName, true);
      when(info.getComponentByName(clientName)).thenReturn(client);
      List<ComponentInfo> components = new java.util.ArrayList<>();
      components.add(client);
      for (String componentName : componentNames) {
        components.add(component(componentName, false));
      }
      when(info.getComponents()).thenReturn(components);
      return info;
    }

    private ComponentInfo component(String name, boolean client) {
      ComponentInfo component = mock(ComponentInfo.class);
      when(component.getName()).thenReturn(name);
      when(component.isClient()).thenReturn(client);
      when(component.isVersionAdvertised()).thenReturn(true);
      when(component.getCommandScript()).thenReturn(mock(CommandScriptDefinition.class));
      when(component.getClientConfigFiles()).thenReturn(
          client ? List.of(mock(ClientConfigFileDefinition.class)) : List.of());
      return component;
    }

    private Map<String, Map<String, String>> consumerConfigurations() {
      Map<String, Map<String, String>> configurations = new TreeMap<>();
      configurations.put("", new TreeMap<>(Map.of("principal_suffix", "")));
      configurations.put("cluster-env", new TreeMap<>(Map.of(
          "smokeuser", "ambari-qa", "smokeuser_principal_name", "ambari-qa@" + REALM,
          "user_group", "hadoop")));
      configurations.put("hadoop-env", new TreeMap<>(Map.of("hdfs_user", "hdfs",
          "proxyuser_group", "hadoop")));
      configurations.put("kerberos-env", new TreeMap<>(Map.of(
          KerberosHelper.DEFAULT_REALM, REALM, KerberosHelper.KDC_TYPE, "mit-kdc",
          KerberosHelper.MANAGE_IDENTITIES, "true",
          KerberosHelper.MANAGE_AUTH_TO_LOCAL_RULES, "true",
          KerberosHelper.CASE_INSENSITIVE_USERNAME_RULES, "false",
          KerberosHelper.INCLUDE_ALL_COMPONENTS_IN_AUTH_TO_LOCAL_RULES, "false",
          KerberosHelper.CREATE_AMBARI_PRINCIPAL, "false")));
      configurations.put("zookeeper-env", new TreeMap<>(Map.of(
          "zk_user", "zookeeper",
          "zookeeper_principal_name", "zookeeper/_HOST@" + REALM,
          "zookeeper_keytab_path", "/etc/security/keytabs/zk.service.keytab")));
      configurations.put("core-site", new TreeMap<>(Map.of(
          "fs.defaultFS", "hdfs://local-ns")));
      return configurations;
    }

    private ScopedWorkflowState servicePlanCheckpoint() {
      Map<String, Object> selected = new TreeMap<>();
      selected.put("HBASE", Map.of("selected", true));
      selected.put("HDFS", Map.of("selected", true));
      if (includeZooKeeper) {
        selected.put("ZOOKEEPER", Map.of("selected", true));
      }
      return new ScopedWorkflowState(7, "alice", "ADD_SERVICE", "SERVICES", Map.of(
          "ADD_SERVICE", Map.of("addServiceSteps", Map.of("SERVICES", Map.of("data", Map.of(
              "services", selected))))));
    }

    Consumer consumer(String source, ConsumerLifecycle lifecycle) {
      Plan plan = Plan.forExistingCluster(CONSUMER_CLUSTER_ID);
      String user = plan.plannedShortUser();
      ManagedDependencyIdentity identity = new ManagedDependencyIdentity(user,
          new TreeSet<>(Set.of(user + "/_HOST@" + REALM)), false, user, true, "0700", true);
      ManagedDependencyVersion version = new ManagedDependencyVersion(
          "BIGTOP", stack.getStackVersion(), true, "2.4.17",
          new TreeMap<>(Map.of("distribution", (stack.getStackVersion() + "-1"))), new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 1L, List.of());
      return new Consumer(source, null, CONSUMER_CLUSTER_ID, "consumer-a", "HBASE", lifecycle,
          version, ManagedDependencySecurityMode.KERBEROS, REALM, identity, plan);
    }

    ManagedDependencyDescriptorResolver.CompleteSelection selection() {
      SortedMap<ManagedDependencyType,
          ManagedDependencyDescriptorResolver.BindingSelection> selections = new TreeMap<>();
      selections.put(ManagedDependencyType.HDFS,
          new ManagedDependencyDescriptorResolver.BindingSelection(hdfsBinding,
              new ManagedDependencyServiceKey(PROVIDER_CLUSTER_ID, "HDFS")));
      if (includeZooKeeper) {
        selections.put(ManagedDependencyType.ZOOKEEPER,
            new ManagedDependencyDescriptorResolver.BindingSelection(zooKeeperBinding,
                new ManagedDependencyServiceKey(ZOOKEEPER_CLUSTER_ID, "ZOOKEEPER")));
      }
      return new ManagedDependencyDescriptorResolver.CompleteSelection(selections);
    }
  }

  private static final class RecordingKerberosHelper extends KerberosHelperImpl {
    private Map<String, Set<String>> lastPlannedServices = Map.of();

    @Override
    public ManagedHBaseKerberosCalculation calculateManagedHBaseKerberosConfiguration(
        Cluster cluster, KerberosDescriptor rawEffectiveComposite,
        @Nullable KerberosDescriptor rawUserDescriptor,
        Map<String, Map<String, String>> existingConfigurations,
        Map<String, Set<String>> plannedServices,
        ManagedHBaseKerberosOverlaySpec overlaySpec,
        boolean applyStackAdvisorUpdates)
        throws KerberosInvalidConfigurationException, AmbariException {
      Map<String, Set<String>> copied = new TreeMap<>();
      plannedServices.forEach((service, components) ->
          copied.put(service, new TreeSet<>(components)));
      lastPlannedServices = copied;
      return super.calculateManagedHBaseKerberosConfiguration(cluster, rawEffectiveComposite,
          rawUserDescriptor, existingConfigurations, plannedServices, overlaySpec,
          applyStackAdvisorUpdates);
    }

    private void setField(String fieldName, Object value) throws Exception {
      Field field = KerberosHelperImpl.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(this, value);
    }
  }

  private static String loadStockTemplate() {
    return loadResource(STOCK_TEMPLATE_RESOURCE);
  }

  private static String loadResource(String resource) {
    try (InputStream stream = ManagedDependencyDescriptorResolverTest.class
        .getResourceAsStream(resource)) {
      if (stream == null) {
        throw new IllegalStateException("Missing BIGTOP stock krb5 template resource");
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read BIGTOP stock krb5 template resource", e);
    }
  }

  private static PropertyInfo stockTemplateProperty() {
    PropertyInfo property = new PropertyInfo();
    property.setFilename("krb5-conf.xml");
    property.setName("content");
    property.setValue(STOCK_TEMPLATE);
    property.setPropertyTypes(Set.of(PropertyInfo.PropertyType.VALUE_FROM_PROPERTY_FILE));
    return property;
  }

  private static Config config(Map<String, String> values) {
    Config config = mock(Config.class);
    when(config.getProperties()).thenReturn(new TreeMap<>(values));
    return config;
  }

  private static Provider hdfsProvider(long clusterId) {
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hdfs", new TreeSet<>(), false, "", false, "0700", false);
    ManagedDependencyVersion version = new ManagedDependencyVersion(
        "BIGTOP", "3.2.0", true, "HDFS-3.2.0",
        new TreeMap<>(Map.of("repository.version", "3.2.0-1")), new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")),
        1L, List.of());
    return new Provider(new ManagedDependencyServiceKey(clusterId, "HDFS"),
        ManagedDependencyType.HDFS, version, true, true,
        ManagedDependencySecurityMode.KERBEROS, "EXAMPLE.COM", identity,
        new ManagedDependencyDescriptor.HdfsEndpoint("hdfs://nn.example.test:8020", false,
            "", Map.of(), "", false, false), null,
        Map.of("fs.defaultFS", "hdfs://nn.example.test:8020",
            "hadoop.security.auth_to_local",
            "RULE:[1:$1@$0](hbase@EXAMPLE.COM)s/.*/hbase/"),
        Map.of(), Map.of(), Set.of("fs.defaultFS"), Set.of());
  }
}
