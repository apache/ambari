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

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

public class ScheduledAMSReporterBuilder<T> {
  private String registryName;
  private MetricRegistry registry;
  private String name;
  private MetricFilter filter;
  private TimeUnit rateUnit;
  private TimeUnit durationUnit;
  private ScheduledExecutorService executor;
  private boolean shutdownExecutorOnStop;
  private Set<MetricAttribute> disabledMetricAttributes;
  private SolrMetricsSink amsClient;
  private GaugeConverter<T> gaugeConverter;

  public ScheduledAMSReporterBuilder<T> setRegistryName(String name) {
    this.registryName = name;
    return this;
  }

  public ScheduledAMSReporterBuilder<T> setRegistry(MetricRegistry registry) {
    this.registry = registry;
    return this;
  }

  public ScheduledAMSReporterBuilder<T> setName(String name) {
    this.name = name;
    return this;
  }

  public ScheduledAMSReporterBuilder<T> setFilter(MetricFilter filter) {
    this.filter = filter;
    return this;
  }

  public ScheduledAMSReporterBuilder<T> setRateUnit(TimeUnit rateUnit) {
    this.rateUnit = rateUnit;
    return this;
  }

  public ScheduledAMSReporterBuilder<T> setDurationUnit(TimeUnit durationUnit) {
    this.durationUnit = durationUnit;
    return this;
  }

  public ScheduledAMSReporterBuilder<T> setExecutor(ScheduledExecutorService executor) {
    this.executor = executor;
    return this;
  }

  public ScheduledAMSReporterBuilder<T> setShutdownExecutorOnStop(boolean shutdownExecutorOnStop) {
    this.shutdownExecutorOnStop = shutdownExecutorOnStop;
    return this;
  }

  public ScheduledAMSReporterBuilder<T> setDisabledMetricAttributes(Set<MetricAttribute> disabledMetricAttributes) {
    this.disabledMetricAttributes = disabledMetricAttributes;
    return this;
  }

  public ScheduledAMSReporterBuilder<T> setAMSClient(SolrMetricsSink amsClient) {
    this.amsClient = amsClient;
    return this;
  }

  public ScheduledAMSReporterBuilder<T> setGaugeConverter(GaugeConverter<T> gaugeConverter) {
    this.gaugeConverter = gaugeConverter;
    return this;
  }

  public ScheduledAMSReporter build() {
    return new ScheduledAMSReporter<>(registryName, registry, name, filter, rateUnit, durationUnit, executor,
            shutdownExecutorOnStop, disabledMetricAttributes, amsClient, gaugeConverter);
  }
}