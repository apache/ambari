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
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.ambari.server.utils.JaxbMapKeyListAdapter;
import org.apache.ambari.server.utils.JaxbMapKeyMapAdapter;
import org.apache.ambari.server.utils.JaxbMapKeyValAdapter;


/**
 * Execution commands are scheduled by action manager, and these are
 * persisted in the database for recovery.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class ExecutionCommand extends AgentCommand {

  public ExecutionCommand() {
    super(AgentCommandType.EXECUTION_COMMAND);
  }

  @XmlElement
  private String clusterName;

  @XmlElement
  private String commandId;

  @XmlElement
  private String hostname;

  @XmlElement
  @XmlJavaTypeAdapter(JaxbMapKeyValAdapter.class)
  private Map<String, String> params = null;

  @XmlElement
  @XmlJavaTypeAdapter(JaxbMapKeyListAdapter.class)
  private Map<String, List<String>> clusterHostInfo = null;

  @XmlElement
  private List<RoleExecution> rolesCommands;

  @XmlElement
  @XmlJavaTypeAdapter(JaxbMapKeyMapAdapter.class)
  private Map<String, Map<String, String>> configurations;

  public String getCommandId() {
    return this.commandId;
  }

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

  @Override //Object
  public boolean equals(Object other) {
    if (!(other instanceof ExecutionCommand)) {
      return false;
    }
    ExecutionCommand o = (ExecutionCommand) other;
    return (this.commandId == o.commandId &&
            this.hostname == o.hostname);
  }

  @Override //Object
  public int hashCode() {
    return (hostname + commandId).hashCode();
  }

  @XmlRootElement
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "", propOrder = {})
  static class RoleExecution {

    @XmlElement
    private String role;

    //These params are at role level
    @XmlElement
    @XmlJavaTypeAdapter(JaxbMapKeyValAdapter.class)
    private Map<String, String> roleParams = null;

    @XmlElement
    private String cmd;

    public RoleExecution(String role, String cmd,
        Map<String, String> roleParams) {
      this.role = role;
      this.cmd = cmd;
      this.roleParams = roleParams;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

    public Map<String, String> getRoleParams() {
      return roleParams;
    }

    public void setRoleParams(Map<String, String> roleParams) {
      this.roleParams = roleParams;
    }

    public String getCmd() {
      return cmd;
    }

    public void setCmd(String cmd) {
      this.cmd = cmd;
    }
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  public Map<String, List<String>> getClusterHostInfo() {
    return clusterHostInfo;
  }

  public void setClusterHostInfo(Map<String, List<String>> clusterHostInfo) {
    this.clusterHostInfo = clusterHostInfo;
  }
  
  public Map<String, Map<String, String>> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(Map<String, Map<String, String>> configurations) {
    this.configurations = configurations;
  }
}
