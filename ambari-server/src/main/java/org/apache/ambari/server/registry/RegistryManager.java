/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.registry;

import java.util.Map;

import org.apache.ambari.server.AmbariException;

/**
 * Provides high-level access to software registries
 */
public interface RegistryManager {

  /**
   * Connect a software registry with provided registry name, type and uri with this Ambari instance
   *
   * @param registryName software registry name
   * @param registryType software registry type
   * @param registryUri software registry uri
   */
  public Registry addRegistry(String registryName, RegistryType registryType, String registryUri)
    throws AmbariException;

  /**
   * Get a software registry given the registry ID
   * @param registryId the registry ID to use to retrieve the software registry
   * @return {@link Registry} identified by the given registry ID
   * @throws AmbariException
   */
  public Registry getRegistry(Long registryId) throws AmbariException;

  /**
   * Get a software registry given the registry name
   * @param registryName the registry name to use to retrieve the software registry
   * @return {@link Registry} identified by the given registry name
   * @throws AmbariException
   */
  public Registry getRegistry(String registryName) throws AmbariException;

  /**
   * Get all software registries associated with this Ambari instance
   * @return {@link Map<Long, Registry>} of all software registries indexed by registry id
   */
  public Map<Long, Registry> getRegistries();
}
