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

import java.util.Date;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.cleanup.TimeBasedCleanupPolicy;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.RequestScheduleBatchRequestEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class RequestScheduleDAO implements Cleanable {

  private static final Logger LOG = LoggerFactory.getLogger(RequestScheduleDAO.class);

  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  private DaoUtils daoUtils;

  @Inject
  private Provider<Clusters> clusters;

  /**
   * Batch size to query the DB and use the results in an IN clause.
   */
  private static final int BATCH_SIZE = 999;

  @RequiresSession
  public RequestScheduleEntity findById(Long id) {
    return entityManagerProvider.get().find(RequestScheduleEntity.class, id);
  }

  @RequiresSession
  public List<RequestScheduleEntity> findByStatus(String status) {
    TypedQuery<RequestScheduleEntity> query = entityManagerProvider.get()
      .createNamedQuery("reqScheduleByStatus", RequestScheduleEntity.class);
    query.setParameter("status", status);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  @RequiresSession
  public List<RequestScheduleEntity> findAll() {
    TypedQuery<RequestScheduleEntity> query = entityManagerProvider.get()
      .createNamedQuery("allReqSchedules", RequestScheduleEntity.class);

    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  @Transactional
  public void create(RequestScheduleEntity requestScheduleEntity) {
    entityManagerProvider.get().persist(requestScheduleEntity);
  }

  @Transactional
  public RequestScheduleEntity merge(RequestScheduleEntity requestScheduleEntity) {
    return entityManagerProvider.get().merge(requestScheduleEntity);
  }

  @Transactional
  public void remove(RequestScheduleEntity requestScheduleEntity) {
    entityManagerProvider.get().remove(merge(requestScheduleEntity));
  }

  @Transactional
  public void removeByPK(Long id) {
    entityManagerProvider.get().remove(findById(id));
  }

  @Transactional
  public void refresh(RequestScheduleEntity requestScheduleEntity) {
    entityManagerProvider.get().refresh(requestScheduleEntity);
  }

  /**
   * Find all @RequestScheduleEntity with date before provided date.
   * @param clusterId cluster id
   * @param beforeDateMillis timestamp in millis
   * @return List<Integer> ids
   */
  private List<Integer> findAllScheduleIdsBeforeDate(Long clusterId, long beforeDateMillis) {

    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<Integer> requestScheduleQuery =
      entityManager.createNamedQuery("RequestScheduleEntity.findAllReqScheduleIdsInClusterBeforeDate", Integer.class);

    requestScheduleQuery.setParameter("clusterId", clusterId);
    requestScheduleQuery.setParameter("beforeDate", beforeDateMillis);

    return daoUtils.selectList(requestScheduleQuery);
  }

  /**
   * Deletes RequestSchedule and RequestScheduleBatchRequest records in relation with RequestSchedule entries older than the given date.
   *
   * @param clusterId        the identifier of the cluster the RequestSchedule belong to
   * @param beforeDateMillis the date in milliseconds the
   * @return a long representing the number of affected (deleted) records
   */
  @Transactional
  int cleanRequestSchedulesAndRequestScheduleBatchRequestsForClusterBeforeDate(Long clusterId, long beforeDateMillis) {
    LOG.info("Deleting RequestSchedule and RequestScheduleBatchRequest entities before date " + new Date(beforeDateMillis));
    EntityManager entityManager = entityManagerProvider.get();
    List<Integer> ids = findAllScheduleIdsBeforeDate(clusterId, beforeDateMillis);
    int affectedRows = 0;

    TypedQuery<RequestScheduleEntity> requestScheduleQuery =
      entityManager.createNamedQuery("RequestScheduleEntity.removeByScheduleIds", RequestScheduleEntity.class);
    TypedQuery<RequestScheduleBatchRequestEntity> requestScheduleBatchRequestQuery =
      entityManager.createNamedQuery("RequestScheduleBatchRequestEntity.removeByScheduleIds", RequestScheduleBatchRequestEntity.class);
    if (ids != null && !ids.isEmpty()) {
      for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
        int endIndex = Math.min((i + BATCH_SIZE), ids.size());
        List<Integer> idsSubList = ids.subList(i, endIndex);
        LOG.info("Deleting RequestScheduleBatchRequest entity batch with schedule ids: " +
          idsSubList.get(0) + " - " + idsSubList.get(idsSubList.size() - 1));
        requestScheduleBatchRequestQuery.setParameter("scheduleIds", idsSubList);
        affectedRows += requestScheduleBatchRequestQuery.executeUpdate();
        LOG.info("Deleting RequestSchedule entity batch with schedule ids: " +
          idsSubList.get(0) + " - " + idsSubList.get(idsSubList.size() - 1));
        requestScheduleQuery.setParameter("scheduleIds", idsSubList);
        affectedRows += requestScheduleQuery.executeUpdate();
      }
    }
    return affectedRows;
  }

  @Transactional
  @Override
  public long cleanup(TimeBasedCleanupPolicy policy) {
    long affectedRows = 0;
    try {
      Long clusterId = clusters.get().getCluster(policy.getClusterName()).getClusterId();
      affectedRows += cleanRequestSchedulesAndRequestScheduleBatchRequestsForClusterBeforeDate(clusterId,
          policy.getToDateInMillis());
    } catch (AmbariException e) {
      LOG.error("Error while looking up cluster with name: {}", policy.getClusterName(), e);
      throw new IllegalStateException(e);
    }

    return affectedRows;
  }
}
