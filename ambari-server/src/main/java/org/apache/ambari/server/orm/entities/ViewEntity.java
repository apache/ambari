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

package org.apache.ambari.server.orm.entities;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.view.ViewSubResourceDefinition;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Entity representing a View.
 */
@Table(name = "viewmain")
@NamedQuery(name = "allViews",
    query = "SELECT view FROM ViewEntity view")
@Entity
public class ViewEntity {
  /**
   * The unique view name.
   */
  @Id
  @Column(name = "view_name", nullable = false, insertable = true,
      updatable = false, unique = true, length = 100)
  private String name;

  /**
   * The public view name.
   */
  @Column
  @Basic
  private String label;

  /**
   * The view version.
   */
  @Column
  @Basic
  private String version;

  /**
   * The view archive.
   */
  @Column
  @Basic
  private String archive;

  /**
   * The list of view parameters.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "view")
  private Collection<ViewParameterEntity> parameters = new HashSet<ViewParameterEntity>();

  /**
   * The list of view resources.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "view")
  private Collection<ViewResourceEntity> resources = new HashSet<ViewResourceEntity>();

  /**
   * The list of view instances.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "view")
  private Collection<ViewInstanceEntity> instances = new HashSet<ViewInstanceEntity>();


  // ----- Transient data ----------------------------------------------------

  /**
   * The associated view configuration.
   */
  @Transient
  private final ViewConfig configuration;

  /**
   * The Ambari configuration properties.
   */
  @Transient
  private final Configuration ambariConfiguration;

  /**
   * The external resource type for the view.
   */
  @Transient
  private final Resource.Type externalResourceType;

  /**
   * The classloader used to load the view.
   */
  @Transient
  private final ClassLoader classLoader;

  /**
   * The mapping of resource type to resource provider.
   */
  @Transient
  private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();

  /**
   * The mapping of resource type to resource definition.
   */
  @Transient
  private final Map<Resource.Type, ViewSubResourceDefinition> resourceDefinitions = new HashMap<Resource.Type, ViewSubResourceDefinition>();

  /**
   * The mapping of resource type to resource configuration.
   */
  @Transient
  private final Map<Resource.Type, ResourceConfig> resourceConfigurations = new HashMap<Resource.Type, ResourceConfig>();


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view entity.
   */
  public ViewEntity() {
    this.configuration        = null;
    this.ambariConfiguration  = null;
    this.classLoader          = null;
    this.archive              = null;
    this.externalResourceType = null;
  }

  /**
   * Construct a view entity from the given configuration.
   *
   * @param configuration        the view configuration
   * @param ambariConfiguration  the Ambari configuration
   * @param classLoader          the class loader
   * @param archivePath          the path of the view archive
   */
  public ViewEntity(ViewConfig configuration, Configuration ambariConfiguration,
                        ClassLoader classLoader, String archivePath) {
    this.configuration       = configuration;
    this.ambariConfiguration = ambariConfiguration;
    this.classLoader         = classLoader;
    this.archive             = archivePath;

    this.name    = configuration.getName();
    this.label   = configuration.getLabel();
    this.version = configuration.getVersion();

    this.externalResourceType =
        new Resource.Type(getQualifiedResourceTypeName(ResourceConfig.EXTERNAL_RESOURCE_PLURAL_NAME));
  }


  // ----- ViewEntity --------------------------------------------------------

  /**
   * Get the view name.
   *
   * @return the view name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the view name.
   *
   * @param name the view name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the view label (display name).
   *
   * @return the view label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Set the view label (display name).
   *
   * @param label  the view label
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Get the view version.
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Set the view version.
   *
   * @param version  the version
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Get the view parameters.
   *
   * @return the view parameters
   */
  public Collection<ViewParameterEntity> getParameters() {
    return parameters;
  }

  /**
   * Set the view parameters.
   *
   * @param parameters  the view parameters
   */
  public void setParameters(Collection<ViewParameterEntity> parameters) {
    this.parameters = parameters;
  }

  /**
   * Get the view resources.
   *
   * @return the view resources
   */
  public Collection<ViewResourceEntity> getResources() {
    return resources;
  }

  /**
   * Set the view resources.
   *
   * @param resources  the view resources
   */
  public void setResources(Collection<ViewResourceEntity> resources) {
    this.resources = resources;
  }

  /**
   * Get the view instances.
   *
   * @return the view instances
   */
  public Collection<ViewInstanceEntity> getInstances() {
    return instances;
  }

  /**
   * Set the view instances.
   *
   * @param instances  the instances
   */
  public void setInstances(Collection<ViewInstanceEntity> instances) {
    this.instances = instances;
  }

  /**
   * Add an instance definition.
   *
   * @param viewInstanceDefinition  the instance definition
   */
  public void addInstanceDefinition(ViewInstanceEntity viewInstanceDefinition) {
    removeInstanceDefinition(viewInstanceDefinition.getName());
    instances.add(viewInstanceDefinition);
  }

  /**
   * Remove an instance definition.
   *
   * @param instanceName  the instance name
   */
  public void removeInstanceDefinition(String instanceName) {
    ViewInstanceEntity entity = getInstanceDefinition(instanceName);
    if (entity != null) {
      instances.remove(entity);
    }
  }

  /**
   * Get an instance definition for the given name.
   *
   * @param instanceName  the instance name
   *
   * @return the instance definition
   */
  public ViewInstanceEntity getInstanceDefinition(String instanceName) {
    for (ViewInstanceEntity viewInstanceEntity : instances) {
      if (viewInstanceEntity.getName().equals(instanceName)) {
        return viewInstanceEntity;
      }
    }
    return null;
  }

  /**
   * Get the path of the view archive.
   *
   * @return  the path of the view archive
   */
  public String getArchive() {
    return archive;
  }

  /**
   * Set the view archive path.
   *
   * @param archive  the view archive path
   */
  public void setArchive(String archive) {
    this.archive = archive;
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
   * Get a resource name qualified by the associated view name.
   *
   * @param resourceTypeName  the resource type name
   *
   * @return the qualified resource name
   */
  public String getQualifiedResourceTypeName(String resourceTypeName) {
    return getName() + "/" + resourceTypeName;
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
   * Get the class loader used to load the view classes.
   *
   * @return the class loader
   */
  public ClassLoader getClassLoader() {
    return classLoader;
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
   * Get the associated view configuration.
   *
   * @return the configuration
   */
  public ViewConfig getConfiguration() {
    return configuration;
  }
}
