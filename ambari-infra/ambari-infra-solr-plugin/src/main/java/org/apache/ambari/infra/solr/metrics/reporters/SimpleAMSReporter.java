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

import static java.util.Collections.singletonList;
import static org.apache.ambari.infra.solr.metrics.reporters.MetricsUtils.toTimelineMetric;

import org.apache.solr.metrics.SolrMetricManager;

public class SimpleAMSReporter extends AMSReporter<Number> {

  public SimpleAMSReporter(SolrMetricManager metricManager, String registryName) {
    super(metricManager, registryName);
  }

  @Override
  protected GaugeConverter<Number> gaugeConverter() {
    return (metricName, gauge, currentMillis) ->
            singletonList(toTimelineMetric(metricName, gauge.getValue().doubleValue(), currentMillis));
  }

}
