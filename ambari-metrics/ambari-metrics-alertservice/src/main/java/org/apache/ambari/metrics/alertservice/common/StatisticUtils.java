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
package org.apache.ambari.metrics.alertservice.common;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class StatisticUtils {

  public static double mean(Collection<Double> values) {
    double sum = 0;
    for (double d : values) {
      sum += d;
    }
    return sum / values.size();
  }

  public static double variance(Collection<Double> values) {
    double avg =  mean(values);
    double variance = 0;
    for (double d : values) {
      variance += Math.pow(d - avg, 2.0);
    }
    return variance;
  }

  public static double sdev(Collection<Double> values, boolean useBesselsCorrection) {
    double variance = variance(values);
    int n = (useBesselsCorrection) ? values.size() - 1 : values.size();
    return Math.sqrt(variance / n);
  }

  public static double median(Collection<Double> values) {
    ArrayList<Double> clonedValues = new ArrayList<Double>(values);
    Collections.sort(clonedValues);
    int n = values.size();

    if (n % 2 != 0) {
      return clonedValues.get((n-1)/2);
    } else {
      return ( clonedValues.get((n-1)/2) + clonedValues.get(n/2) ) / 2;
    }
  }



//  public static void main(String[] args) {
//
//    Collection<Double> values = new ArrayList<>();
//    values.add(1.0);
//    values.add(2.0);
//    values.add(3.0);
//    values.add(4.0);
//    values.add(5.0);
//
//    System.out.println(mean(values));
//    System.out.println(sdev(values, false));
//    System.out.println(median(values));
//  }
}
