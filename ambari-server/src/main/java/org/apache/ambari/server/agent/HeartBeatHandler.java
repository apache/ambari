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

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.state.live.Clusters;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * This class handles the heartbeats coming from the agent, passes on the information
 * to other modules and processes the queue to send heartbeat response.
 */
@Singleton
public class HeartBeatHandler {
  
  private final Clusters clusterFsm;
  private final ActionQueue actionQueue;
  private final ActionManager actionManager;
  private HeartbeatMonitor heartbeatMonitor;
  
  @Inject
  public HeartBeatHandler(Clusters fsm, ActionQueue aq, ActionManager am) {
    this.clusterFsm = fsm;
    this.actionQueue = aq;
    this.actionManager = am;
    this.heartbeatMonitor = new HeartbeatMonitor(fsm, aq, am, 60000);
  }
  
  public void start() {
    heartbeatMonitor.start();
  }
  
  public HeartBeatResponse handleHeartBeat(HeartBeat heartbeat) {

    HeartBeatResponse response = new HeartBeatResponse();
    response.setClusterId("test");
    response.setResponseId(0L);
    List<String> clusterNames = clusterFsm.getClusters(heartbeat.getHostname()); 
    try {
      clusterFsm.handleHeartbeat(heartbeat.getHostname(),
          heartbeat.getTimestamp());
    } catch (Exception ex) {
      // Unexpected heartbeat, reset to init state
      // send registration command
      clusterFsm.updateStatus(heartbeat.getHostname(), "GO_TO_INIT");
      RegistrationCommand regCmd = new RegistrationCommand();
      List<AgentCommand> cmds = new ArrayList<AgentCommand>();
      cmds.add(regCmd);
      response.setAgentCommands(cmds);
      return response;
    }

    // Examine heartbeat for command reports
    List<CommandReport> reports = heartbeat.getCommandReports();
    actionManager.actionResponse(heartbeat.getHostname(), reports);

    // Examine heartbeart for component status
    for (ComponentStatus status : heartbeat.componentStatus) {
      clusterFsm.updateStatus(heartbeat.getHostname(), status.status);
    }
    
    //TODO: Check if heartbeat is unhealthy

    //Send commands if node is active
    if (clusterFsm.isNodeActive(heartbeat.getHostname())) {
      List<AgentCommand> cmds = actionQueue.dequeueAll(heartbeat.getHostname());
      response.setAgentCommands(cmds);
    }
    return response;
  }
  
  public RegistrationResponse handleRegistration(Register register) {
    List<String> roles = clusterFsm.getHostComponents(register.getHostname());
    try {
      clusterFsm.handleRegistration(register.getHostname());
    } catch (Exception ex) {
      //Go to status check state
      clusterFsm.updateStatus(register.getHostname(), "GO TO STATUS CHECK");
    }
    RegistrationResponse response = new RegistrationResponse();
    return response;
  }
}
