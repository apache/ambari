package org.apache.ambari.server.fsm;

import org.apache.ambari.server.NodeState;

public interface NodeFSM {

  public NodeState getState();

  public void setState(NodeState state);

  public void handleEvent(NodeEvent event)
      throws InvalidStateTransitonException;
}
