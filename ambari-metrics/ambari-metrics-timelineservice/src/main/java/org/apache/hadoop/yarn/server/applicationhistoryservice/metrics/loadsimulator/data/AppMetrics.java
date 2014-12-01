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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .loadsimulator.data;

import java.util.ArrayList;
import java.util.Collection;

/**
 * AppMetrics is a class that helps to create properly initialized metrics for
 * current app. It also holds the
 * metrics and can be serialized to json.
 */
public class AppMetrics {

  private final Collection<Metric> metrics = new ArrayList<Metric>();
  private final transient ApplicationInstance applicationId;
  private final transient long startTime;

  public AppMetrics(ApplicationInstance applicationId, long startTime) {
    this.applicationId = applicationId;
    this.startTime = startTime;
  }

  public Metric createMetric(String metricName) {
    return new Metric(applicationId, metricName, startTime);
  }

  public void addMetric(Metric metric) {
    metrics.add(metric);
  }

}
