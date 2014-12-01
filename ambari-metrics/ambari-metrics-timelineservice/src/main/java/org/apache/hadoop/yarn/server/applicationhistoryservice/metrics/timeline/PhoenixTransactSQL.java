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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulate all metrics related SQL queries.
 */
public class PhoenixTransactSQL {

  static final Log LOG = LogFactory.getLog(PhoenixTransactSQL.class);
  // TODO: Configurable TTL values
  /**
   * Create table to store individual metric records.
   */
  public static final String CREATE_METRICS_TABLE_SQL = "CREATE TABLE IF NOT " +
    "EXISTS METRIC_RECORD (METRIC_NAME VARCHAR, " +
    "HOSTNAME VARCHAR, " +
    "SERVER_TIME UNSIGNED_LONG NOT NULL, " +
    "APP_ID VARCHAR, " +
    "INSTANCE_ID VARCHAR, " +
    "START_TIME UNSIGNED_LONG, " +
    "UNITS CHAR(20), " +
    "METRIC_SUM DOUBLE, " +
    "METRIC_COUNT UNSIGNED_INT, " +
    "METRIC_MAX DOUBLE, " +
    "METRIC_MIN DOUBLE, " +
    "METRICS VARCHAR CONSTRAINT pk " +
    "PRIMARY KEY (METRIC_NAME, HOSTNAME, SERVER_TIME, APP_ID, " +
    "INSTANCE_ID)) DATA_BLOCK_ENCODING='%s', IMMUTABLE_ROWS=true, " +
    "TTL=%s, COMPRESSION='%s'";

  public static final String CREATE_METRICS_AGGREGATE_HOURLY_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS METRIC_RECORD_HOURLY " +
      "(METRIC_NAME VARCHAR, " +
      "HOSTNAME VARCHAR, " +
      "APP_ID VARCHAR, " +
      "INSTANCE_ID VARCHAR, " +
      "SERVER_TIME UNSIGNED_LONG NOT NULL, " +
      "UNITS CHAR(20), " +
      "METRIC_SUM DOUBLE," +
      "METRIC_COUNT UNSIGNED_INT, " +
      "METRIC_MAX DOUBLE," +
      "METRIC_MIN DOUBLE CONSTRAINT pk " +
      "PRIMARY KEY (METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, " +
      "SERVER_TIME)) DATA_BLOCK_ENCODING='%s', IMMUTABLE_ROWS=true, " +
      "TTL=%s, COMPRESSION='%s'";

  public static final String CREATE_METRICS_AGGREGATE_MINUTE_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS METRIC_RECORD_MINUTE " +
      "(METRIC_NAME VARCHAR, " +
      "HOSTNAME VARCHAR, " +
      "APP_ID VARCHAR, " +
      "INSTANCE_ID VARCHAR, " +
      "SERVER_TIME UNSIGNED_LONG NOT NULL, " +
      "UNITS CHAR(20), " +
      "METRIC_SUM DOUBLE," +
      "METRIC_COUNT UNSIGNED_INT, " +
      "METRIC_MAX DOUBLE," +
      "METRIC_MIN DOUBLE CONSTRAINT pk " +
      "PRIMARY KEY (METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, " +
      "SERVER_TIME)) DATA_BLOCK_ENCODING='%s', IMMUTABLE_ROWS=true, TTL=%s," +
      " COMPRESSION='%s'";

  public static final String CREATE_METRICS_CLUSTER_AGGREGATE_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS METRIC_AGGREGATE " +
      "(METRIC_NAME VARCHAR, " +
      "APP_ID VARCHAR, " +
      "INSTANCE_ID VARCHAR, " +
      "SERVER_TIME UNSIGNED_LONG NOT NULL, " +
      "UNITS CHAR(20), " +
      "METRIC_SUM DOUBLE, " +
      "HOSTS_COUNT UNSIGNED_INT, " +
      "METRIC_MAX DOUBLE, " +
      "METRIC_MIN DOUBLE " +
      "CONSTRAINT pk PRIMARY KEY (METRIC_NAME, APP_ID, INSTANCE_ID, " +
      "SERVER_TIME)) DATA_BLOCK_ENCODING='%s', IMMUTABLE_ROWS=true, " +
      "TTL=%s, COMPRESSION='%s'";

