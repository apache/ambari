/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.metrics2.sink.timeline;

import org.apache.commons.lang.StringUtils;

public class TimelineMetricKey {
  public String metricName;
  public String appId;
  public String instanceId = null;
  public String hostName;
  public byte[] uuid;

  public TimelineMetricKey(String metricName, String appId, String instanceId, String hostName, byte[] uuid) {
    this.metricName = metricName;
    this.appId = appId;
    this.instanceId = instanceId;
    this.hostName = hostName;
    this.uuid = uuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TimelineMetricKey that = (TimelineMetricKey) o;

    if (!metricName.equals(that.metricName)) return false;
    if (!appId.equals(that.appId)) return false;
    if (!hostName.equals(that.hostName)) return false;
    return (StringUtils.isNotEmpty(instanceId) ? instanceId.equals(that.instanceId) : StringUtils.isEmpty(that.instanceId));
  }

  @Override
  public int hashCode() {
    int result = metricName.hashCode();
    result = 31 * result + (appId != null ? appId.hashCode() : 0);
    result = 31 * result + (instanceId != null ? instanceId.hashCode() : 0);
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    return result;
  }

}
