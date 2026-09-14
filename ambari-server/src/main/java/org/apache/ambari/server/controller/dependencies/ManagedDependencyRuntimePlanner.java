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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.orm.AmbariJpaLocalTxnInterceptor;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyOperationEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.utils.StageUtils;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** Plans the exact managed client profile transported with HBase host commands. */
@Singleton
public class ManagedDependencyRuntimePlanner {
  public static final String BUNDLE_PARAMETER = "managed_dependency_commands";

  private final ServiceDependencyDAO dependencyDAO;
  private final ManagedDependencyDescriptorResolver descriptorResolver;
  private final ManagedServiceDependencyCoordinator coordinator;
  private final ManagedDependencyLifecyclePolicy lifecyclePolicy;

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @Inject
  private ManagedDependencyBlueprintPlan blueprintPlan;

  @Inject
  public ManagedDependencyRuntimePlanner(ServiceDependencyDAO dependencyDAO,
      ManagedDependencyDescriptorResolver descriptorResolver,
      ManagedServiceDependencyCoordinator coordinator,
      ManagedDependencyLifecyclePolicy lifecyclePolicy) {
    this.dependencyDAO = dependencyDAO;
    this.descriptorResolver = descriptorResolver;
    this.coordinator = coordinator;
    this.lifecyclePolicy = lifecyclePolicy;
  }

  /** Compatibility constructor for focused unit tests and non-Guice callers. */
  public ManagedDependencyRuntimePlanner(ServiceDependencyDAO dependencyDAO,
      ManagedDependencyDescriptorResolver descriptorResolver,
      ManagedServiceDependencyCoordinator coordinator) {
    this(dependencyDAO, descriptorResolver, coordinator,
        new ManagedDependencyLifecyclePolicy(dependencyDAO));
  }

  public void augmentHostCommand(Cluster cluster, ServiceComponentHost host,
      RoleCommand roleCommand, Map<String, String> commandParameters,
      Map<String, Map<String, String>> commandConfigurations,
      Set<String> configurationTypeOverrides,
      String repositoryVersion) throws AmbariException {
    if (!"HBASE".equals(host.getServiceName())
        || roleCommand != RoleCommand.INSTALL && roleCommand != RoleCommand.START
            && roleCommand != RoleCommand.RESTART) {
      return;
    }
    if (blueprintPlan != null) blueprintPlan.requireBindings(cluster.getClusterId());
    List<ServiceDependencyBindingEntity> bindings = new ArrayList<>(
        dependencyDAO.findByConsumer(cluster.getClusterId(), "HBASE"));
    if (bindings.isEmpty()) {
      if (roleCommand == RoleCommand.START || roleCommand == RoleCommand.RESTART) {
        withClusterReadLocks(List.of(cluster), 0, () -> {
          coordinator.validateConsumerStart(cluster, host.getHost().getHostId());
          return null;
        });
      }
      return;
    }
    bindings.sort(Comparator.comparing(ServiceDependencyBindingEntity::getDependencyType));
    List<Cluster> parents = new ArrayList<>();
    parents.add(cluster);
    bindings.stream().map(ServiceDependencyBindingEntity::getProviderClusterId)
        .distinct().map(descriptorResolver::cluster).forEach(parents::add);
    withClusterReadLocks(parents, 0, () -> {
      augmentHostCommandLocked(cluster, host, roleCommand, commandParameters,
          commandConfigurations, configurationTypeOverrides, repositoryVersion, bindings);
      return null;
    });
  }

  private void augmentHostCommandLocked(Cluster cluster, ServiceComponentHost host,
      RoleCommand roleCommand, Map<String, String> commandParameters,
      Map<String, Map<String, String>> commandConfigurations,
      Set<String> configurationTypeOverrides, String repositoryVersion,
      List<ServiceDependencyBindingEntity> bindings) throws AmbariException {
    bindings = new ArrayList<>(dependencyDAO.findByConsumer(cluster.getClusterId(), "HBASE"));
    if (roleCommand == RoleCommand.START || roleCommand == RoleCommand.RESTART) {
      coordinator.validateConsumerStart(cluster, host.getHost().getHostId());
    }
    if (bindings.isEmpty()) {
      return;
    }
    for (ServiceDependencyBindingEntity binding : bindings) {
      coordinator.validateDispatchState(binding);
    }

    List<ManagedDependencyCommand> commands = roleCommand == RoleCommand.INSTALL
        ? planPreparations(cluster, host, commandParameters, repositoryVersion, bindings)
        : readyPreparations(cluster, host, bindings);
    if (commands.isEmpty()) {
      throw new AmbariException("Managed dependency preparation is unavailable for this HBase host");
    }
    String identityFingerprint = commands.get(0).parameters().get("identity.fingerprint");
    String consumerUser = commands.get(0).parameters().get("consumer.user");
    ManagedDependencyCommandBundle bundle = ManagedDependencyCommandBundle.of(
        host.getHost().getHostId(), consumerUser, identityFingerprint, commands);
    decorateCommandConfigurations(bundle, commandConfigurations, configurationTypeOverrides);
    commandParameters.put(BUNDLE_PARAMETER, StageUtils.getGson().toJson(bundle));
  }

  /** Applies the approved provider client profile only to this HBase execution command. */
  public void decoratePersistedCommandConfigurations(long hostId, String rawBundle,
      Map<String, Map<String, String>> commandConfigurations,
      Set<String> configurationTypeOverrides) throws AmbariException {
    try {
      ManagedDependencyCommandBundle bundle = StageUtils.getGson().fromJson(
          rawBundle, ManagedDependencyCommandBundle.class);
      if (bundle.hostId() != hostId) {
        throw new AmbariException("Managed dependency preparation targets a different host");
      }
      decorateCommandConfigurations(bundle, commandConfigurations, configurationTypeOverrides);
    } catch (IllegalArgumentException | com.google.gson.JsonParseException e) {
      throw new AmbariException("Managed dependency preparation bundle is invalid", e);
    }
  }

