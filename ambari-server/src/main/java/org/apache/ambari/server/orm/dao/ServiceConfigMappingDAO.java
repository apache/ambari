/*
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
import org.apache.ambari.server.orm.entities.ServiceConfigMappingEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ServiceConfigMappingDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @Transactional
  public List<ServiceConfigMappingEntity> findByType(
      Collection<String> configTypes) {
    TypedQuery<ServiceConfigMappingEntity> query =
        entityManagerProvider.get().createQuery(
            "SELECT config FROM ServiceConfigMappingEntity config"
                + " WHERE config.configType IN ?1",
            ServiceConfigMappingEntity.class);
    return daoUtils.selectList(query, configTypes);
  }

  @Transactional
  public List<ServiceConfigMappingEntity> findByServiceAndType(
      long clusterId, String serviceName,
      Collection<String> configTypes) {
    if (configTypes.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<ServiceConfigMappingEntity> query =
        entityManagerProvider.get().createQuery(
            "SELECT config FROM ServiceConfigMappingEntity config"
                + " WHERE "
                + " config.clusterId = ?1"
                + " AND config.serviceName = ?2"
                + " AND config.configType IN ?5",
            ServiceConfigMappingEntity.class);
    return daoUtils.selectList(query, clusterId, serviceName, configTypes);
  }

  @Transactional
  public void refresh(
      ServiceConfigMappingEntity serviceConfigMappingEntity) {
    entityManagerProvider.get().refresh(serviceConfigMappingEntity);
  }

  @Transactional
  public ServiceConfigMappingEntity merge(
      ServiceConfigMappingEntity serviceConfigMappingEntity) {
    return entityManagerProvider.get().merge(
        serviceConfigMappingEntity);
  }

  @Transactional
  public void remove(
      ServiceConfigMappingEntity serviceConfigMappingEntity) {
    entityManagerProvider.get().remove(merge(serviceConfigMappingEntity));
  }

  @Transactional
  public void removeByType(Collection<String> configTypes) {
    for (ServiceConfigMappingEntity entity : findByType(configTypes)) {
      remove(entity);
    }
  }
}
