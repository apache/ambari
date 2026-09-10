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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand.Result;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand.ResultStatus;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CommandCompletion;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyOperationEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** Accepts managed-dependency evidence only from the exact persisted Ambari task payload. */
@Singleton
public class ManagedDependencyTaskResultProcessor {
  @Inject
  private ManagedDependencyCredentialManager credentialManager;

  private static final Logger LOG = LoggerFactory.getLogger(ManagedDependencyTaskResultProcessor.class);
  private static final String RESULT_KEY = "managedDependencyResult";
  private static final String PREPARATION_RESULTS_KEY = "managedDependencyPreparationResults";
  private static final int MAX_STRUCTURED_OUTPUT_BYTES = 96 * 1024;
  private static final Set<String> DAEMON_COMPONENTS = Set.of(
      "HBASE_MASTER", "HBASE_REGIONSERVER", "HBASE_THRIFT");

  private final ServiceDependencyDAO dependencyDAO;
  private final ManagedDependencyOperationDispatcher dispatcher;
  private final Clusters clusters;
  private final Provider<ActionManager> actionManager;

  @Inject
  public ManagedDependencyTaskResultProcessor(ServiceDependencyDAO dependencyDAO,
      ManagedDependencyOperationDispatcher dispatcher, Clusters clusters,
      Provider<ActionManager> actionManager) {
    this.dependencyDAO = dependencyDAO;
    this.dispatcher = dispatcher;
    this.clusters = clusters;
    this.actionManager = actionManager;
  }

  /** Compatibility constructor for focused policy/result tests. */
  public ManagedDependencyTaskResultProcessor(ServiceDependencyDAO dependencyDAO,
      ManagedDependencyOperationDispatcher dispatcher, Clusters clusters) {
    this(dependencyDAO, dispatcher, clusters, null);
  }

  /**
   * Advances a secure preparation only after the exact scoped keytab tasks have
   * completed. The caller supplies durable request/task identities obtained from
   * the authenticated action response or recovery scan.
   */
  public void dispatchVerificationAfterCredentials(String bindingId, long expectedOperationEpoch,
      long hostId, long credentialRequestId, Set<Long> credentialTaskIds) throws AmbariException {
    dispatchVerificationAfterCredentials(bindingId, expectedOperationEpoch, hostId,
        credentialRequestId, credentialTaskIds, false);
  }

  public void verifyManualCredentials(String bindingId, long epoch, long hostId) throws AmbariException {
    dispatchVerificationAfterCredentials(bindingId, epoch, hostId, 0, Set.of(), true);
  }

