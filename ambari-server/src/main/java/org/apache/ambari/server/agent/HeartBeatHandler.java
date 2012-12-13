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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.ambari.server.*;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.host.HostRegistrationRequestEvent;
import org.apache.ambari.server.state.host.HostStatusUpdatesReceivedEvent;
import org.apache.ambari.server.state.host.HostUnhealthyHeartbeatEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpSucceededEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class handles the heartbeats coming from the agent, passes on the information
 * to other modules and processes the queue to send heartbeat response.
 */
@Singleton
public class HeartBeatHandler {
  private static Log LOG = LogFactory.getLog(HeartBeatHandler.class);
  private final Clusters clusterFsm;
  private final ActionQueue actionQueue;
  private final ActionManager actionManager;
  private HeartbeatMonitor heartbeatMonitor;

  @Inject
  Injector injector;
  @Inject
  Configuration config;
  @Inject
  AmbariMetaInfo ambariMetaInfo;
  @Inject
  ActionMetadata actionMetadata;

  private Map<String, Long> hostResponseIds = new HashMap<String, Long>();
  private Map<String, HeartBeatResponse> hostResponses = new HashMap<String, HeartBeatResponse>();

  @Inject
  public HeartBeatHandler(Clusters fsm, ActionQueue aq, ActionManager am,
      Injector injector) {
    this.clusterFsm = fsm;
    this.actionQueue = aq;
    this.actionManager = am;
    this.heartbeatMonitor = new HeartbeatMonitor(fsm, aq, am, 60000);
    injector.injectMembers(this);
  }

  public void start() {
    heartbeatMonitor.start();
  }

  void setHeartbeatMonitor(HeartbeatMonitor heartbeatMonitor) {
    this.heartbeatMonitor = heartbeatMonitor;
  }

  public HeartBeatResponse handleHeartBeat(HeartBeat heartbeat)
      throws AmbariException {
    String hostname = heartbeat.getHostname();
    Long currentResponseId = hostResponseIds.get(hostname);
    HeartBeatResponse response;
    if (currentResponseId == null) {
      //Server restarted, or unknown host.
      LOG.error("CurrentResponseId unknown - send register command");
      response = new HeartBeatResponse();
      RegistrationCommand regCmd = new RegistrationCommand();
      response.setResponseId(0);
      response.setRegistrationCommand(regCmd);
      return response;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received heartbeat from host"
          +  ", hostname=" + hostname
          + ", currentResponseId=" + currentResponseId
          + ", receivedResponseId=" + heartbeat.getResponseId());
    }

    if (heartbeat.getResponseId() == currentResponseId - 1) {
      LOG.warn("Old responseId received - response was lost - returning cached response");
      return hostResponses.get(hostname);
    }else if (heartbeat.getResponseId() != currentResponseId) {
      LOG.error("Error in responseId sequence - sending agent restart command");
      response = new HeartBeatResponse();
      response.setRestartAgent(true);
      response.setResponseId(currentResponseId);
      return response;
    }

    response = new HeartBeatResponse();
    response.setResponseId(++currentResponseId);

    Host hostObject = clusterFsm.getHost(hostname);

    if (hostObject.getState().equals(HostState.HEARTBEAT_LOST)) {
      // After loosing heartbeat agent should reregister
      LOG.warn("Host is in HEARTBEAT_LOST state - sending register command");
      response = new HeartBeatResponse();
      RegistrationCommand regCmd = new RegistrationCommand();
      response.setResponseId(0);
      response.setRegistrationCommand(regCmd);
      return response;
    }

    hostResponseIds.put(hostname, currentResponseId);
    hostResponses.put(hostname, response);

    long now = System.currentTimeMillis();

    // If the host is waiting for component status updates, notify it
    if (heartbeat.componentStatus.size() > 0
            && hostObject.getState().equals(HostState.WAITING_FOR_HOST_STATUS_UPDATES)) {
      try {
        LOG.debug("Got component status updates");
        hostObject.handleEvent(new HostStatusUpdatesReceivedEvent(hostname, now));
      } catch (InvalidStateTransitionException e) {
        LOG.warn("Failed to notify the host about component status updates", e);
      }
    }

