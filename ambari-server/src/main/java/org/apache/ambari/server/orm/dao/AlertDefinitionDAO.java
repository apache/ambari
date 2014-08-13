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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.alert.Scope;

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
   * Gets all alert definitions for the given service in the specified cluster.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param serviceName
   *          the name of the service.
   *
   * @return all alert definitions for the service or empty list if none exist
   *         (never {@code null}).
   */
  public List<AlertDefinitionEntity> findByService(long clusterId,
      String serviceName) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByService", AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert definitions for the specified services that do not have a
   * component. These definitions are assumed to be run on the master hosts.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param services
   *          the services to match on.
   *
   * @return all alert definitions for the services or empty list if none exist
   *         (never {@code null}).
   */
  public List<AlertDefinitionEntity> findByServiceMaster(long clusterId,
      Set<String> services) {
    if (null == services || services.size() == 0) {
      return Collections.emptyList();
    }

    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByServiceMaster",
        AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("services", services);
    query.setParameter("scope", Scope.SERVICE);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert definitions that are not bound to a particular service. An
   * example of this type of definition is a host capacity alert.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param serviceName
   *          the name of the service (not {@code null}).
   * @param componentName
   *          the name of the service component (not {@code null}).
   * @return all alert definitions that are not bound to a service or an empty
   *         list (never {@code null}).
   */
  public List<AlertDefinitionEntity> findByServiceComponent(long clusterId,
      String serviceName, String componentName) {
    if (null == serviceName || null == componentName) {
      return Collections.emptyList();
    }

    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByServiceAndComponent",
        AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert definitions that are not bound to a particular service. An
   * example of this type of definition is a host capacity alert.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @return all alert definitions that are not bound to a service or an empty
   *         list (never {@code null}).
   */
  public List<AlertDefinitionEntity> findAgentScoped(long clusterId) {
    TypedQuery<AlertDefinitionEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertDefinitionEntity.findByServiceAndComponent",
        AlertDefinitionEntity.class);

    query.setParameter("clusterId", clusterId);

    query.setParameter("serviceName",
        RootServiceResponseFactory.Services.AMBARI.name());

    query.setParameter("componentName",
        RootServiceResponseFactory.Components.AMBARI_AGENT.name());

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
    if (null != alertDefinition) {
      entityManager.remove(alertDefinition);
    }
  }
}
