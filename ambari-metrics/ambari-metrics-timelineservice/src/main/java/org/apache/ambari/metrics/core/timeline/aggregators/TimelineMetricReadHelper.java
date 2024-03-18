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


import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.SingleValuedTimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.ambari.metrics.core.timeline.PhoenixHBaseAccessor;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;

public class TimelineMetricReadHelper {

  private boolean ignoreInstance = false;
  private TimelineMetricMetadataManager metadataManagerInstance = null;

  public TimelineMetricReadHelper() {}

  public TimelineMetricReadHelper(boolean ignoreInstance) {
    this.ignoreInstance = ignoreInstance;
  }

  public TimelineMetricReadHelper(TimelineMetricMetadataManager timelineMetricMetadataManager) {
    this.metadataManagerInstance = timelineMetricMetadataManager;
  }

  public TimelineMetricReadHelper(TimelineMetricMetadataManager timelineMetricMetadataManager, boolean ignoreInstance) {
    this.metadataManagerInstance = timelineMetricMetadataManager;
    this.ignoreInstance = ignoreInstance;
  }

  public TimelineMetric getTimelineMetricFromResultSet(ResultSet rs)
    throws SQLException, IOException {
    TimelineMetric metric = getTimelineMetricCommonsFromResultSet(rs);
    if (metric == null) {
      return null;
    }
    TreeMap<Long, Double> sortedByTimeMetrics = PhoenixHBaseAccessor.readMetricFromJSON(rs.getString("METRICS"));
    metric.setMetricValues(sortedByTimeMetrics);
    return metric;
  }

  public SingleValuedTimelineMetric getAggregatedTimelineMetricFromResultSet(ResultSet rs,
                                                                             Function f,
                                                                             boolean shouldSumMetricAcrossTime) throws SQLException, IOException {

    byte[] uuid = rs.getBytes("UUID");
    TimelineMetric timelineMetric = metadataManagerInstance.getMetricFromUuid(uuid);
    Function function = (f != null) ? f : Function.DEFAULT_VALUE_FUNCTION;
    SingleValuedTimelineMetric metric = new SingleValuedTimelineMetric(
      timelineMetric.getMetricName() + function.getSuffix(),
      timelineMetric.getAppId(),
      timelineMetric.getInstanceId(),
      timelineMetric.getHostName(),
      rs.getLong("SERVER_TIME")
    );

    double value;
    // GET request for sum & avg is handled as the same since 'summing' of values across time does not make sense.
    // If explicit sum downsampling is required across time, we have to use ams-site : timeline.metrics.downsampler.event.metric.patterns.
    switch(function.getReadFunction()){
      case AVG:
        value = rs.getDouble("METRIC_SUM") / rs.getInt("METRIC_COUNT");
        break;
      case MIN:
        value = rs.getDouble("METRIC_MIN");
        break;
      case MAX:
        value = rs.getDouble("METRIC_MAX");
        break;
      case SUM:
        value = rs.getDouble("METRIC_SUM");
        if (!shouldSumMetricAcrossTime) {
          value = value / rs.getInt("METRIC_COUNT");
        }
        break;
      default:
        value = rs.getDouble("METRIC_SUM") / rs.getInt("METRIC_COUNT");
        break;
    }

    metric.setSingleTimeseriesValue(rs.getLong("SERVER_TIME"), value);

    return metric;
  }

  /**
   * Returns common part of timeline metrics record without the values.
   */
  public TimelineMetric getTimelineMetricCommonsFromResultSet(ResultSet rs)
    throws SQLException {

    byte[] uuid = rs.getBytes("UUID");
    TimelineMetric metric = metadataManagerInstance.getMetricFromUuid(uuid);
    if (metric == null) {
      return null;
    }
    if (ignoreInstance) {
      metric.setInstanceId(null);
    }
    metric.setStartTime(rs.getLong("SERVER_TIME"));
    return metric;
  }

  public MetricClusterAggregate getMetricClusterAggregateFromResultSet(ResultSet rs)
    throws SQLException {
    MetricClusterAggregate agg = new MetricClusterAggregate();
    agg.setSum(rs.getDouble("METRIC_SUM"));
    agg.setMax(rs.getDouble("METRIC_MAX"));
    agg.setMin(rs.getDouble("METRIC_MIN"));
    agg.setNumberOfHosts(rs.getInt("HOSTS_COUNT"));

    agg.setDeviation(0.0);

    return agg;
  }

  public MetricClusterAggregate getMetricClusterTimeAggregateFromResultSet(ResultSet rs)
    throws SQLException {
    MetricClusterAggregate agg = new MetricClusterAggregate();
    agg.setSum(rs.getDouble("METRIC_SUM"));
    agg.setMax(rs.getDouble("METRIC_MAX"));
    agg.setMin(rs.getDouble("METRIC_MIN"));
    agg.setNumberOfHosts(rs.getInt("METRIC_COUNT"));

    agg.setDeviation(0.0);

    return agg;
  }

  public TimelineClusterMetric fromResultSet(ResultSet rs) throws SQLException {

    byte[] uuid = rs.getBytes("UUID");
    TimelineMetric timelineMetric = metadataManagerInstance.getMetricFromUuid(uuid);
    if (timelineMetric == null) {
      return null;
    }
    return new TimelineClusterMetric(
      timelineMetric.getMetricName(),
      timelineMetric.getAppId(),
      ignoreInstance ? null : timelineMetric.getInstanceId(),
      rs.getLong("SERVER_TIME"));
  }

  public MetricHostAggregate getMetricHostAggregateFromResultSet(ResultSet rs)
    throws SQLException {
    MetricHostAggregate metricHostAggregate = new MetricHostAggregate();
    metricHostAggregate.setSum(rs.getDouble("METRIC_SUM"));
    metricHostAggregate.setMax(rs.getDouble("METRIC_MAX"));
    metricHostAggregate.setMin(rs.getDouble("METRIC_MIN"));
    metricHostAggregate.setNumberOfSamples(rs.getLong("METRIC_COUNT"));

    metricHostAggregate.setDeviation(0.0);
    return metricHostAggregate;
  }

  public TimelineMetric getTimelineMetricKeyFromResultSet(ResultSet rs)
    throws SQLException, IOException {
    byte[] uuid = rs.getBytes("UUID");
    return metadataManagerInstance.getMetricFromUuid(uuid);
  }
}

