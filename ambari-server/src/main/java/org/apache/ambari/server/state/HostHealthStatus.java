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

package org.apache.ambari.server.state;

public class HostHealthStatus {

  private HealthStatus healthStatus;

  private String healthReport;

  public HostHealthStatus(HealthStatus healthStatus, String healthReport) {
    super();
    this.healthStatus = healthStatus;
    this.healthReport = healthReport;
  }

  public synchronized HealthStatus getHealthStatus() {
    return healthStatus;
  }

  public synchronized void setHealthStatus(HealthStatus healthStatus) {
    this.healthStatus = healthStatus;
  }

  public synchronized void setHealthReport(String healthReport) {
    this.healthReport = healthReport;
  }

  public synchronized String getHealthReport() {
    return healthReport;
  }

  public static enum HealthStatus {
    UNKNOWN,      // lost heartbeat
    HEALTHY,      // all masters and slaves are live
    UNHEALTHY,    // at least one master is dead
    ALERT         // at least one slave is dead
  }
}
