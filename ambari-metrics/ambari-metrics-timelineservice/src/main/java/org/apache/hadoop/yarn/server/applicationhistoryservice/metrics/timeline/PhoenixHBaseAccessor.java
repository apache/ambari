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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.util.RetryCounter;
import org.apache.hadoop.hbase.util.RetryCounterFactory;
import org.apache.hadoop.metrics2.sink.timeline.SingleValuedTimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricClusterAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricHostAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricReadHelper;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.ConnectionProvider;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultPhoenixDataSource;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.SplitByMetricNamesCondition;
import org.apache.hadoop.yarn.util.timeline.TimelineUtils;
import org.apache.phoenix.exception.SQLExceptionCode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_DAILY_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_HOUR_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_MINUTE_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.GLOBAL_MAX_RETRIES;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.GLOBAL_RESULT_LIMIT;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.GLOBAL_RETRY_INTERVAL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HBASE_COMPRESSION_SCHEME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HBASE_ENCODING_SCHEME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_DAILY_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_HOUR_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_MINUTE_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.PRECISION_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.ALTER_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.CREATE_METRICS_AGGREGATE_TABLE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.CREATE_METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.CREATE_METRICS_CLUSTER_AGGREGATE_TABLE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.CREATE_METRICS_TABLE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.DEFAULT_ENCODING;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.DEFAULT_TABLE_COMPRESSION;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_AGGREGATE_RECORD_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_CLUSTER_AGGREGATE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_CLUSTER_AGGREGATE_TIME_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_METRICS_SQL;

/**
 * Provides a facade over the Phoenix API to access HBase schema
 */
public class PhoenixHBaseAccessor {

  static final int PHOENIX_MAX_MUTATION_STATE_SIZE = 50000;
  private static final Log LOG = LogFactory.getLog(PhoenixHBaseAccessor.class);
  private static final TimelineMetricReadHelper TIMELINE_METRIC_READ_HELPER = new TimelineMetricReadHelper();
  /**
   * 4 metrics/min * 60 * 24: Retrieve data for 1 day.
   */
  private static final int METRICS_PER_MINUTE = 4;
  public static int RESULTSET_LIMIT = (int)TimeUnit.DAYS.toMinutes(1) *
    METRICS_PER_MINUTE;
  private static ObjectMapper mapper = new ObjectMapper();
  private static TypeReference<Map<Long, Double>> metricValuesTypeRef =
    new TypeReference<Map<Long, Double>>() {};
  private final Configuration hbaseConf;
  private final Configuration metricsConf;
  private final RetryCounterFactory retryCounterFactory;
  private final ConnectionProvider dataSource;

  public PhoenixHBaseAccessor(Configuration hbaseConf,
                              Configuration metricsConf){
    this(hbaseConf, metricsConf, new DefaultPhoenixDataSource(hbaseConf));
  }

  public PhoenixHBaseAccessor(Configuration hbaseConf,
                              Configuration metricsConf,
                              ConnectionProvider dataSource) {
    this.hbaseConf = hbaseConf;
    this.metricsConf = metricsConf;
    RESULTSET_LIMIT = metricsConf.getInt(GLOBAL_RESULT_LIMIT, 5760);
    try {
      Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
    } catch (ClassNotFoundException e) {
      LOG.error("Phoenix client jar not found in the classpath.", e);
      throw new IllegalStateException(e);
    }
    this.dataSource = dataSource;
    this.retryCounterFactory = new RetryCounterFactory(
      metricsConf.getInt(GLOBAL_MAX_RETRIES, 10),
      (int) SECONDS.toMillis(metricsConf.getInt(GLOBAL_RETRY_INTERVAL, 5)));
  }

  private static TimelineMetric getLastTimelineMetricFromResultSet(ResultSet rs)
    throws SQLException, IOException {
    TimelineMetric metric = TIMELINE_METRIC_READ_HELPER.getTimelineMetricCommonsFromResultSet(rs);
    metric.setMetricValues(readLastMetricValueFromJSON(rs.getString("METRICS")));
    return metric;
  }

