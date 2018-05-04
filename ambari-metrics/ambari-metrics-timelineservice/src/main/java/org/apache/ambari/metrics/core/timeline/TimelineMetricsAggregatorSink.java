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

import java.util.Map;

import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;

/**
 * This Interface for storing aggregated metrics to any external storage
 */
public interface TimelineMetricsAggregatorSink {

  /**
   * Save host aggregated metrics
   *
   * @param hostAggregateMap Map of host aggregated metrics
   * @param precision SECOND, MINUTE, HOUR, DAY
   */
  void saveHostAggregateRecords(
      Map<TimelineMetric, MetricHostAggregate> hostAggregateMap,
      Precision precision);

  /**
   * Save cluster time aggregated metrics
   *
   * @param clusterTimeAggregateMap Map of cluster aggregated metrics
   * @param precision SECOND, MINUTE, HOUR, DAY
   */
  void saveClusterTimeAggregateRecords(
      Map<TimelineClusterMetric, MetricHostAggregate> clusterTimeAggregateMap,
      Precision precision);

  /**
   * Save cluster aggregated metrics
   *
   * @param clusterAggregateMap Map of cluster aggregated metrics
   */
  void saveClusterAggregateRecords(
      Map<TimelineClusterMetric, MetricClusterAggregate> clusterAggregateMap);
}
