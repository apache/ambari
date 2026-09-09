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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ShortTaskStatus;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.RequestResourceProvider;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntityPK;
import org.apache.ambari.server.orm.entities.ServiceDependencyOperationEntity;
import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Dispatches only server-persisted managed dependency operations. Agent-side
 * effects are idempotent by the command envelope, so an INTENT whose action
 * response was lost can be dispatched again without changing its identity.
 */
@Singleton
public class ManagedDependencyOperationDispatcher {
  public static final String INTERNAL_AUTH_TOKEN = "managed-dependency-operation-dispatch";
  public static final String COMMAND_PARAMETER = "managed_dependency_command";

  private static final Logger LOG = LoggerFactory.getLogger(ManagedDependencyOperationDispatcher.class);

  private final ServiceDependencyDAO dependencyDAO;
  private final Clusters clusters;
  private final Provider<AmbariManagementController> managementController;
  private final ManagedDependencyRuntimePlanner runtimePlanner;
  private final Provider<ManagedServiceDependencyCoordinator> coordinator;
  private final Provider<ActionManager> actionManager;
  private final Provider<ManagedDependencyTaskResultProcessor> resultProcessor;

  @Inject
  public ManagedDependencyOperationDispatcher(ServiceDependencyDAO dependencyDAO, Clusters clusters,
      Provider<AmbariManagementController> managementController,
      ManagedDependencyRuntimePlanner runtimePlanner,
      Provider<ManagedServiceDependencyCoordinator> coordinator,
      Provider<ActionManager> actionManager,
      Provider<ManagedDependencyTaskResultProcessor> resultProcessor) {
    this.dependencyDAO = dependencyDAO;
    this.clusters = clusters;
    this.managementController = managementController;
    this.runtimePlanner = runtimePlanner;
    this.coordinator = coordinator;
    this.actionManager = actionManager;
    this.resultProcessor = resultProcessor;
  }

  /** Builds the first immutable command while the coordinator holds both cluster read locks. */
  public ServiceDependencyHostResultEntity initialProviderCommand(
      ServiceDependencyBindingEntity binding, ManagedDependencySnapshot snapshot,
      ServiceDependencyOperationEntity operation) {
    long hostId = selectProviderActionHost(binding);
    ManagedDependencyCommand command = ManagedDependencyCommand.prepareJournal(snapshot,
        java.util.UUID.fromString(operation.getOperationId()), operation.getOperationEpoch(), hostId);
    return commandEntity(command, snapshot.type(), hostId,
        providerComponent(binding.getDependencyType()));
  }

  /** Starts the already-committed first intent; failures remain durable and replayable. */
  public void dispatchInitial(String bindingId) {
    ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(bindingId);
    if (binding == null || binding.getActionHostId() == null) {
      return;
    }
    ServiceDependencyHostResultEntity command = dependencyDAO.findHostResult(bindingId,
        binding.getDesiredSnapshotVersion(), binding.getOperationEpoch(), binding.getActionHostId(),
        binding.getDependencyType(),
        ManagedDependencyCommand.CommandName.PREPARE_BINDING_JOURNAL.name());
    if (command != null) {
      dispatchSafely(command);
    }
  }

  /** Called from server recovery after ORM initialization. */
  public void recoverOutstanding() {
    try {
      Set<Long> recoveredTaskIds = new HashSet<>();
      for (ServiceDependencyHostResultEntity command : dependencyDAO.findOutstandingCommands()) {
        try {
          if (("INTENT".equals(command.getState()) || "SCHEDULING".equals(command.getState()))
              && (isProviderCommand(command.getCheckKind())
                  || isPreparationCommand(command.getCheckKind())
                  || isVerificationCommand(command.getCheckKind()))) {
            dispatchSafely(command);
          } else if ("DISPATCHED".equals(command.getState())
              && command.getAmbariTaskId() != null
              && recoveredTaskIds.add(command.getAmbariTaskId())) {
            HostRoleCommand task = actionManager.get().getTaskById(command.getAmbariTaskId());
            if (task != null && task.getStatus().isCompletedState()) {
              resultProcessor.get().recover(task);
            }
          }
        } catch (RuntimeException e) {
          LOG.warn("Managed dependency recovery item is deferred: binding={}, step={}, reason={}",
              command.getBindingId(), command.getCheckKind(), e.getClass().getSimpleName());
        }
      }
    } catch (RuntimeException e) {
      LOG.warn("Managed dependency recovery scan is deferred: reason={}",
          e.getClass().getSimpleName());
    }
  }

