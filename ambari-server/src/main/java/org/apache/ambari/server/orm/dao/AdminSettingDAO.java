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
import org.apache.ambari.server.orm.entities.AdminSettingEntity;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

@Singleton
public class AdminSettingDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Find an admin setting with the given name.
   *
   * @param name - name of admin setting.
   * @return  a matching admin setting or null
   */
  @RequiresSession
  public AdminSettingEntity findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    TypedQuery<AdminSettingEntity> query = entityManagerProvider.get()
            .createNamedQuery("adminSettingByName", AdminSettingEntity.class);
    query.setParameter("name", name);
    return daoUtils.selectOne(query);
  }

  /**
   * Find all admin settings.
   *
   * @return all admin setting instances.
   */
  @RequiresSession
  public List<AdminSettingEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), AdminSettingEntity.class);
  }

  /**
   * Create a new admin setting entity.
   *
   * @param entity - entity to be created
   */
  @Transactional
  public void create(AdminSettingEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Update admin setting instance.
   *
   * @param entity - entity to be updated.
   * @return - updated admin entity.
   */
  @Transactional
  public AdminSettingEntity merge(AdminSettingEntity entity) {
    return entityManagerProvider.get().merge(entity);
  }

  /**
   * Delete admin setting with given name.
   *
   * @param name - name of admin setting to be deleted.
   */
  @Transactional
  public void removeByName(String name) {
    AdminSettingEntity entity = findByName(name);
    if (entity!= null) {
      entityManagerProvider.get().remove(entity);
    }
  }
}
