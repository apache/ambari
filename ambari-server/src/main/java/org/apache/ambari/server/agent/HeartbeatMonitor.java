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

import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.events.MessageNotDelivered;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHeartbeatLostEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;

/**
 * Monitors the node state and heartbeats.
 */
public class HeartbeatMonitor implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(HeartbeatMonitor.class);
  private Clusters clusters;
  private ActionManager actionManager;
  private final int threadWakeupInterval; //1 minute
  private volatile boolean shouldRun = true;
  private Thread monitorThread = null;
  private final ConfigHelper configHelper;
  private final AmbariMetaInfo ambariMetaInfo;
  private final AmbariManagementController ambariManagementController;
  private final Configuration configuration;
  private final AgentRequests agentRequests;
  private final AmbariEventPublisher ambariEventPublisher;

  public HeartbeatMonitor(Clusters clusters, ActionManager am,
                          int threadWakeupInterval, Injector injector) {
    this.clusters = clusters;
    actionManager = am;
    this.threadWakeupInterval = threadWakeupInterval;
    configHelper = injector.getInstance(ConfigHelper.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariManagementController = injector.getInstance(
            AmbariManagementController.class);
    configuration = injector.getInstance(Configuration.class);
    agentRequests = new AgentRequests();
    ambariEventPublisher = injector.getInstance(AmbariEventPublisher.class);
    ambariEventPublisher.register(this);
  }

  public void shutdown() {
    shouldRun = false;
  }

  public void start() {
    monitorThread = new Thread(this, "ambari-hearbeat-monitor");
    monitorThread.start();
  }

  void join(long millis) throws InterruptedException {
    monitorThread.join(millis);
  }

  public boolean isAlive() {
    return monitorThread.isAlive();
  }

  public AgentRequests getAgentRequests() {
    return agentRequests;
  }

  @Override
  public void run() {
    while (shouldRun) {
      try {
        doWork();
        LOG.trace("Putting monitor to sleep for {} milliseconds", threadWakeupInterval);
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
      if (hostObj.getState() == HostState.HEARTBEAT_LOST) {
        //do not check if host already known be lost
        continue;
      }
      Long hostId = hostObj.getHostId();
      HostState hostState = hostObj.getState();

      long lastHeartbeat = 0;
      try {
        lastHeartbeat = clusters.getHostById(hostId).getLastHeartbeatTime();
      } catch (AmbariException e) {
        LOG.warn("Exception in getting host object; Is it fatal?", e);
      }
      if (lastHeartbeat + 2 * threadWakeupInterval < now) {
        handleHeartbeatLost(hostId);
      }
      if (hostState == HostState.WAITING_FOR_HOST_STATUS_UPDATES) {
        long timeSpentInState = hostObj.getTimeInState();
        if (timeSpentInState + 5 * threadWakeupInterval < now) {
          //Go back to init, the agent will be asked to register again in the next heartbeat
          LOG.warn("timeSpentInState + 5*threadWakeupInterval < now, Go back to init");
          hostObj.setState(HostState.INIT);
        }
      }
    }
  }

  private void handleHeartbeatLost(Long hostId) throws AmbariException, InvalidStateTransitionException {
    Host hostObj = clusters.getHostById(hostId);
    String host = hostObj.getHostName();
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
          State oldState = sch.getState();
          sch.setState(State.UNKNOWN);
          sch.setLastValidState(oldState);
        }
      }
    }

    //Purge action queue
    //notify action manager
    actionManager.handleLostHost(host);
  }

  @Subscribe
  public void onMessageNotDelivered(MessageNotDelivered messageNotDelivered) {
    try {
      Host hostObj = clusters.getHostById(messageNotDelivered.getHostId());
      if (hostObj.getState() == HostState.HEARTBEAT_LOST) {
        //do not check if host already known be lost
        return;
      }
      handleHeartbeatLost(messageNotDelivered.getHostId());
    } catch (Exception e) {
      LOG.error("Error during host to heartbeat lost moving", e);
    }
  }
}
