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
package org.apache.ambari.metrics.core.timeline;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.ambari.metrics.core.timeline.FunctionUtils.findMetricFunctions;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.AGGREGATORS_SKIP_BLOCK_CACHE;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.BLOCKING_STORE_FILES_KEY;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_MINUTE_SLEEP_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_TIMESLICE_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_DAILY_TABLE_TTL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_HOUR_TABLE_TTL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_MINUTE_TABLE_TTL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_SECOND_TABLE_TTL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CONTAINER_METRICS_TTL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.DATE_TIERED_COMPACTION_POLICY;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.FIFO_COMPACTION_POLICY_CLASS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.GLOBAL_MAX_RETRIES;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.GLOBAL_RESULT_LIMIT;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.GLOBAL_RETRY_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HBASE_COMPRESSION_SCHEME;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HBASE_ENCODING_SCHEME;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_MINUTE_SLEEP_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HSTORE_COMPACTION_CLASS_KEY;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HSTORE_ENGINE_CLASS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_EVENT_METRIC_PATTERNS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TRANSIENT_METRIC_PATTERNS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_DAILY_TABLE_TTL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_HOUR_TABLE_TTL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_MINUTE_TABLE_TTL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.METRICS_TRANSIENT_TABLE_TTL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.PRECISION_TABLE_TTL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_COMMIT_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_ENABLED;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_SIZE;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_AGGREGATOR_SINK_CLASS;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CONTAINER_METRICS_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CREATE_CONTAINER_METRICS_TABLE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CREATE_TRANSIENT_METRICS_TABLE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CREATE_METRICS_AGGREGATE_TABLE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CREATE_METRICS_CLUSTER_AGGREGATE_GROUPED_TABLE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CREATE_METRICS_CLUSTER_AGGREGATE_TABLE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CREATE_METRICS_TABLE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.DEFAULT_ENCODING;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.DEFAULT_TABLE_COMPRESSION;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.GET_HOSTED_APPS_METADATA_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.GET_INSTANCE_HOST_METADATA_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.GET_METRIC_METADATA_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.GET_METRIC_METADATA_SQL_V1;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRIC_TRANSIENT_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.PHOENIX_TABLES;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.PHOENIX_TABLES_REGEX_PATTERN;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.UPSERT_AGGREGATE_RECORD_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.UPSERT_CLUSTER_AGGREGATE_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.UPSERT_CLUSTER_AGGREGATE_TIME_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.UPSERT_CONTAINER_METRICS_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.UPSERT_HOSTED_APPS_METADATA_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.UPSERT_INSTANCE_HOST_METADATA_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.UPSERT_METADATA_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.UPSERT_METRICS_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.UPSERT_TRANSIENT_METRICS_SQL;
import static org.apache.ambari.metrics.core.timeline.source.InternalSourceProvider.SOURCE_NAME.RAW_METRICS;
import static org.apache.hadoop.metrics2.sink.timeline.TimelineMetricUtils.getJavaMetricPatterns;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.metrics.core.timeline.aggregators.AggregatorUtils;
import org.apache.ambari.metrics.core.timeline.aggregators.Function;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineMetricReadHelper;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricHostMetadata;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.ambari.metrics.core.timeline.query.Condition;
import org.apache.ambari.metrics.core.timeline.query.DefaultPhoenixDataSource;
import org.apache.ambari.metrics.core.timeline.query.PhoenixConnectionProvider;
import org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL;
import org.apache.ambari.metrics.core.timeline.query.SplitByMetricNamesCondition;
import org.apache.ambari.metrics.core.timeline.sink.ExternalMetricsSink;
import org.apache.ambari.metrics.core.timeline.sink.ExternalSinkProvider;
import org.apache.ambari.metrics.core.timeline.source.InternalMetricsSource;
import org.apache.ambari.metrics.core.timeline.source.InternalSourceProvider;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.DoNotRetryIOException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.RetryCounter;
import org.apache.hadoop.hbase.util.RetryCounterFactory;
import org.apache.hadoop.metrics2.sink.timeline.ContainerMetric;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.SingleValuedTimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.util.timeline.TimelineUtils;
import org.apache.phoenix.exception.PhoenixIOException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.google.common.collect.Multimap;


/**
 * Provides a facade over the Phoenix API to access HBase schema
 */
public class PhoenixHBaseAccessor {
  private static final Log LOG = LogFactory.getLog(PhoenixHBaseAccessor.class);

  static final int PHOENIX_MAX_MUTATION_STATE_SIZE = 50000;

  /**
   * 22 metrics for 2hours in SECONDS, 2 hosts (1 minute data records)
   * 22 * (2 * 60) * 2 = 5280
   * 22 cluster aggregate metrics for 2 hours (30 second data records)
   * 22 * (2 * 60 * 2) = 5280
   * => Reasonable upper bound on the limit such that our Precision calculation for a given time range makes sense.
   */
  public static int RESULTSET_LIMIT = 5760;

  public static int hostMinuteAggregatorDataInterval = 300;
  public static int clusterMinuteAggregatorDataInterval = 300;
  public static int clusterSecondAggregatorDataInterval = 30;

  static TimelineMetricReadHelper TIMELINE_METRIC_READ_HELPER = new TimelineMetricReadHelper();
  static ObjectMapper mapper = new ObjectMapper();
  static TypeReference<TreeMap<Long, Double>> metricValuesTypeRef = new TypeReference<TreeMap<Long, Double>>() {};

  private final Configuration hbaseConf;
  private final Configuration metricsConf;
  private final RetryCounterFactory retryCounterFactory;
  private final PhoenixConnectionProvider dataSource;
  private final int cacheSize;
  private final boolean cacheEnabled;
  private final BlockingQueue<TimelineMetrics> insertCache;
  private ScheduledExecutorService scheduledExecutorService;
  private MetricsCacheCommitterThread metricsCommiterThread;
  private TimelineMetricsAggregatorSink aggregatorSink;
  private final int cacheCommitInterval;
  private final boolean skipBlockCacheForAggregatorsEnabled;
  private TimelineMetricMetadataManager metadataManagerInstance;
  private Set<String> eventMetricPatterns = new HashSet<>();

  private Map<String, Integer> tableTTL = new HashMap<>();

  private final TimelineMetricConfiguration configuration;
  private List<InternalMetricsSource> rawMetricsSources = new ArrayList<>();

  public PhoenixHBaseAccessor(PhoenixConnectionProvider dataSource) {
    this(TimelineMetricConfiguration.getInstance(), dataSource);
  }

