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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.PostProcessingUtil;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;

/**
 *
 */
public class AggregatorUtils {

  private static final Log LOG = LogFactory.getLog(AggregatorUtils.class);

  public static double[] calculateAggregates(Map<Long, Double> metricValues) {
    double[] values = new double[4];
    double max = Double.MIN_VALUE;
    double min = Double.MAX_VALUE;
    double sum = 0.0;
    int metricCount = 0;

    if (metricValues != null && !metricValues.isEmpty()) {
      for (Double value : metricValues.values()) {
        // TODO: Some nulls in data - need to investigate null values from host
        if (value != null) {
          if (value > max) {
            max = value;
          }
          if (value < min) {
            min = value;
          }
          sum += value;
        }
      }
      metricCount = metricValues.values().size();
    }
    // BR: WHY ZERO is a good idea?
    values[0] = sum;
    values[1] = max != Double.MIN_VALUE ? max : 0.0;
    values[2] = min != Double.MAX_VALUE ? min : 0.0;
    values[3] = metricCount;

    return values;
  }

  public static Map<TimelineClusterMetric, Double> sliceFromTimelineMetric(
      TimelineMetric timelineMetric, List<Long[]> timeSlices, boolean interpolationEnabled) {

    if (timelineMetric.getMetricValues().isEmpty()) {
      return null;
    }

    Map<TimelineClusterMetric, Double> timelineClusterMetricMap =
        new HashMap<>();

    Long prevTimestamp = -1l;
    TimelineClusterMetric prevMetric = null;
    int count = 0;
    double sum = 0.0;

    Map<Long,Double> timeSliceValueMap = new HashMap<>();
    for (Map.Entry<Long, Double> metric : timelineMetric.getMetricValues().entrySet()) {
      if (metric.getValue() == null) {
        continue;
      }

      Long timestamp = getSliceTimeForMetric(timeSlices, Long.parseLong(metric.getKey().toString()));
      if (timestamp != -1) {
        // Metric is within desired time range
        TimelineClusterMetric clusterMetric = new TimelineClusterMetric(
            timelineMetric.getMetricName(),
            timelineMetric.getAppId(),
            timelineMetric.getInstanceId(),
            timestamp);

        if (prevTimestamp < 0 || timestamp.equals(prevTimestamp)) {
          Double newValue = metric.getValue();
          if (newValue > 0.0) {
            sum += newValue;
            count++;
          }
        } else {
          double metricValue = (count > 0) ? (sum / count) : 0.0;
          timelineClusterMetricMap.put(prevMetric, metricValue);
          timeSliceValueMap.put(prevMetric.getTimestamp(), metricValue);
          sum = metric.getValue();
          count = sum > 0.0 ? 1 : 0;
        }

        prevTimestamp = timestamp;
        prevMetric = clusterMetric;
      }
    }

    if (prevTimestamp > 0) {
      double metricValue = (count > 0) ? (sum / count) : 0.0;
      timelineClusterMetricMap.put(prevMetric, metricValue);
      timeSliceValueMap.put(prevTimestamp, metricValue);
    }

    if (interpolationEnabled) {
      Map<Long, Double> interpolatedValues = interpolateMissingPeriods(timelineMetric.getMetricValues(), timeSlices, timeSliceValueMap, timelineMetric.getType());
      for (Map.Entry<Long, Double> entry : interpolatedValues.entrySet()) {
        TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(timelineMetric.getMetricName(), timelineMetric.getAppId(), timelineMetric.getInstanceId(), entry.getKey());
        timelineClusterMetricMap.putIfAbsent(timelineClusterMetric, entry.getValue());
      }
    }

    return timelineClusterMetricMap;
  }

