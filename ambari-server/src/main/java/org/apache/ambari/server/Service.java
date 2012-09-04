package org.apache.ambari.server;

import java.util.List;

/**
 * Service Interface for representing various products in the
 * Hadoop eco-system such as HDFS, MAPREDUCE, HBASE.
 */
public interface Service {

  /**
   * @return Name of the Service Instance
   */
  public String getName();

  /**
   * Get the Service Type
   * @return ServiceType
   */
  public ServiceType getType();

  /**
   * Get the ServiceComponents enabled for this Service
   * @return List of ServiceComponents
   */
  public List<ServiceComponent> getServiceComponents();

  /**
   * Get the list of services that this Service depends on
   * @return List of Services
   */
  public List<Service> getDependencies();

  /**
   * Get the list of services that depend on this Service
   * @return List of Services
   */
  public List<Service> getDependents();

  /**
   * Get the list of Actions that are currently being tracked at the
   * Service level
   * @return List of Actions
   */
  public List<Job> getActions();

}
