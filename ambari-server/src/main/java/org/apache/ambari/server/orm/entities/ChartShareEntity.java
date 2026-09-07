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
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

/** Persisted shared-chart configuration. */
@Entity
@Table(name = "chart_share")
@TableGenerator(name = "chart_share_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
    pkColumnValue = "chart_share_id_seq", initialValue = 0)
@NamedQuery(name = "ChartShareEntity.findByCluster",
    query = "SELECT share FROM ChartShareEntity share WHERE share.cluster = :cluster ORDER BY share.createAt DESC")
public class ChartShareEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "chart_share_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "cluster", nullable = false, length = 128)
  private String cluster = "";

  @Column(name = "datasource_id", nullable = false)
  private Long datasourceId = 0L;

  @Lob
  @Basic
  @Column(name = "configs")
  private String configs;

  @Column(name = "create_at", nullable = false)
  private Long createAt = 0L;

  @Column(name = "create_by", nullable = false, length = 64)
  private String createBy = "";

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getCluster() { return cluster; }
  public void setCluster(String cluster) { this.cluster = cluster; }
  public Long getDatasourceId() { return datasourceId; }
  public void setDatasourceId(Long datasourceId) { this.datasourceId = datasourceId; }
  public String getConfigs() { return configs; }
  public void setConfigs(String configs) { this.configs = configs; }
  public Long getCreateAt() { return createAt; }
  public void setCreateAt(Long createAt) { this.createAt = createAt; }
  public String getCreateBy() { return createBy; }
  public void setCreateBy(String createBy) { this.createBy = createBy; }
}
