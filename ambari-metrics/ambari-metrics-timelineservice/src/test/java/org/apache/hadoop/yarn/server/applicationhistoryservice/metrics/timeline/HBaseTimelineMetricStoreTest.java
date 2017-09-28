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

import com.google.common.collect.Multimap;
import junit.framework.Assert;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function.ReadFunction.AVG;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function.ReadFunction.SUM;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function.PostProcessingFunction.RATE;
import static org.assertj.core.api.Assertions.assertThat;

public class HBaseTimelineMetricStoreTest {

  public static final String MEM_METRIC = "mem";
  public static final String BYTES_IN_METRIC = "bytes_in";
  public static final String BYTES_NOT_AFUNCTION_METRIC = "bytes._not._afunction";

  @Test
  public void testParseMetricNamesToAggregationFunctions() throws Exception {
    //giwen
    List<String> metricNames = Arrays.asList(
      MEM_METRIC + "._avg",
      MEM_METRIC + "._sum",
      MEM_METRIC + "._rate._avg",
      BYTES_IN_METRIC,
      BYTES_NOT_AFUNCTION_METRIC);

    //when
    Multimap<String, List<Function>> multimap =
      HBaseTimelineMetricStore.parseMetricNamesToAggregationFunctions(metricNames);

    //then
    Assert.assertEquals(multimap.keySet().size(), 3);
    Assert.assertTrue(multimap.containsKey(MEM_METRIC));
    Assert.assertTrue(multimap.containsKey(BYTES_IN_METRIC));
    Assert.assertTrue(multimap.containsKey(BYTES_NOT_AFUNCTION_METRIC));

    List<List<Function>> metricEntry = (List<List<Function>>) multimap.get(MEM_METRIC);
    HashMap<String, List<Function>> mfm = new HashMap<String, List<Function>>();
    mfm.put(MEM_METRIC, metricEntry.get(0));

    assertThat(mfm.get(MEM_METRIC)).containsOnly(
      new Function(AVG, null));

    mfm = new HashMap<String, List<Function>>();
    mfm.put(MEM_METRIC, metricEntry.get(1));
    assertThat(mfm.get(MEM_METRIC)).containsOnly(
      new Function(SUM, null));

    mfm = new HashMap<String, List<Function>>();
    mfm.put(MEM_METRIC, metricEntry.get(2));
    assertThat(mfm.get(MEM_METRIC)).containsOnly(
      new Function(AVG, RATE));

    metricEntry = (List<List<Function>>) multimap.get(BYTES_IN_METRIC);
    mfm = new HashMap<String, List<Function>>();
    mfm.put(BYTES_IN_METRIC, metricEntry.get(0));

    assertThat(mfm.get(BYTES_IN_METRIC))
      .contains(Function.DEFAULT_VALUE_FUNCTION);

    metricEntry = (List<List<Function>>) multimap.get(BYTES_NOT_AFUNCTION_METRIC);
    mfm = new HashMap<String, List<Function>>();
    mfm.put(BYTES_NOT_AFUNCTION_METRIC, metricEntry.get(0));

    assertThat(mfm.get(BYTES_NOT_AFUNCTION_METRIC))
      .contains(Function.DEFAULT_VALUE_FUNCTION);

  }

  @Test
  public void testRateCalculationOnMetricsWithEqualValues() throws Exception {
    Map<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(1454000000000L, 1.0);
    metricValues.put(1454000001000L, 6.0);
    metricValues.put(1454000002000L, 0.0);
    metricValues.put(1454000003000L, 3.0);
    metricValues.put(1454000004000L, 4.0);
    metricValues.put(1454000005000L, 7.0);

    // Calculate rate
    Map<Long, Double> rates = HBaseTimelineMetricStore.updateValuesAsRate(new TreeMap<>(metricValues), false);

    // Make sure rate is zero
    Assert.assertTrue(rates.size() == 4);

    Assert.assertFalse(rates.containsKey(1454000000000L));
    Assert.assertFalse(rates.containsKey(1454000002000L));

    Assert.assertEquals(rates.get(1454000001000L), 5.0);
    Assert.assertEquals(rates.get(1454000003000L), 3.0);
    Assert.assertEquals(rates.get(1454000004000L), 1.0);
    Assert.assertEquals(rates.get(1454000005000L), 3.0);
  }

  @Test
  public void testDiffCalculation() throws Exception {
    Map<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(1454016368371L, 1011.25);
    metricValues.put(1454016428371L, 1010.25);
    metricValues.put(1454016488371L, 1012.25);
    metricValues.put(1454016548371L, 1015.25);
    metricValues.put(1454016608371L, 1020.25);

    Map<Long, Double> rates = HBaseTimelineMetricStore.updateValuesAsRate(new TreeMap<>(metricValues), true);

    Assert.assertTrue(rates.size() == 3);
    Assert.assertTrue(rates.containsValue(2.0));
    Assert.assertTrue(rates.containsValue(3.0));
    Assert.assertTrue(rates.containsValue(5.0));
  }
}
