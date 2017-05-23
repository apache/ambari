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
package org.apache.hadoop.metrics2.sink.timeline;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

@XmlRootElement(name = "metric")
@XmlAccessorType(XmlAccessType.NONE)
@InterfaceAudience.Public
@InterfaceStability.Unstable
public class TimelineMetric implements Comparable<TimelineMetric> {

  private String metricName;
  private String appId;
  private String instanceId;
  private String hostName;
  private long timestamp;
  private long startTime;
  private String type;
  private String units;
  private TreeMap<Long, Double> metricValues = new TreeMap<Long, Double>();
  private HashMap<String, String> metadata = new HashMap<>();

  // default
  public TimelineMetric() {

  }

  // copy constructor
  public TimelineMetric(TimelineMetric metric) {
    setMetricName(metric.getMetricName());
    setType(metric.getType());
    setUnits(metric.getUnits());
    setTimestamp(metric.getTimestamp());
    setAppId(metric.getAppId());
    setInstanceId(metric.getInstanceId());
    setHostName(metric.getHostName());
    setStartTime(metric.getStartTime());
    setMetricValues(new TreeMap<Long, Double>(metric.getMetricValues()));
  }

  @XmlElement(name = "metricname")
  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  @XmlElement(name = "appid")
  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  @XmlElement(name = "instanceid")
  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  @XmlElement(name = "hostname")
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @XmlElement(name = "timestamp")
  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @XmlElement(name = "starttime")
  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  @XmlElement(name = "type", defaultValue = "UNDEFINED")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @XmlElement(name = "units")
  public String getUnits() {
    return units;
  }

  public void setUnits(String units) {
    this.units = units;
  }

  @XmlElement(name = "metrics")
  public TreeMap<Long, Double> getMetricValues() {
    return metricValues;
  }

  public void setMetricValues(TreeMap<Long, Double> metricValues) {
    this.metricValues = metricValues;
  }

  public void addMetricValues(Map<Long, Double> metricValues) {
    this.metricValues.putAll(metricValues);
  }

  @XmlElement(name = "metadata")
  public HashMap<String,String> getMetadata () {
    return metadata;
  }

  public void setMetadata (HashMap<String,String> metadata) {
    this.metadata = metadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TimelineMetric metric = (TimelineMetric) o;

    if (!metricName.equals(metric.metricName)) return false;
    if (hostName != null ? !hostName.equals(metric.hostName) : metric.hostName != null)
      return false;
    if (appId != null ? !appId.equals(metric.appId) : metric.appId != null)
      return false;
    if (instanceId != null ? !instanceId.equals(metric.instanceId) : metric.instanceId != null)
      return false;
    if (timestamp != metric.timestamp) return false;
    if (startTime != metric.startTime) return false;

    return true;
  }

  public boolean equalsExceptTime(TimelineMetric metric) {
    if (!metricName.equals(metric.metricName)) return false;
    if (hostName != null ? !hostName.equals(metric.hostName) : metric.hostName != null)
      return false;
    if (appId != null ? !appId.equals(metric.appId) : metric.appId != null)
      return false;
    if (instanceId != null ? !instanceId.equals(metric.instanceId) : metric.instanceId != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = metricName.hashCode();
    result = 31 * result + (appId != null ? appId.hashCode() : 0);
    result = 31 * result + (instanceId != null ? instanceId.hashCode() : 0);
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
    return result;
  }

  @Override
  public int compareTo(TimelineMetric other) {
    if (timestamp > other.timestamp) {
      return -1;
    } else if (timestamp < other.timestamp) {
      return 1;
    } else {
      return metricName.compareTo(other.metricName);
    }
  }
}
