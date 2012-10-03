package org.apache.ambari.server.state;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.state.live.Cluster;

public class ServiceImpl implements Service {

  private final Cluster cluster;
  private final String serviceName;
  private DeployState state;
  private Map<String, Config> configs;
  private Map<String, ServiceComponent> components;
  
  private void init() {
    // TODO
    // initialize from DB 
  }
  
  public ServiceImpl(Cluster cluster, String serviceName,
      DeployState state, Map<String, Config> configs) {
    this.cluster = cluster;
    this.serviceName = serviceName;
    this.state = state;
    if (configs != null) {
      this.configs = configs;
    } else {
      this.configs = new HashMap<String, Config>();
    }
    this.components = new HashMap<String, ServiceComponent>();
    init();
  }

  public ServiceImpl(Cluster cluster, String serviceName,
      Map<String, Config> configs) {
    this(cluster, serviceName, DeployState.INIT, configs);
  }
  
  public ServiceImpl(Cluster cluster, String serviceName) {
    this(cluster, serviceName, DeployState.INIT, null);
  }
  
  @Override
  public String getName() {
    return serviceName;
  }

  @Override
  public long getClusterId() {
    return cluster.getClusterId();
  }

  @Override
  public long getCurrentHostComponentMappingVersion() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Map<String, ServiceComponent> getServiceComponents() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public State getState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setState(State state) {
    // TODO Auto-generated method stub

  }

  @Override
  public Config getConfig() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setConfig(Config config) {
    // TODO Auto-generated method stub

  }

  @Override
  public StackVersion getStackVersion() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setStackVersion(StackVersion stackVersion) {
    // TODO Auto-generated method stub

  }

}
