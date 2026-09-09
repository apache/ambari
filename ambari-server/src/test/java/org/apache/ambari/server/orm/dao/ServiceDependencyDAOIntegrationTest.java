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
package org.apache.ambari.server.orm.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommandBundle;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Consumer;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ConsumerLifecycle;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.HdfsEndpoint;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Provider;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptorResolver;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyNamespace;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyRuntimePlanner;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySecurityMode;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyServiceKey;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySnapshot;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySnapshotValidator;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyVersion;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.events.TaskCreateEvent;
import org.apache.ambari.server.events.publishers.TaskEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ScopedWorkflowStateDAO;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CommandCompletion;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CreationBatchResult;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CreationGuard;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CreationItem;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.DraftGuard;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.LifecycleTransition;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.RepositoryGuard;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.ServiceVersionGuard;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyFenceEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntityPK;
import org.apache.ambari.server.orm.entities.ServiceDependencyOperationEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.apache.ambari.server.orm.entities.ScopedWorkflowStateEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.Subscribe;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;
import com.google.inject.util.Modules;

public class ServiceDependencyDAOIntegrationTest {
  private static final String MANAGED_HOST = "dependency-consumer-host";

  private Injector injector;
  private OrmTestHelper helper;
  private ServiceDependencyDAO dependencyDAO;
  private ManagedDependencyDescriptorResolver descriptorResolver;
  private Clusters clusters;
  private Cluster consumerCluster;
  private Cluster providerCluster;
  private Service consumerService;
  private RepositoryVersionEntity repository;
  private ActionDBAccessorImpl actionDBAccessor;
  private StageFactory stageFactory;
  private HostRoleCommandDAO hostRoleCommandDAO;
  private TaskEventPublisher taskEventPublisher;

