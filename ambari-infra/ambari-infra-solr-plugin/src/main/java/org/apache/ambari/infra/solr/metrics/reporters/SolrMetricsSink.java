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
package org.apache.ambari.infra.solr.metrics.reporters;

import static java.util.Arrays.asList;
import static org.apache.ambari.infra.solr.metrics.reporters.AMSProtocol.https;
import static org.apache.commons.lang.StringUtils.join;

import java.util.Collection;

import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrMetricsSink extends AbstractTimelineMetricsSink {
  private static final Logger LOG = LoggerFactory.getLogger(SolrMetricsSink.class);

  private final Collection<String> collectorHosts;
  private final int port;
  private final AMSProtocol protocol;

  public SolrMetricsSink(String[] collectorHosts, int port, AMSProtocol protocol, SolrMetricsSecurityConfig securityConfig) {
    LOG.info("Setting up SolrMetricsSink protocol={} hosts={} port={}", protocol.name(), join(collectorHosts, ","), port);
    this.collectorHosts = asList(collectorHosts);
    this.port = port;
    this.protocol = protocol;

    if (protocol == https)
      loadTruststore(securityConfig.getTrustStoreLocation(), securityConfig.getTrustStoreType(), securityConfig.getTrustStorePassword());
  }

  @Override
  protected String getCollectorUri(String host) {
    return constructTimelineMetricUri(this.protocol.name(), host, getCollectorPort());
  }

  @Override
  protected String getCollectorProtocol() {
    return protocol.name();
  }

  @Override
  protected String getCollectorPort() {
    return Integer.toString(port);
  }

  @Override
  protected int getTimeoutSeconds() {
    return 0;
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
    return MetricsUtils.getHostName();
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
  protected String getHostInMemoryAggregationProtocol() {
    return protocol.name();
  }

  @Override
  public boolean emitMetrics(TimelineMetrics metrics) {
    return super.emitMetrics(metrics);
  }
}
