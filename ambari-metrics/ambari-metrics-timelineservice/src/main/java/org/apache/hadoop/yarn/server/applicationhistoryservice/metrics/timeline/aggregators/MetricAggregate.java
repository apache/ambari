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


import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

/**
*
*/
@JsonSubTypes({@JsonSubTypes.Type(value = MetricClusterAggregate.class),
  @JsonSubTypes.Type(value = MetricHostAggregate.class)})
@InterfaceAudience.Public
@InterfaceStability.Unstable
public class MetricAggregate {
  private static final ObjectMapper mapper = new ObjectMapper();

  protected Double sum = 0.0;
  protected Double deviation;
  protected Double max = Double.MIN_VALUE;
  protected Double min = Double.MAX_VALUE;

  public MetricAggregate() {
  }

  MetricAggregate(Double sum, Double deviation, Double max,
                  Double min) {
    this.sum = sum;
    this.deviation = deviation;
    this.max = max;
    this.min = min;
  }

  public void updateSum(Double sum) {
    this.sum += sum;
  }

  public void updateMax(Double max) {
    if (max > this.max) {
      this.max = max;
    }
  }

  public void updateMin(Double min) {
    if (min < this.min) {
      this.min = min;
    }
  }

  @JsonProperty("sum")
  public Double getSum() {
    return sum;
  }

  @JsonProperty("deviation")
  public Double getDeviation() {
    return deviation;
  }

  @JsonProperty("max")
  public Double getMax() {
    return max;
  }

  @JsonProperty("min")
  public Double getMin() {
    return min;
  }

  public void setSum(Double sum) {
    this.sum = sum;
  }

  public void setDeviation(Double deviation) {
    this.deviation = deviation;
  }

  public void setMax(Double max) {
    this.max = max;
  }

  public void setMin(Double min) {
    this.min = min;
  }

  public String toJSON() throws IOException {
    return mapper.writeValueAsString(this);
  }
}
