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
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.DeployState;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.live.host.HostRegistrationRequestEvent;
import org.apache.ambari.server.state.live.host.HostStatusUpdatesReceivedEvent;
import org.apache.ambari.server.state.live.host.HostUnhealthyHeartbeatEvent;

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
    long now = System.currentTimeMillis();
    try {
      if (heartbeat.getNodeStatus().getStatus()
          .equals(HostStatus.Status.HEALTHY)) {
        hostObject.handleEvent(new HostHealthyHeartbeatEvent(hostname, now));
      } else {
        hostObject.handleEvent(new HostUnhealthyHeartbeatEvent(hostname, now, null));
      }
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
    List<Cluster> clusters = clusterFsm.getClustersForHost(hostname);
    for (Cluster cl : clusters) {
      for (ComponentStatus status : heartbeat.componentStatus) {
        if (status.getClusterName() == cl.getClusterName()) {
          Service svc = cl.getService(status.getServiceName());
          ServiceComponent svcComp = svc.getServiceComponent(
              status.getComponentName());
          ServiceComponentHost scHost = svcComp.getServiceComponentHost(
              hostname);
          State currentState = scHost.getState();
          DeployState liveState = DeployState
              .valueOf(DeployState.class, status.getStatus());
          // Hack
          scHost.setState(new State(currentState
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
    long now = System.currentTimeMillis();
    if (clusterFsm.getHost(hostname) == null) {
      clusterFsm.addHost(hostname);
    }
    Host hostObject = clusterFsm.getHost(hostname);
    List<StatusCommand> cmds = new ArrayList<StatusCommand>();
    for (Cluster cl : clusterFsm.getClustersForHost(hostname)) {
      List<ServiceComponentHost> roleList = cl
          .getServiceComponentHosts(hostname);
      List<String> roles = new ArrayList<String>();
      for (ServiceComponentHost sch : roleList) {
        roles.add(sch.getServiceComponentName());
      }
      StatusCommand statusCmd = new StatusCommand();
      statusCmd.setRoles(roles);
      cmds.add(statusCmd);
    }
    
    hostObject.handleEvent(new HostRegistrationRequestEvent(hostname,
        new AgentVersion("v1"), now, register.getHardwareProfile()));
    RegistrationResponse response = new RegistrationResponse();
    if (cmds.isEmpty()) {
      //No status commands needed let the fsm know that status step is done
      hostObject.handleEvent(new HostStatusUpdatesReceivedEvent(hostname,
          now));
    } else {
      response.setCommands(cmds);
    }
    response.setResponseStatus(RegistrationStatus.OK);
    response.setResponseId(0);
    return response;
  }
}