  private void decorateCommandConfigurations(ManagedDependencyCommandBundle bundle,
      Map<String, Map<String, String>> commandConfigurations,
      Set<String> configurationTypeOverrides) throws AmbariException {
    if (commandConfigurations == null || configurationTypeOverrides == null) {
      throw new AmbariException("HBase execution configurations are unavailable");
    }
    for (ManagedDependencyCommand command : bundle.commands()) {
      Map<String, Map<String, String>> client = clientConfig(command);
      merge(commandConfigurations, "hbase-site", client.get("zooKeeperClient"));
      if (command.name() == ManagedDependencyCommand.CommandName.PREPARE_HDFS_CONSUMER) {
        // Common stack hooks also consume these configurations and may write the
        // local Hadoop client directory. Provider core/hdfs maps belong only to
        // the immutable bundle and the HBase script's dedicated client profile.
        merge(commandConfigurations, "hbase-site", Map.of(
            "hbase.rootdir", command.parameters().get("expected.namespace.root.uri"),
            "hbase.wal.dir", command.parameters().get("expected.namespace.wal.uri")));
      }
      merge(commandConfigurations, "hbase-env", Map.of(
          "hbase_user", command.parameters().get("consumer.user")));
    }
  }

  private Map<String, Map<String, String>> clientConfig(ManagedDependencyCommand command)
      throws AmbariException {
    try {
      Map<String, Map<String, String>> parsed = StageUtils.getGson().fromJson(
          command.parameters().get("client.config.json"),
          new TypeToken<Map<String, Map<String, String>>>() { }.getType());
      if (parsed == null || !parsed.keySet().equals(Set.of(
          "coreSite", "hdfsSite", "zooKeeperClient"))
          || parsed.values().stream().anyMatch(Objects::isNull)) {
        throw new AmbariException("Managed dependency client configuration is invalid");
      }
      return parsed;
    } catch (com.google.gson.JsonParseException e) {
      throw new AmbariException("Managed dependency client configuration is invalid", e);
    }
  }

  private void merge(Map<String, Map<String, String>> configurations,
      String type, Map<String, String> values) {
    if (values == null || values.isEmpty()) {
      return;
    }
    configurations.computeIfAbsent(type, ignored -> new TreeMap<>()).putAll(values);
  }

  private List<ManagedDependencyCommand> planPreparations(Cluster cluster,
      ServiceComponentHost host,
      Map<String, String> commandParameters, String repositoryVersion,
      List<ServiceDependencyBindingEntity> bindings) throws AmbariException {
    if (repositoryVersion == null || repositoryVersion.isBlank()
        || repositoryVersion.indexOf('*') >= 0) {
      throw new AmbariException(
          "Managed dependency preparation requires an exact HBase repository version");
    }
    List<ServiceOsSpecific.Package> packages = StageUtils.getGson().fromJson(
        commandParameters.get("package_list"),
        new TypeToken<List<ServiceOsSpecific.Package>>() { }.getType());
    if (packages == null) {
      throw new AmbariException("Managed dependency preparation requires the exact install package list");
    }

    List<ManagedDependencyCommand> commands = new ArrayList<>();
    for (ServiceDependencyBindingEntity binding : bindings) {
      if (!"APPROVED".equals(binding.getSnapshotApproval())
          || binding.getProviderPreparationHash() == null
          || !Set.of("PROVIDER_PREPARED", "CONSUMER_CREDENTIALS_REQUIRED", "CONSUMER_VERIFYING", "READY")
              .contains(binding.getProvisioningPhase())
          || !Set.of("PROVISIONING", "READY").contains(binding.getState())) {
        throw new AmbariException("Managed dependency provider preparation is not complete");
      }
      ServiceDependencySnapshotEntity snapshotEntity = snapshotEntity(binding);
      ManagedDependencySnapshot snapshot = snapshot(snapshotEntity);
      ServiceDependencyOperationEntity operation = operation(binding);
      String packageName = packageName(packages, binding.getDependencyType(), repositoryVersion);
      String clientVersion = clientSoftwareVersion(snapshotEntity, snapshot);
      String identityFingerprint = identityFingerprint(snapshot);
      ManagedDependencyCommand command = ManagedDependencyCommand.prepareConsumer(snapshot,
          UUID.fromString(operation.getOperationId()), operation.getOperationEpoch(),
          host.getHost().getHostId(), packageName, clientVersion, identityFingerprint);
      commands.add(command);
    }
    return commands;
  }

  private String clientSoftwareVersion(ServiceDependencySnapshotEntity entity,
      ManagedDependencySnapshot snapshot) {
    String metadataVersion = snapshot.type() == ManagedDependencyType.HDFS
        ? snapshot.providerVersion().serviceVersion() : entity.getConsumerServiceVersion();
    return ManagedDependencyVersion.clientSoftwareVersion(
        snapshot.providerVersion().stackName(), metadataVersion);
  }

  /** Persists computed preparation intents only inside the action publication transaction. */
  public void planPreparationCommands(HostRoleCommand task) throws AmbariException {
    if (task == null || task.getRoleCommand() != RoleCommand.INSTALL
        || task.getExecutionCommandWrapper() == null
        || task.getExecutionCommandWrapper().getExecutionCommand().getCommandParams() == null) {
      return;
    }
    org.apache.ambari.server.agent.ExecutionCommand executionCommand = task
        .getExecutionCommandWrapper().getExecutionCommand();
    String rawBundle = executionCommand.getCommandParams().get(BUNDLE_PARAMETER);
    if (rawBundle == null) {
      return;
    }
    ManagedDependencyCommandBundle bundle;
    try {
      bundle = StageUtils.getGson().fromJson(rawBundle, ManagedDependencyCommandBundle.class);
      if (bundle == null || bundle.hostId() != task.getHostId()
          || executionCommand.getClusterId() == null) {
        throw new IllegalArgumentException("preparation bundle identity is invalid");
      }
      long clusterId = Long.parseLong(executionCommand.getClusterId());
      Cluster cluster = descriptorResolver.cluster(clusterId);
      String preparationComponent = canonicalPreparationComponent(cluster, task.getHostId());
      List<ServiceDependencyHostResultEntity> intents = bundle.preparationCommands().stream()
          .map(command -> ManagedDependencyOperationDispatcher.commandEntity(command,
              ManagedDependencyType.valueOf(command.parameters().get("provider.service")),
              task.getHostId(), preparationComponent))
          .toList();
      dependencyDAO.planCommands(intents);
    } catch (AmbariException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new AmbariException("Managed dependency preparation bundle is invalid", e);
    }
  }

