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
package org.apache.ambari.server.agent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.google.gson.annotations.SerializedName;

/**
 * Wraps the information required to create repositories from a command.  This was added
 * as a top level command object.
 */
public class CommandRepository {

  @SerializedName("repositories")
  private List<Repository> m_repositories = new ArrayList<>();

  @SerializedName("repoVersion")
  private String m_repoVersion;

  @SerializedName("repoVersionId")
  private long m_repoVersionId;

  @SerializedName("stackName")
  private String m_stackName;

  @SerializedName("repoFileName")
  private String m_repoFileName;

  @SerializedName("feature")
  private final CommandRepositoryFeature feature = new CommandRepositoryFeature();

  /**
   * Provides {@link CommandRepository} feature
   *
   * @return {@link CommandRepositoryFeature}
   */
  public CommandRepositoryFeature getFeature(){
    return feature;
  }

  /**
   * {@code true} if Ambari believes that this repository has reported back it's
   * version after distribution.
   */
  @SerializedName("resolved")
  private boolean m_resolved;

  /**
   * @param version the repo version
   */
  public void setRepositoryVersion(String version) {
    m_repoVersion = version;
  }

  /**
   * @param id the repository id
   */
  public void setRepositoryVersionId(long id) {
    m_repoVersionId = id;
  }

  /**
   * @param name the stack name
   */
  public void setStackName(String name) {
    m_stackName = name;
  }

  /**
   * @param repositories the repositories if sourced from the stack instead of the repo_version.
   */
  public void setRepositories(Collection<RepositoryInfo> repositories) {
    m_repositories = new ArrayList<>();

    for (RepositoryInfo info : repositories) {
      m_repositories.add(new Repository(info));
    }
  }

  /**
   * @param osType        the OS type for the repositories
   * @param repositories  the repository entities that should be processed into a file
   */
  public void setRepositories(String osType, Collection<RepositoryEntity> repositories) {
    m_repositories = new ArrayList<>();

    for (RepositoryEntity entity : repositories) {
      m_repositories.add(new Repository(osType, entity));
    }
  }

  /**
   * @return the repositories that the command should process into a file.
   */
  public Collection<Repository> getRepositories() {
    return m_repositories;
  }

  /**
   * Sets a uniqueness on the repo ids.
   *
   * @param suffix  the repo id suffix
   */
  public void setUniqueSuffix(String suffix) {
    for (Repository repo : m_repositories) {
      repo.m_repoId = repo.m_repoId + suffix;
    }
  }

  /**
   * Sets fields for non-managed
   */
  public void setNonManaged() {
    for (Repository repo : m_repositories) {
      repo.m_baseUrl = null;
      repo.m_mirrorsList = null;
      repo.m_ambariManaged = false;
    }
  }

  /**
   * Gets whether this repository has been marked as having its version
   * resolved.
   *
   * @return {@code true} if this repository has been confirmed to have the
   *         right version.
   */
  public boolean isResolved() {
    return m_resolved;
  }

  /**
   * Gets whether this repository has had its version resolved.
   *
   * @param resolved
   *          {@code true} to mark this repository as being resolved.
   */
  public void setResolved(boolean resolved) {
    m_resolved = resolved;
  }

  /**
   * Update repository id to be consistent with old format
   *
   * @param repoVersion
   */
  @Deprecated
  @Experimental(feature= ExperimentalFeature.PATCH_UPGRADES)
  public void setLegacyRepoId(String repoVersion){
    for (Repository repo : m_repositories) {
      repo.m_repoId = String.format("%s-%s", repo.getRepoName(), repoVersion);
    }
  }

  /**
   * Sets filename for the repo
   *
   * @param stackName  name of the stack
   * @param repoVersion repository version
   */
  @Deprecated
  @Experimental(feature= ExperimentalFeature.PATCH_UPGRADES)
  public void setLegacyRepoFileName(String stackName, String repoVersion) {
    this.m_repoFileName = String.format("%s-%s", stackName, repoVersion);
  }

  /**
   * Sets filename for the repo
   *
   * @param stackName  name of the stack
   * @param repoVersionId repository version id
   */
  public void setRepoFileName(String stackName, Long repoVersionId) {
    this.m_repoFileName = String.format("ambari-%s-%s", stackName.toLowerCase(), repoVersionId.toString());
  }

  /**
   * Minimal information about repository feature
   */
  public static class CommandRepositoryFeature {

    /**
     * Repository is pre-installed on the host
     */
    @SerializedName("preInstalled")
    private Boolean m_isPreInstalled = false;

    /**
     * Indicates if any operation with the packages should be scoped to this repository only.
     *
     * Currently affecting: getting available packages from the repository
     */
    @SerializedName("scoped")
    private boolean m_isScoped = true;

    public void setIsScoped(boolean isScoped){
      this.m_isScoped = isScoped;
    }

    public void setPreInstalled(String isPreInstalled) {
      this.m_isPreInstalled = isPreInstalled.equalsIgnoreCase("true");
    }

  }

  /**
   * Minimal information required to generate repo files on the agent.  These are copies
   * of the repository objects from repo versions that can be changed for URL overrides, etc.
   */
  public static class Repository {

    @SerializedName("baseUrl")
    private String m_baseUrl;

    @SerializedName("repoId")
    private String m_repoId;

    @SerializedName("ambariManaged")
    private boolean m_ambariManaged = true;


    @SerializedName("repoName")
    private final String m_repoName;

    @SerializedName("distribution")
    private final String m_distribution;

    @SerializedName("components")
    private final String m_components;

    @SerializedName("mirrorsList")
    private String m_mirrorsList;

    private transient String m_osType;

    private Repository(RepositoryInfo info) {
      m_baseUrl = info.getBaseUrl();
      m_osType = info.getOsType();
      m_repoId = info.getRepoId();
      m_repoName = info.getRepoName();
      m_distribution = info.getDistribution();
      m_components = info.getComponents();
      m_mirrorsList = info.getMirrorsList();
    }

    private Repository(String osType, RepositoryEntity entity) {
      m_baseUrl = entity.getBaseUrl();
      m_repoId = entity.getRepositoryId();
      m_repoName = entity.getName();
      m_distribution = entity.getDistribution();
      m_components = entity.getComponents();
      m_mirrorsList = entity.getMirrorsList();
      m_osType = osType;
    }

    public void setRepoId(String repoId){
      m_repoId = repoId;
    }

    public void setBaseUrl(String url) {
      m_baseUrl = url;
    }

    public String getOsType() {
      return m_osType;
    }

    public String getRepoId() {
      return m_repoId;
    }

    public String getRepoName() {
      return m_repoName;
    }

    public String getDistribution() {
      return m_distribution;
    }

    public String getComponents() {
      return m_components;
    }

    public String getBaseUrl() {
      return m_baseUrl;
    }

    public boolean isAmbariManaged() {
      return m_ambariManaged;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return new ToStringBuilder(null)
          .append("os", m_osType)
          .append("name", m_repoName)
          .append("distribution", m_distribution)
          .append("components", m_components)
          .append("id", m_repoId)
          .append("baseUrl", m_baseUrl)
          .toString();
    }

  }
}
