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

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AggregatorUtils.getTimeSlices;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricDistributedCache;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;

public class TimelineMetricClusterAggregatorSecondWithCacheSource extends TimelineMetricClusterAggregatorSecond {
  private TimelineMetricDistributedCache distributedCache;
  public TimelineMetricClusterAggregatorSecondWithCacheSource(AggregationTaskRunner.AGGREGATOR_NAME metricAggregateSecond, TimelineMetricMetadataManager metricMetadataManager, PhoenixHBaseAccessor hBaseAccessor, Configuration metricsConf, String checkpointLocation, long sleepIntervalMillis, int checkpointCutOffMultiplier, String aggregatorDisabledParam, String inputTableName, String outputTableName,
                                                              Long nativeTimeRangeDelay,
                                                              Long timeSliceInterval,
                                                              MetricCollectorHAController haController, TimelineMetricDistributedCache distributedCache) {
    super(metricAggregateSecond, metricMetadataManager, hBaseAccessor, metricsConf, checkpointLocation, sleepIntervalMillis, checkpointCutOffMultiplier, aggregatorDisabledParam, inputTableName, outputTableName, nativeTimeRangeDelay, timeSliceInterval, haController);
    this.distributedCache = distributedCache;
  }

  @Override
  public boolean doWork(long startTime, long endTime) {
    LOG.info("Start aggregation cycle @ " + new Date() + ", " +
          "startTime = " + new Date(startTime) + ", endTime = " + new Date(endTime));
    try {
      Map<String, Double> caheMetrics;
      if (LOG.isDebugEnabled()) {
        caheMetrics = distributedCache.getPointInTimeCacheMetrics();
        LOG.debug("Ignite metrics before eviction : " + caheMetrics);
      }

      LOG.info("Trying to evict elements from cache");
      Map<TimelineClusterMetric, MetricClusterAggregate> metricsFromCache = distributedCache.evictMetricAggregates(startTime - serverTimeShiftAdjustment, endTime - serverTimeShiftAdjustment);
      LOG.info(String.format("Evicted %s elements from cache.", metricsFromCache.size()));

      if (LOG.isDebugEnabled()) {
        caheMetrics = distributedCache.getPointInTimeCacheMetrics();
        LOG.debug("Ignite metrics after eviction : " + caheMetrics);
      }

      List<Long[]> timeSlices = getTimeSlices(startTime - serverTimeShiftAdjustment, endTime - serverTimeShiftAdjustment, timeSliceIntervalMillis);
      Map<TimelineClusterMetric, MetricClusterAggregate> result = aggregateMetricsFromMetricClusterAggregates(metricsFromCache, timeSlices);

      LOG.info("Saving " + result.size() + " metric aggregates.");
      hBaseAccessor.saveClusterAggregateRecords(result);
      LOG.info("End aggregation cycle @ " + new Date());
      return true;
    } catch (Exception e) {
      LOG.error("Exception during aggregation. ", e);
      return false;
    }
  }

  //Slices in cache could be different from aggregate slices, so need to recalculate. Counts hosted apps
  Map<TimelineClusterMetric, MetricClusterAggregate> aggregateMetricsFromMetricClusterAggregates(Map<TimelineClusterMetric, MetricClusterAggregate> metricsFromCache, List<Long[]> timeSlices) {
    //TODO add basic interpolation
    //TODO investigate if needed, maybe add config to disable/enable
    //count hosted apps
    Map<String, MutableInt> hostedAppCounter = new HashMap<>();
    for (Map.Entry<TimelineClusterMetric, MetricClusterAggregate> clusterMetricAggregateEntry : metricsFromCache.entrySet()) {
      int numHosts = clusterMetricAggregateEntry.getValue().getNumberOfHosts();
      String appId = clusterMetricAggregateEntry.getKey().getAppId();
      if (!hostedAppCounter.containsKey(appId)) {
        hostedAppCounter.put(appId, new MutableInt(numHosts));
      } else {
        int currentHostCount = hostedAppCounter.get(appId).intValue();
        if (currentHostCount < numHosts) {
          hostedAppCounter.put(appId, new MutableInt(numHosts));
        }
      }
    }

    // Add liveHosts per AppId metrics.
    processLiveAppCountMetrics(metricsFromCache, hostedAppCounter, timeSlices.get(timeSlices.size() - 1)[1]);

    return metricsFromCache;
  }

}