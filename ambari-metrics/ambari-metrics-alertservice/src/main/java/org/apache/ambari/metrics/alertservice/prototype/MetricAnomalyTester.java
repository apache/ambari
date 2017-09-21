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

import org.apache.ambari.metrics.alertservice.prototype.common.DataSeries;
import org.apache.ambari.metrics.alertservice.prototype.common.ResultSet;
import org.apache.ambari.metrics.alertservice.seriesgenerator.MetricSeriesGeneratorFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MetricAnomalyTester {

  public static String appId = MetricsCollectorInterface.serviceName;
  static final Log LOG = LogFactory.getLog(MetricAnomalyTester.class);
  static Map<String, TimelineMetric> timelineMetricMap = new HashMap<>();

  public static TimelineMetrics runTestAnomalyRequest(MetricAnomalyDetectorTestInput input) throws UnknownHostException {

    long currentTime = System.currentTimeMillis();
    TimelineMetrics timelineMetrics = new TimelineMetrics();
    String hostname = InetAddress.getLocalHost().getHostName();

    //Train data
    TimelineMetric metric1 = new TimelineMetric();
    if (StringUtils.isNotEmpty(input.getTrainDataName())) {
      metric1 = timelineMetricMap.get(input.getTrainDataName());
      if (metric1 == null) {
        metric1 = new TimelineMetric();
        double[] trainSeries = MetricSeriesGeneratorFactory.generateSeries(input.getTrainDataType(), input.getTrainDataSize(), input.getTrainDataConfigs());
        metric1.setMetricName(input.getTrainDataName());
        metric1.setAppId(appId);
        metric1.setHostName(hostname);
        metric1.setStartTime(currentTime);
        metric1.setInstanceId(null);
        metric1.setMetricValues(getAsTimeSeries(currentTime, trainSeries));
        timelineMetricMap.put(input.getTrainDataName(), metric1);
      }
      timelineMetrics.getMetrics().add(metric1);
    } else {
      LOG.error("No train data name specified");
    }

    //Test data
    TimelineMetric metric2 = new TimelineMetric();
    if (StringUtils.isNotEmpty(input.getTestDataName())) {
      metric2 = timelineMetricMap.get(input.getTestDataName());
      if (metric2 == null) {
        metric2 = new TimelineMetric();
        double[] testSeries = MetricSeriesGeneratorFactory.generateSeries(input.getTestDataType(), input.getTestDataSize(), input.getTestDataConfigs());
        metric2.setMetricName(input.getTestDataName());
        metric2.setAppId(appId);
        metric2.setHostName(hostname);
        metric2.setStartTime(currentTime);
        metric2.setInstanceId(null);
        metric2.setMetricValues(getAsTimeSeries(currentTime, testSeries));
        timelineMetricMap.put(input.getTestDataName(), metric2);
      }
      timelineMetrics.getMetrics().add(metric2);
    } else {
      LOG.warn("No test data name specified");
    }

    //Invoke method
    if (CollectionUtils.isNotEmpty(input.getMethods())) {
      RFunctionInvoker.setScriptsDir("/etc/ambari-metrics-collector/conf/R-scripts");
      for (String methodType : input.getMethods()) {
        ResultSet result = RFunctionInvoker.executeMethod(methodType, getAsDataSeries(metric1), getAsDataSeries(metric2), input.getMethodConfigs());
        TimelineMetric timelineMetric = getAsTimelineMetric(result, methodType, input, currentTime, hostname);
        if (timelineMetric != null) {
          timelineMetrics.getMetrics().add(timelineMetric);
        }
      }
    } else {
      LOG.warn("No anomaly method requested");
    }

    return timelineMetrics;
  }


  private static TimelineMetric getAsTimelineMetric(ResultSet result, String methodType, MetricAnomalyDetectorTestInput input, long currentTime, String hostname) {

    if (result == null) {
      return null;
    }

    TimelineMetric timelineMetric = new TimelineMetric();
    if (methodType.equals("tukeys") || methodType.equals("ema")) {
      timelineMetric.setMetricName(input.getTrainDataName() + "_" + input.getTestDataName() + "_" + methodType + "_" + currentTime);
      timelineMetric.setHostName(hostname);
      timelineMetric.setAppId(appId);
      timelineMetric.setInstanceId(null);
      timelineMetric.setStartTime(currentTime);

      TreeMap<Long, Double> metricValues = new TreeMap<>();
      if (result.resultset.size() > 0) {
        double[] ts = result.resultset.get(0);
        double[] metrics = result.resultset.get(1);
        for (int i = 0; i < ts.length; i++) {
          if (i == 0) {
            timelineMetric.setStartTime((long) ts[i]);
          }
          metricValues.put((long) ts[i], metrics[i]);
        }
      }
      timelineMetric.setMetricValues(metricValues);
      return timelineMetric;
    }
    return null;
  }


  private static TreeMap<Long, Double> getAsTimeSeries(long currentTime, double[] values) {

    long startTime = currentTime - (values.length - 1) * 60 * 1000;
    TreeMap<Long, Double> metricValues = new TreeMap<>();

    for (int i = 0; i < values.length; i++) {
      metricValues.put(startTime, values[i]);
      startTime += (60 * 1000);
    }
    return metricValues;
  }

  private static DataSeries getAsDataSeries(TimelineMetric timelineMetric) {

    TreeMap<Long, Double> metricValues = timelineMetric.getMetricValues();
    double[] timestamps = new double[metricValues.size()];
    double[] values = new double[metricValues.size()];
    int i = 0;

    for (Long timestamp : metricValues.keySet()) {
      timestamps[i] = timestamp;
      values[i++] = metricValues.get(timestamp);
    }
    return new DataSeries(timelineMetric.getMetricName() + "_" + timelineMetric.getAppId() + "_" + timelineMetric.getHostName(), timestamps, values);
  }
}
