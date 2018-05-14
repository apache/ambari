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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration;

public class InternalMetricsCacheProvider {
  private Map<String, InternalMetricsCache> metricsCacheMap = new ConcurrentHashMap<>();
  private static final InternalMetricsCacheProvider instance = new InternalMetricsCacheProvider();

  private InternalMetricsCacheProvider() {
  }

  public static InternalMetricsCacheProvider getInstance() {
    return instance;
  }

  public InternalMetricsCache getCacheInstance(String instanceName) {
    if (metricsCacheMap.containsKey(instanceName)) {
      return metricsCacheMap.get(instanceName);
    } else {
      TimelineMetricConfiguration conf = TimelineMetricConfiguration.getInstance();
      InternalMetricsCache cache = new InternalMetricsCache(instanceName,
        conf.getInternalCacheHeapPercent(instanceName));

      metricsCacheMap.put(instanceName, cache);
      return cache;
    }
  }
}
