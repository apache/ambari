package org.apache.ambari.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

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
import org.apache.ambari.resource.statemachine.StateMachineInvoker;

public class HeartbeatHandler {
  
  private Map<String, ControllerResponse> agentToHeartbeatResponseMap = 
      new TreeMap<String, ControllerResponse>();
  
  LinkedBlockingQueue<HeartBeat> heartbeatQueue = 
      new LinkedBlockingQueue<HeartBeat>();
  
  Map<String, List<Action>> responseMap = 
      new HashMap<String, List<Action>>();
  
  public void addActionsForNode(String hostname, List<Action> actions) {
    synchronized (this) {
      List<Action> currentActions = responseMap.get(hostname);
      if (currentActions != null) {
        currentActions.addAll(actions);
      }
    }
  }
  
  public ControllerResponse processHeartBeat(HeartBeat heartbeat) 
      throws DatatypeConfigurationException {
    ControllerResponse response = 
        agentToHeartbeatResponseMap.get(heartbeat.getHostname());
    if (response != null) {
      if (response.getResponseId() == heartbeat.getResponseId()) {
        return response; //duplicate heartbeat
      }
    }

    Node node = Nodes.getInstance().getNodes().get(heartbeat.getHostname());
    NodeState state = node.getNodeState();
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(new Date());
    state.setLastHeartbeatTime(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
    
    Cluster cluster = Clusters.getInstance().getClusterByName(state.getClusterName());
    ClusterContext clusterContext = new ClusterContextImpl(cluster, node);

    //get what is currently running on the node
    List<ServerStatus> servers = heartbeat.getServersStatus();    
    
    //get the state machine reference to the cluster
    org.apache.ambari.resource.statemachine.Cluster stateMachineCluster = 
        StateMachineInvoker.getStateMachineClusterInstance(state.getClusterName());
    
    
    List<Action> actions = new ArrayList<Action>();
    synchronized (this) {
      actions = responseMap.get(node.getName());
    }
    ControllerResponse r = new ControllerResponse();
    r.setActions(actions);
    agentToHeartbeatResponseMap.put(heartbeat.getHostname(), r);
    
    return r;
  }
}
