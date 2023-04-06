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

import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;

import java.util.Arrays;
import java.util.TreeMap;

public class MetricTestHelper {

  public static MetricHostAggregate createMetricHostAggregate(double max, double min, int numberOfSamples, double sum) {
    MetricHostAggregate expectedAggregate = new MetricHostAggregate();
    expectedAggregate.setMax(max);
    expectedAggregate.setMin(min);
    expectedAggregate.setNumberOfSamples(numberOfSamples);
    expectedAggregate.setSum(sum);

    return expectedAggregate;
  }

  public static TimelineMetrics prepareSingleTimelineMetric(long startTime,
                                                            String host,
                                                            String metricName,
                                                            double val) {
    return prepareSingleTimelineMetric(startTime, host, null, metricName, val);
  }

  public static TimelineMetrics prepareSingleTimelineMetric(long startTime,
                                                            String host,
                                                            String instanceId,
                                                            String metricName,
                                                            double val) {
    TimelineMetrics m = new TimelineMetrics();
    m.setMetrics(Arrays.asList(
      createTimelineMetric(startTime, metricName, host, null, instanceId, val)));

    return m;
  }

  public static TimelineMetrics prepareSingleTimelineMetric(long startTime,
                                                            String host,
                                                            String appId,
                                                            String instanceId,
                                                            String metricName,
                                                            double val) {
    TimelineMetrics m = new TimelineMetrics();
    m.setMetrics(Arrays.asList(
      createTimelineMetric(startTime, metricName, host, appId, instanceId, val)));

    return m;
  }


  public static TimelineMetric createTimelineMetric(long startTime,
                                                    String metricName,
                                                    String host,
                                                    String appId,
                                                    String instanceId,
                                                    double val) {
    TimelineMetric m = new TimelineMetric();
    m.setHostName(host);
    m.setAppId(appId != null ? appId : "host");
    m.setInstanceId(instanceId);
    m.setMetricName(metricName);
    m.setStartTime(startTime);
    TreeMap<Long, Double> vals = new TreeMap<Long, Double>();
    vals.put(startTime + 15000l, val);
    vals.put(startTime + 30000l, val);
    vals.put(startTime + 45000l, val);
    vals.put(startTime + 60000l, val);

    m.setMetricValues(vals);

    return m;
  }

  public static TimelineMetric createEmptyTimelineMetric(long startTime) {
    TimelineMetric metric = new TimelineMetric();
    metric.setMetricName("disk_used");
    metric.setAppId("test_app");
    metric.setInstanceId("test_instance");
    metric.setHostName("test_host");
    metric.setStartTime(startTime);

    return metric;
  }

  public static TimelineMetric createEmptyTimelineMetric(String metricName, long startTime) {
    TimelineMetric metric = new TimelineMetric();
    metric.setMetricName(metricName);
    metric.setAppId("test_app");
    metric.setInstanceId("test_instance");
    metric.setHostName("test_host");
    metric.setStartTime(startTime);

    return metric;
  }

  public static TimelineClusterMetric createEmptyTimelineClusterMetric(
    String name, long startTime) {
    TimelineClusterMetric metric = new TimelineClusterMetric(name,
      "test_app", "instance_id", startTime);

    return metric;
  }

  public static TimelineClusterMetric createEmptyTimelineClusterMetric(
    long startTime) {
    return createEmptyTimelineClusterMetric("disk_used", startTime);
  }
}
