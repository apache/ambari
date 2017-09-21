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

public class StepFunctionMetricSeries implements AbstractMetricSeries {

  double startValue = 0.0;
  double steadyValueDeviationPercentage = 0.0;
  double steadyPeriodSlope = 0.5;
  int steadyPeriodMinSize = 10;
  int steadyPeriodMaxSize = 20;
  double stepChangePercentage = 0.0;
  boolean upwardStep = true;

  Random random = new Random();

  // y = mx + c
  double y;
  double m;
  double x;
  double c;
  int currentStepSize;
  int currentIndex;

  public StepFunctionMetricSeries(double startValue,
                                  double steadyValueDeviationPercentage,
                                  double steadyPeriodSlope,
                                  int steadyPeriodMinSize,
                                  int steadyPeriodMaxSize,
                                  double stepChangePercentage,
                                  boolean upwardStep) {
    this.startValue = startValue;
    this.steadyValueDeviationPercentage = steadyValueDeviationPercentage;
    this.steadyPeriodSlope = steadyPeriodSlope;
    this.steadyPeriodMinSize = steadyPeriodMinSize;
    this.steadyPeriodMaxSize = steadyPeriodMaxSize;
    this.stepChangePercentage = stepChangePercentage;
    this.upwardStep = upwardStep;
    init();
  }

  private void init() {
    y = startValue;
    m = steadyPeriodSlope;
    x = 1;
    c = y - (m * x);

    currentStepSize = (int) (steadyPeriodMinSize + (steadyPeriodMaxSize - steadyPeriodMinSize) * random.nextDouble());
    currentIndex = 0;
  }

  @Override
  public double nextValue() {

    double value = 0.0;

    if (currentIndex < currentStepSize) {
      y = m * x + c;
      double valueDeviationLowerLimit = y - steadyValueDeviationPercentage * y;
      double valueDeviationHigherLimit = y + steadyValueDeviationPercentage * y;
      value = valueDeviationLowerLimit + (valueDeviationHigherLimit - valueDeviationLowerLimit) * random.nextDouble();
      x++;
      currentIndex++;
    }

    if (currentIndex == currentStepSize) {
      currentIndex = 0;
      currentStepSize = (int) (steadyPeriodMinSize + (steadyPeriodMaxSize - steadyPeriodMinSize) * random.nextDouble());
      if (upwardStep) {
        y = y + stepChangePercentage * y;
      } else {
        y = y - stepChangePercentage * y;
      }
      x = 1;
      c = y - (m * x);
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
