/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logfeeder.plugin.common;

import java.io.Serializable;

public class MetricData implements Serializable {
  public final String metricsName;
  public final boolean isPointInTime;

  public MetricData(String metricsName, boolean isPointInTime) {
    this.metricsName = metricsName;
    this.isPointInTime = isPointInTime;
  }

  public long value = 0;
  public long prevPublishValue = 0;

  public long prevLogValue = 0;
  public long prevLogTime = System.currentTimeMillis();

  public int publishCount = 0; // Number of times the metric was published so far
}
