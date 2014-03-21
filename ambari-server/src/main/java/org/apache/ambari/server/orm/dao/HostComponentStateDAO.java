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
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntityPK;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

@Singleton
public class HostComponentStateDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @RequiresSession
  public HostComponentStateEntity findByPK(HostComponentStateEntityPK primaryKey) {
    return entityManagerProvider.get().find(HostComponentStateEntity.class, primaryKey);
  }

  @RequiresSession
  public List<HostComponentStateEntity> findAll() {
    TypedQuery<HostComponentStateEntity> query = entityManagerProvider.get()
      .createQuery("SELECT hsc from HostComponentStateEntity hsc", HostComponentStateEntity.class);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  @Transactional
  public void refresh(HostComponentStateEntity hostComponentStateEntity) {
    entityManagerProvider.get().refresh(hostComponentStateEntity);
  }

  @Transactional
  public void create(HostComponentStateEntity hostComponentStateEntity) {
    entityManagerProvider.get().persist(hostComponentStateEntity);
  }

  @Transactional
  public HostComponentStateEntity merge(HostComponentStateEntity hostComponentStateEntity) {
    return entityManagerProvider.get().merge(hostComponentStateEntity);
  }

  @Transactional
  public void remove(HostComponentStateEntity hostComponentStateEntity) {
    entityManagerProvider.get().remove(merge(hostComponentStateEntity));
  }

  @Transactional
  public void removeByPK(HostComponentStateEntityPK primaryKey) {
    remove(findByPK(primaryKey));
  }
}
