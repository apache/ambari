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

package org.apache.hadoop.metrics2.sink.kafka;

import java.util.Map;
import org.junit.Test;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricName;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class JvmMetricSetTest {

  @Test
  public void testGetJvmMetrics() {

    Map<MetricName, Gauge<?>> result = JvmMetricSet.getInstance().getJvmMetrics();

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertThat(
      result.keySet()
        .stream()
        .map(MetricName::getName)
        .collect(toList()),
      hasItems("heap_usage", "thread-states.blocked", "thread-states.timed_waiting", "uptime"));
  }

}