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
 * See the License for the specific language governing privileges and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.controller.PrivilegeResponse;
import org.apache.ambari.server.controller.UserPrivilegeResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.authorization.UserType;
import org.apache.ambari.server.security.authorization.Users;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Resource provider for user privilege resources.
 */
public class UserPrivilegeResourceProvider extends ReadOnlyResourceProvider {

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
  protected static final String PRIVILEGE_USER_NAME_PROPERTY_ID = "PrivilegeInfo/user_name";

  /**
   * Data access object used to obtain user entities.
   */
  protected static UserDAO userDAO;

  /**
   * Data access object used to obtain cluster entities.
   */
  protected static ClusterDAO clusterDAO;

  /**
   * Data access object used to obtain group entities.
   */
  protected static GroupDAO groupDAO;

  /**
   * Data access object used to obtain view instance entities.
   */
  protected static ViewInstanceDAO viewInstanceDAO;

  /**
   * Helper to obtain privilege data for requested users
   */
  private static Users users;

  /**
   * The property ids for a privilege resource.
   */
  private static Set<String> propertyIds = Sets.newHashSet(
      PRIVILEGE_PRIVILEGE_ID_PROPERTY_ID,
      PRIVILEGE_PERMISSION_NAME_PROPERTY_ID,
      PRIVILEGE_PERMISSION_LABEL_PROPERTY_ID,
      PRIVILEGE_PRINCIPAL_NAME_PROPERTY_ID,
      PRIVILEGE_PRINCIPAL_TYPE_PROPERTY_ID,
      PRIVILEGE_VIEW_NAME_PROPERTY_ID,
      PRIVILEGE_VIEW_VERSION_PROPERTY_ID,
      PRIVILEGE_INSTANCE_NAME_PROPERTY_ID,
      PRIVILEGE_CLUSTER_NAME_PROPERTY_ID,
      PRIVILEGE_TYPE_PROPERTY_ID,
      PRIVILEGE_USER_NAME_PROPERTY_ID);

  /**
   * Static initialization.
   *  @param userDAO         the user data access object
   * @param clusterDAO      the cluster data access object
   * @param groupDAO        the group data access object
   * @param viewInstanceDAO the view instance data access object
   * @param users           the Users helper object
   */
  public static void init(UserDAO userDAO, ClusterDAO clusterDAO, GroupDAO groupDAO,
                          ViewInstanceDAO viewInstanceDAO, Users users) {
    UserPrivilegeResourceProvider.userDAO         = userDAO;
    UserPrivilegeResourceProvider.clusterDAO      = clusterDAO;
    UserPrivilegeResourceProvider.groupDAO        = groupDAO;
    UserPrivilegeResourceProvider.viewInstanceDAO = viewInstanceDAO;
    UserPrivilegeResourceProvider.users           = users;
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
  private static Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.User, PRIVILEGE_USER_NAME_PROPERTY_ID)
      .put(Resource.Type.UserPrivilege, PRIVILEGE_PRIVILEGE_ID_PROPERTY_ID)
      .build();

