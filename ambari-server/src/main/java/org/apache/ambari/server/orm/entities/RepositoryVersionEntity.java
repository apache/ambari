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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "repo_version", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"display_name"}),
    @UniqueConstraint(columnNames = {"stack", "version"})
})
@TableGenerator(name = "repository_version_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "repo_version_id_seq",
    initialValue = 0,
    allocationSize = 1
    )
@NamedQueries({
  @NamedQuery(name = "repositoryVersionByDisplayName", query = "SELECT repoversion FROM RepositoryVersionEntity repoversion WHERE repoversion.displayName=:displayname"),
  @NamedQuery(name = "repositoryVersionByStackVersion", query = "SELECT repoversion FROM RepositoryVersionEntity repoversion WHERE repoversion.stack=:stack AND repoversion.version=:version")
})
public class RepositoryVersionEntity {

  @Id
  @Column(name = "repo_version_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "repository_version_id_generator")
  private Long id;

  @Column(name = "stack")
  private String stack;

  @Column(name = "version")
  private String version;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "upgrade_package")
  private String upgradePackage;

  @Column(name = "repositories")
  private String repositories;

  // ----- RepositoryVersionEntity -------------------------------------------------------

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getStack() {
    return stack;
  }

  public void setStack(String stack) {
    this.stack = stack;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getUpgradePackage() {
    return upgradePackage;
  }

  public void setUpgradePackage(String upgradePackage) {
    this.upgradePackage = upgradePackage;
  }

  public String getRepositories() {
    return repositories;
  }

  public void setRepositories(String repositories) {
    this.repositories = repositories;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RepositoryVersionEntity that = (RepositoryVersionEntity) o;

    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (stack != null ? !stack.equals(that.stack) : that.stack != null) return false;
    if (version != null ? !version.equals(that.version) : that.version != null) return false;
    if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
    if (upgradePackage != null ? !upgradePackage.equals(that.upgradePackage) : that.upgradePackage != null) return false;
    if (repositories != null ? !repositories.equals(that.repositories) : that.repositories != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (stack != null ? stack.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (upgradePackage != null ? upgradePackage.hashCode() : 0);
    result = 31 * result + (repositories != null ? repositories.hashCode() : 0);
    return result;
  }
}
