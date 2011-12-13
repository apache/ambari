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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.controller.Clusters;
import org.apache.ambari.controller.Nodes;
import org.apache.ambari.common.rest.agent.Action;
import org.apache.ambari.common.rest.agent.Action.Kind;
import org.apache.ambari.common.rest.agent.ActionResult;
import org.apache.ambari.common.rest.agent.AgentRoleState;
import org.apache.ambari.common.rest.agent.Command;
import org.apache.ambari.common.rest.agent.ConfigFile;
import org.apache.ambari.common.rest.agent.ControllerResponse;
import org.apache.ambari.common.rest.agent.HeartBeat;
import org.apache.ambari.components.ComponentPlugin;
import org.apache.ambari.resource.statemachine.ClusterFSM;
import org.apache.ambari.resource.statemachine.FSMDriverInterface;
import org.apache.ambari.resource.statemachine.RoleFSM;
import org.apache.ambari.resource.statemachine.RoleEvent;
import org.apache.ambari.resource.statemachine.RoleEventType;
import org.apache.ambari.resource.statemachine.ServiceEvent;
import org.apache.ambari.resource.statemachine.ServiceEventType;
import org.apache.ambari.resource.statemachine.ServiceFSM;
import org.apache.ambari.resource.statemachine.ServiceState;
import org.apache.ambari.resource.statemachine.StateMachineInvokerInterface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HeartbeatHandler {
  
  private Map<String, ControllerResponse> agentToHeartbeatResponseMap = 
      Collections.synchronizedMap(new HashMap<String, ControllerResponse>());
  
  private static Log LOG = LogFactory.getLog(HeartbeatHandler.class);
  private final Clusters clusters;
  private final Nodes nodes;
  private StateMachineInvokerInterface stateMachineInvoker;
  private FSMDriverInterface driver;
    
  @Inject
  HeartbeatHandler(Clusters clusters, Nodes nodes, 
      FSMDriverInterface driver, 
      StateMachineInvokerInterface stateMachineInvoker) {
    this.clusters = clusters;
    this.nodes = nodes;
    this.driver = driver;
    this.stateMachineInvoker = stateMachineInvoker;
  }
  
  public ControllerResponse processHeartBeat(HeartBeat heartbeat) 
      throws Exception {
    String hostname = heartbeat.getHostname();
    Date heartbeatTime = new Date(System.currentTimeMillis());
    nodes.checkAndUpdateNode(hostname, heartbeatTime);
    
    ControllerResponse prevResponse = 
        agentToHeartbeatResponseMap.get(heartbeat.getHostname());
    if (prevResponse != null) {
      if (prevResponse.getResponseId() != heartbeat.getResponseId()) {
        return prevResponse; //duplicate heartbeat or the agent restarted
      }
    }

    short responseId = (short)(heartbeat.getResponseId() + 1);
    String clusterName = null;
    int clusterRev = 0;

    List<Action> allActions = new ArrayList<Action>();

    //if the command-execution takes longer than one heartbeat interval
    //the check for idleness will prevent the same node getting more 
    //commands. In the future this could be improved
    //to reflect the command execution state more accurately.
    if (heartbeat.getIdle()) {
      
      List<ClusterNameAndRev> clustersNodeBelongsTo = 
          getClustersNodeBelongsTo(hostname);
      
      if (clustersNodeBelongsTo.isEmpty()) {
        return createResponse(responseId, allActions, heartbeat);
      }
      
      //TODO: have an API in Clusters that can return a script 
      //pertaining to all clusters
      String script = 
          clusters.getInstallAndConfigureScript(
              clustersNodeBelongsTo.get(0).getClusterName(), 
              clustersNodeBelongsTo.get(0).getRevision());
      
      //send the deploy script
      getInstallAndConfigureAction(script, allActions);

      if (!installAndConfigDone(script,heartbeat)) {
        return createResponse(responseId,allActions,heartbeat);
      }

      for (ClusterNameAndRev clusterIdAndRev : clustersNodeBelongsTo) {
        clusterName = clusterIdAndRev.getClusterName();
        clusterRev = clusterIdAndRev.getRevision();

        //get the cluster object corresponding to the clusterId
        Cluster cluster = clusters.getClusterByName(clusterName);
        //get the state machine reference to the cluster
        ClusterFSM clusterFsm = 
            driver.getFSMClusterInstance(clusterName);

        //the state machine references to the services
        List<ServiceFSM> clusterServices = clusterFsm.getServices();
        //go through all the components, and check which role should be started
        for (ServiceFSM service : clusterServices) {
          List<RoleFSM> roles = service.getRoles();
          for (RoleFSM role : roles) {
            boolean nodePlayingRole = 
                nodePlayingRole(hostname, role.getRoleName());
            if (nodePlayingRole) {
              ComponentPlugin plugin = 
                  cluster.getComponentDefinition(service.getServiceName());

              //check whether the agent should start any server
              if (role.shouldStart()) {
                Action action = 
                    plugin.startServer(cluster.getName(), role.getRoleName());
                fillDetailsAndAddAction(action, allActions, clusterName,
                    clusterRev, service.getServiceName(), 
                    role.getRoleName());
                //check the expected state of the agent and whether the start
                //was successful
                if (wasStartRoleSuccessful(clusterIdAndRev, 
                    service.getServiceName(), role.getRoleName(), prevResponse, 
                    heartbeat)) {
                  //raise an event to the state machine for a successful 
                  //role-start
                  stateMachineInvoker.getAMBARIEventHandler()
                  .handle(new RoleEvent(RoleEventType.START_SUCCESS, role));
                }
              }
              //check whether the agent should stop any server
              //note that the 'stop' is implicit - if the heartbeat response
              //doesn't contain the fact that role should be starting/running, 
              //the agent stops it
              if (role.shouldStop()) {
                //raise an event to the state machine for a successful 
                //role-stop instance
                if (wasStopRoleSuccessful(clusterIdAndRev, 
                    service.getServiceName(), role.getRoleName(), prevResponse, 
                    heartbeat)) {
                  stateMachineInvoker.getAMBARIEventHandler()
                  .handle(new RoleEvent(RoleEventType.STOP_SUCCESS, role));
                }
              }
            }
          }
          //check/create the special component/service-level 
          //actions (like safemode check). Only once per component.
          checkAndCreateActions(cluster, clusterFsm, clusterIdAndRev,
              service, heartbeat, allActions);
        }
      }
    }
    return createResponse(responseId,allActions,heartbeat);
  }
  
  private ControllerResponse createResponse(short responseId, 
      List<Action> allActions, HeartBeat heartbeat) {
    ControllerResponse r = new ControllerResponse();
    r.setResponseId(responseId);
    if (allActions.size() > 0) {//TODO: REMOVE THIS
      Action a = new Action();
      a.setKind(Kind.NO_OP_ACTION);
      allActions.add(a);
    }
    r.setActions(allActions);
    agentToHeartbeatResponseMap.put(heartbeat.getHostname(), r);
    return r;
  }
  
  private boolean installAndConfigDone(String script, HeartBeat heartbeat) {
    if (script == null || heartbeat.getInstallScriptHash() == -1) {
      return false;
    }
    if (script.hashCode() == heartbeat.getInstallScriptHash()) {
      return true;
    }
    return false;
  }
    
  private boolean wasStartRoleSuccessful(ClusterNameAndRev clusterIdAndRev, 
      String component, String roleName, ControllerResponse response, 
      HeartBeat heartbeat) {
    List<AgentRoleState> serverStates = heartbeat.getInstalledRoleStates();
    if (serverStates == null) {
      return false;
    }

    //TBD: create a hashmap (don't iterate for every server state)
    for (AgentRoleState serverState : serverStates) {
      if (serverState.getClusterId().equals(clusterIdAndRev.getClusterName()) &&
          serverState.getClusterDefinitionRevision() == clusterIdAndRev.getRevision() &&
          serverState.getComponentName().equals(component) &&
          serverState.getRoleName().equals(roleName)) {
        return true;
      }
    }
    return false;
  }
  
  private void getInstallAndConfigureAction(String script, 
      List<Action> allActions) {
    ConfigFile file = new ConfigFile();
    file.setData(script);
    
    Action action = new Action();
    action.setFile(file);
    action.setKind(Kind.INSTALL_AND_CONFIG_ACTION);
    //in the action ID send the hashCode of the script content so that 
    //the controller can check how the installation went when a heartbeat
    //response is sent back
    action.setId(Integer.toString(script.hashCode()));
    allActions.add(action);
  }
  
  private boolean wasStopRoleSuccessful(ClusterNameAndRev clusterIdAndRev, 
      String component, String roleName, ControllerResponse response, 
      HeartBeat heartbeat) {
    List<AgentRoleState> serverStates = heartbeat.getInstalledRoleStates();
    if (serverStates == null) {
      return true;
    }
    boolean stopped = true;
    //TBD: create a hashmap (don't iterate for every server state)
    for (AgentRoleState serverState : serverStates) {
      if (serverState.getClusterId().equals(clusterIdAndRev.getClusterName()) &&
          serverState.getClusterDefinitionRevision() == clusterIdAndRev.getRevision() &&
          serverState.getComponentName().equals(component) &&
          serverState.getRoleName().equals(roleName)) {
        stopped = false;
      }
    }
    return stopped;
  }
  
  private ActionResult getActionResult(HeartBeat heartbeat, String id) {
    List<ActionResult> actionResults = heartbeat.getActionResults();
    if (actionResults == null) {
      return null;
    }
    for (ActionResult result : actionResults) {
      if (result.getId().equals(id)) {
        return result;
      }
    }
    return null;
  }
  
  private List<ClusterNameAndRev> getClustersNodeBelongsTo(String hostname) 
      throws Exception {
    String clusterName = nodes.getNode(hostname)
        .getNodeState().getClusterName();
    if (clusterName != null) {
      int clusterRev = clusters.
          getClusterByName(clusterName).getLatestRevisionNumber();
      List<ClusterNameAndRev> l = new ArrayList<ClusterNameAndRev>();
      l.add(new ClusterNameAndRev(clusterName, clusterRev));
      return l;
    }
    return new ArrayList<ClusterNameAndRev>(); //empty
  }  
  
  enum SpecialServiceIDs {
      SERVICE_AVAILABILITY_CHECK_ID, SERVICE_PRESTART_CHECK_ID,
      CREATE_STRUCTURE_ACTION_ID
  }
  
  
  static class ClusterNameAndRev implements 
  Comparable<ClusterNameAndRev> {
    String clusterName;
    int revision;
    ClusterNameAndRev(String clusterName, int revision) {
      this.clusterName = clusterName;
      this.revision = revision;
    }
    String getClusterName() {
      return clusterName;
    }
    int getRevision() {
      return revision;
    }
    @Override
    public int hashCode() {
      //note we only consider cluster names (one node can't have
      //more than one version of components of the same cluster name 
      //installed)
      return clusterName.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      //note we only compare cluster names (one node can't have
      //more than one version of components of the same cluster name 
      //installed)
      return this.clusterName.equals(((ClusterNameAndRev)obj).getClusterName());
    }
    @Override
    public int compareTo(ClusterNameAndRev o) {
      return o.getClusterName().compareTo(getClusterName());
    }
  }

  static String getSpecialActionID(ClusterNameAndRev clusterNameAndRev, 
      String component, String role, SpecialServiceIDs serviceId) {
    String id = clusterNameAndRev.getClusterName() +"-"+
      clusterNameAndRev.getRevision() +"-"+ component + "-";
    if (role != null) {
      id += role + "-";
    }
    id += serviceId.toString();
    return id;
  }
  
  private void checkAndCreateActions(Cluster cluster,
      ClusterFSM clusterFsm, ClusterNameAndRev clusterIdAndRev, 
      ServiceFSM service, HeartBeat heartbeat, 
      List<Action> allActions) throws Exception {
    ComponentPlugin plugin = 
        cluster.getComponentDefinition(service.getServiceName());
    //see whether the service is in the STARTED state, and if so,
    //check whether there is any action-result that indicates success
    //of the availability check (safemode, etc.)
    if (service.getServiceState() == ServiceState.STARTED) {
      String role = plugin.runCheckRole();  
      if (nodePlayingRole(heartbeat.getHostname(), role)) {
        String id = getSpecialActionID(clusterIdAndRev, service.getServiceName(), 
            role, SpecialServiceIDs.SERVICE_AVAILABILITY_CHECK_ID);
        ActionResult result = getActionResult(heartbeat, id);
        if (result != null) {
          //this action ran
          //TODO: this needs to be generalized so that it handles the case
          //where the service is not available for a couple of checkservice
          //invocations
          if (result.getCommandResult().getExitCode() == 0) {
            stateMachineInvoker.getAMBARIEventHandler().handle(
                new ServiceEvent(ServiceEventType.AVAILABLE_CHECK_SUCCESS,
                    service));
          } else {
            stateMachineInvoker.getAMBARIEventHandler().handle(
                new ServiceEvent(ServiceEventType.AVAILABLE_CHECK_FAILURE,
                    service));
          }
        } else {
          Action action = plugin.checkService(cluster.getName(), role);
          fillActionDetails(action, clusterIdAndRev.getClusterName(),
              clusterIdAndRev.getRevision(),service.getServiceName(), role);
          action.setId(id);
          action.setKind(Action.Kind.RUN_ACTION);
          addAction(action, allActions);
        }
      }
    }
    
    if (service.getServiceState() == ServiceState.PRESTART) {
      String role = plugin.runPreStartRole();
      if (nodePlayingRole(heartbeat.getHostname(), role)) {
        String id = getSpecialActionID(clusterIdAndRev, service.getServiceName(), 
            role, SpecialServiceIDs.SERVICE_PRESTART_CHECK_ID);
        ActionResult result = getActionResult(heartbeat, id);
        if (result != null) {
          //this action ran
          if (result.getCommandResult().getExitCode() == 0) {
            stateMachineInvoker.getAMBARIEventHandler().handle(
                new ServiceEvent(ServiceEventType.PRESTART_SUCCESS,
                    service));
          } else {
            stateMachineInvoker.getAMBARIEventHandler().handle(
                new ServiceEvent(ServiceEventType.PRESTART_FAILURE,
                    service));
          }
        } else {
          Action action = plugin.preStartAction(cluster.getName(), role);
          fillActionDetails(action, clusterIdAndRev.getClusterName(),
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
    List<String> nodeRoles = nodes.getNodeRoles(host);
    return nodeRoles.contains(role);
  }
  
  private void addAction(Action action, List<Action> allActions) {
    if (action != null) {
      allActions.add(action);
    }
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
    action.setCleanUpCommand(new Command("foobar","",new String[]{"foobar"}));//TODO: this needs fixing at some point
    String workDir = role.equals(component + "-client") ? 
        (clusterId + "-client") : (clusterId + "-" + role);
    action.setWorkDirectoryComponent(workDir);
  }
  
  private void fillDetailsAndAddAction(Action action, List<Action> allActions, 
      String clusterId, 
      long clusterDefRev, String component, String role) {
    fillActionDetails(action, clusterId, clusterDefRev, component, role);
    addAction(action, allActions);
  }
}