    try {
      if (heartbeat.getNodeStatus().getStatus()
          .equals(HostStatus.Status.HEALTHY)) {
        hostObject.handleEvent(new HostHealthyHeartbeatEvent(hostname, now));
      } else {
        hostObject.handleEvent(new HostUnhealthyHeartbeatEvent(hostname, now,
            null));
      }
    } catch (InvalidStateTransitionException ex) {
      LOG.warn("Asking agent to reregister due to " + ex.getMessage(),  ex);
      hostObject.setState(HostState.INIT);
      RegistrationCommand regCmd = new RegistrationCommand();
      response.setRegistrationCommand(regCmd);
      return response;
    }

    //Examine heartbeat for command reports
    List<CommandReport> reports = heartbeat.getReports();
    for (CommandReport report : reports) {
      String clusterName = report.getClusterName();
      if ((clusterName == null) || "".equals(clusterName)) {
        clusterName = "cluster1";
      }
      Cluster cl = clusterFsm.getCluster(report.getClusterName());
      String service = report.getServiceName();
      if (service == null || "".equals(service)) {
        throw new AmbariException("Invalid command report, service: " + service);
      }
      if (actionMetadata.getActions(service.toLowerCase()).contains(report.getRole())) {
        LOG.info(report.getRole() + " is an action - skip component lookup");
      } else {
        try {
          Service svc = cl.getService(service);
          ServiceComponent svcComp = svc.getServiceComponent(report.getRole());
          ServiceComponentHost scHost = svcComp.getServiceComponentHost(hostname);
          if (report.getStatus().equals("COMPLETED")) {
            scHost.handleEvent(new ServiceComponentHostOpSucceededEvent(scHost
                .getServiceComponentName(), hostname, now));
          } else if (report.getStatus().equals("FAILED")) {
            scHost.handleEvent(new ServiceComponentHostOpFailedEvent(scHost
                .getServiceComponentName(), hostname, now));
          } else if (report.getStatus().equals("IN_PROGRESS")) {
            scHost.handleEvent(new ServiceComponentHostOpInProgressEvent(scHost
                .getServiceComponentName(), hostname, now));
          }
        } catch (ServiceComponentNotFoundException scnex) {
          LOG.info("Service component not found ", scnex);
        } catch (InvalidStateTransitionException ex) {
          LOG.warn("State machine exception", ex);
        }
      }
    }
    //Update state machines from reports
    actionManager.processTaskResponse(hostname, reports);

    // Examine heartbeart for component live status reports
    Set<Cluster> clusters = clusterFsm.getClustersForHost(hostname);
    for (Cluster cl : clusters) {
      for (ComponentStatus status : heartbeat.componentStatus) {
        if (status.getClusterName().equals(cl.getClusterName())) {
          try {
            Service svc = cl.getService(status.getServiceName());
            String componentName = status.getComponentName();
            if (svc.getServiceComponents().containsKey(componentName)) {
              ServiceComponent svcComp = svc.getServiceComponent(
                      componentName);
              ServiceComponentHost scHost = svcComp.getServiceComponentHost(
                      hostname);
              State prevState = scHost.getState();
              State liveState = State.valueOf(State.class, status.getStatus());
              if (prevState.equals(State.INSTALLED)
                  || prevState.equals(State.START_FAILED)
                  || prevState.equals(State.STARTED)
                  || prevState.equals(State.STOP_FAILED)) {
                scHost.setState(liveState);
                if (!prevState.equals(liveState)) {
                  LOG.info("State of service component " + componentName
                      + " of service " + status.getServiceName()
                      + " of cluster " + status.getClusterName()
                      + " has changed from " + prevState + " to " + liveState
                      + " at host " + hostname);
                }
              }
              // TODO need to get config version and stack version from live state
            } else {
              // TODO: What should be done otherwise?
            }
          }
          catch (ServiceNotFoundException e) {
            LOG.warn("Received a live status update for a non-initialized"
                + " service"
                + ", clusterName=" + status.getClusterName()
                + ", serviceName=" + status.getServiceName());
            // FIXME ignore invalid live update and continue for now?
            continue;
          }
          catch (ServiceComponentNotFoundException e) {
            LOG.warn("Received a live status update for a non-initialized"
                + " servicecomponent"
                + ", clusterName=" + status.getClusterName()
                + ", serviceName=" + status.getServiceName()
                + ", componentName=" + status.getComponentName());
            // FIXME ignore invalid live update and continue for now?
            continue;
          }
          catch (ServiceComponentHostNotFoundException e) {
            LOG.warn("Received a live status update for a non-initialized"
                + " service"
                + ", clusterName=" + status.getClusterName()
                + ", serviceName=" + status.getServiceName()
                + ", componentName=" + status.getComponentName()
                + ", hostname=" + hostname);
            // FIXME ignore invalid live update and continue for now?
            continue;
          }
        }
      }
    }

