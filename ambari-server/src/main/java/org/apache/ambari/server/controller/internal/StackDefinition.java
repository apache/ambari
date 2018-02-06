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

package org.apache.ambari.server.controller.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.validators.DependencyAndCardinalityValidator;

/**
 * Encapsulates stack information.
 */
// TODO move to topology package
public interface StackDefinition {

  /**
   * @return the IDs for the set of stacks that this stacks is (possibly) composed of.
   */
  Set<StackId> getStackIds();

  /**
   * @return the IDs of the set of stacks that the given service is defined in
   */
  Set<StackId> getStacksForService(String serviceName);

  /**
   * @return the IDs of the set of stacks that the given component is defined in
   */
  Set<StackId> getStacksForComponent(String componentName);

  /**
   * @return the names of services defined the given stack
   */
  Set<String> getServices(StackId stackId);

  /**
   * Get services contained in the stack.
   *
   * @return collection of all services for the stack
   */
  Collection<String> getServices();

  /**
   * Get components contained in the stack for the specified service.
   *
   * @param service  service name
   *
   * @return collection of component names for the specified service
   */
  Collection<String> getComponents(String service);

  /**
   * Get all service components
   *
   * @return collection of all components for the stack
   */
  Collection<String> getComponents();

  /**
   * Get info for the specified component.
   *
   * @param component  component name
   *
   * @return component information for the requested component
   *         or null if the component doesn't exist in the stack
   */
  ComponentInfo getComponentInfo(String component);

  /**
   * Get all configuration types, including excluded types for the specified service.
   *
   * @param service  service name
   *
   * @return collection of all configuration types for the specified service
   */
  Collection<String> getAllConfigurationTypes(String service);

  /**
   * Get configuration types for the specified service.
   * This doesn't include any service excluded types.
   *
   * @param service  service name
   * @return collection of all configuration types for the specified service
   */
  Collection<String> getConfigurationTypes(String service);

  /**
   * Get the set of excluded configuration types for this service.
   *
   * @param service service name
   * @return Set of names of excluded config types. Will not return null.
   */
  Set<String> getExcludedConfigurationTypes(String service);

  /**
   * Get config properties for the specified service and configuration type.
   *
   * @param service  service name
   * @param type     configuration type
   * @return map of property names to values for the specified service and configuration type
   */
  Map<String, String> getConfigurationProperties(String service, String type);

  /**
   * Get config properties with metadata attributes for the specified service and configuration type.
   *
   * @param service  service name
   * @param type     configuration type
   * @return map of property names to properties for the specified service and configuration type
   */
  Map<String, Stack.ConfigProperty> getConfigurationPropertiesWithMetadata(String service, String type);

  /**
   * Get all required config properties for the specified service.
   *
   * @param service  service name
   * @return collection of all required properties for the given service
   */
  Collection<Stack.ConfigProperty> getRequiredConfigurationProperties(String service);

  /**
   * Get required config properties for the specified service which belong to the specified property type.
   *
   * @param service       service name
   * @param propertyType  property type
   *
   * @return collection of required properties for the given service and property type
   */
  Collection<Stack.ConfigProperty> getRequiredConfigurationProperties(String service, PropertyInfo.PropertyType propertyType);

  /**
   * @return true if the given property for the specified service and config type is a password-type property
   * @see org.apache.ambari.server.state.PropertyInfo.PropertyType#PASSWORD
   */
  boolean isPasswordProperty(String service, String type, String propertyName);

  /**
   * @return map of stack-level property names to properties for the specified configuration type
   */
  Map<String, String> getStackConfigurationProperties(String type);

  /**
   * @return true if the given property for the specified service and config type is a Kerberos principal-type property
   * @see org.apache.ambari.server.state.PropertyInfo.PropertyType#KERBEROS_PRINCIPAL
   */
  boolean isKerberosPrincipalNameProperty(String service, String type, String propertyName);

  /**
   * Get config attributes for the specified service and configuration type.
   *
   * @param service  service name
   * @param type     configuration type
   *
   * @return  map of attribute names to map of property names to attribute values
   *          for the specified service and configuration type
   */
  Map<String, Map<String, String>> getConfigurationAttributes(String service, String type);

  /**
   * Get stack-level config attributes for the specified configuration type.
   *
   * @param type     configuration type
   *
   * @return  map of attribute names to map of property names to attribute values
   *          for the specified configuration type
   */
  Map<String, Map<String, String>> getStackConfigurationAttributes(String type);

  /**
   * Get the service for the specified component.
   *
   * @param component  component name
   *
   * @return service name that contains tha specified component
   */
  String getServiceForComponent(String component);

  /**
   * Get the names of the services which contains the specified components.
   *
   * @param components collection of components
   *
   * @return collection of services which contain the specified components
   */
  Collection<String> getServicesForComponents(Collection<String> components);

  /**
   * Obtain the service name which corresponds to the specified configuration.
   *
   * @param config  configuration type
   *
   * @return name of service which corresponds to the specified configuration type
   */
  String getServiceForConfigType(String config);

  /**
   * @return stream of service names which correspond to the specified configuration type name
   */
  Stream<String> getServicesForConfigType(String config);

  /**
   * Return the dependencies specified for the given component.
   *
   * @param component  component to get dependency information for
   *
   * @return collection of dependency information for the specified component
   */
  //todo: full dependency graph
  Collection<DependencyInfo> getDependenciesForComponent(String component);

  /**
   * Get the custom "descriptor" that is used to decide whether component
   * is a managed or non-managed dependency.  The descriptor is formatted as:
   * "config_type/property_name".  Currently it is only used for Hive Metastore's
   * database.
   *
   * @param component component to get dependency information for
   * @return the descriptor of form "config_type/property_name"
   * @see DependencyAndCardinalityValidator#isDependencyManaged
   */
  String getExternalComponentConfig(String component);

  /**
   * Obtain the required cardinality for the specified component.
   */
  Cardinality getCardinality(String component);

  /**
   * Obtain auto-deploy information for the specified component.
   */
  AutoDeployInfo getAutoDeployInfo(String component);

  /**
   * @return true if the given component is a master component
   */
  boolean isMasterComponent(String component);

  /**
   * @return subset of the stack's configuration for the given services
   */
  Configuration getConfiguration(Collection<String> services);

  /**
   * @return the stack's configuration
   */
  Configuration getConfiguration();

  /**
   * Create a stack definition for one or more stacks.
   * When given multiple stacks, it returns a composite stack,
   * while a single stack is returned as is.
   *
   * @param stacks the stack(s) to combine
   * @return composite or single stack
   */
  static StackDefinition of(Set<Stack> stacks) {
    return stacks.size() > 1
      ? new CompositeStack(stacks)
      : stacks.stream().findAny().orElse(null);
  }

}
