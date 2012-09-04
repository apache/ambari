package org.apache.ambari.server;

// TODO
public interface ConfigVersion {

  public Config getServiceConfig();

  public Config getServiceComponentConfig();

  public Config getServiceComponentNodeConfig();

}
