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

package org.apache.hadoop.metrics2.sink.flume;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.flume.Context;
import org.apache.flume.FlumeException;
import org.apache.flume.instrumentation.MonitorService;
import org.apache.flume.instrumentation.util.JMXPollUtil;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.UnableToConnectException;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import org.apache.hadoop.metrics2.sink.timeline.configuration.Configuration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FlumeTimelineMetricsSink extends AbstractTimelineMetricsSink implements MonitorService {
  private String collectorUri;
  private String protocol;
  // Key - component(instance_id)
  private Map<String, TimelineMetricsCache> metricsCaches;
  private int maxRowCacheSize;
  private int metricsSendInterval;
  private ScheduledExecutorService scheduledExecutorService;
  private long pollFrequency;
  private String hostname;
  private String port;
  private Collection<String> collectorHosts;
  private String zookeeperQuorum;
  private final static String COUNTER_METRICS_PROPERTY = "counters";
  private final Set<String> counterMetrics = new HashSet<String>();
  private int timeoutSeconds = 10;
  private boolean setInstanceId;
  private String instanceId;
  private boolean hostInMemoryAggregationEnabled;
  private int hostInMemoryAggregationPort;
  private String hostInMemoryAggregationProtocol;


  @Override
  public void start() {
    LOG.info("Starting Flume Metrics Sink");
    TimelineMetricsCollector timelineMetricsCollector = new TimelineMetricsCollector();
    if (scheduledExecutorService == null || scheduledExecutorService.isShutdown() || scheduledExecutorService.isTerminated()) {
      scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }
    scheduledExecutorService.scheduleWithFixedDelay(timelineMetricsCollector, 0,
        pollFrequency, TimeUnit.MILLISECONDS);
  }

  @Override
  public void stop() {
    LOG.info("Stopping Flume Metrics Sink");
    scheduledExecutorService.shutdown();
  }

  @Override
  public void configure(Context context) {
    LOG.info("Context parameters " + context);
    try {
      hostname = InetAddress.getLocalHost().getHostName();
      //If not FQDN , call  DNS
      if ((hostname == null) || (!hostname.contains("."))) {
        hostname = InetAddress.getLocalHost().getCanonicalHostName();
      }
      hostname = hostname.toLowerCase();

    } catch (UnknownHostException e) {
      LOG.error("Could not identify hostname.");
      throw new FlumeException("Could not identify hostname.", e);
    }
    Configuration configuration = new Configuration("/flume-metrics2.properties");
    timeoutSeconds = Integer.parseInt(configuration.getProperty(METRICS_POST_TIMEOUT_SECONDS,
        String.valueOf(DEFAULT_POST_TIMEOUT_SECONDS)));
    maxRowCacheSize = Integer.parseInt(configuration.getProperty(MAX_METRIC_ROW_CACHE_SIZE,
        String.valueOf(TimelineMetricsCache.MAX_RECS_PER_NAME_DEFAULT)));
    metricsSendInterval = Integer.parseInt(configuration.getProperty(METRICS_SEND_INTERVAL,
        String.valueOf(TimelineMetricsCache.MAX_EVICTION_TIME_MILLIS)));
    metricsCaches = new HashMap<String, TimelineMetricsCache>();
    collectorHosts = parseHostsStringIntoCollection(configuration.getProperty(COLLECTOR_HOSTS_PROPERTY));
    zookeeperQuorum = configuration.getProperty("zookeeper.quorum");
    protocol = configuration.getProperty(COLLECTOR_PROTOCOL, "http");
    port = configuration.getProperty(COLLECTOR_PORT, "6188");
    setInstanceId = Boolean.valueOf(configuration.getProperty(SET_INSTANCE_ID_PROPERTY, "false"));
    instanceId = configuration.getProperty(INSTANCE_ID_PROPERTY, "");

    hostInMemoryAggregationEnabled = Boolean.getBoolean(configuration.getProperty(HOST_IN_MEMORY_AGGREGATION_ENABLED_PROPERTY, "false"));
    hostInMemoryAggregationPort = Integer.valueOf(configuration.getProperty(HOST_IN_MEMORY_AGGREGATION_PORT_PROPERTY, "61888"));
    hostInMemoryAggregationProtocol = configuration.getProperty(HOST_IN_MEMORY_AGGREGATION_PROTOCOL_PROPERTY, "http");
    // Initialize the collector write strategy
    super.init();

    if (protocol.contains("https") || hostInMemoryAggregationProtocol.contains("https")) {
      String trustStorePath = configuration.getProperty(SSL_KEYSTORE_PATH_PROPERTY).trim();
      String trustStoreType = configuration.getProperty(SSL_KEYSTORE_TYPE_PROPERTY).trim();
      String trustStorePwd = configuration.getProperty(SSL_KEYSTORE_PASSWORD_PROPERTY).trim();
      loadTruststore(trustStorePath, trustStoreType, trustStorePwd);
    }
    collectorUri = constructTimelineMetricUri(protocol, findPreferredCollectHost(), port);

    pollFrequency = Long.parseLong(configuration.getProperty("collectionFrequency"));

    String[] metrics = configuration.getProperty(COUNTER_METRICS_PROPERTY).trim().split(",");
    Collections.addAll(counterMetrics, metrics);
  }

  @Override
  public String getCollectorUri(String host) {
    return constructTimelineMetricUri(protocol, host, port);
  }

  @Override
  protected String getCollectorProtocol() {
    return protocol;
  }

  @Override
  protected String getCollectorPort() {
    return port;
  }

  @Override
  protected int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  @Override
  protected String getZookeeperQuorum() {
    return zookeeperQuorum;
  }

  @Override
  protected Collection<String> getConfiguredCollectorHosts() {
    return collectorHosts;
  }

  @Override
  protected String getHostname() {
    return hostname;
  }

  @Override
  protected boolean isHostInMemoryAggregationEnabled() {
    return hostInMemoryAggregationEnabled;
  }

  @Override
  protected int getHostInMemoryAggregationPort() {
    return hostInMemoryAggregationPort;
  }

  @Override
  protected String getHostInMemoryAggregationProtocol() {
    return hostInMemoryAggregationProtocol;
  }

  public void setPollFrequency(long pollFrequency) {
    this.pollFrequency = pollFrequency;
  }

  //Test hepler method
  protected void setMetricsCaches(Map<String, TimelineMetricsCache> metricsCaches) {
    this.metricsCaches = metricsCaches;
  }

  /**
   * Worker which polls JMX for all mbeans with
   * {@link javax.management.ObjectName} within the flume namespace:
   * org.apache.flume. All attributes of such beans are sent
   * to the metrics collector service.
   */
  class TimelineMetricsCollector implements Runnable {
    @Override
    public void run() {
      LOG.debug("Collecting Metrics for Flume");
      try {
        Map<String, Map<String, String>> metricsMap = JMXPollUtil.getAllMBeans();
        long currentTimeMillis = System.currentTimeMillis();
        for (String component : metricsMap.keySet()) {
          Map<String, String> attributeMap = metricsMap.get(component);
          LOG.debug("Attributes for component " + component);
          processComponentAttributes(currentTimeMillis, component, attributeMap);
        }
      } catch (UnableToConnectException uce) {
        LOG.warn("Unable to send metrics to collector by address:" + uce.getConnectUrl());
      } catch (Exception e) {
        LOG.error("Unexpected error", e);
      }
      LOG.debug("Finished collecting Metrics for Flume");
    }

    private void processComponentAttributes(long currentTimeMillis, String component, Map<String, String> attributeMap) throws IOException {
      List<TimelineMetric> metricList = new ArrayList<TimelineMetric>();
      if (!metricsCaches.containsKey(component)) {
        metricsCaches.put(component, new TimelineMetricsCache(maxRowCacheSize, metricsSendInterval));
      }
      TimelineMetricsCache metricsCache = metricsCaches.get(component);
      for (String attributeName : attributeMap.keySet()) {
        String attributeValue = attributeMap.get(attributeName);
        if (NumberUtils.isNumber(attributeValue)) {
          LOG.info(attributeName + " = " + attributeValue);
          TimelineMetric timelineMetric = createTimelineMetric(currentTimeMillis,
              component, attributeName, attributeValue);
          // Put intermediate values into the cache until it is time to send
          metricsCache.putTimelineMetric(timelineMetric, isCounterMetric(attributeName));

          TimelineMetric cachedMetric = metricsCache.getTimelineMetric(attributeName);

          if (cachedMetric != null) {
            metricList.add(cachedMetric);
          }
        }
      }

      if (!metricList.isEmpty()) {
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        timelineMetrics.setMetrics(metricList);
        emitMetrics(timelineMetrics);
      }
    }

    private TimelineMetric createTimelineMetric(long currentTimeMillis, String component, String attributeName, String attributeValue) {
      TimelineMetric timelineMetric = new TimelineMetric();
      timelineMetric.setMetricName(attributeName);
      timelineMetric.setHostName(hostname);
      if (setInstanceId) {
        timelineMetric.setInstanceId(instanceId + component);
      } else {
        timelineMetric.setInstanceId(component);
      }
      timelineMetric.setAppId("FLUME_HANDLER");
      timelineMetric.setStartTime(currentTimeMillis);
      timelineMetric.getMetricValues().put(currentTimeMillis, Double.parseDouble(attributeValue));
      return timelineMetric;
    }
  }

  private boolean isCounterMetric(String attributeName) {
    return counterMetrics.contains(attributeName);
  }
}
