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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.state.stack.RepoTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "repo_definition")
@TableGenerator(name = "repo_definition_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "repo_definition_id_seq"
)
public class RepoDefinitionEntity {
  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "repo_definition_id_generator")
  private Long id;

  @Enumerated(value = EnumType.STRING)
  @ElementCollection(targetClass = RepoTag.class)
  @CollectionTable(name = "repo_tag_states", joinColumns = @JoinColumn(name = "repo_definition_id"))
  @Column(name = "tag_state")
  private Set<RepoTag> repoTags = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "repo_os_id", nullable = false)
  private RepoOsEntity repoOs;

  @Column(name = "repo_name", nullable = false)
  private String repoName;

  @Column(name = "repo_id", nullable = false)
  private String repoID;

  @Column(name = "base_url", nullable = false)
  private String baseUrl;

  @Column(name = "mirrors")
  private String mirrors;

  @Column(name = "distribution")
  private String distribution;

  @Column(name = "components")
  private String components;

  @Column(name = "unique_repo", nullable = false)
  private short unique = 0;



  public String getDistribution() {
    return distribution;
  }

  public void setDistribution(String distribution) {
    this.distribution = distribution;
  }

  public RepoOsEntity getRepoOs() {
    return repoOs;
  }

  public void setRepoOs(RepoOsEntity repoOs) {
    this.repoOs = repoOs;
  }

  public String getRepoName() {
    return repoName;
  }

  public void setRepoName(String repoName) {
    this.repoName = repoName;
  }

  public String getRepoID() {
    return repoID;
  }

  public void setRepoID(String repoID) {
    this.repoID = repoID;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getMirrors() {
    return mirrors;
  }

  public void setMirrors(String mirrors) {
    this.mirrors = mirrors;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getComponents() {
    return components;
  }

  public void setComponents(String components) {
    this.components = components;
  }

  public boolean isUnique() {
    return unique == 1;
  }

  public void setUnique(boolean unique) {
    this.unique = (short) (unique ? 1 : 0);
  }

  public Set<RepoTag> getTags() {
    return repoTags;
  }

  public void setTags(Set<RepoTag> repoTags) {
    this.repoTags = repoTags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RepoDefinitionEntity that = (RepoDefinitionEntity) o;

    if (unique != that.unique) return false;
    if (repoTags != null ? !repoTags.equals(that.repoTags) : that.repoTags != null) return false;
    if (repoName != null ? !repoName.equals(that.repoName) : that.repoName != null) return false;
    if (repoID != null ? !repoID.equals(that.repoID) : that.repoID != null) return false;
    if (baseUrl != null ? !baseUrl.equals(that.baseUrl) : that.baseUrl != null) return false;
    if (mirrors != null ? !mirrors.equals(that.mirrors) : that.mirrors != null) return false;
    if (distribution != null ? !distribution.equals(that.distribution) : that.distribution != null) return false;
    return components != null ? components.equals(that.components) : that.components == null;
  }

  @Override
  public int hashCode() {
    int result = repoTags != null ? repoTags.hashCode() : 0;
    result = 31 * result + (repoName != null ? repoName.hashCode() : 0);
    result = 31 * result + (repoID != null ? repoID.hashCode() : 0);
    result = 31 * result + (baseUrl != null ? baseUrl.hashCode() : 0);
    result = 31 * result + (mirrors != null ? mirrors.hashCode() : 0);
    result = 31 * result + (distribution != null ? distribution.hashCode() : 0);
    result = 31 * result + (components != null ? components.hashCode() : 0);
    result = 31 * result + (int) unique;
    return result;
  }
}