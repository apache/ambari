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

import org.junit.Assert;
import org.junit.Test;

public class MetricSeriesGeneratorTest {

  @Test
  public void testUniformSeries() {

    UniformMetricSeries metricSeries = new UniformMetricSeries(5, 0.2, 0, 0, 0, true);
    Assert.assertTrue(metricSeries.nextValue() <= 6 && metricSeries.nextValue() >= 4);

    double[] uniformSeries = MetricSeriesGeneratorFactory.createUniformSeries(50, 10, 0.2, 0.1, 0.4, 0.5, true);
    Assert.assertTrue(uniformSeries.length == 50);

    for (int i = 0; i < uniformSeries.length; i++) {
      double value = uniformSeries[i];

      if (value > 10 * 1.2) {
        Assert.assertTrue(value >= 10 * 1.4 && value <= 10 * 1.6);
      } else {
        Assert.assertTrue(value >= 10 * 0.8 && value <= 10 * 1.2);
      }
    }
  }

  @Test
  public void testNormalSeries() {
    NormalMetricSeries metricSeries = new NormalMetricSeries(0, 1, 0, 0, 0, true);
    Assert.assertTrue(metricSeries.nextValue() <= 3 && metricSeries.nextValue() >= -3);
  }

  @Test
  public void testMonotonicSeries() {

    MonotonicMetricSeries metricSeries = new MonotonicMetricSeries(0, 0.5, 0, 0, 0, 0, true);
    Assert.assertTrue(metricSeries.nextValue() == 0);
    Assert.assertTrue(metricSeries.nextValue() == 0.5);

    double[] incSeries = MetricSeriesGeneratorFactory.createMonotonicSeries(20, 0, 0.5, 0, 0, 0, 0, true);
    Assert.assertTrue(incSeries.length == 20);
    for (int i = 0; i < incSeries.length; i++) {
      Assert.assertTrue(incSeries[i] == i * 0.5);
    }
  }

  @Test
  public void testDualBandSeries() {
    double[] dualBandSeries = MetricSeriesGeneratorFactory.getDualBandSeries(30, 5, 0.2, 5, 15, 0.3, 4);
    Assert.assertTrue(dualBandSeries[0] >= 4 && dualBandSeries[0] <= 6);
    Assert.assertTrue(dualBandSeries[4] >= 4 && dualBandSeries[4] <= 6);
    Assert.assertTrue(dualBandSeries[5] >= 10.5 && dualBandSeries[5] <= 19.5);
    Assert.assertTrue(dualBandSeries[8] >= 10.5 && dualBandSeries[8] <= 19.5);
    Assert.assertTrue(dualBandSeries[9] >= 4 && dualBandSeries[9] <= 6);
  }

  @Test
  public void testStepSeries() {
    double[] stepSeries = MetricSeriesGeneratorFactory.getStepFunctionSeries(30, 10, 0, 0, 5, 5, 0.5, true);

    Assert.assertTrue(stepSeries[0] == 10);
    Assert.assertTrue(stepSeries[4] == 10);

    Assert.assertTrue(stepSeries[5] == 10*1.5);
    Assert.assertTrue(stepSeries[9] == 10*1.5);

    Assert.assertTrue(stepSeries[10] == 10*1.5*1.5);
    Assert.assertTrue(stepSeries[14] == 10*1.5*1.5);
  }

  @Test
  public void testSteadySeriesWithTurbulence() {
    double[] steadySeriesWithTurbulence = MetricSeriesGeneratorFactory.getSteadySeriesWithTurbulentPeriod(30, 5, 0, 1, 1, 5, 1);

    int count = 0;
    for (int i = 0; i < steadySeriesWithTurbulence.length; i++) {
      if (steadySeriesWithTurbulence[i] == 10) {
        count++;
      }
    }
    Assert.assertTrue(count == 5);
  }
}
