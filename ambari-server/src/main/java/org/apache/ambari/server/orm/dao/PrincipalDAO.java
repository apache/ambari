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
import org.apache.ambari.server.orm.entities.PrincipalEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Principal Data Access Object.
 */
@Singleton
public class PrincipalDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Find a principal with the given id.
   *
   *
   * @param id  type id
   *
   * @return  a matching principal type  or null
   */
  public PrincipalEntity findById(Long id) {
    return entityManagerProvider.get().find(PrincipalEntity.class, id);
  }

  /**
   * Find all principals.
   *
   * @return all principals or an empty List
   */
  public List<PrincipalEntity> findAll() {
    TypedQuery<PrincipalEntity> query = entityManagerProvider.get().createQuery("SELECT principal FROM PrincipalEntity principal", PrincipalEntity.class);
    return daoUtils.selectList(query);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param entity  entity to store
   */
  @Transactional
  public void create(PrincipalEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Merge the given entity.
   *
   * @param entity  the entity
   *
   * @return the managed entity
   */
  @Transactional
  public PrincipalEntity merge(PrincipalEntity entity) {
    return entityManagerProvider.get().merge(entity);
  }
}