  /** Applies managed lifecycle restart desired state only after final validation. */
  public void applyDeferredCustomLifecycleState(HostRoleCommand task) throws AmbariException {
    if (task == null || task.getRoleCommand() != RoleCommand.CUSTOM_COMMAND
        || task.getExecutionCommandWrapper() == null
        || task.isFutureCommand()
        || !"RESTART".equalsIgnoreCase(task.getCustomCommandName())) {
      return;
    }
    org.apache.ambari.server.agent.ExecutionCommand executionCommand = task
        .getExecutionCommandWrapper().getExecutionCommand();
    String serviceName = executionCommand.getServiceName();
    if (serviceName == null || !Set.of("HBASE", "HDFS", "ZOOKEEPER").contains(serviceName)) {
      return;
    }
    try {
      if (executionCommand.getClusterId() == null || task.getRole() == null) {
        throw new IllegalArgumentException("managed restart identity is invalid");
      }
      Cluster cluster = descriptorResolver.cluster(Long.parseLong(executionCommand.getClusterId()));
      org.apache.ambari.server.state.Service service = cluster.getService(serviceName);
      ServiceComponent serviceComponent = service.getServiceComponent(task.getRole().name());
      ServiceComponentHost host = serviceComponent.getServiceComponentHost(task.getHostName());
      if (host != null && host.getDesiredState() != org.apache.ambari.server.state.State.STARTED
          && !serviceComponent.isClientComponent()) {
        host.setDesiredState(org.apache.ambari.server.state.State.STARTED);
      }
    } catch (AmbariException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new AmbariException("Managed lifecycle restart target is invalid", e);
    }
  }

  /** Associates reserved commands and canonical preparation tasks in the action transaction. */
  public void associatePreparationTask(HostRoleCommand task) throws AmbariException {
    if (task == null || task.getExecutionCommandWrapper() == null
        || task.getRoleCommand() != RoleCommand.INSTALL && task.getRoleCommand() != RoleCommand.CUSTOM_COMMAND) return;
    org.apache.ambari.server.agent.ExecutionCommand execution = task.getExecutionCommandWrapper().getExecutionCommand();
    String rawCommand = execution.getCommandParams() == null ? null : execution.getCommandParams()
        .get(ManagedDependencyOperationDispatcher.COMMAND_PARAMETER);
    if (rawCommand == null && ManagedDependencyOperationDispatcher.isReservedCommand(task.getCustomCommandName())) {
      throw new AmbariException("A reserved dependency task cannot be published without its operation identity");
    }
    if (execution.getCommandParams() == null) return;
    if (rawCommand != null) {
      ManagedDependencyCommand command = StageUtils.getGson().fromJson(
          rawCommand, ManagedDependencyCommand.class);
      ServiceDependencyHostResultEntity expected = ManagedDependencyOperationDispatcher.commandEntity(
          command, ManagedDependencyType.valueOf(command.parameters().get("provider.service")),
          task.getHostId(), task.getRole().name());
      if (!dependencyDAO.associatePreparationTask(expected, task.getRole().name(),
          task.getRequestId(), task.getStageId(), task.getTaskId())) {
        throw new AmbariException("The task does not own its reserved dependency command");
      }
      return;
    }
    if (task.getRoleCommand() != RoleCommand.INSTALL) {
      return;
    }
    String rawBundle = task.getExecutionCommandWrapper().getExecutionCommand()
        .getCommandParams().get(BUNDLE_PARAMETER);
    if (rawBundle == null) {
      return;
    }
    ManagedDependencyCommandBundle bundle;
    try {
      bundle = StageUtils.getGson().fromJson(rawBundle, ManagedDependencyCommandBundle.class);
    } catch (RuntimeException e) {
      throw new AmbariException("Managed dependency preparation bundle is invalid", e);
    }
    if (bundle.hostId() != task.getHostId()) {
      throw new AmbariException("Managed dependency preparation task targets a different host");
    }
    List<Cluster> parents = new ArrayList<>();
    String clusterId = task.getExecutionCommandWrapper().getExecutionCommand().getClusterId();
    if (clusterId == null) {
      throw new AmbariException("Managed dependency preparation has no consumer cluster identity");
    }
    parents.add(descriptorResolver.cluster(Long.parseLong(clusterId)));
    bundle.commands().stream()
        .map(command -> Long.parseLong(command.parameters().get("provider.cluster.id")))
        .distinct().map(descriptorResolver::cluster).forEach(parents::add);
    withClusterReadLocks(parents, 0, () -> {
      for (ManagedDependencyCommand command : bundle.preparationCommands().stream()
          .sorted(Comparator.comparing(value -> value.envelope().bindingId()))
          .toList()) {
        ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(
            command.envelope().bindingId().toString());
        if (binding == null) {
          throw new AmbariException("Managed dependency preparation parent is missing");
        }
        coordinator.validateDispatchState(binding);
        ServiceDependencyHostResultEntity expected =
            ManagedDependencyOperationDispatcher.commandEntity(command,
                ManagedDependencyType.valueOf(command.parameters().get("provider.service")),
                task.getHostId(), task.getRole().name());
        dependencyDAO.associatePreparationTask(expected, task.getRole().name(),
            task.getRequestId(), task.getStageId(), task.getTaskId());
      }
      return null;
    });
  }