  public void dispatchSafely(ServiceDependencyHostResultEntity command) {
    try {
      dispatch(command);
    } catch (RuntimeException | AmbariException e) {
      LOG.warn("Managed dependency command remains pending after dispatch failure: binding={}, step={}, type={}",
          command.getBindingId(), command.getCheckKind(), e.getClass().getSimpleName());
    }
  }

  private void dispatch(ServiceDependencyHostResultEntity command) throws AmbariException {
    ServiceDependencyBindingEntity parent = dependencyDAO.findBinding(command.getBindingId());
    if (parent == null) {
      throw new IllegalStateException("Persisted managed dependency parent is missing");
    }
    Cluster consumer = clusters.getClusterById(parent.getConsumerClusterId());
    Cluster provider = clusters.getClusterById(parent.getProviderClusterId());
    if (isPreparationCommand(command.getCheckKind())) {
      List<Cluster> parents = preparationParents(parent, consumer, provider);
      withClusterWriteLocks(parents, 0, () -> {
        assertPreparationParentsCurrent(parent.getConsumerClusterId(), parents);
        dispatchLocked(command);
        return null;
      });
      return;
    }
    withClusterReadLocks(consumer, provider, () -> {
      dispatchLocked(command);
      return null;
    });
  }

  private List<Cluster> preparationParents(ServiceDependencyBindingEntity commandBinding,
      Cluster consumer, Cluster commandProvider) throws AmbariException {
    Map<Long, Cluster> parents = new TreeMap<>();
    parents.put(consumer.getClusterId(), consumer);
    parents.put(commandProvider.getClusterId(), commandProvider);
    for (ServiceDependencyBindingEntity binding : dependencyDAO.findByConsumer(
        commandBinding.getConsumerClusterId(), "HBASE")) {
      Cluster provider = clusters.getClusterById(binding.getProviderClusterId());
      if (provider == null) {
        throw new AmbariException("The managed dependency provider cluster is unavailable");
      }
      parents.put(provider.getClusterId(), provider);
    }
    return new ArrayList<>(parents.values());
  }

  private void assertPreparationParentsCurrent(long consumerClusterId,
      List<Cluster> lockedParents) {
    Set<Long> lockedIds = lockedParents.stream().map(Cluster::getClusterId)
        .collect(java.util.stream.Collectors.toSet());
    for (ServiceDependencyBindingEntity binding : dependencyDAO.findByConsumer(
        consumerClusterId, "HBASE")) {
      if (!lockedIds.contains(binding.getProviderClusterId())) {
        throw new ManagedDependencyIntegrationException(409,
            "DEPENDENCY_PARENT_SET_CHANGED",
            "Managed dependency parents changed while preparation was being published.");
      }
    }
  }

