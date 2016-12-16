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

package org.apache.ambari.server.state;

import org.apache.ambari.server.controller.RepositoryResponse;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

public class RepositoryInfo {
  private String baseUrl;
  private String osType;
  private String repoId;
  private String repoName;
  private String mirrorsList;
  private String defaultBaseUrl;
  private String latestBaseUrl;
  private boolean repoSaved = false;
  private boolean unique = false;

  /**
   * @return the baseUrl
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * @param baseUrl the baseUrl to set
   */
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * @return the osType
   */
  public String getOsType() {
    return osType;
  }

  /**
   * @param osType the osType to set
   */
  public void setOsType(String osType) {
    this.osType = osType;
  }

  /**
   * @return the repoId
   */
  public String getRepoId() {
    return repoId;
  }

  /**
   * @param repoId the repoId to set
   */
  public void setRepoId(String repoId) {
    this.repoId = repoId;
  }

  /**
   * @return the repoName
   */
  public String getRepoName() {
    return repoName;
  }

  /**
   * @param repoName the repoName to set
   */
  public void setRepoName(String repoName) {
    this.repoName = repoName;
  }

  /**
   * @return the mirrorsList
   */
  public String getMirrorsList() {
    return mirrorsList;
  }

  /**
   * @param mirrorsList the mirrorsList to set
   */
  public void setMirrorsList(String mirrorsList) {
    this.mirrorsList = mirrorsList;
  }

  /**
   * @return the default base url
   */
  public String getDefaultBaseUrl() {
    return defaultBaseUrl;
  }

  /**
   * @param url the default base url to set
   */
  public void setDefaultBaseUrl(String url) {
    defaultBaseUrl = url;
  }

  /**
   * @return the latest determined base url
   */
  public String getLatestBaseUrl() {
    return latestBaseUrl;
  }

  /**
   * @param url the latest determined base url
   */
  public void setLatestBaseUrl(String url) {
    latestBaseUrl = url;
  }

  /**
   * @return if the base url or mirrors list was from a saved value
   */
  public boolean isRepoSaved() {
    return repoSaved;
  }

  /**
   * Sets if the base url or mirrors list was from a saved value
   */
  public void setRepoSaved(boolean saved) {
    repoSaved = saved;
  }

  /**
   * @return true if version of HDP that change with each release
   */
  public boolean isUnique() {
    return unique;
  }

  /**
   * @param unique set is version of HDP that change with each release
   */
  public void setUnique(boolean unique) {
    this.unique = unique;
  }

  @Override
  public String toString() {
    return "[ repoInfo: "
        + ", osType=" + osType
        + ", repoId=" + repoId
        + ", baseUrl=" + baseUrl
        + ", repoName=" + repoName
        + ", mirrorsList=" + mirrorsList
        + ", unique=" + unique
        + " ]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RepositoryInfo that = (RepositoryInfo) o;
    return repoSaved == that.repoSaved &&
        unique == that.unique &&
        Objects.equal(baseUrl, that.baseUrl) &&
        Objects.equal(osType, that.osType) &&
        Objects.equal(repoId, that.repoId) &&
        Objects.equal(repoName, that.repoName) &&
        Objects.equal(mirrorsList, that.mirrorsList) &&
        Objects.equal(defaultBaseUrl, that.defaultBaseUrl) &&
        Objects.equal(latestBaseUrl, that.latestBaseUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(baseUrl, osType, repoId, repoName, mirrorsList, defaultBaseUrl, latestBaseUrl, repoSaved, unique);
  }

  public RepositoryResponse convertToResponse()
  {
    return new RepositoryResponse(getBaseUrl(), getOsType(), getRepoId(),
        getRepoName(), getMirrorsList(), getDefaultBaseUrl(), getLatestBaseUrl());
  }

  /**
   * A function that returns the repo name of any RepositoryInfo
   */
  public static final Function<RepositoryInfo, String> GET_REPO_NAME_FUNCTION = new Function<RepositoryInfo, String>() {
    @Override  public String apply(RepositoryInfo input) {
      return input.repoName;
    }
  };

  /**
   * A function that returns the repoId of any RepositoryInfo
   */
  public static final Function<RepositoryInfo, String> GET_REPO_ID_FUNCTION = new Function<RepositoryInfo, String>() {
    @Override  public String apply(RepositoryInfo input) {
      return input.repoId;
    }
  };

  /**
   * A function that returns the baseUrl of any RepositoryInfo
   */
  public static final Function<RepositoryInfo, String> SAFE_GET_BASE_URL_FUNCTION = new Function<RepositoryInfo, String>() {
    @Override  public String apply(RepositoryInfo input) {
      return Strings.nullToEmpty(input.baseUrl);
    }
  };

  /**
   * A function that returns the osType of any RepositoryInfo
   */
  public static final Function<RepositoryInfo, String> GET_OSTYPE_FUNCTION = new Function<RepositoryInfo, String>() {
    @Override  public String apply(RepositoryInfo input) {
      return input.osType;
    }
  };

}