  public static final String CREATE_METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS METRIC_AGGREGATE_HOURLY " +
      "(METRIC_NAME VARCHAR, " +
      "APP_ID VARCHAR, " +
      "INSTANCE_ID VARCHAR, " +
      "SERVER_TIME UNSIGNED_LONG NOT NULL, " +
      "UNITS CHAR(20), " +
      "METRIC_SUM DOUBLE, " +
      "METRIC_COUNT UNSIGNED_INT, " +
      "METRIC_MAX DOUBLE, " +
      "METRIC_MIN DOUBLE " +
      "CONSTRAINT pk PRIMARY KEY (METRIC_NAME, APP_ID, INSTANCE_ID, " +
      "SERVER_TIME)) DATA_BLOCK_ENCODING='%s', IMMUTABLE_ROWS=true, " +
      "TTL=%s, COMPRESSION='%s'";

  /**
   * Insert into metric records table.
   */
  public static final String UPSERT_METRICS_SQL = "UPSERT INTO %s " +
    "(METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, SERVER_TIME, START_TIME, " +
    "UNITS, " +
    "METRIC_SUM, " +
    "METRIC_MAX, " +
    "METRIC_MIN, " +
    "METRIC_COUNT, " +
    "METRICS) VALUES " +
    "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  public static final String UPSERT_CLUSTER_AGGREGATE_SQL = "UPSERT INTO " +
    "METRIC_AGGREGATE (METRIC_NAME, APP_ID, INSTANCE_ID, SERVER_TIME, " +
    "UNITS, " +
    "METRIC_SUM, " +
    "HOSTS_COUNT, " +
    "METRIC_MAX, " +
    "METRIC_MIN) " +
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  public static final String UPSERT_CLUSTER_AGGREGATE_TIME_SQL = "UPSERT INTO" +
    " %s (METRIC_NAME, APP_ID, INSTANCE_ID, SERVER_TIME, " +
    "UNITS, " +
    "METRIC_SUM, " +
    "METRIC_COUNT, " +
    "METRIC_MAX, " +
    "METRIC_MIN) " +
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";


  public static final String UPSERT_AGGREGATE_RECORD_SQL = "UPSERT INTO " +
    "%s (METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, " +
    "SERVER_TIME, " +
    "UNITS, " +
    "METRIC_SUM, " +
    "METRIC_MAX, " +
    "METRIC_MIN," +
    "METRIC_COUNT) " +
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  /**
   * Retrieve a set of rows from metrics records table.
   */
  public static final String GET_METRIC_SQL = "SELECT %s METRIC_NAME, " +
    "HOSTNAME, APP_ID, INSTANCE_ID, SERVER_TIME, START_TIME, UNITS, " +
    "METRIC_SUM, " +
    "METRIC_MAX, " +
    "METRIC_MIN, " +
    "METRIC_COUNT, " +
    "METRICS " +
    "FROM %s";

  public static final String GET_METRIC_AGGREGATE_ONLY_SQL = "SELECT %s " +
    "METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, SERVER_TIME, " +
    "UNITS, " +
    "METRIC_SUM, " +
    "METRIC_MAX, " +
    "METRIC_MIN, " +
    "METRIC_COUNT " +
    "FROM %s";

  public static final String GET_CLUSTER_AGGREGATE_SQL = "SELECT %s " +
    "METRIC_NAME, APP_ID, " +
    "INSTANCE_ID, SERVER_TIME, " +
    "UNITS, " +
    "METRIC_SUM, " +
    "HOSTS_COUNT, " +
    "METRIC_MAX, " +
    "METRIC_MIN " +
    "FROM METRIC_AGGREGATE";

  public static final String METRICS_RECORD_TABLE_NAME = "METRIC_RECORD";
  public static final String METRICS_AGGREGATE_MINUTE_TABLE_NAME =
    "METRIC_RECORD_MINUTE";
  public static final String METRICS_AGGREGATE_HOURLY_TABLE_NAME =
    "METRIC_RECORD_HOURLY";
  public static final String METRICS_CLUSTER_AGGREGATE_TABLE_NAME =
    "METRIC_AGGREGATE";
  public static final String METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME =
    "METRIC_AGGREGATE_HOURLY";
  public static final String DEFAULT_TABLE_COMPRESSION = "SNAPPY";
  public static final String DEFAULT_ENCODING = "FAST_DIFF";
  public static final long NATIVE_TIME_RANGE_DELTA = 120000; // 2 minutes

  /** Filter to optimize HBase scan by using file timestamps. This prevents
   * a full table scan of metric records.
   * @return Phoenix Hint String
   */
  public static String getNaiveTimeRangeHint(Long startTime, Long delta) {
    return String.format("/*+ NATIVE_TIME_RANGE(%s) */", (startTime - delta));
  }

