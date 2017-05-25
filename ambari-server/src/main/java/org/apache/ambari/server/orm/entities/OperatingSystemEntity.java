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
package org.apache.ambari.server.orm.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Emulates entity to provide a quick way to change it to real entity in future.
 */
public class OperatingSystemEntity {

  private String osType;
  private List<RepositoryEntity> repositories = new ArrayList<>();
  private boolean ambariManagedRepos = true;

  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  public List<RepositoryEntity> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<RepositoryEntity> repositories) {
    this.repositories = repositories;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OperatingSystemEntity that = (OperatingSystemEntity) o;

    if (osType != null ? !osType.equals(that.osType) : that.osType != null) return false;

    return true;
  }

  public void setAmbariManagedRepos(boolean managed) {
    ambariManagedRepos = managed;
  }

  /**
   * @return
   */
  public boolean isAmbariManagedRepos() {
    return ambariManagedRepos;
  }

  @Override
  public int hashCode() {
    int result = osType != null ? osType.hashCode() : 0;
    return result;
  }


}
