package org.apache.ambari.resource.statemachine;

import org.apache.ambari.event.AbstractEvent;

public class ClusterEvent extends AbstractEvent<ClusterEventType> {
  private Cluster cluster;
  private Service service;
  public ClusterEvent(ClusterEventType type, Cluster cluster) {
    super(type);
    this.cluster = cluster;
  }
  //Need this to create an event that has details about the service
  //that moved into a different state
  public ClusterEvent(ClusterEventType type, Cluster cluster, Service service) {
    super(type);
    this.cluster = cluster;
    this.service = service;
  }
  public Cluster getCluster() {
    return cluster;
  }
  public Service getServiceCausingTransition() {
    return service;
  }
}
