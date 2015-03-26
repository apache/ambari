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
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.UserWidgetEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

@Singleton
public class UserWidgetDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils daoUtils;

  @RequiresSession
  public UserWidgetEntity findById(Long id) {
    return entityManagerProvider.get().find(UserWidgetEntity.class, id);
  }

  @RequiresSession
  public List<UserWidgetEntity> findByCluster(long clusterId) {
    TypedQuery<UserWidgetEntity> query = entityManagerProvider.get()
            .createNamedQuery("UserWidgetEntity.findByCluster", UserWidgetEntity.class);
    query.setParameter("clusterId", clusterId);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<UserWidgetEntity> findBySectionName(String sectionName) {
    TypedQuery<UserWidgetEntity> query = entityManagerProvider.get()
            .createNamedQuery("UserWidgetEntity.findBySectionName", UserWidgetEntity.class);
    query.setParameter("sectionName", sectionName);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<UserWidgetEntity> findAll() {
    TypedQuery<UserWidgetEntity> query = entityManagerProvider.get()
            .createNamedQuery("UserWidgetEntity.findAll", UserWidgetEntity.class);

    return daoUtils.selectList(query);
  }

  @Transactional
  public void create(UserWidgetEntity userWidgetEntity) {
    entityManagerProvider.get().persist(userWidgetEntity);
  }

  @Transactional
  public UserWidgetEntity merge(UserWidgetEntity userWidgetEntity) {
    return entityManagerProvider.get().merge(userWidgetEntity);
  }

  @Transactional
  public void remove(UserWidgetEntity userWidgetEntity) {
    entityManagerProvider.get().remove(merge(userWidgetEntity));
  }

  @Transactional
  public void removeByPK(Long id) {
    entityManagerProvider.get().remove(findById(id));
  }

  @Transactional
  public void refresh(UserWidgetEntity userWidgetEntity) {
    entityManagerProvider.get().refresh(userWidgetEntity);
  }
}
