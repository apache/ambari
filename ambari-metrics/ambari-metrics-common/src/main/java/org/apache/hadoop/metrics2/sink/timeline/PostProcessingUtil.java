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
package org.apache.hadoop.metrics2.sink.timeline;

import java.util.Map;
import java.util.TreeMap;

public class PostProcessingUtil {

  /*
    Helper function to interpolate missing data on a series.
  */
  public static Map<Long, Double> interpolateMissingData(Map<Long, Double> metricValues, long expectedInterval) {

    if (metricValues == null)
      return null;

    Long prevTime = null;
    Double prevVal = null;
    Map<Long, Double> interpolatedMetricValues = new TreeMap<Long, Double>();

    for (Map.Entry<Long, Double> timeValueEntry : metricValues.entrySet()) {
      Long currTime = timeValueEntry.getKey();
      Double currVal = timeValueEntry.getValue();

      if (prevTime != null) {
        Long stepTime = prevTime;
        while ((currTime - stepTime) > expectedInterval) {
          stepTime+=expectedInterval;
          double interpolatedValue = interpolate(stepTime,
            prevTime, prevVal,
            currTime, currVal);
          interpolatedMetricValues.put(stepTime, interpolatedValue);
        }
      }

      interpolatedMetricValues.put(currTime, currVal);
      prevTime = currTime;
      prevVal = currVal;
    }
    return interpolatedMetricValues;
  }

  public static Double interpolate(Long t, Long t1, Double m1,
                                   Long t2, Double m2) {


    //Linear Interpolation : y = y0 + (y1 - y0) * ((x - x0) / (x1 - x0))
    if (m1 == null && m2 == null) {
      return null;
    }

    if (m1 == null)
      return m2;

    if (m2 == null)
      return m1;

    if (t1 == null || t2 == null)
      return null;

    double slope = (m2 - m1) / (t2 - t1);
    return m1 +  slope * (t - t1);
  }

}
