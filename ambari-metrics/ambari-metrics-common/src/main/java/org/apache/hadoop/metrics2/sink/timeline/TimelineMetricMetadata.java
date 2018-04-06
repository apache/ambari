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
package org.apache.hadoop.metrics2.sink.timeline;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.codehaus.jackson.annotate.JsonIgnore;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "metric_metadata")
@XmlAccessorType(XmlAccessType.NONE)
@InterfaceAudience.Public
@InterfaceStability.Unstable
public class TimelineMetricMetadata {
  private String metricName;
  private String appId;
  private String instanceId;
  private byte[] uuid;
  private String units;
  private String type = "UNDEFINED";
  private Long seriesStartTime;
  boolean supportsAggregates = true;
  boolean isWhitelisted = false;
  // Serialization ignored helper flag
  boolean isPersisted = false;

  // Placeholder to add more type later
  public enum MetricType {
    GAUGE,
    COUNTER,
    UNDEFINED
  }

  // Default constructor
  public TimelineMetricMetadata() {
  }

  public TimelineMetricMetadata(String metricName, String appId, String instanceId, String units,
                                String type, Long seriesStartTime,
                                boolean supportsAggregates, boolean isWhitelisted) {
    this.metricName = metricName;
    this.appId = appId;
    this.instanceId = instanceId;
    this.units = units;
    this.type = type;
    this.seriesStartTime = seriesStartTime;
    this.supportsAggregates = supportsAggregates;
    this.isWhitelisted = isWhitelisted;
  }

  @XmlElement(name = "metricname")
  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  // This is the key for the webservice hence ignored.
  //@XmlElement(name = "appid")
  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  @XmlElement(name = "instanceId")
  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  @XmlElement(name = "uuid")
  public byte[] getUuid() {
    return uuid;
  }

  public void setUuid(byte[] uuid) {
    this.uuid = uuid;
  }

  @XmlElement(name = "units")
  public String getUnits() {
    return units;
  }

  public void setUnits(String units) {
    this.units = units;
  }

  @XmlElement(name = "type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @XmlElement(name = "seriesStartTime")
  public Long getSeriesStartTime() {
    return (seriesStartTime != null) ? seriesStartTime : 0l;
  }

  public void setSeriesStartTime(Long seriesStartTime) {
    this.seriesStartTime = seriesStartTime;
  }

  @XmlElement(name = "supportsAggregation")
  public boolean isSupportsAggregates() {
    return supportsAggregates;
  }

  @XmlElement(name = "isWhitelisted")
  public boolean isWhitelisted() {
    return isWhitelisted;
  }

  public void setSupportsAggregates(boolean supportsAggregates) {
    this.supportsAggregates = supportsAggregates;
  }

  @JsonIgnore
  public boolean isPersisted() {
    return isPersisted;
  }

  public void setIsPersisted(boolean isPersisted) {
    this.isPersisted = isPersisted;
  }

  /**
   * Assumes the key of the object being compared is the same as @TimelineMetricMetadata
   * @param metadata @TimelineMetricMetadata to be compared
   */
  public boolean needsToBeSynced(TimelineMetricMetadata metadata) throws MetadataException {
    if (!this.metricName.equals(metadata.getMetricName()) ||
        !this.appId.equals(metadata.getAppId()) ||
      !(StringUtils.isNotEmpty(instanceId) ? instanceId.equals(metadata.instanceId) : StringUtils.isEmpty(metadata.instanceId))) {
      throw new MetadataException("Unexpected argument: metricName = " +
        metadata.getMetricName() + ", appId = " + metadata.getAppId() + ", instanceId = " + metadata.getInstanceId());
    }

    // Series start time should never change
    return (this.units != null && !this.units.equals(metadata.getUnits())) ||
      (this.type != null && !this.type.equals(metadata.getType())) ||
      //!this.lastRecordedTime.equals(metadata.getLastRecordedTime()) || // TODO: support
      !this.supportsAggregates == metadata.isSupportsAggregates() ||
      this.isWhitelisted != metadata.isWhitelisted;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TimelineMetricMetadata that = (TimelineMetricMetadata) o;

    if (!metricName.equals(that.metricName)) return false;
    if (!appId.equals(that.appId)) return false;
    return (StringUtils.isNotEmpty(instanceId) ? instanceId.equals(that.instanceId) : StringUtils.isEmpty(that.instanceId));
  }

  @Override
  public int hashCode() {
    int result = metricName.hashCode();
    result = 31 * result + (appId != null ? appId.hashCode() : 0);
    result = 31 * result + (instanceId != null ? instanceId.hashCode() : 0);
    return result;
  }
}
