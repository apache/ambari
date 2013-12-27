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
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import java.util.*;

@Singleton
public class HostRoleCommandDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;
  private static Logger LOG = LoggerFactory.getLogger(HostRoleCommandDAO.class);
  private static final int REQUESTS_RESULT_LIMIT = 10;

  @Transactional
  public HostRoleCommandEntity findByPK(long taskId) {
    return entityManagerProvider.get().find(HostRoleCommandEntity.class, taskId);
  }

  @Transactional
  public List<HostRoleCommandEntity> findByPKs(Collection<Long> taskIds) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery(
        "SELECT task FROM HostRoleCommandEntity task WHERE task.taskId IN ?1 " +
            "ORDER BY task.taskId",
        HostRoleCommandEntity.class);
    return daoUtils.selectList(query, taskIds);
  }

  @Transactional
  public List<HostRoleCommandEntity> findByRequestIds(Collection<Long> requestIds) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery(
        "SELECT task FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 " +
            "ORDER BY task.taskId", HostRoleCommandEntity.class);
    return daoUtils.selectList(query, requestIds);
  }

  @Transactional
  public List<Long> findTaskIdsByRequestIds(Collection<Long> requestIds) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT task.taskId FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 " +
            "ORDER BY task.taskId", Long.class);
    return daoUtils.selectList(query, requestIds);
  }

  @Transactional
  public List<HostRoleCommandEntity> findByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery(
        "SELECT DISTINCT task FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 AND task.taskId IN ?2 " +
            "ORDER BY task.taskId", HostRoleCommandEntity.class
    );
    return daoUtils.selectList(query, requestIds, taskIds);
  }

  @Transactional
  public List<Long> findTaskIdsByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT DISTINCT task.taskId FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 AND task.taskId IN ?2 " +
            "ORDER BY task.taskId", Long.class
    );
    return daoUtils.selectList(query, requestIds, taskIds);
  }

  @Transactional
  public List<HostRoleCommandEntity> findSortedCommandsByStageAndHost(StageEntity stageEntity, HostEntity hostEntity) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT hostRoleCommand " +
        "FROM HostRoleCommandEntity hostRoleCommand " +
        "WHERE hostRoleCommand.stage=?1 AND hostRoleCommand.host=?2 " +
        "ORDER BY hostRoleCommand.taskId", HostRoleCommandEntity.class);
    return daoUtils.selectList(query, stageEntity, hostEntity);
  }

  @Transactional
  public Map<String, List<HostRoleCommandEntity>> findSortedCommandsByStage(StageEntity stageEntity) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT hostRoleCommand " +
        "FROM HostRoleCommandEntity hostRoleCommand " +
        "WHERE hostRoleCommand.stage=?1 " +
        "ORDER BY hostRoleCommand.hostName, hostRoleCommand.taskId", HostRoleCommandEntity.class);
    List<HostRoleCommandEntity> commandEntities = daoUtils.selectList(query, stageEntity);

    Map<String, List<HostRoleCommandEntity>> hostCommands = new HashMap<String, List<HostRoleCommandEntity>>();

    for (HostRoleCommandEntity commandEntity : commandEntities) {
      if (!hostCommands.containsKey(commandEntity.getHostName())) {
        hostCommands.put(commandEntity.getHostName(), new ArrayList<HostRoleCommandEntity>());
      }

      hostCommands.get(commandEntity.getHostName()).add(commandEntity);
    }

    return hostCommands;
  }

  @Transactional
  public List<HostRoleCommandEntity> findByHostRole(String hostName, long requestId, long stageId, String role) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT command " +
        "FROM HostRoleCommandEntity command " +
        "WHERE command.hostName=?1 AND command.requestId=?2 " +
        "AND command.stageId=?3 AND command.role=?4 " +
        "ORDER BY command.taskId", HostRoleCommandEntity.class);

    return daoUtils.selectList(query, hostName, requestId, stageId, role);
  }

  @Transactional
  public List<Long> getRequests() {
    String queryStr = "SELECT DISTINCT command.requestId " +
        "FROM HostRoleCommandEntity command ORDER BY command.requestId DESC";
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(queryStr,
        Long.class);
    query.setMaxResults(REQUESTS_RESULT_LIMIT);
    return daoUtils.selectList(query);
  }

  @Transactional
  /**
   * NB: You cannot rely on return value if batch write is enabled
   */
  public int updateStatusByRequestId(long requestId, HostRoleStatus target, Collection<HostRoleStatus> sources) {
    TypedQuery<HostRoleCommandEntity> selectQuery = entityManagerProvider.get().createQuery("SELECT command " +
        "FROM HostRoleCommandEntity command " +
        "WHERE command.requestId=?1 AND command.status IN ?2", HostRoleCommandEntity.class);

    List<HostRoleCommandEntity> commandEntities = daoUtils.selectList(selectQuery, requestId, sources);

    for (HostRoleCommandEntity entity : commandEntities) {
      entity.setStatus(target);
      merge(entity);
    }

    return commandEntities.size();
  }

  @Transactional
  public List<HostRoleCommandEntity> findByRequest(long requestId) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT command " +
        "FROM HostRoleCommandEntity command " +
        "WHERE command.requestId=?1 ORDER BY command.taskId", HostRoleCommandEntity.class);
    return daoUtils.selectList(query, requestId);
  }
  
  @Transactional
  public List<Long> findTaskIdsByRequest(long requestId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery("SELECT command.taskId " +
        "FROM HostRoleCommandEntity command " +
        "WHERE command.requestId=?1 ORDER BY command.taskId", Long.class);
    return daoUtils.selectList(query, requestId);
  }

  @Transactional
  public void create(HostRoleCommandEntity stageEntity) {
    entityManagerProvider.get().persist(stageEntity);
  }

  @Transactional
  public HostRoleCommandEntity merge(HostRoleCommandEntity stageEntity) {
    HostRoleCommandEntity entity = entityManagerProvider.get().merge(stageEntity);
    return entity;

  }

  @Transactional
  public void remove(HostRoleCommandEntity stageEntity) {
    entityManagerProvider.get().remove(merge(stageEntity));
  }

  @Transactional
  public void removeByPK(int taskId) {
    remove(findByPK(taskId));
  }

  @Transactional
  public List<Long> getRequestsByTaskStatus(
      Collection<HostRoleStatus> statuses, boolean match, boolean checkAllTasks) {
    List<Long> results = null;
    StringBuilder queryStr = new StringBuilder();

    queryStr.append("SELECT DISTINCT command.requestId ").append(
        "FROM HostRoleCommandEntity command ");
    if (statuses != null && !statuses.isEmpty()) {
      queryStr.append("WHERE ");

      if (checkAllTasks) {
        queryStr.append("command.requestId ");
        if (!match) {
          queryStr.append("NOT ");
        }
        queryStr.append("IN (").append("SELECT c.requestId ")
            .append("FROM HostRoleCommandEntity c ")
            .append("WHERE c.requestId = command.requestId ")
            .append("AND c.status IN ?1) ");
      } else {
        queryStr.append("command.status ");
        if (!match) {
          queryStr.append("NOT ");
        }
        queryStr.append("IN ?1 ");
      }
    }

    queryStr.append("ORDER BY command.requestId DESC");
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(queryStr.toString(),
        Long.class);
    query.setMaxResults(REQUESTS_RESULT_LIMIT);

    if (statuses != null && !statuses.isEmpty()) {
      results = daoUtils.selectList(query, statuses);
    } else {
      results = daoUtils.selectList(query);
    }
    return results;
  }

}
