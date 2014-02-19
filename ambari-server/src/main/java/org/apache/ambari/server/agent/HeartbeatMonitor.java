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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHeartbeatLostEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.*;

/**
 * Monitors the node state and heartbeats.
 */
public class HeartbeatMonitor implements Runnable {
  private static Log LOG = LogFactory.getLog(HeartbeatMonitor.class);
  private Clusters clusters;
  private ActionQueue actionQueue;
  private ActionManager actionManager;
  private final int threadWakeupInterval; //1 minute
  private volatile boolean shouldRun = true;
  private Thread monitorThread = null;
  private final ConfigHelper configHelper;
  private final AmbariMetaInfo ambariMetaInfo;
  private final AmbariManagementController ambariManagementController;
  private final Configuration configuration;

  public HeartbeatMonitor(Clusters clusters, ActionQueue aq, ActionManager am,
                          int threadWakeupInterval, Injector injector) {
    this.clusters = clusters;
    this.actionQueue = aq;
    this.actionManager = am;
    this.threadWakeupInterval = threadWakeupInterval;
    this.configHelper = injector.getInstance(ConfigHelper.class);
    this.ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    this.ambariManagementController = injector.getInstance(
            AmbariManagementController.class);
    this.configuration = injector.getInstance(Configuration.class);
  }

  public void shutdown() {
    shouldRun = false;
  }

  public void start() {
    monitorThread = new Thread(this);
    monitorThread.start();
  }

  void join(long millis) throws InterruptedException {
    monitorThread.join(millis);
  }

  public boolean isAlive() {
    return monitorThread.isAlive();
  }

  @Override
  public void run() {
    while (shouldRun) {
      try {
        doWork();
        LOG.trace("Putting monitor to sleep for " + threadWakeupInterval + " " +
          "milliseconds");
        Thread.sleep(threadWakeupInterval);
      } catch (InterruptedException ex) {
        LOG.warn("Scheduler thread is interrupted going to stop", ex);
        shouldRun = false;
      } catch (Exception ex) {
        LOG.warn("Exception received", ex);
      } catch (Throwable t) {
        LOG.warn("ERROR", t);
      }
    }
  }

  //Go through all the nodes, check for last heartbeat or any waiting state
  //If heartbeat is lost, update node clusters state, purge the action queue
  //notify action manager for node failure.
  private void doWork() throws InvalidStateTransitionException, AmbariException {
    List<Host> allHosts = clusters.getHosts();
    long now = System.currentTimeMillis();
    for (Host hostObj : allHosts) {
      String host = hostObj.getHostName();
      HostState hostState = hostObj.getState();
      String hostname = hostObj.getHostName();

      long lastHeartbeat = 0;
      try {
        lastHeartbeat = clusters.getHost(host).getLastHeartbeatTime();
      } catch (AmbariException e) {
        LOG.warn("Exception in getting host object; Is it fatal?", e);
      }
      if (lastHeartbeat + 2 * threadWakeupInterval < now) {
        LOG.warn("Heartbeat lost from host " + host);
        //Heartbeat is expired
        hostObj.handleEvent(new HostHeartbeatLostEvent(host));

        // mark all components that are not clients with unknown status
        for (Cluster cluster : clusters.getClustersForHost(hostObj.getHostName())) {
          for (ServiceComponentHost sch : cluster.getServiceComponentHosts(hostObj.getHostName())) {
            Service s = cluster.getService(sch.getServiceName());
            ServiceComponent sc = s.getServiceComponent(sch.getServiceComponentName());
            if (!sc.isClientComponent() &&
              !sch.getState().equals(State.INIT) &&
              !sch.getState().equals(State.INSTALLING) &&
              !sch.getState().equals(State.INSTALL_FAILED) &&
              !sch.getState().equals(State.UNINSTALLED) &&
              !sch.getState().equals(State.DISABLED)) {
              LOG.warn("Setting component state to UNKNOWN for component " + sc.getName() + " on " + host);
              sch.setState(State.UNKNOWN);
            }
          }
        }

        //Purge action queue
        actionQueue.dequeueAll(host);
        //notify action manager
        actionManager.handleLostHost(host);
      }
      if (hostState == HostState.WAITING_FOR_HOST_STATUS_UPDATES) {
        long timeSpentInState = hostObj.getTimeInState();
        if (timeSpentInState + 5 * threadWakeupInterval < now) {
          //Go back to init, the agent will be asked to register again in the next heartbeat
          LOG.warn("timeSpentInState + 5*threadWakeupInterval < now, Go back to init");
          hostObj.setState(HostState.INIT);
        }
      }

      // Get status of service components
      List<StatusCommand> cmds = generateStatusCommands(hostname);
      LOG.trace("Generated " + cmds.size() + " status commands for host: " +
        hostname);
      if (cmds.isEmpty()) {
        // Nothing to do
      } else {
        for (StatusCommand command : cmds) {
          actionQueue.enqueue(hostname, command);
        }
      }
    }
  }

