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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.PREVIEW_SCHEMA_VERSION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.persistence.PersistenceException;

import org.apache.ambari.server.api.services.PersistKeyValueImpl;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Consumer;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ConsumerLifecycle;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.HdfsEndpoint;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Provider;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.ConsumerReference;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.CreateRequest;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.DraftReference;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.LifecycleRequest;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.ProviderReference;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyOperationEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class ManagedServiceDependencyCoordinatorTest {
  private ManagedDependencyDescriptorResolver resolver;
  private ServiceDependencyDAO dependencyDAO;
  private PersistKeyValueImpl persistKeyValue;
  private ManagedServiceDependencyCoordinator coordinator;

  @BeforeEach
  void setUp() {
    resolver = mock(ManagedDependencyDescriptorResolver.class);
    dependencyDAO = mock(ServiceDependencyDAO.class);
    persistKeyValue = mock(PersistKeyValueImpl.class);
    coordinator = new ManagedServiceDependencyCoordinator(resolver, dependencyDAO, persistKeyValue);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void servicePlanAuthorizesConsumerBeforeReadingWorkflowAndOmitsForbiddenProviders() {
    Cluster consumerCluster = cluster(11L, 101L, "consumer", Map.of());
    Cluster providerCluster = cluster(22L, 202L, "provider", Map.of("HDFS", mock(Service.class)));
    when(resolver.cluster(11L)).thenReturn(consumerCluster);
    when(resolver.resolveServicePlan(11L, 7L)).thenReturn(consumer(11L));
    when(resolver.allClusters()).thenReturn(Map.of("provider", providerCluster));

    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator("alice", 202L));
    assertThrows(AuthorizationException.class, () -> coordinator.candidates(
        ConsumerReference.servicePlan(11L, 7L), ManagedDependencyType.HDFS));
    verify(resolver, never()).resolveServicePlan(11L, 7L);

    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator("alice", 101L));
    assertEquals(List.of(), coordinator.candidates(
        ConsumerReference.servicePlan(11L, 7L), ManagedDependencyType.HDFS));
    verify(resolver, never()).resolveProvider(
        new ManagedDependencyServiceKey(22L, "HDFS"));
  }

  @Test
  void servicePlanPreviewRequiresProviderAuthorityIndependently() {
    Cluster consumerCluster = cluster(11L, 101L, "consumer", Map.of());
    Cluster providerCluster = cluster(22L, 202L, "provider", Map.of("HDFS", mock(Service.class)));
    when(resolver.cluster(11L)).thenReturn(consumerCluster);
    when(resolver.cluster(22L)).thenReturn(providerCluster);
    when(resolver.resolveServicePlan(11L, 7L)).thenReturn(consumer(11L));
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator("alice", 101L));

    assertThrows(AuthorizationException.class, () -> coordinator.preview(
        ConsumerReference.servicePlan(11L, 7L), ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS")));
    verify(resolver, never()).resolveProvider(
        new ManagedDependencyServiceKey(22L, "HDFS"));
  }

  @Test
  void exactPreviewIdentityKeepsNamespaceAndFingerprintAcrossPlanAndLiveScopes() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000021");
    UUID otherBindingId = UUID.fromString("00000000-0000-4000-8000-000000000022");
    Cluster consumerCluster = cluster(11L, 101L, "consumer", Map.of("HBASE", mock(Service.class)));
    Cluster providerCluster = cluster(22L, 202L, "provider", Map.of("HDFS", mock(Service.class)));
    when(resolver.cluster(11L)).thenReturn(consumerCluster);
    when(resolver.cluster(22L)).thenReturn(providerCluster);
    when(resolver.resolveServicePlan(11L, 7L)).thenReturn(consumer(11L));
    when(resolver.resolveService(new ManagedDependencyServiceKey(11L, "HBASE")))
        .thenReturn(consumer(11L));
    when(resolver.resolveProvider(new ManagedDependencyServiceKey(22L, "HDFS")))
        .thenReturn(provider());
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));

    Map<String, Object> planned = coordinator.preview(
        ConsumerReference.servicePlan(11L, 7L), ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"), bindingId);
    Map<String, Object> live = coordinator.preview(
        ConsumerReference.service(11L), ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"), bindingId);
    Map<String, Object> other = coordinator.preview(
        ConsumerReference.service(11L), ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"), otherBindingId);

    assertEquals(planned.get("namespace"), live.get("namespace"));
    assertEquals(planned.get("provider_fingerprint"), live.get("provider_fingerprint"));
    org.junit.jupiter.api.Assertions.assertNotEquals(
        live.get("provider_fingerprint"), other.get("provider_fingerprint"));
    verify(dependencyDAO, never()).create(
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void completeSecureCoordinatorUsesTheRealD2ProducerForBothRemoteProviders() throws Exception {
    ManagedDependencyDescriptorResolverTest resolverTest =
        new ManagedDependencyDescriptorResolverTest();
    ManagedDependencyDescriptorResolverTest.ProducerFixture fixture =
        resolverTest.new ProducerFixture(true);
    fixture.consumerServices.put("HBASE", fixture.hbase);
    fixture.consumerServices.put("ZOOKEEPER", fixture.zookeeper);

    ManagedDependencyDescriptorResolver realResolver = spy(fixture.resolver);
    Consumer live = fixture.consumer("SERVICE", ConsumerLifecycle.INIT_UNINSTALLED);
    doReturn(live).when(realResolver).resolveService(any(ManagedDependencyServiceKey.class));
    doReturn(live).when(realResolver).resolveService(any(ManagedDependencyServiceKey.class),
        any(ManagedDependencyType.class));
    ServiceDependencyDAO realDao = mock(ServiceDependencyDAO.class);
    when(realDao.findAllBindings()).thenReturn(List.of());
    executeReadOperations(fixture.consumerCluster);
    executeReadOperations(fixture.providerCluster);
    executeReadOperations(fixture.zooKeeperProvider);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));
    ManagedServiceDependencyCoordinator checked =
        new ManagedServiceDependencyCoordinator(realResolver, realDao, persistKeyValue);

    UUID hdfsBinding = UUID.fromString("11111111-1111-4111-8111-111111111111");
    UUID zooKeeperBinding = UUID.fromString("22222222-2222-4222-8222-222222222222");
    Map<String, Object> response = checked.preview(ConsumerReference.service(11L), List.of(
        new ManagedServiceDependencyCoordinator.PreviewSelection(
            ManagedDependencyType.HDFS, new ProviderReference(22L, "HDFS"), hdfsBinding),
        new ManagedServiceDependencyCoordinator.PreviewSelection(
            ManagedDependencyType.ZOOKEEPER,
            new ProviderReference(33L, "ZOOKEEPER"), zooKeeperBinding)));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
    assertEquals(2, items.size());
    assertEquals("HDFS", items.get(0).get("dependency_type"));
    assertEquals("ZOOKEEPER", items.get(1).get("dependency_type"));
    assertEquals(items.get(0).get("consumer_descriptor_fingerprint"),
        items.get(1).get("consumer_descriptor_fingerprint"));
    for (Map<String, Object> item : items) {
      assertEquals(true, item.get("compatible"));
      assertNotNull(item.get("consumer_descriptor_fingerprint"));
      assertNotNull(item.get("provider_fingerprint"));
      assertNotNull(item.get("snapshot_fingerprint"));
    }
    verify(realResolver).resolveSecureSelection(any(Cluster.class), any(Consumer.class),
        any(ManagedDependencyDescriptorResolver.CompleteSelection.class), isNull(Long.class));
    verify(realDao, never()).createBatch(any());
  }

  @Test
  void secureCompleteCreateRejectsAStaleSecondItemBeforeBatchPersistence() throws Exception {
    ManagedDependencyDescriptorResolverTest resolverTest =
        new ManagedDependencyDescriptorResolverTest();
    ManagedDependencyDescriptorResolverTest.ProducerFixture fixture =
        resolverTest.new ProducerFixture(true);
    fixture.consumerServices.put("HBASE", fixture.hbase);
    fixture.consumerServices.put("ZOOKEEPER", fixture.zookeeper);
    ManagedDependencyDescriptorResolver realResolver = spy(fixture.resolver);
    Consumer live = fixture.consumer("SERVICE", ConsumerLifecycle.INIT_UNINSTALLED);
    doReturn(live).when(realResolver).resolveService(any(ManagedDependencyServiceKey.class));
    doReturn(live).when(realResolver).resolveService(any(ManagedDependencyServiceKey.class),
        any(ManagedDependencyType.class));
    ServiceDependencyDAO realDao = mock(ServiceDependencyDAO.class);
    when(realDao.findAllBindings()).thenReturn(List.of());
    when(realDao.findByConsumer(11L, "HBASE")).thenReturn(List.of());
    executeReadOperations(fixture.consumerCluster);
    executeReadOperations(fixture.providerCluster);
    executeReadOperations(fixture.zooKeeperProvider);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));
    ManagedServiceDependencyCoordinator checked =
        new ManagedServiceDependencyCoordinator(realResolver, realDao, persistKeyValue);

    UUID hdfsBinding = UUID.fromString("31111111-1111-4111-8111-111111111111");
    UUID zooKeeperBinding = UUID.fromString("32222222-2222-4222-8222-222222222222");
    Map<String, Object> preview = checked.preview(ConsumerReference.service(11L), List.of(
        new ManagedServiceDependencyCoordinator.PreviewSelection(
            ManagedDependencyType.HDFS, new ProviderReference(22L, "HDFS"), hdfsBinding),
        new ManagedServiceDependencyCoordinator.PreviewSelection(
            ManagedDependencyType.ZOOKEEPER,
            new ProviderReference(33L, "ZOOKEEPER"), zooKeeperBinding)));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = (List<Map<String, Object>>) preview.get("items");
    CreateRequest hdfs = createRequest(items.get(0), ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"),
        UUID.fromString("33111111-1111-4111-8111-111111111111"));
    CreateRequest zooKeeper = createRequest(items.get(1), ManagedDependencyType.ZOOKEEPER,
        new ProviderReference(33L, "ZOOKEEPER"),
        UUID.fromString("33222222-2222-4222-8222-222222222222"));
    CreateRequest changedZooKeeper = new CreateRequest(zooKeeper.bindingId(), zooKeeper.type(),
        zooKeeper.provider(),
        zooKeeper.expectedProviderFingerprint(), zooKeeper.expectedConsumerFingerprint(),
        "sha256:" + "f".repeat(64), zooKeeper.previewSchemaVersion(), zooKeeper.operationId(),
        null);

    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> checked.create("consumer-a", "HBASE", List.of(hdfs, changedZooKeeper)));

    assertEquals("DEPENDENCY_PREVIEW_STALE", error.getCode());
    verify(realDao, never()).createBatch(any());
  }

  @Test
  void unauthorizedSecondProviderIsRejectedBeforeBatchPersistence() {
    Cluster consumerCluster = cluster(11L, 22L, "consumer", Map.of("HBASE", mock(Service.class)));
    Cluster hdfsCluster = cluster(22L, 22L, "hdfs", Map.of("HDFS", mock(Service.class)));
    Cluster zooKeeperCluster = cluster(33L, 33L, "zookeeper",
        Map.of("ZOOKEEPER", mock(Service.class)));
    when(resolver.cluster("consumer")).thenReturn(consumerCluster);
    when(resolver.cluster(11L)).thenReturn(consumerCluster);
    when(resolver.cluster(22L)).thenReturn(hdfsCluster);
    when(resolver.cluster(33L)).thenReturn(zooKeeperCluster);
    when(resolver.resolveService(new ManagedDependencyServiceKey(11L, "HBASE")))
        .thenReturn(consumer(11L));
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator("alice", 22L));
    CreateRequest hdfs = new CreateRequest(
        UUID.fromString("41111111-1111-4111-8111-111111111111"), ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"), hash('a'), hash('b'), hash('c'),
        PREVIEW_SCHEMA_VERSION,
        UUID.fromString("42111111-1111-4111-8111-111111111111"), null);
    CreateRequest zooKeeper = new CreateRequest(
        UUID.fromString("42222222-2222-4222-8222-222222222222"), ManagedDependencyType.ZOOKEEPER,
        new ProviderReference(33L, "ZOOKEEPER"), hash('d'), hash('e'), hash('f'),
        PREVIEW_SCHEMA_VERSION,
        UUID.fromString("43222222-2222-4222-8222-222222222222"), null);

    assertThrows(AuthorizationException.class, () -> coordinator.preview(
        ConsumerReference.service(11L), List.of(
            new ManagedServiceDependencyCoordinator.PreviewSelection(
                ManagedDependencyType.HDFS, new ProviderReference(22L, "HDFS"), hdfs.bindingId()),
            new ManagedServiceDependencyCoordinator.PreviewSelection(
                ManagedDependencyType.ZOOKEEPER, new ProviderReference(33L, "ZOOKEEPER"),
                zooKeeper.bindingId()))));
    verify(dependencyDAO, never()).findBinding(any(String.class));
    verify(dependencyDAO, never()).findFence(any(String.class));

    assertThrows(AuthorizationException.class,
        () -> coordinator.create("consumer", "HBASE", List.of(hdfs, zooKeeper)));
    verify(dependencyDAO, never()).createBatch(any());
  }

  @Test
  void completeCreateRequiresOneConsistentDraftReferenceInEitherInputOrder() {
    DraftReference draft = new DraftReference(
        UUID.fromString("44111111-1111-4111-8111-111111111111"), 7L);
    CreateRequest withoutDraft = new CreateRequest(
        UUID.fromString("45111111-1111-4111-8111-111111111111"), ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"), hash('a'), hash('b'), hash('c'),
        PREVIEW_SCHEMA_VERSION,
        UUID.fromString("46111111-1111-4111-8111-111111111111"), null);
    CreateRequest withDraft = new CreateRequest(
        UUID.fromString("45222222-2222-4222-8222-222222222222"),
        ManagedDependencyType.ZOOKEEPER,
        new ProviderReference(33L, "ZOOKEEPER"), hash('d'), hash('e'), hash('f'),
        PREVIEW_SCHEMA_VERSION,
        UUID.fromString("46222222-2222-4222-8222-222222222222"), draft);

    for (List<CreateRequest> requests : List.of(
        List.of(withoutDraft, withDraft), List.of(withDraft, withoutDraft))) {
      ManagedDependencyIntegrationException error = assertThrows(
          ManagedDependencyIntegrationException.class,
          () -> coordinator.create("consumer", "HBASE", requests));
      assertEquals("INVALID_MANAGED_DEPENDENCY_PLAN", error.getCode());
    }
  }

  @Test
  void completeReplayReturnsTheCurrentStateAfterARealFixtureUpdate() throws Exception {
    ManagedDependencyDescriptorResolverTest resolverTest =
        new ManagedDependencyDescriptorResolverTest();
    ManagedDependencyDescriptorResolverTest.ProducerFixture fixture =
        resolverTest.new ProducerFixture(true);
    fixture.consumerServices.put("HBASE", fixture.hbase);
    fixture.consumerServices.put("ZOOKEEPER", fixture.zookeeper);
    when(fixture.hbase.getServiceComponents()).thenReturn(Map.of());
    executeReadOperations(fixture.consumerCluster);
    executeReadOperations(fixture.providerCluster);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));
    ServiceDependencyDAO realDao = mock(ServiceDependencyDAO.class);
    ManagedServiceDependencyCoordinator checked =
        new ManagedServiceDependencyCoordinator(fixture.resolver, realDao, persistKeyValue);
    UUID bindingId = UUID.fromString("51111111-1111-4111-8111-111111111111");
    UUID createOperationId = UUID.fromString("52111111-1111-4111-8111-111111111111");
    UUID updateOperationId = UUID.fromString("52222222-2222-4222-8222-222222222222");
    CreateRequest request = new CreateRequest(bindingId, ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"), hash('a'), hash('b'), hash('c'),
        PREVIEW_SCHEMA_VERSION, createOperationId, null);
    ServiceDependencyBindingEntity binding = binding(bindingId, updateOperationId, hash('b'));
    binding.setState("READY");
    binding.setSnapshotApproval("APPROVED");
    binding.setDesiredSnapshotVersion(2L);
    binding.setOperationEpoch(2L);
    binding.setActiveOperationId(updateOperationId.toString());
    ServiceDependencySnapshotEntity original = snapshot(bindingId, hash('a'), hash('b'));
    original.setConsumerFingerprint(hash('a'));
    original.setProviderFingerprint(hash('b'));
    original.setSnapshotFingerprint(hash('c'));
    ServiceDependencySnapshotEntity current = snapshot(bindingId, 2L, hash('d'), hash('e'));
    ServiceDependencyOperationEntity createOperation = operation(
        bindingId, createOperationId, "CREATE", 1L, 1L);
    Method requestHash = ManagedServiceDependencyCoordinator.class.getDeclaredMethod(
        "immutableRequestHash", long.class, CreateRequest.class);
    requestHash.setAccessible(true);
    createOperation.setRequestHash((String) requestHash.invoke(checked, 11L, request));
    ServiceDependencyOperationEntity updateOperation = operation(
        bindingId, updateOperationId, "UPDATE", 2L, 2L);
    when(realDao.findOperation(createOperationId.toString())).thenReturn(createOperation);
    when(realDao.findBinding(bindingId.toString())).thenReturn(binding);
    when(realDao.findSnapshot(bindingId.toString(), 1L)).thenReturn(original);
    when(realDao.findSnapshot(bindingId.toString(), 2L)).thenReturn(current);
    when(realDao.findOperation(updateOperationId.toString())).thenReturn(updateOperation);
    when(realDao.findOperations(bindingId.toString()))
        .thenReturn(List.of(createOperation, updateOperation));
    when(realDao.findHostResults(bindingId.toString(), 1L, 1L)).thenReturn(List.of());
    when(realDao.findHostResults(bindingId.toString(), 2L, 2L)).thenReturn(List.of());

    Map<String, Object> response = checked.create("consumer-a", "HBASE", List.of(request)).get(0);

    assertEquals("READY", response.get("state"));
    assertEquals(2L, response.get("desired_snapshot_version"));
    assertEquals(updateOperationId.toString(),
        ((Map<?, ?>) response.get("operation")).get("operation_id"));
    assertEquals(createOperationId.toString(),
        ((Map<?, ?>) response.get("creation_attempt")).get("operation_id"));
    verify(realDao, never()).createBatch(any());
  }

  @Test
  void secureCandidateWithoutALocalPeerIsAnExplicitIncompletePlan() throws Exception {
    ManagedDependencyDescriptorResolverTest resolverTest =
        new ManagedDependencyDescriptorResolverTest();
    ManagedDependencyDescriptorResolverTest.ProducerFixture fixture =
        resolverTest.new ProducerFixture(true);
    fixture.consumerServices.clear();
    fixture.consumerServices.put("HBASE", fixture.hbase);
    ManagedDependencyDescriptorResolver realResolver = spy(fixture.resolver);
    Consumer live = fixture.consumer("SERVICE", ConsumerLifecycle.INIT_UNINSTALLED);
    doReturn(live).when(realResolver).resolveService(any(ManagedDependencyServiceKey.class));
    doReturn(live).when(realResolver).resolveService(any(ManagedDependencyServiceKey.class),
        any(ManagedDependencyType.class));
    doReturn(Map.of("hdfs-provider", fixture.providerCluster)).when(realResolver).allClusters();
    ServiceDependencyDAO realDao = mock(ServiceDependencyDAO.class);
    when(realDao.findAllBindings()).thenReturn(List.of());
    executeReadOperations(fixture.consumerCluster);
    executeReadOperations(fixture.providerCluster);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));
    ManagedServiceDependencyCoordinator checked =
        new ManagedServiceDependencyCoordinator(realResolver, realDao, persistKeyValue);

    Map<String, Object> candidate = checked.candidates(
        ConsumerReference.service(11L), ManagedDependencyType.HDFS).get(0);

    assertEquals(false, candidate.get("compatible"));
    assertEquals("INCOMPLETE", candidate.get("planning_status"));
    assertEquals(true, candidate.get("requires_complete_selection"));
    @SuppressWarnings("unchecked")
    Map<String, Object> issue = ((List<Map<String, Object>>) candidate.get("errors")).get(0);
    assertEquals("DEPENDENCY_SECURITY_PLAN_INCOMPLETE", issue.get("code"));
  }

  @Test
  void advisorSelectionUsesSchemaTwoFactsAndKeepsDraftClusterIdentityNullable() {
    UUID draftId = UUID.fromString("00000000-0000-4000-8000-000000000023");
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000024");
    Cluster providerCluster = cluster(22L, 202L, "provider", Map.of("HDFS", mock(Service.class)));
    Consumer draft = new Consumer("DRAFT", draftId, null, null, "HBASE",
        ConsumerLifecycle.DRAFT, consumer(11L).version(),
        ManagedDependencySecurityMode.INSECURE, "", consumer(11L).identity(),
        ManagedDependencyIdentity.Plan.forCreationDraft(7, draftId));
    when(resolver.cluster(22L)).thenReturn(providerCluster);
    when(resolver.resolveDraft(draftId, 7L)).thenReturn(draft);
    when(resolver.resolveProvider(new ManagedDependencyServiceKey(22L, "HDFS")))
        .thenReturn(provider());
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));

    ManagedServiceDependencyCoordinator.AdvisorSelection selection =
        coordinator.authorizeAdvisorSelection(ConsumerReference.draft(draftId, 7L),
            ManagedDependencyType.HDFS, new ProviderReference(22L, "HDFS"), bindingId);

    assertEquals(bindingId, selection.bindingId());
    assertEquals(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION,
        selection.previewSchemaVersion());
    assertEquals(null, selection.consumerClusterId());
    assertEquals("BIGTOP", selection.consumerStackName());
    assertEquals("3.3.0", selection.consumerStackVersion());
  }

  @Test
  void installedAdvisorSelectionRequiresTheExactActiveApprovedBinding() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000025");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000026");
    Cluster consumerCluster = cluster(11L, 101L, "consumer", Map.of("HBASE", mock(Service.class)));
    Cluster providerCluster = cluster(22L, 202L, "provider", Map.of("HDFS", mock(Service.class)));
    when(resolver.cluster(11L)).thenReturn(consumerCluster);
    when(resolver.cluster(22L)).thenReturn(providerCluster);
    when(resolver.resolveService(new ManagedDependencyServiceKey(11L, "HBASE")))
        .thenReturn(consumer(11L));
    when(resolver.resolveProvider(new ManagedDependencyServiceKey(22L, "HDFS")))
        .thenReturn(provider());
    ServiceDependencyBindingEntity binding = binding(bindingId, operationId, hash('b'));
    binding.setState("READY");
    binding.setSnapshotApproval("APPROVED");
    when(dependencyDAO.findBinding(bindingId.toString())).thenReturn(binding);
    ManagedDependencySnapshot approved = StageUtils.getGson().fromJson(
        snapshot(bindingId, hash('a'), hash('b')).getSnapshotJson(), ManagedDependencySnapshot.class);
    ManagedServiceDependencyCoordinator checked = spy(coordinator);
    doReturn(approved).when(checked).validateDispatchState(binding);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));

    ManagedServiceDependencyCoordinator.AdvisorSelection selection =
        checked.authorizeAdvisorSelection(ConsumerReference.service(11L),
            ManagedDependencyType.HDFS, new ProviderReference(22L, "HDFS"), bindingId);

    assertEquals(hash('c'), selection.snapshotFingerprint());
    assertEquals(11L, selection.consumerClusterId());
    verify(checked).validateDispatchState(binding);
  }

  @Test
  void exactCommittedCreateRetryIgnoresLaterDraftAndProviderDrift() throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000011");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000012");
    UUID draftId = UUID.fromString("00000000-0000-4000-8000-000000000013");
    String consumerFingerprint = hash('a');
    String providerFingerprint = hash('b');
    CreateRequest request = new CreateRequest(bindingId, ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"), providerFingerprint, consumerFingerprint,
        hash('c'),
        ManagedServiceDependencyCoordinator.PREVIEW_SCHEMA_VERSION, operationId,
        new DraftReference(draftId, 7L));

    Cluster consumerCluster = cluster(11L, 101L, "consumer",
        Map.of("HBASE", mock(Service.class)));
    Cluster providerCluster = cluster(22L, 202L, "provider",
        Map.of("HDFS", mock(Service.class)));
    when(resolver.cluster("consumer")).thenReturn(consumerCluster);
    when(resolver.cluster(22L)).thenReturn(providerCluster);
    when(persistKeyValue.getCreationDraftCluster(draftId.toString()))
        .thenReturn(Map.of("cluster_id", 11L, "cluster_name", "consumer"));

    ServiceDependencyBindingEntity binding = binding(bindingId, operationId, providerFingerprint);
    ServiceDependencySnapshotEntity snapshot = snapshot(
        bindingId, consumerFingerprint, providerFingerprint);
    ServiceDependencyOperationEntity operation = new ServiceDependencyOperationEntity();
    operation.setOperationId(operationId.toString());
    operation.setBindingId(bindingId.toString());
    operation.setOperationKind("CREATE");
    operation.setOperationEpoch(1L);
    operation.setTargetSnapshotVersion(1L);
    Method requestHash = ManagedServiceDependencyCoordinator.class.getDeclaredMethod(
        "immutableRequestHash", long.class, CreateRequest.class);
    requestHash.setAccessible(true);
    operation.setRequestHash((String) requestHash.invoke(coordinator, 11L, request));
    operation.setState("QUEUED");
    when(dependencyDAO.findOperation(operationId.toString())).thenReturn(operation);
    when(dependencyDAO.findBinding(bindingId.toString())).thenReturn(binding);
    when(dependencyDAO.findSnapshot(bindingId.toString(), 1L)).thenReturn(snapshot);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));

    Map<String, Object> response = coordinator.create("consumer", "HBASE", request);

    assertEquals(bindingId.toString(), response.get("binding_id"));
    verify(resolver, never()).resolveService(
        new ManagedDependencyServiceKey(11L, "HBASE"));
    verify(resolver, never()).resolveProvider(
        new ManagedDependencyServiceKey(22L, "HDFS"));
  }

  @Test
  void olderOperationWinningSameBindingKeepsNewAttemptStrictAndReconciliable() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000031");
    UUID oldOperationId = UUID.fromString("00000000-0000-4000-8000-000000000032");
    UUID newOperationId = UUID.fromString("00000000-0000-4000-8000-000000000033");
    Service consumerService = mock(Service.class);
    Service providerService = mock(Service.class);
    Cluster consumerCluster = cluster(11L, 101L, "consumer",
        Map.of("HBASE", consumerService));
    Cluster providerCluster = cluster(22L, 202L, "provider",
        Map.of("HDFS", providerService));
    executeReadOperations(consumerCluster);
    executeReadOperations(providerCluster);
    when(consumerService.getServiceComponents()).thenReturn(Map.of());
    when(providerService.getServiceComponents()).thenReturn(Map.of());
    when(resolver.cluster("consumer")).thenReturn(consumerCluster);
    when(resolver.cluster(11L)).thenReturn(consumerCluster);
    when(resolver.cluster(22L)).thenReturn(providerCluster);
    when(resolver.resolveService(new ManagedDependencyServiceKey(11L, "HBASE")))
        .thenReturn(consumer(11L));
    when(resolver.resolveProvider(new ManagedDependencyServiceKey(22L, "HDFS")))
        .thenReturn(provider());
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));

    Map<String, Object> preview = coordinator.preview(ConsumerReference.service(11L),
        ManagedDependencyType.HDFS, new ProviderReference(22L, "HDFS"), bindingId);
    CreateRequest newer = new CreateRequest(bindingId, ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"), (String) preview.get("provider_fingerprint"),
        (String) preview.get("consumer_descriptor_fingerprint"),
        (String) preview.get("snapshot_fingerprint"), PREVIEW_SCHEMA_VERSION,
        newOperationId, null);
    ServiceDependencyBindingEntity winner = binding(
        bindingId, oldOperationId, newer.expectedProviderFingerprint());
    ServiceDependencySnapshotEntity winnerSnapshot = snapshot(bindingId,
        newer.expectedConsumerFingerprint(), newer.expectedProviderFingerprint());
    doThrow(new PersistenceException("binding primary key won by prior operation"))
        .when(dependencyDAO).create(any(), any(), any(), any());
    when(dependencyDAO.findBinding(bindingId.toString())).thenReturn(winner);
    when(dependencyDAO.findSnapshot(bindingId.toString(), 1L)).thenReturn(winnerSnapshot);

    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> coordinator.create("consumer", "HBASE", newer));

    assertEquals("BINDING_ID_CONFLICT", error.getCode());
    verify(dependencyDAO).create(any(), any(), any(), any());
    verify(resolver).validateEffectiveConsumerConfig(any(), any());
  }

  @Test
  void configPublishedBeforeApprovalRejectsCreateWithoutPersistingAnIntent() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000041");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000042");
    Service consumerService = mock(Service.class);
    Service providerService = mock(Service.class);
    Cluster consumerCluster = cluster(11L, 101L, "consumer",
        Map.of("HBASE", consumerService));
    Cluster providerCluster = cluster(22L, 202L, "provider",
        Map.of("HDFS", providerService));
    executeReadOperations(consumerCluster);
    executeReadOperations(providerCluster);
    when(consumerService.getServiceComponents()).thenReturn(Map.of());
    when(providerService.getServiceComponents()).thenReturn(Map.of());
    when(resolver.cluster("consumer")).thenReturn(consumerCluster);
    when(resolver.cluster(11L)).thenReturn(consumerCluster);
    when(resolver.cluster(22L)).thenReturn(providerCluster);
    when(resolver.resolveService(new ManagedDependencyServiceKey(11L, "HBASE")))
        .thenReturn(consumer(11L));
    when(resolver.resolveProvider(new ManagedDependencyServiceKey(22L, "HDFS")))
        .thenReturn(provider());
    doThrow(new ManagedDependencyIntegrationException(409,
        "DEPENDENCY_MANAGED_CONFIG_MISMATCH",
        "Apply the reviewed managed HBase configuration before approval or dispatch."))
        .when(resolver).validateEffectiveConsumerConfig(any(), any());
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));
    Map<String, Object> preview = coordinator.preview(ConsumerReference.service(11L),
        ManagedDependencyType.HDFS, new ProviderReference(22L, "HDFS"), bindingId);
    CreateRequest request = new CreateRequest(bindingId, ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"), (String) preview.get("provider_fingerprint"),
        (String) preview.get("consumer_descriptor_fingerprint"),
        (String) preview.get("snapshot_fingerprint"), PREVIEW_SCHEMA_VERSION,
        operationId, null);

    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> coordinator.create("consumer", "HBASE", request));

    assertEquals("DEPENDENCY_MANAGED_CONFIG_MISMATCH", error.getCode());
    verify(dependencyDAO, never()).create(any(), any(), any(), any());
  }

  @Test
  void partialTopologyReadCannotAdvertiseStartOrHostReadiness() throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000051");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000052");
    Service hbase = mock(Service.class);
    ServiceComponent master = mock(ServiceComponent.class);
    Cluster consumerCluster = cluster(11L, 101L, "consumer", Map.of("HBASE", hbase));
    executeReadOperations(consumerCluster);
    when(hbase.getServiceComponents()).thenReturn(Map.of("HBASE_MASTER", master));
    when(master.getServiceComponentHosts()).thenThrow(
        new IllegalStateException("topology cache reloading"));
    when(resolver.cluster(11L)).thenReturn(consumerCluster);
    when(dependencyDAO.findHostResults(bindingId.toString(), 1L, 1L)).thenReturn(List.of());
    ServiceDependencyBindingEntity binding = binding(bindingId, operationId, hash('b'));
    binding.setState("READY");
    binding.setSnapshotApproval("APPROVED");
    binding.setProviderPreparationHash(hash('p'));
    binding.setAppliedSnapshotVersion(1L);
    binding.setAppliedProviderFingerprint(hash('b'));

    Map<String, Object> readiness = coordinator.readinessSummary(binding);
    Map<String, Object> capabilities = coordinator.capabilitySummary(
        binding, snapshotValue(snapshot(bindingId, hash('a'), hash('b'))), readiness, true);

    assertEquals(false, readiness.get("topology_current"));
    assertEquals(List.of(), readiness.get("required_daemon_host_ids"));
    assertEquals(false, capabilities.get("start_or_restart_allowed"));
    assertEquals("STATUS_UNAVAILABLE", capabilities.get("next_action"));
    assertFalse(((List<?>) capabilities.get("allowed_actions")).contains("START_OR_RESTART"));
  }

  @Test
  void bindingResponseCarriesConsumerIdentityAndCurrentPreparationLineage() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000056");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000057");
    Service hbase = mock(Service.class);
    Cluster consumerCluster = cluster(11L, 101L, "consumer", Map.of("HBASE", hbase));
    executeReadOperations(consumerCluster);
    when(hbase.getServiceComponents()).thenReturn(Map.of());
    when(resolver.cluster("consumer")).thenReturn(consumerCluster);
    when(resolver.cluster(11L)).thenReturn(consumerCluster);
    ServiceDependencyBindingEntity binding = binding(bindingId, operationId, hash('b'));
    ServiceDependencySnapshotEntity snapshot = snapshot(bindingId, hash('a'), hash('b'));
    ServiceDependencyOperationEntity operation = operation(
        bindingId, operationId, "CREATE", 1L, 1L);
    ServiceDependencyHostResultEntity preparation = new ServiceDependencyHostResultEntity();
    preparation.setBindingId(bindingId.toString());
    preparation.setSnapshotVersion(1L);
    preparation.setHostId(501L);
    preparation.setDependencyType("HDFS");
    preparation.setCheckKind("PREPARE_HDFS_CONSUMER");
    preparation.setOperationEpoch(1L);
    preparation.setOperationId(operationId.toString());
    preparation.setComponentName("HBASE_MASTER");
    preparation.setAmbariRequestId(700L);
    preparation.setAmbariTaskId(701L);
    preparation.setState("DISPATCHED");
    when(dependencyDAO.findBinding(bindingId.toString())).thenReturn(binding);
    when(dependencyDAO.findSnapshot(bindingId.toString(), 1L)).thenReturn(snapshot);
    when(dependencyDAO.findOperation(operationId.toString())).thenReturn(operation);
    when(dependencyDAO.findOperations(bindingId.toString())).thenReturn(List.of(operation));
    when(dependencyDAO.findHostResults(bindingId.toString(), 1L, 1L))
        .thenReturn(List.of(preparation));
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));

    Map<String, Object> response = coordinator.get("consumer", "HBASE", bindingId);

    assertEquals(Map.of("cluster_id", 11L, "service_name", "HBASE"), response.get("consumer"));
    @SuppressWarnings("unchecked")
    Map<String, Object> readiness = (Map<String, Object>) response.get("readiness");
    @SuppressWarnings("unchecked")
    Map<String, Object> lineage = ((List<Map<String, Object>>) readiness
        .get("preparation_requests")).get(0);
    assertEquals(bindingId.toString(), lineage.get("binding_id"));
    assertEquals(operationId.toString(), lineage.get("operation_id"));
    assertEquals(700L, lineage.get("request_id"));
    assertEquals(701L, lineage.get("task_id"));
  }

  @Test
  void staleAndZooKeeperReconciliationStatesExposeOnlySupportedActions() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000061");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000062");
    ServiceDependencyBindingEntity binding = binding(bindingId, operationId, hash('b'));
    binding.setSnapshotApproval("APPROVED");
    binding.setProviderPreparationHash(hash('p'));
    Map<String, Object> readiness = Map.of(
        "topology_current", true,
        "all_current_daemons_prepared", false,
        "all_current_daemons_verified", false,
        "active_command", false);
    ManagedDependencySnapshot snapshot = snapshotValue(
        snapshot(bindingId, hash('a'), hash('b')));

    binding.setState("STALE");
    Map<String, Object> stale = coordinator.capabilitySummary(
        binding, snapshot, readiness, true);
    assertEquals("REVIEW_STALE", stale.get("next_action"));
    assertEquals(false, stale.get("install_or_configure_allowed"));

    binding.setState("READY");
    binding.setAppliedSnapshotVersion(1L);
    binding.setAppliedProviderFingerprint(hash('b'));
    Map<String, Object> unapprovedSnapshot = coordinator.capabilitySummary(
        binding, snapshot, Map.of(
            "topology_current", true,
            "all_current_daemons_prepared", true,
            "all_current_daemons_verified", true,
            "active_command", false), false);
    assertEquals("REVIEW_APPROVAL", unapprovedSnapshot.get("next_action"));
    assertEquals(false, unapprovedSnapshot.get("start_or_restart_allowed"));

    binding.setDependencyType("ZOOKEEPER");
    binding.setProviderPreparationHash(null);
    binding.setState("FENCING_UNCERTAIN");
    binding.setProvisioningPhase("ZOOKEEPER_HANDOFF_RECONCILING");
    binding.setFailureCode("DEPENDENCY_ZOOKEEPER_HANDOFF_RECONCILIATION_REQUIRED");
    Map<String, Object> reconciling = coordinator.capabilitySummary(
        binding, snapshot, readiness, true);
    assertEquals(true, reconciling.get("install_or_configure_allowed"));
    assertEquals("INSTALL_OR_CONFIGURE", reconciling.get("next_action"));
    assertTrue(((List<?>) reconciling.get("allowed_actions"))
        .contains("INSTALL_OR_CONFIGURE"));
  }

  @Test
  void secureCredentialActionRequiresThePersistedCredentialPhase() {
    ServiceDependencyBindingEntity binding = binding(
        UUID.fromString("00000000-0000-4000-8000-000000000071"),
        UUID.fromString("00000000-0000-4000-8000-000000000072"), hash('b'));
    binding.setSnapshotApproval("APPROVED");
    binding.setProviderPreparationHash(hash('p'));
    binding.setState("PROVISIONING");
    ManagedDependencySnapshot secure = mock(ManagedDependencySnapshot.class);
    when(secure.securityMode()).thenReturn(ManagedDependencySecurityMode.KERBEROS);
    Map<String, Object> readiness = Map.of(
        "topology_current", true,
        "all_current_daemons_prepared", true,
        "all_current_daemons_verified", false,
        "active_command", false);

    binding.setProvisioningPhase("CONSUMER_CREDENTIALS_REQUIRED");
    Map<String, Object> required = coordinator.capabilitySummary(
        binding, secure, readiness, true);
    assertEquals("REQUIRED", required.get("credential_status"));
    assertEquals(true, required.get("credentials_required"));
    assertEquals("ISSUE_CREDENTIALS", required.get("next_action"));

    binding.setProvisioningPhase("CONSUMER_VERIFYING");
    Map<String, Object> issued = coordinator.capabilitySummary(
        binding, secure, readiness, true);
    assertEquals("ISSUED", issued.get("credential_status"));
    assertEquals(false, issued.get("credentials_required"));
    assertEquals("VERIFY", issued.get("next_action"));
    assertFalse(((List<?>) issued.get("allowed_actions")).contains("ISSUE_CREDENTIALS"));

    binding.setState("STALE");
    binding.setProvisioningPhase("CONSUMER_CREDENTIALS_REQUIRED");
    Map<String, Object> stale = coordinator.capabilitySummary(
        binding, secure, readiness, true);
    assertEquals("REVIEW_STALE", stale.get("next_action"));
    assertEquals(false, stale.get("credentials_required"));
    assertFalse(((List<?>) stale.get("allowed_actions")).contains("ISSUE_CREDENTIALS"));

    binding.setState("UNRECOGNIZED");
    binding.setProvisioningPhase("PROVIDER_PREPARED");
    Map<String, Object> unknown = coordinator.capabilitySummary(
        binding, secure, Map.of(
            "topology_current", true,
            "all_current_daemons_prepared", false,
            "all_current_daemons_verified", false,
            "active_command", false), true);
    assertEquals("STATUS_UNAVAILABLE", unknown.get("next_action"));
    assertEquals(false, unknown.get("install_or_configure_allowed"));
  }

  @Test
  void retryIsNotAdvertisedOrPersistedWithoutAnExecutablePreparationPlan() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000075");
    UUID currentOperationId = UUID.fromString("00000000-0000-4000-8000-000000000076");
    UUID retryOperationId = UUID.fromString("00000000-0000-4000-8000-000000000077");
    ServiceDependencyBindingEntity binding = binding(bindingId, currentOperationId, hash('b'));
    binding.setState("FAILED");
    binding.setSnapshotApproval("APPROVED");
    binding.setProviderPreparationHash(hash('p'));
    binding.setFailureRetryable(true);
    Cluster consumerCluster = cluster(11L, 101L, "consumer", Map.of("HBASE", mock(Service.class)));
    Cluster providerCluster = cluster(22L, 202L, "provider", Map.of("HDFS", mock(Service.class)));
    executeReadOperations(consumerCluster);
    executeReadOperations(providerCluster);
    when(resolver.cluster("consumer")).thenReturn(consumerCluster);
    when(resolver.cluster(22L)).thenReturn(providerCluster);
    when(dependencyDAO.findBinding(bindingId.toString())).thenReturn(binding);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));

    Map<String, Object> capability = coordinator.capabilitySummary(binding,
        snapshotValue(snapshot(bindingId, hash('a'), hash('b'))), Map.of(
            "topology_current", true,
            "all_current_daemons_prepared", false,
            "all_current_daemons_verified", false,
            "active_command", false), true);
    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> coordinator.retry("consumer", "HBASE", bindingId,
            new LifecycleRequest(retryOperationId, 0L)));

    assertEquals(false, capability.get("retry_allowed"));
    assertEquals("RETRY_UNAVAILABLE", capability.get("next_action"));
    assertEquals("DEPENDENCY_RETRY_REQUIRES_PREPARATION_PLAN", error.getCode());
    verify(dependencyDAO, never()).startRetry(any(), anyLong(), any(), anyInt());
  }

  @Test
  void lostCreateReplayKeepsCurrentAndHistoricalSnapshotsSeparate() throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000081");
    UUID createOperationId = UUID.fromString("00000000-0000-4000-8000-000000000082");
    UUID currentOperationId = UUID.fromString("00000000-0000-4000-8000-000000000083");
    ServiceDependencySnapshotEntity created = snapshot(
        bindingId, 1L, hash('a'), hash('b'));
    ServiceDependencySnapshotEntity current = snapshot(
        bindingId, 2L, hash('d'), hash('b'));
    ServiceDependencyBindingEntity binding = binding(bindingId, currentOperationId, hash('b'));
    binding.setDesiredSnapshotVersion(2L);
    binding.setOperationEpoch(2L);
    CreateRequest request = new CreateRequest(bindingId, ManagedDependencyType.HDFS,
        new ProviderReference(22L, "HDFS"), hash('b'), hash('a'), hash('c'),
        PREVIEW_SCHEMA_VERSION, createOperationId, null);
    ServiceDependencyOperationEntity creation = operation(
        bindingId, createOperationId, "CREATE", 1L, 1L);
    Method requestHash = ManagedServiceDependencyCoordinator.class.getDeclaredMethod(
        "immutableRequestHash", long.class, CreateRequest.class);
    requestHash.setAccessible(true);
    creation.setRequestHash((String) requestHash.invoke(coordinator, 11L, request));
    ServiceDependencyOperationEntity currentOperation = operation(
        bindingId, currentOperationId, "UPDATE", 2L, 2L);
    Cluster consumerCluster = cluster(11L, 101L, "consumer", Map.of("HBASE", mock(Service.class)));
    when(resolver.cluster("consumer")).thenReturn(consumerCluster);
    when(dependencyDAO.findOperation(createOperationId.toString())).thenReturn(creation);
    when(dependencyDAO.findOperation(currentOperationId.toString())).thenReturn(currentOperation);
    when(dependencyDAO.findBinding(bindingId.toString())).thenReturn(binding);
    when(dependencyDAO.findSnapshot(bindingId.toString(), 1L)).thenReturn(created);
    when(dependencyDAO.findSnapshot(bindingId.toString(), 2L)).thenReturn(current);
    when(dependencyDAO.findOperations(bindingId.toString()))
        .thenReturn(List.of(creation, currentOperation));
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(7, "alice"));

    Map<String, Object> response = coordinator.create("consumer", "HBASE", request);

    assertEquals(2L, ((Map<?, ?>) response.get("snapshot")).get("version"));
    Map<?, ?> creationSummary = (Map<?, ?>) response.get("creation_attempt");
    assertEquals(createOperationId.toString(), creationSummary.get("operation_id"));
    assertEquals(1L, ((Map<?, ?>) creationSummary.get("target_snapshot")).get("version"));
    Map<?, ?> attempted = (Map<?, ?>) response.get("attempted_operation");
    assertEquals(1L, ((Map<?, ?>) attempted.get("target_snapshot")).get("version"));
    assertEquals(currentOperationId.toString(),
        ((Map<?, ?>) response.get("operation")).get("operation_id"));
  }

  private Cluster cluster(long clusterId, long resourceId, String name,
      Map<String, Service> services) {
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterId()).thenReturn(clusterId);
    when(cluster.getResourceId()).thenReturn(resourceId);
    when(cluster.getClusterName()).thenReturn(name);
    when(cluster.getServices()).thenReturn(services);
    return cluster;
  }

  @SuppressWarnings("unchecked")
  private void executeReadOperations(Cluster cluster) {
    when(cluster.executeUnderReadLock(any())).thenAnswer(invocation ->
        ((Supplier<Object>) invocation.getArgument(0)).get());
  }

  private Consumer consumer(long clusterId) {
    ManagedDependencyIdentity.Plan plan = ManagedDependencyIdentity.Plan.forExistingCluster(clusterId);
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        plan.plannedShortUser(), new TreeSet<>(), false, plan.plannedShortUser(), true, "0700", false);
    ManagedDependencyVersion version = new ManagedDependencyVersion("BIGTOP", "3.3.0", true,
        "2.4.17", new TreeMap<>(Map.of("distribution", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 31L, List.of());
    return new Consumer("SERVICE_PLAN", null, clusterId, "consumer", "HBASE",
        ConsumerLifecycle.DRAFT, version, ManagedDependencySecurityMode.INSECURE, "", identity, plan);
  }

  private Provider provider() {
    ManagedDependencyVersion version = new ManagedDependencyVersion("BIGTOP", "3.3.0", true,
        "3.3.0", new TreeMap<>(Map.of("distribution", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 32L, List.of());
    return new Provider(new ManagedDependencyServiceKey(22L, "HDFS"),
        ManagedDependencyType.HDFS, version, true, true,
        ManagedDependencySecurityMode.INSECURE, "", null,
        new HdfsEndpoint("hdfs://provider.example.test:8020", false, "",
            new TreeMap<>(), "", false, false), null,
        Map.of("fs.defaultFS", "hdfs://provider.example.test:8020",
            "hadoop.security.authentication", "simple"),
        Map.of(), Map.of(), Set.of("fs.defaultFS"), Set.of());
  }

  private ServiceDependencyBindingEntity binding(UUID bindingId, UUID operationId,
      String providerFingerprint) {
    ServiceDependencyBindingEntity binding = new ServiceDependencyBindingEntity();
    binding.setBindingId(bindingId.toString());
    binding.setConsumerClusterId(11L);
    binding.setConsumerServiceName("HBASE");
    binding.setProviderClusterId(22L);
    binding.setProviderServiceName("HDFS");
    binding.setDependencyType("HDFS");
    binding.setState("PROVISIONING");
    binding.setProvisioningPhase("PROVIDER_PREPARING");
    binding.setRowVersion(0L);
    binding.setDesiredSnapshotVersion(1L);
    binding.setProviderFingerprint(providerFingerprint);
    binding.setActiveOperationId(operationId.toString());
    return binding;
  }

  private ServiceDependencySnapshotEntity snapshot(UUID bindingId,
      String consumerFingerprint, String providerFingerprint) {
    return snapshot(bindingId, 1L, consumerFingerprint, providerFingerprint);
  }

  private ServiceDependencySnapshotEntity snapshot(UUID bindingId, long snapshotVersion,
      String consumerFingerprint, String providerFingerprint) {
    ManagedDependencyIdentity.Plan plan = ManagedDependencyIdentity.Plan.forExistingCluster(11L);
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        plan.plannedShortUser(), new TreeSet<>(), false, plan.plannedShortUser(), true, "0700", false);
    ManagedDependencyVersion version = new ManagedDependencyVersion("BIGTOP", "3.3.0", true,
        "3.3.0", new TreeMap<>(Map.of("distribution", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 32L, List.of());
    ManagedDependencySnapshot value = new ManagedDependencySnapshot(
        ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION, bindingId, snapshotVersion,
        ManagedDependencyType.HDFS, new ManagedDependencyServiceKey(22L, "HDFS"),
        ManagedDependencyNamespace.hdfs(bindingId, "hdfs://provider.example.test:8020"),
        version.compatibility(), ManagedDependencySecurityMode.INSECURE, identity,
        new TreeMap<>(Map.of("fs.defaultFS", "hdfs://provider.example.test:8020")),
        new TreeMap<>(), new TreeMap<>(), consumerFingerprint, providerFingerprint, hash('c'));
    ServiceDependencySnapshotEntity snapshot = new ServiceDependencySnapshotEntity();
    snapshot.setBindingId(bindingId.toString());
    snapshot.setSnapshotVersion(snapshotVersion);
    snapshot.setSchemaVersion(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION);
    snapshot.setConsumerFingerprint(consumerFingerprint);
    snapshot.setProviderFingerprint(providerFingerprint);
    snapshot.setProviderDisplayName("provider");
    snapshot.setConsumerServiceVersion("3.3.0");
    snapshot.setSnapshotFingerprint(hash('c'));
    snapshot.setSnapshotJson(StageUtils.getGson().toJson(value));
    return snapshot;
  }

  private ManagedDependencySnapshot snapshotValue(ServiceDependencySnapshotEntity snapshot) {
    return StageUtils.getGson().fromJson(
        snapshot.getSnapshotJson(), ManagedDependencySnapshot.class);
  }

  private CreateRequest createRequest(Map<String, Object> preview,
      ManagedDependencyType type, ProviderReference provider, UUID operationId) {
    return new CreateRequest(UUID.fromString((String) preview.get("binding_id")), type, provider,
        (String) preview.get("provider_fingerprint"),
        (String) preview.get("consumer_descriptor_fingerprint"),
        (String) preview.get("snapshot_fingerprint"),
        ((Number) preview.get("preview_schema_version")).intValue(), operationId, null);
  }

  private ServiceDependencyOperationEntity operation(UUID bindingId, UUID operationId,
      String kind, long epoch, long snapshotVersion) {
    ServiceDependencyOperationEntity operation = new ServiceDependencyOperationEntity();
    operation.setOperationId(operationId.toString());
    operation.setBindingId(bindingId.toString());
    operation.setOperationKind(kind);
    operation.setOperationEpoch(epoch);
    operation.setTargetSnapshotVersion(snapshotVersion);
    operation.setRequestHash(hash('f'));
    operation.setState("QUEUED");
    return operation;
  }

  private String hash(char digit) {
    return "sha256:" + String.valueOf(digit).repeat(64);
  }
}