  // Test friendly construction since mock instrumentation is difficult to get
  // working with hadoop mini cluster
  PhoenixHBaseAccessor(TimelineMetricConfiguration configuration, PhoenixConnectionProvider dataSource) {
    this.configuration = TimelineMetricConfiguration.getInstance();
    try {
      this.hbaseConf = configuration.getHbaseConf();
      this.metricsConf = configuration.getMetricsConf();
    } catch (Exception e) {
      throw new ExceptionInInitializerError("Cannot initialize configuration.");
    }
    if (dataSource == null) {
      dataSource = new DefaultPhoenixDataSource(hbaseConf);
    }
    this.dataSource = dataSource;

    RESULTSET_LIMIT = metricsConf.getInt(GLOBAL_RESULT_LIMIT, RESULTSET_LIMIT);
    clusterSecondAggregatorDataInterval = metricsConf.getInt(CLUSTER_AGGREGATOR_TIMESLICE_INTERVAL, 30);
    hostMinuteAggregatorDataInterval = metricsConf.getInt(HOST_AGGREGATOR_MINUTE_SLEEP_INTERVAL, 300);
    clusterMinuteAggregatorDataInterval = metricsConf.getInt(CLUSTER_AGGREGATOR_MINUTE_SLEEP_INTERVAL, 300);

    try {
      Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
    } catch (ClassNotFoundException e) {
      LOG.error("Phoenix client jar not found in the classpath.", e);
      throw new IllegalStateException(e);
    }

    this.retryCounterFactory = new RetryCounterFactory(metricsConf.getInt(GLOBAL_MAX_RETRIES, 10),
      (int) SECONDS.toMillis(metricsConf.getInt(GLOBAL_RETRY_INTERVAL, 3)));
    this.cacheEnabled = Boolean.valueOf(metricsConf.get(TIMELINE_METRICS_CACHE_ENABLED, "true"));
    this.cacheSize = Integer.valueOf(metricsConf.get(TIMELINE_METRICS_CACHE_SIZE, "150"));
    this.cacheCommitInterval = Integer.valueOf(metricsConf.get(TIMELINE_METRICS_CACHE_COMMIT_INTERVAL, "3"));
    this.insertCache = new ArrayBlockingQueue<TimelineMetrics>(cacheSize);
    this.skipBlockCacheForAggregatorsEnabled = metricsConf.getBoolean(AGGREGATORS_SKIP_BLOCK_CACHE, false);

    String eventMetricPatternStrings = metricsConf.get(TIMELINE_METRICS_EVENT_METRIC_PATTERNS, StringUtils.EMPTY);
    eventMetricPatterns.addAll(getJavaMetricPatterns(eventMetricPatternStrings));

    tableTTL.put(METRICS_RECORD_TABLE_NAME, metricsConf.getInt(PRECISION_TABLE_TTL, 1 * 86400));  // 1 day
    tableTTL.put(CONTAINER_METRICS_TABLE_NAME, metricsConf.getInt(CONTAINER_METRICS_TTL, 14 * 86400));  // 30 days
    tableTTL.put(METRICS_AGGREGATE_MINUTE_TABLE_NAME, metricsConf.getInt(HOST_MINUTE_TABLE_TTL, 7 * 86400)); //7 days
    tableTTL.put(METRICS_AGGREGATE_HOURLY_TABLE_NAME, metricsConf.getInt(HOST_HOUR_TABLE_TTL, 30 * 86400)); //30 days
    tableTTL.put(METRICS_AGGREGATE_DAILY_TABLE_NAME, metricsConf.getInt(HOST_DAILY_TABLE_TTL, 365 * 86400)); //1 year
    tableTTL.put(METRICS_CLUSTER_AGGREGATE_TABLE_NAME, metricsConf.getInt(CLUSTER_SECOND_TABLE_TTL, 7 * 86400)); //7 days
    tableTTL.put(METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME, metricsConf.getInt(CLUSTER_MINUTE_TABLE_TTL, 30 * 86400)); //30 days
    tableTTL.put(METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME, metricsConf.getInt(CLUSTER_HOUR_TABLE_TTL, 365 * 86400)); //1 year
    tableTTL.put(METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME, metricsConf.getInt(CLUSTER_DAILY_TABLE_TTL, 730 * 86400)); //2 years
    tableTTL.put(METRIC_TRANSIENT_TABLE_NAME, metricsConf.getInt(METRICS_TRANSIENT_TABLE_TTL, 7 * 86400)); //7 days

    if (cacheEnabled) {
      LOG.debug("Initialising and starting metrics cache committer thread...");
      metricsCommiterThread = new MetricsCacheCommitterThread(this);
      scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
      scheduledExecutorService.scheduleWithFixedDelay(metricsCommiterThread, 0, cacheCommitInterval, TimeUnit.SECONDS);
    }

    Class<? extends TimelineMetricsAggregatorSink> metricSinkClass =
        metricsConf.getClass(TIMELINE_METRIC_AGGREGATOR_SINK_CLASS, null,
            TimelineMetricsAggregatorSink.class);
    if (metricSinkClass != null) {
      aggregatorSink = ReflectionUtils.newInstance(metricSinkClass, metricsConf);
      LOG.info("Initialized aggregator sink class " + metricSinkClass);
    }

    List<ExternalSinkProvider> externalSinkProviderList = configuration.getExternalSinkProviderList();
    InternalSourceProvider internalSourceProvider = configuration.getInternalSourceProvider();
    if (!externalSinkProviderList.isEmpty()) {
      for (ExternalSinkProvider externalSinkProvider : externalSinkProviderList) {
        ExternalMetricsSink rawMetricsSink = externalSinkProvider.getExternalMetricsSink(RAW_METRICS);
        int interval = configuration.getExternalSinkInterval(externalSinkProvider.getClass().getSimpleName(), RAW_METRICS);
        if (interval == -1) {
          interval = cacheCommitInterval;
        }
        rawMetricsSources.add(internalSourceProvider.getInternalMetricsSource(RAW_METRICS, interval, rawMetricsSink));
      }
    }
    TIMELINE_METRIC_READ_HELPER = new TimelineMetricReadHelper(this.metadataManagerInstance);
  }

  public boolean isInsertCacheEmpty() {
    return insertCache.isEmpty();
  }

