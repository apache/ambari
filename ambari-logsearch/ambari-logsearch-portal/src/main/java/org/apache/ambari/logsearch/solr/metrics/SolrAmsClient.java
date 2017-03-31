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

package org.apache.ambari.logsearch.solr.metrics;

import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import java.util.Collection;

// TODO: Refactor for failover
public class SolrAmsClient extends AbstractTimelineMetricsSink {
  private final String collectorHost;

  public SolrAmsClient(String collectorHost) {
    this.collectorHost = collectorHost;
  }

  @Override
  public String getCollectorUri(String host) {
    return collectorHost;
  }

  @Override
  protected int getTimeoutSeconds() {
    return 10;
  }

  @Override
  protected String getZookeeperQuorum() {
    return null;
  }

  @Override
  protected Collection<String> getConfiguredCollectorHosts() {
    return null;
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
    return null;
  }

  @Override
  protected String getCollectorPort() {
    return null;
  }
}
