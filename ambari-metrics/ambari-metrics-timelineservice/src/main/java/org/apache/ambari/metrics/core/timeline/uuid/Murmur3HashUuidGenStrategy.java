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
 * WITHOUT WARRANTIES   OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics.core.timeline.uuid;

import com.google.common.hash.Hashing;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.commons.lang.StringUtils;

public class Murmur3HashUuidGenStrategy extends MetricUuidGenNullRestrictedStrategy {

  /**
   * Compute Murmur3Hash 16 byte UUID for a Metric-App-Instance.
   * @param timelineClusterMetric input metric
   * @param maxLength Max length of returned UUID. (Will always be 16 for this technique)
   * @return 16 byte UUID.
   */  @Override
  byte[] computeUuidInternal(TimelineClusterMetric timelineClusterMetric, int maxLength) {

    String metricString = timelineClusterMetric.getMetricName() + timelineClusterMetric.getAppId();
    if (StringUtils.isNotEmpty(timelineClusterMetric.getInstanceId())) {
      metricString += timelineClusterMetric.getInstanceId();
    }
    byte[] metricBytes = metricString.getBytes();
    return Hashing.murmur3_128().hashBytes(metricBytes).asBytes();
  }

  /**
   * Compute Murmur3Hash 4 byte UUID for a String.
   * @param value String input
   * @param maxLength Max length of returned UUID. (Will always be 4 for this technique)
   * @return 4 byte UUID.
   */
  @Override
  byte[] computeUuidInternal(String value, int maxLength) {
    byte[] valueBytes = value.getBytes();
    return Hashing.murmur3_32().hashBytes(valueBytes).asBytes();
  }
}
