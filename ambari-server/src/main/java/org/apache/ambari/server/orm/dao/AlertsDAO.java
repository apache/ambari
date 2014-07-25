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
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link AlertsDAO} class manages the {@link AlertHistoryEntity} and
 * {@link AlertCurrentEntity} instances. Each {@link AlertHistoryEntity} is
 * known as an "alert" that has been triggered and received.
 */
@Singleton
public class AlertsDAO {
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
   * Gets an alert with the specified ID.
   * 
   * @param alertId
   *          the ID of the alert to retrieve.
   * @return the alert or {@code null} if none exists.
   */
  public AlertHistoryEntity findById(long alertId) {
    return entityManagerProvider.get().find(AlertHistoryEntity.class, alertId);
  }

  /**
   * Gets all alerts stored in the database across all clusters.
   * 
   * @return all alerts or an empty list if none exist (never {@code null}).
   */
  public List<AlertHistoryEntity> findAll() {
    TypedQuery<AlertHistoryEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAll", AlertHistoryEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alerts stored in the database for the given cluster.
   * 
   * @param clusterId
   *          the ID of the cluster.
   * @return all alerts in the specified cluster or an empty list if none exist
   *         (never {@code null}).
   */
  public List<AlertHistoryEntity> findAll(long clusterId) {
    TypedQuery<AlertHistoryEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAllInCluster", AlertHistoryEntity.class);

    query.setParameter("clusterId", clusterId);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alerts stored in the database for the given cluster that have one
   * of the specified alert states.
   * 
   * @param clusterId
   *          the ID of the cluster.
   * @param alertStates
   *          the states to match for the retrieved alerts (not {@code null}).
   * @return the alerts matching the specified states and cluster, or an empty
   *         list if none.
   */
  public List<AlertHistoryEntity> findAll(long clusterId,
      List<AlertState> alertStates) {
    if (null == alertStates || alertStates.size() == 0) {
      return Collections.emptyList();
    }

    TypedQuery<AlertHistoryEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAllInClusterWithState",
        AlertHistoryEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("alertStates", alertStates);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alerts stored in the database for the given cluster and that fall
   * withing the specified date range. Dates are expected to be in milliseconds
   * since the epoch, normalized to UTC time.
   * 
   * @param clusterId
   *          the ID of the cluster.
   * @param startDate
   *          the date that the earliest entry must occur after, normalized to
   *          UTC, or {@code null} for all entries that occur before the given
   *          end date.
   * @param endDate
   *          the date that the latest entry must occur before, normalized to
   *          UTC, or {@code null} for all entries that occur after the given
   *          start date.
   * @return the alerts matching the specified date range.
   */
  public List<AlertHistoryEntity> findAll(long clusterId, Date startDate,
      Date endDate) {
    if (null == startDate && null == endDate)
      return Collections.emptyList();

    TypedQuery<AlertHistoryEntity> query = null;

    if (null != startDate && null != endDate) {
      if (startDate.after(endDate)) {
        return Collections.emptyList();
      }

      query = entityManagerProvider.get().createNamedQuery(
          "AlertHistoryEntity.findAllInClusterBetweenDates",
          AlertHistoryEntity.class);

      query.setParameter("clusterId", clusterId);
      query.setParameter("startDate", startDate.getTime());
      query.setParameter("endDate", endDate.getTime());
    } else if (null != startDate) {
      query = entityManagerProvider.get().createNamedQuery(
          "AlertHistoryEntity.findAllInClusterAfterDate",
          AlertHistoryEntity.class);

      query.setParameter("clusterId", clusterId);
      query.setParameter("afterDate", startDate.getTime());
    } else if (null != endDate) {
      query = entityManagerProvider.get().createNamedQuery(
          "AlertHistoryEntity.findAllInClusterBeforeDate",
          AlertHistoryEntity.class);

      query.setParameter("clusterId", clusterId);
      query.setParameter("beforeDate", endDate.getTime());
    }

    if (null == query)
      return Collections.emptyList();

    return daoUtils.selectList(query);
  }

  /**
   * Gets the current alerts.
   * 
   * @return the current alerts or an empty list if none exist (never
   *         {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrent() {
    TypedQuery<AlertCurrentEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findAll", AlertCurrentEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets a current alert with the specified ID.
   * 
   * @param alertId
   *          the ID of the alert to retrieve.
   * @return the alert or {@code null} if none exists.
   */
  @RequiresSession
  public AlertCurrentEntity findCurrentById(long alertId) {
    return entityManagerProvider.get().find(AlertCurrentEntity.class, alertId);
  }

  /**
   * Gets the current alerts for a given service.
   * 
   * @return the current alerts for the given service or an empty list if none
   *         exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrentByService(long clusterId,
      String serviceName) {
    TypedQuery<AlertCurrentEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findByService", AlertCurrentEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the current alerts for a given host.
   * 
   * @return the current alerts for the given host or an empty list if none
   *         exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrentByHost(long clusterId,
      String hostName) {
    TypedQuery<AlertCurrentEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findByHost", AlertCurrentEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("hostName", hostName);

    return daoUtils.selectList(query);
  }

  /**
   * Removes alert history and current alerts for the specified alert defintiion
   * ID. This will invoke {@link EntityManager#clear()} when completed since the
   * JPQL statement will remove entries without going through the EM.
   * 
   * @param definitionId
   *          the ID of the definition to remove.
   */
  @Transactional
  public void removeByDefinitionId(long definitionId) {
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<AlertCurrentEntity> currentQuery = entityManager.createNamedQuery(
        "AlertCurrentEntity.removeByDefinitionId", AlertCurrentEntity.class);

    currentQuery.setParameter("definitionId", definitionId);
    currentQuery.executeUpdate();

    TypedQuery<AlertHistoryEntity> historyQuery = entityManager.createNamedQuery(
        "AlertHistoryEntity.removeByDefinitionId", AlertHistoryEntity.class);

    historyQuery.setParameter("definitionId", definitionId);
    historyQuery.executeUpdate();

    entityManager.clear();
  }

  /**
   * Remove a current alert whose history entry matches the specfied ID.
   * 
   * @param   historyId the ID of the history entry.
   * @return  the number of alerts removed.
   */
  @Transactional
  public int removeCurrentByHistoryId(long historyId) {
    TypedQuery<AlertCurrentEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.removeByHistoryId", AlertCurrentEntity.class);

    query.setParameter("historyId", historyId);
    return query.executeUpdate();
  }

  /**
   * Persists a new alert.
   * 
   * @param alert
   *          the alert to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertHistoryEntity alert) {
    entityManagerProvider.get().persist(alert);
  }

  /**
   * Refresh the state of the alert from the database.
   * 
   * @param alert
   *          the alert to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertHistoryEntity alert) {
    entityManagerProvider.get().refresh(alert);
  }

  /**
   * Merge the speicified alert with the existing alert in the database.
   * 
   * @param alert
   *          the alert to merge (not {@code null}).
   * @return the updated alert with merged content (never {@code null}).
   */
  @Transactional
  public AlertHistoryEntity merge(AlertHistoryEntity alert) {
    return entityManagerProvider.get().merge(alert);
  }

  /**
   * Removes the specified alert from the database.
   * 
   * @param alert
   *          the alert to remove.
   */
  @Transactional
  public void remove(AlertHistoryEntity alert) {
    alert = merge(alert);

    removeCurrentByHistoryId(alert.getAlertId());
    entityManagerProvider.get().remove(alert);
  }

  /**
   * Persists a new current alert.
   * 
   * @param alert
   *          the current alert to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertCurrentEntity alert) {
    entityManagerProvider.get().persist(alert);
  }

  /**
   * Refresh the state of the current alert from the database.
   * 
   * @param alert
   *          the current alert to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertCurrentEntity alert) {
    entityManagerProvider.get().refresh(alert);
  }

  /**
   * Merge the speicified current alert with the existing alert in the database.
   * 
   * @param alert
   *          the current alert to merge (not {@code null}).
   * @return the updated current alert with merged content (never {@code null}).
   */
  @Transactional
  public AlertCurrentEntity merge(AlertCurrentEntity alert) {
    return entityManagerProvider.get().merge(alert);
  }

  /**
   * Removes the specified current alert from the database.
   * 
   * @param alert
   *          the current alert to remove.
   */
  @Transactional
  public void remove(AlertCurrentEntity alert) {
    entityManagerProvider.get().remove(merge(alert));
  }
}
