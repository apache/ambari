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

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for cluster privileges.
 */
public class ClusterPrivilegeResourceProvider extends PrivilegeResourceProvider<ClusterEntity>{

  /**
   * Data access object used to obtain privilege entities.
   */
  protected static ClusterDAO clusterDAO;

  /**
   * Cluster privilege property id constants.
   */
  protected static final String PRIVILEGE_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("PrivilegeInfo", "cluster_name");

  /**
   * The property ids for a privilege resource.
   */
  private static Set<String> propertyIds = new HashSet<String>();
  static {
    propertyIds.add(PRIVILEGE_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(PRIVILEGE_ID_PROPERTY_ID);
    propertyIds.add(PERMISSION_NAME_PROPERTY_ID);
    propertyIds.add(PRINCIPAL_NAME_PROPERTY_ID);
    propertyIds.add(PRINCIPAL_TYPE_PROPERTY_ID);
  }

  /**
   * The key property ids for a privilege resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();
  static {
    keyPropertyIds.put(Resource.Type.Cluster, PRIVILEGE_CLUSTER_NAME_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.ClusterPrivilege, PRIVILEGE_ID_PROPERTY_ID);
  }


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an ClusterPrivilegeResourceProvider.
   */
  public ClusterPrivilegeResourceProvider() {
    super(propertyIds, keyPropertyIds, Resource.Type.ClusterPrivilege);
  }


  // ----- ClusterPrivilegeResourceProvider ---------------------------------

  /**
   * Static initialization.
   *
   * @param dao  the cluster data access object
   */
  public static void init(ClusterDAO dao) {
    clusterDAO  = dao;
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- PrivilegeResourceProvider -----------------------------------------

  @Override
  public Map<Long, ClusterEntity> getResourceEntities(Map<String, Object> properties) {

    String clusterName = (String) properties.get(PRIVILEGE_CLUSTER_NAME_PROPERTY_ID);

    if (clusterName == null) {
      Map<Long, ClusterEntity> resourceEntities = new HashMap<Long, ClusterEntity>();

      List<ClusterEntity> clusterEntities = clusterDAO.findAll();

      for (ClusterEntity clusterEntity : clusterEntities) {
        resourceEntities.put(clusterEntity.getResource().getId(), clusterEntity);
      }
      return resourceEntities;
    }
    ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);
    return Collections.singletonMap(clusterEntity.getResource().getId(), clusterEntity);
  }


  // ----- helper methods ----------------------------------------------------

  @Override
  protected Resource toResource(PrivilegeEntity privilegeEntity,
                                Map<Long, UserEntity> userEntities,
                                Map<Long, GroupEntity> groupEntities,
                                Map<Long, ClusterEntity> resourceEntities,
                                Set<String> requestedIds) {

    Resource resource = super.toResource(privilegeEntity, userEntities, groupEntities, resourceEntities, requestedIds);
    if (resource != null) {
      ClusterEntity clusterEntity = resourceEntities.get(privilegeEntity.getResource().getId());
      setResourceProperty(resource, PRIVILEGE_CLUSTER_NAME_PROPERTY_ID, clusterEntity.getClusterName(), requestedIds);
    }
    return resource;
  }
}

