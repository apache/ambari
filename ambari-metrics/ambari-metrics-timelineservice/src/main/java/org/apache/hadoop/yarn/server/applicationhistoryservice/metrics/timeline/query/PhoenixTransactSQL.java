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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.PrecisionLimitExceededException;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;

/**
 * Encapsulate all metrics related SQL queries.
 */
public class PhoenixTransactSQL {

  public static final Log LOG = LogFactory.getLog(PhoenixTransactSQL.class);

  /**
   * Create table to store individual metric records.
   */
  public static final String CREATE_METRICS_TABLE_SQL = "CREATE TABLE IF NOT " +
    "EXISTS METRIC_RECORD (UUID BINARY(20) NOT NULL, " +
    "SERVER_TIME BIGINT NOT NULL, " +
    "METRIC_SUM DOUBLE, " +
    "METRIC_COUNT UNSIGNED_INT, " +
    "METRIC_MAX DOUBLE, " +
    "METRIC_MIN DOUBLE, " +
    "METRICS VARCHAR CONSTRAINT pk " +
    "PRIMARY KEY (UUID, SERVER_TIME ROW_TIMESTAMP)) DATA_BLOCK_ENCODING='%s', IMMUTABLE_ROWS=true, " +
    "TTL=%s, COMPRESSION='%s'";

  public static final String CREATE_CONTAINER_METRICS_TABLE_SQL =
      "CREATE TABLE IF NOT EXISTS CONTAINER_METRICS "
      + "(APP_ID VARCHAR, "
      + " CONTAINER_ID VARCHAR,"
      + " START_TIME TIMESTAMP,"
      + " FINISH_TIME TIMESTAMP, "
      + " DURATION BIGINT,"
      + " HOSTNAME VARCHAR,"
      + " EXIT_CODE INTEGER,"
      + " LOCALIZATION_DURATION BIGINT,"
      + " LAUNCH_DURATION BIGINT,"
      + " MEM_REQUESTED_GB DOUBLE,"
      + " MEM_REQUESTED_GB_MILLIS DOUBLE,"
      + " MEM_VIRTUAL_GB DOUBLE,"
      + " MEM_USED_GB_MIN DOUBLE,"
      + " MEM_USED_GB_MAX DOUBLE,"
      + " MEM_USED_GB_AVG DOUBLE,"
      + " MEM_USED_GB_50_PCT DOUBLE,"
      + " MEM_USED_GB_75_PCT DOUBLE,"
      + " MEM_USED_GB_90_PCT DOUBLE,"
      + " MEM_USED_GB_95_PCT DOUBLE,"
      + " MEM_USED_GB_99_PCT DOUBLE,"
      + " MEM_UNUSED_GB DOUBLE,"
      + " MEM_UNUSED_GB_MILLIS DOUBLE "
      + " CONSTRAINT pk PRIMARY KEY(APP_ID, CONTAINER_ID)) DATA_BLOCK_ENCODING='%s',"
      + " IMMUTABLE_ROWS=true, TTL=%s, COMPRESSION='%s'";

  public static final String CREATE_METRICS_AGGREGATE_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS %s " +
      "(UUID BINARY(20) NOT NULL, " +
      "SERVER_TIME BIGINT NOT NULL, " +
      "METRIC_SUM DOUBLE," +
      "METRIC_COUNT UNSIGNED_INT, " +
      "METRIC_MAX DOUBLE," +
      "METRIC_MIN DOUBLE CONSTRAINT pk " +
      "PRIMARY KEY (UUID, SERVER_TIME ROW_TIMESTAMP)) DATA_BLOCK_ENCODING='%s', IMMUTABLE_ROWS=true, TTL=%s," +
      " COMPRESSION='%s'";

