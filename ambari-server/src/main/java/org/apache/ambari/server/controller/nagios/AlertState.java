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
package org.apache.ambari.server.controller.nagios;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds information about cluster alerts.
 */
class AlertState {

  private AtomicBoolean reloadNeeded = new AtomicBoolean(false);
  private List<NagiosAlert> alerts = new ArrayList<NagiosAlert>();
  
  AlertState(List<NagiosAlert> alerts) {
    this.alerts = alerts;
  }
  
  /**
   * @return whether or not the data should be reloaded
   */
  AtomicBoolean isReloadNeeded() {
    return reloadNeeded;
  }
  
  /**
   * @param alerts the current alerts from Nagios
   */
  void setAlerts(List<NagiosAlert> alerts) {
    this.alerts = alerts;
  }

  /**
   * @return the most current alerts from Nagios
   */
  List<NagiosAlert> getAlerts() {
    return alerts;
  }
  
}
