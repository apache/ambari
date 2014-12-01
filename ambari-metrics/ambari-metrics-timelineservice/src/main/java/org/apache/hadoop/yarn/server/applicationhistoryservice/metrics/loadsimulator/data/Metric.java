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

import java.util.LinkedHashMap;
import java.util.Map;

public class Metric {

  private String instanceid;
  private String hostname;
  private Map<String, String> metrics = new LinkedHashMap<String, String>();
  private String starttime;
  private String appid;
  private String metricname;

  // i don't like this ctor, but it has to be public for json deserialization
  public Metric() {
  }

  public Metric(ApplicationInstance app, String metricName, long startTime) {
    this.hostname = app.getHostName();
    this.appid = app.getAppId().getId();
    this.instanceid = app.getInstanceId();
    this.metricname = metricName;
    this.starttime = Long.toString(startTime);
  }

  public void putMetric(long timestamp, String value) {
    metrics.put(Long.toString(timestamp), value);
  }

  public String getInstanceid() {
    return instanceid;
  }

  public String getHostname() {
    return hostname;
  }

  public Map<String, String> getMetrics() {
    return metrics;
  }

  public String getStarttime() {
    return starttime;
  }

  public String getAppid() {
    return appid;
  }

  public String getMetricname() {
    return metricname;
  }
}