  /**
   * Validates the complete set of generated lifecycle tasks before the caller
   * applies desired-state changes. Command metadata is read from the generated
   * task, so custom command hints cannot change the effective lifecycle action.
   */
  public void validateLifecycleStages(Cluster requestCluster, List<Stage> stages)
      throws AmbariException {
    if (stages == null) {
      return;
    }
    Map<ManagedDependencyLifecyclePolicy.ImpactTarget, Map<String, String>> providerTargets =
        new LinkedHashMap<>();
    for (Stage stage : stages) {
      for (HostRoleCommand task : stage.getOrderedHostRoleCommands()) {
        ManagedDependencyLifecyclePolicy.ImpactTarget target =
            validateLifecycleTask(requestCluster, task);
        if (target != null) {
          org.apache.ambari.server.agent.ExecutionCommand command = task
              .getExecutionCommandWrapper().getExecutionCommand();
          Map<String, String> previous = providerTargets.putIfAbsent(target,
              command.getCommandParams());
          if (previous != null && !sameConfirmation(previous, command.getCommandParams())) {
            throw new ManagedDependencyIntegrationException(409,
                "DEPENDENCY_IMPACT_CONFIRMATION_CONFLICT",
                "Provider impact confirmations conflict within this request.");
          }
        }
      }
    }
    lifecyclePolicy.validateProviderTargetSet(providerTargets);
  }

  private ManagedDependencyLifecyclePolicy.ImpactTarget validateLifecycleTask(
      Cluster requestCluster, HostRoleCommand task)
      throws AmbariException {
    if (task.getExecutionCommandWrapper() == null) {
      return null;
    }
    org.apache.ambari.server.agent.ExecutionCommand command = task
        .getExecutionCommandWrapper().getExecutionCommand();
    String serviceName = command.getServiceName();
    String action = effectiveLifecycleAction(task);
    if (serviceName == null || action == null) {
      return null;
    }
    requireTaskCluster(requestCluster, command);
    if (Set.of("HDFS", "ZOOKEEPER").contains(serviceName)
        && Set.of("STOP", "RESTART").contains(action)) {
      boolean requiresConfirmation = lifecyclePolicy.validateProviderAction(
          requestCluster, serviceName, action, command.getCommandParams());
      return requiresConfirmation ? new ManagedDependencyLifecyclePolicy.ImpactTarget(
          requestCluster.getClusterId(), serviceName, action) : null;
    } else if ("HBASE".equals(serviceName)
        && Set.of("START", "RESTART").contains(action)) {
      coordinator.validateConsumerStart(requestCluster, task.getHostId());
    }
    return null;
  }

  private boolean sameConfirmation(Map<String, String> left, Map<String, String> right) {
    return Objects.equals(ManagedDependencyLifecyclePolicy.parameter(left,
            ManagedDependencyLifecyclePolicy.IMPACT_ACTION_PARAMETER),
        ManagedDependencyLifecyclePolicy.parameter(right,
            ManagedDependencyLifecyclePolicy.IMPACT_ACTION_PARAMETER))
        && Objects.equals(ManagedDependencyLifecyclePolicy.parameter(left,
            ManagedDependencyLifecyclePolicy.IMPACT_REVISION_PARAMETER),
        ManagedDependencyLifecyclePolicy.parameter(right,
            ManagedDependencyLifecyclePolicy.IMPACT_REVISION_PARAMETER))
        && Objects.equals(ManagedDependencyLifecyclePolicy.parameter(left,
            ManagedDependencyLifecyclePolicy.IMPACT_CONFIRMATION_MODE_PARAMETER),
        ManagedDependencyLifecyclePolicy.parameter(right,
            ManagedDependencyLifecyclePolicy.IMPACT_CONFIRMATION_MODE_PARAMETER));
  }

  private String effectiveLifecycleAction(HostRoleCommand task) {
    if (task.getRoleCommand() == RoleCommand.STOP
        || task.getRoleCommand() == RoleCommand.START
        || task.getRoleCommand() == RoleCommand.RESTART) {
      return task.getRoleCommand().name();
    }
    if (task.getRoleCommand() == RoleCommand.CUSTOM_COMMAND
        && task.getCustomCommandName() != null) {
      return task.getCustomCommandName().trim().toUpperCase(java.util.Locale.ROOT);
    }
    return null;
  }

  private void requireTaskCluster(Cluster requestCluster,
      org.apache.ambari.server.agent.ExecutionCommand command) throws AmbariException {
    if (requestCluster == null) {
      throw new AmbariException("Managed dependency request cluster does not exist");
    }
    try {
      if (command.getClusterId() == null
          || Long.parseLong(command.getClusterId()) != requestCluster.getClusterId()) {
        throw new AmbariException("Managed dependency task cluster does not match its request cluster");
      }
    } catch (NumberFormatException e) {
      throw new AmbariException("Managed dependency task cluster identity is invalid", e);
    }
  }

  /** Holds every consumer/provider parent write lock through action commit. */
  public boolean hasManagedLifecycleStages(List<Stage> stages) {
    if (stages == null) {
      return false;
    }
    return stages.stream().flatMap(stage -> stage.getOrderedHostRoleCommands().stream())
        .anyMatch(this::isManagedLifecycleTask);
  }

  private boolean isManagedLifecycleTask(HostRoleCommand task) {
    if (task.getExecutionCommandWrapper() == null) {
      return false;
    }
    org.apache.ambari.server.agent.ExecutionCommand executionCommand = task
        .getExecutionCommandWrapper().getExecutionCommand();
    String serviceName = executionCommand.getServiceName();
    String actionName = effectiveLifecycleAction(task);
    boolean managedTask = task.getRoleCommand() == RoleCommand.INSTALL
        && executionCommand.getCommandParams() != null
        && executionCommand.getCommandParams().get(BUNDLE_PARAMETER) != null;
    managedTask |= "HBASE".equals(serviceName) && actionName != null
        && Set.of("START", "RESTART").contains(actionName);
    managedTask |= serviceName != null && Set.of("HDFS", "ZOOKEEPER").contains(serviceName)
        && actionName != null && Set.of("STOP", "RESTART").contains(actionName);
    return managedTask;
  }

