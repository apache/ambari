/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntityPK;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ServiceGroupDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public ServiceGroupEntity findByPK(ServiceGroupEntityPK clusterServiceGroupEntityPK) {
    return entityManagerProvider.get().find(ServiceGroupEntity.class, clusterServiceGroupEntityPK);
  }

  @RequiresSession
  public ServiceGroupEntity findByClusterAndServiceGroupIds(Long clusterId, Long serviceGroupId) {
    TypedQuery<ServiceGroupEntity> query = entityManagerProvider.get()
      .createNamedQuery("serviceGroupByClusterAndServiceGroupIds", ServiceGroupEntity.class);
    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceGroupId", serviceGroupId);

    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  @RequiresSession
  public List<ServiceGroupEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), ServiceGroupEntity.class);
  }

  @Transactional
  public void refresh(ServiceGroupEntity clusterServiceGroupEntity) {
    entityManagerProvider.get().refresh(clusterServiceGroupEntity);
  }

  @Transactional
  public void create(ServiceGroupEntity clusterServiceGroupEntity) {
    entityManagerProvider.get().persist(clusterServiceGroupEntity);
  }

  @Transactional
  public ServiceGroupEntity merge(ServiceGroupEntity clusterServiceGroupEntity) {
    return entityManagerProvider.get().merge(clusterServiceGroupEntity);
  }

  @Transactional
  public void remove(ServiceGroupEntity clusterServiceGroupEntity) {
    entityManagerProvider.get().remove(merge(clusterServiceGroupEntity));
  }

  @Transactional
  public void removeByPK(ServiceGroupEntityPK clusterServiceGroupEntityPK) {
    ServiceGroupEntity entity = findByPK(clusterServiceGroupEntityPK);
    entityManagerProvider.get().remove(entity);
  }

}