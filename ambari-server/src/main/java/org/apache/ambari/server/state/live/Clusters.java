package org.apache.ambari.server.state.live;

public interface Clusters {

  /**
   * Get the Cluster given the cluster name
   * @param clusterName Name of the Cluster to retrieve
   * @return
   */
  public Cluster getCluster(String clusterName);
  
}
