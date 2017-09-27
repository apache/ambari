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

public class MonotonicMetricSeries implements AbstractMetricSeries {

  double startValue = 0.0;
  double slope = 0.5;
  double deviationPercentage = 0.0;
  double outlierProbability = 0.0;
  double outlierDeviationLowerPercentage = 0.0;
  double outlierDeviationHigherPercentage = 0.0;
  boolean outliersAboveValue = true;

  Random random = new Random();
  double nonOutlierProbability;

  // y = mx + c
  double y;
  double m;
  double x;
  double c;

  public MonotonicMetricSeries(double startValue,
                               double slope,
                               double deviationPercentage,
                               double outlierProbability,
                               double outlierDeviationLowerPercentage,
                               double outlierDeviationHigherPercentage,
                               boolean outliersAboveValue) {
    this.startValue = startValue;
    this.slope = slope;
    this.deviationPercentage = deviationPercentage;
    this.outlierProbability = outlierProbability;
    this.outlierDeviationLowerPercentage = outlierDeviationLowerPercentage;
    this.outlierDeviationHigherPercentage = outlierDeviationHigherPercentage;
    this.outliersAboveValue = outliersAboveValue;
    init();
  }

  private void init() {
    y = startValue;
    m = slope;
    x = 1;
    c = y - (m * x);
    nonOutlierProbability = 1.0 - outlierProbability;
  }

  @Override
  public double nextValue() {

    double value;
    double probability = random.nextDouble();

    y = m * x + c;
    if (probability <= nonOutlierProbability) {
      double valueDeviationLowerLimit = y - deviationPercentage * y;
      double valueDeviationHigherLimit = y + deviationPercentage * y;
      value = valueDeviationLowerLimit + (valueDeviationHigherLimit - valueDeviationLowerLimit) * random.nextDouble();
    } else {
      if (outliersAboveValue) {
        double outlierLowerLimit = y + outlierDeviationLowerPercentage * y;
        double outlierUpperLimit = y + outlierDeviationHigherPercentage * y;
        value = outlierLowerLimit + (outlierUpperLimit - outlierLowerLimit) * random.nextDouble();
      } else {
        double outlierLowerLimit = y - outlierDeviationLowerPercentage * y;
        double outlierUpperLimit = y - outlierDeviationHigherPercentage * y;
        value = outlierUpperLimit + (outlierLowerLimit - outlierUpperLimit) * random.nextDouble();
      }
    }
    x++;
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
