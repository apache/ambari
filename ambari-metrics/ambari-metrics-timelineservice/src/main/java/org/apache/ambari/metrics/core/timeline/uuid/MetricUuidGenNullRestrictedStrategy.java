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
package org.apache.ambari.metrics.core.timeline.uuid;

import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * HBase represents null value for BINARY fields as set of zero bytes. This means that we are not able to difference
 * byte[]{0,0,0,0} and null values in DB. So we should not generate Uuids which contains only zero bytes.
 */
public abstract class MetricUuidGenNullRestrictedStrategy implements MetricUuidGenStrategy {
  private static final Log LOG = LogFactory.getLog(MetricUuidGenNullRestrictedStrategy.class);

  static final int RETRY_NUMBER = 5;

  @Override
  public byte[] computeUuid(TimelineClusterMetric timelineClusterMetric, int maxLength) {
    int retry_attempt = 0;
    byte[] result = null;
    while (retry_attempt++ < RETRY_NUMBER) {
      result = computeUuidInternal(timelineClusterMetric, maxLength);
      for (byte b : result) {
        if (b != 0) {
          return result;
        }
      }
      LOG.debug("Was generated Uuid which can represent null value in DB.");
    }

    LOG.error(String.format("After %n attempts was generated Uuid which can represent null value in DB", RETRY_NUMBER));
    return result;
  }

  @Override
  public byte[] computeUuid(String value, int maxLength) {
    int retry_attempt = 0;
    byte[] result = null;
    while (retry_attempt++ < RETRY_NUMBER) {
      result = computeUuidInternal(value, maxLength);
      for (byte b : result) {
        if (b != 0) {
          return result;
        }
      }
      LOG.debug("Was generated Uuid which can represent null value in DB.");
    }

    LOG.error(String.format("After %n attempts was generated Uuid which can represent null value in DB", RETRY_NUMBER));
    return result;
  }

  abstract byte[] computeUuidInternal(TimelineClusterMetric timelineClusterMetric, int maxLength);
  abstract byte[] computeUuidInternal(String value, int maxLength);
}