  private void dispatchLocked(ServiceDependencyHostResultEntity supplied) throws AmbariException {
    ServiceDependencyHostResultEntity command = dependencyDAO.findHostResult(
        supplied.getBindingId(), supplied.getSnapshotVersion(), supplied.getOperationEpoch(),
        supplied.getHostId(),
        supplied.getDependencyType(), supplied.getCheckKind());
    if (command == null) {
      throw new IllegalStateException("Persisted managed dependency command is missing");
    }
    ManagedDependencyCommand payload = StageUtils.getGson().fromJson(
        command.getCommandJson(), ManagedDependencyCommand.class);
    if (!payload.envelope().immutableRequestHash().equals(command.getCommandRequestHash())
        || !payload.name().name().equals(command.getCheckKind())) {
      throw new IllegalStateException("Persisted managed dependency command identity is corrupt");
    }
    ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(command.getBindingId());
    if (binding == null || !binding.getProviderClusterId().equals(
        Long.parseLong(payload.parameters().get("provider.cluster.id")))) {
      throw new IllegalStateException("Persisted managed dependency provider identity is corrupt");
    }
    if (!Objects.equals(binding.getActiveOperationId(), command.getOperationId())
        || !Objects.equals(binding.getOperationEpoch(), command.getOperationEpoch())
        || !Objects.equals(binding.getDesiredSnapshotVersion(), command.getSnapshotVersion())) {
      throw new IllegalStateException("Persisted managed dependency command is stale");
    }
    boolean providerCommand = isProviderCommand(command.getCheckKind());
    boolean preparationCommand = isPreparationCommand(command.getCheckKind());
    if (!providerCommand && !preparationCommand && !isVerificationCommand(command.getCheckKind())) {
      throw new IllegalStateException(
          "Only provider effects, preparation and strict verification are dispatchable");
    }
    validatePhase(binding, command.getCheckKind());
    if (ManagedDependencyCommand.CommandName.INVALIDATE_BINDING_EPOCH.name()
        .equals(command.getCheckKind())) {
      coordinator.get().validateDetachmentState(binding);
    } else {
      coordinator.get().validateDispatchState(binding);
    }
    Cluster cluster = clusters.getClusterById(providerCommand
        ? binding.getProviderClusterId() : binding.getConsumerClusterId());
    Host host = clusters.getHostById(command.getHostId());
    String serviceName = providerCommand ? binding.getProviderServiceName() : "HBASE";
    String component = providerCommand
        ? providerComponent(binding.getDependencyType()) : command.getComponentName();
    if (component == null || !cluster.getService(serviceName).getServiceComponent(component)
        .getServiceComponentHosts().containsKey(host.getHostName())) {
      throw new IllegalStateException("Persisted managed dependency target is no longer current");
    }
    if (!dependencyDAO.claimCommandDispatch(id(command))) {
      return;
    }
    Map<String, String> commandParameters;
    if (providerCommand) {
      commandParameters = Map.of(COMMAND_PARAMETER, command.getCommandJson());
    } else {
      String rawBundle;
      if (preparationCommand) {
        rawBundle = StageUtils.getGson().toJson(runtimePlanner.buildRetryPreparationPlan(
            binding.getConsumerClusterId(), command.getHostId(),
            UUID.fromString(command.getBindingId())).bundle());
      } else {
        rawBundle = runtimePlanner.persistedPreparationBundle(
            binding.getConsumerClusterId(), command.getHostId());
      }
      commandParameters = Map.of(COMMAND_PARAMETER, command.getCommandJson(),
          ManagedDependencyRuntimePlanner.BUNDLE_PARAMETER, rawBundle);
    }
    ExecuteActionRequest request = new ExecuteActionRequest(cluster.getClusterName(),
        payload.name().name(), null,
        List.of(new RequestResourceFilter(serviceName, component,
            List.of(host.getHostName()))), null,
        commandParameters, true);

    Authentication saved = SecurityContextHolder.getContext().getAuthentication();
    try {
      InternalAuthenticationToken internal = new InternalAuthenticationToken(INTERNAL_AUTH_TOKEN);
      internal.setAuthenticated(true);
      SecurityContextHolder.getContext().setAuthentication(internal);
      RequestStatusResponse response = managementController.get().createAction(request,
          Map.of(RequestResourceProvider.CONTEXT,
              "Managed dependency " + binding.getDependencyType() + " " + command.getCheckKind()));
      ShortTaskStatus task = exactTask(response, host.getHostName(), component);
      dependencyDAO.markCommandDispatched(id(command), response.getRequestId(),
          task.getStageId(), task.getTaskId());
    } finally {
      SecurityContextHolder.getContext().setAuthentication(saved);
    }
  }

