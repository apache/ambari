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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.cleanup.TimeBasedCleanupPolicy;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.RequestOperationLevelEntity;
import org.apache.ambari.server.orm.entities.RequestResourceFilterEntity;
import org.apache.ambari.server.orm.entities.RoleSuccessCriteriaEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyHostTaskEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalTaskEntity;
import org.apache.ambari.server.orm.helpers.SQLConstants;
import org.apache.ambari.server.orm.helpers.SQLOperations;
import org.apache.ambari.server.state.Clusters;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class RequestDAO implements Cleanable {

  private static final Logger LOG = LoggerFactory.getLogger(RequestDAO.class);

  /**
   * SQL template to retrieve all request IDs, sorted by the ID.
   */
  private final static String REQUEST_IDS_SORTED_SQL = "SELECT request.requestId FROM RequestEntity request ORDER BY request.requestId {0}";

  /**
   * Requests by cluster.  Cannot be a NamedQuery due to the ORDER BY clause.
   */
  private final static String REQUESTS_WITH_CLUSTER_SQL =
      "SELECT request.requestId FROM RequestEntity request WHERE request.clusterId = %s ORDER BY request.requestId %s";
  /**
   * Requests by cluster.  Cannot be a NamedQuery due to the ORDER BY clause.
   */
  private final static String REQUESTS_WITH_NO_CLUSTER_SQL =
      "SELECT request.requestId FROM RequestEntity request WHERE request.clusterId = -1 OR request.clusterId IS NULL ORDER BY request.requestId %s";

  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  DaoUtils daoUtils;

  @Inject
  private Provider<Clusters> m_clusters;

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  private TopologyLogicalTaskDAO topologyLogicalTaskDAO;

  @Inject
  private TopologyHostTaskDAO topologyHostTaskDAO;

  @Inject
  private TopologyLogicalRequestDAO topologyLogicalRequestDAO;

  @Inject
  private TopologyRequestDAO topologyRequestDAO;

  @RequiresSession
  public RequestEntity findByPK(Long requestId) {
    return entityManagerProvider.get().find(RequestEntity.class, requestId);
  }

  @RequiresSession
  public List<RequestEntity> findByPks(Collection<Long> requestIds) {
    return findByPks(requestIds, false);
  }

  /**
   * Given a collection of request ids, load the corresponding entities
   * @param requestIds  the collection of request ids
   * @param refreshHint {@code true} to hint JPA that the list should be refreshed
   * @return the list entities. An empty list if the requestIds are not provided
   */
  @RequiresSession
  public List<RequestEntity> findByPks(Collection<Long> requestIds, boolean refreshHint) {
    TypedQuery<RequestEntity> query = entityManagerProvider.get().createQuery("SELECT request FROM RequestEntity request " +
        "WHERE request.requestId IN ?1", RequestEntity.class);

    // !!! https://bugs.eclipse.org/bugs/show_bug.cgi?id=398067
    // ensure that an associated entity with a JOIN is not stale
    if (refreshHint) {
      query.setHint(QueryHints.REFRESH, HintValues.TRUE);
    }

    List<RequestEntity> result = new ArrayList<>();
    SQLOperations.batch(requestIds, SQLConstants.IN_ARGUMENT_MAX_SIZE, (chunk, currentBatch, totalBatches, totalSize) -> {
      result.addAll(daoUtils.selectList(query, chunk));
      return 0;
    });

    return Lists.newArrayList(result);
  }

  @RequiresSession
  public List<RequestEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), RequestEntity.class);
  }

  @RequiresSession
  public List<Long> findAllRequestIds(int limit, boolean ascending) {
    String sort = "ASC";
    if (!ascending) {
      sort = "DESC";
    }

    String sql = MessageFormat.format(REQUEST_IDS_SORTED_SQL, sort);
    TypedQuery<Long> query = entityManagerProvider.get().createQuery(sql,
        Long.class);

    query.setMaxResults(limit);

    return daoUtils.selectList(query);
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
  public RequestEntity updateStatus(long requestId, HostRoleStatus status, HostRoleStatus displayStatus) {
    RequestEntity requestEntity = findByPK(requestId);
    requestEntity.setStatus(status);
    requestEntity.setDisplayStatus(displayStatus);
    return merge(requestEntity);
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

  /**
   * Retrieves from the database for a cluster, or specifically for non-cluster requests.
   * This method should be considered temporary until Request/Stage/Task cleanup is achieved.
   *
   * @param limit the max number to return
   * @param sortAscending {@code true} to sort by requestId ascending, {@code false} for descending
   * @param clusterId   the cluster to find, or {@code null} to search for requests without cluster
   */
  @RequiresSession
  public List<Long> findAllRequestIds(int limit, boolean sortAscending, Long clusterId) {

    final String sql;

    if (null == clusterId) {
      sql = String.format(REQUESTS_WITH_NO_CLUSTER_SQL, sortAscending ? "ASC" : "DESC");
    } else {
      sql = String.format(REQUESTS_WITH_CLUSTER_SQL, clusterId, sortAscending ? "ASC" : "DESC");
    }

    TypedQuery<Long> query = entityManagerProvider.get().createQuery(sql,
        Long.class);

    query.setMaxResults(limit);

    return daoUtils.selectList(query);
  }

  public static final class StageEntityPK {
    private Long requestId;
    private Long stageId;

    public StageEntityPK(Long requestId, Long stageId) {
      this.requestId = requestId;
      this.stageId = stageId;
    }

    public Long getStageId() {
      return stageId;
    }

    public void setStageId(Long stageId) {
      this.stageId = stageId;
    }

    public Long getRequestId() {
      return requestId;
    }

    public void setRequestId(Long requestId) {
      this.requestId = requestId;
    }
  }

  /**
   * Search for all request ids in Upgrade table
   * @return the list of request ids
   */
  private Set<Long> findAllRequestIdsFromUpgrade() {
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<Long> upgradeQuery =
            entityManager.createNamedQuery("UpgradeEntity.findAllRequestIds", Long.class);

    return Sets.newHashSet(daoUtils.selectList(upgradeQuery));
  }

  /**
   * Search for all request and stage ids in Request and Stage tables
   * @return the list of request/stage ids
   */
  public List<StageEntityPK> findRequestAndStageIdsInClusterBeforeDate(Long clusterId, long  beforeDateMillis) {
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<StageEntityPK> requestQuery =
            entityManager.createNamedQuery("RequestEntity.findRequestStageIdsInClusterBeforeDate", StageEntityPK.class);

    requestQuery.setParameter("clusterId", clusterId);
    requestQuery.setParameter("beforeDate", beforeDateMillis);

    return daoUtils.selectList(requestQuery);
  }

  /**
   * In this method we are removing entities using passed ids,
   * To prevent issues we are using batch request to remove limited
   * count of entities.
   * @param ids              list of ids that we are using to remove rows from table
   * @param paramName        name of parameter that we are using in sql query (taskIds, stageIds)
   * @param entityName       name of entity which we will remove
   * @param beforeDateMillis timestamp which was set by user (remove all entities that were created before),
   *                         we are using it only for logging
   * @param entityQuery      name of NamedQuery which we will use to remove needed entities
   * @param type             type of entity class which we will use for casting query result
   * @return                 rows count that were removed
   */
  @Transactional
  protected <T> int cleanTableByIds(Set<Long> ids, String paramName, String entityName, Long beforeDateMillis,
                                  String entityQuery, Class<T> type) {
    LOG.info(String.format("Purging %s entity records before date %s", entityName, new Date(beforeDateMillis)));

    if (CollectionUtils.isEmpty(ids)) {
      return 0;
    }

    final EntityManager entityManager = entityManagerProvider.get();
    final TypedQuery<T> query = entityManager.createNamedQuery(entityQuery, type);

    // Batch delete
    return SQLOperations.batch(ids, SQLConstants.IN_ARGUMENT_MAX_SIZE, (chunk, currentBatch, totalBatches, totalSize) -> {
      LOG.info(String.format("Purging %s entity, batch %s/%s.", entityName, currentBatch, totalBatches));
      query.setParameter(paramName, chunk);
      return query.executeUpdate();
    });
  }

  /**
   * In this method we are removing entities using passed few ids,
   * To prevent issues we are using batch request to remove limited
   * count of entities.
   * @param ids              list of ids pairs that we are using to remove rows from table
   * @param paramNames       list of two names of parameters that we are using in sql query (taskIds, stageIds)
   * @param entityName       name of entity which we will remove
   * @param beforeDateMillis timestamp which was set by user (remove all entities that were created before),
   *                         we are using it only for logging
   * @param entityQuery      name of NamedQuery which we will use to remove needed entities
   * @param type             type of entity class which we will use for casting query result
   * @return                 rows count that were removed
   */
  @Transactional
  protected <T> int cleanTableByStageEntityPK(List<StageEntityPK> ids, LinkedList<String> paramNames, String entityName, Long beforeDateMillis,
                                  String entityQuery, Class<T> type) {
    LOG.info(String.format("Purging %s entity records before date %s", entityName, new Date(beforeDateMillis)));

    if (CollectionUtils.isEmpty(ids)) {
      return 0;
    }

    final EntityManager entityManager = entityManagerProvider.get();
    final TypedQuery<T> query = entityManager.createNamedQuery(entityQuery, type);

    // Batch delete
    return SQLOperations.batch(ids, SQLConstants.IN_ARGUMENT_MAX_SIZE, (chunk, currentBatch, totalBatches, totalSize) -> {
      int affectedRows = 0;

      for (StageEntityPK requestIds : chunk) {
        query.setParameter(paramNames.get(0), requestIds.getStageId());
        query.setParameter(paramNames.get(1), requestIds.getRequestId());
        affectedRows += query.executeUpdate();
      }
      return affectedRows;
    });
  }

  @Transactional
  @Override
  public long cleanup(TimeBasedCleanupPolicy policy) {
    long affectedRows = 0;
    try {
      Long clusterId = m_clusters.get().getCluster(policy.getClusterName()).getClusterId();
      // find request and stage ids that were created before date populated by user.
      List<StageEntityPK> requestStageIds = findRequestAndStageIdsInClusterBeforeDate(clusterId, policy.getToDateInMillis());

      // find request ids from Upgrade table and exclude these ids from
      // request ids set that we already have. We don't want to make any changes for upgrade
      Set<Long> requestIdsFromUpgrade = findAllRequestIdsFromUpgrade();
      Iterator<StageEntityPK> requestStageIdsIterator =  requestStageIds.iterator();
      Set<Long> requestIds = new HashSet<>();
      while (requestStageIdsIterator.hasNext()) {
        StageEntityPK nextRequestStageIds = requestStageIdsIterator.next();
        if (requestIdsFromUpgrade.contains(nextRequestStageIds.getRequestId())) {
          requestStageIdsIterator.remove();
          continue;
        }
        requestIds.add(nextRequestStageIds.getRequestId());
      }

      // find task ids using request stage ids
      Set<Long> taskIds = hostRoleCommandDAO.findTaskIdsByRequestStageIds(requestStageIds);
      LinkedList<String> params = new LinkedList<>();
      params.add("stageId");
      params.add("requestId");

      // find host task ids, to find related host requests and also to remove needed host tasks
      Set<Long> hostTaskIds = topologyLogicalTaskDAO.findHostTaskIdsByPhysicalTaskIds(taskIds);

      // find host request ids by host task ids to remove later needed host requests
      Set<Long> hostRequestIds = topologyHostTaskDAO.findHostRequestIdsByHostTaskIds(hostTaskIds);
      Set<Long> topologyRequestIds = topologyLogicalRequestDAO.findRequestIdsByIds(hostRequestIds);

      //removing all entities one by one according to their relations using stage, task and request ids
      affectedRows += cleanTableByIds(taskIds, "taskIds", "ExecutionCommand", policy.getToDateInMillis(),
        "ExecutionCommandEntity.removeByTaskIds", ExecutionCommandEntity.class);
      affectedRows += cleanTableByIds(taskIds, "taskIds", "TopologyLogicalTask", policy.getToDateInMillis(),
        "TopologyLogicalTaskEntity.removeByPhysicalTaskIds", TopologyLogicalTaskEntity.class);
      affectedRows += cleanTableByIds(hostTaskIds, "hostTaskIds", "TopologyHostTask", policy.getToDateInMillis(),
        "TopologyHostTaskEntity.removeByTaskIds", TopologyHostTaskEntity.class);
      affectedRows += cleanTableByIds(hostRequestIds, "hostRequestIds", "TopologyHostRequest", policy.getToDateInMillis(),
        "TopologyHostRequestEntity.removeByIds", TopologyHostRequestEntity.class);
      for (Long topologyRequestId : topologyRequestIds) {
        topologyRequestDAO.removeByPK(topologyRequestId);
      }
      affectedRows += cleanTableByIds(taskIds, "taskIds", "HostRoleCommand", policy.getToDateInMillis(),
        "HostRoleCommandEntity.removeByTaskIds", HostRoleCommandEntity.class);
      affectedRows += cleanTableByStageEntityPK(requestStageIds, params, "RoleSuccessCriteria", policy.getToDateInMillis(),
        "RoleSuccessCriteriaEntity.removeByRequestStageIds", RoleSuccessCriteriaEntity.class);
      affectedRows += cleanTableByStageEntityPK(requestStageIds, params, "Stage", policy.getToDateInMillis(),
        "StageEntity.removeByRequestStageIds", StageEntity.class);
      affectedRows += cleanTableByIds(requestIds, "requestIds", "RequestResourceFilter", policy.getToDateInMillis(),
        "RequestResourceFilterEntity.removeByRequestIds", RequestResourceFilterEntity.class);
      affectedRows += cleanTableByIds(requestIds, "requestIds", "RequestOperationLevel", policy.getToDateInMillis(),
        "RequestOperationLevelEntity.removeByRequestIds", RequestOperationLevelEntity.class);
      affectedRows += cleanTableByIds(requestIds, "requestIds", "Request", policy.getToDateInMillis(),
        "RequestEntity.removeByRequestIds", RequestEntity.class);

    } catch (AmbariException e) {
      LOG.error("Error while looking up cluster with name: {}", policy.getClusterName(), e);
      throw new IllegalStateException(e);
    }

    return affectedRows;
  }
}
