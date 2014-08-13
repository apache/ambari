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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.host.HostRegistrationRequestEvent;
import org.apache.ambari.server.state.host.HostStatusUpdatesReceivedEvent;
import org.apache.ambari.server.state.host.HostUnhealthyHeartbeatEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpSucceededEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStoppedEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;


/**
 * This class handles the heartbeats coming from the agent, passes on the information
 * to other modules and processes the queue to send heartbeat response.
 */
@Singleton
public class HeartBeatHandler {
  private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
  private static Log LOG = LogFactory.getLog(HeartBeatHandler.class);
  private final Clusters clusterFsm;
  private final ActionQueue actionQueue;
  private final ActionManager actionManager;
  private HeartbeatMonitor heartbeatMonitor;

  @Inject
  private Injector injector;

  @Inject
  private Configuration config;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private ActionMetadata actionMetadata;

  @Inject
  private Gson gson;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private AlertDefinitionHash alertDefinitionHash;

  private Map<String, Long> hostResponseIds = new ConcurrentHashMap<String, Long>();

  private Map<String, HeartBeatResponse> hostResponses = new ConcurrentHashMap<String, HeartBeatResponse>();

  @Inject
  public HeartBeatHandler(Clusters fsm, ActionQueue aq, ActionManager am,
                          Injector injector) {
    clusterFsm = fsm;
    actionQueue = aq;
    actionManager = am;
    heartbeatMonitor = new HeartbeatMonitor(fsm, aq, am, 60000, injector);
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
    long now = System.currentTimeMillis();
    if(heartbeat.getAgentEnv() != null && heartbeat.getAgentEnv().getHostHealth() != null) {
      heartbeat.getAgentEnv().getHostHealth().setServerTimeStampAtReporting(now);
    }

    String hostname = heartbeat.getHostname();
    Long currentResponseId = hostResponseIds.get(hostname);
    HeartBeatResponse response;

    if (currentResponseId == null) {
      //Server restarted, or unknown host.
      LOG.error("CurrentResponseId unknown for " + hostname + " - send register command");
      return createRegisterCommand();
    }

    LOG.debug("Received heartbeat from host"
        + ", hostname=" + hostname
        + ", currentResponseId=" + currentResponseId
        + ", receivedResponseId=" + heartbeat.getResponseId());

    if (heartbeat.getResponseId() == currentResponseId - 1) {
      LOG.warn("Old responseId received - response was lost - returning cached response");
      return hostResponses.get(hostname);
    } else if (heartbeat.getResponseId() != currentResponseId) {
      LOG.error("Error in responseId sequence - sending agent restart command");
      return createRestartCommand(currentResponseId);
    }

    response = new HeartBeatResponse();
    response.setResponseId(++currentResponseId);

    Host hostObject = clusterFsm.getHost(hostname);

    if (hostObject.getState().equals(HostState.HEARTBEAT_LOST)) {
      // After loosing heartbeat agent should reregister
      LOG.warn("Host is in HEARTBEAT_LOST state - sending register command");
      return createRegisterCommand();
    }

    hostResponseIds.put(hostname, currentResponseId);
    hostResponses.put(hostname, response);

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
      if (heartbeat.getNodeStatus().getStatus().equals(HostStatus.Status.HEALTHY)) {
        hostObject.handleEvent(new HostHealthyHeartbeatEvent(hostname, now,
            heartbeat.getAgentEnv(), heartbeat.getMounts()));
      } else {
        hostObject.handleEvent(new HostUnhealthyHeartbeatEvent(hostname, now,
            null));
      }
    } catch (InvalidStateTransitionException ex) {
      LOG.warn("Asking agent to reregister due to " + ex.getMessage(), ex);
      hostObject.setState(HostState.INIT);
      return createRegisterCommand();
    }

    //Examine heartbeat for command reports
    processCommandReports(heartbeat, hostname, clusterFsm, now);

