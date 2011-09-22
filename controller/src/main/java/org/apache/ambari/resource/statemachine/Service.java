package org.apache.ambari.resource.statemachine;

public interface Service extends LifeCycle {
  
  public ServiceState getServiceState();
  
  public String getServiceName();
  
  public Cluster getAssociatedCluster();
}
