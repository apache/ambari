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

package org.apache.ambari.server.orm.dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Privilege Data Access Object.
 */
@Singleton
public class PrivilegeDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Find a resource with the given id.
   *
   * @param id  type id
   *
   * @return  a matching resource type  or null
   */
  public PrivilegeEntity findById(Integer id) {
    return entityManagerProvider.get().find(PrivilegeEntity.class, id);
  }

  /**
   * Find all resources.
   *
   * @return all resources or an empty List
   */
  public List<PrivilegeEntity> findAll() {
    TypedQuery<PrivilegeEntity> query = entityManagerProvider.get().createQuery("SELECT privilege FROM PrivilegeEntity privilege", PrivilegeEntity.class);
    return daoUtils.selectList(query);
  }

  /**
   * Determine whether or not the given privilege entity exists.
   *
   * @param entity  the privilege entity
   *
   * @return true if the given privilege entity already exists
   */
  public boolean exists(PrivilegeEntity entity) {
    return exists(entity.getPrincipal(), entity.getResource(), entity.getPermission());
  }

  /**
   * Determine whether or not the privilege entity exists defined by the given principal, resource and
   * permission exists.
   *
   * @param principalEntity   the principal entity
   * @param resourceEntity    the resource entity
   * @param permissionEntity  the permission entity
   *
   * @return true if the privilege entity already exists
   */
  public boolean exists(PrincipalEntity principalEntity, ResourceEntity resourceEntity, PermissionEntity permissionEntity) {
    TypedQuery<PrivilegeEntity> query = entityManagerProvider.get().createQuery(
        "SELECT privilege FROM PrivilegeEntity privilege WHERE privilege.principal = :principal AND privilege.resource = :resource AND privilege.permission = :permission", PrivilegeEntity.class);

    query.setParameter("principal", principalEntity);
    query.setParameter("resource", resourceEntity);
    query.setParameter("permission", permissionEntity);

    List<PrivilegeEntity> privilegeEntities = daoUtils.selectList(query);
    return !(privilegeEntities == null || privilegeEntities.isEmpty());
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param entity  entity to persist
   */
  @Transactional
  public void create(PrivilegeEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Merge the state of the given entity into the current persistence context.
   *
   * @param entity  entity to merge
   *
   * @return the merged entity
   */
  @Transactional
  public PrivilegeEntity merge(PrivilegeEntity entity) {
    return entityManagerProvider.get().merge(entity);
  }
  /**
   * Remove the entity instance.
   *
   * @param entity  entity to remove
   */
  @Transactional
  public void remove(PrivilegeEntity entity) {
    entityManagerProvider.get().remove(merge(entity));
  }
}
