/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics.core.timeline.discovery;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TimelineMetricHostMetadata {
  //need concurrent data structure, only keys are used.
  private ConcurrentHashMap<String, String> hostedApps = new ConcurrentHashMap<>();
  private byte[] uuid;

  // Default constructor
  public TimelineMetricHostMetadata() {
  }

  public TimelineMetricHostMetadata(ConcurrentHashMap<String, String> hostedApps) {
    this.hostedApps = hostedApps;
  }

  public TimelineMetricHostMetadata(Set<String> hostedApps) {
    ConcurrentHashMap<String, String> appIdsMap = new ConcurrentHashMap<>();
    for (String appId : hostedApps) {
      appIdsMap.put(appId, appId);
    }
    this.hostedApps = appIdsMap;
  }

  public ConcurrentHashMap<String, String> getHostedApps() {
    return hostedApps;
  }

  public void setHostedApps(ConcurrentHashMap<String, String> hostedApps) {
    this.hostedApps = hostedApps;
  }

  public byte[] getUuid() {
    return uuid;
  }

  public void setUuid(byte[] uuid) {
    this.uuid = uuid;
  }
}
