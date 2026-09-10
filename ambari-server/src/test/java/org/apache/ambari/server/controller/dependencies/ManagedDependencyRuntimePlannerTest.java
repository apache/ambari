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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand.CommandName;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.jupiter.api.Test;

class ManagedDependencyRuntimePlannerTest {
  private static final UUID BINDING_ID =
      UUID.fromString("dd74167b-b0f5-477e-a795-2850bc13a12b");
  private static final UUID OPERATION_ID =
      UUID.fromString("8b7695e7-c78c-4bc8-bb90-cdd46e8f44c5");
  private static final String HASH = "sha256:" + "a".repeat(64);

  @Test
  void providerClientMapsReplaceOnlyManagedHbaseExecutionConfiguration() throws Exception {
    ManagedDependencyRuntimePlanner planner = new ManagedDependencyRuntimePlanner(
        mock(ServiceDependencyDAO.class), mock(ManagedDependencyDescriptorResolver.class),
        mock(ManagedServiceDependencyCoordinator.class));
    ManagedDependencyCommand preparation = preparation(41L);
    String bundle = StageUtils.getGson().toJson(ManagedDependencyCommandBundle.of(
        41L, "hbase_mc_cb", HASH, List.of(preparation)));
    Map<String, Map<String, String>> localDesired = new TreeMap<>();
    localDesired.put("core-site", new TreeMap<>(Map.of(
        "fs.defaultFS", "hdfs://local", "local.only", "retained")));
    localDesired.put("hdfs-site", new TreeMap<>(Map.of("dfs.nameservices", "local-ha")));
    Map<String, Map<String, String>> execution = deepCopy(localDesired);
    execution.put("hbase-site", new TreeMap<>(Map.of("unrelated", "retained")));
    Set<String> replacements = new HashSet<>();

    planner.decoratePersistedCommandConfigurations(41L, bundle, execution, replacements);

    assertEquals("hdfs://provider", execution.get("core-site").get("fs.defaultFS"));
    assertEquals(Map.of("fs.defaultFS", "hdfs://provider"), execution.get("core-site"));
    assertEquals(Map.of("dfs.client.socket-timeout", "60000"),
        execution.get("hdfs-site"));
    assertEquals("hdfs://local", localDesired.get("core-site").get("fs.defaultFS"));
    assertEquals("retained", localDesired.get("core-site").get("local.only"));
    assertEquals(Map.of("dfs.nameservices", "local-ha"), localDesired.get("hdfs-site"));
    assertEquals(Set.of("core-site", "hdfs-site"), replacements);
    assertEquals("hdfs://provider/apps/ambari-managed/hbase/" + BINDING_ID + "/root",
        execution.get("hbase-site").get("hbase.rootdir"));
    assertEquals("retained", execution.get("hbase-site").get("unrelated"));
  }

  @Test
  void zookeeperProfileDoesNotReplaceConsumerLocalHdfsConfiguration() throws Exception {
    ManagedDependencyRuntimePlanner planner = new ManagedDependencyRuntimePlanner(
        mock(ServiceDependencyDAO.class), mock(ManagedDependencyDescriptorResolver.class),
        mock(ManagedServiceDependencyCoordinator.class));
    ManagedDependencyCommand preparation = ManagedDependencyCommand.prepareConsumer(
        zooKeeperSnapshot(), OPERATION_ID, 3L, 41L,
        "hbase_3_3_0_0_1", "3.3.0", HASH);
    String bundle = StageUtils.getGson().toJson(ManagedDependencyCommandBundle.of(
        41L, "hbase_mc_cb", HASH, List.of(preparation)));
    Map<String, Map<String, String>> configurations = deepCopy(Map.of(
        "core-site", Map.of("fs.defaultFS", "hdfs://local", "local.only", "retained"),
        "hdfs-site", Map.of("dfs.nameservices", "local-ha")));
    Set<String> replacements = new HashSet<>();

    planner.decoratePersistedCommandConfigurations(
        41L, bundle, configurations, replacements);

    assertEquals(Map.of("fs.defaultFS", "hdfs://local", "local.only", "retained"),
        configurations.get("core-site"));
    assertEquals(Map.of("dfs.nameservices", "local-ha"),
        configurations.get("hdfs-site"));
    assertEquals(Set.of(), replacements);
    assertEquals("zk-a,zk-b", configurations.get("hbase-site")
        .get("hbase.zookeeper.quorum"));
  }

