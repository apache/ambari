package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics;

import org.apache.ambari.metrics.alertservice.prototype.TestSeriesInputRequest;
import org.apache.ambari.metrics.alertservice.seriesgenerator.AbstractMetricSeries;
import org.apache.ambari.metrics.alertservice.seriesgenerator.MetricSeriesGeneratorFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricStore;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class TestMetricSeriesGenerator implements Runnable {

  private Map<TestSeriesInputRequest, AbstractMetricSeries> configuredSeries = new HashMap<>();
  private static final Log LOG = LogFactory.getLog(TestMetricSeriesGenerator.class);
  private TimelineMetricStore metricStore;
  private String hostname;

  public TestMetricSeriesGenerator(TimelineMetricStore metricStore) {
    this.metricStore = metricStore;
    try {
      this.hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }

  public void addSeries(TestSeriesInputRequest inputRequest) {
    if (!configuredSeries.containsKey(inputRequest)) {
      AbstractMetricSeries metricSeries = MetricSeriesGeneratorFactory.generateSeries(inputRequest.getSeriesType(), inputRequest.getConfigs());
      configuredSeries.put(inputRequest, metricSeries);
      LOG.info("Added series " + inputRequest.getSeriesName());
    }
  }

  public void removeSeries(String seriesName) {
    boolean isPresent = false;
    TestSeriesInputRequest tbd = null;
    for (TestSeriesInputRequest inputRequest : configuredSeries.keySet()) {
      if (inputRequest.getSeriesName().equals(seriesName)) {
        isPresent = true;
        tbd = inputRequest;
      }
    }
    if (isPresent) {
      LOG.info("Removing series " + seriesName);
      configuredSeries.remove(tbd);
    } else {
      LOG.info("Series not found : " + seriesName);
    }
  }

  @Override
  public void run() {
    long currentTime = System.currentTimeMillis();
    TimelineMetrics timelineMetrics = new TimelineMetrics();

    for (TestSeriesInputRequest input : configuredSeries.keySet()) {
      AbstractMetricSeries metricSeries = configuredSeries.get(input);
      TimelineMetric timelineMetric = new TimelineMetric();
      timelineMetric.setMetricName(input.getSeriesName());
      timelineMetric.setAppId("anomaly-engine-test-metric");
      timelineMetric.setInstanceId(null);
      timelineMetric.setStartTime(currentTime);
      timelineMetric.setHostName(hostname);
      TreeMap<Long, Double> metricValues = new TreeMap();
      metricValues.put(currentTime, metricSeries.nextValue());
      timelineMetric.setMetricValues(metricValues);
      timelineMetrics.addOrMergeTimelineMetric(timelineMetric);
      LOG.info("Emitting metric with appId = " + timelineMetric.getAppId());
    }
    try {
      LOG.info("Publishing test metrics for " + timelineMetrics.getMetrics().size() + " series.");
      metricStore.putMetrics(timelineMetrics);
    } catch (Exception e) {
      LOG.error(e);
    }
  }
}
