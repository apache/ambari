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

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

public class MetricSeriesGeneratorFactory {

  /**
   * Return a normally distributed data series with some deviation % and outliers.
   *
   * @param n                                size of the data series
   * @param value                            The value around which the uniform data series is centered on.
   * @param deviationPercentage              The allowed deviation % on either side of the uniform value. For example, if value = 10, and deviation % is 0.1, the series values lie between 0.9 to 1.1.
   * @param outlierProbability               The probability of finding an outlier in the series.
   * @param outlierDeviationLowerPercentage  min percentage outlier should be away from the uniform value in % terms. if value = 10 and outlierDeviationPercentage = 30%, the outlier is 7 and  13.
   * @param outlierDeviationHigherPercentage max percentage outlier should be away from the uniform value in % terms. if value = 10 and outlierDeviationPercentage = 60%, the outlier is 4 and  16.
   * @param outliersAboveValue               Outlier should be greater or smaller than the value.
   * @return uniform series
   */
  public static double[] createUniformSeries(int n,
                                             double value,
                                             double deviationPercentage,
                                             double outlierProbability,
                                             double outlierDeviationLowerPercentage,
                                             double outlierDeviationHigherPercentage,
                                             boolean outliersAboveValue) {

    UniformMetricSeries metricSeries = new UniformMetricSeries(value,
      deviationPercentage,
      outlierProbability,
      outlierDeviationLowerPercentage,
      outlierDeviationHigherPercentage,
      outliersAboveValue);

    return metricSeries.getSeries(n);
  }


  /**
   * /**
   * Returns a normally distributed series.
   *
   * @param n                             size of the data series
   * @param mean                          mean of the distribution
   * @param sd                            sd of the distribution
   * @param outlierProbability            sd of the distribution
   * @param outlierDeviationSDTimesLower  Lower Limit of the outlier with respect to times sdev from the mean.
   * @param outlierDeviationSDTimesHigher Higher Limit of the outlier with respect to times sdev from the mean.
   * @param outlierOnRightEnd             Outlier should be on the right end or the left end.
   * @return normal series
   */
  public static double[] createNormalSeries(int n,
                                            double mean,
                                            double sd,
                                            double outlierProbability,
                                            double outlierDeviationSDTimesLower,
                                            double outlierDeviationSDTimesHigher,
                                            boolean outlierOnRightEnd) {


    NormalMetricSeries metricSeries = new NormalMetricSeries(mean,
      sd,
      outlierProbability,
      outlierDeviationSDTimesLower,
      outlierDeviationSDTimesHigher,
      outlierOnRightEnd);

    return metricSeries.getSeries(n);
  }


  /**
   * Returns a monotonically increasing / decreasing series
   *
   * @param n                                size of the data series
   * @param startValue                       Start value of the monotonic sequence
   * @param slope                            direction of monotonicity m > 0 for increasing and m < 0 for decreasing.
   * @param deviationPercentage              The allowed deviation % on either side of the current 'y' value. For example, if current value = 10 according to slope, and deviation % is 0.1, the series values lie between 0.9 to 1.1.
   * @param outlierProbability               The probability of finding an outlier in the series.
   * @param outlierDeviationLowerPercentage  min percentage outlier should be away from the current 'y' value in % terms. if value = 10 and outlierDeviationPercentage = 30%, the outlier is 7 and  13.
   * @param outlierDeviationHigherPercentage max percentage outlier should be away from the current 'y' value in % terms. if value = 10 and outlierDeviationPercentage = 60%, the outlier is 4 and  16.
   * @param outliersAboveValue               Outlier should be greater or smaller than the 'y' value.
   * @return
   */
  public static double[] createMonotonicSeries(int n,
                                               double startValue,
                                               double slope,
                                               double deviationPercentage,
                                               double outlierProbability,
                                               double outlierDeviationLowerPercentage,
                                               double outlierDeviationHigherPercentage,
                                               boolean outliersAboveValue) {

    MonotonicMetricSeries metricSeries = new MonotonicMetricSeries(startValue,
      slope,
      deviationPercentage,
      outlierProbability,
      outlierDeviationLowerPercentage,
      outlierDeviationHigherPercentage,
      outliersAboveValue);

    return metricSeries.getSeries(n);
  }


  /**
   * Returns a dual band series (lower and higher)
   *
   * @param n                           size of the data series
   * @param lowBandValue                lower band value
   * @param lowBandDeviationPercentage  lower band deviation
   * @param lowBandPeriodSize           lower band
   * @param highBandValue               high band centre value
   * @param highBandDeviationPercentage high band deviation.
   * @param highBandPeriodSize          high band size
   * @return
   */
  public static double[] getDualBandSeries(int n,
                                           double lowBandValue,
                                           double lowBandDeviationPercentage,
                                           int lowBandPeriodSize,
                                           double highBandValue,
                                           double highBandDeviationPercentage,
                                           int highBandPeriodSize) {

    DualBandMetricSeries metricSeries  = new DualBandMetricSeries(lowBandValue,
      lowBandDeviationPercentage,
      lowBandPeriodSize,
      highBandValue,
      highBandDeviationPercentage,
      highBandPeriodSize);

    return metricSeries.getSeries(n);
  }

