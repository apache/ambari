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

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.ambari.server.api.query.JpaPredicateVisitor;
import org.apache.ambari.server.api.query.JpaSortBuilder;
import org.apache.ambari.server.controller.AlertCurrentRequest;
import org.apache.ambari.server.controller.AlertHistoryRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.events.AggregateAlertRecalculateEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity_;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity_;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.alert.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link AlertsDAO} class manages the {@link AlertHistoryEntity} and
 * {@link AlertCurrentEntity} instances. Each {@link AlertHistoryEntity} is
 * known as an "alert" that has been triggered and received.
 */
@Singleton
public class AlertsDAO {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertsDAO.class);

  /**
   * A template of JPQL for getting the number of hosts in various states.
   */
  private static final String HOST_COUNT_SQL_TEMPLATE = "SELECT MAX("
      + "CASE "
      + "  WHEN history.alertState = :criticalState AND alert.maintenanceState = :maintenanceStateOff THEN 3 "
      + "  WHEN history.alertState = :warningState AND alert.maintenanceState = :maintenanceStateOff THEN 2 "
      + "  WHEN history.alertState = :unknownState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END) "
      + "FROM AlertCurrentEntity alert JOIN alert.alertHistory history "
      + "WHERE history.clusterId = :clusterId AND history.hostName IS NOT NULL GROUP BY history.hostName";

  private static final String ALERT_COUNT_SQL_TEMPLATE = "SELECT NEW %s("
      + "SUM(CASE WHEN history.alertState = :okState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN history.alertState = :warningState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN history.alertState = :criticalState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN history.alertState = :unknownState AND alert.maintenanceState = :maintenanceStateOff THEN 1 ELSE 0 END), "
      + "SUM(CASE WHEN alert.maintenanceState != :maintenanceStateOff THEN 1 ELSE 0 END)) "
      + "FROM AlertCurrentEntity alert JOIN alert.alertHistory history WHERE history.clusterId = :clusterId";

  /**
   * JPA entity manager
   */
  @Inject
  private Provider<EntityManager> m_entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils m_daoUtils;

  /**
   * Publishes alert events when particular DAO methods are called.
   */
  @Inject
  private AlertEventPublisher m_alertEventPublisher;

  /**
   * Used to lookup clusters.
   */
  @Inject
  private Provider<Clusters> m_clusters;

  /**
   * Gets an alert with the specified ID.
   *
   * @param alertId
   *          the ID of the alert to retrieve.
   * @return the alert or {@code null} if none exists.
   */
  @RequiresSession
  public AlertHistoryEntity findById(long alertId) {
    return m_entityManagerProvider.get().find(AlertHistoryEntity.class, alertId);
  }

  /**
   * Gets all alerts stored in the database across all clusters.
   *
   * @return all alerts or an empty list if none exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll() {
    TypedQuery<AlertHistoryEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAll", AlertHistoryEntity.class);

    return m_daoUtils.selectList(query);
  }

  /**
   * Gets all alerts stored in the database for the given cluster.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @return all alerts in the specified cluster or an empty list if none exist
   *         (never {@code null}).
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll(long clusterId) {
    TypedQuery<AlertHistoryEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAllInCluster", AlertHistoryEntity.class);

    query.setParameter("clusterId", clusterId);

    return m_daoUtils.selectList(query);
  }

  /**
   * Gets all alerts stored in the database for the given cluster that have one
   * of the specified alert states.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param alertStates
   *          the states to match for the retrieved alerts (not {@code null}).
   * @return the alerts matching the specified states and cluster, or an empty
   *         list if none.
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll(long clusterId,
      List<AlertState> alertStates) {
    if (null == alertStates || alertStates.size() == 0) {
      return Collections.emptyList();
    }

    TypedQuery<AlertHistoryEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertHistoryEntity.findAllInClusterWithState",
        AlertHistoryEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("alertStates", alertStates);

    return m_daoUtils.selectList(query);
  }

  /**
   * Gets all alerts stored in the database for the given cluster and that fall
   * withing the specified date range. Dates are expected to be in milliseconds
   * since the epoch, normalized to UTC time.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param startDate
   *          the date that the earliest entry must occur after, normalized to
   *          UTC, or {@code null} for all entries that occur before the given
   *          end date.
   * @param endDate
   *          the date that the latest entry must occur before, normalized to
   *          UTC, or {@code null} for all entries that occur after the given
   *          start date.
   * @return the alerts matching the specified date range.
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll(long clusterId, Date startDate,
      Date endDate) {
    if (null == startDate && null == endDate) {
      return Collections.emptyList();
    }

    TypedQuery<AlertHistoryEntity> query = null;

    if (null != startDate && null != endDate) {
      if (startDate.after(endDate)) {
        return Collections.emptyList();
      }

      query = m_entityManagerProvider.get().createNamedQuery(
          "AlertHistoryEntity.findAllInClusterBetweenDates",
          AlertHistoryEntity.class);

      query.setParameter("clusterId", clusterId);
      query.setParameter("startDate", startDate.getTime());
      query.setParameter("endDate", endDate.getTime());
    } else if (null != startDate) {
      query = m_entityManagerProvider.get().createNamedQuery(
          "AlertHistoryEntity.findAllInClusterAfterDate",
          AlertHistoryEntity.class);

      query.setParameter("clusterId", clusterId);
      query.setParameter("afterDate", startDate.getTime());
    } else if (null != endDate) {
      query = m_entityManagerProvider.get().createNamedQuery(
          "AlertHistoryEntity.findAllInClusterBeforeDate",
          AlertHistoryEntity.class);

      query.setParameter("clusterId", clusterId);
      query.setParameter("beforeDate", endDate.getTime());
    }

    if (null == query) {
      return Collections.emptyList();
    }

    return m_daoUtils.selectList(query);
  }

  /**
   * Finds all {@link AlertHistoryEntity} that match the provided
   * {@link AlertHistoryRequest}. This method will make JPA do the heavy lifting
   * of providing a slice of the result set.
   *
   * @param request
   * @return
   */
  @RequiresSession
  public List<AlertHistoryEntity> findAll(AlertHistoryRequest request) {
    EntityManager entityManager = m_entityManagerProvider.get();

    // convert the Ambari predicate into a JPA predicate
    HistoryPredicateVisitor visitor = new HistoryPredicateVisitor();
    PredicateHelper.visit(request.Predicate, visitor);

    CriteriaQuery<AlertHistoryEntity> query = visitor.getCriteriaQuery();
    javax.persistence.criteria.Predicate jpaPredicate = visitor.getJpaPredicate();

    if (null != jpaPredicate) {
      query.where(jpaPredicate);
    }

    // sorting
    JpaSortBuilder<AlertHistoryEntity> sortBuilder = new JpaSortBuilder<AlertHistoryEntity>();
    List<Order> sortOrders = sortBuilder.buildSortOrders(request.Sort, visitor);
    query.orderBy(sortOrders);

    // pagination
    TypedQuery<AlertHistoryEntity> typedQuery = entityManager.createQuery(query);
    if (null != request.Pagination) {
      typedQuery.setFirstResult(request.Pagination.getOffset());
      typedQuery.setMaxResults(request.Pagination.getPageSize());
    }

    return m_daoUtils.selectList(typedQuery);
  }

  /**
   * Finds all {@link AlertCurrentEntity} that match the provided
   * {@link AlertCurrentRequest}. This method will make JPA do the heavy lifting
   * of providing a slice of the result set.
   *
   * @param request
   * @return
   */
  @Transactional
  public List<AlertCurrentEntity> findAll(AlertCurrentRequest request) {
    EntityManager entityManager = m_entityManagerProvider.get();

    // convert the Ambari predicate into a JPA predicate
    CurrentPredicateVisitor visitor = new CurrentPredicateVisitor();
    PredicateHelper.visit(request.Predicate, visitor);

    CriteriaQuery<AlertCurrentEntity> query = visitor.getCriteriaQuery();
    javax.persistence.criteria.Predicate jpaPredicate = visitor.getJpaPredicate();

    if (null != jpaPredicate) {
      query.where(jpaPredicate);
    }

    // sorting
    JpaSortBuilder<AlertCurrentEntity> sortBuilder = new JpaSortBuilder<AlertCurrentEntity>();
    List<Order> sortOrders = sortBuilder.buildSortOrders(request.Sort, visitor);
    query.orderBy(sortOrders);

    // pagination
    TypedQuery<AlertCurrentEntity> typedQuery = entityManager.createQuery(query);
    if( null != request.Pagination ){
      // prevent JPA errors when -1 is passed in by accident
      int offset = request.Pagination.getOffset();
      if (offset < 0) {
        offset = 0;
      }

      typedQuery.setFirstResult(offset);
      typedQuery.setMaxResults(request.Pagination.getPageSize());
    }

    return m_daoUtils.selectList(typedQuery);
  }

  /**
   * Gets the total count of all {@link AlertHistoryEntity} rows that match the
   * specified {@link Predicate}.
   *
   * @param predicate
   *          the predicate to apply, or {@code null} for none.
   * @return the total count of rows that would be returned in a result set.
   */
  public int getCount(Predicate predicate) {
    return 0;
  }

  /**
   * Gets the current alerts.
   *
   * @return the current alerts or an empty list if none exist (never
   *         {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrent() {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findAll", AlertCurrentEntity.class);

    return m_daoUtils.selectList(query);
  }

  /**
   * Gets a current alert with the specified ID.
   *
   * @param alertId
   *          the ID of the alert to retrieve.
   * @return the alert or {@code null} if none exists.
   */
  @RequiresSession
  public AlertCurrentEntity findCurrentById(long alertId) {
    return m_entityManagerProvider.get().find(AlertCurrentEntity.class, alertId);
  }

  /**
   * Gets the current alerts for the specified definition ID.
   *
   * @param definitionId
   *          the ID of the definition to retrieve current alerts for.
   * @return the current alerts for the definition or an empty list if none
   *         exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrentByDefinitionId(long definitionId) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findByDefinitionId", AlertCurrentEntity.class);

    query.setParameter("definitionId", Long.valueOf(definitionId));

    return m_daoUtils.selectList(query);
  }

  /**
   * Gets the current alerts for a given cluster.
   *
   * @return the current alerts for the given cluster or an empty list if none
   *         exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrentByCluster(long clusterId) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findByCluster", AlertCurrentEntity.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));

    return m_daoUtils.selectList(query);
  }

  /**
   * Retrieves the summary information for a particular scope. The result is a
   * DTO since the columns are aggregated and don't fit to an entity.
   *
   * @param clusterId
   *          the cluster id
   * @param serviceName
   *          the service name. Use {@code null} to not filter on service.
   * @param hostName
   *          the host name. Use {@code null} to not filter on host.
   * @return the summary DTO
   */
  @RequiresSession
  public AlertSummaryDTO findCurrentCounts(long clusterId, String serviceName, String hostName) {
    String sql = String.format(ALERT_COUNT_SQL_TEMPLATE,
        AlertSummaryDTO.class.getName());

    StringBuilder sb = new StringBuilder(sql);

    if (null != serviceName) {
      sb.append(" AND history.serviceName = :serviceName");
    }

    if (null != hostName) {
      sb.append(" AND history.hostName = :hostName");
    }

    TypedQuery<AlertSummaryDTO> query = m_entityManagerProvider.get().createQuery(
        sb.toString(), AlertSummaryDTO.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("okState", AlertState.OK);
    query.setParameter("warningState", AlertState.WARNING);
    query.setParameter("criticalState", AlertState.CRITICAL);
    query.setParameter("unknownState", AlertState.UNKNOWN);
    query.setParameter("maintenanceStateOff", MaintenanceState.OFF);

    if (null != serviceName) {
      query.setParameter("serviceName", serviceName);
    }

    if (null != hostName) {
      query.setParameter("hostName", hostName);
    }

    return m_daoUtils.selectSingle(query);
  }

  /**
   * Retrieve the summary alert information for all hosts. This is different
   * from {@link #findCurrentCounts(long, String, String)} since this will
   * return only alerts related to hosts and those values will be the total
   * number of hosts affected, not the total number of alerts.
   *
   * @param clusterId
   *          the cluster id
   * @return the summary DTO for host alerts.
   */
  @RequiresSession
  public AlertHostSummaryDTO findCurrentHostCounts(long clusterId) {
    // use Number here since some databases like MySQL return Long and some
    // return Integer and we don't want a class cast exception
    TypedQuery<Number> query = m_entityManagerProvider.get().createQuery(
        HOST_COUNT_SQL_TEMPLATE, Number.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("criticalState", AlertState.CRITICAL);
    query.setParameter("warningState", AlertState.WARNING);
    query.setParameter("unknownState", AlertState.UNKNOWN);
    query.setParameter("maintenanceStateOff", MaintenanceState.OFF);

    int okCount = 0;
    int warningCount = 0;
    int criticalCount = 0;
    int unknownCount = 0;

    List<Number> hostStateValues = m_daoUtils.selectList(query);
    for (Number hostStateValue : hostStateValues) {
      if (null == hostStateValue) {
        continue;
      }

      int integerValue = hostStateValue.intValue();

      switch (integerValue) {
        case 0:
          okCount++;
          break;
        case 1:
          unknownCount++;
          break;
        case 2:
          warningCount++;
          break;
        case 3:
          criticalCount++;
          break;
      }
    }

    AlertHostSummaryDTO hostSummary = new AlertHostSummaryDTO(okCount,
        unknownCount, warningCount, criticalCount);

    return hostSummary;
  }

  /**
   * Gets the current alerts for a given service.
   *
   * @return the current alerts for the given service or an empty list if none
   *         exist (never {@code null}).
   */
  @RequiresSession
  public List<AlertCurrentEntity> findCurrentByService(long clusterId,
      String serviceName) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findByService", AlertCurrentEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);
    query.setParameter("inlist", EnumSet.of(Scope.ANY, Scope.SERVICE));

    return m_daoUtils.selectList(query);
  }

  @RequiresSession
  public AlertCurrentEntity findCurrentByHostAndName(long clusterId, String hostName,
      String alertName) {

    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findByHostAndName", AlertCurrentEntity.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("hostName", hostName);
    query.setParameter("definitionName", alertName);

    return m_daoUtils.selectOne(query);
  }

  /**
   * Removes alert history and current alerts for the specified alert defintiion
   * ID. This will invoke {@link EntityManager#clear()} when completed since the
   * JPQL statement will remove entries without going through the EM.
   *
   * @param definitionId
   *          the ID of the definition to remove.
   */
  @Transactional
  public void removeByDefinitionId(long definitionId) {
    EntityManager entityManager = m_entityManagerProvider.get();
    TypedQuery<AlertCurrentEntity> currentQuery = entityManager.createNamedQuery(
        "AlertCurrentEntity.removeByDefinitionId", AlertCurrentEntity.class);

    currentQuery.setParameter("definitionId", definitionId);
    currentQuery.executeUpdate();

    TypedQuery<AlertHistoryEntity> historyQuery = entityManager.createNamedQuery(
        "AlertHistoryEntity.removeByDefinitionId", AlertHistoryEntity.class);

    historyQuery.setParameter("definitionId", definitionId);
    historyQuery.executeUpdate();

    entityManager.clear();
  }

  /**
   * Remove a current alert whose history entry matches the specfied ID.
   *
   * @param   historyId the ID of the history entry.
   * @return  the number of alerts removed.
   */
  @Transactional
  public int removeCurrentByHistoryId(long historyId) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.removeByHistoryId", AlertCurrentEntity.class);

    query.setParameter("historyId", historyId);
    return query.executeUpdate();
  }

  /**
   * Remove all current alerts that are disabled.
   *
   * @return the number of alerts removed.
   */
  @Transactional
  public int removeCurrentDisabledAlerts() {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.removeDisabled", AlertCurrentEntity.class);

    return query.executeUpdate();
  }

  /**
   * Remove the current alert that matches the given service. This is used in
   * cases where the service was removed from the cluster.
   * <p>
   * This method will also fire an {@link AggregateAlertRecalculateEvent} in
   * order to recalculate all aggregates.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param serviceName
   *          the name of the service that the current alerts are being removed
   *          for (not {@code null}).
   * @return the number of alerts removed.
   */
  @Transactional
  public int removeCurrentByService(long clusterId, String serviceName) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.removeByService", AlertCurrentEntity.class);

    query.setParameter("serviceName", serviceName);

    int removedItems = query.executeUpdate();

    // publish the event to recalculate aggregates
    m_alertEventPublisher.publish(new AggregateAlertRecalculateEvent(clusterId));
    return removedItems;
  }

  /**
   * Remove the current alert that matches the given host. This is used in cases
   * where the host was removed from the cluster.
   * <p>
   * This method will also fire an {@link AggregateAlertRecalculateEvent} in
   * order to recalculate all aggregates.
   *
   * @param hostName
   *          the name of the host that the current alerts are being removed for
   *          (not {@code null}).
   * @return the number of alerts removed.
   */
  @Transactional
  public int removeCurrentByHost(String hostName) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.removeByHost", AlertCurrentEntity.class);

    query.setParameter("hostName", hostName);
    int removedItems = query.executeUpdate();

    // publish the event to recalculate aggregates for every cluster since a host could potentially have several clusters
    try {
      Map<String, Cluster> clusters = m_clusters.get().getClusters();
      for (Map.Entry<String, Cluster> entry : clusters.entrySet()) {
        m_alertEventPublisher.publish(new AggregateAlertRecalculateEvent(
            entry.getValue().getClusterId()));
      }

    } catch (Exception ambariException) {
      LOG.warn("Unable to recalcuate aggregate alerts after removing host {}", hostName);
    }

    return removedItems;
  }

  /**
   * Remove the current alert that matches the given service, component and
   * host. This is used in cases where the component was removed from the host.
   * <p>
   * This method will also fire an {@link AggregateAlertRecalculateEvent} in
   * order to recalculate all aggregates.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param serviceName
   *          the name of the service that the current alerts are being removed
   *          for (not {@code null}).
   * @param componentName
   *          the name of the component that the current alerts are being
   *          removed for (not {@code null}).
   * @param hostName
   *          the name of the host that the current alerts are being removed for
   *          (not {@code null}).
   * @return the number of alerts removed.
   */
  @Transactional
  public int removeCurrentByServiceComponentHost(long clusterId, String serviceName,
      String componentName, String hostName) {

    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.removeByHostComponent", AlertCurrentEntity.class);

    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);
    query.setParameter("hostName", hostName);

    int removedItems = query.executeUpdate();

    // publish the event to recalculate aggregates
    m_alertEventPublisher.publish(new AggregateAlertRecalculateEvent(clusterId));

    return removedItems;
  }

  /**
   * Persists a new alert.
   *
   * @param alert
   *          the alert to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertHistoryEntity alert) {
    m_entityManagerProvider.get().persist(alert);
  }

  /**
   * Refresh the state of the alert from the database.
   *
   * @param alert
   *          the alert to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertHistoryEntity alert) {
    m_entityManagerProvider.get().refresh(alert);
  }

  /**
   * Merge the speicified alert with the existing alert in the database.
   *
   * @param alert
   *          the alert to merge (not {@code null}).
   * @return the updated alert with merged content (never {@code null}).
   */
  @Transactional
  public AlertHistoryEntity merge(AlertHistoryEntity alert) {
    return m_entityManagerProvider.get().merge(alert);
  }

  /**
   * Removes the specified alert from the database.
   *
   * @param alert
   *          the alert to remove.
   */
  @Transactional
  public void remove(AlertHistoryEntity alert) {
    alert = merge(alert);

    removeCurrentByHistoryId(alert.getAlertId());
    m_entityManagerProvider.get().remove(alert);
  }

  /**
   * Persists a new current alert.
   *
   * @param alert
   *          the current alert to persist (not {@code null}).
   */
  @Transactional
  public void create(AlertCurrentEntity alert) {
    m_entityManagerProvider.get().persist(alert);
  }

  /**
   * Refresh the state of the current alert from the database.
   *
   * @param alert
   *          the current alert to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(AlertCurrentEntity alert) {
    m_entityManagerProvider.get().refresh(alert);
  }

  /**
   * Merge the speicified current alert with the existing alert in the database.
   *
   * @param alert
   *          the current alert to merge (not {@code null}).
   * @return the updated current alert with merged content (never {@code null}).
   */
  @Transactional
  public AlertCurrentEntity merge(AlertCurrentEntity alert) {
    return m_entityManagerProvider.get().merge(alert);
  }

  /**
   * Merge the specified current alert with the history and
   * the existing alert in the database in a single transaction.
   *
   * @param alert
   *          the current alert to merge (not {@code null}).
   * @param history
   *          the history to set to alert (not {@code null}).
   * @return the updated current alert with merged content (never @code null}).
   */
  @Transactional
  public AlertCurrentEntity mergeAlertCurrentWithAlertHistory(
      AlertCurrentEntity alert, AlertHistoryEntity history) {

    // manually create the new history entity since we are merging into
    // an existing current entity
    create(history);
    alert.setAlertHistory(history);
    return merge(alert);
  }

  /**
   * Removes the specified current alert from the database.
   *
   * @param alert
   *          the current alert to remove.
   */
  @Transactional
  public void remove(AlertCurrentEntity alert) {
    m_entityManagerProvider.get().remove(merge(alert));
  }

  /**
   * Finds the aggregate counts for an alert name, across all hosts.
   * @param clusterId the cluster id
   * @param alertName the name of the alert to find the aggregate
   * @return the summary data
   */
  @RequiresSession
  public AlertSummaryDTO findAggregateCounts(long clusterId, String alertName) {
    String sql = String.format(ALERT_COUNT_SQL_TEMPLATE,
        AlertSummaryDTO.class.getName());

    StringBuilder buffer = new StringBuilder(sql);
    buffer.append(" AND history.alertDefinition.definitionName = :definitionName");

    TypedQuery<AlertSummaryDTO> query = m_entityManagerProvider.get().createQuery(
        buffer.toString(), AlertSummaryDTO.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("okState", AlertState.OK);
    query.setParameter("warningState", AlertState.WARNING);
    query.setParameter("criticalState", AlertState.CRITICAL);
    query.setParameter("unknownState", AlertState.UNKNOWN);
    query.setParameter("maintenanceStateOff", MaintenanceState.OFF);
    query.setParameter("definitionName", alertName);

    return m_daoUtils.selectSingle(query);
  }

  /**
   * Locate the current alert for the provided service and alert name, but when
   * host is not set ({@code IS NULL}).
   * @param clusterId the cluster id
   * @param alertName the name of the alert
   * @return the current record, or {@code null} if not found
   */
  @RequiresSession
  public AlertCurrentEntity findCurrentByNameNoHost(long clusterId, String alertName) {
    TypedQuery<AlertCurrentEntity> query = m_entityManagerProvider.get().createNamedQuery(
        "AlertCurrentEntity.findByNameAndNoHost", AlertCurrentEntity.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));
    query.setParameter("definitionName", alertName);

    return m_daoUtils.selectOne(query);
  }

  /**
   * The {@link HistoryPredicateVisitor} is used to convert an Ambari
   * {@link Predicate} into a JPA {@link javax.persistence.criteria.Predicate}.
   */
  private final class HistoryPredicateVisitor extends
      JpaPredicateVisitor<AlertHistoryEntity> {

    /**
     * Constructor.
     *
     */
    public HistoryPredicateVisitor() {
      super(m_entityManagerProvider.get(), AlertHistoryEntity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<AlertHistoryEntity> getEntityClass() {
      return AlertHistoryEntity.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SingularAttribute<?, ?>> getPredicateMapping(
        String propertyId) {
      return AlertHistoryEntity_.getPredicateMapping().get(propertyId);
    }
  }

  /**
   * The {@link CurrentPredicateVisitor} is used to convert an Ambari
   * {@link Predicate} into a JPA {@link javax.persistence.criteria.Predicate}.
   */
  private final class CurrentPredicateVisitor extends
      JpaPredicateVisitor<AlertCurrentEntity> {

    /**
     * Constructor.
     *
     */
    public CurrentPredicateVisitor() {
      super(m_entityManagerProvider.get(), AlertCurrentEntity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<AlertCurrentEntity> getEntityClass() {
      return AlertCurrentEntity.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SingularAttribute<?, ?>> getPredicateMapping(
        String propertyId) {
      return AlertCurrentEntity_.getPredicateMapping().get(propertyId);
    }
  }
}
