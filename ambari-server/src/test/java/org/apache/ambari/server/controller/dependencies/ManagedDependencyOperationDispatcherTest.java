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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.StaleApprovalException;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.junit.jupiter.api.Test;

import com.google.inject.Provider;

class ManagedDependencyOperationDispatcherTest {

  @Test
  void oneFailedRecoveredCallbackDoesNotBlockAnotherBinding() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    ActionManager actionManager = mock(ActionManager.class);
    ManagedDependencyTaskResultProcessor processor =
        mock(ManagedDependencyTaskResultProcessor.class);
    HostRoleCommand firstTask = completedTask();
    HostRoleCommand secondTask = completedTask();
    ServiceDependencyHostResultEntity first = dispatched("binding-a", 101L);
    ServiceDependencyHostResultEntity second = dispatched("binding-b", 102L);
    when(dao.findOutstandingCommands(0, 256)).thenReturn(List.of(first, second));
    when(actionManager.getTaskById(101L)).thenReturn(firstTask);
    when(actionManager.getTaskById(102L)).thenReturn(secondTask);
    doThrow(new IllegalStateException("first callback is temporarily unavailable"))
        .when(processor).recover(firstTask);

    ManagedDependencyOperationDispatcher dispatcher = new ManagedDependencyOperationDispatcher(
        dao, mock(Clusters.class), provider(mock(AmbariManagementController.class)),
        mock(ManagedDependencyRuntimePlanner.class),
        provider(mock(ManagedServiceDependencyCoordinator.class)),
        provider(actionManager), provider(processor));

