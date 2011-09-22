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

package org.apache.ambari.common.rest.entities.agent;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * Data model for Ambari Agent to send hardware profile to Ambari Controller.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class HardwareProfile {
  @XmlElement
  private int coreCount;
  @XmlElement
  private int diskCount;
  @XmlElement
  private long ramSize;
  @XmlElement
  private int cpuSpeed;
  @XmlElement
  private long netSpeed;
  @XmlElement
  private String cpuFlags;
  
  public int getCoreCount() {
    return coreCount;
  }
  
  public int getDiskCount() {
    return diskCount;
  }
  
  public long ramSize() {
    return ramSize;
  }
  
  public int getCpuSpeed() {
    return cpuSpeed;
  }
  
  public long getNetSpeed() {
    return netSpeed;
  }
  
  public String getCpuFlags() {
    return cpuFlags;
  }
  
  public void setCoreCount(int coreCount) {
    this.coreCount = coreCount;
  }
  
  public void setDiskCount(int diskCount) {
    this.diskCount = diskCount;
  }
  
  public void setRamSize(long ramSize) {
    this.ramSize = ramSize;
  }
  
  public void setCpuSpeed(int cpuSpeed) {
    this.cpuSpeed = cpuSpeed;
  }
  
  public void setNetSpeed(long netSpeed) {
    this.netSpeed = netSpeed;
  }
  
  public void setCpuFlags(String cpuFlags) {
    this.cpuFlags = cpuFlags;
  }
}
