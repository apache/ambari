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
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityPK;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * View Instance Data Access Object.
 */
@Singleton
public class ViewInstanceDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * Find a view with the given names.
   *
   * @param viewName      name of view
   * @param instanceName  name of the instance
   *
   * @return  a matching view instance or null
   */
  public ViewInstanceEntity findByName(String viewName, String instanceName) {
    EntityManager entityManager = entityManagerProvider.get();
    ViewInstanceEntityPK pk = new ViewInstanceEntityPK();
    pk.setViewName(viewName);
    pk.setName(instanceName);

    return entityManager.find(ViewInstanceEntity.class, pk);
  }

  /**
   * Find all view instances.
   *
   * @return all views or an empty List
   */
  public List<ViewInstanceEntity> findAll() {
    TypedQuery<ViewInstanceEntity> query = entityManagerProvider.get().
        createNamedQuery("allViewInstances", ViewInstanceEntity.class);

    return query.getResultList();
  }

  /**
   * Refresh the state of the instance from the database,
   * overwriting changes made to the entity, if any.
   *
   * @param ViewInstanceEntity  entity to refresh
   */
  @Transactional
  public void refresh(ViewInstanceEntity ViewInstanceEntity) {
    entityManagerProvider.get().refresh(ViewInstanceEntity);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param ViewInstanceEntity  entity to persist
   */
  @Transactional
  public void create(ViewInstanceEntity ViewInstanceEntity) {
    entityManagerProvider.get().persist(ViewInstanceEntity);
  }

  /**
   * Merge the state of the given entity into the current persistence context.
   *
   * @param ViewInstanceEntity  entity to merge
   * @return the merged entity
   */
  @Transactional
  public ViewInstanceEntity merge(ViewInstanceEntity ViewInstanceEntity) {
    return entityManagerProvider.get().merge(ViewInstanceEntity);
  }

  /**
   * Remove the entity instance.
   *
   * @param ViewInstanceEntity  entity to remove
   */
  @Transactional
  public void remove(ViewInstanceEntity ViewInstanceEntity) {
    entityManagerProvider.get().remove(merge(ViewInstanceEntity));
  }
}
