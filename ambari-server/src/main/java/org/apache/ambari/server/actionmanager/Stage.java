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
package org.apache.ambari.server.actionmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.mortbay.log.Log;

//This class encapsulates the stage. The stage encapsulates all the information
//required to persist an action.
public class Stage {
  private final long requestId;
  private final String clusterName;
  private long stageId = -1;
  private final String logDir;

  //Map of roles to successFactors for this stage. Default is 1 i.e. 100%
  private Map<Role, Float> successFactors = new HashMap<Role, Float>();

  //Map of host to host-roles
  private Map<String, Map<String, HostRoleCommand>> hostRoleCommands = 
      new TreeMap<String, Map<String, HostRoleCommand>>();
  private Map<String, List<ExecutionCommand>> commandsToSend = 
      new TreeMap<String, List<ExecutionCommand>>();

  public Stage(long requestId, String logDir, String clusterName) {
    this.requestId = requestId;
    this.logDir = logDir;
    this.clusterName = clusterName;
  }

  public synchronized void setStageId(long stageId) {
    if (this.stageId != -1) {
      throw new RuntimeException("Attempt to set stageId again! Not allowed.");
    }
    this.stageId = stageId;
    for (String host: this.commandsToSend.keySet()) {
      for (ExecutionCommand cmd : this.commandsToSend.get(host)) {
        cmd.setCommandId(StageUtils.getActionId(requestId, stageId));
      }
    }
  }

  public synchronized long getStageId() {
    return stageId;
  }

  public String getActionId() {
    return StageUtils.getActionId(requestId, stageId);
  }

  /**
   * A new host role command is created for execution.
   * Creates both ExecutionCommand and HostRoleCommand objects and
   * adds them to the Stage. This should be called only once for a host-role
   * for a given stage.
   */
  public synchronized void addHostRoleExecutionCommand(String host, Role role,  RoleCommand command, 
      ServiceComponentHostEvent event, String clusterName, String serviceName) {
    Log.info("Adding host role command for role: "+role+", command: "+command
        +", event: "+event+", clusterName: "+clusterName+", serviceName: "+serviceName);
    HostRoleCommand hrc = new HostRoleCommand(host, role, event);
    ExecutionCommand cmd = new ExecutionCommand();
    cmd.setHostname(host);
    cmd.setClusterName(clusterName);
    cmd.setServiceName(serviceName);
    cmd.setCommandId(this.getActionId());
    cmd.setRole(role);
    cmd.setRoleCommand(command);
    Map<String, HostRoleCommand> hrcMap = this.hostRoleCommands.get(host);
    if (hrcMap == null) {
      hrcMap = new TreeMap<String, HostRoleCommand>();
      this.hostRoleCommands.put(host, hrcMap);
    }
    if (hrcMap.get(role.toString()) != null) {
      throw new RuntimeException(
          "Setting the host role command second time for same stage: stage="
              + this.getActionId() + ", host=" + host + ", role=" + role);
    }
    hrcMap.put(role.toString(), hrc);
    List<ExecutionCommand> execCmdList = this.commandsToSend.get(host);
    if (execCmdList == null) {
      execCmdList = new ArrayList<ExecutionCommand>();
      this.commandsToSend.put(host, execCmdList);
    }
    if (execCmdList.contains(cmd)) {
      throw new RuntimeException(
          "Setting the execution command second time for same stage: stage="
              + this.getActionId() + ", host=" + host + ", role=" + role);
    }
    execCmdList.add(cmd);
  }
  
  /**
   * 
   * @return list of hosts
   */
  public synchronized List<String> getHosts() {
    List<String> hlist = new ArrayList<String>();
    for (String h : this.hostRoleCommands.keySet()) {
      hlist.add(h);
    }
    return hlist;
  }

  synchronized float getSuccessFactor(Role r) {
    Float f = successFactors.get(r);
    if (f == null) {
      return 1;
    } else {
      return f;
    }
  }
  
  public synchronized void setSuccessFactors(Map<Role, Float> suc) {
    successFactors = suc;
  }
  
  public synchronized Map<Role, Float> getSuccessFactors() {
    return successFactors;
  }

