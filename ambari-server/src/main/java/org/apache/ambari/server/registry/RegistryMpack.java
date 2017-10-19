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
package org.apache.ambari.server.registry;

import java.util.List;

import org.apache.ambari.server.AmbariException;

/**
 * Represents a single instance of a software registry
 */
public interface RegistryMpack {
  /**
   * Get mpack name
   * @return
   */
  public String getMpackName();

  /**
   * Get mpack display name
   * @return
   */
  public String getMpackDisplayName();

  /**
   * Get mpack description
   * @return
   */
  public String getMpackDescription();

  /**
   * Get mpack logo url
   * @return
   */
  public String getMpackLogoUrl();

  /**
   * Get list of mpack versions
   * @return
   */
  public List<? extends RegistryMpackVersion> getMpackVersions();

  /**
   * Get specific mpack version
   * @return {@link RegistryMpackVersion}
   */
  public RegistryMpackVersion getMpackVersion(String mpackVersion) throws AmbariException;
}
