/*
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
package org.apache.ambari.server.alerts;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.jmx.JMXMetricHolder;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.MetricSource;
import org.apache.ambari.server.state.alert.Reporting;
import org.apache.ambari.server.state.alert.ServerSource;
import org.apache.ambari.server.state.services.MetricsRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * I represent a "SERVER" {@link org.apache.ambari.server.state.alert.SourceType} alert
 * which can pull JMX metrics from a remote cluster.
 */
public class JmxServerSideAlert extends AlertRunnable {
  private final static Logger LOG = LoggerFactory.getLogger(JmxServerSideAlert.class);
  @Inject
  private AlertDefinitionFactory definitionFactory;
  @Inject
  private MetricsRetrievalService metricsRetrievalService;
  @Inject
  private ConfigHelper configHelper;

  public JmxServerSideAlert(String definitionName) {
    super(definitionName);
  }

  @Override
  List<Alert> execute(Cluster cluster, AlertDefinitionEntity entity) throws AmbariException {
    AlertDefinition alertDef = definitionFactory.coerce(entity);
    ServerSource serverSource = (ServerSource) alertDef.getSource();
    URI jmxUrl = jmxUrl(cluster, serverSource);
    JMXMetricHolder metricHolder = jmxMetric(serverSource, jmxUrl);
    return metricHolder == null
      ? emptyList()
      : alerts(alertDef, serverSource.getJmxInfo(), metricHolder, serverSource.getReporting());
  }

  private URI jmxUrl(Cluster cluster, ServerSource serverSource) throws AmbariException {
    return serverSource.getUri().resolve(config(cluster)).resolve(serverSource.getJmxInfo().getUrlSuffix());
  }

  private Map<String, Map<String, String>> config(Cluster cluster) throws AmbariException {
    return configHelper.getEffectiveConfigProperties(cluster, configHelper.getEffectiveDesiredTags(cluster, null));
  }

  private JMXMetricHolder jmxMetric(ServerSource serverSource, URI jmxUri) {
    URLStreamProvider streamProvider = new URLStreamProvider(
      serverSource.getUri().getConnectionTimeoutMsec(),
      serverSource.getUri().getReadTimeoutMsec(),
      ComponentSSLConfiguration.instance());
    metricsRetrievalService.submitRequest(MetricsRetrievalService.MetricSourceType.JMX, streamProvider, jmxUri.toString());
    return metricsRetrievalService.getCachedJMXMetric(jmxUri.toString());
  }

  private List<Alert> alerts(AlertDefinition alertDef, MetricSource.JmxInfo jmxInfo, JMXMetricHolder jmxMetricHolder, Reporting reporting) throws AmbariException {
    List<Object> metrics = jmxMetricHolder.findAll(jmxInfo.getPropertyList());
    if (metrics.isEmpty()) {
      return emptyList();
    }
    if (metrics.get(0) instanceof Number) {
      Alert alert = reporting.alert(((Number) metrics.get(0)).doubleValue(), metrics, alertDef);
      return singletonList(alert);
    } else {
      LOG.info("Unsupported metrics value: {} when running alert: {}", metrics.get(0), alertDef);
      return emptyList();
    }
  }
}
