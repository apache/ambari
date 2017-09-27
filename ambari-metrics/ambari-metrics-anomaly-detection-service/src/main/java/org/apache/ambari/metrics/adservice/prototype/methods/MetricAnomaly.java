/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.adservice.prototype.methods;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MetricAnomaly implements Serializable{

  private String methodType;
  private double anomalyScore;
  private String metricKey;
  private long timestamp;
  private double metricValue;


  public MetricAnomaly(String metricKey, long timestamp, double metricValue, String methodType, double anomalyScore) {
    this.metricKey = metricKey;
    this.timestamp = timestamp;
    this.metricValue = metricValue;
    this.methodType = methodType;
    this.anomalyScore = anomalyScore;

  }

  public String getMethodType() {
    return methodType;
  }

  public void setMethodType(String methodType) {
    this.methodType = methodType;
  }

  public double getAnomalyScore() {
    return anomalyScore;
  }

  public void setAnomalyScore(double anomalyScore) {
    this.anomalyScore = anomalyScore;
  }

  public void setMetricKey(String metricKey) {
    this.metricKey = metricKey;
  }

  public String getMetricKey() {
    return metricKey;
  }

  public void setMetricName(String metricName) {
    this.metricKey = metricName;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public double getMetricValue() {
    return metricValue;
  }

  public void setMetricValue(double metricValue) {
    this.metricValue = metricValue;
  }

}