  public static PreparedStatement prepareGetMetricsSqlStmt(
    Connection connection, Condition condition) throws SQLException {

    if (condition.isEmpty()) {
      throw new IllegalArgumentException("Condition is empty.");
    }
    String stmtStr;
    if (condition.getStatement() != null) {
      stmtStr = condition.getStatement();
    } else {
      stmtStr = String.format(GET_METRIC_SQL,
        getNaiveTimeRangeHint(condition.getStartTime(), NATIVE_TIME_RANGE_DELTA),
        METRICS_RECORD_TABLE_NAME);
    }

    StringBuilder sb = new StringBuilder(stmtStr);
    sb.append(" WHERE ");
    sb.append(condition.getConditionClause());
    String orderByClause = condition.getOrderByClause();

    if (orderByClause != null) {
      sb.append(orderByClause);
    } else {
      sb.append(" ORDER BY METRIC_NAME, SERVER_TIME ");
    }
    if (condition.getLimit() != null) {
      sb.append(" LIMIT ").append(condition.getLimit());
    }

    LOG.debug("SQL: " + sb.toString() + ", condition: " + condition);
    PreparedStatement stmt = connection.prepareStatement(sb.toString());
    int pos = 1;
    if (condition.getMetricNames() != null) {
      for (; pos <= condition.getMetricNames().size(); pos++) {
        LOG.debug("Setting pos: " + pos + ", value = " + condition.getMetricNames().get(pos - 1));
        stmt.setString(pos, condition.getMetricNames().get(pos - 1));
      }
    }
    if (condition.getHostname() != null) {
      LOG.debug("Setting pos: " + pos + ", value: " + condition.getHostname());
      stmt.setString(pos++, condition.getHostname());
    }
    // TODO: Upper case all strings on POST
    if (condition.getAppId() != null) {
      // TODO: fix case of appId coming from host metrics
      String appId = condition.getAppId();
      if (!condition.getAppId().equals("HOST")) {
        appId = appId.toLowerCase();
      }
      LOG.debug("Setting pos: " + pos + ", value: " + appId);
      stmt.setString(pos++, appId);
    }
    if (condition.getInstanceId() != null) {
      LOG.debug("Setting pos: " + pos + ", value: " + condition.getInstanceId());
      stmt.setString(pos++, condition.getInstanceId());
    }
    if (condition.getStartTime() != null) {
      LOG.debug("Setting pos: " + pos + ", value: " + condition.getStartTime());
      stmt.setLong(pos++, condition.getStartTime());
    }
    if (condition.getEndTime() != null) {
      LOG.debug("Setting pos: " + pos + ", value: " + condition.getEndTime());
      stmt.setLong(pos, condition.getEndTime());
    }
    if (condition.getFetchSize() != null) {
      stmt.setFetchSize(condition.getFetchSize());
    }

    return stmt;
  }


  public static PreparedStatement prepareGetAggregateSqlStmt(
    Connection connection, Condition condition) throws SQLException {

    if (condition.isEmpty()) {
      throw new IllegalArgumentException("Condition is empty.");
    }

    StringBuilder sb = new StringBuilder(GET_CLUSTER_AGGREGATE_SQL);
    sb.append(" WHERE ");
    sb.append(condition.getConditionClause());
    sb.append(" ORDER BY METRIC_NAME, SERVER_TIME");
    if (condition.getLimit() != null) {
      sb.append(" LIMIT ").append(condition.getLimit());
    }

    LOG.debug("SQL => " + sb.toString() + ", condition => " + condition);
    PreparedStatement stmt = connection.prepareStatement(sb.toString());
    int pos = 1;
    if (condition.getMetricNames() != null) {
      for (; pos <= condition.getMetricNames().size(); pos++) {
        stmt.setString(pos, condition.getMetricNames().get(pos - 1));
      }
    }
    // TODO: Upper case all strings on POST
    if (condition.getAppId() != null) {
      stmt.setString(pos++, condition.getAppId().toLowerCase());
    }
    if (condition.getInstanceId() != null) {
      stmt.setString(pos++, condition.getInstanceId());
    }
    if (condition.getStartTime() != null) {
      stmt.setLong(pos++, condition.getStartTime());
    }
    if (condition.getEndTime() != null) {
      stmt.setLong(pos, condition.getEndTime());
    }

    return stmt;
  }

  static class Condition {
    List<String> metricNames;
    String hostname;
    String appId;
    String instanceId;
    Long startTime;
    Long endTime;
    Integer limit;
    boolean grouped;
    boolean noLimit = false;
    Integer fetchSize;
    String statement;
    Set<String> orderByColumns = new LinkedHashSet<String>();