    // Examine heartbeart for component live status reports
    processStatusReports(heartbeat, hostname, clusterFsm);

    // Calculate host status
    // NOTE: This step must be after processing command/status reports
    processHostStatus(heartbeat, hostname);

    calculateHostAlerts(heartbeat, hostname);

    // Send commands if node is active
    if (hostObject.getState().equals(HostState.HEALTHY)) {
      sendCommands(hostname, response);
      annotateResponse(hostname, response);
    }

    // send the alert definition hash for this host
    Map<String, String> alertDefinitionHashes = alertDefinitionHash.getHashes(hostname);
    response.setAlertDefinitionHash(alertDefinitionHashes);

    return response;
  }

  protected void calculateHostAlerts(HeartBeat heartbeat, String hostname)
          throws AmbariException {
      if (heartbeat != null && hostname != null) {
        for (Cluster cluster : clusterFsm.getClustersForHost(hostname)) {
          cluster.addAlerts(heartbeat.getNodeStatus().getAlerts());
        }
      }
  }

  protected void processHostStatus(HeartBeat heartbeat, String hostname) throws AmbariException {

    Host host = clusterFsm.getHost(hostname);
    HealthStatus healthStatus = host.getHealthStatus().getHealthStatus();

    if (!healthStatus.equals(HostHealthStatus.HealthStatus.UNKNOWN)) {

      List<ComponentStatus> componentStatuses = heartbeat.getComponentStatus();
      //Host status info could be calculated only if agent returned statuses in heartbeat
      //Or, if a command is executed that can change component status
      boolean calculateHostStatus = false;
      String clusterName = null;
      if (componentStatuses.size() > 0) {
        calculateHostStatus = true;
        for (ComponentStatus componentStatus : componentStatuses) {
          clusterName = componentStatus.getClusterName();
          break;
        }
      }

      if (!calculateHostStatus) {
        List<CommandReport> reports = heartbeat.getReports();
        for (CommandReport report : reports) {
          if (RoleCommand.ACTIONEXECUTE.toString().equals(report.getRoleCommand())) {
            continue;
          }

          String service = report.getServiceName();
          if (actionMetadata.getActions(service.toLowerCase()).contains(report.getRole())) {
            continue;
          }
          if (report.getStatus().equals("COMPLETED")) {
            calculateHostStatus = true;
            clusterName = report.getClusterName();
            break;
          }
        }
      }

      if (calculateHostStatus) {
        //Use actual component status to compute the host status
        int masterCount = 0;
        int mastersRunning = 0;
        int slaveCount = 0;
        int slavesRunning = 0;

        StackId stackId;
        Cluster cluster = clusterFsm.getCluster(clusterName);
        stackId = cluster.getDesiredStackVersion();

        MaintenanceStateHelper psh = injector.getInstance(MaintenanceStateHelper.class);

        List<ServiceComponentHost> scHosts = cluster.getServiceComponentHosts(heartbeat.getHostname());
        for (ServiceComponentHost scHost : scHosts) {
          ComponentInfo componentInfo =
              ambariMetaInfo.getComponent(stackId.getStackName(),
                  stackId.getStackVersion(), scHost.getServiceName(),
                  scHost.getServiceComponentName());

          String status = scHost.getState().name();

          String category = componentInfo.getCategory();

          if (MaintenanceState.OFF == psh.getEffectiveState(scHost, host)) {
            if (category.equals("MASTER")) {
              ++masterCount;
              if (status.equals("STARTED")) {
                ++mastersRunning;
              }
            } else if (category.equals("SLAVE")) {
              ++slaveCount;
              if (status.equals("STARTED")) {
                ++slavesRunning;
              }
            }
          }
        }

        if (masterCount == mastersRunning && slaveCount == slavesRunning) {
          healthStatus = HealthStatus.HEALTHY;
        } else if (masterCount > 0 && mastersRunning < masterCount) {
          healthStatus = HealthStatus.UNHEALTHY;
        } else {
          healthStatus = HealthStatus.ALERT;
        }

        host.setStatus(healthStatus.name());
        host.persist();
      }

      //If host doesn't belongs to any cluster
      if ((clusterFsm.getClustersForHost(host.getHostName())).size() == 0) {
        healthStatus = HealthStatus.HEALTHY;
        host.setStatus(healthStatus.name());
        host.persist();
      }
    }
  }

  protected void processCommandReports(
      HeartBeat heartbeat, String hostname, Clusters clusterFsm, long now)
      throws AmbariException {
    List<CommandReport> reports = heartbeat.getReports();

    // Cache HostRoleCommand entities because we will need them few times
    List<Long> taskIds = new ArrayList<Long>();
    for (CommandReport report : reports) {
      taskIds.add(report.getTaskId());
    }
    Collection<HostRoleCommand> commands = actionManager.getTasks(taskIds);

    Iterator<HostRoleCommand> hostRoleCommandIterator = commands.iterator();
    for (CommandReport report : reports) {
      LOG.debug("Received command report: " + report);
      // Fetch HostRoleCommand that corresponds to a given task ID
      HostRoleCommand hostRoleCommand = hostRoleCommandIterator.next();
      // Skip sending events for command reports for ABORTed commands
      if (hostRoleCommand.getStatus() == HostRoleStatus.ABORTED) {
        continue;
      }
      //pass custom STAR, STOP and RESTART
      if (RoleCommand.ACTIONEXECUTE.toString().equals(report.getRoleCommand()) ||
         (RoleCommand.CUSTOM_COMMAND.toString().equals(report.getRoleCommand()) &&
         !("RESTART".equals(report.getCustomCommand()) ||
         "START".equals(report.getCustomCommand()) ||
         "STOP".equals(report.getCustomCommand())))) {
        continue;
      }

      Cluster cl = clusterFsm.getCluster(report.getClusterName());
      String service = report.getServiceName();
      if (service == null || service.isEmpty()) {
        throw new AmbariException("Invalid command report, service: " + service);
      }
      if (actionMetadata.getActions(service.toLowerCase()).contains(report.getRole())) {
        LOG.debug(report.getRole() + " is an action - skip component lookup");
      } else {
        try {
          Service svc = cl.getService(service);
          ServiceComponent svcComp = svc.getServiceComponent(report.getRole());
          ServiceComponentHost scHost = svcComp.getServiceComponentHost(hostname);
          String schName = scHost.getServiceComponentName();

          if (report.getStatus().equals("COMPLETED")) {
            // Updating stack version, if needed
            if (scHost.getState().equals(State.UPGRADING)) {
              scHost.setStackVersion(scHost.getDesiredStackVersion());
            } else if ((report.getRoleCommand().equals(RoleCommand.START.toString()) ||
                (report.getRoleCommand().equals(RoleCommand.CUSTOM_COMMAND.toString()) &&
                    ("START".equals(report.getCustomCommand()) ||
                    "RESTART".equals(report.getCustomCommand()))))
                && null != report.getConfigurationTags()
                && !report.getConfigurationTags().isEmpty()) {
              LOG.info("Updating applied config on service " + scHost.getServiceName() +
                ", component " + scHost.getServiceComponentName() + ", host " + scHost.getHostName());
              scHost.updateActualConfigs(report.getConfigurationTags());
              scHost.setRestartRequired(false);
            }

            if (RoleCommand.CUSTOM_COMMAND.toString().equals(report.getRoleCommand()) &&
                !("START".equals(report.getCustomCommand()) ||
                 "STOP".equals(report.getCustomCommand()))) {
              //do not affect states for custom commands except START and STOP
              //lets status commands to be responsible for this
              continue;
            }

            if (RoleCommand.START.toString().equals(report.getRoleCommand()) ||
                (RoleCommand.CUSTOM_COMMAND.toString().equals(report.getRoleCommand()) &&
                    "START".equals(report.getCustomCommand()))) {
              scHost.handleEvent(new ServiceComponentHostStartedEvent(schName,
                  hostname, now));
              scHost.setRestartRequired(false);
            } else if (RoleCommand.STOP.toString().equals(report.getRoleCommand()) ||
                (RoleCommand.CUSTOM_COMMAND.toString().equals(report.getRoleCommand()) &&
                    "STOP".equals(report.getCustomCommand()))) {
              scHost.handleEvent(new ServiceComponentHostStoppedEvent(schName,
                  hostname, now));
            } else {
              scHost.handleEvent(new ServiceComponentHostOpSucceededEvent(schName,
                  hostname, now));
            }
          } else if (report.getStatus().equals("FAILED")) {
            LOG.warn("Operation failed - may be retried. Service component host: "
                + schName + ", host: " + hostname + " Action id" + report.getActionId());
            if (actionManager.isInProgressCommand(report)) {
              scHost.handleEvent(new ServiceComponentHostOpFailedEvent
                (schName, hostname, now));
            } else {
              LOG.info("Received report for a command that is no longer active. " + report);
            }
          } else if (report.getStatus().equals("IN_PROGRESS")) {
            scHost.handleEvent(new ServiceComponentHostOpInProgressEvent(schName,
                hostname, now));
          }
        } catch (ServiceComponentNotFoundException scnex) {
          LOG.warn("Service component not found ", scnex);
        } catch (InvalidStateTransitionException ex) {
          if (LOG.isDebugEnabled()) {
            LOG.warn("State machine exception.", ex);
          } else {
            LOG.warn("State machine exception. " + ex.getMessage());
          }
        }
      }
    }
    //Update state machines from reports
    actionManager.processTaskResponse(hostname, reports, commands);
  }

  protected void processStatusReports(HeartBeat heartbeat,
                                      String hostname,
                                      Clusters clusterFsm)
      throws AmbariException {
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
                  || prevState.equals(State.STARTED)
                  || prevState.equals(State.STARTING)
                  || prevState.equals(State.STOPPING)
                  || prevState.equals(State.UNKNOWN)) {
                scHost.setState(liveState); //TODO direct status set breaks state machine sometimes !!!
                if (!prevState.equals(liveState)) {
                  LOG.info("State of service component " + componentName
                      + " of service " + status.getServiceName()
                      + " of cluster " + status.getClusterName()
                      + " has changed from " + prevState + " to " + liveState
                      + " at host " + hostname);
                }
              }

              if (null != status.getStackVersion() && !status.getStackVersion().isEmpty()) {
                scHost.setStackVersion(gson.fromJson(status.getStackVersion(), StackId.class));
              }

              if (null != status.getConfigTags()) {
                scHost.updateActualConfigs(status.getConfigTags());
              }

              Map<String, Object> extra = status.getExtra();
              if (null != extra && !extra.isEmpty()) {
                try {
                  if (extra.containsKey("processes")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> list = (List<Map<String, String>>) extra.get("processes");
                    scHost.setProcesses(list);
                  }

                } catch (Exception e) {
                  LOG.error("Could not access extra JSON for " +
                      scHost.getServiceComponentName() + " from " +
                      scHost.getHostName() + ": " + status.getExtra() +
                      " (" + e.getMessage() + ")");
                }
              }

              if (null != status.getAlerts()) {
                List<Alert> clusterAlerts = new ArrayList<Alert>();
                for (AgentAlert aa : status.getAlerts()) {
                  Alert alert = new Alert(aa.getName(), aa.getInstance(),
                      scHost.getServiceName(), scHost.getServiceComponentName(),
                      scHost.getHostName(), aa.getState());
                  alert.setLabel(aa.getLabel());
                  alert.setText(aa.getText());

                  clusterAlerts.add(alert);
                }

               if (0 != clusterAlerts.size()) {
                cl.addAlerts(clusterAlerts);
              }
              }


            } else {
              // TODO: What should be done otherwise?
            }
          } catch (ServiceNotFoundException e) {
            LOG.warn("Received a live status update for a non-initialized"
                + " service"
                + ", clusterName=" + status.getClusterName()
                + ", serviceName=" + status.getServiceName());
            // FIXME ignore invalid live update and continue for now?
            continue;
          } catch (ServiceComponentNotFoundException e) {
            LOG.warn("Received a live status update for a non-initialized"
                + " servicecomponent"
                + ", clusterName=" + status.getClusterName()
                + ", serviceName=" + status.getServiceName()
                + ", componentName=" + status.getComponentName());
            // FIXME ignore invalid live update and continue for now?
            continue;
          } catch (ServiceComponentHostNotFoundException e) {
            LOG.warn("Received a live status update for a non-initialized"
                + " service"
                + ", clusterName=" + status.getClusterName()
                + ", serviceName=" + status.getServiceName()
                + ", componentName=" + status.getComponentName()
                + ", hostname=" + hostname);
            // FIXME ignore invalid live update and continue for now?
            continue;
          } catch (RuntimeException e) {
            LOG.warn("Received a live status with invalid payload"
                + " service"
                + ", clusterName=" + status.getClusterName()
                + ", serviceName=" + status.getServiceName()
                + ", componentName=" + status.getComponentName()
                + ", hostname=" + hostname
                + ", error=" + e.getMessage());
            continue;
          }
        }
      }
    }
  }

  /**
   * Adds commands from action queue to a heartbeat responce
   */
  protected void sendCommands(String hostname, HeartBeatResponse response)
      throws AmbariException {
    List<AgentCommand> cmds = actionQueue.dequeueAll(hostname);
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
          case BACKGROUND_EXECUTION_COMMAND:
          case EXECUTION_COMMAND: {
            response.addExecutionCommand((ExecutionCommand) ac);
            break;
          }
          case STATUS_COMMAND: {
            response.addStatusCommand((StatusCommand) ac);
            break;
          }
          case CANCEL_COMMAND: {
            response.addCancelCommand((CancelCommand) ac);
            break;
          }
          default:
            LOG.error("There is no action for agent command =" +
                ac.getCommandType().name());
        }
      }
    }
  }

  public String getOsType(String os, String osRelease) {
    String osType = "";
    if (os != null) {
      osType = os;
    }
    if (osRelease != null) {
      String[] release = DOT_PATTERN.split(osRelease);
      if (release.length > 0) {
        osType += release[0];
      }
    }
    return osType.toLowerCase();
  }

  protected HeartBeatResponse createRegisterCommand() {
    HeartBeatResponse response = new HeartBeatResponse();
    RegistrationCommand regCmd = new RegistrationCommand();
    response.setResponseId(0);
    response.setRegistrationCommand(regCmd);
    return response;
  }

  protected HeartBeatResponse createRestartCommand(Long currentResponseId) {
    HeartBeatResponse response = new HeartBeatResponse();
    response.setRestartAgent(true);
    response.setResponseId(currentResponseId);
    return response;
  }

  public RegistrationResponse handleRegistration(Register register)
      throws InvalidStateTransitionException, AmbariException {
    String hostname = register.getHostname();
    int currentPingPort = register.getCurrentPingPort();
    long now = System.currentTimeMillis();

    String agentVersion = register.getAgentVersion();
    String serverVersion = ambariMetaInfo.getServerVersion();
    if (!VersionUtils.areVersionsEqual(serverVersion, agentVersion, true)) {
      LOG.warn("Received registration request from host with non compatible"
          + " agent version"
          + ", hostname=" + hostname
          + ", agentVersion=" + agentVersion
          + ", serverVersion=" + serverVersion);
      throw new AmbariException("Cannot register host with non compatible"
          + " agent version"
          + ", hostname=" + hostname
          + ", agentVersion=" + agentVersion
          + ", serverVersion=" + serverVersion);
    }

    String agentOsType = getOsType(register.getHardwareProfile().getOS(),
        register.getHardwareProfile().getOSRelease());
    if (!ambariMetaInfo.isOsSupported(agentOsType)) {
      LOG.warn("Received registration request from host with not supported"
          + " os type"
          + ", hostname=" + hostname
          + ", serverOsType=" + config.getServerOsType()
          + ", agentOsType=" + agentOsType);
      throw new AmbariException("Cannot register host with not supported"
          + " os type"
          + ", hostname=" + hostname
          + ", serverOsType=" + config.getServerOsType()
          + ", agentOsType=" + agentOsType);
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

    // Set ping port for agent
    hostObject.setCurrentPingPort(currentPingPort);

    // Get status of service components
    List<StatusCommand> cmds = heartbeatMonitor.generateStatusCommands(hostname);

    // Save the prefix of the log file paths
    hostObject.setPrefix(register.getPrefix());

    hostObject.handleEvent(new HostRegistrationRequestEvent(hostname,
        null != register.getPublicHostname() ? register.getPublicHostname() : hostname,
        new AgentVersion(register.getAgentVersion()), now, register.getHardwareProfile(),
        register.getAgentEnv()));
    RegistrationResponse response = new RegistrationResponse();
    if (cmds.isEmpty()) {
      //No status commands needed let the fsm know that status step is done
      hostObject.handleEvent(new HostStatusUpdatesReceivedEvent(hostname,
          now));
    }

    configHelper.invalidateStaleConfigsCache(hostname);

    response.setStatusCommands(cmds);

    response.setResponseStatus(RegistrationStatus.OK);

    Long requestId = 0L;
    hostResponseIds.put(hostname, requestId);
    response.setResponseId(requestId);
    return response;
  }

  /**
   * Annotate the response with some housekeeping details.
   * hasMappedComponents - indicates if any components are mapped to the host
   * @param hostname
   * @param response
   * @throws org.apache.ambari.server.AmbariException
   */
  private void annotateResponse(String hostname, HeartBeatResponse response) throws AmbariException {
    for (Cluster cl : clusterFsm.getClustersForHost(hostname)) {
      List<ServiceComponentHost> scHosts = cl.getServiceComponentHosts(hostname);
      if (scHosts != null && scHosts.size() > 0) {
        response.setHasMappedComponents(true);
        break;
      }
    }
  }

  /**
   * Response contains information about HDP Stack in use
   * @param clusterName
   * @return @ComponentsResponse
   * @throws org.apache.ambari.server.AmbariException
   */
  public ComponentsResponse handleComponents(String clusterName)
      throws AmbariException {
    ComponentsResponse response = new ComponentsResponse();

    Cluster cluster = clusterFsm.getCluster(clusterName);
    StackId stackId = cluster.getCurrentStackVersion();
    if (stackId == null) {
      throw new AmbariException("Cannot provide stack components map. " +
        "Stack hasn't been selected yet.");
    }
    StackInfo stack = ambariMetaInfo.getStackInfo(stackId.getStackName(),
        stackId.getStackVersion());

    response.setClusterName(clusterName);
    response.setStackName(stackId.getStackName());
    response.setStackVersion(stackId.getStackVersion());
    response.setComponents(getComponentsMap(stack));

    return response;
  }

  private Map<String, Map<String, String>> getComponentsMap(StackInfo stack) {
    Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();

    for (ServiceInfo service : stack.getServices()) {
      Map<String, String> components = new HashMap<String, String>();

      for (ComponentInfo component : service.getComponents()) {
        components.put(component.getName(), component.getCategory());
      }

      result.put(service.getName(), components);
    }

    return result;
  }
}
