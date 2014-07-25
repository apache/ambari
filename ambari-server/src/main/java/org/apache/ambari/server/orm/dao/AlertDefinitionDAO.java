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

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link AlertDefinitionDAO} class is used to manage the persistence and
 * retrieval of {@link AlertDefinitionEntity} instances.
 */
@Singleton
public class AlertDefinitionDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  DaoUtils daoUtils;

  /**
   * Alert history DAO.
   */
  @Inject
  AlertsDAO alertsDao;

  /**
   * Alert dispatch DAO.
   */
  @Inject
  AlertDispatchDAO dispatchDao;

  /**
   * Gets an alert definition with the specified ID.
   * 
   * @param definitionId
   *          the ID of the definition to retrieve.
   * @return the alert definition or {@code null} if none exists.
   */
  public AlertDefinitionEntity findById(long definitionId) {
    return entityManagerProvider.get().find(AlertDefinitionEntity.class,
        definitionId);
  }

  /**
   * Gets an alert definition with the specified name. Alert definition names
   * are unique within a cluster.
   * 
   * @param clusterId
   *          the ID of the cluster.
   * @param definitionName
   *          the name of the definition (not {@code null}).
   * @return the alert definition or {@code null} if none exists.
   */
  public AlertDefinitionEntity findByName(long clusterId, String definitionName) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByName", AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("definitionName", definitionName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets all alert definitions stored in the database.
   * 
   * @return all alert definitions or an empty list if none exist (never
   *         {@code null}).
   */
  public List<AlertDefinitionEntity> findAll() {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findAll", AlertDefinitionEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert definitions stored in the database.
   * 
   * @return all alert definitions or empty list if none exist (never
   *         {@code null}).
   */
  public List<AlertDefinitionEntity> findAll(long clusterId) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findAllInCluster", AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);

    return daoUtils.selectList(query);
  }

  /**
   * Persists a new alert definition.
   * 
   * @param alertDefinition
   *          the definition to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertDefinitionEntity alertDefinition) {
    entityManagerProvider.get().persist(alertDefinition);
  }

  /**
   * Refresh the state of the alert definition from the database.
   * 
   * @param alertDefinition
   *          the definition to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertDefinitionEntity alertDefinition) {
    entityManagerProvider.get().refresh(alertDefinition);
  }

  /**
   * Merge the speicified alert definition with the existing definition in the
   * database.
   * 
   * @param alertDefinition
   *          the definition to merge (not {@code null}).
   * @return the updated definition with merged content (never {@code null}).
   */
  @Transactional
  public AlertDefinitionEntity merge(AlertDefinitionEntity alertDefinition) {
    return entityManagerProvider.get().merge(alertDefinition);
  }

  /**
   * Removes the specified alert definition and all related history and
   * associations from the database.
   * 
   * @param alertDefinition
   *          the definition to remove.
   */
  @Transactional
  public void remove(AlertDefinitionEntity alertDefinition) {
    alertDefinition = merge(alertDefinition);
    dispatchDao.removeNoticeByDefinitionId(alertDefinition.getDefinitionId());
    alertsDao.removeByDefinitionId(alertDefinition.getDefinitionId());

    EntityManager entityManager = entityManagerProvider.get();

    alertDefinition = findById(alertDefinition.getDefinitionId());
    if (null != alertDefinition)
      entityManager.remove(alertDefinition);
  }
}
