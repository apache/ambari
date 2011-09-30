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

package org.apache.ambari.controller.rest.resources;

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
import org.apache.ambari.common.rest.entities.agent.Command;
import org.apache.ambari.common.rest.entities.agent.CommandResult;
import org.apache.ambari.common.rest.entities.agent.ControllerResponse;
import org.apache.ambari.common.rest.entities.agent.HardwareProfile;
import org.apache.ambari.common.rest.entities.agent.HeartBeat;
import org.apache.ambari.common.rest.entities.agent.ServerStatus;

/** 
 * Controller Resource represents Ambari controller.
 * It provides API for Ambari agents to get the cluster configuration changes
 * as well as report the node attributes and state of services running the on 
 * the cluster nodes
 */
@Path(value = "/controller")
public class ControllerResource {
	
  /** 
   * Update state of the node (Internal API to be used by Ambari agent).
   *  
   * @response.representation.200.doc This API is invoked by Ambari agent running
   *  on a cluster to update the state of various services running on the node.
   * @response.representation.200.mediaType application/json
   * @response.representation.500.doc Error in accepting heartbeat message
   * @param message Heartbeat message
   */
  @Path(value = "/agent/{hostname}")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public ControllerResponse heartbeat(HeartBeat message) {
    ControllerResponse controllerResponse = new ControllerResponse();
    controllerResponse.setResponseId("id-00002");    
    List<Command> commands = new ArrayList<Command>();
    String[] cmd = { "ls", "-l" };
    commands.add(new Command("root", cmd));
    commands.add(new Command("root", cmd));
    commands.add(new Command("root", cmd));

    List<Command> cleanUps = new ArrayList<Command>();
    String[] cleanUpCmd = { "ls", "-t" };
    cleanUps.add(new Command("hdfs", cleanUpCmd));
    cleanUps.add(new Command("hdfs", cleanUpCmd));
    
    Action action = new Action();
    action.setUser("hdfs");
    action.setComponent("hdfs");
    action.setRole("datanode");
    action.setKind(Kind.STOP_ACTION);
    action.setSignal(Signal.KILL);
    action.setClusterId("cluster-001");
    action.setId("action-001");

    Action action2 = new Action();
    action2.setUser("hdfs");
    action2.setKind(Kind.START_ACTION);
    action2.setId("action-002");
    action2.setClusterId("cluster-002");
    action2.setCommands(commands);
    action2.setCleanUpCommands(cleanUps);
    action2.setComponent("hdfs");
    action2.setRole("datanode");

    Action action3 = new Action();
    action3.setUser("hdfs");
    action3.setKind(Kind.RUN_ACTION);
    action3.setId("action-003");
    action3.setClusterId("cluster-003");
    action3.setCommands(commands);
    action3.setCleanUpCommands(cleanUps);

    List<Action> actions = new ArrayList<Action>();
    actions.add(action);
    actions.add(action2);
    actions.add(action3);
    controllerResponse.setActions(actions);
    return controllerResponse;
  }

  /**
   * Sample Ambari heartbeat message
   * 
   * @response.representation.200.doc Print example of Ambari heartbeat message
   * @response.representation.200.mediaType application/json
   * @param stackId Stack ID
   * @return Heartbeat message
   */
  @Path("agent/heartbeat/sample")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public HeartBeat getHeartBeat(@DefaultValue("stack-123") 
                                @QueryParam("stackId") String stackId) {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      List<ActionResult> actionResults = new ArrayList<ActionResult>();      

      List<CommandResult> commandResults = new ArrayList<CommandResult>();
      commandResults.add(new CommandResult(0, "stdout", "stderr"));
      List<CommandResult> cleanUpResults = new ArrayList<CommandResult>();
      cleanUpResults.add(new CommandResult(0, "stdout", "stderr"));
      ActionResult actionResult = new ActionResult();
      actionResult.setId("action-001");
      actionResult.setClusterId("cluster-001");
      actionResult.setKind(Kind.STOP_ACTION);

      ActionResult actionResult2 = new ActionResult();
      actionResult2.setClusterId("cluster-002");
      actionResult2.setCommandResults(commandResults);
      actionResult2.setCleanUpResults(cleanUpResults);
      actionResult2.setKind(Kind.START_ACTION);
      actionResult2.setComponent("hadoop");
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
      
      HeartBeat hb = new HeartBeat();
      hb.setResponseId("unknown");
      hb.setClusterId("cluster-123");
      hb.setTimestamp(System.currentTimeMillis());
      hb.setHostname(addr.getHostName());
      hb.setStackId(stackId);
      hb.setActionResults(actionResults);
      hb.setHardwareProfile(hp);
      List<ServerStatus> serversStatus = new ArrayList<ServerStatus>();
      serversStatus.add(new ServerStatus("hdfs","datanode", 
          ServerStatus.State.STARTED));
      serversStatus.add(new ServerStatus("hdfs","tasktracker", 
          ServerStatus.State.STARTED));
      hb.setServersStatus(serversStatus);
      hb.setIdle(false);
      return hb;
    } catch (UnknownHostException e) {
      throw new WebApplicationException(e);
    }
  }
  
  /**
   * Sample controller to agent response message
   * 
   * @response.representation.200.doc Print an example of Controller Response to Agent
   * @response.representation.200.mediaType application/json
   * @return ControllerResponse A list of command to execute on agent
   */
  @Path("response/sample")
  @GET
  @Produces("application/json")
  public ControllerResponse getControllerResponse() {
    ControllerResponse controllerResponse = new ControllerResponse();
    controllerResponse.setResponseId("id-00002");    
    List<Command> commands = new ArrayList<Command>();
    String[] cmd = { "top" };
    commands.add(new Command("root", cmd));

    List<Command> cleanUps = new ArrayList<Command>();
    String[] cleanUpCmd = { "ls", "-t" };
    cleanUps.add(new Command("hdfs", cleanUpCmd));
    cleanUps.add(new Command("hdfs", cleanUpCmd));
    
    Action action = new Action();
    action.setUser("hdfs");
    action.setComponent("hdfs");
    action.setRole("datanode");
    action.setKind(Kind.STOP_ACTION);
    action.setSignal(Signal.KILL);
    action.setClusterId("cluster-001");
    action.setId("action-001");

    Action action2 = new Action();
    action2.setUser("hdfs");
    action2.setKind(Kind.START_ACTION);
    action2.setId("action-002");
    action2.setClusterId("cluster-002");
    action2.setCommands(commands);
    action2.setCleanUpCommands(cleanUps);
    action2.setComponent("hdfs");
    action2.setRole("datanode");
    
    Action action3 = new Action();
    action3.setUser("hdfs");
    action3.setKind(Kind.RUN_ACTION);
    action3.setId("action-003");
    action3.setClusterId("cluster-002");
    List<Command> configFiles = new ArrayList<Command>();
    String[] writeFile = { 
        "ambari-write-file",
        "hdfs",
        "hadoop",
        "0700",
        "/tmp/test",
        "content of file"
        };
    configFiles.add(new Command("hdfs", writeFile));
    action3.setCommands(configFiles);
    List<Action> actions = new ArrayList<Action>();
    actions.add(action);
    actions.add(action2);
    actions.add(action3);
    controllerResponse.setActions(actions);
    return controllerResponse;
  }
}