    com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule() {
      @Override protected void configure() {
        bind(ManagedDependencyCredentialManager.class).toInstance(mock(ManagedDependencyCredentialManager.class));
        bind(ManagedDependencyDeploymentCoordinator.class).toInstance(mock(ManagedDependencyDeploymentCoordinator.class));
        bind(org.apache.ambari.server.events.publishers.TaskEventPublisher.class)
            .toInstance(mock(org.apache.ambari.server.events.publishers.TaskEventPublisher.class));
      }
    }).injectMembers(dispatcher);
    dispatcher.recoverOutstanding();

    verify(processor).recover(firstTask);
    verify(processor).recover(secondTask);
  }

  @Test
  void restartRecoversOneTerminalInstallBundleOnceForAllDependencyRows() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    ActionManager actionManager = mock(ActionManager.class);
    ManagedDependencyTaskResultProcessor processor =
        mock(ManagedDependencyTaskResultProcessor.class);
    HostRoleCommand installTask = completedTask();
    ServiceDependencyHostResultEntity hdfs = dispatched(
        "binding-hdfs", 201L, "PREPARE_HDFS_CONSUMER");
    ServiceDependencyHostResultEntity zooKeeper = dispatched(
        "binding-zookeeper", 201L, "PREPARE_ZOOKEEPER_CONSUMER");
    when(dao.findOutstandingCommands(0, 256)).thenReturn(List.of(hdfs, zooKeeper));
    when(actionManager.getTaskById(201L)).thenReturn(installTask);
    ManagedDependencyOperationDispatcher dispatcher = new ManagedDependencyOperationDispatcher(
        dao, mock(Clusters.class), provider(mock(AmbariManagementController.class)),
        mock(ManagedDependencyRuntimePlanner.class),
        provider(mock(ManagedServiceDependencyCoordinator.class)),
        provider(actionManager), provider(processor));

    com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule() {
      @Override protected void configure() {
        bind(ManagedDependencyCredentialManager.class).toInstance(mock(ManagedDependencyCredentialManager.class));
        bind(ManagedDependencyDeploymentCoordinator.class).toInstance(mock(ManagedDependencyDeploymentCoordinator.class));
        bind(org.apache.ambari.server.events.publishers.TaskEventPublisher.class)
            .toInstance(mock(org.apache.ambari.server.events.publishers.TaskEventPublisher.class));
      }
    }).injectMembers(dispatcher);
    dispatcher.recoverOutstanding();

    verify(processor, times(1)).recover(installTask);
  }

  @Test
  void staleSnapshotIsRejectedBeforeAnAmbariActionIsCreated() throws Exception {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    Clusters clusters = mock(Clusters.class);
    Cluster consumer = cluster(11L);
    Cluster providerCluster = cluster(22L);
    when(clusters.getClusterById(11L)).thenReturn(consumer);
    when(clusters.getClusterById(22L)).thenReturn(providerCluster);
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000071");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000072");
    ManagedDependencySnapshot snapshot = snapshot(bindingId);
    ManagedDependencyCommand payload = ManagedDependencyCommand.prepareJournal(
        snapshot, operationId, 1L, 41L);
    ServiceDependencyHostResultEntity command =
        ManagedDependencyOperationDispatcher.commandEntity(
            payload, ManagedDependencyType.HDFS, 41L);
    ServiceDependencyBindingEntity binding = new ServiceDependencyBindingEntity();
    binding.setBindingId(bindingId.toString());
    binding.setConsumerClusterId(11L);
    binding.setConsumerServiceName("HBASE");
    binding.setProviderClusterId(22L);
    binding.setProviderServiceName("HDFS");
    binding.setDependencyType("HDFS");
    binding.setState("PROVISIONING");
    binding.setProvisioningPhase("PROVIDER_PREPARING");
    binding.setOperationEpoch(1L);
    binding.setDesiredSnapshotVersion(1L);
    binding.setActiveOperationId(operationId.toString());
    when(dao.findBinding(bindingId.toString())).thenReturn(binding);
    when(dao.findHostResult(bindingId.toString(), 1L, 1L, 41L, "HDFS",
        "PREPARE_BINDING_JOURNAL")).thenReturn(command);
    ManagedServiceDependencyCoordinator coordinator =
        mock(ManagedServiceDependencyCoordinator.class);
    doThrow(new StaleApprovalException("provider changed"))
        .when(coordinator).validateDispatchState(binding);
    AmbariManagementController managementController = mock(AmbariManagementController.class);
    ManagedDependencyOperationDispatcher dispatcher = new ManagedDependencyOperationDispatcher(
        dao, clusters, provider(managementController),
        mock(ManagedDependencyRuntimePlanner.class), provider(coordinator),
        provider(mock(ActionManager.class)),
        provider(mock(ManagedDependencyTaskResultProcessor.class)));

    dispatcher.dispatchSafely(command);

    verify(dao, never()).claimCommandDispatch(any());
    verify(managementController, never()).createAction(any(), any());
  }

  @Test
  void dispatcherReadPublicationDoesNotDeadlockWithProviderStopWritePublication()
      throws Exception {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    Clusters clusters = mock(Clusters.class);
    ReentrantReadWriteLock consumerLock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock providerLock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock hrcLock = new ReentrantReadWriteLock();
    Cluster consumer = lockedCluster(11L, consumerLock);
    Cluster provider = lockedCluster(22L, providerLock);
    when(clusters.getClusterById(11L)).thenReturn(consumer);
    when(clusters.getClusterById(22L)).thenReturn(provider);

    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000081");
    UUID operationId = UUID.fromString("00000000-0000-4000-8000-000000000082");
    ManagedDependencyCommand payload = ManagedDependencyCommand.prepareJournal(
        snapshot(bindingId), operationId, 1L, 41L);
    ServiceDependencyHostResultEntity command =
        ManagedDependencyOperationDispatcher.commandEntity(
            payload, ManagedDependencyType.HDFS, 41L, "NAMENODE");
    ServiceDependencyBindingEntity binding = new ServiceDependencyBindingEntity();
    binding.setBindingId(bindingId.toString());
    binding.setConsumerClusterId(11L);
    binding.setConsumerServiceName("HBASE");
    binding.setProviderClusterId(22L);
    binding.setProviderServiceName("HDFS");
    binding.setDependencyType("HDFS");
    binding.setState("PROVISIONING");
    binding.setProvisioningPhase("PROVIDER_PREPARING");
    binding.setOperationEpoch(1L);
    binding.setDesiredSnapshotVersion(1L);
    binding.setActiveOperationId(operationId.toString());
    when(dao.findBinding(bindingId.toString())).thenReturn(binding);
    when(dao.findHostResult(bindingId.toString(), 1L, 1L, 41L, "HDFS",
        "PREPARE_BINDING_JOURNAL")).thenReturn(command);
    when(dao.claimCommandDispatch(any())).thenReturn(true);

    Host host = mock(Host.class);
    when(host.getHostName()).thenReturn("provider-1");
    when(clusters.getHostById(41L)).thenReturn(host);
    ServiceComponent providerComponent = mock(ServiceComponent.class);
    when(providerComponent.getServiceComponentHosts()).thenReturn(
        Map.of("provider-1", mock(ServiceComponentHost.class)));
    Service providerService = mock(Service.class);
    when(providerService.getServiceComponent("NAMENODE")).thenReturn(providerComponent);
    when(provider.getService("HDFS")).thenReturn(providerService);

    CountDownLatch dispatcherAtPublication = new CountDownLatch(1);
    AmbariManagementController managementController = mock(AmbariManagementController.class);
    when(managementController.createAction(any(), any())).thenAnswer(invocation -> {
      dispatcherAtPublication.countDown();
      hrcLock.writeLock().lock();
      hrcLock.writeLock().unlock();
      return null;
    });
    ManagedServiceDependencyCoordinator coordinator =
        mock(ManagedServiceDependencyCoordinator.class);
    ManagedDependencyOperationDispatcher dispatcher = new ManagedDependencyOperationDispatcher(
        dao, clusters, provider(managementController),
        mock(ManagedDependencyRuntimePlanner.class), provider(coordinator),
        provider(mock(ActionManager.class)),
        provider(mock(ManagedDependencyTaskResultProcessor.class)));

    ManagedDependencyDescriptorResolver resolver = mock(ManagedDependencyDescriptorResolver.class);
    when(resolver.cluster(22L)).thenReturn(provider);
    ManagedDependencyLifecyclePolicy policy = mock(ManagedDependencyLifecyclePolicy.class);
    ManagedDependencyRuntimePlanner stopPlanner = new ManagedDependencyRuntimePlanner(
        dao, resolver, coordinator, policy);
    Request stopRequest = providerStopRequest();
    AtomicBoolean providerPublished = new AtomicBoolean();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> dispatcherFuture = executor.submit(() -> dispatcher.dispatchSafely(command));
      if (!dispatcherAtPublication.await(30, TimeUnit.SECONDS)) {
        throw new AssertionError("dispatcher did not reach action publication");
      }
      Future<?> providerStopFuture = executor.submit(() -> {
        stopPlanner.executeWithPreparationParentLocks(stopRequest, () -> {
          hrcLock.writeLock().lock();
          try {
            providerPublished.set(true);
          } finally {
            hrcLock.writeLock().unlock();
          }
        });
        return null;
      });

      dispatcherFuture.get(30, TimeUnit.SECONDS);
      providerStopFuture.get(30, TimeUnit.SECONDS);
      assertTrue(providerPublished.get());
    } finally {
      executor.shutdownNow();
    }
  }

  private HostRoleCommand completedTask() {
    HostRoleCommand task = mock(HostRoleCommand.class);
    when(task.getStatus()).thenReturn(HostRoleStatus.COMPLETED);
    return task;
  }

  private ServiceDependencyHostResultEntity dispatched(String bindingId, long taskId) {
    return dispatched(bindingId, taskId, "VERIFY_HDFS_CONSUMER");
  }

  private ServiceDependencyHostResultEntity dispatched(
      String bindingId, long taskId, String checkKind) {
    ServiceDependencyHostResultEntity result = new ServiceDependencyHostResultEntity();
    result.setBindingId(bindingId);
    result.setCheckKind(checkKind);
    result.setState("DISPATCHED");
    result.setAmbariTaskId(taskId);
    return result;
  }

  private <T> Provider<T> provider(T value) {
    return () -> value;
  }

  private Request providerStopRequest() {
    ExecutionCommand execution = mock(ExecutionCommand.class);
    when(execution.getClusterId()).thenReturn("22");
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
    when(request.getClusterId()).thenReturn(22L);
    when(request.getStages()).thenReturn(List.of(stage));
    return request;
  }

  @SuppressWarnings("unchecked")
  private Cluster lockedCluster(long clusterId, ReentrantReadWriteLock lock) {
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterId()).thenReturn(clusterId);
    when(cluster.executeUnderReadLock(any())).thenAnswer(invocation -> {
      lock.readLock().lock();
      try {
        return ((Supplier<Object>) invocation.getArgument(0)).get();
      } finally {
        lock.readLock().unlock();
      }
    });
    when(cluster.executeUnderWriteLockUntilTransactionCompletion(any())).thenAnswer(invocation -> {
      lock.writeLock().lock();
      try {
        return ((Supplier<Object>) invocation.getArgument(0)).get();
      } finally {
        lock.writeLock().unlock();
      }
    });
    return cluster;
  }

  @SuppressWarnings("unchecked")
  private Cluster cluster(long clusterId) {
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterId()).thenReturn(clusterId);
    when(cluster.executeUnderReadLock(any())).thenAnswer(
        invocation -> ((Supplier<Object>) invocation.getArgument(0)).get());
    return cluster;
  }

  private ManagedDependencySnapshot snapshot(UUID bindingId) {
    String hash = "sha256:" + "a".repeat(64);
    ManagedDependencyVersion version = new ManagedDependencyVersion(
        "BIGTOP", "3.3.0", true, "3.3.0",
        new TreeMap<>(Map.of("distribution", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 31L, List.of());
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hbase_mc_cb", new TreeSet<>(), false, "hbase_mc_cb", true, "0700", false);
    return new ManagedDependencySnapshot(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION,
        bindingId, 1L, ManagedDependencyType.HDFS,
        new ManagedDependencyServiceKey(22L, "HDFS"),
        ManagedDependencyNamespace.hdfs(bindingId, "hdfs://provider"),
        version.compatibility(), ManagedDependencySecurityMode.INSECURE, identity,
        new TreeMap<>(Map.of("fs.defaultFS", "hdfs://provider")), new TreeMap<>(),
        new TreeMap<>(), hash, hash, hash);
  }
}