  public static SingleValuedTimelineMetric getAggregatedTimelineMetricFromResultSet(
      ResultSet rs, Function f) throws SQLException, IOException {

    SingleValuedTimelineMetric metric = new SingleValuedTimelineMetric(
      rs.getString("METRIC_NAME") + f.getSuffix(),
      rs.getString("APP_ID"),
      rs.getString("INSTANCE_ID"),
      rs.getString("HOSTNAME"),
      rs.getLong("SERVER_TIME"),
      rs.getLong("SERVER_TIME"),
      rs.getString("UNITS")
    );

    // get functions for metricnames

    double value;
    switch(f.getReadFunction()){
      case AVG:
        value = rs.getDouble("METRIC_SUM") / rs.getInt("METRIC_COUNT");
        break;
      case MIN:
        value = rs.getDouble("METRIC_MIN");
        break;
      case MAX:
        value = rs.getDouble("METRIC_MAX");
        break;
      case SUM:
        value = rs.getDouble("METRIC_SUM");
        break;
      default:
        value = rs.getDouble("METRIC_SUM") / rs.getInt("METRIC_COUNT");
        break;
    }

    metric.setSingleTimeseriesValue(rs.getLong("SERVER_TIME"), value);

    return metric;
  }

  private static Map<Long, Double> readLastMetricValueFromJSON(String json)
    throws IOException {
    Map<Long, Double> values = readMetricFromJSON(json);
    Long lastTimeStamp = Collections.max(values.keySet());

    HashMap<Long, Double> valueMap = new HashMap<Long, Double>(1);
    valueMap.put(lastTimeStamp, values.get(lastTimeStamp));
    return valueMap;
  }

  @SuppressWarnings("unchecked")
  public static Map<Long, Double>  readMetricFromJSON(String json) throws IOException {
    return (Map<Long, Double>) mapper.readValue(json, metricValuesTypeRef);
  }

  private Connection getConnectionRetryingOnException()
    throws SQLException, InterruptedException {
    RetryCounter retryCounter = retryCounterFactory.create();
    while (true) {
      try{
        return getConnection();
      } catch (SQLException e) {
        if(!retryCounter.shouldRetry()){
          LOG.error("HBaseAccessor getConnection failed after "
            + retryCounter.getMaxAttempts() + " attempts");
          throw e;
        }
      }
      retryCounter.sleepUntilNextRetry();
    }
  }

