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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.DoNotRetryIOException;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.util.RetryCounter;
import org.apache.hadoop.hbase.util.RetryCounterFactory;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.SingleValuedTimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricClusterAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricHostAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricReadHelper;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultPhoenixDataSource;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixConnectionProvider;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.SplitByMetricNamesCondition;
import org.apache.hadoop.yarn.util.timeline.TimelineUtils;
import org.apache.phoenix.exception.PhoenixIOException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.AGGREGATE_TABLE_SPLIT_POINTS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HBASE_BLOCKING_STORE_FILES;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_TABLES_DURABILITY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_DAILY_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_HOUR_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_MINUTE_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_SECOND_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.GLOBAL_MAX_RETRIES;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.GLOBAL_RESULT_LIMIT;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.GLOBAL_RETRY_INTERVAL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HBASE_COMPRESSION_SCHEME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HBASE_ENCODING_SCHEME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_DAILY_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_HOUR_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_MINUTE_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.OUT_OFF_BAND_DATA_TIME_ALLOWANCE;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.PRECISION_TABLE_SPLIT_POINTS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.PRECISION_TABLE_TTL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.CREATE_HOSTED_APPS_METADATA_TABLE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.CREATE_METRICS_AGGREGATE_TABLE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.CREATE_METRICS_CLUSTER_AGGREGATE_GROUPED_TABLE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.CREATE_METRICS_CLUSTER_AGGREGATE_TABLE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.CREATE_METRICS_METADATA_TABLE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.CREATE_METRICS_TABLE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.DEFAULT_ENCODING;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.DEFAULT_TABLE_COMPRESSION;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_HOSTED_APPS_METADATA_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_METRIC_METADATA_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.PHOENIX_TABLES;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_AGGREGATE_RECORD_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_CLUSTER_AGGREGATE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_CLUSTER_AGGREGATE_TIME_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_HOSTED_APPS_METADATA_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_METADATA_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.UPSERT_METRICS_SQL;

/**
 * Provides a facade over the Phoenix API to access HBase schema
 */
public class PhoenixHBaseAccessor {
  private static final Log LOG = LogFactory.getLog(PhoenixHBaseAccessor.class);

  static final int PHOENIX_MAX_MUTATION_STATE_SIZE = 50000;
  // Default stale data allowance set to 3 minutes, 2 minutes more than time
  // it was collected. Also 2 minutes is the default aggregation interval at
  // cluster and host levels.
  static final long DEFAULT_OUT_OF_BAND_TIME_ALLOWANCE = 300000;
  /**
   * 22 metrics for 2hours in SECONDS (10 second data)
   * => Reasonable upper bound on the limit such that our Precision calculation for a given time range makes sense.
   */
  private static final int METRICS_PER_MINUTE = 22;
  private static final int POINTS_PER_MINUTE = 6;
  public static int RESULTSET_LIMIT = (int)TimeUnit.HOURS.toMinutes(2) * METRICS_PER_MINUTE * POINTS_PER_MINUTE ;

  private static final TimelineMetricReadHelper TIMELINE_METRIC_READ_HELPER = new TimelineMetricReadHelper();
  private static ObjectMapper mapper = new ObjectMapper();
  private static TypeReference<TreeMap<Long, Double>> metricValuesTypeRef = new TypeReference<TreeMap<Long, Double>>() {};

  private final Configuration hbaseConf;
  private final Configuration metricsConf;
  private final RetryCounterFactory retryCounterFactory;
  private final PhoenixConnectionProvider dataSource;
  private final long outOfBandTimeAllowance;
  private final String timelineMetricsTablesDurability;

  static final String HSTORE_COMPACTION_CLASS_KEY =
    "hbase.hstore.defaultengine.compactionpolicy.class";
  static final String FIFO_COMPACTION_POLICY_CLASS =
    "org.apache.hadoop.hbase.regionserver.compactions.FIFOCompactionPolicy";
  static final String DEFAULT_COMPACTION_POLICY_CLASS =
    "org.apache.hadoop.hbase.regionserver.compactions.ExploringCompactionPolicy";
  static final String BLOCKING_STORE_FILES_KEY =
    "hbase.hstore.blockingStoreFiles";

