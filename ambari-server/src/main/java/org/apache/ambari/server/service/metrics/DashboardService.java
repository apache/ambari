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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.metrics.BoardRequest;
import org.apache.ambari.server.api.services.metrics.BoardResponse;
import org.apache.ambari.server.orm.dao.BoardDAO;
import org.apache.ambari.server.orm.dao.BoardPayloadDAO;
import org.apache.ambari.server.orm.entities.BoardEntity;
import org.apache.ambari.server.orm.entities.BoardPayloadEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class DashboardService {
  private static final Logger LOG = LoggerFactory.getLogger(DashboardService.class);
  private static final int MAX_PAYLOAD_LENGTH = 16 * 1024 * 1024;
  private static final Set<RoleAuthorization> READ_AUTHORIZATIONS =
      Set.of(RoleAuthorization.CLUSTER_VIEW_METRICS);
  private static final Set<RoleAuthorization> MANAGE_AUTHORIZATIONS =
      Set.of(RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA);
  private static final String DASHBOARD_SCHEMA_VERSION = "3.0.0";
  private static final Set<String> DASHBOARD_FIELDS = Set.of("version", "var", "panels", "graphTooltip", "graphZoom");
  private static final Set<String> VARIABLE_FIELDS = Set.of("name", "label", "type", "definition", "value",
      "multi", "includeAll");
  private static final Set<String> PANEL_FIELDS = Set.of("id", "name", "titleKey", "description", "type",
      "datasourceCate", "datasourceValue", "targets", "layout", "version", "collapsed", "custom",
      "options", "overrides", "links", "maxPerRow", "transformations", "panels");
  private static final Set<String> TARGET_FIELDS = Set.of("refId", "expr", "legend", "instant", "hide",
      "maxDataPoints", "time", "variables", "__mode__");
  private static final Set<String> LAYOUT_FIELDS = Set.of("h", "w", "x", "y", "i", "isResizable");
  private static final Set<String> PANEL_TYPES = Set.of("row", "timeseries", "stat", "gauge", "barGauge",
      "table", "tableNG", "pie", "barchart", "heatmap", "hexbin", "text", "iframe");

  private final BoardDAO boardDAO;
  private final BoardPayloadDAO payloadDAO;
  private final BuiltinDashboardProvisioner builtinDashboardProvisioner;
  private final Provider<Clusters> clusters;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Inject
  public DashboardService(BoardDAO boardDAO, BoardPayloadDAO payloadDAO,
      BuiltinDashboardProvisioner builtinDashboardProvisioner, Provider<Clusters> clusters) {
    this.boardDAO = boardDAO;
    this.payloadDAO = payloadDAO;
    this.builtinDashboardProvisioner = builtinDashboardProvisioner;
    this.clusters = clusters;
  }

  public List<BoardResponse> list(String clusterName, String query, boolean publicOnly)
      throws AmbariException, AuthorizationException {
    verifyRead(clusterName);
    provisionBuiltinDashboards();
    List<BoardResponse> result = new ArrayList<>();
    for (BoardEntity board : publicOnly
        ? boardDAO.findPublicVisibleByCluster(clusterName)
        : boardDAO.findVisibleByCluster(clusterName)) {
      if (matches(board, query)) {
        result.add(new BoardResponse(board, null));
      }
    }
    return result;
  }

  private void provisionBuiltinDashboards() {
    try {
      builtinDashboardProvisioner.provision();
    } catch (RuntimeException e) {
      LOG.warn("Unable to provision built-in monitoring dashboards", e);
    }
  }

  public BoardResponse get(String clusterName, String idOrIdent, boolean includePayload)
      throws AmbariException, AuthorizationException {
    verifyRead(clusterName);
    BoardEntity board = requireReadableBoard(clusterName, idOrIdent);
    BoardPayloadEntity payload = includePayload ? payloadDAO.findByPK(board.getId()) : null;
    return new BoardResponse(board, payload == null ? null : payload.getPayload());
  }

  @Transactional
  public BoardResponse create(String clusterName, BoardRequest request)
      throws AmbariException, AuthorizationException {
    verifyManage(clusterName);
    validateRequest(request, true);
    validateIdent(clusterName, request.getIdent(), null);

    String actor = AuthorizationHelper.getAuthenticatedName("");
    BoardEntity board = new BoardEntity();
    board.setClusterName(clusterName);
    applyMetadata(board, request, true);
    board.setCreateBy(actor);
    board.setUpdateBy(actor);
    boardDAO.create(board);
    if (request.getConfigs() != null) {
      createPayload(board.getId(), request.getConfigs());
    }
    return new BoardResponse(board, request.getConfigs());
  }

  @Transactional
  public BoardResponse update(String clusterName, long id, BoardRequest request)
      throws AmbariException, AuthorizationException {
    verifyManage(clusterName);
    BoardEntity board = requireMutableBoard(clusterName, id);
    if (request.getName() != null && request.getName().isBlank()) {
      throw new IllegalArgumentException("Dashboard name cannot be empty");
    }
    if (request.getIdent() != null) {
      validateIdent(clusterName, request.getIdent(), id);
    }
    applyMetadata(board, request, false);
    board.setUpdateBy(AuthorizationHelper.getAuthenticatedName(""));
    board = boardDAO.merge(board);
    BoardPayloadEntity payload = payloadDAO.findByPK(id);
    return new BoardResponse(board, payload == null ? null : payload.getPayload());
  }

  @Transactional
  public BoardResponse updateConfigs(String clusterName, String idOrIdent, String configs)
      throws AmbariException, AuthorizationException {
    verifyManage(clusterName);
    validatePayload(configs);
    BoardEntity board = requireMutableBoard(clusterName, idOrIdent);
    BoardPayloadEntity payload = payloadDAO.findByPK(board.getId());
    if (payload == null) {
      createPayload(board.getId(), configs);
    } else {
      payload.setPayload(configs);
      payloadDAO.merge(payload);
    }
    board.setUpdateBy(AuthorizationHelper.getAuthenticatedName(""));
    board = boardDAO.merge(board);
    return new BoardResponse(board, configs);
  }

  @Transactional
  public BoardResponse clone(String clusterName, long id) throws AmbariException, AuthorizationException {
    verifyManage(clusterName);
    BoardEntity source = requireReadableBoard(clusterName, id);
    BoardPayloadEntity sourcePayload = payloadDAO.findByPK(id);
    String actor = AuthorizationHelper.getAuthenticatedName("");

    BoardEntity clone = new BoardEntity();
    clone.setClusterName(clusterName);
    clone.setGroupId(source.getGroupId());
    clone.setName(copyName(clusterName, source.getGroupId(), source.getName()));
    clone.setIdent(copyIdent(source.getIdent()));
    clone.setTags(source.getTags());
    clone.setPublicFlag(source.getPublicFlag());
    clone.setBuiltIn(0);
    clone.setHidden(source.getHidden());
    clone.setPublicCategory(source.getPublicCategory());
    clone.setDisplayLocations(source.getDisplayLocations());
    clone.setCreateBy(actor);
    clone.setUpdateBy(actor);
    boardDAO.create(clone);
    if (sourcePayload != null) {
      createPayload(clone.getId(), sourcePayload.getPayload());
    }
    return new BoardResponse(clone, sourcePayload == null ? null : sourcePayload.getPayload());
  }

  @Transactional
  public Map<String, String> cloneMany(String clusterName, List<Long> ids)
      throws AmbariException, AuthorizationException {
    verifyManage(clusterName);
    if (ids == null || ids.isEmpty() || ids.size() > 100) {
      throw new IllegalArgumentException("board_ids must contain between 1 and 100 dashboard IDs");
    }
    Map<String, String> failures = new LinkedHashMap<>();
    for (Long id : ids) {
      try {
        clone(clusterName, id);
      } catch (IllegalArgumentException e) {
        failures.put(String.valueOf(id), e.getMessage());
      }
    }
    return failures;
  }

  @Transactional
  public void delete(String clusterName, long id) throws AmbariException, AuthorizationException {
    verifyManage(clusterName);
    BoardEntity board = requireMutableBoard(clusterName, id);
    BoardPayloadEntity payload = payloadDAO.findByPK(id);
    if (payload != null) {
      payloadDAO.remove(payload);
    }
    boardDAO.remove(board);
  }

  private void applyMetadata(BoardEntity board, BoardRequest request, boolean create) {
    if (request.getGroupId() != null) board.setGroupId(request.getGroupId());
    if (request.getName() != null) board.setName(request.getName().trim());
    if (request.getIdent() != null) board.setIdent(request.getIdent().trim());
    if (request.getTags() != null) board.setTags(request.getTags());
    if (request.getPublicFlag() != null) board.setPublicFlag(flag(request.getPublicFlag(), "public"));
    if (request.getBuiltIn() != null) board.setBuiltIn(flag(request.getBuiltIn(), "built_in"));
    if (request.getHidden() != null) board.setHidden(flag(request.getHidden(), "hide"));
    if (request.getPublicCategory() != null) board.setPublicCategory(request.getPublicCategory());
    if (request.getDisplayLocations() != null) board.setDisplayLocations(request.getDisplayLocations());
    if (create && board.getName() == null) {
      throw new IllegalArgumentException("Dashboard name is required");
    }
  }

  private int flag(int value, String name) {
    if (value != 0 && value != 1) {
      throw new IllegalArgumentException(name + " must be 0 or 1");
    }
    return value;
  }

  private void validateRequest(BoardRequest request, boolean payloadAllowed) {
    if (request == null || request.getName() == null || request.getName().isBlank()) {
      throw new IllegalArgumentException("Dashboard name is required");
    }
    if (request.getName().length() > 191) {
      throw new IllegalArgumentException("Dashboard name is too long");
    }
    if (payloadAllowed && request.getConfigs() != null) {
      validatePayload(request.getConfigs());
    }
  }

  private void validateIdent(String clusterName, String ident, Long currentId) {
    if (ident == null || ident.isBlank()) {
      return;
    }
    if (ident.length() > 200) {
      throw new IllegalArgumentException("Dashboard ident is too long");
    }
    BoardEntity existing = boardDAO.findByIdentAndCluster(ident, clusterName);
    if (existing == null) {
      existing = boardDAO.findBuiltinByIdent(ident);
    }
    if (existing != null && !existing.getId().equals(currentId)) {
      throw new IllegalArgumentException("Dashboard ident already exists");
    }
  }

  private void validatePayload(String payload) {
    if (payload == null) {
      throw new IllegalArgumentException("configs is required");
    }
    if (payload.length() > MAX_PAYLOAD_LENGTH) {
      throw new IllegalArgumentException("Dashboard payload exceeds the 16 MiB limit");
    }
    try {
      JsonNode document = objectMapper.readTree(payload);
      validateDashboardDocument(document);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Dashboard payload must be valid JSON", e);
    }
  }

  private void validateDashboardDocument(JsonNode document) {
    requireObject(document, "Dashboard payload");
    rejectUnknownFields(document, DASHBOARD_FIELDS, "Dashboard payload");
    if (!document.path("version").isTextual()
        || !DASHBOARD_SCHEMA_VERSION.equals(document.path("version").asText())) {
      throw new IllegalArgumentException("Dashboard payload version must be " + DASHBOARD_SCHEMA_VERSION);
    }
    if (!document.path("var").isArray() || !document.path("panels").isArray()) {
      throw new IllegalArgumentException("Dashboard payload must contain var and panels arrays");
    }
    if (document.has("graphTooltip") && !document.path("graphTooltip").isTextual()) {
      throw new IllegalArgumentException("Dashboard payload graphTooltip must be a string");
    }
    if (document.has("graphZoom") && !document.path("graphZoom").isTextual()) {
      throw new IllegalArgumentException("Dashboard payload graphZoom must be a string");
    }
    int variableIndex = 0;
    for (JsonNode variable : document.path("var")) {
      String path = "var[" + variableIndex++ + "]";
      requireObject(variable, path);
      rejectUnknownFields(variable, VARIABLE_FIELDS, path);
      requireText(variable, "name", path);
      String type = requireText(variable, "type", path);
      if (!Set.of("textbox", "datasource", "query").contains(type)) {
        throw new IllegalArgumentException(path + ".type must be textbox, datasource, or query");
      }
      if ("query".equals(type)) {
        requireText(variable, "definition", path);
        for (String field : List.of("multi", "includeAll")) {
          if (variable.has(field) && !variable.path(field).isBoolean()) {
            throw new IllegalArgumentException(path + "." + field + " must be boolean");
          }
        }
      } else if (variable.has("multi") || variable.has("includeAll")) {
        throw new IllegalArgumentException(path + ".multi and .includeAll require type query");
      }
    }
    int panelIndex = 0;
    for (JsonNode panel : document.path("panels")) {
      validatePanel(panel, "panels[" + panelIndex++ + "]");
    }
  }

  private void validatePanel(JsonNode panel, String path) {
    requireObject(panel, path);
    rejectUnknownFields(panel, PANEL_FIELDS, path);
    requireText(panel, "id", path);
    requireText(panel, "name", path);
    if (panel.has("titleKey")) {
      requireText(panel, "titleKey", path);
    }
    String type = requireText(panel, "type", path);
    if (!PANEL_TYPES.contains(type)) {
      throw new IllegalArgumentException(path + ".type is not supported: " + type);
    }
    JsonNode layout = panel.path("layout");
    requireObject(layout, path + ".layout");
    rejectUnknownFields(layout, LAYOUT_FIELDS, path + ".layout");
    for (String field : List.of("h", "w", "x", "y")) {
      if (!layout.path(field).canConvertToInt() || layout.path(field).asInt() < 0) {
        throw new IllegalArgumentException(path + ".layout." + field + " must be a non-negative integer");
      }
    }
    if (layout.path("w").asInt() == 0 || layout.path("h").asInt() == 0
        || layout.path("w").asInt() > 24 || layout.path("x").asInt() + layout.path("w").asInt() > 24) {
      throw new IllegalArgumentException(path + ".layout does not fit the 24-column grid");
    }
    if ("row".equals(type) && layout.path("w").asInt() != 24) {
      throw new IllegalArgumentException(path + ".layout.w must be 24 for row panels");
    }
    requireText(layout, "i", path + ".layout");
    if (!layout.path("isResizable").isBoolean()) {
      throw new IllegalArgumentException(path + ".layout.isResizable must be boolean");
    }
    JsonNode targets = panel.path("targets");
    if (!targets.isArray()) {
      if (!Set.of("row", "text", "iframe").contains(type)) {
        throw new IllegalArgumentException(path + ".targets must be an array");
      }
    } else {
      int targetIndex = 0;
      for (JsonNode target : targets) {
        String targetPath = path + ".targets[" + targetIndex++ + "]";
        requireObject(target, targetPath);
        rejectUnknownFields(target, TARGET_FIELDS, targetPath);
        requireText(target, "refId", targetPath);
        requireText(target, "expr", targetPath);
      }
      if (!Set.of("row", "text", "iframe").contains(type) && targets.isEmpty()) {
        throw new IllegalArgumentException(path + ".targets must not be empty");
      }
    }
    if (panel.has("panels")) {
      if (!panel.path("panels").isArray()) throw new IllegalArgumentException(path + ".panels must be an array");
      int childIndex = 0;
      for (JsonNode child : panel.path("panels")) validatePanel(child, path + ".panels[" + childIndex++ + "]");
    }
  }

  private void rejectUnknownFields(JsonNode object, Set<String> allowed, String path) {
    object.fieldNames().forEachRemaining(field -> {
      if (!allowed.contains(field)) throw new IllegalArgumentException(path + "." + field + " is not supported");
    });
  }

  private void requireObject(JsonNode node, String path) {
    if (node == null || !node.isObject()) throw new IllegalArgumentException(path + " must be an object");
  }

  private String requireText(JsonNode object, String field, String path) {
    JsonNode value = object.path(field);
    if (!value.isTextual() || value.asText().isBlank()) {
      throw new IllegalArgumentException(path + "." + field + " is required");
    }
    return value.asText();
  }

  private void createPayload(long boardId, String rawPayload) {
    validatePayload(rawPayload);
    BoardPayloadEntity payload = new BoardPayloadEntity();
    payload.setId(boardId);
    payload.setPayload(rawPayload);
    payloadDAO.create(payload);
  }

  private String copyIdent(String ident) {
    if (ident == null || ident.isBlank()) {
      return "";
    }
    String suffix = "-copy-" + UUID.randomUUID().toString().substring(0, 8);
    return ident.substring(0, Math.min(ident.length(), 200 - suffix.length())) + suffix;
  }

  private String copyName(String clusterName, long groupId, String sourceName) {
    for (int copyNumber = 1; ; copyNumber++) {
      String suffix = copyNumber == 1 ? " Copy" : " Copy " + copyNumber;
      String baseName = sourceName.substring(0, Math.min(sourceName.length(), 191 - suffix.length())).stripTrailing();
      String candidate = baseName + suffix;
      if (boardDAO.findByClusterGroupAndName(clusterName, groupId, candidate) == null) {
        return candidate;
      }
    }
  }

  private boolean matches(BoardEntity board, String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String nameAndTags = (board.getName() + " " + board.getTags()).toLowerCase(Locale.ROOT);
    for (String term : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
      boolean excluded = term.startsWith("-");
      String value = excluded ? term.substring(1) : term;
      if ((!excluded && !nameAndTags.contains(value)) || (excluded && nameAndTags.contains(value))) {
        return false;
      }
    }
    return true;
  }

  private BoardEntity requireReadableBoard(String clusterName, String idOrIdent) {
    if (idOrIdent == null || idOrIdent.isBlank()) {
      throw new IllegalArgumentException("Dashboard id is required");
    }
    try {
      return requireReadableBoard(clusterName, Long.parseLong(idOrIdent));
    } catch (NumberFormatException ignored) {
      BoardEntity board = boardDAO.findByIdentAndCluster(idOrIdent, clusterName);
      if (board == null) {
        board = boardDAO.findBuiltinByIdent(idOrIdent);
      }
      if (board == null) {
        throw new IllegalArgumentException("Dashboard not found");
      }
      return board;
    }
  }

  private BoardEntity requireReadableBoard(String clusterName, long id) {
    BoardEntity board = boardDAO.findByIdAndCluster(id, clusterName);
    if (board == null) {
      BoardEntity candidate = boardDAO.findByPK(id);
      if (candidate != null && candidate.getBuiltIn() == 1
          && BoardEntity.BUILTIN_CLUSTER.equals(candidate.getClusterName())) {
        board = candidate;
      }
    }
    if (board == null) {
      throw new IllegalArgumentException("Dashboard not found");
    }
    return board;
  }

  private BoardEntity requireMutableBoard(String clusterName, String idOrIdent) {
    if (idOrIdent == null || idOrIdent.isBlank()) {
      throw new IllegalArgumentException("Dashboard id is required");
    }
    try {
      return requireMutableBoard(clusterName, Long.parseLong(idOrIdent));
    } catch (NumberFormatException ignored) {
      BoardEntity board = boardDAO.findByIdentAndCluster(idOrIdent, clusterName);
      if (board == null) {
        throw new IllegalArgumentException("Dashboard not found");
      }
      return board;
    }
  }

  private BoardEntity requireMutableBoard(String clusterName, long id) {
    BoardEntity board = boardDAO.findByIdAndCluster(id, clusterName);
    if (board == null) {
      throw new IllegalArgumentException("Dashboard not found");
    }
    return board;
  }

  private void verifyRead(String clusterName) throws AmbariException, AuthorizationException {
    Cluster cluster = getCluster(clusterName);
    AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, cluster.getResourceId(), READ_AUTHORIZATIONS);
  }

  private void verifyManage(String clusterName) throws AmbariException, AuthorizationException {
    Cluster cluster = getCluster(clusterName);
    AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, cluster.getResourceId(), MANAGE_AUTHORIZATIONS);
  }

  private Cluster getCluster(String clusterName) throws AmbariException {
    if (clusterName == null || clusterName.isBlank()) {
      throw new IllegalArgumentException("cluster_name is required");
    }
    return clusters.get().getCluster(clusterName);
  }
}