  private void validatePhase(ServiceDependencyBindingEntity binding, String commandKind) {
    Set<String> phases = switch (ManagedDependencyCommand.CommandName.valueOf(commandKind)) {
      case PREPARE_BINDING_JOURNAL -> Set.of("PROVIDER_PREPARING");
      case INITIALIZE_BINDING_JOURNAL -> Set.of("PROVIDER_INITIALIZING");
      case PROVISION_HDFS_NAMESPACE, PROVISION_ZOOKEEPER_NAMESPACE ->
          Set.of("PROVIDER_PROVISIONING");
      case PREPARE_HDFS_CONSUMER, PREPARE_ZOOKEEPER_CONSUMER ->
          Set.of("PROVIDER_PREPARED", "CONSUMER_VERIFYING");
      case VERIFY_HDFS_CONSUMER, VERIFY_ZOOKEEPER_CONSUMER ->
          Set.of("CONSUMER_CREDENTIALS_REQUIRED", "CONSUMER_VERIFYING", "READY");
      case INVALIDATE_BINDING_EPOCH -> Set.of("DETACHING");
      default -> Set.of();
    };
    if (!phases.contains(binding.getProvisioningPhase())) {
      throw new IllegalStateException("The managed dependency command is not current for its phase");
    }
  }

  private <T> T withClusterReadLocks(Cluster firstCandidate, Cluster secondCandidate,
      CheckedSupplier<T> operation) throws AmbariException {
    try {
      Supplier<T> unchecked = () -> {
        try {
          return operation.get();
        } catch (AmbariException e) {
          throw new DispatchException(e);
        }
      };
      if (firstCandidate.getClusterId() == secondCandidate.getClusterId()) {
        return firstCandidate.executeUnderReadLock(unchecked);
      }
      Cluster first = firstCandidate.getClusterId() < secondCandidate.getClusterId()
          ? firstCandidate : secondCandidate;
      Cluster second = first == firstCandidate ? secondCandidate : firstCandidate;
      return first.executeUnderReadLock(() -> second.executeUnderReadLock(unchecked));
    } catch (DispatchException e) {
      throw e.getCause();
    }
  }

  private <T> T withClusterWriteLocks(List<Cluster> ordered, int index,
      CheckedSupplier<T> operation) throws AmbariException {
    if (index == ordered.size()) {
      return operation.get();
    }
    try {
      return ordered.get(index).executeUnderWriteLockUntilTransactionCompletion(() -> {
        try {
          return withClusterWriteLocks(ordered, index + 1, operation);
        } catch (AmbariException e) {
          throw new DispatchException(e);
        }
      });
    } catch (DispatchException e) {
      throw e.getCause();
    }
  }

  @FunctionalInterface
  private interface CheckedSupplier<T> {
    T get() throws AmbariException;
  }

  private static final class DispatchException extends RuntimeException {
    private final AmbariException cause;

    private DispatchException(AmbariException cause) {
      super(cause);
      this.cause = cause;
    }

    @Override
    public AmbariException getCause() {
      return cause;
    }
  }

  private long selectProviderActionHost(ServiceDependencyBindingEntity binding) {
    try {
      Cluster cluster = clusters.getClusterById(binding.getProviderClusterId());
      Service service = cluster.getService(binding.getProviderServiceName());
      ServiceComponent component = service.getServiceComponent(providerComponent(binding.getDependencyType()));
      return component.getServiceComponentHosts().values().stream()
          .filter(host -> host.getState() == State.STARTED)
          .sorted(java.util.Comparator.comparing(ServiceComponentHost::getHostName))
          .map(ServiceComponentHost::getHost)
          .mapToLong(Host::getHostId)
          .findFirst()
          .orElseThrow(() -> new IllegalStateException(
              "No started provider action host is available for the managed dependency"));
    } catch (AmbariException e) {
      throw new IllegalStateException("The managed dependency provider host is unavailable", e);
    }
  }

