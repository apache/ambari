package org.apache.ambari.server.state;

import java.util.Map;

import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHost;

public interface ServiceComponent {

  public String getName();
  
  public String getServiceName();

  public long getClusterId();
  
  public State getState();
  
  public void setState(State state);

  public Config getConfig();

  public void setConfig(Config config);
  
  public StackVersion getStackVersion();
  
  public void setStackVersion(StackVersion stackVersion);

  public Map<String, ServiceComponentHost> getServiceComponentHosts();
  
}
