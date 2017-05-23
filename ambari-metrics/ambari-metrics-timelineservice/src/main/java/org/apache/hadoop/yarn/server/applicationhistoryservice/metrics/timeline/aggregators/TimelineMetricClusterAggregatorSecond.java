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


import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.SERVER_SIDE_TIMESIFT_ADJUSTMENT;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_CLUSTER_AGGREGATOR_INTERPOLATION_ENABLED;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_AGGREGATION_SQL_FILTERS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_METRIC_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.PostProcessingUtil;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;

/**
 * Aggregates a metric across all hosts in the cluster. Reads metrics from
 * the precision table and saves into the aggregate.
 */
public class TimelineMetricClusterAggregatorSecond extends AbstractTimelineAggregator {
  public Long timeSliceIntervalMillis;
  private TimelineMetricReadHelper timelineMetricReadHelper = new TimelineMetricReadHelper(true);
  // Aggregator to perform app-level aggregates for host metrics
  private final TimelineMetricAppAggregator appAggregator;
  // 1 minute client side buffering adjustment
  private final Long serverTimeShiftAdjustment;
  private final boolean interpolationEnabled;
  private TimelineMetricMetadataManager metadataManagerInstance;
  private String skipAggrPatternStrings;

  public TimelineMetricClusterAggregatorSecond(AGGREGATOR_NAME aggregatorName,
                                               TimelineMetricMetadataManager metadataManager,
                                               PhoenixHBaseAccessor hBaseAccessor,
                                               Configuration metricsConf,
                                               String checkpointLocation,
                                               Long sleepIntervalMillis,
                                               Integer checkpointCutOffMultiplier,
                                               String aggregatorDisabledParam,
                                               String tableName,
                                               String outputTableName,
                                               Long nativeTimeRangeDelay,
                                               Long timeSliceInterval,
                                               MetricCollectorHAController haController) {
    super(aggregatorName, hBaseAccessor, metricsConf, checkpointLocation,
      sleepIntervalMillis, checkpointCutOffMultiplier, aggregatorDisabledParam,
      tableName, outputTableName, nativeTimeRangeDelay, haController);

    this.metadataManagerInstance = metadataManager;
    appAggregator = new TimelineMetricAppAggregator(metadataManager, metricsConf);
    this.timeSliceIntervalMillis = timeSliceInterval;
    this.serverTimeShiftAdjustment = Long.parseLong(metricsConf.get(SERVER_SIDE_TIMESIFT_ADJUSTMENT, "90000"));
    this.interpolationEnabled = Boolean.parseBoolean(metricsConf.get(TIMELINE_METRICS_CLUSTER_AGGREGATOR_INTERPOLATION_ENABLED, "true"));
    this.skipAggrPatternStrings = metricsConf.get(TIMELINE_METRIC_AGGREGATION_SQL_FILTERS);
  }

  @Override
  protected void aggregate(ResultSet rs, long startTime, long endTime) throws SQLException, IOException {
    // Account for time shift due to client side buffering by shifting the
    // timestamps with the difference between server time and series start time
    // Also, we do not want to look at the shift time period from the end as well since we can interpolate those points
    // that come earlier than the expected, during the next run.
    List<Long[]> timeSlices = getTimeSlices(startTime - serverTimeShiftAdjustment, endTime - serverTimeShiftAdjustment);
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

    List<String> metricNames = new ArrayList<>();
    boolean metricNamesNotCondition = false;

    if (!StringUtils.isEmpty(skipAggrPatternStrings)) {
      LOG.info("Skipping aggregation for metric patterns : " + skipAggrPatternStrings);
      metricNames.addAll(Arrays.asList(skipAggrPatternStrings.split(",")));
      metricNamesNotCondition = true;
    }

    Condition condition = new DefaultCondition(metricNames, null, null, null, startTime - serverTimeShiftAdjustment,
      endTime, null, null, true);
    condition.setMetricNamesNotCondition(metricNamesNotCondition);
    condition.setNoLimit();
    condition.setFetchSize(resultsetFetchSize);
    condition.setStatement(String.format(GET_METRIC_SQL,
      getQueryHint(startTime), METRICS_RECORD_TABLE_NAME));
    // Retaining order of the row-key avoids client side merge sort.
    condition.addOrderByColumn("METRIC_NAME");
    condition.addOrderByColumn("HOSTNAME");
    condition.addOrderByColumn("APP_ID");
    condition.addOrderByColumn("INSTANCE_ID");
    condition.addOrderByColumn("SERVER_TIME");
    return condition;
  }

  /**
   * Return time slices to normalize the timeseries data.
   */
  List<Long[]> getTimeSlices(long startTime, long endTime) {
    List<Long[]> timeSlices = new ArrayList<Long[]>();
    long sliceStartTime = startTime;
    while (sliceStartTime < endTime) {
      timeSlices.add(new Long[] { sliceStartTime, sliceStartTime + timeSliceIntervalMillis });
      sliceStartTime += timeSliceIntervalMillis;
    }
    return timeSlices;
  }