  private ShortTaskStatus exactTask(RequestStatusResponse response, String hostName, String component) {
    if (response == null || response.getTasks() == null) {
      throw new IllegalStateException("Managed dependency dispatch did not create an Ambari task");
    }
    List<ShortTaskStatus> tasks = response.getTasks().stream()
        .filter(task -> component.equals(task.getRole()))
        .toList();
    if (tasks.size() != 1) {
      throw new IllegalStateException("Managed dependency dispatch must create one exact task");
    }
    ShortTaskStatus task = tasks.get(0);
    if (!hostName.equals(task.getHostName())) {
      throw new IllegalStateException("Managed dependency dispatch task has the wrong host identity");
    }
    return task;
  }

  static ServiceDependencyHostResultEntity commandEntity(ManagedDependencyCommand command,
      ManagedDependencyType type, long hostId) {
    return commandEntity(command, type, hostId, null);
  }

  static ServiceDependencyHostResultEntity commandEntity(ManagedDependencyCommand command,
      ManagedDependencyType type, long hostId, String componentName) {
    ServiceDependencyHostResultEntity entity = new ServiceDependencyHostResultEntity();
    entity.setBindingId(command.envelope().bindingId().toString());
    entity.setSnapshotVersion(command.envelope().snapshotVersion());
    entity.setHostId(hostId);
    entity.setDependencyType(type.name());
    entity.setCheckKind(command.name().name());
    entity.setOperationEpoch(command.envelope().epoch());
    entity.setOperationId(command.envelope().operationId().toString());
    entity.setComponentName(componentName);
    entity.setCommandRequestHash(command.envelope().immutableRequestHash());
    entity.setCommandJson(StageUtils.getGson().toJson(command));
    entity.setRequiredPackageHash(command.envelope().immutableRequestHash());
    entity.setState("INTENT");
    entity.setCheckTimestamp(System.currentTimeMillis());
    return entity;
  }

  static ServiceDependencyHostResultEntityPK id(ServiceDependencyHostResultEntity command) {
    return new ServiceDependencyHostResultEntityPK(command.getBindingId(), command.getSnapshotVersion(),
        command.getOperationEpoch(), command.getHostId(), command.getDependencyType(),
        command.getCheckKind());
  }

  public static boolean isProviderCommand(String commandName) {
    try {
      return switch (ManagedDependencyCommand.CommandName.valueOf(commandName)) {
        case PREPARE_BINDING_JOURNAL, INITIALIZE_BINDING_JOURNAL,
            PROVISION_HDFS_NAMESPACE, PROVISION_ZOOKEEPER_NAMESPACE,
            INVALIDATE_BINDING_EPOCH -> true;
        default -> false;
      };
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static boolean isVerificationCommand(String commandName) {
    try {
      return switch (ManagedDependencyCommand.CommandName.valueOf(commandName)) {
        case VERIFY_HDFS_CONSUMER, VERIFY_ZOOKEEPER_CONSUMER -> true;
        default -> false;
      };
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static boolean isPreparationCommand(String commandName) {
    try {
      return switch (ManagedDependencyCommand.CommandName.valueOf(commandName)) {
        case PREPARE_HDFS_CONSUMER, PREPARE_ZOOKEEPER_CONSUMER -> true;
        default -> false;
      };
    } catch (IllegalArgumentException | NullPointerException e) {
      return false;
    }
  }

  public static boolean isReservedCommand(String commandName) {
    try {
      ManagedDependencyCommand.CommandName.valueOf(commandName);
      return true;
    } catch (IllegalArgumentException | NullPointerException e) {
      return false;
    }
  }

  public static boolean isInternalDispatch() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication instanceof InternalAuthenticationToken
        && authentication.isAuthenticated()
        && INTERNAL_AUTH_TOKEN.equals(authentication.getCredentials());
  }

  private static String providerComponent(String dependencyType) {
    return ManagedDependencyType.HDFS.name().equals(dependencyType)
        ? "NAMENODE" : "ZOOKEEPER_SERVER";
  }
}