  public static final String CREATE_METRICS_CLUSTER_AGGREGATE_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS %s " +
      "(UUID BINARY(16) NOT NULL, " +
      "SERVER_TIME BIGINT NOT NULL, " +
      "METRIC_SUM DOUBLE, " +
      "HOSTS_COUNT UNSIGNED_INT, " +
      "METRIC_MAX DOUBLE, " +
      "METRIC_MIN DOUBLE " +
      "CONSTRAINT pk PRIMARY KEY (UUID, SERVER_TIME ROW_TIMESTAMP)) DATA_BLOCK_ENCODING='%s', IMMUTABLE_ROWS=true, " +
      "TTL=%s, COMPRESSION='%s'";

  // HOSTS_COUNT vs METRIC_COUNT
  public static final String CREATE_METRICS_CLUSTER_AGGREGATE_GROUPED_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS %s " +
      "(UUID BINARY(16) NOT NULL, " +
      "SERVER_TIME BIGINT NOT NULL, " +
      "METRIC_SUM DOUBLE, " +
      "METRIC_COUNT UNSIGNED_INT, " +
      "METRIC_MAX DOUBLE, " +
      "METRIC_MIN DOUBLE " +
      "CONSTRAINT pk PRIMARY KEY (UUID, SERVER_TIME ROW_TIMESTAMP)) DATA_BLOCK_ENCODING='%s', IMMUTABLE_ROWS=true, " +
      "TTL=%s, COMPRESSION='%s'";

  public static final String CREATE_METRICS_METADATA_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS METRICS_METADATA " +
      "(METRIC_NAME VARCHAR, " +
      "APP_ID VARCHAR, " +
      "INSTANCE_ID VARCHAR, " +
      "UUID BINARY(16), " +
      "UNITS CHAR(20), " +
      "TYPE CHAR(20), " +
      "START_TIME UNSIGNED_LONG, " +
      "SUPPORTS_AGGREGATION BOOLEAN, " +
      "IS_WHITELISTED BOOLEAN " +
      "CONSTRAINT pk PRIMARY KEY (METRIC_NAME, APP_ID)) " +
      "DATA_BLOCK_ENCODING='%s', COMPRESSION='%s'";

  public static final String CREATE_HOSTED_APPS_METADATA_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS HOSTED_APPS_METADATA " +
      "(HOSTNAME VARCHAR, UUID BINARY(4), APP_IDS VARCHAR, " +
      "CONSTRAINT pk PRIMARY KEY (HOSTNAME))" +
      "DATA_BLOCK_ENCODING='%s', COMPRESSION='%s'";

  public static final String CREATE_INSTANCE_HOST_TABLE_SQL =
    "CREATE TABLE IF NOT EXISTS INSTANCE_HOST_METADATA " +
      "(INSTANCE_ID VARCHAR, HOSTNAME VARCHAR, " +
      "CONSTRAINT pk PRIMARY KEY (INSTANCE_ID, HOSTNAME))" +
      "DATA_BLOCK_ENCODING='%s', COMPRESSION='%s'";

  public static final String ALTER_METRICS_METADATA_TABLE =
    "ALTER TABLE METRICS_METADATA ADD IF NOT EXISTS IS_WHITELISTED BOOLEAN";

  /**
   * ALTER table to set new options
   */
  public static final String ALTER_SQL = "ALTER TABLE %s SET TTL=%s";

  /**
   * Insert into metric records table.
   */
  public static final String UPSERT_METRICS_SQL = "UPSERT INTO %s " +
    "(UUID, " +
    "SERVER_TIME, " +
    "METRIC_SUM, " +
    "METRIC_MAX, " +
    "METRIC_MIN, " +
    "METRIC_COUNT, " +
    "METRICS) VALUES " +
    "(?, ?, ?, ?, ?, ?, ?)";

