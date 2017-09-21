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
package org.apache.ambari.metrics.alertservice.prototype;

import org.apache.ambari.metrics.alertservice.prototype.methods.MetricAnomaly;
import org.apache.ambari.metrics.alertservice.prototype.methods.ema.EmaTechnique;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.TreeMap;

public class TestTukeys {

  @BeforeClass
  public static void init() throws URISyntaxException {
    Assume.assumeTrue(System.getenv("R_HOME") != null);
  }

  @Test
  public void testPointInTimeDetectionSystem() throws UnknownHostException, URISyntaxException {

    URL url = ClassLoader.getSystemResource("R-scripts");
    String fullFilePath = new File(url.toURI()).getAbsolutePath();
    RFunctionInvoker.setScriptsDir(fullFilePath);

    MetricsCollectorInterface metricsCollectorInterface = new MetricsCollectorInterface("avijayan-ams-1.openstacklocal","http", "6188");

    EmaTechnique ema = new EmaTechnique(0.5, 3);
    long now = System.currentTimeMillis();

    TimelineMetric metric1 = new TimelineMetric();
    metric1.setMetricName("mm9");
    metric1.setHostName(MetricsCollectorInterface.getDefaultLocalHostName());
    metric1.setStartTime(now);
    metric1.setAppId("aa9");
    metric1.setInstanceId(null);
    metric1.setType("Integer");

    //Train
    TreeMap<Long, Double> metricValues = new TreeMap<Long, Double>();

    //2hr data.
    for (int i = 0; i < 120; i++) {
      double metric = 20000 + Math.random();
      metricValues.put(now - i * 60 * 1000, metric);
    }
    metric1.setMetricValues(metricValues);
    TimelineMetrics timelineMetrics = new TimelineMetrics();
    timelineMetrics.addOrMergeTimelineMetric(metric1);

    metricsCollectorInterface.emitMetrics(timelineMetrics);

    List<MetricAnomaly> anomalyList = ema.test(metric1);
    metricsCollectorInterface.publish(anomalyList);
//
//    PointInTimeADSystem pointInTimeADSystem = new PointInTimeADSystem(ema, metricsCollectorInterface, 3, 5*60*1000, 15*60*1000);
//    pointInTimeADSystem.runOnce();
//
//    List<MetricAnomaly> anomalyList2 = ema.test(metric1);
//
//    pointInTimeADSystem.runOnce();
//    List<MetricAnomaly> anomalyList3 = ema.test(metric1);
//
//    pointInTimeADSystem.runOnce();
//    List<MetricAnomaly> anomalyList4 = ema.test(metric1);
//
//    pointInTimeADSystem.runOnce();
//    List<MetricAnomaly> anomalyList5 = ema.test(metric1);
//
//    pointInTimeADSystem.runOnce();
//    List<MetricAnomaly> anomalyList6 = ema.test(metric1);
//
//    Assert.assertTrue(anomalyList6.size() < anomalyList.size());
  }
}
