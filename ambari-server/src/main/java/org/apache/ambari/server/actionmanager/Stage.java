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
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RoleSuccessCriteriaEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUpgradeEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nullable;

//This class encapsulates the stage. The stage encapsulates all the information
//required to persist an action.
public class Stage {

  private static Logger LOG = LoggerFactory.getLogger(Stage.class);
  private final long requestId;
  private final String clusterName;
  private long stageId = -1;
  private final String logDir;
  private final String requestContext;
  private final String clusterHostInfo;

  public String getClusterHostInfo() {
    return clusterHostInfo;
  }

  private int taskTimeout = -1;
  private int perTaskTimeFactor = 60000;

  //Map of roles to successFactors for this stage. Default is 1 i.e. 100%
  private Map<Role, Float> successFactors = new HashMap<Role, Float>();

  //Map of host to host-roles
  Map<String, Map<String, HostRoleCommand>> hostRoleCommands =
      new TreeMap<String, Map<String, HostRoleCommand>>();
  private Map<String, List<ExecutionCommandWrapper>> commandsToSend =
      new TreeMap<String, List<ExecutionCommandWrapper>>();

  @AssistedInject
  public Stage(@Assisted long requestId, @Assisted("logDir") String logDir, @Assisted("clusterName") String clusterName,
               @Assisted("requestContext") @Nullable String requestContext, @Assisted("clusterHostInfo") String clusterHostInfo) {
    this.requestId = requestId;
    this.logDir = logDir;
    this.clusterName = clusterName;
    this.requestContext = requestContext == null ? "" : requestContext;
    this.clusterHostInfo = clusterHostInfo;
  }

  /**
   * Creates Stage existing in database
   * @param actionId "requestId-stageId" string
   */
  @AssistedInject
  public Stage(@Assisted String actionId, Injector injector) {
    this(injector.getInstance(StageDAO.class).findByActionId(actionId), injector);
  }

  @AssistedInject
  public Stage(@Assisted StageEntity stageEntity, Injector injector) {
    HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    HostRoleCommandFactory hostRoleCommandFactory = injector.getInstance(HostRoleCommandFactory.class);

    requestId = stageEntity.getRequestId();
    stageId = stageEntity.getStageId();
    logDir = stageEntity.getLogInfo();
    clusterName = stageEntity.getCluster().getClusterName();
    requestContext = stageEntity.getRequestContext();
    clusterHostInfo = stageEntity.getClusterHostInfo();


    Map<String, List<HostRoleCommandEntity>> hostCommands = hostRoleCommandDAO.findSortedCommandsByStage(stageEntity);

    for (Map.Entry<String, List<HostRoleCommandEntity>> entry : hostCommands.entrySet()) {
      String hostname = entry.getKey();
      commandsToSend.put(hostname, new ArrayList<ExecutionCommandWrapper>());
      hostRoleCommands.put(hostname, new TreeMap<String, HostRoleCommand>());
      for (HostRoleCommandEntity hostRoleCommandEntity : entry.getValue()) {
        HostRoleCommand hostRoleCommand = hostRoleCommandFactory.createExisting(hostRoleCommandEntity);


        hostRoleCommands.get(hostname).put(hostRoleCommand.getRole().toString(), hostRoleCommand);
        commandsToSend.get(hostname).add(hostRoleCommand.getExecutionCommandWrapper());
      }
    }

    for (RoleSuccessCriteriaEntity successCriteriaEntity : stageEntity.getRoleSuccessCriterias()) {
      successFactors.put(successCriteriaEntity.getRole(), successCriteriaEntity.getSuccessFactor().floatValue());
    }
  }

  /**
   * Creates object to be persisted in database
   * @return StageEntity
   */
  public synchronized StageEntity constructNewPersistenceEntity() {
    StageEntity stageEntity = new StageEntity();
    stageEntity.setRequestId(requestId);
    stageEntity.setStageId(getStageId());
    stageEntity.setLogInfo(logDir);
    stageEntity.setRequestContext(requestContext);
    stageEntity.setHostRoleCommands(new ArrayList<HostRoleCommandEntity>());
    stageEntity.setRoleSuccessCriterias(new ArrayList<RoleSuccessCriteriaEntity>());
    stageEntity.setClusterHostInfo(clusterHostInfo);

    for (Role role : successFactors.keySet()) {
      RoleSuccessCriteriaEntity roleSuccessCriteriaEntity = new RoleSuccessCriteriaEntity();
      roleSuccessCriteriaEntity.setRole(role);
      roleSuccessCriteriaEntity.setStage(stageEntity);
      roleSuccessCriteriaEntity.setSuccessFactor(successFactors.get(role).doubleValue());
      stageEntity.getRoleSuccessCriterias().add(roleSuccessCriteriaEntity);
    }
    return stageEntity;
  }

