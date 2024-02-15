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

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.commons.lang.StringUtils;

public class MD5UuidGenStrategy extends MetricUuidGenNullRestrictedStrategy {

  public MD5UuidGenStrategy() {
  }

  @Override
  protected byte[] computeUuidInternal(TimelineClusterMetric timelineClusterMetric, int maxLength) {

    String metricString = timelineClusterMetric.getMetricName() + timelineClusterMetric.getAppId();
    if (StringUtils.isNotEmpty(timelineClusterMetric.getInstanceId())) {
      metricString += timelineClusterMetric.getInstanceId();
    }
    byte[] metricBytes = metricString.getBytes();

    UUID uuid = UUID.nameUUIDFromBytes(metricBytes);
    ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[maxLength]);
    byteBuffer.putLong(uuid.getMostSignificantBits());
    byteBuffer.putLong(uuid.getLeastSignificantBits());

    return byteBuffer.array();
  }

  @Override
  protected byte[] computeUuidInternal(String value, int maxLength) {

    byte[] valueBytes = value.getBytes();
    UUID uuid = UUID.nameUUIDFromBytes(valueBytes);
    ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[maxLength]);
    byteBuffer.putLong(uuid.getMostSignificantBits());
    byteBuffer.putLong(uuid.getLeastSignificantBits());

    return byteBuffer.array();
  }
}
