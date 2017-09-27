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

import org.apache.ambari.metrics.alertservice.prototype.core.RFunctionInvoker;
import org.apache.ambari.metrics.alertservice.prototype.methods.MetricAnomaly;
import org.apache.ambari.metrics.alertservice.prototype.methods.ema.EmaTechnique;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.TreeMap;

import static org.apache.ambari.metrics.alertservice.prototype.TestRFunctionInvoker.getTS;

public class TestEmaTechnique {

  private static double[] ts;
  private static String fullFilePath;

  @BeforeClass
  public static void init() throws URISyntaxException {

    Assume.assumeTrue(System.getenv("R_HOME") != null);
    ts = getTS(1000);
    URL url = ClassLoader.getSystemResource("R-scripts");
    fullFilePath = new File(url.toURI()).getAbsolutePath();
    RFunctionInvoker.setScriptsDir(fullFilePath);
  }

  @Test
  public void testEmaInitialization() {

    EmaTechnique ema = new EmaTechnique(0.5, 3);
    Assert.assertTrue(ema.getTrackedEmas().isEmpty());
    Assert.assertTrue(ema.getStartingWeight() == 0.5);
    Assert.assertTrue(ema.getStartTimesSdev() == 2);
  }

  @Test
  public void testEma() {
    EmaTechnique ema = new EmaTechnique(0.5, 3);

    long now = System.currentTimeMillis();

    TimelineMetric metric1 = new TimelineMetric();
    metric1.setMetricName("M1");
    metric1.setHostName("H1");
    metric1.setStartTime(now - 1000);
    metric1.setAppId("A1");
    metric1.setInstanceId(null);
    metric1.setType("Integer");

    //Train
    TreeMap<Long, Double> metricValues = new TreeMap<Long, Double>();
    for (int i = 0; i < 50; i++) {
      double metric = 20000 + Math.random();
      metricValues.put(now - i * 100, metric);
    }
    metric1.setMetricValues(metricValues);
    List<MetricAnomaly> anomalyList = ema.test(metric1);
//    Assert.assertTrue(anomalyList.isEmpty());

    metricValues = new TreeMap<Long, Double>();
    for (int i = 0; i < 50; i++) {
      double metric = 20000 + Math.random();
      metricValues.put(now - i * 100, metric);
    }
    metric1.setMetricValues(metricValues);
    anomalyList = ema.test(metric1);
    Assert.assertTrue(!anomalyList.isEmpty());
    int l1 = anomalyList.size();

    Assert.assertTrue(ema.updateModel(metric1, false, 20));
    anomalyList = ema.test(metric1);
    int l2 = anomalyList.size();
    Assert.assertTrue(l2 < l1);

    Assert.assertTrue(ema.updateModel(metric1, true, 50));
    anomalyList = ema.test(metric1);
    int l3 = anomalyList.size();
    Assert.assertTrue(l3 > l2 && l3 > l1);

  }
}
