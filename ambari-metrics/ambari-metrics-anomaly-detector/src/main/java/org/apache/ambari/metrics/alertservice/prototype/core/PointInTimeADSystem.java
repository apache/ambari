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
package org.apache.ambari.metrics.alertservice.prototype.core;

import org.apache.ambari.metrics.alertservice.prototype.common.DataSeries;
import org.apache.ambari.metrics.alertservice.prototype.common.ResultSet;
import org.apache.ambari.metrics.alertservice.prototype.methods.ema.EmaModel;
import org.apache.ambari.metrics.alertservice.prototype.methods.ema.EmaTechnique;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PointInTimeADSystem implements Serializable {

  //private EmaTechnique emaTechnique;
  private MetricsCollectorInterface metricsCollectorInterface;
  private Map<String, Double> tukeysNMap;
  private double defaultTukeysN = 3;

  private long testIntervalMillis = 5*60*1000; //10mins
  private long trainIntervalMillis = 15*60*1000; //1hour

  private static final Log LOG = LogFactory.getLog(PointInTimeADSystem.class);

  private AmbariServerInterface ambariServerInterface;
  private int sensitivity = 50;
  private int minSensitivity = 0;
  private int maxSensitivity = 100;

  public PointInTimeADSystem(MetricsCollectorInterface metricsCollectorInterface, double defaultTukeysN,
                             long testIntervalMillis, long trainIntervalMillis, String ambariServerHost, String clusterName) {
    this.metricsCollectorInterface = metricsCollectorInterface;
    this.defaultTukeysN = defaultTukeysN;
    this.tukeysNMap = new HashMap<>();
    this.testIntervalMillis = testIntervalMillis;
    this.trainIntervalMillis = trainIntervalMillis;
    this.ambariServerInterface = new AmbariServerInterface(ambariServerHost, clusterName);
    LOG.info("Starting PointInTimeADSystem...");
  }

  public void runTukeysAndRefineEma(EmaTechnique emaTechnique, long startTime) {
    LOG.info("Running Tukeys for test data interval [" + new Date(startTime - testIntervalMillis) + " : " + new Date(startTime) + "], with train data period [" + new Date(startTime  - testIntervalMillis - trainIntervalMillis) + " : " + new Date(startTime - testIntervalMillis) + "]");

    int requiredSensivity = ambariServerInterface.getPointInTimeSensitivity();
    if (requiredSensivity == -1 || requiredSensivity == sensitivity) {
      LOG.info("No change in sensitivity needed.");
    } else {
      LOG.info("Current tukey's N value = " + defaultTukeysN);
      if (requiredSensivity > sensitivity) {
        int targetSensitivity = Math.min(maxSensitivity, requiredSensivity);
        while (sensitivity < targetSensitivity) {
          defaultTukeysN = defaultTukeysN + defaultTukeysN * 0.05;
          sensitivity++;
        }
      } else {
        int targetSensitivity = Math.max(minSensitivity, requiredSensivity);
        while (sensitivity > targetSensitivity) {
          defaultTukeysN = defaultTukeysN - defaultTukeysN * 0.05;
          sensitivity--;
        }
      }
      LOG.info("New tukey's N value = " + defaultTukeysN);
    }

    TimelineMetrics timelineMetrics = new TimelineMetrics();
    for (String metricKey : emaTechnique.getTrackedEmas().keySet()) {
      LOG.info("EMA key = " + metricKey);
      EmaModel emaModel = emaTechnique.getTrackedEmas().get(metricKey);
      String metricName = emaModel.getMetricName();
      String appId = emaModel.getAppId();
      String hostname = emaModel.getHostname();

      TimelineMetrics tukeysData = metricsCollectorInterface.fetchMetrics(metricName, appId, hostname, startTime - (testIntervalMillis + trainIntervalMillis),
        startTime);

      if (tukeysData.getMetrics().isEmpty()) {
        LOG.info("No metrics fetched for Tukeys, metricKey = " + metricKey);
        continue;
      }

      List<Double> trainTsList = new ArrayList<>();
      List<Double> trainDataList = new ArrayList<>();
      List<Double> testTsList = new ArrayList<>();
      List<Double> testDataList = new ArrayList<>();

      for (TimelineMetric metric : tukeysData.getMetrics()) {
        for (Long timestamp : metric.getMetricValues().keySet()) {
          if (timestamp <= (startTime - testIntervalMillis)) {
            trainDataList.add(metric.getMetricValues().get(timestamp));
            trainTsList.add((double)timestamp);
          } else {
            testDataList.add(metric.getMetricValues().get(timestamp));
            testTsList.add((double)timestamp);
          }
        }
      }

      if (trainDataList.isEmpty() || testDataList.isEmpty() || trainDataList.size() < testDataList.size()) {
        LOG.info("Not enough train/test data to perform analysis.");
        continue;
      }

      String tukeysTrainSeries = "tukeysTrainSeries";
      double[] trainTs = new double[trainTsList.size()];
      double[] trainData = new double[trainTsList.size()];
      for (int i = 0; i < trainTs.length; i++) {
        trainTs[i] = trainTsList.get(i);
        trainData[i] = trainDataList.get(i);
      }

      String tukeysTestSeries = "tukeysTestSeries";
      double[] testTs = new double[testTsList.size()];
      double[] testData = new double[testTsList.size()];
      for (int i = 0; i < testTs.length; i++) {
        testTs[i] = testTsList.get(i);
        testData[i] = testDataList.get(i);
      }

      LOG.info("Train Size = " + trainTs.length + ", Test Size = " + testTs.length);

      DataSeries tukeysTrainData = new DataSeries(tukeysTrainSeries, trainTs, trainData);
      DataSeries tukeysTestData = new DataSeries(tukeysTestSeries, testTs, testData);

      if (!tukeysNMap.containsKey(metricKey)) {
        tukeysNMap.put(metricKey, defaultTukeysN);
      }

      Map<String, String> configs = new HashMap<>();
      configs.put("tukeys.n", String.valueOf(tukeysNMap.get(metricKey)));

      ResultSet rs = RFunctionInvoker.tukeys(tukeysTrainData, tukeysTestData, configs);

      List<TimelineMetric> tukeysMetrics = getAsTimelineMetric(rs, metricName, appId, hostname);
      LOG.info("Tukeys anomalies size : " + tukeysMetrics.size());
      TreeMap<Long, Double> tukeysMetricValues = new TreeMap<>();

      for (TimelineMetric tukeysMetric : tukeysMetrics) {
        tukeysMetricValues.putAll(tukeysMetric.getMetricValues());
        timelineMetrics.addOrMergeTimelineMetric(tukeysMetric);
      }

      TimelineMetrics emaData = metricsCollectorInterface.fetchMetrics(metricKey, MetricsCollectorInterface.serviceName+"-ema", MetricsCollectorInterface.getDefaultLocalHostName(), startTime - testIntervalMillis, startTime);
      TreeMap<Long, Double> emaMetricValues = new TreeMap();
      if (!emaData.getMetrics().isEmpty()) {
        emaMetricValues = emaData.getMetrics().get(0).getMetricValues();
      }

      LOG.info("Ema anomalies size : " + emaMetricValues.size());
      int tp = 0;
      int tn = 0;
      int fp = 0;
      int fn = 0;

      for (double ts : testTs) {
        long timestamp = (long) ts;
        if (tukeysMetricValues.containsKey(timestamp)) {
          if (emaMetricValues.containsKey(timestamp)) {
            tp++;
          } else {
            fn++;
          }
        } else {
          if (emaMetricValues.containsKey(timestamp)) {
            fp++;
          } else {
            tn++;
          }
        }
      }

      double recall = (double) tp / (double) (tp + fn);
      double precision = (double) tp / (double) (tp + fp);
      LOG.info("----------------------------");
      LOG.info("Precision Recall values for " + metricKey);
      LOG.info("tp=" + tp + ", fp=" + fp + ", tn=" + tn + ", fn=" + fn);
      LOG.info("----------------------------");

      if (recall < 0.5) {
        LOG.info("Increasing EMA sensitivity by 10%");
        emaModel.updateModel(true, 5);
      } else if (precision < 0.5) {
        LOG.info("Decreasing EMA sensitivity by 10%");
        emaModel.updateModel(false, 5);
      }

    }

    if (emaTechnique.getTrackedEmas().isEmpty()){
      LOG.info("No EMA Technique keys tracked!!!!");
    }

    if (!timelineMetrics.getMetrics().isEmpty()) {
      metricsCollectorInterface.emitMetrics(timelineMetrics);
    }
  }

  private static List<TimelineMetric> getAsTimelineMetric(ResultSet result, String metricName, String appId, String hostname) {

    List<TimelineMetric> timelineMetrics = new ArrayList<>();

    if (result == null) {
      LOG.info("ResultSet from R call is null!!");
      return null;
    }

    if (result.resultset.size() > 0) {
      double[] ts = result.resultset.get(0);
      double[] metrics = result.resultset.get(1);
      double[] anomalyScore = result.resultset.get(2);
      for (int i = 0; i < ts.length; i++) {
        TimelineMetric timelineMetric = new TimelineMetric();
        timelineMetric.setMetricName(metricName + ":" + appId + ":" + hostname);
        timelineMetric.setHostName(MetricsCollectorInterface.getDefaultLocalHostName());
        timelineMetric.setAppId(MetricsCollectorInterface.serviceName + "-tukeys");
        timelineMetric.setInstanceId(null);
        timelineMetric.setStartTime((long) ts[i]);
        TreeMap<Long, Double> metricValues = new TreeMap<>();
        metricValues.put((long) ts[i], metrics[i]);

        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("method", "tukeys");
        if (String.valueOf(anomalyScore[i]).equals("infinity")) {
          LOG.info("Got anomalyScore = infinity for " + metricName + ":" + appId + ":" + hostname);
        } else {
          metadata.put("anomaly-score", String.valueOf(anomalyScore[i]));
        }
        timelineMetric.setMetadata(metadata);

        timelineMetric.setMetricValues(metricValues);
        timelineMetrics.add(timelineMetric);
      }
    }

    return timelineMetrics;
  }
}