  public static final String UPSERT_CONTAINER_METRICS_SQL = "UPSERT INTO %s " +
      "(APP_ID,"
      + " CONTAINER_ID,"
      + " START_TIME,"
      + " FINISH_TIME,"
      + " DURATION,"
      + " HOSTNAME,"
      + " EXIT_CODE,"
      + " LOCALIZATION_DURATION,"
      + " LAUNCH_DURATION,"
      + " MEM_REQUESTED_GB,"
      + " MEM_REQUESTED_GB_MILLIS,"
      + " MEM_VIRTUAL_GB,"
      + " MEM_USED_GB_MIN,"
      + " MEM_USED_GB_MAX,"
      + " MEM_USED_GB_AVG,"
      + " MEM_USED_GB_50_PCT,"
      + " MEM_USED_GB_75_PCT,"
      + " MEM_USED_GB_90_PCT,"
      + " MEM_USED_GB_95_PCT,"
      + " MEM_USED_GB_99_PCT,"
      + " MEM_UNUSED_GB,"
      + " MEM_UNUSED_GB_MILLIS) VALUES " +
      "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  public static final String UPSERT_CLUSTER_AGGREGATE_SQL = "UPSERT INTO " +
    "%s (UUID, " +
    "SERVER_TIME, " +
    "METRIC_SUM, " +
    "HOSTS_COUNT, " +
    "METRIC_MAX, " +
    "METRIC_MIN) " +
    "VALUES (?, ?, ?, ?, ?, ?)";

  public static final String UPSERT_CLUSTER_AGGREGATE_TIME_SQL = "UPSERT INTO" +
    " %s (UUID, SERVER_TIME, " +
    "METRIC_SUM, " +
    "METRIC_COUNT, " +
    "METRIC_MAX, " +
    "METRIC_MIN) " +
    "VALUES (?, ?, ?, ?, ?, ?)";

  public static final String UPSERT_AGGREGATE_RECORD_SQL = "UPSERT INTO " +
    "%s (UUID, " +
    "SERVER_TIME, " +
    "METRIC_SUM, " +
    "METRIC_MAX, " +
    "METRIC_MIN," +
    "METRIC_COUNT) " +
    "VALUES (?, ?, ?, ?, ?, ?)";

  public static final String UPSERT_METADATA_SQL =
    "UPSERT INTO METRICS_METADATA (METRIC_NAME, APP_ID, INSTANCE_ID, UUID, UNITS, TYPE, " +
      "START_TIME, SUPPORTS_AGGREGATION, IS_WHITELISTED) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  public static final String UPSERT_HOSTED_APPS_METADATA_SQL =
    "UPSERT INTO HOSTED_APPS_METADATA (HOSTNAME, UUID, APP_IDS) VALUES (?, ?, ?)";

  public static final String UPSERT_INSTANCE_HOST_METADATA_SQL =
    "UPSERT INTO INSTANCE_HOST_METADATA (INSTANCE_ID, HOSTNAME) VALUES (?, ?)";

  /**
   * Retrieve a set of rows from metrics records table.
   */
  public static final String GET_METRIC_SQL = "SELECT UUID, SERVER_TIME, " +
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
  public static final String GET_LATEST_METRIC_SQL = "SELECT %s E.UUID AS UUID, " +
    "E.SERVER_TIME AS SERVER_TIME, " +
    "E.METRIC_SUM AS METRIC_SUM, " +
    "E.METRIC_MAX AS METRIC_MAX, E.METRIC_MIN AS METRIC_MIN, " +
    "E.METRIC_COUNT AS METRIC_COUNT, E.METRICS AS METRICS " +
    "FROM %s AS E " +
    "INNER JOIN " +
    "(SELECT UUID, MAX(SERVER_TIME) AS MAX_SERVER_TIME " +
    "FROM %s " +
    "WHERE " +
    "%s " +
    "GROUP BY UUID) " +
    "AS I " +
    "ON E.UUID=I.UUID " +
    "AND E.SERVER_TIME=I.MAX_SERVER_TIME";

  public static final String GET_METRIC_AGGREGATE_ONLY_SQL = "SELECT UUID, " +
    "SERVER_TIME, " +
    "METRIC_SUM, " +
    "METRIC_MAX, " +
    "METRIC_MIN, " +
    "METRIC_COUNT " +
    "FROM %s";

  public static final String GET_CLUSTER_AGGREGATE_SQL = "SELECT " +
    "UUID, " +
    "SERVER_TIME, " +
    "METRIC_SUM, " +
    "HOSTS_COUNT, " +
    "METRIC_MAX, " +
    "METRIC_MIN " +
    "FROM %s";

