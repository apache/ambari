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
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.state.RepositoryVersionState;

import com.google.inject.Singleton;

/**
 * The {@link ClusterVersionDAO} class manages the {@link ClusterVersionEntity} instances associated with a cluster.
 * Each cluster can have multiple stack versions {@link RepositoryVersionState#INSTALLED},
 * exactly one stack version that is {@link RepositoryVersionState#CURRENT}, and at most one
 * stack version that is {@link RepositoryVersionState#UPGRADING}.
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
   * @param stack Stack name (e.g., HDP)
   * @param version Stack version (e.g., 2.2.0.1-995)
   * @return Return a list of cluster versions that match the stack and version.
   */
  @RequiresSession
  public List<ClusterVersionEntity> findByStackAndVersion(String stack, String version) {
    final TypedQuery<ClusterVersionEntity> query = entityManagerProvider.get().createNamedQuery("clusterVersionByStackVersion", ClusterVersionEntity.class);
    query.setParameter("stack", stack);
    query.setParameter("version", version);

    return daoUtils.selectList(query);
  }

  /**
   * Get the cluster version for the given cluster name, stack name, and stack version.
   *
   * @param clusterName Cluster name
   * @param stack Stack name (e.g., HDP)
   * @param version Stack version (e.g., 2.2.0.1-995)
   * @return Return all of the cluster versions associated with the given cluster.
   */
  @RequiresSession
  public ClusterVersionEntity findByClusterAndStackAndVersion(String  clusterName, String stack, String version) {
    final TypedQuery<ClusterVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("clusterVersionByClusterAndStackAndVersion", ClusterVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("stack", stack);
    query.setParameter("version", version);

    return daoUtils.selectSingle(query);
  }

  /**
   * Get the cluster version for the given cluster name.
   *
   * @param clusterName Cluster name
   * @return Return all of the cluster versions associated with the given cluster.
   */
  public List<ClusterVersionEntity> findByCluster(String  clusterName) {
    final TypedQuery<ClusterVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("clusterVersionByCluster", ClusterVersionEntity.class);
    query.setParameter("clusterName", clusterName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve the single cluster version whose state is {@link RepositoryVersionState#CURRENT}, of which there should be exactly one at all times
   * for the given cluster.
   *
   * @param clusterName Cluster name
   * @return Returns the single cluster version for this cluster whose state is {@link RepositoryVersionState#CURRENT}, or {@code null} otherwise.
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
}
