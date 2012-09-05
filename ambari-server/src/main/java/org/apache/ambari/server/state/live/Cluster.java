package org.apache.ambari.server.state.live;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;

public interface Cluster {

  /**
   * Get the State for a given Node
   * @param nodeName Node hostname for which to retrieve state
   * @return
   */
  public NodeState getNodeState(String nodeName);
  
  /**
   * Set the State for a given Node
   * @param nodeName Node's hostname for which state is to be set
   * @param state NodeState to set
   */
  public void setNodeState(String nodeName, NodeState state);
  
  /**
   * Send event to the given Node
   * @param nodeName Node's hostname
   * @param event Event to be handled
   */
  public void handleNodeEvent(String nodeName, NodeEvent event)
      throws InvalidStateTransitonException;
  
  /**
   * Get the State for a given ServiceComponentNode
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param nodeName Node name
   * @return ServiceComponentNodeState
   */
  public ServiceComponentNodeState getServiceComponentNodeState(String service,
      String serviceComponent, String nodeName);

  /**
   * Set the State for a given ServiceComponentNode
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param nodeName Node name
   * @param state State to set
   */
  public void setServiceComponentNodeState(String service,
      String serviceComponent, String nodeName,
      ServiceComponentNodeState state);

  /**
   * Send an Event to a given ServiceComponentNode
   * @param service Service name
   * @param serviceComponent ServiceComponent name
   * @param nodeName Node name
   * @param event Event to be handled
   */
  public void handleServiceComponentNodeEvent(String service,
      String serviceComponent, String nodeName,
      ServiceComponentNodeEvent event) throws InvalidStateTransitonException;
  
}
