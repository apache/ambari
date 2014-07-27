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
 * See the License for the specific language governing privileges and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

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
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.UserEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract resource provider for privilege resources.
 */
public abstract class PrivilegeResourceProvider<T> extends AbstractResourceProvider {

  /**
   * Data access object used to obtain privilege entities.
   */
  protected static PrivilegeDAO privilegeDAO;

  /**
   * Data access object used to obtain user entities.
   */
  protected static UserDAO userDAO;

  /**
   * Data access object used to obtain group entities.
   */
  protected static GroupDAO groupDAO;

  /**
   * Data access object used to obtain principal entities.
   */
  protected static PrincipalDAO principalDAO;

  /**
   * Data access object used to obtain permission entities.
   */
  protected static PermissionDAO permissionDAO;

  /**
   * Data access object used to obtain resource entities.
   */
  protected static ResourceDAO resourceDAO;

  /**
   * Privilege property id constants.
   */
  public static final String PRIVILEGE_ID_PROPERTY_ID    = "PrivilegeInfo/privilege_id";
  public static final String PERMISSION_NAME_PROPERTY_ID = "PrivilegeInfo/permission_name";
  public static final String PRINCIPAL_NAME_PROPERTY_ID  = "PrivilegeInfo/principal_name";
  public static final String PRINCIPAL_TYPE_PROPERTY_ID  = "PrivilegeInfo/principal_type";

  /**
   * The privilege resource type.
   */
  private final Resource.Type resourceType;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a privilege resource provider.
   */
  public PrivilegeResourceProvider(Set<String> propertyIds,
                                   Map<Resource.Type, String> keyPropertyIds,
                                   Resource.Type resourceType) {
    super(propertyIds, keyPropertyIds);
    this.resourceType = resourceType;
  }


  // ----- PrivilegeResourceProvider ----------------------------------------

  /**
   * Static initialization.
   *
   * @param privDAO  the privilege data access object
   * @param usrDAO   the user data access object
   * @param grpDAO   the group data access object
   * @param prinDAO  the principal data access object
   * @param permDAO  the permission data access object
   * @param resDAO   the resource data access object
   */
  public static void init(PrivilegeDAO privDAO, UserDAO usrDAO, GroupDAO grpDAO, PrincipalDAO prinDAO,
                          PermissionDAO permDAO, ResourceDAO resDAO) {
    privilegeDAO  = privDAO;
    userDAO       = usrDAO;
    groupDAO      = grpDAO;
    principalDAO  = prinDAO;
    permissionDAO = permDAO;
    resourceDAO   = resDAO;
  }

  /**
   * Get the entities for the owning resources from the given properties.
   *
   * @param properties  the set of properties
   *
   * @return the entities
   */
  public abstract Map<Long, T> getResourceEntities(Map<String, Object> properties);


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
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Long>     resourceIds  = new HashSet<Long>();

    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    if (propertyMaps.isEmpty()) {
      propertyMaps.add(Collections.<String, Object>emptyMap());
    }

