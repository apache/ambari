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
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.persistence.PersistenceException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.PersistKeyValueImpl;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Consumer;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Provider;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity.Allocation;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseSecurityDescriptorAdapter;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySnapshotValidator.Issue;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySnapshotValidator.ValidationResult;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CreationGuard;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.DraftGuard;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.LifecycleTransition;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.UpdateFacts;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.RepositoryGuard;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.ServiceVersionGuard;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.StaleApprovalException;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CreationBatchResult;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CreationItem;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CreationResult;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyFenceEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyOperationEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.utils.StageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Authorized persistence boundary for managed HBase dependencies. */
@Singleton
public class ManagedServiceDependencyCoordinator {
  public static final int PREVIEW_SCHEMA_VERSION = ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION;
  private static final String CONSUMER_SERVICE = "HBASE";

  @Inject
  private com.google.inject.Provider<ManagedDependencyTaskResultProcessor> taskResults;

  private final ManagedDependencyDescriptorResolver resolver;
  private final ServiceDependencyDAO dependencyDAO;
  private final PersistKeyValueImpl persistKeyValue;
  private final ManagedDependencySnapshotValidator validator;
  private final ManagedDependencyLifecyclePolicy lifecyclePolicy;
  private com.google.inject.Provider<ManagedDependencyOperationDispatcher> operationDispatcher;
  private com.google.inject.Provider<ManagedDependencyRuntimePlanner> runtimePlanner;
  @Inject private com.google.inject.Provider<org.apache.ambari.server.actionmanager.ActionManager> actions;

  @Inject
  public ManagedServiceDependencyCoordinator(ManagedDependencyDescriptorResolver resolver,
      ServiceDependencyDAO dependencyDAO, PersistKeyValueImpl persistKeyValue,
      ManagedDependencyLifecyclePolicy lifecyclePolicy) {
    this.resolver = resolver;
    this.dependencyDAO = dependencyDAO;
    this.persistKeyValue = persistKeyValue;
    this.lifecyclePolicy = lifecyclePolicy;
    validator = new ManagedDependencySnapshotValidator(true);
  }

  /** Compatibility constructor for focused unit tests and non-Guice callers. */
  public ManagedServiceDependencyCoordinator(ManagedDependencyDescriptorResolver resolver,
      ServiceDependencyDAO dependencyDAO, PersistKeyValueImpl persistKeyValue) {
    this(resolver, dependencyDAO, persistKeyValue,
        new ManagedDependencyLifecyclePolicy(dependencyDAO));
  }

  @Inject
  void setOperationDispatcher(com.google.inject.Provider<ManagedDependencyOperationDispatcher> operationDispatcher) {
    this.operationDispatcher = operationDispatcher;
  }

  @Inject
  void setRuntimePlanner(com.google.inject.Provider<ManagedDependencyRuntimePlanner> runtimePlanner) {
    this.runtimePlanner = runtimePlanner;
  }

  public List<Map<String, Object>> candidates(ConsumerReference consumerReference,
      ManagedDependencyType type) {
    Consumer consumer = resolveConsumer(consumerReference, ReadLevel.VIEW);
    if (consumerReference.draftId() == null && !consumerReference.servicePlan()) {
      consumer = resolver.resolveService(new ManagedDependencyServiceKey(
          consumer.clusterId(), CONSUMER_SERVICE), type);
    }
    Consumer selectedConsumer = consumer;
    List<Map<String, Object>> candidates = new ArrayList<>();
    resolver.allClusters().values().stream()
        .sorted(Comparator.comparingLong(Cluster::getClusterId))
        .filter(cluster -> cluster.getServices().containsKey(type.getProviderServiceName()))
        .filter(cluster -> can(cluster, RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE))
        .forEach(cluster -> candidates.add(candidate(consumerReference, selectedConsumer, cluster, type)));
    return candidates;
  }

  public Map<String, Object> preview(ConsumerReference consumerReference,
      ManagedDependencyType type, ProviderReference providerReference) {
    return preview(consumerReference, type, providerReference, null);
  }

  public Map<String, Object> preview(ConsumerReference consumerReference,
      ManagedDependencyType type, ProviderReference providerReference, UUID requestedBindingId) {
    Consumer consumer = resolveConsumer(consumerReference, ReadLevel.MODIFY);
    Provider provider = resolveProvider(providerReference, type, true);
    UUID bindingId = requestedBindingId == null ? UUID.randomUUID() : requestedBindingId;
    if (requestedBindingId != null
        && (dependencyDAO.findBinding(bindingId.toString()) != null
            || dependencyDAO.findFence(bindingId.toString()) != null)) {
      throw new ManagedDependencyIntegrationException(409, "BINDING_ID_UNAVAILABLE",
          "The requested binding UUID is unavailable; use the existing binding recovery route or a new UUID.");
    }
    ValidationResult result = validate(bindingId, type, consumer, provider);
    return previewResponse(bindingId, consumer, provider, result);
  }

  /**
   * Resolves a complete one- or two-provider selection with one authoritative security
   * calculation. The returned item DTOs retain the legacy preview shape so callers can
   * persist each immutable request without exposing the transient adapter result.
   */
  public Map<String, Object> preview(ConsumerReference consumerReference,
      List<PreviewSelection> selections) {
    List<PreviewSelection> input = validatePreviewSelections(selections);
    Consumer authorizedConsumer = resolveConsumer(consumerReference, ReadLevel.MODIFY);
    SortedMap<ManagedDependencyType, ProviderReference> providerReferences = new java.util.TreeMap<>();
    Map<ManagedDependencyType, Cluster> providerClusters = new java.util.TreeMap<>();
    SortedMap<ManagedDependencyType, UUID> bindingIds = new java.util.TreeMap<>();
    Set<UUID> usedBindingIds = new HashSet<>();
    for (PreviewSelection selection : input) {
      UUID bindingId = selection.bindingId() == null ? UUID.randomUUID() : selection.bindingId();
      if (!usedBindingIds.add(bindingId)) {
        throw new ManagedDependencyIntegrationException(409, "BINDING_ID_UNAVAILABLE",
            "A selected binding UUID is unavailable; review the dependency selection again.");
      }
      bindingIds.put(selection.type(), bindingId);
      providerReferences.put(selection.type(), selection.provider());
    }
    for (PreviewSelection selection : input) {
      providerClusters.put(selection.type(),
          authorizeProviderParent(selection.provider(), selection.type(), true));
    }
    for (UUID bindingId : bindingIds.values()) {
      if (dependencyDAO.findBinding(bindingId.toString()) != null
          || dependencyDAO.findFence(bindingId.toString()) != null) {
        throw new ManagedDependencyIntegrationException(409, "BINDING_ID_UNAVAILABLE",
            "A selected binding UUID is unavailable; review the dependency selection again.");
      }
    }
    if (authorizedConsumer.clusterId() == null) {
      return withClusterReadLocks(providerClusters.values(), () -> {
        SortedMap<ManagedDependencyType, Provider> providers = new java.util.TreeMap<>();
        providerReferences.forEach((type, provider) ->
            providers.put(type, resolver.resolveProvider(provider.serviceKey())));
        return legacyPreviewItems(input, authorizedConsumer, providers, bindingIds);
      });
    }
    Cluster consumerCluster = resolver.cluster(authorizedConsumer.clusterId());
    List<Cluster> parents = new ArrayList<>();
    parents.add(consumerCluster);
    parents.addAll(providerClusters.values());
    return withClusterReadLocks(parents, () -> completePreviewItems(
        consumerReference, authorizedConsumer, consumerCluster, input, providerReferences,
        bindingIds));
  }

  private Map<String, Object> completePreviewItems(ConsumerReference reference,
      Consumer authorizedConsumer, Cluster consumerCluster, List<PreviewSelection> input,
      SortedMap<ManagedDependencyType, ProviderReference> providerReferences,
      SortedMap<ManagedDependencyType, UUID> bindingIds) {
    SortedMap<ManagedDependencyType, Provider> providers = new java.util.TreeMap<>();
    providerReferences.forEach((type, provider) ->
        providers.put(type, resolver.resolveProvider(provider.serviceKey())));
    Map<ManagedDependencyType, Consumer> consumers = typedConsumers(reference,
        authorizedConsumer, input);
    if (authorizedConsumer.securityMode() != ManagedDependencySecurityMode.KERBEROS) {
      return legacyPreviewItems(input, authorizedConsumer, providers, bindingIds, consumers);
    }
    ManagedDependencyDescriptorResolver.CompleteSelection selection = completeSelection(
        bindingIds, providers);
    ManagedHBaseSecurityDescriptorAdapter.Resolution resolution =
        resolver.resolveSecureSelection(consumerCluster, authorizedConsumer, selection,
            reference.servicePlan() ? reference.expectedDraftRevision() : null);
    List<ManagedDependencyNamespace> namespaces = new ArrayList<>(existingNamespaces());
    List<Allocation> allocations = new ArrayList<>(existingAllocations());
    List<Map<String, Object>> items = new ArrayList<>();
    for (PreviewSelection selected : input) {
      ManagedDependencyType type = selected.type();
      Consumer consumer = consumers.get(type);
      Provider provider = providers.get(type);
      ValidationResult result = validator.validate(bindingIds.get(type), 1L, type, consumer,
          provider, namespaces, allocations, currentSnapshot(consumer, type),
          resolution.contextFor(type));
      items.add(previewResponse(bindingIds.get(type), consumer, provider, result));
      result.snapshot().ifPresent(snapshot -> namespaces.add(snapshot.namespace()));
    }
    return Map.of("items", List.copyOf(items));
  }

  private Map<String, Object> legacyPreviewItems(List<PreviewSelection> input, Consumer consumer,
      SortedMap<ManagedDependencyType, Provider> providers,
      SortedMap<ManagedDependencyType, UUID> bindingIds) {
    return legacyPreviewItems(input, consumer, providers, bindingIds,
        input.stream().collect(Collectors.toMap(PreviewSelection::type, ignored -> consumer,
            (first, ignored) -> first, java.util.TreeMap::new)));
  }

  private Map<String, Object> legacyPreviewItems(List<PreviewSelection> input, Consumer fallback,
      SortedMap<ManagedDependencyType, Provider> providers,
      SortedMap<ManagedDependencyType, UUID> bindingIds,
      Map<ManagedDependencyType, Consumer> consumers) {
    List<Map<String, Object>> items = new ArrayList<>();
    for (PreviewSelection selected : input) {
      ManagedDependencyType type = selected.type();
      Consumer consumer = consumers.getOrDefault(type, fallback);
      ValidationResult result = validate(bindingIds.get(type), type, consumer, providers.get(type));
      items.add(previewResponse(bindingIds.get(type), consumer, providers.get(type), result));
    }
    return Map.of("items", List.copyOf(items));
  }

  private Map<ManagedDependencyType, Consumer> typedConsumers(ConsumerReference reference,
      Consumer authorizedConsumer, List<PreviewSelection> selections) {
    if (reference.draftId() != null || reference.servicePlan()) {
      return selections.stream().collect(Collectors.toMap(PreviewSelection::type,
          ignored -> authorizedConsumer, (first, ignored) -> first, java.util.TreeMap::new));
    }
    return selections.stream().collect(Collectors.toMap(PreviewSelection::type,
        selected -> resolver.resolveService(new ManagedDependencyServiceKey(
            authorizedConsumer.clusterId(), CONSUMER_SERVICE), selected.type()),
        (first, ignored) -> first, java.util.TreeMap::new));
  }

  private ManagedDependencyDescriptorResolver.CompleteSelection completeSelection(
      SortedMap<ManagedDependencyType, UUID> bindingIds,
      SortedMap<ManagedDependencyType, Provider> providers) {
    SortedMap<ManagedDependencyType, ManagedDependencyDescriptorResolver.BindingSelection> selected =
        new java.util.TreeMap<>();
    for (ManagedDependencyType type : bindingIds.keySet()) {
      selected.put(type, new ManagedDependencyDescriptorResolver.BindingSelection(bindingIds.get(type),
          providers.get(type).serviceKey()));
    }
    return new ManagedDependencyDescriptorResolver.CompleteSelection(selected);
  }

  private List<PreviewSelection> validatePreviewSelections(List<PreviewSelection> selections) {
    if (selections == null || selections.isEmpty() || selections.size() > 2) {
      throw badRequest("INVALID_MANAGED_DEPENDENCY_PLAN",
          "A dependency preview must contain one or two selections.");
    }
    Set<ManagedDependencyType> types = java.util.EnumSet.noneOf(ManagedDependencyType.class);
    Set<UUID> ids = new HashSet<>();
    List<PreviewSelection> copy = List.copyOf(selections);
    for (PreviewSelection selection : copy) {
      Objects.requireNonNull(selection, "selection");
      if (!types.add(selection.type())) {
        throw badRequest("INVALID_MANAGED_DEPENDENCY_PLAN",
            "Dependency types must be unique in a complete selection.");
      }
      if (selection.bindingId() != null && !ids.add(selection.bindingId())) {
        throw badRequest("INVALID_MANAGED_DEPENDENCY_PLAN",
            "Binding IDs must be unique in a complete selection.");
      }
    }
    return copy;
  }

