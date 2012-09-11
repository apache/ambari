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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Information about a mounted disk on a given node
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
//TODO convert the type safe ints/longs for used/percent/size.  
public class DiskInfo {
  @XmlElement
  String available;
  @XmlElement
  String mountpoint;
  @XmlElement
  String device;
  @XmlElement
  String used;
  @XmlElement
  String percent;
  @XmlElement
  String size;
  
  /**
   * DiskInfo object that tracks information about a disk.
   * @param mountpoint 
   * @param available
   * @param used
   * @param percent
   * @param size
   */
  public DiskInfo(String device, String mountpoint, String available, 
      String used, String percent, String size) {
    this.device = device;
    this.mountpoint = mountpoint;
    this.available = available;
    this.used = used;
    this.percent = percent;
    this.size = size;
  }
  
  /**
   * Needed for JAXB
   */
  public DiskInfo() {}
  
  public String getAvailable() {
    return this.available;
  }
  
  public String getMountPoint() {
    return this.mountpoint;
  }
  
  public String getUsed() {
    return this.used;
  }
  
  public String getPercent() {
    return this.percent;
  }
  
  @Override
  public String toString() {
    return "available=" + this.available + ",mountpoint=" + this.mountpoint
         + ",used=" + this.used + ",percent=" + this.percent + ",size=" + 
        this.size + ",device=" + this.device;
  }
}
