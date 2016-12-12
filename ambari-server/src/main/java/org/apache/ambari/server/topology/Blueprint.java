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

package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.Map;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.entities.BlueprintEntity;

/**
 * Blueprint representation.
 */
public interface Blueprint {

  /**
   * Get the name of the blueprint.
   *
   * @return blueprint name
   */
  public String getName();

  /**
   * Get the hot groups contained in the blueprint.
   * @return map of host group name to host group
   */
  public Map<String, HostGroup> getHostGroups();

  /**
   * Get a hostgroup specified by name.
   *
   * @param name  name of the host group to get
   *
   * @return the host group with the given name or null
   */
  public HostGroup getHostGroup(String name);

  /**
   * Get the Blueprint cluster scoped configuration.
   * The blueprint cluster scoped configuration has the stack
   * configuration with the config types associated with the blueprint
   * set as it's parent.
   *
   * @return blueprint cluster scoped configuration
   */
  public Configuration getConfiguration();

  /**
   * Get the Blueprint cluster scoped setting.
   * The blueprint cluster scoped setting has the setting properties
   * with the setting names associated with the blueprint.
   *
   * @return blueprint cluster scoped setting
   */
  public Setting getSetting();

  /**
   * Get all of the services represented in the blueprint.
   *
   * @return collection of all represented service names
   */
  public Collection<String> getServices();

  /**
   * Get the components that are included in the blueprint for the specified service.
   *
   * @param service  service name
   *
   * @return collection of component names for the service.  Will not return null.
   */
  public Collection<String> getComponents(String service);

  /**
   * Get whether a component is enabled for auto start.
   *
   * @param serviceName - Service name.
   * @param componentName - Component name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  public String getRecoveryEnabled(String serviceName, String componentName);

  /**
   * Get whether a service is enabled for credential store use.
   *
   * @param serviceName - Service name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  public String getCredentialStoreEnabled(String serviceName);

  /**
   * Check if auto skip failure is enabled.
   * @return true if enabled, otherwise false.
   */
  public boolean shouldSkipFailure();

  /**
   * Get the stack associated with the blueprint.
   *
   * @return associated stack
   */
  public Stack getStack();

  /**
   * Get the host groups which contain components for the specified service.
   *
   * @param service  service name
   *
   * @return collection of host groups containing components for the specified service;
   *         will not return null
   */
  public Collection<HostGroup> getHostGroupsForService(String service);

  /**
   * Get the host groups which contain the give component.
   *
   * @param component  component name
   *
   * @return collection of host groups containing the specified component; will not return null
   */
  public Collection<HostGroup> getHostGroupsForComponent(String component);

  public SecurityConfiguration getSecurity();

  /**
   * Validate the blueprint topology.
   *
   * @throws InvalidTopologyException if the topology is invalid
   */
  public void validateTopology() throws InvalidTopologyException;

  /**
   * Validate that the blueprint contains all of the required properties.
   *
   * @throws InvalidTopologyException if the blueprint doesn't contain all required properties
   */
  public void validateRequiredProperties() throws InvalidTopologyException;

  /**
   * Obtain the blueprint as an entity.
   *
   * @return entity representation of the blueprint
   */
  public BlueprintEntity toEntity();
}
