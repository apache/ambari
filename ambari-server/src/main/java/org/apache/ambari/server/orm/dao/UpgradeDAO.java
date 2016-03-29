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
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Manages the UpgradeEntity and UpgradeItemEntity classes
 */
@Singleton
public class UpgradeDAO {

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @Inject
  private DaoUtils daoUtils;

  /**
   * Get all items.
   * @return List of all of the UpgradeEntity items.
   */
  @RequiresSession
  public List<UpgradeEntity> findAll() {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findAll", UpgradeEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * @param clusterId the cluster id
   * @return the list of upgrades initiated for the cluster
   */
  @RequiresSession
  public List<UpgradeEntity> findUpgrades(long clusterId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findAllForCluster", UpgradeEntity.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));

    return daoUtils.selectList(query);
  }

  /**
   * Finds a specific upgrade
   * @param upgradeId the id
   * @return the entity, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeEntity findUpgrade(long upgradeId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findUpgrade", UpgradeEntity.class);

    query.setParameter("upgradeId", Long.valueOf(upgradeId));

    return daoUtils.selectSingle(query);
  }

  @RequiresSession
  public UpgradeEntity findUpgradeByRequestId(Long requestId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createQuery(
        "SELECT p FROM UpgradeEntity p WHERE p.requestId = :requestId", UpgradeEntity.class);
    query.setParameter("requestId", requestId);
    query.setHint(QueryHints.REFRESH, HintValues.TRUE);

    return daoUtils.selectSingle(query);
  }

  /**
   * Creates the upgrade entity in the database
   * @param entity the entity
   */
  @Transactional
  public void create(UpgradeEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Removes all upgrades associated with the cluster.
   * @param clusterId the cluster id
   */
  @Transactional
  public void removeAll(long clusterId) {
    List<UpgradeEntity> entities = findUpgrades(clusterId);

    for (UpgradeEntity entity : entities) {
      entityManagerProvider.get().remove(entity);
    }
  }

  /**
   * @param groupId the group id
   * @return the group, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeGroupEntity findUpgradeGroup(Long groupId) {

    TypedQuery<UpgradeGroupEntity> query = entityManagerProvider.get().createQuery(
        "SELECT p FROM UpgradeGroupEntity p WHERE p.upgradeGroupId = :groupId", UpgradeGroupEntity.class);
    query.setParameter("groupId", groupId);
    query.setHint(QueryHints.REFRESH, HintValues.TRUE);

    return daoUtils.selectSingle(query);
  }

  /**
   * @param itemId the item id
   * @return the upgrade item entity, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeItemEntity findUpgradeItem(long itemId) {
    TypedQuery<UpgradeItemEntity> query = entityManagerProvider.get().createQuery(
        "SELECT p FROM UpgradeItemEntity p WHERE p.upgradeItemId = :itemId", UpgradeItemEntity.class);
    query.setParameter("itemId", Long.valueOf(itemId));
    query.setHint(QueryHints.REFRESH, HintValues.TRUE);

    return daoUtils.selectSingle(query);
  }


  /**
   * @param requestId the request id
   * @param stageId the stage id
   * @return the upgrade entity, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeItemEntity findUpgradeItemByRequestAndStage(Long requestId, Long stageId) {
    TypedQuery<UpgradeItemEntity> query = entityManagerProvider.get().createQuery(
        "SELECT p FROM UpgradeItemEntity p WHERE p.stageId = :stageId AND p.upgradeGroupEntity.upgradeEntity.requestId = :requestId",
        UpgradeItemEntity.class);
    query.setParameter("requestId", requestId);
    query.setParameter("stageId", stageId);

    query.setHint(QueryHints.REFRESH, HintValues.TRUE);

    return daoUtils.selectSingle(query);
  }

  /**
   * @param clusterId the cluster id
   * @return the upgrade entity, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeEntity findLastUpgradeForCluster(long clusterId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findLatestForClusterInDirection", UpgradeEntity.class);
    query.setMaxResults(1);
    query.setParameter("clusterId", clusterId);
    query.setParameter("direction", Direction.UPGRADE);

    query.setHint(QueryHints.REFRESH, HintValues.TRUE);

    return daoUtils.selectSingle(query);
  }

  /**
   * @param clusterId the cluster id
   * @return the upgrade entity, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeEntity findLastUpgradeOrDowngradeForCluster(long clusterId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findLatestForCluster", UpgradeEntity.class);
    query.setMaxResults(1);
    query.setParameter("clusterId", clusterId);

    query.setHint(QueryHints.REFRESH, HintValues.TRUE);

    return daoUtils.selectSingle(query);
  }

  @Transactional
  public UpgradeEntity merge(UpgradeEntity upgradeEntity) {
    return entityManagerProvider.get().merge(upgradeEntity);
  }
}
