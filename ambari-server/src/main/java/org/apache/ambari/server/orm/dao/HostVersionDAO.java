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
   * Retrieve all of the host versions for the given cluster name, host name, and state.
   *
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
   * Retrieve the single host version whose state is {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT}, of which there should be exactly one at all times
   * for the given host.
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
   * Retrieve the single host version for the given cluster, stack name, stack
   * version, and host name.
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

  @Transactional
  public void removeByHostName(String hostName) {
    Collection<HostVersionEntity> hostVersions = this.findByHost(hostName);
    for (HostVersionEntity hostVersion : hostVersions) {
      this.remove(hostVersion);
    }
  }
}
