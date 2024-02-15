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

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This differs from TimelineMetric in that this class contains all the fields
 * for a single metric.
 */
@XmlRootElement(name = "containermetric")
@XmlAccessorType(XmlAccessType.FIELD)
@InterfaceAudience.Public
@InterfaceStability.Unstable
public class ContainerMetric {
  private String hostName;
  private String containerId;
  private int pmemLimit;
  private int vmemLimit;
  private int pmemUsedAvg;
  private int pmemUsedMin;
  private int pmemUsedMax;
  private int pmem50Pct;
  private int pmem75Pct;
  private int pmem90Pct;
  private int pmem95Pct;
  private int pmem99Pct;
  private long launchDuration;
  private long localizationDuration;
  private long startTime;
  private long finishTime;
  private int exitCode;


  public ContainerMetric() {

  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getContainerId() {
    return containerId;
  }

  public void setContainerId(String containerId) {
    this.containerId = containerId;
  }

  public int getPmemLimit() {
    return pmemLimit;
  }

  public void setPmemLimit(int pmemLimit) {
    this.pmemLimit = pmemLimit;
  }

  public int getVmemLimit() {
    return vmemLimit;
  }

  public void setVmemLimit(int vmemLimit) {
    this.vmemLimit = vmemLimit;
  }

  public int getPmemUsedAvg() {
    return pmemUsedAvg;
  }

  public void setPmemUsedAvg(int pmemUsedAvg) {
    this.pmemUsedAvg = pmemUsedAvg;
  }

  public int getPmemUsedMin() {
    return pmemUsedMin;
  }

  public void setPmemUsedMin(int pmemUsedMin) {
    this.pmemUsedMin = pmemUsedMin;
  }

  public int getPmemUsedMax() {
    return pmemUsedMax;
  }

  public void setPmemUsedMax(int pmemUsedMax) {
    this.pmemUsedMax = pmemUsedMax;
  }

  public int getPmem50Pct() {
    return pmem50Pct;
  }

  public void setPmem50Pct(int pmem50Pct) {
    this.pmem50Pct = pmem50Pct;
  }

  public int getPmem75Pct() {
    return pmem75Pct;
  }

  public void setPmem75Pct(int pmem75Pct) {
    this.pmem75Pct = pmem75Pct;
  }

  public int getPmem90Pct() {
    return pmem90Pct;
  }

  public void setPmem90Pct(int pmem90Pct) {
    this.pmem90Pct = pmem90Pct;
  }

  public int getPmem95Pct() {
    return pmem95Pct;
  }

  public void setPmem95Pct(int pmem95Pct) {
    this.pmem95Pct = pmem95Pct;
  }

  public int getPmem99Pct() {
    return pmem99Pct;
  }

  public void setPmem99Pct(int pmem99Pct) {
    this.pmem99Pct = pmem99Pct;
  }

  public long getLaunchDuration() {
    return launchDuration;
  }

  public void setLaunchDuration(long launchDuration) {
    this.launchDuration = launchDuration;
  }

  public long getLocalizationDuration() {
    return localizationDuration;
  }

  public void setLocalizationDuration(long localizationDuration) {
    this.localizationDuration = localizationDuration;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getFinishTime() {
    return finishTime;
  }

  public void setFinishTime(long finishTime) {
    this.finishTime = finishTime;
  }

  public int getExitCode() {
    return exitCode;
  }

  public void setExitCode(int exitCode) {
    this.exitCode = exitCode;
  }

  @Override
  public String toString() {
    return "ContainerMetric{" +
        "hostName='" + hostName + '\'' +
        ", containerId='" + containerId + '\'' +
        ", pmemLimit=" + pmemLimit +
        ", vmemLimit=" + vmemLimit +
        ", pmemUsedAvg=" + pmemUsedAvg +
        ", pmemUsedMin=" + pmemUsedMin +
        ", pmemUsedMax=" + pmemUsedMax +
        ", pmem50Pct=" + pmem50Pct +
        ", pmem75Pct=" + pmem75Pct +
        ", pmem90Pct=" + pmem90Pct +
        ", pmem95Pct=" + pmem95Pct +
        ", pmem99Pct=" + pmem99Pct +
        ", launchDuration=" + launchDuration +
        ", localizationDuration=" + localizationDuration +
        ", startTime=" + startTime +
        ", finishTime=" + finishTime +
        ", exitCode=" + exitCode +
        '}';
  }
}