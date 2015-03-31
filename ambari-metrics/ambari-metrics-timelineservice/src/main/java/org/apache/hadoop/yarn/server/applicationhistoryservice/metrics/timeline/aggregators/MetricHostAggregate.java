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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;


import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents a collection of minute based aggregation of values for
 * resolution greater than a minute.
 */
public class MetricHostAggregate extends MetricAggregate {

  private long numberOfSamples = 0;

  @JsonCreator
  public MetricHostAggregate() {
    super(0.0, 0.0, Double.MIN_VALUE, Double.MAX_VALUE);
  }

  public MetricHostAggregate(Double sum, int numberOfSamples,
                             Double deviation,
                             Double max, Double min) {
    super(sum, deviation, max, min);
    this.numberOfSamples = numberOfSamples;
  }

  @JsonProperty("numberOfSamples")
  public long getNumberOfSamples() {
    return numberOfSamples == 0 ? 1 : numberOfSamples;
  }

  public void updateNumberOfSamples(long count) {
    this.numberOfSamples += count;
  }

  public void setNumberOfSamples(long numberOfSamples) {
    this.numberOfSamples = numberOfSamples;
  }

  public double getAvg() {
    return sum / numberOfSamples;
  }

  /**
   * Find and update min, max and avg for a minute
   */
  public void updateAggregates(MetricHostAggregate hostAggregate) {
    updateMax(hostAggregate.getMax());
    updateMin(hostAggregate.getMin());
    updateSum(hostAggregate.getSum());
    updateNumberOfSamples(hostAggregate.getNumberOfSamples());
  }

  @Override
  public String toString() {
    return "MetricHostAggregate{" +
      "sum=" + sum +
      ", numberOfSamples=" + numberOfSamples +
      ", deviation=" + deviation +
      ", max=" + max +
      ", min=" + min +
      '}';
  }
}
