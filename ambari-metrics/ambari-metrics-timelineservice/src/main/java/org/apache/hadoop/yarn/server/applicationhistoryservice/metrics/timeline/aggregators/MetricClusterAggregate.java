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
*
*/
public class MetricClusterAggregate extends MetricAggregate {
  private int numberOfHosts;

  @JsonCreator
  public MetricClusterAggregate() {
  }

  public MetricClusterAggregate(Double sum, int numberOfHosts, Double deviation,
                         Double max, Double min) {
    super(sum, deviation, max, min);
    this.numberOfHosts = numberOfHosts;
  }

  @JsonProperty("numberOfHosts")
  public int getNumberOfHosts() {
    return numberOfHosts;
  }

  public void updateNumberOfHosts(int count) {
    this.numberOfHosts += count;
  }

  public void setNumberOfHosts(int numberOfHosts) {
    this.numberOfHosts = numberOfHosts;
  }

  /**
   * Find and update min, max and avg for a minute
   */
  public void updateAggregates(MetricClusterAggregate hostAggregate) {
    updateMax(hostAggregate.getMax());
    updateMin(hostAggregate.getMin());
    updateSum(hostAggregate.getSum());
    updateNumberOfHosts(hostAggregate.getNumberOfHosts());
  }

  @Override
  public String toString() {
    return "MetricAggregate{" +
      "sum=" + sum +
      ", numberOfHosts=" + numberOfHosts +
      ", deviation=" + deviation +
      ", max=" + max +
      ", min=" + min +
      '}';
  }
}