  /**
   * Get JDBC connection to HBase store. Assumption is that the hbase
   * configuration is present on the classpath and loaded by the caller into
   * the Configuration object.
   * Phoenix already caches the HConnection between the client and HBase
   * cluster.
   *
   * @return @java.sql.Connection
   */
  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  protected void initMetricSchema() {
    Connection conn = null;
    Statement stmt = null;

    String encoding = metricsConf.get(HBASE_ENCODING_SCHEME, DEFAULT_ENCODING);
    String compression = metricsConf.get(HBASE_COMPRESSION_SCHEME, DEFAULT_TABLE_COMPRESSION);
    String precisionTtl = metricsConf.get(PRECISION_TABLE_TTL, "86400");
    String hostMinTtl = metricsConf.get(HOST_MINUTE_TABLE_TTL, "604800");
    String hostHourTtl = metricsConf.get(HOST_HOUR_TABLE_TTL, "2592000");
    String hostDailyTtl = metricsConf.get(HOST_DAILY_TABLE_TTL, "31536000");
    String clusterMinTtl = metricsConf.get(CLUSTER_MINUTE_TABLE_TTL, "2592000");
    String clusterHourTtl = metricsConf.get(CLUSTER_HOUR_TABLE_TTL, "31536000");
    String clusterDailyTtl = metricsConf.get(CLUSTER_DAILY_TABLE_TTL, "31536000");

    try {
      LOG.info("Initializing metrics schema...");
      conn = getConnectionRetryingOnException();
      stmt = conn.createStatement();

      // Host level
      stmt.executeUpdate(String.format(CREATE_METRICS_TABLE_SQL,
        encoding, precisionTtl, compression));
      stmt.executeUpdate(String.format(CREATE_METRICS_AGGREGATE_TABLE_SQL,
        METRICS_AGGREGATE_MINUTE_TABLE_NAME, encoding, hostMinTtl, compression));
      stmt.executeUpdate(String.format(CREATE_METRICS_AGGREGATE_TABLE_SQL,
        METRICS_AGGREGATE_HOURLY_TABLE_NAME, encoding, hostHourTtl, compression));
      stmt.executeUpdate(String.format(CREATE_METRICS_AGGREGATE_TABLE_SQL,
        METRICS_AGGREGATE_DAILY_TABLE_NAME, encoding, hostDailyTtl, compression));

      // Cluster level
      stmt.executeUpdate(String.format(CREATE_METRICS_CLUSTER_AGGREGATE_TABLE_SQL,
        METRICS_CLUSTER_AGGREGATE_TABLE_NAME, encoding, clusterMinTtl, compression));
      stmt.executeUpdate(String.format(CREATE_METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_SQL,
        METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME, encoding, clusterHourTtl, compression));
      stmt.executeUpdate(String.format(CREATE_METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_SQL,
        METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME, encoding, clusterDailyTtl, compression));

      //alter TTL options to update tables
      stmt.executeUpdate(String.format(ALTER_SQL,
        METRICS_RECORD_TABLE_NAME,
        precisionTtl));
      stmt.executeUpdate(String.format(ALTER_SQL,
        METRICS_AGGREGATE_MINUTE_TABLE_NAME,
        hostMinTtl));
      stmt.executeUpdate(String.format(ALTER_SQL,
        METRICS_AGGREGATE_HOURLY_TABLE_NAME,
        hostHourTtl));
      stmt.executeUpdate(String.format(ALTER_SQL,
        METRICS_AGGREGATE_DAILY_TABLE_NAME,
        hostDailyTtl));
      stmt.executeUpdate(String.format(ALTER_SQL,
        METRICS_CLUSTER_AGGREGATE_TABLE_NAME,
        clusterMinTtl));
      stmt.executeUpdate(String.format(ALTER_SQL,
        METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME,
        clusterHourTtl));
      stmt.executeUpdate(String.format(ALTER_SQL,
        METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME,
        clusterDailyTtl));

      conn.commit();

      LOG.info("Metrics schema initialized.");
    } catch (SQLException sql) {
      if (sql.getErrorCode() ==
        SQLExceptionCode.SET_UNSUPPORTED_PROP_ON_ALTER_TABLE.getErrorCode()) {
        LOG.warn("Cannot update TTL on tables. " + sql.getMessage());
      } else {
        LOG.error("Error creating Metrics Schema in HBase using Phoenix.", sql);
        throw new MetricsSystemInitializationException(
          "Error creating Metrics Schema in HBase using Phoenix.", sql);
      }
    } catch (InterruptedException e) {
      LOG.error("Error creating Metrics Schema in HBase using Phoenix.", e);
      throw new MetricsSystemInitializationException(
        "Error creating Metrics Schema in HBase using Phoenix.", e);
    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
    }
  }

