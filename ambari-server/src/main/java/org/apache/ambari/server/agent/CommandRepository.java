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
import java.util.Set;

import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.stack.RepoTag;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Wraps the information required to create repositories from a command.  This was added
 * as a top level command object.
 */
public class CommandRepository {

  @SerializedName("repositories")
  @JsonProperty("repositories")
  private List<Repository> m_repositories = new ArrayList<>();

  @SerializedName("mpackId")
  private long m_mpackId;

  @SerializedName("mpackName")
  private String m_mpackName;

  @SerializedName("mpackVersion")
  private String m_mpackVersion;

  @SerializedName("repoFileName")
  @JsonProperty("repoFileName")
  private String m_repoFileName;

  @SerializedName("feature")
  @JsonProperty("feature")
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
   * @param id
   *          the mpack id
   */
  public void setMpackId(long id) {
    m_mpackId = id;
  }

  /**
   * @param name
   *          the mpack name
   */
  public void setMpackName(String name) {
    m_mpackName = name;
  }

  /**
   * @param mpackVersion
   *          the mpack version
   */
  public void setMpackVersion(String mpackVersion) {
    m_mpackVersion = mpackVersion;
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
  public void setRepositories(String osType, Collection<RepoDefinitionEntity> repositories) {
    m_repositories = new ArrayList<>();

    for (RepoDefinitionEntity entity : repositories) {
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
   * Sets filename for the repo
   *
   * @param stackName  name of the stack
   * @param repoVersionId repository version id
   */
  public void setRepoFileName(String stackName, Long repoVersionId) {
    m_repoFileName = String.format("ambari-%s-%s", stackName.toLowerCase(), repoVersionId.toString());
  }

  /**
   * Minimal information about repository feature
   */
  public static class CommandRepositoryFeature {

    /**
     * Repository is pre-installed on the host
     */
    @SerializedName("preInstalled")
    @JsonProperty("preInstalled")
    private Boolean m_isPreInstalled = false;

    /**
     * Indicates if any operation with the packages should be scoped to this repository only.
     *
     * Currently affecting: getting available packages from the repository
     */
    @SerializedName("scoped")
    @JsonProperty("scoped")
    private boolean m_isScoped = true;

    public void setIsScoped(boolean isScoped){
      m_isScoped = isScoped;
    }

    public void setPreInstalled(String isPreInstalled) {
      m_isPreInstalled = isPreInstalled.equalsIgnoreCase("true");
    }
  }

  /**
   * Minimal information required to generate repo files on the agent.  These are copies
   * of the repository objects from repo versions that can be changed for URL overrides, etc.
   */
  public static class Repository {

    @SerializedName("baseUrl")
    @JsonProperty("baseUrl")
    private String m_baseUrl;

    @SerializedName("repoId")
    @JsonProperty("repoId")
    private String m_repoId;

    @SerializedName("ambariManaged")
    @JsonProperty("ambariManaged")
    private boolean m_ambariManaged = true;

    @SerializedName("repoName")
    @JsonProperty("repoName")
    private final String m_repoName;

    @SerializedName("distribution")
    @JsonProperty("distribution")
    private final String m_distribution;

    @SerializedName("components")
    @JsonProperty("components")
    private final String m_components;

    @SerializedName("mirrorsList")
    @JsonProperty("mirrorsList")
    private String m_mirrorsList;

    @SerializedName("tags")
    private Set<RepoTag> m_tags;


    private transient String m_osType;

    private Repository(RepositoryInfo info) {
      m_baseUrl = info.getBaseUrl();
      m_osType = info.getOsType();
      m_repoId = info.getRepoId();
      m_repoName = info.getRepoName();
      m_distribution = info.getDistribution();
      m_components = info.getComponents();
      m_mirrorsList = info.getMirrorsList();
      m_tags = info.getTags();
    }

    private Repository(String osType, RepoDefinitionEntity entity) {
      m_baseUrl = entity.getBaseUrl();
      m_repoId = entity.getRepoID();
      m_repoName = entity.getRepoName();
      m_distribution = entity.getDistribution();
      m_components = entity.getComponents();
      m_mirrorsList = entity.getMirrors();
      m_osType = osType;
      m_tags = entity.getTags();
    }

    public void setRepoId(String repoId){
      m_repoId = repoId;
    }

    public void setBaseUrl(String url) {
      m_baseUrl = url;
    }

    public String getRepoName() {
      return m_repoName;
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
