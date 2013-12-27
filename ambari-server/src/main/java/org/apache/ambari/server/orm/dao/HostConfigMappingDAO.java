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

import java.util.*;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.google.inject.Singleton;
import org.apache.ambari.server.orm.entities.HostConfigMappingEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

/**
 * Used for host configuration mapping operations.
 */
@Singleton
public class HostConfigMappingDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;
  
  @Transactional
  public void create(HostConfigMappingEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  @Transactional
  public HostConfigMappingEntity merge(HostConfigMappingEntity entity) {
    return entityManagerProvider.get().merge(entity);
  }

  @Transactional
  public List<HostConfigMappingEntity> findByType(long clusterId, String hostName, String type) {
    TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createQuery(
      "SELECT entity FROM HostConfigMappingEntity entity " +
      "WHERE entity.clusterId = ?1 AND entity.hostName = ?2 AND entity.type = ?3",
      HostConfigMappingEntity.class);

    return daoUtils.selectList(query, Long.valueOf(clusterId), hostName, type);
  }

  @Transactional
  public HostConfigMappingEntity findSelectedByType(long clusterId,
      String hostName, String type) {

    TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createQuery(
        "SELECT entity FROM HostConfigMappingEntity entity " +
        "WHERE entity.clusterId = ?1 AND entity.hostName = ?2 " +
            "AND entity.type = ?3 " +
            "AND entity.selected > 0",
        HostConfigMappingEntity.class);

    return daoUtils.selectSingle(query, Long.valueOf(clusterId), hostName, type);
  }

  @Transactional
  public List<HostConfigMappingEntity> findSelected(long clusterId, String hostName) {
    TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createQuery(
        "SELECT entity FROM HostConfigMappingEntity entity " +
        "WHERE entity.clusterId = ?1 AND entity.hostName = ?2 " +
            "AND entity.selected > 0",
        HostConfigMappingEntity.class);

    return daoUtils.selectList(query, Long.valueOf(clusterId), hostName);
  }

  @Transactional
  public List<HostConfigMappingEntity> findSelectedByHosts(long clusterId, Collection<String> hostNames) {

    if (hostNames == null || hostNames.isEmpty()) {
      return Collections.emptyList();
    }

    TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createQuery(
        "SELECT entity FROM HostConfigMappingEntity entity " +
            "WHERE entity.clusterId = ?1 AND entity.hostName IN ?2 " +
            "AND entity.selected > 0",
        HostConfigMappingEntity.class);

    return daoUtils.selectList(query, Long.valueOf(clusterId), hostNames);
  }

  @Transactional
  public List<HostConfigMappingEntity> findSelectedHostsByType(long clusterId,
      String type) {
    TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createQuery(
        "SELECT entity FROM HostConfigMappingEntity entity " +
        "WHERE entity.clusterId = ?1 AND entity.type = ?2 AND entity.selected > 0",
    HostConfigMappingEntity.class);
    return daoUtils.selectList(query, Long.valueOf(clusterId), type);
  }

  @Transactional
  public Map<String, List<HostConfigMappingEntity>> findSelectedHostsByTypes(long clusterId,
                                                                             Collection<String> types) {
    TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createQuery(
        "SELECT entity FROM HostConfigMappingEntity entity " +
            "WHERE entity.clusterId = ?1 AND entity.type IN ?2 AND entity.selected > 0",
        HostConfigMappingEntity.class);

    Map<String, List<HostConfigMappingEntity>> mappingsByType = new HashMap<String, List<HostConfigMappingEntity>>();

    for (String type : types) {
      if (!mappingsByType.containsKey(type)) {
        mappingsByType.put(type, new ArrayList<HostConfigMappingEntity>());
      }
    }

    if (!types.isEmpty()) {
      List<HostConfigMappingEntity> mappings = daoUtils.selectList(query, clusterId, types);
      for (HostConfigMappingEntity mapping : mappings) {
        mappingsByType.get(mapping.getType()).add(mapping);
      }
    }

    return mappingsByType;
  }

  /**
   * @param clusterId
   * @param hostName
   */
  @Transactional
  public void removeHost(long clusterId, String hostName) {
    TypedQuery<HostConfigMappingEntity> query = entityManagerProvider.get().createQuery(
        "SELECT entity FROM HostConfigMappingEntity entity " +
        "WHERE entity.clusterId = ?1 AND entity.hostName = ?2",
        HostConfigMappingEntity.class);
    
    List<HostConfigMappingEntity> list = daoUtils.selectList(query, Long.valueOf(clusterId), hostName);

      for (HostConfigMappingEntity entity : list) {
        entityManagerProvider.get().remove(entity);
      }
    
  }

}
