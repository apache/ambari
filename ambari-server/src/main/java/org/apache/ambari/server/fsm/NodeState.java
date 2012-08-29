package org.apache.ambari.server.fsm;

public enum NodeState {
  INIT,
  WAITING_FOR_VERIFICATION,
  HEALTHY,
  HEARTBEAT_LOST,
  UNHEALTHY
}
