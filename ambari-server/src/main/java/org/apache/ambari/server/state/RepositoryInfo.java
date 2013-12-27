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

public class RepositoryInfo {
  private String baseUrl;
  private String osType;
  private String repoId;
  private String repoName;
  private String mirrorsList;
  private String defaultBaseUrl;

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

  @Override
  public String toString() {
    return "[ repoInfo: "
        + ", osType=" + osType
        + ", repoId=" + repoId
        + ", baseUrl=" + baseUrl
        + ", repoName=" + repoName
        + ", mirrorsList=" + mirrorsList
        + " ]";
  }
  
  
  public RepositoryResponse convertToResponse()
  {
    return new RepositoryResponse(getBaseUrl(), getOsType(), getRepoId(), getRepoName(), getMirrorsList(), getDefaultBaseUrl());
  }
}