  /**
   * Resolves one caller-supplied advisor selection to authoritative server facts.
   * Prospective scopes use the same authorization and validation as preview;
   * installed services must refer to the exact active approved binding.
   */
  public AdvisorSelection authorizeAdvisorSelection(ConsumerReference consumerReference,
      ManagedDependencyType type, ProviderReference providerReference, UUID bindingId) {
    Objects.requireNonNull(consumerReference, "consumerReference");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(providerReference, "providerReference");
    Objects.requireNonNull(bindingId, "bindingId");

    boolean prospective = consumerReference.draftId() != null || consumerReference.servicePlan();
    Consumer consumer = resolveConsumer(consumerReference,
        prospective ? ReadLevel.MODIFY : ReadLevel.VIEW);
    Provider provider = resolveProvider(providerReference, type, prospective);
    ManagedDependencySnapshot snapshot;
    if (prospective) {
      if (dependencyDAO.findBinding(bindingId.toString()) != null
          || dependencyDAO.findFence(bindingId.toString()) != null) {
        throw new ManagedDependencyIntegrationException(409, "BINDING_ID_UNAVAILABLE",
            "The requested binding UUID is unavailable; review the dependency selection again.");
      }
      ValidationResult result = validate(bindingId, type, consumer, provider);
      if (!result.isValid()) {
        throw issue(result.issues().get(0));
      }
      snapshot = result.snapshot().orElseThrow();
    } else {
      ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(bindingId.toString());
      if (binding == null
          || !Objects.equals(binding.getConsumerClusterId(), consumer.clusterId())
          || !CONSUMER_SERVICE.equals(binding.getConsumerServiceName())
          || binding.getProviderClusterId() != providerReference.clusterId()
          || !providerReference.serviceName().equals(binding.getProviderServiceName())
          || !type.name().equals(binding.getDependencyType())
          || !"APPROVED".equals(binding.getSnapshotApproval())
          || Set.of("STALE", "FENCING_UNCERTAIN", "DETACHING", "RETIRED")
              .contains(binding.getState())) {
        throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_PREVIEW_STALE",
            "The managed dependency selection is no longer an active approved binding.");
      }
      snapshot = validateDispatchState(binding);
    }
    if (snapshot.schemaVersion() != PREVIEW_SCHEMA_VERSION) {
      throw new ManagedDependencyIntegrationException(409,
          ManagedDependencyErrorCode.DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED.name(),
          "The managed dependency must be reviewed with the current preview schema.");
    }
    return new AdvisorSelection(bindingId, type, consumer.clusterId(), consumer.serviceName(),
        provider.serviceKey().clusterId(), provider.serviceKey().serviceName(),
        PREVIEW_SCHEMA_VERSION, consumer.version().stackName(), consumer.version().stackVersion(),
        snapshot.providerFingerprint(), snapshot.consumerFingerprint(),
        snapshot.snapshotFingerprint());
  }

  /**
   * Authorizes a complete advisor selection against one shared security calculation.
   * Existing live bindings retain their current approved snapshot; a fresh INIT live
   * consumer may supply a new binding identity for the same request-local calculation.
   */
  public List<AdvisorSelection> authorizeAdvisorSelections(ConsumerReference consumerReference,
      List<AdvisorSelectionRequest> selections) {
    List<AdvisorSelectionRequest> input = validateAdvisorSelections(selections);
    boolean prospective = consumerReference.draftId() != null || consumerReference.servicePlan();
    Consumer authorizedConsumer = resolveConsumer(consumerReference,
        prospective ? ReadLevel.MODIFY : ReadLevel.VIEW);
    SortedMap<ManagedDependencyType, ProviderReference> providerReferences = new java.util.TreeMap<>();
    Map<ManagedDependencyType, Cluster> providerClusters = new java.util.TreeMap<>();
    for (AdvisorSelectionRequest selection : input) {
      providerReferences.put(selection.type(), selection.provider());
      providerClusters.put(selection.type(), authorizeProviderParent(
          selection.provider(), selection.type(), prospective));
    }
    List<Cluster> parents = new ArrayList<>();
    if (authorizedConsumer.clusterId() != null) {
      parents.add(resolver.cluster(authorizedConsumer.clusterId()));
    }
    parents.addAll(providerClusters.values());
    if (parents.isEmpty()) {
      return authorizeAdvisorSelectionsLocked(consumerReference, authorizedConsumer,
          input, providerReferences);
    }
    return withClusterReadLocks(parents, () -> authorizeAdvisorSelectionsLocked(
        consumerReference, authorizedConsumer, input, providerReferences));
  }

  private List<AdvisorSelection> authorizeAdvisorSelectionsLocked(
      ConsumerReference reference, Consumer authorizedConsumer,
      List<AdvisorSelectionRequest> input,
      SortedMap<ManagedDependencyType, ProviderReference> providerReferences) {
    SortedMap<ManagedDependencyType, Provider> providers = new java.util.TreeMap<>();
    providerReferences.forEach((type, provider) ->
        providers.put(type, resolver.resolveProvider(provider.serviceKey())));
    Map<ManagedDependencyType, Consumer> consumers = typedConsumers(reference,
        authorizedConsumer, input.stream().map(selection -> new PreviewSelection(
            selection.type(), selection.provider(), selection.bindingId())).toList());
    ManagedHBaseSecurityDescriptorAdapter.Resolution resolution = null;
    if (authorizedConsumer.clusterId() != null
        && authorizedConsumer.securityMode() == ManagedDependencySecurityMode.KERBEROS
        && reference.draftId() == null) {
      SortedMap<ManagedDependencyType, UUID> ids = input.stream().collect(Collectors.toMap(
          AdvisorSelectionRequest::type, AdvisorSelectionRequest::bindingId,
          (first, ignored) -> first, java.util.TreeMap::new));
      resolution = resolver.resolveSecureSelection(resolver.cluster(authorizedConsumer.clusterId()),
          authorizedConsumer, completeSelection(ids, providers),
          reference.servicePlan() ? reference.expectedDraftRevision() : null);
    }

    List<AdvisorSelection> result = new ArrayList<>();
    List<ManagedDependencyNamespace> namespaces = new ArrayList<>(existingNamespaces());
    List<Allocation> allocations = new ArrayList<>(existingAllocations());
    for (AdvisorSelectionRequest selected : input) {
      ManagedDependencyType type = selected.type();
      Consumer consumer = consumers.get(type);
      Provider provider = providers.get(type);
      ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(
          selected.bindingId().toString());
      ManagedDependencySnapshot snapshot;
      if (binding != null) {
        requireAdvisorBinding(binding, consumer, selected);
        snapshot = validateDispatchState(binding);
      } else {
        if (consumer.lifecycle() != ManagedDependencyDescriptor.ConsumerLifecycle.INIT_UNINSTALLED
            && !reference.servicePlan()) {
          throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_PREVIEW_STALE",
              "The advisor selection is not an active approved binding or fresh INIT consumer.");
        }
        ValidationResult validation = resolution == null
            ? validate(selected.bindingId(), type, consumer, provider)
            : validator.validate(selected.bindingId(), 1L, type, consumer, provider,
                namespaces, allocations, null, resolution.contextFor(type));
        if (!validation.isValid()) {
          throw issue(validation.issues().get(0));
        }
        snapshot = validation.snapshot().orElseThrow();
      }
      if (snapshot.schemaVersion() != PREVIEW_SCHEMA_VERSION) {
        throw new ManagedDependencyIntegrationException(409,
            ManagedDependencyErrorCode.DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED.name(),
            "The managed dependency must be reviewed with the current preview schema.");
      }
      result.add(advisorSelection(selected.bindingId(), type, consumer, provider, snapshot));
      namespaces.add(snapshot.namespace());
    }
    return List.copyOf(result);
  }

  private void requireAdvisorBinding(ServiceDependencyBindingEntity binding, Consumer consumer,
      AdvisorSelectionRequest selected) {
    if (!Objects.equals(binding.getConsumerClusterId(), consumer.clusterId())
        || !CONSUMER_SERVICE.equals(binding.getConsumerServiceName())
        || binding.getProviderClusterId() != selected.provider().clusterId()
        || !selected.provider().serviceName().equals(binding.getProviderServiceName())
        || !selected.type().name().equals(binding.getDependencyType())
        || !"APPROVED".equals(binding.getSnapshotApproval())
        || Set.of("STALE", "FENCING_UNCERTAIN", "DETACHING", "RETIRED")
            .contains(binding.getState())) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_PREVIEW_STALE",
          "The managed dependency selection is no longer an active approved binding.");
    }
  }

  private AdvisorSelection advisorSelection(UUID bindingId, ManagedDependencyType type,
      Consumer consumer, Provider provider, ManagedDependencySnapshot snapshot) {
    return new AdvisorSelection(bindingId, type, consumer.clusterId(), consumer.serviceName(),
        provider.serviceKey().clusterId(), provider.serviceKey().serviceName(),
        PREVIEW_SCHEMA_VERSION, consumer.version().stackName(), consumer.version().stackVersion(),
        snapshot.providerFingerprint(), snapshot.consumerFingerprint(),
        snapshot.snapshotFingerprint());
  }

  private List<AdvisorSelectionRequest> validateAdvisorSelections(
      List<AdvisorSelectionRequest> selections) {
    if (selections == null || selections.isEmpty() || selections.size() > 2) {
      throw badRequest("INVALID_MANAGED_DEPENDENCY_PLAN",
          "An advisor plan must contain one or two selections.");
    }
    Set<ManagedDependencyType> types = java.util.EnumSet.noneOf(ManagedDependencyType.class);
    Set<UUID> ids = new HashSet<>();
    List<AdvisorSelectionRequest> copy = List.copyOf(selections);
    for (AdvisorSelectionRequest selection : copy) {
      Objects.requireNonNull(selection, "advisor selection");
      if (!types.add(selection.type()) || !ids.add(selection.bindingId())) {
        throw badRequest("INVALID_MANAGED_DEPENDENCY_PLAN",
            "Advisor types and binding IDs must be unique.");
      }
    }
    return copy;
  }

  public Map<String, Object> create(String clusterName, String serviceName, CreateRequest request) {
    Cluster consumerCluster = exactConsumer(clusterName, serviceName, ReadLevel.MODIFY);
    Cluster providerCluster = authorizeProviderParent(request.provider(), request.type(), true);
    Map<String, Object> existing = reconcileExactRetry(consumerCluster.getClusterId(), request);
    if (existing != null) {
      return existing;
    }
    return withClusterReadLocks(consumerCluster, providerCluster,
        () -> createLocked(consumerCluster, request));
  }

  /**
   * Atomically creates a complete dependency plan. Parent authorization is performed
   * before recovery reads, and the resolver/adapter is invoked once for the full secure
   * provider set while all participating cluster read locks are held.
   */
  public List<Map<String, Object>> create(String clusterName, String serviceName,
      List<CreateRequest> requests) {
    List<CreateRequest> input = validateCreateRequests(requests);
    Cluster consumerCluster = exactConsumer(clusterName, serviceName, ReadLevel.MODIFY);
    Map<ManagedDependencyType, Cluster> providerClusters = new java.util.TreeMap<>();
    for (CreateRequest request : input) {
      providerClusters.put(request.type(),
          authorizeProviderParent(request.provider(), request.type(), true));
    }
    List<Cluster> parents = new ArrayList<>();
    parents.add(consumerCluster);
    parents.addAll(providerClusters.values());
    return withClusterReadLocks(parents, () -> {
      List<Map<String, Object>> replay = reconcileBatchExactRetry(
          consumerCluster.getClusterId(), input);
      if (replay != null) {
        return replay;
      }

      Consumer authorizedConsumer = resolver.resolveService(
          new ManagedDependencyServiceKey(consumerCluster.getClusterId(), CONSUMER_SERVICE));
      rejectSecureCreationExpansion(consumerCluster, authorizedConsumer);
      SortedMap<ManagedDependencyType, Provider> providers = new java.util.TreeMap<>();
      SortedMap<ManagedDependencyType, UUID> bindingIds = new java.util.TreeMap<>();
      for (CreateRequest request : input) {
        providers.put(request.type(), resolver.resolveProvider(request.provider().serviceKey()));
        bindingIds.put(request.type(), request.bindingId());
      }
      List<PreviewSelection> selections = input.stream().map(request ->
          new PreviewSelection(request.type(), request.provider(), request.bindingId())).toList();
      Map<ManagedDependencyType, Consumer> consumers = typedConsumers(
          ConsumerReference.service(consumerCluster.getClusterId()), authorizedConsumer, selections);
      Map<ManagedDependencyType, ManagedDependencySnapshot> snapshots = resolveCreateSnapshots(
          ConsumerReference.service(consumerCluster.getClusterId()), authorizedConsumer,
          consumerCluster, input, selections, consumers, providers, bindingIds);

      int userId = authenticatedUserId();
      List<CreationItem> items = new ArrayList<>();
      for (CreateRequest request : input) {
        Consumer consumer = consumers.get(request.type());
        Provider provider = providers.get(request.type());
        ManagedDependencySnapshot snapshot = snapshots.get(request.type());
        String requestHash = immutableRequestHash(consumerCluster.getClusterId(), request);
        ServiceDependencyBindingEntity binding = bindingEntity(
            consumerCluster, provider, snapshot, request, userId);
        ServiceDependencySnapshotEntity snapshotEntity =
            snapshotEntity(snapshot, consumer, provider, userId);
        ServiceDependencyOperationEntity operation = operationEntity(request, requestHash);
        ServiceDependencyHostResultEntity command = operationDispatcher == null ? null
            : operationDispatcher.get().initialProviderCommand(binding, snapshot, operation);
        items.add(new CreationItem(binding, snapshotEntity, operation, command,
            creationGuard(consumerCluster, consumer, provider, request, userId)));
      }

      CreationBatchResult created;
      try {
        created = dependencyDAO.createBatch(items);
      } catch (StaleApprovalException e) {
        throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_PREVIEW_STALE",
            "Provider, consumer, or draft facts changed after preview; review the dependency again.", e);
      } catch (PersistenceException | IllegalStateException e) {
        List<Map<String, Object>> reconciled = reconcileBatchExactRetry(
            consumerCluster.getClusterId(), input);
        if (reconciled != null) {
          return reconciled;
        }
        throw e;
      }
      List<Map<String, Object>> responses = new ArrayList<>();
      for (CreationResult result : created.items()) {
        responses.add(bindingResponse(result.binding(), result.snapshot(), result.operation()));
      }
      if (operationDispatcher != null) {
        for (CreationResult result : created.items()) {
          if (result.created()) {
            operationDispatcher.get().dispatchInitial(result.binding().getBindingId());
          }
        }
      }
      return List.copyOf(responses);
    });
  }

  private List<CreateRequest> validateCreateRequests(List<CreateRequest> requests) {
    if (requests == null || requests.isEmpty() || requests.size() > 2) {
      throw badRequest("INVALID_MANAGED_DEPENDENCY_PLAN",
          "A dependency plan must contain one or two CreateRequest items.");
    }
    Set<ManagedDependencyType> types = java.util.EnumSet.noneOf(ManagedDependencyType.class);
    Set<UUID> bindings = new HashSet<>();
    Set<UUID> operations = new HashSet<>();
    CreateRequest first = Objects.requireNonNull(requests.get(0), "create request");
    DraftReference draft = first.draft();
    for (int index = 0; index < requests.size(); index++) {
      CreateRequest request = requests.get(index);
      Objects.requireNonNull(request, "create request");
      if (!types.add(request.type()) || !bindings.add(request.bindingId())
          || !operations.add(request.operationId())) {
        throw badRequest("INVALID_MANAGED_DEPENDENCY_PLAN",
            "Dependency types, binding IDs, and operation IDs must be unique.");
      }
      if (index > 0 && !Objects.equals(draft, request.draft())) {
        throw badRequest("INVALID_MANAGED_DEPENDENCY_PLAN",
            "All dependency items must use the same draft reference.");
      }
    }
    return List.copyOf(requests);
  }

  private Map<ManagedDependencyType, ManagedDependencySnapshot> resolveCreateSnapshots(
      ConsumerReference reference, Consumer authorizedConsumer, Cluster consumerCluster,
      List<CreateRequest> requests, List<PreviewSelection> selections,
      Map<ManagedDependencyType, Consumer> consumers,
      SortedMap<ManagedDependencyType, Provider> providers,
      SortedMap<ManagedDependencyType, UUID> bindingIds) {
    for (CreateRequest request : requests) {
      if (request.previewSchemaVersion() != PREVIEW_SCHEMA_VERSION) {
        throw unprocessable("DEPENDENCY_PREVIEW_STALE",
            "Repeat dependency preview using the current server schema.");
      }
    }
    ManagedHBaseSecurityDescriptorAdapter.Resolution resolution = null;
    if (authorizedConsumer.securityMode() == ManagedDependencySecurityMode.KERBEROS) {
      resolution = resolver.resolveSecureSelection(consumerCluster,
          authorizedConsumer, completeSelection(bindingIds, providers), null);
    }
    List<ManagedDependencyNamespace> namespaces = new ArrayList<>(existingNamespaces());
    List<Allocation> allocations = new ArrayList<>(existingAllocations());
    Map<ManagedDependencyType, ManagedDependencySnapshot> snapshots = new java.util.TreeMap<>();
    for (CreateRequest request : requests) {
      Consumer consumer = consumers.get(request.type());
      Provider provider = providers.get(request.type());
      if (request.draft() != null) {
        verifyDraftMaterialization(consumerCluster, consumer, request.draft());
      }
      ValidationResult result = resolution == null
          ? validate(request.bindingId(), request.type(), consumer, provider)
          : validator.validate(request.bindingId(), 1L, request.type(), consumer, provider,
              namespaces, allocations, currentSnapshot(consumer, request.type()),
              resolution.contextFor(request.type()));
      if (!result.isValid()) {
        throw issue(result.issues().get(0));
      }
      ManagedDependencySnapshot snapshot = result.snapshot().orElseThrow();
      if (!snapshot.providerFingerprint().equals(request.expectedProviderFingerprint())
          || !snapshot.consumerFingerprint().equals(request.expectedConsumerFingerprint())
          || !snapshot.snapshotFingerprint().equals(request.expectedSnapshotFingerprint())) {
        throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_PREVIEW_STALE",
            "Provider or consumer facts changed after preview; review the dependency again.");
      }
      snapshots.put(request.type(), snapshot);
      namespaces.add(snapshot.namespace());
    }
    for (ManagedDependencySnapshot snapshot : snapshots.values()) {
      resolver.validateEffectiveConsumerConfig(consumerCluster, snapshot);
    }
    return snapshots;
  }

  private void rejectSecureCreationExpansion(Cluster consumerCluster, Consumer consumer) {
    if (consumer.securityMode() != ManagedDependencySecurityMode.KERBEROS) {
      return;
    }
    if (!dependencyDAO.findByConsumer(consumerCluster.getClusterId(), CONSUMER_SERVICE).isEmpty()) {
      throw new ManagedDependencyIntegrationException(409,
          "DEPENDENCY_SECURITY_PLAN_INCOMPLETE",
          "An existing secure dependency requires a complete approved plan before another type can be added.");
    }
  }

  private List<Map<String, Object>> reconcileBatchExactRetry(long consumerClusterId,
      List<CreateRequest> requests) {
    List<ServiceDependencyOperationEntity> operations = requests.stream()
        .map(request -> dependencyDAO.findOperation(request.operationId().toString())).toList();
    boolean any = operations.stream().anyMatch(Objects::nonNull);
    if (!any) {
      return null;
    }
    if (operations.stream().anyMatch(Objects::isNull)) {
      throw new ManagedDependencyIntegrationException(409, "OPERATION_ID_CONFLICT",
          "A dependency plan was only partially materialized; recover the complete original plan.");
    }
    List<Map<String, Object>> responses = new ArrayList<>();
    for (int i = 0; i < requests.size(); i++) {
      CreateRequest request = requests.get(i);
      ServiceDependencyOperationEntity operation = operations.get(i);
      ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(request.bindingId().toString());
      ServiceDependencySnapshotEntity snapshot = binding == null ? null
          : dependencyDAO.findSnapshot(binding.getBindingId(), operation.getTargetSnapshotVersion());
      String requestHash = immutableRequestHash(consumerClusterId, request);
      if (binding == null || snapshot == null || !"CREATE".equals(operation.getOperationKind())
          || !request.bindingId().toString().equals(operation.getBindingId())
          || !requestHash.equals(operation.getRequestHash())
          || !sameImmutableSpec(binding, snapshot, consumerClusterId, request)) {
        throw new ManagedDependencyIntegrationException(409, "OPERATION_ID_CONFLICT",
            "The dependency plan does not match its original immutable CREATE.");
      }
      List<ServiceDependencyHostResultEntity> commands = dependencyDAO.findHostResults(
          binding.getBindingId(), operation.getTargetSnapshotVersion(),
          operation.getOperationEpoch()).stream()
          .filter(command -> ManagedDependencyOperationDispatcher.isProviderCommand(
              command.getCheckKind())).toList();
      if (commands.size() > 1) {
        throw new ManagedDependencyIntegrationException(409, "OPERATION_ID_CONFLICT",
            "The original dependency CREATE contains conflicting provider intents.");
      }
      Map<String, Object> response = bindingResponse(binding, snapshot, operation);
      responses.add(response);
    }
    return List.copyOf(responses);
  }

  private Map<String, Object> createLocked(Cluster cluster, CreateRequest request) {
    Consumer consumer = resolver.resolveService(
        new ManagedDependencyServiceKey(cluster.getClusterId(), CONSUMER_SERVICE));
    Provider provider = resolver.resolveProvider(request.provider().serviceKey());
    if (request.previewSchemaVersion() != PREVIEW_SCHEMA_VERSION) {
      throw unprocessable("DEPENDENCY_PREVIEW_STALE",
          "Repeat dependency preview using the current server schema.");
    }
    if (request.draft() != null) {
      verifyDraftMaterialization(cluster, consumer, request.draft());
    }
    ValidationResult result = validate(request.bindingId(), request.type(), consumer, provider);
    if (!result.isValid()) {
      throw issue(result.issues().get(0));
    }
    ManagedDependencySnapshot snapshot = result.snapshot().orElseThrow();
    if (!snapshot.providerFingerprint().equals(request.expectedProviderFingerprint())
        || !snapshot.consumerFingerprint().equals(request.expectedConsumerFingerprint())
        || !snapshot.snapshotFingerprint().equals(request.expectedSnapshotFingerprint())) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_PREVIEW_STALE",
          "Provider or consumer facts changed after preview; review the dependency again.");
    }
    resolver.validateEffectiveConsumerConfig(cluster, snapshot);

    int userId = authenticatedUserId();
    String requestHash = immutableRequestHash(cluster.getClusterId(), request);
    ServiceDependencyBindingEntity binding = bindingEntity(cluster, provider, snapshot, request, userId);
    ServiceDependencySnapshotEntity snapshotEntity = snapshotEntity(snapshot, consumer, provider, userId);
    ServiceDependencyOperationEntity operation = operationEntity(request, requestHash);
    try {
      if (operationDispatcher == null) {
        dependencyDAO.create(binding, snapshotEntity, operation,
            creationGuard(cluster, consumer, provider, request, userId));
      } else {
        dependencyDAO.create(binding, snapshotEntity, operation,
            operationDispatcher.get().initialProviderCommand(binding, snapshot, operation),
            creationGuard(cluster, consumer, provider, request, userId));
      }
      Map<String, Object> response = bindingResponse(binding, snapshotEntity, operation);
      if (operationDispatcher != null) {
        operationDispatcher.get().dispatchInitial(binding.getBindingId());
      }
      return response;
    } catch (StaleApprovalException e) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_PREVIEW_STALE",
          "Provider, consumer, or draft facts changed after preview; review the dependency again.", e);
    } catch (PersistenceException | IllegalStateException e) {
      Map<String, Object> reconciled = reconcileCreate(cluster.getClusterId(), request, requestHash);
      if (reconciled != null) {
        return reconciled;
      }
      if (e instanceof IllegalStateException) {
        throw new ManagedDependencyIntegrationException(409, "BINDING_ID_RETIRED",
            "This binding UUID was detached and cannot be reused.", e);
      }
      throw e;
    }
  }

  private CreationGuard creationGuard(Cluster cluster, Consumer consumer, Provider provider,
      CreateRequest request, int userId) {
    DraftGuard draftGuard = request.draft() == null ? null : new DraftGuard(
        PersistKeyValueImpl.creationDraftStorageKey(userId, request.draft().id()),
        userId, request.draft().revision(), cluster.getClusterId());
    return new CreationGuard(draftGuard, List.of(
        repositoryGuard(consumer.version()), repositoryGuard(provider.version())), List.of(
            serviceVersionGuard(cluster, CONSUMER_SERVICE, consumer.version()),
            serviceVersionGuard(resolver.cluster(provider.serviceKey().clusterId()),
                provider.serviceKey().serviceName(), provider.version())));
  }

  private RepositoryGuard repositoryGuard(ManagedDependencyVersion version) {
    if (version.repositoryRowId() == null) {
      throw unprocessable("DEPENDENCY_VERSION_UNSUPPORTED",
          "Dependency approval requires an authoritative repository row.");
    }
    String repositoryVersion = version.resolvedVersions().get("distribution");
    if (repositoryVersion == null || repositoryVersion.isBlank()) {
      throw unprocessable("DEPENDENCY_VERSION_UNSUPPORTED",
          "Dependency approval requires an authoritative resolved repository version.");
    }
    return new RepositoryGuard(version.repositoryRowId(), repositoryVersion, version.active());
  }

  private ServiceVersionGuard serviceVersionGuard(Cluster cluster, String serviceName,
      ManagedDependencyVersion version) {
    Service service = cluster.getServices().get(serviceName);
    if (service == null || version.repositoryRowId() == null) {
      throw unprocessable("DEPENDENCY_VERSION_UNSUPPORTED",
          "Dependency approval requires a current service repository target.");
    }
    Set<String> componentNames = new java.util.TreeSet<>();
    Set<String> advertisedComponents = new java.util.TreeSet<>();
    Map<String, String> observedVersions = new java.util.TreeMap<>();
    for (ServiceComponent component : service.getServiceComponents().values()) {
      componentNames.add(component.getName());
      if (!component.isVersionAdvertised()) {
        continue;
      }
      advertisedComponents.add(component.getName());
      for (ServiceComponentHost host : component.getServiceComponentHosts().values()) {
        observedVersions.put(component.getName() + "\u0000" + host.getHostName(), host.getVersion());
      }
    }
    return new ServiceVersionGuard(cluster.getClusterId(), serviceName,
        version.repositoryRowId(), componentNames, advertisedComponents, observedVersions);
  }

  public List<Map<String, Object>> list(String clusterName, String serviceName) {
    Cluster cluster = exactConsumer(clusterName, serviceName, ReadLevel.VIEW);
    Map<String, ServiceDependencyBindingEntity> rows = dependencyDAO.findByConsumer(
        cluster.getClusterId(), CONSUMER_SERVICE).stream().collect(java.util.stream.Collectors.toMap(
            ServiceDependencyBindingEntity::getDependencyType, binding -> binding));
    List<Map<String, Object>> result = new ArrayList<>();
    for (ManagedDependencyType type : ManagedDependencyType.values()) {
      ServiceDependencyBindingEntity binding = rows.get(type.name());
      if (binding == null) {
        result.add(Map.of(
            "dependency_type", type.name(),
            "ownership", resolver.unboundOwnership(cluster, type)));
      } else {
        ServiceDependencySnapshotEntity snapshot = dependencyDAO.findSnapshot(
            binding.getBindingId(), binding.getDesiredSnapshotVersion());
        result.add(bindingSummary(binding, snapshot));
      }
    }
    return result;
  }

  public long consumerClusterId(String clusterName, String serviceName) {
    return exactConsumer(clusterName, serviceName, ReadLevel.VIEW).getClusterId();
  }

  public Map<String, Object> get(String clusterName, String serviceName, UUID bindingId) {
    Cluster cluster = exactConsumer(clusterName, serviceName, ReadLevel.VIEW);
    ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(bindingId.toString());
    if (binding == null || binding.getConsumerClusterId() != cluster.getClusterId()
        || !CONSUMER_SERVICE.equals(binding.getConsumerServiceName())) {
      throw notFound();
    }
    ServiceDependencySnapshotEntity snapshot = dependencyDAO.findSnapshot(
        binding.getBindingId(), binding.getDesiredSnapshotVersion());
    ServiceDependencyOperationEntity operation = dependencyDAO.findOperation(
        binding.getActiveOperationId());
    return bindingResponse(binding, snapshot, operation);
  }

  /** Returns server-derived provider metadata and immutable facts for an update. */
  public Map<String, Object> previewUpdate(String clusterName, String serviceName, UUID bindingId) {
    Cluster consumer = exactConsumer(clusterName, serviceName, ReadLevel.MODIFY);
    ServiceDependencyBindingEntity visible = exactOwnedBinding(consumer.getClusterId(), bindingId);
    ManagedDependencyType type = ManagedDependencyType.valueOf(visible.getDependencyType());
    Cluster provider = authorizeProviderParent(new ProviderReference(
        visible.getProviderClusterId(), visible.getProviderServiceName()), type, true);
    return withClusterReadLocks(consumer, provider, () -> {
      UpdateCalculation calculation = calculateUpdate(exactOwnedBinding(
          consumer.getClusterId(), bindingId));
      return updatePreviewResponse(calculation);
    });
  }

  /** Commits an approved provider metadata update and dispatches its journal intent afterwards. */
  public Map<String, Object> update(String clusterName, String serviceName, UUID bindingId,
      UpdateRequest request) {
    Cluster consumer = exactConsumer(clusterName, serviceName, ReadLevel.MODIFY);
    ServiceDependencyBindingEntity visible = exactOwnedBinding(consumer.getClusterId(), bindingId);
    ManagedDependencyType type = ManagedDependencyType.valueOf(visible.getDependencyType());
    Cluster provider = authorizeProviderParent(new ProviderReference(
        visible.getProviderClusterId(), visible.getProviderServiceName()), type, true);
    UpdateExecution execution = withClusterReadLocks(consumer, provider, () -> updateLocked(
        consumer, bindingId, request));
    if (execution.transition().created() && execution.transition().command() != null
        && operationDispatcher != null) {
      operationDispatcher.get().dispatchSafely(execution.transition().command());
    }
    return execution.response();
  }

  private UpdateExecution updateLocked(Cluster consumer, UUID bindingId, UpdateRequest request) {
    ServiceDependencyBindingEntity binding = exactOwnedBinding(consumer.getClusterId(), bindingId);
    String requestHash = updateRequestHash(consumer.getClusterId(), bindingId, request);
    ServiceDependencyOperationEntity existing = dependencyDAO.findOperation(
        request.operationId().toString());
    if (existing != null) {
      if (!bindingId.toString().equals(existing.getBindingId())
          || !"UPDATE".equals(existing.getOperationKind())
          || !requestHash.equals(existing.getRequestHash())) {
        throw new ManagedDependencyIntegrationException(409, "OPERATION_ID_CONFLICT",
            "The operation UUID is already associated with another update request.");
      }
      ServiceDependencySnapshotEntity target = dependencyDAO.findSnapshot(
          existing.getBindingId(), existing.getTargetSnapshotVersion());
      List<ServiceDependencyHostResultEntity> providerCommands = dependencyDAO.findHostResults(
          existing.getBindingId(), existing.getTargetSnapshotVersion(), existing.getOperationEpoch())
          .stream().filter(command -> ManagedDependencyOperationDispatcher.isProviderCommand(
              command.getCheckKind())).toList();
      if (providerCommands.size() > 1) {
        throw new ManagedDependencyIntegrationException(409, "OPERATION_ID_CONFLICT",
            "The update contains conflicting provider intents.");
      }
      LifecycleTransition transition = dependencyDAO.startUpdate(binding.getBindingId(),
          request.expectedRowVersion(), new UpdateFacts(request.expectedSnapshotVersion(),
              request.expectedProviderFingerprint(), request.expectedConsumerFingerprint(),
              request.expectedSnapshotFingerprint(), request.expectedSnapshotApproval()), target,
          existing, providerCommands.isEmpty() ? null : providerCommands.get(0), authenticatedUserId());
      return new UpdateExecution(bindingResponse(transition.binding(), target,
          transition.operation()), transition);
    }

    UpdateCalculation calculation = calculateUpdate(binding);
    ServiceDependencyOperationEntity operation = updateOperationEntity(binding, request,
        calculation.newSnapshotEntity(), requestHash);
    ServiceDependencyHostResultEntity command = operationDispatcher == null ? null
        : operationDispatcher.get().initialProviderCommand(binding, calculation.nextSnapshot(), operation);
    LifecycleTransition transition = dependencyDAO.startUpdate(binding.getBindingId(),
        request.expectedRowVersion(), new UpdateFacts(request.expectedSnapshotVersion(),
            request.expectedProviderFingerprint(), request.expectedConsumerFingerprint(),
            request.expectedSnapshotFingerprint(), request.expectedSnapshotApproval()),
        calculation.newSnapshotEntity(), operation, command, authenticatedUserId());
    return new UpdateExecution(bindingResponse(transition.binding(), calculation.newSnapshotEntity(),
        transition.operation()), transition);
  }

  private UpdateCalculation calculateUpdate(ServiceDependencyBindingEntity binding) {
    if (Set.of("STALE", "FENCING_UNCERTAIN", "DETACHING", "RETIRED").contains(binding.getState())) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_UPDATE_UNAVAILABLE",
          "The managed dependency must be current before its provider metadata can be updated.");
    }
    ManagedDependencyType type = ManagedDependencyType.valueOf(binding.getDependencyType());
    ServiceDependencySnapshotEntity currentEntity = dependencyDAO.findSnapshot(
        binding.getBindingId(), binding.getDesiredSnapshotVersion());
    ManagedDependencySnapshot current = readSnapshot(currentEntity);
    if (current == null) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_STATE_CORRUPT",
          "The current dependency snapshot is missing.");
    }
    Consumer consumer = resolver.resolveService(new ManagedDependencyServiceKey(
        binding.getConsumerClusterId(), CONSUMER_SERVICE), type);
    Provider provider = resolver.resolveProvider(new ManagedDependencyServiceKey(
        binding.getProviderClusterId(), binding.getProviderServiceName()));
    ValidationResult validation;
    if (consumer.securityMode() == ManagedDependencySecurityMode.KERBEROS) {
      SortedMap<ManagedDependencyType, UUID> ids = new java.util.TreeMap<>();
      ids.put(type, UUID.fromString(binding.getBindingId()));
      SortedMap<ManagedDependencyType, Provider> providers = new java.util.TreeMap<>();
      providers.put(type, provider);
      ManagedHBaseSecurityDescriptorAdapter.Resolution resolution = resolver.resolveSecureSelection(
          resolver.cluster(binding.getConsumerClusterId()), consumer,
          completeSelection(ids, providers), null);
      validation = validator.validate(UUID.fromString(binding.getBindingId()),
          binding.getDesiredSnapshotVersion() + 1, type, consumer, provider,
          existingNamespaces(), existingAllocations(), current, resolution.contextFor(type));
    } else {
      validation = validator.validate(UUID.fromString(binding.getBindingId()),
          binding.getDesiredSnapshotVersion() + 1, type, consumer, provider,
          existingNamespaces(), existingAllocations(), current);
    }
    if (!validation.isValid()) {
      throw issue(validation.issues().get(0));
    }
    ManagedDependencySnapshot next = validation.snapshot().orElseThrow();
    if (!current.namespace().equals(next.namespace())
        || !current.providerService().equals(next.providerService())
        || !current.consumerIdentity().equals(next.consumerIdentity())) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_UPDATE_MAPPING_UNSUPPORTED",
          "An update must preserve the provider, consumer identity, and private namespace.");
    }
    return new UpdateCalculation(binding, currentEntity, current, consumer, provider, next,
        snapshotEntity(next, consumer, provider, authenticatedUserId()));
  }

  private Map<String, Object> updatePreviewResponse(UpdateCalculation calculation) {
    Map<String, Object> response = new LinkedHashMap<>();
    ServiceDependencyBindingEntity binding = calculation.binding();
    response.put("binding_id", binding.getBindingId());
    response.put("dependency_type", binding.getDependencyType());
    response.put("consumer", consumerSummary(calculation.consumer()));
    response.put("provider", providerSummary(calculation.provider()));
    response.put("row_version", binding.getRowVersion());
    response.put("operation_epoch", binding.getOperationEpoch());
    response.put("current_snapshot_version", binding.getDesiredSnapshotVersion());
    response.put("next_snapshot_version", calculation.nextSnapshot().snapshotVersion());
    response.put("snapshot_approval", binding.getSnapshotApproval());
    response.put("provider_fingerprint", calculation.current().providerFingerprint());
    response.put("consumer_descriptor_fingerprint", calculation.current().consumerFingerprint());
    response.put("snapshot_fingerprint", calculation.current().snapshotFingerprint());
    response.put("old_client_config", clientConfig(calculation.current()));
    response.put("new_client_config", clientConfig(calculation.nextSnapshot()));
    response.put("old_snapshot", snapshotSummary(calculation.currentEntity()));
    response.put("new_snapshot", snapshotSummary(calculation.newSnapshotEntity()));
    return response;
  }

  public Map<String, Object> retry(String clusterName, String serviceName, UUID bindingId,
      LifecycleRequest request) {
    Cluster consumer = exactConsumer(clusterName, serviceName, ReadLevel.MODIFY);
    ServiceDependencyBindingEntity visible = exactOwnedBinding(
        consumer.getClusterId(), bindingId);
    ManagedDependencyType type = ManagedDependencyType.valueOf(visible.getDependencyType());
    Cluster provider = authorizeProviderParent(new ProviderReference(
        visible.getProviderClusterId(), visible.getProviderServiceName()), type, true);
    RetryExecution execution = withClusterReadLocks(consumer, provider, () -> retryLocked(
        consumer, bindingId, request));
    if (operationDispatcher != null) {
      for (ServiceDependencyHostResultEntity command : execution.commands()) {
        operationDispatcher.get().dispatchSafely(command);
      }
    }
    return execution.response();
  }

  private RetryExecution retryLocked(Cluster consumer, UUID bindingId,
      LifecycleRequest request) {
    return retryLocked(consumer, bindingId, request, null);
  }

  /** Only the deployment owner invokes this path with its persisted failed request identity. */
  void retryInstallation(String clusterName, long failedRequestId, UUID attemptId) {
    Cluster consumer = exactConsumer(clusterName, "HBASE", ReadLevel.MODIFY);
    List<org.apache.ambari.server.actionmanager.HostRoleCommand> tasks = actions.get().getRequestTasks(failedRequestId);
    if (tasks.isEmpty() || tasks.stream().anyMatch(task -> task.getRequestId() != failedRequestId
        || !Long.toString(consumer.getClusterId()).equals(task.getExecutionCommandWrapper().getExecutionCommand().getClusterId())
        || !task.getStatus().isCompletedState())) {
      throw lifecycleConflict("DEPLOYMENT_TASK_LINEAGE_MISSING", "The failed installation request is unavailable or incomplete.");
    }
    List<org.apache.ambari.server.actionmanager.HostRoleCommand> failedInstalls = tasks.stream()
        .filter(task -> task.getRoleCommand() == org.apache.ambari.server.RoleCommand.INSTALL
            && task.getStatus() != org.apache.ambari.server.actionmanager.HostRoleStatus.COMPLETED
            && "HBASE".equals(task.getExecutionCommandWrapper().getExecutionCommand().getServiceName())).toList();
    if (failedInstalls.isEmpty()) return;
    for (ServiceDependencyBindingEntity binding : dependencyDAO.findByConsumer(consumer.getClusterId(), "HBASE")) {
      authorizeProviderParent(new ProviderReference(binding.getProviderClusterId(), binding.getProviderServiceName()),
          ManagedDependencyType.valueOf(binding.getDependencyType()), true);
      UUID operation = UUID.nameUUIDFromBytes((attemptId + ":install:" + binding.getBindingId())
          .getBytes(java.nio.charset.StandardCharsets.UTF_8));
      retryLocked(consumer, UUID.fromString(binding.getBindingId()),
          new LifecycleRequest(operation, binding.getRowVersion()), failedInstalls);
    }
    // The common reconciler dispatches ready intents after the deployment transaction commits.
    // AWAITING_INSTALL intents belong exclusively to the next INSTALL publication.
  }

  private RetryExecution retryLocked(Cluster consumer, UUID bindingId, LifecycleRequest request,
      List<org.apache.ambari.server.actionmanager.HostRoleCommand> failedInstalls) {
    ServiceDependencyBindingEntity binding = exactOwnedBinding(consumer.getClusterId(), bindingId);
    ServiceDependencyOperationEntity operation = lifecycleOperation(binding, request,
        "RETRY", lifecycleRequestHash(consumer.getClusterId(), bindingId, request,
            failedInstalls == null ? "RETRY" : "REINSTALL"));
    boolean exactCommittedRetry = Objects.equals(binding.getActiveOperationId(),
        operation.getOperationId()) && Objects.equals(binding.getOperationEpoch(),
            operation.getOperationEpoch());
    List<ServiceDependencyHostResultEntity> retryCommands;
    if (!exactCommittedRetry) {
      retryCommands = retryPreparationCommands(binding, request.operationId(), operation.getOperationEpoch());
    } else {
      retryCommands = dependencyDAO.findHostResults(binding.getBindingId(),
          binding.getDesiredSnapshotVersion(), binding.getOperationEpoch()).stream()
          .filter(command -> command.getCheckKind().equals(
              "PREPARE_" + binding.getDependencyType() + "_CONSUMER"))
          .toList();
    }
    try {
      if (failedInstalls != null && !exactCommittedRetry) {
        for (ServiceDependencyHostResultEntity command : retryCommands) {
          if (failedInstalls.stream().anyMatch(task -> Objects.equals(command.getHostId(), task.getHostId())
              && command.getComponentName().equals(task.getRole().name()))) {
            command.setState("AWAITING_INSTALL");
          }
        }
      }
      LifecycleTransition transition = failedInstalls == null
          ? dependencyDAO.startConsumerRetry(binding.getBindingId(), request.expectedRowVersion(), operation,
              retryCommands, authenticatedUserId())
          : dependencyDAO.startConsumerReinstallation(binding.getBindingId(), request.expectedRowVersion(), operation,
              retryCommands, authenticatedUserId());
      ServiceDependencySnapshotEntity snapshot = dependencyDAO.findSnapshot(
          binding.getBindingId(), transition.operation().getTargetSnapshotVersion());
      return new RetryExecution(bindingResponse(transition.binding(), snapshot, transition.operation()),
          transition, retryCommands);
    } catch (StaleApprovalException e) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_OPERATION_STALE",
          "The dependency changed or is not retryable; reload its current status.", e);
    }
  }

  private List<ServiceDependencyHostResultEntity> retryPreparationCommands(
      ServiceDependencyBindingEntity binding, UUID operationId, long epoch) {
    if (runtimePlanner == null) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_RETRY_REQUIRES_PREPARATION_PLAN",
          "A dependency retry requires an executable HBase preparation plan.");
    }
    try {
      Map<String, Object> readiness = readinessSummary(binding);
      @SuppressWarnings("unchecked")
      List<Long> hosts = (List<Long>) readiness.get("required_daemon_host_ids");
      if (!Boolean.TRUE.equals(readiness.get("topology_current")) || hosts == null || hosts.isEmpty()
          || Boolean.TRUE.equals(readiness.get("active_command"))) {
        throw new AmbariException("The current consumer topology is not retryable");
      }
      ManagedDependencySnapshot snapshot = readSnapshot(dependencyDAO.findSnapshot(
          binding.getBindingId(), binding.getDesiredSnapshotVersion()));
      List<ServiceDependencyHostResultEntity> commands = new ArrayList<>();
      for (long hostId : hosts) {
        ManagedDependencyRuntimePlanner.RetryPreparationPlan plan = runtimePlanner.get()
            .buildRetryPreparationPlan(binding.getConsumerClusterId(), hostId,
                UUID.fromString(binding.getBindingId()));
        ManagedDependencyCommand source = plan.bundle().preparationCommands().stream()
            .filter(command -> binding.getBindingId().equals(command.envelope().bindingId().toString()))
            .findFirst().orElseThrow(() -> new AmbariException("The consumer preparation profile is missing"));
        ManagedDependencyCommand command = ManagedDependencyCommand.prepareConsumer(snapshot, operationId,
            epoch, hostId, source.parameters().get("client.package.name"),
            source.parameters().get("client.software.semantic.version"),
            source.parameters().get("identity.fingerprint"));
        commands.add(ManagedDependencyOperationDispatcher.commandEntity(command,
            ManagedDependencyType.valueOf(binding.getDependencyType()), hostId, plan.componentName()));
      }
      return commands;
    } catch (AmbariException | RuntimeException e) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_RETRY_REQUIRES_PREPARATION_PLAN",
          "The current dependency has no executable consumer preparation plan.", e);
    }
  }

  public Map<String, Object> verifyManualCredentials(String clusterName, String serviceName,
      UUID bindingId, long expectedEpoch) {
    Cluster consumer = exactConsumer(clusterName, serviceName, ReadLevel.MODIFY);
    ServiceDependencyBindingEntity binding = exactOwnedBinding(consumer.getClusterId(), bindingId);
    if (!Objects.equals(binding.getOperationEpoch(), expectedEpoch)) {
      throw lifecycleConflict("DEPENDENCY_OPERATION_STALE", "Reload the current credential operation.");
    }
    Cluster provider = authorizeProviderParent(new ProviderReference(binding.getProviderClusterId(),
        binding.getProviderServiceName()), ManagedDependencyType.valueOf(binding.getDependencyType()), false);
    return withClusterReadLocks(consumer, provider, () -> {
      validateDispatchState(binding);
      @SuppressWarnings("unchecked")
      List<Long> hosts = (List<Long>) readinessSummary(binding).get("required_daemon_host_ids");
      try {
        for (long hostId : hosts) {
          taskResults.get().verifyManualCredentials(binding.getBindingId(), expectedEpoch, hostId);
        }
      } catch (AmbariException e) {
        throw lifecycleConflict("DEPENDENCY_CREDENTIALS_NOT_READY", "Manual credential verification is not ready.", e);
      }
      return get(clusterName, serviceName, bindingId);
    });
  }

  public Map<String, Object> detach(String clusterName, String serviceName, UUID bindingId,
      LifecycleRequest request) {
    Cluster consumer = exactConsumer(clusterName, serviceName, ReadLevel.MODIFY);
    ServiceDependencyBindingEntity visible = dependencyDAO.findBinding(bindingId.toString());
    if (visible == null) {
      return reconcileDetached(consumer.getClusterId(), bindingId, request);
    }
    requireOwnedBinding(visible, consumer.getClusterId(), bindingId);
    ManagedDependencyType type = ManagedDependencyType.valueOf(visible.getDependencyType());
    Cluster provider = authorizeProviderParent(new ProviderReference(
        visible.getProviderClusterId(), visible.getProviderServiceName()), type, true);
    return withClusterReadLocks(consumer, provider, () -> {
      lifecyclePolicy.validateConsumerDetach(consumer);
      return detachLocked(consumer.getClusterId(), bindingId, request);
    });
  }

  private Map<String, Object> detachLocked(long consumerClusterId, UUID bindingId,
      LifecycleRequest request) {
    ServiceDependencyBindingEntity binding = exactOwnedBinding(consumerClusterId, bindingId);
    ManagedDependencySnapshot snapshot = validateDetachmentState(binding);
    if (binding.getActionHostId() == null) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_OPERATION_STALE",
          "The dependency has no pinned provider action host; complete or retry preparation first.");
    }
    ServiceDependencyOperationEntity operation = lifecycleOperation(binding, request,
        "DETACH", lifecycleRequestHash(consumerClusterId, bindingId, request, "DETACH"));
    ManagedDependencyType type = ManagedDependencyType.valueOf(binding.getDependencyType());
    ManagedDependencyCommand command = ManagedDependencyCommand.invalidate(snapshot,
        request.operationId(), operation.getOperationEpoch(), binding.getActionHostId());
    ServiceDependencyHostResultEntity invalidation =
        ManagedDependencyOperationDispatcher.commandEntity(command, type,
            binding.getActionHostId(), type == ManagedDependencyType.HDFS
                ? "NAMENODE" : "ZOOKEEPER_SERVER");
    try {
      LifecycleTransition transition = dependencyDAO.startDetach(binding.getBindingId(),
          request.expectedRowVersion(), operation, invalidation, authenticatedUserId());
      Map<String, Object> response = bindingResponse(transition.binding(),
          dependencyDAO.findSnapshot(binding.getBindingId(), binding.getDesiredSnapshotVersion()),
          transition.operation());
      if (transition.command() != null && operationDispatcher != null) {
        operationDispatcher.get().dispatchSafely(transition.command());
      }
      return response;
    } catch (StaleApprovalException e) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_OPERATION_STALE",
          "The dependency changed or has active work; reload its current status.", e);
    }
  }

  public Map<String, Object> dependents(String clusterName, String serviceName) {
    Cluster provider = exactProvider(clusterName, serviceName, ReadLevel.VIEW);
    List<Map<String, Object>> visible = new ArrayList<>();
    int hidden = 0;
    for (ServiceDependencyBindingEntity binding : dependencyDAO.findByProvider(
        provider.getClusterId(), serviceName)) {
      Cluster consumer = resolver.cluster(binding.getConsumerClusterId());
      if (can(consumer, RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE)) {
        visible.add(Map.of(
            "binding_id", binding.getBindingId(),
            "consumer_cluster_id", binding.getConsumerClusterId(),
            "consumer_cluster_name", consumer.getClusterName(),
            "consumer_service_name", binding.getConsumerServiceName(),
            "dependency_type", binding.getDependencyType(),
            "state", binding.getState()));
      } else {
        hidden++;
      }
    }
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("items", visible);
    response.put("hidden_dependent_count", hidden);
    response.put("impact_revision", lifecyclePolicy.impactRevision(provider.getClusterId(), serviceName));
    return response;
  }

  public Map<String, Object> impact(String clusterName, String serviceName, String action) {
    Cluster provider = exactProvider(clusterName, serviceName, ReadLevel.VIEW);
    String effectiveAction = action == null ? null : action.trim().toUpperCase(java.util.Locale.ROOT);
    if (!Set.of("STOP", "RESTART").contains(effectiveAction)) {
      throw badRequest("INVALID_DEPENDENCY_ACTION",
          "Only STOP and RESTART impact are supported.");
    }
    return provider.executeUnderWriteLockUntilTransactionCompletion(() -> {
      List<ServiceDependencyBindingEntity> current = dependencyDAO.findByProvider(provider.getClusterId(), serviceName);
      Map<String, Object> projection = dependents(clusterName, serviceName);
      Map<String, Object> result = new LinkedHashMap<>(projection);
      result.put("provider_cluster_id", provider.getClusterId());
      result.put("service_name", serviceName);
      result.put("action", effectiveAction);
      result.put("dependent_count", current.size());
      result.put("impact_revision", lifecyclePolicy.impactRevision(provider.getClusterId(), serviceName, current));
      result.put("requires_confirmation", !current.isEmpty());
      return result;
    });
  }

  private Map<String, Object> candidate(ConsumerReference reference, Consumer consumer,
      Cluster cluster, ManagedDependencyType type) {
    ProviderReference providerReference =
        new ProviderReference(cluster.getClusterId(), type.getProviderServiceName());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("cluster_id", cluster.getClusterId());
    result.put("cluster_name", cluster.getClusterName());
    result.put("service_name", type.getProviderServiceName());
    try {
      CandidateEvaluation evaluation;
      if (consumer.clusterId() != null
          && consumer.securityMode() == ManagedDependencySecurityMode.KERBEROS) {
        evaluation = candidateSecureValidation(reference, consumer, cluster, type);
      } else if (consumer.clusterId() != null) {
        Cluster consumerCluster = resolver.cluster(consumer.clusterId());
        evaluation = withClusterReadLocks(List.of(consumerCluster, cluster), () -> {
          Provider provider = resolver.resolveProvider(providerReference.serviceKey());
          return new CandidateEvaluation(provider,
              validate(UUID.randomUUID(), type, consumer, provider), false);
        });
      } else {
        Provider provider = resolver.resolveProvider(providerReference.serviceKey());
        evaluation = new CandidateEvaluation(provider,
            validate(UUID.randomUUID(), type, consumer, provider), false);
      }
      Provider provider = evaluation.provider();
      result.put("version", versionSummary(provider.version().compatibility()));
      result.put("installed", provider.installed());
      result.put("healthy", provider.healthy());
      result.put("security_mode", provider.securityMode().name());
      if (evaluation.requiresCompleteSelection()) {
        result.put("compatible", false);
        result.put("planning_status", "INCOMPLETE");
        result.put("requires_complete_selection", true);
        result.put("errors", List.of(Map.of(
            "code", "DEPENDENCY_SECURITY_PLAN_INCOMPLETE",
            "message", "Select the HDFS and ZooKeeper providers together and run complete preview.")));
      } else {
        ValidationResult validation = evaluation.validation();
        result.put("compatible", validation.isValid());
        result.put("errors", issues(validation.issues()));
      }
    } catch (ManagedDependencyIntegrationException e) {
      result.put("compatible", false);
      if ("DEPENDENCY_SECURITY_PLAN_INCOMPLETE".equals(e.getCode())) {
        result.put("planning_status", "INCOMPLETE");
        result.put("requires_complete_selection", true);
      }
      result.put("errors", List.of(Map.of("code", e.getCode(), "message", e.getMessage())));
    }
    return result;
  }

  private CandidateEvaluation candidateSecureValidation(ConsumerReference consumerReference,
      Consumer consumer, Cluster providerCluster, ManagedDependencyType type) {
    Cluster consumerCluster = resolver.cluster(consumer.clusterId());
    return withClusterReadLocks(List.of(consumerCluster, providerCluster), () -> {
      Provider provider = resolver.resolveProvider(new ManagedDependencyServiceKey(
          providerCluster.getClusterId(), type.getProviderServiceName()));
      if (!hasLocalCounterpart(consumerCluster, type)) {
        return new CandidateEvaluation(provider, null, true);
      }
      UUID bindingId = UUID.randomUUID();
      SortedMap<ManagedDependencyType, UUID> ids = new java.util.TreeMap<>();
      ids.put(type, bindingId);
      SortedMap<ManagedDependencyType, Provider> providers = new java.util.TreeMap<>();
      providers.put(type, provider);
      ManagedHBaseSecurityDescriptorAdapter.Resolution resolution =
          resolver.resolveSecureSelection(consumerCluster, consumer,
              completeSelection(ids, providers),
              consumerReference.servicePlan()
                  ? consumerReference.expectedDraftRevision() : null);
      return new CandidateEvaluation(provider, validator.validate(bindingId, 1L, type, consumer, provider,
          existingNamespaces(), existingAllocations(), currentSnapshot(consumer, type),
          resolution.contextFor(type)), false);
    });
  }

  private boolean hasLocalCounterpart(Cluster consumerCluster, ManagedDependencyType type) {
    String counterpart = type == ManagedDependencyType.HDFS ? "ZOOKEEPER" : "HDFS";
    return consumerCluster.getServices().containsKey(counterpart);
  }

  private ValidationResult validate(UUID bindingId, ManagedDependencyType type,
      Consumer consumer, Provider provider) {
    return validator.validate(bindingId, 1, type, consumer, provider,
        existingNamespaces(), existingAllocations(), currentSnapshot(consumer, type));
  }

  /** Revalidates one persisted operation while its consumer and provider cluster locks are held. */
  ManagedDependencySnapshot validateDispatchState(ServiceDependencyBindingEntity expected) {
    ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(expected.getBindingId());
    if (binding == null
        || !Objects.equals(binding.getActiveOperationId(), expected.getActiveOperationId())
        || !Objects.equals(binding.getOperationEpoch(), expected.getOperationEpoch())
        || !Objects.equals(binding.getDesiredSnapshotVersion(), expected.getDesiredSnapshotVersion())
        || !"APPROVED".equals(binding.getSnapshotApproval())
        || Set.of("STALE", "FENCING_UNCERTAIN", "DETACHING", "RETIRED")
            .contains(binding.getState())) {
      throw new StaleApprovalException(
          "The managed dependency operation no longer owns an approved snapshot");
    }
    ManagedDependencyType type = ManagedDependencyType.valueOf(binding.getDependencyType());
    Consumer consumer = resolver.resolveService(new ManagedDependencyServiceKey(
        binding.getConsumerClusterId(), binding.getConsumerServiceName()));
    Provider provider = resolver.resolveProvider(new ManagedDependencyServiceKey(
        binding.getProviderClusterId(), binding.getProviderServiceName()));
    ManagedDependencySnapshot persisted = readSnapshot(dependencyDAO.findSnapshot(
        binding.getBindingId(), binding.getDesiredSnapshotVersion()));
    if (persisted == null) {
      throw new StaleApprovalException("The approved dependency snapshot is missing");
    }
    ValidationResult current = validator.validate(UUID.fromString(binding.getBindingId()),
        binding.getDesiredSnapshotVersion(), type, consumer, provider,
        List.of(persisted.namespace()), List.of(), persisted);
    if (!current.isValid()) {
      throw new StaleApprovalException(
          "Provider or consumer facts changed after dependency approval");
    }
    ManagedDependencySnapshot resolved = current.snapshot().orElseThrow();
    if (!Objects.equals(persisted.snapshotFingerprint(), resolved.snapshotFingerprint())
        || !Objects.equals(binding.getProviderFingerprint(), resolved.providerFingerprint())) {
      throw new StaleApprovalException(
          "Provider or consumer facts changed after dependency approval");
    }
    resolver.validateEffectiveConsumerConfig(resolver.cluster(binding.getConsumerClusterId()),
        persisted);
    return persisted;
  }

  /** Uses the immutable approved snapshot for provider fencing even after drift. */
  ManagedDependencySnapshot validateDetachmentState(ServiceDependencyBindingEntity expected) {
    ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(expected.getBindingId());
    if (binding == null
        || !Objects.equals(binding.getActiveOperationId(), expected.getActiveOperationId())
        || !Objects.equals(binding.getOperationEpoch(), expected.getOperationEpoch())
        || !Objects.equals(binding.getDesiredSnapshotVersion(), expected.getDesiredSnapshotVersion())
        || !"APPROVED".equals(binding.getSnapshotApproval())
        || "RETIRED".equals(binding.getState())) {
      throw new StaleApprovalException("The managed dependency cannot be fenced");
    }
    ManagedDependencySnapshot snapshot = readSnapshot(dependencyDAO.findSnapshot(
        binding.getBindingId(), binding.getDesiredSnapshotVersion()));
    if (snapshot == null || !binding.getBindingId().equals(snapshot.bindingId().toString())
        || !binding.getDependencyType().equals(snapshot.type().name())
        || !binding.getProviderClusterId().equals(snapshot.providerService().clusterId())
        || !binding.getProviderServiceName().equals(snapshot.providerService().serviceName())
        || !binding.getProviderFingerprint().equals(snapshot.providerFingerprint())) {
      throw new StaleApprovalException("The immutable dependency fence snapshot is missing");
    }
    return snapshot;
  }

  private Collection<ManagedDependencyNamespace> existingNamespaces() {
    List<ManagedDependencyNamespace> namespaces = new ArrayList<>();
    for (ServiceDependencyBindingEntity binding : dependencyDAO.findAllBindings()) {
      if (ManagedDependencyType.HDFS.name().equals(binding.getDependencyType())) {
        namespaces.add(new ManagedDependencyNamespace(
            binding.getNamespaceRoot(), binding.getNamespaceWal(), ""));
      } else {
        namespaces.add(new ManagedDependencyNamespace("", "", binding.getNamespaceZnode()));
      }
    }
    return namespaces;
  }

  private Collection<Allocation> existingAllocations() {
    List<Allocation> allocations = new ArrayList<>();
    for (ServiceDependencyBindingEntity binding : dependencyDAO.findAllBindings()) {
      ServiceDependencySnapshotEntity entity = dependencyDAO.findSnapshot(
          binding.getBindingId(), binding.getDesiredSnapshotVersion());
      ManagedDependencySnapshot snapshot = readSnapshot(entity);
      if (snapshot != null) {
        allocations.add(new Allocation(snapshot.consumerIdentity().effectiveShortUser(),
            identityPlanOwner(snapshot.consumerIdentity().effectiveShortUser(), binding.getConsumerClusterId())));
      }
    }
    return allocations;
  }

  private String identityPlanOwner(String user, long clusterId) {
    Cluster cluster = resolver.cluster(clusterId);
    Integer creatorUserId = cluster.getClusterEntity().getCreatorUserId();
    String draftId = cluster.getClusterEntity().getCreationDraftId();
    ManagedDependencyIdentity.Plan plan = creatorUserId != null && draftId != null
        ? ManagedDependencyIdentity.Plan.forCreationDraft(creatorUserId, UUID.fromString(draftId))
        : ManagedDependencyIdentity.Plan.forExistingCluster(clusterId);
    return plan.plannedShortUser().equals(user) ? plan.planFingerprint() : "existing:" + clusterId;
  }

  private ManagedDependencySnapshot currentSnapshot(Consumer consumer, ManagedDependencyType type) {
    if (consumer.clusterId() == null) {
      return null;
    }
    ServiceDependencyBindingEntity binding = dependencyDAO.findByConsumerAndType(
        consumer.clusterId(), CONSUMER_SERVICE, type.name());
    return binding == null ? null : readSnapshot(dependencyDAO.findSnapshot(
        binding.getBindingId(), binding.getDesiredSnapshotVersion()));
  }

  private ManagedDependencySnapshot readSnapshot(ServiceDependencySnapshotEntity entity) {
    if (entity == null) {
      return null;
    }
    try {
      return StageUtils.getGson().fromJson(entity.getSnapshotJson(), ManagedDependencySnapshot.class);
    } catch (RuntimeException e) {
      throw new ManagedDependencyIntegrationException(500, "DEPENDENCY_STATE_CORRUPT",
          "Persisted dependency state is invalid; repair it before retrying.", e);
    }
  }

  private Consumer resolveConsumer(ConsumerReference reference, ReadLevel level) {
    if (reference.draftId() != null) {
      verifyAmbari(RoleAuthorization.AMBARI_ADD_DELETE_CLUSTERS);
      return resolver.resolveDraft(reference.draftId(), reference.expectedDraftRevision());
    }
    Cluster cluster = resolver.cluster(reference.clusterId());
    if (reference.servicePlan()) {
      authorize(cluster, Set.of(RoleAuthorization.SERVICE_ADD_DELETE_SERVICES));
    }
    authorize(cluster, level == ReadLevel.VIEW
        ? RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE
        : Set.of(RoleAuthorization.SERVICE_MODIFY_CONFIGS));
    if (level == ReadLevel.MODIFY) {
      authorize(cluster, Set.of(RoleAuthorization.SERVICE_SET_SERVICE_USERS_GROUPS));
    }
    if (reference.servicePlan()) {
      return resolver.resolveServicePlan(
          cluster.getClusterId(), reference.expectedDraftRevision());
    }
    return resolver.resolveService(new ManagedDependencyServiceKey(cluster.getClusterId(), CONSUMER_SERVICE));
  }

  private Provider resolveProvider(ProviderReference reference, ManagedDependencyType type,
      boolean mutationPreview) {
    Cluster cluster = authorizeProviderParent(reference, type, mutationPreview);
    return resolver.resolveProvider(reference.serviceKey());
  }

  private Cluster authorizeProviderParent(ProviderReference reference, ManagedDependencyType type,
      boolean mutation) {
    if (!type.getProviderServiceName().equals(reference.serviceName())) {
      throw badRequest("INVALID_PROVIDER_DESCRIPTOR", "Provider service does not match dependency type.");
    }
    Cluster cluster = resolver.cluster(reference.clusterId());
    authorize(cluster, RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE);
    if (mutation) {
      authorize(cluster, Set.of(RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND));
    }
    if (!cluster.getServices().containsKey(reference.serviceName())) {
      throw notFound();
    }
    return cluster;
  }

  private <T> T withClusterReadLocks(Cluster firstCandidate, Cluster secondCandidate,
      Supplier<T> operation) {
    if (firstCandidate.getClusterId() == secondCandidate.getClusterId()) {
      return firstCandidate.executeUnderReadLock(operation);
    }
    Cluster first = firstCandidate.getClusterId() < secondCandidate.getClusterId()
        ? firstCandidate : secondCandidate;
    Cluster second = first == firstCandidate ? secondCandidate : firstCandidate;
    return first.executeUnderReadLock(() -> second.executeUnderReadLock(operation));
  }

  private <T> T withClusterReadLocks(Collection<Cluster> candidates, Supplier<T> operation) {
    List<Cluster> ordered = candidates.stream().filter(Objects::nonNull)
        .collect(Collectors.toMap(Cluster::getClusterId, cluster -> cluster,
            (first, ignored) -> first, java.util.TreeMap::new)).values().stream().toList();
    return withClusterReadLocks(ordered, 0, operation);
  }

  private <T> T withClusterReadLocks(List<Cluster> ordered, int index, Supplier<T> operation) {
    if (index == ordered.size()) {
      return operation.get();
    }
    return ordered.get(index).executeUnderReadLock(
        () -> withClusterReadLocks(ordered, index + 1, operation));
  }

  private Cluster exactConsumer(String clusterName, String serviceName, ReadLevel level) {
    if (!CONSUMER_SERVICE.equals(serviceName)) {
      throw notFound();
    }
    Cluster cluster = resolver.cluster(clusterName);
    authorize(cluster, level == ReadLevel.VIEW
        ? RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE
        : Set.of(RoleAuthorization.SERVICE_MODIFY_CONFIGS));
    if (level == ReadLevel.MODIFY) {
      authorize(cluster, Set.of(RoleAuthorization.SERVICE_SET_SERVICE_USERS_GROUPS));
    }
    if (!cluster.getServices().containsKey(CONSUMER_SERVICE)) {
      throw notFound();
    }
    return cluster;
  }

  private Cluster exactProvider(String clusterName, String serviceName, ReadLevel level) {
    if (!Set.of("HDFS", "ZOOKEEPER").contains(serviceName)) {
      throw notFound();
    }
    Cluster cluster = resolver.cluster(clusterName);
    authorize(cluster, RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE);
    if (!cluster.getServices().containsKey(serviceName)) {
      throw notFound();
    }
    return cluster;
  }

  private void verifyDraftMaterialization(Cluster cluster, Consumer live, DraftReference draft) {
    Consumer planned = resolver.resolveDraft(draft.id(), draft.revision());
    Map<String, Object> association;
    try {
      association = persistKeyValue.getCreationDraftCluster(draft.id().toString());
    } catch (AuthorizationException e) {
      throw new ManagedDependencyIntegrationException(403, "DEPENDENCY_AUTHORIZATION_FAILED",
          "The authenticated user cannot materialize this creation draft.", e);
    }
    if (!Objects.equals(((Number) association.get("cluster_id")).longValue(), cluster.getClusterId())
        || !validator.consumerFingerprint(planned).equals(validator.consumerFingerprint(live))) {
      throw new ManagedDependencyIntegrationException(409, "DRAFT_MATERIALIZATION_MISMATCH",
          "The owned draft no longer matches this fresh HBASE service.");
    }
  }

  private ServiceDependencyBindingEntity bindingEntity(Cluster consumer, Provider provider,
      ManagedDependencySnapshot snapshot, CreateRequest request, int userId) {
    ServiceDependencyBindingEntity entity = new ServiceDependencyBindingEntity();
    entity.setBindingId(request.bindingId().toString());
    entity.setConsumerClusterId(consumer.getClusterId());
    entity.setConsumerServiceName(CONSUMER_SERVICE);
    entity.setProviderClusterId(provider.serviceKey().clusterId());
    entity.setProviderServiceName(provider.serviceKey().serviceName());
    entity.setDependencyType(request.type().name());
    entity.setState("PROVISIONING");
    entity.setProvisioningPhase("PROVIDER_PREPARING");
    entity.setDesiredSnapshotVersion(snapshot.snapshotVersion());
    entity.setSnapshotApproval("APPROVED");
    entity.setProviderFingerprint(snapshot.providerFingerprint());
    entity.setNamespaceRoot(emptyToNull(snapshot.namespace().rootUri()));
    entity.setNamespaceWal(emptyToNull(snapshot.namespace().walUri()));
    entity.setNamespaceZnode(emptyToNull(snapshot.namespace().znode()));
    entity.setActiveOperationId(request.operationId().toString());
    entity.setFailureRetryable(false);
    entity.setCreatedByUserId(userId);
    entity.setUpdatedByUserId(userId);
    return entity;
  }

  private ServiceDependencySnapshotEntity snapshotEntity(ManagedDependencySnapshot snapshot,
      Consumer consumer, Provider provider, int userId) {
    ServiceDependencySnapshotEntity entity = new ServiceDependencySnapshotEntity();
    entity.setBindingId(snapshot.bindingId().toString());
    entity.setSnapshotVersion(snapshot.snapshotVersion());
    entity.setSchemaVersion(snapshot.schemaVersion());
    entity.setConsumerFingerprint(snapshot.consumerFingerprint());
    entity.setProviderFingerprint(snapshot.providerFingerprint());
    entity.setProviderDisplayName(resolver.cluster(provider.serviceKey().clusterId()).getClusterName());
    entity.setConsumerServiceVersion(consumer.version().serviceVersion());
    entity.setSnapshotFingerprint(snapshot.snapshotFingerprint());
    entity.setClientFeaturesHash(hash(provider.version().clientFeatures().toString()));
    entity.setSecurityPolicyHash(hash(snapshot.securityMode().name(),
        snapshot.consumerIdentity().effectiveShortUser(), snapshot.consumerIdentity().directoryMode()));
    entity.setSnapshotJson(StageUtils.getGson().toJson(snapshot));
    entity.setCreatedByUserId(userId);
    entity.setCreateTimestamp(System.currentTimeMillis());
    return entity;
  }

  private ServiceDependencyOperationEntity operationEntity(CreateRequest request, String requestHash) {
    long now = System.currentTimeMillis();
    ServiceDependencyOperationEntity entity = new ServiceDependencyOperationEntity();
    entity.setOperationId(request.operationId().toString());
    entity.setBindingId(request.bindingId().toString());
    entity.setOperationKind("CREATE");
    entity.setOperationEpoch(1L);
    entity.setTargetSnapshotVersion(1L);
    entity.setRequestHash(requestHash);
    entity.setState("QUEUED");
    entity.setCreateTimestamp(now);
    entity.setUpdateTimestamp(now);
    return entity;
  }

  private ServiceDependencyOperationEntity updateOperationEntity(
      ServiceDependencyBindingEntity binding, UpdateRequest request,
      ServiceDependencySnapshotEntity snapshot, String requestHash) {
    long now = System.currentTimeMillis();
    ServiceDependencyOperationEntity entity = new ServiceDependencyOperationEntity();
    entity.setOperationId(request.operationId().toString());
    entity.setBindingId(binding.getBindingId());
    entity.setOperationKind("UPDATE");
    entity.setOperationEpoch(binding.getOperationEpoch() + 1);
    entity.setTargetSnapshotVersion(snapshot.getSnapshotVersion());
    entity.setRequestHash(requestHash);
    entity.setState("QUEUED");
    entity.setCreateTimestamp(now);
    entity.setUpdateTimestamp(now);
    return entity;
  }

  private Map<String, Object> reconcileCreate(long consumerClusterId, CreateRequest request,
      String requestHash) {
    ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(request.bindingId().toString());
    if (binding != null) {
      ServiceDependencySnapshotEntity snapshot = dependencyDAO.findSnapshot(
          binding.getBindingId(), binding.getDesiredSnapshotVersion());
      ServiceDependencyOperationEntity operation = dependencyDAO.findOperation(
          request.operationId().toString());
      if (sameImmutableSpec(binding, snapshot, consumerClusterId, request)
          && operation != null && binding.getBindingId().equals(operation.getBindingId())
          && requestHash.equals(operation.getRequestHash())) {
        return bindingResponse(binding, snapshot, operation);
      }
      throw new ManagedDependencyIntegrationException(409, "BINDING_ID_CONFLICT",
          "The binding UUID is already associated with a different immutable specification.");
    }
    if (dependencyDAO.findFence(request.bindingId().toString()) != null) {
      throw new ManagedDependencyIntegrationException(409, "BINDING_ID_RETIRED",
          "This binding UUID was detached and cannot be reused.");
    }
    ServiceDependencyBindingEntity owner = dependencyDAO.findByConsumerAndType(
        consumerClusterId, CONSUMER_SERVICE, request.type().name());
    if (owner != null) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_ALREADY_BOUND",
          "This HBASE service already has a managed dependency of the requested type.");
    }
    ServiceDependencyOperationEntity operation = dependencyDAO.findOperation(request.operationId().toString());
    if (operation != null) {
      throw new ManagedDependencyIntegrationException(409, "OPERATION_ID_CONFLICT",
          "The operation UUID is already associated with a different request.");
    }
    return null;
  }

  private Map<String, Object> reconcileExactRetry(long consumerClusterId, CreateRequest request) {
    ServiceDependencyOperationEntity operation = dependencyDAO.findOperation(request.operationId().toString());
    if (operation == null) {
      return null;
    }
    ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(operation.getBindingId());
    ServiceDependencySnapshotEntity snapshot = dependencyDAO.findSnapshot(
        operation.getBindingId(), operation.getTargetSnapshotVersion());
    String requestHash = immutableRequestHash(consumerClusterId, request);
    if (binding != null && request.bindingId().toString().equals(binding.getBindingId())
        && sameImmutableSpec(binding, snapshot, consumerClusterId, request)
        && requestHash.equals(operation.getRequestHash())) {
      verifyRetriedDraftOwnership(consumerClusterId, request.draft());
      return bindingResponse(binding, snapshot, operation);
    }
    throw new ManagedDependencyIntegrationException(409, "OPERATION_ID_CONFLICT",
        "The operation UUID is already associated with a different request.");
  }

  private void verifyRetriedDraftOwnership(long consumerClusterId, DraftReference draft) {
    if (draft == null) {
      return;
    }
    try {
      Map<String, Object> association = persistKeyValue.getCreationDraftCluster(draft.id().toString());
      if (((Number) association.get("cluster_id")).longValue() != consumerClusterId) {
        throw new ManagedDependencyIntegrationException(409, "DRAFT_MATERIALIZATION_MISMATCH",
            "The owned draft is not associated with this HBASE cluster.");
      }
    } catch (AuthorizationException e) {
      throw new ManagedDependencyIntegrationException(403, "DEPENDENCY_AUTHORIZATION_FAILED",
          "The authenticated user cannot reconcile this creation draft.", e);
    }
  }

  private boolean sameImmutableSpec(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity snapshot, long consumerClusterId, CreateRequest request) {
    return binding.getConsumerClusterId() == consumerClusterId
        && CONSUMER_SERVICE.equals(binding.getConsumerServiceName())
        && binding.getProviderClusterId() == request.provider().clusterId()
        && binding.getProviderServiceName().equals(request.provider().serviceName())
        && binding.getDependencyType().equals(request.type().name())
        && snapshot != null
        && snapshot.getProviderFingerprint().equals(request.expectedProviderFingerprint())
        && snapshot.getConsumerFingerprint().equals(request.expectedConsumerFingerprint())
        && snapshot.getSnapshotFingerprint().equals(request.expectedSnapshotFingerprint());
  }

  private Map<String, Object> previewResponse(UUID bindingId, Consumer consumer, Provider provider,
      ValidationResult result) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("binding_id", bindingId.toString());
    response.put("preview_schema_version", PREVIEW_SCHEMA_VERSION);
    response.put("dependency_type", provider.type().name());
    response.put("consumer", consumerSummary(consumer));
    response.put("provider", providerSummary(provider));
    response.put("compatible", result.isValid());
    response.put("errors", issues(result.issues()));
    result.snapshot().ifPresent(snapshot -> {
      response.put("consumer_descriptor_fingerprint", snapshot.consumerFingerprint());
      response.put("provider_fingerprint", snapshot.providerFingerprint());
      response.put("snapshot_fingerprint", snapshot.snapshotFingerprint());
      response.put("namespace", namespace(snapshot.namespace()));
      response.put("client_config", clientConfig(snapshot));
    });
    return response;
  }

  private Map<String, Object> bindingResponse(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity attemptedSnapshot,
      ServiceDependencyOperationEntity attemptedOperation) {
    ServiceDependencySnapshotEntity currentSnapshot = attemptedSnapshot != null
        && Objects.equals(binding.getDesiredSnapshotVersion(), attemptedSnapshot.getSnapshotVersion())
        ? attemptedSnapshot : dependencyDAO.findSnapshot(
            binding.getBindingId(), binding.getDesiredSnapshotVersion());
    Map<String, Object> response = new LinkedHashMap<>(bindingSummary(binding, currentSnapshot));
    if (currentSnapshot != null) {
      response.put("snapshot", snapshotSummary(currentSnapshot));
    }
    List<ServiceDependencyOperationEntity> operations = dependencyDAO.findOperations(
        binding.getBindingId());
    ServiceDependencyOperationEntity current = dependencyDAO.findOperation(
        binding.getActiveOperationId());
    if (current == null && attemptedOperation != null
        && binding.getActiveOperationId().equals(attemptedOperation.getOperationId())) {
      current = attemptedOperation;
    }
    if (current != null) {
      response.put("operation", operationSummary(current));
    }
    ServiceDependencyOperationEntity creation = operations.stream()
        .filter(value -> "CREATE".equals(value.getOperationKind()))
        .min(Comparator.comparingLong(ServiceDependencyOperationEntity::getOperationEpoch))
        .orElse("CREATE".equals(attemptedOperation == null ? null
            : attemptedOperation.getOperationKind()) ? attemptedOperation : null);
    if (creation != null) {
      response.put("creation_attempt", operationSummaryWithTarget(
          creation, attemptedSnapshot, attemptedOperation));
    }
    if (attemptedOperation != null && (current == null
        || !attemptedOperation.getOperationId().equals(current.getOperationId()))) {
      response.put("attempted_operation", operationSummaryWithTarget(
          attemptedOperation, attemptedSnapshot, attemptedOperation));
    }
    return response;
  }

  private Map<String, Object> bindingSummary(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity snapshotEntity) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("binding_id", binding.getBindingId());
    response.put("dependency_type", binding.getDependencyType());
    response.put("state", binding.getState());
    response.put("phase", binding.getProvisioningPhase());
    response.put("row_version", binding.getRowVersion());
    response.put("operation_epoch", binding.getOperationEpoch());
    response.put("ownership", "managed");
    response.put("consumer", Map.of(
        "cluster_id", binding.getConsumerClusterId(),
        "service_name", CONSUMER_SERVICE));
    Map<String, Object> provider = new LinkedHashMap<>();
    provider.put("cluster_id", binding.getProviderClusterId());
    provider.put("cluster_name", snapshotEntity == null ? null : snapshotEntity.getProviderDisplayName());
    provider.put("service_name", binding.getProviderServiceName());
    ManagedDependencySnapshot snapshot = readSnapshot(snapshotEntity);
    boolean snapshotCurrent = isCurrentSnapshot(binding, snapshotEntity, snapshot);
    if (snapshotCurrent) {
      provider.put("version", versionSummary(snapshot.providerVersion()));
      provider.put("security_mode", snapshot.securityMode().name());
      response.put("namespace", namespace(snapshot.namespace()));
      response.put("planned_hbase_user", snapshot.consumerIdentity().effectiveShortUser());
    }
    response.put("provider", provider);
    response.put("desired_snapshot_version", binding.getDesiredSnapshotVersion());
    response.put("applied_snapshot_version", binding.getAppliedSnapshotVersion());
    response.put("failure_code", binding.getFailureCode());
    response.put("failure_message", binding.getFailureMessage());
    response.put("failure_retryable", binding.getFailureRetryable());
    Map<String, Object> readiness = readinessSummary(binding);
    response.put("readiness", readiness);
    Map<String, Object> capabilities = capabilitySummary(
        binding, snapshot, readiness, snapshotCurrent);
    response.put("capabilities", capabilities);
    response.put("allowed_actions", capabilities.get("allowed_actions"));
    response.put("next_action", capabilities.get("next_action"));
    return response;
  }

  private Map<String, Object> operationSummary(ServiceDependencyOperationEntity operation) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("operation_id", operation.getOperationId());
    result.put("kind", operation.getOperationKind());
    result.put("state", operation.getState());
    result.put("epoch", operation.getOperationEpoch());
    result.put("target_snapshot_version", operation.getTargetSnapshotVersion());
    result.put("request_id", operation.getAmbariRequestId());
    result.put("failure_code", operation.getFailureCode());
    result.put("failure_message", operation.getFailureMessage());
    return result;
  }

  private Map<String, Object> operationSummaryWithTarget(
      ServiceDependencyOperationEntity operation,
      ServiceDependencySnapshotEntity attemptedSnapshot,
      ServiceDependencyOperationEntity attemptedOperation) {
    Map<String, Object> result = new LinkedHashMap<>(operationSummary(operation));
    ServiceDependencySnapshotEntity target = attemptedSnapshot != null
        && attemptedOperation != null
        && operation.getOperationId().equals(attemptedOperation.getOperationId())
        && Objects.equals(operation.getTargetSnapshotVersion(), attemptedSnapshot.getSnapshotVersion())
        ? attemptedSnapshot : dependencyDAO.findSnapshot(
            operation.getBindingId(), operation.getTargetSnapshotVersion());
    if (target != null) {
      result.put("target_snapshot", snapshotSummary(target));
    }
    return result;
  }

  private Map<String, Object> snapshotSummary(ServiceDependencySnapshotEntity snapshot) {
    return Map.of(
        "version", snapshot.getSnapshotVersion(),
        "schema_version", snapshot.getSchemaVersion(),
        "fingerprint", snapshot.getSnapshotFingerprint(),
        "consumer_fingerprint", snapshot.getConsumerFingerprint(),
        "provider_fingerprint", snapshot.getProviderFingerprint());
  }

  private boolean isCurrentSnapshot(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity entity, ManagedDependencySnapshot snapshot) {
    return entity != null && snapshot != null
        && binding.getBindingId().equals(entity.getBindingId())
        && Objects.equals(binding.getDesiredSnapshotVersion(), entity.getSnapshotVersion())
        && Objects.equals(entity.getSchemaVersion(), snapshot.schemaVersion())
        && Objects.equals(entity.getSnapshotVersion(), snapshot.snapshotVersion())
        && binding.getBindingId().equals(snapshot.bindingId().toString())
        && binding.getDependencyType().equals(snapshot.type().name())
        && Objects.equals(binding.getProviderClusterId(), snapshot.providerService().clusterId())
        && binding.getProviderServiceName().equals(snapshot.providerService().serviceName())
        && binding.getProviderFingerprint().equals(snapshot.providerFingerprint())
        && entity.getConsumerFingerprint().equals(snapshot.consumerFingerprint())
        && entity.getProviderFingerprint().equals(snapshot.providerFingerprint())
        && entity.getSnapshotFingerprint().equals(snapshot.snapshotFingerprint());
  }

  Map<String, Object> readinessSummary(ServiceDependencyBindingEntity binding) {
    List<ServiceDependencyHostResultEntity> commands = dependencyDAO.findHostResults(
        binding.getBindingId(), binding.getDesiredSnapshotVersion(), binding.getOperationEpoch());
    try {
      Cluster cluster = resolver.cluster(binding.getConsumerClusterId());
      return cluster.executeUnderReadLock(() -> readinessSummaryLocked(binding, cluster, commands));
    } catch (RuntimeException e) {
      return unavailableReadiness(binding, commands);
    }
  }

  /**
   * Validates a current HBase daemon START/RESTART target. This method is used
   * both while building a command and again in the action transaction; callers
   * must hold the consumer and all current provider parent locks for the final
   * check.
   */
  public void validateConsumerStart(Cluster cluster, long hostId) throws AmbariException {
    if (!cluster.getServices().containsKey(CONSUMER_SERVICE)) {
      return;
    }
    List<ServiceDependencyBindingEntity> bindings = dependencyDAO.findByConsumer(
        cluster.getClusterId(), CONSUMER_SERVICE);
    if (bindings.isEmpty()) {
      if (!dependencyDAO.findFencesByConsumer(cluster.getClusterId(), CONSUMER_SERVICE).isEmpty()) {
        throw lifecycleConflict("DEPENDENCY_CONSUMER_FENCED",
            "The HBase dependency was detached and must be reviewed before it can start.");
      }
      return;
    }

    for (ServiceDependencyBindingEntity binding : bindings) {
      try {
        validateDispatchState(binding);
      } catch (IllegalStateException e) {
        throw lifecycleConflict("DEPENDENCY_CONSUMER_NOT_READY",
            "HBase cannot start until every managed dependency is current.", e);
      }
      List<ServiceDependencyHostResultEntity> commands = dependencyDAO.findHostResults(
          binding.getBindingId(), binding.getDesiredSnapshotVersion(), binding.getOperationEpoch());
      Map<String, Object> readiness = readinessSummaryLocked(binding, cluster, commands);
      @SuppressWarnings("unchecked")
      List<Long> requiredHosts = (List<Long>) readiness.get("required_daemon_host_ids");
      if (!"READY".equals(binding.getState())
          || !Objects.equals(binding.getDesiredSnapshotVersion(), binding.getAppliedSnapshotVersion())
          || !Objects.equals(binding.getProviderFingerprint(), binding.getAppliedProviderFingerprint())
          || !Boolean.TRUE.equals(readiness.get("topology_current"))
          || !Boolean.TRUE.equals(readiness.get("all_current_daemons_verified"))
          || Boolean.TRUE.equals(readiness.get("active_command"))
          || !requiredHosts.contains(hostId)) {
        throw lifecycleConflict("DEPENDENCY_CONSUMER_NOT_READY",
            "HBase cannot start until every current daemon has exact current dependency evidence.");
      }
    }
  }

  private ManagedDependencyIntegrationException lifecycleConflict(String code, String message) {
    return new ManagedDependencyIntegrationException(409, code, message);
  }

  private ManagedDependencyIntegrationException lifecycleConflict(String code, String message,
      Throwable cause) {
    return new ManagedDependencyIntegrationException(409, code, message, cause);
  }

  private Map<String, Object> readinessSummaryLocked(ServiceDependencyBindingEntity binding,
      Cluster cluster, List<ServiceDependencyHostResultEntity> commands) {
    Map<String, Object> result = new LinkedHashMap<>();
    Set<Long> required = new java.util.TreeSet<>();
    try {
      Service hbase = cluster.getService(CONSUMER_SERVICE);
      for (String componentName : Set.of(
          "HBASE_MASTER", "HBASE_REGIONSERVER", "HBASE_THRIFT")) {
        ServiceComponent component = hbase.getServiceComponents().get(componentName);
        if (component != null) {
          component.getServiceComponentHosts().values().stream()
              .map(ServiceComponentHost::getHost)
              .mapToLong(host -> host.getHostId())
              .forEach(required::add);
        }
      }
    } catch (org.apache.ambari.server.AmbariException e) {
      throw new IllegalStateException("The current HBase topology is unavailable", e);
    }
    Map<Long, ServiceDependencyHostResultEntity> preparations = new LinkedHashMap<>();
    Map<Long, ServiceDependencyHostResultEntity> verifications = new LinkedHashMap<>();
    boolean activeCommand = false;
    for (ServiceDependencyHostResultEntity command : commands) {
      activeCommand |= Set.of("INTENT", "SCHEDULING", "DISPATCHED").contains(command.getState());
      if (command.getCheckKind().equals("PREPARE_" + binding.getDependencyType() + "_CONSUMER")) {
        preparations.put(command.getHostId(), command);
      } else if (command.getCheckKind().equals(
          "VERIFY_" + binding.getDependencyType() + "_CONSUMER")) {
        verifications.put(command.getHostId(), command);
      }
    }
    Set<Long> prepared = new java.util.TreeSet<>();
    Set<Long> verified = new java.util.TreeSet<>();
    for (Long hostId : required) {
      ServiceDependencyHostResultEntity preparation = preparations.get(hostId);
      ServiceDependencyHostResultEntity verification = verifications.get(hostId);
      if (preparation != null && "SUCCEEDED".equals(preparation.getState())
          && Objects.equals(binding.getOperationEpoch(), preparation.getOperationEpoch())
          && preparation.getPreparationObservationId() != null
          && preparation.getCommandRequestHash() != null
          && preparation.getPreparationObservationFingerprint() != null
          && preparation.getPackageName() != null
          && preparation.getPackageVersion() != null
          && preparation.getClientSoftwareVersion() != null
          && preparation.getObservedPackageHash() != null
          && preparation.getRenderedConfigHash() != null
          && preparation.getIdentityFingerprint() != null
          && Objects.equals(binding.getActiveOperationId(), preparation.getOperationId())) {
        prepared.add(hostId);
        if (verification != null && "SUCCEEDED".equals(verification.getState())
            && Objects.equals(binding.getOperationEpoch(), verification.getOperationEpoch())
            && Objects.equals(preparation.getPreparationObservationId(),
                verification.getPreparationObservationId())
            && Objects.equals(preparation.getCommandRequestHash(),
                verification.getPreparationRequestHash())
            && Objects.equals(preparation.getPreparationObservationFingerprint(),
                verification.getPreparationObservationFingerprint())
            && Objects.equals(preparation.getPackageName(), verification.getPackageName())
            && Objects.equals(preparation.getPackageVersion(), verification.getPackageVersion())
            && Objects.equals(preparation.getClientSoftwareVersion(),
                verification.getClientSoftwareVersion())
            && Objects.equals(preparation.getObservedPackageHash(),
                verification.getObservedPackageHash())
            && Objects.equals(preparation.getRenderedConfigHash(),
                verification.getRenderedConfigHash())
            && Objects.equals(preparation.getIdentityFingerprint(),
                verification.getIdentityFingerprint())
            && Objects.equals(binding.getActiveOperationId(), verification.getOperationId())) {
          verified.add(hostId);
        }
      }
    }
    List<Map<String, Object>> preparationRequests = commands.stream()
        .filter(command -> Objects.equals(command.getSnapshotVersion(),
            binding.getDesiredSnapshotVersion())
            && Objects.equals(command.getOperationEpoch(), binding.getOperationEpoch())
            && command.getCheckKind().equals(
                "PREPARE_" + binding.getDependencyType() + "_CONSUMER"))
        .map(this::preparationRequestSummary)
        .toList();
    result.put("topology_current", !required.isEmpty());
    result.put("required_daemon_host_ids", List.copyOf(required));
    result.put("prepared_daemon_host_ids", List.copyOf(prepared));
    result.put("verified_daemon_host_ids", List.copyOf(verified));
    result.put("all_current_daemons_prepared",
        !required.isEmpty() && prepared.equals(required));
    result.put("all_current_daemons_verified",
        !required.isEmpty() && verified.equals(required));
    result.put("active_command", activeCommand);
    result.put("manual_credentials", !required.isEmpty() && required.stream().allMatch(hostId -> {
      ServiceDependencyHostResultEntity preparation = preparations.get(hostId);
      ManagedDependencyCredentialManager.Plan plan = preparation == null ? null
          : ManagedDependencyCredentialManager.plan(preparation);
      return plan != null && plan.manual();
    }));
    result.put("preparation_requests", preparationRequests);
    return result;
  }

  private Map<String, Object> preparationRequestSummary(ServiceDependencyHostResultEntity command) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("binding_id", command.getBindingId());
    result.put("operation_id", command.getOperationId());
    result.put("epoch", command.getOperationEpoch());
    result.put("snapshot_version", command.getSnapshotVersion());
    result.put("host_id", command.getHostId());
    result.put("component_name", command.getComponentName());
    result.put("request_id", command.getAmbariRequestId());
    result.put("task_id", command.getAmbariTaskId());
    result.put("state", command.getState());
    return result;
  }

  private Map<String, Object> unavailableReadiness(
      ServiceDependencyBindingEntity binding, List<ServiceDependencyHostResultEntity> commands) {
    boolean activeCommand = commands.stream().anyMatch(command ->
        Set.of("INTENT", "SCHEDULING", "DISPATCHED").contains(command.getState()));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("topology_current", false);
    result.put("required_daemon_host_ids", List.of());
    result.put("prepared_daemon_host_ids", List.of());
    result.put("verified_daemon_host_ids", List.of());
    result.put("all_current_daemons_prepared", false);
    result.put("all_current_daemons_verified", false);
    result.put("active_command", activeCommand);
    result.put("preparation_requests", commands.stream()
        .filter(command -> Objects.equals(command.getSnapshotVersion(),
            binding.getDesiredSnapshotVersion())
            && Objects.equals(command.getOperationEpoch(), binding.getOperationEpoch())
            && command.getCheckKind().startsWith("PREPARE_"))
        .map(this::preparationRequestSummary)
        .toList());
    return result;
  }

  Map<String, Object> capabilitySummary(ServiceDependencyBindingEntity binding,
      ManagedDependencySnapshot snapshot, Map<String, Object> readiness,
      boolean snapshotCurrent) {
    boolean approved = "APPROVED".equals(binding.getSnapshotApproval());
    boolean topologyCurrent = Boolean.TRUE.equals(readiness.get("topology_current"));
    boolean trustedCurrent = approved && snapshotCurrent && topologyCurrent;
    boolean providerPrepared = approved && snapshotCurrent
        && binding.getProviderPreparationHash() != null;
    boolean allPrepared = Boolean.TRUE.equals(readiness.get("all_current_daemons_prepared"));
    boolean allVerified = Boolean.TRUE.equals(readiness.get("all_current_daemons_verified"));
    boolean exactApplied = Objects.equals(binding.getAppliedSnapshotVersion(),
        binding.getDesiredSnapshotVersion()) && Objects.equals(
            binding.getAppliedProviderFingerprint(), binding.getProviderFingerprint());
    boolean secure = snapshot != null
        && snapshot.securityMode() == ManagedDependencySecurityMode.KERBEROS;
    boolean activeCommand = Boolean.TRUE.equals(readiness.get("active_command"));
    boolean reconcilingZooKeeper = trustedCurrent
        && ManagedDependencyType.ZOOKEEPER.name().equals(binding.getDependencyType())
        && "FENCING_UNCERTAIN".equals(binding.getState())
        && "ZOOKEEPER_HANDOFF_RECONCILING".equals(binding.getProvisioningPhase())
        && "DEPENDENCY_ZOOKEEPER_HANDOFF_RECONCILIATION_REQUIRED"
            .equals(binding.getFailureCode());
    boolean normalOperationalState = Set.of("PROVISIONING", "READY")
        .contains(binding.getState());
    boolean startAllowed = trustedCurrent && "READY".equals(binding.getState())
        && exactApplied && allVerified && !activeCommand;
    boolean retryCandidate = trustedCurrent && "FAILED".equals(binding.getState())
        && Boolean.TRUE.equals(binding.getFailureRetryable()) && providerPrepared;
    boolean retryAllowed = false;
    if (retryCandidate && !activeCommand) {
      try {
        retryAllowed = !retryPreparationCommands(binding,
            UUID.fromString(binding.getActiveOperationId()), binding.getOperationEpoch() + 1).isEmpty();
      } catch (ManagedDependencyIntegrationException e) {
        retryAllowed = false;
      }
    }
    boolean installAllowed = trustedCurrent && !activeCommand
        && (providerPrepared || reconcilingZooKeeper)
        && ("PROVISIONING".equals(binding.getState())
            || "READY".equals(binding.getState()) && !allVerified
            || reconcilingZooKeeper);
    String credentialStatus = !snapshotCurrent ? "UNKNOWN"
        : !secure ? "NOT_REQUIRED"
            : "CONSUMER_CREDENTIALS_REQUIRED".equals(binding.getProvisioningPhase())
                ? "REQUIRED"
                : Set.of("CONSUMER_VERIFYING", "READY")
                    .contains(binding.getProvisioningPhase()) ? "ISSUED" : "UNKNOWN";
    boolean credentialsRequired = trustedCurrent && "PROVISIONING".equals(binding.getState())
        && allPrepared
        && "REQUIRED".equals(credentialStatus) && !activeCommand;
    boolean retryDetach = "DETACHING".equals(binding.getState())
        && Boolean.TRUE.equals(binding.getFailureRetryable()) && !activeCommand;
    boolean detachAllowed = approved && snapshotCurrent
        && Set.of("PROVISIONING", "READY", "FAILED", "STALE", "FENCING_UNCERTAIN", "DETACHING")
            .contains(binding.getState())
        && (!"DETACHING".equals(binding.getState()) || retryDetach)
        && binding.getActionHostId() != null && !activeCommand
        && lifecyclePolicy.consumerDetachAllowed(resolver.cluster(binding.getConsumerClusterId()));
    List<String> actions = new ArrayList<>();
    if (installAllowed) {
      actions.add("INSTALL_OR_CONFIGURE");
    }
    if (credentialsRequired) {
      actions.add("ISSUE_CREDENTIALS");
    }
    if (startAllowed) {
      actions.add("START_OR_RESTART");
    }
    if (retryAllowed) {
      actions.add("RETRY");
    }
    if (detachAllowed) {
      actions.add("DETACH");
    }
    String nextAction;
    if ("DETACHING".equals(binding.getState())) {
      nextAction = retryDetach ? "RETRY_DETACH" : "WAIT_FOR_DETACH";
    } else if (!approved || !snapshotCurrent) {
      nextAction = "REVIEW_APPROVAL";
    } else if (!topologyCurrent) {
      nextAction = "STATUS_UNAVAILABLE";
    } else if ("STALE".equals(binding.getState())) {
      nextAction = "REVIEW_STALE";
    } else if ("FENCING_UNCERTAIN".equals(binding.getState())
        && !reconcilingZooKeeper) {
      nextAction = "REVIEW_FENCING";
    } else if ("FAILED".equals(binding.getState())) {
      nextAction = retryAllowed ? "RETRY"
          : retryCandidate ? "RETRY_UNAVAILABLE" : "REVIEW_FAILURE";
    } else if (!normalOperationalState && !reconcilingZooKeeper) {
      nextAction = "STATUS_UNAVAILABLE";
    } else if (reconcilingZooKeeper) {
      nextAction = allPrepared
          ? "CREDENTIAL_STATUS_UNAVAILABLE" : "INSTALL_OR_CONFIGURE";
    } else if (!providerPrepared) {
      nextAction = "WAIT_FOR_PROVIDER_PREPARATION";
    } else if (!allPrepared) {
      nextAction = "INSTALL_OR_CONFIGURE";
    } else if (credentialsRequired) {
      nextAction = "ISSUE_CREDENTIALS";
    } else if (secure && allPrepared && "UNKNOWN".equals(credentialStatus)) {
      nextAction = "CREDENTIAL_STATUS_UNAVAILABLE";
    } else if (!allVerified && normalOperationalState) {
      nextAction = "VERIFY";
    } else if (normalOperationalState) {
      nextAction = startAllowed ? "START_OR_RESTART" : "WAIT_FOR_READY_COMMIT";
    } else {
      nextAction = "STATUS_UNAVAILABLE";
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("provider_prepared", providerPrepared);
    result.put("install_or_configure_allowed", installAllowed);
    result.put("credential_status", credentialStatus);
    result.put("credentials_required", credentialsRequired);
    result.put("manual_credentials_allowed", credentialsRequired
        && Boolean.TRUE.equals(readiness.get("manual_credentials")));
    result.put("start_or_restart_allowed", startAllowed);
    result.put("retry_allowed", retryAllowed);
    result.put("detach_allowed", detachAllowed);
    result.put("allowed_actions", List.copyOf(actions));
    result.put("next_action", nextAction);
    return result;
  }

  private Map<String, Object> consumerSummary(Consumer consumer) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("scope", consumer.sourceScope());
    if (consumer.clusterId() != null) {
      result.put("cluster_id", consumer.clusterId());
      result.put("cluster_name", consumer.clusterName());
    }
    result.put("service_name", consumer.serviceName());
    result.put("lifecycle", consumer.lifecycle().name());
    result.put("planned_hbase_user", consumer.identity().effectiveShortUser());
    return result;
  }

  private Map<String, Object> providerSummary(Provider provider) {
    Cluster cluster = resolver.cluster(provider.serviceKey().clusterId());
    return Map.of(
        "cluster_id", provider.serviceKey().clusterId(),
        "cluster_name", cluster.getClusterName(),
        "service_name", provider.serviceKey().serviceName(),
        "version", versionSummary(provider.version().compatibility()),
        "installed", provider.installed(),
        "healthy", provider.healthy(),
        "security_mode", provider.securityMode().name());
  }

  private Map<String, Object> versionSummary(ManagedDependencyVersion.Compatibility version) {
    return Map.of(
        "stack_name", version.stackName(),
        "stack_version", version.stackVersion(),
        "active", version.active(),
        "service_version", version.serviceVersion(),
        "resolved_versions", version.resolvedVersions(),
        "client_features", version.clientFeatures());
  }

  private Map<String, Object> namespace(ManagedDependencyNamespace namespace) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (!namespace.rootUri().isEmpty()) {
      result.put("root_uri", namespace.rootUri());
      result.put("wal_uri", namespace.walUri());
    } else {
      result.put("container_znode", ManagedDependencyNamespace.zooKeeperContainer(
          UUID.fromString(namespace.znode().split("/")[2])));
      result.put("znode", namespace.znode());
    }
    return result;
  }

  private Map<String, Object> clientConfig(ManagedDependencySnapshot snapshot) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (!snapshot.coreSite().isEmpty()) {
      result.put("core-site", snapshot.coreSite());
    }
    if (!snapshot.hdfsSite().isEmpty()) {
      result.put("hdfs-site", snapshot.hdfsSite());
    }
    if (!snapshot.zooKeeperClient().isEmpty()) {
      result.put("hbase-site", snapshot.zooKeeperClient());
    }
    return result;
  }

  private List<Map<String, String>> issues(List<Issue> issues) {
    return issues.stream().map(issue -> Map.of(
        "code", issue.code().name(), "message", issue.message())).toList();
  }

  private String immutableRequestHash(long consumerClusterId, CreateRequest request) {
    return hash(Long.toString(consumerClusterId), CONSUMER_SERVICE, request.bindingId().toString(),
        request.type().name(), Long.toString(request.provider().clusterId()),
        request.provider().serviceName(), request.expectedProviderFingerprint(),
        request.expectedConsumerFingerprint(), request.expectedSnapshotFingerprint(),
        Integer.toString(request.previewSchemaVersion()),
        request.operationId().toString(), request.draft() == null ? "" : request.draft().id().toString(),
        request.draft() == null ? "" : Long.toString(request.draft().revision()));
  }

  private String updateRequestHash(long consumerClusterId, UUID bindingId,
      UpdateRequest request) {
    return hash(Long.toString(consumerClusterId), CONSUMER_SERVICE, bindingId.toString(), "UPDATE",
        request.operationId().toString(), Long.toString(request.expectedRowVersion()),
        Long.toString(request.expectedSnapshotVersion()), request.expectedProviderFingerprint(),
        request.expectedConsumerFingerprint(), request.expectedSnapshotFingerprint(),
        request.expectedSnapshotApproval(), Integer.toString(request.previewSchemaVersion()));
  }

  private ServiceDependencyBindingEntity exactOwnedBinding(long consumerClusterId,
      UUID bindingId) {
    ServiceDependencyBindingEntity binding = dependencyDAO.findBinding(bindingId.toString());
    requireOwnedBinding(binding, consumerClusterId, bindingId);
    return binding;
  }

  private void requireOwnedBinding(ServiceDependencyBindingEntity binding,
      long consumerClusterId, UUID bindingId) {
    if (binding == null || !bindingId.toString().equals(binding.getBindingId())
        || !Objects.equals(binding.getConsumerClusterId(), consumerClusterId)
        || !CONSUMER_SERVICE.equals(binding.getConsumerServiceName())) {
      throw notFound();
    }
  }

  private ServiceDependencyOperationEntity lifecycleOperation(
      ServiceDependencyBindingEntity binding, LifecycleRequest request,
      String kind, String requestHash) {
    ServiceDependencyOperationEntity existing = dependencyDAO.findOperation(
        request.operationId().toString());
    if (existing != null) {
      if (!binding.getBindingId().equals(existing.getBindingId())
          || !kind.equals(existing.getOperationKind())
          || !requestHash.equals(existing.getRequestHash())
          || !Objects.equals(binding.getDesiredSnapshotVersion(),
              existing.getTargetSnapshotVersion())) {
        throw new ManagedDependencyIntegrationException(409, "OPERATION_ID_CONFLICT",
            "The operation UUID is already associated with another dependency request.");
      }
      return existing;
    }
    long now = System.currentTimeMillis();
    ServiceDependencyOperationEntity operation = new ServiceDependencyOperationEntity();
    operation.setOperationId(request.operationId().toString());
    operation.setBindingId(binding.getBindingId());
    operation.setOperationKind(kind);
    operation.setOperationEpoch(binding.getOperationEpoch() + 1);
    operation.setTargetSnapshotVersion(binding.getDesiredSnapshotVersion());
    operation.setRequestHash(requestHash);
    operation.setState("QUEUED");
    operation.setCreateTimestamp(now);
    operation.setUpdateTimestamp(now);
    return operation;
  }

  private String lifecycleRequestHash(long consumerClusterId, UUID bindingId,
      LifecycleRequest request, String action) {
    return hash(Long.toString(consumerClusterId), CONSUMER_SERVICE, bindingId.toString(),
        action, request.operationId().toString(), Long.toString(request.expectedRowVersion()));
  }

  private Map<String, Object> reconcileDetached(long consumerClusterId, UUID bindingId,
      LifecycleRequest request) {
    ServiceDependencyFenceEntity fence = dependencyDAO.findFence(bindingId.toString());
    if (fence == null || !Objects.equals(fence.getConsumerClusterId(), consumerClusterId)
        || !CONSUMER_SERVICE.equals(fence.getConsumerServiceName())) {
      throw notFound();
    }
    String requestHash = lifecycleRequestHash(
        consumerClusterId, bindingId, request, "DETACH");
    if (!request.operationId().toString().equals(fence.getDetachOperationId())
        || !requestHash.equals(fence.getDetachRequestHash())) {
      throw new ManagedDependencyIntegrationException(409, "BINDING_ID_RETIRED",
          "This binding UUID has already been detached and cannot be reused.");
    }
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("binding_id", bindingId.toString());
    response.put("dependency_type", fence.getDependencyType());
    response.put("state", "RETIRED");
    response.put("ownership", "managed");
    response.put("detached", true);
    response.put("operation", Map.of(
        "operation_id", fence.getDetachOperationId(),
        "kind", "DETACH", "state", "SUCCEEDED", "epoch", fence.getFinalEpoch()));
    return response;
  }

  private static String hash(String... values) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (String value : values) {
        byte[] bytes = Objects.requireNonNullElse(value, "").getBytes(StandardCharsets.UTF_8);
        digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) ':');
        digest.update(bytes);
      }
      return "sha256:" + HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
    }
  }

  private record CandidateEvaluation(Provider provider, ValidationResult validation,
      boolean requiresCompleteSelection) {
  }

  private int authenticatedUserId() {
    int userId = AuthorizationHelper.getAuthenticatedId();
    if (userId <= 0) {
      throw new ManagedDependencyIntegrationException(403, "DEPENDENCY_AUTHORIZATION_FAILED",
          "An active authenticated user is required.");
    }
    return userId;
  }

  private void authorize(Cluster cluster, Set<RoleAuthorization> required) {
    try {
      AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, cluster.getResourceId(), required);
    } catch (AuthorizationException e) {
      throw new ManagedDependencyIntegrationException(403, "DEPENDENCY_AUTHORIZATION_FAILED",
          "The authenticated user is not authorized for this cluster operation.", e);
    }
  }

  private boolean can(Cluster cluster, Set<RoleAuthorization> required) {
    return AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), required);
  }

  private void verifyAmbari(RoleAuthorization required) {
    try {
      AuthorizationHelper.verifyAuthorization(ResourceType.AMBARI, null, Set.of(required));
    } catch (AuthorizationException e) {
      throw new ManagedDependencyIntegrationException(403, "DEPENDENCY_AUTHORIZATION_FAILED",
          "The authenticated user is not authorized for this Ambari operation.", e);
    }
  }

  private String emptyToNull(String value) {
    return value == null || value.isEmpty() ? null : value;
  }

  private ManagedDependencyIntegrationException issue(Issue issue) {
    return unprocessable(issue.code().name(), issue.message());
  }

  private ManagedDependencyIntegrationException badRequest(String code, String message) {
    return new ManagedDependencyIntegrationException(400, code, message);
  }

  private ManagedDependencyIntegrationException unprocessable(String code, String message) {
    return new ManagedDependencyIntegrationException(422, code, message);
  }

  private ManagedDependencyIntegrationException notFound() {
    return new ManagedDependencyIntegrationException(404, "DEPENDENCY_NOT_FOUND",
        "The requested managed dependency does not exist.");
  }

  public record ConsumerReference(UUID draftId, Long clusterId, long expectedDraftRevision,
      boolean servicePlan) {
    public ConsumerReference {
      if ((draftId == null) == (clusterId == null)) {
        throw new IllegalArgumentException("Exactly one consumer scope is required");
      }
      if (servicePlan && (draftId != null || expectedDraftRevision < 1)) {
        throw new IllegalArgumentException(
            "A service plan requires a cluster and positive workflow revision");
      }
    }

    public static ConsumerReference draft(UUID draftId, long revision) {
      return new ConsumerReference(draftId, null, revision, false);
    }

    public static ConsumerReference service(long clusterId) {
      return new ConsumerReference(null, clusterId, 0, false);
    }

    public static ConsumerReference servicePlan(long clusterId, long revision) {
      return new ConsumerReference(null, clusterId, revision, true);
    }
  }

  public record ProviderReference(long clusterId, String serviceName) {
    public ProviderReference {
      if (clusterId <= 0 || serviceName == null || serviceName.isBlank()) {
        throw new IllegalArgumentException("A provider cluster and service are required");
      }
    }

    ManagedDependencyServiceKey serviceKey() {
      return new ManagedDependencyServiceKey(clusterId, serviceName);
    }
  }

  /** One provider selection in a complete preview request. */
  public record PreviewSelection(ManagedDependencyType type, ProviderReference provider,
      UUID bindingId) {
    public PreviewSelection {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(provider, "provider");
    }
  }

  public record DraftReference(UUID id, long revision) {
    public DraftReference {
      Objects.requireNonNull(id, "id");
      if (revision < 1) {
        throw new IllegalArgumentException("Draft revision must be positive");
      }
    }
  }

  /** Trusted, minimal facts consumed by request-local Stack Advisor planning. */
  public record AdvisorSelection(UUID bindingId, ManagedDependencyType type,
      Long consumerClusterId, String consumerServiceName,
      long providerClusterId, String providerServiceName, int previewSchemaVersion,
      String consumerStackName, String consumerStackVersion,
      String providerFingerprint, String consumerDescriptorFingerprint,
      String snapshotFingerprint) {
    public AdvisorSelection {
      Objects.requireNonNull(bindingId, "bindingId");
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(consumerServiceName, "consumerServiceName");
      Objects.requireNonNull(providerServiceName, "providerServiceName");
      Objects.requireNonNull(consumerStackName, "consumerStackName");
      Objects.requireNonNull(consumerStackVersion, "consumerStackVersion");
      Objects.requireNonNull(providerFingerprint, "providerFingerprint");
      Objects.requireNonNull(consumerDescriptorFingerprint, "consumerDescriptorFingerprint");
      Objects.requireNonNull(snapshotFingerprint, "snapshotFingerprint");
    }
  }

  /** Caller-supplied identity and provider key for one complete advisor item. */
  public record AdvisorSelectionRequest(ManagedDependencyType type,
      ProviderReference provider, UUID bindingId) {
    public AdvisorSelectionRequest {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(provider, "provider");
      Objects.requireNonNull(bindingId, "bindingId");
    }
  }

  public record CreateRequest(UUID bindingId, ManagedDependencyType type,
      ProviderReference provider, String expectedProviderFingerprint,
      String expectedConsumerFingerprint, String expectedSnapshotFingerprint,
      int previewSchemaVersion,
      UUID operationId, DraftReference draft) {
    public CreateRequest {
      Objects.requireNonNull(bindingId, "bindingId");
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(provider, "provider");
      Objects.requireNonNull(expectedProviderFingerprint, "expectedProviderFingerprint");
      Objects.requireNonNull(expectedConsumerFingerprint, "expectedConsumerFingerprint");
      Objects.requireNonNull(expectedSnapshotFingerprint, "expectedSnapshotFingerprint");
      Objects.requireNonNull(operationId, "operationId");
    }
  }

  /** Facts echoed by preview-update; no provider or client configuration is accepted. */
  public record UpdateRequest(UUID operationId, long expectedRowVersion,
      long expectedSnapshotVersion, String expectedProviderFingerprint,
      String expectedConsumerFingerprint, String expectedSnapshotFingerprint,
      String expectedSnapshotApproval, int previewSchemaVersion) {
    public UpdateRequest {
      Objects.requireNonNull(operationId, "operationId");
      if (expectedRowVersion < 0 || expectedSnapshotVersion <= 0) {
        throw new IllegalArgumentException("Update row and snapshot versions are invalid");
      }
      Objects.requireNonNull(expectedProviderFingerprint, "expectedProviderFingerprint");
      Objects.requireNonNull(expectedConsumerFingerprint, "expectedConsumerFingerprint");
      Objects.requireNonNull(expectedSnapshotFingerprint, "expectedSnapshotFingerprint");
      if (!"APPROVED".equals(expectedSnapshotApproval)) {
        throw new IllegalArgumentException("Only an approved dependency snapshot can be updated");
      }
      if (previewSchemaVersion <= 0) {
        throw new IllegalArgumentException("A positive preview schema version is required");
      }
    }
  }

  public record LifecycleRequest(UUID operationId, long expectedRowVersion) {
    public LifecycleRequest {
      Objects.requireNonNull(operationId, "operationId");
      if (expectedRowVersion < 0) {
        throw new IllegalArgumentException("Expected row version must be non-negative");
      }
    }
  }

  private enum ReadLevel {
    VIEW,
    MODIFY
  }

  private record UpdateCalculation(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity currentEntity, ManagedDependencySnapshot current,
      Consumer consumer, Provider provider, ManagedDependencySnapshot nextSnapshot,
      ServiceDependencySnapshotEntity newSnapshotEntity) {
  }

  private record UpdateExecution(Map<String, Object> response, LifecycleTransition transition) {
  }

  private record RetryExecution(Map<String, Object> response, LifecycleTransition transition,
      List<ServiceDependencyHostResultEntity> commands) {
  }
}
