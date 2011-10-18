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

package org.apache.ambari.controller.rest.agent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.common.rest.entities.agent.Action;
import org.apache.ambari.common.rest.entities.agent.Action.Kind;
import org.apache.ambari.common.rest.entities.agent.Action.Signal;
import org.apache.ambari.common.rest.entities.agent.ActionResult;
import org.apache.ambari.common.rest.entities.agent.AgentRoleState;
import org.apache.ambari.common.rest.entities.agent.Command;
import org.apache.ambari.common.rest.entities.agent.CommandResult;
import org.apache.ambari.common.rest.entities.agent.ControllerResponse;
import org.apache.ambari.common.rest.entities.agent.ConfigFile;
import org.apache.ambari.common.rest.entities.agent.HardwareProfile;
import org.apache.ambari.common.rest.entities.agent.HeartBeat;
import org.apache.ambari.common.util.ExceptionUtil;
import org.apache.ambari.controller.HeartbeatHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * Controller Resource represents Ambari controller.
 * It provides API for Ambari agents to get the cluster configuration changes
 * as well as report the node attributes and state of services running the on 
 * the cluster nodes
 */
@Path("controller")
public class ControllerResource {
  private HeartbeatHandler hh = new HeartbeatHandler();
	private static Log LOG = LogFactory.getLog(ControllerResource.class);
  /** 
   * Update state of the node (Internal API to be used by Ambari agent).
   *  
   * @response.representation.200.doc This API is invoked by Ambari agent running
   *  on a cluster to update the state of various services running on the node.
   * @response.representation.200.mediaType application/json
   * @response.representation.406.doc Error in heartbeat message format
   * @response.representation.408.doc Request Timed out
   * @param message Heartbeat message
   * @throws Exception 
   */
  @Path("heartbeat/{hostname}")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public ControllerResponse heartbeat(HeartBeat message) 
      throws WebApplicationException {
    ControllerResponse controllerResponse = getControllerResponse();
    try {
      controllerResponse = hh.processHeartBeat(message);
    } catch (Exception e) {
      LOG.info(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }

//    controllerResponse.setResponseId("id-00002");    
//    String script = "import os\nos._exit(0)";
//    String[] param = { "cluster", "role" };
//    Command command = new Command("root", script, param);
//
//    Command cleanUp = new Command("root", script, param);
//    
//    Action action = new Action();
//    action.setUser("hdfs");
//    action.setKind(Kind.STOP_ACTION);
//    action.setSignal(Signal.KILL);
//    action.setClusterId("cluster-001");
//    action.setClusterDefinitionRevision(1);
//    action.setComponent("hdfs");
//    action.setRole("datanode");
//    action.setId("action-001");
//
//    Action action2 = new Action();
//    action2.setUser("hdfs");
//    action2.setKind(Kind.START_ACTION);
//    action2.setId("action-002");
//    action2.setClusterId("cluster-002");
//    action2.setCommand(command);
//    action2.setCleanUpCommand(cleanUp);
//    action2.setClusterDefinitionRevision(1);
//    action2.setComponent("hdfs");
//    action2.setRole("datanode");
//
//    Action action3 = new Action();
//    action3.setUser("hdfs");
//    action3.setKind(Kind.RUN_ACTION);
//    action3.setId("action-003");
//    action3.setClusterId("cluster-002");
//    action3.setClusterDefinitionRevision(1);
//    action3.setComponent("hdfs");
//    action3.setRole("datanode");
//    action3.setCommand(command);
//    action3.setCleanUpCommand(cleanUp);
//
//    Action action4 = new Action();
//    action4.setId("action-004");
//    action4.setClusterId("cluster-002");
//    action4.setClusterDefinitionRevision(1);
//    action4.setUser("hdfs");
//    action4.setKind(Kind.WRITE_FILE_ACTION);
//    action4.setComponent("hdfs");    
//    action4.setRole("namenode");
//    String owner ="hdfs";
//    String group = "hadoop";
//    String permission = "0700";
//    String path = "$prefix/config";
//    String umask = "022";
//    String data = "Content of the file";
//    action4.setFile(new ConfigFile(owner, group, permission, path, umask, data));
//    
//    List<Action> actions = new ArrayList<Action>();
//    actions.add(action);
//    actions.add(action2);
//    actions.add(action3);
//    actions.add(action4);
//    controllerResponse.setActions(actions);
    return controllerResponse;
  }

  /**
   * Sample Ambari heartbeat message
   * 
   * @response.representation.200.example 
   * {
       "responseId": "-1",
       "timestamp": "1318955147616",
       "hostname": "host.example.com",
       "hardwareProfile": {
           "coreCount": "8",
           "diskCount": "4",
           "ramSize": "16442752",
           "cpuSpeed": "2003",
           "netSpeed": "1000",
           "cpuFlags": "vmx est tm2..."
       },
       "installedRoleStates": [
           {
               "clusterId": "cluster-003",
               "clusterDefinitionRevision": "2",
               "componentName": "hdfs",
               "roleName": "datanode",
               "serverStatus": "STARTED"
           }
       ],
       "actionResults": [
           {
               "clusterId": "cluster-001",
               "id": "action-001",
               "kind": "STOP_ACTION",
               "clusterDefinitionRevision": "1"
           },
           {
               "clusterId": "cluster-002",
               "kind": "START_ACTION",
               "commandResult": {
                   "exitCode": "0",
                   "stdout": "stdout",
                   "stderr": "stderr"
               },
               "cleanUpCommandResult": {
                   "exitCode": "0",
                   "stdout": "stdout",
                   "stderr": "stderr"
               },
               "component": "hdfs",
               "role": "datanode",
               "clusterDefinitionRevision": "2"
           }
       ],
       "idle": "false"
     }
   * @response.representation.200.doc Print example of Ambari heartbeat message
   * @response.representation.200.mediaType application/json
   * @param stackId Stack ID
   * @return Heartbeat message
   */
  @Path("heartbeat/sample")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public HeartBeat getHeartBeat(@DefaultValue("stack-123") 
                                @QueryParam("stackId") String stackId) {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      List<ActionResult> actionResults = new ArrayList<ActionResult>();      

      ActionResult actionResult = new ActionResult();
      actionResult.setClusterDefinitionRevision(1);
      actionResult.setId("action-001");
      actionResult.setClusterId("cluster-001");
      actionResult.setKind(Kind.STOP_ACTION);

      ActionResult actionResult2 = new ActionResult();
      actionResult2.setClusterDefinitionRevision(2);
      actionResult2.setClusterId("cluster-002");
      actionResult2.setCommandResult(new CommandResult(0, "stdout", "stderr"));
      actionResult2.setCleanUpResult(new CommandResult(0, "stdout", "stderr"));
      actionResult2.setKind(Kind.START_ACTION);
      actionResult2.setComponent("hdfs");
      actionResult2.setRole("datanode");

      actionResults.add(actionResult);
      actionResults.add(actionResult2);

      HardwareProfile hp = new HardwareProfile();
      hp.setCoreCount(8);
      hp.setCpuFlags("fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush dts acpi mmx fxsr sse sse2 ss ht tm syscall nx lm constant_tsc pni monitor ds_cpl vmx est tm2 ssse3 cx16 xtpr sse4_1 lahf_lm");
      hp.setCpuSpeed(2003);
      hp.setDiskCount(4);
      hp.setNetSpeed(1000);
      hp.setRamSize(16442752);
      
      List<AgentRoleState> agentRoles = new ArrayList<AgentRoleState>(2);
      AgentRoleState agentRole1 = new AgentRoleState();
      agentRole1.setClusterDefinitionRevision(2);
      agentRole1.setClusterId("cluster-003");
      agentRole1.setComponentName("hdfs");
      agentRole1.setRoleName("datanode");
      agentRole1.setServerStatus(AgentRoleState.State.STARTED);
      agentRoles.add(agentRole1);
      
      HeartBeat hb = new HeartBeat();
      hb.setResponseId((short)-1);
      hb.setTimestamp(System.currentTimeMillis());
      hb.setHostname(addr.getHostName());
      hb.setActionResults(actionResults);
      hb.setHardwareProfile(hp);
      hb.setInstalledRoleStates(agentRoles);
      hb.setIdle(false);
      return hb;
    } catch (UnknownHostException e) {
      throw new WebApplicationException(e);
    }
  }
  