  Map<TimelineClusterMetric, MetricClusterAggregate> aggregateMetricsFromResultSet(ResultSet rs, List<Long[]> timeSlices)
      throws SQLException, IOException {
    Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    TimelineMetric metric = null;
    Map<String, MutableInt> hostedAppCounter = new HashMap<>();
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
          int numHosts = processAggregateClusterMetrics(aggregateClusterMetrics, metric, timeSlices);
          if (!hostedAppCounter.containsKey(metric.getAppId())) {
            hostedAppCounter.put(metric.getAppId(), new MutableInt(numHosts));
          } else {
            int currentHostCount = hostedAppCounter.get(metric.getAppId()).intValue();
            if (currentHostCount < numHosts) {
              hostedAppCounter.put(metric.getAppId(), new MutableInt(numHosts));
            }
          }
          metric = nextMetric;
        }
      }
    }
    // Process last metric
    if (metric != null) {
      int numHosts = processAggregateClusterMetrics(aggregateClusterMetrics, metric, timeSlices);
      if (!hostedAppCounter.containsKey(metric.getAppId())) {
        hostedAppCounter.put(metric.getAppId(), new MutableInt(numHosts));
      } else {
        int currentHostCount = hostedAppCounter.get(metric.getAppId()).intValue();
        if (currentHostCount < numHosts) {
          hostedAppCounter.put(metric.getAppId(), new MutableInt(numHosts));
        }
      }
    }

    // Add app level aggregates to save
    aggregateClusterMetrics.putAll(appAggregator.getAggregateClusterMetrics());

    // Add liveHosts per AppId metrics.
    long timestamp = timeSlices.get(timeSlices.size() - 1)[1];
    processLiveAppCountMetrics(aggregateClusterMetrics, hostedAppCounter, timestamp);

    return aggregateClusterMetrics;
  }

  /**
   * Slice metric values into interval specified by :
   * timeline.metrics.cluster.aggregator.minute.timeslice.interval
   * Normalize value by averaging them within the interval
   */
  protected int processAggregateClusterMetrics(Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics,
                                              TimelineMetric metric, List<Long[]> timeSlices) {
    // Create time slices
    TimelineMetricMetadataKey appKey =  new TimelineMetricMetadataKey(metric.getMetricName(), metric.getAppId());
    TimelineMetricMetadata metricMetadata = metadataManagerInstance.getMetadataCacheValue(appKey);

    if (metricMetadata != null && !metricMetadata.isSupportsAggregates()) {
      LOG.debug("Skipping cluster aggregation for " + metric.getMetricName());
      return 0;
    }

    Map<TimelineClusterMetric, Double> clusterMetrics = sliceFromTimelineMetric(metric, timeSlices);
    int numHosts = 0;

    if (clusterMetrics != null && !clusterMetrics.isEmpty()) {
      for (Map.Entry<TimelineClusterMetric, Double> clusterMetricEntry : clusterMetrics.entrySet()) {

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

        numHosts = aggregate.getNumberOfHosts();
        // Update app level aggregates
        appAggregator.processTimelineClusterMetric(clusterMetric, metric.getHostName(), avgValue);
      }
    }
    return numHosts;
  }

  protected Map<TimelineClusterMetric, Double> sliceFromTimelineMetric(
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

    Long prevTimestamp = -1l;
    TimelineClusterMetric prevMetric = null;
    int count = 0;
    double sum = 0.0;

    Map<Long,Double> timeSliceValueMap = new HashMap<>();
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

        if (prevTimestamp < 0 || timestamp.equals(prevTimestamp)) {
          Double newValue = metric.getValue();
          if (newValue > 0.0) {
            sum += newValue;
            count++;
          }
        } else {
          double metricValue = (count > 0) ? (sum / count) : 0.0;
            timelineClusterMetricMap.put(prevMetric, metricValue);
          timeSliceValueMap.put(prevMetric.getTimestamp(), metricValue);
          sum = metric.getValue();
          count = sum > 0.0 ? 1 : 0;
        }

        prevTimestamp = timestamp;
        prevMetric = clusterMetric;
      }
    }

    if (prevTimestamp > 0) {
      double metricValue = (count > 0) ? (sum / count) : 0.0;
      timelineClusterMetricMap.put(prevMetric, metricValue);
      timeSliceValueMap.put(prevTimestamp, metricValue);
    }

    if (interpolationEnabled) {
      interpolateMissingPeriods(timelineClusterMetricMap, timelineMetric, timeSlices, timeSliceValueMap);
    }

    return timelineClusterMetricMap;
  }

  private void interpolateMissingPeriods(Map<TimelineClusterMetric, Double> timelineClusterMetricMap,
                                         TimelineMetric timelineMetric,
                                         List<Long[]> timeSlices,
                                         Map<Long, Double> timeSliceValueMap) {


    if (StringUtils.isNotEmpty(timelineMetric.getType()) && "COUNTER".equalsIgnoreCase(timelineMetric.getType())) {
      //For Counter Based metrics, ok to do interpolation and extrapolation

      List<Long> requiredTimestamps = new ArrayList<>();
      for (Long[] timeSlice : timeSlices) {
        if (!timeSliceValueMap.containsKey(timeSlice[1])) {
          requiredTimestamps.add(timeSlice[1]);
        }
      }
      Map<Long, Double> interpolatedValuesMap = PostProcessingUtil.interpolate(timelineMetric.getMetricValues(), requiredTimestamps);

      if (interpolatedValuesMap != null) {
        for (Map.Entry<Long, Double> entry : interpolatedValuesMap.entrySet()) {
          Double interpolatedValue = entry.getValue();

          if (interpolatedValue != null) {
            TimelineClusterMetric clusterMetric = new TimelineClusterMetric(
              timelineMetric.getMetricName(),
              timelineMetric.getAppId(),
              timelineMetric.getInstanceId(),
              entry.getKey(),
              timelineMetric.getType());

            timelineClusterMetricMap.put(clusterMetric, interpolatedValue);
          } else {
            LOG.debug("Cannot compute interpolated value, hence skipping.");
          }
        }
      }
    } else {
      //For other metrics, ok to do only interpolation

      Double defaultNextSeenValue = null;
      if (MapUtils.isEmpty(timeSliceValueMap) && MapUtils.isNotEmpty(timelineMetric.getMetricValues())) {
        //If no value was found within the start_time based slices, but the metric has value in the server_time range,
        // use that.

        LOG.debug("No value found within range for metric : " + timelineMetric.getMetricName());
        Map.Entry<Long,Double> firstEntry  = timelineMetric.getMetricValues().firstEntry();
        defaultNextSeenValue = firstEntry.getValue();
        LOG.debug("Found a data point outside timeslice range: " + new Date(firstEntry.getKey()) + ": " + defaultNextSeenValue);
      }

      for (int sliceNum = 0; sliceNum < timeSlices.size(); sliceNum++) {
        Long[] timeSlice = timeSlices.get(sliceNum);

        if (!timeSliceValueMap.containsKey(timeSlice[1])) {
          LOG.debug("Found an empty slice : " + new Date(timeSlice[0]) + ", " + new Date(timeSlice[1]));

          Double lastSeenValue = null;
          int index = sliceNum - 1;
          Long[] prevTimeSlice = null;
          while (lastSeenValue == null && index >= 0) {
            prevTimeSlice = timeSlices.get(index--);
            lastSeenValue = timeSliceValueMap.get(prevTimeSlice[1]);
          }

          Double nextSeenValue = null;
          index = sliceNum + 1;
          Long[] nextTimeSlice = null;
          while (nextSeenValue == null && index < timeSlices.size()) {
            nextTimeSlice = timeSlices.get(index++);
            nextSeenValue = timeSliceValueMap.get(nextTimeSlice[1]);
          }

          if (nextSeenValue == null) {
            nextSeenValue = defaultNextSeenValue;
          }

          Double interpolatedValue = PostProcessingUtil.interpolate(timeSlice[1],
            (prevTimeSlice != null ? prevTimeSlice[1] : null), lastSeenValue,
            (nextTimeSlice != null ? nextTimeSlice[1] : null), nextSeenValue);

          if (interpolatedValue != null) {
            TimelineClusterMetric clusterMetric = new TimelineClusterMetric(
              timelineMetric.getMetricName(),
              timelineMetric.getAppId(),
              timelineMetric.getInstanceId(),
              timeSlice[1],
              timelineMetric.getType());

            LOG.debug("Interpolated value : " + interpolatedValue);
            timelineClusterMetricMap.put(clusterMetric, interpolatedValue);
          } else {
            LOG.debug("Cannot compute interpolated value, hence skipping.");
          }
        }
      }
    }
  }

  /**
   * Return end of the time slice into which the metric fits.
   */
  private Long getSliceTimeForMetric(List<Long[]> timeSlices, Long timestamp) {
    for (Long[] timeSlice : timeSlices) {
      if (timestamp > timeSlice[0] && timestamp <= timeSlice[1]) {
        return timeSlice[1];
      }
    }
    return -1l;
  }

  /* Add cluster metric for number of hosts that are hosting an appId */
  private void processLiveAppCountMetrics(Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics,
      Map<String, MutableInt> appHostsCount, long timestamp) {

    for (Map.Entry<String, MutableInt> appHostsEntry : appHostsCount.entrySet()) {
      TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(
        "live_hosts", appHostsEntry.getKey(), null, timestamp, null);

      Integer numOfHosts = appHostsEntry.getValue().intValue();

      MetricClusterAggregate metricClusterAggregate = new MetricClusterAggregate(
        (double) numOfHosts, 1, null, (double) numOfHosts, (double) numOfHosts);

      aggregateClusterMetrics.put(timelineClusterMetric, metricClusterAggregate);
    }

  }
}
