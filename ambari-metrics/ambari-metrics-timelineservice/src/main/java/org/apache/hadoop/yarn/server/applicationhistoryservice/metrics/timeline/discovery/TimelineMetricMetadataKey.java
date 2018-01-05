/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;

@XmlRootElement
public class TimelineMetricMetadataKey {
  String metricName;
  String appId;
  String instanceId;

  public TimelineMetricMetadataKey(String metricName, String appId, String instanceId) {
    this.metricName = metricName;
    this.appId = appId;
    this.instanceId = instanceId;
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

  public void setAppId(String appId) {
    this.appId = appId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TimelineMetricMetadataKey that = (TimelineMetricMetadataKey) o;

    if (!metricName.equals(that.metricName)) return false;
    if (!appId.equals(that.appId)) return false;
    return (StringUtils.isNotEmpty(instanceId) ? instanceId.equals(that.instanceId) : StringUtils.isEmpty(that.instanceId));
  }

  @Override
  public int hashCode() {
    int result = metricName.hashCode();
    result = 31 * result + (appId != null ? appId.hashCode() : 0);
    result = 31 * result + (instanceId != null ? instanceId.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TimelineMetricMetadataKey{" +
      "metricName='" + metricName + '\'' +
      ", appId='" + appId + '\'' +
      ", instanceId='" + instanceId + '\'' +
      '}';
  }
}
