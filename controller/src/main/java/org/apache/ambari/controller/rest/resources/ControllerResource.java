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
import org.apache.ambari.common.rest.entities.agent.ActionResult;
import org.apache.ambari.common.rest.entities.agent.Command;
import org.apache.ambari.common.rest.entities.agent.CommandResult;
import org.apache.ambari.common.rest.entities.agent.ControllerResponse;
import org.apache.ambari.common.rest.entities.agent.HardwareProfile;
import org.apache.ambari.common.rest.entities.agent.HeartBeat;
import org.apache.ambari.common.rest.entities.agent.ServerStatus;
import org.apache.ambari.common.rest.entities.agent.ServersStatus;

/** Controller Resource represents Ambari controller.
 *	It provides API for Ambari agents to get the cluster configuration changes
 *	as well as report the node attributes and state of services running the on the 
 *	cluster nodes
 */
@Path(value = "/controller")
public class ControllerResource {
	
	/** Update state of the node (Internal API to be used by Ambari agent).
	 *  <p>
	 *	This API is invoked by Ambari agent running on a cluster to update the 
	 *	the state of various services running on the nodes. This API also registers 
	 *	the node w/ controller (if not already done).
	 *  <p>
	 *  REST:<br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;URL Path                                    : /controller/agent/{hostname}<br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Method                                 : PUT <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Request Header	                        : <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;HTTP Response Header                        : <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-type        = application/json <br>
	 *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Accept              = application/json <br>
	 *  <p> 
	 * 
	 * @response.representation.200.doc This API is invoked by Ambari agent running
	 *  on a cluster to update the state of various services running on the node.
	 * @response.representation.200.mediaType application/json
	 * @response.representation.500.doc Error in accepting heartbeat message
	 * @param message Heartbeat message
	 * @throws Exception	throws Exception
	 */
  @Path(value = "/agent/{hostname}")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public ControllerResponse heartbeat(HeartBeat message) {
    ControllerResponse response = new ControllerResponse();
  	return response;
	}

  /**
   * @response.representation.200.doc Print an example of the Ambari heartbeat message
   * @response.representation.200.mediaType application/json
   * @param stackId
   * @return Heartbeat message
   */
  @Path("agent/heartbeat/sample")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public HeartBeat getHeartBeat(@DefaultValue("stack-123") @QueryParam("stackId") String stackId) {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      List<ActionResult> actionResults = new ArrayList<ActionResult>();      

      List<CommandResult> commandResults = new ArrayList<CommandResult>();
      commandResults.add(new CommandResult("id", 0, "stdout", "stderr"));
      List<CommandResult> cleanUpResults = new ArrayList<CommandResult>();
      cleanUpResults.add(new CommandResult("id", 0, "stdout", "stderr"));
      ActionResult actionResult = new ActionResult();
      actionResult.setCommandResults(commandResults);
      actionResult.setCleanUpResults(cleanUpResults);

      ActionResult actionResult2 = new ActionResult();
      actionResult2.setCommandResults(commandResults);
      actionResult2.setCleanUpResults(cleanUpResults);

      actionResults.add(actionResult);
      actionResults.add(actionResult2);

      ServersStatus serversStatus = new ServersStatus();
      serversStatus.add("hadoop.datanode", ServerStatus.State.STARTED);

      HardwareProfile hp = new HardwareProfile();
      hp.setCoreCount(8);
      hp.setCpuFlags("fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush dts acpi mmx fxsr sse sse2 ss ht tm syscall nx lm constant_tsc pni monitor ds_cpl vmx est tm2 ssse3 cx16 xtpr sse4_1 lahf_lm");
      hp.setCpuSpeed(2003);
      hp.setDiskCount(4);
      hp.setNetSpeed(1000);
      hp.setRamSize(16442752);
      
      HeartBeat hb = new HeartBeat();
      hb.setClusterId("cluster-123");
      hb.setTimestamp(System.currentTimeMillis());
      hb.setHostname(addr.getHostName());
      hb.setStackId(stackId);
      hb.setActionResults(actionResults);
      hb.setHardwareProfile(hp);
      return hb;
    } catch (UnknownHostException e) {
      throw new WebApplicationException(e);
    }
  }
  
  /**
   * @response.representation.200.doc Print an example of Controller Response to Agent
   * @response.representation.200.mediaType application/json
   * @return
   */
  @Path("response/sample")
  @GET
  @Produces("application/json")
  public ControllerResponse getControllerResponse() {
    ControllerResponse controllerResponse = new ControllerResponse();
        
    List<Command> commands = new ArrayList<Command>();
    String[] cmd = { "ls", "-l" };
    commands.add(new Command("cmd-001", "root", cmd));
    commands.add(new Command("cmd-002", "root", cmd));
    commands.add(new Command("cmd-003", "root", cmd));

    List<Command> cleanUps = new ArrayList<Command>();
    String[] cleanUpCmd = { "ls", "-t" };
    cleanUps.add(new Command("clean-01", "hdfs", cleanUpCmd));
    cleanUps.add(new Command("clean-02", "hdfs", cleanUpCmd));
    
    Action action = new Action();
    action.setId("action-001");
    action.setCommands(commands);
    action.setCleanUpCommands(cleanUps);

    Action action2 = new Action();
    action2.setId("action-002");
    action2.setCommands(commands);
    action2.setCleanUpCommands(cleanUps);
    
    List<Action> actions = new ArrayList<Action>();
    actions.add(action);
    actions.add(action2);
    controllerResponse.setActions(actions);
    return controllerResponse;
  }
}
