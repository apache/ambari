/**
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

/**
 * Emulates entity to provide a quick way to change it to real entity in future.
 */
public class RepositoryEntity {

  private String name;
  private String baseUrl;
  private String repositoryId;
  private String mirrorsList;
  private boolean unique;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RepositoryEntity that = (RepositoryEntity) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (baseUrl != null ? !baseUrl.equals(that.baseUrl) : that.baseUrl != null) return false;
    if (repositoryId != null ? !repositoryId.equals(that.repositoryId) : that.repositoryId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (baseUrl != null ? baseUrl.hashCode() : 0);
    result = 31 * result + (repositoryId != null ? repositoryId.hashCode() : 0);
    return result;
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
}
