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

package org.apache.hms.common.entity.command;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.hms.common.entity.RestSource;
import org.apache.hms.common.entity.Status;
import org.apache.hms.common.entity.Status.StatusAdapter;
import org.apache.hms.common.entity.action.Action;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD) 
@XmlType(name="", propOrder = {})
public class CommandStatus extends RestSource {
  @XmlElement
  @XmlJavaTypeAdapter(StatusAdapter.class)
  protected Status status;
  
  @XmlElement
  protected String startTime;

  @XmlElement
  protected String endTime;
  
  @XmlElement
  protected String clusterName;
  
  @XmlElement
  protected int totalActions;
  
  @XmlElement
  protected int completedActions;
  
  @XmlElement
  protected List<ActionEntry> actionEntries;
  
  public CommandStatus() {
  }
  
  public CommandStatus(Status status, String startTime) {
    this.status = status;
    this.startTime = startTime;
  }
  
  public CommandStatus(Status status, String startTime, String clusterName) {
    this(status, startTime);
    this.clusterName = clusterName;
  }
  
  public Status getStatus() {
    return status;
  }
  
  public String getStartTime() {
    return startTime;
  }
  
  public String getEndTime() {
    return endTime;
  }
  
  public String getClusterName() {
    return clusterName;
  }
  
  public int getTotalActions() {
    return totalActions;
  }
  
  public int getCompletedActions() {
    return completedActions;
  }
  
  public List<ActionEntry> getActionEntries() {
    return actionEntries;
  }
  
  public void setStatus(Status status) {
    this.status = status;  
  }
  
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }
  
  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }
  
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }
  
  public void setTotalActions(int totalActions) {
    this.totalActions = totalActions;
  }
  
  public void setCompletedActions(int completedActions) {
    this.completedActions = completedActions;
  }
  
  public void setActionEntries(List<ActionEntry> actionEntries) {
    this.actionEntries = actionEntries;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("cmdStatus=");
    sb.append(status);
    sb.append(", startTime=");
    sb.append(startTime);
    sb.append(", endTime=");
    sb.append(endTime);
    sb.append(", clusterName=");
    sb.append(clusterName);
    sb.append(", totalActions=");
    sb.append(totalActions);
    sb.append(", completedActions=");
    sb.append(completedActions);
    sb.append(", actions=");
    if (actionEntries != null) {
      for(ActionEntry a : actionEntries) {
        sb.append("\n");
        sb.append(a);
      }
    }
    return sb.toString();
  }
  
  @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER) 
  @XmlRootElement
  @XmlType(name="", propOrder = {})
  public static class ActionEntry {
    protected Action action;    
    protected List<HostStatusPair> hostStatus;
    
    public ActionEntry() {
    }
    
    public ActionEntry(Action action, List<HostStatusPair> hostStatus) {
      this.action = action;
      this.hostStatus = hostStatus;
    }
    
    public Action getAction() {
      return action;
    }
    
    public List<HostStatusPair> getHostStatus() {
      return hostStatus;
    }
    
    public void setAction(Action action) {
      this.action = action;
    }
    
    public void setHostStatus(List<HostStatusPair> hostStatus) {
      this.hostStatus = hostStatus;
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[(");
      sb.append(action);
      sb.append("), (hoststatus=");
      if (hostStatus != null) {
        for(HostStatusPair a : hostStatus) {
          sb.append(a);
          sb.append(", ");
        }
      }
      sb.append(")]");
      return sb.toString();
    }
  }
  
  @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
  @XmlRootElement
  @XmlType(name="", propOrder = {})
  public static class HostStatusPair {
    protected String host;
    
    protected Status status;
    
    public HostStatusPair(){
    }
    
    public HostStatusPair(String host, Status status) {
      this.host = host;
      this.status = status;
    }
   
    public String getHost() {
      return host;
    }
    
    @XmlJavaTypeAdapter(StatusAdapter.class)    
    public Status getStatus() {
      return status;
    }
    
    public void setHost(String host) {
      this.host = host;
    }
    
    public void setStatus(Status status) {
      this.status = status;
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(host);
      sb.append(":");
      sb.append(status);
      return sb.toString();
    }
  }
}
