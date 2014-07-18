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
import org.apache.ambari.server.orm.entities.PermissionEntity;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Permission Data Access Object.
 */
@Singleton
public class PermissionDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Find a permission entity with the given id.
   *
   * @param id  type id
   *
   * @return  a matching permission entity or null
   */
  public PermissionEntity findById(Integer id) {
    return entityManagerProvider.get().find(PermissionEntity.class, id);
  }

  /**
   * Find all permission entities.
   *
   * @return all entities or an empty List
   */
  public List<PermissionEntity> findAll() {
    TypedQuery<PermissionEntity> query = entityManagerProvider.get().createQuery("SELECT resource FROM PermissionEntity resource", PermissionEntity.class);
    return daoUtils.selectList(query);
  }

  /**
   * Find a permission entity by name.
   *
   * @param name  the permission name
   *
   * @return  a matching permission entity or null
   */
  public PermissionEntity findPermissionByName(String name) {
    final TypedQuery<PermissionEntity> query = entityManagerProvider.get().createNamedQuery("permissionByName", PermissionEntity.class);
    query.setParameter("permissionname", name);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
