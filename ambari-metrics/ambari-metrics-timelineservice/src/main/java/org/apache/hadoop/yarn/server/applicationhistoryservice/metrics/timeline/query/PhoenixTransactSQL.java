/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.PrecisionLimitExceededException;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulate all metrics related SQL queries.
 */
public class PhoenixTransactSQL {

  public static final Log LOG = LogFactory.getLog(PhoenixTransactSQL.class);

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

  public static final String CREATE_METRICS_AGGREGATE_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS %s " +
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
    "CREATE TABLE IF NOT EXISTS %s " +
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

  // HOSTS_COUNT vs METRIC_COUNT
  public static final String CREATE_METRICS_CLUSTER_AGGREGATE_GROUPED_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS %s " +
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

  public static final String CREATE_METRICS_METADATA_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS METRICS_METADATA " +
      "(METRIC_NAME VARCHAR, " +
      "APP_ID VARCHAR, " +
      "UNITS CHAR(20), " +
      "TYPE CHAR(20), " +
      "START_TIME UNSIGNED_LONG, " +
      "SUPPORTS_AGGREGATION BOOLEAN " +
      "CONSTRAINT pk PRIMARY KEY (METRIC_NAME, APP_ID)) " +
      "DATA_BLOCK_ENCODING='%s', COMPRESSION='%s'";

  public static final String CREATE_HOSTED_APPS_METADATA_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS HOSTED_APPS_METADATA " +
      "(HOSTNAME VARCHAR, APP_IDS VARCHAR, " +
      "CONSTRAINT pk PRIMARY KEY (HOSTNAME))" +
      "DATA_BLOCK_ENCODING='%s', COMPRESSION='%s'";

  /**
   * ALTER table to set new options
   */
  public static final String ALTER_SQL = "ALTER TABLE %s SET TTL=%s";

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
    "%s (METRIC_NAME, APP_ID, INSTANCE_ID, SERVER_TIME, " +
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

  public static final String UPSERT_METADATA_SQL =
    "UPSERT INTO METRICS_METADATA (METRIC_NAME, APP_ID, UNITS, TYPE, " +
      "START_TIME, SUPPORTS_AGGREGATION) " +
      "VALUES (?, ?, ?, ?, ?, ?)";

  public static final String UPSERT_HOSTED_APPS_METADATA_SQL =
    "UPSERT INTO HOSTED_APPS_METADATA (HOSTNAME, APP_IDS) VALUES (?, ?)";

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

  /**
   * Get latest metrics for a number of hosts
   *
   * Different queries for a number and a single hosts are used due to bug
   * in Apache Phoenix
   */
  public static final String GET_LATEST_METRIC_SQL = "SELECT %s " +
    "E.METRIC_NAME AS METRIC_NAME, E.HOSTNAME AS HOSTNAME, " +
    "E.APP_ID AS APP_ID, E.INSTANCE_ID AS INSTANCE_ID, " +
    "E.SERVER_TIME AS SERVER_TIME, E.START_TIME AS START_TIME, " +
    "E.UNITS AS UNITS, E.METRIC_SUM AS METRIC_SUM, " +
    "E.METRIC_MAX AS METRIC_MAX, E.METRIC_MIN AS METRIC_MIN, " +
    "E.METRIC_COUNT AS METRIC_COUNT, E.METRICS AS METRICS " +
    "FROM %s AS E " +
    "INNER JOIN " +
    "(SELECT METRIC_NAME, HOSTNAME, MAX(SERVER_TIME) AS MAX_SERVER_TIME, " +
    "APP_ID, INSTANCE_ID " +
    "FROM %s " +
    "WHERE " +
    "%s " +
    "GROUP BY METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID) " +
    "AS I " +
    "ON E.METRIC_NAME=I.METRIC_NAME " +
    "AND E.HOSTNAME=I.HOSTNAME " +
    "AND E.SERVER_TIME=I.MAX_SERVER_TIME " +
    "AND E.APP_ID=I.APP_ID " +
    "AND E.INSTANCE_ID=I.INSTANCE_ID";

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
    "FROM %s";

  public static final String GET_CLUSTER_AGGREGATE_TIME_SQL = "SELECT %s " +
    "METRIC_NAME, APP_ID, " +
    "INSTANCE_ID, SERVER_TIME, " +
    "UNITS, " +
    "METRIC_SUM, " +
    "METRIC_COUNT, " +
    "METRIC_MAX, " +
    "METRIC_MIN " +
    "FROM %s";

