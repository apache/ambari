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
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.SERVER_SIDE_TIMESIFT_ADJUSTMENT;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_METRIC_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.NATIVE_TIME_RANGE_DELTA;

/**
 * Aggregates a metric across all hosts in the cluster. Reads metrics from
 * the precision table and saves into the aggregate.
 */
public class TimelineMetricClusterAggregatorMinute extends AbstractTimelineAggregator {
  private static final Log LOG = LogFactory.getLog(TimelineMetricClusterAggregatorMinute.class);
  public Long timeSliceIntervalMillis;
  private TimelineMetricReadHelper timelineMetricReadHelper = new TimelineMetricReadHelper(true);
  // Aggregator to perform app-level aggregates for host metrics
  private final TimelineMetricAppAggregator appAggregator;
  // 1 minute client side buffering adjustment
  private final Long serverTimeShiftAdjustment;

  public TimelineMetricClusterAggregatorMinute(PhoenixHBaseAccessor hBaseAccessor,
                                               Configuration metricsConf,
                                               String checkpointLocation,
                                               Long sleepIntervalMillis,
                                               Integer checkpointCutOffMultiplier,
                                               String aggregatorDisabledParam,
                                               String tableName,
                                               String outputTableName,
                                               Long nativeTimeRangeDelay,
                                               Long timeSliceInterval) {
    super(hBaseAccessor, metricsConf, checkpointLocation, sleepIntervalMillis,
      checkpointCutOffMultiplier, aggregatorDisabledParam, tableName,
      outputTableName, nativeTimeRangeDelay);

    appAggregator = new TimelineMetricAppAggregator(metricsConf);
    this.timeSliceIntervalMillis = timeSliceInterval;
    this.serverTimeShiftAdjustment = Long.parseLong(metricsConf.get(SERVER_SIDE_TIMESIFT_ADJUSTMENT, "90000"));
  }

  @Override
  protected void aggregate(ResultSet rs, long startTime, long endTime) throws SQLException, IOException {
    // Account for time shift due to client side buffering by shifting the
    // timestamps with the difference between server time and series start time
    List<Long[]> timeSlices = getTimeSlices(startTime - serverTimeShiftAdjustment, endTime);
    // Initialize app aggregates for host metrics
    appAggregator.init();
    Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics =
      aggregateMetricsFromResultSet(rs, timeSlices);

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
    // Retaining order of the row-key avoids client side merge sort.
    condition.addOrderByColumn("METRIC_NAME");
    condition.addOrderByColumn("HOSTNAME");
    condition.addOrderByColumn("SERVER_TIME");
    condition.addOrderByColumn("APP_ID");
    return condition;
  }

  /**
   * Return time slices to normalize the timeseries data.
   */
  private List<Long[]> getTimeSlices(long startTime, long endTime) {
    List<Long[]> timeSlices = new ArrayList<Long[]>();
    long sliceStartTime = startTime;
    while (sliceStartTime < endTime) {
      timeSlices.add(new Long[] { sliceStartTime, sliceStartTime + timeSliceIntervalMillis });
      sliceStartTime += timeSliceIntervalMillis;
    }
    return timeSlices;
  }

  private Map<TimelineClusterMetric, MetricClusterAggregate> aggregateMetricsFromResultSet(ResultSet rs, List<Long[]> timeSlices)
      throws SQLException, IOException {
    Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    TimelineMetric metric = null;
    if (rs.next()) {
      metric = timelineMetricReadHelper.getTimelineMetricFromResultSet(rs);

      // Call slice after all rows for a host are read
      while (rs.next()) {
        TimelineMetric nextMetric = timelineMetricReadHelper.getTimelineMetricFromResultSet(rs);
        // If rows belong to same host combine them before slicing. This
        // avoids issues across rows that belong to same hosts but get
        // counted as coming from different ones.
        if (metric.equalsExceptTime(nextMetric)) {
          metric.addMetricValues(nextMetric.getMetricValues());
        } else {
          // Process the current metric
          processAggregateClusterMetrics(aggregateClusterMetrics, metric, timeSlices);
          metric = nextMetric;
        }
      }
    }
    // Process last metric
    if (metric != null) {
      processAggregateClusterMetrics(aggregateClusterMetrics, metric, timeSlices);
    }

    // Add app level aggregates to save
    aggregateClusterMetrics.putAll(appAggregator.getAggregateClusterMetrics());
    return aggregateClusterMetrics;
  }

  /**
   * Slice metric values into interval specified by :
   * timeline.metrics.cluster.aggregator.minute.timeslice.interval
   * Normalize value by averaging them within the interval
   */
  private void processAggregateClusterMetrics(Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics,
                                              TimelineMetric metric, List<Long[]> timeSlices) {
    // Create time slices
    Map<TimelineClusterMetric, Double> clusterMetrics = sliceFromTimelineMetric(metric, timeSlices);

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
        appAggregator.processTimelineClusterMetric(clusterMetric, metric.getHostName(), avgValue);
      }
    }
  }

  private Map<TimelineClusterMetric, Double> sliceFromTimelineMetric(
      TimelineMetric timelineMetric, List<Long[]> timeSlices) {

    if (timelineMetric.getMetricValues().isEmpty()) {
      return null;
    }

    Map<TimelineClusterMetric, Double> timelineClusterMetricMap =
      new HashMap<TimelineClusterMetric, Double>();

    Long timeShift = timelineMetric.getTimestamp() - timelineMetric.getStartTime();
    if (timeShift < 0) {
      LOG.debug("Invalid time shift found, possible discrepancy in clocks. " +
        "timeShift = " + timeShift);
      timeShift = 0l;
    }

    for (Map.Entry<Long, Double> metric : timelineMetric.getMetricValues().entrySet()) {
      // TODO: investigate null values - pre filter
      if (metric.getValue() == null) {
        continue;
      }

      Long timestamp = getSliceTimeForMetric(timeSlices, Long.parseLong(metric.getKey().toString()));
      if (timestamp != -1) {
        // Metric is within desired time range
        TimelineClusterMetric clusterMetric = new TimelineClusterMetric(
          timelineMetric.getMetricName(),
          timelineMetric.getAppId(),
          timelineMetric.getInstanceId(),
          timestamp,
          timelineMetric.getType());

        // do a sum / count here to get average for all points in a slice
        int count = 1;
        Double sum;
        if (!timelineClusterMetricMap.containsKey(clusterMetric)) {
          sum = metric.getValue();
        } else {
          count++;
          Double oldValue = timelineClusterMetricMap.get(clusterMetric);
          sum = oldValue + metric.getValue();
        }
        timelineClusterMetricMap.put(clusterMetric, (sum / count));
      } else {
        if (timelineMetric.getMetricName().equals("tserver.general.entries")) {
          LOG.info("--- Fallen off: serverTs = " + timelineMetric.getTimestamp() +
            ", timeShift: " + timeShift +
            ", timestamp: " + Long.parseLong(metric.getKey().toString()) +
            ", host = " + timelineMetric.getHostName());
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
