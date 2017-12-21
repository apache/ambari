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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.events.ActionFinalReportReceivedEvent;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.HostComponentVersionAdvertisedEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.KerberosKeytabDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpSucceededEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStoppedEvent;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * HeartbeatProcessor class is used for bulk processing data retrieved from agents in background
 */
public class HeartbeatProcessor extends AbstractService{
  private static final Logger LOG = LoggerFactory.getLogger(HeartbeatProcessor.class);

  private ScheduledExecutorService executor;

  private ConcurrentLinkedQueue<HeartBeat> heartBeatsQueue = new ConcurrentLinkedQueue<>();

  private volatile boolean shouldRun = true;

  //TODO rewrite to correlate with heartbeat frequency, hardcoded in agent as of now
  private long delay = 5000;
  private long period = 1000;

  private int poolSize = 1;

  private Clusters clusterFsm;
  private HeartbeatMonitor heartbeatMonitor;
  private Injector injector;
  private ActionManager actionManager;

  /**
   * Publishes {@link AlertEvent} instances.
   */
  @Inject
  AlertEventPublisher alertEventPublisher;

  @Inject
  AmbariEventPublisher ambariEventPublisher;

  @Inject
  VersionEventPublisher versionEventPublisher;

  @Inject
  ActionMetadata actionMetadata;

  @Inject
  MaintenanceStateHelper maintenanceStateHelper;

  @Inject
  AmbariMetaInfo ambariMetaInfo;

  @Inject
  KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO;

  @Inject
  KerberosKeytabDAO kerberosKeytabDAO;

  @Inject
  Gson gson;

  @Inject
  public HeartbeatProcessor(Clusters clusterFsm, ActionManager am, HeartbeatMonitor heartbeatMonitor,
                            Injector injector) {
    injector.injectMembers(this);

    this.injector = injector;
    this.heartbeatMonitor = heartbeatMonitor;
    this.clusterFsm = clusterFsm;
    actionManager = am;
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("ambari-heartbeat-processor-%d").build();
    executor = Executors.newScheduledThreadPool(poolSize, threadFactory);
  }

  @Override
  protected void doStart() {
    LOG.info("**** Starting heartbeats processing threads ****");
    for (int i = 0; i < poolSize; i++) {
      executor.scheduleAtFixedRate(new HeartbeatProcessingTask(), delay, period, TimeUnit.MILLISECONDS);
    }
  }

  @Override
  protected void doStop() {
    LOG.info("**** Stopping heartbeats processing threads ****");
    shouldRun = false;
    executor.shutdown();
  }

  public void addHeartbeat(HeartBeat heartBeat) {
    heartBeatsQueue.add(heartBeat);
  }

  private HeartBeat pollHeartbeat() {
    return heartBeatsQueue.poll();
  }

  /**
   * Processing task to be scheduled for execution
   */
  private class HeartbeatProcessingTask implements Runnable {

    @Override
    public void run() {
      while (shouldRun) {
        try {
          HeartBeat heartbeat = pollHeartbeat();
          if (heartbeat == null) {
            break;
          }
          processHeartbeat(heartbeat);
        } catch (Exception e) {
          LOG.error("Exception received while processing heartbeat", e);
        } catch (Throwable throwable) {
          //catch everything to prevent task suppression
          LOG.error("ERROR: ", throwable);
        }


      }
    }
  }

  /**
   * Incapsulates logic for processing data from agent heartbeat
   *
   * @param heartbeat Agent heartbeat object
   * @throws AmbariException
   */
  public void processHeartbeat(HeartBeat heartbeat) throws AmbariException {
    long now = System.currentTimeMillis();

    processAlerts(heartbeat);

    //process status reports before command reports to prevent status override immediately after task finish
    processStatusReports(heartbeat);
    processCommandReports(heartbeat, now);
    //host status calculation are based on task and status reports, should be performed last
    processHostStatus(heartbeat);
  }


