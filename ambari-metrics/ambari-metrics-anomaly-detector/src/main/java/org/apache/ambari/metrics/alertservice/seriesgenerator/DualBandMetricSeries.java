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
package org.apache.ambari.metrics.alertservice.seriesgenerator;

import java.util.Random;

public class DualBandMetricSeries implements AbstractMetricSeries {

  double lowBandValue = 0.0;
  double lowBandDeviationPercentage = 0.0;
  int lowBandPeriodSize = 10;
  double highBandValue = 1.0;
  double highBandDeviationPercentage = 0.0;
  int highBandPeriodSize = 10;

  Random random = new Random();
  double lowBandValueLowerLimit, lowBandValueHigherLimit;
  double highBandLowerLimit, highBandUpperLimit;
  int l = 0, h = 0;

  public DualBandMetricSeries(double lowBandValue,
                              double lowBandDeviationPercentage,
                              int lowBandPeriodSize,
                              double highBandValue,
                              double highBandDeviationPercentage,
                              int highBandPeriodSize) {
    this.lowBandValue = lowBandValue;
    this.lowBandDeviationPercentage = lowBandDeviationPercentage;
    this.lowBandPeriodSize = lowBandPeriodSize;
    this.highBandValue = highBandValue;
    this.highBandDeviationPercentage = highBandDeviationPercentage;
    this.highBandPeriodSize = highBandPeriodSize;
    init();
  }

  private void init() {
    lowBandValueLowerLimit = lowBandValue - lowBandDeviationPercentage * lowBandValue;
    lowBandValueHigherLimit = lowBandValue + lowBandDeviationPercentage * lowBandValue;
    highBandLowerLimit = highBandValue - highBandDeviationPercentage * highBandValue;
    highBandUpperLimit = highBandValue + highBandDeviationPercentage * highBandValue;
  }

  @Override
  public double nextValue() {

    double value = 0.0;

    if (l < lowBandPeriodSize) {
      value = lowBandValueLowerLimit + (lowBandValueHigherLimit - lowBandValueLowerLimit) * random.nextDouble();
      l++;
    } else if (h < highBandPeriodSize) {
      value = highBandLowerLimit + (highBandUpperLimit - highBandLowerLimit) * random.nextDouble();
      h++;
    }

    if (l == lowBandPeriodSize && h == highBandPeriodSize) {
      l = 0;
      h = 0;
    }

    return value;
  }

  @Override
  public double[] getSeries(int n) {
    double[] series = new double[n];
    for (int i = 0; i < n; i++) {
      series[i] = nextValue();
    }
    return series;
  }

}