  public void executeWithPreparationParentLocks(Request request, CheckedAction action)
      throws AmbariException {
    Map<Long, Cluster> parentsById = new TreeMap<>();
    boolean managedLifecycle = false;
    for (Stage stage : request.getStages()) {
      for (HostRoleCommand task : stage.getOrderedHostRoleCommands()) {
        if (task.getExecutionCommandWrapper() == null) {
          continue;
        }
        org.apache.ambari.server.agent.ExecutionCommand executionCommand = task
            .getExecutionCommandWrapper().getExecutionCommand();
        String serviceName = executionCommand.getServiceName();
        if (!isManagedLifecycleTask(task)) {
          continue;
        }
        managedLifecycle = true;
        if (request.getClusterId() == null || request.getClusterId() <= 0) {
          throw new AmbariException("Managed dependency request cluster identity is invalid");
        }
        requireTaskCluster(descriptorResolver.cluster(request.getClusterId()), executionCommand);
        parentsById.put(request.getClusterId(), descriptorResolver.cluster(request.getClusterId()));

        Map<String, String> commandParameters = executionCommand.getCommandParams();
        String rawBundle = commandParameters == null ? null : commandParameters.get(BUNDLE_PARAMETER);
        if (rawBundle != null) {
          ManagedDependencyCommandBundle bundle;
          try {
            bundle = StageUtils.getGson().fromJson(rawBundle, ManagedDependencyCommandBundle.class);
          } catch (RuntimeException e) {
            throw new AmbariException("Managed dependency preparation bundle is invalid", e);
          }
          if (bundle == null) {
            throw new AmbariException("Managed dependency preparation bundle is invalid");
          }
          for (ManagedDependencyCommand dependencyCommand : bundle.commands()) {
            try {
              long providerClusterId = Long.parseLong(
                  dependencyCommand.parameters().get("provider.cluster.id"));
              parentsById.put(providerClusterId, descriptorResolver.cluster(providerClusterId));
            } catch (RuntimeException e) {
              throw new AmbariException("Managed dependency provider cluster identity is invalid", e);
            }
          }
        }
        if ("HBASE".equals(serviceName)) {
          for (ServiceDependencyBindingEntity binding : dependencyDAO.findByConsumer(
              request.getClusterId(), "HBASE")) {
            parentsById.put(binding.getProviderClusterId(),
                descriptorResolver.cluster(binding.getProviderClusterId()));
          }
        }
      }
    }
    if (!managedLifecycle) {
      action.run();
      return;
    }
    rejectUnsupportedOuterTransaction();
    List<Cluster> parents = new ArrayList<>(parentsById.values());
    withClusterWriteLocksUntilTransactionCompletion(parents, 0, () -> {
      assertParentSetStable(request, parentsById);
      validateLifecycleStages(descriptorResolver.cluster(request.getClusterId()),
          new ArrayList<>(request.getStages()));
      action.run();
      return null;
    });
  }

  /**
   * A caller that manually starts JPA cannot safely hand lexical parent locks
   * across its commit boundary. The supported transaction interceptor marks
   * its own outer transaction and retains dynamic locks until completion.
   */
  private void rejectUnsupportedOuterTransaction() throws AmbariException {
    if (AmbariJpaLocalTxnInterceptor.isTransactionActive() || entityManagerProvider == null) {
      return;
    }
    try {
      if (entityManagerProvider.get().getTransaction().isActive()) {
        throw new AmbariException(
            "Managed dependency publication requires the Ambari transaction boundary");
      }
    } catch (IllegalStateException e) {
      // No persistence context is available in non-Guice focused callers.
    }
  }

  /**
   * A binding creator holds the same consumer/provider parent read locks. If a
   * binding appeared after the initial scan, either its provider is already in
   * the ordered lock set or this request must fail before validation/publication.
   */
  private void assertParentSetStable(Request request, Map<Long, Cluster> lockedParents)
      throws AmbariException {
    boolean hasHbaseLifecycleTask = false;
    for (Stage stage : request.getStages()) {
      for (HostRoleCommand task : stage.getOrderedHostRoleCommands()) {
        if (task.getExecutionCommandWrapper() == null) {
          continue;
        }
        org.apache.ambari.server.agent.ExecutionCommand command = task
            .getExecutionCommandWrapper().getExecutionCommand();
        if ("HBASE".equals(command.getServiceName())) {
          hasHbaseLifecycleTask = true;
          break;
        }
      }
      if (hasHbaseLifecycleTask) {
        break;
      }
    }
    if (!hasHbaseLifecycleTask) {
      return;
    }
    for (ServiceDependencyBindingEntity binding : dependencyDAO.findByConsumer(
        request.getClusterId(), "HBASE")) {
      if (!lockedParents.containsKey(binding.getProviderClusterId())) {
        throw new ManagedDependencyIntegrationException(409,
            "DEPENDENCY_PARENT_SET_CHANGED",
            "Managed dependency parents changed while this request was being prepared.");
      }
    }
  }

  /** Reuses the exact successful preparation profile for a managed HBase custom command. */
  public String readyPreparationBundle(long consumerClusterId, long hostId)
      throws AmbariException {
    Cluster cluster = descriptorResolver.cluster(consumerClusterId);
    List<ServiceDependencyBindingEntity> bindings = new ArrayList<>(
        dependencyDAO.findByConsumer(consumerClusterId, "HBASE"));
    if (bindings.isEmpty()) {
      withClusterReadLocks(List.of(cluster), 0, () -> {
        coordinator.validateConsumerStart(cluster, hostId);
        return null;
      });
      return null;
    }
    bindings.sort(Comparator.comparing(ServiceDependencyBindingEntity::getDependencyType));
    List<Cluster> parents = new ArrayList<>();
    parents.add(cluster);
    bindings.stream().map(ServiceDependencyBindingEntity::getProviderClusterId)
        .distinct().map(descriptorResolver::cluster).forEach(parents::add);
    return withClusterReadLocks(parents, 0, () -> {
      List<ServiceDependencyBindingEntity> currentBindings = new ArrayList<>(
          dependencyDAO.findByConsumer(consumerClusterId, "HBASE"));
      coordinator.validateConsumerStart(cluster, hostId);
      if (currentBindings.isEmpty()) {
        return null;
      }
      for (ServiceDependencyBindingEntity binding : currentBindings) {
        coordinator.validateDispatchState(binding);
        if (!"READY".equals(binding.getState())) {
          throw new AmbariException("Managed dependency preparation is not ready");
        }
      }
      ServiceComponentHost host = cluster.getService("HBASE").getServiceComponents().values()
          .stream().flatMap(component -> component.getServiceComponentHosts().values().stream())
          .filter(componentHost -> componentHost.getHost().getHostId() == hostId)
          .findFirst().orElseThrow(() -> new AmbariException(
              "Managed dependency preparation targets an unassigned HBase host"));
      List<ManagedDependencyCommand> commands = readyPreparations(cluster, host, currentBindings);
      ManagedDependencyCommand first = commands.get(0);
      return StageUtils.getGson().toJson(ManagedDependencyCommandBundle.of(hostId,
          first.parameters().get("consumer.user"),
          first.parameters().get("identity.fingerprint"), commands));
    });
  }

