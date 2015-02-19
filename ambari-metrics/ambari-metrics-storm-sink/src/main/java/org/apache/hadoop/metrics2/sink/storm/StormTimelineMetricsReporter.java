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

import backtype.storm.generated.ClusterSummary;
import backtype.storm.generated.SupervisorSummary;
import backtype.storm.generated.TopologySummary;
import backtype.storm.metric.IClusterReporter;
import backtype.storm.utils.NimbusClient;
import backtype.storm.utils.Utils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.Validate;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.util.Servers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StormTimelineMetricsReporter extends AbstractTimelineMetricsSink
  implements IClusterReporter {

  public static final String COLLECTOR_HOST = "host";
  public static final String COLLECTOR_PORT = "port";

  public static final String METRICS_COLLECTOR = "metrics_collector";

  public static final String APP_ID = "nimbus";

  private String hostname;
  private SocketAddress socketAddress;
  private String collectorUri;
  private NimbusClient nimbusClient;

  public StormTimelineMetricsReporter() {

  }

  @Override
  protected SocketAddress getServerSocketAddress() {
    return this.socketAddress;
  }

  @Override
  protected String getCollectorUri() {
    return this.collectorUri;
  }

  @Override
  public void prepare(Map conf) {
    LOG.info("Preparing Storm Metrics Reporter");
    try {
      try {
        hostname = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        LOG.error("Could not identify hostname.");
        throw new RuntimeException("Could not identify hostname.", e);
      }
      Validate.notNull(conf.get(METRICS_COLLECTOR), METRICS_COLLECTOR + " can not be null");
      Map cf = (Map) conf.get(METRICS_COLLECTOR);
      Map stormConf = Utils.readStormConfig();
      this.nimbusClient = NimbusClient.getConfiguredClient(stormConf);
      String collectorHostname = cf.get(COLLECTOR_HOST).toString();
      String port = cf.get(COLLECTOR_PORT).toString();
      collectorUri = "http://" + collectorHostname + ":" + port + "/ws/v1/timeline/metrics";
      List<InetSocketAddress> socketAddresses =
        Servers.parse(collectorHostname, Integer.valueOf(port));
      if (socketAddresses != null && !socketAddresses.isEmpty()) {
        socketAddress = socketAddresses.get(0);
      }
    } catch (Exception e) {
      LOG.warn("could not initialize metrics collector, please specify host, port under $STORM_HOME/conf/config.yaml ", e);
    }

  }

  @Override
  public void reportMetrics() throws Exception {
    List<TimelineMetric> totalMetrics = new ArrayList<TimelineMetric>(7);
    ClusterSummary cs = this.nimbusClient.getClient().getClusterInfo();
    long currentTimeMillis = System.currentTimeMillis();
    totalMetrics.add(createTimelineMetric(currentTimeMillis,
      APP_ID, "Supervisors", String.valueOf(cs.get_supervisors_size())));
    totalMetrics.add(createTimelineMetric(currentTimeMillis,
      APP_ID, "Topologies", String.valueOf(cs.get_topologies_size())));

    List<SupervisorSummary> sups = cs.get_supervisors();
    int totalSlots = 0;
    int usedSlots = 0;
    for (SupervisorSummary ssum : sups) {
      totalSlots += ssum.get_num_workers();
      usedSlots += ssum.get_num_used_workers();
    }
    int freeSlots = totalSlots - usedSlots;

    totalMetrics.add(createTimelineMetric(currentTimeMillis,
      APP_ID, "Total Slots", String.valueOf(totalSlots)));
    totalMetrics.add(createTimelineMetric(currentTimeMillis,
      APP_ID, "Used Slots", String.valueOf(usedSlots)));
    totalMetrics.add(createTimelineMetric(currentTimeMillis,
      APP_ID, "Free Slots", String.valueOf(freeSlots)));

    List<TopologySummary> topos = cs.get_topologies();
    int totalExecutors = 0;
    int totalTasks = 0;
    for (TopologySummary topo : topos) {
      totalExecutors += topo.get_num_executors();
      totalTasks += topo.get_num_tasks();
    }

    totalMetrics.add(createTimelineMetric(currentTimeMillis,
      APP_ID, "Total Executors", String.valueOf(totalExecutors)));
    totalMetrics.add(createTimelineMetric(currentTimeMillis,
      APP_ID, "Total Tasks", String.valueOf(totalTasks)));

    TimelineMetrics timelineMetrics = new TimelineMetrics();
    timelineMetrics.setMetrics(totalMetrics);

    emitMetrics(timelineMetrics);

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

}
