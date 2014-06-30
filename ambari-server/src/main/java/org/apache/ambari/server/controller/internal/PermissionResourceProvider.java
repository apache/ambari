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

import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for permission instances.
 */
public class PermissionResourceProvider extends AbstractResourceProvider {
  /**
   * Permission property id constants.
   */
  public static final String PERMISSION_ID_PROPERTY_ID   = "PermissionInfo/permission_id";
  public static final String PERMISSION_NAME_PROPERTY_ID = "PermissionInfo/permission_name";
  public static final String RESOURCE_NAME_PROPERTY_ID   = "PermissionInfo/resource_name";


  /**
   * The key property ids for a permission resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();
  static {
    keyPropertyIds.put(Resource.Type.Permission, PERMISSION_ID_PROPERTY_ID);
  }

  /**
   * The property ids for a permission resource.
   */
  private static Set<String> propertyIds = new HashSet<String>();
  static {
    propertyIds.add(PERMISSION_ID_PROPERTY_ID);
    propertyIds.add(PERMISSION_NAME_PROPERTY_ID);
    propertyIds.add(RESOURCE_NAME_PROPERTY_ID);
  }


  /**
   * Builtin permissions
   */
  private static final Set<Resource> builtinPermissions = new HashSet<Resource>();

  static {
    // AMBARI.ADMIN
    Resource resource = new ResourceImpl(Resource.Type.Permission);
    resource.setProperty(PERMISSION_ID_PROPERTY_ID, 0);
    resource.setProperty(PERMISSION_NAME_PROPERTY_ID, "ADMIN");
    resource.setProperty(RESOURCE_NAME_PROPERTY_ID, "AMBARI");
    builtinPermissions.add(resource);

    // CLUSTER.READ
    resource = new ResourceImpl(Resource.Type.Permission);
    resource.setProperty(PERMISSION_ID_PROPERTY_ID, 1);
    resource.setProperty(PERMISSION_NAME_PROPERTY_ID, "READ");
    resource.setProperty(RESOURCE_NAME_PROPERTY_ID, "CLUSTER");
    builtinPermissions.add(resource);

    // CLUSTER.OPERATE
    resource = new ResourceImpl(Resource.Type.Permission);
    resource.setProperty(PERMISSION_ID_PROPERTY_ID, 2);
    resource.setProperty(PERMISSION_NAME_PROPERTY_ID, "OPERATE");
    resource.setProperty(RESOURCE_NAME_PROPERTY_ID, "CLUSTER");
    builtinPermissions.add(resource);

    // CLUSTER.OPERATE
    resource = new ResourceImpl(Resource.Type.Permission);
    resource.setProperty(PERMISSION_ID_PROPERTY_ID, 3);
    resource.setProperty(PERMISSION_NAME_PROPERTY_ID, "USE");
    resource.setProperty(RESOURCE_NAME_PROPERTY_ID, "VIEW");
    builtinPermissions.add(resource);
  }


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a permission resource provider.
   */
  public PermissionResourceProvider() {
    super(propertyIds, keyPropertyIds);
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
    // TODO : add custom permissions.
    return new HashSet<Resource>(builtinPermissions);
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
}
