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
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@RequiresSession
public class ServiceConfigDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;


  public ServiceConfigEntity find(Long serviceConfigId) {
    return entityManagerProvider.get().find(ServiceConfigEntity.class, serviceConfigId);
  }

  public ServiceConfigEntity findByServiceAndVersion(String serviceName, Long version) {
    TypedQuery<ServiceConfigEntity> query = entityManagerProvider.get().
        createQuery("SELECT scv FROM ServiceConfigEntity scv " +
            "WHERE scv.serviceName=?1 AND scv.version=?2", ServiceConfigEntity.class);
    return daoUtils.selectOne(query, serviceName, version);
  }

  public Map<String, Long> findMaxVersions(Long clusterId) {
    Map<String, Long> maxVersions = new HashMap<String, Long>();

    TypedQuery<String> query = entityManagerProvider.get().createQuery("SELECT DISTINCT scv.serviceName FROM ServiceConfigEntity scv WHERE scv.clusterId = ?1", String.class);
    List<String> serviceNames = daoUtils.selectList(query, clusterId);

    for (String serviceName : serviceNames) {
      maxVersions.put(serviceName, findMaxVersion(clusterId, serviceName).getVersion());
    }

    return maxVersions;
  }

  public List<Long> getServiceConfigVersionsByConfig(Long clusterId, String configType, Long configVersion) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery("SELECT scv.version " +
        "FROM ServiceConfigEntity scv JOIN scv.clusterConfigEntities cc " +
        "WHERE cc.clusterId=?1 AND cc.type = ?2 AND cc.version = ?3", Long.class);
    return daoUtils.selectList(query, clusterId, configType, configVersion);
  }

  public List<ServiceConfigEntity> getLastServiceConfigs(Long clusterId) {
    TypedQuery<ServiceConfigEntity> query = entityManagerProvider.get().
      createQuery("SELECT scv FROM ServiceConfigEntity scv " +
        "WHERE scv.clusterId = ?1 AND scv.createTimestamp = (" +
        "SELECT MAX(scv2.createTimestamp) FROM ServiceConfigEntity scv2 " +
        "WHERE scv2.serviceName = scv.serviceName AND scv2.clusterId = ?1)",
        ServiceConfigEntity.class);

    return daoUtils.selectList(query, clusterId);
  }

  public ServiceConfigEntity getLastServiceConfig(Long clusterId, String serviceName) {
    TypedQuery<ServiceConfigEntity> query = entityManagerProvider.get().
        createQuery("SELECT scv FROM ServiceConfigEntity scv " +
          "WHERE scv.clusterId = ?1 AND scv.serviceName = ?2 " +
          "ORDER BY scv.createTimestamp DESC",
          ServiceConfigEntity.class);

    return daoUtils.selectOne(query, clusterId, serviceName);
  }

  public ServiceConfigEntity findMaxVersion(Long clusterId, String serviceName) {
    TypedQuery<ServiceConfigEntity> query = entityManagerProvider.get().createQuery("SELECT scv FROM ServiceConfigEntity scv " +
      "WHERE scv.clusterId=?1 AND scv.serviceName=?2 AND scv.version = (" +
      "SELECT max(scv2.version) FROM ServiceConfigEntity scv2 " +
      "WHERE scv2.clusterId=?1 AND scv2.serviceName=?2)", ServiceConfigEntity.class);

    return daoUtils.selectSingle(query, clusterId, serviceName);
  }

  public List<ServiceConfigEntity> getServiceConfigs(Long clusterId) {
    TypedQuery<ServiceConfigEntity> query = entityManagerProvider.get()
      .createQuery("SELECT scv FROM ServiceConfigEntity scv " +
        "WHERE scv.clusterId=?1 " +
        "ORDER BY scv.createTimestamp DESC", ServiceConfigEntity.class);

    return daoUtils.selectList(query, clusterId);
  }

  @Transactional
  public void create(ServiceConfigEntity serviceConfigEntity) {
    entityManagerProvider.get().persist(serviceConfigEntity);
  }

  @Transactional
  public ServiceConfigEntity merge(ServiceConfigEntity serviceConfigEntity) {
    return entityManagerProvider.get().merge(serviceConfigEntity);
  }

  @Transactional
  public void remove(ServiceConfigEntity serviceConfigEntity) {
    entityManagerProvider.get().remove(merge(serviceConfigEntity));
  }


}
