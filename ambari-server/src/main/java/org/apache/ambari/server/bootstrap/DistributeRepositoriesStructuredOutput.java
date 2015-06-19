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
 *
 * */

package org.apache.ambari.server.bootstrap;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * This class is used for mapping json of structured output for
 * "Distribute repositories/install packages" action.
 */
public class DistributeRepositoriesStructuredOutput {

  /**
   * Repository version that has been (re)installed as a result of current custom action
   */
  @SerializedName("installed_repository_version")
  private String installedRepositoryVersion;

  /**
   * All Ambari-managed repositories that are installed side by side on host
   */
  @SerializedName("ambari_repositories")
  private List<String> ambariRepositories;

  /**
   * Either SUCCESS or FAIL
   */
  @SerializedName("package_installation_result")
  private String packageInstallationResult;

  /**
   * The actual version returned, even when a failure during install occurs.
   */
  @SerializedName("actual_version")
  private String actualVersion;

  /**
   * The stack id used to look up version
   */
  @SerializedName("stack_id")
  private String stackId;

  public String getInstalledRepositoryVersion() {

    return installedRepositoryVersion;
  }

  public List<String> getAmbariRepositories() {
    return ambariRepositories;
  }

  public String getPackageInstallationResult() {
    return packageInstallationResult;
  }

  public String getActualVersion() {
    return actualVersion;
  }

  public String getStackId() {
    return stackId;
  }
}
