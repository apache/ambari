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
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHeartbeatLostEvent;
import org.apache.ambari.server.state.svccomphost.HBaseMasterPortScanner;
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
  private HBaseMasterPortScanner scanner;

  public void setScanner(HBaseMasterPortScanner scanner) {
        this.scanner = scanner;
  }

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
  //If heartbeat is lost, update node fsm state, purge the action queue
  //notify action manager for node failure.
  private void doWork() throws InvalidStateTransitionException, AmbariException {
    List<Host> allHosts = fsm.getHosts();
    long now = System.currentTimeMillis();
    for (Host hostObj : allHosts) {
      String host = hostObj.getHostName();
      HostState hostState = hostObj.getState();
      String hostname = hostObj.getHostName();

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
        
        // mark all components that are not clients with unknown status
        for (Cluster cluster : fsm.getClustersForHost(hostObj.getHostName())) {
          for (ServiceComponentHost sch : cluster.getServiceComponentHosts(hostObj.getHostName())) {
            Service s = cluster.getService(sch.getServiceName());
            ServiceComponent sc = s.getServiceComponent(sch.getServiceComponentName());
            if (!sc.isClientComponent() &&
                !sch.getState().equals(State.INIT) &&
                !sch.getState().equals(State.INSTALLING) &&
                !sch.getState().equals(State.INSTALL_FAILED) &&
                !sch.getState().equals(State.UNINSTALLED)) {
              sch.setState(State.UNKNOWN);
            }
          }
        }
        
        // hbase
        if(hostState != hostObj.getState() && scanner != null) scanner.updateHBaseMaster(hostObj);
        
        //Purge action queue
        actionQueue.dequeueAll(host);
        //notify action manager
        actionManager.handleLostHost(host);
      }
      if (hostState == HostState.WAITING_FOR_HOST_STATUS_UPDATES) {
        long timeSpentInState = hostObj.getTimeInState();
        if (timeSpentInState + 5*threadWakeupInterval < now) {
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
   * @return  list of commands to get status of service components on a concrete host
   */
  public List<StatusCommand> generateStatusCommands(String hostname) throws AmbariException {
    List<StatusCommand> cmds = new ArrayList<StatusCommand>();
    
    for (Cluster cl : fsm.getClustersForHost(hostname)) {
      
      for (ServiceComponentHost sch : cl.getServiceComponentHosts(hostname)) {
        String serviceName = sch.getServiceName();
        if (LOG.isDebugEnabled()) {
          LOG.debug("Live status will include status of service " + serviceName + " of cluster " + cl.getClusterName());
        }
        
        Map<String, Map<String, String>> configurations = new TreeMap<String, Map<String, String>>();
        
        // get the cluster config for type 'global'
        // apply service overrides, if the tag is not the same
        // apply host overrides, if any
        
        Config clusterConfig = cl.getDesiredConfigByType("global");
        if (null != clusterConfig) {
          // cluster config for 'global'
          Map<String,String> props = new HashMap<String, String>(clusterConfig.getProperties());

          // apply service overrides, only if the tag is not the same (for when service configs are overrides)
          Service service = cl.getService(sch.getServiceName());
          Config svcConfig = service.getDesiredConfigs().get("global");
          if (null != svcConfig && !svcConfig.getVersionTag().equals(clusterConfig.getVersionTag())) {
            props.putAll(svcConfig.getProperties());
          }
          
          // apply host overrides, if any
          Host host = fsm.getHost(hostname);
          DesiredConfig dc = host.getDesiredConfigs(cl.getClusterId()).get("global");
          if (null != dc) {
            Config hostConfig = cl.getConfig("global", dc.getVersion());
            if (null != hostConfig) {
              props.putAll(hostConfig.getProperties());
            }
          }
          
          configurations.put("global", props);
        }
        
        // HACK - if any service exists with global tag, and we have none, use
        // that instead
        if (configurations.isEmpty()) {
          Service service = cl.getService(sch.getServiceName());
          Config config = service.getDesiredConfigs().get("global");
          if (null != config)
            configurations.put("global", new HashMap<String,String>(config.getProperties()));
        }
        
        StatusCommand statusCmd = new StatusCommand();
        statusCmd.setClusterName(cl.getClusterName());
        statusCmd.setServiceName(serviceName);
        statusCmd.setComponentName(sch.getServiceComponentName());
        statusCmd.setConfigurations(configurations);
        cmds.add(statusCmd);
      }
    }
    return cmds;
  }
}
