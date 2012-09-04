package org.apache.ambari.server.state.live;

import java.util.List;

import org.apache.ambari.server.state.ConfigVersion;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;


public interface ServiceComponentNode {

  /**
   * Get the ServiceComponent this object maps to
   * @return Name of the ServiceComponent
   */
  public String getServiceComponentName();
  
  /**
   * Get the Node this object maps to
   * @return Node's hostname
   */
  public String getNodeName();
  
  
  /**
   * Get the Config Version
   * @return ConfigVersion
   */
  public ConfigVersion getConfigVersion();

  /**
   * Get the list of Jobs that are currently being tracked at the
   * ServiceComponentNode level
   * @return List of Jobs
   */
  public List<Job> getJobs();
  
  
  /**
   * Get ServiceComponent-Node State
   * @return ServiceComponentNodeState
   */
  public ServiceComponentNodeState getState();

  /**
   * Set the State for this ServiceComponent-Node
   * @param state ServiceComponentNodeState to set to
   */
  public void setState(ServiceComponentNodeState state);

  /**
   * Send a ServiceComponentNodeState event to the StateMachine
   * @param event Event to handle
   * @throws InvalidStateTransitonException
   */
  public void handleEvent(ServiceComponentNodeEvent event)
      throws InvalidStateTransitonException;

}