  public List<HostRoleCommand> getOrderedHostRoleCommands() {
    List<HostRoleCommand> commands = new ArrayList<HostRoleCommand>();
    //TODO trick for proper storing order, check it
    for (String hostName : hostRoleCommands.keySet()) {
      for (ExecutionCommandWrapper executionCommandWrapper : commandsToSend.get(hostName)) {
        for (HostRoleCommand hostRoleCommand : hostRoleCommands.get(hostName).values()) {
          if (hostRoleCommand.getExecutionCommandWrapper() == executionCommandWrapper) {
            commands.add(hostRoleCommand);
          }
        }
      }
    }
    return commands;
  }

  public synchronized void setStageId(long stageId) {
    if (this.stageId != -1) {
      throw new RuntimeException("Attempt to set stageId again! Not allowed.");
    }
    this.stageId = stageId;
    for (String host: this.commandsToSend.keySet()) {
      for (ExecutionCommandWrapper wrapper : this.commandsToSend.get(host)) {
        ExecutionCommand cmd = wrapper.getExecutionCommand();
        cmd.setCommandId(StageUtils.getActionId(requestId, stageId));
      }
    }
  }

  public synchronized long getStageId() {
    return stageId;
  }

  public String getActionId() {
    return StageUtils.getActionId(requestId, getStageId());
  }

  /**
   * A new host role command is created for execution.
   * Creates both ExecutionCommand and HostRoleCommand objects and
   * adds them to the Stage. This should be called only once for a host-role
   * for a given stage.
   */
  public synchronized void addHostRoleExecutionCommand(String host, Role role,  RoleCommand command,
      ServiceComponentHostEvent event, String clusterName, String serviceName) {
    HostRoleCommand hrc = new HostRoleCommand(host, role, event, command);
    ExecutionCommand cmd = new ExecutionCommand();
    ExecutionCommandWrapper wrapper = new ExecutionCommandWrapper(cmd);
    hrc.setExecutionCommandWrapper(wrapper);
    cmd.setHostname(host);
    cmd.setClusterName(clusterName);
    cmd.setServiceName(serviceName);
    cmd.setCommandId(this.getActionId());
    cmd.setRole(role.name());
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
    List<ExecutionCommandWrapper> execCmdList = this.commandsToSend.get(host);
    if (execCmdList == null) {
      execCmdList = new ArrayList<ExecutionCommandWrapper>();
      this.commandsToSend.put(host, execCmdList);
    }

    if (execCmdList.contains(wrapper)) {
      //todo: proper exception
      throw new RuntimeException(
          "Setting the execution command second time for same stage: stage="
              + this.getActionId() + ", host=" + host + ", role=" + role);
    }
    execCmdList.add(wrapper);
  }

  public synchronized void addServerActionCommand(
      String actionName, Role role,  RoleCommand command, String clusterName,
      ServiceComponentHostUpgradeEvent event, String hostName) {
    HostRoleCommand hrc = new HostRoleCommand(hostName, role, event, command);
    ExecutionCommand cmd = new ExecutionCommand();
    ExecutionCommandWrapper wrapper = new ExecutionCommandWrapper(cmd);
    hrc.setExecutionCommandWrapper(wrapper);
    cmd.setHostname(hostName);
    cmd.setClusterName(clusterName);
    cmd.setServiceName("");
    cmd.setCommandId(this.getActionId());
    cmd.setRole(role.name());
    cmd.setRoleCommand(command);

    Map<String, String> roleParams = new HashMap<String, String>();
    roleParams.put(ServerAction.ACTION_NAME, actionName);
    cmd.setRoleParams(roleParams);
    Map<String, HostRoleCommand> hrcMap = this.hostRoleCommands.get(hostName);
    if (hrcMap == null) {
      hrcMap = new TreeMap<String, HostRoleCommand>();
      this.hostRoleCommands.put(hostName, hrcMap);
    }
    if (hrcMap.get(role.toString()) != null) {
      throw new RuntimeException(
          "Setting the server action the second time for same stage: stage="
              + this.getActionId() + ", action=" + actionName);
    }
    hrcMap.put(role.toString(), hrc);
    List<ExecutionCommandWrapper> execCmdList = this.commandsToSend.get(hostName);
    if (execCmdList == null) {
      execCmdList = new ArrayList<ExecutionCommandWrapper>();
      this.commandsToSend.put(hostName, execCmdList);
    }

    if (execCmdList.contains(wrapper)) {
      //todo: proper exception
      throw new RuntimeException(
          "Setting the execution command second time for same stage: stage="
              + this.getActionId() + ", action=" + actionName);
    }
    execCmdList.add(wrapper);
  }

