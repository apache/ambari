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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.authorization.Users;

import com.google.inject.Inject;

/**
 * Resource provider for group privilege resources.
 */
@StaticallyInject
public class GroupPrivilegeResourceProvider extends ReadOnlyResourceProvider {

  protected static final String PRIVILEGE_PRIVILEGE_ID_PROPERTY_ID = PrivilegeResourceProvider.PRIVILEGE_ID_PROPERTY_ID;
  protected static final String PRIVILEGE_PERMISSION_NAME_PROPERTY_ID = PrivilegeResourceProvider.PERMISSION_NAME_PROPERTY_ID;
  protected static final String PRIVILEGE_PERMISSION_LABEL_PROPERTY_ID = PrivilegeResourceProvider.PERMISSION_LABEL_PROPERTY_ID;
  protected static final String PRIVILEGE_PRINCIPAL_NAME_PROPERTY_ID = PrivilegeResourceProvider.PRINCIPAL_NAME_PROPERTY_ID;
  protected static final String PRIVILEGE_PRINCIPAL_TYPE_PROPERTY_ID = PrivilegeResourceProvider.PRINCIPAL_TYPE_PROPERTY_ID;
  protected static final String PRIVILEGE_VIEW_NAME_PROPERTY_ID = ViewPrivilegeResourceProvider.PRIVILEGE_VIEW_NAME_PROPERTY_ID;
  protected static final String PRIVILEGE_VIEW_VERSION_PROPERTY_ID = ViewPrivilegeResourceProvider.PRIVILEGE_VIEW_VERSION_PROPERTY_ID;
  protected static final String PRIVILEGE_INSTANCE_NAME_PROPERTY_ID = ViewPrivilegeResourceProvider.PRIVILEGE_INSTANCE_NAME_PROPERTY_ID;
  protected static final String PRIVILEGE_CLUSTER_NAME_PROPERTY_ID = ClusterPrivilegeResourceProvider.PRIVILEGE_CLUSTER_NAME_PROPERTY_ID;
  protected static final String PRIVILEGE_TYPE_PROPERTY_ID = AmbariPrivilegeResourceProvider.PRIVILEGE_TYPE_PROPERTY_ID;
  protected static final String PRIVILEGE_GROUP_NAME_PROPERTY_ID = "PrivilegeInfo/group_name";

  /**
   * Data access object used to obtain cluster entities.
   */
  @Inject
  protected static ClusterDAO clusterDAO;

  /**
   * Data access object used to obtain group entities.
   */
  @Inject
  protected static GroupDAO groupDAO;

  /**
   * Data access object used to obtain view instance entities.
   */
  @Inject
  protected static ViewInstanceDAO viewInstanceDAO;

  /**
   * Users (helper) object used to obtain privilege entities.
   */
  @Inject
  protected static Users users;

  /**
   * The property ids for a privilege resource.
   */
  private static Set<String> propertyIds = new HashSet<String>();

  static {
    propertyIds.add(PRIVILEGE_PRIVILEGE_ID_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_PERMISSION_NAME_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_PERMISSION_LABEL_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_PRINCIPAL_NAME_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_PRINCIPAL_TYPE_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_VIEW_NAME_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_VIEW_VERSION_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_INSTANCE_NAME_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_TYPE_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_GROUP_NAME_PROPERTY_ID);
  }

  /**
   * Static initialization.
   *  @param clusterDAO      the cluster data access object
   * @param groupDAO        the group data access object
   * @param viewInstanceDAO the view instance data access object
   * @param users           the users helper instance
   */
  public static void init(ClusterDAO clusterDAO, GroupDAO groupDAO,
                          ViewInstanceDAO viewInstanceDAO, Users users) {
    GroupPrivilegeResourceProvider.clusterDAO = clusterDAO;
    GroupPrivilegeResourceProvider.groupDAO = groupDAO;
    GroupPrivilegeResourceProvider.viewInstanceDAO = viewInstanceDAO;
    GroupPrivilegeResourceProvider.users = users;
  }

  @SuppressWarnings("serial")
  private static Set<String> pkPropertyIds = new HashSet<String>() {
    {
      add(PRIVILEGE_PRIVILEGE_ID_PROPERTY_ID);
    }
  };

