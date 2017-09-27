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
package org.apache.ambari.metrics.adservice.prototype.core;

import org.apache.ambari.metrics.adservice.prototype.methods.MetricAnomaly;
import org.apache.ambari.metrics.adservice.prototype.methods.hsdev.HsdevTechnique;
import org.apache.ambari.metrics.adservice.prototype.common.DataSeries;
import org.apache.ambari.metrics.adservice.prototype.methods.kstest.KSTechnique;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TrendADSystem implements Serializable {

  private MetricsCollectorInterface metricsCollectorInterface;
  private List<TrendMetric> trendMetrics;

  private long ksTestIntervalMillis = 10 * 60 * 1000;
  private long ksTrainIntervalMillis = 10 * 60 * 1000;
  private KSTechnique ksTechnique;

  private HsdevTechnique hsdevTechnique;
  private int hsdevNumHistoricalPeriods = 3;

  private Map<KsSingleRunKey, MetricAnomaly> trackedKsAnomalies = new HashMap<>();
  private static final Log LOG = LogFactory.getLog(TrendADSystem.class);
  private String inputFile = "";

  public TrendADSystem(MetricsCollectorInterface metricsCollectorInterface,
                       long ksTestIntervalMillis,
                       long ksTrainIntervalMillis,
                       int hsdevNumHistoricalPeriods) {

    this.metricsCollectorInterface = metricsCollectorInterface;
    this.ksTestIntervalMillis = ksTestIntervalMillis;
    this.ksTrainIntervalMillis = ksTrainIntervalMillis;
    this.hsdevNumHistoricalPeriods = hsdevNumHistoricalPeriods;

    this.ksTechnique = new KSTechnique();
    this.hsdevTechnique = new HsdevTechnique();

    trendMetrics = new ArrayList<>();
  }

  public void runKSTest(long currentEndTime, Set<TrendMetric> trendMetrics) {
    readInputFile(inputFile);

    long ksTestIntervalStartTime = currentEndTime - ksTestIntervalMillis;
    LOG.info("Running KS Test for test data interval [" + new Date(ksTestIntervalStartTime) + " : " +
      new Date(currentEndTime) + "], with train data period [" + new Date(ksTestIntervalStartTime - ksTrainIntervalMillis)
      + " : " + new Date(ksTestIntervalStartTime) + "]");

    for (TrendMetric metric : trendMetrics) {
      String metricName = metric.metricName;
      String appId = metric.appId;
      String hostname = metric.hostname;
      String key = metricName + ":" + appId + ":" + hostname;

      TimelineMetrics ksData = metricsCollectorInterface.fetchMetrics(metricName, appId, hostname, ksTestIntervalStartTime - ksTrainIntervalMillis,
        currentEndTime);

      if (ksData.getMetrics().isEmpty()) {
        LOG.info("No metrics fetched for KS, metricKey = " + key);
        continue;
      }

      List<Double> trainTsList = new ArrayList<>();
      List<Double> trainDataList = new ArrayList<>();
      List<Double> testTsList = new ArrayList<>();
      List<Double> testDataList = new ArrayList<>();

      for (TimelineMetric timelineMetric : ksData.getMetrics()) {
        for (Long timestamp : timelineMetric.getMetricValues().keySet()) {
          if (timestamp <= ksTestIntervalStartTime) {
            trainDataList.add(timelineMetric.getMetricValues().get(timestamp));
            trainTsList.add((double) timestamp);
          } else {
            testDataList.add(timelineMetric.getMetricValues().get(timestamp));
            testTsList.add((double) timestamp);
          }
        }
      }

      LOG.info("Train Data size : " + trainDataList.size() + ", Test Data Size : " + testDataList.size());
      if (trainDataList.isEmpty() || testDataList.isEmpty() || trainDataList.size() < testDataList.size()) {
        LOG.info("Not enough train/test data to perform KS analysis.");
        continue;
      }

      String ksTrainSeries = "KSTrainSeries";
      double[] trainTs = new double[trainTsList.size()];
      double[] trainData = new double[trainTsList.size()];
      for (int i = 0; i < trainTs.length; i++) {
        trainTs[i] = trainTsList.get(i);
        trainData[i] = trainDataList.get(i);
      }

      String ksTestSeries = "KSTestSeries";
      double[] testTs = new double[testTsList.size()];
      double[] testData = new double[testTsList.size()];
      for (int i = 0; i < testTs.length; i++) {
        testTs[i] = testTsList.get(i);
        testData[i] = testDataList.get(i);
      }

      LOG.info("Train Size = " + trainTs.length + ", Test Size = " + testTs.length);

      DataSeries ksTrainData = new DataSeries(ksTrainSeries, trainTs, trainData);
      DataSeries ksTestData = new DataSeries(ksTestSeries, testTs, testData);

      MetricAnomaly metricAnomaly = ksTechnique.runKsTest(key, ksTrainData, ksTestData);
      if (metricAnomaly == null) {
        LOG.info("No anomaly from KS test.");
      } else {
        LOG.info("Found Anomaly in KS Test. Publishing KS Anomaly metric....");
        TimelineMetric timelineMetric = getAsTimelineMetric(metricAnomaly,
          ksTestIntervalStartTime, currentEndTime, ksTestIntervalStartTime - ksTrainIntervalMillis, ksTestIntervalStartTime);
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        timelineMetrics.addOrMergeTimelineMetric(timelineMetric);
        metricsCollectorInterface.emitMetrics(timelineMetrics);

        trackedKsAnomalies.put(new KsSingleRunKey(ksTestIntervalStartTime, currentEndTime, metricName, appId, hostname), metricAnomaly);
      }
    }

    if (trendMetrics.isEmpty()) {
      LOG.info("No Trend metrics tracked!!!!");
    }

  }

  private TimelineMetric getAsTimelineMetric(MetricAnomaly metricAnomaly,
                                   long testStart,
                                   long testEnd,
                                   long trainStart,
                                   long trainEnd) {

    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName(metricAnomaly.getMetricKey());
    timelineMetric.setAppId(MetricsCollectorInterface.serviceName + "-" + metricAnomaly.getMethodType());
    timelineMetric.setInstanceId(null);
    timelineMetric.setHostName(MetricsCollectorInterface.getDefaultLocalHostName());
    timelineMetric.setStartTime(testEnd);
    HashMap<String, String> metadata = new HashMap<>();
    metadata.put("method", metricAnomaly.getMethodType());
    metadata.put("anomaly-score", String.valueOf(metricAnomaly.getAnomalyScore()));
    metadata.put("test-start-time", String.valueOf(testStart));
    metadata.put("train-start-time", String.valueOf(trainStart));
    metadata.put("train-end-time", String.valueOf(trainEnd));
    timelineMetric.setMetadata(metadata);
    TreeMap<Long,Double> metricValues = new TreeMap<>();
    metricValues.put(testEnd, metricAnomaly.getMetricValue());
    timelineMetric.setMetricValues(metricValues);
    return timelineMetric;

  }

  public void runHsdevMethod() {

    List<TimelineMetric> hsdevMetricAnomalies = new ArrayList<>();

    for (KsSingleRunKey ksSingleRunKey : trackedKsAnomalies.keySet()) {

      long hsdevTestEnd = ksSingleRunKey.endTime;
      long hsdevTestStart = ksSingleRunKey.startTime;

      long period = hsdevTestEnd - hsdevTestStart;

      long hsdevTrainStart = hsdevTestStart - (hsdevNumHistoricalPeriods) * period;
      long hsdevTrainEnd = hsdevTestStart;

      LOG.info("Running HSdev Test for test data interval [" + new Date(hsdevTestStart) + " : " +
        new Date(hsdevTestEnd) + "], with train data period [" + new Date(hsdevTrainStart)
        + " : " + new Date(hsdevTrainEnd) + "]");

      String metricName = ksSingleRunKey.metricName;
      String appId = ksSingleRunKey.appId;
      String hostname = ksSingleRunKey.hostname;
      String key = metricName + "_" + appId + "_" + hostname;

      TimelineMetrics hsdevData = metricsCollectorInterface.fetchMetrics(
        metricName,
        appId,
        hostname,
        hsdevTrainStart,
        hsdevTestEnd);

      if (hsdevData.getMetrics().isEmpty()) {
        LOG.info("No metrics fetched for HSDev, metricKey = " + key);
        continue;
      }

      List<Double> trainTsList = new ArrayList<>();
      List<Double> trainDataList = new ArrayList<>();
      List<Double> testTsList = new ArrayList<>();
      List<Double> testDataList = new ArrayList<>();

      for (TimelineMetric timelineMetric : hsdevData.getMetrics()) {
        for (Long timestamp : timelineMetric.getMetricValues().keySet()) {
          if (timestamp <= hsdevTestStart) {
            trainDataList.add(timelineMetric.getMetricValues().get(timestamp));
            trainTsList.add((double) timestamp);
          } else {
            testDataList.add(timelineMetric.getMetricValues().get(timestamp));
            testTsList.add((double) timestamp);
          }
        }
      }

      if (trainDataList.isEmpty() || testDataList.isEmpty() || trainDataList.size() < testDataList.size()) {
        LOG.info("Not enough train/test data to perform Hsdev analysis.");
        continue;
      }

      String hsdevTrainSeries = "HsdevTrainSeries";
      double[] trainTs = new double[trainTsList.size()];
      double[] trainData = new double[trainTsList.size()];
      for (int i = 0; i < trainTs.length; i++) {
        trainTs[i] = trainTsList.get(i);
        trainData[i] = trainDataList.get(i);
      }

      String hsdevTestSeries = "HsdevTestSeries";
      double[] testTs = new double[testTsList.size()];
      double[] testData = new double[testTsList.size()];
      for (int i = 0; i < testTs.length; i++) {
        testTs[i] = testTsList.get(i);
        testData[i] = testDataList.get(i);
      }

      LOG.info("Train Size = " + trainTs.length + ", Test Size = " + testTs.length);

      DataSeries hsdevTrainData = new DataSeries(hsdevTrainSeries, trainTs, trainData);
      DataSeries hsdevTestData = new DataSeries(hsdevTestSeries, testTs, testData);

      MetricAnomaly metricAnomaly = hsdevTechnique.runHsdevTest(key, hsdevTrainData, hsdevTestData);
      if (metricAnomaly == null) {
        LOG.info("No anomaly from Hsdev test. Mismatch between KS and HSDev. ");
        ksTechnique.updateModel(key, false, 10);
      } else {
        LOG.info("Found Anomaly in Hsdev Test. This confirms KS anomaly.");
        hsdevMetricAnomalies.add(getAsTimelineMetric(metricAnomaly,
          hsdevTestStart, hsdevTestEnd, hsdevTrainStart, hsdevTrainEnd));
      }
    }
    clearTrackedKsRunKeys();

    if (!hsdevMetricAnomalies.isEmpty()) {
      LOG.info("Publishing Hsdev Anomalies....");
      TimelineMetrics timelineMetrics = new TimelineMetrics();
      timelineMetrics.setMetrics(hsdevMetricAnomalies);
      metricsCollectorInterface.emitMetrics(timelineMetrics);
    }
  }

  private void clearTrackedKsRunKeys() {
    trackedKsAnomalies.clear();
  }

  private void readInputFile(String fileName) {
    trendMetrics.clear();
    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      for (String line; (line = br.readLine()) != null; ) {
        String[] splits = line.split(",");
        LOG.info("Adding a new metric to track in Trend AD system : " + splits[0]);
        trendMetrics.add(new TrendMetric(splits[0], splits[1], splits[2]));
      }
    } catch (IOException e) {
      LOG.error("Error reading input file : " + e);
    }
  }

  class KsSingleRunKey implements Serializable{

    long startTime;
    long endTime;
    String metricName;
    String appId;
    String hostname;

    public KsSingleRunKey(long startTime, long endTime, String metricName, String appId, String hostname) {
      this.startTime = startTime;
      this.endTime = endTime;
      this.metricName = metricName;
      this.appId = appId;
      this.hostname = hostname;
    }
  }
}
