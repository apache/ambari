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
package org.apache.ambari.metrics.core.timeline.aggregators;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_DAILY_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_DAILY_DISABLED;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_DAILY_SLEEP_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_HOUR_DISABLED;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_HOUR_SLEEP_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_MINUTE_DISABLED;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_MINUTE_SLEEP_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_SECOND_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_SECOND_DISABLED;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_SECOND_SLEEP_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_TIMESLICE_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.DEFAULT_CHECKPOINT_LOCATION;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_DAILY_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_DAILY_DISABLED;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_DAILY_SLEEP_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_HOUR_DISABLED;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_HOUR_SLEEP_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_MINUTE_DISABLED;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_MINUTE_SLEEP_INTERVAL;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.USE_GROUPBY_AGGREGATOR_QUERIES;
import static org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_DAILY;
import static org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_HOURLY;
import static org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_MINUTE;
import static org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_SECOND;
import static org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_RECORD_DAILY;
import static org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_RECORD_HOURLY;
import static org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_RECORD_MINUTE;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.metrics.core.timeline.availability.MetricCollectorHAController;
import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.ambari.metrics.core.timeline.PhoenixHBaseAccessor;
import org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration;
import org.apache.ambari.metrics.core.timeline.TimelineMetricDistributedCache;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;

/**
 * Factory class that knows how to create a aggregator instance using
 * TimelineMetricConfiguration
 */
public class TimelineMetricAggregatorFactory {
  private static final String HOST_AGGREGATE_MINUTE_CHECKPOINT_FILE =
    "timeline-metrics-host-aggregator-checkpoint";
  private static final String HOST_AGGREGATE_HOURLY_CHECKPOINT_FILE =
    "timeline-metrics-host-aggregator-hourly-checkpoint";
  private static final String HOST_AGGREGATE_DAILY_CHECKPOINT_FILE =
    "timeline-metrics-host-aggregator-daily-checkpoint";
  private static final String CLUSTER_AGGREGATOR_CHECKPOINT_FILE =
    "timeline-metrics-cluster-aggregator-checkpoint";
  private static final String CLUSTER_AGGREGATOR_MINUTE_CHECKPOINT_FILE =
    "timeline-metrics-cluster-aggregator-minute-checkpoint";
  private static final String CLUSTER_AGGREGATOR_HOURLY_CHECKPOINT_FILE =
    "timeline-metrics-cluster-aggregator-hourly-checkpoint";
  private static final String CLUSTER_AGGREGATOR_DAILY_CHECKPOINT_FILE =
    "timeline-metrics-cluster-aggregator-daily-checkpoint";

  private static boolean useGroupByAggregator(Configuration metricsConf) {
    return Boolean.parseBoolean(metricsConf.get(USE_GROUPBY_AGGREGATOR_QUERIES, "true"));
  }