  private String canonicalPreparationComponent(Cluster cluster, long hostId)
      throws AmbariException {
    List<String> priority = List.of(
        "HBASE_MASTER", "HBASE_REGIONSERVER", "HBASE_THRIFT", "HBASE_CLIENT");
    Map<String, org.apache.ambari.server.state.ServiceComponent> components =
        cluster.getService("HBASE").getServiceComponents();
    for (String componentName : priority) {
      if (components.containsKey(componentName)
          && components.get(componentName).getServiceComponentHosts().values().stream()
              .anyMatch(candidate -> candidate.getHost().getHostId() == hostId)) {
        return componentName;
      }
    }
    throw new AmbariException("Managed dependency HBase host has no canonical component owner");
  }

  private List<ManagedDependencyCommand> readyPreparations(Cluster cluster, ServiceComponentHost host,
      List<ServiceDependencyBindingEntity> bindings) throws AmbariException {
    return persistedPreparationCommands(host.getHost().getHostId(), bindings, true);
  }

  private Set<Long> strictlyVerifiedHostIds(ServiceDependencyBindingEntity binding,
      String verifyKind) {
    String prepareKind = "PREPARE_" + binding.getDependencyType() + "_CONSUMER";
    return dependencyDAO.findHostResults(
        binding.getBindingId(), binding.getDesiredSnapshotVersion()).stream()
        .filter(result -> verifyKind.equals(result.getCheckKind()))
        .filter(result -> "SUCCEEDED".equals(result.getState()))
        .filter(result -> Objects.equals(binding.getOperationEpoch(), result.getOperationEpoch()))
        .filter(result -> {
          ServiceDependencyHostResultEntity preparation = dependencyDAO.findHostResult(
              binding.getBindingId(), binding.getDesiredSnapshotVersion(),
              binding.getOperationEpoch(), result.getHostId(),
              binding.getDependencyType(), prepareKind);
          return preparation != null && "SUCCEEDED".equals(preparation.getState())
              && preparation.getPreparationObservationId() != null
              && preparation.getPreparationObservationFingerprint() != null
              && preparation.getPackageName() != null
              && preparation.getPackageVersion() != null
              && preparation.getClientSoftwareVersion() != null
              && preparation.getObservedPackageHash() != null
              && preparation.getRenderedConfigHash() != null
              && preparation.getIdentityFingerprint() != null
              && Objects.equals(binding.getActiveOperationId(), preparation.getOperationId())
              && Objects.equals(binding.getActiveOperationId(), result.getOperationId())
              && Objects.equals(binding.getOperationEpoch(), preparation.getOperationEpoch())
              && Objects.equals(preparation.getCommandRequestHash(),
                  result.getPreparationRequestHash())
              && Objects.equals(preparation.getPreparationObservationId(),
                  result.getPreparationObservationId())
              && Objects.equals(preparation.getPreparationObservationFingerprint(),
                  result.getPreparationObservationFingerprint())
              && Objects.equals(preparation.getPackageName(), result.getPackageName())
              && Objects.equals(preparation.getPackageVersion(), result.getPackageVersion())
              && Objects.equals(preparation.getClientSoftwareVersion(),
                  result.getClientSoftwareVersion())
              && Objects.equals(preparation.getObservedPackageHash(),
                  result.getObservedPackageHash())
              && result.getRenderedConfigHash() != null
              && result.getIdentityFingerprint() != null;
        })
        .map(ServiceDependencyHostResultEntity::getHostId)
        .collect(java.util.stream.Collectors.toSet());
  }

  private Set<Long> daemonHostIds(Cluster cluster) throws AmbariException {
    Set<String> daemonComponents = Set.of(
        "HBASE_MASTER", "HBASE_REGIONSERVER", "HBASE_THRIFT");
    if (!cluster.getServices().containsKey("HBASE")) {
      throw new AmbariException("Managed dependency HBase service is missing");
    }
    Set<Long> hosts = cluster.getService("HBASE").getServiceComponents().values().stream()
        .filter(component -> daemonComponents.contains(component.getName()))
        .flatMap(component -> component.getServiceComponentHosts().values().stream())
        .map(componentHost -> componentHost.getHost().getHostId())
        .collect(java.util.stream.Collectors.toSet());
    if (hosts.isEmpty()) {
      throw new AmbariException("Managed dependency HBase has no daemon host assignments");
    }
    return Set.copyOf(hosts);
  }

  public String persistedPreparationBundle(long consumerClusterId, long hostId)
      throws AmbariException {
    return persistedPreparationBundle(consumerClusterId, hostId, null);
  }

  /**
   * Returns the complete current profile while marking only one binding as the
   * reporting owner.  Retry dispatch uses this form so a companion remains a
   * rendered client profile without creating a second observation.
   */
  public String persistedPreparationBundle(long consumerClusterId, long hostId,
      UUID preparationBindingId) throws AmbariException {
    List<ServiceDependencyBindingEntity> bindings = new ArrayList<>(
        dependencyDAO.findByConsumer(consumerClusterId, "HBASE"));
    bindings.sort(Comparator.comparing(ServiceDependencyBindingEntity::getDependencyType));
    List<ManagedDependencyCommand> commands = persistedPreparationCommands(hostId, bindings, false);
    if (commands.isEmpty()) {
      throw new AmbariException("No managed dependency preparation exists for this HBase host");
    }
    ManagedDependencyCommand first = commands.get(0);
    ManagedDependencyCommandBundle bundle = preparationBindingId == null
        ? ManagedDependencyCommandBundle.of(hostId, first.parameters().get("consumer.user"),
            first.parameters().get("identity.fingerprint"), commands)
        : ManagedDependencyCommandBundle.of(hostId, first.parameters().get("consumer.user"),
            first.parameters().get("identity.fingerprint"), commands,
            Set.of(preparationBindingId));
    return StageUtils.getGson().toJson(bundle);
  }

