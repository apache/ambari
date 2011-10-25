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
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

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
    long clusterRev = 0L;

    Nodes.getInstance().checkAndUpdateNode(hostname, heartbeatTime);

    List<Action> allActions = new ArrayList<Action>();

    clusterId = Nodes.getInstance().getNode(hostname)
        .getNodeState().getClusterID();
    if (clusterId != null) {
      clusterRev = Clusters.getInstance().
          getClusterByID(clusterId).getLatestRevision(); 
    }
    
    ComponentAndRoleStates componentStates = 
        new ComponentAndRoleStates();
    //create some datastructures by looking at agent state
    inspectAgentState(heartbeat, componentStates);
    
    //get the clusters the node belongs to
    Set<ClusterIdAndRev> clustersNodeBelongsTo = 
        componentStates.getClustersNodeBelongsTo();
    
    //if the command-execution takes longer than one heartbeat interval
    //the check for idleness will prevent the same node getting the same 
    //command more than once. In the future this could be improved
    //to reflect the command execution state more accurately.
    if (heartbeat.getIdle()) {  
      for (ClusterIdAndRev clusterIdAndRev : clustersNodeBelongsTo) {
        //check whether this node is out-of-sync w.r.t what's running &
        //installed, or is it compatible
        if (!isCompatible(clusterIdAndRev.getClusterId(), 
            clusterIdAndRev.getRevision(), clusterId, clusterRev)) {
          createStopAndUninstallActions(componentStates, allActions, 
              clusterIdAndRev, true);
          continue;
        }
        //get the cluster object corresponding to the clusterId
        Cluster cluster = Clusters.getInstance()
            .getClusterByID(clusterIdAndRev.getClusterId());
        //get the state machine reference to the cluster
        ClusterFSM clusterFsm = StateMachineInvoker
            .getStateMachineClusterInstance(clusterIdAndRev.getClusterId());

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
                  clusterIdAndRev,
                  role.getAssociatedService().getServiceName(), 
                  role.getRoleName());     
              boolean roleServerRunning = componentStates.isStarted(
                  clusterIdAndRev,
                  role.getAssociatedService().getServiceName(),
                  role.getRoleName()) 
                  || componentStates.isStartInProgress(clusterIdAndRev,
                      role.getAssociatedService().getServiceName(), 
                      role.getRoleName());
              boolean agentRoleStateChanged = componentStates.hasStateChanged(
                  clusterIdAndRev, role.getAssociatedService().getServiceName(), 
                  role.getRoleName());
              ComponentPlugin plugin = 
                  cluster.getComponentDefinition(service.getServiceName());
              
              //check whether the agent should start any server
              if (role.shouldStart()) {
                if (!roleInstalled) {
                  //send action for creating dir structure
                  Action action = new Action();
                  action.setKind(Kind.CREATE_STRUCTURE_ACTION);
                  fillDetailsAndAddAction(action, allActions, clusterId,
                      clusterRev, service.getServiceName(), 
                      role.getRoleName());
                  
                  //send action for installing the role
                  action = plugin.install(cluster.getName(), 
                      role.getRoleName());
                  fillDetailsAndAddAction(action, allActions, clusterId,
                      clusterRev, service.getServiceName(), 
                      role.getRoleName());
                  
                  //send action for configuring
                  action = plugin.configure(clusterId, role.getRoleName());
                  fillDetailsAndAddAction(action, allActions, clusterId,
                      clusterRev, service.getServiceName(), 
                      role.getRoleName());
                  continue;
                }
                if (role.getRoleName().equals("-client")) { //TODO: have a good place to define this
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
                      clusterRev, service.getServiceName(), 
                      role.getRoleName());
                }
                //raise an event to the state machine for a successful 
                //role-start instance
                if (roleServerRunning && agentRoleStateChanged) {
                  StateMachineInvoker.getAMBARIEventHandler()
                  .handle(new RoleEvent(RoleEventType.START_SUCCESS, role));
                }
                componentStates.
                  continueRunning(clusterIdAndRev, 
                      role.getAssociatedService().getServiceName(), 
                      role.getRoleName());
              }
              //check whether the agent should stop any server
              if (role.shouldStop()) {
                if (role.getRoleName().contains("-client")) { //TODO: have a good place to define this
                  //Client roles are special cases. Setting the 
                  //boolean to false ensures that the FSM gets an event (albeit a fake one) 
                  roleServerRunning = false;
                }
                if (roleServerRunning) {
                  //TODO: keep track of retries (via checkActionResults)
                  addAction(getStopRoleAction(clusterId, 
                      clusterRev, 
                      role.getAssociatedService().getServiceName(), 
                      role.getRoleName()), allActions);
                }
                //raise an event to the state machine for a successful 
                //role-stop instance
                if (!roleServerRunning && agentRoleStateChanged) {
                  StateMachineInvoker.getAMBARIEventHandler()
                  .handle(new RoleEvent(RoleEventType.STOP_SUCCESS, role));
                }
                if (roleInstalled && 
                    clusterFsm.getClusterState()
                    .equals(ClusterState.CLUSTER_STATE_ATTIC)) {
                  Action action = plugin.uninstall(cluster.getName(), 
                      role.getRoleName());
                  fillDetailsAndAddAction(action, allActions, clusterId, 
                      clusterRev, 
                      role.getAssociatedService().getServiceName(), 
                      role.getRoleName());
                }
              }
            }
            //check/create the special component/service-level 
            //actions (like safemode check)
            checkAndCreateActions(cluster, clusterFsm,clusterIdAndRev, service, heartbeat, 
                allActions, componentStates);
          }
        }
        createStopAndUninstallActions(componentStates, allActions, clusterIdAndRev, false);
      }
    }
    ControllerResponse r = new ControllerResponse();
    r.setResponseId(responseId);
    r.setActions(allActions);
    agentToHeartbeatResponseMap.put(heartbeat.getHostname(), r);
    return r;
  }
  
  private void createStopAndUninstallActions(ComponentAndRoleStates componentAndRoleStates, 
      List<Action> allActions, ClusterIdAndRev clusterIdAndRev, boolean forceUninstall) {
    Map<String, 
        Map<String,RoleStateTracker>>
    entrySet = componentAndRoleStates.getAllRoles(clusterIdAndRev);
    for (Map.Entry<String, 
        Map<String,RoleStateTracker>> entry : 
          entrySet.entrySet()) {
      String componentName = entry.getKey();
      Set<Map.Entry<String,RoleStateTracker>> roleSet = entry.getValue().entrySet();
      for (Map.Entry<String,RoleStateTracker> entryVal : roleSet) {
        String roleName = entryVal.getKey();
        if (forceUninstall) {
          addAction(getStopRoleAction(clusterIdAndRev.getClusterId(), 
              clusterIdAndRev.getRevision(), 
              componentName, roleName), allActions);
          addAction(getUninstallRoleAction(clusterIdAndRev.getClusterId(), 
              clusterIdAndRev.getRevision(), 
              componentName, roleName), allActions);
        } else {
          RoleStateTracker stateTracker = entryVal.getValue();
          if (stateTracker.continueRunning) continue;

          addAction(getStopRoleAction(clusterIdAndRev.getClusterId(), 
              clusterIdAndRev.getRevision(), 
              componentName, roleName), allActions);

          if (stateTracker.uninstall)
            addAction(getUninstallRoleAction(clusterIdAndRev.getClusterId(), 
                clusterIdAndRev.getRevision(), 
                componentName, roleName), allActions);
        }
      }
    }
  }
  private static class ComponentAndRoleStates {
    //Convenience class to aid in heartbeat processing
    private Map<ClusterIdAndRev, Map<String, Map<String, RoleStateTracker>>>
    componentRoleMap = new HashMap<ClusterIdAndRev, 
                           Map<String, Map<String, RoleStateTracker>>>();
    
    private Map<String, ActionResult> actionIds = 
        new HashMap<String, ActionResult>();
    
    private static Map<String, List<AgentRoleState>> previousStateMap =
        new ConcurrentHashMap<String, List<AgentRoleState>>();
    
    private Set<ClusterIdAndRev> clusterNodeBelongsTo = 
        new TreeSet<ClusterIdAndRev>();
    
    Map<String,Map<String,RoleStateTracker>> getAllRoles(
        ClusterIdAndRev clusterIdAndRev) {
      return componentRoleMap.get(clusterIdAndRev);
    }
    
    void recordRoleState(String host, AgentRoleState state) {
      ClusterIdAndRev clusterIdAndRev = 
          new ClusterIdAndRev(state.getClusterId(),
              state.getClusterDefinitionRevision());
      clusterNodeBelongsTo.add(clusterIdAndRev);
      
      recordState(clusterIdAndRev,state.getComponentName(),
          state.getRoleName(),state);
      
      List<AgentRoleState> agentRoleStates = null;
      boolean alreadyPresent = false;

      if ((agentRoleStates = previousStateMap.get(host)) != null) {
        for (AgentRoleState agentRoleState : agentRoleStates) {
          if (agentRoleState.roleAttributesEqual(state)) {
            alreadyPresent = true; 
            if (agentRoleState.getServerStatus() != state.getServerStatus()) { 
              //state of the server is different. Record that.
              setStateChanged(clusterIdAndRev,state.getComponentName(),
                  state.getRoleName());
              agentRoleState.setServerStatus(state.getServerStatus());
            }
          }
        }
      } else {
        agentRoleStates = new ArrayList<AgentRoleState>();
        previousStateMap.put(host, agentRoleStates);
      }
      if (!alreadyPresent) {
        agentRoleStates.add(state); 
      }
    }
    
    boolean isRoleInstalled(ClusterIdAndRev clusterIdAndRev, String role) {
      //problematic in the case where role is not unique (like 'client')
      //TODO: no iteration please
      Set<Map.Entry<String, Map<String, RoleStateTracker>>> entrySet = 
          componentRoleMap.get(clusterIdAndRev).entrySet();
      for (Map.Entry<String, Map<String, RoleStateTracker>> entry : entrySet) {
        if (entry.getValue().containsKey(role)) {
          return true;
        }
      }
      return false;
    }
    
    boolean isStarted(ClusterIdAndRev clusterIdAndRev, String component, 
        String role) {
      Map<String,Map<String,RoleStateTracker>> componentsMap = 
          componentRoleMap.get(clusterIdAndRev);
      if (componentsMap == null) {
        return false;
      }
      Map<String, RoleStateTracker> startedServerMap;
      if ((startedServerMap = componentsMap.get(component)) != null) {
        RoleStateTracker state = startedServerMap.get(role);
        if (state == null) 
          return false;
        return state.state == AgentRoleState.State.STARTED;
      }
      return false;
    }
    
    boolean isStartInProgress(ClusterIdAndRev clusterIdAndRev, 
        String component, String role) {
      Map<String,Map<String,RoleStateTracker>> componentsMap = 
          componentRoleMap.get(clusterIdAndRev);
      if (componentsMap == null) {
        return false;
      }
      Map<String, RoleStateTracker> startedServerMap;
      if ((startedServerMap = componentsMap.get(component)) != null) {
        RoleStateTracker state = startedServerMap.get(role);
        if (state != null) {
          return state.state == AgentRoleState.State.STARTED ||
              state.state == AgentRoleState.State.STARTING;
        } else return false;
      }
      return false;
    }
    
    boolean isInstalled(ClusterIdAndRev clusterIdAndRev, 
        String component, String role) {
      Map<String,Map<String,RoleStateTracker>> componentsMap = 
          componentRoleMap.get(clusterIdAndRev);
      if (componentsMap == null) {
        return false;
      }
      Map<String, RoleStateTracker> startedRoleMap;
      if ((startedRoleMap = componentsMap.get(component)) != null) {
        RoleStateTracker state = startedRoleMap.get(role);
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
    private void recordState(ClusterIdAndRev clusterIdAndRev, String component,
        String roleServer, AgentRoleState state) {
      Map<String, Map<String, RoleStateTracker>> componentMap = null;
      
      if ((componentMap = componentRoleMap.get(clusterIdAndRev))
          != null) {
        Map<String,RoleStateTracker> roles = componentMap.get(component);
        RoleStateTracker roleState = new RoleStateTracker(state.getServerStatus(), 
            state.getClusterId(), state.getClusterDefinitionRevision());
        if (roles != null) {
          roles.put(roleServer, roleState);
        } else {
          roles = new HashMap<String, RoleStateTracker>();
          componentMap.put(component, roles);
        }
        return;
      }
      componentMap = new HashMap<String, Map<String,RoleStateTracker>>();
      componentRoleMap.put(clusterIdAndRev, componentMap);
      Map<String,RoleStateTracker> roleMap = new HashMap<String,RoleStateTracker>();
      
      roleMap.put(roleServer, 
          new RoleStateTracker(state.getServerStatus(), 
              state.getClusterId(), state.getClusterDefinitionRevision()));
      componentMap.put(component, roleMap);
    }
    boolean hasStateChanged(ClusterIdAndRev clusterIdAndRev, String component,
        String roleServer) {
      return componentRoleMap.get(clusterIdAndRev).get(component)
          .get(roleServer).stateChanged;
    }
    Set<ClusterIdAndRev> getClustersNodeBelongsTo() {
      return clusterNodeBelongsTo;
    }
    private void setStateChanged(ClusterIdAndRev clusterIdAndRev, 
        String component, String roleServer) {
      componentRoleMap.get(clusterIdAndRev).get(component)
         .get(roleServer).stateChanged = true;
    }
    private void continueRunning(ClusterIdAndRev clusterIdAndRev, 
        String component, String roleServer) {
      componentRoleMap.get(clusterIdAndRev).get(component)
         .get(roleServer).continueRunning = true;
    }
  }
  
  
  private enum SpecialServiceIDs {
      SERVICE_AVAILABILITY_CHECK_ID, SERVICE_PREINSTALL_CHECK_ID
  }  
  
  private static class ClusterIdAndRev {
    String clusterId;
    long revision;
    ClusterIdAndRev(String clusterId, long revision) {
      this.clusterId = clusterId;
      this.revision = revision;
    }
    String getClusterId() {
      return clusterId;
    }
    long getRevision() {
      return revision;
    }
    @Override
    public int hashCode() {
      return (clusterId + String.valueOf(revision)).hashCode();
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      return this.clusterId.equals(((ClusterIdAndRev)obj).getClusterId()) &&
          this.clusterId.equals(((ClusterIdAndRev)obj).getRevision());
    }
  }

  private static class RoleStateTracker {
    AgentRoleState.State state; //the current state of the server
    boolean stateChanged; //whether the state of the server changed 
                          //since the last heartbeat
    boolean continueRunning; //whether the server should continue
                             //running
    boolean uninstall;
    
    RoleStateTracker(AgentRoleState.State state, 
        String clusterId, long clusterRev) {
      this.state = state;
      this.stateChanged = false;
      this.continueRunning = false;
      this.uninstall = false;
    }
  }

  private boolean isCompatible(String nodeClusterId, long nodeClusterRev, 
      String controllerClusterId, long controllerClusterRev) {
    //TODO: make this "smart"
    if (!nodeClusterId.equals(controllerClusterId)) {
      return false;
    }
    if (nodeClusterRev != controllerClusterRev) {
      return false;
    }
    return true;
  }
  private static String getSpecialActionID(ClusterIdAndRev clusterIdAndRev, 
      ServiceFSM service, boolean availabilityChecker, 
      boolean preinstallChecker) {
    String id = clusterIdAndRev.getClusterId() + clusterIdAndRev.getRevision() 
        + service.getServiceName();
    if (preinstallChecker) {
      id += SpecialServiceIDs.SERVICE_PREINSTALL_CHECK_ID.toString();
    }
    if (availabilityChecker) {
      id += SpecialServiceIDs.SERVICE_AVAILABILITY_CHECK_ID.toString();
    }
    return id;
  }
  
  private List<Cluster> inspectAgentState(HeartBeat heartbeat, 
      ComponentAndRoleStates componentServers)
          throws IOException {
    List<AgentRoleState> agentRoleStates = 
        heartbeat.getInstalledRoleStates();
    List<Cluster> clustersNodeBelongsTo = new ArrayList<Cluster>();
    for (AgentRoleState agentRoleState : agentRoleStates) {
      componentServers.recordRoleState(heartbeat.getHostname(),agentRoleState);
      Cluster c = Clusters.getInstance().
          getClusterByID(agentRoleState.getClusterId());
      clustersNodeBelongsTo.add(c);
    }
    checkActionResults(heartbeat, componentServers);
    return clustersNodeBelongsTo;
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
      ClusterFSM clusterFsm, ClusterIdAndRev clusterIdAndRev, 
      ServiceFSM service, HeartBeat heartbeat, List<Action> allActions, 
      ComponentAndRoleStates installedOrStartedComponents) 
          throws IOException {
    //see whether the service is in the STARTED state, and if so,
    //check whether there is any action-result that indicates success
    //of the availability check (safemode, etc.)
    if (service.getServiceState() == ServiceState.STARTED) {
      String id = getSpecialActionID(clusterIdAndRev, service, true, false);
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
        if (installedOrStartedComponents.isRoleInstalled(clusterIdAndRev,role)) {
          Action action = plugin.checkService(cluster.getName(), role);
          fillActionDetails(action, clusterIdAndRev.getClusterId(),
              clusterIdAndRev.getRevision(),service.getServiceName(), role);
          action.setId(id);
          action.setKind(Action.Kind.RUN_ACTION);
          addAction(action, allActions);
        }
      }
    }
    
    if (service.getServiceState() == ServiceState.PRESTART) {
      String id = getSpecialActionID(clusterIdAndRev, service, false, true);
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
        if (installedOrStartedComponents.isRoleInstalled(clusterIdAndRev,role)) {
          Action action = plugin.preinstallAction(cluster.getName(), role);
          fillActionDetails(action, clusterIdAndRev.getClusterId(),
              clusterIdAndRev.getRevision(),service.getServiceName(), role);
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
  
  private Action getUninstallRoleAction(String clusterId, long clusterRev, 
      String componentName, String roleName) {
    Action action = new Action();
    fillActionDetails(action, clusterId, clusterRev, componentName, roleName);
    action.setKind(Kind.DELETE_STRUCTURE_ACTION);
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
