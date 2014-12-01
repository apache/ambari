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
package org.apache.hadoop.metrics2.sink.timeline;

import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.MetricsSink;
import org.apache.hadoop.metrics2.util.Servers;
import org.apache.hadoop.net.DNS;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.List;

public abstract class AbstractTimelineMetricsSink implements MetricsSink {

  public final Log LOG = LogFactory.getLog(this.getClass());

  private SubsetConfiguration conf;
  private String hostName = "UNKNOWN.example.com";
  private String serviceName = "";
  private final String COLLECTOR_HOST_PROPERTY = "collector";
  private final int DEFAULT_PORT = 8188;

  private List<? extends SocketAddress> metricsServers;
  private String collectorUri;

  @Override
  public void init(SubsetConfiguration conf) {
    this.conf = conf;
    LOG.info("Initializing Timeline metrics sink.");

    // Take the hostname from the DNS class.
    if (conf.getString("slave.host.name") != null) {
      hostName = conf.getString("slave.host.name");
    } else {
      try {
        hostName = DNS.getDefaultHost(
          conf.getString("dfs.datanode.dns.interface", "default"),
          conf.getString("dfs.datanode.dns.nameserver", "default"));
      } catch (UnknownHostException uhe) {
        LOG.error(uhe);
        hostName = "UNKNOWN.example.com";
      }
    }

    serviceName = getFirstConfigPrefix(conf);

    // Load collector configs
    metricsServers = Servers.parse(conf.getString(COLLECTOR_HOST_PROPERTY),
      DEFAULT_PORT);

    if (metricsServers == null || metricsServers.isEmpty()) {
      LOG.error("No Metric collector configured.");
    } else {
      collectorUri = "http://" + conf.getString(COLLECTOR_HOST_PROPERTY).trim()
        + "/ws/v1/timeline/metrics";
    }
  }

  protected String getHostName() {
    return hostName;
  }

  protected String getServiceName() {
    return serviceName;
  }

  private String getFirstConfigPrefix(SubsetConfiguration conf) {
    while (conf.getParent() instanceof SubsetConfiguration) {
      conf = (SubsetConfiguration) conf.getParent();
    }
    return conf.getPrefix();
  }

  protected SocketAddress getServerSocketAddress() {
    if (metricsServers != null && !metricsServers.isEmpty()) {
      return metricsServers.get(0);
    }
    return null;
  }

  protected String getCollectorUri() {
    return collectorUri;
  }
}
