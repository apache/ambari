/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.core.timeline.source.cache;

public class InternalMetricCacheKey {
  private String metricName;
  private String appId;
  private String instanceId;
  private String hostname;
  private long startTime; // Useful for debugging

  public InternalMetricCacheKey(String metricName, String appId, String instanceId, String hostname, long startTime) {
    this.metricName = metricName;
    this.appId = appId;
    this.instanceId = instanceId;
    this.hostname = hostname;
    this.startTime = startTime;
  }

  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    InternalMetricCacheKey that = (InternalMetricCacheKey) o;

    if (!getMetricName().equals(that.getMetricName())) return false;
    if (!getAppId().equals(that.getAppId())) return false;
    if (getInstanceId() != null ? !getInstanceId().equals(that.getInstanceId()) : that.getInstanceId() != null)
      return false;
    return getHostname() != null ? getHostname().equals(that.getHostname()) : that.getHostname() == null;

  }

  @Override
  public int hashCode() {
    int result = getMetricName().hashCode();
    result = 31 * result + getAppId().hashCode();
    result = 31 * result + (getInstanceId() != null ? getInstanceId().hashCode() : 0);
    result = 31 * result + (getHostname() != null ? getHostname().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "InternalMetricCacheKey{" +
      "metricName='" + metricName + '\'' +
      ", appId='" + appId + '\'' +
      ", instanceId='" + instanceId + '\'' +
      ", hostname='" + hostname + '\'' +
      ", startTime=" + startTime +
      '}';
  }
}