  @Test
  void startAcceptsPerHostPackageVersionsOnlyWithExactPreparationLineage() throws Exception {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    ManagedDependencyDescriptorResolver resolver = mock(ManagedDependencyDescriptorResolver.class);
    ManagedServiceDependencyCoordinator coordinator = mock(ManagedServiceDependencyCoordinator.class);
    ManagedDependencyRuntimePlanner planner = new ManagedDependencyRuntimePlanner(
        dao, resolver, coordinator);
    ManagedDependencySnapshot snapshot = snapshot();
    ServiceDependencyBindingEntity binding = binding();
    ServiceDependencySnapshotEntity snapshotEntity = new ServiceDependencySnapshotEntity();
    snapshotEntity.setBindingId(BINDING_ID.toString());
    snapshotEntity.setSnapshotVersion(1L);
    snapshotEntity.setSnapshotJson(StageUtils.getGson().toJson(snapshot));
    ServiceComponentHost host41 = componentHost(41L, "hbase-a");
    ServiceComponentHost host42 = componentHost(42L, "hbase-b");
    Cluster cluster = cluster(host41, host42);
    Cluster providerCluster = mock(Cluster.class);
    when(providerCluster.getClusterId()).thenReturn(22L);
    executeReadOperations(cluster);
    executeReadOperations(providerCluster);
    when(resolver.cluster(22L)).thenReturn(providerCluster);
    ServiceDependencyHostResultEntity preparation41 = completedPreparation(41L, "1.rpm");
    ServiceDependencyHostResultEntity preparation42 = completedPreparation(42L, "1.deb");
    ServiceDependencyHostResultEntity verification41 = completedVerification(preparation41);
    ServiceDependencyHostResultEntity verification42 = completedVerification(preparation42);
    when(dao.findByConsumer(11L, "HBASE")).thenReturn(List.of(binding));
    when(dao.findSnapshot(BINDING_ID.toString(), 1L)).thenReturn(snapshotEntity);
    when(dao.findHostResults(BINDING_ID.toString(), 1L))
        .thenReturn(List.of(verification41, verification42));
    when(dao.findHostResult(BINDING_ID.toString(), 1L, 3L, 41L, "HDFS",
        "PREPARE_HDFS_CONSUMER")).thenReturn(preparation41);
    when(dao.findHostResult(BINDING_ID.toString(), 1L, 3L, 42L, "HDFS",
        "PREPARE_HDFS_CONSUMER")).thenReturn(preparation42);
    Map<String, String> parameters = new TreeMap<>();
    Map<String, Map<String, String>> configurations = new TreeMap<>();
    Set<String> replacements = new HashSet<>();

    planner.augmentHostCommand(cluster, host41, RoleCommand.START, parameters,
        configurations, replacements, null);

    assertEquals(Set.of("core-site", "hdfs-site"), replacements);
    assertEquals("hdfs://provider", configurations.get("core-site").get("fs.defaultFS"));

    when(dao.findHostResults(BINDING_ID.toString(), 1L))
        .thenReturn(List.of(verification41));
    doThrow(new AmbariException("dependency verification is incomplete"))
        .when(coordinator).validateConsumerStart(any(), anyLong());
    assertThrows(AmbariException.class, () -> planner.augmentHostCommand(
        cluster, host41, RoleCommand.START, new TreeMap<>(), new TreeMap<>(),
        new HashSet<>(), null));
  }

