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
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntityPK;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

@Singleton
public class ConfigGroupHostMappingDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @Transactional
  public ConfigGroupHostMappingEntity findByPK(ConfigGroupHostMappingEntityPK
        configGroupHostMappingEntityPK) {
    return entityManagerProvider.get().find(ConfigGroupHostMappingEntity
      .class, configGroupHostMappingEntityPK);
  }

  @Transactional
  public List<ConfigGroupHostMappingEntity> findByHost(String hostname) {
    TypedQuery<ConfigGroupHostMappingEntity> query = entityManagerProvider
      .get().createNamedQuery("groupsByHost", ConfigGroupHostMappingEntity
        .class);

    query.setParameter("hostname", hostname);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  @Transactional
  public List<ConfigGroupHostMappingEntity> findByGroup(Long groupId) {
    TypedQuery<ConfigGroupHostMappingEntity> query = entityManagerProvider
      .get().createNamedQuery("hostsByGroup", ConfigGroupHostMappingEntity
        .class);

    query.setParameter("groupId", groupId);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  @Transactional
  public void create(ConfigGroupHostMappingEntity
                         configGroupHostMappingEntity) {
    entityManagerProvider.get().persist(configGroupHostMappingEntity);
  }

  @Transactional
  public ConfigGroupHostMappingEntity merge(ConfigGroupHostMappingEntity
                         configGroupHostMappingEntity) {
    return entityManagerProvider.get().merge(configGroupHostMappingEntity);
  }

  @Transactional
  public void refresh(ConfigGroupHostMappingEntity
                         configGroupHostMappingEntity) {
    entityManagerProvider.get().refresh(configGroupHostMappingEntity);
  }

  @Transactional
  public void remove(ConfigGroupHostMappingEntity
                         configGroupHostMappingEntity) {
    entityManagerProvider.get().remove(merge(configGroupHostMappingEntity));
  }

  @Transactional
  public void removeByPK(ConfigGroupHostMappingEntityPK
                         configGroupHostMappingEntityPK) {
    entityManagerProvider.get().remove(findByPK
      (configGroupHostMappingEntityPK));
  }

  @Transactional
  public void removeAllByGroup(Long groupId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery
      ("DELETE FROM ConfigGroupHostMappingEntity confighosts WHERE " +
        "confighosts.configGroupId = ?1", Long.class);

    daoUtils.executeUpdate(query, groupId);
    // Flush to current transaction required in order to avoid Eclipse link
    // from re-ordering delete
    entityManagerProvider.get().flush();
  }

  @Transactional
  public void removeAllByHost(String hostname) {
    TypedQuery<String> query = entityManagerProvider.get().createQuery
      ("DELETE FROM ConfigGroupHostMappingEntity confighosts WHERE " +
        "confighosts.hostname = ?1", String.class);

    daoUtils.executeUpdate(query, hostname);
  }
}
