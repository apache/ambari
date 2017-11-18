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

import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.state.stack.RepoTag;

/**
 * Emulates entity to provide a quick way to change it to real entity in future.
 */
public class RepositoryEntity {

  private String name;
  private String distribution;
  private String components;
  private String baseUrl;
  private String repositoryId;
  private String mirrorsList;
  private boolean unique;

  private Set<RepoTag> tags;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDistribution() {
    return distribution;
  }

  public void setDistribution(String distribution) {
    this.distribution = distribution;
  }

  public String getComponents() {
    return components;
  }

  public void setComponents(String components) {
    this.components = components;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getMirrorsList() {
    return mirrorsList;
  }

  public void setMirrorsList(String mirrorsList) {
    this.mirrorsList = mirrorsList;
  }

  public boolean isUnique() {
    return unique;
  }

  public void setUnique(boolean unique) {
    this.unique = unique;
  }

  /**
   * @return the repo tags
   */
  public Set<RepoTag> getTags() {
    return tags == null ? Collections.<RepoTag>emptySet() : tags;
  }

  /**
   * @param repoTags the tags to set
   */
  public void setTags(Set<RepoTag> repoTags) {
    tags = repoTags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RepositoryEntity that = (RepositoryEntity) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (distribution != null ? !distribution.equals(that.distribution) : that.distribution != null) return false;
    if (components != null ? !components.equals(that.components) : that.components != null) return false;
    if (baseUrl != null ? !baseUrl.equals(that.baseUrl) : that.baseUrl != null) return false;
    if (repositoryId != null ? !repositoryId.equals(that.repositoryId) : that.repositoryId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (distribution != null ? distribution.hashCode() : 0);
    result = 31 * result + (components != null ? components.hashCode() : 0);
    result = 31 * result + (baseUrl != null ? baseUrl.hashCode() : 0);
    result = 31 * result + (repositoryId != null ? repositoryId.hashCode() : 0);
    return result;
  }
}