  public static final String GET_CLUSTER_AGGREGATE_TIME_SQL = "SELECT " +
    "UUID, " +
    "SERVER_TIME, " +
    "METRIC_SUM, " +
    "METRIC_COUNT, " +
    "METRIC_MAX, " +
    "METRIC_MIN " +
    "FROM %s";

  public static final String TOP_N_INNER_SQL = "SELECT UUID " +
    "FROM %s WHERE %s GROUP BY UUID ORDER BY %s LIMIT %s";

  public static final String GET_METRIC_METADATA_SQL = "SELECT " +
    "METRIC_NAME, APP_ID, INSTANCE_ID, UUID, UNITS, TYPE, START_TIME, " +
    "SUPPORTS_AGGREGATION, IS_WHITELISTED FROM METRICS_METADATA";

  public static final String GET_HOSTED_APPS_METADATA_SQL = "SELECT " +
    "HOSTNAME, UUID, APP_IDS FROM HOSTED_APPS_METADATA";

  public static final String GET_INSTANCE_HOST_METADATA_SQL = "SELECT " +
    "INSTANCE_ID, HOSTNAME FROM INSTANCE_HOST_METADATA";

  /**
   * Aggregate host metrics using a GROUP BY clause to take advantage of
   * N - way parallel scan where N = number of regions.
   */
  public static final String GET_AGGREGATED_HOST_METRIC_GROUPBY_SQL = "UPSERT " +
    "INTO %s (UUID, SERVER_TIME, METRIC_SUM, METRIC_COUNT, METRIC_MAX, METRIC_MIN) " +
    "SELECT UUID, %s AS SERVER_TIME, " +
    "ROUND(SUM(METRIC_SUM)/SUM(METRIC_COUNT),2), SUM(METRIC_COUNT), MAX(METRIC_MAX), MIN(METRIC_MIN) " +
    "FROM %s WHERE%s SERVER_TIME > %s AND SERVER_TIME <= %s GROUP BY UUID";

  /**
   * Downsample host metrics.
   */
  public static final String DOWNSAMPLE_HOST_METRIC_SQL_UPSERT_PREFIX = "UPSERT INTO %s (UUID, SERVER_TIME, " +
    "METRIC_SUM, METRIC_COUNT, METRIC_MAX, METRIC_MIN) ";

  public static final String TOPN_DOWNSAMPLER_HOST_METRIC_SELECT_SQL = "SELECT UUID, " +
    "%s AS SERVER_TIME, %s, 1, %s, %s FROM %s WHERE UUID IN %s AND SERVER_TIME > %s AND SERVER_TIME <= %s " +
    "GROUP BY UUID ORDER BY %s DESC LIMIT %s";

  /**
   * Aggregate app metrics using a GROUP BY clause to take advantage of
   * N - way parallel scan where N = number of regions.
   */
  public static final String GET_AGGREGATED_APP_METRIC_GROUPBY_SQL = "UPSERT " +
    "INTO %s (UUID, SERVER_TIME, METRIC_SUM, METRIC_COUNT, METRIC_MAX, METRIC_MIN) SELECT UUID, %s AS SERVER_TIME, " +
    "ROUND(AVG(METRIC_SUM),2), ROUND(AVG(%s)), MAX(METRIC_MAX), MIN(METRIC_MIN) FROM %s WHERE%s SERVER_TIME > %s AND " +
    "SERVER_TIME <= %s GROUP BY UUID";

  /**
   * Downsample cluster metrics.
   */
  public static final String DOWNSAMPLE_CLUSTER_METRIC_SQL_UPSERT_PREFIX = "UPSERT INTO %s (UUID, SERVER_TIME, " +
    "METRIC_SUM, METRIC_COUNT, METRIC_MAX, METRIC_MIN) ";

