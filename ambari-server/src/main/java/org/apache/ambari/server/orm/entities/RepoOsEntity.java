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

  @Column(name = "family")
  private String family;

  @Column(name = "ambari_managed", nullable = false)
  private short ambariManaged = 0;

  @OneToMany(orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "repoOs")
  private List<RepoDefinitionEntity> repoDefinitionEntities = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "repo_version_id", nullable = false)
  private RepositoryVersionEntity repositoryVersionEntity;

  public List<RepoDefinitionEntity> getRepoDefinitionEntities() {
    return repoDefinitionEntities;
  }

  public void addRepoDefinitionEntities(List<RepoDefinitionEntity> repoDefinitionEntities) {
    this.repoDefinitionEntities.addAll(repoDefinitionEntities);
    for (RepoDefinitionEntity repoDefinitionEntity : repoDefinitionEntities) {
      repoDefinitionEntity.setRepoOs(this);
    }
  }

  public void addRepoDefinition(RepoDefinitionEntity repoDefinition) {
    this.repoDefinitionEntities.add(repoDefinition);
    repoDefinition.setRepoOs(this);
  }

  public RepositoryVersionEntity getRepositoryVersionEntity() {
    return repositoryVersionEntity;
  }

  public void setRepositoryVersionEntity(RepositoryVersionEntity repositoryVersionEntity) {
    this.repositoryVersionEntity = repositoryVersionEntity;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RepoOsEntity that = (RepoOsEntity) o;

    if (ambariManaged != that.ambariManaged) return false;
    if (family != null ? !family.equals(that.family) : that.family != null) return false;
    return repoDefinitionEntities != null ? repoDefinitionEntities.equals(that.repoDefinitionEntities) : that.repoDefinitionEntities == null;
  }

  @Override
  public int hashCode() {
    int result = family != null ? family.hashCode() : 0;
    result = 31 * result + (int) ambariManaged;
    result = 31 * result + (repoDefinitionEntities != null ? repoDefinitionEntities.hashCode() : 0);
    return result;
  }
}
