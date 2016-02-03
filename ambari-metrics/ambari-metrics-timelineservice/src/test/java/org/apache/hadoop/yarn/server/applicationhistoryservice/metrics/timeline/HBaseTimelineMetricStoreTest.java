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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline;

import junit.framework.Assert;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function.ReadFunction.AVG;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function.PostProcessingFunction.RATE;
import static org.assertj.core.api.Assertions.*;

public class HBaseTimelineMetricStoreTest {

  public static final String MEM_METRIC = "mem";
  public static final String BYTES_IN_METRIC = "bytes_in";
  public static final String BYTES_NOT_AFUNCTION_METRIC = "bytes._not._afunction";

  @Test
  public void testParseMetricNamesToAggregationFunctions() throws Exception {
    //giwen
    List<String> metricNames = Arrays.asList(
      MEM_METRIC + "._avg",
      MEM_METRIC + "._rate._avg",
      BYTES_IN_METRIC,
      BYTES_NOT_AFUNCTION_METRIC);

    //when
    HashMap<String, List<Function>> mfm =
      HBaseTimelineMetricStore.parseMetricNamesToAggregationFunctions(metricNames);

    //then
    assertThat(mfm).hasSize(3)
      .containsKeys(MEM_METRIC, BYTES_IN_METRIC, BYTES_NOT_AFUNCTION_METRIC);

    assertThat(mfm.get(MEM_METRIC)).containsOnly(
      new Function(AVG, null),
      new Function(AVG, RATE));

    assertThat(mfm.get(BYTES_IN_METRIC))
      .contains(Function.DEFAULT_VALUE_FUNCTION);

    assertThat(mfm.get(BYTES_NOT_AFUNCTION_METRIC))
      .contains(Function.DEFAULT_VALUE_FUNCTION);

  }

  @Test
  public void testRateCalculationOnMetricsWithEqualValues() throws Exception {
    Map<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(1454016368371L, 1011.25);
    metricValues.put(1454016428371L, 1011.25);
    metricValues.put(1454016488371L, 1011.25);
    metricValues.put(1454016548371L, 1011.25);
    metricValues.put(1454016608371L, 1011.25);
    metricValues.put(1454016668371L, 1011.25);
    metricValues.put(1454016728371L, 1011.25);

    // Calculate rate
    Map<Long, Double> rates = HBaseTimelineMetricStore.updateValuesAsRate(new TreeMap<>(metricValues));

    // Make sure rate is zero
    for (Map.Entry<Long, Double> rateEntry : rates.entrySet()) {
      Assert.assertEquals("Rate should be zero, key = " + rateEntry.getKey()
          + ", value = " + rateEntry.getValue(), 0.0, rateEntry.getValue());
    }
  }
}