  /**
   * Returns a step function series.
   *
   * @param n                              size of the data series
   * @param startValue                     start steady value
   * @param steadyValueDeviationPercentage required devation in the steady state value
   * @param steadyPeriodSlope              direction of monotonicity m > 0 for increasing and m < 0 for decreasing, m = 0 no increase or decrease.
   * @param steadyPeriodMinSize            min size for step period
   * @param steadyPeriodMaxSize            max size for step period.
   * @param stepChangePercentage           Increase / decrease in steady state to denote a step in terms of deviation percentage from the last value.
   * @param upwardStep                     upward or downward step.
   * @return
   */
  public static double[] getStepFunctionSeries(int n,
                                               double startValue,
                                               double steadyValueDeviationPercentage,
                                               double steadyPeriodSlope,
                                               int steadyPeriodMinSize,
                                               int steadyPeriodMaxSize,
                                               double stepChangePercentage,
                                               boolean upwardStep) {

    StepFunctionMetricSeries metricSeries = new StepFunctionMetricSeries(startValue,
      steadyValueDeviationPercentage,
      steadyPeriodSlope,
      steadyPeriodMinSize,
      steadyPeriodMaxSize,
      stepChangePercentage,
      upwardStep);

    return metricSeries.getSeries(n);
  }

  /**
   * Series with small period of turbulence and then back to steady.
   *
   * @param n                                        size of the data series
   * @param steadyStateValue                         steady state center value
   * @param steadyStateDeviationPercentage           steady state deviation in percentage
   * @param turbulentPeriodDeviationLowerPercentage  turbulent state lower limit in terms of percentage from centre value.
   * @param turbulentPeriodDeviationHigherPercentage turbulent state higher limit in terms of percentage from centre value.
   * @param turbulentPeriodLength                    turbulent period length (number of points)
   * @param turbulentStatePosition                   Where the turbulent state should be 0 - at the beginning, 1 - in the middle (25% - 50% of the series), 2 - at the end of the series.
   * @return
   */
  public static double[] getSteadySeriesWithTurbulentPeriod(int n,
                                                            double steadyStateValue,
                                                            double steadyStateDeviationPercentage,
                                                            double turbulentPeriodDeviationLowerPercentage,
                                                            double turbulentPeriodDeviationHigherPercentage,
                                                            int turbulentPeriodLength,
                                                            int turbulentStatePosition
  ) {


    SteadyWithTurbulenceMetricSeries metricSeries = new SteadyWithTurbulenceMetricSeries(n,
      steadyStateValue,
      steadyStateDeviationPercentage,
      turbulentPeriodDeviationLowerPercentage,
      turbulentPeriodDeviationHigherPercentage,
      turbulentPeriodLength,
      turbulentStatePosition);

    return metricSeries.getSeries(n);
  }


