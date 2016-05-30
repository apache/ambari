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
package org.apache.hadoop.metrics2.sink.timeline.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@InterfaceAudience.Public
@InterfaceStability.Evolving
public class TimelineMetricsCache {

  private final Cache<String, TimelineMetricWrapper> timelineMetricCache;
  private static final Log LOG = LogFactory.getLog(TimelineMetric.class);
  public static final int MAX_RECS_PER_NAME_DEFAULT = 10000;
  public static final int MAX_EVICTION_TIME_MILLIS = 59000; // ~ 1 min
  private final int maxRecsPerName;
  private final int maxEvictionTimeInMillis;
  private boolean skipCounterTransform = true;
  private final Map<String, Double> counterMetricLastValue = new HashMap<String, Double>();

  public TimelineMetricsCache(int maxRecsPerName, int maxEvictionTimeInMillis) {
    this(maxRecsPerName, maxEvictionTimeInMillis, false);
  }

  public TimelineMetricsCache(int maxRecsPerName, int maxEvictionTimeInMillis,
                              boolean skipCounterTransform) {
    this.maxRecsPerName = maxRecsPerName;
    this.maxEvictionTimeInMillis = maxEvictionTimeInMillis;
    this.skipCounterTransform = skipCounterTransform;
    this.timelineMetricCache = CacheBuilder.newBuilder().expireAfterWrite(maxEvictionTimeInMillis * 2, TimeUnit.MILLISECONDS).build();
  }

  class TimelineMetricWrapper {
    private Cache<Long, Double> dataPointsCache;
    private TimelineMetric timelineMetric;
    private Long oldestTimeStamp;
    private Long newestTimeStamp;

    TimelineMetricWrapper(TimelineMetric timelineMetric) {
      this.timelineMetric = timelineMetric;
      dataPointsCache = CacheBuilder.newBuilder().
              maximumSize(maxRecsPerName).expireAfterWrite(maxEvictionTimeInMillis * 2, TimeUnit.MILLISECONDS).build();

      putMetric(timelineMetric);
    }

    public synchronized void putMetric(TimelineMetric metric) {
      if (dataPointsCache.size() == 0) {
        oldestTimeStamp = metric.getStartTime();
        newestTimeStamp = metric.getStartTime();
      }
      TreeMap<Long, Double> metricValues = metric.getMetricValues();
      for (Map.Entry<Long, Double> entry : metricValues.entrySet()) {
        Long key = entry.getKey();
        dataPointsCache.put(key, entry.getValue());
      }
      oldestTimeStamp = Math.min(oldestTimeStamp, metric.getStartTime());
      newestTimeStamp = Math.max(newestTimeStamp, metric.getStartTime());
    }

    public synchronized TimelineMetric getTimelineMetric() {
      TreeMap<Long, Double> metricValues = new TreeMap<>(dataPointsCache.asMap());
      if (metricValues.isEmpty() || newestTimeStamp - oldestTimeStamp < maxEvictionTimeInMillis) {
        return null;
      }
      dataPointsCache.invalidateAll();
      timelineMetric.setStartTime(metricValues.firstKey());
      timelineMetric.setMetricValues(metricValues);
      return new TimelineMetric(timelineMetric);
    }
  }

  public TimelineMetric getTimelineMetric(String metricName) {
    TimelineMetricWrapper timelineMetricWrapper = timelineMetricCache.getIfPresent(metricName);
    if (timelineMetricWrapper != null) {
      return timelineMetricWrapper.getTimelineMetric();
    }
    return null;
  }

  public TimelineMetrics getAllMetrics() {
    TimelineMetrics timelineMetrics = new TimelineMetrics();
    Collection<TimelineMetricWrapper> timelineMetricWrapperCollection = timelineMetricCache.asMap().values();
    List<TimelineMetric> timelineMetricList =
            new ArrayList<>(timelineMetricWrapperCollection.size());

    for (TimelineMetricWrapper timelineMetricWrapper : timelineMetricWrapperCollection) {
      timelineMetricList.add(timelineMetricWrapper.getTimelineMetric());
    }

    timelineMetrics.setMetrics(timelineMetricList);
    return timelineMetrics;
  }


  public void putTimelineMetric(TimelineMetric timelineMetric) {
    String metricName = timelineMetric.getMetricName();
    TimelineMetricWrapper timelineMetricWrapper = timelineMetricCache.getIfPresent(metricName);

    if (timelineMetricWrapper != null) {
      timelineMetricWrapper.putMetric(timelineMetric);
    } else {
      timelineMetricCache.put(metricName, new TimelineMetricWrapper(timelineMetric));
    }
  }

  private void transformMetricValuesToDerivative(TimelineMetric timelineMetric) {
    String metricName = timelineMetric.getMetricName();
    double firstValue = timelineMetric.getMetricValues().size() > 0
        ? timelineMetric.getMetricValues().entrySet().iterator().next().getValue() : 0;
    Double value = counterMetricLastValue.get(metricName);
    double previousValue = value != null ? value : firstValue;
    Map<Long, Double> metricValues = timelineMetric.getMetricValues();
    TreeMap<Long, Double>   newMetricValues = new TreeMap<Long, Double>();
    for (Map.Entry<Long, Double> entry : metricValues.entrySet()) {
      newMetricValues.put(entry.getKey(), entry.getValue() - previousValue);
      previousValue = entry.getValue();
    }
    timelineMetric.setMetricValues(newMetricValues);
    counterMetricLastValue.put(metricName, previousValue);
  }

  public void putTimelineMetric(TimelineMetric timelineMetric, boolean isCounter) {
    if (isCounter && !skipCounterTransform) {
      transformMetricValuesToDerivative(timelineMetric);
    }
    putTimelineMetric(timelineMetric);
  }
}
