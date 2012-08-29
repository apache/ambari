package org.apache.ambari.server.fsm;

import org.apache.ambari.server.fsm.event.AbstractEvent;

public class NodeEvent extends AbstractEvent<NodeEventType> {

  // TODO
  // this should have some node identifier
  public NodeEvent(NodeEventType type) {
    super(type);
    // TODO Auto-generated constructor stub
  }

}
