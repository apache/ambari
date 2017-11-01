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
package org.apache.ambari.metrics.adservice.prototype.common;


import java.util.Arrays;

public class StatisticUtils {

  public static double mean(double[] values) {
    double sum = 0;
    for (double d : values) {
      sum += d;
    }
    return sum / values.length;
  }

  public static double variance(double[] values) {
    double avg =  mean(values);
    double variance = 0;
    for (double d : values) {
      variance += Math.pow(d - avg, 2.0);
    }
    return variance;
  }

  public static double sdev(double[]  values, boolean useBesselsCorrection) {
    double variance = variance(values);
    int n = (useBesselsCorrection) ? values.length - 1 : values.length;
    return Math.sqrt(variance / n);
  }

  public static double median(double[] values) {
    double[] clonedValues = Arrays.copyOf(values, values.length);
    Arrays.sort(clonedValues);
    int n = values.length;

    if (n % 2 != 0) {
      return clonedValues[(n-1)/2];
    } else {
      return ( clonedValues[(n-1)/2] + clonedValues[n/2] ) / 2;
    }
  }
}
