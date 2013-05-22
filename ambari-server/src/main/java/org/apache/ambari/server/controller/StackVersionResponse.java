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

package org.apache.ambari.server.controller;

import java.util.List;

import org.apache.ambari.server.state.RepositoryInfo;

public class StackVersionResponse {

  private String stackVersion;
  private String minUpgradeVersion;
  private boolean active;
  private List<RepositoryInfo> repositories;

  public StackVersionResponse(String stackVersion, String minUpgradeVersion,
                              boolean active) {
    setStackVersion(stackVersion);
    setMinUpgradeVersion(minUpgradeVersion);
    setActive(active);
  }

  public String getStackVersion() {
    return stackVersion;
  }

  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  public List<RepositoryInfo> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<RepositoryInfo> repositories) {
    this.repositories = repositories;
  }

  public String getMinUpgradeVersion() {
    return minUpgradeVersion;
  }

  public void setMinUpgradeVersion(String minUpgradeVersion) {
    this.minUpgradeVersion = minUpgradeVersion;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
