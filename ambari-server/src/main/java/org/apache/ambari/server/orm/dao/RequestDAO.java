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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.RequestResourceFilterEntity;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class RequestDAO {
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
    if (null == requestIds || 0 == requestIds.size()) {
      return Collections.emptyList();
    }

    TypedQuery<RequestEntity> query = entityManagerProvider.get().createQuery("SELECT request FROM RequestEntity request " +
        "WHERE request.requestId IN ?1", RequestEntity.class);

    // !!! https://bugs.eclipse.org/bugs/show_bug.cgi?id=398067
    // ensure that an associated entity with a JOIN is not stale
    if (refreshHint) {
      query.setHint(QueryHints.REFRESH, HintValues.TRUE);
    }

    return daoUtils.selectList(query, requestIds);
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
   * @param maxResults  the max number to return
   * @param ascOrder    {@code true} to sort by requestId ascending, {@code false} for descending
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
}
