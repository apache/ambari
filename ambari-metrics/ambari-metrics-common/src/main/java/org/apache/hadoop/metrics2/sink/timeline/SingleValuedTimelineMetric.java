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
package org.apache.hadoop.metrics2.sink.timeline;

/**
 * This class prevents creating a TreeMap for every instantiation of a metric
 * read from the store. The methods are meant to provide interoperability
 * with @TimelineMetric
 */
public class SingleValuedTimelineMetric {
  private Double value;
  private String metricName;
  private String appId;
  private String instanceId;
  private String hostName;
  private Long startTime;

  public void setSingleTimeseriesValue(Long startTime, Double value) {
    this.startTime = startTime;
    this.value = value;
  }

  public SingleValuedTimelineMetric(String metricName, String appId,
                                    String instanceId, String hostName,
                                    long startTime) {
    this.metricName = metricName;
    this.appId = appId;
    this.instanceId = instanceId;
    this.hostName = hostName;
    this.startTime = startTime;
  }

  public long getStartTime() {
    return startTime;
  }

  public Double getValue() {
    return value;
  }

  public String getMetricName() {
    return metricName;
  }

  public String getAppId() {
    return appId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getHostName() {
    return hostName;
  }

  public boolean equalsExceptTime(TimelineMetric metric) {
    if (!metricName.equals(metric.getMetricName())) return false;
    if (hostName != null ? !hostName.equals(metric.getHostName()) : metric.getHostName() != null)
      return false;
    if (appId != null ? !appId.equals(metric.getAppId()) : metric.getAppId() != null)
      return false;
    if (instanceId != null ? !instanceId.equals(metric.getInstanceId()) : metric.getInstanceId() != null) return false;

    return true;
  }

  public TimelineMetric getTimelineMetric() {
    TimelineMetric metric = new TimelineMetric();
    metric.setMetricName(this.metricName);
    metric.setAppId(this.appId);
    metric.setHostName(this.hostName);
    metric.setInstanceId(this.instanceId);
    metric.setStartTime(this.startTime);
    metric.getMetricValues().put(startTime, value);
    return metric;
  }
}