    for (Map<String, Object> properties : propertyMaps) {
      Map<Long, T> resourceEntities = getResourceEntities(properties);

      resourceIds.addAll(resourceEntities.keySet());

      Map<Long, PrivilegeEntity> entityMap     = new HashMap<Long, PrivilegeEntity>();
      List<PrincipalEntity>      principalList = new LinkedList<PrincipalEntity>();


      List<PrivilegeEntity> entities = privilegeDAO.findAll();

      for(PrivilegeEntity privilegeEntity : entities){
        if (resourceIds.contains(privilegeEntity.getResource().getId())) {
          PrincipalEntity principal = privilegeEntity.getPrincipal();
          entityMap.put(principal.getId(), privilegeEntity);
          principalList.add(principal);
        }
      }

      Map<Long, UserEntity> userEntities = new HashMap<Long, UserEntity>();
      List<UserEntity>      userList     = userDAO.findUsersByPrincipal(principalList);

      for (UserEntity userEntity : userList) {
        userEntities.put(userEntity.getPrincipal().getId(), userEntity);
      }

      Map<Long, GroupEntity> groupEntities = new HashMap<Long, GroupEntity>();
      List<GroupEntity>      groupList     = groupDAO.findGroupsByPrincipal(principalList);

      for (GroupEntity groupEntity : groupList) {
        groupEntities.put(groupEntity.getPrincipal().getId(), groupEntity);
      }

      for(PrivilegeEntity privilegeEntity : entityMap.values()){
        Resource resource = toResource(privilegeEntity, userEntities, groupEntities, resourceEntities, requestedIds);
        if (predicate == null || predicate.evaluate(resource)) {
          resources.add(resource);
        }
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
    modifyResources(getDeleteCommand(predicate));
    notifyDelete(Resource.Type.ViewInstance, predicate);
    return getRequestStatus(null);
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<String>(getKeyPropertyIds().values());
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Check to see if the given privilege entity's permission is allowable for the
   * resource type.
   *
   * @param entity  the privilege entity
   *
   * @throws AmbariException if the the privilege permission is not allowable for the resource type
   */
  protected boolean checkResourceTypes(PrivilegeEntity entity) throws AmbariException {
    Integer resourceType           = entity.getResource().getResourceType().getId();
    Integer permissionResourceType = entity.getPermission().getResourceType().getId();

    return resourceType.equals(permissionResourceType);
  }

  /**
   * Convert the given privilege entity into a Resource.
   *
   * @param privilegeEntity   the privilege entity to be converted
   * @param userEntities      the map of user entities keyed by resource id
   * @param groupEntities     the map of group entities keyed by resource id
   * @param resourceEntities  the map of resource entities keyed by resource id
   * @param requestedIds      the requested property ids
   *
   * @return the resource
   */
  protected Resource toResource(PrivilegeEntity privilegeEntity,
                                Map<Long, UserEntity> userEntities,
                                Map<Long, GroupEntity> groupEntities,
                                Map<Long, T> resourceEntities,
                                Set<String> requestedIds) {
    Resource resource = new ResourceImpl(resourceType);

    setResourceProperty(resource, PRIVILEGE_ID_PROPERTY_ID,
        privilegeEntity.getId(), requestedIds);
    setResourceProperty(resource, PERMISSION_NAME_PROPERTY_ID,
        privilegeEntity.getPermission().getPermissionName(), requestedIds);

    PrincipalEntity principal   = privilegeEntity.getPrincipal();
    Long            principalId = principal.getId();

    if (userEntities.containsKey(principalId)) {
      UserEntity userEntity = userEntities.get(principalId);
      setResourceProperty(resource, PRINCIPAL_NAME_PROPERTY_ID, userEntity.getUserName(), requestedIds);
    } else if (groupEntities.containsKey(principalId)){
      GroupEntity groupEntity = groupEntities.get(principalId);
      setResourceProperty(resource, PRINCIPAL_NAME_PROPERTY_ID, groupEntity.getGroupName(), requestedIds);
    }

    setResourceProperty(resource, PRINCIPAL_TYPE_PROPERTY_ID, principal.getPrincipalType().getName(), requestedIds);
    return resource;
  }

  /**
   * Convert the given map of properties to a privilege entity for the resource
   * identified by the given id.
   *
   * @param properties  the property map
   * @param resourceId  the resource id
   *
   * @return the new privilege entity
   */
  protected PrivilegeEntity toEntity(Map<String, Object> properties, Long resourceId) {
    PrivilegeEntity entity         = new PrivilegeEntity();
    String          permissionName = (String) properties.get(PERMISSION_NAME_PROPERTY_ID);
    ResourceEntity  resourceEntity = resourceDAO.findById(resourceId);

    entity.setResource(resourceEntity);
    entity.setPermission(permissionDAO.findPermissionByNameAndType(permissionName, resourceEntity.getResourceType()));

    String principalName = (String) properties.get(PRINCIPAL_NAME_PROPERTY_ID);
    String principalType = (String) properties.get(PRINCIPAL_TYPE_PROPERTY_ID);

    if (PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME.equalsIgnoreCase(principalType)) {
      GroupEntity groupEntity = groupDAO.findGroupByName(principalName);
      if (groupEntity != null) {
        entity.setPrincipal(principalDAO.findById(groupEntity.getPrincipal().getId()));
      }
    } else if (PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME.equalsIgnoreCase(principalType)) {
      UserEntity userEntity = userDAO.findLocalUserByName(principalName);
      if (userEntity != null) {
        entity.setPrincipal(principalDAO.findById(userEntity.getPrincipal().getId()));
      }
    }
    return entity;
  }

  // Create a create command with the given properties map.
  private Command<Void> getCreateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {

        // for a create there should only be one resource ...
        Set<Long> resourceIds = getResourceEntities(properties).keySet();
        Long      resourceId  = resourceIds.iterator().next();

        PrivilegeEntity entity = toEntity(properties, resourceId);

        if (entity.getPrincipal() == null) {
          throw new AmbariException("Can't find principal " + properties.get(PRINCIPAL_TYPE_PROPERTY_ID) +
              " " + properties.get(PRINCIPAL_NAME_PROPERTY_ID) + " for privilege.");
        }
        if (privilegeDAO.exists(entity)) {
            throw new DuplicateResourceException("The privilege already exists.");
        }
        if (!checkResourceTypes(entity)) {
          throw new AmbariException("Can't grant " + entity.getPermission().getResourceType().getName() +
              " permission on a " + entity.getResource().getResourceType().getName() + " resource.");
        }

        privilegeDAO.create(entity);
        return null;
      }
    };
  }

  // Create a delete command with the given predicate.
  private Command<Void> getDeleteCommand(final Predicate predicate) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        try {
          Set<Resource> resources = getResources(PropertyHelper.getReadRequest(), predicate);
          for (Resource resource : resources) {

            PrivilegeEntity entity =
                privilegeDAO.findById((Integer) resource.getPropertyValue(PRIVILEGE_ID_PROPERTY_ID));

            if (entity != null) {
              privilegeDAO.remove(entity);
            }
          }
        } catch (Exception e) {
          throw new AmbariException("Caught exception deleting privilege.", e);
        }
        return null;
      }
    };
  }
}

