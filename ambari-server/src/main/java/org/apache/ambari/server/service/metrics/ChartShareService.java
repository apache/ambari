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
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.metrics.ChartShareRequest;
import org.apache.ambari.server.api.services.metrics.ChartShareResponse;
import org.apache.ambari.server.orm.dao.ChartShareDAO;
import org.apache.ambari.server.orm.entities.ChartShareEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ChartShareService {
  private static final int MAX_SHARES = 50;
  private static final int MAX_CONFIG_LENGTH = 1024 * 1024;
  private static final Set<RoleAuthorization> READ_AUTHORIZATIONS =
      Set.of(RoleAuthorization.CLUSTER_VIEW_METRICS);

  private final ChartShareDAO chartShareDAO;
  private final DatasourceService datasourceService;
  private final Provider<Clusters> clusters;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Inject
  public ChartShareService(ChartShareDAO chartShareDAO, DatasourceService datasourceService,
      Provider<Clusters> clusters) {
    this.chartShareDAO = chartShareDAO;
    this.datasourceService = datasourceService;
    this.clusters = clusters;
  }

  public List<ChartShareResponse> get(String clusterName, List<Long> ids)
      throws AmbariException, AuthorizationException {
    verifyRead(clusterName);
    if (ids == null || ids.isEmpty() || ids.size() > MAX_SHARES) {
      throw new IllegalArgumentException("ids must contain between 1 and " + MAX_SHARES + " share IDs");
    }
    Map<Long, ChartShareEntity> accessible = new LinkedHashMap<>();
    for (ChartShareEntity entity : chartShareDAO.findByCluster(clusterName)) {
      accessible.put(entity.getId(), entity);
    }
    // Existing 3.0_metrics rows used an empty cluster. Keep them readable after
    // the caller has passed authorization for a real Ambari cluster.
    for (ChartShareEntity entity : chartShareDAO.findByCluster("")) {
      accessible.putIfAbsent(entity.getId(), entity);
    }
    List<ChartShareResponse> result = new ArrayList<>();
    for (Long id : ids) {
      ChartShareEntity entity = accessible.get(id);
      if (entity != null) {
        result.add(new ChartShareResponse(entity));
      }
    }
    return result;
  }

  @Transactional
  public List<Long> create(String clusterName, List<ChartShareRequest> requests)
      throws AmbariException, AuthorizationException {
    verifyRead(clusterName);
    if (requests == null || requests.isEmpty() || requests.size() > MAX_SHARES) {
      throw new IllegalArgumentException("Request must contain between 1 and " + MAX_SHARES + " chart shares");
    }
    String actor = AuthorizationHelper.getAuthenticatedName("");
    long now = System.currentTimeMillis() / 1000;
    List<Long> ids = new ArrayList<>();
    for (ChartShareRequest request : requests) {
      validateConfigs(request == null ? null : request.getConfigs());
      long datasourceId = resolveDatasourceId(request);
      if (datasourceId <= 0) {
        throw new IllegalArgumentException("datasource_id is required");
      }
      datasourceService.get(datasourceId, clusterName);
      ChartShareEntity entity = new ChartShareEntity();
      entity.setCluster(clusterName);
      entity.setDatasourceId(datasourceId);
      entity.setConfigs(request.getConfigs());
      entity.setCreateAt(now);
      entity.setCreateBy(actor);
      chartShareDAO.create(entity);
      ids.add(entity.getId());
    }
    return ids;
  }

  private long resolveDatasourceId(ChartShareRequest request) {
    if (request.getDatasourceId() != null) {
      return request.getDatasourceId();
    }
    try {
      JsonNode root = objectMapper.readTree(request.getConfigs());
      JsonNode dataProps = root.path("dataProps");
      JsonNode value = dataProps.path("datasourceValue");
      return value.canConvertToLong() ? value.asLong() : 0L;
    } catch (JsonProcessingException e) {
      return 0L;
    }
  }

  private void validateConfigs(String configs) {
    if (configs == null || configs.isBlank()) {
      throw new IllegalArgumentException("configs is required");
    }
    if (configs.length() > MAX_CONFIG_LENGTH) {
      throw new IllegalArgumentException("Chart share configs exceed the 1 MiB limit");
    }
    try {
      if (objectMapper.readTree(configs) == null) {
        throw new IllegalArgumentException("Chart share configs must be valid JSON");
      }
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Chart share configs must be valid JSON", e);
    }
  }

  private void verifyRead(String clusterName) throws AmbariException, AuthorizationException {
    if (clusterName == null || clusterName.isBlank()) {
      throw new IllegalArgumentException("cluster_name is required");
    }
    Cluster cluster = clusters.get().getCluster(clusterName);
    AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, cluster.getResourceId(), READ_AUTHORIZATIONS);
  }
}
