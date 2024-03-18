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

package org.apache.ambari.metrics.core.timeline;

import junit.framework.Assert;
import org.apache.ambari.metrics.core.timeline.query.TopNCondition;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TopNConditionTest {


  @Test
  public void testTopNCondition() {

    List<String> metricNames = new ArrayList<>();
    List<String> hostnames = new ArrayList<>();

    //Valid Cases

    // "H" hosts and 1 Metric
    hostnames.add("h1");
    hostnames.add("h2");
    metricNames.add("m1");
    Assert.assertTrue(TopNCondition.isTopNHostCondition(metricNames, hostnames));
    Assert.assertFalse(TopNCondition.isTopNMetricCondition(metricNames, hostnames));
    hostnames.clear();

    // Host(s) with wildcard & 1 Metric
    hostnames.add("h%");
    hostnames.add("g1");
    Assert.assertTrue(TopNCondition.isTopNHostCondition(metricNames, hostnames));
    Assert.assertFalse(TopNCondition.isTopNMetricCondition(metricNames, hostnames));
    hostnames.clear();

    // M Metrics and No host
    metricNames.add("m2");
    metricNames.add("m3");
    Assert.assertFalse(TopNCondition.isTopNHostCondition(metricNames, hostnames));
    Assert.assertTrue(TopNCondition.isTopNMetricCondition(metricNames, hostnames));

    // M Metrics with wildcard and No host
    metricNames.add("m2");
    metricNames.add("m%");
    Assert.assertFalse(TopNCondition.isTopNHostCondition(metricNames, hostnames));
    Assert.assertTrue(TopNCondition.isTopNMetricCondition(metricNames, hostnames));

    // M Metrics with wildcard and 1 host
    metricNames.add("m2");
    metricNames.add("m%");
    hostnames.add("h1");
    Assert.assertFalse(TopNCondition.isTopNHostCondition(metricNames, hostnames));
    Assert.assertTrue(TopNCondition.isTopNMetricCondition(metricNames, hostnames));

    metricNames.clear();
    hostnames.clear();

    //Invalid Cases
    // M metrics and H hosts
    metricNames.add("m1");
    metricNames.add("m2");
    hostnames.add("h1");
    hostnames.add("h2");
    Assert.assertFalse(TopNCondition.isTopNHostCondition(metricNames, hostnames));
    Assert.assertFalse(TopNCondition.isTopNMetricCondition(metricNames, hostnames));

    metricNames.clear();
    hostnames.clear();

    // Wildcard in 1 and multiple in other
    metricNames.add("m1");
    metricNames.add("m2");
    hostnames.add("%");
    Assert.assertFalse(TopNCondition.isTopNHostCondition(metricNames, hostnames));
    Assert.assertFalse(TopNCondition.isTopNMetricCondition(metricNames, hostnames));

    metricNames.clear();
    hostnames.clear();

    //Wildcard in both
    metricNames.add("m%");
    hostnames.add("%");
    Assert.assertFalse(TopNCondition.isTopNHostCondition(metricNames, hostnames));
    Assert.assertFalse(TopNCondition.isTopNMetricCondition(metricNames, hostnames));

  }
}