  /**
   * Minute based aggregation for hosts.
   * Interval : 5 mins
   */
  public static TimelineMetricAggregator createTimelineMetricAggregatorMinute
    (PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf,
     TimelineMetricMetadataManager metadataManager,
     MetricCollectorHAController haController) {

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);
    String checkpointLocation = FilenameUtils.concat(checkpointDir,
      HOST_AGGREGATE_MINUTE_CHECKPOINT_FILE);
    long sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (HOST_AGGREGATOR_MINUTE_SLEEP_INTERVAL, 300l));  // 5 mins

    int checkpointCutOffMultiplier = metricsConf.getInt
      (HOST_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER, 3);
    String hostAggregatorDisabledParam = HOST_AGGREGATOR_MINUTE_DISABLED;

    String inputTableName = METRICS_RECORD_TABLE_NAME;
    String outputTableName = METRICS_AGGREGATE_MINUTE_TABLE_NAME;

    if (useGroupByAggregator(metricsConf)) {
      return new org.apache.ambari.metrics.core.timeline.aggregators.v2.TimelineMetricHostAggregator(
        METRIC_RECORD_MINUTE,
        hBaseAccessor, metricsConf,
        checkpointLocation,
        sleepIntervalMillis,
        checkpointCutOffMultiplier,
        hostAggregatorDisabledParam,
        inputTableName,
        outputTableName,
        120000l,
        haController
      );
    }

    return new TimelineMetricHostAggregator(
      METRIC_RECORD_MINUTE,
      metadataManager,
      hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      hostAggregatorDisabledParam,
      inputTableName,
      outputTableName,
      120000l,
      haController);
  }

  /**
   * Hourly aggregation for hosts.
   * Interval : 1 hour
   */
  public static TimelineMetricAggregator createTimelineMetricAggregatorHourly
    (PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf,
     TimelineMetricMetadataManager metadataManager,
     MetricCollectorHAController haController) {

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);
    String checkpointLocation = FilenameUtils.concat(checkpointDir,
      HOST_AGGREGATE_HOURLY_CHECKPOINT_FILE);
    long sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (HOST_AGGREGATOR_HOUR_SLEEP_INTERVAL, 3600l));

    int checkpointCutOffMultiplier = metricsConf.getInt
      (HOST_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER, 2);
    String hostAggregatorDisabledParam = HOST_AGGREGATOR_HOUR_DISABLED;

    String inputTableName = METRICS_AGGREGATE_MINUTE_TABLE_NAME;
    String outputTableName = METRICS_AGGREGATE_HOURLY_TABLE_NAME;

    if (useGroupByAggregator(metricsConf)) {
      return new org.apache.ambari.metrics.core.timeline.aggregators.v2.TimelineMetricHostAggregator(
        METRIC_RECORD_HOURLY,
        hBaseAccessor, metricsConf,
        checkpointLocation,
        sleepIntervalMillis,
        checkpointCutOffMultiplier,
        hostAggregatorDisabledParam,
        inputTableName,
        outputTableName,
        3600000l,
        haController
      );
    }

    return new TimelineMetricHostAggregator(
      METRIC_RECORD_HOURLY,
      metadataManager,
      hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      hostAggregatorDisabledParam,
      inputTableName,
      outputTableName,
      3600000l,
      haController);
  }

  /**
   * Daily aggregation for hosts.
   * Interval : 1 day
   */
  public static TimelineMetricAggregator createTimelineMetricAggregatorDaily
    (PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf,
     TimelineMetricMetadataManager metadataManager,
     MetricCollectorHAController haController) {

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);
    String checkpointLocation = FilenameUtils.concat(checkpointDir,
      HOST_AGGREGATE_DAILY_CHECKPOINT_FILE);
    long sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (HOST_AGGREGATOR_DAILY_SLEEP_INTERVAL, 86400l));

    int checkpointCutOffMultiplier = metricsConf.getInt
      (HOST_AGGREGATOR_DAILY_CHECKPOINT_CUTOFF_MULTIPLIER, 1);
    String hostAggregatorDisabledParam = HOST_AGGREGATOR_DAILY_DISABLED;

    String inputTableName = METRICS_AGGREGATE_HOURLY_TABLE_NAME;
    String outputTableName = METRICS_AGGREGATE_DAILY_TABLE_NAME;

    if (useGroupByAggregator(metricsConf)) {
      return new org.apache.ambari.metrics.core.timeline.aggregators.v2.TimelineMetricHostAggregator(
        METRIC_RECORD_DAILY,
        hBaseAccessor, metricsConf,
        checkpointLocation,
        sleepIntervalMillis,
        checkpointCutOffMultiplier,
        hostAggregatorDisabledParam,
        inputTableName,
        outputTableName,
        3600000l,
        haController
      );
    }

    return new TimelineMetricHostAggregator(
      METRIC_RECORD_DAILY,
      metadataManager,
      hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      hostAggregatorDisabledParam,
      inputTableName,
      outputTableName,
      3600000l,
      haController);
  }

  /**
   * Second aggregation for cluster.
   * Interval : 2 mins
   * Timeslice : 30 sec
   */
  public static TimelineMetricAggregator createTimelineClusterAggregatorSecond(
    PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf,
    TimelineMetricMetadataManager metadataManager,
    MetricCollectorHAController haController,
    TimelineMetricDistributedCache distributedCache) {

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);

    String checkpointLocation = FilenameUtils.concat(checkpointDir,
      CLUSTER_AGGREGATOR_CHECKPOINT_FILE);

    long sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (CLUSTER_AGGREGATOR_SECOND_SLEEP_INTERVAL, 120l));

    long timeSliceIntervalMillis = SECONDS.toMillis(metricsConf.getInt
      (CLUSTER_AGGREGATOR_TIMESLICE_INTERVAL, 30));

    int checkpointCutOffMultiplier =
      metricsConf.getInt(CLUSTER_AGGREGATOR_SECOND_CHECKPOINT_CUTOFF_MULTIPLIER, 2);

    String outputTableName = METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
    String aggregatorDisabledParam = CLUSTER_AGGREGATOR_SECOND_DISABLED;

    // Second based aggregation have added responsibility of time slicing
    if (TimelineMetricConfiguration.getInstance().isCollectorInMemoryAggregationEnabled()) {
      return new TimelineMetricClusterAggregatorSecondWithCacheSource(
        METRIC_AGGREGATE_SECOND,
        metadataManager,
        hBaseAccessor, metricsConf,
        checkpointLocation,
        sleepIntervalMillis,
        checkpointCutOffMultiplier,
        aggregatorDisabledParam,
        null,
        outputTableName,
        120000l,
        timeSliceIntervalMillis,
        haController,
        distributedCache
      );
    }

    String inputTableName = METRICS_RECORD_TABLE_NAME;
    return new TimelineMetricClusterAggregatorSecond(
      METRIC_AGGREGATE_SECOND,
      metadataManager,
      hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      aggregatorDisabledParam,
      inputTableName,
      outputTableName,
      120000l,
      timeSliceIntervalMillis,
      haController
    );
  }

  /**
   * Minute aggregation for cluster.
   * Interval : 5 mins
   */
  public static TimelineMetricAggregator createTimelineClusterAggregatorMinute(
    PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf,
    TimelineMetricMetadataManager metadataManager,
    MetricCollectorHAController haController) {

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);

    String checkpointLocation = FilenameUtils.concat(checkpointDir,
      CLUSTER_AGGREGATOR_MINUTE_CHECKPOINT_FILE);

    long sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (CLUSTER_AGGREGATOR_MINUTE_SLEEP_INTERVAL, 300l));

    int checkpointCutOffMultiplier = metricsConf.getInt
      (CLUSTER_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER, 2);

    String inputTableName = METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
    String outputTableName = METRICS_CLUSTER_AGGREGATE_MINUTE_TABLE_NAME;
    String aggregatorDisabledParam = CLUSTER_AGGREGATOR_MINUTE_DISABLED;

    if (useGroupByAggregator(metricsConf)) {
      return new org.apache.ambari.metrics.core.timeline.aggregators.v2.TimelineMetricClusterAggregator(
        METRIC_AGGREGATE_MINUTE,
        hBaseAccessor, metricsConf,
        checkpointLocation,
        sleepIntervalMillis,
        checkpointCutOffMultiplier,
        aggregatorDisabledParam,
        inputTableName,
        outputTableName,
        120000l,
        haController
      );
    }

    return new TimelineMetricClusterAggregator(
      METRIC_AGGREGATE_MINUTE,
      metadataManager,
      hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      aggregatorDisabledParam,
      inputTableName,
      outputTableName,
      120000l,
      haController
    );
  }

  /**
   * Hourly aggregation for cluster.
   * Interval : 1 hour
   */
  public static TimelineMetricAggregator createTimelineClusterAggregatorHourly(
    PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf,
    TimelineMetricMetadataManager metadataManager,
    MetricCollectorHAController haController) {

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);

    String checkpointLocation = FilenameUtils.concat(checkpointDir,
      CLUSTER_AGGREGATOR_HOURLY_CHECKPOINT_FILE);

    long sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (CLUSTER_AGGREGATOR_HOUR_SLEEP_INTERVAL, 3600l));

    int checkpointCutOffMultiplier = metricsConf.getInt
      (CLUSTER_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER, 2);

    String inputTableName = METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
    String outputTableName = METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
    String aggregatorDisabledParam = CLUSTER_AGGREGATOR_HOUR_DISABLED;

    if (useGroupByAggregator(metricsConf)) {
      return new org.apache.ambari.metrics.core.timeline.aggregators.v2.TimelineMetricClusterAggregator(
        METRIC_AGGREGATE_HOURLY,
        hBaseAccessor, metricsConf,
        checkpointLocation,
        sleepIntervalMillis,
        checkpointCutOffMultiplier,
        aggregatorDisabledParam,
        inputTableName,
        outputTableName,
        120000l,
        haController
      );
    }

    return new TimelineMetricClusterAggregator(
      METRIC_AGGREGATE_HOURLY,
      metadataManager,
      hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      aggregatorDisabledParam,
      inputTableName,
      outputTableName,
      120000l,
      haController
    );
  }

  /**
   * Daily aggregation for cluster.
   * Interval : 1 day
   */
  public static TimelineMetricAggregator createTimelineClusterAggregatorDaily(
    PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf,
    TimelineMetricMetadataManager metadataManager,
    MetricCollectorHAController haController) {

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);

    String checkpointLocation = FilenameUtils.concat(checkpointDir,
      CLUSTER_AGGREGATOR_DAILY_CHECKPOINT_FILE);

    long sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (CLUSTER_AGGREGATOR_DAILY_SLEEP_INTERVAL, 86400l));

    int checkpointCutOffMultiplier = metricsConf.getInt
      (CLUSTER_AGGREGATOR_DAILY_CHECKPOINT_CUTOFF_MULTIPLIER, 1);

    String inputTableName = METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
    String outputTableName = METRICS_CLUSTER_AGGREGATE_DAILY_TABLE_NAME;
    String aggregatorDisabledParam = CLUSTER_AGGREGATOR_DAILY_DISABLED;

    if (useGroupByAggregator(metricsConf)) {
      return new org.apache.ambari.metrics.core.timeline.aggregators.v2.TimelineMetricClusterAggregator(
        METRIC_AGGREGATE_DAILY,
        hBaseAccessor, metricsConf,
        checkpointLocation,
        sleepIntervalMillis,
        checkpointCutOffMultiplier,
        aggregatorDisabledParam,
        inputTableName,
        outputTableName,
        120000l,
        haController
      );
    }

    return new TimelineMetricClusterAggregator(
      METRIC_AGGREGATE_DAILY,
      metadataManager,
      hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      aggregatorDisabledParam,
      inputTableName,
      outputTableName,
      120000l,
      haController
    );
  }

  //TODO : Fix this.
  public static TimelineMetricAggregator createFilteringTimelineMetricAggregatorMinute(PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf, TimelineMetricMetadataManager metricMetadataManager, MetricCollectorHAController haController, ConcurrentHashMap<String, Long> postedAggregatedMap) {
    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);
    String checkpointLocation = FilenameUtils.concat(checkpointDir,
      HOST_AGGREGATE_MINUTE_CHECKPOINT_FILE);
    long sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (HOST_AGGREGATOR_MINUTE_SLEEP_INTERVAL, 300l));  // 5 mins

    int checkpointCutOffMultiplier = metricsConf.getInt
      (HOST_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER, 3);
    String hostAggregatorDisabledParam = HOST_AGGREGATOR_MINUTE_DISABLED;

    String inputTableName = METRICS_RECORD_TABLE_NAME;
    String outputTableName = METRICS_AGGREGATE_MINUTE_TABLE_NAME;

    if (useGroupByAggregator(metricsConf)) {
      return new org.apache.ambari.metrics.core.timeline.aggregators.v2.TimelineMetricFilteringHostAggregator(
        METRIC_RECORD_MINUTE,
        metricMetadataManager,
        hBaseAccessor, metricsConf,
        checkpointLocation,
        sleepIntervalMillis,
        checkpointCutOffMultiplier,
        hostAggregatorDisabledParam,
        inputTableName,
        outputTableName,
        120000l,
        haController,
        postedAggregatedMap
      );
    }

    return new TimelineMetricFilteringHostAggregator(
      METRIC_RECORD_MINUTE,
      metricMetadataManager,
      hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      hostAggregatorDisabledParam,
      inputTableName,
      outputTableName,
      120000l,
      haController,
      postedAggregatedMap);
  }
}
