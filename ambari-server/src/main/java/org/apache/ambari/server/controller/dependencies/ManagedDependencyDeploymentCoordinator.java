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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.dao.ServiceDependencyDeploymentDAO;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyDeploymentEntity;
import org.apache.ambari.server.security.authentication.AmbariUserAuthentication;
import org.apache.ambari.server.security.authentication.AmbariUserDetailsImpl;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
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

/** Sole owner of INSTALL -> dependency readiness -> START for a durable consumer deployment. */
@Singleton
public class ManagedDependencyDeploymentCoordinator {
  private static final Logger LOG = LoggerFactory.getLogger(ManagedDependencyDeploymentCoordinator.class);
  private static final Set<String> ACTIVE = Set.of("NEW", "WAIT_PROVIDER", "INSTALLING", "WAIT_DEPENDENCIES", "STARTING", "CHECKING");
  private final ServiceDependencyDeploymentDAO deployments;
  private final ServiceDependencyDAO dependencies;
  private final Clusters clusters;
  private final Users users;
  private final Provider<ActionManager> actions;
  private final Provider<AmbariManagementController> controller;
  private final ResourceProviderFactory resourceProviders;
  private final Provider<ManagedServiceDependencyCoordinator> bindings;
  private int recoveryOffset;

  @Inject
  public ManagedDependencyDeploymentCoordinator(ServiceDependencyDeploymentDAO deployments,
      ServiceDependencyDAO dependencies, Clusters clusters, Users users, Provider<ActionManager> actions,
      Provider<AmbariManagementController> controller, ResourceProviderFactory resourceProviders,
      Provider<ManagedServiceDependencyCoordinator> bindings) {
    this.deployments = deployments;
    this.dependencies = dependencies;
    this.clusters = clusters;
    this.users = users;
    this.actions = actions;
    this.controller = controller;
    this.resourceProviders = resourceProviders;
    this.bindings = bindings;
  }

  public Map<String, Object> launch(String clusterName, UUID id, List<Target> proposed, boolean installOnly) {
    Cluster cluster = cluster(clusterName, true);
    List<Target> targets = canonicalTargets(cluster, proposed);
    List<BindingVersion> versions = dependencies.findByConsumer(cluster.getClusterId(), "HBASE").stream()
        .map(value -> new BindingVersion(value.getBindingId(), value.getDesiredSnapshotVersion(), value.getOperationEpoch()))
        .sorted(Comparator.comparing(BindingVersion::bindingId)).toList();
    if (versions.isEmpty() || targets.stream().noneMatch(target -> "HBASE".equals(target.serviceName()))) {
      throw conflict("DEPLOYMENT_PLAN_INVALID", "A managed deployment must contain HBase and approved bindings.");
    }
    long now = System.currentTimeMillis();
    ServiceDependencyDeploymentEntity value = new ServiceDependencyDeploymentEntity();
    value.setDeploymentId(id.toString());
    value.setClusterId(cluster.getClusterId());
    value.setOwnerUserId(AuthorizationHelper.getAuthenticatedId());
    value.setPlanJson(StageUtils.getGson().toJson(new Plan(targets, versions, installOnly)));
    value.setProgressJson(StageUtils.getGson().toJson(new Progress(id.toString(), null, "INSTALL", List.of(), null)));
    value.setState("NEW");
    value.setRowVersion(0L);
    value.setCreateTimestamp(now);
    value.setUpdateTimestamp(now);
    ServiceDependencyDeploymentEntity existing = deployments.find(id.toString());
    if (existing != null) {
      requireOwner(cluster, existing);
      Plan original = plan(existing);
      // Later binding retries do not change the immutable launch target/installation identity.
      if (!original.targets().equals(targets) || original.installOnly() != installOnly) {
        throw conflict("DEPLOYMENT_ID_CONFLICT", "The deployment ID belongs to a different target plan.");
      }
      return summary(cluster, existing);
    }
    try {
      return withParentLocks(cluster, () -> summary(cluster, deployments.create(value)));
    } catch (RuntimeException e) {
      throw conflict("DEPLOYMENT_CONFLICT", "Resume the existing deployment or review the target plan.");
    }
  }

  public Map<String, Object> get(String clusterName, UUID id) {
    Cluster cluster = cluster(clusterName, false);
    ServiceDependencyDeploymentEntity value = owned(cluster, id);
    return summary(cluster, value);
  }

