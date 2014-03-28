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

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.view.ResourceProvider;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Represents an instance of a View.
 */
@javax.persistence.IdClass(ViewInstanceEntityPK.class)
@Table(name = "viewinstance")
@NamedQuery(name = "allViewInstances",
    query = "SELECT viewInstance FROM ViewInstanceEntity viewInstance")
@Entity
public class ViewInstanceEntity {
  /**
   * The prefix for every view instance context path.
   */
  public static final String VIEWS_CONTEXT_PATH_PREFIX = "/views/";

  @Id
  @Column(name = "view_name", nullable = false, insertable = false, updatable = false)
  private String viewName;

  /**
   * The instance name.
   */
  @Id
  @Column(name = "name", nullable = false, insertable = true, updatable = false)
  private String name;

  /**
   * The instance properties.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "viewInstance")
  private Collection<ViewInstancePropertyEntity> properties = new HashSet<ViewInstancePropertyEntity>();

  /**
   * The instance data.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "viewInstance")
  private Collection<ViewInstanceDataEntity> data = new HashSet<ViewInstanceDataEntity>();

  @ManyToOne
  @JoinColumn(name = "view_name", referencedColumnName = "view_name", nullable = false)
  private ViewEntity view;


  // ----- transient data ----------------------------------------------------

  /**
   * The associated configuration.  This will be null if the instance was not
   * defined in the archive.
   */
  @Transient
  private final InstanceConfig instanceConfig;

  /**
   * The mapping of resource type to resource provider.  Calculated when the
   * instance is added.
   */
  @Transient
  private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<Resource.Type, ResourceProvider>();

  /**
   * The mapping of the resource plural name to service.  Calculated when the
   * instance is added.
   */
  @Transient
  private final Map<String, Object> services = new HashMap<String, Object>();


  // ----- Constructors ------------------------------------------------------

  public ViewInstanceEntity() {
    instanceConfig = null;
  }

  /**
   * Construct a view instance definition.
   *
   * @param view  the parent view definition
   * @param instanceConfig  the associated configuration
   */
  public ViewInstanceEntity(ViewEntity view, InstanceConfig instanceConfig) {
    this.name           = instanceConfig.getName();
    this.instanceConfig = instanceConfig;
    this.view           = view;
    this.viewName       = view.getName();
  }

  /**
   * Construct a view instance definition.
   *
   * @param view  the parent view definition
   * @param name  the instance name
   */
  public ViewInstanceEntity(ViewEntity view, String name) {
    this.name           = name;
    this.instanceConfig = null;
    this.view           = view;
    this.viewName       = view.getName();
  }


  // ----- ViewInstanceEntity ------------------------------------------------

  /**
   * Get the view name.
   *
   * @return the view name
   */
  public String getViewName() {
    return viewName;
  }

  /**
   * Set the view name.
   *
   * @param viewName  the view name
   */
  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  /**
   * Get the name of this instance.
   *
   * @return the instance name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of this instance.
   *
   * @param name  the instance name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the instance properties.
   *
   * @return the instance properties
   */
  public Collection<ViewInstancePropertyEntity> getProperties() {
    return properties;
  }

  /**
   * Get the instance property map.
   *
   * @return the map of instance properties
   */
  public Map<String, String> getPropertyMap() {
    Map<String, String> propertyMap = new HashMap<String, String>();

    for (ViewInstancePropertyEntity viewInstancePropertyEntity : properties) {
      propertyMap.put(viewInstancePropertyEntity.getName(), viewInstancePropertyEntity.getValue());
    }
    return propertyMap;
  }

  /**
   * Add a property value to this instance.
   *
   * @param key    the property key
   * @param value  the property value
   */
  public void putProperty(String key, String value) {
    removeInstanceData(key);
    ViewInstancePropertyEntity viewInstancePropertyEntity = new ViewInstancePropertyEntity();
    viewInstancePropertyEntity.setViewName(viewName);
    viewInstancePropertyEntity.setViewInstanceName(name);
    viewInstancePropertyEntity.setName(key);
    viewInstancePropertyEntity.setValue(value);
    viewInstancePropertyEntity.setViewInstanceEntity(this);
    properties.add(viewInstancePropertyEntity);
  }

