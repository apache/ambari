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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;


/**
 * Execution commands are scheduled by action manager, and these are
 * persisted in the database for recovery.
 */
public class ExecutionCommand extends AgentCommand {

  public ExecutionCommand() {
    super(AgentCommandType.EXECUTION_COMMAND);
  }

  private String clusterName;

  private String commandId;

  private String hostname;
  
  private Map<String, String> params = new HashMap<String, String>();

  private Map<String, List<String>> clusterHostInfo = 
      new HashMap<String, List<String>>();

  private List<RoleExecution> rolesCommands;
  private Map<String, Map<String, String>> configurations;

  
  @JsonProperty("commandId")
  public String getCommandId() {
    return this.commandId;
  }
  
  @JsonProperty("commandId")
  public void setCommandId(String commandId) {
    this.commandId = commandId;
  }
  
  public synchronized void addRoleCommand(String role, String cmd,
      Map<String, String> roleParams) {
    RoleExecution rec = new RoleExecution(role, cmd, roleParams);
    if (rolesCommands == null) {
      rolesCommands = new ArrayList<RoleExecution>();
    }
    rolesCommands.add(rec);
  }
  
  @JsonProperty("roleCommands")
  public synchronized List<RoleExecution> getRoleCommands() {
    return this.rolesCommands;
  }
  
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ExecutionCommand)) {
      return false;
    }
    ExecutionCommand o = (ExecutionCommand) other;
    return (this.commandId == o.commandId &&
            this.hostname == o.hostname);
  }
  
  @Override
  public String toString() {
    return "Host=" + hostname + ", commandId="+commandId;
  }

  @Override
  public int hashCode() {
    return (hostname + commandId).hashCode();
  }

  /**
   * Role Execution commands sent to the 
   *
   */
  public static class RoleExecution {

    private String role;

    private Map<String, String> roleParams = null;

    private String cmd;

    public RoleExecution() {}
    
    public RoleExecution(String role, String cmd,
        Map<String, String> roleParams) {
      this.role = role;
      this.cmd = cmd;
      this.roleParams = roleParams;
    }
    
    @JsonProperty("role")
    public String getRole() {
      return role;
    }
    
    @JsonProperty("role")
    public void setRole(String role) {
      this.role = role;
    }
    
    @JsonProperty("roleParams")
    public Map<String, String> getRoleParams() {
      return roleParams;
    }

    @JsonProperty("roleParams")
    public void setRoleParams(Map<String, String> roleParams) {
      this.roleParams = roleParams;
    }

    @JsonProperty("cmd")
    public String getCmd() {
      return cmd;
    }
    
    @JsonProperty("cmd")
    public void setCmd(String cmd) {
      this.cmd = cmd;
    }
    
    public String toString() {
      return null;
    }
  }
  
  @JsonProperty("clusterName")
  public String getClusterName() {
    return clusterName;
  }
  
  @JsonProperty("clusterName")
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @JsonProperty("hostname")
  public String getHostname() {
    return hostname;
  }

  @JsonProperty("hostname")
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  @JsonProperty("params")
  public Map<String, String> getParams() {
    return params;
  }

  @JsonProperty("params")
  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  @JsonProperty("clusterHostInfo")
  public Map<String, List<String>> getClusterHostInfo() {
    return clusterHostInfo;
  }

  @JsonProperty("clusterHostInfo")
  public void setClusterHostInfo(Map<String, List<String>> clusterHostInfo) {
    this.clusterHostInfo = clusterHostInfo;
  }
  
  @JsonProperty("configurations")
  public Map<String, Map<String, String>> getConfigurations() {
    return configurations;
  }

  @JsonProperty("configurations")
  public void setConfigurations(Map<String, Map<String, String>> configurations) {
    this.configurations = configurations;
  }
}
