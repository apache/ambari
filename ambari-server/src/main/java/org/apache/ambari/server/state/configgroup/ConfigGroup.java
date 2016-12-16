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

package org.apache.ambari.server.state.configgroup;

import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ConfigGroupResponse;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;

/**
 * Configuration group or Config group is a type of Ambari resource that
 * supports grouping of configuration resources and host resources for a
 * service, service component or host component.
 */
public interface ConfigGroup {
  /**
   * Primary key of config group
   * @return
   */
  public Long getId();

  /**
   * Unique config group name
   * @return
   */
  public String getName();

  /**
   * Update Group name
   * @param name
   */
  public void setName(String name);

  /**
   * Cluster name to which config group belongs
   * @return
   */
  public String getClusterName();

  /**
   * Tag which associates config group to service
   * @return
   */
  public String getTag();

  /**
   * Update tag
   * @param tag
   */
  public void setTag(String tag);

  /**
   * Config group description
   * @return
   */
  public String getDescription();

  /**
   * Update description
   * @param description
   */
  public void setDescription(String description);

  /**
   * Gets an unmodifiable list of {@link Host}s.
   *
   * @return
   */
  public Map<Long, Host> getHosts();

  /**
   * Gets an unmodifiable map of {@link Config}s.
   *
   * @return
   */
  public Map<String, Config> getConfigurations();

  /**
   * Delete config group and the related host and config mapping
   * entities from the persistence store
   */
  public void delete();

  /**
   * Add host to Config group
   * @param host
   * @throws AmbariException
   */
  public void addHost(Host host) throws AmbariException;

  /**
   * Return @ConfigGroupResponse for the config group
   *
   * @return @ConfigGroupResponse
   * @throws AmbariException
   */
  public ConfigGroupResponse convertToResponse() throws AmbariException;

  /**
   * Reassign the set of hosts associated with this config group
   * @param hosts
   */
  public void setHosts(Map<Long, Host> hosts);

  /**
   * Reassign the set of configs associated with this config group
   * @param configs
   */
  public void setConfigurations(Map<String, Config> configs);

  /**
   * Remove host mapping
   */
  public void removeHost(Long hostId) throws AmbariException;

  /**
   * Name of service which config group is wired to
   */
  String getServiceName();

  void setServiceName(String serviceName);
}
