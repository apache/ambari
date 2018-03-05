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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;

import com.google.common.base.Objects;

/**
 * Represents a Repository operation system type.
 */
@Entity
@Table(name = "repo_os")
@TableGenerator(name = "repo_os_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "repo_os_id_seq"
)
public class RepoOsEntity {
  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "repo_os_id_generator")
  private Long id;

  /**
   * The ID of the mpack that this repository entry belongs to.
   */
  @Column(name = "mpack_id", updatable = false, insertable = false)
  private long mpackId;

  @Column(name = "family")
  private String family;

  @Column(name = "ambari_managed", nullable = false)
  private short ambariManaged = 0;

  /**
   * one-to-many association to {@link RepoDefinitionEntity}
   */
  @OneToMany(orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "repoOs")
  private List<RepoDefinitionEntity> repoDefinitionEntities = new ArrayList<>();

  /**
   * many-to-one association to {@link RepositoryVersionEntity}
   */
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "repo_version_id", nullable = true)
  private RepositoryVersionEntity repositoryVersionEntity;

  @ManyToOne
  @JoinColumn(name = "mpack_id", referencedColumnName = "id", nullable = false)
  private MpackEntity mpackEntity;

  /**
   * @return repoDefinitionEntities
   */
  public List<RepoDefinitionEntity> getRepoDefinitionEntities() {
    return repoDefinitionEntities;
  }

  /**
   * Update one-to-many relation without rebuilding the whole entity
   *
   * @param repoDefinitionEntities list of many-to-one entities
   */
  public void addRepoDefinitionEntities(List<RepoDefinitionEntity> repoDefinitionEntities) {
    this.repoDefinitionEntities.addAll(repoDefinitionEntities);
    for (RepoDefinitionEntity repoDefinitionEntity : repoDefinitionEntities) {
      repoDefinitionEntity.setRepoOs(this);
    }
  }

  /**
   * Update one-to-many relation without rebuilding the whole entity
   * @param repoDefinition many-to-one entity
   */
  public void addRepoDefinition(RepoDefinitionEntity repoDefinition) {
    repoDefinitionEntities.add(repoDefinition);
    repoDefinition.setRepoOs(this);
  }


  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  public void setRepositoryVersionEntity(RepositoryVersionEntity repositoryVersionEntity) {
    this.repositoryVersionEntity = repositoryVersionEntity;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Gets the management pack ID.
   *
   * @return the management pack ID.
   */
  public long getMpackId() {
    return mpackId;
  }

  /**
   * Sets the management pack ID.
   *
   * @param mpackId
   *          the ID of the management pack which owns this repository.
   */
  public void setMpackId(long mpackId) {
    this.mpackId = mpackId;
  }

  public String getFamily() {
    return family;
  }

  public void setFamily(String family) {
    this.family = family;
  }

  public boolean isAmbariManaged() {
    return ambariManaged == 1;
  }

  public void setAmbariManaged(boolean ambariManaged) {
    this.ambariManaged = (short) (ambariManaged ? 1 : 0);
  }

  /**
   * Gets the Mpack which is associated with this repository operating system.
   *
   * @return the Mpack
   */
  public MpackEntity getMpackEntity() {
    return mpackEntity;
  }

  /**
   * Sets the Mpack which is associated with this repository operating system.
   *
   * @param mpackEntity
   *          the Mpack
   */
  public void setMpackEntity(MpackEntity mpackEntity) {
    this.mpackEntity = mpackEntity;
    mpackId = mpackEntity.getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return java.util.Objects.hash(mpackId, family, ambariManaged, repoDefinitionEntities);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (null == object) {
      return false;
    }

    if (this == object) {
      return true;
    }

    if (object.getClass() != getClass()) {
      return false;
    }

    RepoOsEntity that = (RepoOsEntity) object;
    return Objects.equal(mpackId, that.mpackId)
        && Objects.equal(ambariManaged, that.ambariManaged)
        && Objects.equal(family, that.family)
        && Objects.equal(repoDefinitionEntities, that.repoDefinitionEntities);
  }
}
