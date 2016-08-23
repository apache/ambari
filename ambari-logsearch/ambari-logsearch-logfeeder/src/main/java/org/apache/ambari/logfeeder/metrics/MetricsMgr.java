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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.log4j.Logger;

public class MetricsMgr {
  private static final Logger logger = Logger.getLogger(MetricsMgr.class);

  private boolean isMetricsEnabled = false;
  private String nodeHostName = null;
  private String appId = "logfeeder";

  private long lastPublishTimeMS = 0; // Let's do the first publish immediately
  private long lastFailedPublishTimeMS = System.currentTimeMillis(); // Reset the clock

  private int publishIntervalMS = 60 * 1000;
  private int maxMetricsBuffer = 60 * 60 * 1000; // If AMS is down, we should not keep
  // the metrics in memory forever
  private HashMap<String, TimelineMetric> metricsMap = new HashMap<String, TimelineMetric>();
  private LogFeederAMSClient amsClient = null;

  public void init() {
    logger.info("Initializing MetricsMgr()");
    amsClient = new LogFeederAMSClient();

    if (amsClient.getCollectorUri() != null) {
      nodeHostName = LogFeederUtil.getStringProperty("node.hostname");
      if (nodeHostName == null) {
        try {
          nodeHostName = InetAddress.getLocalHost().getHostName();
        } catch (Throwable e) {
          logger.warn(
            "Error getting hostname using InetAddress.getLocalHost().getHostName()",
            e);
        }
        if (nodeHostName == null) {
          try {
            nodeHostName = InetAddress.getLocalHost()
              .getCanonicalHostName();
          } catch (Throwable e) {
            logger.warn(
              "Error getting hostname using InetAddress.getLocalHost().getCanonicalHostName()",
              e);
          }
        }
      }
      if (nodeHostName == null) {
        isMetricsEnabled = false;
        logger.error("Failed getting hostname for node. Disabling publishing LogFeeder metrics");
      } else {
        isMetricsEnabled = true;
        logger.info("LogFeeder Metrics is enabled. Metrics host="
          + amsClient.getCollectorUri());
      }
    } else {
      logger.info("LogFeeder Metrics publish is disabled");
    }
  }

  public boolean isMetricsEnabled() {
    return isMetricsEnabled;
  }

  synchronized public void useMetrics(List<MetricCount> metricsList) {
    if (!isMetricsEnabled) {
      return;
    }
    logger.info("useMetrics() metrics.size=" + metricsList.size());
    long currMS = System.currentTimeMillis();
    Long currMSLong = new Long(currMS);
    for (MetricCount metric : metricsList) {
      if (metric.metricsName == null) {
        logger.debug("metric.metricsName is null");
        // Metrics is not meant to be published
        continue;
      }
      long currCount = metric.count;
      if (!metric.isPointInTime && metric.publishCount > 0
        && currCount <= metric.prevPublishCount) {
        // No new data added, so let's ignore it
        logger.debug("Nothing changed. " + metric.metricsName
          + ", currCount=" + currCount + ", prevPublishCount="
          + metric.prevPublishCount);
        continue;
      }
      metric.publishCount++;

      TimelineMetric timelineMetric = metricsMap.get(metric.metricsName);
      if (timelineMetric == null) {
        logger.debug("Creating new metric obbject for "
          + metric.metricsName);
        // First time for this metric
        timelineMetric = new TimelineMetric();
        timelineMetric.setMetricName(metric.metricsName);
        timelineMetric.setHostName(nodeHostName);
        timelineMetric.setAppId(appId);
        timelineMetric.setStartTime(currMS);
        timelineMetric.setType("Long");
        timelineMetric.setMetricValues(new TreeMap<Long, Double>());

        metricsMap.put(metric.metricsName, timelineMetric);
      }
      logger.debug("Adding metrics=" + metric.metricsName);
      if (metric.isPointInTime) {
        timelineMetric.getMetricValues().put(currMSLong,
          new Double(currCount));
      } else {
        Double value = timelineMetric.getMetricValues().get(currMSLong);
        if (value == null) {
          value = new Double(0);
        }
        value += (currCount - metric.prevPublishCount);
        timelineMetric.getMetricValues().put(currMSLong, value);
        metric.prevPublishCount = currCount;
      }
    }

    if (metricsMap.size() > 0
      && currMS - lastPublishTimeMS > publishIntervalMS) {
      try {
        // Time to publish
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        List<TimelineMetric> timeLineMetricList = new ArrayList<TimelineMetric>();
        timeLineMetricList.addAll(metricsMap.values());
        timelineMetrics.setMetrics(timeLineMetricList);
        amsClient.emitMetrics(timelineMetrics);
        logger.info("Published " + timeLineMetricList.size()
          + " metrics to AMS");
        metricsMap.clear();
        timeLineMetricList.clear();
        lastPublishTimeMS = currMS;
      } catch (Throwable t) {
        logger.warn("Error sending metrics to AMS.", t);
        if (currMS - lastFailedPublishTimeMS > maxMetricsBuffer) {
          logger.error("AMS was not sent for last "
            + maxMetricsBuffer
            / 1000
            + " seconds. Purging it and will start rebuilding it again");
          metricsMap.clear();
          lastFailedPublishTimeMS = currMS;
        }
      }
    } else {
      logger.info("Not publishing metrics. metrics.size()="
        + metricsMap.size() + ", lastPublished="
        + (currMS - lastPublishTimeMS) / 1000
        + " seconds ago, intervalConfigured=" + publishIntervalMS
        / 1000);
    }
  }
}
