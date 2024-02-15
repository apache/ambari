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

import static org.apache.ambari.infra.solr.metrics.reporters.AMSProtocol.https;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.concurrent.TimeUnit;

import org.apache.solr.metrics.FilteringSolrMetricReporter;
import org.apache.solr.metrics.SolrMetricManager;

public abstract class AMSReporter<T> extends FilteringSolrMetricReporter {

  private ScheduledAMSReporter reporter;

  private String amsCollectorHosts;
  private int amsCollectorPort;
  private AMSProtocol amsCollectorProtocol;
  private String trustStoreLocation;
  private String trustStoreType;
  private String trustStorePassword;

  public AMSReporter(SolrMetricManager metricManager, String registryName) {
    super(metricManager, registryName);
  }

  protected abstract GaugeConverter<T> gaugeConverter();

  @Override
  protected void doInit() {
    reporter = new ScheduledAMSReporterBuilder<T>()
            .setRegistry(metricManager.registry(registryName))
            .setRateUnit(TimeUnit.SECONDS)
            .setDurationUnit(TimeUnit.MILLISECONDS)
            .setFilter(newMetricFilter())
            .setAMSClient(new SolrMetricsSink(amsCollectorHosts.split(","), amsCollectorPort, amsCollectorProtocol,
                    new SolrMetricsSecurityConfig(trustStoreLocation, trustStoreType, trustStorePassword)))
            .setGaugeConverter(gaugeConverter())
            .setRegistryName(registryName)
            .build();
    reporter.start(period, TimeUnit.SECONDS);
  }

  @Override
  protected void validate() throws IllegalStateException {
    if (period < 1) {
      throw new IllegalStateException("Init argument 'period' is in time unit 'seconds' and must be at least 1.");
    }
    if (https == amsCollectorProtocol) {
      if (isBlank(trustStoreLocation))
        throw new IllegalStateException("If amsCollectorProtocol is https then trustStoreLocation can not be null or empty!");
      if (isBlank(trustStoreType))
        throw new IllegalStateException("If amsCollectorProtocol is https then trustStoreType can not be null or empty!");
    }
  }

  @Override
  public void close() {
    if (reporter != null) {
      reporter.close();
    }
  }

  public void setAmsCollectorHosts(String amsCollectorHosts) {
    this.amsCollectorHosts = amsCollectorHosts;
  }

  public void setAmsCollectorPort(int amsCollectorPort) {
    this.amsCollectorPort = amsCollectorPort;
  }

  public void setAmsCollectorProtocol(String amsCollectorProtocol) {
    this.amsCollectorProtocol = AMSProtocol.valueOf(amsCollectorProtocol);
  }

  public void setTrustStoreLocation(String trustStoreLocation) {
    this.trustStoreLocation = trustStoreLocation;
  }

  public void setTrustStoreType(String trustStoreType) {
    this.trustStoreType = trustStoreType;
  }

  public void setTrustStorePassword(String trustStorePassword) {
    this.trustStorePassword = trustStorePassword;
  }
}