  private static Map<Long, Double> interpolateMissingPeriods(TreeMap<Long, Double> metricValues,
                                               List<Long[]> timeSlices,
                                               Map<Long, Double> timeSliceValueMap, String type) {
    Map<Long, Double> resultClusterMetricMap = new HashMap<>();

    if (StringUtils.isNotEmpty(type) && "COUNTER".equalsIgnoreCase(type)) {
      //For Counter Based metrics, ok to do interpolation and extrapolation

      List<Long> requiredTimestamps = new ArrayList<>();
      for (Long[] timeSlice : timeSlices) {
        if (!timeSliceValueMap.containsKey(timeSlice[1])) {
          requiredTimestamps.add(timeSlice[1]);
        }
      }
      Map<Long, Double> interpolatedValuesMap = PostProcessingUtil.interpolate(metricValues, requiredTimestamps);

      if (interpolatedValuesMap != null) {
        for (Map.Entry<Long, Double> entry : interpolatedValuesMap.entrySet()) {
          Double interpolatedValue = entry.getValue();

          if (interpolatedValue != null) {
            resultClusterMetricMap.put( entry.getKey(), interpolatedValue);
          } else {
            LOG.debug("Cannot compute interpolated value, hence skipping.");
          }
        }
      }
    } else {
      //For other metrics, ok to do only interpolation

      Double defaultNextSeenValue = null;
      if (MapUtils.isEmpty(timeSliceValueMap) && MapUtils.isNotEmpty(metricValues)) {
        //If no value was found within the start_time based slices, but the metric has value in the server_time range,
        // use that.

        Map.Entry<Long,Double> firstEntry  = metricValues.firstEntry();
        defaultNextSeenValue = firstEntry.getValue();
        LOG.debug("Found a data point outside timeslice range: " + new Date(firstEntry.getKey()) + ": " + defaultNextSeenValue);
      }

      for (int sliceNum = 0; sliceNum < timeSlices.size(); sliceNum++) {
        Long[] timeSlice = timeSlices.get(sliceNum);

        if (!timeSliceValueMap.containsKey(timeSlice[1])) {
          LOG.debug("Found an empty slice : " + new Date(timeSlice[0]) + ", " + new Date(timeSlice[1]));

          Double lastSeenValue = null;
          int index = sliceNum - 1;
          Long[] prevTimeSlice = null;
          while (lastSeenValue == null && index >= 0) {
            prevTimeSlice = timeSlices.get(index--);
            lastSeenValue = timeSliceValueMap.get(prevTimeSlice[1]);
          }

          Double nextSeenValue = null;
          index = sliceNum + 1;
          Long[] nextTimeSlice = null;
          while (nextSeenValue == null && index < timeSlices.size()) {
            nextTimeSlice = timeSlices.get(index++);
            nextSeenValue = timeSliceValueMap.get(nextTimeSlice[1]);
          }

          if (nextSeenValue == null) {
            nextSeenValue = defaultNextSeenValue;
          }

          Double interpolatedValue = PostProcessingUtil.interpolate(timeSlice[1],
              (prevTimeSlice != null ? prevTimeSlice[1] : null), lastSeenValue,
              (nextTimeSlice != null ? nextTimeSlice[1] : null), nextSeenValue);

          if (interpolatedValue != null) {
            LOG.debug("Interpolated value : " + interpolatedValue);
            resultClusterMetricMap.put(timeSlice[1], interpolatedValue);
          } else {
            LOG.debug("Cannot compute interpolated value, hence skipping.");
          }
        }
      }
    }
    return resultClusterMetricMap;
  }

  /**
   * Return end of the time slice into which the metric fits.
   */
  public static Long getSliceTimeForMetric(List<Long[]> timeSlices, Long timestamp) {
    for (Long[] timeSlice : timeSlices) {
      if (timestamp >= timeSlice[0] && timestamp < timeSlice[1]) {
        return timeSlice[1];
      }
    }
    return -1l;
  }

  /**
   * Return time slices to normalize the timeseries data.
   */
  public static  List<Long[]> getTimeSlices(long startTime, long endTime, long timeSliceIntervalMillis) {
    List<Long[]> timeSlices = new ArrayList<Long[]>();
    long sliceStartTime = startTime;
    while (sliceStartTime < endTime) {
      timeSlices.add(new Long[] { sliceStartTime, sliceStartTime + timeSliceIntervalMillis });
      sliceStartTime += timeSliceIntervalMillis;
    }
    return timeSlices;
  }

  public static long getRoundedCheckPointTimeMillis(long referenceTime, long aggregatorPeriod) {
    return referenceTime - (referenceTime % aggregatorPeriod);
  }

  public static long getRoundedAggregateTimeMillis(long aggregatorPeriod) {
    long currentTime = System.currentTimeMillis();
    return currentTime - (currentTime % aggregatorPeriod);
  }

  public static String getJavaRegexFromSqlRegex(String sqlRegex) {
    String javaRegEx;
    if (sqlRegex.contains("*") || sqlRegex.contains("__%")) {
      //Special case handling for metric name with * and __%.
      //For example, dfs.NNTopUserOpCounts.windowMs=300000.op=*.user=%.count
      // or dfs.NNTopUserOpCounts.windowMs=300000.op=__%.user=%.count
      String metricNameWithEscSeq = sqlRegex.replace("*", "\\*").replace("__%", "..%");
      javaRegEx = metricNameWithEscSeq.replace("%", ".*");
    } else {
      javaRegEx = sqlRegex.replace("%", ".*");
    }
    return javaRegEx;
  }
}