  @Test
  void actionPublicationCallbackRetainsAllParentLocksUntilItReturns() throws Exception {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    ManagedDependencyDescriptorResolver resolver = mock(ManagedDependencyDescriptorResolver.class);
    ManagedDependencyRuntimePlanner planner = new ManagedDependencyRuntimePlanner(
        dao, resolver, mock(ManagedServiceDependencyCoordinator.class));
    Cluster consumer = mock(Cluster.class);
    Cluster provider = mock(Cluster.class);
    when(consumer.getClusterId()).thenReturn(11L);
    when(provider.getClusterId()).thenReturn(22L);
    ReentrantReadWriteLock consumerLock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock providerLock = new ReentrantReadWriteLock();
    executeStateOperations(consumer, consumerLock);
    executeStateOperations(provider, providerLock);
    when(resolver.cluster(11L)).thenReturn(consumer);
    when(resolver.cluster(22L)).thenReturn(provider);

    ManagedDependencyCommand command = preparation(41L);
    String bundle = StageUtils.getGson().toJson(ManagedDependencyCommandBundle.of(
        41L, "hbase_mc_cb", HASH, List.of(command)));
    ExecutionCommand execution = mock(ExecutionCommand.class);
    when(execution.getClusterId()).thenReturn("11");
    when(execution.getServiceName()).thenReturn("HBASE");
    when(execution.getCommandParams()).thenReturn(Map.of(
        ManagedDependencyRuntimePlanner.BUNDLE_PARAMETER, bundle));
    ExecutionCommandWrapper wrapper = mock(ExecutionCommandWrapper.class);
    when(wrapper.getExecutionCommand()).thenReturn(execution);
    HostRoleCommand task = mock(HostRoleCommand.class);
    when(task.getRoleCommand()).thenReturn(RoleCommand.INSTALL);
    when(task.getExecutionCommandWrapper()).thenReturn(wrapper);
    Stage stage = mock(Stage.class);
    when(stage.getOrderedHostRoleCommands()).thenReturn(List.of(task));
    Request request = mock(Request.class);
    when(request.getStages()).thenReturn(List.of(stage));
    when(request.getClusterId()).thenReturn(11L);

    CountDownLatch publicationEntered = new CountDownLatch(1);
    CountDownLatch releasePublication = new CountDownLatch(1);
    CountDownLatch writerStarted = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> publication = executor.submit(() -> {
        planner.executeWithPreparationParentLocks(request, () -> {
          publicationEntered.countDown();
          await(releasePublication);
        });
        return null;
      });
      assertTrue(publicationEntered.await(30, TimeUnit.SECONDS));
      Future<?> writer = executor.submit(() -> {
        writerStarted.countDown();
        provider.executeUnderWriteLock(() -> { });
        return null;
      });
      assertTrue(writerStarted.await(30, TimeUnit.SECONDS));
      assertThrows(TimeoutException.class, () -> writer.get(250, TimeUnit.MILLISECONDS));

      releasePublication.countDown();
      publication.get(30, TimeUnit.SECONDS);
      writer.get(30, TimeUnit.SECONDS);
    } finally {
      releasePublication.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void mismatchedManagedTaskClusterIsRejectedBeforePublicationCallback() throws Exception {
    ManagedDependencyDescriptorResolver resolver = mock(ManagedDependencyDescriptorResolver.class);
    ManagedDependencyRuntimePlanner planner = new ManagedDependencyRuntimePlanner(
        mock(ServiceDependencyDAO.class), resolver,
        mock(ManagedServiceDependencyCoordinator.class));
    ManagedDependencyCommand command = preparation(41L);
    String bundle = StageUtils.getGson().toJson(ManagedDependencyCommandBundle.of(
        41L, "hbase_mc_cb", HASH, List.of(command)));
    ExecutionCommand execution = mock(ExecutionCommand.class);
    when(execution.getClusterId()).thenReturn("12");
    when(execution.getCommandParams()).thenReturn(Map.of(
        ManagedDependencyRuntimePlanner.BUNDLE_PARAMETER, bundle));
    ExecutionCommandWrapper wrapper = mock(ExecutionCommandWrapper.class);
    when(wrapper.getExecutionCommand()).thenReturn(execution);
    HostRoleCommand task = mock(HostRoleCommand.class);
    when(task.getRoleCommand()).thenReturn(RoleCommand.INSTALL);
    when(task.getExecutionCommandWrapper()).thenReturn(wrapper);
    Stage stage = mock(Stage.class);
    when(stage.getOrderedHostRoleCommands()).thenReturn(List.of(task));
    Request request = mock(Request.class);
    when(request.getStages()).thenReturn(List.of(stage));
    when(request.getClusterId()).thenReturn(11L);
    AtomicBoolean published = new AtomicBoolean();

    assertThrows(AmbariException.class,
        () -> planner.executeWithPreparationParentLocks(request, () -> published.set(true)));

    assertEquals(false, published.get());
  }

  @Test
  void rejectedFinalPublicationSkipsDeferredStateAndIntentCallbacks() throws Exception {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    ManagedDependencyDescriptorResolver resolver = mock(ManagedDependencyDescriptorResolver.class);
    ManagedDependencyRuntimePlanner planner = new ManagedDependencyRuntimePlanner(
        dao, resolver, mock(ManagedServiceDependencyCoordinator.class));
    Cluster consumer = mock(Cluster.class);
    when(consumer.getClusterId()).thenReturn(11L);
    executeWriteThroughTransaction(consumer, new ReentrantReadWriteLock());
    when(resolver.cluster(11L)).thenReturn(consumer);
    when(dao.findByProvider(11L, "HDFS")).thenReturn(List.of(binding()));

    ExecutionCommand execution = mock(ExecutionCommand.class);
    when(execution.getClusterId()).thenReturn("11");
    when(execution.getServiceName()).thenReturn("HDFS");
    when(execution.getCommandParams()).thenReturn(Map.of());
    ExecutionCommandWrapper wrapper = mock(ExecutionCommandWrapper.class);
    when(wrapper.getExecutionCommand()).thenReturn(execution);
    HostRoleCommand task = mock(HostRoleCommand.class);
    when(task.getRoleCommand()).thenReturn(RoleCommand.STOP);
    when(task.getExecutionCommandWrapper()).thenReturn(wrapper);
    Stage stage = mock(Stage.class);
    when(stage.getOrderedHostRoleCommands()).thenReturn(List.of(task));
    Request request = mock(Request.class);
    when(request.getStages()).thenReturn(List.of(stage));
    when(request.getClusterId()).thenReturn(11L);
    RequestFactory requestFactory = mock(RequestFactory.class);
    when(requestFactory.createNewFromStages(List.of(stage), "{}")).thenReturn(request);
    RequestStageContainer container = new RequestStageContainer(1L, List.of(stage),
        requestFactory, mock(ActionManager.class));
    AtomicBoolean desiredStateUpdated = new AtomicBoolean();
    AtomicBoolean intentsPlanned = new AtomicBoolean();
    container.addPrePersistAction(ignored -> desiredStateUpdated.set(true));
    container.setPersistenceHandler((persisted, publication) -> {
      ManagedDependencyIntegrationException error = assertThrows(
          ManagedDependencyIntegrationException.class,
          () -> planner.executeWithPreparationParentLocks(persisted, () -> {
            intentsPlanned.set(true);
            publication.run(persisted);
          }));
      assertEquals("DEPENDENCY_IMPACT_CONFIRMATION_REQUIRED", error.getCode());
    });

    container.persist();

    assertFalse(desiredStateUpdated.get());
    assertFalse(intentsPlanned.get());
  }

  @Test
  void successfulFinalPublicationRunsDeferredCallbacksInsidePlannerGuard() throws Exception {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    ManagedDependencyDescriptorResolver resolver = mock(ManagedDependencyDescriptorResolver.class);
    ManagedDependencyRuntimePlanner planner = new ManagedDependencyRuntimePlanner(
        dao, resolver, mock(ManagedServiceDependencyCoordinator.class));
    Cluster consumer = mock(Cluster.class);
    when(consumer.getClusterId()).thenReturn(11L);
    ReentrantReadWriteLock consumerLock = new ReentrantReadWriteLock();
    executeWriteThroughTransaction(consumer, consumerLock);
    when(resolver.cluster(11L)).thenReturn(consumer);
    when(dao.findByProvider(11L, "HDFS")).thenReturn(List.of(binding()));
    ManagedDependencyLifecyclePolicy policy = new ManagedDependencyLifecyclePolicy(dao);
    String revision = policy.impactRevision(11L, "HDFS");

    ExecutionCommand execution = mock(ExecutionCommand.class);
    when(execution.getClusterId()).thenReturn("11");
    when(execution.getServiceName()).thenReturn("HDFS");
    when(execution.getCommandParams()).thenReturn(Map.of(
        ManagedDependencyLifecyclePolicy.IMPACT_ACTION_PARAMETER, "STOP",
        ManagedDependencyLifecyclePolicy.IMPACT_REVISION_PARAMETER, revision));
    ExecutionCommandWrapper wrapper = mock(ExecutionCommandWrapper.class);
    when(wrapper.getExecutionCommand()).thenReturn(execution);
    HostRoleCommand task = mock(HostRoleCommand.class);
    when(task.getRoleCommand()).thenReturn(RoleCommand.STOP);
    when(task.getExecutionCommandWrapper()).thenReturn(wrapper);
    Stage stage = mock(Stage.class);
    when(stage.getOrderedHostRoleCommands()).thenReturn(List.of(task));
    Request request = mock(Request.class);
    when(request.getStages()).thenReturn(List.of(stage));
    when(request.getClusterId()).thenReturn(11L);
    RequestFactory requestFactory = mock(RequestFactory.class);
    when(requestFactory.createNewFromStages(List.of(stage), "{}")).thenReturn(request);
    RequestStageContainer container = new RequestStageContainer(1L, List.of(stage),
        requestFactory, mock(ActionManager.class));
    AtomicBoolean desiredStateUpdated = new AtomicBoolean();
    AtomicBoolean intentsPlanned = new AtomicBoolean();
    AtomicBoolean publicationObserved = new AtomicBoolean();
    container.addPrePersistAction(ignored -> {
      assertTrue(consumerLock.isWriteLockedByCurrentThread());
      desiredStateUpdated.set(true);
    });
    container.setPersistenceHandler((persisted, publication) ->
        planner.executeWithPreparationParentLocks(persisted, () -> {
          intentsPlanned.set(true);
          publication.run(persisted);
          publicationObserved.set(true);
        }));

    container.persist();

    assertTrue(desiredStateUpdated.get());
    assertTrue(intentsPlanned.get());
    assertTrue(publicationObserved.get());
  }

  @Test
  void successfulPublicationPlansAndAssociatesPreparationInsideGuard() throws Exception {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    ManagedDependencyDescriptorResolver resolver = mock(ManagedDependencyDescriptorResolver.class);
    ManagedServiceDependencyCoordinator coordinator = mock(ManagedServiceDependencyCoordinator.class);
    ManagedDependencyRuntimePlanner planner = new ManagedDependencyRuntimePlanner(
        dao, resolver, coordinator);
    ServiceComponentHost host = componentHost(41L, "hbase-a");
    Cluster consumer = cluster(host);
    Cluster provider = mock(Cluster.class);
    when(provider.getClusterId()).thenReturn(22L);
    ReentrantReadWriteLock consumerLock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock providerLock = new ReentrantReadWriteLock();
    executeStateOperations(consumer, consumerLock);
    executeWriteThroughTransaction(consumer, consumerLock);
    executeStateOperations(provider, providerLock);
    executeWriteThroughTransaction(provider, providerLock);
    when(resolver.cluster(11L)).thenReturn(consumer);
    when(resolver.cluster(22L)).thenReturn(provider);
    when(dao.findByConsumer(11L, "HBASE")).thenReturn(List.of());
    when(dao.findBinding(BINDING_ID.toString())).thenReturn(binding());
    AtomicBoolean intentsPlanned = new AtomicBoolean();
    AtomicBoolean preparationAssociated = new AtomicBoolean();
    doAnswer(invocation -> {
      intentsPlanned.set(true);
      return List.of();
    }).when(dao).planCommands(anyList());
    when(dao.associatePreparationTask(any(), anyString(), anyLong(), anyLong(), anyLong()))
        .thenAnswer(invocation -> {
          preparationAssociated.set(true);
          return true;
        });

    ManagedDependencyCommand command = preparation(41L);
    String bundle = StageUtils.getGson().toJson(ManagedDependencyCommandBundle.of(
        41L, "hbase_mc_cb", HASH, List.of(command)));
    ExecutionCommand execution = mock(ExecutionCommand.class);
    when(execution.getClusterId()).thenReturn("11");
    when(execution.getServiceName()).thenReturn("HBASE");
    when(execution.getCommandParams()).thenReturn(Map.of(
        ManagedDependencyRuntimePlanner.BUNDLE_PARAMETER, bundle));
    ExecutionCommandWrapper wrapper = mock(ExecutionCommandWrapper.class);
    when(wrapper.getExecutionCommand()).thenReturn(execution);
    HostRoleCommand task = mock(HostRoleCommand.class);
    when(task.getRoleCommand()).thenReturn(RoleCommand.INSTALL);
    when(task.getExecutionCommandWrapper()).thenReturn(wrapper);
    when(task.getRole()).thenReturn(org.apache.ambari.server.Role.HBASE_REGIONSERVER);
    when(task.getHostId()).thenReturn(41L);
    when(task.getHostName()).thenReturn("hbase-a");
    when(task.getRequestId()).thenReturn(7L);
    when(task.getStageId()).thenReturn(8L);
    when(task.getTaskId()).thenReturn(9L);
    Stage stage = mock(Stage.class);
    when(stage.getOrderedHostRoleCommands()).thenReturn(List.of(task));
    Request request = mock(Request.class);
    when(request.getStages()).thenReturn(List.of(stage));
    when(request.getClusterId()).thenReturn(11L);
    RequestFactory requestFactory = mock(RequestFactory.class);
    when(requestFactory.createNewFromStages(List.of(stage), "{}")).thenReturn(request);
    RequestStageContainer container = new RequestStageContainer(1L, List.of(stage),
        requestFactory, mock(ActionManager.class));
    AtomicBoolean desiredStateUpdated = new AtomicBoolean();
    AtomicBoolean publicationObserved = new AtomicBoolean();
    container.addPrePersistAction(ignored -> {
      assertTrue(consumerLock.isWriteLockedByCurrentThread());
      assertTrue(providerLock.isWriteLockedByCurrentThread());
      desiredStateUpdated.set(true);
    });
    container.setPersistenceHandler((persisted, publication) ->
        planner.executeWithPreparationParentLocks(persisted, () -> {
          planner.planPreparationCommands(task);
          planner.associatePreparationTask(task);
          publication.run(persisted);
          publicationObserved.set(true);
        }));

    container.persist();

    assertTrue(desiredStateUpdated.get());
    assertTrue(publicationObserved.get());
    assertTrue(intentsPlanned.get());
    assertTrue(preparationAssociated.get());
  }

  private ManagedDependencyCommand preparation(long hostId) {
    return ManagedDependencyCommand.prepareConsumer(snapshot(), OPERATION_ID, 3L, hostId,
        "hadoop_3_3_0_0_1-client", "3.3.0", HASH);
  }

  private ServiceDependencyBindingEntity binding() {
    ServiceDependencyBindingEntity binding = new ServiceDependencyBindingEntity();
    binding.setBindingId(BINDING_ID.toString());
    binding.setConsumerClusterId(11L);
    binding.setConsumerServiceName("HBASE");
    binding.setProviderClusterId(22L);
    binding.setProviderServiceName("HDFS");
    binding.setDependencyType("HDFS");
    binding.setState("READY");
    binding.setOperationEpoch(3L);
    binding.setDesiredSnapshotVersion(1L);
    binding.setAppliedSnapshotVersion(1L);
    binding.setProviderFingerprint(HASH);
    binding.setAppliedProviderFingerprint(HASH);
    return binding;
  }

  private ServiceDependencyHostResultEntity completedPreparation(long hostId,
      String packageVersion) {
    ManagedDependencyCommand command = preparation(hostId);
    ServiceDependencyHostResultEntity entity =
        ManagedDependencyOperationDispatcher.commandEntity(
            command, ManagedDependencyType.HDFS, hostId, "HBASE_REGIONSERVER");
    entity.setState("SUCCEEDED");
    entity.setPreparationObservationId(UUID.randomUUID().toString());
    entity.setPreparationRequestHash(command.envelope().immutableRequestHash());
    entity.setPreparationObservationFingerprint(HASH);
    entity.setPackageName("hadoop_3_3_0_0_1-client");
    entity.setPackageVersion(packageVersion);
    entity.setClientSoftwareVersion("3.3.0");
    return entity;
  }

  private ServiceDependencyHostResultEntity completedVerification(
      ServiceDependencyHostResultEntity preparation) {
    ServiceDependencyHostResultEntity entity = new ServiceDependencyHostResultEntity();
    entity.setBindingId(BINDING_ID.toString());
    entity.setSnapshotVersion(1L);
    entity.setHostId(preparation.getHostId());
    entity.setDependencyType("HDFS");
    entity.setCheckKind(CommandName.VERIFY_HDFS_CONSUMER.name());
    entity.setOperationEpoch(3L);
    entity.setState("SUCCEEDED");
    entity.setPreparationObservationId(preparation.getPreparationObservationId());
    entity.setPreparationRequestHash(preparation.getCommandRequestHash());
    entity.setPreparationObservationFingerprint(
        preparation.getPreparationObservationFingerprint());
    entity.setPackageName(preparation.getPackageName());
    entity.setPackageVersion(preparation.getPackageVersion());
    entity.setClientSoftwareVersion(preparation.getClientSoftwareVersion());
    entity.setObservedPackageHash(HASH);
    entity.setRenderedConfigHash(HASH);
    entity.setIdentityFingerprint(HASH);
    return entity;
  }

  private Cluster cluster(ServiceComponentHost... hosts) throws Exception {
    Map<String, ServiceComponentHost> assignments = new TreeMap<>();
    for (ServiceComponentHost host : hosts) {
      assignments.put(host.getHostName(), host);
    }
    ServiceComponent component = mock(ServiceComponent.class);
    when(component.getName()).thenReturn("HBASE_REGIONSERVER");
    when(component.getServiceComponentHosts()).thenReturn(assignments);
    Service service = mock(Service.class);
    when(service.getServiceComponents()).thenReturn(Map.of("HBASE_REGIONSERVER", component));
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterId()).thenReturn(11L);
    when(cluster.getServices()).thenReturn(Map.of("HBASE", service));
    when(cluster.getService("HBASE")).thenReturn(service);
    return cluster;
  }

