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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.function;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import com.google.common.base.Joiner;

public abstract class AbstractTimelineMetricsSeriesAggregateFunction
    implements TimelineMetricsSeriesAggregateFunction {

  @Override
  public TimelineMetric apply(TimelineMetrics timelineMetrics) {
    Set<String> metricNameSet = new TreeSet<>();
    Set<String> hostNameSet = new TreeSet<>();
    Set<String> appIdSet = new TreeSet<>();
    Set<String> instanceIdSet = new TreeSet<>();
    TreeMap<Long, List<Double>> metricValues = new TreeMap<>();

    for (TimelineMetric timelineMetric : timelineMetrics.getMetrics()) {
      metricNameSet.add(timelineMetric.getMetricName());
      addToSetOnlyNotNull(hostNameSet, timelineMetric.getHostName());
      addToSetOnlyNotNull(appIdSet, timelineMetric.getAppId());
      addToSetOnlyNotNull(instanceIdSet, timelineMetric.getInstanceId());

      for (Map.Entry<Long, Double> metricValue : timelineMetric.getMetricValues().entrySet()) {
        Long timestamp = metricValue.getKey();
        Double value = metricValue.getValue();
        if (!metricValues.containsKey(timestamp)) {
          metricValues.put(timestamp, new LinkedList<Double>());
        }
        metricValues.get(timestamp).add(value);
      }
    }

    TreeMap<Long, Double> aggregatedMetricValues = new TreeMap<>();
    for (Map.Entry<Long, List<Double>> metricValue : metricValues.entrySet()) {
      List<Double> values = metricValue.getValue();
      if (values.size() == 0) {
        throw new IllegalArgumentException("count of values should be more than 0");
      }
      aggregatedMetricValues.put(metricValue.getKey(), applyFunction(values));
    }

    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName(getMetricName(metricNameSet.iterator()));
    timelineMetric.setHostName(joinStringsWithComma(hostNameSet.iterator()));
    timelineMetric.setAppId(joinStringsWithComma(appIdSet.iterator()));
    timelineMetric.setInstanceId(joinStringsWithComma(instanceIdSet.iterator()));
    if (aggregatedMetricValues.size() > 0) {
      timelineMetric.setStartTime(aggregatedMetricValues.firstKey());
    }
    timelineMetric.setMetricValues(aggregatedMetricValues);
    return timelineMetric;
  }

  protected String getMetricName(Iterator<String> metricNames) {
    return getFunctionName() + "(" + Joiner.on(",").join(metricNames) + ")";
  }

  protected String joinStringsWithComma(Iterator<String> hostNames) {
    return Joiner.on(",").join(hostNames);
  }

  protected abstract Double applyFunction(List<Double> values);
  protected abstract String getFunctionName();

  private void addToSetOnlyNotNull(Set<String> set, String value) {
    if (value != null) {
      set.add(value);
    }
  }

}
