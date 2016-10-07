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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentHistoryEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentVersionEntity;

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
   * @param serviceName
   *          the service name (not {@code null})
   * @param componentName
   *          the component name (not {@code null})
   */
  @RequiresSession
  public ServiceComponentDesiredStateEntity findByName(long clusterId, String serviceName,
      String componentName) {
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<ServiceComponentDesiredStateEntity> query = entityManager.createNamedQuery(
        "ServiceComponentDesiredStateEntity.findByName", ServiceComponentDesiredStateEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);

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
  public void removeByName(long clusterId, String serviceName, String componentName) {
    ServiceComponentDesiredStateEntity entity = findByName(clusterId, serviceName, componentName);
    if (null != entity) {
      entityManagerProvider.get().remove(entity);
    }
  }

  /**
   * Creates a service component upgrade/downgrade historical event.
   *
   * @param serviceComponentHistoryEntity
   */
  @Transactional
  public void create(ServiceComponentHistoryEntity serviceComponentHistoryEntity) {
    entityManagerProvider.get().persist(serviceComponentHistoryEntity);
  }

  /**
   * Merges a service component upgrade/downgrade historical event, creating it
   * in the process if it does not already exist. The associated
   * {@link ServiceComponentDesiredStateEntity} is automatically merged via its
   * {@link CascadeType}.
   *
   * @param serviceComponentHistoryEntity
   * @return
   */
  @Transactional
  public ServiceComponentHistoryEntity merge(
      ServiceComponentHistoryEntity serviceComponentHistoryEntity) {
    return entityManagerProvider.get().merge(serviceComponentHistoryEntity);
  }

  /**
   * Gets the history for a component.
   *
   * @param clusterId
   *          the component's cluster.
   * @param serviceName
   *          the component's service (not {@code null}).
   * @param componentName
   *          the component's name (not {@code null}).
   * @return
   */
  @RequiresSession
  public List<ServiceComponentHistoryEntity> findHistory(long clusterId, String serviceName,
      String componentName) {
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<ServiceComponentHistoryEntity> query = entityManager.createNamedQuery(
        "ServiceComponentHistoryEntity.findByComponent", ServiceComponentHistoryEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);

    return daoUtils.selectList(query);
  }

  /**
   * @param clusterId     the cluster id
   * @param serviceName   the service name
   * @param componentName the component name
   * @return the list of repository versions for a component
   */
  @RequiresSession
  public List<ServiceComponentVersionEntity> findVersions(long clusterId, String serviceName,
      String componentName) {
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<ServiceComponentVersionEntity> query = entityManager.createNamedQuery(
        "ServiceComponentVersionEntity.findByComponent", ServiceComponentVersionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);

    return daoUtils.selectList(query);
  }

}