  private ServiceComponentHost componentHost(long hostId, String hostName) {
    Host host = mock(Host.class);
    when(host.getHostId()).thenReturn(hostId);
    ServiceComponentHost componentHost = mock(ServiceComponentHost.class);
    when(componentHost.getHost()).thenReturn(host);
    when(componentHost.getHostName()).thenReturn(hostName);
    when(componentHost.getServiceName()).thenReturn("HBASE");
    when(componentHost.getServiceComponentName()).thenReturn("HBASE_REGIONSERVER");
    return componentHost;
  }

  @SuppressWarnings("unchecked")
  private void executeReadOperations(Cluster cluster) {
    when(cluster.executeUnderReadLock(org.mockito.ArgumentMatchers.any())).thenAnswer(
        invocation -> ((Supplier<Object>) invocation.getArgument(0)).get());
  }

  @SuppressWarnings("unchecked")
  private void executeStateOperations(Cluster cluster, ReentrantReadWriteLock lock) {
    when(cluster.executeUnderReadLock(org.mockito.ArgumentMatchers.any())).thenAnswer(
        invocation -> {
          lock.readLock().lock();
          try {
            return ((Supplier<Object>) invocation.getArgument(0)).get();
          } finally {
            lock.readLock().unlock();
          }
        });
    doAnswer(invocation -> {
      lock.writeLock().lock();
      try {
        ((Runnable) invocation.getArgument(0)).run();
        return null;
      } finally {
        lock.writeLock().unlock();
      }
    }).when(cluster).executeUnderWriteLock(org.mockito.ArgumentMatchers.any());
    doAnswer(invocation -> {
      lock.writeLock().lock();
      try {
        return ((Supplier<Object>) invocation.getArgument(0)).get();
      } finally {
        lock.writeLock().unlock();
      }
    }).when(cluster).executeUnderWriteLockUntilTransactionCompletion(
        org.mockito.ArgumentMatchers.any());
  }