  public Map<String, Object> retry(String clusterName, UUID id, UUID attemptId) {
    Cluster cluster = cluster(clusterName, true);
    ServiceDependencyDeploymentEntity current = owned(cluster, id);
    requireOwner(cluster, current);
    return withParentLocks(cluster, () -> {
      try {
        return summary(cluster, deployments.mutate(id.toString(), value -> {
          Progress previous = progress(value);
          if (previous.attemptId().equals(attemptId.toString()) || previous.history().stream()
              .anyMatch(attempt -> attempt.attemptId().equals(attemptId.toString()))) {
            return;
          }
          if (!retryable(value, bindings.get().list(clusterName, "HBASE"))) {
            throw conflict("DEPLOYMENT_NOT_RETRYABLE", "Reconcile the current deployment before retrying.");
          }
          if ("INSTALL".equals(previous.phase()) && previous.requestId() != null) {
            bindings.get().retryInstallation(clusterName, previous.requestId(), attemptId);
          } else {
            for (ServiceDependencyBindingEntity binding : dependencies.findByConsumer(value.getClusterId(), "HBASE")) {
              if ("FAILED".equals(binding.getState())) {
                UUID operationId = UUID.nameUUIDFromBytes((attemptId + ":" + binding.getBindingId())
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                bindings.get().retry(clusterName, "HBASE", UUID.fromString(binding.getBindingId()),
                    new ManagedServiceDependencyCoordinator.LifecycleRequest(operationId, binding.getRowVersion()));
              }
            }
          }
          value.setState(Set.of("START", "CHECKS", "DEPENDENCIES").contains(previous.phase())
              ? "WAIT_DEPENDENCIES" : "NEW");
          value.setProgressJson(StageUtils.getGson().toJson(new Progress(attemptId.toString(), null,
              previous.phase(), previous.history(), null)));
        }));
      } catch (AmbariException e) {
        throw conflict("DEPLOYMENT_RETRY_FAILED", "The deployment could not be retried; reload its current state.");
      }
    });
  }

  /** Called by the shared bounded task/restart reconciler; no browser is required. */
  public synchronized void recoverOutstanding() {
    List<ServiceDependencyDeploymentEntity> page = deployments.active(recoveryOffset, 64);
    recoveryOffset = page.size() < 64 ? 0 : recoveryOffset + page.size();
    for (ServiceDependencyDeploymentEntity value : page) {
      Authentication saved = SecurityContextHolder.getContext().getAuthentication();
      try {
        var owner = users.getUserEntity(value.getOwnerUserId());
        if (owner == null || !Boolean.TRUE.equals(owner.getActive())) {
          throw new IllegalStateException("Deployment owner is inactive");
        }
        SecurityContextHolder.getContext().setAuthentication(new AmbariUserAuthentication(null,
            new AmbariUserDetailsImpl(users.getUser(owner), null, users.getUserAuthorities(owner)), true));
        Cluster cluster = clusters.getClusterById(value.getClusterId());
        authorize(cluster, true);
        withParentLocks(cluster, () -> {
          try {
            return deployments.mutate(value.getDeploymentId(), current -> advance(cluster, current));
          } catch (AmbariException e) {
            throw new IllegalStateException("Deployment publication failed", e);
          }
        });
      } catch (RuntimeException | AmbariException e) {
        LOG.warn("Deployment reconciliation deferred: id={}, reason={}", value.getDeploymentId(), e.getClass().getSimpleName());
        try {
          deployments.mutate(value.getDeploymentId(), current -> {
            // A committed request remains authoritative even if the caller lost its result.
            if (!Set.of("INSTALLING", "STARTING", "CHECKING").contains(current.getState())) {
              fail(current, "DEPLOYMENT_PUBLICATION_OR_AUTHORIZATION_FAILED");
            }
          });
        } catch (AmbariException ignored) {
          LOG.warn("Could not record deferred deployment {}", value.getDeploymentId());
        }
      } finally {
        SecurityContextHolder.getContext().setAuthentication(saved);
      }
    }
  }

  private void advance(Cluster cluster, ServiceDependencyDeploymentEntity value) throws Exception {
    if (!ACTIVE.contains(value.getState())) {
      return;
    }
    Plan plan = plan(value);
    canonicalTargets(cluster, plan.targets());
    List<ServiceDependencyBindingEntity> currentBindings = dependencies.findByConsumer(cluster.getClusterId(), "HBASE");
    if (currentBindings.size() != plan.bindings().size()) {
      value.setState("UNRESOLVED");
      setFailure(value, "DEPLOYMENT_BINDING_LINEAGE_CHANGED");
      return;
    }
    for (BindingVersion expected : plan.bindings()) {
      ServiceDependencyBindingEntity current = currentBindings.stream()
          .filter(binding -> expected.bindingId().equals(binding.getBindingId())).findFirst().orElse(null);
      if (current == null || !Objects.equals(current.getDesiredSnapshotVersion(), expected.snapshotVersion())) {
        value.setState("UNRESOLVED");
        setFailure(value, "DEPLOYMENT_BINDING_LINEAGE_CHANGED");
        return;
      }
    }
    Progress progress = progress(value);
    if (Set.of("INSTALLING", "STARTING", "CHECKING").contains(value.getState())) {
      if (progress.requestId() == null) {
        value.setState("UNRESOLVED");
        setFailure(value, "DEPLOYMENT_REQUEST_LINEAGE_MISSING");
        return;
      }
      var requests = actions.get().getRequests(List.of(progress.requestId()));
      if (requests.size() != 1 || !Objects.equals(requests.get(0).getClusterId(), cluster.getClusterId())) {
        value.setState("UNRESOLVED");
        setFailure(value, "DEPLOYMENT_REQUEST_OWNERSHIP_INVALID");
        return;
      }
      List<HostRoleCommand> tasks = actions.get().getRequestTasks(progress.requestId());
      if (tasks.isEmpty()) {
        value.setState("UNRESOLVED");
        setFailure(value, "DEPLOYMENT_TASK_LINEAGE_MISSING");
        return;
      }
      if (tasks.stream().anyMatch(task -> !task.getStatus().isCompletedState())) {
        return;
      }
      if (tasks.stream().anyMatch(task -> task.getStatus() != HostRoleStatus.COMPLETED)
          || !targetsSatisfied(cluster, plan.targets(), !"INSTALLING".equals(value.getState()))) {
        fail(value, "DEPLOYMENT_TASK_FAILED");
        return;
      }
      if ("CHECKING".equals(value.getState())) {
        value.setState("COMPLETE");
        return;
      }
      if ("STARTING".equals(value.getState())) {
        publishChecks(cluster, value);
        return;
      }
      value.setState(plan.installOnly() ? "INSTALL_ONLY" : "WAIT_DEPENDENCIES");
      if (!"WAIT_DEPENDENCIES".equals(value.getState())) {
        return;
      }
    }
    if (Set.of("NEW", "WAIT_PROVIDER").contains(value.getState())) {
      List<Map<String, Object>> summaries = bindings.get().list(cluster.getClusterName(), "HBASE");
      if (currentBindings.stream().anyMatch(binding ->
          Set.of("FAILED", "STALE", "FENCING_UNCERTAIN", "DETACHING", "RETIRED").contains(binding.getState()))) {
        fail(value, "DEPLOYMENT_PROVIDER_NOT_READY");
        return;
      }
      if (!currentBindings.stream().allMatch(binding -> binding.getProviderPreparationHash() != null)) {
        value.setState("WAIT_PROVIDER");
        return;
      }
      boolean pendingHbase = plan.targets().stream().anyMatch(target -> "HBASE".equals(target.serviceName())
          && !targetSatisfied(cluster, target, false));
      for (Map<String, Object> summary : summaries) {
        if (pendingHbase && "managed".equals(summary.get("ownership"))) {
          @SuppressWarnings("unchecked") Map<String, Object> capabilities = (Map<String, Object>) summary.get("capabilities");
          if (capabilities == null || !Boolean.TRUE.equals(capabilities.get("install_or_configure_allowed"))) {
            value.setState("WAIT_PROVIDER");
            return;
          }
        }
      }
      publish(cluster, value, false);
    } else if ("WAIT_DEPENDENCIES".equals(value.getState())) {
      if (currentBindings.stream().anyMatch(binding -> Set.of("FAILED", "STALE", "FENCING_UNCERTAIN").contains(binding.getState()))) {
        Progress current = progress(value);
        value.setProgressJson(StageUtils.getGson().toJson(new Progress(current.attemptId(), current.requestId(),
            "DEPENDENCIES", current.history(), null)));
        fail(value, "DEPLOYMENT_DEPENDENCY_FAILED");
        return;
      }
      if (currentBindings.stream().allMatch(binding -> "READY".equals(binding.getState()))) {
        if ("CHECKS".equals(progress(value).phase())) publishChecks(cluster, value);
        else publish(cluster, value, true);
      }
    }
  }

  private void publish(Cluster cluster, ServiceDependencyDeploymentEntity value, boolean start) throws Exception {
    Plan plan = plan(value);
    List<Target> pending = plan.targets().stream()
        .filter(target -> !targetSatisfied(cluster, target, start)).toList();
    Progress previous = progress(value);
    String phase = start ? "START" : "INSTALL";
    Long requestId = null;
    if (!pending.isEmpty()) {
      Predicate[] targets = pending.stream().map(target -> new PredicateBuilder()
          .property("HostRoles/cluster_name").equals(cluster.getClusterName()).and()
          .property("HostRoles/service_name").equals(target.serviceName()).and()
          .property("HostRoles/component_name").equals(target.componentName()).and()
          .property("HostRoles/host_name").equals(target.hostName()).toPredicate()).toArray(Predicate[]::new);
      HostComponentResourceProvider provider = (HostComponentResourceProvider)
          resourceProviders.getHostComponentResourceProvider(controller.get());
      RequestStageContainer stages = provider.doUpdateResources(null,
          PropertyHelper.getUpdateRequest(Map.of("HostRoles/state", start ? "STARTED" : "INSTALLED"),
              Map.of("context", start ? "Start managed deployment" : "Install managed deployment")),
          new OrPredicate(targets), true, false, false);
      if (stages == null || stages.getStages().isEmpty()) {
        if (!targetsSatisfied(cluster, plan.targets(), start)) {
          throw new AmbariException("No task was generated for incomplete deployment targets");
        }
      } else {
        stages.persist();
        requestId = stages.getId();
      }
    }
    List<Attempt> history = new ArrayList<>(previous.history());
    history.add(new Attempt(previous.attemptId(), phase, requestId, pending));
    value.setProgressJson(StageUtils.getGson().toJson(new Progress(previous.attemptId(), requestId,
        phase, List.copyOf(history), null)));
    if (start && requestId == null) {
      publishChecks(cluster, value);
      return;
    }
    // A no-op is established by exact target state, never by a missing response ID.
    value.setState(requestId != null ? (start ? "STARTING" : "INSTALLING")
        : start ? "COMPLETE" : plan.installOnly() ? "INSTALL_ONLY" : "WAIT_DEPENDENCIES");
  }

  private void publishChecks(Cluster cluster, ServiceDependencyDeploymentEntity value) throws AmbariException {
    List<org.apache.ambari.server.controller.internal.RequestResourceFilter> filters = new ArrayList<>();
    for (String service : plan(value).targets().stream().filter(target -> {
      try { return !cluster.getService(target.serviceName()).getServiceComponent(target.componentName()).isClientComponent(); }
      catch (AmbariException e) { throw new IllegalStateException(e); }
    }).map(Target::serviceName).distinct().sorted().toList()) {
      filters.add(new org.apache.ambari.server.controller.internal.RequestResourceFilter(service, null, null));
    }
    if (filters.isEmpty()) {
      value.setState("COMPLETE");
      return;
    }
    var response = controller.get().createAction(new org.apache.ambari.server.controller.ExecuteActionRequest(
        cluster.getClusterName(), "SERVICE_CHECK", null, filters, null, Map.of(), true),
        Map.of("context", "Check managed deployment services"));
    if (response == null || response.getRequestId() <= 0 || response.getTasks() == null || response.getTasks().isEmpty()) {
      throw new AmbariException("The service checks did not publish an owned request");
    }
    Progress previous = progress(value);
    List<Attempt> history = new ArrayList<>(previous.history());
    history.add(new Attempt(previous.attemptId(), "CHECKS", response.getRequestId(), plan(value).targets()));
    value.setProgressJson(StageUtils.getGson().toJson(new Progress(previous.attemptId(), response.getRequestId(),
        "CHECKS", List.copyOf(history), null)));
    value.setState("CHECKING");
  }

  private List<Target> canonicalTargets(Cluster cluster, List<Target> targets) {
    if (targets == null || targets.isEmpty() || targets.size() > 10000) {
      throw conflict("DEPLOYMENT_TARGETS_INVALID", "The exact deployment targets are required.");
    }
    return targets.stream().map(target -> {
      try {
        ServiceComponentHost host = cluster.getService(target.serviceName())
            .getServiceComponent(target.componentName()).getServiceComponentHost(target.hostName());
        long hostId = host.getHost().getHostId();
        if (target.hostId() != 0 && target.hostId() != hostId) {
          throw new IllegalStateException("The deployment host identity changed");
        }
        return new Target(target.serviceName(), target.componentName(), target.hostName(), hostId);
      } catch (AmbariException | RuntimeException e) {
        throw conflict("DEPLOYMENT_TARGETS_INVALID", "A deployment target no longer belongs to this cluster.");
      }
    }).distinct().sorted(Comparator.comparing(Target::serviceName).thenComparing(Target::componentName)
        .thenComparing(Target::hostName)).toList();
  }

  private boolean targetsSatisfied(Cluster cluster, List<Target> targets, boolean start) {
    return targets.stream().allMatch(target -> targetSatisfied(cluster, target, start));
  }

  private boolean targetSatisfied(Cluster cluster, Target target, boolean start) {
    try {
      ServiceComponent component = cluster.getService(target.serviceName()).getServiceComponent(target.componentName());
      State state = component.getServiceComponentHost(target.hostName()).getState();
      return start && !component.isClientComponent() ? state == State.STARTED
          : state == State.INSTALLED || state == State.STARTED;
    } catch (AmbariException e) {
      throw new IllegalStateException("The exact deployment target is unavailable", e);
    }
  }

  private Map<String, Object> summary(Cluster cluster, ServiceDependencyDeploymentEntity value) {
    Progress progress = progress(value);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("deployment_id", value.getDeploymentId());
    result.put("cluster_id", value.getClusterId());
    result.put("state", value.getState());
    result.put("phase", progress.phase());
    result.put("request_id", progress.requestId());
    result.put("attempt_id", progress.attemptId());
    result.put("failure_code", progress.failureCode());
    result.put("targets", plan(value).targets());
    result.put("history", progress.history());
    List<Map<String, Object>> summaries = bindings.get().list(cluster.getClusterName(), "HBASE");
    result.put("bindings", summaries);
    result.put("retry_allowed", retryable(value, summaries)
        && Objects.equals(value.getOwnerUserId(), AuthorizationHelper.getAuthenticatedId())
        && canModify(cluster));
    result.put("completed", "COMPLETE".equals(value.getState()));
    result.put("install_only", "INSTALL_ONLY".equals(value.getState()));
    return result;
  }

  /** The same persisted phase and binding capabilities govern both presentation and mutation. */
  private boolean retryable(ServiceDependencyDeploymentEntity value, List<Map<String, Object>> summaries) {
    if (!"FAILED".equals(value.getState())) return false;
    Progress previous = progress(value);
    boolean reinstall = "INSTALL".equals(previous.phase()) && previous.requestId() != null;
    List<ServiceDependencyBindingEntity> current = dependencies.findByConsumer(value.getClusterId(), "HBASE");
    if (current.size() != plan(value).bindings().size()) return false;
    for (BindingVersion expected : plan(value).bindings()) {
      ServiceDependencyBindingEntity binding = current.stream()
          .filter(candidate -> expected.bindingId().equals(candidate.getBindingId())).findFirst().orElse(null);
      if (binding == null || !Objects.equals(binding.getDesiredSnapshotVersion(), expected.snapshotVersion())
          || !Set.of("PROVISIONING", "READY", "FAILED").contains(binding.getState())
          || binding.getProviderPreparationHash() == null) return false;
      if ("FAILED".equals(binding.getState()) && !reinstall) {
        Map<String, Object> summary = summaries.stream()
            .filter(candidate -> binding.getBindingId().equals(candidate.get("binding_id"))).findFirst().orElse(Map.of());
        if (!(summary.get("capabilities") instanceof Map<?, ?> capabilities)
            || !Boolean.TRUE.equals(capabilities.get("retry_allowed"))) return false;
      }
    }
    return true;
  }

  private Cluster cluster(String name, boolean modify) {
    try {
      Cluster cluster = clusters.getCluster(name);
      authorize(cluster, modify);
      return cluster;
    } catch (AmbariException e) {
      throw new ManagedDependencyIntegrationException(404, "DEPLOYMENT_NOT_FOUND", "The deployment is unavailable.");
    }
  }

  private void authorize(Cluster cluster, boolean modify) {
    if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
        RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE)) {
      throw new ManagedDependencyIntegrationException(403, "DEPLOYMENT_FORBIDDEN", "The deployment is not authorized.");
    }
    if (modify && !canModify(cluster)) {
      throw new ManagedDependencyIntegrationException(403, "DEPLOYMENT_FORBIDDEN", "The deployment mutation is not authorized.");
    }
  }

  private boolean canModify(Cluster cluster) {
    return Set.of(RoleAuthorization.SERVICE_START_STOP, RoleAuthorization.HOST_ADD_DELETE_COMPONENTS,
        RoleAuthorization.SERVICE_MODIFY_CONFIGS, RoleAuthorization.SERVICE_RUN_SERVICE_CHECK).stream()
        .allMatch(role -> AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), role));
  }

  private ServiceDependencyDeploymentEntity owned(Cluster cluster, UUID id) {
    ServiceDependencyDeploymentEntity value = deployments.find(id.toString());
    if (value == null || !Objects.equals(value.getClusterId(), cluster.getClusterId())) {
      throw new ManagedDependencyIntegrationException(404, "DEPLOYMENT_NOT_FOUND", "The deployment is unavailable.");
    }
    return value;
  }

  private void requireOwner(Cluster cluster, ServiceDependencyDeploymentEntity value) {
    if (!Objects.equals(value.getClusterId(), cluster.getClusterId())
        || !Objects.equals(value.getOwnerUserId(), AuthorizationHelper.getAuthenticatedId())) {
      throw new ManagedDependencyIntegrationException(404, "DEPLOYMENT_NOT_FOUND", "The deployment is unavailable.");
    }
  }

  private <T> T withParentLocks(Cluster consumer, Supplier<T> work) {
    Map<Long, Cluster> parents = new java.util.TreeMap<>();
    parents.put(consumer.getClusterId(), consumer);
    for (ServiceDependencyBindingEntity binding : dependencies.findByConsumer(consumer.getClusterId(), "HBASE")) {
      try {
        Cluster provider = clusters.getClusterById(binding.getProviderClusterId());
        parents.put(provider.getClusterId(), provider);
      } catch (AmbariException e) {
        throw new IllegalStateException("The deployment provider no longer exists", e);
      }
    }
    return lock(new ArrayList<>(parents.values()), 0, work);
  }

  private <T> T lock(List<Cluster> parents, int index, Supplier<T> work) {
    return index == parents.size() ? work.get()
        : parents.get(index).executeUnderWriteLockUntilTransactionCompletion(() -> lock(parents, index + 1, work));
  }

  private static Plan plan(ServiceDependencyDeploymentEntity value) {
    return StageUtils.getGson().fromJson(value.getPlanJson(), Plan.class);
  }

  private static Progress progress(ServiceDependencyDeploymentEntity value) {
    return StageUtils.getGson().fromJson(value.getProgressJson(), Progress.class);
  }

  private void fail(ServiceDependencyDeploymentEntity value, String code) {
    value.setState("FAILED");
    setFailure(value, code);
  }

  private void setFailure(ServiceDependencyDeploymentEntity value, String code) {
    Progress previous = progress(value);
    value.setProgressJson(StageUtils.getGson().toJson(new Progress(previous.attemptId(), previous.requestId(),
        previous.phase(), previous.history(), code)));
  }

  private static ManagedDependencyIntegrationException conflict(String code, String message) {
    return new ManagedDependencyIntegrationException(409, code, message);
  }

  public record Target(String serviceName, String componentName, String hostName, long hostId) { }
  private record BindingVersion(String bindingId, long snapshotVersion, long initialEpoch) { }
  private record Plan(List<Target> targets, List<BindingVersion> bindings, boolean installOnly) { }
  private record Attempt(String attemptId, String phase, Long requestId, List<Target> targets) { }
  private record Progress(String attemptId, Long requestId, String phase, List<Attempt> history, String failureCode) { }
}
