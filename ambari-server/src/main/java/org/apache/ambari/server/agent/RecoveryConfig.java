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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Recovery config to be sent to the agent
 */
public class RecoveryConfig {

  @SerializedName("type")
  @JsonProperty("type")
  private String type;

  @SerializedName("maxCount")
  @JsonProperty("maxCount")
  private String maxCount;

  @SerializedName("windowInMinutes")
  @JsonProperty("windowInMinutes")
  private String windowInMinutes;

  @SerializedName("retryGap")
  @JsonProperty("retryGap")
  private String retryGap;

  @SerializedName("maxLifetimeCount")
  @JsonProperty("maxLifetimeCount")
  private String maxLifetimeCount;

  @SerializedName("components")
  @JsonProperty("components")
  private List<RecoveryConfigComponent> enabledComponents;

  public RecoveryConfig(String type, String maxCount, String windowInMinutes, String retryGap, String maxLifetimeCount,
                        List<RecoveryConfigComponent> enabledComponents) {
    this.type = type;
    this.maxCount = maxCount;
    this.windowInMinutes = windowInMinutes;
    this.retryGap = retryGap;
    this.maxLifetimeCount = maxLifetimeCount;
    this.enabledComponents = enabledComponents;
  }

  public List<RecoveryConfigComponent> getEnabledComponents() {
    return enabledComponents == null ? null : Collections.unmodifiableList(enabledComponents);
  }

  public String getType() {
    return type;
  }

  public String getMaxCount() {
    return maxCount;
  }

  public String getWindowInMinutes() {
    return windowInMinutes;
  }

  public String getRetryGap() {
    return retryGap;
  }

  public String getMaxLifetimeCount() {
    return maxLifetimeCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RecoveryConfig that = (RecoveryConfig) o;

    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (maxCount != null ? !maxCount.equals(that.maxCount) : that.maxCount != null) return false;
    if (windowInMinutes != null ? !windowInMinutes.equals(that.windowInMinutes) : that.windowInMinutes != null)
      return false;
    if (retryGap != null ? !retryGap.equals(that.retryGap) : that.retryGap != null) return false;
    if (maxLifetimeCount != null ? !maxLifetimeCount.equals(that.maxLifetimeCount) : that.maxLifetimeCount != null)
      return false;
    return enabledComponents != null ? enabledComponents.equals(that.enabledComponents) : that.enabledComponents == null;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (maxCount != null ? maxCount.hashCode() : 0);
    result = 31 * result + (windowInMinutes != null ? windowInMinutes.hashCode() : 0);
    result = 31 * result + (retryGap != null ? retryGap.hashCode() : 0);
    result = 31 * result + (maxLifetimeCount != null ? maxLifetimeCount.hashCode() : 0);
    result = 31 * result + (enabledComponents != null ? enabledComponents.hashCode() : 0);
    return result;
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
    buffer.append('}');
    return buffer.toString();
  }
}
