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

package org.apache.ambari.common.rest.agent;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * Data model for Ambari Agent to send heartbeat to Ambari Controller.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"responseId","timestamp", 
    "hostname", "hardwareProfile", "installedRoleStates",
    "serverStates", "deployState", "actionResults", "idle"})
public class HeartBeat {
  @XmlElement
  private short responseId = -1;
  @XmlElement
  private long timestamp;
  @XmlElement
  private String hostname;
  @XmlElement
  private HardwareProfile hardwareProfile;
  @XmlElement
  private List<AgentRoleState> installedRoleStates;
  @XmlElement
  private int installScriptHash;
  @XmlElement
  private List<ActionResult> actionResults;
  @XmlElement
  private boolean idle;
  
  public short getResponseId() {
    return responseId;
  }
  
  public void setResponseId(short responseId) {
    this.responseId=responseId;
  }
  
  public long getTimestamp() {
    return timestamp;
  }
  
  public String getHostname() {
    return hostname;
  }
  
  public boolean getIdle() {
    return idle;
  }
  
  public HardwareProfile getHardwareProfile() {
    return hardwareProfile;
  }
  
  public List<ActionResult> getActionResults() {
    return actionResults;
  }
  
  public List<AgentRoleState> getInstalledRoleStates() {
    return installedRoleStates;
  }
  
  public int getInstallScriptHash() {
    return installScriptHash;
  }
  
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
  
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }
    
  public void setActionResults(List<ActionResult> actionResults) {
    this.actionResults = actionResults;
  }

  public void setHardwareProfile(HardwareProfile hardwareProfile) {
    this.hardwareProfile = hardwareProfile;    
  }
  
  public void setInstalledRoleStates(List<AgentRoleState> installedRoleStates) {
    this.installedRoleStates = installedRoleStates;
  }
  
  public void setIdle(boolean idle) {
    this.idle = idle;
  }
  
  public void setInstallScriptHash(int hash) {
    this.installScriptHash = hash;
  }
}
