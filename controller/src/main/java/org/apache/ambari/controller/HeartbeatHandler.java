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
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.ambari.components.ClusterContext;
import org.apache.ambari.components.impl.ClusterContextImpl;
import org.apache.ambari.components.impl.HDFSPluginImpl;
import org.apache.ambari.resource.statemachine.Role;
import org.apache.ambari.resource.statemachine.RoleEvent;
import org.apache.ambari.resource.statemachine.RoleEventType;
import org.apache.ambari.resource.statemachine.Service;
import org.apache.ambari.resource.statemachine.StateMachineInvoker;

public class HeartbeatHandler {
  
  private Map<String, ControllerResponse> agentToHeartbeatResponseMap = 
      Collections.synchronizedMap(new HashMap<String, ControllerResponse>());
  
  private RetryCountForRoleServerAction retryCountForRole = new RetryCountForRoleServerAction();
  
  final short MAX_RETRY_COUNT = 3;
  
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
    
    List<Action> allActions = new ArrayList<Action>();
    
    if (heartbeat.getIdle()) {
      //if the command-execution takes longer than one heartbeat interval
      //the check for idleness will prevent the same node getting the same 
      //command more than once. In the future this could be improved
      //to reflect the command execution state more accurately.
      
      //get what is currently running on the node
      List<ServerStatus> roleStatuses = heartbeat.getServersStatus();
      
      //what servers are running currently
      //ADD LOGIC FOR CAPTURING THE CLUSTER-ID THE SERVERS BELONG TO
      //IF THEY BELONG TO THE CLUSTER-ID THIS AGENT IS PART OF, WELL AND GOOD
      //IF NOT, THEN SEND COMMANDS TO STOP THE SERVERS
      StartedComponentServers componentServers = new StartedComponentServers();
      for (ServerStatus status : roleStatuses) {
        componentServers.roleServerStarted(status.getComponent(), 
            status.getRole());
      }

      //get the state machine reference to the cluster
      org.apache.ambari.resource.statemachine.Cluster clusterSMobject = 
          StateMachineInvoker
          .getStateMachineClusterInstance(state.getClusterName());
      //the state machine reference to the services
      List<Service> clusterServices = clusterSMobject.getServices();
      //go through all the services, and check which role should be started
      for (Service service : clusterServices) {
        List<Role> roles = service.getRoles();
        
        for (Role role : roles) {
          boolean roleServerRunning = componentServers.isStarted(
              role.getAssociatedService().getServiceName(),
              role.getRoleName());
          //TODO: get reference to the plugin impl for this service/component
          HDFSPluginImpl plugin = new HDFSPluginImpl();
          //check whether the agent should start any server
          if (role.shouldStart()) {
            if (!roleServerRunning) {
              short retryCount = retryCountForRole.get(role);
              if (retryCount > MAX_RETRY_COUNT) {
                //LOG the failure to start the role server
                StateMachineInvoker.getAMBARIEventHandler()
                .handle(new RoleEvent(RoleEventType.START_FAILURE, role));
                retryCountForRole.reset(role);
                continue;
              }
              List<Action> actions = 
                  plugin.startRoleServer(clusterContext, role.getRoleName());
              allActions.addAll(actions);
              retryCountForRole.incr(role);
            }
            //raise an event to the state machine for a successful role-start
            if (roleServerRunning) {
              retryCountForRole.reset(role);
              StateMachineInvoker.getAMBARIEventHandler()
              .handle(new RoleEvent(RoleEventType.START_SUCCESS, role));
            }
          }
          //check whether the agent should stop any server
          if (role.shouldStop()) {
            if (roleServerRunning) {
              short retryCount = retryCountForRole.get(role);
              if (retryCount > MAX_RETRY_COUNT) {
                //LOG the failure to stop the role server
                StateMachineInvoker.getAMBARIEventHandler()
                .handle(new RoleEvent(RoleEventType.STOP_FAILURE, role));
                retryCountForRole.reset(role);
                continue;
              }
              List<Action> actions = 
                  plugin.stopRoleServer(clusterContext, role.getRoleName());
              allActions.addAll(actions);
              retryCountForRole.incr(role);
            }
            //raise an event to the state machine for a successful role-stop
            if (!roleServerRunning) {
              retryCountForRole.reset(role);
              StateMachineInvoker.getAMBARIEventHandler()
              .handle(new RoleEvent(RoleEventType.STOP_SUCCESS, role));
            }
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
    void roleServerStarted(String component, String roleServer) {
      Map<String, Boolean> serverStartedMap = null;
      if ((serverStartedMap = startedComponentServerMap.get(component))
          != null) {
        serverStartedMap.put(roleServer, true);
        return;
      }
      serverStartedMap = new HashMap<String, Boolean>();
      serverStartedMap.put(roleServer, true);
      startedComponentServerMap.put(component, serverStartedMap);
    }
    boolean isStarted(String component, String server) {
      Map<String, Boolean> startedServerMap;
      if ((startedServerMap=startedComponentServerMap.get(component)) 
          != null) {
        return startedServerMap.get(server) != null;
      }
      return false;
    }
  }
  
  private static class RetryCountForRoleServerAction {
    private Map<Role, Short> countMap = new HashMap<Role, Short>();
    public short get(Role role) {
      return countMap.get(role);
    }
    public void incr(Role role) {
      Short currentCount = 0;
      if ((currentCount = countMap.get(role)) == null) {
        currentCount = 0;
      }
      countMap.put(role, (short) (currentCount + 1));
    }
    public void reset(Role role) {
      countMap.remove(role);
    }
  }
}
