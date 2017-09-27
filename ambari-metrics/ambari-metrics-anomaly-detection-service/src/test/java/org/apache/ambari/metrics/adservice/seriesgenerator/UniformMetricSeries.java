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
package org.apache.ambari.metrics.adservice.seriesgenerator;

import java.util.Random;

public class UniformMetricSeries implements AbstractMetricSeries {

  double value = 0.0;
  double deviationPercentage = 0.0;
  double outlierProbability = 0.0;
  double outlierDeviationLowerPercentage = 0.0;
  double outlierDeviationHigherPercentage = 0.0;
  boolean outliersAboveValue= true;

  Random random = new Random();
  double valueDeviationLowerLimit;
  double valueDeviationHigherLimit;
  double outlierLeftLowerLimit;
  double outlierLeftHigherLimit;
  double outlierRightLowerLimit;
  double outlierRightUpperLimit;
  double nonOutlierProbability;


  public UniformMetricSeries(double value,
                             double deviationPercentage,
                             double outlierProbability,
                             double outlierDeviationLowerPercentage,
                             double outlierDeviationHigherPercentage,
                             boolean outliersAboveValue) {
    this.value = value;
    this.deviationPercentage = deviationPercentage;
    this.outlierProbability = outlierProbability;
    this.outlierDeviationLowerPercentage = outlierDeviationLowerPercentage;
    this.outlierDeviationHigherPercentage = outlierDeviationHigherPercentage;
    this.outliersAboveValue = outliersAboveValue;
    init();
  }

  private void init() {
    valueDeviationLowerLimit = value - deviationPercentage * value;
    valueDeviationHigherLimit = value + deviationPercentage * value;

    outlierLeftLowerLimit = value - outlierDeviationHigherPercentage * value;
    outlierLeftHigherLimit = value - outlierDeviationLowerPercentage * value;
    outlierRightLowerLimit = value + outlierDeviationLowerPercentage * value;
    outlierRightUpperLimit = value + outlierDeviationHigherPercentage * value;

    nonOutlierProbability = 1.0 - outlierProbability;
  }

  @Override
  public double nextValue() {

    double value;
    double probability = random.nextDouble();

    if (probability <= nonOutlierProbability) {
      value = valueDeviationLowerLimit + (valueDeviationHigherLimit - valueDeviationLowerLimit) * random.nextDouble();
    } else {
      if (!outliersAboveValue) {
        value = outlierLeftLowerLimit + (outlierLeftHigherLimit - outlierLeftLowerLimit) * random.nextDouble();
      } else {
        value = outlierRightLowerLimit + (outlierRightUpperLimit - outlierRightLowerLimit) * random.nextDouble();
      }
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