  public long getRequestId() {
    return requestId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public long getLastAttemptTime(String host, String role) {
    return this.hostRoleCommands.get(host).get(role).getLastAttemptTime();
  }

  public short getAttemptCount(String host, String role) {
    return this.hostRoleCommands.get(host).get(role).getAttemptCount();
  }

  public void incrementAttemptCount(String hostname, String role) {
    this.hostRoleCommands.get(hostname).get(role).incrementAttemptCount();
  }

  public void setLastAttemptTime(String host, String role, long t) {
    this.hostRoleCommands.get(host).get(role).setLastAttemptTime(t);
  }

  public ExecutionCommand getExecutionCommand(String hostname, String role) {
    for (ExecutionCommand execCmd : this.commandsToSend.get(hostname)) {
      if (role.equals(execCmd.getRole().toString())) {
        return execCmd;
      }
    }
    return null;
  }
  
  public List<ExecutionCommand> getExecutionCommands(String hostname) {
    return this.commandsToSend.get(hostname);
  }

  public long getStartTime(String hostname, String role) {
    return this.hostRoleCommands.get(hostname).get(role).getStartTime();
  }
  
  public void setStartTime(String hostname, String role, long startTime) {
    this.hostRoleCommands.get(hostname).get(role).setStartTime(startTime);
  }
  
  public HostRoleStatus getHostRoleStatus(String hostname, String role) {
    return this.hostRoleCommands.get(hostname).get(role).getStatus();
  }
  
  public void setHostRoleStatus(String host, String role,
      HostRoleStatus status) {
    this.hostRoleCommands.get(host).get(role).setStatus(status);
  }
  
  public ServiceComponentHostEvent getFsmEvent(String hostname, String roleStr) {
    return this.hostRoleCommands.get(hostname).get(roleStr).getEvent();
  }
  

  public void setExitCode(String hostname, String role, int exitCode) {
    this.hostRoleCommands.get(hostname).get(role).setExitCode(exitCode);
  }
  
  public int getExitCode(String hostname, String role) {
    return this.hostRoleCommands.get(hostname).get(role).getExitCode();
  }

  public void setStderr(String hostname, String role, String stdErr) {
    this.hostRoleCommands.get(hostname).get(role).setStderr(stdErr);
  }

  public void setStdout(String hostname, String role, String stdOut) {
    this.hostRoleCommands.get(hostname).get(role).setStdout(stdOut);
  }
  
  public synchronized boolean isStageInProgress() {
    for(String host: hostRoleCommands.keySet()) {
      for (String role : hostRoleCommands.get(host).keySet()) {
        HostRoleCommand hrc = hostRoleCommands.get(host).get(role);
        if (hrc == null) {
          return false;
        }
        if (hrc.getStatus().equals(HostRoleStatus.PENDING) ||
            hrc.getStatus().equals(HostRoleStatus.QUEUED) || 
            hrc.getStatus().equals(HostRoleStatus.IN_PROGRESS)) {
          return true;
        }
      }
    }
    return false;
  }

  public Map<String, List<ExecutionCommand>> getExecutionCommands() {
    return this.commandsToSend;
  }

  public String getLogDir() {
    return this.logDir;
  }

  /**
   * This method should be used only in stage planner. To add
   * a new execution command use
   * {@link #addHostRoleExecutionCommand(String, Role, RoleCommand, 
   * ServiceComponentHostEvent, String, String)}
   */
  public synchronized void addExecutionCommand(Stage origStage,
      ExecutionCommand executionCommand) {
    String hostname = executionCommand.getHostname();
    String role = executionCommand.getRole().toString();
    if (commandsToSend.get(hostname) == null) {
      commandsToSend.put(hostname, new ArrayList<ExecutionCommand>());
    }
    commandsToSend.get(hostname).add(executionCommand);
    if (hostRoleCommands.get(hostname) == null) {
      hostRoleCommands.put(hostname, new TreeMap<String, HostRoleCommand>());
    }
    hostRoleCommands.get(hostname).put(role,
        origStage.getHostRoleCommand(hostname, role));
  }

  private HostRoleCommand getHostRoleCommand(String hostname, String role) {
    return hostRoleCommands.get(hostname).get(role);
  }
  
  @Override //Object
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("STAGE DESCRIPTION BEGIN\n");
    builder.append("requestId="+requestId+"\n");
    builder.append("stageId="+stageId+"\n");
    builder.append("clusterName="+clusterName+"\n");
    builder.append("logDir=" + logDir+"\n");
    builder.append("Success Factors:\n");
    for (Role r : successFactors.keySet()) {
      builder.append("  role: "+r+", factor: "+successFactors.get(r)+"\n");
    }
    for (String host : commandsToSend.keySet()) {
      builder.append("HOST: " + host + " :\n");
      for (ExecutionCommand ec : commandsToSend.get(host)) {
        builder.append(ec.toString());
        builder.append("\n");
        HostRoleCommand hrc = hostRoleCommands.get(host).get(
            ec.getRole().toString());
        builder.append(hrc.toString());
        builder.append("\n");
      }
    }
    builder.append("STAGE DESCRIPTION END\n");
    return builder.toString();
  }
}
