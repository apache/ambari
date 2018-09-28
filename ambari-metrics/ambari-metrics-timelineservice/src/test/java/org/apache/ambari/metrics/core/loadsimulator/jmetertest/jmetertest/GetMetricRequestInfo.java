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
package org.apache.ambari.metrics.core.loadsimulator.jmetertest.jmetertest;

import org.apache.commons.lang.StringUtils;

import java.util.List;


public class GetMetricRequestInfo {

  private String metricStringPayload;
  private boolean needsTimestamps;
  private boolean needsHost;

  public GetMetricRequestInfo(List<String> metrics, boolean needsTimestamps, boolean needsHost) {

    this.setMetricStringPayload(StringUtils.join(metrics, ","));
    this.setNeedsTimestamps(needsTimestamps);
    this.setNeedsHost(needsHost);
  }

  public String getMetricStringPayload() {
    return metricStringPayload;
  }

  public void setMetricStringPayload(String metricStringPayload) {
    this.metricStringPayload = metricStringPayload;
  }

  public boolean needsTimestamps() {
    return needsTimestamps;
  }

  public void setNeedsTimestamps(boolean needsTimestamps) {
    this.needsTimestamps = needsTimestamps;
  }

  public boolean needsHost() {
    return needsHost;
  }

  public void setNeedsHost(boolean needsHost) {
    this.needsHost = needsHost;
  }
}