  private HashMap<String, String> tableTTL = new HashMap<>();

  public PhoenixHBaseAccessor(Configuration hbaseConf,
                              Configuration metricsConf){
    this(hbaseConf, metricsConf, new DefaultPhoenixDataSource(hbaseConf));
  }

  public PhoenixHBaseAccessor(Configuration hbaseConf,
                              Configuration metricsConf,
                              PhoenixConnectionProvider dataSource) {
    this.hbaseConf = hbaseConf;
    this.metricsConf = metricsConf;
    RESULTSET_LIMIT = metricsConf.getInt(GLOBAL_RESULT_LIMIT, RESULTSET_LIMIT);
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
    this.outOfBandTimeAllowance = metricsConf.getLong(OUT_OFF_BAND_DATA_TIME_ALLOWANCE,
      DEFAULT_OUT_OF_BAND_TIME_ALLOWANCE);
    this.timelineMetricsTablesDurability = metricsConf.get(TIMELINE_METRICS_TABLES_DURABILITY, "");

    tableTTL.put(METRICS_RECORD_TABLE_NAME, metricsConf.get(PRECISION_TABLE_TTL, String.valueOf(1 * 86400)));  // 1 day
    tableTTL.put(METRICS_AGGREGATE_MINUTE_TABLE_NAME, metricsConf.get(HOST_MINUTE_TABLE_TTL, String.valueOf(7 * 86400))); //7 days
    tableTTL.put(METRICS_AGGREGATE_HOURLY_TABLE_NAME, metricsConf.get(HOST_HOUR_TABLE_TTL, String.valueOf(30 * 86400))); //30 days
    tableTTL.put(METRICS_AGGREGATE_DAILY_TABLE_NAME, metricsConf.get(HOST_DAILY_TABLE_TTL, String.valueOf(365 * 86400))); //1 year
    tableTTL.put(METRICS_CLUSTER_AGGREGATE_TABLE_NAME, metricsConf.get(CLUSTER_SECOND_TABLE_TTL, String.valueOf(7 * 86400))); //7 days
    tableTTL.put(METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME, metricsConf.get(CLUSTER_MINUTE_TABLE_TTL, String.valueOf(30 * 86400))); //30 days
    tableTTL.put(METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME, metricsConf.get(CLUSTER_HOUR_TABLE_TTL, String.valueOf(365 * 86400))); //1 year
    tableTTL.put(METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME, metricsConf.get(CLUSTER_DAILY_TABLE_TTL, String.valueOf(730 * 86400))); //2 years
  }

  private static TimelineMetric getLastTimelineMetricFromResultSet(ResultSet rs)
      throws SQLException, IOException {
    TimelineMetric metric = TIMELINE_METRIC_READ_HELPER.getTimelineMetricCommonsFromResultSet(rs);
    metric.setMetricValues(readLastMetricValueFromJSON(rs.getString("METRICS")));
    return metric;
  }

  private static TreeMap<Long, Double> readLastMetricValueFromJSON(String json)
      throws IOException {
    TreeMap<Long, Double> values = readMetricFromJSON(json);
    Long lastTimeStamp = values.lastKey();

    TreeMap<Long, Double> valueMap = new TreeMap<Long, Double>();
    valueMap.put(lastTimeStamp, values.get(lastTimeStamp));
    return valueMap;
  }

