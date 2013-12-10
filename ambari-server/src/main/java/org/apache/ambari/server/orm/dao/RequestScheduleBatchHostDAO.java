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
import org.apache.ambari.server.orm.entities.RequestScheduleBatchHostEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleBatchHostEntityPK;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

@Singleton
public class RequestScheduleBatchHostDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @Transactional
  public RequestScheduleBatchHostEntity findByPK
    (RequestScheduleBatchHostEntityPK requestScheduleBatchHostEntityPK) {
    return entityManagerProvider.get().find(RequestScheduleBatchHostEntity
      .class, requestScheduleBatchHostEntityPK);
  }

  @Transactional
  public List<RequestScheduleBatchHostEntity> findBySchedule(Long scheduleId) {
    TypedQuery<RequestScheduleBatchHostEntity> query = entityManagerProvider
      .get().createNamedQuery("batchHostsBySchedule",
        RequestScheduleBatchHostEntity.class);

    query.setParameter("id", scheduleId);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  @Transactional
  public void create(RequestScheduleBatchHostEntity batchHostEntity) {
    entityManagerProvider.get().persist(batchHostEntity);
  }

  @Transactional
  public RequestScheduleBatchHostEntity merge(RequestScheduleBatchHostEntity batchHostEntity) {
    return entityManagerProvider.get().merge(batchHostEntity);
  }

  @Transactional
  public void refresh(RequestScheduleBatchHostEntity batchHostEntity) {
    entityManagerProvider.get().refresh(batchHostEntity);
  }

  @Transactional
  public void remove(RequestScheduleBatchHostEntity batchHostEntity) {
    entityManagerProvider.get().remove(batchHostEntity);
  }

  @Transactional
  public void removeBySchedule(Long scheduleId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
      "DELETE FROM RequestScheduleBatchHostEntity batchHosts WHERE " +
        "batchHosts.scheduleId = ?1", Long.class);

    daoUtils.executeUpdate(query, scheduleId);
    entityManagerProvider.get().flush();
  }
}
