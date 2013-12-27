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
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntityPK;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

@Singleton
public class ConfigGroupConfigMappingDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @Transactional
  public ConfigGroupConfigMappingEntity findByPK
    (ConfigGroupConfigMappingEntityPK configGroupConfigMappingEntityPK) {
    return entityManagerProvider.get().find(ConfigGroupConfigMappingEntity.class,
      configGroupConfigMappingEntityPK);
  }

  @Transactional
  public List<ConfigGroupConfigMappingEntity> findByGroup(Long groupId) {
    TypedQuery<ConfigGroupConfigMappingEntity> query = entityManagerProvider
      .get().createNamedQuery("configsByGroup", ConfigGroupConfigMappingEntity.class);

    query.setParameter("groupId", groupId);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  @Transactional
  public void create(ConfigGroupConfigMappingEntity
                         configGroupConfigMappingEntity) {
    entityManagerProvider.get().persist(configGroupConfigMappingEntity);
  }

  @Transactional
  public ConfigGroupConfigMappingEntity merge(ConfigGroupConfigMappingEntity
                        configGroupConfigMappingEntity) {
    return entityManagerProvider.get().merge(configGroupConfigMappingEntity);
  }

  @Transactional
  public void refresh(ConfigGroupConfigMappingEntity
                          configGroupConfigMappingEntity) {
    entityManagerProvider.get().refresh(configGroupConfigMappingEntity);
  }

  @Transactional
  public void remove(ConfigGroupConfigMappingEntity
                         configGroupConfigMappingEntity) {
    entityManagerProvider.get().remove(merge(configGroupConfigMappingEntity));
  }

  @Transactional
  public void removeByPK(ConfigGroupConfigMappingEntityPK
                           configGroupConfigMappingEntityPK) {
    entityManagerProvider.get().remove(findByPK
      (configGroupConfigMappingEntityPK));
  }

  @Transactional
  public void removeAllByGroup(Long groupId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery
      ("DELETE FROM ConfigGroupConfigMappingEntity configs WHERE configs" +
        ".configGroupId = ?1", Long.class);

    daoUtils.executeUpdate(query, groupId);
    entityManagerProvider.get().flush();
  }
}
