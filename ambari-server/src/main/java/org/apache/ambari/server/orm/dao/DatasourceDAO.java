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

import java.util.List;

import jakarta.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.DatasourceEntity;

import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class DatasourceDAO extends CrudDAO<DatasourceEntity, Long> {
  public DatasourceDAO() {
    super(DatasourceEntity.class);
  }

  @RequiresSession
  public List<DatasourceEntity> findByCluster(String clusterName) {
    TypedQuery<DatasourceEntity> query = entityManagerProvider.get()
        .createNamedQuery("DatasourceEntity.findByCluster", DatasourceEntity.class);
    query.setParameter("clusterName", clusterName);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public DatasourceEntity findByIdAndCluster(long id, String clusterName) {
    TypedQuery<DatasourceEntity> query = entityManagerProvider.get()
        .createNamedQuery("DatasourceEntity.findByIdAndCluster", DatasourceEntity.class);
    query.setParameter("id", id);
    query.setParameter("clusterName", clusterName);
    return daoUtils.selectOne(query);
  }

  @RequiresSession
  public DatasourceEntity findByNameAndCluster(String name, String clusterName) {
    TypedQuery<DatasourceEntity> query = entityManagerProvider.get()
        .createNamedQuery("DatasourceEntity.findByNameAndCluster", DatasourceEntity.class);
    query.setParameter("name", name);
    query.setParameter("clusterName", clusterName);
    return daoUtils.selectOne(query);
  }

  @RequiresSession
  public DatasourceEntity findByName(String name) {
    TypedQuery<DatasourceEntity> query = entityManagerProvider.get()
        .createNamedQuery("DatasourceEntity.findByName", DatasourceEntity.class);
    query.setParameter("name", name);
    return daoUtils.selectOne(query);
  }

  @RequiresSession
  public DatasourceEntity findDefaultByCluster(String clusterName) {
    TypedQuery<DatasourceEntity> query = entityManagerProvider.get()
        .createNamedQuery("DatasourceEntity.findDefaultByCluster", DatasourceEntity.class);
    query.setParameter("clusterName", clusterName);
    return daoUtils.selectOne(query);
  }

  @Transactional
  public void create(DatasourceEntity entity) {
    super.create(entity);
  }
}
