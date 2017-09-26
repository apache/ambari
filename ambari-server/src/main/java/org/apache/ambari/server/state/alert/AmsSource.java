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
package org.apache.ambari.server.state.alert;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Alert when the source type is defined as {@link org.apache.ambari.server.state.alert.SourceType#METRIC}
 * <p/>
 * Equality checking for instances of this class should be executed on every
 * member to ensure that reconciling stack differences is correct.
 */
public class AmsSource extends Source {

  @SerializedName("uri")
  private AlertUri uri = null;

  @SerializedName("ams")
  private AmsInfo amsInfo = null;

  /**
   * @return the ams info, if this metric is ams-based
   */
  public AmsInfo getAmsInfo() {
    return amsInfo;
  }

  /**
   * @return the uri info, which may include port information
   */
  public AlertUri getUri() {
    return uri;
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    result = prime * result + ((amsInfo == null) ? 0 : amsInfo.hashCode());

    return result;
  }

  /**
   *
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!super.equals(obj)) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    AmsSource other = (AmsSource) obj;

    if (uri == null) {
      if (other.uri != null) {
        return false;
      }
    } else if (!uri.equals(other.uri)) {
      return false;
    }

    if (amsInfo == null) {
      if (other.amsInfo != null) {
        return false;
      }
    } else if (!amsInfo.equals(other.amsInfo)) {
      return false;
    }

    return true;
  }

  /**
   * Represents the {@code ams} element in a Metric alert.
   */
  public static class AmsInfo {

    @SerializedName("metric_list")
    private List<String> metricList;

    private String value;

    private int interval = 60;

    private String compute;

    @SerializedName("app_id")
    private String appId;

    @SerializedName("minimum_value")
    private int minimumValue;

    public String getAppId() {
      return appId;
    }

    public int getInterval() {
      return interval;
    }

    public String getCompute() {
      return compute;
    }

    public List<String> getMetricList() {
      return metricList;
    }

    public String getValue() {
      return value;
    }

    public int getMinimumValue() {
      return minimumValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      AmsInfo amsInfo = (AmsInfo) o;

      if (interval != amsInfo.interval) return false;
      if (minimumValue != amsInfo.minimumValue) return false;
      if (appId != null ? !appId.equals(amsInfo.appId) : amsInfo.appId != null)
        return false;
      if (compute != null ? !compute.equals(amsInfo.compute) : amsInfo.compute != null)
        return false;
      if (metricList != null ? !metricList.equals(amsInfo.metricList) : amsInfo.metricList != null)
        return false;
      if (value != null ? !value.equals(amsInfo.value) : amsInfo.value != null)
        return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = metricList != null ? metricList.hashCode() : 0;
      result = 31 * result + (value != null ? value.hashCode() : 0);
      result = 31 * result + interval;
      result = 31 * result + (compute != null ? compute.hashCode() : 0);
      result = 31 * result + (appId != null ? appId.hashCode() : 0);
      result = 31 * result + minimumValue;
      return result;
    }
  }
}
