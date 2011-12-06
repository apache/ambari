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
import org.apache.ambari.common.rest.agent.Command;
import org.apache.ambari.common.rest.agent.ConfigFile;
import org.apache.ambari.common.rest.agent.ControllerResponse;
import org.apache.ambari.common.rest.agent.HeartBeat;
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

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HeartbeatHandler {
  
  private Map<String, ControllerResponse> agentToHeartbeatResponseMap = 
      Collections.synchronizedMap(new HashMap<String, ControllerResponse>());
  
  private static Log LOG = LogFactory.getLog(HeartbeatHandler.class);
  private final Clusters clusters;
  private final Nodes nodes;
  
  @Inject
  HeartbeatHandler(Clusters clusters, Nodes nodes) {
    this.clusters = clusters;
    this.nodes = nodes;
  }
  
  public ControllerResponse processHeartBeat(HeartBeat heartbeat) 
      throws Exception {
    String hostname = heartbeat.getHostname();
    Date heartbeatTime = new Date(System.currentTimeMillis());
    nodes.checkAndUpdateNode(hostname, heartbeatTime);
    
    ControllerResponse response = 
        agentToHeartbeatResponseMap.get(heartbeat.getHostname());
    if (response != null) {
      if (response.getResponseId() == heartbeat.getResponseId()) {
        return response; //duplicate heartbeat
      }
    }

    short responseId = (short)(heartbeat.getResponseId() + 1);
    String clusterName = null;
    int clusterRev = 0;

    List<Action> allActions = new ArrayList<Action>();

    //if the command-execution takes longer than one heartbeat interval
    //the check for idleness will prevent the same node getting the same 
    //command more than once. In the future this could be improved
    //to reflect the command execution state more accurately.
    if (heartbeat.getIdle()) {
      List<ClusterNameAndRev> clustersNodeBelongsTo = 
          getClustersNodeBelongsTo(hostname);

      for (ClusterNameAndRev clusterIdAndRev : clustersNodeBelongsTo) {

        String script = 
            clusters.getInstallAndConfigureScript(clusterName, 
                clusterRev);
        
        //send the deploy script
        getInstallAndConfigureAction(script,clusterIdAndRev, allActions);

        //get the cluster object corresponding to the clusterId
        Cluster cluster = clusters.getClusterByName(clusterName);
        //get the state machine reference to the cluster
        ClusterFSM clusterFsm = StateMachineInvoker
            .getStateMachineClusterInstance(clusterName);

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
                    role.getRoleName(), response, heartbeat)) {
                  //raise an event to the state machine for a successful 
                  //role-start
                  StateMachineInvoker.getAMBARIEventHandler()
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
                    role.getRoleName(), response, heartbeat)) {
                  StateMachineInvoker.getAMBARIEventHandler()
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
    ControllerResponse r = new ControllerResponse();
    r.setResponseId(responseId);
    //TODO: need to persist this state (if allActions are different from the 
    //last allActions)
    r.setActions(allActions);
    agentToHeartbeatResponseMap.put(heartbeat.getHostname(), r);
    return r;
  }
  
  private boolean wasStartRoleSuccessful(ClusterNameAndRev clusterIdAndRev, 
      String roleName, ControllerResponse response, HeartBeat heartbeat) {
    //Check whether the statechange was successful on the agent, and if
    //the state information sent to the agent in the previous heartbeat
    //included the start-action for the role in question.
    if (!heartbeat.getStateChangeStatus()) {
      return false;
    }
    List<Action> actions = response.getActions();
    for (Action action : actions) { //TBD: no iteration for every invocation of this method
      if (action.kind != Action.Kind.START_ACTION) {
        continue;
      }
      if (action.getClusterId().equals(clusterIdAndRev.getClusterName()) && 
          action.getClusterDefinitionRevision() == 
          clusterIdAndRev.getRevision() &&
          action.getRole().equals(roleName)) {
        return true;
      }
    }
    return false;
  }
  
  private void getInstallAndConfigureAction(String script, 
      ClusterNameAndRev clusterNameRev, List<Action> allActions) {
    ConfigFile file = new ConfigFile();
    file.setData(script);
    //TODO: this should be written in Ambari's scratch space directory
    file.setPath("/tmp/" + clusterNameRev.getClusterName() 
        + "_" + clusterNameRev.getRevision());
    
    Action action = new Action();
    action.setFile(file);
    action.setClusterId(clusterNameRev.getClusterName());
    action.setClusterDefinitionRevision(clusterNameRev.getRevision());
    action.setKind(Kind.WRITE_FILE_ACTION);
    allActions.add(action);
    
    action = new Action();
    action.setClusterId(clusterNameRev.getClusterName());
    action.setClusterDefinitionRevision(clusterNameRev.getRevision());
    String deployCmd = Util.getInstallAndConfigureCommand();
    //TODO: assumption is that the file is passed as an argument
    //Should generally hold for many install/config systems like Puppet
    //but is something that needs to be thought about more
    Command command = new Command(null,deployCmd,new String[]{file.getPath()});
    action.setCommand(command);
    action.setKind(Kind.RUN_ACTION);
    allActions.add(action);
  }
  
  private boolean wasStopRoleSuccessful(ClusterNameAndRev clusterIdAndRev, 
      String roleName, ControllerResponse response, HeartBeat heartbeat) {
    //Check whether the statechange was successful on the agent, and if
    //the state information to the agent included the start-action for the
    //role in question.If the state information didn't include the start-action
    //command, the controller wants the role stopped
    if (!heartbeat.getStateChangeStatus()) {
      return false;
    }
    List<Action> actions = response.getActions();
    for (Action action : actions) {
      if (action.getClusterId() == clusterIdAndRev.getClusterName() && 
          action.getClusterDefinitionRevision() == 
          clusterIdAndRev.getRevision() &&
          action.getRole().equals(roleName) &&
          action.kind == Action.Kind.START_ACTION) {
        return false;
      }
    }
    return true;
  }
  
  private ActionResult getActionResult(HeartBeat heartbeat, String id) {
    List<ActionResult> actionResults = heartbeat.getActionResults();
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
  
  private enum SpecialServiceIDs {
      SERVICE_AVAILABILITY_CHECK_ID, SERVICE_PRESTART_CHECK_ID,
      CREATE_STRUCTURE_ACTION_ID
  }  
  
  private static class ClusterNameAndRev implements 
  Comparable<ClusterNameAndRev> {
    String clusterName;
    long revision;
    ClusterNameAndRev(String clusterName, long revision) {
      this.clusterName = clusterName;
      this.revision = revision;
    }
    String getClusterName() {
      return clusterName;
    }
    long getRevision() {
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

  private static String getSpecialActionID(ClusterNameAndRev clusterNameAndRev, 
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
      List<Action> allActions) 
          throws Exception {
    //see whether the service is in the STARTED state, and if so,
    //check whether there is any action-result that indicates success
    //of the availability check (safemode, etc.)
    if (service.getServiceState() == ServiceState.STARTED) {
      String id = getSpecialActionID(clusterIdAndRev, service.getServiceName(), 
          null, SpecialServiceIDs.SERVICE_AVAILABILITY_CHECK_ID);
      ActionResult result = getActionResult(heartbeat, id);
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
        if (nodePlayingRole(heartbeat.getHostname(), role)) {
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
      String id = getSpecialActionID(clusterIdAndRev, service.getServiceName(), 
          null, SpecialServiceIDs.SERVICE_PRESTART_CHECK_ID);
      ActionResult result = getActionResult(heartbeat, id);
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
        String role = plugin.runPreStartRole();
        if (nodePlayingRole(heartbeat.getHostname(), role)) {
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
    List<String> nodeRoles = nodes.getNode(host).getNodeState().
        getNodeRoleNames();
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
