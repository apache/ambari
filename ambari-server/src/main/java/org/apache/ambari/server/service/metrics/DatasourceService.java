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
package org.apache.ambari.server.service.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.metrics.DatasourceRequest;
import org.apache.ambari.server.api.services.metrics.DatasourceResponse;
import org.apache.ambari.server.api.services.metrics.DatasourceResponseMapper;
import org.apache.ambari.server.orm.dao.BoardPayloadDAO;
import org.apache.ambari.server.orm.dao.DatasourceDAO;
import org.apache.ambari.server.orm.entities.BoardPayloadEntity;
import org.apache.ambari.server.orm.entities.DatasourceEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class DatasourceService {
  private static final String CREDENTIAL_ALIAS_FIELD = "credential_alias";
  private static final String CREDENTIAL_ALIAS_PREFIX = "metrics.datasource.";
  private static final String CREDENTIAL_PRINCIPAL = "ambari-monitoring";
  private static final String SECRET_FORMAT_FIELD = "format";
  private static final String SECRET_FORMAT = "ambari-prometheus-datasource-v1";
  private static final Set<RoleAuthorization> READ_AUTHORIZATIONS =
      Set.of(RoleAuthorization.CLUSTER_VIEW_METRICS);

  private static final Map<String, String> OPEN_SOURCE_PLUGINS = Map.of(
      "prometheus", "Prometheus",
      "elasticsearch", "Elasticsearch",
      "loki", "Loki",
      "jaeger", "Jaeger",
      "tdengine", "TDengine");

  private final DatasourceDAO datasourceDAO;
  private final BoardPayloadDAO boardPayloadDAO;
  private final DatasourceResponseMapper responseMapper;
  private final CredentialStoreService credentialStoreService;
  private final BuiltinDatasourceProvisioner builtinDatasourceProvisioner;
  private final Provider<Clusters> clusters;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final DatasourceConfigurationSecrets configurationSecrets =
      new DatasourceConfigurationSecrets(objectMapper);

  @Inject
  public DatasourceService(DatasourceDAO datasourceDAO, BoardPayloadDAO boardPayloadDAO,
      DatasourceResponseMapper responseMapper, CredentialStoreService credentialStoreService,
      BuiltinDatasourceProvisioner builtinDatasourceProvisioner, Provider<Clusters> clusters) {
    this.datasourceDAO = datasourceDAO;
    this.boardPayloadDAO = boardPayloadDAO;
    this.responseMapper = responseMapper;
    this.credentialStoreService = credentialStoreService;
    this.builtinDatasourceProvisioner = builtinDatasourceProvisioner;
    this.clusters = clusters;
  }

  public List<DatasourceResponse> list(String clusterName) throws AmbariException, AuthorizationException {
    verifyRead(clusterName);
    builtinDatasourceProvisioner.provision(clusterName);
    List<DatasourceResponse> result = new ArrayList<>();
    for (DatasourceEntity entity : datasourceDAO.findByCluster(clusterName)) {
      result.add(responseMapper.toResponse(entity));
    }
    return result;
  }

  public List<DatasourceResponse> query(String clusterName, String type, String category, String name)
      throws AmbariException, AuthorizationException {
    verifyRead(clusterName);
    builtinDatasourceProvisioner.provision(clusterName);
    List<DatasourceResponse> result = new ArrayList<>();
    for (DatasourceEntity entity : datasourceDAO.findByCluster(clusterName)) {
      if (matches(entity.getPluginType(), type) && matches(entity.getCategory(), category)
          && contains(entity.getName(), name)) {
        result.add(responseMapper.toResponse(entity));
      }
    }
    return result;
  }

  public DatasourceResponse get(long id, String clusterName) throws AmbariException, AuthorizationException {
    verifyRead(clusterName);
    return responseMapper.toResponse(requireDatasource(id, clusterName));
  }

  public List<Map<String, Object>> brief(String clusterName) throws AmbariException, AuthorizationException {
    verifyRead(clusterName);
    builtinDatasourceProvisioner.provision(clusterName);
    List<Map<String, Object>> result = new ArrayList<>();
    for (DatasourceEntity entity : datasourceDAO.findByCluster(clusterName)) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", entity.getId());
      item.put("name", entity.getName());
      item.put("plugin_type", entity.getPluginType());
      item.put("category", entity.getCategory());
      item.put("status", entity.getStatus());
      item.put("is_default", Boolean.TRUE.equals(entity.getDefaultDatasource()));
      result.add(item);
    }
    return result;
  }

  public List<Map<String, String>> plugins(String clusterName) throws AmbariException, AuthorizationException {
    verifyRead(clusterName);
    builtinDatasourceProvisioner.provision(clusterName);
    Map<String, String> plugins = new LinkedHashMap<>(OPEN_SOURCE_PLUGINS);
    for (DatasourceEntity entity : datasourceDAO.findByCluster(clusterName)) {
      plugins.putIfAbsent(entity.getPluginType(), entity.getPluginTypeName());
    }
    List<Map<String, String>> result = new ArrayList<>();
    plugins.forEach((type, name) -> result.add(Map.of("type", type, "name", name)));
    return result;
  }

  @Transactional
  public DatasourceResponse create(DatasourceRequest request) throws AmbariException, AuthorizationException {
    validate(request, false);
    verifyManage(request.getClusterName());
    if (datasourceDAO.findByNameAndCluster(request.getName(), request.getClusterName()) != null) {
      throw new IllegalArgumentException("A datasource with this name already exists");
    }

    DatasourceEntity entity = new DatasourceEntity();
    apply(entity, request, false);
    String actor = AuthorizationHelper.getAuthenticatedName("");
    entity.setCreatedBy(actor);
    entity.setUpdatedBy(actor);
    storeConfiguration(entity,
        valueOrEmpty(request.getAuth()),
        valueOrEmpty(request.getHttp()),
        valueOrEmpty(request.getSettings()));
    datasourceDAO.create(entity);
    ensureSingleDefault(entity);
    return responseMapper.toResponse(entity);
  }

  @Transactional
  public DatasourceResponse update(long id, DatasourceRequest request)
      throws AmbariException, AuthorizationException {
    validate(request, true);
    verifyManage(request.getClusterName());
    DatasourceEntity entity = requireDatasource(id, request.getClusterName());
    DatasourceEntity sameName = datasourceDAO.findByNameAndCluster(request.getName(), request.getClusterName());
    if (sameName != null && !sameName.getId().equals(id)) {
      throw new IllegalArgumentException("A datasource with this name already exists");
    }

    JsonNode currentAuth = resolveAuth(entity);
    JsonNode currentHttp = resolveHttp(entity);
    JsonNode currentSettings = resolveSettings(entity);
    apply(entity, request, true);
    entity.setUpdatedBy(AuthorizationHelper.getAuthenticatedName(""));
    storeConfiguration(entity,
        request.getAuth() == null
            ? currentAuth : request.getAuth().isNull() || (request.getAuth().isObject() && request.getAuth().isEmpty())
                ? objectMapper.createObjectNode() : mergeJson(currentAuth.toString(), request.getAuth()),
        request.getHttp() == null ? currentHttp : mergeJson(currentHttp.toString(), request.getHttp()),
        request.getSettings() == null
            ? currentSettings : mergeJson(currentSettings.toString(), request.getSettings()));
    entity = datasourceDAO.merge(entity);
    ensureSingleDefault(entity);
    return responseMapper.toResponse(entity);
  }

  @Transactional
  public DatasourceResponse updateStatus(long id, String clusterName, String status)
      throws AmbariException, AuthorizationException {
    verifyManage(clusterName);
    validateStatus(status);
    DatasourceEntity entity = requireDatasource(id, clusterName);
    entity.setStatus(status);
    entity.setUpdatedBy(AuthorizationHelper.getAuthenticatedName(""));
    return responseMapper.toResponse(datasourceDAO.merge(entity));
  }

  @Transactional
  public void delete(long id, String clusterName) throws AmbariException, AuthorizationException {
    verifyManage(clusterName);
    DatasourceEntity entity = requireDatasource(id, clusterName);
    if (isReferencedByDashboard(id)) {
      throw new IllegalStateException("The datasource is referenced by a dashboard");
    }
    datasourceDAO.remove(entity);
    removeStoredAuth(entity);
  }

  public DatasourceEntity requireQueryable(long id) throws AmbariException, AuthorizationException {
    DatasourceEntity entity = datasourceDAO.findByPK(id);
    if (entity == null) {
      throw new IllegalArgumentException("Datasource not found");
    }
    verifyRead(entity.getClusterName());
    if (!DatasourceEntity.STATUS_ENABLED.equals(entity.getStatus())) {
      throw new IllegalStateException("Datasource is disabled");
    }
    return entity;
  }

  public void verifyTestAccess(long id, String clusterName) throws AmbariException, AuthorizationException {
    verifyManage(clusterName);
    requireDatasource(id, clusterName);
  }

  public JsonNode resolveAuth(DatasourceEntity entity) throws AmbariException {
    JsonNode stored = readStoredConfiguration(entity);
    if (SECRET_FORMAT.equals(stored.path(SECRET_FORMAT_FIELD).asText())) {
      return stored.path("auth").isMissingNode()
          ? objectMapper.createObjectNode() : stored.path("auth").deepCopy();
    }
    return stored;
  }

  public JsonNode resolveHttp(DatasourceEntity entity) throws AmbariException {
    JsonNode stored = readStoredConfiguration(entity);
    JsonNode secrets = SECRET_FORMAT.equals(stored.path(SECRET_FORMAT_FIELD).asText())
        ? stored.path("http") : null;
    return configurationSecrets.restore(readJson(entity.getHttp()), secrets);
  }

  JsonNode resolveSettings(DatasourceEntity entity) throws AmbariException {
    JsonNode stored = readStoredConfiguration(entity);
    JsonNode secrets = SECRET_FORMAT.equals(stored.path(SECRET_FORMAT_FIELD).asText())
        ? stored.path("settings") : null;
    return configurationSecrets.restore(readJson(entity.getSettings()), secrets);
  }

  private void apply(DatasourceEntity entity, DatasourceRequest request, boolean update) {
    entity.setName(request.getName().trim());
    entity.setDescription(valueOrExisting(request.getDescription(), update ? entity.getDescription() : ""));
    entity.setCategory(valueOrExisting(request.getCategory(), request.getPluginType()));
    if (!update || request.getPluginId() != null) {
      entity.setPluginId(request.getPluginId() == null ? 0L : request.getPluginId());
    }
    entity.setPluginType(request.getPluginType());
    entity.setPluginTypeName(valueOrExisting(request.getPluginTypeName(), request.getPluginType()));
    entity.setClusterName(request.getClusterName());
    if (request.getStatus() != null) {
      validateStatus(request.getStatus());
      entity.setStatus(request.getStatus());
    }
    if (request.getDefaultDatasource() != null) {
      entity.setDefaultDatasource(request.getDefaultDatasource());
    }
  }

  private String valueOrExisting(String value, String fallback) {
    return value == null ? fallback : value;
  }

  JsonNode mergeJson(String existingJson, JsonNode patch) {
    JsonNode existing = readJson(existingJson);
    JsonNode restoredPatch = restoreRedacted(existing, patch);
    if (!restoredPatch.isObject()) {
      return restoredPatch;
    }
    ObjectNode result = existing.isObject()
        ? (ObjectNode) existing.deepCopy()
        : objectMapper.createObjectNode();
    restoredPatch.fields().forEachRemaining(field -> {
      if (field.getValue().isNull()) {
        result.remove(field.getKey());
      } else if (field.getValue().isObject() && result.path(field.getKey()).isObject()) {
        result.set(field.getKey(), mergeJson(result.path(field.getKey()).toString(), field.getValue()));
      } else {
        result.set(field.getKey(), field.getValue().deepCopy());
      }
    });
    return result;
  }

  private JsonNode restoreRedacted(JsonNode existing, JsonNode patch) {
    if (patch == null) {
      return objectMapper.getNodeFactory().nullNode();
    }
    if (patch.isTextual() && DatasourceConfigurationSecrets.REDACTED.equals(patch.textValue())
        && existing != null && !existing.isMissingNode()) {
      return existing.deepCopy();
    }
    if (patch.isObject()) {
      ObjectNode restored = objectMapper.createObjectNode();
      patch.fields().forEachRemaining(field -> restored.set(field.getKey(),
          restoreRedacted(existing == null ? null : existing.path(field.getKey()), field.getValue())));
      return restored;
    }
    if (patch.isArray()) {
      ArrayNode restored = objectMapper.createArrayNode();
      for (int index = 0; index < patch.size(); index++) {
        JsonNode patchItem = patch.get(index);
        JsonNode existingItem = matchingArrayItem(existing, patchItem, index);
        restored.add(restoreRedacted(existingItem, patchItem));
      }
      return restored;
    }
    return patch.deepCopy();
  }

  private JsonNode matchingArrayItem(JsonNode existing, JsonNode patchItem, int index) {
    if (existing == null || !existing.isArray()) {
      return null;
    }
    String identity = patchItem.path("key").asText(patchItem.path("name").asText(null));
    if (identity != null) {
      for (JsonNode candidate : existing) {
        String candidateIdentity = candidate.path("key").asText(candidate.path("name").asText(null));
        if (identity.equalsIgnoreCase(candidateIdentity == null ? "" : candidateIdentity)) {
          return candidate;
        }
      }
    }
    return index < existing.size() ? existing.get(index) : null;
  }

  private void validate(DatasourceRequest request, boolean update) throws AmbariException {
    if (request == null || request.getName() == null || request.getName().isBlank()) {
      throw new IllegalArgumentException("Datasource name is required");
    }
    if (request.getName().length() > 191) {
      throw new IllegalArgumentException("Datasource name is too long");
    }
    if (request.getPluginType() == null || request.getPluginType().isBlank()) {
      throw new IllegalArgumentException("Datasource plugin_type is required");
    }
    if (request.getClusterName() == null || request.getClusterName().isBlank()) {
      throw new IllegalArgumentException("Datasource cluster_name is required");
    }
    if (!update && request.getHttp() == null) {
      throw new IllegalArgumentException("Datasource http configuration is required");
    }
  }

  private void validateStatus(String status) {
    if (!DatasourceEntity.STATUS_ENABLED.equals(status) && !DatasourceEntity.STATUS_DISABLED.equals(status)) {
      throw new IllegalArgumentException("Datasource status must be enabled or disabled");
    }
  }

  private void storeConfiguration(DatasourceEntity entity, JsonNode auth, JsonNode http, JsonNode settings)
      throws AmbariException {
    DatasourceConfigurationSecrets.Split httpSplit = configurationSecrets.split(http);
    DatasourceConfigurationSecrets.Split settingsSplit = configurationSecrets.split(settings);
    boolean hasAuth = auth != null && !auth.isNull() && !(auth.isObject() && auth.isEmpty());
    boolean hasSecrets = hasAuth || configurationSecrets.hasSecrets(httpSplit.secrets())
        || configurationSecrets.hasSecrets(settingsSplit.secrets());

    entity.setHttp(httpSplit.sanitized().toString());
    entity.setSettings(settingsSplit.sanitized().toString());
    if (!hasSecrets) {
      removeStoredAuth(entity);
      entity.setAuth("{}");
      return;
    }
    if (!credentialStoreService.isInitialized(CredentialStoreType.PERSISTED)) {
      throw new IllegalStateException("Persistent credential storage is required for datasource authentication");
    }
    String alias = credentialAlias(entity);
    ObjectNode secretEnvelope = objectMapper.createObjectNode();
    secretEnvelope.put(SECRET_FORMAT_FIELD, SECRET_FORMAT);
    if (hasAuth) {
      secretEnvelope.set("auth", auth.deepCopy());
    }
    if (configurationSecrets.hasSecrets(httpSplit.secrets())) {
      secretEnvelope.set("http", httpSplit.secrets());
    }
    if (configurationSecrets.hasSecrets(settingsSplit.secrets())) {
      secretEnvelope.set("settings", settingsSplit.secrets());
    }
    credentialStoreService.setCredential(entity.getClusterName(), alias,
        new PrincipalKeyCredential(CREDENTIAL_PRINCIPAL, secretEnvelope.toString()), CredentialStoreType.PERSISTED);
    ObjectNode marker = objectMapper.createObjectNode();
    marker.put(CREDENTIAL_ALIAS_FIELD, alias);
    entity.setAuth(marker.toString());
  }

  private String credentialAlias(DatasourceEntity entity) {
    String existing = readJson(entity.getAuth()).path(CREDENTIAL_ALIAS_FIELD).asText(null);
    return existing == null ? CREDENTIAL_ALIAS_PREFIX + UUID.randomUUID() : existing;
  }

  private void removeStoredAuth(DatasourceEntity entity) throws AmbariException {
    JsonNode stored = readJson(entity.getAuth());
    String alias = stored.path(CREDENTIAL_ALIAS_FIELD).asText(null);
    if (alias != null) {
      credentialStoreService.removeCredential(entity.getClusterName(), alias);
    }
  }

  private JsonNode readStoredConfiguration(DatasourceEntity entity) throws AmbariException {
    JsonNode marker = readJson(entity.getAuth());
    String alias = marker.path(CREDENTIAL_ALIAS_FIELD).asText(null);
    if (alias == null) {
      return marker;
    }
    Credential credential = credentialStoreService.getCredential(entity.getClusterName(), alias,
        CredentialStoreType.PERSISTED);
    if (!(credential instanceof PrincipalKeyCredential)) {
      throw new IllegalStateException("Datasource credentials are unavailable");
    }
    char[] key = ((PrincipalKeyCredential) credential).getKey();
    try {
      return readJson(new String(key));
    } finally {
      Arrays.fill(key, '\0');
    }
  }

  private JsonNode valueOrEmpty(JsonNode value) {
    return value == null || value.isNull() ? objectMapper.createObjectNode() : value;
  }

  private void ensureSingleDefault(DatasourceEntity selected) {
    if (!Boolean.TRUE.equals(selected.getDefaultDatasource())) {
      return;
    }
    for (DatasourceEntity entity : datasourceDAO.findByCluster(selected.getClusterName())) {
      if (!entity.getId().equals(selected.getId()) && Boolean.TRUE.equals(entity.getDefaultDatasource())) {
        entity.setDefaultDatasource(false);
        datasourceDAO.merge(entity);
      }
    }
  }

  private boolean isReferencedByDashboard(long datasourceId) {
    for (BoardPayloadEntity payload : boardPayloadDAO.findAll()) {
      try {
        if (containsDatasourceReference(objectMapper.readTree(payload.getPayload()), datasourceId)) {
          return true;
        }
      } catch (JsonProcessingException ignored) {
        // Malformed legacy payload remains untouched and is not normalized here.
      }
    }
    return false;
  }

  private boolean containsDatasourceReference(JsonNode node, long datasourceId) {
    if (node == null) {
      return false;
    }
    if (node.isArray()) {
      for (JsonNode child : node) {
        if (containsDatasourceReference(child, datasourceId)) {
          return true;
        }
      }
      return false;
    }
    if (!node.isObject()) {
      return false;
    }
    Set<String> names = new LinkedHashSet<>(Set.of("datasource_id", "datasourceId", "datasourceValue"));
    for (String name : names) {
      JsonNode value = node.get(name);
      if (value != null && ((value.isNumber() && value.asLong() == datasourceId)
          || (value.isTextual() && Long.toString(datasourceId).equals(value.asText())))) {
        return true;
      }
    }
    for (JsonNode child : node) {
      if (containsDatasourceReference(child, datasourceId)) {
        return true;
      }
    }
    return false;
  }

  private boolean matches(String actual, String expected) {
    return expected == null || expected.isBlank()
        || expected.equalsIgnoreCase(actual == null ? "" : actual);
  }

  private boolean contains(String actual, String expected) {
    return expected == null || expected.isBlank()
        || (actual != null && actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT)));
  }

  private DatasourceEntity requireDatasource(long id, String clusterName) {
    DatasourceEntity entity = datasourceDAO.findByIdAndCluster(id, clusterName);
    if (entity == null) {
      throw new IllegalArgumentException("Datasource not found");
    }
    return entity;
  }

  private JsonNode readJson(String json) {
    if (json == null || json.isBlank()) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Stored datasource configuration is invalid");
    }
  }

  private void verifyRead(String clusterName) throws AmbariException, AuthorizationException {
    Cluster cluster = getCluster(clusterName);
    AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, cluster.getResourceId(), READ_AUTHORIZATIONS);
  }

  private void verifyManage(String clusterName) throws AmbariException, AuthorizationException {
    getCluster(clusterName);
    AuthorizationHelper.verifyAuthorization(ResourceType.AMBARI, null,
        Set.of(RoleAuthorization.AMBARI_MANAGE_SETTINGS));
  }

  private Cluster getCluster(String clusterName) throws AmbariException {
    if (clusterName == null || clusterName.isBlank()) {
      throw new IllegalArgumentException("cluster_name is required");
    }
    return clusters.get().getCluster(clusterName);
  }
}
