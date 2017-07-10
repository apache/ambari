/*
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

import com.google.gson.annotations.SerializedName;


/**
 * Recovery config to be sent to the agent
 */
public class RecoveryConfig {

  /**
   * Creates a holder for agent requests
   */
  public RecoveryConfig() {
  }

  @SerializedName("type")
  private String type;

  @SerializedName("maxCount")
  private String maxCount;

  @SerializedName("windowInMinutes")
  private String windowInMinutes;

  @SerializedName("retryGap")
  private String retryGap;

  @SerializedName("maxLifetimeCount")
  private String maxLifetimeCount;

  @SerializedName("components")
  private String enabledComponents;

  @SerializedName("recoveryTimestamp")
  private long recoveryTimestamp;

  public String getEnabledComponents() {
    return enabledComponents;
  }

  public void setEnabledComponents(String enabledComponents) {
    this.enabledComponents = enabledComponents;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMaxCount() {
    return maxCount;
  }

  public void setMaxCount(String maxCount) {
    this.maxCount = maxCount;
  }

  public String getWindowInMinutes() {
    return windowInMinutes;
  }

  public void setWindowInMinutes(String windowInMinutes) {
    this.windowInMinutes = windowInMinutes;
  }

  public String getRetryGap() {
    return retryGap;
  }

  public void setRetryGap(String retryGap) {
    this.retryGap = retryGap;
  }

  public String getMaxLifetimeCount() {
    return maxLifetimeCount;
  }

  public void setMaxLifetimeCount(String maxLifetimeCount) {
    this.maxLifetimeCount = maxLifetimeCount;
  }

  /**
   * Timestamp when the recovery values were last updated.
   *
   * @return - Timestamp.
   */
  public long getRecoveryTimestamp() {
    return recoveryTimestamp;
  }

  /**
   * Set the timestamp when the recovery values were last updated.
   *
   * @param recoveryTimestamp
   */
  public void setRecoveryTimestamp(long recoveryTimestamp) {
    this.recoveryTimestamp = recoveryTimestamp;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("RecoveryConfig{");
    buffer.append(", type=").append(type);
    buffer.append(", maxCount=").append(maxCount);
    buffer.append(", windowInMinutes=").append(windowInMinutes);
    buffer.append(", retryGap=").append(retryGap);
    buffer.append(", maxLifetimeCount=").append(maxLifetimeCount);
    buffer.append(", components=").append(enabledComponents);
    buffer.append(", recoveryTimestamp=").append(recoveryTimestamp);
    buffer.append('}');
    return buffer.toString();
  }
}
