package org.apache.ambari.server;

/**
 * Cluster represents a set of Nodes and Services deployed on these Nodes
 */
public interface Cluster {

  /**
   * Get the Name of the Cluster
   * @return Cluster Name
   */
  public String getName();

}
