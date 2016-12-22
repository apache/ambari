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


import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
}
