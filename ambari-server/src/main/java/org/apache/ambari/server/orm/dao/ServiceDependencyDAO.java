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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyServiceKey;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySnapshot;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ScopedWorkflowStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyFenceEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntityPK;
import org.apache.ambari.server.orm.entities.ServiceDependencyOperationEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntityPK;
import org.apache.ambari.server.utils.StageUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ServiceDependencyDAO {
  private static final Comparator<ManagedDependencyServiceKey> SERVICE_KEY_ORDER =
      Comparator.comparingLong(ManagedDependencyServiceKey::clusterId)
          .thenComparing(ManagedDependencyServiceKey::serviceName);

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @RequiresSession
  public ServiceDependencyBindingEntity findBinding(String bindingId) {
    return entityManagerProvider.get().find(ServiceDependencyBindingEntity.class, bindingId);
  }

  @RequiresSession
  public ServiceDependencySnapshotEntity findSnapshot(String bindingId, long snapshotVersion) {
    return entityManagerProvider.get().find(ServiceDependencySnapshotEntity.class,
        new ServiceDependencySnapshotEntityPK(bindingId, snapshotVersion));
  }

  @RequiresSession
  public ServiceDependencyOperationEntity findOperation(String operationId) {
    return entityManagerProvider.get().find(ServiceDependencyOperationEntity.class, operationId);
  }

  @RequiresSession
  public ServiceDependencyHostResultEntity findHostResult(String bindingId, long snapshotVersion,
      long operationEpoch, long hostId, String dependencyType, String checkKind) {
    return entityManagerProvider.get().find(ServiceDependencyHostResultEntity.class,
        new ServiceDependencyHostResultEntityPK(
            bindingId, snapshotVersion, operationEpoch, hostId, dependencyType, checkKind));
  }

  @RequiresSession
  public List<ServiceDependencyHostResultEntity> findOutstandingCommands() {
    return entityManagerProvider.get().createNamedQuery(
        "ServiceDependencyHostResultEntity.findOutstanding",
        ServiceDependencyHostResultEntity.class).getResultList();
  }

  @RequiresSession
  public List<ServiceDependencyHostResultEntity> findHostResults(
      String bindingId, long snapshotVersion) {
    return entityManagerProvider.get().createQuery(
        "SELECT result FROM ServiceDependencyHostResultEntity result "
            + "WHERE result.bindingId=:bindingId AND result.snapshotVersion=:snapshotVersion "
            + "ORDER BY result.hostId, result.dependencyType, result.checkKind",
        ServiceDependencyHostResultEntity.class)
        .setParameter("bindingId", bindingId)
        .setParameter("snapshotVersion", snapshotVersion)
        .getResultList();
  }

  @RequiresSession
  public List<ServiceDependencyHostResultEntity> findHostResults(
      String bindingId, long snapshotVersion, long operationEpoch) {
    return entityManagerProvider.get().createQuery(
        "SELECT result FROM ServiceDependencyHostResultEntity result "
            + "WHERE result.bindingId=:bindingId AND result.snapshotVersion=:snapshotVersion "
            + "AND result.operationEpoch=:operationEpoch "
            + "ORDER BY result.hostId, result.dependencyType, result.checkKind",
        ServiceDependencyHostResultEntity.class)
        .setParameter("bindingId", bindingId)
        .setParameter("snapshotVersion", snapshotVersion)
        .setParameter("operationEpoch", operationEpoch)
        .getResultList();
  }

  @RequiresSession
  public List<ServiceDependencyHostResultEntity> findHostResultsByTask(long taskId) {
    return entityManagerProvider.get().createNamedQuery(
        "ServiceDependencyHostResultEntity.findByTask", ServiceDependencyHostResultEntity.class)
        .setParameter("taskId", taskId)
        .getResultList();
  }

  @RequiresSession
  public ServiceDependencyFenceEntity findFence(String bindingId) {
    return entityManagerProvider.get().find(ServiceDependencyFenceEntity.class, bindingId);
  }

  @RequiresSession
  public List<ServiceDependencyFenceEntity> findFencesByConsumer(long clusterId,
      String serviceName) {
    return entityManagerProvider.get().createNamedQuery(
        "ServiceDependencyFenceEntity.findByConsumer", ServiceDependencyFenceEntity.class)
        .setParameter("clusterId", clusterId)
        .setParameter("serviceName", serviceName)
        .getResultList();
  }

  @RequiresSession
  public List<ServiceDependencyBindingEntity> findAllBindings() {
    return entityManagerProvider.get().createQuery(
        "SELECT binding FROM ServiceDependencyBindingEntity binding ORDER BY binding.bindingId",
        ServiceDependencyBindingEntity.class).getResultList();
  }

  @RequiresSession
  public List<ServiceDependencyOperationEntity> findOperations(String bindingId) {
    return entityManagerProvider.get().createNamedQuery(
        "ServiceDependencyOperationEntity.findByBinding", ServiceDependencyOperationEntity.class)
        .setParameter("bindingId", bindingId)
        .getResultList();
  }

  @RequiresSession
  public List<ServiceDependencyBindingEntity> findByConsumer(long clusterId, String serviceName) {
    return entityManagerProvider.get().createNamedQuery(
        "ServiceDependencyBindingEntity.findByConsumer", ServiceDependencyBindingEntity.class)
        .setParameter("clusterId", clusterId)
        .setParameter("serviceName", serviceName)
        .getResultList();
  }

  @RequiresSession
  public List<ServiceDependencyBindingEntity> findByProvider(long clusterId, String serviceName) {
    return entityManagerProvider.get().createNamedQuery(
        "ServiceDependencyBindingEntity.findByProvider", ServiceDependencyBindingEntity.class)
        .setParameter("clusterId", clusterId)
        .setParameter("serviceName", serviceName)
        .getResultList();
  }

  @RequiresSession
  public ServiceDependencyBindingEntity findByConsumerAndType(
      long clusterId, String serviceName, String dependencyType) {
    List<ServiceDependencyBindingEntity> rows = entityManagerProvider.get().createNamedQuery(
        "ServiceDependencyBindingEntity.findByConsumerAndType", ServiceDependencyBindingEntity.class)
        .setParameter("clusterId", clusterId)
        .setParameter("serviceName", serviceName)
        .setParameter("dependencyType", dependencyType)
        .setMaxResults(1)
        .getResultList();
    return rows.isEmpty() ? null : rows.get(0);
  }

  /**
   * Reads the complete approved HBase dependency plan under its service-row
   * publication lock. The returned snapshots are detached immutable values.
   */
  @Transactional
  public Optional<List<ManagedDependencySnapshot>> findApprovedLivePlanSnapshots(
      long consumerClusterId) {
    EntityManager entityManager = entityManagerProvider.get();
    ClusterServiceEntityPK serviceId = new ClusterServiceEntityPK();
    serviceId.setClusterId(consumerClusterId);
    serviceId.setServiceName("HBASE");
    ClusterServiceEntity service = entityManager.find(
        ClusterServiceEntity.class, serviceId, LockModeType.PESSIMISTIC_READ);
    if (service == null) {
      return Optional.empty();
    }
    List<ServiceDependencyBindingEntity> bindings = entityManager.createNamedQuery(
        "ServiceDependencyBindingEntity.findByConsumer", ServiceDependencyBindingEntity.class)
        .setParameter("clusterId", consumerClusterId)
        .setParameter("serviceName", "HBASE")
        .setLockMode(LockModeType.PESSIMISTIC_READ)
        .getResultList();
    if (bindings.isEmpty()) {
      return Optional.empty();
    }
    List<ManagedDependencySnapshot> snapshots = new java.util.ArrayList<>();
    Set<String> types = new java.util.HashSet<>();
    for (ServiceDependencyBindingEntity binding : bindings) {
      boolean reconcilingZooKeeper = "ZOOKEEPER".equals(binding.getDependencyType())
          && "FENCING_UNCERTAIN".equals(binding.getState())
          && "ZOOKEEPER_HANDOFF_RECONCILING".equals(binding.getProvisioningPhase())
          && "DEPENDENCY_ZOOKEEPER_HANDOFF_RECONCILIATION_REQUIRED"
              .equals(binding.getFailureCode());
      if (!types.add(binding.getDependencyType())
          || !"APPROVED".equals(binding.getSnapshotApproval())
          || !Set.of("PROVISIONING", "READY", "FAILED").contains(binding.getState())
              && !reconcilingZooKeeper) {
        throw invalidLivePlan("The active managed HBase dependency state is inconsistent.");
      }
      ServiceDependencySnapshotEntity entity = entityManager.find(
          ServiceDependencySnapshotEntity.class,
          new ServiceDependencySnapshotEntityPK(
              binding.getBindingId(), binding.getDesiredSnapshotVersion()),
          LockModeType.PESSIMISTIC_READ);
      if (entity == null) {
        throw invalidLivePlan("The approved managed HBase dependency snapshot is missing.");
      }
      try {
        ManagedDependencySnapshot snapshot = StageUtils.getGson().fromJson(
            entity.getSnapshotJson(), ManagedDependencySnapshot.class);
        if (snapshot == null
            || !Objects.equals(entity.getSchemaVersion(), snapshot.schemaVersion())
            || !Objects.equals(entity.getSnapshotVersion(), snapshot.snapshotVersion())
            || !binding.getBindingId().equals(snapshot.bindingId().toString())
            || !binding.getDependencyType().equals(snapshot.type().name())
            || !binding.getProviderClusterId().equals(snapshot.providerService().clusterId())
            || !binding.getProviderServiceName().equals(snapshot.providerService().serviceName())
            || !entity.getConsumerFingerprint().equals(snapshot.consumerFingerprint())
            || !entity.getProviderFingerprint().equals(snapshot.providerFingerprint())
            || !binding.getProviderFingerprint().equals(snapshot.providerFingerprint())
            || !entity.getSnapshotFingerprint().equals(snapshot.snapshotFingerprint())) {
          throw invalidLivePlan(
              "The approved managed HBase dependency snapshot requires review.");
        }
        snapshots.add(snapshot);
      } catch (ManagedDependencyIntegrationException e) {
        throw e;
      } catch (RuntimeException e) {
        throw new ManagedDependencyIntegrationException(409,
            "DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED",
            "The approved managed HBase dependency snapshot cannot be verified.", e);
      }
    }
    return Optional.of(List.copyOf(snapshots));
  }

  private ManagedDependencyIntegrationException invalidLivePlan(String message) {
    return new ManagedDependencyIntegrationException(409,
        "DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED", message);
  }

  /**
   * Locks both service rows in canonical order before publishing the immutable
   * snapshot, initial operation and active binding in one transaction.
   */
  @Transactional
  public void create(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity snapshot, ServiceDependencyOperationEntity operation,
      CreationGuard guard) {
    createBatchInternal(List.of(new CreationItem(binding, snapshot, operation, null, guard)));
  }

  @Transactional
  public void create(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity snapshot, ServiceDependencyOperationEntity operation,
      ServiceDependencyHostResultEntity initialCommand, CreationGuard guard) {
    createBatchInternal(List.of(new CreationItem(binding, snapshot, operation, initialCommand, guard)));
  }

  /**
   * Publishes one complete managed HBase dependency plan in one transaction.
   * The bounded input is either entirely new or an exact replay of all of its
   * original CREATE operations. Binding and operation locks follow canonical
   * binding UUID order; the result retains the caller's item order.
   */
  @Transactional
  public CreationBatchResult createBatch(List<CreationItem> items) {
    return createBatchInternal(items);
  }

  private CreationBatchResult createBatchInternal(List<CreationItem> items) {
    List<CreationItem> input = validateCreationItems(items);
    List<CreationItem> lockOrder = input.stream()
        .sorted(Comparator.comparing(item -> item.binding().getBindingId()))
        .toList();
    EntityManager entityManager = entityManagerProvider.get();
    CreationGuard guard = coalesceGuards(input);

    Set<ManagedDependencyServiceKey> serviceKeys = new java.util.TreeSet<>(SERVICE_KEY_ORDER);
    for (CreationItem item : lockOrder) {
      serviceKeys.add(serviceKey(item.binding().getConsumerClusterId(),
          item.binding().getConsumerServiceName()));
      serviceKeys.add(serviceKey(item.binding().getProviderClusterId(),
          item.binding().getProviderServiceName()));
    }
    for (ManagedDependencyServiceKey serviceKey : serviceKeys) {
      lockService(entityManager, serviceKey);
    }

    List<ExistingCreation> existing = new java.util.ArrayList<>();
    boolean anyExisting = false;
    for (CreationItem item : lockOrder) {
      ExistingCreation materialized = findExistingCreation(entityManager, item);
      existing.add(materialized);
      anyExisting |= materialized != null;
    }
    if (anyExisting) {
      if (existing.stream().anyMatch(java.util.Objects::isNull)) {
        throw new StaleApprovalException(
            "A dependency plan replay is missing one of its original CREATE operations");
      }
      List<CreationResult> replay = new java.util.ArrayList<>();
      for (int i = 0; i < lockOrder.size(); i++) {
        CreationItem item = lockOrder.get(i);
        ExistingCreation materialized = existing.get(i);
        validateExactReplay(item, materialized);
        replay.add(new CreationResult(materialized.binding(), materialized.snapshot(),
            materialized.operation(), materialized.initialCommand(), false));
      }
      return new CreationBatchResult(inInputOrder(input, replay));
    }

    lockAndValidateDraft(entityManager, guard.draft());
    lockAndValidateRepositories(entityManager, guard.repositories());
    lockAndValidateServiceVersions(entityManager, guard.services());

    for (CreationItem item : lockOrder) {
      if (entityManager.find(ServiceDependencyFenceEntity.class, item.binding().getBindingId(),
          LockModeType.PESSIMISTIC_WRITE) != null) {
        throw new StaleApprovalException("A detached binding UUID cannot be reused");
      }
    }
    List<CreationResult> created = new java.util.ArrayList<>();
    for (CreationItem item : lockOrder) {
      ServiceDependencyBindingEntity binding = item.binding();
      ServiceDependencyOperationEntity operation = item.operation();
      entityManager.persist(binding);
      entityManager.persist(item.snapshot());
      entityManager.persist(operation);
      ServiceDependencyHostResultEntity initialCommand = item.initialCommand();
      if (initialCommand != null) {
        binding.setActionHostId(initialCommand.getHostId());
        entityManager.persist(initialCommand);
      }
      created.add(new CreationResult(binding, item.snapshot(), operation, initialCommand, true));
    }
    entityManager.flush();
    return new CreationBatchResult(inInputOrder(input, created));
  }

  private List<CreationResult> inInputOrder(List<CreationItem> input,
      List<CreationResult> canonicalResults) {
    Map<String, CreationResult> byBindingId = new java.util.HashMap<>();
    for (CreationResult result : canonicalResults) {
      byBindingId.put(result.binding().getBindingId(), result);
    }
    return input.stream().map(item -> byBindingId.get(item.binding().getBindingId())).toList();
  }

  private List<CreationItem> validateCreationItems(List<CreationItem> items) {
    if (items == null || items.isEmpty() || items.size() > 2) {
      throw new IllegalArgumentException("A dependency plan must contain one or two items");
    }
    Set<String> bindingIds = new java.util.HashSet<>();
    Set<String> operationIds = new java.util.HashSet<>();
    Set<ManagedDependencyType> dependencyTypes =
        java.util.EnumSet.noneOf(ManagedDependencyType.class);
    ManagedDependencyServiceKey consumerKey = null;
    List<CreationItem> input = List.copyOf(items);
    for (CreationItem item : input) {
      Objects.requireNonNull(item, "creation item");
      ServiceDependencyBindingEntity binding = Objects.requireNonNull(item.binding(), "binding");
      ServiceDependencyOperationEntity operation = Objects.requireNonNull(item.operation(), "operation");
      Objects.requireNonNull(item.snapshot(), "snapshot");
      Objects.requireNonNull(item.guard(), "guard");
      if (binding.getBindingId() == null || !bindingIds.add(binding.getBindingId())) {
        throw new IllegalArgumentException("Creation items must have unique binding UUIDs");
      }
      if (operation.getOperationId() == null || !operationIds.add(operation.getOperationId())) {
        throw new IllegalArgumentException("Creation items must have unique operation UUIDs");
      }
      if (binding.getConsumerClusterId() == null || binding.getProviderClusterId() == null
          || binding.getConsumerServiceName() == null || binding.getProviderServiceName() == null
          || binding.getDependencyType() == null) {
        throw new IllegalArgumentException("Creation items must identify both parent services");
      }
      if (binding.getConsumerClusterId() <= 0 || binding.getProviderClusterId() <= 0
          || !"HBASE".equals(binding.getConsumerServiceName())) {
        throw new IllegalArgumentException(
            "A dependency plan must target one positive-ID HBASE consumer");
      }
      ManagedDependencyServiceKey currentConsumer = serviceKey(
          binding.getConsumerClusterId(), binding.getConsumerServiceName());
      if (consumerKey == null) {
        consumerKey = currentConsumer;
      } else if (!consumerKey.equals(currentConsumer)) {
        throw new IllegalArgumentException(
            "All dependency plan items must target the same HBASE consumer");
      }
      ManagedDependencyType dependencyType;
      try {
        dependencyType = ManagedDependencyType.valueOf(binding.getDependencyType());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("A dependency plan contains an unsupported provider type", e);
      }
      if (!dependencyTypes.add(dependencyType)
          || !dependencyType.getProviderServiceName().equals(binding.getProviderServiceName())) {
        throw new IllegalArgumentException(
            "Dependency plan provider types must be unique and match their service names");
      }
      validateImmutableCreateItem(item);
    }
    return input;
  }

  private void validateImmutableCreateItem(CreationItem item) {
    ServiceDependencyBindingEntity binding = item.binding();
    ServiceDependencySnapshotEntity snapshot = item.snapshot();
    ServiceDependencyOperationEntity operation = item.operation();
    if (binding.getBindingId().isBlank() || operation.getOperationId().isBlank()
        || binding.getActiveOperationId() == null || binding.getActiveOperationId().isBlank()
        || !Objects.equals(binding.getBindingId(), snapshot.getBindingId())
        || binding.getOperationEpoch() == null || binding.getOperationEpoch() <= 0
        || binding.getDesiredSnapshotVersion() == null || binding.getDesiredSnapshotVersion() <= 0
        || !Objects.equals(binding.getDesiredSnapshotVersion(), snapshot.getSnapshotVersion())
        || !Objects.equals(binding.getBindingId(), operation.getBindingId())
        || !"CREATE".equals(operation.getOperationKind())
        || !Objects.equals(binding.getOperationEpoch(), operation.getOperationEpoch())
        || !Objects.equals(binding.getDesiredSnapshotVersion(), operation.getTargetSnapshotVersion())
        || !Objects.equals(binding.getActiveOperationId(), operation.getOperationId())
        || snapshot.getSnapshotVersion() == null || snapshot.getSnapshotVersion() <= 0
        || snapshot.getSchemaVersion() == null
        || (snapshot.getSchemaVersion() != ManagedDependencySnapshot.LEGACY_INSECURE_SCHEMA_VERSION
            && snapshot.getSchemaVersion() != ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION)
        || operation.getRequestHash() == null || operation.getRequestHash().isBlank()) {
      throw new IllegalArgumentException(
          "The dependency CREATE entities do not form one immutable plan item");
    }
    if (item.initialCommand() != null) {
      validateProviderIntent(binding, operation, item.initialCommand());
    }
  }

  private CreationGuard coalesceGuards(List<CreationItem> input) {
    DraftGuard draft = null;
    boolean draftSeen = false;
    Map<Long, RepositoryGuard> repositories = new TreeMap<>();
    Map<ManagedDependencyServiceKey, ServiceVersionGuard> services =
        new java.util.TreeMap<>(SERVICE_KEY_ORDER);
    for (CreationItem item : input) {
      CreationGuard itemGuard = item.guard();
      if (!draftSeen) {
        draft = itemGuard.draft();
        draftSeen = true;
      } else if (!Objects.equals(draft, itemGuard.draft())) {
        throw new StaleApprovalException("Dependency plan items use different draft references");
      }
      for (RepositoryGuard repository : itemGuard.repositories()) {
        RepositoryGuard prior = repositories.putIfAbsent(repository.rowId(), repository);
        if (prior != null && !prior.equals(repository)) {
          throw new StaleApprovalException("Dependency plan items use conflicting repository guards");
        }
      }
      for (ServiceVersionGuard service : itemGuard.services()) {
        ManagedDependencyServiceKey key = serviceKey(service.clusterId(), service.serviceName());
        ServiceVersionGuard prior = services.putIfAbsent(key, service);
        if (prior != null && !prior.equals(service)) {
          throw new StaleApprovalException("Dependency plan items use conflicting service guards");
        }
      }
    }
    return new CreationGuard(draft, List.copyOf(repositories.values()),
        List.copyOf(services.values()));
  }

  private ExistingCreation findExistingCreation(EntityManager entityManager, CreationItem item) {
    ServiceDependencyBindingEntity binding = entityManager.find(
        ServiceDependencyBindingEntity.class, item.binding().getBindingId(),
        LockModeType.PESSIMISTIC_WRITE);
    ServiceDependencyOperationEntity operation = entityManager.find(
        ServiceDependencyOperationEntity.class, item.operation().getOperationId(),
        LockModeType.PESSIMISTIC_WRITE);
    if (operation == null && binding == null) {
      binding = findBindingByConsumerAndType(entityManager, item.binding());
      if (binding == null) {
        return null;
      }
    }
    if (operation == null || binding == null) {
      throw new StaleApprovalException("A dependency plan replay is only partially materialized");
    }
    if (!Objects.equals(binding.getBindingId(), operation.getBindingId())
        || !"CREATE".equals(operation.getOperationKind())
        || operation.getOperationEpoch() == null || operation.getOperationEpoch() <= 0
        || operation.getTargetSnapshotVersion() == null || operation.getTargetSnapshotVersion() <= 0
        || operation.getRequestHash() == null || operation.getRequestHash().isBlank()) {
      throw new StaleApprovalException("The operation UUID is not the original dependency CREATE");
    }
    ServiceDependencySnapshotEntity snapshot = entityManager.find(
        ServiceDependencySnapshotEntity.class,
        new ServiceDependencySnapshotEntityPK(binding.getBindingId(),
            operation.getTargetSnapshotVersion()), LockModeType.PESSIMISTIC_READ);
    if (snapshot == null) {
      throw new StaleApprovalException("A materialized dependency plan is missing its snapshot");
    }
    ServiceDependencyHostResultEntity initialCommand = findOriginalCommand(entityManager, item,
        binding);
    return new ExistingCreation(binding, snapshot, operation, initialCommand);
  }

  private ServiceDependencyBindingEntity findBindingByConsumerAndType(EntityManager entityManager,
      ServiceDependencyBindingEntity expected) {
    TypedQuery<ServiceDependencyBindingEntity> query = entityManager.createNamedQuery(
        "ServiceDependencyBindingEntity.findByConsumerAndType", ServiceDependencyBindingEntity.class);
    List<ServiceDependencyBindingEntity> rows = query
        .setParameter("clusterId", expected.getConsumerClusterId())
        .setParameter("serviceName", expected.getConsumerServiceName())
        .setParameter("dependencyType", expected.getDependencyType())
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .setMaxResults(1).getResultList();
    return rows.isEmpty() ? null : rows.get(0);
  }

  private ServiceDependencyHostResultEntity findOriginalCommand(EntityManager entityManager,
      CreationItem item, ServiceDependencyBindingEntity binding) {
    ServiceDependencyHostResultEntity requested = item.initialCommand();
    if (requested != null && (requested.getCheckKind() == null
        || !isProviderCommand(requested.getCheckKind())
        || !Objects.equals(requested.getDependencyType(), binding.getDependencyType()))) {
      throw new StaleApprovalException("The replay initial command is not a provider intent");
    }
    List<ServiceDependencyHostResultEntity> providerCommands = entityManager.createQuery(
        "SELECT result FROM ServiceDependencyHostResultEntity result "
            + "WHERE result.bindingId=:bindingId AND result.operationId=:operationId",
        ServiceDependencyHostResultEntity.class)
        .setParameter("bindingId", binding.getBindingId())
        .setParameter("operationId", item.operation().getOperationId())
        .getResultList().stream().filter(command -> isProviderCommand(command.getCheckKind()))
        .toList();
    if (requested == null) {
      if (!providerCommands.isEmpty()) {
        throw new StaleApprovalException("The replay omitted its original provider intent");
      }
      return null;
    }
    ServiceDependencyHostResultEntity existing = entityManager.find(
        ServiceDependencyHostResultEntity.class, commandId(requested), LockModeType.PESSIMISTIC_READ);
    if (existing == null) {
      throw new StaleApprovalException("The original provider intent is missing from the replay");
    }
    return existing;
  }

  private void validateExactReplay(CreationItem item, ExistingCreation existing) {
    if (!sameImmutableBinding(item.binding(), existing.binding())
        || !sameImmutableSnapshot(item.snapshot(), existing.snapshot())
        || !sameImmutableOperation(item.operation(), existing.operation())
        || !sameImmutableCommand(item.initialCommand(), existing.initialCommand())) {
      throw new StaleApprovalException("The dependency plan replay does not match its original CREATE");
    }
  }

  private void validateProviderIntent(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity operation, ServiceDependencyHostResultEntity command) {
    if (command.getBindingId() == null || command.getOperationId() == null
        || command.getSnapshotVersion() == null || command.getSnapshotVersion() <= 0
        || command.getOperationEpoch() == null || command.getOperationEpoch() <= 0
        || command.getHostId() == null || command.getHostId() <= 0
        || command.getDependencyType() == null
        || !Objects.equals(command.getDependencyType(), binding.getDependencyType())
        || command.getCheckKind() == null || command.getCommandRequestHash() == null
        || command.getCommandRequestHash().isBlank() || command.getCommandJson() == null
        || command.getCommandJson().isBlank() || command.getRequiredPackageHash() == null
        || command.getRequiredPackageHash().isBlank() || !isProviderCommand(command.getCheckKind())) {
      throw new IllegalArgumentException("The initial dependency command is not a provider intent");
    }
    requireCurrentCommandOwner(binding, operation, command);
  }

  private boolean sameImmutableBinding(ServiceDependencyBindingEntity expected,
      ServiceDependencyBindingEntity actual) {
    return Objects.equals(expected.getBindingId(), actual.getBindingId())
        && Objects.equals(expected.getConsumerClusterId(), actual.getConsumerClusterId())
        && Objects.equals(expected.getConsumerServiceName(), actual.getConsumerServiceName())
        && Objects.equals(expected.getProviderClusterId(), actual.getProviderClusterId())
        && Objects.equals(expected.getProviderServiceName(), actual.getProviderServiceName())
        && Objects.equals(expected.getDependencyType(), actual.getDependencyType())
        && Objects.equals(expected.getNamespaceRoot(), actual.getNamespaceRoot())
        && Objects.equals(expected.getNamespaceWal(), actual.getNamespaceWal())
        && Objects.equals(expected.getNamespaceZnode(), actual.getNamespaceZnode());
  }

  private boolean sameImmutableSnapshot(ServiceDependencySnapshotEntity expected,
      ServiceDependencySnapshotEntity actual) {
    return Objects.equals(expected.getBindingId(), actual.getBindingId())
        && Objects.equals(expected.getSnapshotVersion(), actual.getSnapshotVersion())
        && Objects.equals(expected.getSchemaVersion(), actual.getSchemaVersion())
        && Objects.equals(expected.getConsumerFingerprint(), actual.getConsumerFingerprint())
        && Objects.equals(expected.getProviderFingerprint(), actual.getProviderFingerprint())
        && Objects.equals(expected.getProviderDisplayName(), actual.getProviderDisplayName())
        && Objects.equals(expected.getConsumerServiceVersion(), actual.getConsumerServiceVersion())
        && Objects.equals(expected.getSnapshotFingerprint(), actual.getSnapshotFingerprint())
        && Objects.equals(expected.getClientFeaturesHash(), actual.getClientFeaturesHash())
        && Objects.equals(expected.getSecurityPolicyHash(), actual.getSecurityPolicyHash())
        && Objects.equals(expected.getSnapshotJson(), actual.getSnapshotJson())
        && Objects.equals(expected.getCreatedByUserId(), actual.getCreatedByUserId());
  }

  private boolean sameImmutableOperation(ServiceDependencyOperationEntity expected,
      ServiceDependencyOperationEntity actual) {
    return Objects.equals(expected.getOperationId(), actual.getOperationId())
        && Objects.equals(expected.getBindingId(), actual.getBindingId())
        && Objects.equals(expected.getOperationKind(), actual.getOperationKind())
        && Objects.equals(expected.getOperationEpoch(), actual.getOperationEpoch())
        && Objects.equals(expected.getTargetSnapshotVersion(), actual.getTargetSnapshotVersion())
        && Objects.equals(expected.getRequestHash(), actual.getRequestHash());
  }

  private boolean sameImmutableCommand(ServiceDependencyHostResultEntity expected,
      ServiceDependencyHostResultEntity actual) {
    if (expected == null || actual == null) {
      return expected == actual;
    }
    return Objects.equals(expected.getBindingId(), actual.getBindingId())
        && Objects.equals(expected.getSnapshotVersion(), actual.getSnapshotVersion())
        && Objects.equals(expected.getOperationEpoch(), actual.getOperationEpoch())
        && Objects.equals(expected.getHostId(), actual.getHostId())
        && Objects.equals(expected.getDependencyType(), actual.getDependencyType())
        && Objects.equals(expected.getCheckKind(), actual.getCheckKind())
        && Objects.equals(expected.getOperationId(), actual.getOperationId())
        && Objects.equals(expected.getComponentName(), actual.getComponentName())
        && Objects.equals(expected.getCommandRequestHash(), actual.getCommandRequestHash())
        && Objects.equals(expected.getCommandJson(), actual.getCommandJson())
        && Objects.equals(expected.getRequiredPackageHash(), actual.getRequiredPackageHash());
  }

  private ManagedDependencyServiceKey serviceKey(long clusterId, String serviceName) {
    return new ManagedDependencyServiceKey(clusterId, serviceName);
  }

  /** Starts a new consumer retry epoch while retaining all prior command evidence. */
  @Transactional
  public LifecycleTransition startRetry(String bindingId, long expectedRowVersion,
      ServiceDependencyOperationEntity requested, int userId) {
    return startRetry(bindingId, expectedRowVersion, requested, null, userId);
  }

  /** Starts a retry and atomically records the selected consumer preparation intent. */
  @Transactional
  public LifecycleTransition startRetry(String bindingId, long expectedRowVersion,
      ServiceDependencyOperationEntity requested, ServiceDependencyHostResultEntity retryCommand,
      int userId) {
    EntityManager entityManager = entityManagerProvider.get();
    ServiceDependencyBindingEntity binding = entityManager.find(
        ServiceDependencyBindingEntity.class, bindingId, LockModeType.PESSIMISTIC_WRITE);
    if (binding == null) {
      throw new StaleApprovalException("The managed dependency binding no longer exists");
    }
    ServiceDependencyOperationEntity existing = entityManager.find(
        ServiceDependencyOperationEntity.class, requested.getOperationId(),
        LockModeType.PESSIMISTIC_WRITE);
    if (existing != null) {
      requireExactLifecycleRetry(binding, existing, requested);
      ServiceDependencyHostResultEntity command = retryCommand == null ? null
          : entityManager.find(ServiceDependencyHostResultEntity.class, commandId(retryCommand),
              LockModeType.PESSIMISTIC_READ);
      if (retryCommand != null && command == null) {
        throw new StaleApprovalException("The retry preparation intent is missing");
      }
      return new LifecycleTransition(binding, existing, command, false);
    }
    if (!Objects.equals(binding.getRowVersion(), expectedRowVersion)) {
      throw new StaleApprovalException("The managed dependency changed after it was read");
    }
    if (!"FAILED".equals(binding.getState()) || !Boolean.TRUE.equals(binding.getFailureRetryable())) {
      throw new StaleApprovalException("The managed dependency is not in a retryable failed state");
    }
    if (binding.getProviderPreparationHash() == null) {
      throw new StaleApprovalException(
          "Provider preparation cannot be retried under a new identity without fencing review");
    }
    requireNextLifecycleOperation(binding, requested, "RETRY", binding.getDesiredSnapshotVersion());
    if (retryCommand != null) {
      validateRetryPreparationIntent(binding, requested, retryCommand);
    }
    entityManager.persist(requested);
    if (retryCommand != null) {
      entityManager.persist(retryCommand);
    }
    binding.setOperationEpoch(requested.getOperationEpoch());
    binding.setActiveOperationId(requested.getOperationId());
    binding.setActiveRequestId(null);
    binding.setState("PROVISIONING");
    binding.setProvisioningPhase("PROVIDER_PREPARED");
    binding.setFailureCode(null);
    binding.setFailurePhase(null);
    binding.setFailureMessage(null);
    binding.setFailureRetryable(false);
    binding.setUpdatedByUserId(userId);
    binding.setUpdateTimestamp(System.currentTimeMillis());
    entityManager.flush();
    return new LifecycleTransition(binding, requested, retryCommand, true);
  }

  /**
   * Commits one immutable provider-metadata update and its first provider intent.
   * An existing operation is reconciled before any current-row approval checks so
   * a lost response can be replayed after a later lifecycle transition.
   */
  @Transactional
  public LifecycleTransition startUpdate(String bindingId, long expectedRowVersion,
      UpdateFacts expected, ServiceDependencySnapshotEntity requestedSnapshot,
      ServiceDependencyOperationEntity requested,
      ServiceDependencyHostResultEntity initialCommand, int userId) {
    EntityManager entityManager = entityManagerProvider.get();
    ServiceDependencyBindingEntity binding = entityManager.find(
        ServiceDependencyBindingEntity.class, bindingId, LockModeType.PESSIMISTIC_WRITE);
    if (binding == null) {
      throw new StaleApprovalException("The managed dependency binding no longer exists");
    }
    ServiceDependencyOperationEntity existing = entityManager.find(
        ServiceDependencyOperationEntity.class, requested.getOperationId(),
        LockModeType.PESSIMISTIC_WRITE);
    if (existing != null) {
      requireExactUpdateReplay(binding, existing, requested, requestedSnapshot, initialCommand,
          entityManager);
      ServiceDependencySnapshotEntity target = entityManager.find(
          ServiceDependencySnapshotEntity.class,
          new ServiceDependencySnapshotEntityPK(bindingId, existing.getTargetSnapshotVersion()),
          LockModeType.PESSIMISTIC_READ);
      ServiceDependencyHostResultEntity command = initialCommand == null ? null
          : entityManager.find(ServiceDependencyHostResultEntity.class, commandId(initialCommand),
              LockModeType.PESSIMISTIC_READ);
      return new LifecycleTransition(binding, existing, command, false);
    }
    if (!Objects.equals(binding.getRowVersion(), expectedRowVersion)) {
      throw new StaleApprovalException("The managed dependency changed after it was read");
    }
    ServiceDependencySnapshotEntity current = entityManager.find(
        ServiceDependencySnapshotEntity.class,
        new ServiceDependencySnapshotEntityPK(bindingId, binding.getDesiredSnapshotVersion()),
        LockModeType.PESSIMISTIC_READ);
    if (current == null || !Objects.equals(binding.getSnapshotApproval(), expected.snapshotApproval())
        || !Objects.equals(current.getSnapshotVersion(), expected.snapshotVersion())
        || !Objects.equals(current.getProviderFingerprint(), expected.providerFingerprint())
        || !Objects.equals(current.getConsumerFingerprint(), expected.consumerFingerprint())
        || !Objects.equals(current.getSnapshotFingerprint(), expected.snapshotFingerprint())) {
      throw new StaleApprovalException("Provider, consumer, or snapshot facts changed after preview");
    }
    if (Set.of("DETACHING", "RETIRED", "FENCING_UNCERTAIN").contains(binding.getState())) {
      throw new StaleApprovalException("The managed dependency is not available for update");
    }
    long activeCommands = entityManager.createQuery(
        "SELECT COUNT(result) FROM ServiceDependencyHostResultEntity result "
            + "WHERE result.bindingId=:bindingId AND result.operationEpoch=:operationEpoch "
            + "AND result.state IN ('INTENT', 'SCHEDULING', 'DISPATCHED')", Long.class)
        .setParameter("bindingId", bindingId)
        .setParameter("operationEpoch", binding.getOperationEpoch())
        .getSingleResult();
    if (activeCommands != 0) {
      throw new StaleApprovalException(
          "A managed dependency command is still running; update after it is terminal");
    }
    requireNextLifecycleOperation(binding, requested, "UPDATE",
        binding.getDesiredSnapshotVersion() + 1);
    requireUpdateIdentity(binding, current, requestedSnapshot, requested);
    if (initialCommand != null) {
      validateUpdateProviderIntent(binding, requested, requestedSnapshot, initialCommand);
    }
    entityManager.persist(requestedSnapshot);
    entityManager.persist(requested);
    if (initialCommand != null) {
      binding.setActionHostId(initialCommand.getHostId());
      entityManager.persist(initialCommand);
    }
    binding.setOperationEpoch(requested.getOperationEpoch());
    binding.setActiveOperationId(requested.getOperationId());
    binding.setActiveRequestId(null);
    binding.setDesiredSnapshotVersion(requestedSnapshot.getSnapshotVersion());
    binding.setSnapshotApproval("APPROVED");
    binding.setProviderFingerprint(requestedSnapshot.getProviderFingerprint());
    // The validator has already required the immutable private namespace to match
    // the current snapshot. Keep the binding's existing namespace columns intact.
    binding.setProviderPreparationHash(null);
    binding.setState("PROVISIONING");
    binding.setProvisioningPhase("PROVIDER_PREPARING");
    binding.setFailureCode(null);
    binding.setFailurePhase(null);
    binding.setFailureMessage(null);
    binding.setFailureRetryable(false);
    binding.setUpdatedByUserId(userId);
    binding.setUpdateTimestamp(System.currentTimeMillis());
    entityManager.flush();
    return new LifecycleTransition(binding, requested, initialCommand, true);
  }

  /** Starts provider-journal invalidation only after every prior command is terminal. */
  @Transactional
  public LifecycleTransition startDetach(String bindingId, long expectedRowVersion,
      ServiceDependencyOperationEntity requested, ServiceDependencyHostResultEntity invalidation,
      int userId) {
    EntityManager entityManager = entityManagerProvider.get();
    ServiceDependencyBindingEntity binding = entityManager.find(
        ServiceDependencyBindingEntity.class, bindingId, LockModeType.PESSIMISTIC_WRITE);
    if (binding == null) {
      throw new StaleApprovalException("The managed dependency binding no longer exists");
    }
    ServiceDependencyOperationEntity existing = entityManager.find(
        ServiceDependencyOperationEntity.class, requested.getOperationId(),
        LockModeType.PESSIMISTIC_WRITE);
    if (existing != null) {
      requireExactLifecycleRetry(binding, existing, requested);
      ServiceDependencyHostResultEntity command = entityManager.find(
          ServiceDependencyHostResultEntity.class, commandId(invalidation));
      return new LifecycleTransition(binding, existing, command, false);
    }
    if (!Objects.equals(binding.getRowVersion(), expectedRowVersion)) {
      throw new StaleApprovalException("The managed dependency changed after it was read");
    }
    if (binding.getActionHostId() == null) {
      throw new StaleApprovalException("The provider action host has not been durably pinned");
    }
    long activeCommands = entityManager.createQuery(
        "SELECT COUNT(result) FROM ServiceDependencyHostResultEntity result "
            + "WHERE result.bindingId=:bindingId AND result.operationEpoch=:operationEpoch "
            + "AND result.state IN ('INTENT', 'SCHEDULING', 'DISPATCHED')", Long.class)
        .setParameter("bindingId", bindingId)
        .setParameter("operationEpoch", binding.getOperationEpoch())
        .getSingleResult();
    if (activeCommands != 0) {
      throw new StaleApprovalException(
          "A managed dependency command is still running; retry detach after it is terminal");
    }
    requireNextLifecycleOperation(binding, requested, "DETACH", binding.getDesiredSnapshotVersion());
    requireCurrentCommandOwnerForTransition(binding, requested, invalidation);
    if (!"INVALIDATE_BINDING_EPOCH".equals(invalidation.getCheckKind())
        || !Objects.equals(binding.getActionHostId(), invalidation.getHostId())
        || !Objects.equals(binding.getDependencyType(), invalidation.getDependencyType())) {
      throw new StaleApprovalException("The detach command does not match the active provider fence");
    }
    entityManager.persist(requested);
    entityManager.persist(invalidation);
    binding.setOperationEpoch(requested.getOperationEpoch());
    binding.setActiveOperationId(requested.getOperationId());
    binding.setActiveRequestId(null);
    binding.setState("DETACHING");
    binding.setProvisioningPhase("DETACHING");
    binding.setFailureCode(null);
    binding.setFailurePhase(null);
    binding.setFailureMessage(null);
    binding.setFailureRetryable(false);
    binding.setUpdatedByUserId(userId);
    binding.setUpdateTimestamp(System.currentTimeMillis());
    entityManager.flush();
    return new LifecycleTransition(binding, requested, invalidation, true);
  }

  /** Persists an exact command before its external Ambari request is created. */
  @Transactional
  public ServiceDependencyHostResultEntity planCommand(ServiceDependencyHostResultEntity command) {
    EntityManager entityManager = entityManagerProvider.get();
    ServiceDependencyHostResultEntity result = planCommand(entityManager, command);
    entityManager.flush();
    return result;
  }

  /** Atomically plans a multi-dependency preparation bundle in binding-ID order. */
  @Transactional
  public List<ServiceDependencyHostResultEntity> planCommands(
      List<ServiceDependencyHostResultEntity> commands) {
    EntityManager entityManager = entityManagerProvider.get();
    List<ServiceDependencyHostResultEntity> result = new java.util.ArrayList<>();
    for (ServiceDependencyHostResultEntity command : commands.stream()
        .sorted(Comparator.comparing(ServiceDependencyHostResultEntity::getBindingId))
        .toList()) {
      result.add(planCommand(entityManager, command));
    }
    entityManager.flush();
    return List.copyOf(result);
  }

  private ServiceDependencyHostResultEntity planCommand(EntityManager entityManager,
      ServiceDependencyHostResultEntity command) {
    ServiceDependencyBindingEntity binding = entityManager.find(
        ServiceDependencyBindingEntity.class, command.getBindingId(), LockModeType.PESSIMISTIC_WRITE);
    ServiceDependencyOperationEntity operation = entityManager.find(
        ServiceDependencyOperationEntity.class, command.getOperationId(), LockModeType.PESSIMISTIC_WRITE);
    requireCurrentCommandOwner(binding, operation, command);
    if (isProviderCommand(command.getCheckKind())) {
      if (binding.getActionHostId() == null) {
        binding.setActionHostId(command.getHostId());
      } else if (!binding.getActionHostId().equals(command.getHostId())) {
        throw new StaleApprovalException("A different provider host is already pinned to this binding");
      }
    }
    ServiceDependencyHostResultEntityPK id = commandId(command);
    ServiceDependencyHostResultEntity existing = entityManager.find(
        ServiceDependencyHostResultEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (existing != null) {
      if (!Objects.equals(existing.getOperationId(), command.getOperationId())
          || !Objects.equals(existing.getOperationEpoch(), command.getOperationEpoch())
          || !Objects.equals(existing.getCommandRequestHash(), command.getCommandRequestHash())
          || !Objects.equals(existing.getCommandJson(), command.getCommandJson())
          || !Objects.equals(existing.getComponentName(), command.getComponentName())) {
        throw new StaleApprovalException("A different command already owns this dependency step");
      }
      return existing;
    }
    if ("READY".equals(binding.getState())
        && command.getCheckKind().startsWith("PREPARE_")
        && Set.of("HBASE_MASTER", "HBASE_REGIONSERVER", "HBASE_THRIFT")
            .contains(command.getComponentName())) {
      binding.setState("PROVISIONING");
      binding.setProvisioningPhase("CONSUMER_VERIFYING");
      binding.setFailureCode(null);
      binding.setFailurePhase(null);
      binding.setFailureMessage(null);
      binding.setFailureRetryable(false);
      binding.setUpdateTimestamp(System.currentTimeMillis());
      operation.setState("CONSUMER_VERIFYING");
      operation.setFailureCode(null);
      operation.setFailureMessage(null);
      operation.setUpdateTimestamp(System.currentTimeMillis());
    }
    entityManager.persist(command);
    return command;
  }

  /** Associates a canonical HBase INSTALL task before the action transaction is published. */
  @Transactional
  public boolean associatePreparationTask(ServiceDependencyHostResultEntity expected,
      String actualComponentName, long requestId, long stageId, long taskId) {
    EntityManager entityManager = entityManagerProvider.get();
    ServiceDependencyBindingEntity binding = entityManager.find(
        ServiceDependencyBindingEntity.class, expected.getBindingId(), LockModeType.PESSIMISTIC_WRITE);
    ServiceDependencyOperationEntity operation = entityManager.find(
        ServiceDependencyOperationEntity.class, expected.getOperationId(), LockModeType.PESSIMISTIC_WRITE);
    ServiceDependencyHostResultEntity command = entityManager.find(
        ServiceDependencyHostResultEntity.class, commandId(expected), LockModeType.PESSIMISTIC_WRITE);
    requireCurrentCommandOwner(binding, operation, command);
    if (!Objects.equals(command.getCommandRequestHash(), expected.getCommandRequestHash())
        || !Objects.equals(command.getCommandJson(), expected.getCommandJson())) {
      throw new StaleApprovalException("The HBase task does not match its preparation intent");
    }
    if (!Objects.equals(command.getComponentName(), actualComponentName)) {
      return false;
    }
    if (command.getAmbariTaskId() != null
        && (!command.getAmbariRequestId().equals(requestId)
            || !command.getAmbariStageId().equals(stageId)
            || !command.getAmbariTaskId().equals(taskId))) {
      throw new StaleApprovalException("A different HBase task owns this preparation intent");
    }
    command.setAmbariRequestId(requestId);
    command.setAmbariStageId(stageId);
    command.setAmbariTaskId(taskId);
    command.setState("DISPATCHED");
    command.setCheckTimestamp(System.currentTimeMillis());
    operation.setAmbariRequestId(requestId);
    binding.setActiveRequestId(requestId);
    binding.setUpdateTimestamp(System.currentTimeMillis());
    entityManager.flush();
    return true;
  }

  /** Claims a current command before the dispatcher creates its external Ambari action. */
  @Transactional
  public boolean claimCommandDispatch(ServiceDependencyHostResultEntityPK id) {
    EntityManager entityManager = entityManagerProvider.get();
    ServiceDependencyHostResultEntity reference = entityManager.find(
        ServiceDependencyHostResultEntity.class, id);
    if (reference == null) {
      throw new StaleApprovalException("The persisted dependency command no longer exists");
    }
    ServiceDependencyBindingEntity binding = entityManager.find(
        ServiceDependencyBindingEntity.class, id.getBindingId(), LockModeType.PESSIMISTIC_WRITE);
    ServiceDependencyOperationEntity operation = entityManager.find(
        ServiceDependencyOperationEntity.class, reference.getOperationId(), LockModeType.PESSIMISTIC_WRITE);
    ServiceDependencyHostResultEntity command = entityManager.find(
        ServiceDependencyHostResultEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    requireCurrentCommandOwner(binding, operation, command);
    if ("SUCCEEDED".equals(command.getState()) || "DISPATCHED".equals(command.getState())) {
      return false;
    }
    if (!"INTENT".equals(command.getState()) && !"SCHEDULING".equals(command.getState())) {
      throw new StaleApprovalException("The dependency command is not dispatchable");
    }
    command.setState("SCHEDULING");
    command.setCheckTimestamp(System.currentTimeMillis());
    entityManager.flush();
    return true;
  }

  /** Records the exact Ambari task created for an already-persisted command intent. */
  @Transactional
  public void markCommandDispatched(ServiceDependencyHostResultEntityPK id,
      long requestId, long stageId, long taskId) {
    EntityManager entityManager = entityManagerProvider.get();
    ServiceDependencyHostResultEntity reference = entityManager.find(
        ServiceDependencyHostResultEntity.class, id);
    if (reference == null) {
      throw new StaleApprovalException("The persisted dependency command no longer exists");
    }
    ServiceDependencyBindingEntity binding = entityManager.find(
        ServiceDependencyBindingEntity.class, id.getBindingId(), LockModeType.PESSIMISTIC_WRITE);
    ServiceDependencyOperationEntity operation = entityManager.find(
        ServiceDependencyOperationEntity.class, reference.getOperationId(), LockModeType.PESSIMISTIC_WRITE);
    ServiceDependencyHostResultEntity command = entityManager.find(
        ServiceDependencyHostResultEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    requireCurrentCommandOwner(binding, operation, command);
    if ("SUCCEEDED".equals(command.getState())) {
      return;
    }
    if (!"SCHEDULING".equals(command.getState()) && !"DISPATCHED".equals(command.getState())) {
      throw new StaleApprovalException("The dependency command is not dispatchable");
    }
    command.setAmbariRequestId(requestId);
    command.setAmbariStageId(stageId);
    command.setAmbariTaskId(taskId);
    command.setState("DISPATCHED");
    command.setCheckTimestamp(System.currentTimeMillis());
    operation.setAmbariRequestId(requestId);
    operation.setState("DISPATCHED");
    operation.setUpdateTimestamp(System.currentTimeMillis());
    binding.setActiveRequestId(requestId);
    binding.setUpdateTimestamp(System.currentTimeMillis());
    entityManager.flush();
  }

  /**
   * Commits one validated result and its optional successor intent atomically.
   * This prevents restart from observing an applied provider effect without the
   * exact next command or terminal binding state.
   */
  @Transactional
  public void completeCommand(ServiceDependencyHostResultEntityPK id,
      long requestId, long stageId, long taskId, CommandCompletion completion) {
    EntityManager entityManager = entityManagerProvider.get();
    ServiceDependencyHostResultEntity reference = entityManager.find(
        ServiceDependencyHostResultEntity.class, id);
    if (reference == null) {
      throw new StaleApprovalException("The persisted dependency command no longer exists");
    }
    ServiceDependencyBindingEntity binding = entityManager.find(
        ServiceDependencyBindingEntity.class, id.getBindingId(), LockModeType.PESSIMISTIC_WRITE);
    ServiceDependencyOperationEntity operation = entityManager.find(
        ServiceDependencyOperationEntity.class, reference.getOperationId(), LockModeType.PESSIMISTIC_WRITE);
    ServiceDependencyHostResultEntity command = entityManager.find(
        ServiceDependencyHostResultEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    requireCurrentCommandOwner(binding, operation, command);
    if (command.getResultHash() != null) {
      if (!command.getResultHash().equals(completion.resultHash())) {
        throw new StaleApprovalException("A conflicting result was reported for this dependency command");
      }
      return;
    }
    if (command.getAmbariTaskId() == null
        || !command.getAmbariTaskId().equals(taskId)
        || !command.getAmbariRequestId().equals(requestId)
        || !command.getAmbariStageId().equals(stageId)) {
      throw new StaleApprovalException("The dependency result came from an unowned Ambari task");
    }
    command.setAmbariRequestId(requestId);
    command.setAmbariStageId(stageId);
    command.setAmbariTaskId(taskId);
    command.setResultJson(completion.resultJson());
    command.setResultHash(completion.resultHash());
    command.setState(completion.commandState());
    command.setFailureCode(completion.failureCode());
    command.setFailureMessage(completion.failureMessage());
    command.setCheckTimestamp(System.currentTimeMillis());
    command.setPreparationObservationId(completion.preparationObservationId());
    command.setPreparationRequestHash(completion.preparationRequestHash());
    command.setPreparationObservationFingerprint(completion.preparationObservationFingerprint());
    command.setPackageName(completion.packageName());
    command.setPackageVersion(completion.packageVersion());
    command.setClientSoftwareVersion(completion.clientSoftwareVersion());
    command.setObservedPackageHash(completion.observedPackageHash());
    command.setRenderedConfigHash(completion.renderedConfigHash());
    command.setIdentityFingerprint(completion.identityFingerprint());

    if ("INVALIDATE_BINDING_EPOCH".equals(command.getCheckKind())
        && "SUCCEEDED".equals(completion.commandState())) {
      operation.setState("SUCCEEDED");
      operation.setFailureCode(null);
      operation.setFailureMessage(null);
      operation.setUpdateTimestamp(System.currentTimeMillis());
      finalizeDetach(entityManager, binding);
      entityManager.flush();
      return;
    }

    if (completion.nextCommand() != null) {
      ServiceDependencyHostResultEntity next = completion.nextCommand();
      requireCurrentCommandOwner(binding, operation, next);
      ServiceDependencyHostResultEntity existing = entityManager.find(
          ServiceDependencyHostResultEntity.class, commandId(next), LockModeType.PESSIMISTIC_WRITE);
      if (existing == null) {
        entityManager.persist(next);
      } else if (!Objects.equals(existing.getCommandRequestHash(), next.getCommandRequestHash())
          || !Objects.equals(existing.getCommandJson(), next.getCommandJson())) {
        throw new StaleApprovalException("A different successor command already exists");
      }
    }

    long now = System.currentTimeMillis();
    boolean wasReady = "READY".equals(binding.getState())
        && Objects.equals(binding.getDesiredSnapshotVersion(), binding.getAppliedSnapshotVersion())
        && Objects.equals(binding.getProviderFingerprint(), binding.getAppliedProviderFingerprint());
    boolean affectsReadiness = affectsReadiness(command, completion.readinessHostIds());
    if (!wasReady || affectsReadiness) {
      binding.setState(completion.bindingState());
      binding.setProvisioningPhase(completion.bindingPhase());
      operation.setState(completion.operationState());
      if (completion.providerPreparationHash() != null) {
        binding.setProviderPreparationHash(completion.providerPreparationHash());
      }
      if (completion.appliedSnapshotVersion() != null) {
        binding.setAppliedSnapshotVersion(completion.appliedSnapshotVersion());
      }
      if (completion.appliedProviderFingerprint() != null) {
        binding.setAppliedProviderFingerprint(completion.appliedProviderFingerprint());
      }
      binding.setFailureCode(completion.failureCode());
      binding.setFailurePhase(completion.failurePhase());
      binding.setFailureMessage(completion.failureMessage());
      binding.setFailureRetryable(completion.retryable());
      operation.setFailureCode(completion.failureCode());
      operation.setFailureMessage(completion.failureMessage());
      if (completion.readinessHostIds() != null
          && allRequiredHostsVerified(entityManager, command, completion.readinessHostIds())) {
        binding.setState("READY");
        binding.setProvisioningPhase("READY");
        binding.setAppliedSnapshotVersion(binding.getDesiredSnapshotVersion());
        binding.setAppliedProviderFingerprint(binding.getProviderFingerprint());
        binding.setFailureCode(null);
        binding.setFailurePhase(null);
        binding.setFailureMessage(null);
        binding.setFailureRetryable(false);
        operation.setState("READY");
        operation.setFailureCode(null);
        operation.setFailureMessage(null);
      } else {
        ServiceDependencyHostResultEntity failure = currentFailure(entityManager, command);
        if (failure != null) {
          applyFailure(binding, operation, failure);
        }
      }
    }
    binding.setUpdateTimestamp(now);
    operation.setUpdateTimestamp(now);
    entityManager.flush();
  }

  /** Records invalid terminal task evidence without trusting raw task output. */
  @Transactional
  public void failTaskCommands(long requestId, long stageId, long taskId,
      String failureCode, String failureMessage) {
    EntityManager entityManager = entityManagerProvider.get();
    List<ServiceDependencyHostResultEntity> references = entityManager.createNamedQuery(
        "ServiceDependencyHostResultEntity.findByTask", ServiceDependencyHostResultEntity.class)
        .setParameter("taskId", taskId)
        .getResultList().stream()
        .sorted(java.util.Comparator.comparing(ServiceDependencyHostResultEntity::getBindingId))
        .toList();
    for (ServiceDependencyHostResultEntity reference : references) {
      ServiceDependencyBindingEntity binding = entityManager.find(
          ServiceDependencyBindingEntity.class, reference.getBindingId(),
          LockModeType.PESSIMISTIC_WRITE);
      ServiceDependencyOperationEntity operation = entityManager.find(
          ServiceDependencyOperationEntity.class, reference.getOperationId(),
          LockModeType.PESSIMISTIC_WRITE);
      ServiceDependencyHostResultEntity command = entityManager.find(
          ServiceDependencyHostResultEntity.class, commandId(reference),
          LockModeType.PESSIMISTIC_WRITE);
      try {
        requireCurrentCommandOwner(binding, operation, command);
      } catch (StaleApprovalException e) {
        continue;
      }
      if (command.getResultHash() != null) {
        continue;
      }
      if (!Objects.equals(command.getAmbariRequestId(), requestId)
          || !Objects.equals(command.getAmbariStageId(), stageId)
          || !Objects.equals(command.getAmbariTaskId(), taskId)) {
        continue;
      }
      long now = System.currentTimeMillis();
      command.setState("FAILED");
      command.setFailureCode(failureCode);
      command.setFailureMessage(failureMessage);
      command.setCheckTimestamp(now);
      boolean wasReady = "READY".equals(binding.getState())
          && Objects.equals(binding.getDesiredSnapshotVersion(), binding.getAppliedSnapshotVersion())
          && Objects.equals(binding.getProviderFingerprint(), binding.getAppliedProviderFingerprint());
      if (!wasReady || affectsReadiness(command, null)) {
        applyFailure(binding, operation, currentFailure(entityManager, command));
      }
      binding.setUpdateTimestamp(now);
      operation.setUpdateTimestamp(now);
    }
    entityManager.flush();
  }

  private ServiceDependencyHostResultEntity currentFailure(EntityManager entityManager,
      ServiceDependencyHostResultEntity current) {
    return entityManager.createQuery(
        "SELECT result FROM ServiceDependencyHostResultEntity result "
            + "WHERE result.bindingId=:bindingId AND result.snapshotVersion=:snapshotVersion "
            + "AND result.operationEpoch=:operationEpoch "
            + "AND result.state IN ('FAILED', 'STALE_REJECTED', 'RECONCILIATION_REQUIRED')",
        ServiceDependencyHostResultEntity.class)
        .setParameter("bindingId", current.getBindingId())
        .setParameter("snapshotVersion", current.getSnapshotVersion())
        .setParameter("operationEpoch", current.getOperationEpoch())
        .getResultStream()
        .sorted(Comparator.comparingInt(this::failurePriority)
            .thenComparing(ServiceDependencyHostResultEntity::getCheckTimestamp)
            .thenComparing(ServiceDependencyHostResultEntity::getHostId)
            .thenComparing(ServiceDependencyHostResultEntity::getDependencyType)
            .thenComparing(ServiceDependencyHostResultEntity::getCheckKind))
        .findFirst()
        .orElse(null);
  }

  private int failurePriority(ServiceDependencyHostResultEntity failure) {
    return switch (failure.getState()) {
      case "RECONCILIATION_REQUIRED" -> 0;
      case "STALE_REJECTED" -> 1;
      default -> 2;
    };
  }

  private void applyFailure(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity operation, ServiceDependencyHostResultEntity failure) {
    boolean reconciliation = "RECONCILIATION_REQUIRED".equals(failure.getState());
    binding.setState(reconciliation ? "FENCING_UNCERTAIN" : "FAILED");
    binding.setProvisioningPhase(reconciliation
        ? "ZOOKEEPER_HANDOFF_RECONCILING" : failure.getCheckKind());
    binding.setFailureCode(failure.getFailureCode());
    binding.setFailurePhase(failure.getCheckKind());
    binding.setFailureMessage(failure.getFailureMessage());
    binding.setFailureRetryable("FAILED".equals(failure.getState()));
    operation.setState(failure.getState());
    operation.setFailureCode(failure.getFailureCode());
    operation.setFailureMessage(failure.getFailureMessage());
  }

  private void finalizeDetach(EntityManager entityManager,
      ServiceDependencyBindingEntity binding) {
    ServiceDependencySnapshotEntity snapshot = entityManager.find(
        ServiceDependencySnapshotEntity.class, new ServiceDependencySnapshotEntityPK(
            binding.getBindingId(), binding.getDesiredSnapshotVersion()));
    if (snapshot == null) {
      throw new StaleApprovalException("The dependency snapshot required for detach is missing");
    }
    ServiceDependencyFenceEntity fence = new ServiceDependencyFenceEntity();
    fence.setBindingId(binding.getBindingId());
    fence.setFinalEpoch(binding.getOperationEpoch());
    fence.setImmutableSpecHash(stateHash(binding.getBindingId(), binding.getDependencyType(),
        Long.toString(binding.getConsumerClusterId()), binding.getConsumerServiceName(),
        Long.toString(binding.getProviderClusterId()), binding.getProviderServiceName(),
        snapshot.getSnapshotFingerprint()));
    fence.setDependencyType(binding.getDependencyType());
    fence.setConsumerClusterId(binding.getConsumerClusterId());
    fence.setConsumerServiceName(binding.getConsumerServiceName());
    fence.setProviderClusterId(binding.getProviderClusterId());
    fence.setProviderServiceName(binding.getProviderServiceName());
    fence.setNamespaceHash(stateHash(binding.getNamespaceRoot(), binding.getNamespaceWal(),
        binding.getNamespaceZnode()));
    ServiceDependencyOperationEntity detachOperation = entityManager.find(
        ServiceDependencyOperationEntity.class, binding.getActiveOperationId());
    if (detachOperation == null || !"DETACH".equals(detachOperation.getOperationKind())) {
      throw new StaleApprovalException("The detach operation identity is missing");
    }
    fence.setDetachOperationId(detachOperation.getOperationId());
    fence.setDetachRequestHash(detachOperation.getRequestHash());
    fence.setDetachedByUserId(binding.getUpdatedByUserId());
    fence.setDetachTimestamp(System.currentTimeMillis());
    entityManager.persist(fence);

    for (ServiceDependencyHostResultEntity result : entityManager.createQuery(
        "SELECT result FROM ServiceDependencyHostResultEntity result "
            + "WHERE result.bindingId=:bindingId", ServiceDependencyHostResultEntity.class)
        .setParameter("bindingId", binding.getBindingId()).getResultList()) {
      entityManager.remove(result);
    }
    for (ServiceDependencyOperationEntity operation : entityManager.createNamedQuery(
        "ServiceDependencyOperationEntity.findByBinding", ServiceDependencyOperationEntity.class)
        .setParameter("bindingId", binding.getBindingId()).getResultList()) {
      entityManager.remove(operation);
    }
    for (ServiceDependencySnapshotEntity historical : entityManager.createQuery(
        "SELECT snapshot FROM ServiceDependencySnapshotEntity snapshot "
            + "WHERE snapshot.bindingId=:bindingId", ServiceDependencySnapshotEntity.class)
        .setParameter("bindingId", binding.getBindingId()).getResultList()) {
      entityManager.remove(historical);
    }
    entityManager.remove(binding);
  }

  private String stateHash(String... values) {
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

  private boolean affectsReadiness(ServiceDependencyHostResultEntity command,
      Set<Long> requiredHostIds) {
    if (!command.getCheckKind().startsWith("PREPARE_")
        && !command.getCheckKind().startsWith("VERIFY_")) {
      return false;
    }
    if (requiredHostIds != null) {
      return requiredHostIds.contains(command.getHostId());
    }
    return Set.of("HBASE_MASTER", "HBASE_REGIONSERVER", "HBASE_THRIFT")
        .contains(command.getComponentName());
  }

  private boolean allRequiredHostsVerified(EntityManager entityManager,
      ServiceDependencyHostResultEntity command, Set<Long> requiredHostIds) {
    if (requiredHostIds.isEmpty()) {
      return false;
    }
    String verifyKind = "VERIFY_" + command.getDependencyType() + "_CONSUMER";
    String prepareKind = "PREPARE_" + command.getDependencyType() + "_CONSUMER";
    for (Long hostId : requiredHostIds) {
      ServiceDependencyHostResultEntity verification = entityManager.find(
          ServiceDependencyHostResultEntity.class, new ServiceDependencyHostResultEntityPK(
              command.getBindingId(), command.getSnapshotVersion(), command.getOperationEpoch(), hostId,
              command.getDependencyType(), verifyKind));
      ServiceDependencyHostResultEntity preparation = entityManager.find(
          ServiceDependencyHostResultEntity.class, new ServiceDependencyHostResultEntityPK(
              command.getBindingId(), command.getSnapshotVersion(), command.getOperationEpoch(), hostId,
              command.getDependencyType(), prepareKind));
      if (!isStrictCurrentPair(command, preparation, verification)) {
        return false;
      }
    }
    return true;
  }

  private boolean isStrictCurrentPair(ServiceDependencyHostResultEntity current,
      ServiceDependencyHostResultEntity preparation,
      ServiceDependencyHostResultEntity verification) {
    return preparation != null && verification != null
        && "SUCCEEDED".equals(preparation.getState())
        && "SUCCEEDED".equals(verification.getState())
        && preparation.getPreparationObservationId() != null
        && preparation.getCommandRequestHash() != null
        && preparation.getPreparationObservationFingerprint() != null
        && preparation.getPackageName() != null
        && preparation.getPackageVersion() != null
        && preparation.getClientSoftwareVersion() != null
        && preparation.getObservedPackageHash() != null
        && preparation.getRenderedConfigHash() != null
        && preparation.getIdentityFingerprint() != null
        && Objects.equals(current.getOperationId(), preparation.getOperationId())
        && Objects.equals(current.getOperationId(), verification.getOperationId())
        && Objects.equals(current.getOperationEpoch(), preparation.getOperationEpoch())
        && Objects.equals(current.getOperationEpoch(), verification.getOperationEpoch())
        && Objects.equals(preparation.getCommandRequestHash(),
            verification.getPreparationRequestHash())
        && Objects.equals(preparation.getPreparationObservationId(),
            verification.getPreparationObservationId())
        && Objects.equals(preparation.getPreparationObservationFingerprint(),
            verification.getPreparationObservationFingerprint())
        && Objects.equals(preparation.getPackageName(), verification.getPackageName())
        && Objects.equals(preparation.getPackageVersion(), verification.getPackageVersion())
        && Objects.equals(preparation.getClientSoftwareVersion(),
            verification.getClientSoftwareVersion())
        && Objects.equals(preparation.getObservedPackageHash(),
            verification.getObservedPackageHash())
        && Objects.equals(preparation.getRenderedConfigHash(), verification.getRenderedConfigHash())
        && Objects.equals(preparation.getIdentityFingerprint(), verification.getIdentityFingerprint())
        && verification.getObservedPackageHash() != null;
  }

  private void requireCurrentCommandOwner(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity operation, ServiceDependencyHostResultEntity command) {
    if (binding == null || operation == null || command == null
        || !Objects.equals(binding.getActiveOperationId(), command.getOperationId())
        || !Objects.equals(binding.getOperationEpoch(), command.getOperationEpoch())
        || !Objects.equals(binding.getDesiredSnapshotVersion(), command.getSnapshotVersion())
        || !Objects.equals(operation.getBindingId(), command.getBindingId())
        || !Objects.equals(operation.getOperationEpoch(), command.getOperationEpoch())
        || !Objects.equals(operation.getTargetSnapshotVersion(), command.getSnapshotVersion())
        || "DETACHING".equals(binding.getState()) || "RETIRED".equals(binding.getState())) {
      throw new StaleApprovalException("The dependency command no longer owns the active operation");
    }
  }

  private void requireNextLifecycleOperation(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity operation, String kind, long snapshotVersion) {
    if (!binding.getBindingId().equals(operation.getBindingId())
        || !kind.equals(operation.getOperationKind())
        || operation.getOperationEpoch() == null
        || operation.getOperationEpoch() != binding.getOperationEpoch() + 1
        || !Objects.equals(operation.getTargetSnapshotVersion(), snapshotVersion)
        || operation.getRequestHash() == null || operation.getRequestHash().isBlank()) {
      throw new StaleApprovalException("The lifecycle operation does not own the next binding epoch");
    }
  }

  private void requireCurrentCommandOwnerForTransition(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity operation, ServiceDependencyHostResultEntity command) {
    if (command == null || !binding.getBindingId().equals(command.getBindingId())
        || !operation.getOperationId().equals(command.getOperationId())
        || !operation.getOperationEpoch().equals(command.getOperationEpoch())
        || !operation.getTargetSnapshotVersion().equals(command.getSnapshotVersion())
        || !"INVALIDATE_BINDING_EPOCH".equals(command.getCheckKind())
        || !Objects.equals(binding.getActionHostId(), command.getHostId())) {
      throw new StaleApprovalException("The detach command does not own the next binding epoch");
    }
  }

  private void requireExactLifecycleRetry(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity existing, ServiceDependencyOperationEntity requested) {
    if (!binding.getBindingId().equals(existing.getBindingId())
        || !binding.getActiveOperationId().equals(existing.getOperationId())
        || !Objects.equals(binding.getOperationEpoch(), existing.getOperationEpoch())
        || !Objects.equals(existing.getBindingId(), requested.getBindingId())
        || !Objects.equals(existing.getOperationKind(), requested.getOperationKind())
        || !Objects.equals(existing.getOperationEpoch(), requested.getOperationEpoch())
        || !Objects.equals(existing.getTargetSnapshotVersion(), requested.getTargetSnapshotVersion())
        || !Objects.equals(existing.getRequestHash(), requested.getRequestHash())) {
      throw new StaleApprovalException("The operation UUID belongs to a different lifecycle request");
    }
  }

  private void requireExactUpdateReplay(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity existing, ServiceDependencyOperationEntity requested,
      ServiceDependencySnapshotEntity requestedSnapshot,
      ServiceDependencyHostResultEntity initialCommand, EntityManager entityManager) {
    if (!binding.getBindingId().equals(existing.getBindingId())
        || !Objects.equals(existing.getBindingId(), requested.getBindingId())
        || !Objects.equals(existing.getOperationId(), requested.getOperationId())
        || !"UPDATE".equals(existing.getOperationKind())
        || !Objects.equals(existing.getOperationKind(), requested.getOperationKind())
        || !Objects.equals(existing.getOperationEpoch(), requested.getOperationEpoch())
        || !Objects.equals(existing.getTargetSnapshotVersion(), requested.getTargetSnapshotVersion())
        || !Objects.equals(existing.getRequestHash(), requested.getRequestHash())
        || requestedSnapshot == null
        || !Objects.equals(existing.getBindingId(), requestedSnapshot.getBindingId())
        || !Objects.equals(existing.getTargetSnapshotVersion(), requestedSnapshot.getSnapshotVersion())) {
      throw new StaleApprovalException("The operation UUID belongs to a different update request");
    }
    List<ServiceDependencyHostResultEntity> persisted = entityManager.createQuery(
        "SELECT result FROM ServiceDependencyHostResultEntity result "
            + "WHERE result.bindingId=:bindingId AND result.operationId=:operationId",
        ServiceDependencyHostResultEntity.class)
        .setParameter("bindingId", binding.getBindingId())
        .setParameter("operationId", existing.getOperationId())
        .getResultList().stream().filter(command -> isProviderCommand(command.getCheckKind()))
        .toList();
    if (initialCommand == null) {
      if (!persisted.isEmpty()) {
        throw new StaleApprovalException("The update replay omitted its provider intent");
      }
      return;
    }
    ServiceDependencyHostResultEntity actual = entityManager.find(
        ServiceDependencyHostResultEntity.class, commandId(initialCommand),
        LockModeType.PESSIMISTIC_READ);
    if (actual == null || persisted.size() != 1 || !sameImmutableCommand(initialCommand, actual)) {
      throw new StaleApprovalException("The update replay does not match its provider intent");
    }
  }

  private void requireUpdateIdentity(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity current, ServiceDependencySnapshotEntity requestedSnapshot,
      ServiceDependencyOperationEntity requested) {
    if (requestedSnapshot.getSnapshotVersion() != binding.getDesiredSnapshotVersion() + 1
        || !Objects.equals(requestedSnapshot.getBindingId(), binding.getBindingId())
        || !Objects.equals(requestedSnapshot.getConsumerFingerprint(), current.getConsumerFingerprint())
        || !Objects.equals(requested.getTargetSnapshotVersion(), requestedSnapshot.getSnapshotVersion())) {
      throw new StaleApprovalException("The update changes immutable consumer or provider identity");
    }
    try {
      ManagedDependencySnapshot oldSnapshot = StageUtils.getGson().fromJson(
          current.getSnapshotJson(), ManagedDependencySnapshot.class);
      ManagedDependencySnapshot newSnapshot = StageUtils.getGson().fromJson(
          requestedSnapshot.getSnapshotJson(), ManagedDependencySnapshot.class);
      if (oldSnapshot == null || newSnapshot == null
          || !oldSnapshot.providerService().equals(newSnapshot.providerService())
          || !oldSnapshot.consumerIdentity().equals(newSnapshot.consumerIdentity())
          || !oldSnapshot.namespace().equals(newSnapshot.namespace())
          || oldSnapshot.type() != newSnapshot.type()) {
        throw new StaleApprovalException("The update changes immutable consumer or provider identity");
      }
    } catch (StaleApprovalException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new StaleApprovalException("The update snapshots cannot be verified");
    }
  }

  private void validateUpdateProviderIntent(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity operation, ServiceDependencySnapshotEntity snapshot,
      ServiceDependencyHostResultEntity command) {
    if (command.getBindingId() == null || command.getOperationId() == null
        || command.getSnapshotVersion() == null || command.getSnapshotVersion() <= 0
        || command.getOperationEpoch() == null || command.getOperationEpoch() <= 0
        || command.getHostId() == null || command.getHostId() <= 0
        || !Objects.equals(command.getBindingId(), binding.getBindingId())
        || !Objects.equals(command.getOperationId(), operation.getOperationId())
        || !Objects.equals(command.getOperationEpoch(), operation.getOperationEpoch())
        || !Objects.equals(command.getSnapshotVersion(), snapshot.getSnapshotVersion())
        || !Objects.equals(command.getDependencyType(), binding.getDependencyType())
        || command.getCheckKind() == null || command.getCommandRequestHash() == null
        || command.getCommandRequestHash().isBlank() || command.getCommandJson() == null
        || command.getCommandJson().isBlank() || command.getRequiredPackageHash() == null
        || command.getRequiredPackageHash().isBlank() || !isProviderCommand(command.getCheckKind())) {
      throw new IllegalArgumentException("The update initial command is not a provider intent");
    }
  }

  private void validateRetryPreparationIntent(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity operation, ServiceDependencyHostResultEntity command) {
    if (command.getBindingId() == null || command.getOperationId() == null
        || command.getSnapshotVersion() == null || command.getSnapshotVersion() <= 0
        || command.getOperationEpoch() == null || command.getOperationEpoch() <= 0
        || command.getHostId() == null || command.getHostId() <= 0
        || !Objects.equals(command.getBindingId(), binding.getBindingId())
        || !Objects.equals(command.getOperationId(), operation.getOperationId())
        || !Objects.equals(command.getOperationEpoch(), operation.getOperationEpoch())
        || !Objects.equals(command.getSnapshotVersion(), binding.getDesiredSnapshotVersion())
        || !Objects.equals(command.getDependencyType(), binding.getDependencyType())
        || command.getCheckKind() == null
        || !command.getCheckKind().equals("PREPARE_" + binding.getDependencyType() + "_CONSUMER")
        || command.getCommandRequestHash() == null || command.getCommandRequestHash().isBlank()
        || command.getCommandJson() == null || command.getCommandJson().isBlank()
        || command.getRequiredPackageHash() == null || command.getRequiredPackageHash().isBlank()) {
      throw new IllegalArgumentException("The retry command is not a consumer preparation intent");
    }
  }

  private ServiceDependencyHostResultEntityPK commandId(ServiceDependencyHostResultEntity command) {
    return new ServiceDependencyHostResultEntityPK(command.getBindingId(), command.getSnapshotVersion(),
        command.getOperationEpoch(), command.getHostId(), command.getDependencyType(),
        command.getCheckKind());
  }

  private boolean isProviderCommand(String checkKind) {
    return Set.of("PREPARE_BINDING_JOURNAL", "INITIALIZE_BINDING_JOURNAL",
        "PROVISION_HDFS_NAMESPACE", "PROVISION_ZOOKEEPER_NAMESPACE",
        "INVALIDATE_BINDING_EPOCH").contains(checkKind);
  }

  private void lockAndValidateDraft(EntityManager entityManager, DraftGuard guard) {
    if (guard == null) {
      return;
    }
    ScopedWorkflowStateEntity draft = entityManager.find(ScopedWorkflowStateEntity.class,
        guard.scopeKey(), LockModeType.PESSIMISTIC_WRITE);
    if (draft == null || !Objects.equals(draft.getOwnerUserId(), guard.ownerUserId())
        || !Objects.equals(draft.getRevision(), guard.revision())
        || !Objects.equals(draft.getCreatedClusterId(), guard.consumerClusterId())
        || !"CLUSTER_CREATE".equals(draft.getWorkflow())) {
      throw new StaleApprovalException(
          "The cluster creation draft changed after dependency preview");
    }
  }

  private void lockAndValidateRepositories(EntityManager entityManager,
      List<RepositoryGuard> guards) {
    Map<Long, RepositoryGuard> unique = new LinkedHashMap<>();
    guards.stream().sorted(Comparator.comparingLong(RepositoryGuard::rowId))
        .forEach(guard -> unique.putIfAbsent(guard.rowId(), guard));
    for (RepositoryGuard guard : unique.values()) {
      RepositoryVersionEntity repository = entityManager.find(RepositoryVersionEntity.class,
          guard.rowId(), LockModeType.PESSIMISTIC_READ);
      if (repository == null || !Objects.equals(repository.getVersion(), guard.version())
          || repository.isResolved() != guard.resolved()) {
        throw new StaleApprovalException(
            "A repository version changed after dependency preview");
      }
    }
  }

  private void lockAndValidateServiceVersions(EntityManager entityManager,
      List<ServiceVersionGuard> guards) {
    for (ServiceVersionGuard guard : guards.stream().sorted(Comparator
        .comparingLong(ServiceVersionGuard::clusterId)
        .thenComparing(ServiceVersionGuard::serviceName)).toList()) {
      ServiceDesiredStateEntityPK serviceId = new ServiceDesiredStateEntityPK();
      serviceId.setClusterId(guard.clusterId());
      serviceId.setServiceName(guard.serviceName());
      ServiceDesiredStateEntity service = entityManager.find(ServiceDesiredStateEntity.class,
          serviceId, LockModeType.PESSIMISTIC_READ);
      if (service == null || service.getDesiredRepositoryVersion() == null
          || !Objects.equals(service.getDesiredRepositoryVersion().getId(), guard.repositoryRowId())) {
        throw new StaleApprovalException(
            "A service repository target changed after dependency preview");
      }

      List<ServiceComponentDesiredStateEntity> components = entityManager.createQuery(
          "SELECT component FROM ServiceComponentDesiredStateEntity component " +
              "WHERE component.clusterId=:clusterId AND component.serviceName=:serviceName " +
              "ORDER BY component.componentName",
          ServiceComponentDesiredStateEntity.class)
          .setParameter("clusterId", guard.clusterId())
          .setParameter("serviceName", guard.serviceName())
          .setLockMode(LockModeType.PESSIMISTIC_READ)
          .getResultList();
      if (!components.stream().map(ServiceComponentDesiredStateEntity::getComponentName)
          .collect(java.util.stream.Collectors.toSet()).equals(guard.componentNames())
          || components.stream().anyMatch(component -> component.getDesiredRepositoryVersion() == null
              || !Objects.equals(component.getDesiredRepositoryVersion().getId(), guard.repositoryRowId()))) {
        throw new StaleApprovalException(
            "Service component repository targets changed after dependency preview");
      }

      Map<String, String> actualVersions = new TreeMap<>();
      for (HostComponentStateEntity state : entityManager.createQuery(
          "SELECT state FROM HostComponentStateEntity state " +
              "WHERE state.clusterId=:clusterId AND state.serviceName=:serviceName ORDER BY state.id",
          HostComponentStateEntity.class)
          .setParameter("clusterId", guard.clusterId())
          .setParameter("serviceName", guard.serviceName())
          .setLockMode(LockModeType.PESSIMISTIC_READ)
          .getResultList()) {
        if (guard.versionAdvertisedComponents().contains(state.getComponentName())) {
          if (state.getUpgradeState() != org.apache.ambari.server.state.UpgradeState.NONE) {
            throw new StaleApprovalException(
                "A version-advertising component entered an upgrade after dependency preview");
          }
          actualVersions.put(state.getComponentName() + "\u0000" + state.getHostName(),
              state.getVersion());
        }
      }
      if (!actualVersions.equals(guard.observedVersions())) {
        throw new StaleApprovalException(
            "Advertised component versions changed after dependency preview");
      }
    }
  }

  private void lockService(EntityManager entityManager, ManagedDependencyServiceKey key) {
    ClusterServiceEntityPK id = new ClusterServiceEntityPK();
    id.setClusterId(key.clusterId());
    id.setServiceName(key.serviceName());
    if (entityManager.find(ClusterServiceEntity.class, id, LockModeType.PESSIMISTIC_WRITE) == null) {
      throw new StaleApprovalException("A referenced cluster service no longer exists");
    }
  }

  public record CreationItem(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity snapshot, ServiceDependencyOperationEntity operation,
      ServiceDependencyHostResultEntity initialCommand, CreationGuard guard) {
    public CreationItem {
      Objects.requireNonNull(binding, "binding");
      Objects.requireNonNull(snapshot, "snapshot");
      Objects.requireNonNull(operation, "operation");
      Objects.requireNonNull(guard, "guard");
    }
  }

  public record CreationResult(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity snapshot, ServiceDependencyOperationEntity operation,
      ServiceDependencyHostResultEntity initialCommand, boolean created) {
  }

  public record CreationBatchResult(List<CreationResult> items) {
    public CreationBatchResult {
      if (items == null || items.isEmpty() || items.size() > 2) {
        throw new IllegalArgumentException("A dependency plan result must contain one or two items");
      }
      items = List.copyOf(items);
    }
  }

  private record ExistingCreation(ServiceDependencyBindingEntity binding,
      ServiceDependencySnapshotEntity snapshot, ServiceDependencyOperationEntity operation,
      ServiceDependencyHostResultEntity initialCommand) {
  }

  public record CreationGuard(DraftGuard draft, List<RepositoryGuard> repositories,
      List<ServiceVersionGuard> services) {
    public CreationGuard {
      repositories = repositories == null ? List.of() : List.copyOf(repositories);
      services = services == null ? List.of() : List.copyOf(services);
    }
  }

  public record DraftGuard(String scopeKey, int ownerUserId, long revision,
      long consumerClusterId) {
    public DraftGuard {
      Objects.requireNonNull(scopeKey, "scopeKey");
    }
  }

  public record RepositoryGuard(long rowId, String version, boolean resolved) {
    public RepositoryGuard {
      if (rowId <= 0) {
        throw new IllegalArgumentException("A positive repository row ID is required");
      }
      Objects.requireNonNull(version, "version");
    }
  }

  public record ServiceVersionGuard(long clusterId, String serviceName, long repositoryRowId,
      Set<String> componentNames, Set<String> versionAdvertisedComponents,
      Map<String, String> observedVersions) {
    public ServiceVersionGuard {
      if (clusterId <= 0 || repositoryRowId <= 0) {
        throw new IllegalArgumentException("Positive cluster and repository IDs are required");
      }
      Objects.requireNonNull(serviceName, "serviceName");
      componentNames = Set.copyOf(componentNames);
      versionAdvertisedComponents = Set.copyOf(versionAdvertisedComponents);
      observedVersions = Map.copyOf(observedVersions);
    }
  }

  public record LifecycleTransition(ServiceDependencyBindingEntity binding,
      ServiceDependencyOperationEntity operation,
      ServiceDependencyHostResultEntity command, boolean created) {
  }

  /** Immutable facts echoed by preview-update and checked only for new updates. */
  public record UpdateFacts(long snapshotVersion, String providerFingerprint,
      String consumerFingerprint, String snapshotFingerprint, String snapshotApproval) {
    public UpdateFacts {
      if (snapshotVersion <= 0) {
        throw new IllegalArgumentException("A positive snapshot version is required");
      }
      Objects.requireNonNull(providerFingerprint, "providerFingerprint");
      Objects.requireNonNull(consumerFingerprint, "consumerFingerprint");
      Objects.requireNonNull(snapshotFingerprint, "snapshotFingerprint");
      Objects.requireNonNull(snapshotApproval, "snapshotApproval");
    }
  }

  public record CommandCompletion(String resultJson, String resultHash, String commandState,
      String failureCode, String failurePhase, String failureMessage, boolean retryable,
      String bindingState, String bindingPhase, String operationState,
      String providerPreparationHash, Long appliedSnapshotVersion,
      String appliedProviderFingerprint, String preparationObservationId,
      String preparationRequestHash, String preparationObservationFingerprint,
      String packageName, String packageVersion, String clientSoftwareVersion,
      String observedPackageHash, String renderedConfigHash, String identityFingerprint,
      Set<Long> readinessHostIds,
      ServiceDependencyHostResultEntity nextCommand) {
    public CommandCompletion {
      Objects.requireNonNull(resultJson, "resultJson");
      Objects.requireNonNull(resultHash, "resultHash");
      Objects.requireNonNull(commandState, "commandState");
      Objects.requireNonNull(bindingState, "bindingState");
      Objects.requireNonNull(operationState, "operationState");
      readinessHostIds = readinessHostIds == null ? null : Set.copyOf(readinessHostIds);
    }
  }

  public static final class StaleApprovalException extends IllegalStateException {
    public StaleApprovalException(String message) {
      super(message);
    }
  }
}