  /**
   * Sample controller to agent response message
   * 
   * @response.representation.200.example 
   * {
      "responseId": "2",
      "actions": [
        {
            "kind": "CREATE_STRUCTURE_ACTION",
            "clusterId": "cluster-001",
            "id": "action-000",
            "component": "hdfs",
            "role": "datanode",
            "clusterDefinitionRevision": "0"
        },
        {
            "kind": "STOP_ACTION",
            "clusterId": "cluster-001",
            "user": "hdfs",
            "id": "action-001",
            "component": "hdfs",
            "role": "datanode",
            "signal": "KILL",
            "clusterDefinitionRevision": "2"
        },
        {
            "kind": "START_ACTION",
            "clusterId": "cluster-001",
            "user": "hdfs",
            "id": "action-002",
            "component": "hdfs",
            "role": "datanode",
            "command": {
                "script": "import os\nos._exit(0)",
                "param": [
                    "cluster",
                    "role"
                ],
                "user": "root"
            },
            "cleanUpCommand": {
                "script": "import os\nos._exit(0)",
                "param": [
                    "cluster",
                    "role"
                ],
                "user": "root"
            },
            "clusterDefinitionRevision": "3"
        },
        {
            "kind": "RUN_ACTION",
            "clusterId": "cluster-001",
            "user": "hdfs",
            "id": "action-003",
            "component": "hdfs",
            "role": "datanode",
            "command": {
                "script": "import os\nos._exit(0)",
                "param": [
                    "cluster",
                    "role"
                ],
                "user": "root"
            },
            "cleanUpCommand": {
                "script": "import os\nos._exit(0)",
                "param": [
                    "cluster",
                    "role"
                ],
                "user": "root"
            },
            "clusterDefinitionRevision": "3"
        },
        {
            "kind": "WRITE_FILE_ACTION",
            "clusterId": "cluster-001",
            "user": "hdfs",
            "id": "action-004",
            "component": "hdfs",
            "role": "datanode",
            "clusterDefinitionRevision": "4",
            "file": {
                "data": "Content of the file",
                "umask": "022",
                "path": "config",
                "owner": "hdfs",
                "group": "hadoop",
                "permission": "0700"
            }
        },
        {
            "kind": "DELETE_STRUCTURE_ACTION",
            "clusterId": "cluster-001",
            "user": "hdfs",
            "id": "action-005",
            "component": "hdfs",
            "role": "datanode",
            "clusterDefinitionRevision": "0"
        }
      ]
    }
   * @response.representation.200.doc Print an example of Controller Response to Agent
   * @response.representation.200.mediaType application/json
   * @return ControllerResponse A list of command to execute on agent
   */
  @Path("response/sample")
  @GET
  @Produces("application/json")
  public ControllerResponse getControllerResponse() {
    ControllerResponse controllerResponse = new ControllerResponse();
    controllerResponse.setResponseId((short)2);    
    
    String script = "import os\nos._exit(0)";
    String[] param = { "cluster", "role" };

    Command command = new Command("root", script, param);
    Command cleanUp = new Command("root", script, param);
    
    Action action = new Action();
    action.setClusterId("cluster-001");
    action.setId("action-000");
    action.setKind(Kind.CREATE_STRUCTURE_ACTION);
    action.setComponent("hdfs");
    action.setRole("datanode");
    
    Action action1 = new Action();
    action1.setClusterDefinitionRevision(2);

    action1.setUser("hdfs");
    action1.setComponent("hdfs");
    action1.setRole("datanode");
    action1.setKind(Kind.STOP_ACTION);
    action1.setSignal(Signal.KILL);
    action1.setClusterId("cluster-001");
    action1.setId("action-001");

    Action action2 = new Action();
    action2.setClusterDefinitionRevision(3);
    action2.setKind(Kind.START_ACTION);
    action2.setId("action-002");
    action2.setClusterId("cluster-001");
    action2.setCommand(command);
    action2.setCleanUpCommand(cleanUp);
    action2.setUser("hdfs");
    action2.setComponent("hdfs");
    action2.setRole("datanode");
    
    Action action3 = new Action();
    action3.setClusterDefinitionRevision(3);
    action3.setUser("hdfs");
    action3.setKind(Kind.RUN_ACTION);
    action3.setId("action-003");
    action3.setClusterId("cluster-001");
    action3.setCommand(command);
    action3.setCleanUpCommand(cleanUp);
    action3.setUser("hdfs");
    action3.setComponent("hdfs");
    action3.setRole("datanode");
    
    Action action4 = new Action();
    action4.setId("action-004");
    action4.setClusterId("cluster-001");
    action4.setClusterDefinitionRevision(4);
    action4.setKind(Kind.WRITE_FILE_ACTION);
    action4.setUser("hdfs");
    action4.setComponent("hdfs");
    action4.setRole("datanode");
    String owner ="hdfs";
    String group = "hadoop";
    String permission = "0700";
    String path = "config";
    String umask = "022";
    String data = "Content of the file";
    action4.setFile(new ConfigFile(owner, group, permission, path, umask, data));
    
    Action action5 = new Action();
    action5.setKind(Kind.DELETE_STRUCTURE_ACTION);
    action5.setId("action-005");
    action5.setClusterId("cluster-001");
    action5.setUser("hdfs");
    action5.setComponent("hdfs");
    action5.setRole("datanode");
    
    List<Action> actions = new ArrayList<Action>();
    actions.add(action);
    actions.add(action1);
    actions.add(action2);
    actions.add(action3);
    actions.add(action4);
    actions.add(action5);
    controllerResponse.setActions(actions);
    return controllerResponse;
  }
}
