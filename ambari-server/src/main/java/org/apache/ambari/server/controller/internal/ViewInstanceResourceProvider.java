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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
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
import org.apache.ambari.server.orm.entities.ViewURLEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.server.view.validation.InstanceValidationResultImpl;
import org.apache.ambari.server.view.validation.ValidationException;
import org.apache.ambari.server.view.validation.ValidationResultImpl;
import org.apache.ambari.view.ClusterType;
import org.apache.ambari.view.validation.Validator;

import com.google.inject.persist.Transactional;

/**
 * Resource provider for view instances.
 */
public class ViewInstanceResourceProvider extends AbstractAuthorizedResourceProvider {

  /**
   * View instance property id constants.
   */
  public static final String VIEW_NAME_PROPERTY_ID      = "ViewInstanceInfo/view_name";
  public static final String VIEW_VERSION_PROPERTY_ID   = "ViewInstanceInfo/version";
  public static final String INSTANCE_NAME_PROPERTY_ID  = "ViewInstanceInfo/instance_name";
  public static final String LABEL_PROPERTY_ID          = "ViewInstanceInfo/label";
  public static final String DESCRIPTION_PROPERTY_ID    = "ViewInstanceInfo/description";
  public static final String VISIBLE_PROPERTY_ID        = "ViewInstanceInfo/visible";
  public static final String ICON_PATH_ID               = "ViewInstanceInfo/icon_path";
  public static final String ICON64_PATH_ID             = "ViewInstanceInfo/icon64_path";
  public static final String PROPERTIES_PROPERTY_ID     = "ViewInstanceInfo/properties";
  public static final String DATA_PROPERTY_ID           = "ViewInstanceInfo/instance_data";
  public static final String CONTEXT_PATH_PROPERTY_ID   = "ViewInstanceInfo/context_path";
  public static final String STATIC_PROPERTY_ID         = "ViewInstanceInfo/static";
  public static final String CLUSTER_HANDLE_PROPERTY_ID = "ViewInstanceInfo/cluster_handle";
  public static final String CLUSTER_TYPE_PROPERTY_ID = "ViewInstanceInfo/cluster_type";
  public static final String SHORT_URL_PROPERTY_ID      = "ViewInstanceInfo/short_url";
  public static final String SHORT_URL_NAME_PROPERTY_ID = "ViewInstanceInfo/short_url_name";

