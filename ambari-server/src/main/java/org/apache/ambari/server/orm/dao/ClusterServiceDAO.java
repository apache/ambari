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
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

@Singleton
public class ClusterServiceDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Transactional
  public ClusterServiceEntity findByPK(ClusterServiceEntityPK clusterServiceEntityPK) {
    return entityManagerProvider.get().find(ClusterServiceEntity.class, clusterServiceEntityPK);
  }

  @Transactional
  public ClusterServiceEntity findByClusterAndServiceNames(String  clusterName, String serviceName) {
    TypedQuery<ClusterServiceEntity> query = entityManagerProvider.get()
            .createNamedQuery("clusterServiceByClusterAndServiceNames", ClusterServiceEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("serviceName", serviceName);

    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
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
  public ClusterServiceEntity merge(ClusterServiceEntity clusterServiceEntity) {
    return entityManagerProvider.get().merge(clusterServiceEntity);
  }

  @Transactional
  public void remove(ClusterServiceEntity clusterServiceEntity) {
    entityManagerProvider.get().remove(merge(clusterServiceEntity));
  }

  @Transactional
  public void removeByPK(ClusterServiceEntityPK clusterServiceEntityPK) {
    remove(findByPK(clusterServiceEntityPK));
  }

}
