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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyHostResultEntityPK;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileReader;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** Owns the exact producer/consumer credential association, not deployment transitions. */
@Singleton
public class ManagedDependencyCredentialManager {
  private static final Logger LOG = LoggerFactory.getLogger(ManagedDependencyCredentialManager.class);
  private static final Map<String, String> KEYTAB_PROPERTIES = Map.of(
      "HBASE_MASTER", "hbase.master.keytab.file",
      "HBASE_REGIONSERVER", "hbase.regionserver.keytab.file",
      "HBASE_THRIFT", "hbase.thrift.keytab.file");
  private final ServiceDependencyDAO dao;
  private final Clusters clusters;
  private final Provider<ActionManager> actions;
  private final Provider<ManagedDependencyTaskResultProcessor> results;

  @Inject
  public ManagedDependencyCredentialManager(ServiceDependencyDAO dao, Clusters clusters,
      Provider<ActionManager> actions, Provider<ManagedDependencyTaskResultProcessor> results) {
    this.dao = dao;
    this.clusters = clusters;
    this.actions = actions;
    this.results = results;
  }

  /** Retry credential production runs as the persisted approver, with current permissions. */
  public void createRetryCredentials(org.apache.ambari.server.state.Cluster cluster,
      org.apache.ambari.server.controller.ExecuteActionRequest request,
      org.apache.ambari.server.controller.internal.RequestStageContainer stages,
      org.apache.ambari.server.controller.KerberosHelper kerberos,
      org.apache.ambari.server.security.authorization.Users users) throws AmbariException {
    ManagedDependencyCommand command = StageUtils.getGson().fromJson(
        request.getParameters().get(ManagedDependencyOperationDispatcher.COMMAND_PARAMETER),
        ManagedDependencyCommand.class);
    var binding = dao.findBinding(command.envelope().bindingId().toString());
    var owner = users.getUserEntity(binding.getUpdatedByUserId());
    if (owner == null || !Boolean.TRUE.equals(owner.getActive())) {
      throw new AmbariException("The credential operation owner is inactive");
    }
    var context = org.springframework.security.core.context.SecurityContextHolder.getContext();
    var saved = context.getAuthentication();
    try {
      context.setAuthentication(new org.apache.ambari.server.security.authentication.AmbariUserAuthentication(null,
          new org.apache.ambari.server.security.authentication.AmbariUserDetailsImpl(
              users.getUser(owner), null, users.getUserAuthorities(owner)), true));
      if (!org.apache.ambari.server.security.authorization.AuthorizationHelper.isAuthorized(
          org.apache.ambari.server.security.authorization.ResourceType.CLUSTER, cluster.getResourceId(),
          Set.of(org.apache.ambari.server.security.authorization.RoleAuthorization.SERVICE_SET_SERVICE_USERS_GROUPS))) {
        throw new AmbariException("The credential operation owner is no longer authorized");
      }
      Set<String> hosts = request.getResourceFilters().stream().flatMap(filter -> filter.getHostNames().stream())
          .collect(java.util.stream.Collectors.toSet());
      Set<String> components = cluster.getService("HBASE").getServiceComponents().values().stream()
          .filter(component -> component.getServiceComponentHosts().keySet().stream().anyMatch(hosts::contains))
          .map(ServiceComponent::getName).collect(java.util.stream.Collectors.toSet());
      kerberos.ensureIdentities(cluster, Map.of("HBASE", components), hosts, null, hosts, stages, null);
    } catch (org.apache.ambari.server.serveraction.kerberos.KerberosOperationException e) {
      throw new AmbariException("Could not create the scoped credential plan", e);
    } finally {
      context.setAuthentication(saved);
    }
  }

