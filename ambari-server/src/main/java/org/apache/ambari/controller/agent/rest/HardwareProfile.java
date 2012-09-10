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

package org.apache.ambari.controller.agent.rest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * Data model for Ambari Agent to send hardware profile to Ambari Server.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class HardwareProfile {
  @XmlElement
  private String architecture;
  @XmlElement
  private String augeasversion;
  @XmlElement
  private String domain;
  @XmlElement
  private String fqdn;
  @XmlElement
  private String hardwareisa;
  @XmlElement
  private String hardwaremodel;
  @XmlElement
  private String hostname;
  @XmlElement
  private String id;
  @XmlElement
  private String interfaces;
  @XmlElement
  private String ipaddress;
  @XmlElement
  private boolean is_virtual;
  @XmlElement
  private String kernel;
  @XmlElement
  private String kernelmajversion;
  @XmlElement
  private String kernelrelease;
  @XmlElement
  private String kernelversion;
  @XmlElement
  private String lsbdistcodename;
  @XmlElement
  private String lsbdistdescription;
  @XmlElement
  private String lsbdistid;
  @XmlElement
  private String lsbdistrelease;
  @XmlElement
  private String lsbmajdistrelease;
  @XmlElement
  private String lsbrelease;
  @XmlElement
  private String macaddress;
  @XmlElement
  private String memoryfree;
  @XmlElement
  private String memorysize;
  @XmlElement
  private String mounts;
  @XmlElement
  private String memorytotal;
  @XmlElement
  private String netmask;
  @XmlElement
  private String operatingsystem;
  @XmlElement
  private String operatingsystemrelease;
  @XmlElement
  private String osfamily;
  @XmlElement
  private String physicalprocessorcount;
  @XmlElement
  private String processorcount;
  @XmlElement
  private String puppetversion;
  @XmlElement
  private String rubyversion;
  @XmlElement
  private boolean selinux;
  @XmlElement
  private String swapfree;
  @XmlElement
  private String swapsize;
  @XmlElement
  private String timezone;
  @XmlElement
  private String uptime;
  @XmlElement
  private long uptime_days;
  @XmlElement
  private long uptime_hours;   
  
  public long getUpTimeDays() {
    return this.uptime_days;
  }
  
  public void setUpTimeDays(long uptime_days) {
    this.uptime_days = uptime_days;
  }
  
  public String toString() {
    return "memory=" + this.memorytotal + "\n" +
           "uptime_hours=" + this.uptime_hours;
   }
}
