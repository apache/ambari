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
package org.apache.ambari.server.agent;

import java.util.Collection;

import org.apache.ambari.server.state.Alert;

import com.google.gson.annotations.SerializedName;

/**
 * Specialized command that updates Nagios with alert data
 */
public class NagiosAlertCommand extends StatusCommand {
  @SerializedName("alerts")
  private Collection<Alert> alerts = null;

  /**
   * @param alerts
   */
  public void setAlerts(Collection<Alert> alertData) {
    alerts = alertData;
  }

  /**
   * @return the alerts
   */
  public Collection<Alert> getAlerts() {
    return alerts;
  }

}
