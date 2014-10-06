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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.apache.ambari.server.orm.DBAccessor.DbType.ORACLE;
import static org.apache.ambari.server.orm.dao.DaoUtils.ORACLE_LIST_LIMIT;

@Singleton
public class HostRoleCommandDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public HostRoleCommandEntity findByPK(long taskId) {
    return entityManagerProvider.get().find(HostRoleCommandEntity.class, taskId);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByPKs(Collection<Long> taskIds) {
    if (taskIds == null || taskIds.isEmpty()) {
      return Collections.emptyList();
    }

    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery(
      "SELECT task FROM HostRoleCommandEntity task WHERE task.taskId IN ?1 " +
        "ORDER BY task.taskId",
      HostRoleCommandEntity.class);

    if (daoUtils.getDbType().equals(ORACLE) && taskIds.size() > ORACLE_LIST_LIMIT) {
      List<HostRoleCommandEntity> result = new ArrayList<HostRoleCommandEntity>();

      List<List<Long>> lists = Lists.partition(new ArrayList<Long>(taskIds), ORACLE_LIST_LIMIT);
      for (List<Long> list : lists) {
        result.addAll(daoUtils.selectList(query, list));
      }

      return result;
    }

    return daoUtils.selectList(query, taskIds);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByRequestIds(Collection<Long> requestIds) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery(
        "SELECT task FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 " +
            "ORDER BY task.taskId", HostRoleCommandEntity.class);
    return daoUtils.selectList(query, requestIds);
  }

  @RequiresSession
  public List<Long> findTaskIdsByRequestIds(Collection<Long> requestIds) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT task.taskId FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 " +
            "ORDER BY task.taskId", Long.class);
    return daoUtils.selectList(query, requestIds);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery(
        "SELECT DISTINCT task FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 AND task.taskId IN ?2 " +
            "ORDER BY task.taskId", HostRoleCommandEntity.class
    );
    return daoUtils.selectList(query, requestIds, taskIds);
  }

  @RequiresSession
  public List<Long> findTaskIdsByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT DISTINCT task.taskId FROM HostRoleCommandEntity task " +
            "WHERE task.requestId IN ?1 AND task.taskId IN ?2 " +
            "ORDER BY task.taskId", Long.class
    );

    if (daoUtils.getDbType().equals(ORACLE) && taskIds.size() > ORACLE_LIST_LIMIT) {
      List<Long> result = new ArrayList<Long>();

      List<List<Long>> lists = Lists.partition(new ArrayList<Long>(taskIds), ORACLE_LIST_LIMIT);
      for (List<Long> taskIdList : lists) {
        result.addAll(daoUtils.selectList(query, requestIds, taskIdList));
      }

      return result;
    }
    return daoUtils.selectList(query, requestIds, taskIds);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findSortedCommandsByStageAndHost(StageEntity stageEntity, HostEntity hostEntity) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT hostRoleCommand " +
        "FROM HostRoleCommandEntity hostRoleCommand " +
        "WHERE hostRoleCommand.stage=?1 AND hostRoleCommand.host=?2 " +
        "ORDER BY hostRoleCommand.taskId", HostRoleCommandEntity.class);
    return daoUtils.selectList(query, stageEntity, hostEntity);
  }

  @RequiresSession
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

  @RequiresSession
  public List<Long> findTaskIdsByStage(long requestId, long stageId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery("SELECT hostRoleCommand.taskId " +
        "FROM HostRoleCommandEntity hostRoleCommand " +
        "WHERE hostRoleCommand.stage.requestId=?1 " +
        "AND hostRoleCommand.stage.stageId=?2 "+
        "ORDER BY hostRoleCommand.taskId", Long.class);

    return daoUtils.selectList(query, requestId, stageId);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByHostRole(String hostName, long requestId, long stageId, String role) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT command " +
        "FROM HostRoleCommandEntity command " +
        "WHERE command.hostName=?1 AND command.requestId=?2 " +
        "AND command.stageId=?3 AND command.role=?4 " +
        "ORDER BY command.taskId", HostRoleCommandEntity.class);

    return daoUtils.selectList(query, hostName, requestId, stageId, role);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findByRequest(long requestId) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT command " +
      "FROM HostRoleCommandEntity command " +
      "WHERE command.requestId=?1 ORDER BY command.taskId", HostRoleCommandEntity.class);
    return daoUtils.selectList(query, requestId);
  }

  @RequiresSession
  public List<Long> findTaskIdsByRequest(long requestId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery("SELECT command.taskId " +
      "FROM HostRoleCommandEntity command " +
      "WHERE command.requestId=?1 ORDER BY command.taskId", Long.class);
    return daoUtils.selectList(query, requestId);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), HostRoleCommandEntity.class);
  }

  @RequiresSession
  public List<Long> getRequestsByTaskStatus(Collection<HostRoleStatus> statuses,
    boolean match, boolean checkAllTasks, int maxResults, boolean ascOrder) {

    List<Long> results;
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

    queryStr.append("ORDER BY command.requestId ").append(ascOrder ? "ASC" : "DESC");
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(queryStr.toString(),
      Long.class);
    query.setMaxResults(maxResults);

    if (statuses != null && !statuses.isEmpty()) {
      results = daoUtils.selectList(query, statuses);
    } else {
      results = daoUtils.selectList(query);
    }
    return results;
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
  public void create(HostRoleCommandEntity stageEntity) {
    entityManagerProvider.get().persist(stageEntity);
  }

  @Transactional
  public HostRoleCommandEntity merge(HostRoleCommandEntity stageEntity) {
    HostRoleCommandEntity entity = entityManagerProvider.get().merge(stageEntity);
    return entity;
  }

  @Transactional
  public List<HostRoleCommandEntity> mergeAll(Collection<HostRoleCommandEntity> entities) {
    List<HostRoleCommandEntity> managedList = new ArrayList<HostRoleCommandEntity>(entities.size());
    for (HostRoleCommandEntity entity : entities) {
      managedList.add(entityManagerProvider.get().merge(entity));
    }
    return managedList;
  }

  @Transactional
  public void remove(HostRoleCommandEntity stageEntity) {
    entityManagerProvider.get().remove(merge(stageEntity));
  }

  @Transactional
  public void removeByPK(int taskId) {
    remove(findByPK(taskId));
  }
}
