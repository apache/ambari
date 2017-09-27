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
package org.apache.ambari.metrics.adservice.prototype.testing.utilities;

/**
 * Class which was originally used to send test series from AMS to Spark through Kafka.
 */

public class TestMetricSeriesGenerator {
  //implements Runnable {

//  private Map<TestSeriesInputRequest, AbstractMetricSeries> configuredSeries = new HashMap<>();
//  private static final Log LOG = LogFactory.getLog(TestMetricSeriesGenerator.class);
//  private TimelineMetricStore metricStore;
//  private String hostname;
//
//  public TestMetricSeriesGenerator(TimelineMetricStore metricStore) {
//    this.metricStore = metricStore;
//    try {
//      this.hostname = InetAddress.getLocalHost().getHostName();
//    } catch (UnknownHostException e) {
//      e.printStackTrace();
//    }
//  }
//
//  public void addSeries(TestSeriesInputRequest inputRequest) {
//    if (!configuredSeries.containsKey(inputRequest)) {
//      AbstractMetricSeries metricSeries = MetricSeriesGeneratorFactory.generateSeries(inputRequest.getSeriesType(), inputRequest.getConfigs());
//      configuredSeries.put(inputRequest, metricSeries);
//      LOG.info("Added series " + inputRequest.getSeriesName());
//    }
//  }
//
//  public void removeSeries(String seriesName) {
//    boolean isPresent = false;
//    TestSeriesInputRequest tbd = null;
//    for (TestSeriesInputRequest inputRequest : configuredSeries.keySet()) {
//      if (inputRequest.getSeriesName().equals(seriesName)) {
//        isPresent = true;
//        tbd = inputRequest;
//      }
//    }
//    if (isPresent) {
//      LOG.info("Removing series " + seriesName);
//      configuredSeries.remove(tbd);
//    } else {
//      LOG.info("Series not found : " + seriesName);
//    }
//  }
//
//  @Override
//  public void run() {
//    long currentTime = System.currentTimeMillis();
//    TimelineMetrics timelineMetrics = new TimelineMetrics();
//
//    for (TestSeriesInputRequest input : configuredSeries.keySet()) {
//      AbstractMetricSeries metricSeries = configuredSeries.get(input);
//      TimelineMetric timelineMetric = new TimelineMetric();
//      timelineMetric.setMetricName(input.getSeriesName());
//      timelineMetric.setAppId("anomaly-engine-test-metric");
//      timelineMetric.setInstanceId(null);
//      timelineMetric.setStartTime(currentTime);
//      timelineMetric.setHostName(hostname);
//      TreeMap<Long, Double> metricValues = new TreeMap();
//      metricValues.put(currentTime, metricSeries.nextValue());
//      timelineMetric.setMetricValues(metricValues);
//      timelineMetrics.addOrMergeTimelineMetric(timelineMetric);
//      LOG.info("Emitting metric with appId = " + timelineMetric.getAppId());
//    }
//    try {
//      LOG.info("Publishing test metrics for " + timelineMetrics.getMetrics().size() + " series.");
//      metricStore.putMetrics(timelineMetrics);
//    } catch (Exception e) {
//      LOG.error(e);
//    }
//  }
}
