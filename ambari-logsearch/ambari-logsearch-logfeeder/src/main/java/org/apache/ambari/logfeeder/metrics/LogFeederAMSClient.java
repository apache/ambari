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

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.util.SSLUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.log4j.Logger;

import com.google.common.base.Splitter;

import java.util.Collection;
import java.util.List;

// TODO: Refactor for failover
public class LogFeederAMSClient extends AbstractTimelineMetricsSink {
  private static final Logger LOG = Logger.getLogger(LogFeederAMSClient.class);

  private final List<String> collectorHosts;
  private final String collectorProtocol;
  private final String collectorPort;
  private final String collectorPath;

  public LogFeederAMSClient() {
    String collectorHostsString = LogFeederUtil.getStringProperty("logfeeder.metrics.collector.hosts");
    if (!StringUtils.isBlank(collectorHostsString)) {
      collectorHostsString = collectorHostsString.trim();
      LOG.info("AMS collector Hosts=" + collectorHostsString);
      
      collectorHosts = Splitter.on(",").splitToList(collectorHostsString);
      collectorProtocol = LogFeederUtil.getStringProperty("logfeeder.metrics.collector.protocol");
      collectorPort = LogFeederUtil.getStringProperty("logfeeder.metrics.collector.port");
      collectorPath = LogFeederUtil.getStringProperty("logfeeder.metrics.collector.path");
    } else {
      collectorHosts = null;
      collectorProtocol = null;
      collectorPort = null;
      collectorPath = null;
    }
    
    if (StringUtils.isNotBlank(SSLUtil.getTrustStoreLocation())) {
      loadTruststore(SSLUtil.getTrustStoreLocation(), SSLUtil.getTrustStoreType(), SSLUtil.getTrustStorePassword());
    }
  }

  @Override
  public String getCollectorUri(String host) {
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