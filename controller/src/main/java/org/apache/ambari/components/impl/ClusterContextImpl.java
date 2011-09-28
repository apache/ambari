package org.apache.ambari.components.impl;

import java.util.List;

import org.apache.ambari.common.rest.entities.Blueprint;
import org.apache.ambari.common.rest.entities.Cluster;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.components.ClusterContext;

public class ClusterContextImpl implements ClusterContext {

  Cluster cluster;
  Node node;
  
  public ClusterContextImpl(Cluster cluster, Node node) {
    this.cluster = cluster;
    this.node = node;
  }
  
  @Override
  public String getClusterName() {
    return cluster.getClusterDefinition().getName();
  }

  @Override
  public String[] getAllRoles() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getInstallDirectory() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getConfigDirectory() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ClusterDefinition getClusterDefinition() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Blueprint getBlueprint() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getClusterComponents() {
    List<String> roles = cluster.getClusterDefinition().getActiveServices();
    return roles.toArray(new String[1]);
  }

}
