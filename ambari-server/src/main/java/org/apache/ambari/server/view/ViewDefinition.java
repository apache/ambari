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

package org.apache.ambari.server.view;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides access to the attributes of a view.
 */
public class ViewDefinition {
  /**
   * The associated view configuration.
   */
  private final ViewConfig configuration;

  /**
   * The mapping of resource type to resource provider.
   */
  private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();

  /**
   * The mapping of resource type to resource definition.
   */
  private final Map<Resource.Type, ViewSubResourceDefinition> resourceDefinitions = new HashMap<Resource.Type, ViewSubResourceDefinition>();

  /**
   * The mapping of resource type to resource configuration.
   */
  private final Map<Resource.Type, ResourceConfig> resourceConfigurations = new HashMap<Resource.Type, ResourceConfig>();

  /**
   * The mapping of instance name to instance definition.
   */
  private final Map<String, ViewInstanceDefinition> instanceDefinitions = new HashMap<String, ViewInstanceDefinition>();

  /**
   * The Ambari configuration properties.
   */
  private final Configuration ambariConfiguration;

  /**
   * The external resource type for the view.
   */
  private final Resource.Type externalResourceType;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view definition from the given configuration.
   *
   * @param configuration        the view configuration
   * @param ambariConfiguration  the Ambari configuration
   */
  public ViewDefinition(ViewConfig configuration, Configuration ambariConfiguration) {
    this.configuration       = configuration;
    this.ambariConfiguration = ambariConfiguration;

    this.externalResourceType =
        new Resource.Type(getQualifiedResourceTypeName(ResourceConfig.EXTERNAL_RESOURCE_PLURAL_NAME));
  }


  // ----- ViewDefinition ----------------------------------------------------

  /**
   * Get the view name.
   *
   * @return the view name
   */
  public String getName() {
    return configuration.getName();
  }

  /**
   * Get the view label.
   *
   * @return the view label
   */
  public String getLabel() {
    return configuration.getLabel();
  }

  /**
   * Get the view version.
   *
   * @return the version
   */
  public String getVersion() {
    return configuration.getVersion();
  }

  /**
   * Get the associated view configuration.
   *
   * @return the configuration
   */
  public ViewConfig getConfiguration() {
    return configuration;
  }

  /**
   * Get a property for the given key from the ambari configuration.
   *
   * @param key  the property key
   *
   * @return the property value; null indicates that the configuration contains no mapping for the key
   */
  public String getAmbariProperty(String key) {
    return ambariConfiguration.getProperty(key);
  }

  /**
   * Add a resource provider for the given type.
   *
   * @param type      the resource type
   * @param provider  the resource provider
   */
  public void addResourceProvider(Resource.Type type, ResourceProvider provider) {
    resourceProviders.put(type, provider);
  }

  /**
   * Get the resource provider for the given type.
   *
   * @param type  the resource type
   *
   * @return the resource provider associated with the given type
   */
  public ResourceProvider getResourceProvider(Resource.Type type) {
    return resourceProviders.get(type);
  }

  /**
   * Add a resource definition.
   *
   * @param definition  the resource definition
   */
  public void addResourceDefinition(ViewSubResourceDefinition definition) {
    resourceDefinitions.put(definition.getType(), definition);
  }

  /**
   * Get the resource definition for the given type.
   *
   * @param type  the resource type
   *
   * @return the resource definition associated with the given type
   */
  public ViewSubResourceDefinition getResourceDefinition(Resource.Type type) {
    return resourceDefinitions.get(type);
  }

  /**
   * Get the mapping of resource type to resource definitions.
   *
   * @return the mapping of resource type to resource definitions
   */
  public Map<Resource.Type, ViewSubResourceDefinition> getResourceDefinitions() {
    return resourceDefinitions;
  }

  /**
   * Add a resource configuration for the given type.
   *
   * @param type    the resource type
   * @param config  the configuration
   */
  public void addResourceConfiguration(Resource.Type type, ResourceConfig config) {
    resourceConfigurations.put(type, config);
  }

  /**
   * Get the mapping of resource type to resource configurations.
   *
   * @return the mapping of resource types to resource configurations
   */
  public Map<Resource.Type, ResourceConfig> getResourceConfigurations() {
    return resourceConfigurations;
  }

  /**
   * Get the set of resource types for this view.
   *
   * @return the set of resource type
   */
  public Set<Resource.Type> getViewResourceTypes() {
    return resourceProviders.keySet();
  }

  /**
   * Add an instance definition.
   *
   * @param viewInstanceDefinition  the instance definition
   */
  public void addInstanceDefinition(ViewInstanceDefinition viewInstanceDefinition) {
    instanceDefinitions.put(viewInstanceDefinition.getName(), viewInstanceDefinition);
  }

  /**
   * Get the collection of all instance definitions for this view.
   *
   * @return the collection of instance definitions
   */
  public Collection<ViewInstanceDefinition> getInstanceDefinitions() {
    return instanceDefinitions.values();
  }

  /**
   * Get an instance definition for the given name.
   *
   * @param instanceName  the instance name
   *
   * @return the instance definition
   */
  public ViewInstanceDefinition getInstanceDefinition(String instanceName) {
    return instanceDefinitions.get(instanceName);
  }

  /**
   * Get the external resource type for the view.
   *
   * @return the external resource type
   */
  public Resource.Type getExternalResourceType() {
    return externalResourceType;
  }

  /**
   * Get a resource name qualified by the associated view name.
   *
   * @param resourceTypeName  the resource type name
   *
   * @return the qualified resource name
   */
  public String getQualifiedResourceTypeName(String resourceTypeName) {
    return configuration.getName() + "/" + resourceTypeName;
  }
}