  public void insertMetricRecords(TimelineMetrics metrics) throws SQLException, IOException {

    List<TimelineMetric> timelineMetrics = metrics.getMetrics();
    if (timelineMetrics == null || timelineMetrics.isEmpty()) {
      LOG.debug("Empty metrics insert request.");
      return;
    }

    Connection conn = getConnection();
    PreparedStatement metricRecordStmt = null;
    long currentTime = System.currentTimeMillis();

    try {
      metricRecordStmt = conn.prepareStatement(String.format(
        UPSERT_METRICS_SQL, METRICS_RECORD_TABLE_NAME));

      for (TimelineMetric metric : timelineMetrics) {
        metricRecordStmt.clearParameters();

        if (LOG.isTraceEnabled()) {
          LOG.trace("host: " + metric.getHostName() + ", " +
            "metricName = " + metric.getMetricName() + ", " +
            "values: " + metric.getMetricValues());
        }
        double[] aggregates =  AggregatorUtils.calculateAggregates(
          metric.getMetricValues());

        metricRecordStmt.setString(1, metric.getMetricName());
        metricRecordStmt.setString(2, metric.getHostName());
        metricRecordStmt.setString(3, metric.getAppId());
        metricRecordStmt.setString(4, metric.getInstanceId());
        metricRecordStmt.setLong(5, currentTime);
        metricRecordStmt.setLong(6, metric.getStartTime());
        metricRecordStmt.setString(7, metric.getType());
        metricRecordStmt.setDouble(8, aggregates[0]);
        metricRecordStmt.setDouble(9, aggregates[1]);
        metricRecordStmt.setDouble(10, aggregates[2]);
        metricRecordStmt.setLong(11, (long) aggregates[3]);
        String json = TimelineUtils.dumpTimelineRecordtoJSON(metric.getMetricValues());
        metricRecordStmt.setString(12, json);

        try {
          metricRecordStmt.executeUpdate();
        } catch (SQLException sql) {
          LOG.error(sql);
        }
      }

      conn.commit();

    } finally {
      if (metricRecordStmt != null) {
        try {
          metricRecordStmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException sql) {
          // Ignore
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public TimelineMetrics getMetricRecords(
    final Condition condition, Map<String, List<Function>> metricFunctions)
    throws SQLException, IOException {

    validateConditionIsNotEmpty(condition);

    Connection conn = getConnection();
    PreparedStatement stmt = null;
    ResultSet rs = null;
    TimelineMetrics metrics = new TimelineMetrics();

    try {
      //get latest
      if(condition.isPointInTime()){
        getLatestMetricRecords(condition, conn, metrics);
      } else {
        stmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
        rs = stmt.executeQuery();
        while (rs.next()) {
          appendMetricFromResultSet(metrics, condition, metricFunctions, rs);
        }
      }

    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException sql) {
          // Ignore
        }
      }
    }

    LOG.debug("Metrics records size: " + metrics.getMetrics().size());
    return metrics;
  }

  private void appendMetricFromResultSet(
      TimelineMetrics metrics, Condition condition, Map<String,
      List<Function>> metricFunctions, ResultSet rs)
      throws SQLException, IOException {
    if (condition.getPrecision() == Precision.HOURS
      || condition.getPrecision() == Precision.MINUTES) {

      String metricName = rs.getString("METRIC_NAME");
      List<Function> functions = metricFunctions.get(metricName);

      for (Function f : functions) {
        SingleValuedTimelineMetric metric = getAggregatedTimelineMetricFromResultSet(rs, f);

        if (condition.isGrouped()) {
          metrics.addOrMergeTimelineMetric(metric);
        } else {
          metrics.getMetrics().add(metric.getTimelineMetric());
        }
      }
    }
    else {
      TimelineMetric metric;
      metric = TIMELINE_METRIC_READ_HELPER.getTimelineMetricFromResultSet(rs);

      if (condition.isGrouped()) {
        metrics.addOrMergeTimelineMetric(metric);
      } else {
        metrics.getMetrics().add(metric);
      }
    }
  }

  private void getLatestMetricRecords(
    Condition condition, Connection conn, TimelineMetrics metrics)
    throws SQLException, IOException {

    validateConditionIsNotEmpty(condition);

    PreparedStatement stmt;

    stmt = PhoenixTransactSQL.prepareGetLatestMetricSqlStmt(conn,
        condition);
    ResultSet rs = null;
    try {
      rs = stmt.executeQuery();
      while (rs.next()) {
        TimelineMetric metric = getLastTimelineMetricFromResultSet(rs);
        metrics.getMetrics().add(metric);
      }
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (stmt != null) {
        stmt.close();
      }
    }
  }

  /**
   * Get metrics aggregated across hosts.
   *
   * @param condition @Condition
   * @return @TimelineMetrics
   * @throws SQLException
   */
  public TimelineMetrics getAggregateMetricRecords(final Condition condition,
      Map<String, List<Function>> metricFunctions) throws SQLException {

    validateConditionIsNotEmpty(condition);

    Connection conn = getConnection();
    PreparedStatement stmt = null;
    ResultSet rs = null;
    TimelineMetrics metrics = new TimelineMetrics();

    try {
      //get latest
      if(condition.isPointInTime()) {
        getLatestAggregateMetricRecords(condition, conn, metrics, metricFunctions);
      } else {
        stmt = PhoenixTransactSQL.prepareGetAggregateSqlStmt(conn, condition);

        rs = stmt.executeQuery();
        while (rs.next()) {
          appendAggregateMetricFromResultSet(metrics, condition, metricFunctions, rs);
        }
      }
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException sql) {
          // Ignore
        }
      }
    }

    LOG.debug("Aggregate records size: " + metrics.getMetrics().size());
    return metrics;
  }

