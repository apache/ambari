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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.loadsimulator.data;


import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .loadsimulator.util.RandomMetricsProvider;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .loadsimulator.util.TimeStampProvider;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class HostMetricsGenerator {

  private Map<String, RandomMetricsProvider> metricDataProviders = new HashMap<String, RandomMetricsProvider>();
  private final TimeStampProvider tsp;
  private final ApplicationInstance id;

  public HostMetricsGenerator(ApplicationInstance id,
                              TimeStampProvider timeStamps,
                              Map<String, RandomMetricsProvider> metricDataProviders) {
    this.id = id;
    this.tsp = timeStamps;
    this.metricDataProviders = metricDataProviders;
  }

  public AppMetrics createMetrics() {
    long[] timestamps = tsp.timestampsForNextInterval();
    AppMetrics appMetrics = new AppMetrics(id, timestamps[0]);

    for (Map.Entry<String, RandomMetricsProvider> entry : metricDataProviders.entrySet()) {
      String metricName = entry.getKey();
      RandomMetricsProvider metricData = entry.getValue();

      Metric metric = appMetrics.createMetric(metricName);
      for (long timestamp : timestamps) {
        metric.putMetric(timestamp, String.valueOf(metricData.next()));
      }
      appMetrics.addMetric(metric);
    }

    return appMetrics;
  }

}