  private void dispatchVerificationAfterCredentials(String bindingId, long expectedOperationEpoch,
      long hostId, long credentialRequestId, Set<Long> credentialTaskIds, boolean manual) throws AmbariException {
    if (!manual && (actionManager == null || credentialRequestId <= 0 || credentialTaskIds == null
        || credentialTaskIds.isEmpty() || credentialTaskIds.stream().anyMatch(id -> id == null || id <= 0))) {
      throw new AmbariException("The secure managed dependency credential task evidence is missing");
    }
    ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(bindingId);
    if (binding == null || !Objects.equals(binding.getOperationEpoch(), expectedOperationEpoch)
        || binding.getActiveOperationId() == null
        || binding.getConsumerClusterId() == null
        || !"PROVISIONING".equals(binding.getState())
        || !Set.of("CONSUMER_CREDENTIALS_REQUIRED", "CONSUMER_VERIFYING")
            .contains(binding.getProvisioningPhase())) {
      throw new AmbariException("The managed dependency binding is missing or stale");
    }
    List<HostRoleCommand> tasks = manual ? List.of() : actionManager.get().getRequestTasks(credentialRequestId);
    Set<Long> matchedTaskIds = new HashSet<>();
    for (HostRoleCommand task : tasks) {
      if (!credentialTaskIds.contains(task.getTaskId())) {
        continue;
      }
      matchedTaskIds.add(task.getTaskId());
      if (task.getRequestId() != credentialRequestId
          || task.getStatus() != HostRoleStatus.COMPLETED
          || task.getRole() != Role.KERBEROS_CLIENT
          || !"SET_KEYTAB".equals(task.getCustomCommandName())
          || task.getHostId() != hostId
          || task.getExecutionCommandWrapper() == null
          || task.getExecutionCommandWrapper().getExecutionCommand() == null
          || !Long.toString(binding.getConsumerClusterId()).equals(task.getExecutionCommandWrapper()
              .getExecutionCommand().getClusterId())) {
        throw new AmbariException("A secure managed dependency credential task is incomplete");
      }
    }
    if (matchedTaskIds.size() != credentialTaskIds.size()) {
      throw new AmbariException("No authenticated credential task completed on the dependency host");
    }
    ServiceDependencyHostResultEntity preparation = dependencyDAO.findHostResult(bindingId,
        binding.getDesiredSnapshotVersion(), binding.getOperationEpoch(), hostId,
        binding.getDependencyType(), "PREPARE_" + binding.getDependencyType() + "_CONSUMER");
    if (preparation == null || !"SUCCEEDED".equals(preparation.getState())
        || preparation.getPreparationObservationId() == null
        || preparation.getPreparationObservationFingerprint() == null
        || preparation.getPackageName() == null || preparation.getPackageVersion() == null
        || preparation.getClientSoftwareVersion() == null || preparation.getIdentityFingerprint() == null
        || preparation.getObservedPackageHash() == null || preparation.getRenderedConfigHash() == null
        || preparation.getResultHash() == null
        || !Objects.equals(binding.getActiveOperationId(), preparation.getOperationId())
        || !Objects.equals(binding.getOperationEpoch(), preparation.getOperationEpoch())) {
      throw new AmbariException("The secure managed dependency preparation is not current");
    }
    if (credentialManager == null) {
      throw new AmbariException("The credential producer association is unavailable");
    }
    if (manual) {
      ManagedDependencyCredentialManager.Plan plan = ManagedDependencyCredentialManager.plan(preparation);
      if (plan == null || !plan.manual()) {
        throw new AmbariException("Manual credential verification is not enabled for this preparation");
      }
    } else {
      credentialManager.requireCompleted(preparation, credentialRequestId, credentialTaskIds);
    }
    ServiceDependencySnapshotEntity snapshotEntity = dependencyDAO.findSnapshot(bindingId,
        binding.getDesiredSnapshotVersion());
    if (snapshotEntity == null) {
      throw new AmbariException("The managed dependency snapshot is missing");
    }
    ManagedDependencySnapshot snapshot;
    try {
      snapshot = StageUtils.getGson().fromJson(
          snapshotEntity.getSnapshotJson(), ManagedDependencySnapshot.class);
    } catch (RuntimeException e) {
      throw new AmbariException("The managed dependency snapshot is invalid", e);
    }
    if (snapshot.securityMode() != ManagedDependencySecurityMode.KERBEROS) {
      throw new AmbariException("Credential completion is only valid for secure dependencies");
    }
    ManagedDependencyCommand preparationCommand;
    try {
      preparationCommand = StageUtils.getGson().fromJson(
          preparation.getCommandJson(), ManagedDependencyCommand.class);
    } catch (RuntimeException e) {
      throw new AmbariException("The secure managed dependency preparation is invalid", e);
    }
    if (!ManagedDependencyOperationDispatcher.isPreparationCommand(preparationCommand.name().name())
        || !bindingId.equals(preparationCommand.envelope().bindingId().toString())
        || !Objects.equals(binding.getActiveOperationId(),
            preparationCommand.envelope().operationId().toString())
        || !Objects.equals(binding.getOperationEpoch(), preparationCommand.envelope().epoch())
        || !Objects.equals(binding.getDesiredSnapshotVersion(),
            preparationCommand.envelope().snapshotVersion())) {
      throw new AmbariException("The secure managed dependency preparation is stale");
    }
    ManagedDependencyCommand verification;
    try {
      verification = ManagedDependencyCommand.verifyConsumer(snapshot,
          UUID.fromString(preparation.getOperationId()), preparation.getOperationEpoch(), hostId,
          preparation.getPackageName(), preparation.getPackageVersion(),
          preparation.getClientSoftwareVersion(), preparation.getIdentityFingerprint(),
          UUID.fromString(preparation.getPreparationObservationId()),
          preparationCommand.envelope().immutableRequestHash(),
          preparation.getPreparationObservationFingerprint());
    } catch (RuntimeException e) {
      throw new AmbariException("The secure managed dependency verification is invalid", e);
    }
    ServiceDependencyHostResultEntity next = ManagedDependencyOperationDispatcher.commandEntity(
        verification, snapshot.type(), hostId, preparation.getComponentName());
    dependencyDAO.planCommand(next);
    dispatcher.dispatchSafely(next);
  }

