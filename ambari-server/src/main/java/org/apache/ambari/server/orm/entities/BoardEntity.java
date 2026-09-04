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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

/** Dashboard metadata compatible with the 3.0_metrics board table. */
@Entity
@Table(name = "board")
@TableGenerator(name = "board_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
    pkColumnValue = "board_id_seq", initialValue = 0)
@NamedQueries({
    @NamedQuery(name = "BoardEntity.findVisibleByCluster",
        query = "SELECT board FROM BoardEntity board WHERE board.hidden = 0 "
            + "AND (board.clusterName = :clusterName "
            + "OR (board.clusterName = '__ambari_builtin__' AND board.builtIn = 1)) "
            + "ORDER BY board.name"),
    @NamedQuery(name = "BoardEntity.findPublicVisibleByCluster",
        query = "SELECT board FROM BoardEntity board WHERE board.hidden = 0 AND board.publicFlag = 1 "
            + "AND (board.clusterName = :clusterName "
            + "OR (board.clusterName = '__ambari_builtin__' AND board.builtIn = 1)) "
            + "ORDER BY board.name"),
    @NamedQuery(name = "BoardEntity.findByIdAndCluster",
        query = "SELECT board FROM BoardEntity board WHERE board.id = :id AND board.clusterName = :clusterName"),
    @NamedQuery(name = "BoardEntity.findByIdentAndCluster",
        query = "SELECT board FROM BoardEntity board WHERE board.ident = :ident AND board.clusterName = :clusterName"),
    @NamedQuery(name = "BoardEntity.findByClusterGroupAndName",
        query = "SELECT board FROM BoardEntity board WHERE board.clusterName = :clusterName "
            + "AND board.groupId = :groupId AND board.name = :name"),
    @NamedQuery(name = "BoardEntity.findBuiltinByIdent",
        query = "SELECT board FROM BoardEntity board WHERE board.ident = :ident "
            + "AND board.clusterName = '__ambari_builtin__' AND board.builtIn = 1")
})
public class BoardEntity {
  public static final String BUILTIN_CLUSTER = "__ambari_builtin__";

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "board_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "cluster_name", nullable = false, length = 255)
  private String clusterName = BUILTIN_CLUSTER;

  @Column(name = "group_id", nullable = false)
  private Long groupId = 0L;

  @Column(name = "name", nullable = false, length = 191)
  private String name;

  @Column(name = "ident", nullable = false, length = 200)
  private String ident = "";

  @Column(name = "tags", nullable = false, length = 255)
  private String tags = "";

  @Column(name = "public", nullable = false)
  private Integer publicFlag = 0;

  @Column(name = "built_in", nullable = false)
  private Integer builtIn = 0;

  @Column(name = "hide", nullable = false)
  private Integer hidden = 0;

  @Column(name = "create_at", nullable = false)
  private Long createAt;

  @Column(name = "create_by", nullable = false, length = 64)
  private String createBy = "";

  @Column(name = "update_at", nullable = false)
  private Long updateAt;

  @Column(name = "update_by", nullable = false, length = 64)
  private String updateBy = "";

  @Column(name = "public_cate", nullable = false)
  private Long publicCategory = 0L;

  @Column(name = "display_locations", nullable = false, length = 255)
  private String displayLocations = "";

  @PrePersist
  protected void onCreate() {
    long now = currentEpochSeconds();
    if (createAt == null) {
      createAt = now;
    }
    if (updateAt == null) {
      updateAt = createAt;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updateAt = currentEpochSeconds();
  }

  protected long currentEpochSeconds() {
    return System.currentTimeMillis() / 1000;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getClusterName() { return clusterName; }
  public void setClusterName(String clusterName) { this.clusterName = clusterName; }
  public Long getGroupId() { return groupId; }
  public void setGroupId(Long groupId) { this.groupId = groupId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getIdent() { return ident; }
  public void setIdent(String ident) { this.ident = ident; }
  public String getTags() { return tags; }
  public void setTags(String tags) { this.tags = tags; }
  public Integer getPublicFlag() { return publicFlag; }
  public void setPublicFlag(Integer publicFlag) { this.publicFlag = publicFlag; }
  public Integer getBuiltIn() { return builtIn; }
  public void setBuiltIn(Integer builtIn) { this.builtIn = builtIn; }
  public Integer getHidden() { return hidden; }
  public void setHidden(Integer hidden) { this.hidden = hidden; }
  public Long getCreateAt() { return createAt; }
  public void setCreateAt(Long createAt) { this.createAt = createAt; }
  public String getCreateBy() { return createBy; }
  public void setCreateBy(String createBy) { this.createBy = createBy; }
  public Long getUpdateAt() { return updateAt; }
  public void setUpdateAt(Long updateAt) { this.updateAt = updateAt; }
  public String getUpdateBy() { return updateBy; }
  public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
  public Long getPublicCategory() { return publicCategory; }
  public void setPublicCategory(Long publicCategory) { this.publicCategory = publicCategory; }
  public String getDisplayLocations() { return displayLocations; }
  public void setDisplayLocations(String displayLocations) { this.displayLocations = displayLocations; }
}