  /**
   * Rebuilds a retry from the current approved snapshots and the prior failed
   * preparation's immutable package facts.  Only the requested binding is
   * assigned to the new preparation operation; companion commands retain their
   * current persisted envelope.
   */
  public RetryPreparationPlan buildRetryPreparationPlan(long consumerClusterId, long hostId,
      UUID targetBindingId) throws AmbariException {
    Cluster cluster = descriptorResolver.cluster(consumerClusterId);
    List<ServiceDependencyBindingEntity> bindings = new ArrayList<>(
        dependencyDAO.findByConsumer(consumerClusterId, "HBASE"));
    bindings.sort(Comparator.comparing(ServiceDependencyBindingEntity::getDependencyType));
    ServiceDependencyBindingEntity target = bindings.stream()
        .filter(binding -> targetBindingId.equals(UUID.fromString(binding.getBindingId())))
        .findFirst()
        .orElseThrow(() -> new AmbariException("The managed dependency retry binding is missing"));
    List<ManagedDependencyCommand> commands = new ArrayList<>();
    for (ServiceDependencyBindingEntity binding : bindings) {
      if (!"APPROVED".equals(binding.getSnapshotApproval())
          || binding.getProviderPreparationHash() == null) {
        throw new AmbariException("The managed dependency retry approval is no longer current");
      }
      coordinator.validateDispatchState(binding);
      ServiceDependencyCommandSource source = currentPreparationSource(binding, hostId);
      if (source == null) {
        throw new AmbariException("The managed dependency retry has no immutable preparation profile");
      }
      ManagedDependencyCommand command = source.command();
      if (!binding.getBindingId().equals(command.envelope().bindingId().toString())
          || !Objects.equals(binding.getDesiredSnapshotVersion(),
              command.envelope().snapshotVersion())
          || !binding.getDependencyType().equals(
              command.parameters().get("provider.service"))) {
        throw new AmbariException("The managed dependency retry profile is stale");
      }
      if (binding == target) {
        ServiceDependencySnapshotEntity entity = snapshotEntity(binding);
        ManagedDependencySnapshot currentSnapshot = snapshot(entity);
        ServiceDependencyOperationEntity currentOperation = operation(binding);
        command = ManagedDependencyCommand.prepareConsumer(currentSnapshot,
            UUID.fromString(currentOperation.getOperationId()), currentOperation.getOperationEpoch(),
            hostId, source.packageName(), clientSoftwareVersion(entity, currentSnapshot),
            identityFingerprint(currentSnapshot));
      }
      commands.add(command);
    }
    String component = canonicalPreparationComponent(cluster, hostId);
    ManagedDependencyCommand first = commands.get(0);
    ManagedDependencyCommandBundle bundle = ManagedDependencyCommandBundle.of(hostId,
        first.parameters().get("consumer.user"), first.parameters().get("identity.fingerprint"),
        commands, Set.of(targetBindingId));
    return new RetryPreparationPlan(bundle, component, targetBindingId);
  }

  /** Persists the selected retry intent before its external INSTALL request. */
  public void persistRetryPreparationPlan(RetryPreparationPlan plan) {
    List<ServiceDependencyHostResultEntity> intents = plan.bundle().preparationCommands().stream()
        .map(command -> ManagedDependencyOperationDispatcher.commandEntity(command,
            ManagedDependencyType.valueOf(command.parameters().get("provider.service")),
            plan.bundle().hostId(), plan.componentName()))
        .toList();
    dependencyDAO.planCommands(intents);
  }

  private List<ManagedDependencyCommand> persistedPreparationCommands(long hostId,
      List<ServiceDependencyBindingEntity> bindings, boolean requireSucceeded) throws AmbariException {
    List<ManagedDependencyCommand> commands = new ArrayList<>();
    for (ServiceDependencyBindingEntity binding : bindings) {
      ServiceDependencyHostResultEntity preparation = dependencyDAO.findHostResult(
          binding.getBindingId(), binding.getDesiredSnapshotVersion(), binding.getOperationEpoch(), hostId,
          binding.getDependencyType(), "PREPARE_" + binding.getDependencyType() + "_CONSUMER");
      if (preparation == null || requireSucceeded
          && (preparation.getPreparationObservationId() == null
              || !"SUCCEEDED".equals(preparation.getState())
              || !Objects.equals(binding.getOperationEpoch(), preparation.getOperationEpoch()))) {
        throw new AmbariException(
            "HBase cannot use this host because its dependency preparation is incomplete");
      }
      commands.add(StageUtils.getGson().fromJson(
          preparation.getCommandJson(), ManagedDependencyCommand.class));
    }
    return commands;
  }

  private ServiceDependencyCommandSource currentPreparationSource(
      ServiceDependencyBindingEntity binding, long hostId) {
    String kind = "PREPARE_" + binding.getDependencyType() + "_CONSUMER";
    ServiceDependencyHostResultEntity current = dependencyDAO.findHostResult(
        binding.getBindingId(), binding.getDesiredSnapshotVersion(), binding.getOperationEpoch(),
        hostId, binding.getDependencyType(), kind);
    ServiceDependencyHostResultEntity source = current;
    if (source == null) {
      source = dependencyDAO.findHostResults(binding.getBindingId(),
              binding.getDesiredSnapshotVersion()).stream()
          .filter(result -> kind.equals(result.getCheckKind())
              && Objects.equals(result.getHostId(), hostId))
          .max(Comparator.comparing(ServiceDependencyHostResultEntity::getOperationEpoch))
          .orElse(null);
    }
    if (source == null || source.getCommandJson() == null) {
      return null;
    }
    try {
      ManagedDependencyCommand command = StageUtils.getGson().fromJson(
          source.getCommandJson(), ManagedDependencyCommand.class);
      String packageName = command.parameters().get("client.package.name");
      String softwareVersion = command.parameters().get("client.software.semantic.version");
      if (packageName == null || softwareVersion == null) {
        return null;
      }
      return new ServiceDependencyCommandSource(command, packageName, softwareVersion);
    } catch (RuntimeException e) {
      return null;
    }
  }