  /**
   * Extracts all of the {@link Alert}s from the heartbeat and fires
   * {@link AlertEvent}s for each one. If there is a problem looking up the
   * cluster, then alerts will not be processed.
   *
   * @param heartbeat the heartbeat to process.
   */
  protected void processAlerts(HeartBeat heartbeat) {
    if (heartbeat == null) {
      return;
    }

    String hostname = heartbeat.getHostname();

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

  /**
   * Update host status basing on components statuses
   *
   * @param heartbeat heartbeat to process
   * @throws AmbariException
   */
  protected void processHostStatus(HeartBeat heartbeat) throws AmbariException {

    String hostname = heartbeat.getHostname();
    Host host = clusterFsm.getHost(hostname);
    HostHealthStatus.HealthStatus healthStatus = host.getHealthStatus().getHealthStatus();

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

        Cluster cluster = clusterFsm.getCluster(clusterName);


        List<ServiceComponentHost> scHosts = cluster.getServiceComponentHosts(heartbeat.getHostname());
        for (ServiceComponentHost scHost : scHosts) {
          StackId stackId = scHost.getDesiredStackId();

          ComponentInfo componentInfo =
              ambariMetaInfo.getComponent(stackId.getStackName(),
                  stackId.getStackVersion(), scHost.getServiceName(),
                  scHost.getServiceComponentName());

          String status = scHost.getState().name();

          String category = componentInfo.getCategory();

          if (MaintenanceState.OFF == maintenanceStateHelper.getEffectiveState(scHost, host)) {
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
          healthStatus = HostHealthStatus.HealthStatus.HEALTHY;
        } else if (masterCount > 0 && mastersRunning < masterCount) {
          healthStatus = HostHealthStatus.HealthStatus.UNHEALTHY;
        } else {
          healthStatus = HostHealthStatus.HealthStatus.ALERT;
        }

        host.setStatus(healthStatus.name());
      }

      //If host doesn't belong to any cluster
      if ((clusterFsm.getClustersForHost(host.getHostName())).size() == 0) {
        healthStatus = HostHealthStatus.HealthStatus.HEALTHY;
        host.setStatus(healthStatus.name());
      }
    }
  }

