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
package org.apache.ambari.metrics.core.loadsimulator;


import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.ambari.metrics.core.loadsimulator.util.Json;
import org.apache.ambari.metrics.core.loadsimulator.data.AppMetrics;
import org.apache.ambari.metrics.core.loadsimulator.data.HostMetricsGenerator;
import org.apache.ambari.metrics.core.loadsimulator.net.MetricsSender;
import org.apache.ambari.metrics.core.loadsimulator.net.RestMetricsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MetricsSenderWorker implements Callable<String> {
  private final static Logger LOG = LoggerFactory.getLogger(RestMetricsSender.class);

  MetricsSender sender;
  HostMetricsGenerator hmg;

  public MetricsSenderWorker(MetricsSender sender, HostMetricsGenerator metricsGenerator) {
    this.sender = sender;
    hmg = metricsGenerator;
  }

  @Override
  public String call() throws Exception {
    AppMetrics hostMetrics = hmg.createMetrics();

    try {
      String request = new Json().serialize(hostMetrics); //inject?
      String response = sender.pushMetrics(request);

      return response;
    } catch (IOException e) {
      LOG.error("Error while pushing metrics: ", e);
      throw e;
    }

  }
}
