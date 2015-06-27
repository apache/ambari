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
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntityPK;
import org.apache.ambari.server.orm.entities.HostEntity;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

@Singleton
public class HostComponentStateDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @Inject
  HostDAO hostDAO;

  @RequiresSession
  public HostComponentStateEntity findByPK(HostComponentStateEntityPK primaryKey) {
    return entityManagerProvider.get().find(HostComponentStateEntity.class, primaryKey);
  }

  @RequiresSession
  public List<HostComponentStateEntity> findAll() {
    final TypedQuery<HostComponentStateEntity> query = entityManagerProvider.get().createNamedQuery("HostComponentStateEntity.findAll", HostComponentStateEntity.class);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  /**
   * Retrieve all of the Host Component States for the given host.
   *
   * @param hostName HOst name
   * @return Return all of the Host Component States that match the criteria.
   */
  @RequiresSession
  public List<HostComponentStateEntity> findByHost(String hostName) {
    final TypedQuery<HostComponentStateEntity> query = entityManagerProvider.get().createNamedQuery("HostComponentStateEntity.findByHost", HostComponentStateEntity.class);
    query.setParameter("hostName", hostName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the Host Component States for the given service.
   *
   * @param serviceName Service Name
   * @return Return all of the Host Component States that match the criteria.
   */
  @RequiresSession
  public List<HostComponentStateEntity> findByService(String serviceName) {
    final TypedQuery<HostComponentStateEntity> query = entityManagerProvider.get().createNamedQuery("HostComponentStateEntity.findByService", HostComponentStateEntity.class);
    query.setParameter("serviceName", serviceName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the Host Component States for the given service and component.
   *
   * @param serviceName Service Name
   * @param componentName Component Name
   * @return Return all of the Host Component States that match the criteria.
   */
  @RequiresSession
  public List<HostComponentStateEntity> findByServiceAndComponent(String serviceName, String componentName) {
    final TypedQuery<HostComponentStateEntity> query = entityManagerProvider.get().createNamedQuery("HostComponentStateEntity.findByServiceAndComponent", HostComponentStateEntity.class);
    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve the single Host Component State for the given unique service, component, and host.
   *
   * @param serviceName Service Name
   * @param componentName Component Name
   * @param hostName Host Name
   * @return Return all of the Host Component States that match the criteria.
   */
  @RequiresSession
  public HostComponentStateEntity findByServiceComponentAndHost(String serviceName, String componentName, String hostName) {
    final TypedQuery<HostComponentStateEntity> query = entityManagerProvider.get().createNamedQuery("HostComponentStateEntity.findByServiceComponentAndHost", HostComponentStateEntity.class);
    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);
    query.setParameter("hostName", hostName);

    return daoUtils.selectSingle(query);
  }

  @Transactional
  public void refresh(HostComponentStateEntity hostComponentStateEntity) {
    entityManagerProvider.get().refresh(hostComponentStateEntity);
  }

  @Transactional
  public void create(HostComponentStateEntity hostComponentStateEntity) {
    entityManagerProvider.get().persist(hostComponentStateEntity);
  }

  @Transactional
  public HostComponentStateEntity merge(HostComponentStateEntity hostComponentStateEntity) {
    return entityManagerProvider.get().merge(hostComponentStateEntity);
  }

  @Transactional
  public void remove(HostComponentStateEntity hostComponentStateEntity) {
    HostEntity hostEntity = hostDAO.findByName(hostComponentStateEntity.getHostName());

    entityManagerProvider.get().remove(merge(hostComponentStateEntity));

    // Make sure that the state entity is removed from its host entity
    hostEntity.removeHostComponentStateEntity(hostComponentStateEntity);
    hostDAO.merge(hostEntity);
  }

  @Transactional
  public void removeByPK(HostComponentStateEntityPK primaryKey) {
    remove(findByPK(primaryKey));
  }
}
