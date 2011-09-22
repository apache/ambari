package org.apache.ambari.resource.statemachine;

public enum ServiceState {
  INACTIVE, STARTING, ACTIVE, FAIL, STOPPING, UNCLEAN_STOP
}