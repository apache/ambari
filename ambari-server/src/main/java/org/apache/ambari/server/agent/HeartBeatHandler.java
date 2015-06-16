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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import org.apache.ambari.server.Role;
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
import org.apache.ambari.server.events.ActionFinalReportReceivedEvent;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.HostComponentVersionEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalHostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileReader;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileReaderFactory;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
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
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.host.HostRegistrationRequestEvent;
import org.apache.ambari.server.state.host.HostStatusUpdatesReceivedEvent;
import org.apache.ambari.server.state.host.HostUnhealthyHeartbeatEvent;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpSucceededEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStoppedEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;


/**
 * This class handles the heartbeats coming from the agent, passes on the information
 * to other modules and processes the queue to send heartbeat response.
 */
@Singleton
public class HeartBeatHandler {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(HeartBeatHandler.class);

  private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
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
  private HostDAO hostDAO;

  @Inject
  private AlertDefinitionHash alertDefinitionHash;

  /**
   * Publishes {@link AlertEvent} instances.
   */
  @Inject
  private AlertEventPublisher alertEventPublisher;

  @Inject
  private AmbariEventPublisher ambariEventPublisher;

  @Inject
  private VersionEventPublisher versionEventPublisher;


  /**
   * KerberosPrincipalHostDAO used to set and get Kerberos principal details
   */
  @Inject
  private KerberosPrincipalHostDAO kerberosPrincipalHostDAO;

  /**
   * KerberosIdentityDataFileReaderFactory used to create KerberosIdentityDataFileReader instances
   */
  @Inject
  private KerberosIdentityDataFileReaderFactory kerberosIdentityDataFileReaderFactory;

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

    if (heartbeat.getRecoveryReport() != null) {
      RecoveryReport rr = heartbeat.getRecoveryReport();
      processRecoveryReport(rr, hostname);
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
      LOG.warn("Asking agent to re-register due to " + ex.getMessage(), ex);
      hostObject.setState(HostState.INIT);
      return createRegisterCommand();
    }

    // Examine heartbeat for command reports
    processCommandReports(heartbeat, hostname, clusterFsm, now);

    // Examine heartbeat for component live status reports
    processStatusReports(heartbeat, hostname, clusterFsm);

    // Calculate host status
    // NOTE: This step must be after processing command/status reports
    processHostStatus(heartbeat, hostname);

    // Example heartbeat for alerts from the host or its components
    processAlerts(heartbeat, hostname);

    // Send commands if node is active
    if (hostObject.getState().equals(HostState.HEALTHY)) {
      sendCommands(hostname, response);
      annotateResponse(hostname, response);
    }

