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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.UnableToConnectException;

import backtype.storm.generated.ClusterSummary;
import backtype.storm.generated.SupervisorSummary;
import backtype.storm.generated.TopologySummary;
import backtype.storm.metric.IClusterReporter;
import backtype.storm.utils.NimbusClient;
import backtype.storm.utils.Utils;

public class StormTimelineMetricsReporter extends AbstractTimelineMetricsSink
    implements IClusterReporter {

  public static final String METRICS_COLLECTOR_CATEGORY = "metrics_collector";
  public static final String APP_ID = "appId";

  private String hostname;
  private String collectorUri;
  private String port;
  private Collection<String> collectorHosts;
  private String zkQuorum;
  private String protocol;
  private boolean setInstanceId;
  private String instanceId;
  private NimbusClient nimbusClient;
  private String applicationId;
  private int timeoutSeconds;
  private boolean hostInMemoryAggregationEnabled;
  private int hostInMemoryAggregationPort;

  public StormTimelineMetricsReporter() {

  }

  @Override
  protected String getCollectorUri(String host) {
    return constructTimelineMetricUri(protocol, host, port);
  }

  @Override
  protected String getCollectorProtocol() {
    return protocol;
  }

  @Override
  protected int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  @Override
  protected String getZookeeperQuorum() {
    return zkQuorum;
  }

  @Override
  protected Collection<String> getConfiguredCollectorHosts() {
    return collectorHosts;
  }

  @Override
  protected String getCollectorPort() {
    return port;
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
  public void prepare(Map conf) {
    LOG.info("Preparing Storm Metrics Reporter");
    try {
      try {
        hostname = InetAddress.getLocalHost().getHostName();
        // If not FQDN , call  DNS
        if ((hostname == null) || (!hostname.contains("."))) {
          hostname = InetAddress.getLocalHost().getCanonicalHostName();
        }
      } catch (UnknownHostException e) {
        LOG.error("Could not identify hostname.");
        throw new RuntimeException("Could not identify hostname.", e);
      }
      Validate.notNull(conf.get(METRICS_COLLECTOR_CATEGORY), METRICS_COLLECTOR_CATEGORY + " can not be null");
      Map cf = (Map) conf.get(METRICS_COLLECTOR_CATEGORY);
      Map stormConf = Utils.readStormConfig();
      this.nimbusClient = NimbusClient.getConfiguredClient(stormConf);

      collectorHosts = parseHostsStringIntoCollection(cf.get(COLLECTOR_HOSTS_PROPERTY).toString());
      protocol = cf.get(COLLECTOR_PROTOCOL) != null ? cf.get(COLLECTOR_PROTOCOL).toString() : "http";
      port = cf.get(COLLECTOR_PORT) != null ? cf.get(COLLECTOR_PORT).toString() : "6188";
      Object zkQuorumObj = cf.get(COLLECTOR_ZOOKEEPER_QUORUM);
      if (zkQuorumObj != null) {
        zkQuorum = zkQuorumObj.toString();
      } else {
        zkQuorum = cf.get(ZOOKEEPER_QUORUM) != null ? cf.get(ZOOKEEPER_QUORUM).toString() : null;
      }

      timeoutSeconds = cf.get(METRICS_POST_TIMEOUT_SECONDS) != null ?
          Integer.parseInt(cf.get(METRICS_POST_TIMEOUT_SECONDS).toString()) :
          DEFAULT_POST_TIMEOUT_SECONDS;
      applicationId = cf.get(APP_ID).toString();
      if (cf.containsKey(SET_INSTANCE_ID_PROPERTY)) {
        setInstanceId = Boolean.getBoolean(cf.get(SET_INSTANCE_ID_PROPERTY).toString());
        instanceId = cf.get(INSTANCE_ID_PROPERTY).toString();
      }
      hostInMemoryAggregationEnabled = Boolean.valueOf(cf.get(HOST_IN_MEMORY_AGGREGATION_ENABLED_PROPERTY).toString());
      hostInMemoryAggregationPort = Integer.valueOf(cf.get(HOST_IN_MEMORY_AGGREGATION_PORT_PROPERTY).toString());

      collectorUri = constructTimelineMetricUri(protocol, findPreferredCollectHost(), port);
      if (protocol.contains("https")) {
        String trustStorePath = cf.get(SSL_KEYSTORE_PATH_PROPERTY).toString().trim();
        String trustStoreType = cf.get(SSL_KEYSTORE_TYPE_PROPERTY).toString().trim();
        String trustStorePwd = cf.get(SSL_KEYSTORE_PASSWORD_PROPERTY).toString().trim();
        loadTruststore(trustStorePath, trustStoreType, trustStorePwd);
      }
    } catch (Exception e) {
      LOG.warn("Could not initialize metrics collector, please specify " +
          "protocol, host, port under $STORM_HOME/conf/config.yaml ", e);
    }
    // Initialize the collector write strategy
    super.init();
  }

  @Override
  public void reportMetrics() throws Exception {
    List<TimelineMetric> totalMetrics = new ArrayList<TimelineMetric>(7);
    ClusterSummary cs = this.nimbusClient.getClient().getClusterInfo();
    long currentTimeMillis = System.currentTimeMillis();
    totalMetrics.add(createTimelineMetric(currentTimeMillis,
        applicationId, "Supervisors", String.valueOf(cs.get_supervisors_size())));
    totalMetrics.add(createTimelineMetric(currentTimeMillis,
        applicationId, "Topologies", String.valueOf(cs.get_topologies_size())));

    List<SupervisorSummary> sups = cs.get_supervisors();
    int totalSlots = 0;
    int usedSlots = 0;
    for (SupervisorSummary ssum : sups) {
      totalSlots += ssum.get_num_workers();
      usedSlots += ssum.get_num_used_workers();
    }
    int freeSlots = totalSlots - usedSlots;

    totalMetrics.add(createTimelineMetric(currentTimeMillis,
        applicationId, "Total Slots", String.valueOf(totalSlots)));
    totalMetrics.add(createTimelineMetric(currentTimeMillis,
        applicationId, "Used Slots", String.valueOf(usedSlots)));
    totalMetrics.add(createTimelineMetric(currentTimeMillis,
        applicationId, "Free Slots", String.valueOf(freeSlots)));

    List<TopologySummary> topos = cs.get_topologies();
    int totalExecutors = 0;
    int totalTasks = 0;
    for (TopologySummary topo : topos) {
      totalExecutors += topo.get_num_executors();
      totalTasks += topo.get_num_tasks();
    }

    totalMetrics.add(createTimelineMetric(currentTimeMillis,
        applicationId, "Total Executors", String.valueOf(totalExecutors)));
    totalMetrics.add(createTimelineMetric(currentTimeMillis,
        applicationId, "Total Tasks", String.valueOf(totalTasks)));

    TimelineMetrics timelineMetrics = new TimelineMetrics();
    timelineMetrics.setMetrics(totalMetrics);

    try {
      emitMetrics(timelineMetrics);
    } catch (UnableToConnectException e) {
      LOG.warn("Unable to connect to Metrics Collector " + e.getConnectUrl() + ". " + e.getMessage());
    }

  }

  private TimelineMetric createTimelineMetric(long currentTimeMillis, String component, String attributeName, String attributeValue) {
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName(attributeName);
    timelineMetric.setHostName(hostname);
    if (setInstanceId) {
      timelineMetric.setInstanceId(instanceId);
    }
    timelineMetric.setAppId(component);
    timelineMetric.setStartTime(currentTimeMillis);
    timelineMetric.getMetricValues().put(currentTimeMillis, Double.parseDouble(attributeValue));
    return timelineMetric;
  }

}
