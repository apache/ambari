package org.apache.ambari.resource.statemachine;

import java.util.List;
import java.util.Map;

public interface Cluster extends LifeCycle {
  public String getClusterName();
  public List<Service> getServices();
  public ClusterState getClusterState();
  public Map<String, String> getServiceStates();
  
  public void addServices(List<Service> services);
  public void terminate();
}
