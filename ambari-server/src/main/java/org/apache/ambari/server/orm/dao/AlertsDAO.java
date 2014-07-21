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
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;

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
  @RequiresSession
  public AlertHistoryEntity findById(long alertId) {
    return entityManagerProvider.get().find(AlertHistoryEntity.class, alertId);
  }

  /**
   * Gets all alerts stored in the database across all clusters.
   * 
   * @return all alerts or an empty list if none exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll() {
    TypedQuery<AlertHistoryEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAll", AlertHistoryEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alerts stored in the database for the given cluster.
   * 
   * @return all alerts in the specified cluster or an empty list if none exist
   *         (never {@code null}).
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll(long clusterId) {
    TypedQuery<AlertHistoryEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAllInCluster", AlertHistoryEntity.class);

    query.setParameter("clusterId", clusterId);

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
    entityManagerProvider.get().remove(merge(alert));
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