  /** Called in the action publication transaction after all task IDs exist. */
  public void associate(long requestId, List<HostRoleCommand> tasks) throws AmbariException {
    for (ServiceDependencyHostResultEntity preparation : dao.findPreparationsByRequest(requestId)) {
      if (!KEYTAB_PROPERTIES.containsKey(preparation.getComponentName())) {
        continue;
      }
      ManagedDependencySnapshot snapshot = StageUtils.getGson().fromJson(dao.findSnapshot(
          preparation.getBindingId(), preparation.getSnapshotVersion()).getSnapshotJson(),
          ManagedDependencySnapshot.class);
      if (snapshot.securityMode() != ManagedDependencySecurityMode.KERBEROS) {
        continue;
      }
      HostRoleCommand install = tasks.stream()
          .filter(task -> Objects.equals(preparation.getAmbariTaskId(), task.getTaskId()))
          .findFirst().orElseThrow(() -> new AmbariException("The credential preparation task is missing"));
      List<HostRoleCommand> credentials = tasks.stream()
          .filter(task -> task.getHostId() == install.getHostId() && isKeytabTask(task)).toList();
      if (credentials.size() > 1) {
        throw new AmbariException("A managed preparation requires one exact credential producer per host");
      }
      ExecutionCommand execution = install.getExecutionCommandWrapper().getExecutionCommand();
      var cluster = clusters.getClusterById(Long.parseLong(execution.getClusterId()));
      Map<String, String> hbase = cluster.getDesiredConfigByType("hbase-site").getProperties();
      String principal = snapshot.consumerIdentity().principalPatterns().first()
          .replace("_HOST", install.getHostName());
      List<Identity> identities = new ArrayList<>();
      for (ServiceComponent component : cluster.getService("HBASE").getServiceComponents().values()) {
        String property = KEYTAB_PROPERTIES.get(component.getName());
        if (property != null && component.getServiceComponentHosts().containsKey(install.getHostName())) {
          String path = hbase.get(property);
          if (path == null || !path.startsWith("/")) {
            throw new AmbariException("The approved consumer keytab path is missing");
          }
          identities.add(new Identity(principal, path));
        }
      }
      if (identities.isEmpty()) {
        throw new AmbariException("The managed credential identity set is empty");
      }
      boolean manual = credentials.isEmpty();
      if (manual && !"false".equalsIgnoreCase(cluster.getDesiredConfigByType("kerberos-env")
          .getProperties().get("manage_identities"))) {
        throw new AmbariException("The secure preparation has no scoped credential producer");
      }
      HostRoleCommand credential = manual ? null : credentials.get(0);
      ManagedDependencyCommand command = StageUtils.getGson().fromJson(
          preparation.getCommandJson(), ManagedDependencyCommand.class);
      Plan plan = new Plan(requestId, manual ? 0 : credential.getStageId(),
          manual ? 0 : credential.getTaskId(), command.parameters().get("identity.fingerprint"),
          canonical(identities), manual, false);
      dao.recordCredentialPlan(id(preparation), null, StageUtils.getGson().toJson(plan));
    }
  }

