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


import org.apache.hadoop.metrics2.sink.timeline.SingleValuedTimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

public class TimelineMetricReadHelper {

  private boolean ignoreInstance = false;

  public TimelineMetricReadHelper() {}

  public TimelineMetricReadHelper(boolean ignoreInstance) {
    this.ignoreInstance = ignoreInstance;
  }

  public TimelineMetric getTimelineMetricFromResultSet(ResultSet rs)
      throws SQLException, IOException {
    TimelineMetric metric = getTimelineMetricCommonsFromResultSet(rs);
    TreeMap<Long, Double> sortedByTimeMetrics =
      PhoenixHBaseAccessor.readMetricFromJSON(rs.getString("METRICS"));
    metric.setMetricValues(sortedByTimeMetrics);
    return metric;
  }

  public SingleValuedTimelineMetric getAggregatedTimelineMetricFromResultSet(ResultSet rs,
      Function f) throws SQLException, IOException {

    Function function = (f != null) ? f : Function.DEFAULT_VALUE_FUNCTION;
    SingleValuedTimelineMetric metric = new SingleValuedTimelineMetric(
      rs.getString("METRIC_NAME") + function.getSuffix(),
      rs.getString("APP_ID"),
      rs.getString("INSTANCE_ID"),
      rs.getString("HOSTNAME"),
      rs.getLong("SERVER_TIME"),
      rs.getLong("SERVER_TIME"),
      rs.getString("UNITS")
    );

    double value;
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
        value = rs.getDouble("METRIC_SUM") / rs.getInt("METRIC_COUNT");
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
    TimelineMetric metric = new TimelineMetric();
    metric.setMetricName(rs.getString("METRIC_NAME"));
    metric.setAppId(rs.getString("APP_ID"));
    if (!ignoreInstance) {
      metric.setInstanceId(rs.getString("INSTANCE_ID"));
    }
    metric.setHostName(rs.getString("HOSTNAME"));
    metric.setTimestamp(rs.getLong("SERVER_TIME"));
    metric.setStartTime(rs.getLong("START_TIME"));
    metric.setType(rs.getString("UNITS"));
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
    return new TimelineClusterMetric(
      rs.getString("METRIC_NAME"),
      rs.getString("APP_ID"),
      ignoreInstance ? null : rs.getString("INSTANCE_ID"),
      rs.getLong("SERVER_TIME"),
      rs.getString("UNITS"));
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
    TimelineMetric metric = new TimelineMetric();
    metric.setMetricName(rs.getString("METRIC_NAME"));
    metric.setAppId(rs.getString("APP_ID"));
    metric.setInstanceId(rs.getString("INSTANCE_ID"));
    metric.setHostName(rs.getString("HOSTNAME"));
    metric.setTimestamp(rs.getLong("SERVER_TIME"));
    metric.setType(rs.getString("UNITS"));
    return metric;
  }
}

