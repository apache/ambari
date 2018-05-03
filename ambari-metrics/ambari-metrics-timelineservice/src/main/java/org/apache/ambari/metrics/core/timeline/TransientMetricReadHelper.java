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

import com.google.common.collect.Multimap;
import org.apache.ambari.metrics.core.timeline.aggregators.Function;
import org.apache.ambari.metrics.core.timeline.query.Condition;
import org.apache.hadoop.metrics2.sink.timeline.SingleValuedTimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import static org.apache.ambari.metrics.core.timeline.FunctionUtils.findMetricFunctions;

class TransientMetricReadHelper {

  static void appendMetricFromResultSet(TimelineMetrics metrics, Condition condition,
                                        Multimap<String, List<Function>> metricFunctions,
                                        ResultSet rs) throws SQLException, IOException {
    String metricName = rs.getString("METRIC_NAME");
    Collection<List<Function>> functionList = findMetricFunctions(metricFunctions, metricName);

    for (List<Function> functions : functionList) {
      // Apply aggregation function if present
      if ((functions != null && !functions.isEmpty())) {
        if (functions.size() > 1) {
          throw new IllegalArgumentException("Multiple aggregate functions not supported.");
        }
        for (Function f : functions) {
          if (f.getReadFunction() == Function.ReadFunction.VALUE) {
            getTimelineMetricsFromResultSet(metrics, f, condition, rs);
          } else {
            SingleValuedTimelineMetric metric = getAggregatedTimelineMetricFromResultSet(rs, f);
            if (condition.isGrouped()) {
              metrics.addOrMergeTimelineMetric(metric);
            } else {
              metrics.getMetrics().add(metric.getTimelineMetric());
            }
          }
        }
      } else {
        // No aggregation requested
        // Execution never goes here, function always contain at least 1 element
        getTimelineMetricsFromResultSet(metrics, null, condition, rs);
      }
    }
  }

  private static TimelineMetric getTimelineMetricFromResultSet(ResultSet rs)
    throws SQLException, IOException {
    TimelineMetric metric = getTimelineMetricCommonsFromResultSet(rs);
    TreeMap<Long, Double> sortedByTimeMetrics = PhoenixHBaseAccessor.readMetricFromJSON(rs.getString("METRICS"));
    metric.setMetricValues(sortedByTimeMetrics);
    return metric;
  }

  private static TimelineMetric getTimelineMetricCommonsFromResultSet(ResultSet rs)
    throws SQLException {
    TimelineMetric metric = new TimelineMetric();
    metric.setMetricName(rs.getString("METRIC_NAME"));
    metric.setAppId(rs.getString("APP_ID"));
    metric.setInstanceId(rs.getString("INSTANCE_ID"));
    metric.setHostName(rs.getString("HOSTNAME"));
    metric.setStartTime(rs.getLong("SERVER_TIME"));
    metric.setType(rs.getString("UNITS"));
    return metric;
  }

  private static void getTimelineMetricsFromResultSet(TimelineMetrics metrics, Function f, Condition condition, ResultSet rs) throws SQLException, IOException {
    TimelineMetric metric = getTimelineMetricFromResultSet(rs);
    if (f != null && f.getSuffix() != null) {
      metric.setMetricName(metric.getMetricName() + f.getSuffix());
    }
    if (condition.isGrouped()) {
      metrics.addOrMergeTimelineMetric(metric);
    } else {
      metrics.getMetrics().add(metric);
    }
  }


  private static SingleValuedTimelineMetric getAggregatedTimelineMetricFromResultSet(ResultSet rs,
                                                                                     Function f) throws SQLException, IOException {

    Function function = (f != null) ? f : Function.DEFAULT_VALUE_FUNCTION;
    SingleValuedTimelineMetric metric = new SingleValuedTimelineMetric(
      rs.getString("METRIC_NAME") + function.getSuffix(),
      rs.getString("APP_ID"),
      rs.getString("INSTANCE_ID"),
      rs.getString("HOSTNAME"),
      rs.getLong("SERVER_TIME")
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
        value = rs.getDouble("METRIC_SUM");
        break;
      default:
        value = rs.getDouble("METRIC_SUM") / rs.getInt("METRIC_COUNT");
        break;
    }

    metric.setSingleTimeseriesValue(rs.getLong("SERVER_TIME"), value);

    return metric;
  }
}
