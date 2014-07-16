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

import org.apache.ambari.server.orm.RequiresSession;
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
   * Gets an alert definition with the specified ID.
   * 
   * @param definitionId
   *          the ID of the definition to retrieve.
   * @return the alert definition or {@code null} if none exists.
   */
  @RequiresSession
  public AlertDefinitionEntity findById(long definitionId) {
    return entityManagerProvider.get().find(AlertDefinitionEntity.class,
        definitionId);
  }

  /**
   * Gets an alert definition with the specified name.
   * 
   * @param definitionName
   *          the name of the definition (not {@code null}).
   * @return the alert definition or {@code null} if none exists.
   */
  @RequiresSession
  public AlertDefinitionEntity findByName(String definitionName) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByName", AlertDefinitionEntity.class);

    query.setParameter("definitionName", definitionName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets all alert definitions stored in the database.
   * 
   * @return all alert definitions or {@code null} if none exist.
   */
  @RequiresSession
  public List<AlertDefinitionEntity> findAll() {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findAll", AlertDefinitionEntity.class);

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
   * Removes the specified alert definition from the database.
   * 
   * @param alertDefinition
   *          the definition to remove.
   */
  @Transactional
  public void remove(AlertDefinitionEntity alertDefinition) {
    entityManagerProvider.get().remove(merge(alertDefinition));
  }
}
