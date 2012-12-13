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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.entities.HostComponentConfigMappingEntity;

public class HostComponentConfigMappingDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @Transactional
  public List<HostComponentConfigMappingEntity> findByType(
      Collection<String> configTypes) {
    TypedQuery<HostComponentConfigMappingEntity> query =
        entityManagerProvider.get().createQuery(
            "SELECT config FROM HostComponentConfigMappingEntity config"
            + " WHERE config.configType IN ?1",
        HostComponentConfigMappingEntity.class);
    return daoUtils.selectList(query, configTypes);
  }

  @Transactional
  public void refresh(
      HostComponentConfigMappingEntity componentConfigMappingEntity) {
    entityManagerProvider.get().refresh(componentConfigMappingEntity);
  }

  @Transactional
  public HostComponentConfigMappingEntity merge(
      HostComponentConfigMappingEntity componentConfigMappingEntity) {
    return entityManagerProvider.get().merge(
        componentConfigMappingEntity);
  }

  @Transactional
  public void remove(
      HostComponentConfigMappingEntity componentConfigMappingEntity) {
    entityManagerProvider.get().remove(merge(componentConfigMappingEntity));
  }

  @Transactional
  public void removeByType(Collection<String> configTypes) {
    for (HostComponentConfigMappingEntity entity : findByType(configTypes)) {
      remove(entity);
    }
  }

  @Transactional
  public List<HostComponentConfigMappingEntity> findByHostComponentAndType(
      long clusterId, String serviceName, String componentName,
      String hostname,
      Collection<String> configTypes) {
    if (configTypes.isEmpty()) {
      return new ArrayList<HostComponentConfigMappingEntity>();
    }    
    TypedQuery<HostComponentConfigMappingEntity> query =
        entityManagerProvider.get().createQuery(
            "SELECT config FROM HostComponentConfigMappingEntity config"
            + " WHERE "
            + " config.clusterId = ?1"
            + " AND config.serviceName = ?2"
            + " AND config.componentName = ?3"
            + " AND config.hostName = ?4"
            + " AND config.configType IN ?5",
        HostComponentConfigMappingEntity.class);
    return daoUtils.selectList(query, clusterId, serviceName,
        componentName, hostname, configTypes);
  }

}
