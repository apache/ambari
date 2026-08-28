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
    clone.setName(source.getName() + " Copy");
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
      if (objectMapper.readTree(payload) == null) {
        throw new IllegalArgumentException("Dashboard payload must be valid JSON");
      }
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Dashboard payload must be valid JSON", e);
    }
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
