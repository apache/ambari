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

import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.common.rest.entities.NodeState;
import org.apache.ambari.controller.Clusters;
import org.apache.ambari.controller.Nodes;
import org.apache.ambari.common.rest.entities.agent.Action;
import org.apache.ambari.common.rest.entities.agent.Action.Kind;
import org.apache.ambari.common.rest.entities.agent.ActionResult;
import org.apache.ambari.common.rest.entities.agent.AgentRoleState;
import org.apache.ambari.common.rest.entities.agent.ControllerResponse;
import org.apache.ambari.common.rest.entities.agent.HeartBeat;
import org.apache.ambari.common.rest.entities.agent.Action.Signal;
import org.apache.ambari.components.ClusterContext;
import org.apache.ambari.components.impl.ClusterContextImpl;
import org.apache.ambari.components.impl.HDFSPluginImpl;
import org.apache.ambari.resource.statemachine.ClusterFSM;
import org.apache.ambari.resource.statemachine.RoleFSM;
import org.apache.ambari.resource.statemachine.RoleEvent;
import org.apache.ambari.resource.statemachine.RoleEventType;
import org.apache.ambari.resource.statemachine.ServiceEvent;
import org.apache.ambari.resource.statemachine.ServiceEventType;
import org.apache.ambari.resource.statemachine.ServiceFSM;
import org.apache.ambari.resource.statemachine.ServiceState;
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
    
    String hostname = heartbeat.getHostname();

    Node node = Nodes.getInstance().getNodes().get(hostname);
    NodeState state = node.getNodeState();
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(new Date());
    state.setLastHeartbeatTime(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
    
    Cluster cluster = 
        Clusters.getInstance().getClusterByID(state.getClusterID());
    ClusterContext clusterContext = new ClusterContextImpl(cluster, node);
    
    List<Action> allActions = new ArrayList<Action>();
    
    if (heartbeat.getIdle()) {
      //if the command-execution takes longer than one heartbeat interval
      //the check for idleness will prevent the same node getting the same 
      //command more than once. In the future this could be improved
      //to reflect the command execution state more accurately.
      
      String desiredClusterId = cluster.getID();
      
      StartedComponentServers componentServers = new StartedComponentServers();

      //get the list of uninstall actions
      //create a map from component/role to 'started' for easy lookup later
      allActions = 
          getStopAndUninstallActions(heartbeat, clusterContext, 
              componentServers);

      //get the state machine reference to the cluster
      ClusterFSM clusterSMobject = StateMachineInvoker
          .getStateMachineClusterInstance(desiredClusterId);
      //the state machine reference to the services
      List<ServiceFSM> clusterServices = clusterSMobject.getServices();
      //go through all the services, and check which role should be started
      for (ServiceFSM service : clusterServices) {
        List<RoleFSM> roles = service.getRoles();
        
        for (RoleFSM role : roles) {
          boolean roleServerRunning = componentServers.isStarted(
              role.getAssociatedService().getServiceName(),
              role.getRoleName());
          //TODO: get reference to the plugin impl for this service/component
          HDFSPluginImpl plugin = new HDFSPluginImpl();
          //check whether the agent should start any server
          if (role.shouldStart()) {
            if (!roleServerRunning) {
              short retryCount = retryCountForRole.get(hostname,role.getRoleName());
              if (retryCount > MAX_RETRY_COUNT) {
                //LOG the failure to start the role server
                StateMachineInvoker.getAMBARIEventHandler()
                .handle(new RoleEvent(RoleEventType.START_FAILURE, role));
                retryCountForRole.resetAttemptCount(hostname,role.getRoleName());
                continue;
              }
              List<Action> actions = 
                  plugin.startRoleServer(clusterContext, role.getRoleName());
              allActions.addAll(actions);
              retryCountForRole.incrAttemptCount(hostname,role.getRoleName());
            }
            //raise an event to the state machine for a successful role-start
            if (roleServerRunning) {
              retryCountForRole.resetAttemptCount(hostname,role.getRoleName());
              StateMachineInvoker.getAMBARIEventHandler()
              .handle(new RoleEvent(RoleEventType.START_SUCCESS, role));
            }
          }
          //check whether the agent should stop any server
          if (role.shouldStop()) {
            if (roleServerRunning) {
              short retryCount = retryCountForRole.get(hostname,role.getRoleName());
              if (retryCount > MAX_RETRY_COUNT) {
                //LOG the failure to stop the role server
                StateMachineInvoker.getAMBARIEventHandler()
                .handle(new RoleEvent(RoleEventType.STOP_FAILURE, role));
                retryCountForRole.resetAttemptCount(hostname,role.getRoleName());
                continue;
              }
              List<Action> actions = 
                  plugin.stopRoleServer(clusterContext, role.getRoleName());
              allActions.addAll(actions);
              retryCountForRole.incrAttemptCount(hostname,role.getRoleName());
            }
            //raise an event to the state machine for a successful role-stop
            if (!roleServerRunning) {
              retryCountForRole.resetAttemptCount(hostname,role.getRoleName());
              StateMachineInvoker.getAMBARIEventHandler()
              .handle(new RoleEvent(RoleEventType.STOP_SUCCESS, role));
            }
          }
        }
        checkActionResults(service, cluster, heartbeat, allActions);
      }
    }
    ControllerResponse r = new ControllerResponse();
    r.setResponseId(String.valueOf(responseId));
    r.setActions(allActions);
    agentToHeartbeatResponseMap.put(heartbeat.getHostname(), r);
    return r;
  }
  
  private static class StartedComponentServers {
    //Convenience class to aid in heartbeat processing
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
    //currently handles only one role start at a time on a node
    //fix this to take care of multiple roles started in parallel 
    //on a node
    private Map<String, Short> countMap = new HashMap<String, Short>();
    public short get(String hostname, String role) {
      return countMap.get(hostname+role);
    }
    public void incrAttemptCount(String hostname, String role) {
      Short currentCount = 0;
      if ((currentCount = countMap.get(hostname+role)) == null) {
        currentCount = 0;
      }
      countMap.put(hostname, (short) (currentCount + 1));
    }
    public void resetAttemptCount(String hostname, String role) {
      countMap.remove(hostname+role);
    }
  }
  
  private final static String SERVICE_AVAILABILITY_CHECK_ID = 
      "SERVICE_AVAILABILITY_CHECK_ID";
  
  private static String getActionIDForServiceAvailability(Cluster cluster, 
      ServiceFSM service) {
    String id = cluster.getID() + cluster.getLatestRevision() + 
        service.getServiceName() + SERVICE_AVAILABILITY_CHECK_ID;
    return id;
  }
  
  private static void checkActionResults(ServiceFSM service, Cluster cluster,
      HeartBeat heartbeat, List<Action> allActions) {
    //see whether the service is in the STARTED state, and if so,
    //check whether there is any action-result that indicates success
    //of the availability check (safemode, etc.)
    if (service.getServiceState() == ServiceState.STARTED) {
      //TODO: check with plugin whether this node can run the health-check
      String id = getActionIDForServiceAvailability(cluster, service);
      boolean serviceActivated = false;
      for (ActionResult result : heartbeat.getActionResults()) {
        if (result.getId().equals(id)) {
          if (result.getCommandResult().getExitCode() == 0) {
            StateMachineInvoker.getAMBARIEventHandler().handle(
               new ServiceEvent(ServiceEventType.AVAILABLE_CHECK_SUCCESS,
                   service));
            serviceActivated = true;
          }
          break;
        }
      }
      //TODO: check with plugin whether this node can run the health-check (OWEN)
//      if (!serviceActivated) {
//        Action action = plugin.getAvailabilityCheck(cluster.getLatestClusterDefinition().getName()));
//        action.setId(id);
//        allActions.add(action);
//      }
    }
  }
  
  private static List<Action> getStopAndUninstallActions(
      HeartBeat heartbeat, ClusterContext context, 
      StartedComponentServers componentServers)
          throws IOException {
    List<AgentRoleState> agentRoleStates = 
        heartbeat.getInstalledRoleStates();
    List<Action> killAndUninstallCmds = new ArrayList<Action>();
    //Go over all the reported role states, and stop/uninstall the
    //unnecessary ones
    for (AgentRoleState agentRoleState : agentRoleStates) {
      boolean stopRole = false;
      boolean uninstall = false;
      
      ClusterFSM clusterFSM = StateMachineInvoker
          .getStateMachineClusterInstance(agentRoleState.getClusterId());
      if (clusterFSM == null) {
        //ask the agent to stop everything belonging to this role
        //since the controller can't be in a state where the clusterFSM
        //is null and the cluster is not in ATTIC or deleted state
        stopRole = true;
        uninstall = true;
      }
      if (clusterFSM != null) {
        if (clusterFSM.getClusterState().getState()
            .equals(ClusterState.CLUSTER_STATE_INACTIVE)) {
          stopRole = true;
          uninstall = false;
        }
        else if (clusterFSM.getClusterState().getState()
            .equals(ClusterState.CLUSTER_STATE_ATTIC)) {
          stopRole = true;
          uninstall = true;
        }
      }
      if (stopRole && 
        //TODO: not sure whether this state requires to be checked...
        agentRoleState.getServerStatus() == AgentRoleState.State.STARTED) {
        Action action = new Action();
        action.setClusterId(agentRoleState.getClusterId());
        action.setClusterDefinitionRevision(agentRoleState.getClusterDefinitionRevision());
        action.setRole(agentRoleState.getRoleName());
        action.setComponent(agentRoleState.getComponentName());
        action.setKind(Kind.STOP_ACTION);
        action.setSignal(Signal.KILL);
        killAndUninstallCmds.add(action);
      }
      if (uninstall) {
        //TODO: get reference to the plugin impl for this service/component
        HDFSPluginImpl plugin = new HDFSPluginImpl();
        List<Action> uninstallAction = plugin.uninstall(context);
        killAndUninstallCmds.addAll(uninstallAction);
      }
      if (!stopRole && !uninstall && 
          agentRoleState.getServerStatus() == AgentRoleState.State.STARTED) {
        //make a note of the fact that a server is running for reference later
        componentServers.roleServerStarted(agentRoleState.getComponentName(), 
              agentRoleState.getRoleName());
      }
    }
    return killAndUninstallCmds;
  }
}
