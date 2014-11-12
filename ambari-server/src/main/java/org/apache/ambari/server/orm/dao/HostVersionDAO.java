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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.state.UpgradeState;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * The {@link org.apache.ambari.server.orm.dao.HostVersionDAO} class manages the {@link org.apache.ambari.server.orm.entities.HostVersionEntity}
 * instances associated with a host. Each host can have multiple stack versions in {@link org.apache.ambari.server.state.UpgradeState#NONE}
 * which are installed, exactly one stack version that is either {@link org.apache.ambari.server.state.UpgradeState#PENDING} or
 * {@link org.apache.ambari.server.state.UpgradeState#IN_PROGRESS}.
 */
@Singleton
public class HostVersionDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Get the object with the given id.
   *
   * @param id Primary key id
   * @return Return the object with the given primary key
   */
  @RequiresSession
  public HostVersionEntity findByPK(long id) {
    return entityManagerProvider.get().find(HostVersionEntity.class, id);
  }

  /**
   * Retrieve all of the host versions for the given cluster name, stack name, and stack version.
   *
   * @param clusterName Cluster name
   * @param stack Stack name (e.g., HDP)
   * @param version Stack version (e.g., 2.2.0.1-995)
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByClusterStackAndVersion(String clusterName, String stack, String version) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get().createNamedQuery("hostVersionByClusterAndStackAndVersion", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("stack", stack);
    query.setParameter("version", version);

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
   * @param state Upgrade state
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByClusterHostAndState(String  clusterName, String hostName, UpgradeState state) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByClusterHostnameAndState", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("hostName", hostName);
    query.setParameter("state", state);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve the single host version for the given cluster, stack name, stack version, and host name.
   *
   * @param clusterName Cluster name
   * @param stack Stack name (e.g., HDP)
   * @param version Stack version (e.g., 2.2.0.1-995)
   * @param hostName FQDN of host
   * @return Returns the single host version that matches the criteria.
   */
  @RequiresSession
  public HostVersionEntity findByClusterStackVersionAndHost(String clusterName, String stack, String version, String hostName) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByClusterStackVersionAndHostname", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("stack", stack);
    query.setParameter("version", version);
    query.setParameter("hostName", hostName);

    return daoUtils.selectSingle(query);
  }

  @RequiresSession
  public List<HostVersionEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), HostVersionEntity.class);
  }

  @Transactional
  public void refresh(HostVersionEntity hostVersionEntity) {
    entityManagerProvider.get().refresh(hostVersionEntity);
  }

  @Transactional
  public void create(HostVersionEntity hostVersionEntity) {
    entityManagerProvider.get().persist(hostVersionEntity);
  }

  @Transactional
  public HostVersionEntity merge(HostVersionEntity hostVersionEntity) {
    return entityManagerProvider.get().merge(hostVersionEntity);
  }

  @Transactional
  public void remove(HostVersionEntity hostVersionEntity) {
    entityManagerProvider.get().remove(merge(hostVersionEntity));
  }

  @Transactional
  public void removeByPK(long id) {
    remove(findByPK(id));
  }
}
