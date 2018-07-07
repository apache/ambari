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

import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Aggregator Memory sink implementation to perform test
 */
public class TimelineMetricsAggregatorMemorySink
    implements TimelineMetricsAggregatorSink {

  private static Map<Precision, Map<TimelineMetric, MetricHostAggregate>> hostAggregateRecords =
      new HashMap<>();
  private static Map<Precision, Map<TimelineClusterMetric, MetricHostAggregate>> clusterTimeAggregateRecords =
      new HashMap<>();
  private static Map<TimelineClusterMetric, MetricClusterAggregate> clusterAggregateRecords =
      new HashMap<>();

  @Override
  public void saveHostAggregateRecords(
      Map<TimelineMetric, MetricHostAggregate> hostAggregateMap,
      Precision precision) {
    if (hostAggregateMap == null || hostAggregateMap.size() == 0) {
      return;
    }

    Map<TimelineMetric, MetricHostAggregate> aggregatedValue = null;
    if (hostAggregateRecords.containsKey(precision)) {
      aggregatedValue = hostAggregateRecords.get(precision);
    } else {
      aggregatedValue = new HashMap<>();
      hostAggregateRecords.put(precision, aggregatedValue);
    }

    for (Entry<TimelineMetric, MetricHostAggregate> entry : hostAggregateMap
        .entrySet()) {
      TimelineMetric timelineMetricClone = new TimelineMetric(entry.getKey());
      MetricHostAggregate hostAggregate = entry.getValue();
      MetricHostAggregate hostAggregateClone = new MetricHostAggregate(
          hostAggregate.getSum(), (int) hostAggregate.getNumberOfSamples(),
          hostAggregate.getDeviation(), hostAggregate.getMax(),
          hostAggregate.getMin());
      aggregatedValue.put(timelineMetricClone, hostAggregateClone);
    }
  }

  @Override
  public void saveClusterTimeAggregateRecords(
      Map<TimelineClusterMetric, MetricHostAggregate> clusterTimeAggregateMap,
      Precision precision) {
    if (clusterTimeAggregateMap == null
        || clusterTimeAggregateMap.size() == 0) {
      return;
    }

    Map<TimelineClusterMetric, MetricHostAggregate> aggregatedValue = null;
    if (clusterTimeAggregateRecords.containsKey(precision)) {
      aggregatedValue = clusterTimeAggregateRecords.get(precision);
    } else {
      aggregatedValue = new HashMap<>();
      clusterTimeAggregateRecords.put(precision, aggregatedValue);
    }

    for (Entry<TimelineClusterMetric, MetricHostAggregate> entry : clusterTimeAggregateMap
        .entrySet()) {
      TimelineClusterMetric clusterMetric = entry.getKey();
      TimelineClusterMetric clusterMetricClone =
          new TimelineClusterMetric(clusterMetric.getMetricName(),
              clusterMetric.getAppId(), clusterMetric.getInstanceId(),
              clusterMetric.getTimestamp());
      MetricHostAggregate hostAggregate = entry.getValue();
      MetricHostAggregate hostAggregateClone = new MetricHostAggregate(
          hostAggregate.getSum(), (int) hostAggregate.getNumberOfSamples(),
          hostAggregate.getDeviation(), hostAggregate.getMax(),
          hostAggregate.getMin());
      aggregatedValue.put(clusterMetricClone, hostAggregateClone);
    }
  }

  @Override
  public void saveClusterAggregateRecords(
      Map<TimelineClusterMetric, MetricClusterAggregate> clusterAggregateMaps) {

    if (clusterAggregateMaps == null || clusterAggregateMaps.size() == 0) {
      return;
    }

    for (Entry<TimelineClusterMetric, MetricClusterAggregate> entry : clusterAggregateMaps
        .entrySet()) {
      TimelineClusterMetric clusterMetric = entry.getKey();
      TimelineClusterMetric clusterMetricClone =
          new TimelineClusterMetric(clusterMetric.getMetricName(),
              clusterMetric.getAppId(), clusterMetric.getInstanceId(),
              clusterMetric.getTimestamp());
      MetricClusterAggregate clusterAggregate = entry.getValue();
      MetricClusterAggregate clusterAggregateClone = new MetricClusterAggregate(
          clusterAggregate.getSum(), (int) clusterAggregate.getNumberOfHosts(),
          clusterAggregate.getDeviation(), clusterAggregate.getMax(),
          clusterAggregate.getMin());
      clusterAggregateRecords.put(clusterMetricClone, clusterAggregateClone);
    }
  }

  public Map<Precision, Map<TimelineMetric, MetricHostAggregate>> getHostAggregateRecords() {
    return Collections.unmodifiableMap(hostAggregateRecords);
  }

  public Map<Precision, Map<TimelineClusterMetric, MetricHostAggregate>> getClusterTimeAggregateRecords() {
    return Collections.unmodifiableMap(clusterTimeAggregateRecords);
  }

  public Map<TimelineClusterMetric, MetricClusterAggregate> getClusterAggregateRecords() {
    return Collections.unmodifiableMap(clusterAggregateRecords);
  }

}