  @Before
  public void setUp() throws Exception {
    descriptorResolver = mock(ManagedDependencyDescriptorResolver.class);
    injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule())
        .with(new AbstractModule() {
          @Override
          protected void configure() {
            bind(ManagedDependencyDescriptorResolver.class).toInstance(descriptorResolver);
          }
        }));
    injector.getInstance(GuiceJpaInitializer.class);
    helper = injector.getInstance(OrmTestHelper.class);
    dependencyDAO = injector.getInstance(ServiceDependencyDAO.class);
    actionDBAccessor = injector.getInstance(ActionDBAccessorImpl.class);
    stageFactory = injector.getInstance(StageFactory.class);
    hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    taskEventPublisher = injector.getInstance(TaskEventPublisher.class);
    helper.createCluster("dependency-consumer");
    helper.createCluster("dependency-provider");
    clusters = injector.getInstance(Clusters.class);
    consumerCluster = clusters.getCluster("dependency-consumer");
    providerCluster = clusters.getCluster("dependency-provider");
    repository = helper.getOrCreateRepositoryVersion(new StackId("HDP", "2.0.6"), "2.0.6");
    consumerService = consumerCluster.addService("HBASE", repository);
    providerCluster.addService("HDFS", repository);
    clusters.addHost(MANAGED_HOST);
    clusters.mapHostToCluster(MANAGED_HOST, consumerCluster.getClusterName());
    when(descriptorResolver.cluster(consumerCluster.getClusterId())).thenReturn(consumerCluster);
    when(descriptorResolver.cluster(providerCluster.getClusterId())).thenReturn(providerCluster);
    doCallRealMethod().when(descriptorResolver).validateEffectiveConsumerConfig(
        any(Cluster.class), any(ManagedDependencySnapshot.class));
  }

  @After
  public void tearDown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testCreateBeforeDeleteBlocksProviderAndConsumerRemoval() throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000101");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000102");
    publishBinding(bindingId, operationId);

    try {
      providerCluster.deleteService("HDFS", new DeleteHostComponentStatusMetaData());
      fail("An active provider reference must block provider service deletion");
    } catch (ManagedDependencyIntegrationException expected) {
      assertEquals("DEPENDENCY_PROVIDER_DELETE_BLOCKED", expected.getCode());
    }
    try {
      consumerCluster.deleteService("HBASE", new DeleteHostComponentStatusMetaData());
      fail("An active consumer reference must block HBase service deletion");
    } catch (ManagedDependencyIntegrationException expected) {
      assertEquals("DEPENDENCY_CONSUMER_DELETE_REQUIRES_DETACH", expected.getCode());
    }

    assertTrue(providerCluster.getServices().containsKey("HDFS"));
    assertTrue(consumerCluster.getServices().containsKey("HBASE"));
  }

  @Test
  public void testCreateBatchPersistsReversedProviderPairAndInitialIntentsAtomically() {
    providerCluster.addService("ZOOKEEPER", repository);
    UUID hdfsBindingId = UUID.fromString("00000000-0000-4000-8000-000000000201");
    UUID hdfsOperationId = UUID.fromString("00000000-0000-4000-8000-000000000202");
    UUID zooKeeperBindingId = UUID.fromString("00000000-0000-4000-8000-000000000203");
    UUID zooKeeperOperationId = UUID.fromString("00000000-0000-4000-8000-000000000204");
    CreationGuard guard = batchGuard();

    CreationItem hdfs = creationItem(hdfsBindingId, hdfsOperationId,
        ManagedDependencyType.HDFS, 901L, guard);
    CreationItem zooKeeper = creationItem(zooKeeperBindingId, zooKeeperOperationId,
        ManagedDependencyType.ZOOKEEPER, 902L, guard);
    CreationBatchResult result = dependencyDAO.createBatch(List.of(zooKeeper, hdfs));

    assertEquals(2, result.items().size());
    assertTrue(result.items().stream().allMatch(ServiceDependencyDAO.CreationResult::created));
    assertEquals(2, dependencyDAO.findAllBindings().size());
    assertNotNull(dependencyDAO.findBinding(hdfsBindingId.toString()));
    assertNotNull(dependencyDAO.findBinding(zooKeeperBindingId.toString()));
    assertEquals(1, dependencyDAO.findOperations(hdfsBindingId.toString()).size());
    assertEquals(1, dependencyDAO.findOperations(zooKeeperBindingId.toString()).size());
    assertEquals(2, dependencyDAO.findOutstandingCommands().size());
  }

  @Test
  public void testStaleSecondBatchGuardLeavesNoRowsOrProviderIntents() {
    providerCluster.addService("ZOOKEEPER", repository);
    UUID hdfsBindingId = UUID.fromString("00000000-0000-4000-8000-000000000211");
    UUID hdfsOperationId = UUID.fromString("00000000-0000-4000-8000-000000000212");
    UUID zooKeeperBindingId = UUID.fromString("00000000-0000-4000-8000-000000000213");
    UUID zooKeeperOperationId = UUID.fromString("00000000-0000-4000-8000-000000000214");

    CreationItem hdfs = creationItem(hdfsBindingId, hdfsOperationId,
        ManagedDependencyType.HDFS, 911L, batchGuard());
    CreationItem zooKeeper = creationItem(zooKeeperBindingId, zooKeeperOperationId,
        ManagedDependencyType.ZOOKEEPER, 912L, staleRepositoryGuard());

    assertThrows(ServiceDependencyDAO.StaleApprovalException.class,
        () -> dependencyDAO.createBatch(List.of(hdfs, zooKeeper)));
    assertTrue(dependencyDAO.findAllBindings().isEmpty());
    assertTrue(dependencyDAO.findOperations(hdfsBindingId.toString()).isEmpty());
    assertTrue(dependencyDAO.findOperations(zooKeeperBindingId.toString()).isEmpty());
    assertTrue(dependencyDAO.findOutstandingCommands().isEmpty());
  }

  @Test
  public void testExactBatchReplayUsesOriginalTargetsAfterMutableUpdates() {
    providerCluster.addService("ZOOKEEPER", repository);
    String draftKey = "replay-draft:7";
    injector.getInstance(ScopedWorkflowStateDAO.class).updateWithLock(draftKey, draft -> {
      draft.setOwnerUserId(7);
      draft.setCreatedClusterId(consumerCluster.getClusterId());
      draft.setWorkflow("CLUSTER_CREATE");
      draft.setPhase("REVIEW");
      draft.setPayload("{}");
      return draft;
    });
    CreationGuard guard = batchGuard(new DraftGuard(draftKey, 7, 0L,
        consumerCluster.getClusterId()));
    UUID hdfsBindingId = UUID.fromString("00000000-0000-4000-8000-000000000221");
    UUID hdfsOperationId = UUID.fromString("00000000-0000-4000-8000-000000000222");
    UUID zooKeeperBindingId = UUID.fromString("00000000-0000-4000-8000-000000000223");
    UUID zooKeeperOperationId = UUID.fromString("00000000-0000-4000-8000-000000000224");
    CreationItem hdfs = creationItem(hdfsBindingId, hdfsOperationId,
        ManagedDependencyType.HDFS, 921L, guard);
    CreationItem zooKeeper = creationItem(zooKeeperBindingId, zooKeeperOperationId,
        ManagedDependencyType.ZOOKEEPER, 922L, guard);
    dependencyDAO.createBatch(List.of(hdfs, zooKeeper));

    String replayedRepositoryVersion = injector.getInstance(ReplayMutation.class).update(
        hdfsBindingId.toString(), zooKeeperBindingId.toString(), draftKey, repository.getId());

    CreationBatchResult replay = dependencyDAO.createBatch(List.of(
        creationItem(hdfsBindingId, hdfsOperationId, ManagedDependencyType.HDFS, 921L, guard),
        creationItem(zooKeeperBindingId, zooKeeperOperationId,
            ManagedDependencyType.ZOOKEEPER, 922L, guard)));

    assertEquals(2, replay.items().size());
    assertTrue(replay.items().stream().noneMatch(ServiceDependencyDAO.CreationResult::created));
    ServiceDependencyBindingEntity current = dependencyDAO.findBinding(hdfsBindingId.toString());
    assertEquals("READY", current.getState());
    assertEquals(Long.valueOf(2L), current.getDesiredSnapshotVersion());
    assertEquals(hash('9'), current.getProviderFingerprint());
    ServiceDependencyBindingEntity currentZooKeeper =
        dependencyDAO.findBinding(zooKeeperBindingId.toString());
    assertEquals("READY", currentZooKeeper.getState());
    assertEquals(Long.valueOf(2L), currentZooKeeper.getDesiredSnapshotVersion());
    assertEquals(hash('9'), currentZooKeeper.getProviderFingerprint());
    assertEquals(Long.valueOf(1L), replay.items().get(0).snapshot().getSnapshotVersion());
    assertEquals(Long.valueOf(1L), replay.items().get(1).snapshot().getSnapshotVersion());
    assertEquals(2, dependencyDAO.findAllBindings().size());
    assertEquals(1, dependencyDAO.findOperations(hdfsBindingId.toString()).size());
    assertEquals(2L, injector.getInstance(ScopedWorkflowStateDAO.class)
        .findByKey(draftKey).getRevision().longValue());
    assertEquals("2.0.6-replayed", replayedRepositoryVersion);
  }

  @Test
  public void testPartialAndMismatchedBatchReplayCannotCreateOrRewritePeers() {
    providerCluster.addService("ZOOKEEPER", repository);
    UUID hdfsBindingId = UUID.fromString("00000000-0000-4000-8000-000000000231");
    UUID hdfsOperationId = UUID.fromString("00000000-0000-4000-8000-000000000232");
    UUID zooKeeperBindingId = UUID.fromString("00000000-0000-4000-8000-000000000233");
    UUID zooKeeperOperationId = UUID.fromString("00000000-0000-4000-8000-000000000234");
    CreationGuard guard = batchGuard();
    dependencyDAO.createBatch(List.of(
        creationItem(hdfsBindingId, hdfsOperationId, ManagedDependencyType.HDFS, 931L, guard),
        creationItem(zooKeeperBindingId, zooKeeperOperationId,
            ManagedDependencyType.ZOOKEEPER, 932L, guard)));

    UUID missingBindingId = UUID.fromString("00000000-0000-4000-8000-000000000235");
    UUID missingOperationId = UUID.fromString("00000000-0000-4000-8000-000000000236");
    assertThrows(ServiceDependencyDAO.StaleApprovalException.class,
        () -> dependencyDAO.createBatch(List.of(
            creationItem(hdfsBindingId, hdfsOperationId, ManagedDependencyType.HDFS, 931L, guard),
            creationItem(missingBindingId, missingOperationId,
                ManagedDependencyType.ZOOKEEPER, 932L, guard))));
    assertEquals(2, dependencyDAO.findAllBindings().size());
    assertEquals(2, dependencyDAO.findOutstandingCommands().size());

    ServiceDependencySnapshotEntity mismatch = snapshot(zooKeeperBindingId,
        ManagedDependencyType.ZOOKEEPER);
    mismatch.setSnapshotFingerprint(hash('0'));
    assertThrows(ServiceDependencyDAO.StaleApprovalException.class,
        () -> dependencyDAO.createBatch(List.of(
            creationItem(hdfsBindingId, hdfsOperationId, ManagedDependencyType.HDFS, 931L, guard),
            new CreationItem(binding(zooKeeperBindingId, zooKeeperOperationId,
                ManagedDependencyType.ZOOKEEPER), mismatch,
                operation(zooKeeperBindingId, zooKeeperOperationId),
                providerIntent(zooKeeperBindingId, zooKeeperOperationId, 932L,
                    ManagedDependencyType.ZOOKEEPER), guard))));
    assertEquals(2, dependencyDAO.findAllBindings().size());
    assertEquals("PROVISIONING", dependencyDAO.findBinding(zooKeeperBindingId.toString()).getState());
  }

  @Test
  public void testMixedBulkDeletionPrevalidatesEveryServiceBeforeMutation() throws Exception {
    providerCluster.addService("TEZ", repository);
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000111");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000112");
    publishBinding(bindingId, operationId);

    try {
      providerCluster.deleteAllServices();
      fail("A bulk delete must reject the referenced provider before removing any service");
    } catch (ManagedDependencyIntegrationException expected) {
      assertEquals("DEPENDENCY_PROVIDER_DELETE_BLOCKED", expected.getCode());
    }

    assertTrue(providerCluster.getServices().containsKey("HDFS"));
    assertTrue(providerCluster.getServices().containsKey("TEZ"));
  }

  @Test
  public void testDeletionWriteBoundaryBlocksConcurrentBindingPublication() throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-0000-000000000131");
    UUID operationId = UUID.fromString("00000000-0000-4000-0000-000000000132");
    providerCluster.addService("TEZ", repository);
    ServiceDependencyBindingEntity binding = binding(bindingId, operationId);
    ServiceDependencySnapshotEntity snapshot = snapshot(bindingId);
    ServiceDependencyOperationEntity operation = operation(bindingId, operationId);
    CreationGuard guard = new CreationGuard(null,
        List.of(new RepositoryGuard(repository.getId(), repository.getVersion(), true)),
        List.of(serviceGuard(consumerCluster, "HBASE"), serviceGuard(providerCluster, "HDFS")));
    CountDownLatch deletionEntered = new CountDownLatch(1);
    CountDownLatch releaseDeletion = new CountDownLatch(1);
    CountDownLatch publicationStarted = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    DeletionTransactionBarrier deletionBarrier =
        injector.getInstance(DeletionTransactionBarrier.class);
    try {
      Future<?> deletion = executor.submit(() -> {
        try {
          deletionBarrier.deleteAllServicesAndWait(
              providerCluster, deletionEntered, releaseDeletion);
        } catch (Exception e) {
          throw new AssertionError("The unreferenced provider cluster should be removable", e);
        }
        return null;
      });
      assertTrue(deletionEntered.await(30, TimeUnit.SECONDS));

      Future<?> publication = executor.submit(() -> {
        publicationStarted.countDown();
        withCanonicalReadLocks(() -> dependencyDAO.create(binding, snapshot, operation, guard));
        return null;
      });
      assertTrue(publicationStarted.await(30, TimeUnit.SECONDS));
      try {
        publication.get(250, TimeUnit.MILLISECONDS);
        fail("Binding publication bypassed the deletion write boundary");
      } catch (TimeoutException expected) {
        // The publisher holds parent read locks only after the deletion releases its write lock.
      }

      releaseDeletion.countDown();
      deletion.get(30, TimeUnit.SECONDS);
      try {
        publication.get(30, TimeUnit.SECONDS);
        fail("Publication must recheck the deleted provider service row");
      } catch (ExecutionException expected) {
        assertTrue(hasCause(expected, ServiceDependencyDAO.StaleApprovalException.class));
      }
      assertFalse(providerCluster.getServices().containsKey("HDFS"));
      assertFalse(providerCluster.getServices().containsKey("TEZ"));
      assertTrue(dependencyDAO.findAllBindings().isEmpty());
    } finally {
      releaseDeletion.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void testApprovalPublicationCompletesBeforeConcurrentVersionWriter() throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000021");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000022");
    ServiceDependencyBindingEntity binding = binding(bindingId, operationId);
    ServiceDependencySnapshotEntity snapshot = snapshot(bindingId);
    ServiceDependencyOperationEntity operation = operation(bindingId, operationId);
    CreationGuard guard = new CreationGuard(null,
        List.of(new RepositoryGuard(repository.getId(), repository.getVersion(), true)),
        List.of(serviceGuard(consumerCluster, "HBASE"), serviceGuard(providerCluster, "HDFS")));
    RepositoryVersionEntity replacement = helper.getOrCreateRepositoryVersion(
        new StackId("HDP", "2.0.6"), "2.0.6.1");

    CountDownLatch approvalHasReadLocks = new CountDownLatch(1);
    CountDownLatch publishApproval = new CountDownLatch(1);
    CountDownLatch writerStarted = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> approval = executor.submit(() -> withCanonicalReadLocks(() -> {
        approvalHasReadLocks.countDown();
        await(publishApproval);
        dependencyDAO.create(binding, snapshot, operation, guard);
      }));
      if (!approvalHasReadLocks.await(30, TimeUnit.SECONDS)) {
        fail("Approval did not acquire the cluster read locks");
      }
      Future<?> writer = executor.submit(() -> {
        writerStarted.countDown();
        consumerService.setDesiredRepositoryVersion(replacement);
        return null;
      });
      if (!writerStarted.await(30, TimeUnit.SECONDS)) {
        fail("Version writer did not start");
      }
      try {
        writer.get(250, TimeUnit.MILLISECONDS);
        fail("The version writer bypassed the approval read lock");
      } catch (TimeoutException expected) {
        // The writer remains behind the cluster write lock until publication commits.
      }

      publishApproval.countDown();
      approval.get(30, TimeUnit.SECONDS);
      writer.get(30, TimeUnit.SECONDS);
      assertNotNull(dependencyDAO.findBinding(bindingId.toString()));
      assertEquals(replacement.getId(), consumerService.getDesiredRepositoryVersion().getId());
      assertFalse(repository.getId().equals(replacement.getId()));
    } finally {
      publishApproval.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void testCommittedApprovalRejectsWaitingConflictingHbaseConfigWriter() throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000031");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000032");
    ServiceDependencyBindingEntity binding = binding(bindingId, operationId);
    ServiceDependencySnapshotEntity snapshot = snapshot(bindingId);
    ServiceDependencyOperationEntity operation = operation(bindingId, operationId);
    CreationGuard guard = new CreationGuard(null,
        List.of(new RepositoryGuard(repository.getId(), repository.getVersion(), true)),
        List.of(serviceGuard(consumerCluster, "HBASE"), serviceGuard(providerCluster, "HDFS")));
    Config conflicting = injector.getInstance(ConfigFactory.class).createNew(consumerCluster,
        "hbase-site", "conflicting-managed-path", Map.of(
            "hbase.rootdir", "hdfs://other/root",
            "hbase.wal.dir", "hdfs://other/wal"), Map.of());

    CountDownLatch approvalHasReadLocks = new CountDownLatch(1);
    CountDownLatch publishApproval = new CountDownLatch(1);
    CountDownLatch writerStarted = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> approval = executor.submit(() -> withCanonicalReadLocks(() -> {
        approvalHasReadLocks.countDown();
        await(publishApproval);
        dependencyDAO.create(binding, snapshot, operation, guard);
      }));
      assertTrue(approvalHasReadLocks.await(30, TimeUnit.SECONDS));
      Future<?> writer = executor.submit(() -> {
        writerStarted.countDown();
        consumerCluster.addDesiredConfig("admin", Set.of(conflicting));
        return null;
      });
      assertTrue(writerStarted.await(30, TimeUnit.SECONDS));
      try {
        writer.get(250, TimeUnit.MILLISECONDS);
        fail("The config writer bypassed the approval read lock");
      } catch (TimeoutException expected) {
        // The writer evaluates the binding only after the approval transaction commits.
      }

      publishApproval.countDown();
      approval.get(30, TimeUnit.SECONDS);
      try {
        writer.get(30, TimeUnit.SECONDS);
        fail("A conflicting HBase configuration was published after binding approval");
      } catch (ExecutionException expected) {
        assertTrue(expected.getCause() instanceof ManagedDependencyIntegrationException);
        assertEquals("DEPENDENCY_MANAGED_CONFIG_CONFLICT",
            ((ManagedDependencyIntegrationException) expected.getCause()).getCode());
      }
      assertNotNull(dependencyDAO.findBinding(bindingId.toString()));
    } finally {
      publishApproval.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void testApprovedLivePlanReadIsAtomicWithBindingPublication() throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-0000000000d1");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-0000000000d2");
    ServiceDependencyBindingEntity binding = binding(bindingId, operationId);
    CreationGuard guard = new CreationGuard(null,
        List.of(new RepositoryGuard(repository.getId(), repository.getVersion(), true)),
        List.of(serviceGuard(consumerCluster, "HBASE"), serviceGuard(providerCluster, "HDFS")));
    CountDownLatch readLocked = new CountDownLatch(1);
    CountDownLatch releaseRead = new CountDownLatch(1);
    CountDownLatch publisherStarted = new CountDownLatch(1);
    AtomicReference<Optional<List<ManagedDependencySnapshot>>> observed = new AtomicReference<>();
    LivePlanReadBarrier readBarrier = injector.getInstance(LivePlanReadBarrier.class);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> reader = executor.submit(() -> readBarrier.readAndHold(
          consumerCluster.getClusterId(), observed, readLocked, releaseRead));
      assertTrue(readLocked.await(30, TimeUnit.SECONDS));
      assertTrue(observed.get().isEmpty());

      Future<?> publisher = executor.submit(() -> {
        publisherStarted.countDown();
        dependencyDAO.create(binding, snapshot(bindingId),
            operation(bindingId, operationId), guard);
        return null;
      });
      assertTrue(publisherStarted.await(30, TimeUnit.SECONDS));
      try {
        publisher.get(250, TimeUnit.MILLISECONDS);
        fail("Binding publication bypassed the live-plan service-row read lock");
      } catch (TimeoutException expected) {
        // The writer publishes only after the complete empty-plan read commits.
      }

      releaseRead.countDown();
      reader.get(30, TimeUnit.SECONDS);
      publisher.get(30, TimeUnit.SECONDS);
      Optional<List<ManagedDependencySnapshot>> published =
          dependencyDAO.findApprovedLivePlanSnapshots(consumerCluster.getClusterId());
      assertTrue(published.isPresent());
      assertEquals(List.of(ManagedDependencyType.HDFS),
          published.orElseThrow().stream().map(ManagedDependencySnapshot::type).toList());
    } finally {
      releaseRead.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void testActionPublicationRetainsClusterLocksThroughTaskAssociationCommit()
      throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000091");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000092");
    ManagedDependencySnapshot managedSnapshot = dispatchSnapshot(bindingId);
    Config exactHbaseSite = hbaseSite(managedSnapshot, "managed-exact");
    Config exactHbaseEnv = hbaseEnv(managedSnapshot, "managed-exact");
    Config conflicting = injector.getInstance(ConfigFactory.class).createNew(consumerCluster,
        "hbase-site", "managed-conflict", Map.of(
            "hbase.rootdir", "hdfs://other/root",
            "hbase.wal.dir", "hdfs://other/wal"), Map.of());
    consumerCluster.addDesiredConfig("admin", Set.of(exactHbaseSite, exactHbaseEnv));
    DispatchScenario scenario = dispatchScenario(bindingId, operationId, managedSnapshot, 901L);

    CountDownLatch publicationEntered = new CountDownLatch(1);
    CountDownLatch releasePublication = new CountDownLatch(1);
    CountDownLatch writerStarted = new CountDownLatch(1);
    taskEventPublisher.register(new Object() {
      @Subscribe
      public void onTaskCreate(TaskCreateEvent event) {
        if (event.getHostRoleCommands().stream()
            .anyMatch(command -> command.getRequestId() == scenario.request().getRequestId())) {
          publicationEntered.countDown();
          await(releasePublication);
        }
      }
    });

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> publication = executor.submit(() -> {
        actionDBAccessor.persistActions(scenario.request());
        return null;
      });
      assertTrue(publicationEntered.await(30, TimeUnit.SECONDS));

      Future<?> writer = executor.submit(() -> {
        writerStarted.countDown();
        consumerCluster.addDesiredConfig("admin", Set.of(conflicting));
        return null;
      });
      assertTrue(writerStarted.await(30, TimeUnit.SECONDS));
      try {
        writer.get(250, TimeUnit.MILLISECONDS);
        fail("The config writer entered before the action transaction committed");
      } catch (TimeoutException expected) {
        // The writer remains behind the canonical consumer cluster read lock.
      }

      releasePublication.countDown();
      publication.get(30, TimeUnit.SECONDS);
      try {
        writer.get(30, TimeUnit.SECONDS);
        fail("A conflicting managed HBase configuration was published");
      } catch (ExecutionException expected) {
        assertTrue(expected.getCause() instanceof ManagedDependencyIntegrationException);
        assertEquals("DEPENDENCY_MANAGED_CONFIG_CONFLICT",
            ((ManagedDependencyIntegrationException) expected.getCause()).getCode());
      }

      ServiceDependencyHostResultEntity associated = dependencyDAO.findHostResult(
          bindingId.toString(), 1L, 1L, scenario.hostId(), "HDFS", "PREPARE_HDFS_CONSUMER");
      assertEquals("DISPATCHED", associated.getState());
      assertNotNull(associated.getAmbariTaskId());
      assertEquals(1, hostRoleCommandDAO.findByRequest(
          scenario.request().getRequestId(), true).size());
    } finally {
      releasePublication.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void testConfigCommittedFirstRejectsActionPublicationAndRollsBackTaskAssociation()
      throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-0000000000a2");
    ManagedDependencySnapshot managedSnapshot = dispatchSnapshot(bindingId);
    Config conflicting = injector.getInstance(ConfigFactory.class).createNew(consumerCluster,
        "hbase-site", "managed-conflict-first", Map.of(
            "hbase.rootdir", "hdfs://other/root",
            "hbase.wal.dir", "hdfs://other/wal"), Map.of());
    Config exactHbaseEnv = hbaseEnv(managedSnapshot, "managed-conflict-first");
    consumerCluster.addDesiredConfig("admin", Set.of(conflicting, exactHbaseEnv));
    DispatchScenario scenario = dispatchScenario(bindingId, operationId, managedSnapshot, 902L);

    try {
      actionDBAccessor.persistActions(scenario.request());
      fail("Action publication accepted configuration that changed before its transaction");
    } catch (ManagedDependencyIntegrationException expected) {
      assertEquals("DEPENDENCY_MANAGED_CONFIG_MISMATCH", expected.getCode());
    }

    ServiceDependencyHostResultEntity unassociated = dependencyDAO.findHostResult(
        bindingId.toString(), 1L, 1L, scenario.hostId(), "HDFS", "PREPARE_HDFS_CONSUMER");
    assertEquals("INTENT", unassociated.getState());
    org.junit.Assert.assertNull(unassociated.getAmbariTaskId());
    assertTrue(hostRoleCommandDAO.findByRequest(scenario.request().getRequestId(), true).isEmpty());
  }

  @Test
  public void testCheckedActionPublicationFailureRollsBackRowsAndReleasesParentLocks()
      throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-0000-0000000000b1");
    UUID operationId = UUID.fromString("00000000-0000-4000-0000-0000000000b2");
    ManagedDependencySnapshot managedSnapshot = dispatchSnapshot(bindingId);
    DispatchScenario scenario = dispatchScenario(bindingId, operationId, managedSnapshot, 903L);

    long differentHostId = scenario.hostId() + 1;
    ManagedDependencyCommand mismatchedCommand = ManagedDependencyCommand.prepareConsumer(
        managedSnapshot, operationId, 1L, differentHostId, "hadoop_3_3_0_0_1-client",
        managedSnapshot.providerVersion().serviceVersion(), hash('i'));
    ManagedDependencyCommandBundle mismatchedBundle = ManagedDependencyCommandBundle.of(
        differentHostId, managedSnapshot.consumerIdentity().effectiveShortUser(), hash('i'),
        List.of(mismatchedCommand));
    scenario.request().getStages().get(0).getOrderedHostRoleCommands().get(0)
        .getExecutionCommandWrapper().getExecutionCommand().setCommandParams(Map.of(
            ManagedDependencyRuntimePlanner.BUNDLE_PARAMETER,
            StageUtils.getGson().toJson(mismatchedBundle)));

    assertThrows(AmbariException.class, () -> actionDBAccessor.persistActions(scenario.request()));

    ServiceDependencyHostResultEntity intent = dependencyDAO.findHostResult(
        bindingId.toString(), 1L, 1L, scenario.hostId(), "HDFS", "PREPARE_HDFS_CONSUMER");
    assertEquals("INTENT", intent.getState());
    org.junit.Assert.assertNull(intent.getAmbariTaskId());
    assertTrue(hostRoleCommandDAO.findByRequest(scenario.request().getRequestId(), true).isEmpty());
    assertTrue(actionDBAccessor.getRequestEntity(scenario.request().getRequestId()) == null);

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<?> lockProbe = executor.submit(() -> {
        consumerCluster.executeUnderWriteLock(() -> { });
        providerCluster.executeUnderWriteLock(() -> { });
      });
      lockProbe.get(30, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void testVerificationFailureCannotBeHiddenByAnotherHostOrLatePreparation() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000041");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000042");
    createVerificationScenario(bindingId, operationId, 41L, 42L);

    completeVerification(bindingId, 41L, false, Set.of(41L, 42L));
    completeVerification(bindingId, 42L, true, Set.of(41L, 42L));

    ServiceDependencyBindingEntity failed = dependencyDAO.findBinding(bindingId.toString());
    assertEquals("FAILED", failed.getState());
    assertEquals("VERIFY_FAILED", failed.getFailureCode());

    UUID reverseBindingId = UUID.fromString("00000000-0000-4000-8000-000000000051");
    UUID reverseOperationId = UUID.fromString("00000000-0000-4000-8000-000000000052");
    createVerificationScenario(reverseBindingId, reverseOperationId, 51L, 52L);

    completeVerification(reverseBindingId, 52L, true, Set.of(51L, 52L));
    completeVerification(reverseBindingId, 51L, false, Set.of(51L, 52L));

    ServiceDependencyBindingEntity reverseFailed =
        dependencyDAO.findBinding(reverseBindingId.toString());
    assertEquals("FAILED", reverseFailed.getState());
    assertEquals("VERIFY_FAILED", reverseFailed.getFailureCode());
  }

  @Test
  public void testLateClientPreparationFailureDoesNotDowngradeReadyBinding() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000061");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000062");
    createVerificationScenario(bindingId, operationId, 61L, 62L);
    completeVerification(bindingId, 61L, true, Set.of(61L, 62L));
    completeVerification(bindingId, 62L, true, Set.of(61L, 62L));
    assertEquals("READY", dependencyDAO.findBinding(bindingId.toString()).getState());

    ServiceDependencyHostResultEntity late = command(
        bindingId, operationId, 1L, 63L, "PREPARE_HDFS_CONSUMER", "HBASE_CLIENT");
    dependencyDAO.planCommand(late);
    dependencyDAO.associatePreparationTask(late, "HBASE_CLIENT", 601L, 1L, 601L);
    dependencyDAO.completeCommand(id(late), 601L, 1L, 601L,
        completion("late-preparation-failed", "FAILED", "PREPARE_FAILED",
            "PREPARE_HDFS_CONSUMER", null));

    ServiceDependencyBindingEntity stillReady = dependencyDAO.findBinding(bindingId.toString());
    assertEquals("READY", stillReady.getState());
    org.junit.Assert.assertNull(stillReady.getFailureCode());

    ServiceDependencyHostResultEntity malformed = command(
        bindingId, operationId, 1L, 64L, "PREPARE_HDFS_CONSUMER", "HBASE_CLIENT");
    dependencyDAO.planCommand(malformed);
    dependencyDAO.associatePreparationTask(malformed, "HBASE_CLIENT", 602L, 1L, 602L);
    dependencyDAO.failTaskCommands(602L, 1L, 602L,
        "DEPENDENCY_RESULT_INVALID", "The managed dependency task returned an invalid result.");

    stillReady = dependencyDAO.findBinding(bindingId.toString());
    assertEquals("READY", stillReady.getState());
    org.junit.Assert.assertNull(stillReady.getFailureCode());
    assertEquals("FAILED", dependencyDAO.findHostResult(bindingId.toString(), 1L, 1L, 64L,
        "HDFS", "PREPARE_HDFS_CONSUMER").getState());
  }

  @Test
  public void testStaleVerificationCannotBeHiddenByLaterHostSuccess() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000071");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000072");
    createVerificationScenario(bindingId, operationId, 71L, 72L);

    completeVerification(bindingId, 71L, "STALE_REJECTED",
        "DEPENDENCY_OPERATION_STALE", false, Set.of(71L, 72L));
    completeVerification(bindingId, 72L, true, Set.of(71L, 72L));

    ServiceDependencyBindingEntity failed = dependencyDAO.findBinding(bindingId.toString());
    assertEquals("FAILED", failed.getState());
    assertEquals("DEPENDENCY_OPERATION_STALE", failed.getFailureCode());
    assertFalse(failed.getFailureRetryable());
    assertEquals("STALE_REJECTED",
        dependencyDAO.findOperation(operationId.toString()).getState());
  }

  @Test
  public void testMalformedRequiredDaemonPreparationInvalidatesReadyState() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000081");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000082");
    createVerificationScenario(bindingId, operationId, 81L, 82L);
    completeVerification(bindingId, 81L, true, Set.of(81L, 82L));
    completeVerification(bindingId, 82L, true, Set.of(81L, 82L));
    assertEquals("READY", dependencyDAO.findBinding(bindingId.toString()).getState());

    ServiceDependencyHostResultEntity newDaemon = command(
        bindingId, operationId, 1L, 83L, "PREPARE_HDFS_CONSUMER", "HBASE_MASTER");
    dependencyDAO.planCommand(newDaemon);
    assertEquals("PROVISIONING", dependencyDAO.findBinding(bindingId.toString()).getState());
    dependencyDAO.associatePreparationTask(newDaemon, "HBASE_MASTER", 801L, 1L, 801L);
    dependencyDAO.failTaskCommands(801L, 1L, 801L,
        "DEPENDENCY_RESULT_INVALID", "The managed dependency task returned an invalid result.");

    ServiceDependencyBindingEntity failed = dependencyDAO.findBinding(bindingId.toString());
    assertEquals("FAILED", failed.getState());
    assertEquals("DEPENDENCY_RESULT_INVALID", failed.getFailureCode());
  }

  @Test
  public void testSameSnapshotRetryRetainsPriorEpochAndRejectsOldCallback() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-0000000000b1");
    UUID originalOperationId = UUID.fromString("00000000-0000-4000-8000-0000000000b2");
    UUID retryOperationId = UUID.fromString("00000000-0000-4000-8000-0000000000b3");
    createVerificationScenario(bindingId, originalOperationId, 91L, 92L);
    completeVerification(bindingId, 91L, false, Set.of(91L, 92L));

    ServiceDependencyBindingEntity failed = dependencyDAO.findBinding(bindingId.toString());
    ServiceDependencyOperationEntity retry = operation(bindingId, retryOperationId);
    retry.setOperationKind("RETRY");
    retry.setOperationEpoch(2L);
    retry.setRequestHash(hash('r'));
    LifecycleTransition transition = dependencyDAO.startRetry(
        bindingId.toString(), failed.getRowVersion(), retry, 7);

    ServiceDependencyHostResultEntity retried = command(
        bindingId, retryOperationId, 1L, 91L,
        "PREPARE_HDFS_CONSUMER", "HBASE_REGIONSERVER");
    retried.setOperationEpoch(2L);
    dependencyDAO.planCommand(retried);
    LifecycleTransition exactReplay = dependencyDAO.startRetry(
        bindingId.toString(), failed.getRowVersion(), retry, 7);

    ServiceDependencyHostResultEntity prior = dependencyDAO.findHostResult(
        bindingId.toString(), 1L, 1L, 91L, "HDFS", "VERIFY_HDFS_CONSUMER");
    assertEquals("FAILED", prior.getState());
    assertNotNull(dependencyDAO.findHostResult(
        bindingId.toString(), 1L, 2L, 91L, "HDFS", "PREPARE_HDFS_CONSUMER"));
    assertEquals(2L, transition.binding().getOperationEpoch().longValue());
    assertFalse(exactReplay.created());
    assertEquals(2, dependencyDAO.findOperations(bindingId.toString()).size());
    try {
      dependencyDAO.completeCommand(id(prior), prior.getAmbariRequestId(),
          prior.getAmbariStageId(), prior.getAmbariTaskId(),
          completion("late-old-epoch", "SUCCEEDED", null,
              "VERIFY_HDFS_CONSUMER", Set.of(91L)));
      fail("A callback from the fenced operation epoch was accepted");
    } catch (ServiceDependencyDAO.StaleApprovalException expected) {
      assertEquals(2L, dependencyDAO.findBinding(bindingId.toString())
          .getOperationEpoch().longValue());
    }
  }

  @Test
  public void testSuccessfulDetachRetainsReplayFenceAndReleasesServiceReferences()
      throws Exception {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-0000000000c1");
    UUID createOperationId = UUID.fromString("00000000-0000-4000-8000-0000000000c2");
    UUID detachOperationId = UUID.fromString("00000000-0000-4000-8000-0000000000c3");
    ServiceDependencyBindingEntity binding = binding(bindingId, createOperationId);
    long actionHostId = clusters.getHost(MANAGED_HOST).getHostId();
    binding.setProviderPreparationHash(hash('p'));
    binding.setActionHostId(actionHostId);
    CreationGuard guard = new CreationGuard(null,
        List.of(new RepositoryGuard(repository.getId(), repository.getVersion(), true)),
        List.of(serviceGuard(consumerCluster, "HBASE"), serviceGuard(providerCluster, "HDFS")));
    dependencyDAO.create(binding, snapshot(bindingId),
        operation(bindingId, createOperationId), guard);

    ServiceDependencyOperationEntity detach = operation(bindingId, detachOperationId);
    detach.setOperationKind("DETACH");
    detach.setOperationEpoch(2L);
    detach.setRequestHash(hash('x'));
    ManagedDependencySnapshot snapshotValue = StageUtils.getGson().fromJson(
        snapshot(bindingId).getSnapshotJson(), ManagedDependencySnapshot.class);
    ManagedDependencyCommand invalidation = ManagedDependencyCommand.invalidate(
        snapshotValue, detachOperationId, 2L, actionHostId);
    ServiceDependencyHostResultEntity command = dispatchCommand(invalidation, actionHostId);
    command.setComponentName("NAMENODE");

    ServiceDependencyBindingEntity current = dependencyDAO.findBinding(bindingId.toString());
    LifecycleTransition transition = dependencyDAO.startDetach(bindingId.toString(),
        current.getRowVersion(), detach, command, 7);
    dependencyDAO.claimCommandDispatch(id(transition.command()));
    dependencyDAO.markCommandDispatched(id(transition.command()), 1201L, 1L, 1201L);
    dependencyDAO.completeCommand(id(transition.command()), 1201L, 1L, 1201L,
        new CommandCompletion("{}", hash('y'), "SUCCEEDED", null, null, null, false,
            "DETACHING", "DETACHING", "SUCCEEDED", null, null, null,
            null, null, null, null, null, null, null, null, null, null, null));

    org.junit.Assert.assertNull(dependencyDAO.findBinding(bindingId.toString()));
    ServiceDependencyFenceEntity fence = dependencyDAO.findFence(bindingId.toString());
    assertNotNull(fence);
    assertEquals(detachOperationId.toString(), fence.getDetachOperationId());
    assertEquals(hash('x'), fence.getDetachRequestHash());
    assertNotNull(providerCluster.getService("HDFS"));
    assertTrue(dependencyDAO.findByConsumer(consumerCluster.getClusterId(), "HBASE").isEmpty());
    consumerCluster.deleteService("HBASE", new DeleteHostComponentStatusMetaData());
    assertFalse(consumerCluster.getServices().containsKey("HBASE"));
  }

  private ManagedDependencySnapshot dispatchSnapshot(UUID bindingId) throws Exception {
    if (consumerService.getServiceComponents().isEmpty()) {
      consumerService.addServiceComponent("HBASE_MASTER").addServiceComponentHost(MANAGED_HOST);
    }
    ManagedDependencyIdentity.Plan plan =
        ManagedDependencyIdentity.Plan.forExistingCluster(consumerCluster.getClusterId());
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        plan.plannedShortUser(), new TreeSet<>(), false,
        plan.plannedShortUser(), true, "0700", false);
    TreeMap<String, String> resolvedVersions = new TreeMap<>(
        Map.of("distribution", "3.3.0-1"));
    TreeSet<String> clientFeatures = new TreeSet<>(Set.of("STANDARD_RPC_CLIENT"));
    ManagedDependencyVersion consumerVersion = new ManagedDependencyVersion(
        "BIGTOP", "3.3.0", true, "3.3.0", resolvedVersions,
        clientFeatures, repository.getId(), List.of());
    ManagedDependencyVersion providerVersion = new ManagedDependencyVersion(
        "BIGTOP", "3.3.0", true, "3.3.0", resolvedVersions,
        clientFeatures, repository.getId(), List.of());
    Consumer initialConsumer = new Consumer("SERVICE", null, consumerCluster.getClusterId(),
        consumerCluster.getClusterName(), "HBASE", ConsumerLifecycle.INIT_UNINSTALLED,
        consumerVersion, ManagedDependencySecurityMode.INSECURE, "", identity, plan);
    Consumer managedConsumer = new Consumer("SERVICE", null, consumerCluster.getClusterId(),
        consumerCluster.getClusterName(), "HBASE", ConsumerLifecycle.MANAGED_UPDATE,
        consumerVersion, ManagedDependencySecurityMode.INSECURE, "", identity, plan);
    Provider provider = new Provider(new ManagedDependencyServiceKey(
        providerCluster.getClusterId(), "HDFS"), ManagedDependencyType.HDFS,
        providerVersion, true, true, ManagedDependencySecurityMode.INSECURE, "", null,
        new HdfsEndpoint("hdfs://provider.example.test:8020", false, "",
            new TreeMap<>(), "", false, false), null,
        Map.of("fs.defaultFS", "hdfs://provider.example.test:8020",
            "hadoop.security.authentication", "simple"),
        Map.of(), Map.of(), Set.of("fs.defaultFS"), Set.of());
    ManagedDependencySnapshotValidator.ValidationResult validation =
        new ManagedDependencySnapshotValidator(false).validate(bindingId, 1L,
            ManagedDependencyType.HDFS, initialConsumer, provider, List.of());
    assertTrue("The deterministic dispatch descriptor must be valid: " + validation.issues(),
        validation.isValid());
    when(descriptorResolver.resolveService(new ManagedDependencyServiceKey(
        consumerCluster.getClusterId(), "HBASE"))).thenReturn(managedConsumer);
    when(descriptorResolver.resolveProvider(new ManagedDependencyServiceKey(
        providerCluster.getClusterId(), "HDFS"))).thenReturn(provider);
    return validation.snapshot().orElseThrow();
  }

  private Config hbaseSite(ManagedDependencySnapshot snapshot, String tag) {
    return injector.getInstance(ConfigFactory.class).createNew(consumerCluster,
        "hbase-site", tag, Map.of(
            "hbase.rootdir", snapshot.namespace().rootUri(),
            "hbase.wal.dir", snapshot.namespace().walUri()), Map.of());
  }

  private Config hbaseEnv(ManagedDependencySnapshot snapshot, String tag) {
    return injector.getInstance(ConfigFactory.class).createNew(consumerCluster,
        "hbase-env", tag, Map.of(
            "hbase_user", snapshot.consumerIdentity().effectiveShortUser()), Map.of());
  }

  private DispatchScenario dispatchScenario(UUID bindingId, UUID operationId,
      ManagedDependencySnapshot snapshot, long requestId) throws Exception {
    ServiceDependencyBindingEntity binding = dispatchBinding(bindingId, operationId, snapshot);
    ServiceDependencyOperationEntity operation = operation(bindingId, operationId);
    operation.setState("PROVIDER_PREPARED");
    CreationGuard guard = new CreationGuard(null,
        List.of(new RepositoryGuard(repository.getId(), repository.getVersion(), true)),
        List.of(serviceGuard(consumerCluster, "HBASE"),
            serviceGuard(providerCluster, "HDFS")));
    dependencyDAO.create(binding, dispatchSnapshotEntity(snapshot), operation, guard);

    long hostId = clusters.getHost(MANAGED_HOST).getHostId();
    ManagedDependencyCommand command = ManagedDependencyCommand.prepareConsumer(
        snapshot, operationId, 1L, hostId, "hadoop_3_3_0_0_1-client",
        snapshot.providerVersion().serviceVersion(), hash('i'));
    dependencyDAO.planCommand(dispatchCommand(command, hostId));
    ManagedDependencyCommandBundle bundle = ManagedDependencyCommandBundle.of(
        hostId, snapshot.consumerIdentity().effectiveShortUser(), hash('i'), List.of(command));

    Stage stage = stageFactory.createNew(requestId, "/tmp", consumerCluster.getClusterName(),
        consumerCluster.getClusterId(), "managed dependency publication", "{}", "{}");
    stage.setStageId(1L);
    stage.addHostRoleExecutionCommand(MANAGED_HOST, Role.HBASE_MASTER, RoleCommand.INSTALL,
        new ServiceComponentHostInstallEvent("HBASE_MASTER", MANAGED_HOST,
            System.currentTimeMillis(), consumerCluster.getDesiredStackVersion().getStackId()),
        consumerCluster.getClusterName(), "HBASE", false, false);
    HostRoleCommand task = stage.getOrderedHostRoleCommands().get(0);
    ExecutionCommand execution = task.getExecutionCommandWrapper().getExecutionCommand();
    execution.setCommandParams(Map.of(ManagedDependencyRuntimePlanner.BUNDLE_PARAMETER,
        StageUtils.getGson().toJson(bundle)));
    return new DispatchScenario(new Request(List.of(stage), "{}", clusters), hostId);
  }

  private ServiceDependencyBindingEntity dispatchBinding(UUID bindingId, UUID operationId,
      ManagedDependencySnapshot snapshot) {
    ServiceDependencyBindingEntity entity = binding(bindingId, operationId);
    entity.setProvisioningPhase("PROVIDER_PREPARED");
    entity.setProviderPreparationHash(hash('p'));
    entity.setProviderFingerprint(snapshot.providerFingerprint());
    entity.setNamespaceRoot(snapshot.namespace().rootUri());
    entity.setNamespaceWal(snapshot.namespace().walUri());
    return entity;
  }

  private ServiceDependencySnapshotEntity dispatchSnapshotEntity(
      ManagedDependencySnapshot snapshot) {
    ServiceDependencySnapshotEntity entity = new ServiceDependencySnapshotEntity();
    entity.setBindingId(snapshot.bindingId().toString());
    entity.setSnapshotVersion(snapshot.snapshotVersion());
    entity.setSchemaVersion(snapshot.schemaVersion());
    entity.setConsumerFingerprint(snapshot.consumerFingerprint());
    entity.setProviderFingerprint(snapshot.providerFingerprint());
    entity.setProviderDisplayName(providerCluster.getClusterName());
    entity.setConsumerServiceVersion("3.3.0");
    entity.setSnapshotFingerprint(snapshot.snapshotFingerprint());
    entity.setClientFeaturesHash(hash('d'));
    entity.setSecurityPolicyHash(hash('e'));
    entity.setSnapshotJson(StageUtils.getGson().toJson(snapshot));
    entity.setCreatedByUserId(1);
    entity.setCreateTimestamp(System.currentTimeMillis());
    return entity;
  }

  private ServiceDependencyHostResultEntity dispatchCommand(
      ManagedDependencyCommand command, long hostId) {
    ServiceDependencyHostResultEntity entity = new ServiceDependencyHostResultEntity();
    entity.setBindingId(command.envelope().bindingId().toString());
    entity.setSnapshotVersion(command.envelope().snapshotVersion());
    entity.setHostId(hostId);
    entity.setDependencyType(ManagedDependencyType.HDFS.name());
    entity.setCheckKind(command.name().name());
    entity.setOperationEpoch(command.envelope().epoch());
    entity.setOperationId(command.envelope().operationId().toString());
    entity.setComponentName("HBASE_MASTER");
    entity.setCommandRequestHash(command.envelope().immutableRequestHash());
    entity.setCommandJson(StageUtils.getGson().toJson(command));
    entity.setRequiredPackageHash(command.envelope().immutableRequestHash());
    entity.setState("INTENT");
    entity.setCheckTimestamp(System.currentTimeMillis());
    return entity;
  }

  private record DispatchScenario(Request request, long hostId) {
  }

  private void createVerificationScenario(UUID bindingId, UUID operationId,
      long firstHostId, long secondHostId) {
    ServiceDependencyBindingEntity binding = binding(bindingId, operationId);
    binding.setProvisioningPhase("CONSUMER_VERIFYING");
    binding.setProviderPreparationHash(hash('p'));
    ServiceDependencyOperationEntity operation = operation(bindingId, operationId);
    operation.setState("CONSUMER_VERIFYING");
    CreationGuard guard = new CreationGuard(null,
        List.of(new RepositoryGuard(repository.getId(), repository.getVersion(), true)),
        List.of(serviceGuard(consumerCluster, "HBASE"), serviceGuard(providerCluster, "HDFS")));
    dependencyDAO.create(binding, snapshot(bindingId), operation, guard);
    prepareHost(bindingId, operationId, firstHostId, 1L, 101L + firstHostId);
    prepareHost(bindingId, operationId, secondHostId, 1L, 101L + secondHostId);
  }

  private void prepareHost(UUID bindingId, UUID operationId, long hostId,
      long stageId, long taskId) {
    ServiceDependencyHostResultEntity preparation = command(
        bindingId, operationId, 1L, hostId,
        "PREPARE_HDFS_CONSUMER", "HBASE_REGIONSERVER");
    dependencyDAO.planCommand(preparation);
    dependencyDAO.associatePreparationTask(
        preparation, "HBASE_REGIONSERVER", taskId, stageId, taskId);
    dependencyDAO.completeCommand(id(preparation), taskId, stageId, taskId,
        successfulPreparation("preparation-" + hostId, hostId));
    ServiceDependencyHostResultEntity completedPreparation = dependencyDAO.findHostResult(
        bindingId.toString(), 1L, 1L, hostId, "HDFS", "PREPARE_HDFS_CONSUMER");

    ServiceDependencyHostResultEntity verification = command(
        bindingId, operationId, 1L, hostId,
        "VERIFY_HDFS_CONSUMER", "HBASE_REGIONSERVER");
    verification.setPreparationObservationId(completedPreparation.getPreparationObservationId());
    verification.setPreparationRequestHash(completedPreparation.getCommandRequestHash());
    verification.setPreparationObservationFingerprint(
        completedPreparation.getPreparationObservationFingerprint());
    dependencyDAO.planCommand(verification);
    dependencyDAO.claimCommandDispatch(id(verification));
    dependencyDAO.markCommandDispatched(id(verification), taskId + 1000, stageId, taskId + 1000);
  }

  private void completeVerification(UUID bindingId, long hostId,
      boolean success, Set<Long> requiredHosts) {
    completeVerification(bindingId, hostId, success ? "SUCCEEDED" : "FAILED",
        success ? null : "VERIFY_FAILED", !success, requiredHosts);
  }

  private void completeVerification(UUID bindingId, long hostId,
      String state, String failureCode, boolean retryable, Set<Long> requiredHosts) {
    ServiceDependencyHostResultEntity verification = dependencyDAO.findHostResult(
        bindingId.toString(), 1L, 1L, hostId, "HDFS", "VERIFY_HDFS_CONSUMER");
    long persistedTaskId = verification.getAmbariTaskId();
    boolean success = "SUCCEEDED".equals(state);
    CommandCompletion completion = new CommandCompletion(
        "{\"verification\":" + success + ",\"host\":" + hostId + "}",
        hash(success ? 's' : 'f'),
        state, failureCode, "VERIFY_HDFS_CONSUMER",
        success ? null : "Managed dependency verification failed.", retryable,
        success ? "PROVISIONING" : "FAILED",
        success ? "CONSUMER_VERIFYING" : "VERIFY_HDFS_CONSUMER",
        success ? "CONSUMER_VERIFYING" : state, null, null, null,
        verification.getPreparationObservationId(), verification.getPreparationRequestHash(),
        verification.getPreparationObservationFingerprint(), "hadoop-client", "1.rpm", "3.3.0",
        hash('k'), hash('r'), hash('i'), requiredHosts, null);
    dependencyDAO.completeCommand(id(verification), verification.getAmbariRequestId(),
        verification.getAmbariStageId(), persistedTaskId, completion);
  }

  private CommandCompletion successfulPreparation(String result, long hostId) {
    return new CommandCompletion(result, hash('a'), "SUCCEEDED",
        null, null, null, false, "PROVISIONING", "CONSUMER_VERIFYING",
        "CONSUMER_VERIFYING", null, null, null,
        "00000000-0000-4000-8000-" + String.format("%012d", hostId),
        "prepare-request", hash('o'),
        "hadoop-client", "1.rpm", "3.3.0", hash('k'), hash('r'), hash('i'), null, null);
  }

  private CommandCompletion completion(String result, String state, String failureCode,
      String phase, Set<Long> readinessHosts) {
    return new CommandCompletion(result, hash('z'), state,
        failureCode, phase, "Managed dependency preparation failed.", true,
        "FAILED", phase, "FAILED", null, null, null, null, null, null,
        null, null, null, null, null, null, readinessHosts, null);
  }

  private ServiceDependencyHostResultEntity command(UUID bindingId, UUID operationId,
      long snapshotVersion, long hostId, String kind, String component) {
    ServiceDependencyHostResultEntity entity = new ServiceDependencyHostResultEntity();
    entity.setBindingId(bindingId.toString());
    entity.setSnapshotVersion(snapshotVersion);
    entity.setHostId(hostId);
    entity.setDependencyType("HDFS");
    entity.setCheckKind(kind);
    entity.setOperationEpoch(1L);
    entity.setOperationId(operationId.toString());
    entity.setComponentName(component);
    entity.setCommandRequestHash(hash(kind.charAt(0)));
    entity.setCommandJson("{\"kind\":\"" + kind + "\",\"host\":" + hostId + "}");
    entity.setRequiredPackageHash(hash('q'));
    entity.setState("INTENT");
    entity.setCheckTimestamp(System.currentTimeMillis());
    return entity;
  }

  private ServiceDependencyHostResultEntityPK id(ServiceDependencyHostResultEntity entity) {
    return new ServiceDependencyHostResultEntityPK(entity.getBindingId(),
        entity.getSnapshotVersion(), entity.getOperationEpoch(), entity.getHostId(), entity.getDependencyType(),
        entity.getCheckKind());
  }

  private void withCanonicalReadLocks(Runnable operation) {
    Cluster first = consumerCluster.getClusterId() < providerCluster.getClusterId()
        ? consumerCluster : providerCluster;
    Cluster second = first == consumerCluster ? providerCluster : consumerCluster;
    first.executeUnderReadLock(() -> second.executeUnderReadLock(() -> {
      operation.run();
      return null;
    }));
  }

  private void await(CountDownLatch latch) {
    try {
      if (!latch.await(30, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for the test barrier");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for the test barrier", e);
    }
  }

  private CreationGuard batchGuard() {
    return batchGuard(null, new RepositoryGuard(repository.getId(), repository.getVersion(), true));
  }

  private CreationGuard staleRepositoryGuard() {
    return batchGuard(null, new RepositoryGuard(99991L, "stale-repository", true));
  }

  private CreationGuard batchGuard(DraftGuard draft, RepositoryGuard... repositories) {
    return new CreationGuard(draft, List.of(repositories), List.of(
        serviceGuard(consumerCluster, "HBASE"), serviceGuard(providerCluster, "HDFS"),
        serviceGuard(providerCluster, "ZOOKEEPER")));
  }

  private CreationItem creationItem(UUID bindingId, UUID operationId,
      ManagedDependencyType type, long hostId, CreationGuard guard) {
    return new CreationItem(binding(bindingId, operationId, type),
        snapshot(bindingId, type), operation(bindingId, operationId),
        providerIntent(bindingId, operationId, hostId, type), guard);
  }

  private ServiceDependencyHostResultEntity providerIntent(UUID bindingId, UUID operationId,
      long hostId, ManagedDependencyType type) {
    ServiceDependencyHostResultEntity entity = new ServiceDependencyHostResultEntity();
    entity.setBindingId(bindingId.toString());
    entity.setSnapshotVersion(1L);
    entity.setHostId(hostId);
    entity.setDependencyType(type.name());
    entity.setCheckKind("PREPARE_BINDING_JOURNAL");
    entity.setOperationEpoch(1L);
    entity.setOperationId(operationId.toString());
    entity.setComponentName(type.getProviderServiceName());
    entity.setCommandRequestHash(hash(type == ManagedDependencyType.HDFS ? 'h' : 'z'));
    entity.setCommandJson("{\"provider\":\"" + type.name() + "\",\"host\":" + hostId + "}");
    entity.setRequiredPackageHash(hash('q'));
    entity.setState("INTENT");
    entity.setCheckTimestamp(System.currentTimeMillis());
    return entity;
  }

  private void publishBinding(UUID bindingId, UUID operationId) {
    CreationGuard guard = new CreationGuard(null,
        List.of(new RepositoryGuard(repository.getId(), repository.getVersion(), true)),
        List.of(serviceGuard(consumerCluster, "HBASE"), serviceGuard(providerCluster, "HDFS")));
    dependencyDAO.create(binding(bindingId, operationId), snapshot(bindingId),
        operation(bindingId, operationId), guard);
  }

  private boolean hasCause(Throwable error, Class<? extends Throwable> expectedType) {
    Throwable current = error;
    while (current != null) {
      if (expectedType.isInstance(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private ServiceVersionGuard serviceGuard(Cluster cluster, String serviceName) {
    Set<String> componentNames;
    try {
      componentNames = new TreeSet<>(
          cluster.getService(serviceName).getServiceComponents().keySet());
    } catch (Exception e) {
      throw new AssertionError("The test service guard could not be resolved", e);
    }
    return new ServiceVersionGuard(cluster.getClusterId(), serviceName, repository.getId(),
        componentNames, Set.of(), Map.of());
  }

  private ServiceDependencyBindingEntity binding(UUID bindingId, UUID operationId) {
    return binding(bindingId, operationId, ManagedDependencyType.HDFS);
  }

  private ServiceDependencyBindingEntity binding(UUID bindingId, UUID operationId,
      ManagedDependencyType type) {
    ServiceDependencyBindingEntity entity = new ServiceDependencyBindingEntity();
    entity.setBindingId(bindingId.toString());
    entity.setConsumerClusterId(consumerCluster.getClusterId());
    entity.setConsumerServiceName("HBASE");
    entity.setProviderClusterId(providerCluster.getClusterId());
    entity.setProviderServiceName(type.getProviderServiceName());
    entity.setDependencyType(type.name());
    entity.setState("PROVISIONING");
    entity.setProvisioningPhase("PROVIDER_PREPARING");
    entity.setRowVersion(0L);
    entity.setOperationEpoch(1L);
    entity.setDesiredSnapshotVersion(1L);
    entity.setSnapshotApproval("APPROVED");
    entity.setProviderFingerprint(hash('b'));
    ManagedDependencyNamespace namespace = type == ManagedDependencyType.HDFS
        ? ManagedDependencyNamespace.hdfs(bindingId, "hdfs://provider")
        : ManagedDependencyNamespace.zooKeeper(bindingId);
    entity.setNamespaceRoot(namespace.rootUri().isEmpty() ? null : namespace.rootUri());
    entity.setNamespaceWal(namespace.walUri().isEmpty() ? null : namespace.walUri());
    entity.setNamespaceZnode(namespace.znode().isEmpty() ? null : namespace.znode());
    entity.setActiveOperationId(operationId.toString());
    entity.setFailureRetryable(false);
    entity.setCreatedByUserId(1);
    entity.setUpdatedByUserId(1);
    return entity;
  }

  private ServiceDependencySnapshotEntity snapshot(UUID bindingId) {
    return snapshot(bindingId, ManagedDependencyType.HDFS);
  }

  private ServiceDependencySnapshotEntity snapshot(UUID bindingId, ManagedDependencyType type) {
    ServiceDependencySnapshotEntity entity = new ServiceDependencySnapshotEntity();
    entity.setBindingId(bindingId.toString());
    entity.setSnapshotVersion(1L);
    entity.setSchemaVersion(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION);
    entity.setConsumerFingerprint(hash('a'));
    entity.setProviderFingerprint(hash('b'));
    entity.setProviderDisplayName("dependency-provider");
    entity.setConsumerServiceVersion("3.3.0");
    entity.setSnapshotFingerprint(hash('c'));
    entity.setClientFeaturesHash(hash('d'));
    entity.setSecurityPolicyHash(hash('e'));
    ManagedDependencyVersion version = new ManagedDependencyVersion(
        "HDP", "2.0.6", true, "2.0.6", Map.of(),
        Set.of("STANDARD_RPC_CLIENT"), repository.getId(), List.of());
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hbase_mc_cb", Set.of(), false, "hbase_mc_cb", true, "0700", false);
    ManagedDependencyNamespace namespace = type == ManagedDependencyType.HDFS
        ? ManagedDependencyNamespace.hdfs(bindingId, "hdfs://provider")
        : ManagedDependencyNamespace.zooKeeper(bindingId);
    ManagedDependencySnapshot value = new ManagedDependencySnapshot(
        ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION, bindingId, 1L,
        type,
        new ManagedDependencyServiceKey(providerCluster.getClusterId(), type.getProviderServiceName()),
        namespace,
        version.compatibility(), ManagedDependencySecurityMode.INSECURE, identity,
        new java.util.TreeMap<>(Map.of("fs.defaultFS", "hdfs://provider")),
        new java.util.TreeMap<>(), new java.util.TreeMap<>(), hash('a'), hash('b'), hash('c'),
        null, null, null);
    entity.setSnapshotJson(StageUtils.getGson().toJson(value));
    entity.setCreatedByUserId(1);
    entity.setCreateTimestamp(System.currentTimeMillis());
    return entity;
  }

  private ServiceDependencyOperationEntity operation(UUID bindingId, UUID operationId) {
    long now = System.currentTimeMillis();
    ServiceDependencyOperationEntity entity = new ServiceDependencyOperationEntity();
    entity.setOperationId(operationId.toString());
    entity.setBindingId(bindingId.toString());
    entity.setOperationKind("CREATE");
    entity.setOperationEpoch(1L);
    entity.setTargetSnapshotVersion(1L);
    entity.setRequestHash(hash('f'));
    entity.setState("QUEUED");
    entity.setCreateTimestamp(now);
    entity.setUpdateTimestamp(now);
    return entity;
  }

  private String hash(char digit) {
    return "sha256:" + String.valueOf(digit).repeat(64);
  }

  public static class ReplayMutation {
    private final EntityManager entityManager;

    @Inject
    ReplayMutation(EntityManager entityManager) {
      this.entityManager = entityManager;
    }

    @Transactional
    String update(String hdfsBindingId, String zooKeeperBindingId, String draftKey,
        long repositoryId) {
      ServiceDependencyBindingEntity hdfs = entityManager.find(
          ServiceDependencyBindingEntity.class, hdfsBindingId, LockModeType.PESSIMISTIC_WRITE);
      hdfs.setState("READY");
      hdfs.setProvisioningPhase("READY");
      hdfs.setDesiredSnapshotVersion(2L);
      hdfs.setProviderFingerprint("sha256:" + "9".repeat(64));
      hdfs.setAppliedSnapshotVersion(2L);

      ServiceDependencyBindingEntity zooKeeper = entityManager.find(
          ServiceDependencyBindingEntity.class, zooKeeperBindingId, LockModeType.PESSIMISTIC_WRITE);
      zooKeeper.setState("READY");
      zooKeeper.setProvisioningPhase("READY");
      zooKeeper.setDesiredSnapshotVersion(2L);
      zooKeeper.setProviderFingerprint("sha256:" + "9".repeat(64));
      zooKeeper.setAppliedSnapshotVersion(2L);

      ScopedWorkflowStateEntity draft = entityManager.find(
          ScopedWorkflowStateEntity.class, draftKey, LockModeType.PESSIMISTIC_WRITE);
      draft.setRevision(2L);
      RepositoryVersionEntity repository = entityManager.find(
          RepositoryVersionEntity.class, repositoryId, LockModeType.PESSIMISTIC_WRITE);
      repository.setVersion("2.0.6-replayed");
      entityManager.flush();
      return repository.getVersion();
    }
  }

  static class LivePlanReadBarrier {
    private final ServiceDependencyDAO dao;

    @Inject
    LivePlanReadBarrier(ServiceDependencyDAO dao) {
      this.dao = dao;
    }

    @Transactional
    void readAndHold(long clusterId,
        AtomicReference<Optional<List<ManagedDependencySnapshot>>> observed,
        CountDownLatch locked, CountDownLatch release) {
      observed.set(dao.findApprovedLivePlanSnapshots(clusterId));
      locked.countDown();
      try {
        if (!release.await(30, TimeUnit.SECONDS)) {
          throw new AssertionError("Timed out while holding the live-plan read transaction");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Interrupted while holding the live-plan read transaction", e);
      }
    }
  }

  public static class DeletionTransactionBarrier {
    @Transactional
    public void deleteAllServicesAndWait(Cluster cluster,
        CountDownLatch entered, CountDownLatch release) throws Exception {
      cluster.deleteAllServices();
      entered.countDown();
      if (!release.await(30, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out while holding the deletion transaction");
      }
    }
  }
}
