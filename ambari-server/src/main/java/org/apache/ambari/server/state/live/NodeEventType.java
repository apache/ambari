package org.apache.ambari.server.state.live;

public enum NodeEventType {
  /**
   * Event to denote when a registration request is received from a Node
   */
  NODE_REGISTRATION_REQUEST,
  /**
   * Node authenticated/verified.
   */
  NODE_VERIFIED,
  /**
   * A healthy heartbeat event received from the Node.
   */
  NODE_HEARTBEAT_HEALTHY,
  /**
   * No heartbeat received from the Node within the defined expiry interval.
   */
  NODE_HEARTBEAT_TIMED_OUT,
  /**
   * A non-healthy heartbeat event received from the Node.
   */
  NODE_HEARTBEAT_UNHEALTHY
}
