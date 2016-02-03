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

import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function.fromMetricName;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function.ReadFunction.AVG;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function.PostProcessingFunction.RATE;
import static org.assertj.core.api.Assertions.assertThat;

public class FunctionTest {

  @Test
  public void testCreation() throws Exception {
    Function f = fromMetricName("Metric._avg");
    assertThat(f).isEqualTo(new Function(AVG, null));

    f = fromMetricName("Metric._rate._avg");
    assertThat(f).isEqualTo(new Function(AVG, RATE));

    f = fromMetricName("bytes_in");
    assertThat(f).isEqualTo(Function.DEFAULT_VALUE_FUNCTION);

    // Rate support without aggregates
    f = fromMetricName("Metric._rate");
    assertThat(f).isEqualTo(new Function(null, RATE));
  }

  @Ignore // If unknown function: behavior is best effort query without function
  @Test(expected = Function.FunctionFormatException.class)
  public void testNotAFunction() throws Exception {
    fromMetricName("bytes._not._afunction");
  }
}
