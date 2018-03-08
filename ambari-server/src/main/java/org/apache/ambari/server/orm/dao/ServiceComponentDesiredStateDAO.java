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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ServiceComponentDesiredStateDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils daoUtils;

  /**
   * Gets a {@link ServiceComponentDesiredStateEntity} by its PK ID.
   *
   * @param id
   *          the ID.
   * @return the entity or {@code null} if it does not exist.
   */
  @RequiresSession
  public ServiceComponentDesiredStateEntity findById(long id) {
    return entityManagerProvider.get().find(ServiceComponentDesiredStateEntity.class, id);
  }

  @RequiresSession
  public List<ServiceComponentDesiredStateEntity> findAll() {
    TypedQuery<ServiceComponentDesiredStateEntity> query =
      entityManagerProvider.get().
        createQuery("SELECT scd from ServiceComponentDesiredStateEntity scd", ServiceComponentDesiredStateEntity.class);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  /**
   * Finds a {@link ServiceComponentDesiredStateEntity} by a combination of
   * cluster, service, and component.
   *
   * @param clusterId
   *          the cluster ID
   * @param serviceGroupId
   *          the service group ID
   * @param serviceId
   *          the service ID
   * @param componentName
   *          the component name (not {@code null})
   */
  @RequiresSession
  public ServiceComponentDesiredStateEntity findByName(long clusterId, long serviceGroupId, long serviceId,
       String componentName, String componentType) {
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<ServiceComponentDesiredStateEntity> query = entityManager.createNamedQuery(
        "ServiceComponentDesiredStateEntity.findByName", ServiceComponentDesiredStateEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceGroupId", serviceGroupId);
    query.setParameter("serviceId", serviceId);
    query.setParameter("componentName", componentName);
    query.setParameter("componentType", componentType);

    ServiceComponentDesiredStateEntity entity = null;
    List<ServiceComponentDesiredStateEntity> entities = daoUtils.selectList(query);
    if (null != entities && !entities.isEmpty()) {
      entity = entities.get(0);
    }

    return entity;
  }

  /**
   * Finds a {@link ServiceComponentDesiredStateEntity} by a combination of
   * cluster, service, and component.
   *
   * @param clusterId
   *          the cluster ID
   * @param serviceGroupId
   *          the service group ID
   * @param serviceId
   *          the service ID
   * @param componentId
   *          the component id (not {@code null})
   */
  @RequiresSession
  public ServiceComponentDesiredStateEntity findById(long clusterId, long serviceGroupId, long serviceId,
                                                       Long componentId) {
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<ServiceComponentDesiredStateEntity> query = entityManager.createNamedQuery(
            "ServiceComponentDesiredStateEntity.findById", ServiceComponentDesiredStateEntity.class);

    query.setParameter("id", componentId);

    ServiceComponentDesiredStateEntity entity = null;
    List<ServiceComponentDesiredStateEntity> entities = daoUtils.selectList(query);
    if (null != entities && !entities.isEmpty()) {
      entity = entities.get(0);
    }

    return entity;
  }

  @Transactional
  public void refresh(ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    entityManagerProvider.get().refresh(serviceComponentDesiredStateEntity);
  }

  @Transactional
  public void create(ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    entityManagerProvider.get().persist(serviceComponentDesiredStateEntity);
  }

  @Transactional
  public ServiceComponentDesiredStateEntity merge(ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    return entityManagerProvider.get().merge(serviceComponentDesiredStateEntity);
  }

  @Transactional
  public void remove(ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    entityManagerProvider.get().remove(serviceComponentDesiredStateEntity);
  }

  @Transactional
  public void removeByName(long clusterId, long serviceGroupId, long serviceId, String componentName, String componentType) {
    ServiceComponentDesiredStateEntity entity = findByName(clusterId, serviceGroupId, serviceId, componentName, componentType);
    if (null != entity) {
      entityManagerProvider.get().remove(entity);
    }
  }
}
