package org.apache.ambari.metrics.core.timeline.source;

import org.apache.ambari.metrics.core.timeline.sink.ExternalMetricsSink;

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
public interface InternalSourceProvider {

  enum SOURCE_NAME {
    RAW_METRICS,
    MINUTE_HOST_AGGREAGATE_METRICS,
    HOURLY_HOST_AGGREAGATE_METRICS,
    DAILY_HOST_AGGREAGATE_METRICS,
    MINUTE_CLUSTER_AGGREAGATE_METRICS,
    HOURLY_CLUSTER_AGGREAGATE_METRICS,
    DAILY_CLUSTER_AGGREAGATE_METRICS,
  }

  /**
   * Provide Source for metrics data.
   * @return {@link InternalMetricsSource}
   */
  InternalMetricsSource getInternalMetricsSource(SOURCE_NAME sourceName, int sinkIntervalSeconds, ExternalMetricsSink sink);
}