  public static double[] generateSeries(String type, int n, Map<String, String> configs) {

    double[] series;
    switch (type) {

      case "normal":
        series = createNormalSeries(n,
          Double.parseDouble(configs.getOrDefault("mean", "0")),
          Double.parseDouble(configs.getOrDefault("sd", "1")),
          Double.parseDouble(configs.getOrDefault("outlierProbability", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationSDTimesLower", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationSDTimesHigher", "0")),
          Boolean.parseBoolean(configs.getOrDefault("outlierOnRightEnd", "true")));
        break;

      case "uniform":
        series = createUniformSeries(n,
          Double.parseDouble(configs.getOrDefault("value", "10")),
          Double.parseDouble(configs.getOrDefault("deviationPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("outlierProbability", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationLowerPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationHigherPercentage", "0")),
          Boolean.parseBoolean(configs.getOrDefault("outliersAboveValue", "true")));
        break;

      case "monotonic":
        series = createMonotonicSeries(n,
          Double.parseDouble(configs.getOrDefault("startValue", "10")),
          Double.parseDouble(configs.getOrDefault("slope", "0")),
          Double.parseDouble(configs.getOrDefault("deviationPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("outlierProbability", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationLowerPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationHigherPercentage", "0")),
          Boolean.parseBoolean(configs.getOrDefault("outliersAboveValue", "true")));
        break;

      case "dualband":
        series = getDualBandSeries(n,
          Double.parseDouble(configs.getOrDefault("lowBandValue", "10")),
          Double.parseDouble(configs.getOrDefault("lowBandDeviationPercentage", "0")),
          Integer.parseInt(configs.getOrDefault("lowBandPeriodSize", "0")),
          Double.parseDouble(configs.getOrDefault("highBandValue", "10")),
          Double.parseDouble(configs.getOrDefault("highBandDeviationPercentage", "0")),
          Integer.parseInt(configs.getOrDefault("highBandPeriodSize", "0")));
        break;

      case "step":
        series = getStepFunctionSeries(n,
          Double.parseDouble(configs.getOrDefault("startValue", "10")),
          Double.parseDouble(configs.getOrDefault("steadyValueDeviationPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("steadyPeriodSlope", "0")),
          Integer.parseInt(configs.getOrDefault("steadyPeriodMinSize", "0")),
          Integer.parseInt(configs.getOrDefault("steadyPeriodMaxSize", "0")),
          Double.parseDouble(configs.getOrDefault("stepChangePercentage", "0")),
          Boolean.parseBoolean(configs.getOrDefault("upwardStep", "true")));
        break;

      case "turbulence":
        series = getSteadySeriesWithTurbulentPeriod(n,
          Double.parseDouble(configs.getOrDefault("steadyStateValue", "10")),
          Double.parseDouble(configs.getOrDefault("steadyStateDeviationPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("turbulentPeriodDeviationLowerPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("turbulentPeriodDeviationHigherPercentage", "10")),
          Integer.parseInt(configs.getOrDefault("turbulentPeriodLength", "0")),
          Integer.parseInt(configs.getOrDefault("turbulentStatePosition", "0")));
        break;

      default:
        series = createNormalSeries(n,
          0,
          1,
          0,
          0,
          0,
          true);
    }
    return series;
  }

  public static AbstractMetricSeries generateSeries(String type, Map<String, String> configs) {

    AbstractMetricSeries series;
    switch (type) {

      case "normal":
        series = new NormalMetricSeries(Double.parseDouble(configs.getOrDefault("mean", "0")),
          Double.parseDouble(configs.getOrDefault("sd", "1")),
          Double.parseDouble(configs.getOrDefault("outlierProbability", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationSDTimesLower", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationSDTimesHigher", "0")),
          Boolean.parseBoolean(configs.getOrDefault("outlierOnRightEnd", "true")));
        break;

      case "uniform":
        series = new UniformMetricSeries(
          Double.parseDouble(configs.getOrDefault("value", "10")),
          Double.parseDouble(configs.getOrDefault("deviationPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("outlierProbability", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationLowerPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationHigherPercentage", "0")),
          Boolean.parseBoolean(configs.getOrDefault("outliersAboveValue", "true")));
        break;

      case "monotonic":
        series = new MonotonicMetricSeries(
          Double.parseDouble(configs.getOrDefault("startValue", "10")),
          Double.parseDouble(configs.getOrDefault("slope", "0")),
          Double.parseDouble(configs.getOrDefault("deviationPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("outlierProbability", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationLowerPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("outlierDeviationHigherPercentage", "0")),
          Boolean.parseBoolean(configs.getOrDefault("outliersAboveValue", "true")));
        break;

      case "dualband":
        series = new DualBandMetricSeries(
          Double.parseDouble(configs.getOrDefault("lowBandValue", "10")),
          Double.parseDouble(configs.getOrDefault("lowBandDeviationPercentage", "0")),
          Integer.parseInt(configs.getOrDefault("lowBandPeriodSize", "0")),
          Double.parseDouble(configs.getOrDefault("highBandValue", "10")),
          Double.parseDouble(configs.getOrDefault("highBandDeviationPercentage", "0")),
          Integer.parseInt(configs.getOrDefault("highBandPeriodSize", "0")));
        break;

      case "step":
        series = new StepFunctionMetricSeries(
          Double.parseDouble(configs.getOrDefault("startValue", "10")),
          Double.parseDouble(configs.getOrDefault("steadyValueDeviationPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("steadyPeriodSlope", "0")),
          Integer.parseInt(configs.getOrDefault("steadyPeriodMinSize", "0")),
          Integer.parseInt(configs.getOrDefault("steadyPeriodMaxSize", "0")),
          Double.parseDouble(configs.getOrDefault("stepChangePercentage", "0")),
          Boolean.parseBoolean(configs.getOrDefault("upwardStep", "true")));
        break;

      case "turbulence":
        series = new SteadyWithTurbulenceMetricSeries(
          Integer.parseInt(configs.getOrDefault("approxSeriesLength", "100")),
          Double.parseDouble(configs.getOrDefault("steadyStateValue", "10")),
          Double.parseDouble(configs.getOrDefault("steadyStateDeviationPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("turbulentPeriodDeviationLowerPercentage", "0")),
          Double.parseDouble(configs.getOrDefault("turbulentPeriodDeviationHigherPercentage", "10")),
          Integer.parseInt(configs.getOrDefault("turbulentPeriodLength", "0")),
          Integer.parseInt(configs.getOrDefault("turbulentStatePosition", "0")));
        break;

      default:
        series = new NormalMetricSeries(0,
          1,
          0,
          0,
          0,
          true);
    }
    return series;
  }

}