  public static final String TOPN_DOWNSAMPLER_CLUSTER_METRIC_SELECT_SQL = "SELECT UUID, " +
    "%s AS SERVER_TIME, %s, 1, %s, %s FROM %s WHERE UUID IN %s AND SERVER_TIME > %s AND SERVER_TIME <= %s " +
    "GROUP BY UUID ORDER BY %s DESC LIMIT %s";

  /**
   * Event based downsampler SELECT query.
   */
  public static final String EVENT_DOWNSAMPLER_HOST_METRIC_SELECT_SQL = "SELECT METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, " +
    "%s AS SERVER_TIME, UNITS, SUM(METRIC_SUM), SUM(METRIC_COUNT), MAX(METRIC_MAX), MIN(METRIC_MIN) " +
    "FROM %s WHERE METRIC_NAME LIKE %s AND SERVER_TIME > %s AND SERVER_TIME <= %s GROUP BY METRIC_NAME, HOSTNAME, APP_ID, INSTANCE_ID, UNITS";

  public static final String EVENT_DOWNSAMPLER_CLUSTER_METRIC_SELECT_SQL = "SELECT METRIC_NAME, APP_ID, " +
    "INSTANCE_ID, %s AS SERVER_TIME, UNITS, SUM(METRIC_SUM), SUM(%s), " +
    "MAX(METRIC_MAX), MIN(METRIC_MIN) FROM %s WHERE METRIC_NAME LIKE %s AND SERVER_TIME > %s AND " +
    "SERVER_TIME <= %s GROUP BY METRIC_NAME, APP_ID, INSTANCE_ID, UNITS";

  public static final String METRICS_RECORD_TABLE_NAME = "METRIC_RECORD";

  public static final String CONTAINER_METRICS_TABLE_NAME = "CONTAINER_METRICS";

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

  public static final Pattern PHOENIX_TABLES_REGEX_PATTERN = Pattern.compile("METRIC_");

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

