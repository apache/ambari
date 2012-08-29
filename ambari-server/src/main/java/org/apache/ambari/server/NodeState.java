package org.apache.ambari.server;

public enum NodeState {
  /**
   * New node state
   */
  INIT,
  /**
   * State when a registration request is received from the Node but
   * the node has not been verified/authenticated.
   */
  WAITING_FOR_VERIFICATION,
  /**
   * State when the server is receiving heartbeats regularly from the Node
   * and the state of the Node is healthy
   */
  HEALTHY,
  /**
   * State when the server has not received a heartbeat from the Node in the
   * configured heartbeat expiry window.
   */
  HEARTBEAT_LOST,
  /**
   * Node is in unhealthy state as reported either by the Node itself or via
   * any other additional means ( monitoring layer )
   */
  UNHEALTHY
}