  /**
   * Process reports of tasks executed on agents
   *
   * @param heartbeat heartbeat to process
   * @param now       cached current time
   * @throws AmbariException
   */
  protected void processCommandReports(
      HeartBeat heartbeat, long now)
      throws AmbariException {
    String hostname = heartbeat.getHostname();
    List<CommandReport> reports = heartbeat.getReports();

    // Cache HostRoleCommand entities because we will need them few times
    List<Long> taskIds = new ArrayList<>();
    for (CommandReport report : reports) {
      taskIds.add(report.getTaskId());
    }
    Map<Long, HostRoleCommand> commands = actionManager.getTasksMap(taskIds);

    for (CommandReport report : reports) {

      Long clusterId = null;
      if (report.getClusterName() != null) {
        try {
          Cluster cluster = clusterFsm.getCluster(report.getClusterName());
          clusterId = cluster.getClusterId();
        } catch (AmbariException e) {
          // null clusterId reported and handled by the listener (DistributeRepositoriesActionListener)
        }
      }

      LOG.debug("Received command report: {}", report);

      // get this locally; don't touch the database
      Host host = clusterFsm.getHost(hostname);
      if (host == null) {
        LOG.error("Received a command report and was unable to retrieve Host for hostname = " + hostname);
        continue;
      }

      // Send event for final command reports for actions
      if (RoleCommand.valueOf(report.getRoleCommand()) == RoleCommand.ACTIONEXECUTE &&
          HostRoleStatus.valueOf(report.getStatus()).isCompletedState()) {
        ActionFinalReportReceivedEvent event = new ActionFinalReportReceivedEvent(
            clusterId, hostname, report, false);
        ambariEventPublisher.publish(event);
      }

      // Fetch HostRoleCommand that corresponds to a given task ID
      HostRoleCommand hostRoleCommand = commands.get(report.getTaskId());
      if (hostRoleCommand == null) {
        LOG.warn("Can't fetch HostRoleCommand with taskId = " + report.getTaskId());
      } else {
        // Skip sending events for command reports for ABORTed commands
        if (hostRoleCommand.getStatus() == HostRoleStatus.ABORTED) {
          continue;
        }
        if (hostRoleCommand.getStatus() == HostRoleStatus.QUEUED &&
            report.getStatus().equals("IN_PROGRESS")) {
          hostRoleCommand.setStartTime(now);

          // Because the task may be retried several times, set the original start time only once.
          if (hostRoleCommand.getOriginalStartTime() == -1) {
            hostRoleCommand.setOriginalStartTime(now);
          }
        }
      }

      // If the report indicates the keytab file was successfully transferred to a host or removed
      // from a host, record this for future reference
      if (Service.Type.KERBEROS.name().equalsIgnoreCase(report.getServiceName()) &&
          Role.KERBEROS_CLIENT.name().equalsIgnoreCase(report.getRole()) &&
          RoleCommand.CUSTOM_COMMAND.name().equalsIgnoreCase(report.getRoleCommand()) &&
          RequestExecution.Status.COMPLETED.name().equalsIgnoreCase(report.getStatus())) {

        String customCommand = report.getCustomCommand();

        if (SET_KEYTAB.equalsIgnoreCase(customCommand) || REMOVE_KEYTAB.equalsIgnoreCase(customCommand)) {
          WriteKeytabsStructuredOut writeKeytabsStructuredOut;
          try {
            writeKeytabsStructuredOut = gson.fromJson(report.getStructuredOut(), WriteKeytabsStructuredOut.class);
          } catch (JsonSyntaxException ex) {
            //Json structure was incorrect do nothing, pass this data further for processing
            writeKeytabsStructuredOut = null;
          }

          if (writeKeytabsStructuredOut != null) {
            // TODO rework this. Make sure that keytab check and write commands returns principal list for each keytab
            if (SET_KEYTAB.equalsIgnoreCase(customCommand)) {
              Map<String, String> keytabs = writeKeytabsStructuredOut.getKeytabs();
              if (keytabs != null) {
                for (Map.Entry<String, String> entry : keytabs.entrySet()) {
                  String principal = entry.getKey();
                  String keytabPath = entry.getValue();
                  for (KerberosKeytabPrincipalEntity kkpe: kerberosKeytabPrincipalDAO.findByHostAndKeytab(host.getHostId(), keytabPath)) {
                    kkpe.setDistributed(true);
                    kerberosKeytabPrincipalDAO.merge(kkpe);
                  }
                }
              }
            } else if (REMOVE_KEYTAB.equalsIgnoreCase(customCommand)) {
              // TODO check if additional processing of removed records(besides existent in DestroyPrincipalsServerAction)
              // TODO is required
            }
          }
        } else if (CHECK_KEYTABS.equalsIgnoreCase(customCommand)) {
          ListKeytabsStructuredOut structuredOut = gson.fromJson(report.getStructuredOut(), ListKeytabsStructuredOut.class);
          for (MissingKeytab each : structuredOut.missingKeytabs) {
            LOG.info("Missing principal: {} for keytab: {} on host: {}", each.principal, each.keytabFilePath, hostname);
            for (KerberosKeytabPrincipalEntity kkpe: kerberosKeytabPrincipalDAO.findByHostAndKeytab(host.getHostId(), each.keytabFilePath)) {
              kkpe.setDistributed(false);
              kerberosKeytabPrincipalDAO.merge(kkpe);
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
        LOG.debug("{} is an action - skip component lookup", report.getRole());
      } else {
        try {
          Service svc = cl.getService(service);
          ServiceComponent svcComp = svc.getServiceComponent(report.getRole());
          ServiceComponentHost scHost = svcComp.getServiceComponentHost(hostname);
          String schName = scHost.getServiceComponentName();

          if (report.getStatus().equals(HostRoleStatus.COMPLETED.toString())) {

            // Reading component version if it is present
            if (StringUtils.isNotBlank(report.getStructuredOut())
                && !StringUtils.equals("{}", report.getStructuredOut())) {
              ComponentVersionStructuredOut structuredOutput = null;
              try {
                structuredOutput = gson.fromJson(report.getStructuredOut(), ComponentVersionStructuredOut.class);
              } catch (JsonSyntaxException ex) {
                //Json structure for component version was incorrect
                //do nothing, pass this data further for processing
              }

              String newVersion = structuredOutput == null ? null : structuredOutput.version;
              Long repoVersionId = structuredOutput == null ? null : structuredOutput.repositoryVersionId;

              HostComponentVersionAdvertisedEvent event = new HostComponentVersionAdvertisedEvent(
                  cl, scHost, newVersion, repoVersionId);

              versionEventPublisher.publish(event);
            }

            if ((report.getRoleCommand().equals(RoleCommand.START.toString()) ||
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
                    "INSTALL".equals(report.getCustomCommand()))) && svcComp.isClientComponent()) {
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

            if (StringUtils.isNotBlank(report.getStructuredOut())) {
              try {
                ComponentVersionStructuredOut structuredOutput = gson.fromJson(report.getStructuredOut(), ComponentVersionStructuredOut.class);

                if (null != structuredOutput.upgradeDirection) {
                  scHost.setUpgradeState(UpgradeState.FAILED);
                }
              } catch (JsonSyntaxException ex) {
                LOG.warn("Structured output was found, but not parseable: {}", report.getStructuredOut());
              }
            }

            LOG.error("Operation failed - may be retried. Service component host: "
                + schName + ", host: " + hostname + " Action id " + report.getActionId() + " and taskId " + report.getTaskId());
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

  /**
   * Process reports of status commands
   *
   * @param heartbeat heartbeat to process
   * @throws AmbariException
   */
  protected void processStatusReports(HeartBeat heartbeat) throws AmbariException {
    String hostname = heartbeat.getHostname();
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
              org.apache.ambari.server.state.State prevState = scHost.getState();
              org.apache.ambari.server.state.State liveState =
                  org.apache.ambari.server.state.State.valueOf(org.apache.ambari.server.state.State.class,
                      status.getStatus());
              //ignore reports from status commands if component is in INIT or any "in progress" state
              if (prevState.equals(org.apache.ambari.server.state.State.INSTALLED)
                  || prevState.equals(org.apache.ambari.server.state.State.STARTED)
                  || prevState.equals(org.apache.ambari.server.state.State.UNKNOWN)) {
                scHost.setState(liveState);
                if (!prevState.equals(liveState)) {
                  LOG.info("State of service component " + componentName
                      + " of service " + status.getServiceName()
                      + " of cluster " + status.getClusterName()
                      + " has changed from " + prevState + " to " + liveState
                      + " at host " + hostname
                      + " according to STATUS_COMMAND report");
                }
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

                    HostComponentVersionAdvertisedEvent event = new HostComponentVersionAdvertisedEvent(cl, scHost, version);
                    versionEventPublisher.publish(event);
                  }

                } catch (Exception e) {
                  LOG.error("Could not access extra JSON for " +
                      scHost.getServiceComponentName() + " from " +
                      scHost.getHostName() + ": " + status.getExtra() +
                      " (" + e.getMessage() + ")");
                }
              }

              heartbeatMonitor.getAgentRequests()
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
   * This class is used for mapping json of structured output for keytab distribution actions.
   */
  private static class WriteKeytabsStructuredOut {
    @SerializedName("keytabs")
    private Map<String, String> keytabs;

    @SerializedName("removedKeytabs")
    private Map<String, String> removedKeytabs;

    public Map<String, String> getKeytabs() {
      return keytabs;
    }

    public void setKeytabs(Map<String, String> keytabs) {
      this.keytabs = keytabs;
    }

    public Map<String, String> getRemovedKeytabs() {
      return removedKeytabs;
    }

    public void setRemovedKeytabs(Map<String, String> removedKeytabs) {
      this.removedKeytabs = removedKeytabs;
    }
  }

  private static class ListKeytabsStructuredOut {
    @SerializedName("missing_keytabs")
    private final List<MissingKeytab> missingKeytabs;

    public ListKeytabsStructuredOut(List<MissingKeytab> missingKeytabs) {
      this.missingKeytabs = missingKeytabs;
    }
  }

  private static class MissingKeytab {
    @SerializedName("principal")
    private final String principal;
    @SerializedName("keytab_file_path")
    private final String keytabFilePath;

    public MissingKeytab(String principal, String keytabFilePath) {
      this.principal = principal;
      this.keytabFilePath = keytabFilePath;
    }
  }

  /**
   * This class is used for mapping json of structured output for component START action.
   */
  private static class ComponentVersionStructuredOut {
    @SerializedName("version")
    private String version;

    @SerializedName("upgrade_type")
    private UpgradeType upgradeType = null;

    @SerializedName("direction")
    private Direction upgradeDirection = null;

    @SerializedName(KeyNames.REPO_VERSION_ID)
    private Long repositoryVersionId;

  }
}
