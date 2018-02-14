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

import org.apache.ambari.logfeeder.conf.LogFeederSecurityConfig;
import org.apache.ambari.logfeeder.conf.MetricsCollectorConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.List;

// TODO: Refactor for failover
public class LogFeederAMSClient extends AbstractTimelineMetricsSink {
  private static final Logger LOG = Logger.getLogger(LogFeederAMSClient.class);

  private final List<String> collectorHosts;
  private final String collectorProtocol;
  private final String collectorPort;
  private final String collectorPath;

  public LogFeederAMSClient(MetricsCollectorConfig metricsCollectorConfig, LogFeederSecurityConfig securityConfig) {
    String collectorHostsString = metricsCollectorConfig.getHostsString();
    if (!StringUtils.isBlank(collectorHostsString)) {
      collectorHostsString = collectorHostsString.trim();
      LOG.info("AMS collector Hosts=" + collectorHostsString);
      
      collectorHosts = metricsCollectorConfig.getHosts();
      collectorProtocol = metricsCollectorConfig.getProtocol();
      collectorPort = metricsCollectorConfig.getPort();
      collectorPath = metricsCollectorConfig.getPath();
    } else {
      collectorHosts = null;
      collectorProtocol = null;
      collectorPort = null;
      collectorPath = null;
    }
    
    if (StringUtils.isNotBlank(securityConfig.getTrustStoreLocation())) {
      loadTruststore(securityConfig.getTrustStoreLocation(), securityConfig.getTrustStoreType(), securityConfig.getTrustStorePassword());
    }
  }

  @Override
  public String getCollectorUri(String host) {
    if (collectorProtocol == null || host == null || collectorPort == null || collectorPath == null) {
      return null;
    }
    return String.format("%s://%s:%s%s", collectorProtocol, host, collectorPort, collectorPath);
  }

  @Override
  protected int getTimeoutSeconds() {
    // TODO: Hard coded timeout
    return 10;
  }

  @Override
  protected String getZookeeperQuorum() {
    return null;
  }

  @Override
  protected Collection<String> getConfiguredCollectorHosts() {
    return collectorHosts;
  }

  @Override
  protected String getHostname() {
    return null;
  }

  @Override
  protected boolean isHostInMemoryAggregationEnabled() {
    return false;
  }

  @Override
  protected int getHostInMemoryAggregationPort() {
    return 0;
  }

  @Override
  protected boolean emitMetrics(TimelineMetrics metrics) {
    return super.emitMetrics(metrics);
  }

  @Override
  protected String getCollectorProtocol() {
    return collectorProtocol;
  }

  @Override
  protected String getCollectorPort() {
    return collectorPort;
  }

}
