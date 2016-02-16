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

package org.apache.hadoop.metrics2.sink.storm;

import backtype.storm.metric.api.IMetricsConsumer;
import backtype.storm.task.IErrorReporter;
import backtype.storm.task.TopologyContext;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.UnableToConnectException;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import org.apache.hadoop.metrics2.sink.timeline.configuration.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache.*;

public class StormTimelineMetricsSink extends AbstractTimelineMetricsSink implements IMetricsConsumer {
  private String collectorUri;
  private TimelineMetricsCache metricsCache;
  private String hostname;
  private int timeoutSeconds;

  @Override
  protected String getCollectorUri() {
    return collectorUri;
  }

  @Override
  protected int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  @Override
  public void prepare(Map map, Object o, TopologyContext topologyContext, IErrorReporter iErrorReporter) {
    LOG.info("Preparing Storm Metrics Sink");
    try {
      hostname = InetAddress.getLocalHost().getHostName();
      //If not FQDN , call  DNS
      if ((hostname == null) || (!hostname.contains("."))) {
        hostname = InetAddress.getLocalHost().getCanonicalHostName();
      }
    } catch (UnknownHostException e) {
      LOG.error("Could not identify hostname.");
      throw new RuntimeException("Could not identify hostname.", e);
    }
    Configuration configuration = new Configuration("/storm-metrics2.properties");
    timeoutSeconds = Integer.parseInt(configuration.getProperty(METRICS_POST_TIMEOUT_SECONDS,
        String.valueOf(DEFAULT_POST_TIMEOUT_SECONDS)));
    int maxRowCacheSize = Integer.parseInt(configuration.getProperty(MAX_METRIC_ROW_CACHE_SIZE,
        String.valueOf(MAX_RECS_PER_NAME_DEFAULT)));
    int metricsSendInterval = Integer.parseInt(configuration.getProperty(METRICS_SEND_INTERVAL,
        String.valueOf(MAX_EVICTION_TIME_MILLIS)));
    metricsCache = new TimelineMetricsCache(maxRowCacheSize, metricsSendInterval);
    collectorUri = configuration.getProperty(COLLECTOR_PROPERTY) + WS_V1_TIMELINE_METRICS;
    if (collectorUri.toLowerCase().startsWith("https://")) {
      String trustStorePath = configuration.getProperty(SSL_KEYSTORE_PATH_PROPERTY).trim();
      String trustStoreType = configuration.getProperty(SSL_KEYSTORE_TYPE_PROPERTY).trim();
      String trustStorePwd = configuration.getProperty(SSL_KEYSTORE_PASSWORD_PROPERTY).trim();
      loadTruststore(trustStorePath, trustStoreType, trustStorePwd);
    }
  }

  @Override
  public void handleDataPoints(TaskInfo taskInfo, Collection<DataPoint> dataPoints) {
    List<TimelineMetric> metricList = new ArrayList<TimelineMetric>();
    for (DataPoint dataPoint : dataPoints) {
      if (dataPoint.value != null && NumberUtils.isNumber(dataPoint.value.toString())) {
        LOG.debug(dataPoint.name + " = " + dataPoint.value);
        TimelineMetric timelineMetric = createTimelineMetric(taskInfo.timestamp,
            taskInfo.srcComponentId, dataPoint.name, dataPoint.value.toString());
        // Put intermediate values into the cache until it is time to send
        metricsCache.putTimelineMetric(timelineMetric);

        TimelineMetric cachedMetric = metricsCache.getTimelineMetric(dataPoint.name);

        if (cachedMetric != null) {
          metricList.add(cachedMetric);
        }
      }
    }

    if (!metricList.isEmpty()) {
      TimelineMetrics timelineMetrics = new TimelineMetrics();
      timelineMetrics.setMetrics(metricList);
      try {
        emitMetrics(timelineMetrics);
      } catch (UnableToConnectException uce) {
        LOG.warn("Unable to send metrics to collector by address:" + uce.getConnectUrl());
      }
    }
  }

  @Override
  public void cleanup() {
    LOG.info("Stopping Storm Metrics Sink");
  }

  private TimelineMetric createTimelineMetric(long currentTimeMillis, String component, String attributeName, String attributeValue) {
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName(attributeName);
    timelineMetric.setHostName(hostname);
    timelineMetric.setAppId(component);
    timelineMetric.setStartTime(currentTimeMillis);
    timelineMetric.setType(ClassUtils.getShortCanonicalName(
        attributeValue, "Number"));
    timelineMetric.getMetricValues().put(currentTimeMillis, Double.parseDouble(attributeValue));
    return timelineMetric;
  }

  public void setMetricsCache(TimelineMetricsCache metricsCache) {
    this.metricsCache = metricsCache;
  }

}
