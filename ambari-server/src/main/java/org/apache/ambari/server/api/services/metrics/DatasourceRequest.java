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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatasourceRequest {
  private Long id;
  private String name;
  private String description;
  private String category;
  private Long pluginId;
  private String pluginType;
  private String pluginTypeName;
  private String clusterName;
  private JsonNode settings;
  private JsonNode http;
  private JsonNode auth;
  private String status;
  private Boolean defaultDatasource;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  @JsonProperty("plugin_id") public Long getPluginId() { return pluginId; }
  @JsonProperty("plugin_id") public void setPluginId(Long pluginId) { this.pluginId = pluginId; }
  @JsonProperty("plugin_type") public String getPluginType() { return pluginType; }
  @JsonProperty("plugin_type") public void setPluginType(String pluginType) { this.pluginType = pluginType; }
  @JsonProperty("plugin_type_name") public String getPluginTypeName() { return pluginTypeName; }
  @JsonProperty("plugin_type_name") public void setPluginTypeName(String pluginTypeName) { this.pluginTypeName = pluginTypeName; }
  @JsonProperty("cluster_name") public String getClusterName() { return clusterName; }
  @JsonProperty("cluster_name") public void setClusterName(String clusterName) { this.clusterName = clusterName; }
  public JsonNode getSettings() { return settings; }
  public void setSettings(JsonNode settings) { this.settings = settings; }
  public JsonNode getHttp() { return http; }
  public void setHttp(JsonNode http) { this.http = http; }
  public JsonNode getAuth() { return auth; }
  public void setAuth(JsonNode auth) { this.auth = auth; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  @JsonProperty("is_default") public Boolean getDefaultDatasource() { return defaultDatasource; }
  @JsonProperty("is_default") public void setDefaultDatasource(Boolean defaultDatasource) { this.defaultDatasource = defaultDatasource; }
}