  @SuppressWarnings("unchecked")
  private void executeWriteThroughTransaction(Cluster cluster, ReentrantReadWriteLock lock) {
    doAnswer(invocation -> {
      lock.writeLock().lock();
      try {
        return ((Supplier<Object>) invocation.getArgument(0)).get();
      } finally {
        lock.writeLock().unlock();
      }
    }).when(cluster).executeUnderWriteLockUntilTransactionCompletion(
        org.mockito.ArgumentMatchers.any());
  }

  private void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(30, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for the test barrier", e);
    }
  }

  private ManagedDependencySnapshot snapshot() {
    ManagedDependencyVersion version = new ManagedDependencyVersion(
        "BIGTOP", "3.3.0", true, "3.3.0",
        new TreeMap<>(Map.of("distribution", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 31L, List.of());
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hbase_mc_cb", new TreeSet<>(), false, "hbase_mc_cb", true, "0700", false);
    return new ManagedDependencySnapshot(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION,
        BINDING_ID, 1L, ManagedDependencyType.HDFS,
        new ManagedDependencyServiceKey(22L, "HDFS"),
        ManagedDependencyNamespace.hdfs(BINDING_ID, "hdfs://provider"),
        version.compatibility(), ManagedDependencySecurityMode.INSECURE, identity,
        new TreeMap<>(Map.of("fs.defaultFS", "hdfs://provider")),
        new TreeMap<>(Map.of("dfs.client.socket-timeout", "60000")),
        new TreeMap<>(), HASH, HASH, HASH);
  }

  private ManagedDependencySnapshot zooKeeperSnapshot() {
    ManagedDependencyVersion version = new ManagedDependencyVersion(
        "BIGTOP", "3.3.0", true, "3.3.0",
        new TreeMap<>(Map.of("distribution", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 32L, List.of());
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hbase_mc_cb", new TreeSet<>(), false, "hbase_mc_cb", true, "0700", false);
    return new ManagedDependencySnapshot(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION,
        BINDING_ID, 1L, ManagedDependencyType.ZOOKEEPER,
        new ManagedDependencyServiceKey(22L, "ZOOKEEPER"),
        ManagedDependencyNamespace.zooKeeper(BINDING_ID),
        version.compatibility(), ManagedDependencySecurityMode.INSECURE, identity,
        new TreeMap<>(), new TreeMap<>(), new TreeMap<>(Map.of(
            "hbase.zookeeper.quorum", "zk-a,zk-b",
            "hbase.zookeeper.property.clientPort", "2181")), HASH, HASH, HASH);
  }

  private Map<String, Map<String, String>> deepCopy(
      Map<String, Map<String, String>> source) {
    Map<String, Map<String, String>> copy = new TreeMap<>();
    source.forEach((type, values) -> copy.put(type, new TreeMap<>(values)));
    return copy;
  }
}
