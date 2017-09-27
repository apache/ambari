/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.adservice.prototype;

import org.apache.ambari.metrics.adservice.prototype.common.DataSeries;
import org.apache.ambari.metrics.adservice.prototype.common.ResultSet;
import org.apache.ambari.metrics.adservice.prototype.core.RFunctionInvoker;
import org.apache.ambari.metrics.adservice.seriesgenerator.UniformMetricSeries;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TestRFunctionInvoker {

  private static String metricName = "TestMetric";
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
  public void testTukeys() throws URISyntaxException {

    double[] train_ts = ArrayUtils.subarray(ts, 0, 750);
    double[] train_x = getRandomData(750);
    DataSeries trainData = new DataSeries(metricName, train_ts, train_x);

    double[] test_ts = ArrayUtils.subarray(ts, 750, 1000);
    double[] test_x = getRandomData(250);
    test_x[50] = 5.5; //Anomaly
    DataSeries testData = new DataSeries(metricName, test_ts, test_x);
    Map<String, String> configs = new HashMap();
    configs.put("tukeys.n", "3");

    ResultSet rs = RFunctionInvoker.tukeys(trainData, testData, configs);
    Assert.assertEquals(rs.resultset.size(), 2);
    Assert.assertEquals(rs.resultset.get(1)[0], 5.5, 0.1);

  }

  public static void main(String[] args) throws URISyntaxException {

    String metricName = "TestMetric";
    double[] ts = getTS(1000);
    URL url = ClassLoader.getSystemResource("R-scripts");
    String fullFilePath = new File(url.toURI()).getAbsolutePath();
    RFunctionInvoker.setScriptsDir(fullFilePath);

    double[] train_ts = ArrayUtils.subarray(ts, 0, 750);
    double[] train_x = getRandomData(750);
    DataSeries trainData = new DataSeries(metricName, train_ts, train_x);

    double[] test_ts = ArrayUtils.subarray(ts, 750, 1000);
    double[] test_x = getRandomData(250);
    test_x[50] = 5.5; //Anomaly
    DataSeries testData = new DataSeries(metricName, test_ts, test_x);
    ResultSet rs;

    Map<String, String> configs = new HashMap();

    System.out.println("TUKEYS");
    configs.put("tukeys.n", "3");
    rs = RFunctionInvoker.tukeys(trainData, testData, configs);
    rs.print();
    System.out.println("--------------");

//    System.out.println("EMA Global");
//    configs.put("ema.n", "3");
//    configs.put("ema.w", "0.8");
//    rs = RFunctionInvoker.ema_global(trainData, testData, configs);
//    rs.print();
//    System.out.println("--------------");
//
//    System.out.println("EMA Daily");
//    rs = RFunctionInvoker.ema_daily(trainData, testData, configs);
//    rs.print();
//    System.out.println("--------------");
//
//    configs.put("ks.p_value", "0.00005");
//    System.out.println("KS Test");
//    rs = RFunctionInvoker.ksTest(trainData, testData, configs);
//    rs.print();
//    System.out.println("--------------");
//
    ts = getTS(5000);
    train_ts = ArrayUtils.subarray(ts, 0, 4800);
    train_x = getRandomData(4800);
    trainData = new DataSeries(metricName, train_ts, train_x);
    test_ts = ArrayUtils.subarray(ts, 4800, 5000);
    test_x = getRandomData(200);
    for (int i = 0; i < 200; i++) {
      test_x[i] = test_x[i] * 5;
    }
    testData = new DataSeries(metricName, test_ts, test_x);
    configs.put("hsdev.n", "3");
    configs.put("hsdev.nhp", "3");
    configs.put("hsdev.interval", "86400000");
    configs.put("hsdev.period", "604800000");
    System.out.println("HSdev");
    rs = RFunctionInvoker.hsdev(trainData, testData, configs);
    rs.print();
    System.out.println("--------------");

  }

  static double[] getTS(int n) {
    long currentTime = System.currentTimeMillis();
    double[] ts = new double[n];
    currentTime = currentTime - (currentTime % (5 * 60 * 1000));

    for (int i = 0, j = n - 1; i < n; i++, j--) {
      ts[j] = currentTime;
      currentTime = currentTime - (5 * 60 * 1000);
    }
    return ts;
  }

  static double[] getRandomData(int n) {

    UniformMetricSeries metricSeries =  new UniformMetricSeries(10, 0.1,0.05, 0.6, 0.8, true);
    return metricSeries.getSeries(n);

//    double[] metrics = new double[n];
//    Random random = new Random();
//    for (int i = 0; i < n; i++) {
//      metrics[i] = random.nextDouble();
//    }
//    return metrics;
  }
}
