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

public class NormalMetricSeries implements AbstractMetricSeries {

  double mean = 0.0;
  double sd = 1.0;
  double outlierProbability = 0.0;
  double outlierDeviationSDTimesLower = 0.0;
  double outlierDeviationSDTimesHigher = 0.0;
  boolean outlierOnRightEnd = true;

  Random random = new Random();
  double nonOutlierProbability;


  public NormalMetricSeries(double mean,
                            double sd,
                            double outlierProbability,
                            double outlierDeviationSDTimesLower,
                            double outlierDeviationSDTimesHigher,
                            boolean outlierOnRightEnd) {
    this.mean = mean;
    this.sd = sd;
    this.outlierProbability = outlierProbability;
    this.outlierDeviationSDTimesLower = outlierDeviationSDTimesLower;
    this.outlierDeviationSDTimesHigher = outlierDeviationSDTimesHigher;
    this.outlierOnRightEnd = outlierOnRightEnd;
    init();
  }

  private void init() {
    nonOutlierProbability = 1.0 - outlierProbability;
  }

  @Override
  public double nextValue() {

    double value;
    double probability = random.nextDouble();

    if (probability <= nonOutlierProbability) {
      value = random.nextGaussian() * sd + mean;
    } else {
      if (outlierOnRightEnd) {
        value = mean + (outlierDeviationSDTimesLower + (outlierDeviationSDTimesHigher - outlierDeviationSDTimesLower) * random.nextDouble()) * sd;
      } else {
        value = mean - (outlierDeviationSDTimesLower + (outlierDeviationSDTimesHigher - outlierDeviationSDTimesLower) * random.nextDouble()) * sd;
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
