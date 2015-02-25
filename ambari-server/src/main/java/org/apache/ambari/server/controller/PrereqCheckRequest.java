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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.checks.CheckDescription;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;

/**
 * Represents a prerequisite check request.
 */
public class PrereqCheckRequest {
  private String m_clusterName;
  private String m_repositoryVersion;
  private Map<CheckDescription, PrereqCheckStatus> m_results =
      new HashMap<CheckDescription, PrereqCheckStatus>();

  public PrereqCheckRequest(String clusterName) {
    m_clusterName = clusterName;
  }

  public String getClusterName() {
    return m_clusterName;
  }

  public String getRepositoryVersion() {
    return m_repositoryVersion;
  }

  public void setRepositoryVersion(String repositoryVersion) {
    m_repositoryVersion = repositoryVersion;
  }

  /**
   * Sets the result of a check.
   * @param description the description
   * @param status      the status result
   */
  public void addResult(CheckDescription description, PrereqCheckStatus status) {
    m_results.put(description, status);
  }

  /**
   * Gets the result of a check of the supplied description
   * @param description the description
   * @return the return value, or {@code null} if it has not been run
   */
  public PrereqCheckStatus getResult(CheckDescription description) {
    return m_results.get(description);
  }
}
