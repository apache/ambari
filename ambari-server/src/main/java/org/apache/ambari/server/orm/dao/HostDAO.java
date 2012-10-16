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
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

public class HostDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;

  public HostEntity findByName(String hostName) {
    return entityManagerProvider.get().find(HostEntity.class, hostName);
  }

  public List<HostEntity> findAll() {
    TypedQuery<HostEntity> query = entityManagerProvider.get().createQuery("SELECT host FROM HostEntity host", HostEntity.class);
    return query.getResultList();
  }

  /**
   * Refreshes entity state from database
   * @param hostEntity entity to refresh
   */
  public void refresh(HostEntity hostEntity) {
    entityManagerProvider.get().refresh(hostEntity);
  }

  @Transactional
  public void create(HostEntity hostEntity) {
    entityManagerProvider.get().persist(hostEntity);
  }

  @Transactional
  public HostEntity merge(HostEntity hostEntity) {
    return entityManagerProvider.get().merge(hostEntity);
  }

  @Transactional
  public void remove(HostEntity hostEntity) {
    entityManagerProvider.get().remove(hostEntity);
  }

  @Transactional
  public void removeByName(String hostName) {
    remove(findByName(hostName));
  }

}
