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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.view.ViewRegistry;

/**
 * Resource provider for custom view permissions.
 */
public class ViewPermissionResourceProvider extends AbstractResourceProvider {

  /**
   * Data access object used to obtain permission entities.
   */
  protected static PermissionDAO permissionDAO;

  /**
   * Permission property id constants.
   */
  public static final String VIEW_NAME_PROPERTY_ID       = "PermissionInfo/view_name";
  public static final String VIEW_VERSION_PROPERTY_ID    = "PermissionInfo/version";
  public static final String PERMISSION_ID_PROPERTY_ID   = "PermissionInfo/permission_id";
  public static final String PERMISSION_NAME_PROPERTY_ID = "PermissionInfo/permission_name";
  public static final String RESOURCE_NAME_PROPERTY_ID   = "PermissionInfo/resource_name";


  /**
   * The key property ids for a permission resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();
  static {
    keyPropertyIds.put(Resource.Type.View, VIEW_NAME_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.ViewVersion, VIEW_VERSION_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.ViewPermission, PERMISSION_ID_PROPERTY_ID);
  }

  /**
   * The property ids for a permission resource.
   */
  private static Set<String> propertyIds = new HashSet<String>();
  static {
    propertyIds.add(VIEW_NAME_PROPERTY_ID);
    propertyIds.add(VIEW_VERSION_PROPERTY_ID);
    propertyIds.add(PERMISSION_ID_PROPERTY_ID);
    propertyIds.add(PERMISSION_NAME_PROPERTY_ID);
    propertyIds.add(RESOURCE_NAME_PROPERTY_ID);
  }


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a permission resource provider.
   */
  public ViewPermissionResourceProvider() {
    super(propertyIds, keyPropertyIds);
  }


  // ----- PermissionResourceProvider ----------------------------------------

  /**
   * Static initialization.
   *
   * @param dao  permission data access object
   */
  public static void init(PermissionDAO dao) {
    permissionDAO = dao;
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException,
      ResourceAlreadyExistsException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    ViewRegistry  viewRegistry = ViewRegistry.getInstance();
    Set<Resource> resources    = new HashSet<Resource>();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);

    PermissionEntity viewUsePermission = permissionDAO.findViewUsePermission();
    for (Map<String, Object> propertyMap: getPropertyMaps(predicate)) {
      Object viewName = propertyMap.get(VIEW_NAME_PROPERTY_ID);
      Object viewVersion = propertyMap.get(VIEW_VERSION_PROPERTY_ID);
      if (viewName != null && viewVersion != null) {
        ViewEntity viewEntity = viewRegistry.getDefinition(viewName.toString(), viewVersion.toString());

        // do not report permissions for views that are not loaded.
        if (viewEntity.isDeployed()) {
          resources.add(toResource(viewUsePermission, viewEntity.getResourceType(), viewEntity, requestedIds));
        }
      }
    }

    for(PermissionEntity permissionEntity : permissionDAO.findAll()){
      ResourceTypeEntity resourceType = permissionEntity.getResourceType();

      ViewEntity viewEntity = viewRegistry.getDefinition(resourceType);

      if (viewEntity != null && viewEntity.isDeployed()) {
        resources.add(toResource(permissionEntity, resourceType, viewEntity, requestedIds));
      }
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not supported.");
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

  // convert the given permission entity to a resource
  private Resource toResource(PermissionEntity entity, ResourceTypeEntity resourceType,
                              ViewEntity viewEntity, Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.ViewPermission);

    setResourceProperty(resource, VIEW_NAME_PROPERTY_ID, viewEntity.getCommonName(), requestedIds);
    setResourceProperty(resource, VIEW_VERSION_PROPERTY_ID, viewEntity.getVersion(), requestedIds);

    setResourceProperty(resource, PERMISSION_ID_PROPERTY_ID, entity.getId(), requestedIds);
    setResourceProperty(resource, PERMISSION_NAME_PROPERTY_ID, entity.getPermissionName(), requestedIds);
    setResourceProperty(resource, RESOURCE_NAME_PROPERTY_ID, resourceType.getName(), requestedIds);

    return resource;
  }
}