  public static final String GET_METRIC_METADATA_SQL = "SELECT " +
    "METRIC_NAME, APP_ID, UNITS, TYPE, START_TIME, " +
    "SUPPORTS_AGGREGATION FROM METRICS_METADATA";

  public static final String GET_HOSTED_APPS_METADATA_SQL = "SELECT " +
    "HOSTNAME, APP_IDS FROM HOSTED_APPS_METADATA";

  /**
   * Aggregate host metrics using a GROUP BY clause to take advantage of
   * N - way parallel scan where N = number of regions.
   */
  public static final String GET_AGGREGATED_HOST_METRIC_GROUPBY_SQL = "UPSERT %s " +
    "INTO %s (METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, SERVER_TIME, UNITS, " +
    "METRIC_SUM, METRIC_COUNT, METRIC_MAX, METRIC_MIN) " +
    "SELECT METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, %s AS SERVER_TIME, UNITS, " +
    "SUM(METRIC_SUM), SUM(METRIC_COUNT), MAX(METRIC_MAX), MIN(METRIC_MIN) " +
    "FROM %s WHERE SERVER_TIME > %s AND SERVER_TIME <= %s " +
    "GROUP BY METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, UNITS";

  /**
   * Aggregate app metrics using a GROUP BY clause to take advantage of
   * N - way parallel scan where N = number of regions.
   */
  public static final String GET_AGGREGATED_APP_METRIC_GROUPBY_SQL = "UPSERT %s " +
    "INTO %s (METRIC_NAME, APP_ID, INSTANCE_ID, SERVER_TIME, UNITS, " +
    "METRIC_SUM, METRIC_COUNT, METRIC_MAX, METRIC_MIN) SELECT METRIC_NAME, APP_ID, " +
    "INSTANCE_ID, %s AS SERVER_TIME, UNITS, ROUND(AVG(METRIC_SUM),2), ROUND(AVG(%s)), " +
    "MAX(METRIC_MAX), MIN(METRIC_MIN) FROM %s WHERE SERVER_TIME > %s AND " +
    "SERVER_TIME <= %s GROUP BY METRIC_NAME, APP_ID, INSTANCE_ID, UNITS";

  public static final String METRICS_RECORD_TABLE_NAME = "METRIC_RECORD";
  public static final String METRICS_AGGREGATE_MINUTE_TABLE_NAME =
    "METRIC_RECORD_MINUTE";
  public static final String METRICS_AGGREGATE_HOURLY_TABLE_NAME =
    "METRIC_RECORD_HOURLY";
  public static final String METRICS_AGGREGATE_DAILY_TABLE_NAME =
    "METRIC_RECORD_DAILY";
  public static final String METRICS_CLUSTER_AGGREGATE_TABLE_NAME =
    "METRIC_AGGREGATE";
  public static final String METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME =
    "METRIC_AGGREGATE_MINUTE";
  public static final String METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME =
    "METRIC_AGGREGATE_HOURLY";
  public static final String METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME =
    "METRIC_AGGREGATE_DAILY";

  public static final String[] PHOENIX_TABLES = {
    METRICS_RECORD_TABLE_NAME,
    METRICS_AGGREGATE_MINUTE_TABLE_NAME,
    METRICS_AGGREGATE_HOURLY_TABLE_NAME,
    METRICS_AGGREGATE_DAILY_TABLE_NAME,
    METRICS_CLUSTER_AGGREGATE_TABLE_NAME,
    METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME,
    METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME,
    METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME
  };

  public static final String DEFAULT_TABLE_COMPRESSION = "SNAPPY";
  public static final String DEFAULT_ENCODING = "FAST_DIFF";
  public static final long NATIVE_TIME_RANGE_DELTA = 120000; // 2 minutes
  public static final long HOUR = 3600000; // 1 hour
  public static final long DAY = 86400000; // 1 day
  private static boolean sortMergeJoinEnabled = false;

  /**
   * Filter to optimize HBase scan by using file timestamps. This prevents
   * a full table scan of metric records.
   *
   * @return Phoenix Hint String
   */
  public static String getNaiveTimeRangeHint(Long startTime, Long delta) {
    return String.format("/*+ NATIVE_TIME_RANGE(%s) */", (startTime - delta));
  }