  public void process(CommandReport report, String authenticatedHostName,
      HostRoleCommand hostRoleCommand) {
    if (hostRoleCommand == null || !isCompleted(report.getStatus())) {
      return;
    }
    try {
      ExecutionCommand execution = hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand();
      String rawCommand = execution.getCommandParams()
          .get(ManagedDependencyOperationDispatcher.COMMAND_PARAMETER);
      String rawBundle = execution.getCommandParams()
          .get(ManagedDependencyRuntimePlanner.BUNDLE_PARAMETER);
      if (credentialManager != null && ManagedDependencyCredentialManager.isKeytabTask(hostRoleCommand)) {
        credentialManager.taskCompleted(hostRoleCommand);
      }
      if (rawCommand != null && rawBundle != null && taskIsPreparation(hostRoleCommand, rawCommand)) {
        processPreparationResults(report, authenticatedHostName, hostRoleCommand, rawBundle,
            rawCommand);
      } else if (rawCommand != null) {
        processReservedCommand(report, authenticatedHostName, hostRoleCommand,
            rawCommand, rawBundle);
      } else if (rawBundle != null) {
        processPreparationResults(report, authenticatedHostName, hostRoleCommand, rawBundle);
      }
    } catch (RuntimeException e) {
      LOG.warn("Rejected managed dependency task result: task={}, reason={}",
          report.getTaskId(), e.getClass().getSimpleName());
      if (hostRoleCommand.getTaskId() == report.getTaskId()
          && Objects.equals(authenticatedHostName, hostRoleCommand.getHostName())) {
        dependencyDAO.failTaskCommands(hostRoleCommand.getRequestId(),
            hostRoleCommand.getStageId(), hostRoleCommand.getTaskId(),
            "DEPENDENCY_RESULT_INVALID",
            "The managed dependency task completed without valid structured evidence.");
      }
    }
  }

  /** Replays a terminal persisted task whose heartbeat callback was lost during restart. */
  public void recover(HostRoleCommand task) {
    if (task == null || !task.getStatus().isCompletedState()) {
      return;
    }
    CommandReport report = new CommandReport();
    report.setTaskId(task.getTaskId());
    report.setStatus(task.getStatus().name());
    report.setRole(task.getRole().name());
    report.setRoleCommand(task.getRoleCommand().name());
    report.setCustomCommand(task.getCustomCommandName());
    report.setStructuredOut(task.getStructuredOut());
    process(report, task.getHostName(), task);
  }

  private void processReservedCommand(CommandReport report, String authenticatedHostName,
      HostRoleCommand task, String rawCommand, String rawBundle) {
    requireBounded(rawCommand, "Managed dependency command");
    ManagedDependencyCommand command = StageUtils.getGson().fromJson(
        rawCommand, ManagedDependencyCommand.class);
    if (!ManagedDependencyOperationDispatcher.isReservedCommand(command.name().name())) {
      return;
    }
    validateReservedTaskIdentity(report, authenticatedHostName, task, command, rawBundle);
    Result result = result(report.getStructuredOut());
    validateTaskOutcome(report, result);
    command.validateResult(result);
    ServiceDependencyHostResultEntity persisted = persisted(task, command, rawCommand);
    if (!task.getRole().name().equals(persisted.getComponentName())) {
      throw new IllegalStateException("The reporting task does not own this dependency command");
    }
    if (persisted.getResultHash() != null) {
      requireSameResultAndResume(persisted, command, result);
      return;
    }
    if (ManagedDependencyOperationDispatcher.isProviderCommand(command.name().name())) {
      completeProviderCommand(persisted, command, result, task);
    } else if (ManagedDependencyOperationDispatcher.isVerificationCommand(command.name().name())) {
      completeVerification(persisted, command, result, task);
    }
  }