    return response;
  }

  /**
   * Extracts all of the {@link Alert}s from the heartbeat and fires
   * {@link AlertEvent}s for each one. If there is a problem looking up the
   * cluster, then alerts will not be processed.
   *
   * @param heartbeat
   *          the heartbeat to process.
   * @param hostname
   *          the host that the heartbeat is for.
   */
  protected void processAlerts(HeartBeat heartbeat, String hostname) {

    if (null == hostname || null == heartbeat) {
      return;
    }

    if (null != heartbeat.getAlerts()) {
      AlertEvent event = new AlertReceivedEvent(heartbeat.getAlerts());
      for (Alert alert : event.getAlerts()) {
        if (alert.getHostName() == null) {
          alert.setHostName(hostname);
        }
      }
      alertEventPublisher.publish(event);

    }
  }

  protected void processRecoveryReport(RecoveryReport recoveryReport, String hostname) throws AmbariException {
    LOG.debug("Received recovery report: " + recoveryReport.toString());
    Host host = clusterFsm.getHost(hostname);
    host.setRecoveryReport(recoveryReport);
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

      //If host doesn't belong to any cluster
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

      Long clusterId = null;
      if (report.getClusterName() != null) {
        try {
          Cluster cluster = clusterFsm.getCluster(report.getClusterName());
          clusterId = Long.valueOf(cluster.getClusterId());
        } catch (AmbariException e) {
        }
      }

      LOG.debug("Received command report: " + report);
      // Fetch HostRoleCommand that corresponds to a given task ID
      HostRoleCommand hostRoleCommand = hostRoleCommandIterator.next();
      HostEntity hostEntity = hostDAO.findByName(hostname);
      if (hostEntity == null) {
        LOG.error("Received a command report and was unable to retrieve HostEntity for hostname = " + hostname);
        continue;
      }

      // Send event for final command reports for actions
      if (RoleCommand.valueOf(report.getRoleCommand()) == RoleCommand.ACTIONEXECUTE &&
          HostRoleStatus.valueOf(report.getStatus()).isCompletedState()) {
        ActionFinalReportReceivedEvent event = new ActionFinalReportReceivedEvent(
                clusterId, hostname, report, report.getRole());
        ambariEventPublisher.publish(event);
      }

      // Skip sending events for command reports for ABORTed commands
      if (hostRoleCommand.getStatus() == HostRoleStatus.ABORTED) {
        continue;
      }
      if (hostRoleCommand.getStatus() == HostRoleStatus.QUEUED &&
              report.getStatus().equals("IN_PROGRESS")) {
        hostRoleCommand.setStartTime(now);
      }

      // If the report indicates the keytab file was successfully transferred to a host or removed
      // from a host, record this for future reference
      if (Service.Type.KERBEROS.name().equalsIgnoreCase(report.getServiceName()) &&
          Role.KERBEROS_CLIENT.name().equalsIgnoreCase(report.getRole()) &&
          RoleCommand.CUSTOM_COMMAND.name().equalsIgnoreCase(report.getRoleCommand()) &&
          RequestExecution.Status.COMPLETED.name().equalsIgnoreCase(report.getStatus())) {

        String customCommand = report.getCustomCommand();

        boolean adding = "SET_KEYTAB".equalsIgnoreCase(customCommand);
        if (adding || "REMOVE_KEYTAB".equalsIgnoreCase(customCommand)) {
          WriteKeytabsStructuredOut writeKeytabsStructuredOut;
          try {
            writeKeytabsStructuredOut = gson.fromJson(report.getStructuredOut(), WriteKeytabsStructuredOut.class);
          } catch (JsonSyntaxException ex) {
            //Json structure was incorrect do nothing, pass this data further for processing
            writeKeytabsStructuredOut = null;
          }

          if (writeKeytabsStructuredOut != null) {
            Map<String, String> keytabs = writeKeytabsStructuredOut.getKeytabs();
            if (keytabs != null) {
              for (Map.Entry<String, String> entry : keytabs.entrySet()) {
                String principal = entry.getKey();
                if (!kerberosPrincipalHostDAO.exists(principal, hostEntity.getHostId())) {
                  if (adding) {
                    kerberosPrincipalHostDAO.create(principal, hostEntity.getHostId());
                  } else if ("_REMOVED_".equalsIgnoreCase(entry.getValue())) {
                    kerberosPrincipalHostDAO.remove(principal, hostEntity.getHostId());
                  }
                }
              }
            }
          }
        }
      }

      //pass custom START, STOP and RESTART
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

          if (report.getStatus().equals(HostRoleStatus.COMPLETED.toString())) {

            // Reading component version if it is present
            if (StringUtils.isNotBlank(report.getStructuredOut())) {
              ComponentVersionStructuredOut structuredOutput = null;
              try {
                structuredOutput = gson.fromJson(report.getStructuredOut(), ComponentVersionStructuredOut.class);
              } catch (JsonSyntaxException ex) {
                //Json structure for component version was incorrect
                //do nothing, pass this data further for processing
              }

              String newVersion = structuredOutput == null ? null : structuredOutput.getVersion();

              // Pass true to always publish a version event.  It is safer to recalculate the version even if we don't
              // detect a difference in the value.  This is useful in case that a manual database edit is done while
              // ambari-server is stopped.
              handleComponentVersionReceived(cl, scHost, newVersion, true);
            }

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
            // Necessary for resetting clients stale configs after starting service
            if ((RoleCommand.INSTALL.toString().equals(report.getRoleCommand()) ||
                (RoleCommand.CUSTOM_COMMAND.toString().equals(report.getRoleCommand()) &&
                "INSTALL".equals(report.getCustomCommand()))) && svcComp.isClientComponent()){
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

              SecurityState prevSecurityState = scHost.getSecurityState();
              SecurityState currentSecurityState = SecurityState.valueOf(status.getSecurityState());
              if((prevSecurityState != currentSecurityState)) {
                if(prevSecurityState.isEndpoint()) {
                  scHost.setSecurityState(currentSecurityState);
                  LOG.info(String.format("Security of service component %s of service %s of cluster %s " +
                          "has changed from %s to %s on host %s",
                      componentName, status.getServiceName(), status.getClusterName(), prevSecurityState,
                      currentSecurityState, hostname));
                }
                else {
                  LOG.debug(String.format("Security of service component %s of service %s of cluster %s " +
                          "has changed from %s to %s on host %s but will be ignored since %s is a " +
                          "transitional state",
                      componentName, status.getServiceName(), status.getClusterName(),
                      prevSecurityState, currentSecurityState, hostname, prevSecurityState));
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
                  if (extra.containsKey("version")) {
                    String version = extra.get("version").toString();

                    handleComponentVersionReceived(cl, scHost, version, false);
                  }

                } catch (Exception e) {
                  LOG.error("Could not access extra JSON for " +
                      scHost.getServiceComponentName() + " from " +
                      scHost.getHostName() + ": " + status.getExtra() +
                      " (" + e.getMessage() + ")");
                }
              }

              this.heartbeatMonitor.getAgentRequests()
                  .setExecutionDetailsRequest(hostname, componentName, status.getSendExecCmdDet());
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
   * Updates the version of the given service component, sets the upgrade state (if needed)
   * and publishes a version event through the version event publisher.
   *
   * @param cluster        the cluster
   * @param scHost         service component host
   * @param newVersion     new version of service component
   * @param alwaysPublish  if true, always publish a version event; if false,
   *                       only publish if the component version was updated
   */
  private void handleComponentVersionReceived(Cluster cluster, ServiceComponentHost scHost,
                                              String newVersion, boolean alwaysPublish) {

    boolean updated = false;

    if (StringUtils.isNotBlank(newVersion)) {
      final String previousVersion = scHost.getVersion();
      if (!StringUtils.equals(previousVersion, newVersion)) {
        scHost.setVersion(newVersion);
        scHost.setStackVersion(cluster.getDesiredStackVersion());
        if (previousVersion != null && !previousVersion.equalsIgnoreCase(State.UNKNOWN.toString())) {
          scHost.setUpgradeState(UpgradeState.COMPLETE);
        }
        updated = true;
      }
    }

    if (updated || alwaysPublish) {
      HostComponentVersionEvent event = new HostComponentVersionEvent(cluster, scHost);
      versionEventPublisher.publish(event);
    }
  }

  /**
   * Adds commands from action queue to a heartbeat response.
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
            ExecutionCommand ec = (ExecutionCommand)ac;
            Map<String, String> hlp = ec.getHostLevelParams();
            if (hlp != null) {
              String customCommand = hlp.get("custom_command");
              if ("SET_KEYTAB".equalsIgnoreCase(customCommand) || "REMOVE_KEYTAB".equalsIgnoreCase(customCommand)) {
                LOG.info(String.format("%s called", customCommand));
                try {
                  injectKeytab(ec, customCommand, hostname);
                } catch (IOException e) {
                  throw new AmbariException("Could not inject keytab into command", e);
                }
              }
            }
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
          case ALERT_DEFINITION_COMMAND: {
            response.addAlertDefinitionCommand((AlertDefinitionCommand) ac);
            break;
          }
          case ALERT_EXECUTION_COMMAND: {
            response.addAlertExecutionCommand((AlertExecutionCommand) ac);
            break;
          }
          default:
            LOG.error("There is no action for agent command ="
                + ac.getCommandType().name());
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
    LOG.info("agentOsType = "+agentOsType );
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

    // Add request for component version
    for (StatusCommand command: cmds) {
      command.getCommandParams().put("request_version", String.valueOf(true));
    }

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

    // force the registering agent host to receive its list of alert definitions
    List<AlertDefinitionCommand> alertDefinitionCommands = getRegistrationAlertDefinitionCommands(hostname);
    response.setAlertDefinitionCommands(alertDefinitionCommands);

    response.setAgentConfig(config.getAgentConfigsMap());
    if(response.getAgentConfig() != null) {
      LOG.debug("Agent configuration map set to " + response.getAgentConfig());
    }
    response.setRecoveryConfig(RecoveryConfig.getRecoveryConfig(config));
    if(response.getRecoveryConfig() != null) {
      LOG.debug("Recovery configuration set to " + response.getRecoveryConfig().toString());
    }

    Long requestId = 0L;
    hostResponseIds.put(hostname, requestId);
    response.setResponseId(requestId);
    return response;
  }

  /**
   * Annotate the response with some housekeeping details.
   * hasMappedComponents - indicates if any components are mapped to the host
   * hasPendingTasks - indicates if any tasks are pending for the host (they may not be sent yet)
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

    if(actionQueue.hasPendingTask(hostname)) {
      LOG.debug("Host " + hostname + " has pending tasks");
      response.setHasPendingTasks(true);
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
    StackInfo stack = ambariMetaInfo.getStack(stackId.getStackName(),
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

  /**
   * Gets the {@link AlertDefinitionCommand} instances that need to be sent for
   * each cluster that the registering host is a member of.
   *
   * @param hostname
   * @return
   * @throws AmbariException
   */
  private List<AlertDefinitionCommand> getRegistrationAlertDefinitionCommands(
      String hostname) throws AmbariException {

    Set<Cluster> hostClusters = clusterFsm.getClustersForHost(hostname);
    if (null == hostClusters || hostClusters.size() == 0) {
      return null;
    }

    List<AlertDefinitionCommand> commands = new ArrayList<AlertDefinitionCommand>();

    // for every cluster this host is a member of, build the command
    for (Cluster cluster : hostClusters) {
      String clusterName = cluster.getClusterName();
      alertDefinitionHash.invalidate(clusterName, hostname);

      List<AlertDefinition> definitions = alertDefinitionHash.getAlertDefinitions(
          clusterName, hostname);

      String hash = alertDefinitionHash.getHash(clusterName, hostname);
      AlertDefinitionCommand command = new AlertDefinitionCommand(clusterName,
          hostname, hash, definitions);

      command.addConfigs(configHelper, cluster);
      commands.add(command);
    }

    return commands;
  }

  /**
   * Insert Kerberos keytab details into the ExecutionCommand for the SET_KEYTAB custom command if
   * any keytab details and associated data exists for the target host.
   *
   * @param ec the ExecutionCommand to update
   * @param command a name of the relevant keytab command
   * @param targetHost a name of the host the relevant command is destined for
   * @throws AmbariException
   */
  void injectKeytab(ExecutionCommand ec, String command, String targetHost) throws AmbariException {
    String dataDir = ec.getCommandParams().get(KerberosServerAction.DATA_DIRECTORY);

    if(dataDir != null) {
      KerberosIdentityDataFileReader reader = null;
      List<Map<String, String>> kcp = ec.getKerberosCommandParams();

      try {
        reader = kerberosIdentityDataFileReaderFactory.createKerberosIdentityDataFileReader(new File(dataDir, KerberosIdentityDataFileReader.DATA_FILE_NAME));

        for (Map<String, String> record : reader) {
          String hostName = record.get(KerberosIdentityDataFileReader.HOSTNAME);

          if (targetHost.equalsIgnoreCase(hostName)) {

            if ("SET_KEYTAB".equalsIgnoreCase(command)) {
              String keytabFilePath = record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH);

              if (keytabFilePath != null) {

                String sha1Keytab = DigestUtils.sha1Hex(keytabFilePath);
                File keytabFile = new File(dataDir + File.separator + hostName + File.separator + sha1Keytab);

                if (keytabFile.canRead()) {
                  Map<String, String> keytabMap = new HashMap<String, String>();
                  String principal = record.get(KerberosIdentityDataFileReader.PRINCIPAL);
                  String isService = record.get(KerberosIdentityDataFileReader.SERVICE);

                  keytabMap.put(KerberosIdentityDataFileReader.HOSTNAME, hostName);
                  keytabMap.put(KerberosIdentityDataFileReader.SERVICE, isService);
                  keytabMap.put(KerberosIdentityDataFileReader.COMPONENT, record.get(KerberosIdentityDataFileReader.COMPONENT));
                  keytabMap.put(KerberosIdentityDataFileReader.PRINCIPAL, principal);
                  keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH, keytabFilePath);
                  keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME));
                  keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS));
                  keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_NAME, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_NAME));
                  keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS));

                  BufferedInputStream bufferedIn = new BufferedInputStream(new FileInputStream(keytabFile));
                  byte[] keytabContent = null;
                  try {
                    keytabContent = IOUtils.toByteArray(bufferedIn);
                  } finally {
                    bufferedIn.close();
                  }
                  String keytabContentBase64 = Base64.encodeBase64String(keytabContent);
                  keytabMap.put(KerberosServerAction.KEYTAB_CONTENT_BASE64, keytabContentBase64);

                  kcp.add(keytabMap);
                }
              }
            } else if ("REMOVE_KEYTAB".equalsIgnoreCase(command)) {
              Map<String, String> keytabMap = new HashMap<String, String>();

              keytabMap.put(KerberosIdentityDataFileReader.HOSTNAME, hostName);
              keytabMap.put(KerberosIdentityDataFileReader.SERVICE, record.get(KerberosIdentityDataFileReader.SERVICE));
              keytabMap.put(KerberosIdentityDataFileReader.COMPONENT, record.get(KerberosIdentityDataFileReader.COMPONENT));
              keytabMap.put(KerberosIdentityDataFileReader.PRINCIPAL, record.get(KerberosIdentityDataFileReader.PRINCIPAL));
              keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH));

              kcp.add(keytabMap);
            }
          }
        }
      } catch (IOException e) {
        throw new AmbariException("Could not inject keytabs to enable kerberos");
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (Throwable t) {
            // ignored
          }
        }
      }

      ec.setKerberosCommandParams(kcp);
    }
  }

  /**
   * This class is used for mapping json of structured output for component START action.
   */
  private static class ComponentVersionStructuredOut {
    @SerializedName("version")
    private String version;

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }
  }

  /**
   * This class is used for mapping json of structured output for keytab distribution actions.
   */
  private static class WriteKeytabsStructuredOut {
    @SerializedName("keytabs")
    private Map<String,String> keytabs;

    public Map<String, String> getKeytabs() {
      return keytabs;
    }

    public void setKeytabs(Map<String, String> keytabs) {
      this.keytabs = keytabs;
    }
  }

}
