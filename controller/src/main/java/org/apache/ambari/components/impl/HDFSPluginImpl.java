package org.apache.ambari.components.impl;

import java.io.IOException;
import java.util.List;

import org.apache.ambari.common.rest.entities.agent.Action;
import org.apache.ambari.common.rest.entities.agent.Command;
import org.apache.ambari.components.ClusterContext;
import org.apache.ambari.components.ComponentPlugin;

public class HDFSPluginImpl extends ComponentPlugin {

  @Override
  public String[] getRoles() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getRequiredComponents() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isService() throws IOException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<Action> writeConfiguration(ClusterContext cluster)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Action> install(ClusterContext cluster) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Action> uninstall(ClusterContext cluster) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Action> startRoleServer(ClusterContext cluster, String role)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Action> stopRoleServer(ClusterContext cluster, String role)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

}
