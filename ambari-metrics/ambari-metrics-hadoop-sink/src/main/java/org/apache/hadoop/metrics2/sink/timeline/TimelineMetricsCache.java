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
package org.apache.hadoop.metrics2.sink.timeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;

import java.util.LinkedHashMap;
import java.util.Map;

@InterfaceAudience.Public
@InterfaceStability.Evolving
public class TimelineMetricsCache {

  private final TimelineMetricHolder timelineMetricCache = new TimelineMetricHolder();
  private static final Log LOG = LogFactory.getLog(TimelineMetric.class);
  static final int MAX_RECS_PER_NAME_DEFAULT = 10000;
  static final int MAX_EVICTION_TIME_MILLIS = 59000; // ~ 1 min
  private final int maxRecsPerName;
  private final int maxEvictionTimeInMillis;

  TimelineMetricsCache(int maxRecsPerName, int maxEvictionTimeInMillis) {
    this.maxRecsPerName = maxRecsPerName;
    this.maxEvictionTimeInMillis = maxEvictionTimeInMillis;
  }

  class TimelineMetricWrapper {
    private long timeDiff = -1;
    private long oldestTimestamp = -1;
    private TimelineMetric timelineMetric;

    TimelineMetricWrapper(TimelineMetric timelineMetric) {
      this.timelineMetric = timelineMetric;
      this.oldestTimestamp = timelineMetric.getStartTime();
    }

    private void updateTimeDiff(long timestamp) {
      if (oldestTimestamp != -1 && timestamp > oldestTimestamp) {
        timeDiff = timestamp - oldestTimestamp;
      } else {
        oldestTimestamp = timestamp;
      }
    }

    public void putMetric(TimelineMetric metric) {
      this.timelineMetric.addMetricValues(metric.getMetricValues());
      updateTimeDiff(metric.getStartTime());
    }

    public long getTimeDiff() {
      return timeDiff;
    }

    public TimelineMetric getTimelineMetric() {
      return timelineMetric;
    }
  }

  // TODO: Change to ConcurentHashMap with weighted eviction
  class TimelineMetricHolder extends LinkedHashMap<String, TimelineMetricWrapper> {
    private static final long serialVersionUID = 1L;
    private boolean gotOverflow = false;

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, TimelineMetricWrapper> eldest) {
      boolean overflow = size() > maxRecsPerName;
      if (overflow && !gotOverflow) {
        LOG.warn("Metrics cache overflow at "+ size() +" for "+ eldest);
        gotOverflow = true;
      }
      return overflow;
    }

    public TimelineMetric evict(String metricName) {
      TimelineMetricWrapper metricWrapper = this.get(metricName);

      if (metricWrapper == null
        || metricWrapper.getTimeDiff() < maxEvictionTimeInMillis) {
        return null;
      }

      TimelineMetric timelineMetric = metricWrapper.getTimelineMetric();
      this.remove(metricName);

      return timelineMetric;
    }

    public void put(String metricName, TimelineMetric timelineMetric) {

      TimelineMetricWrapper metric = this.get(metricName);
      if (metric == null) {
        this.put(metricName, new TimelineMetricWrapper(timelineMetric));
      } else {
        metric.putMetric(timelineMetric);
      }
    }
  }

  public TimelineMetric getTimelineMetric(String metricName) {
    if (timelineMetricCache.containsKey(metricName)) {
      return timelineMetricCache.evict(metricName);
    }

    return null;
  }

  public void putTimelineMetric(TimelineMetric timelineMetric) {
    timelineMetricCache.put(timelineMetric.getMetricName(), timelineMetric);
  }
}
