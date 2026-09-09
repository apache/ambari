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

package org.apache.ambari.server.api.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.KeyValueDAO;
import org.apache.ambari.server.orm.dao.ScopedWorkflowStateDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.KeyValueEntity;
import org.apache.ambari.server.orm.entities.ScopedWorkflowStateEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ClusterCreationContext;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.StageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.gson.JsonParseException;

@Singleton
public class PersistKeyValueImpl {

  static final String SCOPED_KEY_PREFIX = "ambari.internal.workflow:";

  public static String creationDraftStorageKey(int ownerUserId, UUID draftId) {
    if (ownerUserId <= 0) {
      throw new IllegalArgumentException("A positive draft owner user ID is required");
    }
    return SCOPED_KEY_PREFIX + "drafts:" + ownerUserId + ":" +
        java.util.Objects.requireNonNull(draftId, "draftId");
  }
  private static final int MAX_DRAFT_SUMMARIES = 100;
  private static final int MAX_PHASE_LENGTH = 128;
  static final int MAX_SCOPED_REQUEST_BYTES = 2 * 1024 * 1024;
  private static final int MAX_VALUE_DEPTH = 20;
  private static final int MAX_VALUE_NODES = 100000;
  private static final Pattern SENSITIVE_KEY = Pattern.compile(
      ".*(password|secret|private.?key|ssh.?key|credential|token|cookie|keytab).*", Pattern.CASE_INSENSITIVE);
  private static final Pattern OPAQUE_SENSITIVE_VALUE = Pattern.compile(
      ".*(password|secret|private.?key|ssh.?key|credential|token|cookie|keytab)[\\s\\\"'\\\\]*[:=].*",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern PRIVATE_KEY_VALUE = Pattern.compile(
      ".*-----BEGIN(?: [A-Z0-9]+)* PRIVATE KEY-----.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Set<String> VALUE_FIELDS = Set.of(
      "value", "property_value", "propertyvalue", "savedvalue", "recommendedvalue",
      "initialvalue", "defaultvalue", "currentvalue", "previousvalue", "priorvalue",
      "originalvalue", "uservalue", "changedvalue", "newvalue", "oldvalue",
      "default_value", "initial_value", "previous_value", "recommended_value",
      "new_value", "old_value", "confirmpassword", "confirm_password");
  private static final Set<String> REPOSITORY_URL_FIELDS = Set.of(
      "baseurl", "defaulturl", "version_url", "base_url", "default_base_url");
  private static final Set<String> LEGACY_USER_PREFERENCE_SUFFIXES = Set.of("supports", "dashboard");
  private static final Map<String, RoleAuthorization> CLUSTER_WORKFLOW_AUTHORIZATIONS;
  private static final Map<String, RoleAuthorization> LEGACY_CLUSTER_KEYS;
  private static final Set<String> AMBIGUOUS_LEGACY_KEYS = Set.of(
      "USER_REDIRECTION_URL", "CLUSTER_CURRENT", "CLUSTER_CURRENT_STATUS");

  static {
    Map<String, RoleAuthorization> authorizations = new LinkedHashMap<>();
    authorizations.put("ADD_SERVICE", RoleAuthorization.SERVICE_ADD_DELETE_SERVICES);
    authorizations.put("ADD_HOST", RoleAuthorization.HOST_ADD_DELETE_HOSTS);
    authorizations.put("REASSIGN_COMPONENT", RoleAuthorization.SERVICE_MOVE);
    authorizations.put("ENABLING_KERBEROS", RoleAuthorization.CLUSTER_TOGGLE_KERBEROS);
    authorizations.put("HIGH_AVAILIBILITY_NAMENODE", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("ROLLBACK_HIGH_AVAILABILITY", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("HIGH_AVAILIBILITY_RM_HA", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("HIGH_AVAILIBILITY_RANGER_HA", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("MANAGE_JOURNALNODES", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("OBSERVER_NAMENODE", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("NAMENODE_FEDERATION", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("ROUTER_FEDERATION", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("ADD_HAWQ_STANDBY", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("REMOVE_HAWQ_STANDBY", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("ACTIVATE_HAWQ_STANDBY", RoleAuthorization.SERVICE_ENABLE_HA);
    authorizations.put("UPGRADE", RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK);
    CLUSTER_WORKFLOW_AUTHORIZATIONS = Collections.unmodifiableMap(authorizations);

    Map<String, RoleAuthorization> legacyKeys = new HashMap<>();
    legacyKeys.put("ADD_SERVICE", RoleAuthorization.SERVICE_ADD_DELETE_SERVICES);
    legacyKeys.put("ADD_HOST", RoleAuthorization.HOST_ADD_DELETE_HOSTS);
    legacyKeys.put("REASSIGN_COMPONENT", RoleAuthorization.SERVICE_MOVE);
    legacyKeys.put("ENABLING_KERBEROS", RoleAuthorization.CLUSTER_TOGGLE_KERBEROS);
    legacyKeys.put("HIGH_AVAILIBILITY_NAMENODE", RoleAuthorization.SERVICE_ENABLE_HA);
    legacyKeys.put("HIGH_AVAILIBILITY_RM_HA", RoleAuthorization.SERVICE_ENABLE_HA);
    legacyKeys.put("HIGH_AVAILIBILITY_RANGER_HA", RoleAuthorization.SERVICE_ENABLE_HA);
    legacyKeys.put("MANAGE_JOURNALNODES", RoleAuthorization.SERVICE_ENABLE_HA);
    legacyKeys.put("OBSERVER_NAMENODE", RoleAuthorization.SERVICE_ENABLE_HA);
    legacyKeys.put("NAMENODE_FEDERATION", RoleAuthorization.SERVICE_ENABLE_HA);
    legacyKeys.put("ROUTER_FEDERATION", RoleAuthorization.SERVICE_ENABLE_HA);
    legacyKeys.put("ADD_HAWQ_STANDBY", RoleAuthorization.SERVICE_ENABLE_HA);
    legacyKeys.put("REMOVE_HAWQ_STANDBY", RoleAuthorization.SERVICE_ENABLE_HA);
    legacyKeys.put("ACTIVATE_HAWQ_STANDBY", RoleAuthorization.SERVICE_ENABLE_HA);
    legacyKeys.put("isPatchUpgrade", RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK);
    legacyKeys.put("upgradeIsFinalizeItem", RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK);
    legacyKeys.put("upgradeVersionDisplayName", RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK);
    legacyKeys.put("versionOperations", RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK);
    legacyKeys.put("CLUSTER_STATE", null);
    legacyKeys.put("wizard-data", null);
    LEGACY_CLUSTER_KEYS = Collections.unmodifiableMap(legacyKeys);
  }

  @Inject
  KeyValueDAO keyValueDAO;

  @Inject
  ScopedWorkflowStateDAO scopedWorkflowStateDAO;

  @Inject
  ClusterDAO clusterDAO;

  @Inject
  UserDAO userDAO;

  @Inject
  Clusters clusters;

  public String generateKey() {
    return UUID.randomUUID().toString();
  }

  public Collection<String> generateKeys(int number) {
    List<String> keys = new ArrayList<>(number);
    for (int i = 0; i < number; i++) {
      keys.add(generateKey());
    }
    return keys;
  }

  public synchronized String getValue(String key) {
    rejectReservedKey(key);
    KeyValueEntity keyValueEntity = keyValueDAO.findByKey(key);
    if (keyValueEntity != null) {
      return keyValueEntity.getValue();
    }
    throw new WebApplicationException(Response.Status.NOT_FOUND);
  }

  public synchronized String put(String value) {
    String key = generateKey();
    put(key, value);
    return key;
  }

  public synchronized void put(String key, String value) {
    rejectReservedKey(key);
    KeyValueEntity keyValueEntity = keyValueDAO.findByKey(key);
    if (keyValueEntity != null) {
      keyValueEntity.setValue(value);
      keyValueDAO.merge(keyValueEntity);
    } else {
      keyValueEntity = new KeyValueEntity();
      keyValueEntity.setKey(key);
      keyValueEntity.setValue(value);
      keyValueDAO.create(keyValueEntity);
    }
  }
  
  public synchronized Map<String, String> getAllKeyValues() {
    Map<String, String> map = new HashMap<>();
    for (KeyValueEntity keyValueEntity : keyValueDAO.findAll()) {
      if (!isReservedKey(keyValueEntity.getKey())) {
        map.put(keyValueEntity.getKey(), keyValueEntity.getValue());
      }
    }
    return map;
  }

  public String getLegacyValue(String key) throws AuthorizationException {
    authorizeLegacyKey(key, false);
    return getValue(key);
  }

  public void putLegacyValues(Map<String, String> values) throws AuthorizationException {
    for (String key : values.keySet()) {
      authorizeLegacyKey(key, true);
    }
    for (Map.Entry<String, String> entry : values.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public Collection<String> putLegacyGeneratedValues(Collection<String> values) throws AuthorizationException {
    AuthorizationHelper.verifyAuthorization(ResourceType.AMBARI, null,
        EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION));
    Collection<String> keys = new ArrayList<>(values.size());
    for (String value : values) {
      keys.add(put(value));
    }
    return keys;
  }

  public Map<String, String> getAllLegacyKeyValues() {
    Map<String, String> visible = new HashMap<>();
    for (Map.Entry<String, String> entry : getAllKeyValues().entrySet()) {
      if (isLegacyKeyAuthorized(entry.getKey(), false)) {
        visible.put(entry.getKey(), entry.getValue());
      }
    }
    return visible;
  }

  public ScopedWorkflowState getScopedState(String scopeType, String scopeId, boolean summary)
      throws AuthorizationException {
    AuthenticatedUser user = authenticatedUser();
    ScopeKey scope = resolveScope(scopeType, scopeId, user);
    authorizeRead(scope, summary);
    return toState(scopedWorkflowStateDAO.findByKey(scope.storageKey));
  }

  /**
   * Returns the authenticated user's exact active cluster workflow checkpoint.
   * This is used for side-effect-free planning that must not adopt another
   * administrator's in-progress workflow.
   */
  public ScopedWorkflowState getActiveOwnedClusterWorkflowState(long clusterId,
      String workflow, long expectedRevision) throws AuthorizationException {
    AuthenticatedUser user = authenticatedUser();
    ScopeKey scope = resolveScope("clusters", Long.toString(clusterId), user);
    authorizeRead(scope, false);
    ScopedWorkflowStateEntity entity = scopedWorkflowStateDAO.findByKey(scope.storageKey);
    if (entity == null || !user.id.equals(entity.getOwnerUserId())
        || !workflow.equals(entity.getWorkflow())
        || entity.getRevision() == null || entity.getRevision() != expectedRevision) {
      throw conflict("WORKFLOW_VERSION_CONFLICT",
          "Reload the active workflow checkpoint before previewing dependencies");
    }
    return toState(entity);
  }

  public ScopedWorkflowState putScopedState(String scopeType, String scopeId, ScopedWorkflowUpdate update)
      throws AuthorizationException {
    AuthenticatedUser user = authenticatedUser();
    ScopeKey scope = resolveScope(scopeType, scopeId, user);
    authorizeUpdateScope(scope);
    validateUpdate(scope, update);
    authorizeWorkflowUpdate(scope, update.getWorkflow());
    update.setValues(sanitizeValues(update.getValues(), 0, new int[] {0}, false));

    try {
      return updateScopedState(scope.storageKey, user, update);
    } catch (PersistenceException e) {
      // A missing-row race is complete and rolled back when the DAO proxy returns.
      // Retry against the winning row so optimistic concurrency reports a stable 409.
      if (update.getExpectedRevision() == 0 && scopedWorkflowStateDAO.findByKey(scope.storageKey) != null) {
        return updateScopedState(scope.storageKey, user, update);
      }
      throw e;
    }
  }

  public ClusterCreationContext validateClusterCreationDraft(String scopeId)
      throws AuthorizationException {
    AuthenticatedUser user = authenticatedUser();
    ScopeKey scope = resolveScope("drafts", scopeId, user);
    authorizeRead(scope, true);
    ScopedWorkflowStateEntity draft = scopedWorkflowStateDAO.findByKey(scope.storageKey);
    if (draft == null) {
      throw conflict("INVALID_CREATION_DRAFT",
          "The creation draft is not active for the authenticated user");
    }
    String draftId = canonicalDraftId(scopeId);
    if (draft.getCreatedClusterId() != null
        && clusterForDraftAssociation(draft, user.id, draftId) == null) {
      throw conflict("CREATION_DRAFT_CONSUMED",
          "The creation draft was already consumed and cannot create another cluster");
    }
    if (!user.id.equals(draft.getOwnerUserId())
        || !"CLUSTER_CREATE".equals(draft.getWorkflow())) {
      throw conflict("INVALID_CREATION_DRAFT",
          "The creation draft is not active for the authenticated user");
    }
    return new ClusterCreationContext(user.id, draftId, scope.storageKey);
  }

  public Map<String, Object> getCreationDraftCluster(String scopeId)
      throws AuthorizationException {
    AuthenticatedUser user = authenticatedUser();
    ScopeKey scope = resolveScope("drafts", scopeId, user);
    authorizeRead(scope, true);
    ScopedWorkflowStateEntity draft = scopedWorkflowStateDAO.findByKey(scope.storageKey);
    if (draft == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    ClusterEntity cluster = clusterForDraftAssociation(draft, user.id, canonicalDraftId(scopeId));
    if (cluster == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("cluster_id", cluster.getClusterId());
    response.put("cluster_name", cluster.getClusterName());
    return response;
  }

  public Map<String, Object> getCreationDraftDirectory() throws AuthorizationException {
    AuthenticatedUser user = authenticatedUser();
    AuthorizationHelper.verifyAuthorization(ResourceType.AMBARI, null,
        EnumSet.of(RoleAuthorization.AMBARI_ADD_DELETE_CLUSTERS));
    String prefix = SCOPED_KEY_PREFIX + "drafts:" + user.id + ":";
    Map<Long, ClusterEntity> clustersById = new HashMap<>();
    for (ClusterEntity cluster : clusterDAO.findByCreatorUserId(user.id)) {
      clustersById.put(cluster.getClusterId(), cluster);
    }
    List<Map<String, Object>> items = new ArrayList<>();
    for (ScopedWorkflowStateEntity draft : scopedWorkflowStateDAO.findActiveCreationDrafts(
        user.id, prefix, MAX_DRAFT_SUMMARIES)) {
      String draftId = draft.getScopeKey().substring(prefix.length());
      ClusterEntity cluster = draft.getCreatedClusterId() == null
          ? null : clustersById.get(draft.getCreatedClusterId());
      if (cluster != null && !draftId.equals(cluster.getCreationDraftId())) {
        cluster = null;
      }
      items.add(new ScopedWorkflowDraftSummary(draftId, draft.getRevision(), draft.getWorkflow(),
          draft.getPhase(), cluster == null ? null : cluster.getClusterId(),
          cluster == null ? null : cluster.getClusterName()).toResponse());
    }
    return Map.of("items", items);
  }

  static boolean isReservedKey(String key) {
    return key != null && key.startsWith(SCOPED_KEY_PREFIX);
  }

  private void authorizeLegacyKey(String key, boolean write) throws AuthorizationException {
    rejectReservedKey(key);
    if (AMBIGUOUS_LEGACY_KEYS.contains(key)
        || (LEGACY_CLUSTER_KEYS.containsKey(key) && clusters.getClusters().size() != 1)) {
      throw conflict("LEGACY_PERSIST_SCOPE_REQUIRED",
          "This legacy key has no unambiguous user and cluster scope");
    }
    if (!isLegacyKeyAuthorized(key, write)) {
      throw new AuthorizationException();
    }
  }

  private boolean isLegacyKeyAuthorized(String key, boolean write) {
    AuthenticatedUser user = authenticatedUserOrNull();
    if (user == null) {
      return false;
    }
    if (AMBIGUOUS_LEGACY_KEYS.contains(key)) {
      return false;
    }

    if (isUserPreferenceKey(key, user.username)) {
      return AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, null,
          RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA);
    }

    if (LEGACY_CLUSTER_KEYS.containsKey(key)) {
      Map<String, Cluster> existingClusters = clusters.getClusters();
      if (existingClusters.size() != 1) {
        return false;
      }
      Cluster cluster = existingClusters.values().iterator().next();
      Long resourceId = cluster.getResourceId();
      if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, resourceId,
          RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA)) {
        return false;
      }
      if (!write && !AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, resourceId,
          RoleAuthorization.CLUSTER_VIEW_CONFIGS)) {
        return false;
      }
      RoleAuthorization workflowAuthorization = LEGACY_CLUSTER_KEYS.get(key);
      return workflowAuthorization == null || AuthorizationHelper.isAuthorized(
          ResourceType.CLUSTER, resourceId, workflowAuthorization);
    }

    return AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null,
        RoleAuthorization.AMBARI_MANAGE_CONFIGURATION);
  }

  private boolean isUserPreferenceKey(String key, String username) {
    String normalizedUser = username.toLowerCase(Locale.ROOT);
    String normalizedKey = key == null ? "" : key.toLowerCase(Locale.ROOT);
    if (normalizedKey.equals("admin-settings-show-bg-" + normalizedUser)
        || normalizedKey.equals("admin-settings-timezone-" + normalizedUser)) {
      return true;
    }
    for (String suffix : LEGACY_USER_PREFERENCE_SUFFIXES) {
      if (normalizedKey.equals("user-pref-" + normalizedUser + "-" + suffix)) {
        return true;
      }
    }
    return false;
  }

  private ScopedWorkflowState updateScopedState(String storageKey, AuthenticatedUser user,
      ScopedWorkflowUpdate update) {
    ScopedWorkflowStateEntity value = scopedWorkflowStateDAO.updateWithLock(storageKey, entity -> {
      ScopedWorkflowState current = toState(entity);
      if (current.getRevision() != update.getExpectedRevision()) {
        throw conflict("WORKFLOW_VERSION_CONFLICT", String.format(
            "Expected workflow revision %d but current revision is %d",
            update.getExpectedRevision(), current.getRevision()));
      }
      if (entity.getOwnerUserId() != null && !entity.getOwnerUserId().equals(user.id)) {
        throw conflict("WORKFLOW_OWNED", "The workflow is active for another user");
      }
      if (!"IDLE".equals(current.getWorkflow()) && !"IDLE".equals(update.getWorkflow())
          && !current.getWorkflow().equals(update.getWorkflow())) {
        throw conflict("WORKFLOW_ACTIVE",
            "Release the active workflow before starting a different workflow type");
      }
      if (storageKey.startsWith(SCOPED_KEY_PREFIX + "drafts:")
          && "IDLE".equals(current.getWorkflow()) && current.getRevision() > 0
          && "CLUSTER_CREATE".equals(update.getWorkflow())) {
        throw conflict("CREATION_DRAFT_RETIRED",
            "A released creation draft cannot be reused; start with a new draft ID");
      }

      boolean release = "IDLE".equals(update.getWorkflow());
      Map<String, Object> values = release ? Collections.emptyMap() : update.getValues();
      String payload = StageUtils.getGson().toJson(values);
      if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_SCOPED_REQUEST_BYTES) {
        throw badRequest("WORKFLOW_STATE_TOO_LARGE",
            "The workflow state exceeds the maximum persisted size");
      }
      entity.setRevision(current.getRevision() + 1);
      entity.setOwnerUserId(release ? null : user.id);
      entity.setOwnerName(release ? null : user.username);
      entity.setWorkflow(release ? "IDLE" : update.getWorkflow());
      entity.setPhase(release ? "IDLE" : update.getPhase());
      entity.setPayload(payload);
      return entity;
    });
    return toState(value);
  }

  private ScopeKey resolveScope(String scopeType, String scopeId, AuthenticatedUser user)
      throws AuthorizationException {
    if ("clusters".equals(scopeType)) {
      long clusterId;
      try {
        clusterId = Long.parseLong(scopeId);
      } catch (NumberFormatException e) {
        throw badRequest("INVALID_WORKFLOW_SCOPE", "Cluster scope ID must be numeric");
      }
      if (clusterId <= 0) {
        throw badRequest("INVALID_WORKFLOW_SCOPE", "Cluster scope ID must be positive");
      }

      Cluster cluster;
      try {
        cluster = clusters.getClusterById(clusterId);
      } catch (ClusterNotFoundException e) {
        if (AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null,
            RoleAuthorization.AMBARI_ADD_DELETE_CLUSTERS)) {
          throw badRequest("INVALID_WORKFLOW_SCOPE", "Cluster scope does not exist");
        }
        throw new AuthorizationException();
      } catch (AmbariException e) {
        throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
      }
      return ScopeKey.cluster(cluster, SCOPED_KEY_PREFIX + "clusters:" + clusterId);
    }

    if ("drafts".equals(scopeType)) {
      UUID draftId;
      try {
        draftId = UUID.fromString(scopeId);
      } catch (IllegalArgumentException e) {
        throw badRequest("INVALID_WORKFLOW_SCOPE", "Draft scope ID must be a UUID");
      }
      String storageKey = creationDraftStorageKey(user.id, draftId);
      return ScopeKey.draft(storageKey);
    }

    throw badRequest("INVALID_WORKFLOW_SCOPE", "Scope type must be clusters or drafts");
  }

  private String canonicalDraftId(String scopeId) {
    try {
      return UUID.fromString(scopeId).toString();
    } catch (IllegalArgumentException e) {
      throw badRequest("INVALID_WORKFLOW_SCOPE", "Draft scope ID must be a UUID");
    }
  }

  private void authorizeRead(ScopeKey scope, boolean summary) throws AuthorizationException {
    if (scope.cluster != null) {
      if (summary) {
        AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, scope.cluster.getResourceId(),
            RoleAuthorization.AUTHORIZATIONS_VIEW_CLUSTER);
      } else {
        AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, scope.cluster.getResourceId(),
            EnumSet.of(RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA));
        AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, scope.cluster.getResourceId(),
            EnumSet.of(RoleAuthorization.CLUSTER_VIEW_CONFIGS));
      }
    } else {
      AuthorizationHelper.verifyAuthorization(ResourceType.AMBARI, null,
          EnumSet.of(RoleAuthorization.AMBARI_ADD_DELETE_CLUSTERS));
    }
  }

  private void authorizeUpdateScope(ScopeKey scope) throws AuthorizationException {
    if (scope.cluster == null) {
      AuthorizationHelper.verifyAuthorization(ResourceType.AMBARI, null,
          EnumSet.of(RoleAuthorization.AMBARI_ADD_DELETE_CLUSTERS));
      return;
    }

    AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, scope.cluster.getResourceId(),
        EnumSet.of(RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA));
  }

  private void authorizeWorkflowUpdate(ScopeKey scope, String workflow) throws AuthorizationException {
    if (scope.cluster != null && !"IDLE".equals(workflow)) {
      AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, scope.cluster.getResourceId(),
          EnumSet.of(CLUSTER_WORKFLOW_AUTHORIZATIONS.get(workflow)));
    }
  }

  private void validateUpdate(ScopeKey scope, ScopedWorkflowUpdate update) {
    if (update == null || update.getExpectedRevision() == null || update.getExpectedRevision() < 0) {
      throw badRequest("INVALID_WORKFLOW_STATE", "expected_revision must be a non-negative number");
    }
    String workflow = update.getWorkflow();
    boolean allowedWorkflow = "IDLE".equals(workflow)
        || (scope.cluster == null && "CLUSTER_CREATE".equals(workflow))
        || (scope.cluster != null && CLUSTER_WORKFLOW_AUTHORIZATIONS.containsKey(workflow));
    if (!allowedWorkflow) {
      throw badRequest("INVALID_WORKFLOW_STATE", "Unsupported workflow type");
    }
    if (!"IDLE".equals(workflow)
        && (update.getPhase() == null || update.getPhase().isEmpty()
            || update.getPhase().length() > MAX_PHASE_LENGTH)) {
      throw badRequest("INVALID_WORKFLOW_STATE", "phase must contain 1 to 128 characters");
    }
    if (update.getValues() == null) {
      update.setValues(Collections.emptyMap());
    }
  }

  private ClusterEntity clusterForDraftAssociation(ScopedWorkflowStateEntity draft,
      int creatorUserId, String draftId) {
    if (draft == null || draft.getCreatedClusterId() == null) {
      return null;
    }
    ClusterEntity cluster = clusterDAO.findById(draft.getCreatedClusterId());
    if (cluster == null || !Integer.valueOf(creatorUserId).equals(cluster.getCreatorUserId())
        || !draftId.equals(cluster.getCreationDraftId())) {
      return null;
    }
    return cluster;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> sanitizeValues(Map<String, Object> values, int depth, int[] nodes,
      boolean sensitiveContext) {
    Object sanitized = sanitizeValue(values, depth, nodes, sensitiveContext);
    return sanitized == null ? new LinkedHashMap<>() : (Map<String, Object>) sanitized;
  }

  private Object sanitizeValue(Object value, int depth, int[] nodes, boolean sensitiveContext) {
    if (depth > MAX_VALUE_DEPTH || ++nodes[0] > MAX_VALUE_NODES) {
      throw badRequest("INVALID_WORKFLOW_STATE", "Workflow values exceed structural limits");
    }
    if (value instanceof Map) {
      Map<?, ?> source = (Map<?, ?>) value;
      boolean mapSensitive = sensitiveContext || isSensitiveProperty(source);
      Map<String, Object> sanitized = new LinkedHashMap<>();
      boolean redacted = false;
      for (Map.Entry<?, ?> entry : source.entrySet()) {
        String key = String.valueOf(entry.getKey());
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        boolean sensitiveKey = SENSITIVE_KEY.matcher(key).matches();
        if ("versiondefinitionsource".equals(normalizedKey)) {
          sanitized.put(key, sanitizeVersionDefinitionSource(entry.getValue(), depth + 1, nodes));
          continue;
        }
        if ((mapSensitive && VALUE_FIELDS.contains(normalizedKey)
                && !isSafePathMetadata(source, entry.getValue()))
            || (sensitiveKey && !(entry.getValue() instanceof Map))
            || (REPOSITORY_URL_FIELDS.contains(normalizedKey)
                && containsUrlUserInfo(entry.getValue()))) {
          redacted = true;
          continue;
        }
        Object sanitizedValue = sanitizeValue(entry.getValue(), depth + 1, nodes,
            sensitiveKey);
        if (sanitizedValue != null) {
          sanitized.put(key, sanitizedValue);
        }
      }
      if (redacted) {
        sanitized.put("requires_reentry", true);
      }
      return sanitized;
    } else if (value instanceof Collection) {
      if (sensitiveContext) {
        return Collections.emptyList();
      }
      List<Object> sanitized = new ArrayList<>();
      for (Object item : (Collection<?>) value) {
        Object sanitizedItem = sanitizeValue(item, depth + 1, nodes, false);
        if (sanitizedItem != null) {
          sanitized.add(sanitizedItem);
        }
      }
      return sanitized;
    } else if (value instanceof String && isOpaqueCredentialValue((String) value)) {
      throw badRequest("SENSITIVE_WORKFLOW_STATE",
          "Opaque credential-bearing values cannot be persisted in workflow state");
    } else if (sensitiveContext) {
      return null;
    }
    return value;
  }

  private Map<String, Object> sanitizeVersionDefinitionSource(Object value, int depth, int[] nodes) {
    if (depth > MAX_VALUE_DEPTH || ++nodes[0] > MAX_VALUE_NODES) {
      throw badRequest("INVALID_WORKFLOW_STATE", "Workflow values exceed structural limits");
    }
    Map<String, Object> sanitized = new LinkedHashMap<>();
    if (!(value instanceof Map)) {
      sanitized.put("requires_reentry", true);
      return sanitized;
    }

    Map<?, ?> source = (Map<?, ?>) value;
    String type = source.get("type") instanceof String
        ? ((String) source.get("type")).toLowerCase(Locale.ROOT) : null;
    if (type != null) {
      sanitized.put("type", type);
    }
    if (!"url".equals(type)) {
      sanitized.put("requires_reentry", true);
      return sanitized;
    }

    Object payload = source.get("payload");
    if (payload instanceof Map) {
      Object sanitizedPayload = sanitizeValue(payload, depth + 1, nodes, false);
      sanitized.put("payload", sanitizedPayload);
      if (containsReentryMarker(sanitizedPayload)) {
        sanitized.put("requires_reentry", true);
      }
    } else {
      sanitized.put("requires_reentry", true);
    }
    if (source.containsKey("headers")) {
      sanitized.put("requires_reentry", true);
    }
    return sanitized;
  }

  private boolean containsReentryMarker(Object value) {
    if (value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) value;
      if (Boolean.TRUE.equals(map.get("requires_reentry"))) {
        return true;
      }
      for (Object child : map.values()) {
        if (containsReentryMarker(child)) {
          return true;
        }
      }
    } else if (value instanceof Collection) {
      for (Object child : (Collection<?>) value) {
        if (containsReentryMarker(child)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isSensitiveProperty(Map<?, ?> value) {
    for (String nameKey : List.of("name", "property_name", "propertyName", "key")) {
      Object name = value.get(nameKey);
      if (name != null && SENSITIVE_KEY.matcher(String.valueOf(name)).matches()) {
        return true;
      }
    }
    for (String attributesKey : List.of("propertyAttributes", "property_attributes",
        "property_value_attributes")) {
      Object attributes = value.get(attributesKey);
      if (attributes instanceof Map) {
        Object type = ((Map<?, ?>) attributes).get("type");
        if (type != null && "password".equalsIgnoreCase(String.valueOf(type))) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isSafePathMetadata(Map<?, ?> property, Object value) {
    if (!(value instanceof String) || !((String) value).startsWith("/")) {
      return false;
    }
    String propertyName = null;
    for (String nameKey : List.of("name", "property_name", "propertyName", "key")) {
      if (property.get(nameKey) != null) {
        propertyName = String.valueOf(property.get(nameKey)).toLowerCase(Locale.ROOT);
        break;
      }
    }
    return propertyName != null
        && (propertyName.contains("keytab") || propertyName.contains("principal"))
        && !((String) value).contains("\n") && !((String) value).contains("\u0000");
  }

  private boolean containsUrlUserInfo(Object value) {
    if (!(value instanceof String)) {
      return false;
    }
    try {
      return new URI((String) value).getRawUserInfo() != null;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  private boolean isOpaqueCredentialValue(String value) {
    return OPAQUE_SENSITIVE_VALUE.matcher(value).matches()
        || PRIVATE_KEY_VALUE.matcher(value).matches();
  }

  @SuppressWarnings("unchecked")
  private ScopedWorkflowState toState(ScopedWorkflowStateEntity entity) {
    if (entity == null || entity.getRevision() == null || entity.getRevision() == 0) {
      return ScopedWorkflowState.empty();
    }
    try {
      if (entity.getRevision() < 0 || entity.getWorkflow() == null || entity.getPhase() == null
          || entity.getPayload() == null
          || (!"IDLE".equals(entity.getWorkflow()) && entity.getOwnerUserId() == null)) {
        throw new JsonParseException("Required workflow state fields are missing");
      }
      Map<String, Object> values = StageUtils.getGson().fromJson(entity.getPayload(), Map.class);
      if (values == null) {
        values = Collections.emptyMap();
      }
      return new ScopedWorkflowState(entity.getRevision(), entity.getOwnerName(),
          entity.getWorkflow(), entity.getPhase(), values);
    } catch (JsonParseException | ClassCastException e) {
      throw serverError("WORKFLOW_STATE_CORRUPT", "Persisted workflow state is invalid", e);
    }
  }

  private AuthenticatedUser authenticatedUser() throws AuthorizationException {
    AuthenticatedUser user = authenticatedUserOrNull();
    if (user == null) {
      throw new AuthorizationException();
    }
    return user;
  }

  private AuthenticatedUser authenticatedUserOrNull() {
    String username = AuthorizationHelper.getAuthenticatedName();
    int userId = AuthorizationHelper.getAuthenticatedId();
    if (username == null || username.isEmpty() || userId < 0) {
      return null;
    }
    UserEntity user = userDAO.findByPK(userId);
    if (user == null || !Boolean.TRUE.equals(user.getActive())
        || !username.equalsIgnoreCase(user.getUserName())) {
      return null;
    }
    return new AuthenticatedUser(user.getUserId(), user.getUserName());
  }

  private void rejectReservedKey(String key) {
    if (isReservedKey(key)) {
      throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(error("RESERVED_PERSIST_KEY", "The requested key is reserved for scoped workflow state"))
          .build());
    }
  }

  private WebApplicationException badRequest(String code, String message) {
    return errorResponse(Response.Status.BAD_REQUEST, code, message, null);
  }

  private WebApplicationException conflict(String code, String message) {
    return errorResponse(Response.Status.CONFLICT, code, message, null);
  }

  private WebApplicationException serverError(String code, String message, Throwable cause) {
    return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, code, message, cause);
  }

  private WebApplicationException errorResponse(Response.Status status, String code, String message,
      Throwable cause) {
    Response response = Response.status(status)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(error(code, message))
        .build();
    return cause == null ? new WebApplicationException(response) : new WebApplicationException(cause, response);
  }

  private Map<String, String> error(String code, String message) {
    Map<String, String> error = new LinkedHashMap<>();
    error.put("code", code);
    error.put("message", message);
    return error;
  }

  private static final class ScopeKey {
    private final Cluster cluster;
    private final String storageKey;

    private ScopeKey(Cluster cluster, String storageKey) {
      this.cluster = cluster;
      this.storageKey = storageKey;
    }

    private static ScopeKey cluster(Cluster cluster, String storageKey) {
      return new ScopeKey(cluster, storageKey);
    }

    private static ScopeKey draft(String storageKey) {
      return new ScopeKey(null, storageKey);
    }
  }

  private static final class AuthenticatedUser {
    private final Integer id;
    private final String username;

    private AuthenticatedUser(Integer id, String username) {
      this.id = id;
      this.username = username;
    }
  }
}
