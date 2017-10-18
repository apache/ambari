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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RepositoryVersion {
  @JsonProperty("stack_id")
  private String stackId;

  @JsonProperty("repository_version")
  private String repositoryVersion;

  public RepositoryVersion() { }

  public RepositoryVersion(String stackId, String repositoryVersion) {
    this.stackId = stackId;
    this.repositoryVersion = repositoryVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RepositoryVersion that = (RepositoryVersion) o;

    if (stackId != null ? !stackId.equals(that.stackId) : that.stackId != null) return false;
    return repositoryVersion != null ? repositoryVersion.equals(that.repositoryVersion) : that.repositoryVersion == null;
  }

  @Override
  public int hashCode() {
    int result = stackId != null ? stackId.hashCode() : 0;
    result = 31 * result + (repositoryVersion != null ? repositoryVersion.hashCode() : 0);
    return result;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public String getRepositoryVersion() {
    return repositoryVersion;
  }

  public void setRepositoryVersion(String repositoryVersion) {
    this.repositoryVersion = repositoryVersion;
  }
}
