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

import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link AlertDispatchDAO} class manages the {@link AlertTargetEntity},
 * {@link AlertGroupEntity}, and the associations between them.
 */
@Singleton
public class AlertDispatchDAO {
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
   * Gets an alert group with the specified ID.
   * 
   * @param groupId
   *          the ID of the group to retrieve.
   * @return the group or {@code null} if none exists.
   */
  public AlertGroupEntity findGroupById(long groupId) {
    return entityManagerProvider.get().find(AlertGroupEntity.class, groupId);
  }

  /**
   * Gets an alert target with the specified ID.
   * 
   * @param targetId
   *          the ID of the target to retrieve.
   * @return the target or {@code null} if none exists.
   */
  public AlertTargetEntity findTargetById(long targetId) {
    return entityManagerProvider.get().find(AlertTargetEntity.class, targetId);
  }

  /**
   * Gets a notification with the specified ID.
   * 
   * @param noticeId
   *          the ID of the notification to retrieve.
   * @return the notification or {@code null} if none exists.
   */
  public AlertNoticeEntity findNoticeById(long noticeId) {
    return entityManagerProvider.get().find(AlertNoticeEntity.class, noticeId);
  }

  /**
   * Gets an alert group with the specified name across all clusters. Alert
   * group names are unique within a cluster.
   * 
   * @param groupName
   *          the name of the group (not {@code null}).
   * @return the alert group or {@code null} if none exists.
   */
  public AlertGroupEntity findGroupByName(String groupName) {
    TypedQuery<AlertGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertGroupEntity.findByName", AlertGroupEntity.class);

    query.setParameter("groupName", groupName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets an alert group with the specified name for the given cluster. Alert
   * group names are unique within a cluster.
   * 
   * @param clusterId
   *          the ID of the cluster.
   * @param groupName
   *          the name of the group (not {@code null}).
   * @return the alert group or {@code null} if none exists.
   */
  public AlertGroupEntity findGroupByName(long clusterId, String groupName) {
    TypedQuery<AlertGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertGroupEntity.findByNameInCluster", AlertGroupEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("groupName", groupName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets an alert target with the specified name. Alert target names are unique
   * across all clusters.
   * 
   * @param targetName
   *          the name of the target (not {@code null}).
   * @return the alert target or {@code null} if none exists.
   */
  public AlertTargetEntity findTargetByName(String targetName) {
    TypedQuery<AlertTargetEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertTargetEntity.findByName", AlertTargetEntity.class);

    query.setParameter("targetName", targetName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets all alert groups stored in the database across all clusters.
   * 
   * @return all alert groups or empty list if none exist (never {@code null}).
   */
  public List<AlertGroupEntity> findAllGroups() {
    TypedQuery<AlertGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertGroupEntity.findAll", AlertGroupEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert groups stored in the database for the specified cluster.
   * 
   * @return all alert groups in the specified cluster or empty list if none
   *         exist (never {@code null}).
   */
  public List<AlertGroupEntity> findAllGroups(long clusterId) {
    TypedQuery<AlertGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertGroupEntity.findAllInCluster", AlertGroupEntity.class);

    query.setParameter("clusterId", clusterId);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert targets stored in the database.
   * 
   * @return all alert targets or empty list if none exist (never {@code null}).
   */
  public List<AlertTargetEntity> findAllTargets() {
    TypedQuery<AlertTargetEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertTargetEntity.findAll", AlertTargetEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all alert notifications stored in the database.
   * 
   * @return all alert notifications or empty list if none exist (never
   *         {@code null}).
   */
  public List<AlertNoticeEntity> findAllNotices() {
    TypedQuery<AlertNoticeEntity> query = entityManagerProvider.get().createNamedQuery(
        "AlertNoticeEntity.findAll", AlertNoticeEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Persists a new alert group.
   * 
   * @param alertGroup
   *          the group to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertGroupEntity alertGroup) {
    entityManagerProvider.get().persist(alertGroup);
  }

  /**
   * Refresh the state of the alert group from the database.
   * 
   * @param alertGroup
   *          the group to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertGroupEntity alertGroup) {
    entityManagerProvider.get().refresh(alertGroup);
  }

  /**
   * Merge the speicified alert group with the existing group in the database.
   * 
   * @param alertGroup
   *          the group to merge (not {@code null}).
   * @return the updated group with merged content (never {@code null}).
   */
  @Transactional
  public AlertGroupEntity merge(AlertGroupEntity alertGroup) {
    return entityManagerProvider.get().merge(alertGroup);
  }

  /**
   * Removes the specified alert group from the database.
   * 
   * @param alertGroup
   *          the group to remove.
   */
  @Transactional
  public void remove(AlertGroupEntity alertGroup) {
    entityManagerProvider.get().remove(merge(alertGroup));
  }

  /**
   * Persists a new alert target.
   * 
   * @param alertTarget
   *          the target to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertTargetEntity alertTarget) {
    entityManagerProvider.get().persist(alertTarget);
  }

  /**
   * Refresh the state of the alert target from the database.
   * 
   * @param alertTarget
   *          the target to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertTargetEntity alertTarget) {
    entityManagerProvider.get().refresh(alertTarget);
  }

  /**
   * Merge the speicified alert target with the existing target in the database.
   * 
   * @param alertTarget
   *          the target to merge (not {@code null}).
   * @return the updated target with merged content (never {@code null}).
   */
  @Transactional
  public AlertTargetEntity merge(AlertTargetEntity alertTarget) {
    return entityManagerProvider.get().merge(alertTarget);
  }

  /**
   * Removes the specified alert target from the database.
   * 
   * @param alertTarget
   *          the target to remove.
   */
  @Transactional
  public void remove(AlertTargetEntity alertTarget) {
    entityManagerProvider.get().remove(merge(alertTarget));
  }

  /**
   * Persists a new notification.
   * 
   * @param alertNotice
   *          the notification to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertNoticeEntity alertNotice) {
    entityManagerProvider.get().persist(alertNotice);
  }

  /**
   * Refresh the state of the notification from the database.
   * 
   * @param alertNotice
   *          the notification to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertNoticeEntity alertNotice) {
    entityManagerProvider.get().refresh(alertNotice);
  }

  /**
   * Merge the specified notification with the existing target in the database.
   * 
   * @param alertNotice
   *          the notification to merge (not {@code null}).
   * @return the updated notification with merged content (never {@code null}).
   */
  @Transactional
  public AlertNoticeEntity merge(AlertNoticeEntity alertNotice) {
    return entityManagerProvider.get().merge(alertNotice);
  }

  /**
   * Removes the specified notification from the database.
   * 
   * @param alertNotice
   *          the notification to remove.
   */
  @Transactional
  public void remove(AlertNoticeEntity alertNotice) {
    entityManagerProvider.get().remove(merge(alertNotice));
  }

  /**
   * Removes notifications for the specified alert definition ID. This will
   * invoke {@link EntityManager#clear()} when completed since the JPQL
   * statement will remove entries without going through the EM.
   * 
   * @param definitionId
   *          the ID of the definition to remove.
   */
  @Transactional
  public void removeNoticeByDefinitionId(long definitionId) {
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<AlertNoticeEntity> currentQuery = entityManager.createNamedQuery(
        "AlertNoticeEntity.removeByDefinitionId", AlertNoticeEntity.class);

    currentQuery.setParameter("definitionId", definitionId);
    currentQuery.executeUpdate();
    entityManager.clear();
  }
}
