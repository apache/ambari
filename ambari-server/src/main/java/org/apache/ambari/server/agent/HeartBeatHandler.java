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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.Cluster;
import org.apache.ambari.server.state.live.Clusters;
import org.apache.ambari.server.state.live.host.Host;
import org.apache.ambari.server.state.live.host.HostEvent;
import org.apache.ambari.server.state.live.host.HostEventType;
import org.apache.ambari.server.state.live.host.HostState;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHost;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostLiveState;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostState;

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
  
  public HeartBeatResponse handleHeartBeat(HeartBeat heartbeat) throws AmbariException {

    HeartBeatResponse response = new HeartBeatResponse();
    response.setResponseId(0L);
    String hostname = heartbeat.getHostname();
    Host hostObject = clusterFsm.getHost(hostname);
    try {
      // TODO: handle unhealthy heartbeat as well
      hostObject.handleEvent(new HostEvent(hostname,
          HostEventType.HOST_HEARTBEAT_HEALTHY));
    } catch (InvalidStateTransitonException ex) {
      hostObject.setState(HostState.INIT);
      RegistrationCommand regCmd = new RegistrationCommand();
      List<AgentCommand> cmds = new ArrayList<AgentCommand>();
      cmds.add(regCmd);
      response.setAgentCommands(cmds);
      return response;
    }

    //Examine heartbeat for command reports
    List<CommandReport> reports = heartbeat.getCommandReports();
    actionManager.actionResponse(hostname, reports);

    // Examine heartbeart for component status
    List<Cluster> clusters = clusterFsm.getClusters(hostname);
    for (Cluster cl : clusters) {
      for (ComponentStatus status : heartbeat.componentStatus) {
        if (status.getClusterName() == cl.getClusterName()) {
          ServiceComponentHost scHost = cl.getServiceComponentHost(
              status.getServiceName(), status.getComponentName(), hostname);
          ServiceComponentHostState currentState = scHost.getState();
          ServiceComponentHostLiveState liveState = ServiceComponentHostLiveState
              .valueOf(ServiceComponentHostLiveState.class, status.getStatus());
          // Hack
          scHost.setState(new ServiceComponentHostState(currentState
              .getConfigVersion(), currentState.getStackVersion(), liveState));
        }
      }
    }

    // Send commands if node is active
    if (hostObject.getState().equals(HostState.HEALTHY)) {
      List<AgentCommand> cmds = actionQueue.dequeueAll(heartbeat.getHostname());
      response.setAgentCommands(cmds);
    }
    return response;
  }
  
  public RegistrationResponse handleRegistration(Register register)
      throws InvalidStateTransitonException, AmbariException {
    String hostname = register.getHostname();
    List<String> roles = clusterFsm.getHostComponents(hostname);
    Host hostObject = clusterFsm.getHost(hostname);
    RegistrationResponse response = new RegistrationResponse();
    StatusCommand statusCmd = new StatusCommand();
    statusCmd.setRoles(roles);
    response.setCommand(statusCmd);
    hostObject.handleEvent(new HostEvent(hostname,
        HostEventType.HOST_REGISTRATION_REQUEST));
    return response;
  }
}
