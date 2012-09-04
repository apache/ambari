package org.apache.ambari.server;

import java.util.List;

public interface ServiceComponentNode {

  /**
   * Get the ServiceComponent that this object is mapped to
   * @return ServiceComponent
   */
  public ServiceComponent getServiceComponent();

  /**
   * Get the Node that this object is mapped to
   * @return Node
   */
  public Node getNode();

  /**
   * Get the State for this ServiceComponentNode
   * @return
   */
  public ServiceComponentNodeState getServiceComponentNodeState();

  /**
   * Get the Config Version
   * @return ConfigVersion
   */
  public ConfigVersion getConfigVersion();

  /**
   * Get the list of Actions that are currently being tracked at the
   * ServiceComponentNode level
   * @return List of Actions
   */
  public List<Job> getActions();
}
