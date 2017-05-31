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
package org.apache.ambari.metrics.alertservice.common;

public class MetricAnomaly {

    private String metricKey;
    private long timestamp;
    private double metricValue;
    private MethodResult methodResult;

    public MetricAnomaly(String metricKey, long timestamp, double metricValue, MethodResult methodResult) {
        this.metricKey = metricKey;
        this.timestamp = timestamp;
        this.metricValue = metricValue;
        this.methodResult = methodResult;
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

    public MethodResult getMethodResult() {
        return methodResult;
    }

    public void setMethodResult(MethodResult methodResult) {
        this.methodResult = methodResult;
    }

    public String getAnomalyAsString() {
        return metricKey + ":" + timestamp + ":" + metricValue + ":" + methodResult.prettyPrint();
    }
}