  private void appendAggregateMetricFromResultSet(TimelineMetrics metrics,
      Condition condition, Map<String, List<Function>> metricFunctions,
      ResultSet rs) throws SQLException {

    String metricName = rs.getString("METRIC_NAME");
    List<Function> functions = metricFunctions.get(metricName);

    for (Function aggregateFunction : functions) {
      SingleValuedTimelineMetric metric;

      if (condition.getPrecision() == Precision.HOURS
          || condition.getPrecision() == Precision.DAYS) {
        metric = getAggregateTimelineMetricFromResultSet(rs, aggregateFunction, false);
      } else {
        metric = getAggregateTimelineMetricFromResultSet(rs, aggregateFunction, true);
      }

      if (condition.isGrouped()) {
        metrics.addOrMergeTimelineMetric(metric);
      } else {
        metrics.getMetrics().add(metric.getTimelineMetric());
      }
    }
  }

  private void getLatestAggregateMetricRecords(Condition condition,
      Connection conn, TimelineMetrics metrics,
      Map<String, List<Function>> metricFunctions) throws SQLException {

    PreparedStatement stmt = null;
    SplitByMetricNamesCondition splitCondition =
      new SplitByMetricNamesCondition(condition);

    for (String metricName: splitCondition.getOriginalMetricNames()) {

      splitCondition.setCurrentMetric(metricName);
      stmt = PhoenixTransactSQL.prepareGetLatestAggregateMetricSqlStmt(conn, splitCondition);
      ResultSet rs = null;
      try {
        rs = stmt.executeQuery();
        while (rs.next()) {
          List<Function> functions = metricFunctions.get(metricName);
          if (functions != null) {
            for (Function f : functions) {
              SingleValuedTimelineMetric metric =
                getAggregateTimelineMetricFromResultSet(rs, f, true);

              if (condition.isGrouped()) {
                metrics.addOrMergeTimelineMetric(metric);
              } else {
                metrics.getMetrics().add(metric.getTimelineMetric());
              }
            }
          } else {
            SingleValuedTimelineMetric metric =
              getAggregateTimelineMetricFromResultSet(rs, new Function(), true);
            metrics.getMetrics().add(metric.getTimelineMetric());
          }
        }
      } finally {
        if (rs != null) {
          try {
            rs.close();
          } catch (SQLException e) {
            // Ignore
          }
        }
        if (stmt != null) {
          stmt.close();
        }        
      }
    }
  }

