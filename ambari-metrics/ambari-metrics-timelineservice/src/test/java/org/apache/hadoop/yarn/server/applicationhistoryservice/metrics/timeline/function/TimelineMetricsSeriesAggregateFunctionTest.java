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

import com.google.common.collect.Lists;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimelineMetricsSeriesAggregateFunctionTest {

  public static final double DELTA = 0.00000000001;

  @Test public void testSeriesAggregateBySummation() throws Exception {
    TimelineMetrics testMetrics = getTestObject();
    // all TimelineMetric are having same values
    TreeMap<Long, Double> metricValues = testMetrics.getMetrics().get(0).getMetricValues();

    TimelineMetricsSeriesAggregateFunction function = TimelineMetricsSeriesAggregateFunctionFactory
        .newInstance(SeriesAggregateFunction.SUM);
    TimelineMetric aggregatedMetric = function.apply(testMetrics);

    String aggregatedMetricName = aggregatedMetric.getMetricName();
    String aggregatedHostName = aggregatedMetric.getHostName();
    String aggregatedAppId = aggregatedMetric.getAppId();
    String aggregatedInstanceId = aggregatedMetric.getInstanceId();

    for (TimelineMetric testMetric : testMetrics.getMetrics()) {
      assertTrue(aggregatedMetricName.contains(testMetric.getMetricName()));
      assertTrue(aggregatedHostName.contains(testMetric.getHostName()));
      if (!testMetric.getMetricName().equals("byte_in.3")) {
        assertTrue(aggregatedAppId.contains(testMetric.getAppId()));
        assertTrue(aggregatedInstanceId.contains(testMetric.getInstanceId()));
      }
    }

    TreeMap<Long, Double> summationMetricValues = aggregatedMetric.getMetricValues();
    assertEquals(3, summationMetricValues.size());
    for (Map.Entry<Long, Double> tsAndValue : summationMetricValues.entrySet()) {
      assertEquals(metricValues.get(tsAndValue.getKey()) * 3, tsAndValue.getValue(), DELTA);
    }
  }

  @Test public void testSeriesAggregateByAverage() throws Exception {
    TimelineMetrics testMetrics = getTestObject();
    // all TimelineMetric are having same values
    TreeMap<Long, Double> metricValues = testMetrics.getMetrics().get(0).getMetricValues();

    TimelineMetricsSeriesAggregateFunction function = TimelineMetricsSeriesAggregateFunctionFactory
        .newInstance(SeriesAggregateFunction.AVG);
    TimelineMetric aggregatedMetric = function.apply(testMetrics);

    // checks only values, others are covered by testSeriesAggregateBySummation
    TreeMap<Long, Double> averageMetricValues = aggregatedMetric.getMetricValues();
    assertEquals(3, averageMetricValues.size());
    for (Map.Entry<Long, Double> tsAndValue : averageMetricValues.entrySet()) {
      assertEquals(metricValues.get(tsAndValue.getKey()), tsAndValue.getValue(), DELTA);
    }
  }

  @Test public void testSeriesAggregateByMax() throws Exception {
    TimelineMetrics testMetrics = getTestObject();

    // override metric values
    TreeMap<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(1L, 1.0);
    metricValues.put(2L, 2.0);
    metricValues.put(3L, 3.0);

    testMetrics.getMetrics().get(0).setMetricValues(metricValues);

    TreeMap<Long, Double> metricValues2 = new TreeMap<>();
    metricValues2.put(1L, 2.0);
    metricValues2.put(2L, 4.0);
    metricValues2.put(3L, 6.0);

    testMetrics.getMetrics().get(1).setMetricValues(metricValues2);

    TreeMap<Long, Double> metricValues3 = new TreeMap<>();
    metricValues3.put(1L, 3.0);
    metricValues3.put(2L, 6.0);
    metricValues3.put(3L, 9.0);

    testMetrics.getMetrics().get(2).setMetricValues(metricValues3);

    TimelineMetricsSeriesAggregateFunction function = TimelineMetricsSeriesAggregateFunctionFactory
        .newInstance(SeriesAggregateFunction.MAX);
    TimelineMetric aggregatedMetric = function.apply(testMetrics);

    // checks only values, others are covered by testSeriesAggregateBySummation
    TreeMap<Long, Double> maxMetricValues = aggregatedMetric.getMetricValues();
    assertEquals(3, maxMetricValues.size());
    for (Map.Entry<Long, Double> tsAndValue : maxMetricValues.entrySet()) {
      assertEquals(metricValues3.get(tsAndValue.getKey()), tsAndValue.getValue(), DELTA);
    }
  }

  @Test public void testSeriesAggregateByMin() throws Exception {
    TimelineMetrics testMetrics = getTestObject();

    // override metric values
    TreeMap<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(1L, 1.0);
    metricValues.put(2L, 2.0);
    metricValues.put(3L, 3.0);

    testMetrics.getMetrics().get(0).setMetricValues(metricValues);

    TreeMap<Long, Double> metricValues2 = new TreeMap<>();
    metricValues2.put(1L, 2.0);
    metricValues2.put(2L, 4.0);
    metricValues2.put(3L, 6.0);

    testMetrics.getMetrics().get(1).setMetricValues(metricValues2);

    TreeMap<Long, Double> metricValues3 = new TreeMap<>();
    metricValues3.put(1L, 3.0);
    metricValues3.put(2L, 6.0);
    metricValues3.put(3L, 9.0);

    testMetrics.getMetrics().get(2).setMetricValues(metricValues3);

    TimelineMetricsSeriesAggregateFunction function = TimelineMetricsSeriesAggregateFunctionFactory
        .newInstance(SeriesAggregateFunction.MIN);
    TimelineMetric aggregatedMetric = function.apply(testMetrics);

    // checks only values, others are covered by testSeriesAggregateBySummation
    TreeMap<Long, Double> minMetricValues = aggregatedMetric.getMetricValues();
    assertEquals(3, minMetricValues.size());
    for (Map.Entry<Long, Double> tsAndValue : minMetricValues.entrySet()) {
      assertEquals(metricValues.get(tsAndValue.getKey()), tsAndValue.getValue(), DELTA);
    }
  }

  private TimelineMetrics getTestObject() {
    TimelineMetric metric = new TimelineMetric();
    metric.setMetricName("byte_in.1");
    metric.setHostName("host1");
    metric.setAppId("app1");
    metric.setInstanceId("instance1");

    TimelineMetric metric2 = new TimelineMetric();
    metric2.setMetricName("byte_in.2");
    metric2.setHostName("host2");
    metric2.setAppId("app2");
    metric2.setInstanceId("instance2");

    TimelineMetric metric3 = new TimelineMetric();
    metric3.setMetricName("byte_in.3");
    metric3.setHostName("host3");
    // appId and instanceId for metric3 are null

    TreeMap<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(1L, 3.0);
    metricValues.put(2L, 2 * 3.0);
    metricValues.put(3L, 3 * 3.0);

    metric.setMetricValues(metricValues);
    metric2.setMetricValues(metricValues);
    metric3.setMetricValues(metricValues);

    TimelineMetrics metrics = new TimelineMetrics();
    metrics.setMetrics(Lists.newArrayList(metric, metric2, metric3));

    return metrics;
  }
}