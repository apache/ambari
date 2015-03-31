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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor.getMetricClusterAggregateFromResultSet;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_CLUSTER_AGGREGATE_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_INTERVAL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_HOUR_DISABLED;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_HOUR_SLEEP_INTERVAL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.DEFAULT_CHECKPOINT_LOCATION;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR;

public class TimelineMetricClusterAggregatorHourly extends AbstractTimelineAggregator {
  private static final Log LOG = LogFactory.getLog
    (TimelineMetricClusterAggregatorHourly.class);
  private static final String CLUSTER_AGGREGATOR_HOURLY_CHECKPOINT_FILE =
    "timeline-metrics-cluster-aggregator-hourly-checkpoint";
  private final String checkpointLocation;
  private final long sleepIntervalMillis;
  private final Integer checkpointCutOffMultiplier;
  private long checkpointCutOffIntervalMillis;
  private static final Long NATIVE_TIME_RANGE_DELTA = 3600000l; // 1 hour
  private final TimelineClusterMetricReader timelineClusterMetricReader
     = new TimelineClusterMetricReader(true);

  public TimelineMetricClusterAggregatorHourly(
    PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf) {
    super(hBaseAccessor, metricsConf);

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);

    checkpointLocation = FilenameUtils.concat(checkpointDir,
      CLUSTER_AGGREGATOR_HOURLY_CHECKPOINT_FILE);

    sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (CLUSTER_AGGREGATOR_HOUR_SLEEP_INTERVAL, 3600l));
    checkpointCutOffIntervalMillis =  SECONDS.toMillis(metricsConf.getLong
      (CLUSTER_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_INTERVAL, 7200l));
    checkpointCutOffMultiplier = metricsConf.getInt
      (CLUSTER_AGGREGATOR_HOUR_CHECKPOINT_CUTOFF_MULTIPLIER, 2);
  }

  @Override
  protected String getCheckpointLocation() {
    return checkpointLocation;
  }

  @Override
  protected void aggregate(ResultSet rs, long startTime, long endTime)
    throws SQLException, IOException {
      Map<TimelineClusterMetric, MetricHostAggregate> hostAggregateMap =
        aggregateMetricsFromResultSet(rs);

    LOG.info("Saving " + hostAggregateMap.size() + " metric aggregates.");
    hBaseAccessor.saveClusterAggregateHourlyRecords(hostAggregateMap,
      METRICS_CLUSTER_AGGREGATE_HOURLY_TABLE_NAME);
  }

  @Override
  protected Condition prepareMetricQueryCondition(long startTime,
                                                  long endTime) {
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setNoLimit();
    condition.setFetchSize(resultsetFetchSize);
    condition.setStatement(String.format(GET_CLUSTER_AGGREGATE_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, NATIVE_TIME_RANGE_DELTA),
        METRICS_CLUSTER_AGGREGATE_TABLE_NAME));
    condition.addOrderByColumn("METRIC_NAME");
    condition.addOrderByColumn("APP_ID");
    condition.addOrderByColumn("INSTANCE_ID");
    condition.addOrderByColumn("SERVER_TIME");
    return condition;
  }

  private Map<TimelineClusterMetric, MetricHostAggregate> aggregateMetricsFromResultSet(ResultSet rs)
      throws IOException, SQLException {

    TimelineClusterMetric existingMetric = null;
    MetricHostAggregate hostAggregate = null;
    Map<TimelineClusterMetric, MetricHostAggregate> hostAggregateMap =
      new HashMap<TimelineClusterMetric, MetricHostAggregate>();

    while (rs.next()) {
      TimelineClusterMetric currentMetric =
        timelineClusterMetricReader.fromResultSet(rs);
      MetricClusterAggregate currentHostAggregate =
        getMetricClusterAggregateFromResultSet(rs);

      if (existingMetric == null) {
        // First row
        existingMetric = currentMetric;
        hostAggregate = new MetricHostAggregate();
        hostAggregateMap.put(currentMetric, hostAggregate);
      }

      if (existingMetric.equalsExceptTime(currentMetric)) {
        // Recalculate totals with current metric
        updateAggregatesFromHost(hostAggregate, currentHostAggregate);

      } else {
        // Switched over to a new metric - save existing
        hostAggregate = new MetricHostAggregate();
        updateAggregatesFromHost(hostAggregate, currentHostAggregate);
        hostAggregateMap.put(currentMetric, hostAggregate);
        existingMetric = currentMetric;
      }

    }

    return hostAggregateMap;
  }

  private void updateAggregatesFromHost(MetricHostAggregate agg, MetricClusterAggregate currentClusterAggregate) {
    agg.updateMax(currentClusterAggregate.getMax());
    agg.updateMin(currentClusterAggregate.getMin());
    agg.updateSum(currentClusterAggregate.getSum());
    agg.updateNumberOfSamples(currentClusterAggregate.getNumberOfHosts());
  }

  @Override
  protected Long getSleepIntervalMillis() {
    return sleepIntervalMillis;
  }

  @Override
  protected Integer getCheckpointCutOffMultiplier() {
    return checkpointCutOffMultiplier;
  }

  @Override
  protected Long getCheckpointCutOffIntervalMillis() {
    return checkpointCutOffIntervalMillis;
  }

  @Override
  public boolean isDisabled() {
    return metricsConf.getBoolean(CLUSTER_AGGREGATOR_HOUR_DISABLED, false);
  }


}
