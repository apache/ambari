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

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.view.ResourceProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to the attributes of a view instance.
 */
public class ViewInstanceDefinition {
  /**
   * The associated configuration.
   */
  private final InstanceConfig instanceConfig;

  /**
   * The parent view definition.
   */
  private final ViewDefinition viewDefinition;

  /**
   * The view instance properties.
   */
  private final Map<String, String> properties = new HashMap<String, String>();

  /**
   * The mapping of resource type to resource provider.
   */
  private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();

  /**
   * The mapping of the resource plural name to service.
   */
  private final Map<String, Object> services = new HashMap<String, Object>();

  /**
   * The mapping of servlet name to servlet path spec.
   */
  private final Map<String, String> servletMappings = new HashMap<String, String>();



  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view instance definition.
   *
   * @param viewDefinition  the parent view definition
   * @param instanceConfig  the associated configuration
   */
  public ViewInstanceDefinition(ViewDefinition viewDefinition, InstanceConfig instanceConfig) {
    this.instanceConfig = instanceConfig;
    this.viewDefinition = viewDefinition;
  }


  // ----- ViewInstanceDefinition --------------------------------------------

  /**
   * Get the parent view definition.
   *
   * @return the parent view definition
   */
  public ViewDefinition getViewDefinition() {
    return viewDefinition;
  }

  /**
   * Get the associated configuration.
   *
   * @return the configuration
   */
  public InstanceConfig getConfiguration() {
    return instanceConfig;
  }

  /**
   * Get the view instance name.
   *
   * @return the instance name
   */
  public String getName() {
    return instanceConfig.getName();
  }

  /**
   * Add a mapping from servlet name to path spec.
   *
   * @param servletName  the servlet name
   * @param pathSpec     the path
   */
  public void addServletMapping(String servletName, String pathSpec) {
    servletMappings.put(servletName, pathSpec);
  }

  /**
   * Get the servlet mappings.
   *
   * @return the servlet mappings
   */
  public Map<String, String> getServletMappings() {
    return servletMappings;
  }

  /**
   * Add a view instance property.
   *
   * @param key    the property key
   * @param value  the property value
   */
  public void addProperty(String key, String value) {
    properties.put(key, value);
  }

  /**
   * Get the view instance properties.
   *
   * @return the view instance properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Add a service for the given plural resource name.
   *
   * @param pluralName  the plural resource name
   * @param service     the service
   */
  public void addService(String pluralName, Object service) {
    services.put(pluralName, service);
  }

  /**
   * Get the service associated with the given plural resource name.
   *
   * @param pluralName  the plural resource name
   *
   * @return the service associated with the given name
   */
  public Object getService(String pluralName) {
    return services.get(pluralName);
  }

  /**
   * Add a resource provider for the given resource type.
   *
   * @param type      the resource type
   * @param provider  the resource provider
   */
  public void addResourceProvider(Resource.Type type, ResourceProvider provider) {
    resourceProviders.put(type, provider);
  }

  /**
   * Get the resource provider for the given resource type.
   *
   * @param type  the resource type
   *
   * @return the resource provider
   */
  public ResourceProvider getResourceProvider(Resource.Type type) {
    return resourceProviders.get(type);
  }

  /**
   * Get the resource provider for the given resource type name (scoped to this view).
   *
   * @param type  the resource type name
   *
   * @return the resource provider
   */
  public ResourceProvider getResourceProvider(String type) {
    String typeName = viewDefinition.getName() + "/" + type;
    return resourceProviders.get(Resource.Type.valueOf(typeName));
  }
}
