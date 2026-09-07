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
package org.apache.ambari.server.api.services.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/** Secret-safe API representation of a monitoring datasource. */
public class DatasourceResponse {
  private final Long id;
  private final String name;
  private final String description;
  private final String category;
  private final Long pluginId;
  private final String pluginType;
  private final String pluginTypeName;
  private final String clusterName;
  private final JsonNode settings;
  private final JsonNode http;
  private final boolean authConfigured;
  private final String status;
  private final boolean defaultDatasource;
  private final Long createdAt;
  private final String createdBy;
  private final Long updatedAt;
  private final String updatedBy;

  public DatasourceResponse(Long id, String name, String description, String category, Long pluginId,
      String pluginType, String pluginTypeName, String clusterName, JsonNode settings, JsonNode http,
      boolean authConfigured, String status, boolean defaultDatasource, Long createdAt, String createdBy,
      Long updatedAt, String updatedBy) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.category = category;
    this.pluginId = pluginId;
    this.pluginType = pluginType;
    this.pluginTypeName = pluginTypeName;
    this.clusterName = clusterName;
    this.settings = settings;
    this.http = http;
    this.authConfigured = authConfigured;
    this.status = status;
    this.defaultDatasource = defaultDatasource;
    this.createdAt = createdAt;
    this.createdBy = createdBy;
    this.updatedAt = updatedAt;
    this.updatedBy = updatedBy;
  }

  public Long getId() { return id; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public String getCategory() { return category; }
  @JsonProperty("plugin_id") public Long getPluginId() { return pluginId; }
  @JsonProperty("plugin_type") public String getPluginType() { return pluginType; }
  @JsonProperty("plugin_type_name") public String getPluginTypeName() { return pluginTypeName; }
  @JsonProperty("cluster_name") public String getClusterName() { return clusterName; }
  public JsonNode getSettings() { return settings; }
  public JsonNode getHttp() { return http; }
  @JsonProperty("auth_configured") public boolean isAuthConfigured() { return authConfigured; }
  public String getStatus() { return status; }
  @JsonProperty("is_default") public boolean isDefaultDatasource() { return defaultDatasource; }
  @JsonProperty("created_at") public Long getCreatedAt() { return createdAt; }
  @JsonProperty("created_by") public String getCreatedBy() { return createdBy; }
  @JsonProperty("updated_at") public Long getUpdatedAt() { return updatedAt; }
  @JsonProperty("updated_by") public String getUpdatedBy() { return updatedBy; }
}
