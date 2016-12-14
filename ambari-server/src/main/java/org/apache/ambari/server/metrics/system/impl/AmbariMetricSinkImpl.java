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
package org.apache.ambari.server.metrics.system.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.metrics.system.AmbariMetricSink;
import org.apache.commons.lang.ClassUtils;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import jline.internal.Log;

public class AmbariMetricSinkImpl extends AbstractTimelineMetricsSink implements AmbariMetricSink {
  private static final String APP_ID = "ambari_server";
  private int timeoutSeconds = 10;
  private String collectorProtocol;
  private String collectorUri;
  private String hostName;
  private int counter = 0;
  private int frequency;
  private List<TimelineMetric> buffer = new ArrayList<>();
  @Override
  public void init(String protocol, String collectorUri, int frequency) {

    /**
     * Protocol is either HTTP or HTTPS, and the collectorURI is the domain name of the collector
     * An example of the complete collector URI might be: http://c6403.ambari.org/ws/v1/timeline/metrics
     */
    this.frequency = frequency;
    this.collectorProtocol = protocol;
    this.collectorUri = getCollectorUri(collectorUri);

    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      Log.info("Error getting host address");
    }
  }

  @Override
  public void publish(Map<String, Number> metricsMap) {
    List<TimelineMetric> metricsList =  createMetricsList(metricsMap);

    if(counter > frequency) {
      TimelineMetrics timelineMetrics = new TimelineMetrics();
      timelineMetrics.setMetrics(buffer);

      String connectUrl = collectorUri;
      String jsonData = null;
      try {
        jsonData = mapper.writeValueAsString(timelineMetrics);
      } catch (IOException e) {
        LOG.error("Unable to parse metrics", e);
      }
      if (jsonData != null) {
        emitMetricsJson(connectUrl, jsonData);
      }
      counter = 0;
    } else {
      buffer.addAll(metricsList);
      counter++;
    }

  }


  /**
   * Get a pre-formatted URI for the collector
   *
   * @param host
   */
  @Override
  protected String getCollectorUri(String host) {
    return getCollectorProtocol() + "://" + host + WS_V1_TIMELINE_METRICS;
  }

  @Override
  protected String getCollectorProtocol() {
    return collectorProtocol;
  }

  @Override
  protected String getCollectorPort() {
    return null;
  }

  @Override
  protected int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  /**
   * Get the zookeeper quorum for the cluster used to find collector
   *
   * @return String "host1:port1,host2:port2"
   */
  @Override
  protected String getZookeeperQuorum() {
    return null;
  }

  /**
   * Get pre-configured list of collectors available
   *
   * @return Collection<String> host1,host2
   */
  @Override
  protected Collection<String> getConfiguredCollectorHosts() {
    return null;
  }

  /**
   * Get hostname used for calculating write shard.
   *
   * @return String "host1"
   */
  @Override
  protected String getHostname() {
    return hostName;
  }

  private List<TimelineMetric> createMetricsList(Map<String, Number> metricsMap) {
    final List<TimelineMetric> metricsList = new ArrayList<>();
    for (Map.Entry<String, Number> entry : metricsMap.entrySet()) {
      final long currentTimeMillis = System.currentTimeMillis();
      String metricsName = entry.getKey();
      Number value = entry.getValue();
      TimelineMetric metric = createTimelineMetric(currentTimeMillis, APP_ID, metricsName, value);
      metricsList.add(metric);
    }
    return metricsList;
  }

  private TimelineMetric createTimelineMetric(long currentTimeMillis, String component, String attributeName,
                                              Number attributeValue) {
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName(attributeName);
    timelineMetric.setHostName(hostName);
    timelineMetric.setAppId(component);
    timelineMetric.setStartTime(currentTimeMillis);
    timelineMetric.setType(ClassUtils.getShortCanonicalName(attributeValue, "Number"));
    timelineMetric.getMetricValues().put(currentTimeMillis, attributeValue.doubleValue());
    return timelineMetric;
  }
}