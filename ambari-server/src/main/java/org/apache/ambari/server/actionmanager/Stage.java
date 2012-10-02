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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.agent.AgentCommand;
import org.apache.ambari.server.utils.StageUtils;

//This class encapsulates the stage. The stage encapsulates all the information
//required to persist an action.
public class Stage {
  private final long requestId;
  private final String clusterName;
  private long stageId = -1;

  //Map of roles to successFactors for this stage. Default is 1 i.e. 100%
  private Map<Role, Float> successFactors = new HashMap<Role, Float>();

  //Map of host to host-roles
  private Map<String, HostAction> hostActions = new TreeMap<String, HostAction>();
  private final String logDir;

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
    for (String host: this.hostActions.keySet()) {
      this.hostActions.get(host).setCommandId(this.requestId, this.stageId);
    }
  }

  public synchronized long getStageId() {
    return stageId;
  }

  public String getActionId() {
    return StageUtils.getActionId(requestId, stageId);
  }

  synchronized void addHostAction(String host, HostAction ha) {
    ha.setCommandId(requestId, stageId);
    hostActions.put(host, ha);
  }

  synchronized HostAction getHostAction(String host) {
    return hostActions.get(host);
  }

  /**
   * Returns an internal data structure, please don't modify it.
   * TODO: Ideally should return an iterator.
   */
  synchronized Map<String, HostAction> getHostActions() {
    return hostActions;
  }

  synchronized float getSuccessFactor(Role r) {
    Float f = successFactors.get(r);
    if (f == null) {
      return 1;
    } else {
      return f;
    }
  }

  public long getRequestId() {
    return requestId;
  }

  public String getManifest(String hostName) {
    // TODO Auto-generated method stub
    return getHostAction(hostName).getManifest();
  }

  public String getClusterName() {
    return clusterName;
  }

  public long getLastAttemptTime(String host) {
    return getHostAction(host).getLastAttemptTime();
  }

  public short getAttemptCount(String host) {
    return getHostAction(host).getAttemptCount();
  }

  public void incrementAttemptCount(String hostname) {
    getHostAction(hostname).incrementAttemptCount();
  }

  public void setLastAttemptTime(String hostname, long t) {
    getHostAction(hostname).setLastAttemptTime(t);
  }

  public AgentCommand getExecutionCommand(String hostname) {
    return getHostAction(hostname).getCommandToHost();
  }

  public long getStartTime(String hostname) {
    return getHostAction(hostname).getStartTime();
  }
  
  public synchronized boolean isStageInProgress() {
    for(String host: hostActions.keySet()) {
      for (HostRoleCommand role : hostActions.get(host).getRoleCommands()) {
        if (role.getStatus().equals(HostRoleStatus.PENDING) ||
            role.getStatus().equals(HostRoleStatus.QUEUED) || 
            role.getStatus().equals(HostRoleStatus.IN_PROGRESS)) {
          return true;
        }
      }
    }
    return false;
  }
}