  /**
   * @param hostname
   * @return list of commands to get status of service components on a concrete host
   */
  public List<StatusCommand> generateStatusCommands(String hostname) throws AmbariException {
    List<StatusCommand> cmds = new ArrayList<StatusCommand>();

    for (Cluster cl : clusters.getClustersForHost(hostname)) {
      for (ServiceComponentHost sch : cl.getServiceComponentHosts(hostname)) {
        StatusCommand statusCmd = createStatusCommand(hostname, cl, sch);
        cmds.add(statusCmd);
      }
    }
    return cmds;
  }


  /**
   * Generates status command and fills all apropriate fields.
   * @throws AmbariException
   */
  private StatusCommand createStatusCommand(String hostname, Cluster cluster,
                               ServiceComponentHost sch) throws AmbariException {
    String serviceName = sch.getServiceName();
    String componentName = sch.getServiceComponentName();
    Service service = cluster.getService(sch.getServiceName());
    StackId stackId = cluster.getDesiredStackVersion();
    ServiceInfo serviceInfo = ambariMetaInfo.getServiceInfo(stackId.getStackName(),
            stackId.getStackVersion(), serviceName);
    ComponentInfo componentInfo = ambariMetaInfo.getComponent(
            stackId.getStackName(), stackId.getStackVersion(),
            serviceName, componentName);
    StackInfo stackInfo = ambariMetaInfo.getStackInfo(stackId.getStackName(),
            stackId.getStackVersion());

    Map<String, Map<String, String>> configurations = new TreeMap<String, Map<String, String>>();

    // get the cluster config for type 'global'
    // apply config group overrides

    Config clusterConfig = cluster.getDesiredConfigByType(GLOBAL);
    if (clusterConfig != null) {
      // cluster config for 'global'
      Map<String, String> props = new HashMap<String, String>(clusterConfig.getProperties());

      // Apply global properties for this host from all config groups
      Map<String, Map<String, String>> allConfigTags = configHelper
              .getEffectiveDesiredTags(cluster, hostname);

      Map<String, Map<String, String>> configTags = new HashMap<String,
              Map<String, String>>();

      for (Map.Entry<String, Map<String, String>> entry : allConfigTags.entrySet()) {
        if (entry.getKey().equals(GLOBAL)) {
          configTags.put(GLOBAL, entry.getValue());
        }
      }

      Map<String, Map<String, String>> properties = configHelper
              .getEffectiveConfigProperties(cluster, configTags);

      if (!properties.isEmpty()) {
        for (Map<String, String> propertyMap : properties.values()) {
          props.putAll(propertyMap);
        }
      }

      configurations.put(GLOBAL, props);
    }

    StatusCommand statusCmd = new StatusCommand();
    statusCmd.setClusterName(cluster.getClusterName());
    statusCmd.setServiceName(serviceName);
    statusCmd.setComponentName(componentName);
    statusCmd.setConfigurations(configurations);

    // Fill command params
    Map<String, String> commandParams = statusCmd.getCommandParams();
    commandParams.put(SCHEMA_VERSION, serviceInfo.getSchemaVersion());

    String commandTimeout = configuration.getDefaultAgentTaskTimeout();
    CommandScriptDefinition script = componentInfo.getCommandScript();
    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      if (script != null) {
        commandParams.put(SCRIPT, script.getScript());
        commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());
        if (script.getTimeout() > 0) {
          commandTimeout = String.valueOf(script.getTimeout());
        }
      } else {
        String message = String.format("Component %s of service %s has not " +
                "command script defined", componentName, serviceName);
        throw new AmbariException(message);
      }
    }
    commandParams.put(COMMAND_TIMEOUT, commandTimeout);
    commandParams.put(SERVICE_PACKAGE_FOLDER,
       serviceInfo.getServicePackageFolder());
    commandParams.put(HOOKS_FOLDER, stackInfo.getStackHooksFolder());
    // Fill host level params
    Map<String, String> hostLevelParams = statusCmd.getHostLevelParams();
    hostLevelParams.put(JDK_LOCATION, ambariManagementController.getJdkResourceUrl());
    hostLevelParams.put(STACK_NAME, stackId.getStackName());
    hostLevelParams.put(STACK_VERSION, stackId.getStackVersion());

    return statusCmd;
  }

}