  /**
   * Falling back to sort merge join algorithm if default queries fail.
   *
   * @return Phoenix Hint String
   */
  public static String getLatestMetricsHints() {
    if (sortMergeJoinEnabled) {
      return "/*+ USE_SORT_MERGE_JOIN NO_CACHE */";
    }
    return "";
  }

  public static void setSortMergeJoinEnabled(boolean sortMergeJoinEnabled) {
    PhoenixTransactSQL.sortMergeJoinEnabled = sortMergeJoinEnabled;
  }

  public static PreparedStatement prepareGetMetricsSqlStmt(Connection connection,
                                                           Condition condition) throws SQLException {

    validateConditionIsNotEmpty(condition);
    validateRowCountLimit(condition);

    String stmtStr;
    if (condition.getStatement() != null) {
      stmtStr = condition.getStatement();
    } else {

      String metricsTable;
      String query;
      if (condition.getPrecision() == null) {
        long endTime = condition.getEndTime() == null ? System.currentTimeMillis() : condition.getEndTime();
        long startTime = condition.getStartTime() == null ? 0 : condition.getStartTime();
        Precision precision = Precision.getPrecision(startTime, endTime);
        condition.setPrecision(precision);
      }
      switch (condition.getPrecision()) {
        case DAYS:
          metricsTable = METRICS_AGGREGATE_DAILY_TABLE_NAME;
          query = GET_METRIC_AGGREGATE_ONLY_SQL;
          break;
        case HOURS:
          metricsTable = METRICS_AGGREGATE_HOURLY_TABLE_NAME;
          query = GET_METRIC_AGGREGATE_ONLY_SQL;
          break;
        case MINUTES:
          metricsTable = METRICS_AGGREGATE_MINUTE_TABLE_NAME;
          query = GET_METRIC_AGGREGATE_ONLY_SQL;
          break;
        default:
          metricsTable = METRICS_RECORD_TABLE_NAME;
          query = GET_METRIC_SQL;
      }

      stmtStr = String.format(query,
        getNaiveTimeRangeHint(condition.getStartTime(), NATIVE_TIME_RANGE_DELTA),
        metricsTable);
    }

    StringBuilder sb = new StringBuilder(stmtStr);

    if (!(condition instanceof EmptyCondition)) {
      sb.append(" WHERE ");
      sb.append(condition.getConditionClause());
      String orderByClause = condition.getOrderByClause(true);
      if (orderByClause != null) {
        sb.append(orderByClause);
      } else {
        sb.append(" ORDER BY METRIC_NAME, SERVER_TIME ");
      }
    }

    if (condition.getLimit() != null) {
      sb.append(" LIMIT ").append(condition.getLimit());
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("SQL: " + sb.toString() + ", condition: " + condition);
    }

    PreparedStatement stmt = null;
    try {
      stmt = connection.prepareStatement(sb.toString());
      int pos = 1;
      if (condition.getMetricNames() != null) {
        for (; pos <= condition.getMetricNames().size(); pos++) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Setting pos: " + pos + ", value = " + condition.getMetricNames().get(pos - 1));
          }
          stmt.setString(pos, condition.getMetricNames().get(pos - 1));
        }
      }
      if (condition.getHostnames() != null) {
        for (String hostname : condition.getHostnames()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Setting pos: " + pos + ", value: " + hostname);
          }
          stmt.setString(pos++, hostname);
        }
      }
      if (condition.getAppId() != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Setting pos: " + pos + ", value: " + condition.getAppId());
        }
        stmt.setString(pos++, condition.getAppId());
      }
      if (condition.getInstanceId() != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Setting pos: " + pos + ", value: " + condition.getInstanceId());
        }
        stmt.setString(pos++, condition.getInstanceId());
      }
      if (condition.getStartTime() != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Setting pos: " + pos + ", value: " + condition.getStartTime());
        }
        stmt.setLong(pos++, condition.getStartTime());
      }
      if (condition.getEndTime() != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Setting pos: " + pos + ", value: " + condition.getEndTime());
        }
        stmt.setLong(pos, condition.getEndTime());
      }
      if (condition.getFetchSize() != null) {
        stmt.setFetchSize(condition.getFetchSize());
      }
    } catch (SQLException e) {
      if (stmt != null) {
        stmt.close();
      }
      throw e;
    }

    return stmt;
  }

  private static void validateConditionIsNotEmpty(Condition condition) {
    if (condition.isEmpty()) {
      throw new IllegalArgumentException("Condition is empty.");
    }
  }

  private static void validateRowCountLimit(Condition condition) {
    if (condition.getMetricNames() == null
      || condition.getMetricNames().isEmpty()) {
      //aggregator can use empty metrics query
      return;
    }

    long range = condition.getEndTime() - condition.getStartTime();
    long rowsPerMetric;

    //Get Precision (passed in or computed) and estimate values returned based on that.
    Precision precision = condition.getPrecision();
    if (precision == null) {
      precision = Precision.getPrecision(condition.getStartTime(), condition.getEndTime());
    }

    switch (precision) {
      case DAYS:
        rowsPerMetric = TimeUnit.MILLISECONDS.toDays(range);
        break;
      case HOURS:
        rowsPerMetric = TimeUnit.MILLISECONDS.toHours(range);
        break;
      case MINUTES:
        rowsPerMetric = TimeUnit.MILLISECONDS.toMinutes(range)/2; //2 minute data in METRIC_AGGREGATE_MINUTE table.
        break;
      default:
        rowsPerMetric = TimeUnit.MILLISECONDS.toSeconds(range)/10; //10 second data in METRIC_AGGREGATE table
    }

    long totalRowsRequested = rowsPerMetric * condition.getMetricNames().size();
    if (totalRowsRequested > PhoenixHBaseAccessor.RESULTSET_LIMIT) {
      throw new PrecisionLimitExceededException("Requested precision (" + precision + ") for given time range causes " +
        "result set size of " + totalRowsRequested + ", which exceeds the limit - "
        + PhoenixHBaseAccessor.RESULTSET_LIMIT + ". Please request higher precision.");
    }
  }

  public static PreparedStatement prepareGetLatestMetricSqlStmt(
    Connection connection, Condition condition) throws SQLException {

    validateConditionIsNotEmpty(condition);

    if (condition.getMetricNames() == null
      || condition.getMetricNames().isEmpty()) {
      throw new IllegalArgumentException("Point in time query without "
        + "metric names not supported ");
    }

    String stmtStr;
    if (condition.getStatement() != null) {
      stmtStr = condition.getStatement();
    } else {
      stmtStr = String.format(GET_LATEST_METRIC_SQL,
        getLatestMetricsHints(),
        METRICS_RECORD_TABLE_NAME,
        METRICS_RECORD_TABLE_NAME,
        condition.getConditionClause());
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("SQL: " + stmtStr + ", condition: " + condition);
    }
    PreparedStatement stmt = null;
    try {
      stmt = connection.prepareStatement(stmtStr);
      setQueryParameters(stmt, condition);
    } catch (SQLException e) {
      if (stmt != null) {
        stmt.close();
      }
      throw e;
    }

    return stmt;
  }

  private static PreparedStatement setQueryParameters(PreparedStatement stmt,
                                                      Condition condition) throws SQLException {
    int pos = 1;
    //For GET_LATEST_METRIC_SQL_SINGLE_HOST parameters should be set 2 times
    do {
      if (condition.getMetricNames() != null) {
        for (String metricName : condition.getMetricNames()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Setting pos: " + pos + ", value = " + metricName);
          }
          stmt.setString(pos++, metricName);
        }
      }
      if (condition.getHostnames() != null) {
        for (String hostname : condition.getHostnames()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Setting pos: " + pos + ", value: " + hostname);
          }
          stmt.setString(pos++, hostname);
        }
      }
      if (condition.getAppId() != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Setting pos: " + pos + ", value: " + condition.getAppId());
        }
        stmt.setString(pos++, condition.getAppId());
      }
      if (condition.getInstanceId() != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Setting pos: " + pos +
            ", value: " + condition.getInstanceId());
        }
        stmt.setString(pos++, condition.getInstanceId());
      }

      if (condition.getFetchSize() != null) {
        stmt.setFetchSize(condition.getFetchSize());
        pos++;
      }
    } while (pos < stmt.getParameterMetaData().getParameterCount());

    return stmt;
  }

  public static PreparedStatement prepareGetAggregateSqlStmt(
    Connection connection, Condition condition) throws SQLException {

    validateConditionIsNotEmpty(condition);
    validateRowCountLimit(condition);

    String metricsAggregateTable;
    String queryStmt;
    if (condition.getPrecision() == null) {
      long endTime = condition.getEndTime() == null ? System.currentTimeMillis() : condition.getEndTime();
      long startTime = condition.getStartTime() == null ? 0 : condition.getStartTime();
      condition.setPrecision(Precision.getPrecision(startTime, endTime));
    }
    switch (condition.getPrecision()) {
      case DAYS:
        metricsAggregateTable = METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME;
        queryStmt = GET_CLUSTER_AGGREGATE_TIME_SQL;
        break;
      case HOURS:
        metricsAggregateTable = METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
        queryStmt = GET_CLUSTER_AGGREGATE_TIME_SQL;
        break;
      case MINUTES:
        metricsAggregateTable = METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME;
        queryStmt = GET_CLUSTER_AGGREGATE_TIME_SQL;
        break;
      default:
        metricsAggregateTable = METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
        queryStmt = GET_CLUSTER_AGGREGATE_SQL;
    }

    queryStmt = String.format(queryStmt,
      getNaiveTimeRangeHint(condition.getStartTime(), NATIVE_TIME_RANGE_DELTA),
      metricsAggregateTable);

    StringBuilder sb = new StringBuilder(queryStmt);
    sb.append(" WHERE ");
    sb.append(condition.getConditionClause());
    sb.append(" ORDER BY METRIC_NAME, SERVER_TIME");
    if (condition.getLimit() != null) {
      sb.append(" LIMIT ").append(condition.getLimit());
    }

    String query = sb.toString();

    if (LOG.isDebugEnabled()) {
      LOG.debug("SQL => " + query + ", condition => " + condition);
    }
    PreparedStatement stmt = null;
    try {
      stmt = connection.prepareStatement(query);
      int pos = 1;

      if (condition.getMetricNames() != null) {
        for (; pos <= condition.getMetricNames().size(); pos++) {
          stmt.setString(pos, condition.getMetricNames().get(pos - 1));
        }
      }
      // TODO: Upper case all strings on POST
      if (condition.getAppId() != null) {
        stmt.setString(pos++, condition.getAppId());
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
    } catch (SQLException e) {
      if (stmt != null) {
        stmt.close();
      }
      throw e;
    }

    return stmt;
  }

  public static PreparedStatement prepareGetLatestAggregateMetricSqlStmt(
    Connection connection, Condition condition) throws SQLException {

    validateConditionIsNotEmpty(condition);

    String stmtStr;
    if (condition.getStatement() != null) {
      stmtStr = condition.getStatement();
    } else {
      stmtStr = String.format(GET_CLUSTER_AGGREGATE_SQL, "",
        METRICS_CLUSTER_AGGREGATE_TABLE_NAME);
    }

    StringBuilder sb = new StringBuilder(stmtStr);
    sb.append(" WHERE ");
    sb.append(condition.getConditionClause());
    String orderByClause = condition.getOrderByClause(false);
    if (orderByClause != null) {
      sb.append(orderByClause);
    } else {
      sb.append(" ORDER BY METRIC_NAME DESC, SERVER_TIME DESC  ");
    }

    sb.append(" LIMIT ").append(condition.getMetricNames().size());

    String query = sb.toString();
    if (LOG.isDebugEnabled()) {
      LOG.debug("SQL: " + query + ", condition: " + condition);
    }

    PreparedStatement stmt = null;
    try {
      stmt = connection.prepareStatement(query);
      int pos = 1;
      if (condition.getMetricNames() != null) {
        for (; pos <= condition.getMetricNames().size(); pos++) {
          stmt.setString(pos, condition.getMetricNames().get(pos - 1));
        }
      }
      if (condition.getAppId() != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Setting pos: " + pos + ", value: " + condition.getAppId());
        }
        stmt.setString(pos++, condition.getAppId());
      }
      if (condition.getInstanceId() != null) {
        stmt.setString(pos, condition.getInstanceId());
      }
    } catch (SQLException e) {
      if (stmt != null) {

      }
      throw e;
    }

    return stmt;
  }
}
