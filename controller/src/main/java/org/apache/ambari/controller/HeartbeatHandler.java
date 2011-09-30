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
package org.apache.ambari.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.ambari.common.rest.entities.Cluster;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.common.rest.entities.NodeState;
import org.apache.ambari.controller.Clusters;
import org.apache.ambari.controller.Nodes;
import org.apache.ambari.common.rest.entities.agent.Action;
import org.apache.ambari.common.rest.entities.agent.ControllerResponse;
import org.apache.ambari.common.rest.entities.agent.HeartBeat;
import org.apache.ambari.common.rest.entities.agent.ServerStatus;
import org.apache.ambari.common.rest.entities.agent.ServerStatus.State;
import org.apache.ambari.components.ClusterContext;
import org.apache.ambari.components.impl.ClusterContextImpl;
import org.apache.ambari.components.impl.HDFSPluginImpl;
import org.apache.ambari.resource.statemachine.Role;
import org.apache.ambari.resource.statemachine.Service;
import org.apache.ambari.resource.statemachine.StateMachineInvoker;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;

public class HeartbeatHandler {
  
  private Map<String, ControllerResponse> agentToHeartbeatResponseMap = 
      new TreeMap<String, ControllerResponse>();
  
  LinkedBlockingQueue<HeartBeat> heartbeatQueue = 
      new LinkedBlockingQueue<HeartBeat>();
  
  Map<String, List<Action>> responseMap = 
      new HashMap<String, List<Action>>();
  
  public void addActionsForNode(String hostname, List<Action> actions) {
    synchronized (this) {
      List<Action> currentActions = responseMap.get(hostname);
      if (currentActions != null) {
        currentActions.addAll(actions);
      }
    }
  }
  
  public ControllerResponse processHeartBeat(HeartBeat heartbeat) 
      throws DatatypeConfigurationException, IOException {
    ControllerResponse response = 
        agentToHeartbeatResponseMap.get(heartbeat.getHostname());
    if (response != null) {
      if (response.getResponseId() == heartbeat.getResponseId()) {
        return response; //duplicate heartbeat
      }
    }
    
    short responseId = 
        (short) (Short.parseShort(response.getResponseId()) + 1);

    Node node = Nodes.getInstance().getNodes().get(heartbeat.getHostname());
    NodeState state = node.getNodeState();
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(new Date());
    state.setLastHeartbeatTime(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
    
    Cluster cluster = 
        Clusters.getInstance().getClusterByName(state.getClusterName());
    ClusterContext clusterContext = new ClusterContextImpl(cluster, node);
    
    List <Action> allActions = new ArrayList<Action>();
    
    if (heartbeat.getIdle()) {
      //if the command-execution takes longer than one heartbeat interval
      //the check for idleness will prevent the same node getting the same 
      //command more than once. In the future this could be improved
      //to reflect the command execution state more accurately.
      
      //get what is currently running on the node
      List<ServerStatus> serverStatuses = heartbeat.getServersStatus();
      
      //CHECK what servers moved the role to ACTIVE state
      StartedComponentServers componentServers = new StartedComponentServers();
      for (ServerStatus status : serverStatuses) {
        if (status.getState() == State.STARTED) {
          componentServers.serverStarted(status.getComponent(), 
              status.getRole());
        }
      }

      //get the state machine reference to the cluster
      org.apache.ambari.resource.statemachine.Cluster clusterSMobject = 
          StateMachineInvoker
          .getStateMachineClusterInstance(state.getClusterName());
      //the state machine reference to the services
      List<Service> clusterServices = clusterSMobject.getServices();
      //go through all the services, and check which role should be started
      //Get the corresponding commands
      for (Service service : clusterServices) {
        List<Role> roles = service.getRoles();
        for (Role role : roles) {
          if (role.shouldStart() && 
              !componentServers.isStarted(
                  role.getAssociatedService().getServiceName(),
                  role.getRoleName())) {
            //TODO: get reference to the plugin impl
            HDFSPluginImpl plugin = new HDFSPluginImpl();
            List<Action> actions = 
                plugin.startRoleServer(clusterContext, role.getRoleName());
            allActions.addAll(actions);
          }
        }
      }
    }
    ControllerResponse r = new ControllerResponse();
    r.setResponseId(String.valueOf(responseId));
    r.setActions(allActions);
    agentToHeartbeatResponseMap.put(heartbeat.getHostname(), r);
    return r;
  }
  
  private static class StartedComponentServers {
    private Map<String, Map<String, Boolean>> startedComponentServerMap =
        new HashMap<String, Map<String, Boolean>>();
    void serverStarted(String component, String server) {
      Map<String, Boolean> serverStartedMap = null;
      if ((serverStartedMap = startedComponentServerMap.get(component))
          != null) {
        serverStartedMap.put(server, true);
        return;
      }
      serverStartedMap = new HashMap<String, Boolean>();
      serverStartedMap.put(server, true);
      startedComponentServerMap.put(component, serverStartedMap);
    }
    boolean isStarted(String component, String server) {
      Map<String, Boolean> startedServerMap;
      if ((startedServerMap=startedComponentServerMap.get(component)) != null){
        return startedServerMap.get(server) != null;
      }
      return false;
    }
  }
}