  private SingleValuedTimelineMetric getAggregateTimelineMetricFromResultSet(ResultSet rs,
      Function f, boolean useHostCount) throws SQLException {

    String countColumnName = "METRIC_COUNT";
    if (useHostCount) {
      countColumnName = "HOSTS_COUNT";
    }

    SingleValuedTimelineMetric metric = new SingleValuedTimelineMetric(
      rs.getString("METRIC_NAME") + f.getSuffix(),
      rs.getString("APP_ID"),
      rs.getString("INSTANCE_ID"),
      null,
      rs.getLong("SERVER_TIME"),
      rs.getLong("SERVER_TIME"),
      rs.getString("UNITS")
    );

    double value;
    switch(f.getReadFunction()){
      case AVG:
        value = rs.getDouble("METRIC_SUM") / rs.getInt(countColumnName);
        break;
      case MIN:
        value = rs.getDouble("METRIC_MIN");
        break;
      case MAX:
        value = rs.getDouble("METRIC_MAX");
        break;
      case SUM:
        value = rs.getDouble("METRIC_SUM");
        break;
      default:
        value = rs.getDouble("METRIC_SUM") / rs.getInt(countColumnName);
        break;
    }

    metric.setSingleTimeseriesValue(rs.getLong("SERVER_TIME"), value);

    return metric;
  }

  private void validateConditionIsNotEmpty(Condition condition) {
    if (condition.isEmpty()) {
      throw new IllegalArgumentException("No filter criteria specified.");
    }
  }

