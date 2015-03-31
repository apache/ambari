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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;

import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.DEFAULT_CHECKPOINT_LOCATION;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_HOUR_DISABLED;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_HOUR_SLEEP_INTERVAL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_MINUTE_DISABLED;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.HOST_AGGREGATOR_MINUTE_SLEEP_INTERVAL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR;

public class TimelineMetricAggregatorFactory {
  private static final String MINUTE_AGGREGATE_CHECKPOINT_FILE =
    "timeline-metrics-host-aggregator-checkpoint";
  private static final String MINUTE_AGGREGATE_HOURLY_CHECKPOINT_FILE =
    "timeline-metrics-host-aggregator-hourly-checkpoint";

  public static TimelineMetricAggregator createTimelineMetricAggregatorMinute
    (PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf) {

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);
    String checkpointLocation = FilenameUtils.concat(checkpointDir,
      MINUTE_AGGREGATE_CHECKPOINT_FILE);
    long sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (HOST_AGGREGATOR_MINUTE_SLEEP_INTERVAL, 300l));  // 5 mins

    int checkpointCutOffMultiplier = metricsConf.getInt
      (HOST_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER, 3);
    String hostAggregatorDisabledParam = HOST_AGGREGATOR_MINUTE_DISABLED;

    String inputTableName = METRICS_RECORD_TABLE_NAME;
    String outputTableName = METRICS_AGGREGATE_MINUTE_TABLE_NAME;

    return new TimelineMetricAggregator(hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      hostAggregatorDisabledParam,
      inputTableName,
      outputTableName,
      120000l);
  }

  public static TimelineMetricAggregator createTimelineMetricAggregatorHourly
    (PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf) {

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);
    String checkpointLocation = FilenameUtils.concat(checkpointDir,
      MINUTE_AGGREGATE_HOURLY_CHECKPOINT_FILE);
    long sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (HOST_AGGREGATOR_HOUR_SLEEP_INTERVAL, 3600l));

    int checkpointCutOffMultiplier = metricsConf.getInt
      (HOST_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER, 2);
    String hostAggregatorDisabledParam = HOST_AGGREGATOR_HOUR_DISABLED;

    String inputTableName = METRICS_AGGREGATE_MINUTE_TABLE_NAME;
    String outputTableName = METRICS_AGGREGATE_HOURLY_TABLE_NAME;

    return new TimelineMetricAggregator(hBaseAccessor, metricsConf,
      checkpointLocation,
      sleepIntervalMillis,
      checkpointCutOffMultiplier,
      hostAggregatorDisabledParam,
      inputTableName,
      outputTableName,
      3600000l);
  }


}
