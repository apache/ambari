/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logfeeder.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.ambari.logfeeder.conf.LogFeederSecurityConfig;
import org.apache.ambari.logfeeder.conf.MetricsCollectorConfig;
import org.apache.ambari.logfeeder.plugin.common.MetricData;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class MetricsManager {
  private static final Logger LOG = Logger.getLogger(MetricsManager.class);

  private boolean isMetricsEnabled = false;
  private String appId = "logfeeder";

  private long lastPublishTimeMS = 0; // Let's do the first publish immediately
  private long lastFailedPublishTimeMS = System.currentTimeMillis(); // Reset the clock

  private int publishIntervalMS = 60 * 1000;
  private int maxMetricsBuffer = 60 * 60 * 1000; // If AMS is down, we should not keep the metrics in memory forever
  private HashMap<String, TimelineMetric> metricsMap = new HashMap<>();
  private LogFeederAMSClient amsClient = null;

  @Inject
  private MetricsCollectorConfig metricsCollectorConfig;

  @Inject
  private LogFeederSecurityConfig logFeederSecurityConfig;

  @PostConstruct
  public void init() {
    LOG.info("Initializing MetricsManager()");
    if (amsClient == null) {
      amsClient = new LogFeederAMSClient(metricsCollectorConfig, logFeederSecurityConfig);
    }

    if (amsClient.getCollectorUri(null) != null) {
      if (LogFeederUtil.hostName == null) {
        isMetricsEnabled = false;
        LOG.error("Failed getting hostname for node. Disabling publishing LogFeeder metrics");
      } else {
        isMetricsEnabled = true;
        LOG.info("LogFeeder Metrics is enabled. Metrics host=" + amsClient.getCollectorUri(null));
      }
    } else {
      LOG.info("LogFeeder Metrics publish is disabled");
    }
  }

  public boolean isMetricsEnabled() {
    return isMetricsEnabled;
  }

  public synchronized void useMetrics(List<MetricData> metricsList) {
    if (!isMetricsEnabled) {
      return;
    }
    LOG.info("useMetrics() metrics.size=" + metricsList.size());
    long currMS = System.currentTimeMillis();
    
    gatherMetrics(metricsList, currMS);
    publishMetrics(currMS);
  }

  private void gatherMetrics(List<MetricData> metricsList, long currMS) {
    Long currMSLong = new Long(currMS);
    for (MetricData metric : metricsList) {
      if (metric.metricsName == null) {
        LOG.debug("metric.metricsName is null");
        continue;
      }
      long currCount = metric.value;
      if (!metric.isPointInTime && metric.publishCount > 0 && currCount <= metric.prevPublishValue) {
        LOG.debug("Nothing changed. " + metric.metricsName + ", currCount=" + currCount + ", prevPublishCount=" +
            metric.prevPublishValue);
        continue;
      }
      metric.publishCount++;

      LOG.debug("Ensuring metrics=" + metric.metricsName);
      TimelineMetric timelineMetric = metricsMap.get(metric.metricsName);
      if (timelineMetric == null) {
        LOG.debug("Creating new metric obbject for " + metric.metricsName);
        timelineMetric = new TimelineMetric();
        timelineMetric.setMetricName(metric.metricsName);
        timelineMetric.setHostName(LogFeederUtil.hostName);
        timelineMetric.setAppId(appId);
        timelineMetric.setStartTime(currMS);
        timelineMetric.setType("Long");
        timelineMetric.setMetricValues(new TreeMap<Long, Double>());

        metricsMap.put(metric.metricsName, timelineMetric);
      }
      
      LOG.debug("Adding metrics=" + metric.metricsName);
      if (metric.isPointInTime) {
        timelineMetric.getMetricValues().put(currMSLong, new Double(currCount));
      } else {
        Double value = timelineMetric.getMetricValues().get(currMSLong);
        if (value == null) {
          value = new Double(0);
        }
        value += (currCount - metric.prevPublishValue);
        timelineMetric.getMetricValues().put(currMSLong, value);
        metric.prevPublishValue = currCount;
      }
    }
  }

  private void publishMetrics(long currMS) {
    if (!metricsMap.isEmpty() && currMS - lastPublishTimeMS > publishIntervalMS) {
      try {
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        timelineMetrics.setMetrics(new ArrayList<TimelineMetric>(metricsMap.values()));
        amsClient.emitMetrics(timelineMetrics);
        
        LOG.info("Published " + timelineMetrics.getMetrics().size() + " metrics to AMS");
        metricsMap.clear();
        lastPublishTimeMS = currMS;
      } catch (Throwable t) {
        LOG.warn("Error sending metrics to AMS.", t);
        if (currMS - lastFailedPublishTimeMS > maxMetricsBuffer) {
          LOG.error("AMS was not sent for last " + maxMetricsBuffer / 1000 +
              " seconds. Purging it and will start rebuilding it again");
          metricsMap.clear();
          lastFailedPublishTimeMS = currMS;
        }
      }
    } else {
      LOG.info("Not publishing metrics. metrics.size()=" + metricsMap.size() + ", lastPublished=" +
          (currMS - lastPublishTimeMS) / 1000 + " seconds ago, intervalConfigured=" + publishIntervalMS / 1000);
    }
  }

  public void setAmsClient(LogFeederAMSClient amsClient) {
    this.amsClient = amsClient;
  }
}
