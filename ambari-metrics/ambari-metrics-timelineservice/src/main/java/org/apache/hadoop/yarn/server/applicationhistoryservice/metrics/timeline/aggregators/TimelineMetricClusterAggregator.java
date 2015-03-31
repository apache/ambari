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
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_MINUTE_DISABLED;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_MINUTE_SLEEP_INTERVAL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.CLUSTER_AGGREGATOR_TIMESLICE_INTERVAL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.DEFAULT_CHECKPOINT_LOCATION;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_METRIC_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.NATIVE_TIME_RANGE_DELTA;

/**
 * Aggregates a metric across all hosts in the cluster. Reads metrics from
 * the precision table and saves into the aggregate.
 */
public class TimelineMetricClusterAggregator extends AbstractTimelineAggregator {
  private static final Log LOG = LogFactory.getLog(TimelineMetricClusterAggregator.class);
  private static final String CLUSTER_AGGREGATOR_CHECKPOINT_FILE =
    "timeline-metrics-cluster-aggregator-checkpoint";
  private final String checkpointLocation;
  private final Long sleepIntervalMillis;
  public final int timeSliceIntervalMillis;
  private final Integer checkpointCutOffMultiplier;
  private TimelineMetricReadHelper timelineMetricReadHelper = new TimelineMetricReadHelper(true);
  // Aggregator to perform app-level aggregates for host metrics
  private final TimelineMetricAppAggregator appAggregator;

  public TimelineMetricClusterAggregator(PhoenixHBaseAccessor hBaseAccessor,
                                         Configuration metricsConf) {
    super(hBaseAccessor, metricsConf);

    String checkpointDir = metricsConf.get(
      TIMELINE_METRICS_AGGREGATOR_CHECKPOINT_DIR, DEFAULT_CHECKPOINT_LOCATION);

    checkpointLocation = FilenameUtils.concat(checkpointDir,
      CLUSTER_AGGREGATOR_CHECKPOINT_FILE);

    sleepIntervalMillis = SECONDS.toMillis(metricsConf.getLong
      (CLUSTER_AGGREGATOR_MINUTE_SLEEP_INTERVAL, 120l));
    timeSliceIntervalMillis = (int)SECONDS.toMillis(metricsConf.getInt
      (CLUSTER_AGGREGATOR_TIMESLICE_INTERVAL, 15));
    checkpointCutOffMultiplier =
      metricsConf.getInt(CLUSTER_AGGREGATOR_MINUTE_CHECKPOINT_CUTOFF_MULTIPLIER, 2);

    appAggregator = new TimelineMetricAppAggregator(metricsConf);
  }

  @Override
  protected String getCheckpointLocation() {
    return checkpointLocation;
  }

  @Override
  protected void aggregate(ResultSet rs, long startTime, long endTime)
    throws SQLException, IOException {
    List<Long[]> timeSlices = getTimeSlices(startTime, endTime);
    // Initialize app aggregates for host metrics
    appAggregator.init();
    Map<TimelineClusterMetric, MetricClusterAggregate>
      aggregateClusterMetrics = aggregateMetricsFromResultSet(rs, timeSlices);

    LOG.info("Saving " + aggregateClusterMetrics.size() + " metric aggregates.");
    hBaseAccessor.saveClusterAggregateRecords(aggregateClusterMetrics);
    appAggregator.cleanup();
  }

  @Override
  protected Condition prepareMetricQueryCondition(long startTime, long endTime) {
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setNoLimit();
    condition.setFetchSize(resultsetFetchSize);
    condition.setStatement(String.format(GET_METRIC_SQL,
      PhoenixTransactSQL.getNaiveTimeRangeHint(startTime, NATIVE_TIME_RANGE_DELTA),
      METRICS_RECORD_TABLE_NAME));
    condition.addOrderByColumn("METRIC_NAME");
    condition.addOrderByColumn("APP_ID");
    condition.addOrderByColumn("INSTANCE_ID");
    condition.addOrderByColumn("SERVER_TIME");
    return condition;
  }

