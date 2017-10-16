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

package org.apache.ambari.server.topology;

import org.apache.ambari.server.controller.internal.StackV2;
import org.apache.ambari.server.orm.entities.BlueprintEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Blueprint representation.
 */
public interface BlueprintV2 {

  /**
   * Get the name of the blueprint.
   *
   * @return blueprint name
   */
  String getName();

  /**
   * Get a hostgroup specified by name.
   *
   * @param name  name of the host group to get
   *
   * @return the host group with the given name or null
   */
  HostGroupV2 getHostGroup(String name);

  /**
   * Get the hot groups contained in the blueprint.
   * @return map of host group name to host group
   */
  Map<String, HostGroupV2> getHostGroups();

  /**
   * Get  stacks associated with the blueprint.
   *
   * @return associated stacks
   */
  Collection<StackV2> getStacks();


  Collection<ServiceGroup> getServiceGroups();

  /**
   * Get all of the services represented in the blueprint.
   *
   * @return collection of all represented service names
   */
  Collection<Service> getAllServices();


  /**
   * Get all of the service types represented in the blueprint.
   *
   * @return collection of all represented service types
   */
  Collection<String> getAllServiceTypes();

  /**
   * Get all of the services represented in the blueprint with a given type.
   *
   * @return collection of all represented services represented in the blueprint with a given type.
   */
  Collection<Service> getServicesByType(String serviceType);

  /**
   * Get services by type from a service group.
   * @param serviceGroup
   * @param serviceType
   * @return
   */
  Collection<Service> getServicesFromServiceGroup(ServiceGroup serviceGroup, String serviceType);

  /**
   * Get the components that are included in the blueprint for the specified service.
   *
   * @param service  service name
   *
   * @return collection of component names for the service.  Will not return null.
   */
  Collection<ComponentV2> getComponents(Service service);

  /**
   * Get components by type from a service.
   * @param service
   * @param componentType
   * @return
   */
  Collection<ComponentV2> getComponentsByType(Service service, String componentType);


  /**
   * Get the host groups which contain components for the specified service.
   *
   * @param service  service name
   *
   * @return collection of host groups containing components for the specified service;
   *         will not return null
   */
  Collection<HostGroupV2> getHostGroupsForService(Service service);

  /**
   * Get the host groups which contain the give component.
   *
   * @param component  component name
   *
   * @return collection of host groups containing the specified component; will not return null
   */
  Collection<HostGroupV2> getHostGroupsForComponent(ComponentV2 component);

  /**
   * Get the Blueprint cluster scoped configuration.
   * The blueprint cluster scoped configuration has the stack
   * configuration with the config types associated with the blueprint
   * set as it's parent.
   *
   * @return blueprint cluster scoped configuration
   */
  @Deprecated
  Configuration getConfiguration();

  /**
   * Get the Blueprint cluster scoped setting.
   * The blueprint cluster scoped setting has the setting properties
   * with the setting names associated with the blueprint.
   *
   * @return blueprint cluster scoped setting
   */
  Setting getSetting();


  /**
   * Get whether a component is enabled for auto start.
   *
   * @param component - Component.
   *
   * @return null if value is not specified; true or false if specified.
   */
  String getRecoveryEnabled(ComponentV2 component);

  /**
   * Get whether a service is enabled for credential store use.
   *
   * @param serviceName - Service name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  String getCredentialStoreEnabled(String serviceName);

  /**
   * Check if auto skip failure is enabled.
   * @return true if enabled, otherwise false.
   */
  boolean shouldSkipFailure();


  SecurityConfiguration getSecurity();

  /**
   * Validate the blueprint topology.
   *
   * @throws InvalidTopologyException if the topology is invalid
   */
  void validateTopology() throws InvalidTopologyException;

  /**
   * Validate that the blueprint contains all of the required properties.
   *
   * @throws InvalidTopologyException if the blueprint doesn't contain all required properties
   */
  void validateRequiredProperties() throws InvalidTopologyException;

  /**
   *
   * A config type is valid if there are services related to except cluster-env and global.
   * @param configType
   * @return
   */
  boolean isValidConfigType(String configType);

  /**
   * Obtain the blueprint as an entity.
   *
   * @return entity representation of the blueprint
   */
  BlueprintEntity toEntity();

  List<RepositorySetting> getRepositorySettings();
}
