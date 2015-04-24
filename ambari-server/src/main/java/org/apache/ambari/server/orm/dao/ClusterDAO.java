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

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.StackId;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ClusterDAO {

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @Inject
  private DaoUtils daoUtils;

  @Inject
  private StackDAO stackDAO;

  /**
   * Looks for Cluster by ID
   * @param id ID of Cluster
   * @return Found entity or NULL
   */
  @RequiresSession
  public ClusterEntity findById(long id) {
    return entityManagerProvider.get().find(ClusterEntity.class, id);
  }

  @RequiresSession
  public ClusterEntity findByName(String clusterName) {
    TypedQuery<ClusterEntity> query = entityManagerProvider.get().createNamedQuery("clusterByName", ClusterEntity.class);
    query.setParameter("clusterName", clusterName);
    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }


  @RequiresSession
  public ClusterEntity findByResourceId(long resourceId) {
    TypedQuery<ClusterEntity> query = entityManagerProvider.get().createNamedQuery("clusterByResourceId", ClusterEntity.class);
    query.setParameter("resourceId", resourceId);
    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  @RequiresSession
  public List<ClusterEntity> findAll() {
    TypedQuery<ClusterEntity> query = entityManagerProvider.get().createNamedQuery("allClusters", ClusterEntity.class);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  @RequiresSession
  public ClusterConfigEntity findConfig(Long configEntityPK) {
    return entityManagerProvider.get().find(ClusterConfigEntity.class,
      configEntityPK);
  }

  @RequiresSession
  public ClusterConfigEntity findConfig(Long clusterId, String type, String tag) {
    CriteriaBuilder cb = entityManagerProvider.get().getCriteriaBuilder();
    CriteriaQuery<ClusterConfigEntity> cq = cb.createQuery(ClusterConfigEntity.class);
    Root<ClusterConfigEntity> config = cq.from(ClusterConfigEntity.class);
    cq.where(cb.and(
        cb.equal(config.get("clusterId"), clusterId)),
        cb.equal(config.get("type"), type),
        cb.equal(config.get("tag"), tag)
    );
    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createQuery(cq);
    return daoUtils.selectOne(query);
  }

  @RequiresSession
  public ClusterConfigEntity findConfig(Long clusterId, String type, Long version) {
    CriteriaBuilder cb = entityManagerProvider.get().getCriteriaBuilder();
    CriteriaQuery<ClusterConfigEntity> cq = cb.createQuery(ClusterConfigEntity.class);
    Root<ClusterConfigEntity> config = cq.from(ClusterConfigEntity.class);
    cq.where(cb.and(
        cb.equal(config.get("clusterId"), clusterId)),
      cb.equal(config.get("type"), type),
      cb.equal(config.get("version"), version)
    );
    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createQuery(cq);
    return daoUtils.selectOne(query);
  }

  /**
   * Gets the next version that will be created for a given
   * {@link ClusterConfigEntity}.
   *
   * @param clusterId
   *          the cluster that the service is a part of.
   * @param configType
   *          the name of the configuration type (not {@code null}).
   * @return the highest existing value of the version column + 1
   */
  public Long findNextConfigVersion(long clusterId, String configType) {
    TypedQuery<Number> query = entityManagerProvider.get().createNamedQuery(
        "ClusterConfigEntity.findNextConfigVersion", Number.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("configType", configType);

    return daoUtils.selectSingle(query).longValue();
  }

  /**
   * Get all configurations for the specified cluster and stack. This will
   * return different versions of the same configuration type (cluster-env v1
   * and cluster-env v2) if they exist.
   *
   * @param clusterId
   *          the cluster (not {@code null}).
   * @param stackId
   *          the stack (not {@code null}).
   * @return all service configurations for the cluster and stack.
   */
  @RequiresSession
  public List<ClusterConfigEntity> getAllConfigurations(Long clusterId,
      StackId stackId) {

    StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
        stackId.getStackVersion());

    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createNamedQuery(
        "ClusterConfigEntity.findAllConfigsByStack", ClusterConfigEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("stack", stackEntity);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the latest configurations for a given stack for all of the
   * configurations of the specified cluster.
   *
   * @param clusterId
   *          the cluster that the service is a part of.
   * @param stackId
   *          the stack to get the latest configurations for (not {@code null}).
   * @return the latest configurations for the specified cluster and stack.
   */
  public List<ClusterConfigEntity> getLatestConfigurations(long clusterId,
      StackId stackId) {
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
        stackId.getStackVersion());

    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createNamedQuery(
        "ClusterConfigEntity.findLatestConfigsByStack",
        ClusterConfigEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("stack", stackEntity);

    return daoUtils.selectList(query);
  }

  /**
   * Create Cluster entity in Database
   * @param clusterEntity entity to create
   */
  @Transactional
  public void create(ClusterEntity clusterEntity) {
    entityManagerProvider.get().persist(clusterEntity);
  }

  /**
   * Creates a cluster configuration in the DB.
   */
  @Transactional
  public void createConfig(ClusterConfigEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Remove a cluster configuration in the DB.
   */
  @Transactional
  public void removeConfig(ClusterConfigEntity entity) {
    entityManagerProvider.get().remove(entity);
  }

  /**
   * Remove a cluster configuration mapping from the DB.
   */
  @Transactional
  public void removeConfigMapping(ClusterConfigMappingEntity entity) {
    entityManagerProvider.get().remove(entity);
  }

  /**
   * Retrieve entity data from DB
   * 
   * @param clusterEntity
   *          entity to refresh
   */
  @Transactional
  public void refresh(ClusterEntity clusterEntity) {
    entityManagerProvider.get().refresh(clusterEntity);
  }

  @Transactional
  public ClusterEntity merge(ClusterEntity clusterEntity) {
    return entityManagerProvider.get().merge(clusterEntity);
  }

  @Transactional
  public void remove(ClusterEntity clusterEntity) {
    entityManagerProvider.get().remove(merge(clusterEntity));
  }

  @Transactional
  public void removeByName(String clusterName) {
    remove(findByName(clusterName));
  }

  @Transactional
  public void removeByPK(long id) {
    remove(findById(id));
  }

}
