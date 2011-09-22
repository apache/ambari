package org.apache.ambari.resource.statemachine;

public enum ClusterState {
  INACTIVE, STARTING, ACTIVE, FAIL, ATTIC, STOPPING, UNCLEAN_STOP
}