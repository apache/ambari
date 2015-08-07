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

import static org.apache.ambari.server.orm.DBAccessor.DbType.ORACLE;
import static org.apache.ambari.server.orm.dao.DaoUtils.ORACLE_LIST_LIMIT;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class HostRoleCommandDAO {

  private static final String SUMMARY_DTO = String.format(
    "SELECT NEW %s(" +
      "MAX(hrc.stage.skippable), " +
      "MIN(hrc.startTime), " +
      "MAX(hrc.endTime), " +
      "hrc.stageId, " +
      "SUM(CASE WHEN hrc.status = :aborted THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :completed THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :failed THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :holding THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :holding_failed THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :holding_timedout THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :in_progress THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :pending THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :queued THEN 1 ELSE 0 END), " +
      "SUM(CASE WHEN hrc.status = :timedout THEN 1 ELSE 0 END)" +
      ") FROM HostRoleCommandEntity hrc " +
      " GROUP BY hrc.requestId, hrc.stageId HAVING hrc.requestId = :requestId",
      HostRoleCommandStatusSummaryDTO.class.getName());

  /**
   * SQL template to get requests that have at least one task in any of the
   * specified statuses.
   */
  private static final String REQUESTS_BY_TASK_STATUS_SQL = "SELECT DISTINCT task.requestId FROM HostRoleCommandEntity task WHERE task.status IN :taskStatuses ORDER BY task.requestId {0}";

  /**
   * SQL template to get all requests which have had all of their tasks
   * COMPLETED
   */
  private static final String COMPLETED_REQUESTS_SQL = "SELECT DISTINCT task.requestId FROM HostRoleCommandEntity task WHERE NOT EXISTS (SELECT task.requestId FROM HostRoleCommandEntity task WHERE task.status IN :notCompletedStatuses) ORDER BY task.requestId {0}";

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
  public List<HostRoleCommandEntity> findByHostId(Long hostId) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findByHostId",
        HostRoleCommandEntity.class);

    query.setParameter("hostId", hostId);
    return daoUtils.selectList(query);
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
  public List<Long> findTaskIdsByHostRoleAndStatus(String hostname, String role, HostRoleStatus status) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT DISTINCT task.taskId FROM HostRoleCommandEntity task " +
            "WHERE task.hostEntity.hostName=?1 AND task.role=?2 AND task.status=?3 " +
            "ORDER BY task.taskId", Long.class
    );

    return daoUtils.selectList(query, hostname, role, status);
  }

  @RequiresSession
  public List<Long> findTaskIdsByRoleAndStatus(String role, HostRoleStatus status) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(
        "SELECT DISTINCT task.taskId FROM HostRoleCommandEntity task " +
            "WHERE task.role=?1 AND task.status=?2 " +
            "ORDER BY task.taskId", Long.class);

    return daoUtils.selectList(query, role, status);
  }


  @RequiresSession
  public List<HostRoleCommandEntity> findSortedCommandsByStageAndHost(StageEntity stageEntity, HostEntity hostEntity) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT hostRoleCommand " +
        "FROM HostRoleCommandEntity hostRoleCommand " +
        "WHERE hostRoleCommand.stage=?1 AND hostRoleCommand.hostEntity.hostName=?2 " +
        "ORDER BY hostRoleCommand.taskId", HostRoleCommandEntity.class);
    return daoUtils.selectList(query, stageEntity, hostEntity.getHostName());
  }

  @RequiresSession
  public Map<String, List<HostRoleCommandEntity>> findSortedCommandsByStage(StageEntity stageEntity) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createQuery("SELECT hostRoleCommand " +
        "FROM HostRoleCommandEntity hostRoleCommand " +
        "WHERE hostRoleCommand.stage=?1 " +
        "ORDER BY hostRoleCommand.hostEntity.hostName, hostRoleCommand.taskId", HostRoleCommandEntity.class);
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
        "WHERE command.hostEntity.hostName=?1 AND command.requestId=?2 " +
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

  /**
   * Gets the commands in a particular status.
   *
   * @param statuses
   *          the statuses to include (not {@code null}).
   * @return the commands in the given set of statuses.
   */
  @RequiresSession
  public List<HostRoleCommandEntity> findByStatus(
      Collection<HostRoleStatus> statuses) {
    TypedQuery<HostRoleCommandEntity> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findByCommandStatuses",
        HostRoleCommandEntity.class);

    query.setParameter("statuses", statuses);
    return daoUtils.selectList(query);
  }

  /**
   * Gets the number of commands in a particular status.
   *
   * @param statuses
   *          the statuses to include (not {@code null}).
   * @return the count of commands in the given set of statuses.
   */
  @RequiresSession
  public Number getCountByStatus(Collection<HostRoleStatus> statuses) {
    TypedQuery<Number> query = entityManagerProvider.get().createNamedQuery(
        "HostRoleCommandEntity.findCountByCommandStatuses", Number.class);

    query.setParameter("statuses", statuses);
    return daoUtils.selectSingle(query);
  }

  @RequiresSession
  public List<HostRoleCommandEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), HostRoleCommandEntity.class);
  }

  /**
   * Gets requests that have tasks in any of the specified statuses.
   *
   * @param statuses
   * @param maxResults
   * @param ascOrder
   * @return
   */
  @RequiresSession
  public List<Long> getRequestsByTaskStatus(
      Collection<HostRoleStatus> statuses, int maxResults, boolean ascOrder) {
    String sortOrder = "ASC";
    if (!ascOrder) {
      sortOrder = "DESC";
    }

    String sql = MessageFormat.format(REQUESTS_BY_TASK_STATUS_SQL, sortOrder);
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(sql,
        Long.class);

    query.setParameter("taskStatuses", statuses);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<Long> getCompletedRequests(int maxResults, boolean ascOrder) {
    String sortOrder = "ASC";
    if (!ascOrder) {
      sortOrder = "DESC";
    }

    String sql = MessageFormat.format(COMPLETED_REQUESTS_SQL, sortOrder);
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(sql,
        Long.class);

    query.setParameter("notCompletedStatuses",
        HostRoleStatus.NOT_COMPLETED_STATUSES);

    return daoUtils.selectList(query);
  }

  /**
   * NB: You cannot rely on return value if batch write is enabled
   */
  @Transactional
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
  public void removeByHostId(Long hostId) {
    Collection<HostRoleCommandEntity> commands = this.findByHostId(hostId);
    for (HostRoleCommandEntity cmd : commands) {
      this.remove(cmd);
    }
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


  /**
   * Finds the counts of tasks for a request and groups them by stage id.
   * This allows for very efficient loading when there are a huge number of stages
   * and tasks to iterate (for example, during a Rolling Upgrade).
   * @param requestId the request id
   * @return the map of stage-to-summary objects
   */
  @RequiresSession
  public Map<Long, HostRoleCommandStatusSummaryDTO> findAggregateCounts(Long requestId) {

    TypedQuery<HostRoleCommandStatusSummaryDTO> query = entityManagerProvider.get().createQuery(
        SUMMARY_DTO, HostRoleCommandStatusSummaryDTO.class);

    query.setParameter("requestId", requestId);
    query.setParameter("aborted", HostRoleStatus.ABORTED);
    query.setParameter("completed", HostRoleStatus.COMPLETED);
    query.setParameter("failed", HostRoleStatus.FAILED);
    query.setParameter("holding", HostRoleStatus.HOLDING);
    query.setParameter("holding_failed", HostRoleStatus.HOLDING_FAILED);
    query.setParameter("holding_timedout", HostRoleStatus.HOLDING_TIMEDOUT);
    query.setParameter("in_progress", HostRoleStatus.IN_PROGRESS);
    query.setParameter("pending", HostRoleStatus.PENDING);
    query.setParameter("queued", HostRoleStatus.QUEUED);
    query.setParameter("timedout", HostRoleStatus.TIMEDOUT);

    Map<Long, HostRoleCommandStatusSummaryDTO> map = new HashMap<Long, HostRoleCommandStatusSummaryDTO>();

    for (HostRoleCommandStatusSummaryDTO dto : daoUtils.selectList(query)) {
      map.put(dto.getStageId(), dto);
    }

    return map;
  }

}
