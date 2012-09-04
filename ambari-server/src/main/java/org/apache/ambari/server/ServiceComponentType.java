package org.apache.ambari.server;

import java.util.List;

/**
 * TODO
 * Meta-data object for ServiceComponent
 */
public interface ServiceComponentType {

  /**
   * Get the ServiceType to which this ServiceComponent belongs to
   * @return ServiceType of which this ServiceComponent is part of
   */
  public ServiceType getServiceType();

  /**
   * Get the list of components that this ServiceComponent depends on
   * @return List of ServiceComponentTypes
   */
  public List<ServiceComponentType> getDependencies();

  /**
   * Get the list of components that depend on this ServiceComponent
   * @return List of ServiceComponentTypes
   */
  public List<ServiceComponentType> getDependents();

}
