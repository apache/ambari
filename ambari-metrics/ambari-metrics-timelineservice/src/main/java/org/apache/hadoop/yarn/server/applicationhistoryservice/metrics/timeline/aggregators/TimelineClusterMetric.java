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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;

public class TimelineClusterMetric {
  private String metricName;
  private String appId;
  private String instanceId;
  private long timestamp;

  public TimelineClusterMetric(String metricName, String appId, String instanceId,
                        long timestamp) {
    this.metricName = metricName;
    this.appId = appId;
    this.instanceId = instanceId;
    this.timestamp = timestamp;
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

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TimelineClusterMetric that = (TimelineClusterMetric) o;

    if (timestamp != that.timestamp) return false;
    if (appId != null ? !appId.equals(that.appId) : that.appId != null)
      return false;
    if (instanceId != null ? !instanceId.equals(that.instanceId) : that.instanceId != null)
      return false;
    if (!metricName.equals(that.metricName)) return false;

    return true;
  }

  public boolean equalsExceptTime(TimelineClusterMetric metric) {
    if (!metricName.equals(metric.metricName)) return false;
    if (!appId.equals(metric.appId)) return false;
    if (instanceId != null ? !instanceId.equals(metric.instanceId) : metric.instanceId != null)
      return false;

    return true;
  }
  @Override
  public int hashCode() {
    int result = metricName.hashCode();
    result = 31 * result + (appId != null ? appId.hashCode() : 0);
    result = 31 * result + (instanceId != null ? instanceId.hashCode() : 0);
    result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "TimelineClusterMetric{" +
      "metricName='" + metricName + '\'' +
      ", appId='" + appId + '\'' +
      ", instanceId='" + instanceId + '\'' +
      ", timestamp=" + timestamp +
      '}';
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
}
