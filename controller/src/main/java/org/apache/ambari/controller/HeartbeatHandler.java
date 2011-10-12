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
import org.apache.ambari.components.ComponentPlugin;
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
    
    List<Action> allActions = new ArrayList<Action>();
    
    if (heartbeat.getIdle()) {
      //if the command-execution takes longer than one heartbeat interval
      //the check for idleness will prevent the same node getting the same 
      //command more than once. In the future this could be improved
      //to reflect the command execution state more accurately.
      
      String desiredClusterId = cluster.getID();
      
      InstalledOrStartedComponents componentServers = new InstalledOrStartedComponents();
      //get the state machine reference to the cluster
      ClusterFSM clusterFsm = StateMachineInvoker
          .getStateMachineClusterInstance(desiredClusterId);
      
      //get the list of uninstall actions
      //create a map from component/role to 'started' for easy lookup later
      allActions = 
          inspectAgentState(heartbeat, cluster, componentServers, clusterFsm);

      checkActionResults(cluster, clusterFsm, heartbeat, allActions);
      //the state machine reference to the services
      List<ServiceFSM> clusterServices = clusterFsm.getServices();
      //go through all the services, and check which role should be started
      //TODO: Given that we already look at what is running/installed in 
      //inspectAgentState, having this for loop is inefficient. We
      //should combine the two aspects.
      for (ServiceFSM service : clusterServices) {
        List<RoleFSM> roles = service.getRoles();
        
        for (RoleFSM role : roles) {
          boolean roleInstalled = componentServers.isInstalled(
             role.getAssociatedService().getServiceName(), role.getRoleName());     
          boolean roleServerRunning = componentServers.isStarted(
              role.getAssociatedService().getServiceName(),
              role.getRoleName());
          //TODO: get reference to the plugin impl for this service/component
          ComponentPlugin plugin = 
              cluster.getComponentDefinition(service.getServiceName());
          //check whether the agent should start any server
          if (role.shouldStart()) {
            if (!roleInstalled) {
              Action action = plugin.install(cluster.getName(), 
                                             role.getRoleName());
              continue;
            }
            if (!roleServerRunning) {
              //TODO: keep track of retries (via checkActionResults)
              Action action = 
                  plugin.startServer(cluster.getName(), role.getRoleName());
              addAction(action, allActions);
            }
            //raise an event to the state machine for a successful role-start
            if (roleServerRunning) {
              StateMachineInvoker.getAMBARIEventHandler()
              .handle(new RoleEvent(RoleEventType.START_SUCCESS, role));
            }
          }
          //check whether the agent should stop any server
          if (role.shouldStop()) {
            if (roleServerRunning) {
              //TODO: keep track of retries (via checkActionResults)
              addAction(getStopRoleAction(cluster.getID(), 
                  cluster.getLatestRevision(), 
                  role.getAssociatedService().getServiceName(), 
                  role.getRoleName()), allActions);
            }
            //raise an event to the state machine for a successful role-stop
            if (!roleServerRunning) {
              StateMachineInvoker.getAMBARIEventHandler()
                .handle(new RoleEvent(RoleEventType.STOP_SUCCESS, role));
            }
            if (roleInstalled && 
                clusterFsm.getClusterState()
                  .equals(ClusterState.CLUSTER_STATE_ATTIC)) {
              Action action = plugin.uninstall(cluster.getName(), 
                                               role.getRoleName());
              addAction(action, allActions);
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
  
  private static class InstalledOrStartedComponents {
    //Convenience class to aid in heartbeat processing
    //TODO: do this in a better way (define Map, etc.)
    private enum State {INSTALLED,STARTED}
    private Map<String, Map<String, State>> componentServerMap =
        new HashMap<String, Map<String, State>>();
    void roleInstalled(String component, String role) {
      recordState(component,role,State.INSTALLED);
    }
    void roleServerStarted(String component, String roleServer) {
      recordState(component,roleServer,State.STARTED);
    }
    boolean isStarted(String component, String server) {
      Map<String, State> startedServerMap;
      if ((startedServerMap = componentServerMap.get(component)) 
          != null) {
        State state = startedServerMap.get(server);
        return state == State.STARTED;
      }
      return false;
    }
    boolean isInstalled(String component, String role) {
      Map<String, State> startedServerMap;
      if ((startedServerMap = componentServerMap.get(component)) 
          != null) {
        State state = startedServerMap.get(role);
        return state == State.STARTED || state == State.INSTALLED;
      }
      return false;
    }
    void recordState(String component, String roleServer, State state) {
      Map<String, State> serverStartedMap = null;
      if ((serverStartedMap = componentServerMap.get(component))
          != null) {
        serverStartedMap.put(roleServer, state);
        return;
      }
      serverStartedMap = new HashMap<String, State>();
      serverStartedMap.put(roleServer, state);
      componentServerMap.put(component, serverStartedMap);
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
  
  private static void checkActionResults(Cluster cluster,
      ClusterFSM clusterFsm, HeartBeat heartbeat, List<Action> allActions) {
    //this method should check things like number of retries (based on action-IDs)
    //etc. this method should note issues like too many failures starting
    //a role, etc.
    for (ServiceFSM service : clusterFsm.getServices()) {    
      //see whether the service is in the STARTED state, and if so,
      //check whether there is any action-result that indicates success
      //of the availability check (safemode, etc.)
      if (service.getServiceState() == ServiceState.STARTED) {
        //TODO: check with plugin whether this role/node can run the service availability-check
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
  }
  
  private List<Action> inspectAgentState(
      HeartBeat heartbeat, Cluster cluster, 
      InstalledOrStartedComponents componentServers, ClusterFSM desiredCluster)
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
        addAction(getStopRoleAction(agentRoleState.getClusterId(), 
            agentRoleState.getClusterDefinitionRevision(), 
            agentRoleState.getComponentName(), agentRoleState.getRoleName()),
            killAndUninstallCmds);
      }
      if (uninstall) {
        //TODO: get reference to the plugin impl for this service/component
        ComponentPlugin plugin = 
            cluster.getComponentDefinition(agentRoleState.getComponentName());
        Action uninstallAction = 
            plugin.uninstall(cluster.getName(), agentRoleState.getRoleName());
        addAction(uninstallAction, killAndUninstallCmds);
      }
      if (!stopRole && !uninstall) {
        //this must be one of the roles we care about
        componentServers.roleInstalled(agentRoleState.getComponentName(), 
            agentRoleState.getRoleName());
        if (agentRoleState.getServerStatus() == AgentRoleState.State.STARTED) {
          //make a note of the fact that a server is running for reference
          //later
          componentServers.roleServerStarted(agentRoleState.getComponentName(), 
              agentRoleState.getRoleName());
        }
      }
    }
    return killAndUninstallCmds;
  }
  
  private void addAction(Action action, List<Action> allActions) {
    if (action != null) {
      allActions.add(action);
    }
  }
  
  private Action getStopRoleAction(String clusterId, long clusterRev, 
      String componentName, String roleName) {
    Action action = new Action();
    action.setClusterId(clusterId);
    action.setClusterDefinitionRevision(clusterRev);
    action.setRole(roleName);
    action.setComponent(componentName);
    action.setKind(Kind.STOP_ACTION);
    action.setSignal(Signal.KILL);
    return action;
  }
}
