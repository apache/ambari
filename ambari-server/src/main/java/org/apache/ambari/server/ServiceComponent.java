package org.apache.ambari.server;

import java.util.Map;

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
   * @return ServiceComponents mapped by their names
   */
  public Map<String, ServiceComponent> getDependencies();

  /**
   * Get the list of components that depend on this ServiceComponent
   * @return ServiceComponents mapped by their names
   */
  public Map<String, ServiceComponent> getDependents();
}
