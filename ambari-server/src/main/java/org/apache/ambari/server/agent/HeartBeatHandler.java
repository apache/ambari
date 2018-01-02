/*
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

import static org.apache.ambari.server.controller.KerberosHelperImpl.CHECK_KEYTABS;
import static org.apache.ambari.server.controller.KerberosHelperImpl.REMOVE_KEYTAB;
import static org.apache.ambari.server.controller.KerberosHelperImpl.SET_KEYTAB;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileReader;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.stageutils.KerberosKeytabController;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.host.HostRegistrationRequestEvent;
import org.apache.ambari.server.state.host.HostStatusUpdatesReceivedEvent;
import org.apache.ambari.server.state.host.HostUnhealthyHeartbeatEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private HeartbeatProcessor heartbeatProcessor;

  @Inject
  private Configuration config;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private AlertDefinitionHash alertDefinitionHash;

  @Inject
  private RecoveryConfigHelper recoveryConfigHelper;

  @Inject
  private KerberosKeytabController kerberosKeytabController;

  private Map<String, Long> hostResponseIds = new ConcurrentHashMap<>();

  private Map<String, HeartBeatResponse> hostResponses = new ConcurrentHashMap<>();

  @Inject
  public HeartBeatHandler(Clusters fsm, ActionQueue aq, ActionManager am,
                          Injector injector) {
    clusterFsm = fsm;
    actionQueue = aq;
    actionManager = am;
    heartbeatMonitor = new HeartbeatMonitor(fsm, aq, am, 60000, injector);
    heartbeatProcessor = new HeartbeatProcessor(fsm, am, heartbeatMonitor, injector); //TODO modify to match pattern
    injector.injectMembers(this);
  }

  public void start() {
    heartbeatProcessor.startAsync();
    heartbeatMonitor.start();
  }

  void setHeartbeatMonitor(HeartbeatMonitor heartbeatMonitor) {
    this.heartbeatMonitor = heartbeatMonitor;
  }

  public void setHeartbeatProcessor(HeartbeatProcessor heartbeatProcessor) {
    this.heartbeatProcessor = heartbeatProcessor;
  }

  public HeartbeatProcessor getHeartbeatProcessor() {
    return heartbeatProcessor;
  }

  public HeartBeatResponse handleHeartBeat(HeartBeat heartbeat)
      throws AmbariException {
    long now = System.currentTimeMillis();
    if (heartbeat.getAgentEnv() != null && heartbeat.getAgentEnv().getHostHealth() != null) {
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

    LOG.debug("Received heartbeat from host, hostname={}, currentResponseId={}, receivedResponseId={}", hostname, currentResponseId, heartbeat.getResponseId());

    if (heartbeat.getResponseId() == currentResponseId - 1) {
      HeartBeatResponse heartBeatResponse = hostResponses.get(hostname);

      LOG.warn("Old responseId={} received form host {} - response was lost - returning cached response with responseId={}",
        heartbeat.getResponseId(),
        hostname,
        heartBeatResponse.getResponseId());

      return heartBeatResponse;
    } else if (heartbeat.getResponseId() != currentResponseId) {
      LOG.error("Error in responseId sequence - received responseId={} from host {} - sending agent restart command with responseId={}",
        heartbeat.getResponseId(),
        hostname,
        currentResponseId);

      return createRestartCommand(currentResponseId);
    }

    response = new HeartBeatResponse();
    response.setResponseId(++currentResponseId);

    Host hostObject;
    try {
      hostObject = clusterFsm.getHost(hostname);
    } catch (HostNotFoundException e) {
      LOG.error("Host: {} not found. Agent is still heartbeating.", hostname);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Host associated with the agent heratbeat might have been " +
          "deleted", e);
      }
      // For now return empty response with only response id.
      return response;
    }

    if (hostObject.getState().equals(HostState.HEARTBEAT_LOST)) {
      // After loosing heartbeat agent should reregister
      LOG.warn("Host {} is in HEARTBEAT_LOST state - sending register command", hostname);
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

    /*
     * A host can belong to only one cluster. Though getClustersForHost(hostname)
     * returns a set of clusters, it will have only one entry.
     *
     * TODO: Handle the case when a host is a part of multiple clusters.
     */
    Set<Cluster> clusters = clusterFsm.getClustersForHost(hostname);
    if (clusters.size() > 0) {
      String clusterName = clusters.iterator().next().getClusterName();

      if (recoveryConfigHelper.isConfigStale(clusterName, hostname, heartbeat.getRecoveryTimestamp())) {
        RecoveryConfig rc = recoveryConfigHelper.getRecoveryConfig(clusterName, hostname);
        response.setRecoveryConfig(rc);

        if (response.getRecoveryConfig() != null) {
          LOG.info("Recovery configuration set to {}", response.getRecoveryConfig());
        }
      }
    }

    heartbeatProcessor.addHeartbeat(heartbeat);

    // Send commands if node is active
    if (hostObject.getState().equals(HostState.HEALTHY)) {
      sendCommands(hostname, response);
      annotateResponse(hostname, response);
    }

    return response;
  }



  protected void processRecoveryReport(RecoveryReport recoveryReport, String hostname) throws AmbariException {
    LOG.debug("Received recovery report: {}", recoveryReport);
    Host host = clusterFsm.getHost(hostname);
    host.setRecoveryReport(recoveryReport);
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
            LOG.debug("Sending command string = {}", StageUtils.jaxbToString(ac));
          }
        } catch (Exception e) {
          throw new AmbariException("Could not get jaxb string for command", e);
        }
        switch (ac.getCommandType()) {
          case BACKGROUND_EXECUTION_COMMAND:
          case EXECUTION_COMMAND: {
            ExecutionCommand ec = (ExecutionCommand)ac;
            LOG.info("HeartBeatHandler.sendCommands: sending ExecutionCommand for host {}, role {}, roleCommand {}, and command ID {}, task ID {}",
                     ec.getHostname(), ec.getRole(), ec.getRoleCommand(), ec.getCommandId(), ec.getTaskId());
            Map<String, String> hlp = ec.getHostLevelParams();
            if (hlp != null) {
              String customCommand = hlp.get("custom_command");
              if (SET_KEYTAB.equalsIgnoreCase(customCommand) || REMOVE_KEYTAB.equalsIgnoreCase(customCommand) || CHECK_KEYTABS.equalsIgnoreCase(customCommand)) {
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

    response.setStatusCommands(cmds);

    response.setResponseStatus(RegistrationStatus.OK);

    // force the registering agent host to receive its list of alert definitions
    List<AlertDefinitionCommand> alertDefinitionCommands = getRegistrationAlertDefinitionCommands(hostname);
    response.setAlertDefinitionCommands(alertDefinitionCommands);

    response.setAgentConfig(config.getAgentConfigsMap());
    if(response.getAgentConfig() != null) {
      LOG.debug("Agent configuration map set to {}", response.getAgentConfig());
    }

    /*
     * A host can belong to only one cluster. Though getClustersForHost(hostname)
     * returns a set of clusters, it will have only one entry.
     *
     * TODO: Handle the case when a host is a part of multiple clusters.
     */
    Set<Cluster> clusters = clusterFsm.getClustersForHost(hostname);

    if (clusters.size() > 0) {
      String clusterName = clusters.iterator().next().getClusterName();

      RecoveryConfig rc = recoveryConfigHelper.getRecoveryConfig(clusterName, hostname);
      response.setRecoveryConfig(rc);

      if(response.getRecoveryConfig() != null) {
        LOG.info("Recovery configuration set to " + response.getRecoveryConfig());
      }
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
   * clusterSize - indicates the number of hosts that form the cluster
   * @param hostname
   * @param response
   * @throws org.apache.ambari.server.AmbariException
   */
  private void annotateResponse(String hostname, HeartBeatResponse response) throws AmbariException {
    for (Cluster cl : clusterFsm.getClustersForHost(hostname)) {
      response.setClusterSize(cl.getClusterSize());

      List<ServiceComponentHost> scHosts = cl.getServiceComponentHosts(hostname);
      if (scHosts != null && scHosts.size() > 0) {
        response.setHasMappedComponents(true);
        break;
      }
    }

    if(actionQueue.hasPendingTask(hostname)) {
      LOG.debug("Host {} has pending tasks", hostname);
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

    Map<String, Map<String, String>> componentsMap = new HashMap<>();

    for (org.apache.ambari.server.state.Service service : cluster.getServices().values()) {
      componentsMap.put(service.getName(), new HashMap<>());

      for (ServiceComponent component : service.getServiceComponents().values()) {
        StackId stackId = component.getDesiredStackId();

        ComponentInfo componentInfo = ambariMetaInfo.getComponent(
            stackId.getStackName(), stackId.getStackVersion(), service.getName(), component.getName());

        componentsMap.get(service.getName()).put(component.getName(), componentInfo.getCategory());
      }
    }

    response.setClusterName(clusterName);
    response.setComponents(componentsMap);

    return response;
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

    List<AlertDefinitionCommand> commands = new ArrayList<>();

    // for every cluster this host is a member of, build the command
    for (Cluster cluster : hostClusters) {
      String clusterName = cluster.getClusterName();
      alertDefinitionHash.invalidate(clusterName, hostname);

      List<AlertDefinition> definitions = alertDefinitionHash.getAlertDefinitions(
          clusterName, hostname);

      String hash = alertDefinitionHash.getHash(clusterName, hostname);
      Host host = cluster.getHost(hostname);
      String publicHostName = host == null? hostname : host.getPublicHostName();
      AlertDefinitionCommand command = new AlertDefinitionCommand(clusterName,
          hostname, publicHostName, hash, definitions);

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
    KerberosServerAction.KerberosCommandParameters kerberosCommandParameters = new KerberosServerAction.KerberosCommandParameters(ec);
    if(dataDir != null) {
      List<Map<String, String>> kcp = ec.getKerberosCommandParams();

      try {
        Set<ResolvedKerberosKeytab> keytabsToInject = kerberosKeytabController.getFilteredKeytabs((Map<String, Collection<String>>)kerberosCommandParameters.getServiceComponentFilter(), kerberosCommandParameters.getHostFilter(), kerberosCommandParameters.getIdentityFilter());
        for (ResolvedKerberosKeytab resolvedKeytab : keytabsToInject) {
          for(ResolvedKerberosPrincipal resolvedPrincipal: resolvedKeytab.getPrincipals()) {
            String hostName = resolvedPrincipal.getHostName();

            if (targetHost.equalsIgnoreCase(hostName)) {

              if (SET_KEYTAB.equalsIgnoreCase(command)) {
                String keytabFilePath = resolvedKeytab.getFile();

                if (keytabFilePath != null) {

                  String sha1Keytab = DigestUtils.sha256Hex(keytabFilePath);
                  File keytabFile = new File(dataDir + File.separator + hostName + File.separator + sha1Keytab);

                  if (keytabFile.canRead()) {
                    Map<String, String> keytabMap = new HashMap<>();
                    String principal = resolvedPrincipal.getPrincipal();

                    keytabMap.put(KerberosIdentityDataFileReader.HOSTNAME, hostName);
                    keytabMap.put(KerberosIdentityDataFileReader.PRINCIPAL, principal);
                    keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH, keytabFilePath);
                    keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME, resolvedKeytab.getOwnerName());
                    keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS, resolvedKeytab.getOwnerAccess());
                    keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_NAME, resolvedKeytab.getGroupName());
                    keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS, resolvedKeytab.getGroupAccess());

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
              } else if (REMOVE_KEYTAB.equalsIgnoreCase(command) || CHECK_KEYTABS.equalsIgnoreCase(command)) {
                Map<String, String> keytabMap = new HashMap<>();
                String keytabFilePath = resolvedKeytab.getFile();

                String principal = resolvedPrincipal.getPrincipal();
                for (Map.Entry<String, String> mappingEntry: resolvedPrincipal.getServiceMapping().entries()) {
                  String serviceName = mappingEntry.getKey();
                  String componentName = mappingEntry.getValue();
                  keytabMap.put(KerberosIdentityDataFileReader.HOSTNAME, hostName);
                  keytabMap.put(KerberosIdentityDataFileReader.SERVICE, serviceName);
                  keytabMap.put(KerberosIdentityDataFileReader.COMPONENT, componentName);
                  keytabMap.put(KerberosIdentityDataFileReader.PRINCIPAL, principal);
                  keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH, keytabFilePath);

                }

                kcp.add(keytabMap);
              }
            }
          }
        }
      } catch (IOException e) {
        throw new AmbariException("Could not inject keytabs to enable kerberos");
      }
      ec.setKerberosCommandParams(kcp);
    }
  }

  public void stop() {
    heartbeatMonitor.shutdown();
    heartbeatProcessor.stopAsync();
  }
}