  /**
   * Remove the property identified by the given key from this instance.
   *
   * @param key  the key
   */
  public void removeProperty(String key) {
    ViewInstancePropertyEntity entity = getProperty(key);
    if (entity != null) {
      properties.remove(entity);
    }
  }

  /**
   * Get the instance property entity for the given key.
   *
   * @param key  the key
   *
   * @return the instance property entity identified by the given key
   */
  public ViewInstancePropertyEntity getProperty(String key) {
    for (ViewInstancePropertyEntity viewInstancePropertyEntity : properties) {
      if (viewInstancePropertyEntity.getName().equals(key)) {
        return viewInstancePropertyEntity;
      }
    }
    return null;
  }

  /**
   * Set the collection of instance property entities.
   *
   * @param properties  the collection of instance property entities
   */
  public void setProperties(Collection<ViewInstancePropertyEntity> properties) {
    this.properties = properties;
  }

  /**
   * Get the instance data.
   *
   * @return the instance data
   */
  public Collection<ViewInstanceDataEntity> getData() {
    return data;
  }

  /**
   * Set the collection of instance data entities.
   *
   * @param data  the collection of instance data entities
   */
  public void setData(Collection<ViewInstanceDataEntity> data) {
    this.data = data;
  }

  /**
   * Get the view instance application data.
   *
   * @return the view instance application data map
   */
  public Map<String, String> getInstanceDataMap() {
    Map<String, String> applicationData = new HashMap<String, String>();

    for (ViewInstanceDataEntity viewInstanceDataEntity : data) {
      applicationData.put(viewInstanceDataEntity.getName(), viewInstanceDataEntity.getValue());
    }
    return applicationData;
  }

  /**
   * Associate the given instance data value with the given key.
   *
   * @param key    the key
   * @param value  the value
   */
  public void putInstanceData(String key, String value) {
    removeInstanceData(key);
    ViewInstanceDataEntity viewInstanceDataEntity = new ViewInstanceDataEntity();
    viewInstanceDataEntity.setViewName(viewName);
    viewInstanceDataEntity.setViewInstanceName(name);
    viewInstanceDataEntity.setName(key);
    viewInstanceDataEntity.setValue(value);
    viewInstanceDataEntity.setViewInstanceEntity(this);
    data.add(viewInstanceDataEntity);
  }

  /**
   * Remove the instance data entity associated with the given key.
   *
   * @param key  the key
   */
  public void removeInstanceData(String key) {
    ViewInstanceDataEntity entity = getInstanceData(key);
    if (entity != null) {
      data.remove(entity);
    }
  }

  /**
   * Get the instance data entity for the given key.
   *
   * @param key  the key
   *
   * @return the instance data entity associated with the given key
   */
  public ViewInstanceDataEntity getInstanceData(String key) {
    for (ViewInstanceDataEntity viewInstanceDataEntity : data) {
      if (viewInstanceDataEntity.getName().equals(key)) {
        return viewInstanceDataEntity;
      }
    }
    return null;
  }

  /**
   * Get the parent view entity.
   *
   * @return the parent view entity
   */
  public ViewEntity getViewEntity() {
    return view;
  }

  /**
   * Set the parent view entity.
   *
   * @param view  the parent view entity
   */
  public void setViewEntity(ViewEntity view) {
    this.view = view;
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
    String typeName = view.getName() + "/" + type;
    return resourceProviders.get(Resource.Type.valueOf(typeName));
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
   * Get the context path for the UI for this view.
   *
   * @return the context path
   */
  public String getContextPath() {
    return getContextPath(view.getName(), getName());
  }

  /**
   * Get the context path for a view instance with the given names.
   *
   * @param viewName          the view name
   * @param viewInstanceName  the instance name
   *
   * @return the context path
   */
  public static String getContextPath(String viewName, String viewInstanceName) {
    return VIEWS_CONTEXT_PATH_PREFIX + viewName + "/" + viewInstanceName;
  }
}