  private void processPreparationResults(CommandReport report, String authenticatedHostName,
      HostRoleCommand task, String rawBundle) {
    processPreparationResults(report, authenticatedHostName, task, rawBundle, null);
  }

  private void processPreparationResults(CommandReport report, String authenticatedHostName,
      HostRoleCommand task, String rawBundle, String rawCommand) {
    requireBounded(rawBundle, "Managed dependency preparation bundle");
    ManagedDependencyCommandBundle bundle = StageUtils.getGson().fromJson(
        rawBundle, ManagedDependencyCommandBundle.class);
    if (task.getTaskId() != report.getTaskId()
        || !authenticatedHostName.equals(task.getHostName())
        || task.getRoleCommand() != RoleCommand.INSTALL
        || bundle.hostId() != task.getHostId()) {
      throw new IllegalStateException("The reporting task does not own this preparation bundle");
    }
    if (rawCommand != null) {
      ManagedDependencyCommand requested = StageUtils.getGson().fromJson(
          rawCommand, ManagedDependencyCommand.class);
      if (!ManagedDependencyOperationDispatcher.isPreparationCommand(requested.name().name())
          || bundle.preparationCommands().stream().noneMatch(command ->
              command.name() == requested.name()
                  && command.envelope().equals(requested.envelope()))) {
        throw new IllegalStateException("The preparation task command is not in its bundle");
      }
    }
    JsonArray values = preparationResults(report.getStructuredOut());
    List<ManagedDependencyCommand> preparationCommands = bundle.preparationCommands();
    Set<String> reported = new HashSet<>();
    java.util.List<PreparationEvidence> evidence = new java.util.ArrayList<>();
    for (JsonElement value : values) {
      Result result = StageUtils.getGson().fromJson(value, Result.class);
      ManagedDependencyCommand command = preparationCommands.stream()
          .filter(candidate -> candidate.name() == result.commandName()
              && candidate.envelope().equals(result.envelope()))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException(
              "Preparation result does not belong to the persisted bundle"));
      if (!reported.add(command.envelope().immutableRequestHash())) {
        throw new IllegalStateException("Preparation result is duplicated");
      }
      validateTaskOutcome(report, result);
      command.validateResult(result);
      String serializedCommand = StageUtils.getGson().toJson(command);
      ServiceDependencyHostResultEntity persisted = persisted(task, command, serializedCommand);
      if (!task.getRole().name().equals(persisted.getComponentName())) {
        continue;
      }
      evidence.add(new PreparationEvidence(persisted, command, result));
    }
    if (evidence.isEmpty()) {
      return;
    }
    if (HostRoleStatus.COMPLETED.name().equals(report.getStatus())
        && reported.size() != preparationCommands.size()) {
      throw new IllegalStateException("Successful preparation omitted a dependency result");
    }
    for (PreparationEvidence item : evidence) {
      if (item.persisted().getResultHash() != null) {
        requireSameResultAndResume(item.persisted(), item.command(), item.result());
      } else {
        completePreparation(item.persisted(), item.command(), item.result(), task);
      }
    }
    if (!HostRoleStatus.COMPLETED.name().equals(report.getStatus())
        && reported.size() != preparationCommands.size()) {
      dependencyDAO.failTaskCommands(task.getRequestId(), task.getStageId(), task.getTaskId(),
          "DEPENDENCY_PREPARATION_INCOMPLETE",
          "The HBase installation task ended before all managed dependencies were prepared.");
    }
  }

  private void completeProviderCommand(ServiceDependencyHostResultEntity persisted,
      ManagedDependencyCommand command, Result result, HostRoleCommand task) {
    ParentState parent = parent(persisted);
    ServiceDependencyHostResultEntity next = null;
    String bindingState = parent.binding().getState();
    String phase = parent.binding().getProvisioningPhase();
    String operationState = parent.operation().getState();
    String providerPreparationHash = null;
    Failure failure = failure(result, command.name().name());
    if (result.status() == ResultStatus.SUCCEEDED) {
      switch (command.name()) {
        case PREPARE_BINDING_JOURNAL -> {
          ManagedDependencyCommand successor = ManagedDependencyCommand.initializeJournal(parent.snapshot(),
              command.envelope().operationId(), command.envelope().epoch(), persisted.getHostId(),
              UUID.fromString(result.facts().get("initialization.challenge")), UUID.randomUUID(),
              command.envelope().immutableRequestHash());
          next = ManagedDependencyOperationDispatcher.commandEntity(
              successor, parent.snapshot().type(), persisted.getHostId(),
              persisted.getComponentName());
          phase = "PROVIDER_INITIALIZING";
          operationState = "PROVIDER_INITIALIZING";
        }
        case INITIALIZE_BINDING_JOURNAL -> {
          ManagedDependencyCommand successor = ManagedDependencyCommand.provision(parent.snapshot(),
              command.envelope().operationId(), command.envelope().epoch(), persisted.getHostId());
          next = ManagedDependencyOperationDispatcher.commandEntity(
              successor, parent.snapshot().type(), persisted.getHostId(),
              persisted.getComponentName());
          phase = "PROVIDER_PROVISIONING";
          operationState = "PROVIDER_PROVISIONING";
        }
        case PROVISION_HDFS_NAMESPACE, PROVISION_ZOOKEEPER_NAMESPACE -> {
          phase = "PROVIDER_PREPARED";
          operationState = "PROVIDER_PREPARED";
          providerPreparationHash = hash(resultJson(result));
        }
        case INVALIDATE_BINDING_EPOCH -> {
          bindingState = "DETACHING";
          phase = "DETACHING";
          operationState = "SUCCEEDED";
        }
        default -> throw new IllegalStateException("Unexpected provider dependency result");
      }
    } else {
      boolean detaching = command.name() == ManagedDependencyCommand.CommandName.INVALIDATE_BINDING_EPOCH;
      boolean reconciling = result.status() == ResultStatus.RECONCILIATION_REQUIRED;
      bindingState = detaching ? "DETACHING"
          : reconciling
              ? "FENCING_UNCERTAIN" : "FAILED";
      phase = detaching ? "DETACHING"
          : reconciling ? "ZOOKEEPER_HANDOFF_RECONCILING" : command.name().name();
      operationState = result.status().name();
    }
    complete(persisted, task, result, bindingState, phase, operationState,
        providerPreparationHash, null, null, null, null, null, null, null, null,
        null, failure, null, next);
    if (next != null) {
      dispatcher.dispatchSafely(next);
    }
  }

  private void completePreparation(ServiceDependencyHostResultEntity persisted,
      ManagedDependencyCommand command, Result result, HostRoleCommand task) {
    ParentState parent = parent(persisted);
    Failure failure = failure(result, command.name().name());
    ServiceDependencyHostResultEntity next = null;
    String bindingState = parent.binding().getState();
    boolean credentialsRequired = parent.snapshot().securityMode()
        == ManagedDependencySecurityMode.KERBEROS;
    String phase = result.status() == ResultStatus.SUCCEEDED
        ? (credentialsRequired ? "CONSUMER_CREDENTIALS_REQUIRED" : "CONSUMER_VERIFYING")
        : command.name().name();
    String operationState = result.status() == ResultStatus.SUCCEEDED
        ? (credentialsRequired ? "CONSUMER_CREDENTIALS_REQUIRED" : "CONSUMER_VERIFYING")
        : result.status().name();
    String observationId = null;
    String observationFingerprint = null;
    if (result.status() == ResultStatus.SUCCEEDED) {
      observationId = UUID.randomUUID().toString();
      observationFingerprint = observationFingerprint(command, result, task, observationId);
      if (!credentialsRequired && DAEMON_COMPONENTS.contains(persisted.getComponentName())) {
        ManagedDependencyCommand verification = ManagedDependencyCommand.verifyConsumer(
            parent.snapshot(), command.envelope().operationId(), command.envelope().epoch(),
            persisted.getHostId(), result.facts().get("client.package.name"),
            result.facts().get("client.package.version"),
            result.facts().get("client.software.semantic.version"),
            result.facts().get("identity.fingerprint"), UUID.fromString(observationId),
            command.envelope().immutableRequestHash(), observationFingerprint);
        next = ManagedDependencyOperationDispatcher.commandEntity(verification,
            parent.snapshot().type(), persisted.getHostId(), persisted.getComponentName());
        phase = "CONSUMER_VERIFYING";
        operationState = "CONSUMER_VERIFYING";
      }
    } else {
      bindingState = "FAILED";
      phase = command.name().name();
      operationState = result.status().name();
    }
    String packageName = result.facts().get("client.package.name");
    String packageVersion = result.facts().get("client.package.version");
    complete(persisted, task, result, bindingState, phase, operationState,
        null, observationId, command.envelope().immutableRequestHash(), observationFingerprint,
        packageName, packageVersion, result.facts().get("client.software.semantic.version"),
        packageName == null ? null : hash(packageName + "\u0000" + packageVersion),
        result.facts().get("client.config.fingerprint"),
        result.facts().get("identity.fingerprint"), failure, null, next);
    if (next != null) {
      dispatcher.dispatchSafely(next);
    }
  }

  private void completeVerification(ServiceDependencyHostResultEntity persisted,
      ManagedDependencyCommand command, Result result, HostRoleCommand task) {
    ParentState parent = parent(persisted);
    Cluster cluster;
    try {
      cluster = clusters.getClusterById(parent.binding().getConsumerClusterId());
    } catch (AmbariException e) {
      throw new IllegalStateException("The HBase verification topology is unavailable", e);
    }
    cluster.executeUnderReadLock(() -> {
      Set<Long> requiredHosts = requiredDaemonHostIds(cluster);
      if (!requiredHosts.contains(persisted.getHostId())) {
        throw new IllegalStateException(
            "Verification result came from a stale HBase daemon assignment");
      }
      Failure failure = failure(result, command.name().name());
      boolean succeeded = result.status() == ResultStatus.SUCCEEDED;
      complete(persisted, task, result,
          succeeded && "CONSUMER_CREDENTIALS_REQUIRED".equals(parent.binding().getState())
              ? "PROVISIONING" : succeeded ? parent.binding().getState() : "FAILED",
          succeeded ? "CONSUMER_VERIFYING" : command.name().name(),
          succeeded ? "CONSUMER_VERIFYING" : result.status().name(), null,
          command.parameters().get("preparation.observation.id"),
          command.parameters().get("preparation.request.hash"),
          command.parameters().get("preparation.observation.fingerprint"),
          result.facts().get("client.package.name"), result.facts().get("client.package.version"),
          result.facts().get("client.software.semantic.version"),
          hash(result.facts().get("client.package.name") + "\u0000"
              + result.facts().get("client.package.version")),
          result.facts().get("client.config.fingerprint"),
          result.facts().get("identity.fingerprint"), failure,
          requiredHosts, null);
      return null;
    });
  }

  private void complete(ServiceDependencyHostResultEntity persisted, HostRoleCommand task,
      Result result, String bindingState, String phase, String operationState,
      String providerPreparationHash, String observationId, String preparationRequestHash,
      String observationFingerprint, String packageName, String packageVersion,
      String softwareVersion, String observedPackageHash, String renderedConfigHash,
      String identityFingerprint, Failure failure, Set<Long> readinessHosts,
      ServiceDependencyHostResultEntity next) {
    String json = resultJson(result);
    CommandCompletion completion = new CommandCompletion(json, hash(json), result.status().name(),
        failure.code(), failure.phase(), failure.message(), failure.retryable(), bindingState,
        phase, operationState, providerPreparationHash, null, null, observationId,
        preparationRequestHash, observationFingerprint, packageName, packageVersion,
        softwareVersion, observedPackageHash, renderedConfigHash, identityFingerprint,
        readinessHosts, next);
    dependencyDAO.completeCommand(ManagedDependencyOperationDispatcher.id(persisted),
        task.getRequestId(), task.getStageId(), task.getTaskId(), completion);
  }

  private void validateReservedTaskIdentity(CommandReport report, String authenticatedHostName,
      HostRoleCommand task, ManagedDependencyCommand command, String rawBundle) {
    boolean provider = ManagedDependencyOperationDispatcher.isProviderCommand(command.name().name());
    boolean preparation = ManagedDependencyOperationDispatcher.isPreparationCommand(command.name().name());
    String hostParameter = provider ? "provider.action.host.id" : "host.id";
    if (task.getTaskId() != report.getTaskId()
        || !authenticatedHostName.equals(task.getHostName())
        || task.getRoleCommand() != (preparation ? RoleCommand.INSTALL : RoleCommand.CUSTOM_COMMAND)
        || !preparation && !command.name().name().equals(task.getCustomCommandName())
        || !preparation && !command.name().name().equals(report.getCustomCommand())
        || !Long.toString(task.getHostId()).equals(command.parameters().get(hostParameter))) {
      throw new IllegalStateException("The reporting task does not own this dependency command");
    }
    if (!provider) {
      requireBounded(rawBundle, "Managed dependency preparation bundle");
      ManagedDependencyCommandBundle bundle = StageUtils.getGson().fromJson(
          rawBundle, ManagedDependencyCommandBundle.class);
      ManagedDependencyCommand selectedPreparation = bundle.preparationCommands().stream()
          .filter(candidate -> candidate.parameters().get("provider.service")
              .equals(command.parameters().get("provider.service")))
          .findFirst().orElseThrow(() -> new IllegalStateException(
              "Verification has no matching preparation command"));
      if (bundle.hostId() != task.getHostId()
          || !command.parameters().get("preparation.request.hash")
              .equals(selectedPreparation.envelope().immutableRequestHash())) {
        throw new IllegalStateException("Verification does not match its preparation lineage");
      }
    }
  }

  private boolean taskIsPreparation(HostRoleCommand task, String rawCommand) {
    if (task.getRoleCommand() != RoleCommand.INSTALL) {
      return false;
    }
    try {
      ManagedDependencyCommand command = StageUtils.getGson().fromJson(rawCommand,
          ManagedDependencyCommand.class);
      return command != null
          && ManagedDependencyOperationDispatcher.isPreparationCommand(command.name().name());
    } catch (RuntimeException e) {
      return false;
    }
  }

  private ServiceDependencyHostResultEntity persisted(HostRoleCommand task,
      ManagedDependencyCommand command, String rawCommand) {
    ServiceDependencyHostResultEntity persisted = dependencyDAO.findHostResult(
        command.envelope().bindingId().toString(), command.envelope().snapshotVersion(),
        command.envelope().epoch(), task.getHostId(), command.parameters().get("provider.service"),
        command.name().name());
    if (persisted == null || !rawCommand.equals(persisted.getCommandJson())
        || !command.envelope().immutableRequestHash().equals(persisted.getCommandRequestHash())) {
      throw new IllegalStateException("The task does not match a persisted dependency command");
    }
    return persisted;
  }

  private ParentState parent(ServiceDependencyHostResultEntity persisted) {
    ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(persisted.getBindingId());
    ServiceDependencyOperationEntity operation = dependencyDAO.findOperation(persisted.getOperationId());
    ServiceDependencySnapshotEntity snapshotEntity = dependencyDAO.findSnapshot(
        persisted.getBindingId(), persisted.getSnapshotVersion());
    if (binding == null || operation == null || snapshotEntity == null) {
      throw new IllegalStateException("The dependency command parent state is missing");
    }
    ManagedDependencySnapshot snapshot = StageUtils.getGson().fromJson(
        snapshotEntity.getSnapshotJson(), ManagedDependencySnapshot.class);
    return new ParentState(binding, operation, snapshot);
  }

  private Set<Long> requiredDaemonHostIds(Cluster cluster) {
    try {
      Service service = cluster.getService("HBASE");
      Set<Long> result = new HashSet<>();
      for (String componentName : DAEMON_COMPONENTS) {
        if (!service.getServiceComponents().containsKey(componentName)) {
          continue;
        }
        service.getServiceComponent(componentName).getServiceComponentHosts().values()
            .forEach(host -> result.add(host.getHost().getHostId()));
      }
      if (result.isEmpty()) {
        throw new IllegalStateException("HBase has no daemon hosts to verify");
      }
      return Set.copyOf(result);
    } catch (AmbariException e) {
      throw new IllegalStateException("The HBase verification topology is unavailable", e);
    }
  }

  private void requireSameResultAndResume(ServiceDependencyHostResultEntity persisted,
      ManagedDependencyCommand command, Result result) {
    if (!persisted.getResultHash().equals(hash(resultJson(result)))) {
      throw new IllegalStateException("A conflicting dependency result was already recorded");
    }
    if (result.status() != ResultStatus.SUCCEEDED) {
      return;
    }
    String nextKind = switch (command.name()) {
      case PREPARE_BINDING_JOURNAL -> "INITIALIZE_BINDING_JOURNAL";
      case INITIALIZE_BINDING_JOURNAL -> "PROVISION_" + persisted.getDependencyType() + "_NAMESPACE";
      case PREPARE_HDFS_CONSUMER -> "VERIFY_HDFS_CONSUMER";
      case PREPARE_ZOOKEEPER_CONSUMER -> "VERIFY_ZOOKEEPER_CONSUMER";
      default -> null;
    };
    if (nextKind != null) {
      ServiceDependencyHostResultEntity next = dependencyDAO.findHostResult(
          persisted.getBindingId(), persisted.getSnapshotVersion(), persisted.getOperationEpoch(),
          persisted.getHostId(),
          persisted.getDependencyType(), nextKind);
      if (next != null && "INTENT".equals(next.getState())) {
        dispatcher.dispatchSafely(next);
      }
    }
  }

  private Result result(String structuredOut) {
    JsonElement value = structuredOutput(structuredOut).get(RESULT_KEY);
    if (value == null || !value.isJsonObject()) {
      throw new IllegalArgumentException("Managed dependency result is missing");
    }
    return StageUtils.getGson().fromJson(value, Result.class);
  }

  private JsonArray preparationResults(String structuredOut) {
    JsonElement value = structuredOutput(structuredOut).get(PREPARATION_RESULTS_KEY);
    if (value == null || !value.isJsonArray() || value.getAsJsonArray().isEmpty()
        || value.getAsJsonArray().size() > 2) {
      throw new IllegalArgumentException("Managed dependency preparation results are missing");
    }
    return value.getAsJsonArray();
  }

  private JsonObject structuredOutput(String value) {
    requireBounded(value, "Managed dependency structured output");
    JsonElement parsed = JsonParser.parseString(value);
    if (!parsed.isJsonObject()) {
      throw new IllegalArgumentException("Managed dependency structured output must be an object");
    }
    return parsed.getAsJsonObject();
  }

  private void validateTaskOutcome(CommandReport report, Result result) {
    if (result.status() == ResultStatus.SUCCEEDED
        && !HostRoleStatus.COMPLETED.name().equals(report.getStatus())) {
      throw new IllegalStateException("A failed Ambari task cannot publish successful dependency evidence");
    }
  }

  private Failure failure(Result result, String phase) {
    return result.status() == ResultStatus.SUCCEEDED
        ? new Failure(null, null, null, false)
        : new Failure(result.errorCode().name(), phase, result.errorMessage(),
            result.status() == ResultStatus.FAILED);
  }

  private String observationFingerprint(ManagedDependencyCommand command, Result result,
      HostRoleCommand task, String observationId) {
    return hash(observationId + "\u0000" + command.envelope().immutableRequestHash()
        + "\u0000" + task.getRequestId() + "\u0000" + task.getStageId() + "\u0000"
        + task.getTaskId() + "\u0000" + resultJson(result));
  }

  private String resultJson(Result result) {
    return StageUtils.getGson().toJson(result);
  }

  private boolean isCompleted(String status) {
    try {
      return HostRoleStatus.valueOf(status).isCompletedState();
    } catch (RuntimeException e) {
      return false;
    }
  }

  private void requireBounded(String value, String field) {
    if (value == null || value.getBytes(StandardCharsets.UTF_8).length > MAX_STRUCTURED_OUTPUT_BYTES) {
      throw new IllegalArgumentException(field + " is missing or too large");
    }
  }

  private String hash(String value) {
    try {
      return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
    }
  }

  private record ParentState(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity operation, ManagedDependencySnapshot snapshot) { }

  private record Failure(String code, String phase, String message, boolean retryable) { }

  private record PreparationEvidence(ServiceDependencyHostResultEntity persisted,
      ManagedDependencyCommand command, Result result) { }
}
