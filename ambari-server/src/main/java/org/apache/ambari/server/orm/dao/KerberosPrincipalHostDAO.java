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

package org.apache.ambari.server.orm.dao;


import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.KerberosPrincipalHostEntity;
import org.apache.ambari.server.orm.entities.KerberosPrincipalHostEntityPK;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;


/**
 * HostKerberosPrincipal Data Access Object.
 */
@Singleton
public class KerberosPrincipalHostDAO {

  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * Make an instance managed and persistent.
   *
   * @param kerberosPrincipalHostEntity entity to persist
   */
  @Transactional
  public void create(KerberosPrincipalHostEntity kerberosPrincipalHostEntity) {
    entityManagerProvider.get().persist(kerberosPrincipalHostEntity);
  }

  public void create(String principal, Long hostId) {
    create(new KerberosPrincipalHostEntity(principal, hostId));
  }

  /**
   * Merge the state of the given entity into the current persistence context.
   *
   * @param kerberosPrincipalHostEntity entity to merge
   * @return the merged entity
   */
  @Transactional
  public KerberosPrincipalHostEntity merge(KerberosPrincipalHostEntity kerberosPrincipalHostEntity) {
    return entityManagerProvider.get().merge(kerberosPrincipalHostEntity);
  }

  /**
   * Remove the entity instance.
   *
   * @param kerberosPrincipalHostEntity entity to remove
   */
  @Transactional
  public void remove(KerberosPrincipalHostEntity kerberosPrincipalHostEntity) {
    entityManagerProvider.get().remove(merge(kerberosPrincipalHostEntity));
  }

  /**
   * Refresh the state of the instance from the database,
   * overwriting changes made to the entity, if any.
   *
   * @param kerberosPrincipalHostEntity entity to refresh
   */
  @Transactional
  public void refresh(KerberosPrincipalHostEntity kerberosPrincipalHostEntity) {
    entityManagerProvider.get().refresh(kerberosPrincipalHostEntity);
  }

  /**
   * Finds KerberosPrincipalHostEntities for the requested principal
   *
   * @param principalName a String indicating the name of the requested principal
   * @return a List of requested KerberosPrincipalHostEntities or null if none were found
   */
  @RequiresSession
  public List<KerberosPrincipalHostEntity> findByPrincipal(String principalName) {
    final TypedQuery<KerberosPrincipalHostEntity> query = entityManagerProvider.get()
        .createNamedQuery("KerberosPrincipalHostEntityFindByPrincipal", KerberosPrincipalHostEntity.class);
    query.setParameter("principalName", principalName);
    return query.getResultList();
  }

  /**
   * Find KerberosPrincipalHostEntities for the requested host
   *
   * @param hostId a Long indicating the id of the requested host
   * @return a List of requested KerberosPrincipalHostEntities or null if none were found
   */
  @RequiresSession
  public List<KerberosPrincipalHostEntity> findByHost(Long hostId) {
    final TypedQuery<KerberosPrincipalHostEntity> query = entityManagerProvider.get()
        .createNamedQuery("KerberosPrincipalHostEntityFindByHost", KerberosPrincipalHostEntity.class);
    query.setParameter("hostId", hostId);
    return query.getResultList();
  }

  /**
   * Find the KerberosPrincipalHostEntity for the specified primary key
   *
   * @param primaryKey a KerberosPrincipalHostEntityPK containing the requested principal and host names
   * @return the KerberosPrincipalHostEntity or null if not found
   */
  @RequiresSession
  public KerberosPrincipalHostEntity find(KerberosPrincipalHostEntityPK primaryKey) {
    return entityManagerProvider.get().find(KerberosPrincipalHostEntity.class, primaryKey);
  }

  /**
   * Find the KerberosPrincipalHostEntity for the requested principal name and host
   *
   * @param principalName a String indicating the name of the requested principal
   * @param hostId        a Long indicating the id of the requested host
   * @return the KerberosPrincipalHostEntity or null if not found
   */
  @RequiresSession
  public KerberosPrincipalHostEntity find(String principalName, Long hostId) {
    return entityManagerProvider.get().find(KerberosPrincipalHostEntity.class,
        new KerberosPrincipalHostEntityPK(principalName, hostId));
  }

  /**
   * Find all KerberosPrincipalHostEntities.
   *
   * @return a List of requested KerberosPrincipalHostEntities or null if none were found
   */
  @RequiresSession
  public List<KerberosPrincipalHostEntity> findAll() {
    TypedQuery<KerberosPrincipalHostEntity> query = entityManagerProvider.get().
        createNamedQuery("KerberosPrincipalHostEntityFindAll", KerberosPrincipalHostEntity.class);

    return query.getResultList();
  }


  /**
   * Remove KerberosPrincipalHostEntity instances for the specified principal name
   *
   * @param principalName a String indicating the name of the principal
   */
  @Transactional
  public void removeByPrincipal(String principalName) {
    remove(findByPrincipal(principalName));
  }

  /**
   * Remove KerberosPrincipalHostEntity instances for the specified host
   *
   * @param hostId a Long indicating the id of the host
   */
  @Transactional
  public void removeByHost(Long hostId) {
    remove(findByHost(hostId));
  }

  /**
   * Remove KerberosPrincipalHostEntity instance for the specified principal and host
   *
   * @param principalName a String indicating the name of the principal
   * @param hostId        a Long indicating the id of the host
   * @see #remove(org.apache.ambari.server.orm.entities.KerberosPrincipalHostEntity)
   */
  @Transactional
  public void remove(String principalName, Long hostId) {
    remove(new KerberosPrincipalHostEntity(principalName, hostId));
  }

  /**
   * Tests the existence of a principal on at least one host
   *
   * @param principalName a String indicating the name of the principal to test
   * @return true if a principal is related to one or more hosts; otherwise false
   */
  @RequiresSession
  public boolean exists(String principalName) {
    List<KerberosPrincipalHostEntity> foundEntries = findByPrincipal(principalName);
    return (foundEntries != null) && !foundEntries.isEmpty();
  }

  /**
   * Tests the existence of a particular principal on a specific host
   *
   * @param principalName a String indicating the name of the principal to test
   * @param hostId      a Long indicating the id of the host to test
   * @return true if the requested principal exists
   */
  @RequiresSession
  public boolean exists(String principalName, Long hostId) {
    return find(principalName, hostId) != null;
  }

  /**
   * Removes multiple KerberosPrincipalHostEntity items
   *
   * @param entities a collection of KerberosPrincipalHostEntity items to remove
   */
  private void remove(List<KerberosPrincipalHostEntity> entities) {
    if (entities != null) {
      for (KerberosPrincipalHostEntity entity : entities) {
        entityManagerProvider.get().remove(entity);
      }
    }
  }

}
