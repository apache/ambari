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
package org.apache.ambari.server.agent.stomp.dto;

import java.util.List;

import org.apache.ambari.server.agent.RecoveryConfig;
import org.apache.ambari.server.state.RepositoryInfo;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HostLevelParamsCluster {

  private List<RepositoryInfo> repoInfo;
  private RecoveryConfig recoveryConfig;

  public HostLevelParamsCluster(List<RepositoryInfo> repoInfo, RecoveryConfig recoveryConfig) {
    this.repoInfo = repoInfo;
    this.recoveryConfig = recoveryConfig;
  }

  public List<RepositoryInfo> getRepoInfo() {
    return repoInfo;
  }

  public void setRepoInfo(List<RepositoryInfo> repoInfo) {
    this.repoInfo = repoInfo;
  }

  public RecoveryConfig getRecoveryConfig() {
    return recoveryConfig;
  }

  public void setRecoveryConfig(RecoveryConfig recoveryConfig) {
    this.recoveryConfig = recoveryConfig;
  }
}