  private ThreadLocal<LoadingCache<Long, ClusterEntity>> clusterCache =
      new ThreadLocal<LoadingCache<Long, ClusterEntity>>(){
    @Override
    protected LoadingCache<Long, ClusterEntity> initialValue() {
      CacheLoader<Long, ClusterEntity> loader = new CacheLoader<Long, ClusterEntity>() {
        @Override
        public ClusterEntity load(Long key) throws Exception {
          return clusterDAO.findByResourceId(key);
        }
      };
      return CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.SECONDS).build(loader);
    }
  };

  private ThreadLocal<LoadingCache<Long, ViewInstanceEntity>> viewInstanceCache =
      new ThreadLocal<LoadingCache<Long, ViewInstanceEntity>>(){
    @Override
    protected LoadingCache<Long, ViewInstanceEntity> initialValue() {
      CacheLoader<Long, ViewInstanceEntity> loader = new CacheLoader<Long, ViewInstanceEntity>() {
        @Override
        public ViewInstanceEntity load(Long key) throws Exception {
          return viewInstanceDAO.findByResourceId(key);
        }
      };
      return CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.SECONDS).build(loader);
    }
  };

  private ThreadLocal<LoadingCache<String, UserEntity>> usersCache =
      new ThreadLocal<LoadingCache<String, UserEntity>>(){
        @Override
        protected LoadingCache<String, UserEntity> initialValue() {
          CacheLoader<String, UserEntity> loader = new CacheLoader<String, UserEntity>() {
            @Override
            public UserEntity load(String key) throws Exception {
              //fallback mechanism, mostly for unit tests
              UserEntity userEntity = userDAO.findLocalUserByName(key);
              if (userEntity == null) {
                userEntity = userDAO.findLdapUserByName(key);
              }
              if (userEntity == null) {
                userEntity = userDAO.findUserByNameAndType(key, UserType.JWT);
              }
              return userEntity;
            }
          };

          return CacheBuilder.newBuilder()
              .expireAfterWrite(20, TimeUnit.SECONDS)
              .build(loader);
        }
      };

  private ThreadLocal<LoadingCache<PrincipalEntity, GroupEntity>> groupsCache =
      new ThreadLocal<LoadingCache<PrincipalEntity, GroupEntity>>(){
        @Override
        protected LoadingCache<PrincipalEntity, GroupEntity> initialValue() {
          CacheLoader<PrincipalEntity, GroupEntity> loader = new CacheLoader<PrincipalEntity, GroupEntity>() {
            @Override
            public GroupEntity load(PrincipalEntity key) throws Exception {
              return groupDAO.findGroupByPrincipal(key);
            }
          };

          return CacheBuilder.newBuilder()
              .expireAfterWrite(20, TimeUnit.SECONDS)
              .build(loader);
        }
      };

  private GroupEntity getCachedGroupByPrincipal(PrincipalEntity principalEntity) {
    GroupEntity entity = groupsCache.get().getIfPresent(principalEntity);
    if (entity == null) {
      for (GroupEntity groupEntity : groupDAO.findAll()) {
        groupsCache.get().put(groupEntity.getPrincipal(), groupEntity);
      }
      entity = groupsCache.get().getUnchecked(principalEntity);
    }
    return entity;
  }


  /**
   * Constructor.
   */
  public UserPrivilegeResourceProvider() {
    super(Resource.Type.UserPrivilege, propertyIds, keyPropertyIds, null);

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
    final Set<Resource> resources = new HashSet<>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);


    boolean isUserAdministrator = AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null,
        RoleAuthorization.AMBARI_MANAGE_USERS);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final String userName = (String) propertyMap.get(PRIVILEGE_USER_NAME_PROPERTY_ID);

      // Ensure that the authenticated user has authorization to get this information
      if (!isUserAdministrator && !AuthorizationHelper.getAuthenticatedName().equalsIgnoreCase(userName)) {
        throw new AuthorizationException();
      }

      if (userName != null) {

        UserEntity userEntity = usersCache.get().getIfPresent(userName);
        if (userEntity == null) {
          //temporary tradeoff, add ~200ms for single user call, but start saving time for 100+ subsequent calls
          //usual case for management page is to populate subresources for all users
          Map<String, UserEntity> userNames = new TreeMap<>();
          for (UserEntity entity : userDAO.findAll()) {
            UserEntity existing = userNames.get(entity.getUserName());
            if (existing == null ||
                entity.getUserType() == UserType.LOCAL ||
                existing.getUserType() == UserType.JWT) {
              userNames.put(entity.getUserName(), entity);
            }
          }
          usersCache.get().putAll(userNames);
          userEntity = usersCache.get().getUnchecked(userName);
        }

        if (userEntity == null) {
            userEntity = userDAO.findUserByNameAndType(userName, UserType.PAM);
        }
        if (userEntity == null) {
          throw new SystemException("User " + userName + " was not found");
        }

        final Collection<PrivilegeEntity> privileges = users.getUserPrivileges(userEntity);

        for (PrivilegeEntity privilegeEntity : privileges) {
          UserPrivilegeResponse response = getResponse(privilegeEntity, userName);
          resources.add(toResource(response, requestedIds));
        }
      }
    }
    return resources;
  }

  /**
   * Returns response schema instance for /users/{userName}/privileges REST endpoint
   * @param privilegeEntity {@link PrivilegeEntity}
   * @param userName   user name
   * @return {@link UserPrivilegeResponse}
   */
  protected UserPrivilegeResponse getResponse(PrivilegeEntity privilegeEntity, String userName) {
    String permissionLabel = privilegeEntity.getPermission().getPermissionLabel();
    String permissionName =  privilegeEntity.getPermission().getPermissionName();
    String principalTypeName = privilegeEntity.getPrincipal().getPrincipalType().getName();
    UserPrivilegeResponse userPrivilegeResponse = new UserPrivilegeResponse(userName, permissionLabel , permissionName,
      privilegeEntity.getId(), PrincipalTypeEntity.PrincipalType.valueOf(principalTypeName));

    if (principalTypeName.equals(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME)) {
      final UserEntity user = userDAO.findUserByPrincipal(privilegeEntity.getPrincipal());
      userPrivilegeResponse.setPrincipalName(user.getUserName());
    } else if (principalTypeName.equals(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME)) {
      final GroupEntity groupEntity = getCachedGroupByPrincipal(privilegeEntity.getPrincipal());
      userPrivilegeResponse.setPrincipalName(groupEntity.getGroupName());
    }

    String typeName = privilegeEntity.getResource().getResourceType().getName();
    ResourceType resourceType = ResourceType.translate(typeName);

    if(resourceType != null) {
      switch (resourceType) {
        case AMBARI:
          // there is nothing special to add for this case
          break;
        case CLUSTER:
          final ClusterEntity clusterEntity = clusterCache.get().getUnchecked(privilegeEntity.getResource().getId());
          userPrivilegeResponse.setClusterName(clusterEntity.getClusterName());
          break;
        case VIEW:
          final ViewInstanceEntity viewInstanceEntity = viewInstanceCache.get().getUnchecked(privilegeEntity.getResource().getId());
          final ViewEntity viewEntity = viewInstanceEntity.getViewEntity();

          userPrivilegeResponse.setViewName(viewEntity.getCommonName());
          userPrivilegeResponse.setVersion(viewEntity.getVersion());
          userPrivilegeResponse.setInstanceName(viewInstanceEntity.getName());
          break;
      }

      userPrivilegeResponse.setType(resourceType);
    }

    return userPrivilegeResponse;
  }

  /**
   * Translate the Response into a Resource
   * @param response        {@link PrivilegeResponse}
   * @param requestedIds    the relevant request ids
   * @return a resource
   */
  protected Resource toResource(UserPrivilegeResponse response, Set<String> requestedIds){
    final ResourceImpl resource = new ResourceImpl(Resource.Type.UserPrivilege);



    setResourceProperty(resource, PRIVILEGE_USER_NAME_PROPERTY_ID, response.getUserName(), requestedIds);
    setResourceProperty(resource, PRIVILEGE_PRIVILEGE_ID_PROPERTY_ID, response.getPrivilegeId(), requestedIds);
    setResourceProperty(resource, PRIVILEGE_PERMISSION_NAME_PROPERTY_ID, response.getPermissionName(), requestedIds);
    setResourceProperty(resource, PRIVILEGE_PERMISSION_LABEL_PROPERTY_ID, response.getPermissionLabel(), requestedIds);
    setResourceProperty(resource, PRIVILEGE_PRINCIPAL_TYPE_PROPERTY_ID, response.getPrincipalType().name(), requestedIds);
    if (response.getPrincipalName() != null) {
      setResourceProperty(resource, PRIVILEGE_PRINCIPAL_NAME_PROPERTY_ID, response.getPrincipalName(), requestedIds);
    }
    if (response.getType() != null) {
      setResourceProperty(resource, PRIVILEGE_TYPE_PROPERTY_ID, response.getType().name(), requestedIds);
      switch (response.getType()) {
        case CLUSTER:
          setResourceProperty(resource, PRIVILEGE_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
          break;
        case VIEW:
          setResourceProperty(resource, PRIVILEGE_VIEW_NAME_PROPERTY_ID, response.getViewName(), requestedIds);
          setResourceProperty(resource, PRIVILEGE_VIEW_VERSION_PROPERTY_ID, response.getVersion(), requestedIds);
          setResourceProperty(resource, PRIVILEGE_INSTANCE_NAME_PROPERTY_ID, response.getInstanceName(), requestedIds);
          break;
      }

    }
    return resource;
  }
}