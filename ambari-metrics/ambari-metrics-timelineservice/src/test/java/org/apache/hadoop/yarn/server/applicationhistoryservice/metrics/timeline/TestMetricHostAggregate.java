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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline;

import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricHostAggregate;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMetricHostAggregate {

  @Test
  public void testCreateAggregate() throws Exception {
    // given
    MetricHostAggregate aggregate = createAggregate(3.0, 1.0, 2.0, 2);

    //then
    assertThat(aggregate.getSum()).isEqualTo(3.0);
    assertThat(aggregate.getMin()).isEqualTo(1.0);
    assertThat(aggregate.getMax()).isEqualTo(2.0);
    assertThat(aggregate.getAvg()).isEqualTo(3.0 / 2);
  }

  @Test
  public void testUpdateAggregates() throws Exception {
    // given
    MetricHostAggregate aggregate = createAggregate(3.0, 1.0, 2.0, 2);

    //when
    aggregate.updateAggregates(createAggregate(8.0, 0.5, 7.5, 2));
    aggregate.updateAggregates(createAggregate(1.0, 1.0, 1.0, 1));

    //then
    assertThat(aggregate.getSum()).isEqualTo(12.0);
    assertThat(aggregate.getMin()).isEqualTo(0.5);
    assertThat(aggregate.getMax()).isEqualTo(7.5);
    assertThat(aggregate.getAvg()).isEqualTo((3.0 + 8.0 + 1.0) / 5);
  }

  static MetricHostAggregate createAggregate (Double sum, Double min,
                                              Double max, Integer samplesCount) {
    MetricHostAggregate aggregate = new MetricHostAggregate();
    aggregate.setSum(sum);
    aggregate.setMax(max);
    aggregate.setMin(min);
    aggregate.setDeviation(0.0);
    aggregate.setNumberOfSamples(samplesCount);
    return aggregate;
  }
}