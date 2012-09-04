package org.apache.ambari.server;

import java.util.List;

/**
 * ServiceComponent Interface for representing components of the various
 * products in the Hadoop eco-system such as NAMENODE, DATANODE for HDFS.
 */
public interface ServiceComponent {

  /**
   * @return Name of the ServiceComponent Instance
   */
  public String getName();

  /**
   * Get the ServiceComponentType for this instance
   * @return ServiceComponentType
   */
  public ServiceComponentType getType();

  /**
   * Get the Service to which this ServiceComponent belongs to
   * @return Service of which this ServiceComponent is part of
   */
  public Service getService();

  /**
   * Get the list of components that this ServiceComponent depends on
   * @return List of ServiceComponents
   */
  public List<ServiceComponent> getDependencies();

  /**
   * Get the list of components that depend on this ServiceComponent
   * @return List of ServiceComponents
   */
  public List<ServiceComponent> getDependents();

  /**
   * Get the Nodes assigned to this ServiceComponent
   * @return List of ServiceComponentNodes
   */
  public List<ServiceComponentNode> getServiceComponentNodes();

  /**
   * Get the list of Actions that are currently being tracked at the
   * ServiceComponent level
   * @return List of Actions
   */
  public List<Job> getActions();

}