  // validation properties
  public static final String VALIDATION_RESULT_PROPERTY_ID           = "ViewInstanceInfo/validation_result";
  public static final String PROPERTY_VALIDATION_RESULTS_PROPERTY_ID = "ViewInstanceInfo/property_validation_results";

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
    keyPropertyIds.put(Resource.Type.ViewVersion, VIEW_VERSION_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.ViewInstance, INSTANCE_NAME_PROPERTY_ID);
  }

  /**
   * The property ids for a view instance resource.
   */
  private static Set<String> propertyIds = new HashSet<String>();
  static {
    propertyIds.add(VIEW_NAME_PROPERTY_ID);
    propertyIds.add(VIEW_VERSION_PROPERTY_ID);
    propertyIds.add(INSTANCE_NAME_PROPERTY_ID);
    propertyIds.add(LABEL_PROPERTY_ID);
    propertyIds.add(DESCRIPTION_PROPERTY_ID);
    propertyIds.add(VISIBLE_PROPERTY_ID);
    propertyIds.add(ICON_PATH_ID);
    propertyIds.add(ICON64_PATH_ID);
    propertyIds.add(PROPERTIES_PROPERTY_ID);
    propertyIds.add(DATA_PROPERTY_ID);
    propertyIds.add(CONTEXT_PATH_PROPERTY_ID);
    propertyIds.add(STATIC_PROPERTY_ID);
    propertyIds.add(CLUSTER_HANDLE_PROPERTY_ID);
    propertyIds.add(CLUSTER_TYPE_PROPERTY_ID);
    propertyIds.add(SHORT_URL_PROPERTY_ID);
    propertyIds.add(SHORT_URL_NAME_PROPERTY_ID);
    propertyIds.add(VALIDATION_RESULT_PROPERTY_ID);
    propertyIds.add(PROPERTY_VALIDATION_RESULTS_PROPERTY_ID);
  }

  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view instance resource provider.
   */
  public ViewInstanceResourceProvider() {
    super(propertyIds, keyPropertyIds);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_VIEWS);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  protected RequestStatus createResourcesAuthorized(Request request)
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

      String viewName     = (String) propertyMap.get(VIEW_NAME_PROPERTY_ID);
      String viewVersion  = (String) propertyMap.get(VIEW_VERSION_PROPERTY_ID);
      String instanceName = (String) propertyMap.get(INSTANCE_NAME_PROPERTY_ID);

      for (ViewEntity viewDefinition : viewRegistry.getDefinitions()){
        // do not report instances for views that are not loaded.
        if (viewDefinition.isDeployed()){
          if (viewName == null || viewName.equals(viewDefinition.getCommonName())) {
            for (ViewInstanceEntity viewInstanceDefinition : viewRegistry.getInstanceDefinitions(viewDefinition)) {
              if (instanceName == null || instanceName.equals(viewInstanceDefinition.getName())) {
                if (viewVersion == null || viewVersion.equals(viewDefinition.getVersion())) {
                  if (includeInstance(viewInstanceDefinition, true)) {
                    Resource resource = toResource(viewInstanceDefinition, requestedIds);
                    resources.add(resource);
                  }
                }
              }
            }
          }
        }
      }
    }
    return resources;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
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
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
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
  protected Resource toResource(ViewInstanceEntity viewInstanceEntity, Set<String> requestedIds) {
    Resource   resource   = new ResourceImpl(Resource.Type.ViewInstance);
    ViewEntity viewEntity = viewInstanceEntity.getViewEntity();

    String viewName = viewEntity.getCommonName();
    String version  = viewEntity.getVersion();
    String name     = viewInstanceEntity.getName();

    setResourceProperty(resource, VIEW_NAME_PROPERTY_ID, viewName, requestedIds);
    setResourceProperty(resource, VIEW_VERSION_PROPERTY_ID, version, requestedIds);
    setResourceProperty(resource, INSTANCE_NAME_PROPERTY_ID, name, requestedIds);
    setResourceProperty(resource, LABEL_PROPERTY_ID, viewInstanceEntity.getLabel(), requestedIds);
    setResourceProperty(resource, DESCRIPTION_PROPERTY_ID, viewInstanceEntity.getDescription(), requestedIds);
    setResourceProperty(resource, VISIBLE_PROPERTY_ID, viewInstanceEntity.isVisible(), requestedIds);
    setResourceProperty(resource, STATIC_PROPERTY_ID, viewInstanceEntity.isXmlDriven(), requestedIds);
    setResourceProperty(resource, CLUSTER_HANDLE_PROPERTY_ID, viewInstanceEntity.getClusterHandle(), requestedIds);
    setResourceProperty(resource, CLUSTER_TYPE_PROPERTY_ID, viewInstanceEntity.getClusterType(), requestedIds);
    ViewURLEntity viewUrl = viewInstanceEntity.getViewUrl();
    if(viewUrl != null) {
      setResourceProperty(resource, SHORT_URL_PROPERTY_ID, viewUrl.getUrlSuffix(), requestedIds);
      setResourceProperty(resource, SHORT_URL_NAME_PROPERTY_ID, viewUrl.getUrlName(), requestedIds);
    }

    // only allow an admin to access the view properties
    if (ViewRegistry.getInstance().checkAdmin()) {
      setResourceProperty(resource, PROPERTIES_PROPERTY_ID,
          viewInstanceEntity.getPropertyMap(), requestedIds);
    }

    Map<String, String> applicationData = new HashMap<String, String>();

    String currentUserName = viewInstanceEntity.getCurrentUserName();
    for (ViewInstanceDataEntity viewInstanceDataEntity : viewInstanceEntity.getData()) {
      if (currentUserName.equals(viewInstanceDataEntity.getUser())) {
        applicationData.put(viewInstanceDataEntity.getName(), viewInstanceDataEntity.getValue());
      }
    }
    setResourceProperty(resource, DATA_PROPERTY_ID,
        applicationData, requestedIds);

    String contextPath = ViewInstanceEntity.getContextPath(viewName, version, name);

    setResourceProperty(resource, CONTEXT_PATH_PROPERTY_ID,contextPath, requestedIds);
    setResourceProperty(resource, ICON_PATH_ID, getIconPath(contextPath, viewInstanceEntity.getIcon()), requestedIds);
    setResourceProperty(resource, ICON64_PATH_ID, getIconPath(contextPath, viewInstanceEntity.getIcon64()), requestedIds);

    // if the view provides its own validator then run it
    if (viewEntity.hasValidator()) {

      if (isPropertyRequested(VALIDATION_RESULT_PROPERTY_ID, requestedIds) ||
          isPropertyRequested(PROPERTY_VALIDATION_RESULTS_PROPERTY_ID, requestedIds)) {

        InstanceValidationResultImpl result =
            viewInstanceEntity.getValidationResult(viewEntity, Validator.ValidationContext.EXISTING);

        setResourceProperty(resource, VALIDATION_RESULT_PROPERTY_ID, ValidationResultImpl.create(result), requestedIds);
        setResourceProperty(resource, PROPERTY_VALIDATION_RESULTS_PROPERTY_ID, result.getPropertyResults(), requestedIds);
      }
    }

    return resource;
  }

  // Convert a map of properties to a view instance entity.
  private ViewInstanceEntity toEntity(Map<String, Object> properties, boolean update) throws AmbariException {
    String name = (String) properties.get(INSTANCE_NAME_PROPERTY_ID);
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("View instance name must be provided");
    }

    String version = (String) properties.get(VIEW_VERSION_PROPERTY_ID);
    if (version == null || version.isEmpty()) {
      throw new IllegalArgumentException("View version must be provided");
    }

    String commonViewName = (String) properties.get(VIEW_NAME_PROPERTY_ID);
    if (commonViewName == null || commonViewName.isEmpty()) {
      throw new IllegalArgumentException("View name must be provided");
    }

    ViewRegistry viewRegistry = ViewRegistry.getInstance();
    ViewEntity   viewEntity   = viewRegistry.getDefinition(commonViewName, version);
    String       viewName     = ViewEntity.getViewName(commonViewName, version);

    if (viewEntity == null) {
      throw new IllegalArgumentException("View name " + viewName + " does not exist.");
    }

    ViewInstanceEntity viewInstanceEntity = null;

    if (update) {
      viewInstanceEntity = viewRegistry.getViewInstanceEntity(viewName, name);
    }

    if (viewInstanceEntity == null) {
      viewInstanceEntity = new ViewInstanceEntity();
      viewInstanceEntity.setName(name);
      viewInstanceEntity.setViewName(viewName);
      viewInstanceEntity.setViewEntity(viewEntity);
    }
    if (properties.containsKey(LABEL_PROPERTY_ID)) {
      viewInstanceEntity.setLabel((String) properties.get(LABEL_PROPERTY_ID));
    }

    if (properties.containsKey(DESCRIPTION_PROPERTY_ID)) {
      viewInstanceEntity.setDescription((String) properties.get(DESCRIPTION_PROPERTY_ID));
    }

    String visible = (String) properties.get(VISIBLE_PROPERTY_ID);
    viewInstanceEntity.setVisible(visible==null ? true : Boolean.valueOf(visible));

    if (properties.containsKey(ICON_PATH_ID)) {
      viewInstanceEntity.setIcon((String) properties.get(ICON_PATH_ID));
    }

    if (properties.containsKey(ICON64_PATH_ID)) {
      viewInstanceEntity.setIcon64((String) properties.get(ICON64_PATH_ID));
    }

    if (properties.containsKey(CLUSTER_HANDLE_PROPERTY_ID)) {
      String handle = (String) properties.get(CLUSTER_HANDLE_PROPERTY_ID);
      if (handle != null) {
        viewInstanceEntity.setClusterHandle(Long.valueOf(handle));
      } else {
        viewInstanceEntity.setClusterHandle(null);
      }
    }

    if (properties.containsKey(CLUSTER_TYPE_PROPERTY_ID)) {
      String clusterType = (String) properties.get(CLUSTER_TYPE_PROPERTY_ID);
      viewInstanceEntity.setClusterType(ClusterType.valueOf(clusterType));
    }

    Map<String, String> instanceProperties = new HashMap<String, String>();

    boolean isUserAdmin = viewRegistry.checkAdmin();

    for (Map.Entry<String, Object> entry : properties.entrySet()) {

      String propertyName = entry.getKey();

      if (propertyName.startsWith(PROPERTIES_PREFIX)) {

        // only allow an admin to access the view properties
        if (isUserAdmin) {
          instanceProperties.put(entry.getKey().substring(PROPERTIES_PREFIX.length()), (String) entry.getValue());
        }
      } else if (propertyName.startsWith(DATA_PREFIX)) {
        viewInstanceEntity.putInstanceData(entry.getKey().substring(DATA_PREFIX.length()), (String) entry.getValue());
      }
    }
    if (!instanceProperties.isEmpty()) {
      try {
        viewRegistry.setViewInstanceProperties(viewInstanceEntity, instanceProperties, viewEntity.getConfiguration(), viewEntity.getClassLoader());
      } catch (org.apache.ambari.view.SystemException e) {
        throw new AmbariException("Caught exception trying to set view properties.", e);
      }
    }

    return viewInstanceEntity;
  }

  // Create a create command with all properties set.
  private Command<Void> getCreateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Transactional
      @Override
      public Void invoke() throws AmbariException {
        try {
          ViewRegistry       viewRegistry   = ViewRegistry.getInstance();
          ViewInstanceEntity instanceEntity = toEntity(properties, false);

          ViewEntity viewEntity = instanceEntity.getViewEntity();
          String     viewName   = viewEntity.getCommonName();
          String     version    = viewEntity.getVersion();
          ViewEntity view       = viewRegistry.getDefinition(viewName, version);

          if ( view == null ) {
            throw new IllegalStateException("The view " + viewName + " is not registered.");
          }

          // the view must be in the DEPLOYED state to create an instance
          if (!view.isDeployed()) {
            throw new IllegalStateException("The view " + viewName + " is not loaded.");
          }

          if (viewRegistry.instanceExists(instanceEntity)) {
            throw new DuplicateResourceException("The instance " + instanceEntity.getName() + " already exists.");
          }
          viewRegistry.installViewInstance(instanceEntity);
        } catch (org.apache.ambari.view.SystemException e) {
          throw new AmbariException("Caught exception trying to create view instance.", e);
        } catch (ValidationException e) {
          // results in a BAD_REQUEST (400) response for the validation failure.
          throw new IllegalArgumentException(e.getMessage(), e);
        }
        return null;
      }
    };
  }

  // Create an update command with all properties set.
  private Command<Void> getUpdateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Transactional
      @Override
      public Void invoke() throws AmbariException {
        ViewInstanceEntity instance = toEntity(properties, true);
        ViewEntity         view     = instance.getViewEntity();

        if (includeInstance(view.getCommonName(), view.getVersion(), instance.getInstanceName(), false)) {
          try {
            ViewRegistry.getInstance().updateViewInstance(instance);
            ViewRegistry.getInstance().updateView(instance);
          } catch (org.apache.ambari.view.SystemException e) {
            throw new AmbariException("Caught exception trying to update view instance.", e);
          } catch (ValidationException e) {
            // results in a BAD_REQUEST (400) response for the validation failure.
            throw new IllegalArgumentException(e.getMessage(), e);
          }
        }
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
          // the view must be in the DEPLOYED state to delete an instance
          if (viewEntity.isDeployed()) {
            for (ViewInstanceEntity viewInstanceEntity : viewRegistry.getInstanceDefinitions(viewEntity)){
              Resource resource = toResource(viewInstanceEntity, requestedIds);
              if (predicate == null || predicate.evaluate(resource)) {
                if (includeInstance(viewInstanceEntity, false)) {
                  viewInstanceEntities.add(viewInstanceEntity);
                }
              }
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

  // get the icon path
  private static String getIconPath(String contextPath, String iconPath){
    return iconPath == null || iconPath.length() == 0 ? null :
        contextPath + (iconPath.startsWith("/") ? "" : "/") + iconPath;
  }

  /**
   * Determine whether or not the view instance resource identified
   * by the given instance name should be included based on the permissions
   * granted to the current user.
   *
   * @param viewName      the view name
   * @param version       the view version
   * @param instanceName  the name of the view instance resource
   * @param readOnly      indicate whether or not this is for a read only operation
   *
   * @return true if the view instance should be included based on the permissions of the current user
   */
  private boolean includeInstance(String viewName, String version, String instanceName, boolean readOnly) {

    ViewRegistry viewRegistry = ViewRegistry.getInstance();

    return viewRegistry.checkPermission(viewName, version, instanceName, readOnly);
  }

  /**
   * Determine whether or not the given view instance resource should be included
   * based on the permissions granted to the current user.
   *
   * @param instanceEntity  the view instance entity
   * @param readOnly        indicate whether or not this is for a read only operation
   *
   * @return true if the view instance should be included based on the permissions of the current user
   */
  private boolean includeInstance(ViewInstanceEntity instanceEntity, boolean readOnly) {

    ViewRegistry viewRegistry = ViewRegistry.getInstance();

    return viewRegistry.checkPermission(instanceEntity, readOnly);
  }
}
