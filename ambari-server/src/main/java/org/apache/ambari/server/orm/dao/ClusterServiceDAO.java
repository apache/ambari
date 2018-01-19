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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.ServiceDependencyEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ClusterServiceDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public ClusterServiceEntity findByPK(ClusterServiceEntityPK clusterServiceEntityPK) {
    return entityManagerProvider.get().find(ClusterServiceEntity.class, clusterServiceEntityPK);
  }

  @RequiresSession
  public ClusterServiceEntity findById(Long clusterId, Long serviceGroupId, Long serviceId) {
    TypedQuery<ClusterServiceEntity> query = entityManagerProvider.get()
      .createNamedQuery("clusterServiceById", ClusterServiceEntity.class);
    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceGroupId", serviceGroupId);
    query.setParameter("serviceId", serviceId);

    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  @RequiresSession
  public ClusterServiceEntity findByName(String clusterName, String serviceGroupName, String serviceName) {
    TypedQuery<ClusterServiceEntity> query = entityManagerProvider.get()
            .createNamedQuery("clusterServiceByName" , ClusterServiceEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("serviceGroupName", serviceGroupName);
    query.setParameter("serviceName", serviceName);

    try {
      return query.getSingleResult();
    }
    catch (NoResultException ignored) {
      return null;
    }
  }

  @RequiresSession
  public List<ClusterServiceEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), ClusterServiceEntity.class);
  }

  @Transactional
  public void refresh(ClusterServiceEntity clusterServiceEntity) {
    entityManagerProvider.get().refresh(clusterServiceEntity);
  }

  @Transactional
  public void create(ClusterServiceEntity clusterServiceEntity) {
    entityManagerProvider.get().persist(clusterServiceEntity);
  }

  @Transactional
  public void createServiceDependency(ServiceDependencyEntity serviceDependencyEntity) {
    entityManagerProvider.get().persist(serviceDependencyEntity);
  }

  @Transactional
  public ClusterServiceEntity merge(ClusterServiceEntity clusterServiceEntity) {
    return entityManagerProvider.get().merge(clusterServiceEntity);
  }

  @Transactional
  public ServiceDependencyEntity mergeServiceDependency(ServiceDependencyEntity serviceDependencyEntity) {
    return entityManagerProvider.get().merge(serviceDependencyEntity);
  }

  @Transactional
  public void remove(ClusterServiceEntity clusterServiceEntity) {
    entityManagerProvider.get().remove(merge(clusterServiceEntity));
  }

  @Transactional
  public void removeServiceDependency(ServiceDependencyEntity serviceDependencyEntity) {
    entityManagerProvider.get().remove(mergeServiceDependency(serviceDependencyEntity));
  }

  @Transactional
  public void removeByPK(ClusterServiceEntityPK clusterServiceEntityPK) {
    ClusterServiceEntity entity = findByPK(clusterServiceEntityPK);
    entityManagerProvider.get().remove(entity);
  }

}
