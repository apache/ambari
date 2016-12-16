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

import static org.apache.ambari.server.controller.internal.ClusterPrivilegeResourceProvider.PRIVILEGE_CLUSTER_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.ViewPrivilegeResourceProvider.PRIVILEGE_INSTANCE_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.ViewPrivilegeResourceProvider.PRIVILEGE_VIEW_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.ViewPrivilegeResourceProvider.PRIVILEGE_VIEW_VERSION_PROPERTY_ID;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.view.ViewRegistry;

/**
 * Resource provider for Ambari privileges.
 */
public class AmbariPrivilegeResourceProvider extends PrivilegeResourceProvider<Object> {

  public static final String PRIVILEGE_TYPE_PROPERTY_ID  = "PrivilegeInfo/type";

  /**
   * Data access object used to obtain privilege entities.
   */
  protected static ClusterDAO clusterDAO;

  /**
   * The property ids for an Ambari privilege resource.
   */
  private static Set<String> propertyIds = new HashSet<String>();
  static {
    propertyIds.add(PRIVILEGE_ID_PROPERTY_ID);
    propertyIds.add(PERMISSION_NAME_PROPERTY_ID);
    propertyIds.add(PERMISSION_LABEL_PROPERTY_ID);
    propertyIds.add(PRINCIPAL_NAME_PROPERTY_ID);
    propertyIds.add(PRINCIPAL_TYPE_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_VIEW_NAME_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_VIEW_VERSION_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_INSTANCE_NAME_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_TYPE_PROPERTY_ID);

  }

  /**
   * The key property ids for a privilege resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();
  static {
    keyPropertyIds.put(Resource.Type.AmbariPrivilege, PRIVILEGE_ID_PROPERTY_ID);
  }


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an AmbariPrivilegeResourceProvider.
   */
  public AmbariPrivilegeResourceProvider() {
    super(propertyIds, keyPropertyIds, Resource.Type.AmbariPrivilege);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_ASSIGN_ROLES);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredGetAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }

  // ----- AmbariPrivilegeResourceProvider ---------------------------------

  /**
   * Static initialization.
   *
   * @param clusterDao  the cluster data access object
   */
  public static void init(ClusterDAO clusterDao) {
    clusterDAO  = clusterDao;
  }

  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- PrivilegeResourceProvider -----------------------------------------

  @Override
  public Map<Long, Object> getResourceEntities(Map<String, Object> properties) {
    Map<Long, Object> resourceEntities = new HashMap<Long, Object>();

    resourceEntities.put(ResourceEntity.AMBARI_RESOURCE_ID, null);
    // add cluster entities
    List<ClusterEntity> clusterEntities = clusterDAO.findAll();

    if (clusterEntities != null) {
      for (ClusterEntity clusterEntity : clusterEntities) {
        resourceEntities.put(clusterEntity.getResource().getId(), clusterEntity);
      }
    }
    //add view entities
    ViewRegistry viewRegistry = ViewRegistry.getInstance();
    for (ViewEntity viewEntity : viewRegistry.getDefinitions()) {
      if (viewEntity.isDeployed()) {
        for (ViewInstanceEntity viewInstanceEntity : viewEntity.getInstances()) {
          resourceEntities.put(viewInstanceEntity.getResource().getId(), viewInstanceEntity);
        }
      }
    }
    return resourceEntities;
  }

  @Override
  protected Resource toResource(PrivilegeEntity privilegeEntity,
                                Map<Long, UserEntity> userEntities,
                                Map<Long, GroupEntity> groupEntities,
                                Map<Long, PermissionEntity> roleEntities,
                                Map<Long, Object> resourceEntities,
                                Set<String> requestedIds) {
    Resource resource = super.toResource(privilegeEntity, userEntities, groupEntities, roleEntities, resourceEntities, requestedIds);
    if (resource != null) {
      ResourceEntity resourceEntity = privilegeEntity.getResource();
      ResourceTypeEntity type = resourceEntity.getResourceType();
      String typeName = type.getName();
      ResourceType resourceType = ResourceType.translate(typeName);

      if(resourceType != null) {
        switch (resourceType) {
          case AMBARI:
            // there is nothing special to add for this case
            break;
          case CLUSTER:
            ClusterEntity clusterEntity = (ClusterEntity) resourceEntities.get(resourceEntity.getId());
            setResourceProperty(resource, PRIVILEGE_CLUSTER_NAME_PROPERTY_ID, clusterEntity.getClusterName(), requestedIds);
            break;
          case VIEW:
            ViewInstanceEntity viewInstanceEntity = (ViewInstanceEntity) resourceEntities.get(resourceEntity.getId());
            ViewEntity viewEntity = viewInstanceEntity.getViewEntity();

            setResourceProperty(resource, PRIVILEGE_VIEW_NAME_PROPERTY_ID, viewEntity.getCommonName(), requestedIds);
            setResourceProperty(resource, PRIVILEGE_VIEW_VERSION_PROPERTY_ID, viewEntity.getVersion(), requestedIds);
            setResourceProperty(resource, PRIVILEGE_INSTANCE_NAME_PROPERTY_ID, viewInstanceEntity.getName(), requestedIds);
            break;
        }

        setResourceProperty(resource, PRIVILEGE_TYPE_PROPERTY_ID, resourceType.name(), requestedIds);
      }
    }

    return resource;
  }
  @Override
  public Long getResourceEntityId(Predicate predicate) {
    return ResourceEntity.AMBARI_RESOURCE_ID;
  }
}
