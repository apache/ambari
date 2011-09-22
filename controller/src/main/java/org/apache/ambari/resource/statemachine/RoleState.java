package org.apache.ambari.resource.statemachine;

public enum RoleState {
  INACTIVE, STARTING, ACTIVE, FAIL, STOPPING, UNCLEAN_STOP
}