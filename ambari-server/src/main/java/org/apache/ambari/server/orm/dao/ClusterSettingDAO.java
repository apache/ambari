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
import org.apache.ambari.server.orm.entities.ClusterSettingEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ClusterSettingDAO {
    @Inject
    Provider<EntityManager> entityManagerProvider;
    @Inject
    DaoUtils daoUtils;

    @RequiresSession
    public ClusterSettingEntity findByPK(Long clusterSettingId) {
        TypedQuery<ClusterSettingEntity> query = entityManagerProvider.get()
                .createNamedQuery("clusterSettingById", ClusterSettingEntity.class);
        query.setParameter("clusterSettingId", clusterSettingId);

        try {
            return query.getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    @RequiresSession
    public ClusterSettingEntity findByClusterIdAndSettingName(Long clusterId, String clusterSettingName) {
        TypedQuery<ClusterSettingEntity> query = entityManagerProvider.get()
                .createNamedQuery("clusterSettingByClusterIdAndSettingName", ClusterSettingEntity.class);
        query.setParameter("clusterId", clusterId);
        query.setParameter("clusterSettingName", clusterSettingName);

        try {
            return query.getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    @RequiresSession
    public List<ClusterSettingEntity> findAll() {
        return daoUtils.selectAll(entityManagerProvider.get(), ClusterSettingEntity.class);
    }

    @Transactional
    public void refresh(ClusterSettingEntity clusterSettingEntity) {
        entityManagerProvider.get().refresh(clusterSettingEntity);
    }

    @Transactional
    public void create(ClusterSettingEntity clusterSettingEntity) {
        entityManagerProvider.get().persist(clusterSettingEntity);
    }

    @Transactional
    public ClusterSettingEntity merge(ClusterSettingEntity clusterSettingEntity) {
        return entityManagerProvider.get().merge(clusterSettingEntity);
    }

    @Transactional
    public void remove(ClusterSettingEntity clusterSettingEntity) {
        entityManagerProvider.get().remove(merge(clusterSettingEntity));
    }

    @Transactional
    public void removeByPK(Long clusterId) {
        ClusterSettingEntity entity = findByPK(clusterId);
        entityManagerProvider.get().remove(entity);
    }

}