  /**
   * The key property ids for a privilege resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();

  static {
    keyPropertyIds.put(Resource.Type.Group, PRIVILEGE_GROUP_NAME_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.GroupPrivilege, PRIVILEGE_PRIVILEGE_ID_PROPERTY_ID);
  }


  /**
   * Constructor.
   */
  public GroupPrivilegeResourceProvider() {
    super(propertyIds, keyPropertyIds, null);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_ASSIGN_ROLES);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredGetAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }

  // ----- PrivilegeResourceProvider -----------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<Resource>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    // Ensure that the authenticated user has authorization to get this information
    if (!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_MANAGE_GROUPS)) {
      throw new AuthorizationException();
    }

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final String groupName = (String) propertyMap.get(PRIVILEGE_GROUP_NAME_PROPERTY_ID);

      if (groupName != null) {
        GroupEntity groupEntity = groupDAO.findGroupByName(groupName);

        if (groupEntity == null) {
          throw new SystemException("Group " + groupName + " was not found");
        }

        final Collection<PrivilegeEntity> privileges = users.getGroupPrivileges(groupEntity);

        for (PrivilegeEntity privilegeEntity : privileges) {
          resources.add(toResource(privilegeEntity, groupName, requestedIds));
        }
      }
    }

    return resources;
  }

  /**
   * Translate the found data into a Resource
   *
   * @param privilegeEntity the privilege data
   * @param groupName        the group name
   * @param requestedIds    the relevant request ids
   * @return a resource
   */
  protected Resource toResource(PrivilegeEntity privilegeEntity, Object groupName, Set<String> requestedIds) {
    final ResourceImpl resource = new ResourceImpl(Resource.Type.GroupPrivilege);

    setResourceProperty(resource, PRIVILEGE_GROUP_NAME_PROPERTY_ID, groupName, requestedIds);
    setResourceProperty(resource, PRIVILEGE_PRIVILEGE_ID_PROPERTY_ID, privilegeEntity.getId(), requestedIds);
    setResourceProperty(resource, PRIVILEGE_PERMISSION_NAME_PROPERTY_ID, privilegeEntity.getPermission().getPermissionName(), requestedIds);
    setResourceProperty(resource, PRIVILEGE_PERMISSION_LABEL_PROPERTY_ID, privilegeEntity.getPermission().getPermissionLabel(), requestedIds);
    setResourceProperty(resource, PRIVILEGE_PRINCIPAL_TYPE_PROPERTY_ID, privilegeEntity.getPrincipal().getPrincipalType().getName(), requestedIds);

    final String principalTypeName = privilegeEntity.getPrincipal().getPrincipalType().getName();
    if (principalTypeName.equals(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME)) {
      final GroupEntity groupEntity = groupDAO.findGroupByPrincipal(privilegeEntity.getPrincipal());
      setResourceProperty(resource, PRIVILEGE_PRINCIPAL_NAME_PROPERTY_ID, groupEntity.getGroupName(), requestedIds);
    }

    String typeName = privilegeEntity.getResource().getResourceType().getName();
    ResourceType resourceType = ResourceType.translate(typeName);
    if (resourceType != null) {
      switch (resourceType) {
        case AMBARI:
          // there is nothing special to add for this case
          break;
        case CLUSTER:
          final ClusterEntity clusterEntity = clusterDAO.findByResourceId(privilegeEntity.getResource().getId());
          setResourceProperty(resource, PRIVILEGE_CLUSTER_NAME_PROPERTY_ID, clusterEntity.getClusterName(), requestedIds);
          break;
        case VIEW:
          final ViewInstanceEntity viewInstanceEntity = viewInstanceDAO.findByResourceId(privilegeEntity.getResource().getId());
          final ViewEntity viewEntity = viewInstanceEntity.getViewEntity();

          setResourceProperty(resource, PRIVILEGE_VIEW_NAME_PROPERTY_ID, viewEntity.getCommonName(), requestedIds);
          setResourceProperty(resource, PRIVILEGE_VIEW_VERSION_PROPERTY_ID, viewEntity.getVersion(), requestedIds);
          setResourceProperty(resource, PRIVILEGE_INSTANCE_NAME_PROPERTY_ID, viewInstanceEntity.getName(), requestedIds);
          break;
      }

      setResourceProperty(resource, PRIVILEGE_TYPE_PROPERTY_ID, resourceType.name(), requestedIds);
    }

    return resource;
  }
}