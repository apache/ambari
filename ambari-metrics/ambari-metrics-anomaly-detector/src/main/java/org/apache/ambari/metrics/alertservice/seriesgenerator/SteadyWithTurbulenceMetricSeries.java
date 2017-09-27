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

public class SteadyWithTurbulenceMetricSeries implements AbstractMetricSeries {

  double steadyStateValue = 0.0;
  double steadyStateDeviationPercentage = 0.0;
  double turbulentPeriodDeviationLowerPercentage = 0.3;
  double turbulentPeriodDeviationHigherPercentage = 0.5;
  int turbulentPeriodLength = 5;
  int turbulentStatePosition = 1;
  int approximateSeriesLength = 10;

  Random random = new Random();
  double valueDeviationLowerLimit;
  double valueDeviationHigherLimit;
  double tPeriodLowerLimit;
  double tPeriodUpperLimit;
  int tPeriodStartIndex = 0;
  int index = 0;

  public SteadyWithTurbulenceMetricSeries(int approximateSeriesLength,
                                          double steadyStateValue,
                                          double steadyStateDeviationPercentage,
                                          double turbulentPeriodDeviationLowerPercentage,
                                          double turbulentPeriodDeviationHigherPercentage,
                                          int turbulentPeriodLength,
                                          int turbulentStatePosition) {
    this.approximateSeriesLength = approximateSeriesLength;
    this.steadyStateValue = steadyStateValue;
    this.steadyStateDeviationPercentage = steadyStateDeviationPercentage;
    this.turbulentPeriodDeviationLowerPercentage = turbulentPeriodDeviationLowerPercentage;
    this.turbulentPeriodDeviationHigherPercentage = turbulentPeriodDeviationHigherPercentage;
    this.turbulentPeriodLength = turbulentPeriodLength;
    this.turbulentStatePosition = turbulentStatePosition;
    init();
  }

  private void init() {

    if (turbulentStatePosition == 1) {
      tPeriodStartIndex = (int) (0.25 * approximateSeriesLength + (0.25 * approximateSeriesLength * random.nextDouble()));
    } else if (turbulentStatePosition == 2) {
      tPeriodStartIndex = approximateSeriesLength - turbulentPeriodLength;
    }

    valueDeviationLowerLimit = steadyStateValue - steadyStateDeviationPercentage * steadyStateValue;
    valueDeviationHigherLimit = steadyStateValue + steadyStateDeviationPercentage * steadyStateValue;

    tPeriodLowerLimit = steadyStateValue + turbulentPeriodDeviationLowerPercentage * steadyStateValue;
    tPeriodUpperLimit = steadyStateValue + turbulentPeriodDeviationHigherPercentage * steadyStateValue;
  }

  @Override
  public double nextValue() {

    double value;

    if (index >= tPeriodStartIndex && index <= (tPeriodStartIndex + turbulentPeriodLength)) {
      value = tPeriodLowerLimit + (tPeriodUpperLimit - tPeriodLowerLimit) * random.nextDouble();
    } else {
      value = valueDeviationLowerLimit + (valueDeviationHigherLimit - valueDeviationLowerLimit) * random.nextDouble();
    }
    index++;
    return value;
  }

  @Override
  public double[] getSeries(int n) {

    double[] series = new double[n];
    int turbulentPeriodStartIndex = 0;

    if (turbulentStatePosition == 1) {
      turbulentPeriodStartIndex = (int) (0.25 * n + (0.25 * n * random.nextDouble()));
    } else if (turbulentStatePosition == 2) {
      turbulentPeriodStartIndex = n - turbulentPeriodLength;
    }

    double valueDevLowerLimit = steadyStateValue - steadyStateDeviationPercentage * steadyStateValue;
    double valueDevHigherLimit = steadyStateValue + steadyStateDeviationPercentage * steadyStateValue;

    double turbulentPeriodLowerLimit = steadyStateValue + turbulentPeriodDeviationLowerPercentage * steadyStateValue;
    double turbulentPeriodUpperLimit = steadyStateValue + turbulentPeriodDeviationHigherPercentage * steadyStateValue;

    for (int i = 0; i < n; i++) {
      if (i >= turbulentPeriodStartIndex && i < (turbulentPeriodStartIndex + turbulentPeriodLength)) {
        series[i] = turbulentPeriodLowerLimit + (turbulentPeriodUpperLimit - turbulentPeriodLowerLimit) * random.nextDouble();
      } else {
        series[i] = valueDevLowerLimit + (valueDevHigherLimit - valueDevLowerLimit) * random.nextDouble();
      }
    }

    return series;
  }

}