  public void saveHostAggregateRecords(Map<TimelineMetric, MetricHostAggregate> hostAggregateMap,
                                       String phoenixTableName) throws SQLException {

    if (hostAggregateMap == null || hostAggregateMap.isEmpty()) {
      LOG.debug("Empty aggregate records.");
      return;
    }

    Connection conn = getConnection();
    PreparedStatement stmt = null;

    long start = System.currentTimeMillis();
    int rowCount = 0;

    try {
      stmt = conn.prepareStatement(
        String.format(UPSERT_AGGREGATE_RECORD_SQL, phoenixTableName));

      for (Map.Entry<TimelineMetric, MetricHostAggregate> metricAggregate :
        hostAggregateMap.entrySet()) {

        TimelineMetric metric = metricAggregate.getKey();
        MetricHostAggregate hostAggregate = metricAggregate.getValue();

        rowCount++;
        stmt.clearParameters();
        stmt.setString(1, metric.getMetricName());
        stmt.setString(2, metric.getHostName());
        stmt.setString(3, metric.getAppId());
        stmt.setString(4, metric.getInstanceId());
        stmt.setLong(5, metric.getTimestamp());
        stmt.setString(6, metric.getType());
        stmt.setDouble(7, hostAggregate.getSum());
        stmt.setDouble(8, hostAggregate.getMax());
        stmt.setDouble(9, hostAggregate.getMin());
        stmt.setDouble(10, hostAggregate.getNumberOfSamples());

        try {
          stmt.executeUpdate();
        } catch (SQLException sql) {
          LOG.error(sql);
        }

        if (rowCount >= PHOENIX_MAX_MUTATION_STATE_SIZE - 1) {
          conn.commit();
          rowCount = 0;
        }

      }

      conn.commit();

    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException sql) {
          // Ignore
        }
      }
    }

    long end = System.currentTimeMillis();

    if ((end - start) > 60000l) {
      LOG.info("Time to save map: " + (end - start) + ", " +
        "thread = " + Thread.currentThread().getClass());
    }
  }

  /**
   * Save Metric aggregate records.
   *
   * @throws SQLException
   */
  public void saveClusterAggregateRecords(Map<TimelineClusterMetric, MetricClusterAggregate> records)
      throws SQLException {

    if (records == null || records.isEmpty()) {
      LOG.debug("Empty aggregate records.");
      return;
    }

    long start = System.currentTimeMillis();
    String sqlStr = String.format(UPSERT_CLUSTER_AGGREGATE_SQL, METRICS_CLUSTER_AGGREGATE_TABLE_NAME);
    Connection conn = getConnection();
    PreparedStatement stmt = null;
    try {
      stmt = conn.prepareStatement(sqlStr);
      int rowCount = 0;

      for (Map.Entry<TimelineClusterMetric, MetricClusterAggregate>
        aggregateEntry : records.entrySet()) {
        TimelineClusterMetric clusterMetric = aggregateEntry.getKey();
        MetricClusterAggregate aggregate = aggregateEntry.getValue();

        if (LOG.isTraceEnabled()) {
          LOG.trace("clusterMetric = " + clusterMetric + ", " +
            "aggregate = " + aggregate);
        }

        rowCount++;
        stmt.clearParameters();
        stmt.setString(1, clusterMetric.getMetricName());
        stmt.setString(2, clusterMetric.getAppId());
        stmt.setString(3, clusterMetric.getInstanceId());
        stmt.setLong(4, clusterMetric.getTimestamp());
        stmt.setString(5, clusterMetric.getType());
        stmt.setDouble(6, aggregate.getSum());
        stmt.setInt(7, aggregate.getNumberOfHosts());
        stmt.setDouble(8, aggregate.getMax());
        stmt.setDouble(9, aggregate.getMin());

        try {
          stmt.executeUpdate();
        } catch (SQLException sql) {
          // we have no way to verify it works!!!
          LOG.error(sql);
        }

        if (rowCount >= PHOENIX_MAX_MUTATION_STATE_SIZE - 1) {
          conn.commit();
          rowCount = 0;
        }
      }

      conn.commit();

    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException sql) {
          // Ignore
        }
      }
    }
    long end = System.currentTimeMillis();
    if ((end - start) > 60000l) {
      LOG.info("Time to save: " + (end - start) + ", " +
        "thread = " + Thread.currentThread().getName());
    }
  }


  /**
   * Save Metric aggregate records.
   *
   * @throws SQLException
   */
  public void saveClusterTimeAggregateRecords(Map<TimelineClusterMetric, MetricHostAggregate> records,
                                              String tableName) throws SQLException {
    if (records == null || records.isEmpty()) {
      LOG.debug("Empty aggregate records.");
      return;
    }

    long start = System.currentTimeMillis();

    Connection conn = getConnection();
    PreparedStatement stmt = null;
    try {
      stmt = conn.prepareStatement(String.format(UPSERT_CLUSTER_AGGREGATE_TIME_SQL, tableName));
      int rowCount = 0;

      for (Map.Entry<TimelineClusterMetric, MetricHostAggregate> aggregateEntry : records.entrySet()) {
        TimelineClusterMetric clusterMetric = aggregateEntry.getKey();
        MetricHostAggregate aggregate = aggregateEntry.getValue();

        if (LOG.isTraceEnabled()) {
          LOG.trace("clusterMetric = " + clusterMetric + ", " +
            "aggregate = " + aggregate);
        }

        rowCount++;
        stmt.clearParameters();
        stmt.setString(1, clusterMetric.getMetricName());
        stmt.setString(2, clusterMetric.getAppId());
        stmt.setString(3, clusterMetric.getInstanceId());
        stmt.setLong(4, clusterMetric.getTimestamp());
        stmt.setString(5, clusterMetric.getType());
        stmt.setDouble(6, aggregate.getSum());
        stmt.setLong(7, aggregate.getNumberOfSamples());
        stmt.setDouble(8, aggregate.getMax());
        stmt.setDouble(9, aggregate.getMin());

        try {
          stmt.executeUpdate();
        } catch (SQLException sql) {
          // we have no way to verify it works!!!
          LOG.error(sql);
        }

        if (rowCount >= PHOENIX_MAX_MUTATION_STATE_SIZE - 1) {
          conn.commit();
          rowCount = 0;
        }
      }

      conn.commit();

    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException sql) {
          // Ignore
        }
      }
    }
    long end = System.currentTimeMillis();
    if ((end - start) > 60000l) {
      LOG.info("Time to save: " + (end - start) + ", " +
        "thread = " + Thread.currentThread().getName());
    }
  }
}
