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
package org.apache.ambari.server.orm.entities;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

/**
 * A monitoring datasource. JSON fields are intentionally kept as raw strings
 * so unknown plugin settings from existing installations round-trip unchanged.
 */
@Entity
@Table(name = "datasource")
@TableGenerator(name = "datasource_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
    pkColumnValue = "datasource_id_seq", initialValue = 0)
@NamedQueries({
    @NamedQuery(name = "DatasourceEntity.findByCluster",
        query = "SELECT datasource FROM DatasourceEntity datasource "
            + "WHERE datasource.clusterName = :clusterName ORDER BY datasource.name"),
    @NamedQuery(name = "DatasourceEntity.findByIdAndCluster",
        query = "SELECT datasource FROM DatasourceEntity datasource "
            + "WHERE datasource.id = :id AND datasource.clusterName = :clusterName"),
    @NamedQuery(name = "DatasourceEntity.findByNameAndCluster",
        query = "SELECT datasource FROM DatasourceEntity datasource "
            + "WHERE datasource.name = :name AND datasource.clusterName = :clusterName"),
    @NamedQuery(name = "DatasourceEntity.findByName",
        query = "SELECT datasource FROM DatasourceEntity datasource WHERE datasource.name = :name"),
    @NamedQuery(name = "DatasourceEntity.findDefaultByCluster",
        query = "SELECT datasource FROM DatasourceEntity datasource "
            + "WHERE datasource.clusterName = :clusterName AND datasource.defaultDatasource = true")
})
public class DatasourceEntity {
  public static final String STATUS_ENABLED = "enabled";
  public static final String STATUS_DISABLED = "disabled";

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "datasource_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "name", nullable = false, length = 191)
  private String name;

  @Column(name = "description", nullable = false, length = 255)
  private String description = "";

  @Column(name = "category", nullable = false, length = 255)
  private String category = "";

  @Column(name = "plugin_id", nullable = false)
  private Long pluginId = 0L;

  @Column(name = "plugin_type", nullable = false, length = 255)
  private String pluginType = "";

  @Column(name = "plugin_type_name", nullable = false, length = 255)
  private String pluginTypeName = "";

  @Column(name = "cluster_name", nullable = false, length = 255)
  private String clusterName;

  @Lob
  @Basic
  @Column(name = "settings", nullable = false)
  private String settings = "{}";

  @Column(name = "status", nullable = false, length = 255)
  private String status = STATUS_ENABLED;

  @Lob
  @Basic
  @Column(name = "http", nullable = false)
  private String http = "{}";

  @Lob
  @Basic
  @Column(name = "auth", nullable = false)
  private String auth = "{}";

  @Column(name = "is_default", nullable = false)
  private Boolean defaultDatasource = false;

  @Column(name = "created_at", nullable = false)
  private Long createdAt;

  @Column(name = "created_by", nullable = false, length = 64)
  private String createdBy = "";

  @Column(name = "updated_at", nullable = false)
  private Long updatedAt;

  @Column(name = "updated_by", nullable = false, length = 64)
  private String updatedBy = "";

  @PrePersist
  protected void onCreate() {
    long now = currentEpochSeconds();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = createdAt;
    }
    if (category == null || category.isEmpty()) {
      category = pluginType == null ? "" : pluginType;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = currentEpochSeconds();
    if (category == null || category.isEmpty()) {
      category = pluginType == null ? "" : pluginType;
    }
  }

  protected long currentEpochSeconds() {
    return System.currentTimeMillis() / 1000;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Long getPluginId() {
    return pluginId;
  }

  public void setPluginId(Long pluginId) {
    this.pluginId = pluginId;
  }

  public String getPluginType() {
    return pluginType;
  }

  public void setPluginType(String pluginType) {
    this.pluginType = pluginType;
  }

  public String getPluginTypeName() {
    return pluginTypeName;
  }

  public void setPluginTypeName(String pluginTypeName) {
    this.pluginTypeName = pluginTypeName;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getSettings() {
    return settings;
  }

  public void setSettings(String settings) {
    this.settings = settings;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getHttp() {
    return http;
  }

  public void setHttp(String http) {
    this.http = http;
  }

  public String getAuth() {
    return auth;
  }

  public void setAuth(String auth) {
    this.auth = auth;
  }

  public Boolean getDefaultDatasource() {
    return defaultDatasource;
  }

  public void setDefaultDatasource(Boolean defaultDatasource) {
    this.defaultDatasource = defaultDatasource;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Long updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }
}
