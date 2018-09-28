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


import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.SERVER_SIDE_TIMESIFT_ADJUSTMENT;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_CLUSTER_AGGREGATOR_INTERPOLATION_ENABLED;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_EVENT_METRIC_PATTERNS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_SUPPORT_MULTIPLE_CLUSTERS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_AGGREGATION_SQL_FILTERS;
import static org.apache.ambari.metrics.core.timeline.aggregators.AggregatorUtils.getTimeSlices;
import static org.apache.ambari.metrics.core.timeline.aggregators.AggregatorUtils.sliceFromTimelineMetric;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.GET_METRIC_SQL;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.hadoop.metrics2.sink.timeline.TimelineMetricUtils.getJavaMetricPatterns;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.ambari.metrics.core.timeline.PhoenixHBaseAccessor;
import org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME;
import org.apache.ambari.metrics.core.timeline.availability.MetricCollectorHAController;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.ambari.metrics.core.timeline.query.Condition;
import org.apache.ambari.metrics.core.timeline.query.DefaultCondition;

/**
 * Aggregates a metric across all hosts in the cluster. Reads metrics from
 * the precision table and saves into the aggregate.
 */
public class TimelineMetricClusterAggregatorSecond extends AbstractTimelineAggregator {
  public Long timeSliceIntervalMillis;
  private TimelineMetricReadHelper timelineMetricReadHelper;
  // Aggregator to perform app-level aggregates for host metrics
  private final TimelineMetricAppAggregator appAggregator;
  // 1 minute client side buffering adjustment
  protected final Long serverTimeShiftAdjustment;
  protected final boolean interpolationEnabled;
  private TimelineMetricMetadataManager metadataManagerInstance;
  private String skipAggrPatternStrings;
  private List<String> skipInterpolationMetricPatterns = new ArrayList<>();
  private final static String liveHostsMetricName = "live_hosts";

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
    String skipInterpolationMetricPatternStrings = metricsConf.get(TIMELINE_METRICS_EVENT_METRIC_PATTERNS, "");

    if (StringUtils.isNotEmpty(skipInterpolationMetricPatternStrings)) {
      LOG.info("Skipping Interpolation for patterns : " + skipInterpolationMetricPatternStrings);
      skipInterpolationMetricPatterns.addAll(getJavaMetricPatterns(skipInterpolationMetricPatternStrings));
    }

    if (Boolean.valueOf(metricsConf.get(TIMELINE_METRICS_SUPPORT_MULTIPLE_CLUSTERS, "false"))) {
      this.timelineMetricReadHelper = new TimelineMetricReadHelper(metadataManager);
    } else {
      this.timelineMetricReadHelper = new TimelineMetricReadHelper(metadataManager, true);
    }
  }

  @Override
  protected void aggregate(ResultSet rs, long startTime, long endTime) throws SQLException, IOException {
    // Account for time shift due to client side buffering by shifting the
    // timestamps with the difference between server time and series start time
    // Also, we do not want to look at the shift time period from the end as well since we can interpolate those points
    // that come earlier than the expected, during the next run.
    List<Long[]> timeSlices = getTimeSlices(startTime - serverTimeShiftAdjustment, endTime - serverTimeShiftAdjustment, timeSliceIntervalMillis);
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
      METRICS_RECORD_TABLE_NAME));
    // Retaining order of the row-key avoids client side merge sort.
    condition.addOrderByColumn("UUID");
    condition.addOrderByColumn("SERVER_TIME");
    return condition;
  }

  Map<TimelineClusterMetric, MetricClusterAggregate> aggregateMetricsFromResultSet(ResultSet rs, List<Long[]> timeSlices)
    throws SQLException, IOException {
    Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    TimelineMetric metric = null;
    Map<String, MutableInt> hostedAppCounter = new HashMap<>();
    if (rs.next()) {
      metric = timelineMetricReadHelper.getTimelineMetricFromResultSet(rs);
      while (metric == null && rs.next()) {
        metric = timelineMetricReadHelper.getTimelineMetricFromResultSet(rs);
      }

      // Call slice after all rows for a host are read
      while (rs.next()) {
        TimelineMetric nextMetric = timelineMetricReadHelper.getTimelineMetricFromResultSet(rs);
        // If rows belong to same host combine them before slicing. This
        // avoids issues across rows that belong to same hosts but get
        // counted as coming from different ones.
        if (nextMetric == null) {
          continue;
        }

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
    TimelineMetricMetadataKey appKey =  new TimelineMetricMetadataKey(metric.getMetricName(), metric.getAppId(), metric.getInstanceId());
    TimelineMetricMetadata metricMetadata = metadataManagerInstance.getMetadataCacheValue(appKey);

    if (metricMetadata != null && !metricMetadata.isSupportsAggregates()) {
      LOG.debug("Skipping cluster aggregation for " + metric.getMetricName());
      return 0;
    }

    boolean skipInterpolationForMetric = shouldInterpolationBeSkipped(metric.getMetricName());

    Map<TimelineClusterMetric, Double> clusterMetrics = sliceFromTimelineMetric(metric, timeSlices, !skipInterpolationForMetric && interpolationEnabled);

    return aggregateClusterMetricsFromSlices(clusterMetrics, aggregateClusterMetrics, metric.getHostName());
  }

  protected int aggregateClusterMetricsFromSlices(Map<TimelineClusterMetric, Double> clusterMetrics,
                                                  Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics,
                                                  String hostname) {

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
        appAggregator.processTimelineClusterMetric(clusterMetric, hostname, avgValue);
      }
    }
    return numHosts;
  }

  /* Add cluster metric for number of hosts that are hosting an appId */
  protected void processLiveAppCountMetrics(Map<TimelineClusterMetric, MetricClusterAggregate> aggregateClusterMetrics,
                                            Map<String, MutableInt> appHostsCount, long timestamp) {

    for (Map.Entry<String, MutableInt> appHostsEntry : appHostsCount.entrySet()) {
      TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(
        liveHostsMetricName, appHostsEntry.getKey(), null, timestamp);

      Integer numOfHosts = appHostsEntry.getValue().intValue();

      MetricClusterAggregate metricClusterAggregate = new MetricClusterAggregate(
        (double) numOfHosts, 1, null, (double) numOfHosts, (double) numOfHosts);

      metadataManagerInstance.getUuid(timelineClusterMetric, true);
      aggregateClusterMetrics.put(timelineClusterMetric, metricClusterAggregate);
    }
  }

  private boolean shouldInterpolationBeSkipped(String metricName) {
    for (String pattern : skipInterpolationMetricPatterns) {
      if (metricName.matches(pattern)) {
        return true;
      }
    }
    return false;
  }
}
