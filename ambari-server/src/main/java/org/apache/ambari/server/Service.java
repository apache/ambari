package org.apache.ambari.server;

import java.util.Map;

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
   * Get the ServiceComponents for this Service
   * @return ServiceComponents mapped by their names
   */
  public Map<String, ServiceComponent> getServiceComponents();

  /**
   * Get the list of services that this Service depends on
   * @return Services mapped by their names
   */
  public Map<String, Service> getDependencies();

  /**
   * Get the list of services that depend on this Service
   * @return Services mapped by their names
   */
  public Map<String, Service> getDependents();

}
