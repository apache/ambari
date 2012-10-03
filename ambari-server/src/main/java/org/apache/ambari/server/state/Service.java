package org.apache.ambari.server.state;

import java.util.Map;


public interface Service {

  public String getName();
  
  public long getClusterId();
  
  public long getCurrentHostComponentMappingVersion();

  public Map<String, ServiceComponent> getServiceComponents();
    
  public State getState();
  
  public void setState(State state);

  public Config getConfig();

  public void setConfig(Config config);
  
  public StackVersion getStackVersion();
  
  public void setStackVersion(StackVersion stackVersion);
  
}
