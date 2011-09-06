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

package org.apache.hms.common.entity.action;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.apache.hms.common.entity.RestSource;
import org.apache.hms.common.entity.cluster.MachineState.StateEntry;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * HMS Action defines a operation for HMS Agent to execute, this abstract class defines the basic
 * structure required to construct an HMS Action. 
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@action")
@XmlSeeAlso({ ScriptAction.class, DaemonAction.class, PackageAction.class })
@XmlRootElement
public abstract class Action extends RestSource {
  @XmlElement
  protected int actionId;
  
  /**
   * Reference to original command which generated this action.
   */
  @XmlElement
  protected String cmdPath;
  
  /**
   * Unique identifier of the action type.
   */
  @XmlElement
  protected String actionType;
  
  /**
   * A list of states, this action depends on.
   */
  @XmlElement
  protected List<ActionDependency> dependencies;
  
  /**
   * When the action is successfully executed, expectedResults stores the state
   * entry for the action.
   */
  @XmlElement
  protected List<StateEntry> expectedResults;
  
  /**
   * Role is a reference to a list of nodes that should execute this action.
   */
  @XmlElement
  protected String role;
  
  public int getActionId() {
    return actionId;
  }
  
  public String getCmdPath() {
    return cmdPath;
  }
  
  public String getActionType() {
    return actionType;
  }
  
  public List<ActionDependency> getDependencies() {
    return dependencies;
  }
  
  public List<StateEntry> getExpectedResults() {
    return expectedResults;
  }
  
  public String getRole() {
    return role;
  }
  
  public void setActionId(int actionId) {
    this.actionId = actionId;
  }
  
  public void setCmdPath(String cmdPath) {
    this.cmdPath = cmdPath;
  }
  
  public void setActionType(String actionType) {
    this.actionType = actionType;
  }
  
  public void setDependencies(List<ActionDependency> dependencies) {
    this.dependencies = dependencies;
  }
  
  public void setExpectedResults(List<StateEntry> expectedResults) {
    this.expectedResults = expectedResults;
  }
  
  public void setRole(String role) {
    this.role = role;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("actionId=");
    sb.append(actionId);
    sb.append(", cmdPath=");
    sb.append(cmdPath);
    sb.append(", actionType=");
    sb.append(actionType);
    if (role != null) {
      sb.append(", role=");
      sb.append(role);
    }
    sb.append(", dependencies=[");
    if (dependencies != null) {
      for(ActionDependency a : dependencies) {
        sb.append(a);
        sb.append(", ");
      }
    }
    sb.append("]");
    sb.append(", expectedResults=[");
    if (expectedResults != null) {
      for(StateEntry a : expectedResults) {
        sb.append(a);
        sb.append(", ");
      }
    }
    sb.append("]");
    return sb.toString();
  }
}
