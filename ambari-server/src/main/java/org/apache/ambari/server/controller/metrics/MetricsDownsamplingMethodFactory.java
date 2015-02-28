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
package org.apache.ambari.server.controller.metrics;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class MetricsDownsamplingMethodFactory {
  private static final MetricsDownsamplingMethod perSecondDownsampling = new MetricsAveragePerSecondDownsampling();
  private static final MetricsDownsamplingMethod noDownsampling = new MetricNoDownsampling();

  public static MetricsDownsamplingMethod detectDownsamplingMethod(TimelineMetric metricDecl) {
    if (mustDownsample(metricDecl)) {
      return perSecondDownsampling;
    } else {
      return noDownsampling;
    }
  }

  private static boolean mustDownsample(TimelineMetric metric) {
    //Linear search in Map<K, V>, faster than copying the map in a TreeMap and doing ceilingEntry()
    for (Long time : metric.getMetricValues().keySet()) {
      if (time > 9999999999l)
        return true;
    }
    return false;
  }
}

class MetricNoDownsampling extends MetricsDownsamplingMethod {
  @Override
  public Number[][] reportMetricData(TimelineMetric metricData, MetricsDataTransferMethod dataTransferMethod) {
    Number[][] datapointsArray = new Number[metricData.getMetricValues().size()][2];
    int cnt = 0;

    for (Map.Entry<Long, Double> metricEntry : metricData.getMetricValues().entrySet()) {
      datapointsArray[cnt][0] = dataTransferMethod.getData(metricEntry.getValue());
      datapointsArray[cnt][1] = metricEntry.getKey();
      cnt++;
    }

    return datapointsArray;
  }
}

class MetricsAveragePerSecondDownsampling extends MetricsDownsamplingMethod {
  class Accumulo {
    public long ts;
    public double val;

    public Accumulo(long t, double v) {
      this.ts = t;
      this.val = v;
    }
  }
  @Override
  public Number[][] reportMetricData(TimelineMetric metricData, MetricsDataTransferMethod dataTransferMethod) {
    ArrayList<Accumulo> cache = new ArrayList<Accumulo>();

    final Iterator<Map.Entry<Long, Double>> ci = metricData.getMetricValues().entrySet().iterator();
    if (ci.hasNext()) {
      Map.Entry<Long, Double> e0 = ci.next();
      long t0 = e0.getKey() / 1000;
      double s0 = e0.getValue();
      int nSamples = 1;

      while(ci.hasNext()) {
        e0 = ci.next();
        long t = e0.getKey() / 1000;

        if (t != t0) {
          cache.add(new Accumulo(t0, dataTransferMethod.getData(s0 / nSamples)));
          t0 = t;
          s0 = e0.getValue();
          nSamples = 1;
        } else {
          s0 += e0.getValue();
          nSamples++;
        }
      }

      //Add the last entry into the cache
      cache.add(new Accumulo(t0, dataTransferMethod.getData(s0 / nSamples)));
    }

    Number[][] datapointsArray = new Number[cache.size()][2];
    int cnt = 0;
    for (Accumulo e : cache) {
      datapointsArray[cnt][0] = e.val;
      datapointsArray[cnt][1] = e.ts;
      cnt++;
    }

    return datapointsArray;
  }
}
