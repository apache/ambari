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

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;

import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link ClusterVersionDAO} class manages the {@link ClusterVersionEntity} instances associated with a cluster.
 * Each cluster can have multiple stack versions {@link org.apache.ambari.server.state.RepositoryVersionState#INSTALLED},
 * exactly one stack version that is {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT}, and at most one
 * stack version that is {@link org.apache.ambari.server.state.RepositoryVersionState#UPGRADING}.
 */
@Singleton
public class ClusterVersionDAO extends CrudDAO<ClusterVersionEntity, Long>{
  /**
   * Constructor.
   */
  public ClusterVersionDAO() {
    super(ClusterVersionEntity.class);
  }

  /**
   * Retrieve all of the cluster versions for the given stack and version.
   *
   * @param stackName
   *          the stack name (for example "HDP")
   * @param stackVersion
   *          the stack version (for example "2.2")
   * @param version
   *          Repository version (e.g., 2.2.0.1-995)
   * @return Return a list of cluster versions that match the stack and version.
   */
  @RequiresSession
  public List<ClusterVersionEntity> findByStackAndVersion(String stackName,
      String stackVersion, String version) {
    final TypedQuery<ClusterVersionEntity> query = entityManagerProvider.get().createNamedQuery("clusterVersionByStackVersion", ClusterVersionEntity.class);
    query.setParameter("stackName", stackName);
    query.setParameter("stackVersion", stackVersion);
    query.setParameter("version", version);

    return daoUtils.selectList(query);
  }

  /**
   * Get the cluster version for the given cluster name, stack name, and stack
   * version.
   *
   * @param clusterName
   *          Cluster name
   * @param stackId
   *          Stack id (e.g., HDP-2.2)
   * @param version
   *          Repository version (e.g., 2.2.0.1-995)
   * @return Return all of the cluster versions associated with the given
   *         cluster.
   */
  @RequiresSession
  public ClusterVersionEntity findByClusterAndStackAndVersion(
      String clusterName, StackId stackId, String version) {
    final TypedQuery<ClusterVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("clusterVersionByClusterAndStackAndVersion", ClusterVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("stackName", stackId.getStackName());
    query.setParameter("stackVersion", stackId.getStackVersion());
    query.setParameter("version", version);

    return daoUtils.selectSingle(query);
  }

  /**
   * Retrieve all of the cluster versions for the given cluster.
   *
   * @param clusterName Cluster name
   * @return Return all of the cluster versions associated with the given cluster.
   */
  @RequiresSession
  public List<ClusterVersionEntity> findByCluster(String clusterName) {
    final TypedQuery<ClusterVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("clusterVersionByCluster", ClusterVersionEntity.class);
    query.setParameter("clusterName", clusterName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve the single cluster version whose state is {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT}, of which there should be exactly one at all times
   * for the given cluster.
   *
   * @param clusterName Cluster name
   * @return Returns the single cluster version for this cluster whose state is {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT}, or {@code null} otherwise.
   */
  @RequiresSession
  public ClusterVersionEntity findByClusterAndStateCurrent(String clusterName) {
    final TypedQuery<ClusterVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("clusterVersionByClusterAndState", ClusterVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("state", RepositoryVersionState.CURRENT);

    try {
      List results = query.getResultList();
      if (results.isEmpty()) {
        return null;
      } else {
        if (results.size() == 1) {
          return (ClusterVersionEntity) results.get(0);
        }
      }
      throw new NonUniqueResultException();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  /**
   * Retrieve all of the cluster versions for the cluster with the given name and a state.
   *
   * @param clusterName Cluster name
   * @param state Cluster version state
   * @return Returns a list of cluster versions for the given cluster and a state.
   */
  @RequiresSession
  public List<ClusterVersionEntity> findByClusterAndState(String clusterName, RepositoryVersionState state) {
    final TypedQuery<ClusterVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("clusterVersionByClusterAndState", ClusterVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("state", state);

    return daoUtils.selectList(query);
  }

  /**
   * Construct a Cluster Version. Additionally this will update parent connection relations without
   * forcing refresh of parent entity
   * @param entity entity to create
   */
  @Override
  @Transactional
  public void create(ClusterVersionEntity entity) throws IllegalArgumentException {
    // check if repository version is not missing, to avoid NPE
    if (entity.getRepositoryVersion() == null) {
      throw new IllegalArgumentException("RepositoryVersion argument is not set for the entity");
    }

    super.create(entity);
    entity.getRepositoryVersion().updateClusterVersionEntityRelation(entity);
  }

  /**
   * Construct a Cluster Version and return it. This is primarily used to be able to construct the object and mock
   * the function call.
   * @param cluster Cluster
   * @param repositoryVersion Repository Version
   * @param state Initial State
   * @param startTime Start Time
   * @param endTime End Time
   * @param userName Username, such as "admin"
   * @return Return new ClusterVersion object.
   */
  @Transactional
  public ClusterVersionEntity create(ClusterEntity cluster, RepositoryVersionEntity repositoryVersion,
                                     RepositoryVersionState state, long startTime, long endTime, String userName) {
    ClusterVersionEntity clusterVersionEntity = new ClusterVersionEntity(cluster,
        repositoryVersion, state, startTime, endTime, userName);
    this.create(clusterVersionEntity);
    return clusterVersionEntity;
  }

  /**
   * Updates the cluster version's existing CURRENT record to the INSTALLED, and the target
   * becomes CURRENT.
   * @param clusterId the cluster
   * @param target    the repo version that will be marked as CURRENT
   * @param current   the cluster's current record to be marked INSTALLED
   */
  @Transactional
  public void updateVersions(Long clusterId, RepositoryVersionEntity target, RepositoryVersionEntity current) {
    // !!! first update target to be current
    StringBuilder sb = new StringBuilder("UPDATE ClusterVersionEntity cve");
    sb.append(" SET cve.state = ?1 ");
    sb.append(" WHERE cve.clusterId = ?2");
    sb.append(" AND cve.repositoryVersion = ?3");

    TypedQuery<Long> query = entityManagerProvider.get().createQuery(sb.toString(), Long.class);
    daoUtils.executeUpdate(query, RepositoryVersionState.CURRENT, clusterId, target);

    // !!! then move existing current to installed
    sb = new StringBuilder("UPDATE ClusterVersionEntity cve");
    sb.append(" SET cve.state = ?1 ");
    sb.append(" WHERE cve.clusterId = ?2");
    sb.append(" AND cve.repositoryVersion = ?3");
    sb.append(" AND cve.state = ?4");

    query = entityManagerProvider.get().createQuery(sb.toString(), Long.class);
    daoUtils.executeUpdate(query, RepositoryVersionState.INSTALLED, clusterId, current,
        RepositoryVersionState.CURRENT);

    entityManagerProvider.get().clear();
  }


}
