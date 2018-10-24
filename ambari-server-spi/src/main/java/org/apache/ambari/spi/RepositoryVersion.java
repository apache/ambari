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
package org.apache.ambari.spi;

/**
 * A simple POJO to encapslate information about a repository.
 */
public class RepositoryVersion {

  private final long m_id;
  private final String m_stackId;
  private final String m_version;
  private final RepositoryType m_repositoryType;

  /**
   * Constructor.
   *
   * @param id
   *          the internal ID of the repository stored in Ambari.
   * @param stackId
   *          the stack ID, such as STACK-1.0.0
   * @param version
   *          the specific version of the stack, such as 1.0.1-b2
   * @param repositoryType
   *          the type of repository.
   */
  public RepositoryVersion(long id, String stackId, String version, RepositoryType repositoryType) {
    m_id = id;
    m_stackId = stackId;
    m_version = version;
    m_repositoryType = repositoryType;
  }

  /**
   * Gets the internal ID of the repository version stored in Ambari's database.
   *
   * @return
   */
  public long getId() {
    return m_id;
  }

  /**
   * Gets the ID of the stack, such as STACK-1.0.0
   *
   * @return the stack id.
   */
  public String getStackId() {
    return m_stackId;
  }

  /**
   * Gets the version of the repository, such as 1.0.0-b2
   *
   * @return the version of the repository.
   */
  public String getVersion() {
    return m_version;
  }

  /**
   * Gets the type of repository for the upgrade.
   *
   * @return  the type of repository.
   */
  public RepositoryType getRepositoryType() {
    return m_repositoryType;
  }
}