  public record RetryPreparationPlan(ManagedDependencyCommandBundle bundle,
      String componentName, UUID targetBindingId) {
    public RetryPreparationPlan {
      Objects.requireNonNull(bundle, "bundle");
      Objects.requireNonNull(componentName, "componentName");
      Objects.requireNonNull(targetBindingId, "targetBindingId");
      if (!bundle.preparationBindingIds().contains(targetBindingId)) {
        throw new IllegalArgumentException("retry target must be a preparation owner");
      }
    }
  }

  private record ServiceDependencyCommandSource(ManagedDependencyCommand command,
      String packageName, String softwareVersion) { }

  private ServiceDependencySnapshotEntity snapshotEntity(ServiceDependencyBindingEntity binding)
      throws AmbariException {
    ServiceDependencySnapshotEntity entity = dependencyDAO.findSnapshot(
        binding.getBindingId(), binding.getDesiredSnapshotVersion());
    if (entity == null) {
      throw new AmbariException("Managed dependency snapshot is missing");
    }
    return entity;
  }

  private ServiceDependencyOperationEntity operation(ServiceDependencyBindingEntity binding)
      throws AmbariException {
    ServiceDependencyOperationEntity operation = dependencyDAO.findOperation(binding.getActiveOperationId());
    if (operation == null || !binding.getOperationEpoch().equals(operation.getOperationEpoch())) {
      throw new AmbariException("Managed dependency operation is missing or stale");
    }
    return operation;
  }

  private ManagedDependencySnapshot snapshot(ServiceDependencySnapshotEntity entity)
      throws AmbariException {
    try {
      return StageUtils.getGson().fromJson(entity.getSnapshotJson(), ManagedDependencySnapshot.class);
    } catch (RuntimeException e) {
      throw new AmbariException("Managed dependency snapshot is invalid", e);
    }
  }

  private String packageName(List<ServiceOsSpecific.Package> packages, String dependencyType,
      String repositoryVersion) throws AmbariException {
    String prefix = ManagedDependencyType.HDFS.name().equals(dependencyType) ? "hadoop" : "hbase";
    String template = packages.stream().map(ServiceOsSpecific.Package::getName)
        .filter(name -> name != null && name.contains("${stack_version}"))
        .filter(name -> name.startsWith(prefix + "_") || name.startsWith(prefix + "-"))
        .filter(name -> !ManagedDependencyType.HDFS.name().equals(dependencyType)
            || name.endsWith("-client"))
        .findFirst()
        .orElseThrow(() -> new AmbariException(
            "The HBase install plan has no supported managed dependency client package"));
    String delimiter = template.startsWith(prefix + "-") ? "-" : "_";
    String formattedVersion = repositoryVersion.replace(".", delimiter).replace("-", delimiter);
    return template.replace("${stack_version}", formattedVersion);
  }

  private String identityFingerprint(ManagedDependencySnapshot snapshot) {
    return hash(StageUtils.getGson().toJson(snapshot.consumerIdentity()));
  }

  private String hash(String value) {
    try {
      return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
    }
  }

  private <T> T withClusterReadLocks(List<Cluster> candidates, int index,
      CheckedSupplier<T> operation) throws AmbariException {
    List<Cluster> clusters = candidates.stream()
        .collect(java.util.stream.Collectors.toMap(Cluster::getClusterId, value -> value,
            (left, right) -> left, TreeMap::new))
        .values().stream().toList();
    return withOrderedClusterReadLocks(clusters, index, operation);
  }

  private <T> T withOrderedClusterReadLocks(List<Cluster> clusters, int index,
      CheckedSupplier<T> operation) throws AmbariException {
    if (index == clusters.size()) {
      return operation.get();
    }
    try {
      return clusters.get(index).executeUnderReadLock(() -> {
        try {
          return withOrderedClusterReadLocks(clusters, index + 1, operation);
        } catch (AmbariException e) {
          throw new PlannerException(e);
        }
      });
    } catch (PlannerException e) {
      throw e.getCause();
    }
  }

  private <T> T withClusterWriteLocksUntilTransactionCompletion(List<Cluster> candidates,
      int index, CheckedSupplier<T> operation) throws AmbariException {
    List<Cluster> clusters = candidates.stream()
        .collect(java.util.stream.Collectors.toMap(Cluster::getClusterId, value -> value,
            (left, right) -> left, TreeMap::new))
        .values().stream().toList();
    return withOrderedClusterWriteLocks(clusters, index, operation);
  }

  private <T> T withOrderedClusterWriteLocks(List<Cluster> clusters, int index,
      CheckedSupplier<T> operation) throws AmbariException {
    if (index == clusters.size()) {
      return operation.get();
    }
    try {
      final Cluster cluster = clusters.get(index);
      return cluster.executeUnderWriteLockUntilTransactionCompletion(() -> {
        try {
          return withOrderedClusterWriteLocks(clusters, index + 1, operation);
        } catch (AmbariException e) {
          throw new PlannerException(e);
        }
      });
    } catch (PlannerException e) {
      throw e.getCause();
    }
  }

  @FunctionalInterface
  private interface CheckedSupplier<T> {
    T get() throws AmbariException;
  }

  @FunctionalInterface
  public interface CheckedAction {
    void run() throws AmbariException;
  }

  private static final class PlannerException extends RuntimeException {
    private final AmbariException cause;

    private PlannerException(AmbariException cause) {
      super(cause);
      this.cause = cause;
    }

    @Override
    public AmbariException getCause() {
      return cause;
    }
  }
}
