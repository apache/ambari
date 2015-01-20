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
package org.apache.ambari.server.controller;

/**
 * Represents a prerequisite check request.
 */
public class PrereqCheckRequest {
  private final String clusterName;
  private String repositoryVersion;

  //TODO make repositoryVersionName also final as soon as UI will be changed to always provide it to API
  public PrereqCheckRequest(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getRepositoryVersion() {
    return repositoryVersion;
  }

  public void setRepositoryVersion(String repositoryVersion) {
    this.repositoryVersion = repositoryVersion;
  }
}
