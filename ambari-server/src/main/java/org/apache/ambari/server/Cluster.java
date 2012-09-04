package org.apache.ambari.server;

import java.util.List;

/**
 * Cluster represents a set of Nodes and Services deployed on these Nodes
 */
public interface Cluster {

  /**
   * Get the Name of the Cluster
   * @return Cluster Name
   */
  public String getName();

  /**
   * Get enabled Services for this Cluster
   * @return List of Services
   */
  public List<Service> getServices();

  /**
   * Get the Nodes that belong to this Cluster
   * @return List of Nodes
   */
  public List<Node> getNodes();

  /**
   * Get the list of Actions that are currently being tracked at the
   * Cluster level
   * @return List of Actions
   */
  public List<Job> getActions();



}
