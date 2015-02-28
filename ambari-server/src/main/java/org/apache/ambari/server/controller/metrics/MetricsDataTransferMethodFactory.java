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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MetricsDataTransferMethodFactory {
  private static final Set<String> PERCENTAGE_METRIC;

  static {
    Set<String> temp = new HashSet<String>();
    temp.add("cpu_wio");
    temp.add("cpu_idle");
    temp.add("cpu_nice");
    temp.add("cpu_aidle");
    temp.add("cpu_system");
    temp.add("cpu_user");
    PERCENTAGE_METRIC = Collections.unmodifiableSet(temp);
  }

  private static final MetricsDataTransferMethod percentageAdjustment = new PercentageAdjustmentTransferMethod();
  private static final MetricsDataTransferMethod passThrough = new PassThroughTransferMethod();

  public static MetricsDataTransferMethod detectDataTransferMethod(TimelineMetric metricDecl) {
    if (PERCENTAGE_METRIC.contains(metricDecl.getMetricName())) {
      return percentageAdjustment;
    } else {
      return passThrough;
    }
  }
}

class PercentageAdjustmentTransferMethod extends MetricsDataTransferMethod {

  @Override
  public Double getData(Double data) {
    return new Double(data / 100);
  }
}

class PassThroughTransferMethod extends MetricsDataTransferMethod {

  @Override
  public Double getData(Double data) {
    return data;
  }
}