    Condition(List<String> metricNames, String hostname, String appId,
              String instanceId, Long startTime, Long endTime, Integer limit,
              boolean grouped) {
      this.metricNames = metricNames;
      this.hostname = hostname;
      this.appId = appId;
      this.instanceId = instanceId;
      this.startTime = startTime;
      this.endTime = endTime;
      this.limit = limit;
      this.grouped = grouped;
    }

    String getStatement() {
      return statement;
    }

    void setStatement(String statement) {
      this.statement = statement;
    }

    List<String> getMetricNames() {
      return metricNames == null || metricNames.isEmpty() ? null : metricNames;
    }

    String getMetricsClause() {
      StringBuilder sb = new StringBuilder("(");
      if (metricNames != null) {
        for (String name : metricNames) {
          if (sb.length() != 1) {
            sb.append(", ");
          }
          sb.append("?");
        }
        sb.append(")");
        return sb.toString();
      } else {
        return null;
      }
    }

    String getConditionClause() {
      StringBuilder sb = new StringBuilder();
      boolean appendConjunction = false;

      if (getMetricNames() != null) {
        sb.append("METRIC_NAME IN ");
        sb.append(getMetricsClause());
        appendConjunction = true;
      }
      if (appendConjunction) {
        sb.append(" AND");
      }
      appendConjunction = false;
      if (getHostname() != null) {
        sb.append(" HOSTNAME = ?");
        appendConjunction = true;
      }
      if (appendConjunction) {
        sb.append(" AND");
      }
      appendConjunction = false;
      if (getAppId() != null) {
        sb.append(" APP_ID = ?");
        appendConjunction = true;
      }
      if (appendConjunction) {
        sb.append(" AND");
      }
      appendConjunction = false;
      if (getInstanceId() != null) {
        sb.append(" INSTANCE_ID = ?");
        appendConjunction = true;
      }
      if (appendConjunction) {
        sb.append(" AND");
      }
      appendConjunction = false;
      if (getStartTime() != null) {
        sb.append(" SERVER_TIME >= ?");
        appendConjunction = true;
      }
      if (appendConjunction) {
        sb.append(" AND");
      }
      if (getEndTime() != null) {
        sb.append(" SERVER_TIME < ?");
      }
      return sb.toString();
    }

    String getHostname() {
      return hostname == null || hostname.isEmpty() ? null : hostname;
    }

    String getAppId() {
      return appId == null || appId.isEmpty() ? null : appId;
    }

    String getInstanceId() {
      return instanceId == null || instanceId.isEmpty() ? null : instanceId;
    }

    /**
     * Convert to millis.
     */
    Long getStartTime() {
      if (startTime < 9999999999l) {
        return startTime * 1000;
      } else {
        return startTime;
      }
    }

    Long getEndTime() {
      if (endTime < 9999999999l) {
        return endTime * 1000;
      } else {
        return endTime;
      }
    }

    void setNoLimit() {
      this.noLimit = true;
    }

    Integer getLimit() {
      if (noLimit) {
        return null;
      }
      return limit == null ? PhoenixHBaseAccessor.RESULTSET_LIMIT : limit;
    }

    boolean isGrouped() {
      return grouped;
    }

    boolean isEmpty() {
      return (metricNames == null || metricNames.isEmpty())
        && (hostname == null || hostname.isEmpty())
        && (appId == null || appId.isEmpty())
        && (instanceId == null || instanceId.isEmpty())
        && startTime == null
        && endTime == null;
    }

    Integer getFetchSize() {
      return fetchSize;
    }

    void setFetchSize(Integer fetchSize) {
      this.fetchSize = fetchSize;
    }

    void addOrderByColumn(String column) {
      orderByColumns.add(column);
    }

    String getOrderByClause() {
      String orderByStr = " ORDER BY ";
      if (!orderByColumns.isEmpty()) {
        StringBuilder sb = new StringBuilder(orderByStr);
        for (String orderByColumn : orderByColumns) {
          if (sb.length() != orderByStr.length()) {
            sb.append(", ");
          }
          sb.append(orderByColumn);
        }
        sb.append(" ");
        return sb.toString();
      }
      return null;
    }

    @Override
    public String toString() {
      return "Condition{" +
        "metricNames=" + metricNames +
        ", hostname='" + hostname + '\'' +
        ", appId='" + appId + '\'' +
        ", instanceId='" + instanceId + '\'' +
        ", startTime=" + startTime +
        ", endTime=" + endTime +
        ", limit=" + limit +
        ", grouped=" + grouped +
        ", orderBy=" + orderByColumns +
        ", noLimit=" + noLimit +
        '}';
    }
  }
}
