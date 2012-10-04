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

import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.host.HostHeartbeatLostEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Monitors the node state and heartbeats.
 */
public class HeartbeatMonitor implements Runnable {
  private static Log LOG = LogFactory.getLog(HeartbeatMonitor.class);
  private Clusters fsm;
  private ActionQueue actionQueue;
  private ActionManager actionManager;
  private final int threadWakeupInterval; //1 minute
  private volatile boolean shouldRun = true;
  private Thread monitorThread = null;

  public HeartbeatMonitor(Clusters fsm, ActionQueue aq, ActionManager am,
      int threadWakeupInterval) {
    this.fsm = fsm;
    this.actionQueue = aq;
    this.actionManager = am;
    this.threadWakeupInterval = threadWakeupInterval;
  }

  public void shutdown() {
    shouldRun = false;
  }

  public void start() {
    monitorThread = new Thread(this);
    monitorThread.start();
  }

  @Override
  public void run() {
    while (shouldRun) {
      try {
        Thread.sleep(threadWakeupInterval);
        doWork();
      } catch (InterruptedException ex) {
        LOG.warn("Scheduler thread is interrupted going to stop", ex);
        shouldRun = false;
      } catch (Exception ex) {
        LOG.warn("Exception received", ex);
      }
    }
  }

  //Go through all the nodes, check for last heartbeat or any waiting state
  //If heartbeat is lost, update node fsm state, purge the action queue
  //notify action manager for node failure.
  private void doWork() throws InvalidStateTransitonException {
    List<Host> allHosts = fsm.getAllHosts();
    long now = System.currentTimeMillis();
    for (Host hostObj : allHosts) {
      String host = hostObj.getHostName();
      HostState hostState = hostObj.getState();

      long lastHeartbeat = 0;
      try {
        lastHeartbeat = fsm.getHost(host).getLastHeartbeatTime();
      } catch (AmbariException e) {
        LOG.warn("Exception in getting host object; Is it fatal?", e);
      }
      if (lastHeartbeat + 5*threadWakeupInterval < now) {
        LOG.warn("Hearbeat lost from host "+host);
        //Heartbeat is expired
        hostObj.handleEvent(new HostHeartbeatLostEvent(host));
        //Purge action queue
        actionQueue.dequeueAll(host);
        //notify action manager
        actionManager.handleLostHost(host);
      }
      if (hostState == HostState.WAITING_FOR_HOST_STATUS_UPDATES) {
        long timeSpentInState = hostObj.getTimeInState();
        if (timeSpentInState + 5*threadWakeupInterval < now) {
          //Go back to init, the agent will be asked to register again in the next heartbeat
          hostObj.setState(HostState.INIT);
        }
      }
    }
  }
}
