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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "repo_tag")
@TableGenerator(name = "repo_tag_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "repo_tag_id_seq",
    initialValue = 0
)
public class RepoTagEntity {

  @Id
  @Column(name = "repo_tag_id", nullable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "repo_tag_id_generator")
  private Long id;

  @Column(name = "tag", nullable = false)
  private String tag;

  @ManyToOne
  @JoinColumn(name = "repo_definition_id", referencedColumnName = "repo_definition_id")
  private RepoDefinitionEntity repoDefinitionEntity;

  public RepoTagEntity(String tag) {
    this.tag = tag;
  }

  public RepoTagEntity() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public RepoDefinitionEntity getRepoDefinitionEntity() {
    return repoDefinitionEntity;
  }


  public void setRepoDefinitionEntity(RepoDefinitionEntity repoDefinitionEntity) {
    this.repoDefinitionEntity = repoDefinitionEntity;
  }
}