  private List<Long[]> getTimeSlices(long startTime, long endTime) {
    List<Long[]> timeSlices = new ArrayList<Long[]>();
    long sliceStartTime = startTime;
    while (sliceStartTime < endTime) {
      timeSlices.add(new Long[] { sliceStartTime, sliceStartTime + timeSliceIntervalMillis});
      sliceStartTime += timeSliceIntervalMillis;
    }
    return timeSlices;
  }

  private Map<TimelineClusterMetric, MetricClusterAggregate> aggregateMetricsFromResultSet(ResultSet rs, List<Long[]> timeSlices)
      throws SQLException, IOException {
    Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();
    // Create time slices

    while (rs.next()) {
      TimelineMetric metric = timelineMetricReadHelper.getTimelineMetricFromResultSet(rs);

      Map<TimelineClusterMetric, Double> clusterMetrics =
        sliceFromTimelineMetric(metric, timeSlices);

      if (clusterMetrics != null && !clusterMetrics.isEmpty()) {
        for (Map.Entry<TimelineClusterMetric, Double> clusterMetricEntry :
            clusterMetrics.entrySet()) {

          TimelineClusterMetric clusterMetric = clusterMetricEntry.getKey();
          Double avgValue = clusterMetricEntry.getValue();

          MetricClusterAggregate aggregate = aggregateClusterMetrics.get(clusterMetric);

          if (aggregate == null) {
            aggregate = new MetricClusterAggregate(avgValue, 1, null, avgValue, avgValue);
            aggregateClusterMetrics.put(clusterMetric, aggregate);
          } else {
            aggregate.updateSum(avgValue);
            aggregate.updateNumberOfHosts(1);
            aggregate.updateMax(avgValue);
            aggregate.updateMin(avgValue);
          }
          // Update app level aggregates
          appAggregator.processTimelineClusterMetric(clusterMetric,
            metric.getHostName(), avgValue);
        }
      }
    }
    // Add app level aggregates to save
    aggregateClusterMetrics.putAll(appAggregator.getAggregateClusterMetrics());
    return aggregateClusterMetrics;
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
  public boolean isDisabled() {
    return metricsConf.getBoolean(CLUSTER_AGGREGATOR_MINUTE_DISABLED, false);
  }

  private Map<TimelineClusterMetric, Double> sliceFromTimelineMetric(
        TimelineMetric timelineMetric, List<Long[]> timeSlices) {

    if (timelineMetric.getMetricValues().isEmpty()) {
      return null;
    }

    Map<TimelineClusterMetric, Double> timelineClusterMetricMap =
      new HashMap<TimelineClusterMetric, Double>();

    for (Map.Entry<Long, Double> metric : timelineMetric.getMetricValues().entrySet()) {
      // TODO: investigate null values - pre filter
      if (metric.getValue() == null) {
        continue;
      }
      Long timestamp = getSliceTimeForMetric(timeSlices,
                       Long.parseLong(metric.getKey().toString()));
      if (timestamp != -1) {
        // Metric is within desired time range
        TimelineClusterMetric clusterMetric = new TimelineClusterMetric(
          timelineMetric.getMetricName(),
          timelineMetric.getAppId(),
          timelineMetric.getInstanceId(),
          timestamp,
          timelineMetric.getType());
        if (!timelineClusterMetricMap.containsKey(clusterMetric)) {
          timelineClusterMetricMap.put(clusterMetric, metric.getValue());
        } else {
          Double oldValue = timelineClusterMetricMap.get(clusterMetric);
          Double newValue = (oldValue + metric.getValue()) / 2;
          timelineClusterMetricMap.put(clusterMetric, newValue);
        }
      }
    }

    return timelineClusterMetricMap;
  }

  /**
   * Return beginning of the time slice into which the metric fits.
   */
  private Long getSliceTimeForMetric(List<Long[]> timeSlices, Long timestamp) {
    for (Long[] timeSlice : timeSlices) {
      if (timestamp >= timeSlice[0] && timestamp < timeSlice[1]) {
        return timeSlice[0];
      }
    }
    return -1l;
  }

}