  public void commitMetricsFromCache() {
    LOG.debug("Clearing metrics cache");
    List<TimelineMetrics> metricsList = new ArrayList<TimelineMetrics>(insertCache.size());
    if (!insertCache.isEmpty()) {
      insertCache.drainTo(metricsList); // More performant than poll
    }
    if (metricsList.size() > 0) {
      commitMetrics(metricsList);
      if (!rawMetricsSources.isEmpty()) {
        for (InternalMetricsSource rawMetricsSource : rawMetricsSources) {
          rawMetricsSource.publishTimelineMetrics(metricsList);
        }
      }
    }
  }

  public void commitMetrics(TimelineMetrics timelineMetrics) {
    commitMetrics(Collections.singletonList(timelineMetrics));
  }

  public void commitMetrics(Collection<TimelineMetrics> timelineMetricsCollection) {
    LOG.debug("Committing metrics to store");
    Connection conn = null;
    PreparedStatement metricRecordStmt = null;
    List<TimelineMetric> transientMetrics = new ArrayList<>();
    int rowCount = 0;

    try {
      conn = getConnection();
      metricRecordStmt = conn.prepareStatement(String.format(
              UPSERT_METRICS_SQL, METRICS_RECORD_TABLE_NAME));
      for (TimelineMetrics timelineMetrics : timelineMetricsCollection) {
        for (TimelineMetric metric : timelineMetrics.getMetrics()) {

          if (metadataManagerInstance.isTransientMetric(metric.getMetricName(), metric.getAppId())) {
            transientMetrics.add(metric);
            continue;
          }
          metricRecordStmt.clearParameters();

          if (LOG.isTraceEnabled()) {
            LOG.trace("host: " + metric.getHostName() + ", " +
                    "metricName = " + metric.getMetricName() + ", " +
                    "values: " + metric.getMetricValues());
          }
          double[] aggregates = AggregatorUtils.calculateAggregates(
                  metric.getMetricValues());

          if (aggregates[3] != 0.0) {
            rowCount++;
            byte[] uuid = metadataManagerInstance.getUuid(metric, true);
            if (uuid == null) {
              LOG.error("Error computing UUID for metric. Cannot write metrics : " + metric.toString());
              continue;
            }
            metricRecordStmt.setBytes(1, uuid);
            metricRecordStmt.setLong(2, metric.getStartTime());
            metricRecordStmt.setDouble(3, aggregates[0]);
            metricRecordStmt.setDouble(4, aggregates[1]);
            metricRecordStmt.setDouble(5, aggregates[2]);
            metricRecordStmt.setLong(6, (long) aggregates[3]);
            String json = TimelineUtils.dumpTimelineRecordtoJSON(metric.getMetricValues());
            metricRecordStmt.setString(7, json);

            try {
              int rows = metricRecordStmt.executeUpdate();
            } catch (SQLException | NumberFormatException ex) {
              LOG.warn("Failed on insert records to store : " + ex.getMessage());
              LOG.warn("Metric that cannot be stored : [" + metric.getMetricName() + "," + metric.getAppId() + "]" +
                metric.getMetricValues().toString());
              continue;
            }

            if (rowCount >= PHOENIX_MAX_MUTATION_STATE_SIZE - 1) {
              conn.commit();
              rowCount = 0;
            }

          } else {
            LOG.debug("Discarding empty metric record for : [" + metric.getMetricName() + "," +
              metric.getAppId() + "," +
              metric.getHostName() + "," +
              metric.getInstanceId() + "]");
          }

        }
      }
      if (CollectionUtils.isNotEmpty(transientMetrics)) {
        commitTransientMetrics(conn, transientMetrics);
      }

      // commit() blocked if HBase unavailable
      conn.commit();
    } catch (Exception exception){
      exception.printStackTrace();
    }
    finally {
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

  private void commitTransientMetrics(Connection conn, Collection<TimelineMetric> transientMetrics) throws SQLException, IOException {
    LOG.debug("Committing transient metrics to store");
    PreparedStatement metricTransientRecordStmt = null;

    metricTransientRecordStmt = conn.prepareStatement(String.format(
      UPSERT_TRANSIENT_METRICS_SQL, METRIC_TRANSIENT_TABLE_NAME));
    for (TimelineMetric metric : transientMetrics) {

      metricTransientRecordStmt.clearParameters();

      if (LOG.isTraceEnabled()) {
        LOG.trace("host: " + metric.getHostName() + ", " +
          "metricName = " + metric.getMetricName() + ", " +
          "values: " + metric.getMetricValues());
      }
      double[] aggregates = AggregatorUtils.calculateAggregates(
        metric.getMetricValues());

      metricTransientRecordStmt.setString(1, metric.getMetricName());
      metricTransientRecordStmt.setString(2, metric.getHostName());
      metricTransientRecordStmt.setString(3, metric.getAppId());
      metricTransientRecordStmt.setString(4, metric.getInstanceId());
      metricTransientRecordStmt.setLong(5, metric.getStartTime());
      metricTransientRecordStmt.setString(6, metric.getUnits());
      metricTransientRecordStmt.setDouble(7, aggregates[0]);
      metricTransientRecordStmt.setDouble(8, aggregates[1]);
      metricTransientRecordStmt.setDouble(9, aggregates[2]);
      metricTransientRecordStmt.setLong(10, (long) aggregates[3]);
      String json = TimelineUtils.dumpTimelineRecordtoJSON(metric.getMetricValues());
      metricTransientRecordStmt.setString(11, json);

      try {
        metricTransientRecordStmt.executeUpdate();
      } catch (SQLException sql) {
        LOG.error("Failed on inserting transient metric records to store.", sql);
      }
    }
  }


  private static TimelineMetric getLastTimelineMetricFromResultSet(ResultSet rs) throws SQLException, IOException {
    TimelineMetric metric = TIMELINE_METRIC_READ_HELPER.getTimelineMetricCommonsFromResultSet(rs);
    metric.setMetricValues(readLastMetricValueFromJSON(rs.getString("METRICS")));
    return metric;
  }

  private static TreeMap<Long, Double> readLastMetricValueFromJSON(String json) throws IOException {
    TreeMap<Long, Double> values = readMetricFromJSON(json);
    Long lastTimeStamp = values.lastKey();

    TreeMap<Long, Double> valueMap = new TreeMap<Long, Double>();
    valueMap.put(lastTimeStamp, values.get(lastTimeStamp));
    return valueMap;
  }

  @SuppressWarnings("unchecked")
  public static TreeMap<Long, Double> readMetricFromJSON(String json) throws IOException {
    return mapper.readValue(json, metricValuesTypeRef);
  }

  public Connection getConnectionRetryingOnException() throws SQLException, InterruptedException {
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
  Admin getHBaseAdmin() throws IOException {
    return dataSource.getHBaseAdmin();
  }

  protected void initMetricSchema() {
    Connection conn = null;
    Statement stmt = null;
    PreparedStatement pStmt = null;
    TimelineMetricSplitPointComputer splitPointComputer = new TimelineMetricSplitPointComputer(
      metricsConf, hbaseConf, metadataManagerInstance);
    splitPointComputer.computeSplitPoints();

    String encoding = metricsConf.get(HBASE_ENCODING_SCHEME, DEFAULT_ENCODING);
    String compression = metricsConf.get(HBASE_COMPRESSION_SCHEME, DEFAULT_TABLE_COMPRESSION);


    try {
      LOG.info("Initializing metrics schema...");
      conn = getConnectionRetryingOnException();
      stmt = conn.createStatement();

      // Container Metrics
      stmt.executeUpdate( String.format(CREATE_CONTAINER_METRICS_TABLE_SQL,
        encoding, tableTTL.get(CONTAINER_METRICS_TABLE_NAME), compression));

      // Host level
      String precisionSql = String.format(CREATE_METRICS_TABLE_SQL,
        encoding, tableTTL.get(METRICS_RECORD_TABLE_NAME), compression);
      pStmt = prepareCreateMetricsTableStatement(conn, precisionSql, splitPointComputer.getPrecisionSplitPoints());
      pStmt.executeUpdate();

      String hostMinuteAggregrateSql = String.format(CREATE_METRICS_AGGREGATE_TABLE_SQL,
        METRICS_AGGREGATE_MINUTE_TABLE_NAME, encoding,
        tableTTL.get(METRICS_AGGREGATE_MINUTE_TABLE_NAME),
        compression);
      pStmt = prepareCreateMetricsTableStatement(conn, hostMinuteAggregrateSql, splitPointComputer.getHostAggregateSplitPoints());
      pStmt.executeUpdate();

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
      pStmt = prepareCreateMetricsTableStatement(conn, aggregateSql, splitPointComputer.getClusterAggregateSplitPoints());
      pStmt.executeUpdate();

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

      // Metrics Transient Table
      String transientMetricPatterns = metricsConf.get(TRANSIENT_METRIC_PATTERNS, StringUtils.EMPTY);
      if (StringUtils.isNotEmpty(transientMetricPatterns)) {
        String transientMetricsTableSql = String.format(CREATE_TRANSIENT_METRICS_TABLE_SQL,
          encoding, tableTTL.get(METRIC_TRANSIENT_TABLE_NAME), compression);
        int row = stmt.executeUpdate(transientMetricsTableSql);
      }

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
      if (pStmt != null) {
        try {
          pStmt.close();
        } catch (Exception e) {
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

  boolean initPoliciesAndTTL() {
    boolean modifyAnyTable = false;
    Admin hBaseAdmin = null;
    try {
      hBaseAdmin = dataSource.getHBaseAdmin();
    } catch (IOException e) {
      LOG.warn("Unable to initialize HBaseAdmin for setting policies.", e);
    }

    TableName[] tableNames = null;
    TableName[] containerMetricsTableName = null;

    if (hBaseAdmin != null) {
      try {
        tableNames = hBaseAdmin.listTableNames(PHOENIX_TABLES_REGEX_PATTERN, false);
        containerMetricsTableName = hBaseAdmin.listTableNames(CONTAINER_METRICS_TABLE_NAME, false);
        tableNames = (TableName[]) ArrayUtils.addAll(tableNames, containerMetricsTableName);
      } catch (IOException e) {
        LOG.warn("Unable to get table names from HBaseAdmin for setting policies.", e);
        return false;
      }
      if (tableNames == null || tableNames.length == 0) {
        LOG.warn("Unable to get table names from HBaseAdmin for setting policies.");
        return false;
      }
      for (String tableName : PHOENIX_TABLES) {
        try {
          boolean modifyTable = false;
          Optional<TableName> tableNameOptional = Arrays.stream(tableNames)
            .filter(t -> tableName.equals(t.getNameAsString())).findFirst();

          TableDescriptor tableDescriptor = null;
          if (tableNameOptional.isPresent()) {
            tableDescriptor = hBaseAdmin.getTableDescriptor(tableNameOptional.get());
          }

          if (tableDescriptor == null) {
            LOG.warn("Unable to get table descriptor for " + tableName);
            continue;
          }

          // @TableDescriptor is immutable by design
          TableDescriptorBuilder tableDescriptorBuilder =
            TableDescriptorBuilder.newBuilder(tableDescriptor);

          //Set normalizer preferences
          boolean enableNormalizer = hbaseConf.getBoolean("hbase.normalizer.enabled", false);
          if (enableNormalizer ^ tableDescriptor.isNormalizationEnabled()) {
            tableDescriptorBuilder.setNormalizationEnabled(enableNormalizer);
            LOG.info("Normalizer set to " + enableNormalizer + " for " + tableName);
            modifyTable = true;
          }

          //Set durability preferences
          boolean durabilitySettingsModified = setDurabilityForTable(tableName, tableDescriptorBuilder, tableDescriptor);
          modifyTable = modifyTable || durabilitySettingsModified;

          //Set compaction policy preferences
          boolean compactionPolicyModified = false;
          compactionPolicyModified = setCompactionPolicyForTable(tableName, tableDescriptorBuilder, tableDescriptor);
          modifyTable = modifyTable || compactionPolicyModified;

          // Change TTL setting to match user configuration
          ColumnFamilyDescriptor[] columnFamilyDescriptors = tableDescriptor.getColumnFamilies();
          if (columnFamilyDescriptors != null) {
            for (ColumnFamilyDescriptor familyDescriptor : columnFamilyDescriptors) {
              int ttlValue = familyDescriptor.getTimeToLive();
              if (ttlValue != tableTTL.get(tableName)) {
                ColumnFamilyDescriptorBuilder familyDescriptorBuilder =
                  ColumnFamilyDescriptorBuilder.newBuilder(familyDescriptor);

                familyDescriptorBuilder.setTimeToLive(tableTTL.get(tableName));

                LOG.info("Setting TTL on table: " + tableName + " to : " +
                  tableTTL.get(tableName) + " seconds.");

                hBaseAdmin.modifyColumnFamily(tableNameOptional.get(), familyDescriptorBuilder.build());
                modifyTable = true;
              }
            }
          }

          // Persist only if anything changed
          if (modifyTable) {
            modifyAnyTable = modifyTable;
            hBaseAdmin.modifyTable(tableNameOptional.get(), tableDescriptorBuilder.build());
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
    return modifyAnyTable;
  }

  private boolean setDurabilityForTable(String tableName, TableDescriptorBuilder tableDescriptorBuilder, TableDescriptor tableDescriptor) {
    String tableDurability = metricsConf.get("timeline.metrics." + tableName + ".durability", "");

    if (StringUtils.isEmpty(tableDurability) || tableDescriptor.getDurability().toString().equals(tableDurability)) {
      return false;
    }

    if (StringUtils.isNotEmpty(tableDurability)) {
      LOG.info("Setting WAL option " + tableDurability + " for table : " + tableName);
      boolean validDurability = true;
      if ("SKIP_WAL".equals(tableDurability)) {
        tableDescriptorBuilder.setDurability(Durability.SKIP_WAL);
      } else if ("SYNC_WAL".equals(tableDurability)) {
        tableDescriptorBuilder.setDurability(Durability.SYNC_WAL);
      } else if ("ASYNC_WAL".equals(tableDurability)) {
        tableDescriptorBuilder.setDurability(Durability.ASYNC_WAL);
      } else if ("FSYNC_WAL".equals(tableDurability)) {
        tableDescriptorBuilder.setDurability(Durability.FSYNC_WAL);
      } else {
        LOG.info("Unknown value for durability : " + tableDurability);
        validDurability = false;
      }
      return validDurability;
    }
    return false;
  }


  private boolean setCompactionPolicyForTable(String tableName,
                                              TableDescriptorBuilder tableDescriptorBuilder,
                                              TableDescriptor tableDescriptor) {

    boolean modifyTable = false;

    String keyConfig = "timeline.metrics." + tableName + ".compaction.policy.key";
    String policyConfig = "timeline.metrics." + tableName + ".compaction.policy";
    String storeFilesConfig = "timeline.metrics." + tableName + ".blocking.store.files";

    String compactionPolicyKey = metricsConf.get(keyConfig, HSTORE_ENGINE_CLASS);
    String compactionPolicyClass = metricsConf.get(policyConfig, DATE_TIERED_COMPACTION_POLICY);
    int blockingStoreFiles = hbaseConf.getInt(storeFilesConfig, 60);

    if (tableName.equals(METRICS_RECORD_TABLE_NAME)) {
      compactionPolicyKey = metricsConf.get(keyConfig, HSTORE_COMPACTION_CLASS_KEY);
      compactionPolicyClass = metricsConf.get(policyConfig, FIFO_COMPACTION_POLICY_CLASS);
      blockingStoreFiles = hbaseConf.getInt(storeFilesConfig, 1000);
    }

    if (!compactionPolicyClass.equals(tableDescriptor.getValue(compactionPolicyKey))) {
      tableDescriptorBuilder.setValue(compactionPolicyKey, compactionPolicyClass);
      setHbaseBlockingStoreFiles(tableDescriptorBuilder, tableDescriptor, tableName, blockingStoreFiles);
      modifyTable = true;
      LOG.info("Setting compaction policy for " + tableName + ", " + compactionPolicyKey + "=" + compactionPolicyClass);
    }

    if (!compactionPolicyKey.equals(HSTORE_ENGINE_CLASS)) {
      tableDescriptorBuilder.removeValue(HSTORE_ENGINE_CLASS.getBytes());
    }
    if (!compactionPolicyKey.equals(HSTORE_COMPACTION_CLASS_KEY)) {
      tableDescriptorBuilder.removeValue(HSTORE_COMPACTION_CLASS_KEY.getBytes());
    }

    return modifyTable;
  }

  private boolean setHbaseBlockingStoreFiles(TableDescriptorBuilder tableDescriptorBuilder,
                                             TableDescriptor tableDescriptor, String tableName, int value) {
    if (!String.valueOf(value).equals(tableDescriptor.getValue(BLOCKING_STORE_FILES_KEY))) {
      tableDescriptorBuilder.setValue(BLOCKING_STORE_FILES_KEY, String.valueOf(value));
      LOG.info("Setting config property " + BLOCKING_STORE_FILES_KEY +
        " = " + value + " for " + tableName);
      return true;
    }
    return false;
  }


  private PreparedStatement prepareCreateMetricsTableStatement(Connection connection,
                                                               String sql,
                                                               List<byte[]> splitPoints) throws SQLException {

    String createTableWithSplitPointsSql = sql + getSplitPointsStr(splitPoints.size());
    LOG.debug(createTableWithSplitPointsSql);
    PreparedStatement statement = connection.prepareStatement(createTableWithSplitPointsSql);
    for (int i = 1; i <= splitPoints.size(); i++) {
      statement.setBytes(i, splitPoints.get(i - 1));
    }
    return statement;
  }

  private String getSplitPointsStr(int numSplits) {
    if (numSplits <= 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder(" SPLIT ON ");
    sb.append("(");
    for (int i = 0; i < numSplits; i++) {
      sb.append("?");
      sb.append(",");
    }
    sb.deleteCharAt(sb.length() - 1);
    sb.append(")");
    return sb.toString();
  }

  /**
   * Insert precision YARN container data.
   */
  public void insertContainerMetrics(List<ContainerMetric> metrics) throws SQLException, IOException {
    Connection conn = getConnection();
    PreparedStatement metricRecordStmt = null;

    try {
      metricRecordStmt = conn.prepareStatement(
          String.format(UPSERT_CONTAINER_METRICS_SQL, CONTAINER_METRICS_TABLE_NAME));
      for (ContainerMetric metric : metrics) {
        metricRecordStmt.clearParameters();
        metricRecordStmt.setString(1, ContainerId.fromString(metric.getContainerId())
            .getApplicationAttemptId().getApplicationId().toString());
        metricRecordStmt.setString(2, metric.getContainerId());
        metricRecordStmt.setTimestamp(3, new Timestamp(metric.getStartTime()));
        metricRecordStmt.setTimestamp(4, new Timestamp(metric.getFinishTime()));
        metricRecordStmt.setLong(5, metric.getFinishTime() - metric.getStartTime());
        metricRecordStmt.setString(6, metric.getHostName());
        metricRecordStmt.setInt(7, metric.getExitCode());
        metricRecordStmt.setLong(8, metric.getLocalizationDuration());
        metricRecordStmt.setLong(9, metric.getLaunchDuration());
        metricRecordStmt.setDouble(10, (double) metric.getPmemLimit() / 1024);
        metricRecordStmt.setDouble(11,
            ((double) metric.getPmemLimit() / 1024) * (metric.getFinishTime()
                - metric.getStartTime()));
        metricRecordStmt.setDouble(12, (double) metric.getVmemLimit() / 1024);
        metricRecordStmt.setDouble(13, (double) metric.getPmemUsedMin() / 1024);
        metricRecordStmt.setDouble(14, (double) metric.getPmemUsedMax() / 1024);
        metricRecordStmt.setDouble(15, (double) metric.getPmemUsedAvg() / 1024);
        metricRecordStmt.setDouble(16, (double) metric.getPmem50Pct() / 1024);
        metricRecordStmt.setDouble(17, (double) metric.getPmem75Pct() / 1024);
        metricRecordStmt.setDouble(18, (double) metric.getPmem90Pct() / 1024);
        metricRecordStmt.setDouble(19, (double) metric.getPmem95Pct()/ 1024);
        metricRecordStmt.setDouble(20, (double) metric.getPmem99Pct() / 1024);
        metricRecordStmt.setDouble(21, (double) metric.getPmemLimit() / 1024
            - (double) metric.getPmemUsedMax() / 1024);
        metricRecordStmt.setDouble(22, ((double) metric.getPmemLimit() / 1024
            - (double) metric.getPmemUsedMax() / 1024) * (metric.getFinishTime()
            - metric.getStartTime()));

        try {
          metricRecordStmt.executeUpdate();
        } catch (SQLException sql) {
          LOG.error("Failed on insert records to store.", sql);
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

  /**
   * Insert precision data.
   */
  public void insertMetricRecordsWithMetadata(TimelineMetricMetadataManager metadataManager,
                                              TimelineMetrics metrics, boolean skipCache) throws SQLException, IOException {
    List<TimelineMetric> timelineMetrics = metrics.getMetrics();
    if (timelineMetrics == null || timelineMetrics.isEmpty()) {
      LOG.debug("Empty metrics insert request.");
      return;
    }
    for (Iterator<TimelineMetric> iterator = timelineMetrics.iterator(); iterator.hasNext();) {

      TimelineMetric tm = iterator.next();

      boolean acceptMetric = TimelineMetricsFilter.acceptMetric(tm);

      // Write to metadata cache on successful write to store
      if (metadataManager != null) {
        metadataManager.putIfModifiedTimelineMetricMetadata(
                metadataManager.createTimelineMetricMetadata(tm, acceptMetric));

        metadataManager.putIfModifiedHostedAppsMetadata(
                tm.getHostName(), tm.getAppId());

        if (!tm.getAppId().equals("FLUME_HANDLER")) {
          metadataManager.putIfModifiedHostedInstanceMetadata(tm.getInstanceId(), tm.getHostName());
        }
      }
      if (!acceptMetric) {
        iterator.remove();
      }
    }

    if  (!skipCache && cacheEnabled) {
      LOG.debug("Adding metrics to cache");
      if (insertCache.size() >= cacheSize) {
        commitMetricsFromCache();
      }
      try {
        insertCache.put(metrics); // blocked while the queue is full
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } else {
      LOG.debug("Skipping metrics cache");
      commitMetrics(metrics);
    }
  }

  public void insertMetricRecords(TimelineMetrics metrics, boolean skipCache) throws SQLException, IOException {
    insertMetricRecordsWithMetadata(null, metrics, skipCache);
  }

  public void insertMetricRecords(TimelineMetrics metrics) throws SQLException, IOException {
    insertMetricRecords(metrics, false);
  }


  @SuppressWarnings("unchecked")
  public TimelineMetrics getMetricRecords(
    final Condition condition, Multimap<String, List<Function>> metricFunctions)
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

          if (CollectionUtils.isNotEmpty(condition.getUuids())) {
            stmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
            rs = stmt.executeQuery();
            while (rs.next()) {
              appendMetricFromResultSet(metrics, condition, metricFunctions, rs);
            }
          }

          if (CollectionUtils.isNotEmpty(condition.getTransientMetricNames())) {
            stmt = PhoenixTransactSQL.prepareTransientMetricsSqlStmt(conn, condition);
            if (stmt != null) {
              rs = stmt.executeQuery();
              while (rs.next()) {
                TransientMetricReadHelper.appendMetricFromResultSet(metrics, condition, metricFunctions, rs);
              }
            }
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
                                         Multimap<String, List<Function>> metricFunctions,
                                         ResultSet rs) throws SQLException, IOException {
    byte[] uuid = rs.getBytes("UUID");
    String metricName = metadataManagerInstance.getMetricNameFromUuid(uuid);
    Collection<List<Function>> functionList = findMetricFunctions(metricFunctions, metricName);

    if (CollectionUtils.isEmpty(functionList)) {
      LOG.warn("No metric name or pattern in GET query matched the metric name from the metric store : " + metricName);
      return;
    }

    for (List<Function> functions : functionList) {
      // Apply aggregation function if present
      if ((functions != null && !functions.isEmpty())) {
        if (functions.size() > 1) {
          throw new IllegalArgumentException("Multiple aggregate functions not supported.");
        }
        for (Function f : functions) {
          if (f.getReadFunction() == Function.ReadFunction.VALUE) {
            getTimelineMetricsFromResultSet(metrics, f, condition, rs, isEventDownsampledMetric(metricName));
          } else {
            SingleValuedTimelineMetric metric =
              TIMELINE_METRIC_READ_HELPER.getAggregatedTimelineMetricFromResultSet(rs, f, isEventDownsampledMetric(metricName));

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
        getTimelineMetricsFromResultSet(metrics, null, condition, rs, isEventDownsampledMetric(metricName));
      }
    }
  }

  private boolean isEventDownsampledMetric(String metricName) {
    for (String pattern : eventMetricPatterns) {
      if (metricName.matches(pattern)) {
        return true;
      }
    }
    return false;
  }

  private void getTimelineMetricsFromResultSet(TimelineMetrics metrics, Function f, Condition condition, ResultSet rs, boolean shouldSumAcrossTime)
    throws SQLException, IOException {
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
        TIMELINE_METRIC_READ_HELPER.getAggregatedTimelineMetricFromResultSet(rs, f, shouldSumAcrossTime);
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
      Multimap<String, List<Function>> metricFunctions) throws SQLException, IOException {

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

        if (CollectionUtils.isNotEmpty(condition.getUuids())) {
          stmt = PhoenixTransactSQL.prepareGetAggregateSqlStmt(conn, condition);
          rs = stmt.executeQuery();
          while (rs.next()) {
            appendAggregateMetricFromResultSet(metrics, condition, metricFunctions, rs);
          }
        }

        if (CollectionUtils.isNotEmpty(condition.getTransientMetricNames())) {
          stmt = PhoenixTransactSQL.prepareTransientMetricsSqlStmt(conn, condition);
          if (stmt != null) {
            rs = stmt.executeQuery();
            while (rs.next()) {
              TransientMetricReadHelper.appendMetricFromResultSet(metrics, condition, metricFunctions, rs);
            }
          }
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
      Condition condition, Multimap<String, List<Function>> metricFunctions,
      ResultSet rs) throws SQLException {

    byte[] uuid = rs.getBytes("UUID");
    String metricName = metadataManagerInstance.getMetricNameFromUuid(uuid);
    Collection<List<Function>> functionList = findMetricFunctions(metricFunctions, metricName);

    for (List<Function> functions : functionList) {
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

  }

  private void getLatestAggregateMetricRecords(Condition condition,
      Connection conn, TimelineMetrics metrics,
      Multimap<String, List<Function>> metricFunctions) throws SQLException {

    PreparedStatement stmt = null;
    SplitByMetricNamesCondition splitCondition =
      new SplitByMetricNamesCondition(condition);

    for (byte[] uuid: condition.getUuids()) {

      splitCondition.setCurrentUuid(uuid);
      stmt = PhoenixTransactSQL.prepareGetLatestAggregateMetricSqlStmt(conn, splitCondition);
      ResultSet rs = null;
      try {
        rs = stmt.executeQuery();
        while (rs.next()) {
          String metricName = metadataManagerInstance.getMetricNameFromUuid(uuid);
          Collection<List<Function>> functionList = findMetricFunctions(metricFunctions, metricName);
          for (List<Function> functions : functionList) {
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

    byte[] uuid = rs.getBytes("UUID");
    TimelineMetric timelineMetric = metadataManagerInstance.getMetricFromUuid(uuid);

    SingleValuedTimelineMetric metric = new SingleValuedTimelineMetric(
      timelineMetric.getMetricName() + f.getSuffix(),
      timelineMetric.getAppId(),
      timelineMetric.getInstanceId(),
      null,
      rs.getLong("SERVER_TIME")
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

        byte[] uuid = metadataManagerInstance.getUuid(metric, true);
        if (uuid == null) {
          LOG.error("Error computing UUID for metric. Cannot write aggregate metric : " + metric.toString());
          continue;
        }
        rowCount++;
        stmt.clearParameters();
        stmt.setBytes(1, uuid);
        stmt.setLong(2, metric.getStartTime());
        stmt.setDouble(3, hostAggregate.getSum());
        stmt.setDouble(4, hostAggregate.getMax());
        stmt.setDouble(5, hostAggregate.getMin());
        stmt.setDouble(6, hostAggregate.getNumberOfSamples());

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
    if (aggregatorSink != null) {
      try {
        aggregatorSink.saveHostAggregateRecords(hostAggregateMap,
            getTablePrecision(phoenixTableName));
      } catch (Exception e) {
        LOG.warn(
            "Error writing host aggregate records metrics to external sink. "
                + e);
      }
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
        byte[] uuid =  metadataManagerInstance.getUuid(clusterMetric, true);
        if (uuid == null) {
          LOG.error("Error computing UUID for metric. Cannot write metrics : " + clusterMetric.toString());
          continue;
        }
        stmt.clearParameters();
        stmt.setBytes(1, uuid);
        stmt.setLong(2, clusterMetric.getTimestamp());
        stmt.setDouble(3, aggregate.getSum());
        stmt.setInt(4, aggregate.getNumberOfHosts());
        stmt.setDouble(5, aggregate.getMax());
        stmt.setDouble(6, aggregate.getMin());

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
    if (aggregatorSink != null) {
      try {
        aggregatorSink.saveClusterAggregateRecords(records);
      } catch (Exception e) {
        LOG.warn("Error writing cluster aggregate records metrics to external sink. ", e);
      }
    }
  }


  /**
   * Save Metric aggregate records.
   *
   * @throws SQLException
   */
  public void saveClusterAggregateRecordsSecond(Map<TimelineClusterMetric, MetricHostAggregate> records,
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

        byte[] uuid = metadataManagerInstance.getUuid(clusterMetric, true);
        if (uuid == null) {
          LOG.error("Error computing UUID for metric. Cannot write metric : " + clusterMetric.toString());
          continue;
        }

        rowCount++;
        stmt.clearParameters();
        stmt.setBytes(1, uuid);
        stmt.setLong(2, clusterMetric.getTimestamp());
        stmt.setDouble(3, aggregate.getSum());
        stmt.setLong(4, aggregate.getNumberOfSamples());
        stmt.setDouble(5, aggregate.getMax());
        stmt.setDouble(6, aggregate.getMin());

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
    if (aggregatorSink != null) {
      try {
        aggregatorSink.saveClusterTimeAggregateRecords(records,
            getTablePrecision(tableName));
      } catch (Exception e) {
        LOG.warn(
            "Error writing cluster time aggregate records metrics to external sink. "
                + e);
      }
    }
  }

  /**
   * Get precision for a table
   * @param tableName
   * @return precision
   */
  private Precision getTablePrecision(String tableName) {
    Precision tablePrecision = null;
    switch (tableName) {
    case METRICS_RECORD_TABLE_NAME:
      tablePrecision = Precision.SECONDS;
      break;
    case METRICS_AGGREGATE_MINUTE_TABLE_NAME:
    case METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME:
      tablePrecision = Precision.MINUTES;
      break;
    case METRICS_AGGREGATE_HOURLY_TABLE_NAME:
    case METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME:
      tablePrecision = Precision.HOURS;
      break;
    case METRICS_AGGREGATE_DAILY_TABLE_NAME:
    case METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME:
      tablePrecision = Precision.DAYS;
      break;
    }
    return tablePrecision;
  }

  /**
   * Provide skip block cache hint for aggregator queries.
   */
  public boolean isSkipBlockCacheForAggregatorsEnabled() {
    return skipBlockCacheForAggregatorsEnabled;
  }

  /**
   * One time save of metadata when discovering topology during aggregation.
   * @throws SQLException
   */
  public void saveHostAppsMetadata(Map<String, TimelineMetricHostMetadata> hostMetadata) throws SQLException {
    Connection conn = getConnection();
    PreparedStatement stmt = null;
    try {
      stmt = conn.prepareStatement(UPSERT_HOSTED_APPS_METADATA_SQL);
      int rowCount = 0;

      for (Map.Entry<String, TimelineMetricHostMetadata> hostedAppsEntry : hostMetadata.entrySet()) {
        TimelineMetricHostMetadata timelineMetricHostMetadata = hostedAppsEntry.getValue();
        if (LOG.isTraceEnabled()) {
          LOG.trace("HostedAppsMetadata: " + hostedAppsEntry);
        }

        stmt.clearParameters();
        stmt.setString(1, hostedAppsEntry.getKey());
        stmt.setBytes(2, timelineMetricHostMetadata.getUuid());
        stmt.setString(3, StringUtils.join(timelineMetricHostMetadata.getHostedApps().keySet(), ","));
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

  public void saveInstanceHostsMetadata(Map<String, Set<String>> instanceHostsMap) throws SQLException {
    Connection conn = getConnection();
    PreparedStatement stmt = null;
    try {
      stmt = conn.prepareStatement(UPSERT_INSTANCE_HOST_METADATA_SQL);
      int rowCount = 0;

      for (Map.Entry<String, Set<String>> hostInstancesEntry : instanceHostsMap.entrySet()) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Host Instances Entry: " + hostInstancesEntry);
        }

        String instanceId = hostInstancesEntry.getKey();

        for(String hostname : hostInstancesEntry.getValue()) {
          stmt.clearParameters();
          stmt.setString(1, instanceId);
          stmt.setString(2, hostname);
          try {
            stmt.executeUpdate();
            rowCount++;
          } catch (SQLException sql) {
            LOG.error("Error saving host instances metadata.", sql);
          }
        }

      }

      conn.commit();
      LOG.info("Saved " + rowCount + " host instances metadata records.");

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
        try {
          stmt.clearParameters();
          stmt.setString(1, metadata.getMetricName());
          stmt.setString(2, metadata.getAppId());
          stmt.setString(3, metadata.getInstanceId());
          stmt.setBytes(4, metadata.getUuid());
          stmt.setString(5, metadata.getUnits());
          stmt.setString(6, metadata.getType());
          stmt.setLong(7, metadata.getSeriesStartTime());
          stmt.setBoolean(8, metadata.isSupportsAggregates());
          stmt.setBoolean(9, metadata.isWhitelisted());
        } catch (Exception e) {
          LOG.error("Exception in saving metric metadata entry. ");
          continue;
        }

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

  public Map<String, TimelineMetricHostMetadata> getHostedAppsMetadata() throws SQLException {
    Map<String, TimelineMetricHostMetadata> hostedAppMap = new HashMap<>();
    Connection conn = getConnection();
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      stmt = conn.prepareStatement(GET_HOSTED_APPS_METADATA_SQL);
      rs = stmt.executeQuery();

      while (rs.next()) {
        String appIds = rs.getString("APP_IDS");
        TimelineMetricHostMetadata hostMetadata = new TimelineMetricHostMetadata(new HashSet<>());
        if (StringUtils.isNotEmpty(appIds)) {
          hostMetadata = new TimelineMetricHostMetadata(new HashSet<>(Arrays.asList(StringUtils.split(appIds, ","))));
        }
        hostMetadata.setUuid(rs.getBytes("UUID"));
        hostedAppMap.put(rs.getString("HOSTNAME"), hostMetadata);
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

  public Map<String, Set<String>> getInstanceHostsMetdata() throws SQLException {
    Map<String, Set<String>> instanceHostsMap = new HashMap<>();
    Connection conn = getConnection();
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      stmt = conn.prepareStatement(GET_INSTANCE_HOST_METADATA_SQL);
      rs = stmt.executeQuery();

      while (rs.next()) {
        String instanceId = rs.getString("INSTANCE_ID");
        String hostname = rs.getString("HOSTNAME");

        if (!instanceHostsMap.containsKey(instanceId)) {
          instanceHostsMap.put(instanceId, new HashSet<String>());
        }
        instanceHostsMap.get(instanceId).add(hostname);
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

    return instanceHostsMap;
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
        String instanceId = rs.getString("INSTANCE_ID");
        TimelineMetricMetadata metadata = new TimelineMetricMetadata(
          metricName,
          appId,
          instanceId,
          rs.getString("UNITS"),
          rs.getString("TYPE"),
          rs.getLong("START_TIME"),
          rs.getBoolean("SUPPORTS_AGGREGATION"),
          rs.getBoolean("IS_WHITELISTED")
        );

        TimelineMetricMetadataKey key = new TimelineMetricMetadataKey(metricName, appId, instanceId);
        metadata.setIsPersisted(true); // Always true on retrieval
        metadata.setUuid(rs.getBytes("UUID"));
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

  // No filter criteria support for now.
  public Map<TimelineMetricMetadataKey, TimelineMetricMetadata> getTimelineMetricMetadataV1() throws SQLException {
    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> metadataMap = new HashMap<>();
    Connection conn = getConnection();
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      stmt = conn.prepareStatement(GET_METRIC_METADATA_SQL_V1);
      rs = stmt.executeQuery();

      while (rs.next()) {
        String metricName = rs.getString("METRIC_NAME");
        String appId = rs.getString("APP_ID");
        TimelineMetricMetadata metadata = new TimelineMetricMetadata(
          metricName,
          appId,
          null,
          rs.getString("UNITS"),
          rs.getString("TYPE"),
          rs.getLong("START_TIME"),
          rs.getBoolean("SUPPORTS_AGGREGATION"),
          rs.getBoolean("IS_WHITELISTED")
        );

        TimelineMetricMetadataKey key = new TimelineMetricMetadataKey(metricName, appId, null);
        metadata.setIsPersisted(false);
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

  public void setMetadataInstance(TimelineMetricMetadataManager metadataManager) {
    this.metadataManagerInstance = metadataManager;
    TIMELINE_METRIC_READ_HELPER = new TimelineMetricReadHelper(this.metadataManagerInstance);
  }
}