  /**
   *
   * @return list of hosts
   */
  public synchronized List<String> getHosts() { // TODO: Check whether method should be synchronized
    List<String> hlist = new ArrayList<String>();
    for (String h : this.hostRoleCommands.keySet()) {
      hlist.add(h);
    }
    return hlist;
  }

  synchronized float getSuccessFactor(Role r) {
    Float f = successFactors.get(r);
    if (f == null) {
      if (r.equals(Role.DATANODE) || r.equals(Role.TASKTRACKER) || r.equals(Role.GANGLIA_MONITOR) ||
          r.equals(Role.HBASE_REGIONSERVER)) {
        return (float) 0.5;
      } else {
        return 1;
      }
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

  public String getRequestContext() {
    return requestContext;
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

  public ExecutionCommandWrapper getExecutionCommandWrapper(String hostname,
      String role) {
    HostRoleCommand hrc = hostRoleCommands.get(hostname).get(role);
    if (hrc != null) {
      return hrc.getExecutionCommandWrapper();
    } else {
      return null;
    }
  }

  public List<ExecutionCommandWrapper> getExecutionCommands(String hostname) {
    return commandsToSend.get(hostname);
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

  public ServiceComponentHostEventWrapper getFsmEvent(String hostname, String roleStr) {
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

  public synchronized boolean doesStageHaveHostRoleStatus(
      Set<HostRoleStatus> statuses) {
    for(String host: hostRoleCommands.keySet()) {
      for (String role : hostRoleCommands.get(host).keySet()) {
        HostRoleCommand hrc = hostRoleCommands.get(host).get(role);
        if (hrc == null) {
          return false;
        }
        for (HostRoleStatus status : statuses)
        if (hrc.getStatus().equals(status)) {
          return true;
        }
      }
    }
    return false;
  }

  public Map<String, List<ExecutionCommandWrapper>> getExecutionCommands() {
    return this.commandsToSend;
  }

  public String getLogDir() {
    return this.logDir;
  }

  public Map<String, Map<String, HostRoleCommand>> getHostRoleCommands() {
    return hostRoleCommands;
  }

  /**
   * This method should be used only in stage planner. To add
   * a new execution command use
   * {@link #addHostRoleExecutionCommand(String, Role, RoleCommand,
   * ServiceComponentHostEvent, String, String)}
   */
  public synchronized void addExecutionCommandWrapper(Stage origStage,
      String hostname, Role r) {
    String role = r.toString();
    if (commandsToSend.get(hostname) == null) {
      commandsToSend.put(hostname, new ArrayList<ExecutionCommandWrapper>());
    }
    commandsToSend.get(hostname).add(
        origStage.getExecutionCommandWrapper(hostname, role));
    if (hostRoleCommands.get(hostname) == null) {
      hostRoleCommands.put(hostname, new TreeMap<String, HostRoleCommand>());
    }
    // TODO add reference to ExecutionCommand into HostRoleCommand
    hostRoleCommands.get(hostname).put(role,
        origStage.getHostRoleCommand(hostname, role));
  }

  HostRoleCommand getHostRoleCommand(String hostname, String role) {
    return hostRoleCommands.get(hostname).get(role);
  }
  
  public synchronized int getTaskTimeout() {
    if (taskTimeout == -1) {
      int maxTasks = 0;
      for (String host: commandsToSend.keySet()) {
        if (commandsToSend.get(host).size() > maxTasks) {
          maxTasks = commandsToSend.get(host).size();
        }
      }
      taskTimeout = maxTasks * perTaskTimeFactor;
    }  
    return taskTimeout;
  }

  @Override //Object
  public synchronized String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("STAGE DESCRIPTION BEGIN\n");
    builder.append("requestId="+requestId+"\n");
    builder.append("stageId="+stageId+"\n");
    builder.append("clusterName="+clusterName+"\n");
    builder.append("logDir=" + logDir+"\n");
    builder.append("requestContext="+requestContext+"\n");
    builder.append("clusterHostInfo="+clusterHostInfo+"\n");
    builder.append("Success Factors:\n");
    for (Role r : successFactors.keySet()) {
      builder.append("  role: "+r+", factor: "+successFactors.get(r)+"\n");
    }
    for (HostRoleCommand hostRoleCommand : getOrderedHostRoleCommands()) {
      builder.append("HOST: ").append(hostRoleCommand.getHostName()).append(" :\n");
      builder.append(hostRoleCommand.getExecutionCommandWrapper().getJson());
      builder.append("\n");
      builder.append(hostRoleCommand.toString());
      builder.append("\n");
    }
    builder.append("STAGE DESCRIPTION END\n");
    return builder.toString();
  }
}