      stmtStr = String.format(query, metricsTable);
    }

    StringBuilder sb = new StringBuilder(stmtStr);

    if (!(condition instanceof EmptyCondition)) {
      sb.append(" WHERE ");
      sb.append(condition.getConditionClause());
      String orderByClause = condition.getOrderByClause(true);
      if (orderByClause != null) {
        sb.append(orderByClause);
      } else {
        sb.append(" ORDER BY UUID, SERVER_TIME ");
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
      pos = addUuids(condition, pos, stmt);

      if (condition instanceof TopNCondition) {
        pos = addStartTime(condition, pos, stmt);
        pos = addEndTime(condition, pos, stmt);
      }

      pos = addStartTime(condition, pos, stmt);
      addEndTime(condition, pos, stmt);

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
        rowsPerMetric = TimeUnit.MILLISECONDS.toMinutes(range)/5; //5 minute data in METRIC_AGGREGATE_MINUTE table.
        break;
      default:
        rowsPerMetric = TimeUnit.MILLISECONDS.toSeconds(range)/10; //10 second data in METRIC_AGGREGATE table
    }

    List<String> hostNames = condition.getHostnames();
    int numHosts = (hostNames == null || hostNames.isEmpty()) ? 1 : condition.getHostnames().size();

    long totalRowsRequested = rowsPerMetric * condition.getMetricNames().size() * numHosts;

    if (totalRowsRequested > PhoenixHBaseAccessor.RESULTSET_LIMIT) {
      throw new PrecisionLimitExceededException("Requested " +  condition.getMetricNames().size() + " metrics for "
        + numHosts + " hosts in " + precision +  " precision for the time range of " + range/1000
        + " seconds. Estimated resultset size of " + totalRowsRequested + " is greater than the limit of "
        + PhoenixHBaseAccessor.RESULTSET_LIMIT + ". Request lower precision or fewer number of metrics or hosts." +
        " Alternatively, increase the limit value through ams-site:timeline.metrics.service.default.result.limit config");
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
      if (condition.getUuids() != null) {
        for (byte[] uuid : condition.getUuids()) {
          stmt.setBytes(pos++, uuid);
        }
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

    queryStmt = String.format(queryStmt, metricsAggregateTable);

    StringBuilder sb = new StringBuilder(queryStmt);
    sb.append(" WHERE ");
    sb.append(condition.getConditionClause());
    sb.append(" ORDER BY UUID, SERVER_TIME");
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

      pos = addUuids(condition, pos, stmt);

      if (condition instanceof TopNCondition) {
        pos = addStartTime(condition, pos, stmt);
        pos = addEndTime(condition, pos, stmt);
      }

      // TODO: Upper case all strings on POST
      pos = addStartTime(condition, pos, stmt);
      addEndTime(condition, pos, stmt);
    } catch (SQLException e) {
      if (stmt != null) {
        stmt.close();
      }
      throw e;
    }

    return stmt;
  }

  public static PreparedStatement prepareGetLatestAggregateMetricSqlStmt(
    Connection connection, SplitByMetricNamesCondition condition) throws SQLException {

    validateConditionIsNotEmpty(condition);

    String stmtStr;
    if (condition.getStatement() != null) {
      stmtStr = condition.getStatement();
    } else {
      stmtStr = String.format(GET_CLUSTER_AGGREGATE_SQL,
        METRICS_CLUSTER_AGGREGATE_TABLE_NAME);
    }

    StringBuilder sb = new StringBuilder(stmtStr);
    sb.append(" WHERE ");
    sb.append(condition.getConditionClause());
    String orderByClause = condition.getOrderByClause(false);
    if (orderByClause != null) {
      sb.append(orderByClause);
    } else {
      sb.append(" ORDER BY UUID DESC, SERVER_TIME DESC  ");
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
          stmt.setBytes(pos, condition.getCurrentUuid());
        }
      }
    } catch (SQLException e) {
      if (stmt != null) {

      }
      throw e;
    }

    return stmt;
  }

  public static String getTargetTableUsingPrecision(Precision precision, boolean withHosts) {

    String inputTable = null;
    if (precision != null) {
      if (withHosts) {
        switch (precision) {
          case DAYS:
            inputTable = PhoenixTransactSQL.METRICS_AGGREGATE_DAILY_TABLE_NAME;
            break;
          case HOURS:
            inputTable = PhoenixTransactSQL.METRICS_AGGREGATE_HOURLY_TABLE_NAME;
            break;
          case MINUTES:
            inputTable = PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME;
            break;
          default:
            inputTable = PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
        }
      } else {
        switch (precision) {
          case DAYS:
            inputTable = PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME;
            break;
          case HOURS:
            inputTable = PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
            break;
          case MINUTES:
            inputTable = PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME;
            break;
          default:
            inputTable = PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
        }
      }
    } else {
      if (withHosts) {
        inputTable = PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
      } else {
        inputTable = PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
      }
    }
    return inputTable;
  }

  private static int addUuids(Condition condition, int pos, PreparedStatement stmt) throws SQLException {
    if (condition.getUuids() != null) {
      for (int pos2 = 1 ; pos2 <= condition.getUuids().size(); pos2++,pos++) {
        byte[] uuid = condition.getUuids().get(pos2 - 1);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Setting pos: " + pos + ", value = " + new String(uuid));
        }

        if (uuid.length != TimelineMetricMetadataManager.HOSTNAME_UUID_LENGTH + TimelineMetricMetadataManager.TIMELINE_METRIC_UUID_LENGTH) {
          stmt.setString(pos, new String(uuid));
        } else {
          stmt.setBytes(pos, uuid);
        }
      }
    }
    return pos;
  }

  private static int addStartTime(Condition condition, int pos, PreparedStatement stmt) throws SQLException {
    if (condition.getStartTime() != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting pos: " + pos + ", value: " + condition.getStartTime());
      }
      stmt.setLong(pos++, condition.getStartTime());
    }
    return pos;
  }

  private static int addEndTime(Condition condition, int pos, PreparedStatement stmt) throws SQLException {

    if (condition.getEndTime() != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting pos: " + pos + ", value: " + condition.getEndTime());
      }
      stmt.setLong(pos++, condition.getEndTime());
    }
    return pos;
  }

}