  /** Freeze the actual non-secret identity set before publishing keytab material. */
  public void beforeSend(ExecutionCommand execution, long hostId) {
    for (ServiceDependencyHostResultEntity preparation : dao.findPreparationsByRequest(StageUtils.getRequestStage(execution.getCommandId())[0])) {
      Plan plan = plan(preparation);
      if (plan == null || plan.manual() || plan.taskId() != execution.getTaskId()
          || !Objects.equals(preparation.getHostId(), hostId)) {
        continue;
      }
      List<Identity> sent = canonical(execution.getKerberosCommandParams().stream()
          .map(value -> new Identity(value.get(KerberosIdentityDataFileReader.PRINCIPAL),
              value.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH))).toList());
      if (!sent.containsAll(plan.identities())) {
        throw new IllegalStateException("The credential producer omitted an approved consumer identity");
      }
      Plan transmitted = new Plan(plan.requestId(), plan.stageId(), plan.taskId(),
          plan.identityFingerprint(), plan.identities(), false, true);
      dao.recordCredentialPlan(id(preparation), preparation.getCredentialPlanJson(),
          StageUtils.getGson().toJson(transmitted));
    }
  }

  /** Called for the real SET_KEYTAB result and again during bounded recovery. */
  public void taskCompleted(HostRoleCommand task) {
    if (!isKeytabTask(task)) {
      return;
    }
    for (ServiceDependencyHostResultEntity preparation : dao.findPreparationsByRequest(task.getRequestId())) {
      Plan plan = plan(preparation);
      if (plan != null && plan.taskId() == task.getTaskId()) {
        recover(preparation);
      }
    }
  }

  public void recover(ServiceDependencyHostResultEntity preparation) {
    Plan plan = plan(preparation);
    if (plan == null || plan.manual()) {
      return;
    }
    try {
      HostRoleCommand task = actions.get().getTaskById(plan.taskId());
      if (task != null && task.getStatus().isCompletedState()
          && task.getStatus() != HostRoleStatus.COMPLETED) {
        dao.failCredentials(id(preparation));
        return;
      }
      if (task == null || !task.getStatus().isCompletedState() || !"SUCCEEDED".equals(preparation.getState())) return;
      try {
        requireCompleted(preparation, plan.requestId(), Set.of(plan.taskId()));
      } catch (AmbariException | RuntimeException invalidEvidence) {
        dao.failCredentials(id(preparation));
        return;
      }
      results.get().dispatchVerificationAfterCredentials(preparation.getBindingId(),
          preparation.getOperationEpoch(), preparation.getHostId(), plan.requestId(), Set.of(plan.taskId()));
    } catch (AmbariException | RuntimeException e) {
      LOG.debug("Credential reconciliation deferred: binding={}, host={}, reason={}",
          preparation.getBindingId(), preparation.getHostId(), e.getClass().getSimpleName());
    }
  }

  public void requireCompleted(ServiceDependencyHostResultEntity preparation,
      long requestId, Set<Long> taskIds) throws AmbariException {
    Plan plan = plan(preparation);
    if (plan == null || plan.manual() || !plan.transmitted() || plan.requestId() != requestId
        || !Set.of(plan.taskId()).equals(taskIds)) {
      throw new AmbariException("The credential callback does not own the persisted producer plan");
    }
    ManagedDependencyCommand command = StageUtils.getGson().fromJson(
        preparation.getCommandJson(), ManagedDependencyCommand.class);
    if (!Objects.equals(plan.identityFingerprint(), command.parameters().get("identity.fingerprint"))) {
      throw new AmbariException("The credential identity fingerprint is stale");
    }
    HostRoleCommand task = actions.get().getTaskById(plan.taskId());
    if (task == null || !isKeytabTask(task) || task.getStatus() != HostRoleStatus.COMPLETED
        || task.getRequestId() != plan.requestId() || task.getStageId() != plan.stageId()
        || !Objects.equals(preparation.getHostId(), task.getHostId())) {
      throw new AmbariException("The exact credential task has not completed");
    }
    JsonObject output = JsonParser.parseString(task.getStructuredOut()).getAsJsonObject();
    List<Identity> observed = new ArrayList<>();
    if (output.has("keytabIdentities")) {
      output.getAsJsonArray("keytabIdentities").forEach(value -> {
        JsonObject identity = value.getAsJsonObject();
        observed.add(new Identity(identity.get("principal").getAsString(), identity.get("keytab").getAsString()));
      });
    } else if (output.has("keytabs")) {
      // Older agents report a map; it proves only the identities actually present.
      output.getAsJsonObject("keytabs").entrySet().forEach(value ->
          observed.add(new Identity(value.getKey(), value.getValue().getAsString())));
    }
    if (!observed.containsAll(plan.identities())) {
      throw new AmbariException("The credential result does not prove the complete approved identity set");
    }
  }

  public static boolean isKeytabTask(HostRoleCommand task) {
    return task.getRole() == Role.KERBEROS_CLIENT && task.getRoleCommand() == RoleCommand.CUSTOM_COMMAND
        && "SET_KEYTAB".equals(task.getExecutionCommandWrapper().getExecutionCommand()
            .getCommandParams().get("custom_command"));
  }

  public static Plan plan(ServiceDependencyHostResultEntity preparation) {
    return preparation.getCredentialPlanJson() == null ? null : StageUtils.getGson().fromJson(
        preparation.getCredentialPlanJson(), Plan.class);
  }

  private static List<Identity> canonical(List<Identity> identities) {
    return identities.stream().distinct().sorted(Comparator.comparing(Identity::principal)
        .thenComparing(Identity::keytab)).toList();
  }

  private static ServiceDependencyHostResultEntityPK id(ServiceDependencyHostResultEntity value) {
    return new ServiceDependencyHostResultEntityPK(value.getBindingId(), value.getSnapshotVersion(),
        value.getOperationEpoch(), value.getHostId(), value.getDependencyType(), value.getCheckKind());
  }

  public record Identity(String principal, String keytab) { }
  public record Plan(long requestId, long stageId, long taskId, String identityFingerprint,
      List<Identity> identities, boolean manual, boolean transmitted) { }
}
