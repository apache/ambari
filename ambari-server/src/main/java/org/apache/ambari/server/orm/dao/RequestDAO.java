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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.RequestResourceFilterEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.List;

@Singleton
public class RequestDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public RequestEntity findByPK(Long requestId) {
    return entityManagerProvider.get().find(RequestEntity.class, requestId);
  }

  @RequiresSession
  public List<RequestEntity> findByPks(Collection<Long> requestIds) {
    TypedQuery<RequestEntity> query = entityManagerProvider.get().createQuery("SELECT request FROM RequestEntity request " +
        "WHERE request.requestId IN ?1", RequestEntity.class);
    return daoUtils.selectList(query, requestIds);
  }

  @RequiresSession
  public List<RequestEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), RequestEntity.class);
  }

  @RequiresSession
  public List<RequestResourceFilterEntity> findAllResourceFilters() {
    return daoUtils.selectAll(entityManagerProvider.get(), RequestResourceFilterEntity.class);
  }

  @RequiresSession
  public boolean isAllTasksCompleted(long requestId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT task.taskId FROM HostRoleCommandEntity task WHERE task.requestId = ?1 AND " +
          "task.stageId=(select max(stage.stageId) FROM StageEntity stage WHERE stage.requestId=?1) " +
          "AND task.status NOT IN ?2",
        Long.class
    );
    query.setMaxResults(1); //we don't need all
    return daoUtils.selectList(query, requestId, HostRoleStatus.getCompletedStates()).isEmpty();
  }

  @RequiresSession
  public Long getLastStageId(long requestId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery("SELECT max(stage.stageId) " +
      "FROM StageEntity stage WHERE stage.requestId=?1", Long.class);
    return daoUtils.selectSingle(query, requestId);
  }

  @Transactional
  public void create(RequestEntity requestEntity) {
    entityManagerProvider.get().persist(requestEntity);
  }

  @Transactional
  public RequestEntity merge(RequestEntity requestEntity) {
    return entityManagerProvider.get().merge(requestEntity);
  }

  @Transactional
  public void remove(RequestEntity requestEntity) {
    entityManagerProvider.get().remove(merge(requestEntity));
  }

  @Transactional
  public void removeByPK(Long requestId) {
    remove(findByPK(requestId));
  }
}
