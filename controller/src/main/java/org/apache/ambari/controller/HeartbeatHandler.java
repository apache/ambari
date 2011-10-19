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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.common.rest.entities.ClusterState;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HeartbeatHandler {
  
  private Map<String, ControllerResponse> agentToHeartbeatResponseMap = 
      Collections.synchronizedMap(new HashMap<String, ControllerResponse>());
  
  private static Log LOG = LogFactory.getLog(HeartbeatHandler.class);
  
  public ControllerResponse processHeartBeat(HeartBeat heartbeat) 
      throws Exception {
    ControllerResponse response = 
        agentToHeartbeatResponseMap.get(heartbeat.getHostname());
    if (response != null) {
      if (response.getResponseId() == heartbeat.getResponseId()) {
        return response; //duplicate heartbeat
      }
    }

    short responseId = (short)(heartbeat.getResponseId() + 1);

    String hostname = heartbeat.getHostname();
    Date heartbeatTime = new Date(System.currentTimeMillis());
    String clusterId = null;

    Nodes.getInstance().checkAndUpdateNode(hostname, heartbeatTime);

    List<Action> allActions = new ArrayList<Action>();

    clusterId = Nodes.getInstance().getNode(hostname)
        .getNodeState().getClusterID();
    
    if (clusterId != null) {
      Cluster cluster = 
          Clusters.getInstance().getClusterByID(clusterId);

      if (heartbeat.getIdle()) {
        //if the command-execution takes longer than one heartbeat interval
        //the check for idleness will prevent the same node getting the same 
        //command more than once. In the future this could be improved
        //to reflect the command execution state more accurately.

        ComponentAndRoleStates componentStates = 
            new ComponentAndRoleStates();
        //get the state machine reference to the cluster
        ClusterFSM clusterFsm = StateMachineInvoker
            .getStateMachineClusterInstance(clusterId);

        //create some datastructures by looking at agent state
        inspectAgentState(heartbeat, componentStates);

        //the state machine references to the services
        List<ServiceFSM> clusterServices = clusterFsm.getServices();
        //go through all the services, and check which role should be started
        //TODO: Given that we already look at what is running/installed in 
        //inspectAgentState, maybe we can avoid the following for loop.
        for (ServiceFSM service : clusterServices) {
          List<RoleFSM> roles = service.getRoles();

          for (RoleFSM role : roles) {
            boolean nodePlayingRole = 
                nodePlayingRole(hostname, role.getRoleName());
            if (nodePlayingRole) {
              boolean roleInstalled = componentStates.isInstalled(
                  role.getAssociatedService().getServiceName(), 
                  role.getRoleName());     
              boolean roleServerRunning = componentStates.isStarted(
                  role.getAssociatedService().getServiceName(),
                  role.getRoleName()) 
                  || componentStates.isStartInProgress(
                      role.getAssociatedService().getServiceName(), 
                      role.getRoleName());
              ComponentPlugin plugin = 
                  cluster.getComponentDefinition(service.getServiceName());
              //check whether the agent should start any server
              if (role.shouldStart()) {
                if (!roleInstalled) {
                  //send action for installing the role
                  Action action = plugin.install(cluster.getName(), 
                      role.getRoleName());
                  fillDetailsAndAddAction(action, allActions, clusterId,
                      cluster.getLatestRevision(), service.getServiceName(), 
                      role.getRoleName());
                  //send action for configuring
                  action = plugin.configure(clusterId, role.getRoleName());
                  fillDetailsAndAddAction(action, allActions, clusterId,
                      cluster.getLatestRevision(), service.getServiceName(), 
                      role.getRoleName());
                  continue;
                }
                if (role.getRoleName().equals("CLIENT")) { //TODO: have a good place to define this
                  //Client roles are special cases. They don't have any active servers
                  //but should be considered active when installed. Setting the 
                  //boolean to true ensures that the FSM gets an event (albeit a fake one).
                  roleServerRunning = true;
                }
                if (!roleServerRunning) {
                  //TODO: keep track of retries (via checkActionResults)
                  Action action = 
                      plugin.startServer(cluster.getName(), role.getRoleName());
                  fillDetailsAndAddAction(action, allActions, clusterId,
                      cluster.getLatestRevision(), service.getServiceName(), 
                      role.getRoleName());
                }
                //raise an event to the state machine for a successful 
                //role-start instance
                if (roleServerRunning) {
                  StateMachineInvoker.getAMBARIEventHandler()
                  .handle(new RoleEvent(RoleEventType.START_SUCCESS, role));
                }
              }
              //check whether the agent should stop any server
              if (role.shouldStop()) {
                if (role.getRoleName().equals("CLIENT")) { //TODO: have a good place to define this
                  //Client roles are special cases. Setting the 
                  //boolean to false ensures that the FSM gets an event (albeit a fake one) 
                  roleServerRunning = false;
                }
                if (roleServerRunning) {
                  //TODO: keep track of retries (via checkActionResults)
                  addAction(getStopRoleAction(cluster.getID(), 
                      cluster.getLatestRevision(), 
                      role.getAssociatedService().getServiceName(), 
                      role.getRoleName()), allActions);
                }
                //raise an event to the state machine for a successful 
                //role-stop instance
                if (!roleServerRunning) {
                  StateMachineInvoker.getAMBARIEventHandler()
                  .handle(new RoleEvent(RoleEventType.STOP_SUCCESS, role));
                }
                if (roleInstalled && 
                    clusterFsm.getClusterState()
                    .equals(ClusterState.CLUSTER_STATE_ATTIC)) {
                  Action action = plugin.uninstall(cluster.getName(), 
                      role.getRoleName());
                  fillDetailsAndAddAction(action, allActions, cluster.getID(), 
                      cluster.getLatestRevision(), 
                      role.getAssociatedService().getServiceName(), 
                      role.getRoleName());
                }
              }
            }
            //check/create the special component/service-level 
            //actions (like safemode check)
            checkAndCreateActions(cluster, clusterFsm, service, heartbeat, 
                allActions, componentStates);
          }
        }
      }
    }
    ControllerResponse r = new ControllerResponse();
    r.setResponseId(responseId);
    r.setActions(allActions);
    agentToHeartbeatResponseMap.put(heartbeat.getHostname(), r);
    return r;
  }
  
  private static class ComponentAndRoleStates {
    //Convenience class to aid in heartbeat processing
    private Map<String, Map<String, AgentRoleState>> componentRoleMap =
        new HashMap<String, Map<String, AgentRoleState>>();
    private Map<String, ActionResult> actionIds = 
        new HashMap<String, ActionResult>();
    
    void recordRoleState(AgentRoleState state) {
      recordState(state.getComponentName(),state.getRoleName(),state);
    }
    
    boolean isRoleInstalled(String role) {
      //problematic in the case where role is not unique (like 'client')
      //TODO: no iteration please
      Set<Map.Entry<String, Map<String, AgentRoleState>>> entrySet = 
          componentRoleMap.entrySet();
      for (Map.Entry<String, Map<String, AgentRoleState>> entry : entrySet) {
        if (entry.getValue().containsKey(role)) {
          return true;
        }
      }
      return false;
    }
    
    boolean isStarted(String component, String role) {
      Map<String, AgentRoleState> startedServerMap;
      if ((startedServerMap = componentRoleMap.get(component)) 
          != null) {
        AgentRoleState state = startedServerMap.get(role);
        return state.getServerStatus() == AgentRoleState.State.STARTED;
      }
      return false;
    }
    boolean isStartInProgress(String component, String role) {
      Map<String, AgentRoleState> startedServerMap;
      if ((startedServerMap = componentRoleMap.get(component)) 
          != null) {
        AgentRoleState state = startedServerMap.get(role);
        return state.getServerStatus() == AgentRoleState.State.STARTED ||
            state.getServerStatus() == AgentRoleState.State.STARTING;
      }
      return false;
    }
    boolean isInstalled(String component, String role) {
      Map<String, AgentRoleState> startedRoleMap;
      if ((startedRoleMap = componentRoleMap.get(component)) 
          != null) {
        AgentRoleState state = startedRoleMap.get(role);
        return state != null;
      }
      return false;
    }
    void recordActionId(String actionId, ActionResult actionResult) {
      actionIds.put(actionId, actionResult);
    }
    ActionResult getActionResult(String id) {
      return actionIds.get(id);
    }
    private void recordState(String component, String roleServer, AgentRoleState state) {
      Map<String, AgentRoleState> serverStartedMap = null;
      if ((serverStartedMap = componentRoleMap.get(component))
          != null) {
        serverStartedMap.put(roleServer, state);
        return;
      }
      serverStartedMap = new HashMap<String, AgentRoleState>();
      serverStartedMap.put(roleServer, state);
      componentRoleMap.put(component, serverStartedMap);
    }
  }
  
  
  private enum SpecialServiceIDs {
      SERVICE_AVAILABILITY_CHECK_ID, SERVICE_PREINSTALL_CHECK_ID
  }  
  
  private static String getSpecialActionID(Cluster cluster, 
      ServiceFSM service, boolean availabilityChecker, 
      boolean preinstallChecker) {
    String id = cluster.getID() + cluster.getLatestRevision() + 
        service.getServiceName();
    if (preinstallChecker) {
      id += SpecialServiceIDs.SERVICE_PREINSTALL_CHECK_ID.toString();
    }
    if (availabilityChecker) {
      id += SpecialServiceIDs.SERVICE_AVAILABILITY_CHECK_ID.toString();
    }
    return id;
  }
  
  private void inspectAgentState(HeartBeat heartbeat, 
      ComponentAndRoleStates componentServers)
          throws IOException {
    List<AgentRoleState> agentRoleStates = 
        heartbeat.getInstalledRoleStates();
    for (AgentRoleState agentRoleState : agentRoleStates) {
      componentServers.recordRoleState(agentRoleState);
    }
    checkActionResults(heartbeat, componentServers);
  }
  
  private void checkActionResults(HeartBeat heartbeat,
      ComponentAndRoleStates installOrStartedComponents) {
    
    List<ActionResult> actionResults = heartbeat.getActionResults();
    
    for (ActionResult actionResult : actionResults) {
      if (actionResult.getId().contains(SpecialServiceIDs
          .SERVICE_AVAILABILITY_CHECK_ID.toString())
          || actionResult.getId().contains(SpecialServiceIDs
              .SERVICE_PREINSTALL_CHECK_ID.toString())) {
        installOrStartedComponents.recordActionId(actionResult.getId(),
            actionResult);
      }
    }   
  }
  
  private void checkAndCreateActions(Cluster cluster,
      ClusterFSM clusterFsm, ServiceFSM service, HeartBeat heartbeat,
      List<Action> allActions, 
      ComponentAndRoleStates installedOrStartedComponents) 
          throws IOException {
    //see whether the service is in the STARTED state, and if so,
    //check whether there is any action-result that indicates success
    //of the availability check (safemode, etc.)
    if (service.getServiceState() == ServiceState.STARTED) {
      String id = getSpecialActionID(cluster, service, true, false);
      ActionResult result = installedOrStartedComponents.getActionResult(id);
      if (result != null) {
        //this action ran
        //TODO: this needs to be generalized so that it handles the case
        //where the service is not available for a couple of checkservice
        //invocations
        if (result.getCommandResult().getExitCode() == 0) {
          StateMachineInvoker.getAMBARIEventHandler().handle(
              new ServiceEvent(ServiceEventType.AVAILABLE_CHECK_SUCCESS,
                  service));
        } else {
          StateMachineInvoker.getAMBARIEventHandler().handle(
              new ServiceEvent(ServiceEventType.AVAILABLE_CHECK_FAILURE,
                  service));
        }
      } else {
        ComponentPlugin plugin = 
            cluster.getComponentDefinition(service.getServiceName());
        String role = plugin.runCheckRole();
        if (installedOrStartedComponents.isRoleInstalled(role)) {
          Action action = plugin.checkService(cluster.getName(), role);
          fillActionDetails(action, cluster.getID(),
              cluster.getLatestRevision(),service.getServiceName(), role);
          action.setId(id);
          action.setKind(Action.Kind.RUN_ACTION);
          addAction(action, allActions);
        }
      }
    }
    
    if (service.getServiceState() == ServiceState.PRESTART) {
      String id = getSpecialActionID(cluster, service, false, true);
      ActionResult result = installedOrStartedComponents.getActionResult(id);
      if (result != null) {
        //this action ran
        if (result.getCommandResult().getExitCode() == 0) {
          StateMachineInvoker.getAMBARIEventHandler().handle(
              new ServiceEvent(ServiceEventType.PRESTART_SUCCESS,
                  service));
        } else {
          StateMachineInvoker.getAMBARIEventHandler().handle(
              new ServiceEvent(ServiceEventType.PRESTART_FAILURE,
                  service));
        }
      } else {
        ComponentPlugin plugin = 
            cluster.getComponentDefinition(service.getServiceName());
        String role = plugin.runPreinstallRole();
        if (installedOrStartedComponents.isRoleInstalled(role)) {
          Action action = plugin.preinstallAction(cluster.getName(), role);
          fillActionDetails(action, cluster.getID(),
              cluster.getLatestRevision(),service.getServiceName(), role);
          action.setId(id);
          action.setKind(Action.Kind.RUN_ACTION);
          addAction(action, allActions);
        }
      }
    }
  }
  
  private boolean nodePlayingRole(String host, String role) 
      throws Exception {
    //TODO: iteration on every call seems avoidable ..
    List<String> nodeRoles = Nodes.getInstance()
                      .getNode(host).getNodeState().getNodeRoleNames();
    return nodeRoles.contains(role);
  }
  
  private void addAction(Action action, List<Action> allActions) {
    if (action != null) {
      allActions.add(action);
    }
  }
  
  private Action getStopRoleAction(String clusterId, long clusterRev, 
      String componentName, String roleName) {
    Action action = new Action();
    fillActionDetails(action, clusterId, clusterRev, componentName, roleName);
    action.setKind(Kind.STOP_ACTION);
    action.setSignal(Signal.KILL);
    return action;
  }
  
  private void fillActionDetails(Action action, String clusterId, 
      long clusterDefRev, String component, String role) {
    if (action == null) {
      return;
    }
    action.setClusterId(clusterId);
    action.setClusterDefinitionRevision(clusterDefRev);
    action.setComponent(component);
    action.setRole(role);
  }
  
  private void fillDetailsAndAddAction(Action action, List<Action> allActions, 
      String clusterId, 
      long clusterDefRev, String component, String role) {
    fillActionDetails(action, clusterId, clusterDefRev, component, role);
    addAction(action, allActions);
  }
}
