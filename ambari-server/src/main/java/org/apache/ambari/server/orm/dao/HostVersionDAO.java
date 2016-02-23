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

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link org.apache.ambari.server.orm.dao.HostVersionDAO} class manages the {@link org.apache.ambari.server.orm.entities.HostVersionEntity}
 * instances associated with a host. Each host can have multiple stack versions in {@link org.apache.ambari.server.state.RepositoryVersionState#INSTALLED}
 * which are installed, exactly one stack version that is either {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT} or
 * {@link org.apache.ambari.server.state.RepositoryVersionState#UPGRADING}.
 */
@Singleton
public class HostVersionDAO extends CrudDAO<HostVersionEntity, Long> {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Constructor.
   */
  public HostVersionDAO() {
    super(HostVersionEntity.class);
  }

  /**
   * Construct a Host Version. Additionally this will update parent connection relations without
   * forcing refresh of parent entity
   * @param entity entity to create
   */
  @Override
  @Transactional
  public void create(HostVersionEntity entity) throws IllegalArgumentException{
    // check if repository version is not missing, to avoid NPE
    if (entity.getRepositoryVersion() == null) {
      throw new IllegalArgumentException("RepositoryVersion argument is not set for the entity");
    }

    super.create(entity);
    entity.getRepositoryVersion().updateHostVersionEntityRelation(entity);
  }

  /**
   * Retrieve all of the host versions for the given cluster name, stack name,
   * and stack version.
   *
   * @param clusterName
   *          Cluster name
   * @param stackId
   *          Stack (e.g., HDP-2.2)
   * @param version
   *          Stack version (e.g., 2.2.0.1-995)
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByClusterStackAndVersion(
      String clusterName, StackId stackId, String version) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get().createNamedQuery("hostVersionByClusterAndStackAndVersion", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("stackName", stackId.getStackName());
    query.setParameter("stackVersion", stackId.getStackVersion());
    query.setParameter("version", version);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the host versions for the given host name across all clusters.
   *
   * @param hostName FQDN of host
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByHost(String hostName) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByHostname", HostVersionEntity.class);
    query.setParameter("hostName", hostName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the host versions for the given cluster name and host name.
   *
   * @param clusterName Cluster name
   * @param hostName FQDN of host
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByClusterAndHost(String  clusterName, String hostName) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByClusterAndHostname", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("hostName", hostName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the host versions for the given cluster name, host name, and state. <br/>
   * Consider using faster method: {@link HostVersionDAO#findByClusterHostAndState(long, long, org.apache.ambari.server.state.RepositoryVersionState)}
   * @param clusterName Cluster name
   * @param hostName FQDN of host
   * @param state repository version state
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByClusterHostAndState(String  clusterName, String hostName, RepositoryVersionState state) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByClusterHostnameAndState", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("hostName", hostName);
    query.setParameter("state", state);

    return daoUtils.selectList(query);
  }

  /**
   * Faster version of {@link HostVersionDAO#findByClusterHostAndState(java.lang.String, java.lang.String, org.apache.ambari.server.state.RepositoryVersionState)}
   *
   * @param clusterId Cluster ID
   * @param hostId Host ID
   * @param state repository version state
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByClusterHostAndState(long clusterId, long hostId, RepositoryVersionState state) {
    TypedQuery<HostVersionEntity> query =
        entityManagerProvider.get().createNamedQuery("hostVersionByClusterHostIdAndState", HostVersionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("hostId", hostId);
    query.setParameter("state", state);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve the single host version whose state is {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT}, of which there should be exactly one at all times
   * for the given host.
   * Consider using faster method {@link HostVersionDAO#findByHostAndStateCurrent(long, long)}
   *
   * @param clusterName Cluster name
   * @param hostName Host name
   * @return Returns the single host version for this host whose state is {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT}, or {@code null} otherwise.
   */
  @RequiresSession
  public HostVersionEntity findByHostAndStateCurrent(String clusterName, String hostName) {
    try {
      List<?> results = findByClusterHostAndState(clusterName, hostName, RepositoryVersionState.CURRENT);
      if (results.isEmpty()) {
        return null;
      } else {
        if (results.size() == 1) {
          return (HostVersionEntity) results.get(0);
        }
      }
      throw new NonUniqueResultException();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  /**
   * Retrieve the single host version whose state is {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT}, of which there should be exactly one at all times
   * for the given host.
   * Faster version of {@link HostVersionDAO#findByHostAndStateCurrent(java.lang.String, java.lang.String)}
   * @param clusterId Cluster ID
   * @param hostId host ID
   * @return Returns the single host version for this host whose state is {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT}, or {@code null} otherwise.
   */
  @RequiresSession
  public HostVersionEntity findByHostAndStateCurrent(long clusterId, long hostId) {
    try {
      List<?> results = findByClusterHostAndState(clusterId, hostId, RepositoryVersionState.CURRENT);
      if (results.isEmpty()) {
        return null;
      } else {
        if (results.size() == 1) {
          return (HostVersionEntity) results.get(0);
        }
      }
      throw new NonUniqueResultException();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  /**
   * Retrieve the single host version for the given cluster, stack name, stack
   * version, and host name. <br/>
   * This query is slow and not suitable for frequent use. <br/>
   * Please, use {@link HostVersionDAO#findByClusterStackVersionAndHost(long, org.apache.ambari.server.state.StackId, java.lang.String, long)} <br/>
   * It is ~50 times faster
   *
   * @param clusterName
   *          Cluster name
   * @param stackId
   *          Stack ID (e.g., HDP-2.2)
   * @param version
   *          Stack version (e.g., 2.2.0.1-995)
   * @param hostName
   *          FQDN of host
   * @return Returns the single host version that matches the criteria.
   */
  @RequiresSession
  public HostVersionEntity findByClusterStackVersionAndHost(String clusterName,
      StackId stackId, String version, String hostName) {

    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByClusterStackVersionAndHostname", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("stackName", stackId.getStackName());
    query.setParameter("stackVersion", stackId.getStackVersion());
    query.setParameter("version", version);
    query.setParameter("hostName", hostName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Optimized version of {@link HostVersionDAO#findByClusterStackVersionAndHost(java.lang.String, org.apache.ambari.server.state.StackId, java.lang.String, java.lang.String)}
   * @param clusterId Id of cluster
   * @param stackId Stack ID (e.g., HDP-2.2)
   * @param version Stack version (e.g., 2.2.0.1-995)
   * @param hostId Host Id
   * @return Returns the single host version that matches the criteria.
   */
  @RequiresSession
  public HostVersionEntity findByClusterStackVersionAndHost(long clusterId, StackId stackId, String version,
                                                            long hostId) {
    TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByClusterStackVersionAndHostId", HostVersionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("stackName", stackId.getStackName());
    query.setParameter("stackVersion", stackId.getStackVersion());
    query.setParameter("version", version);
    query.setParameter("hostId", hostId);

    return daoUtils.selectSingle(query);
  }

  @Transactional
  public void removeByHostName(String hostName) {
    Collection<HostVersionEntity> hostVersions = this.findByHost(hostName);
    for (HostVersionEntity hostVersion : hostVersions) {
      this.remove(hostVersion);
    }
  }

  /**
   * Updates the host versions existing CURRENT record to the INSTALLED, and the target
   * becomes CURRENT.
   * @param target    the repo version that all hosts to mark as CURRENT
   * @param current   the repo version that all hosts marked as INSTALLED
   */
  @Transactional
  public void updateVersions(RepositoryVersionEntity target, RepositoryVersionEntity current) {
    // !!! first update target to be current
    StringBuilder sb = new StringBuilder("UPDATE HostVersionEntity hve");
    sb.append(" SET hve.state = ?1 ");
    sb.append(" WHERE hve.repositoryVersion = ?2");

    TypedQuery<Long> query = entityManagerProvider.get().createQuery(sb.toString(), Long.class);
    daoUtils.executeUpdate(query, RepositoryVersionState.CURRENT, target);

    // !!! then move existing current to installed
    sb = new StringBuilder("UPDATE HostVersionEntity hve");
    sb.append(" SET hve.state = ?1 ");
    sb.append(" WHERE hve.repositoryVersion = ?2");
    sb.append(" AND hve.state = ?3");

    query = entityManagerProvider.get().createQuery(sb.toString(), Long.class);
    daoUtils.executeUpdate(query, RepositoryVersionState.INSTALLED, current,
        RepositoryVersionState.CURRENT);


    entityManagerProvider.get().clear();
  }

}
