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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstancePropertyEntity;
import org.apache.ambari.server.view.ViewRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for view instances.
 */
public class ViewInstanceResourceProvider extends AbstractResourceProvider {

  /**
   * View instance property id constants.
   */
  public static final String VIEW_NAME_PROPERTY_ID     = "ViewInstanceInfo/view_name";
  public static final String INSTANCE_NAME_PROPERTY_ID = "ViewInstanceInfo/instance_name";
  public static final String PROPERTIES_PROPERTY_ID    = "ViewInstanceInfo/properties";
  public static final String DATA_PROPERTY_ID          = "ViewInstanceInfo/instance_data";
  public static final String CONTEXT_PATH_PROPERTY_ID  = "ViewInstanceInfo/context_path";

  /**
   * Property prefix values.
   */
  private static final String PROPERTIES_PREFIX = PROPERTIES_PROPERTY_ID + "/";
  private static final String DATA_PREFIX       = DATA_PROPERTY_ID + "/";

  /**
   * The key property ids for a view instance resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();
  static {
    keyPropertyIds.put(Resource.Type.View, VIEW_NAME_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.ViewInstance, INSTANCE_NAME_PROPERTY_ID);
  }

  /**
   * The property ids for a view instance resource.
   */
  private static Set<String> propertyIds = new HashSet<String>();
  static {
    propertyIds.add(VIEW_NAME_PROPERTY_ID);
    propertyIds.add(INSTANCE_NAME_PROPERTY_ID);
    propertyIds.add(PROPERTIES_PROPERTY_ID);
    propertyIds.add(DATA_PROPERTY_ID);
    propertyIds.add(CONTEXT_PATH_PROPERTY_ID);
  }

  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view instance resource provider.
   */
  public ViewInstanceResourceProvider() {
    super(propertyIds, keyPropertyIds);
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException,
             ResourceAlreadyExistsException, NoSuchParentResourceException {
    for (Map<String, Object> properties : request.getProperties()) {
      createResources(getCreateCommand(properties));
    }
    notifyCreate(Resource.Type.ViewInstance, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources    = new HashSet<Resource>();
    ViewRegistry  viewRegistry = ViewRegistry.getInstance();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);

    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);
    if (propertyMaps.isEmpty()) {
      propertyMaps.add(Collections.<String, Object>emptyMap());
    }

    for (Map<String, Object> propertyMap : propertyMaps) {

      String viewName = (String) propertyMap.get(VIEW_NAME_PROPERTY_ID);
      String instanceName = (String) propertyMap.get(INSTANCE_NAME_PROPERTY_ID);

      for (ViewEntity viewDefinition : viewRegistry.getDefinitions()){
        if (viewName == null || viewName.equals(viewDefinition.getName())) {
          for (ViewInstanceEntity viewInstanceDefinition : viewRegistry.getInstanceDefinitions(viewDefinition)) {
            if (instanceName == null || instanceName.equals(viewInstanceDefinition.getName())) {
              Resource resource = toResource(viewInstanceDefinition, requestedIds);
              resources.add(resource);
            }
          }
        }
      }
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        modifyResources(getUpdateCommand(propertyMap));
      }
    }
    notifyUpdate(Resource.Type.ViewInstance, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    modifyResources(getDeleteCommand(predicate));
    notifyDelete(Resource.Type.ViewInstance, predicate);
    return getRequestStatus(null);
  }

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<String>(keyPropertyIds.values());
  }


  // ----- helper methods ----------------------------------------------------

  // Convert an instance entity to a resource
  private Resource toResource(ViewInstanceEntity viewInstanceEntity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.ViewInstance);

    String viewName = viewInstanceEntity.getViewName();
    String name     = viewInstanceEntity.getName();

    setResourceProperty(resource, VIEW_NAME_PROPERTY_ID, viewName, requestedIds);
    setResourceProperty(resource, INSTANCE_NAME_PROPERTY_ID, name, requestedIds);
    Map<String, String> properties = new HashMap<String, String>();

    for (ViewInstancePropertyEntity viewInstancePropertyEntity : viewInstanceEntity.getProperties()) {
      properties.put(viewInstancePropertyEntity.getName(), viewInstancePropertyEntity.getValue());
    }
    setResourceProperty(resource, PROPERTIES_PROPERTY_ID,
        properties, requestedIds);
    Map<String, String> applicationData = new HashMap<String, String>();

    for (ViewInstanceDataEntity viewInstanceDataEntity : viewInstanceEntity.getData()) {
      applicationData.put(viewInstanceDataEntity.getName(), viewInstanceDataEntity.getValue());
    }
    setResourceProperty(resource, DATA_PROPERTY_ID,
        applicationData, requestedIds);
    setResourceProperty(resource, CONTEXT_PATH_PROPERTY_ID,
        ViewInstanceEntity.getContextPath(viewName, name), requestedIds);

    return resource;
  }

  // Convert a map of properties to a view instance entity.
  private ViewInstanceEntity toEntity(Map<String, Object> properties) {
    String name     = (String) properties.get(INSTANCE_NAME_PROPERTY_ID);
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("View instance name must be provided");
    }

    String viewName = (String) properties.get(VIEW_NAME_PROPERTY_ID);
    if (viewName == null || viewName.isEmpty()) {
      throw new IllegalArgumentException("View name must be provided");
    }

    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setName(name);
    viewInstanceEntity.setViewName(viewName);


    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setName(viewName);

    viewInstanceEntity.setViewEntity(viewEntity);

    Collection<ViewInstancePropertyEntity> instanceProperties = new HashSet<ViewInstancePropertyEntity>();
    Collection<ViewInstanceDataEntity>     instanceData       = new HashSet<ViewInstanceDataEntity>();

    for (Map.Entry<String, Object> entry : properties.entrySet()) {

      String propertyName = entry.getKey();

      if (propertyName.startsWith(PROPERTIES_PREFIX)) {
        ViewInstancePropertyEntity viewInstancePropertyEntity = new ViewInstancePropertyEntity();

        viewInstancePropertyEntity.setViewName(viewName);
        viewInstancePropertyEntity.setViewInstanceName(name);
        viewInstancePropertyEntity.setName(entry.getKey().substring(PROPERTIES_PREFIX.length()));
        viewInstancePropertyEntity.setValue((String) entry.getValue());
        viewInstancePropertyEntity.setViewInstanceEntity(viewInstanceEntity);

        instanceProperties.add(viewInstancePropertyEntity);
      } else if (propertyName.startsWith(DATA_PREFIX)) {
        ViewInstanceDataEntity viewInstanceDataEntity = new ViewInstanceDataEntity();

        viewInstanceDataEntity.setViewName(viewName);
        viewInstanceDataEntity.setViewInstanceName(name);
        viewInstanceDataEntity.setName(entry.getKey().substring(DATA_PREFIX.length()));
        viewInstanceDataEntity.setValue((String) entry.getValue());
        viewInstanceDataEntity.setViewInstanceEntity(viewInstanceEntity);

        instanceData.add(viewInstanceDataEntity);
      }
    }
    viewInstanceEntity.setProperties(instanceProperties);
    viewInstanceEntity.setData(instanceData);

    return viewInstanceEntity;
  }

  // Create a create command with all properties set.
  private Command<Void> getCreateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        ViewRegistry.getInstance().installViewInstance(toEntity(properties));
        return null;
      }
    };
  }

  // Create an update command with all properties set.
  private Command<Void> getUpdateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        ViewRegistry.getInstance().updateViewInstance(toEntity(properties));
        return null;
      }
    };
  }

  // Create a delete command with the given predicate.
  private Command<Void> getDeleteCommand(final Predicate predicate) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        Set<String>  requestedIds = getRequestPropertyIds(PropertyHelper.getReadRequest(), predicate);
        ViewRegistry viewRegistry = ViewRegistry.getInstance();

        Set<ViewInstanceEntity> viewInstanceEntities = new HashSet<ViewInstanceEntity>();

        for (ViewEntity viewEntity : viewRegistry.getDefinitions()){
          for (ViewInstanceEntity viewInstanceEntity : viewRegistry.getInstanceDefinitions(viewEntity)){
            Resource resource = toResource(viewInstanceEntity, requestedIds);
            if (predicate == null || predicate.evaluate(resource)) {
              viewInstanceEntities.add(viewInstanceEntity);
            }
          }
        }
        for (ViewInstanceEntity viewInstanceEntity : viewInstanceEntities) {
          viewRegistry.uninstallViewInstance(viewInstanceEntity);
        }
        return null;
      }
    };
  }
}