  @SuppressWarnings("unchecked")
  public static TreeMap<Long, Double>  readMetricFromJSON(String json) throws IOException {
    return mapper.readValue(json, metricValuesTypeRef);
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

  /**
   * Unit test purpose only for now.
   * @return @HBaseAdmin
   * @throws IOException
   */
  HBaseAdmin getHBaseAdmin() throws IOException {
    return dataSource.getHBaseAdmin();
  }

  protected void initMetricSchema() {
    Connection conn = null;
    Statement stmt = null;

    String encoding = metricsConf.get(HBASE_ENCODING_SCHEME, DEFAULT_ENCODING);
    String compression = metricsConf.get(HBASE_COMPRESSION_SCHEME, DEFAULT_TABLE_COMPRESSION);


    try {
      LOG.info("Initializing metrics schema...");
      conn = getConnectionRetryingOnException();
      stmt = conn.createStatement();

      // Metadata
      String metadataSql = String.format(CREATE_METRICS_METADATA_TABLE_SQL,
        encoding, compression);
      stmt.executeUpdate(metadataSql);
      String hostedAppSql = String.format(CREATE_HOSTED_APPS_METADATA_TABLE_SQL,
        encoding, compression);
      stmt.executeUpdate(hostedAppSql);

      // Host level
      String precisionSql = String.format(CREATE_METRICS_TABLE_SQL,
        encoding, tableTTL.get(METRICS_RECORD_TABLE_NAME), compression);
      String splitPoints = metricsConf.get(PRECISION_TABLE_SPLIT_POINTS);
      if (!StringUtils.isEmpty(splitPoints)) {
        precisionSql += getSplitPointsStr(splitPoints);
      }
      stmt.executeUpdate(precisionSql);
      stmt.executeUpdate(String.format(CREATE_METRICS_AGGREGATE_TABLE_SQL,
        METRICS_AGGREGATE_MINUTE_TABLE_NAME, encoding,
        tableTTL.get(METRICS_AGGREGATE_MINUTE_TABLE_NAME),
        compression));
      stmt.executeUpdate(String.format(CREATE_METRICS_AGGREGATE_TABLE_SQL,
        METRICS_AGGREGATE_HOURLY_TABLE_NAME, encoding,
        tableTTL.get(METRICS_AGGREGATE_HOURLY_TABLE_NAME),
        compression));
      stmt.executeUpdate(String.format(CREATE_METRICS_AGGREGATE_TABLE_SQL,
        METRICS_AGGREGATE_DAILY_TABLE_NAME, encoding,
        tableTTL.get(METRICS_AGGREGATE_DAILY_TABLE_NAME),
        compression));

      // Cluster level
      String aggregateSql = String.format(CREATE_METRICS_CLUSTER_AGGREGATE_TABLE_SQL,
        METRICS_CLUSTER_AGGREGATE_TABLE_NAME, encoding,
        tableTTL.get(METRICS_CLUSTER_AGGREGATE_TABLE_NAME),
        compression);
      splitPoints = metricsConf.get(AGGREGATE_TABLE_SPLIT_POINTS);
      if (!StringUtils.isEmpty(splitPoints)) {
        aggregateSql += getSplitPointsStr(splitPoints);
      }
      stmt.executeUpdate(aggregateSql);
      stmt.executeUpdate(String.format(CREATE_METRICS_CLUSTER_AGGREGATE_GROUPED_TABLE_SQL,
        METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME, encoding,
        tableTTL.get(METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME),
        compression));
      stmt.executeUpdate(String.format(CREATE_METRICS_CLUSTER_AGGREGATE_GROUPED_TABLE_SQL,
        METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME, encoding,
        tableTTL.get(METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME),
        compression));
      stmt.executeUpdate(String.format(CREATE_METRICS_CLUSTER_AGGREGATE_GROUPED_TABLE_SQL,
        METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME, encoding,
        tableTTL.get(METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME),
        compression));


      conn.commit();

      LOG.info("Metrics schema initialized.");
    } catch (SQLException | InterruptedException sql) {
      LOG.error("Error creating Metrics Schema in HBase using Phoenix.", sql);
      throw new MetricsSystemInitializationException(
        "Error creating Metrics Schema in HBase using Phoenix.", sql);
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

  protected void initPoliciesAndTTL() {
    boolean enableNormalizer = hbaseConf.getBoolean("hbase.normalizer.enabled", true);
    boolean enableFifoCompaction = metricsConf.getBoolean("timeline.metrics.hbase.fifo.compaction.enabled", true);

    HBaseAdmin hBaseAdmin = null;
    try {
      hBaseAdmin = dataSource.getHBaseAdmin();
    } catch (IOException e) {
      LOG.warn("Unable to initialize HBaseAdmin for setting policies.", e);
    }

    if (hBaseAdmin != null) {
      for (String tableName : PHOENIX_TABLES) {
        try {
          boolean modifyTable = false;
          HTableDescriptor tableDescriptor = hBaseAdmin.getTableDescriptor(tableName.getBytes());

          if (enableNormalizer && !tableDescriptor.isNormalizationEnabled()) {
            tableDescriptor.setNormalizationEnabled(true);
            LOG.info("Enabling normalizer for " + tableName);
            modifyTable = true;
          }

          if (!timelineMetricsTablesDurability.isEmpty()) {
            LOG.info("Setting WAL option " + timelineMetricsTablesDurability + " for table : " + tableName);
            boolean validDurability = true;
            if ("SKIP_WAL".equals(timelineMetricsTablesDurability)) {
              tableDescriptor.setDurability(Durability.SKIP_WAL);
            } else if ("SYNC_WAL".equals(timelineMetricsTablesDurability)) {
              tableDescriptor.setDurability(Durability.SYNC_WAL);
            } else if ("ASYNC_WAL".equals(timelineMetricsTablesDurability)) {
              tableDescriptor.setDurability(Durability.ASYNC_WAL);
            } else if ("FSYNC_WAL".equals(timelineMetricsTablesDurability)) {
              tableDescriptor.setDurability(Durability.FSYNC_WAL);
            } else {
              LOG.info("Unknown value for " + TIMELINE_METRICS_TABLES_DURABILITY + " : " + timelineMetricsTablesDurability);
              validDurability = false;
            }
            if (validDurability) {
              modifyTable = true;
            }
          }

          Map<String, String> config = tableDescriptor.getConfiguration();
          if (enableFifoCompaction &&
             !FIFO_COMPACTION_POLICY_CLASS.equals(config.get(HSTORE_COMPACTION_CLASS_KEY))) {
            tableDescriptor.setConfiguration(HSTORE_COMPACTION_CLASS_KEY,
              FIFO_COMPACTION_POLICY_CLASS);
            LOG.info("Setting config property " + HSTORE_COMPACTION_CLASS_KEY +
              " = " + FIFO_COMPACTION_POLICY_CLASS + " for " + tableName);
            // Need to set blockingStoreFiles to 1000 for FIFO
            int blockingStoreFiles = hbaseConf.getInt(HBASE_BLOCKING_STORE_FILES, 1000);
            if (blockingStoreFiles < 1000) {
              blockingStoreFiles = 1000;
            }
            tableDescriptor.setConfiguration(BLOCKING_STORE_FILES_KEY, String.valueOf(blockingStoreFiles));
            LOG.info("Setting config property " + BLOCKING_STORE_FILES_KEY +
              " = " + blockingStoreFiles + " for " + tableName);
            modifyTable = true;
          }
          // Set back original policy if fifo disabled
          if (!enableFifoCompaction &&
             FIFO_COMPACTION_POLICY_CLASS.equals(config.get(HSTORE_COMPACTION_CLASS_KEY))) {
            tableDescriptor.setConfiguration(HSTORE_COMPACTION_CLASS_KEY,
              DEFAULT_COMPACTION_POLICY_CLASS);
            LOG.info("Setting config property " + HSTORE_COMPACTION_CLASS_KEY +
              " = " + DEFAULT_COMPACTION_POLICY_CLASS + " for " + tableName);

            int blockingStoreFiles = hbaseConf.getInt(HBASE_BLOCKING_STORE_FILES, 300);
            if (blockingStoreFiles > 300) {
              LOG.warn("HBase blocking store file set too high without FIFO " +
                "Compaction policy enabled, restoring low value = 300.");
              blockingStoreFiles = 300;
            }
            tableDescriptor.setConfiguration(BLOCKING_STORE_FILES_KEY, String.valueOf(blockingStoreFiles));
            LOG.info("Setting config property " + BLOCKING_STORE_FILES_KEY +
              " = " + blockingStoreFiles + " for " + tableName);
            modifyTable = true;
          }
          // Change TTL setting to match user configuration
          HColumnDescriptor[] columnFamilies = tableDescriptor.getColumnFamilies();
          if (columnFamilies != null) {
            for (HColumnDescriptor family : columnFamilies) {
              String ttlValue = family.getValue("TTL");
              if (StringUtils.isEmpty(ttlValue) ||
                  !ttlValue.trim().equals(tableTTL.get(tableName))) {
                family.setValue("TTL", tableTTL.get(tableName));
                LOG.info("Setting TTL on table: " + tableName + " to : " +
                  tableTTL.get(tableName) + " seconds.");
                modifyTable = true;
              }
            }
          }

          // Persist only if anything changed
          if (modifyTable) {
            hBaseAdmin.modifyTable(tableName.getBytes(), tableDescriptor);
          }

        } catch (IOException e) {
          LOG.error("Failed setting policies for " + tableName, e);
        }
      }
      try {
        hBaseAdmin.close();
      } catch (IOException e) {
        LOG.warn("Exception on HBaseAdmin close.", e);
      }
    }
  }

  protected String getSplitPointsStr(String splitPoints) {
    if (StringUtils.isEmpty(splitPoints.trim())) {
      return "";
    }
    String[] points = splitPoints.split(",");
    if (points.length > 0) {
      StringBuilder sb = new StringBuilder(" SPLIT ON ");
      sb.append("(");
      for (String point : points) {
        sb.append("'");
        sb.append(point.trim());
        sb.append("'");
        sb.append(",");
      }
      sb.deleteCharAt(sb.length() - 1);
      sb.append(")");
      return sb.toString();
    }
    return "";
  }

  public void insertMetricRecordsWithMetadata(TimelineMetricMetadataManager metadataManager,
                                              TimelineMetrics metrics) throws SQLException, IOException {
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
        if (Math.abs(currentTime - metric.getStartTime()) > outOfBandTimeAllowance) {
          // If timeseries start time is way in the past : discard
          LOG.debug("Discarding out of band timeseries, currentTime = "
            + currentTime + ", startTime = " + metric.getStartTime()
            + ", hostname = " + metric.getHostName());
          continue;
        }

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
        metricRecordStmt.setString(7, metric.getUnits());
        metricRecordStmt.setDouble(8, aggregates[0]);
        metricRecordStmt.setDouble(9, aggregates[1]);
        metricRecordStmt.setDouble(10, aggregates[2]);
        metricRecordStmt.setLong(11, (long) aggregates[3]);
        String json = TimelineUtils.dumpTimelineRecordtoJSON(metric.getMetricValues());
        metricRecordStmt.setString(12, json);

        try {
          metricRecordStmt.executeUpdate();

          // Write to metadata cache on successful write to store
          metadataManager.putIfModifiedTimelineMetricMetadata(
            metadataManager.getTimelineMetricMetadata(metric));

          metadataManager.putIfModifiedHostedAppsMetadata(
            metric.getHostName(), metric.getAppId());

        } catch (SQLException sql) {
          LOG.error("Failed on insert records to store.", sql);
        }
      }

      // commit() blocked if HBase unavailable
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

  public void insertMetricRecords(TimelineMetrics metrics) throws SQLException, IOException {
    insertMetricRecordsWithMetadata(null, metrics);
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
      if (condition.isPointInTime()){
        getLatestMetricRecords(condition, conn, metrics);
      } else {
        if (condition.getEndTime() >= condition.getStartTime()) {
          stmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
          rs = stmt.executeQuery();
          while (rs.next()) {
            appendMetricFromResultSet(metrics, condition, metricFunctions, rs);
          }
        } else {
          LOG.warn("Skipping metrics query because endTime < startTime");
        }
      }

    } catch (PhoenixIOException pioe) {
      Throwable pioe2 = pioe.getCause();
      // Need to find out if this is exception "Could not find hash cache
      // for joinId" or another PhoenixIOException
      if (pioe2 instanceof PhoenixIOException &&
        pioe2.getCause() instanceof DoNotRetryIOException) {
        String className = null;
        for (StackTraceElement ste : pioe2.getCause().getStackTrace()) {
          className = ste.getClassName();
        }

        if (className != null && className.equals("HashJoinRegionScanner")) {
          LOG.error("The cache might have expired and have been removed. Try to" +
            " increase the cache size by setting bigger value for " +
            "phoenix.coprocessor.maxMetaDataCacheSize in ams-hbase-site config." +
            " Falling back to sort-merge join algorithm.");
          PhoenixTransactSQL.setSortMergeJoinEnabled(true);
        }
      }
      throw pioe;
    } catch (RuntimeException ex) {
      // We need to find out if this is a real IO exception
      // or exception "maxStamp is smaller than minStamp"
      // which is thrown in hbase TimeRange.java
      Throwable io = ex.getCause();
      String className = null;
      if (io != null) {
        for (StackTraceElement ste : io.getStackTrace()) {
          className = ste.getClassName();
        }
      }
      if (className != null && className.equals("TimeRange")) {
        // This is "maxStamp is smaller than minStamp" exception
        // Log error and return empty metrics
        LOG.debug(io);
        return new TimelineMetrics();
      } else {
        throw ex;
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

  /**
   * Apply aggregate function to the result if supplied else get precision
   * or aggregate data with default function applied.
   */
  private void appendMetricFromResultSet(TimelineMetrics metrics, Condition condition,
                                         Map<String, List<Function>> metricFunctions,
                                         ResultSet rs) throws SQLException, IOException {
    String metricName = rs.getString("METRIC_NAME");
    List<Function> functions = metricFunctions.get(metricName);

    // Apply aggregation function if present
    if (functions != null && !functions.isEmpty()) {
      if (functions.size() > 1) {
        throw new IllegalArgumentException("Multiple aggregate functions not supported.");
      }
      for (Function f : functions) {
        if (f.getReadFunction() == Function.ReadFunction.VALUE) {
          getTimelineMetricsFromResultSet(metrics, f, condition, rs);
        } else {
          SingleValuedTimelineMetric metric =
            TIMELINE_METRIC_READ_HELPER.getAggregatedTimelineMetricFromResultSet(rs, f);

          if (condition.isGrouped()) {
            metrics.addOrMergeTimelineMetric(metric);
          } else {
            metrics.getMetrics().add(metric.getTimelineMetric());
          }
        }
      }
    } else {
      // No aggregation requested
      // Execution never goes here, function always contain at least 1 element
      getTimelineMetricsFromResultSet(metrics, null, condition, rs);
    }
  }

  private void getTimelineMetricsFromResultSet(TimelineMetrics metrics, Function f, Condition condition, ResultSet rs) throws SQLException, IOException {
    if (condition.getPrecision().equals(Precision.SECONDS)) {
      TimelineMetric metric = TIMELINE_METRIC_READ_HELPER.getTimelineMetricFromResultSet(rs);
      if (f != null && f.getSuffix() != null) { //Case : Requesting "._rate" for precision data
        metric.setMetricName(metric.getMetricName() + f.getSuffix());
      }
      if (condition.isGrouped()) {
        metrics.addOrMergeTimelineMetric(metric);
      } else {
        metrics.getMetrics().add(metric);
      }

    } else {
      SingleValuedTimelineMetric metric =
        TIMELINE_METRIC_READ_HELPER.getAggregatedTimelineMetricFromResultSet(rs, f);
      if (condition.isGrouped()) {
        metrics.addOrMergeTimelineMetric(metric);
      } else {
        metrics.getMetrics().add(metric.getTimelineMetric());
      }
    }
  }

  private void getLatestMetricRecords(Condition condition, Connection conn,
                                      TimelineMetrics metrics) throws SQLException, IOException {

    validateConditionIsNotEmpty(condition);

    PreparedStatement stmt = PhoenixTransactSQL.prepareGetLatestMetricSqlStmt(conn, condition);
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

      if (condition.getPrecision() == Precision.MINUTES
          || condition.getPrecision() == Precision.HOURS
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

  /**
   * One time save of metadata when discovering topology during aggregation.
   * @throws SQLException
   */
  public void saveHostAppsMetadata(Map<String, Set<String>> hostedApps) throws SQLException {
    Connection conn = getConnection();
    PreparedStatement stmt = null;
    try {
      stmt = conn.prepareStatement(UPSERT_HOSTED_APPS_METADATA_SQL);
      int rowCount = 0;

      for (Map.Entry<String, Set<String>> hostedAppsEntry : hostedApps.entrySet()) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("HostedAppsMetadata: " + hostedAppsEntry);
        }

        stmt.clearParameters();
        stmt.setString(1, hostedAppsEntry.getKey());
        stmt.setString(2, StringUtils.join(hostedAppsEntry.getValue(), ","));
        try {
          stmt.executeUpdate();
          rowCount++;
        } catch (SQLException sql) {
          LOG.error("Error saving hosted apps metadata.", sql);
        }
      }

      conn.commit();
      LOG.info("Saved " + rowCount + " hosted apps metadata records.");

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
  }

  /**
   * Save metdata on updates.
   * @param metricMetadata @Collection<@TimelineMetricMetadata>
   * @throws SQLException
   */
  public void saveMetricMetadata(Collection<TimelineMetricMetadata> metricMetadata) throws SQLException {
    if (metricMetadata.isEmpty()) {
      LOG.info("No metadata records to save.");
      return;
    }

    Connection conn = getConnection();
    PreparedStatement stmt = null;

    try {
      stmt = conn.prepareStatement(UPSERT_METADATA_SQL);
      int rowCount = 0;

      for (TimelineMetricMetadata metadata : metricMetadata) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("TimelineMetricMetadata: metricName = " + metadata.getMetricName()
            + ", appId = " + metadata.getAppId()
            + ", seriesStartTime = " + metadata.getSeriesStartTime()
          );
        }

        stmt.clearParameters();
        stmt.setString(1, metadata.getMetricName());
        stmt.setString(2, metadata.getAppId());
        stmt.setString(3, metadata.getUnits());
        stmt.setString(4, metadata.getType());
        stmt.setLong(5, metadata.getSeriesStartTime());
        stmt.setBoolean(6, metadata.isSupportsAggregates());

        try {
          stmt.executeUpdate();
          rowCount++;
        } catch (SQLException sql) {
          LOG.error("Error saving metadata.", sql);
        }
      }

      conn.commit();
      LOG.info("Saved " + rowCount + " metadata records.");

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
  }

  public Map<String, Set<String>> getHostedAppsMetadata() throws SQLException {
    Map<String, Set<String>> hostedAppMap = new HashMap<>();
    Connection conn = getConnection();
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      stmt = conn.prepareStatement(GET_HOSTED_APPS_METADATA_SQL);
      rs = stmt.executeQuery();

      while (rs.next()) {
        hostedAppMap.put(rs.getString("HOSTNAME"),
          new HashSet<>(Arrays.asList(StringUtils.split(rs.getString("APP_IDS"), ","))));
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

    return hostedAppMap;
  }

  // No filter criteria support for now.
  public Map<TimelineMetricMetadataKey, TimelineMetricMetadata> getTimelineMetricMetadata() throws SQLException {
    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> metadataMap = new HashMap<>();
    Connection conn = getConnection();
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      stmt = conn.prepareStatement(GET_METRIC_METADATA_SQL);
      rs = stmt.executeQuery();

      while (rs.next()) {
        String metricName = rs.getString("METRIC_NAME");
        String appId = rs.getString("APP_ID");
        TimelineMetricMetadata metadata = new TimelineMetricMetadata(
          metricName,
          appId,
          rs.getString("UNITS"),
          rs.getString("TYPE"),
          rs.getLong("START_TIME"),
          rs.getBoolean("SUPPORTS_AGGREGATION")
        );

        TimelineMetricMetadataKey key = new TimelineMetricMetadataKey(metricName, appId);
        metadata.setIsPersisted(true); // Always true on retrieval
        metadataMap.put(key, metadata);
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

    return metadataMap;
  }

}
