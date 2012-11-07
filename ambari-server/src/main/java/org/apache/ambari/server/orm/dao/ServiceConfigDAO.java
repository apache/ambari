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
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class ServiceConfigDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Transactional
  public ServiceConfigEntity findByPK(Integer primaryKey) {
    return entityManagerProvider.get().find(ServiceConfigEntity.class, primaryKey);
  }

  @Transactional
  public List<ServiceConfigEntity> findByClusterService(ClusterServiceEntity clusterServiceEntity) {
    TypedQuery<ServiceConfigEntity> query = entityManagerProvider.get().createQuery("select config " +
            "from ServiceConfigEntity config " +
            "join config.clusterServiceEntity clusterService " +
            "where clusterService = :service", ServiceConfigEntity.class);
    query.setParameter("service", clusterServiceEntity);
    return query.getResultList();
  }

  @Transactional
  public List<ServiceConfigEntity> findAll() {
    TypedQuery<ServiceConfigEntity> query = entityManagerProvider.get().createQuery("select c from ServiceConfigEntity c"
            , ServiceConfigEntity.class);
    return query.getResultList();
  }

  @Transactional
  public void refresh(ServiceConfigEntity serviceConfigEntity) {
    entityManagerProvider.get().refresh(serviceConfigEntity);
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

  @Transactional
  public void removeByPK(Integer primaryKey) {
    remove(findByPK(primaryKey));
  }

}
