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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.ambari.server.orm.dao.BoardDAO;
import org.apache.ambari.server.orm.dao.BoardPayloadDAO;
import org.apache.ambari.server.orm.entities.BoardEntity;
import org.apache.ambari.server.orm.entities.BoardPayloadEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/** Adds packaged service dashboards without changing dashboards already present in the database. */
@Singleton
public class BuiltinDashboardProvisioner {
  private static final String RESOURCE_ROOT = "/metrics/integrations/Linux/dashboards/";
  private static final List<DashboardResource> RESOURCES = List.of(
      new DashboardResource("HDFS.json", "HDFS"),
      new DashboardResource("HDFS_DataNode.json", "HDFS"),
      new DashboardResource("HDFS_NameNode.json", "HDFS"),
      new DashboardResource("HbaseMaster.json", "HBASE"),
      new DashboardResource("HbaseRegionServer.json", "HBASE"),
      new DashboardResource("HIVE_Server2.json", "HIVE"),
      new DashboardResource("YARN_NodeManager.json", "YARN"),
      new DashboardResource("YARN_ResourceManager.json", "YARN"),
      new DashboardResource("YARN_ResourceManager_Sys.json", "YARN"),
      new DashboardResource("Linux_Fleet_Overview.json", "Dashboard"),
      new DashboardResource("Linux_Host_Detail.json", "Dashboard"));

  private final BoardDAO boardDAO;
  private final BoardPayloadDAO payloadDAO;
  private final ObjectMapper objectMapper;
  private volatile boolean provisioned;

  @Inject
  public BuiltinDashboardProvisioner(BoardDAO boardDAO, BoardPayloadDAO payloadDAO) {
    this.boardDAO = boardDAO;
    this.payloadDAO = payloadDAO;
    objectMapper = new ObjectMapper();
  }

  @Transactional
  public synchronized void provision() {
    if (provisioned) {
      return;
    }

    for (DashboardResource resource : RESOURCES) {
      JsonNode definition = readDefinition(resource.fileName());
      String ident = requiredText(definition, "ident", resource.fileName());
      if (boardDAO.findBuiltinByIdent(ident) != null) {
        continue;
      }

      BoardEntity board = new BoardEntity();
      board.setClusterName(BoardEntity.BUILTIN_CLUSTER);
      board.setGroupId(definition.path("group_id").asLong(0));
      board.setName(requiredText(definition, "name", resource.fileName()));
      board.setIdent(ident);
      board.setTags(definition.path("tags").asText(""));
      board.setPublicFlag(1);
      board.setBuiltIn(1);
      board.setHidden(definition.path("hide").asInt(0));
      board.setPublicCategory(definition.path("public_cate").asLong(0));
      board.setDisplayLocations(resource.displayLocation());
      board.setCreateBy("system");
      board.setUpdateBy("system");
      boardDAO.create(board);

      JsonNode configs = definition.get("configs");
      if (configs == null || !configs.isObject()) {
        throw new IllegalStateException("Built-in dashboard configs are missing: " + resource.fileName());
      }
      BoardPayloadEntity payload = new BoardPayloadEntity();
      payload.setId(board.getId());
      payload.setPayload(configs.toString());
      payloadDAO.create(payload);
    }
    provisioned = true;
  }

  private JsonNode readDefinition(String fileName) {
    try (InputStream stream = BuiltinDashboardProvisioner.class.getResourceAsStream(RESOURCE_ROOT + fileName)) {
      if (stream == null) {
        throw new IllegalStateException("Built-in dashboard resource is missing: " + fileName);
      }
      return objectMapper.readTree(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read built-in dashboard: " + fileName, e);
    }
  }

  private String requiredText(JsonNode definition, String field, String fileName) {
    String value = definition.path(field).asText("").trim();
    if (value.isEmpty()) {
      throw new IllegalStateException("Built-in dashboard " + field + " is missing: " + fileName);
    }
    return value;
  }

  private record DashboardResource(String fileName, String displayLocation) {
  }
}
