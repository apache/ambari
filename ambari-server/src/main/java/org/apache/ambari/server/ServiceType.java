package org.apache.ambari.server;

import java.util.List;

/**
 * TODO
 * Meta-data object for Service
 */
public interface ServiceType {

  /**
   * Get the list of ServiceComponentTypes for this Service
   * @return List of ServiceComponentTypes
   */
  public List<ServiceComponentType> getServiceComponents();

  /**
   * Get the list of services that this Service depends on
   * @return List of ServiceTypes
   */
  public List<ServiceType> getDependencies();

  /**
   * Get the list of services that depend on this Service
   * @return List of ServiceTypes
   */
  public List<ServiceType> getDependents();


}
