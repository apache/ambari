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
import java.util.List;

import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.utils.StageUtils;

/**
 * Encapsulates entire task for a host for a stage or action. This class
 * contains all the information to generate an
 * {@link org.apache.ambari.server.agent.ExecutionCommand} that will be
 * scheduled for a host.
 */
public class HostAction {
  private final String host;
  private List<HostRoleCommand> roles;
  private long startTime = -1;
  private long lastAttemptTime = -1;
  private short attemptCount = 0;

  /**
   * This object will be serialized and sent to the agent.
   */
  private ExecutionCommand commandToHost;

  public String getManifest() {
    //generate manifest
    return null;
  }

  public HostAction(String host) {
    this.host = host;
    roles = new ArrayList<HostRoleCommand>();
    commandToHost = new ExecutionCommand();
    commandToHost.setHostname(host);
  }

  public HostAction(HostAction ha) {
    this.host = ha.host;
    this.roles = ha.roles;
    this.startTime = ha.startTime;
    this.lastAttemptTime = ha.lastAttemptTime;
    this.attemptCount = ha.attemptCount;
    this.commandToHost = ha.commandToHost;
  }

  public void addHostRoleCommand(HostRoleCommand cmd) {
    roles.add(cmd);
  }

  public List<HostRoleCommand> getRoleCommands() {
    return roles;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getLastAttemptTime() {
    return this.lastAttemptTime;
  }

  public void setLastAttemptTime(long t) {
    this.lastAttemptTime = t;
  }

  public void incrementAttemptCount() {
    this.attemptCount ++;
  }

  public short getAttemptCount() {
    return this.attemptCount;
  }

  public ExecutionCommand getCommandToHost() {
    return this.commandToHost;
  }

  public synchronized void setCommandId(long requestId, long stageId) {
    commandToHost.setCommandId(StageUtils.getActionId(requestId, stageId));
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }
}