    // Send commands if node is active
    if (hostObject.getState().equals(HostState.HEALTHY)) {
      List<AgentCommand> cmds = actionQueue.dequeueAll(heartbeat.getHostname());
      if (cmds != null && !cmds.isEmpty()) {
        for (AgentCommand ac : cmds) {
          try {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Sending command string = " + StageUtils.jaxbToString(ac));
            }
          } catch (Exception e) {
            throw new AmbariException("Could not get jaxb string for command", e);
          }
          switch (ac.getCommandType()) {
            case EXECUTION_COMMAND: {
              response.addExecutionCommand((ExecutionCommand) ac);
              break;
            }
            case STATUS_COMMAND: {
              response.addStatusCommand((StatusCommand) ac);
              break;
            }
              default:
                  LOG.error("There is no action for agent command ="+ ac.getCommandType().name() );
          }
        }
      }
    }
    return response;
  }

  public String getOsType(String os, String osRelease) {
    String osType = "";
    if (os != null) {
      osType = os;
    }
    if (osRelease != null) {
      String[] release = osRelease.split("\\.");
      if (release.length > 0) {
        osType += release[0];
      }
    }
    return osType.toLowerCase();
  }

  public RegistrationResponse handleRegistration(Register register)
    throws InvalidStateTransitionException, AmbariException {
    String hostname = register.getHostname();
    long now = System.currentTimeMillis();

    String agentOsType = getOsType(register.getHardwareProfile().getOS(),
        register.getHardwareProfile().getOSRelease());
    if (!ambariMetaInfo.areOsTypesCompatible(
        config.getServerOsType().toLowerCase(), agentOsType)) {
      LOG.warn("Received registration request from host with non matching"
          + " os type"
          + ", hostname=" + hostname
          + ", serverOsType=" + config.getServerOsType()
          + ", agentOstype=" + agentOsType);
      throw new AmbariException("Cannot register host as it does not match"
          + " server's os type"
          + ", hostname=" + hostname
          + ", serverOsType=" + config.getServerOsType()
          + ", agentOstype=" + agentOsType);
    }

    Host hostObject;
    try {
      hostObject = clusterFsm.getHost(hostname);
    } catch (HostNotFoundException ex) {
      clusterFsm.addHost(hostname);
      hostObject = clusterFsm.getHost(hostname);
    }
    // Resetting host state
    hostObject.setState(HostState.INIT);

    // Get status of service components
    List<StatusCommand> cmds = heartbeatMonitor.generateStatusCommands(hostname);

    hostObject.handleEvent(new HostRegistrationRequestEvent(hostname,
        null != register.getPublicHostname() ? register.getPublicHostname() : hostname,
        new AgentVersion("v1"), now, register.getHardwareProfile()));
    RegistrationResponse response = new RegistrationResponse();
    if (cmds.isEmpty()) {
      //No status commands needed let the fsm know that status step is done
      hostObject.handleEvent(new HostStatusUpdatesReceivedEvent(hostname,
          now));
    }
    response.setStatusCommands(cmds);

    response.setResponseStatus(RegistrationStatus.OK);

    Long requestId = 0L;
    hostResponseIds.put(hostname, requestId);
    response.setResponseId(requestId);
    return response;
  }
}
