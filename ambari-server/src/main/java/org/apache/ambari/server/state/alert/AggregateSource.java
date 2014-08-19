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
package org.apache.ambari.server.state.alert;

import com.google.gson.annotations.SerializedName;

/**
 * Alert when the source type is defined as {@link SourceType#AGGREGATE}.
 * Aggregate alerts are alerts that are triggered by collecting the states of
 * all instances of the defined alert and calculating the overall state.
 */
public class AggregateSource extends Source {

  @SerializedName("alert_name")
  private String m_alertName = null;

  /**
   * @return the unique name of the alert that will have its values aggregated.
   */
  public String getAlertName() {
    return m_alertName;
  }
}
