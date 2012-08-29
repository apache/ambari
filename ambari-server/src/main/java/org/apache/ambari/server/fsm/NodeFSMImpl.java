package org.apache.ambari.server.fsm;

import org.apache.ambari.server.NodeState;
import org.apache.ambari.server.fsm.SingleArcTransition;
import org.apache.ambari.server.fsm.StateMachineFactory;

public class NodeFSMImpl implements NodeFSM {

  private static final StateMachineFactory
    <NodeFSMImpl, NodeState, NodeEventType, NodeEvent>
      stateMachineFactory
        = new StateMachineFactory<NodeFSMImpl, NodeState, NodeEventType, NodeEvent>
        (NodeState.INIT)

   // define the state machine of a Node

   .addTransition(NodeState.INIT, NodeState.WAITING_FOR_VERIFICATION,
       NodeEventType.NODE_REGISTRATION_REQUEST)

   .addTransition(NodeState.WAITING_FOR_VERIFICATION, NodeState.HEALTHY,
       NodeEventType.NODE_VERIFIED, new NodeVerifiedTransition())

   // TODO - should be able to combine multiple into a single multi-arc
   // transition
   .addTransition(NodeState.HEALTHY, NodeState.HEALTHY,
       NodeEventType.NODE_HEARTBEAT_HEALTHY)
   .addTransition(NodeState.HEALTHY, NodeState.HEARTBEAT_LOST,
       NodeEventType.NODE_HEARTBEAT_TIMED_OUT)
   .addTransition(NodeState.HEALTHY, NodeState.UNHEALTHY,
       NodeEventType.NODE_HEARTBEAT_UNHEALTHY)

   .addTransition(NodeState.UNHEALTHY, NodeState.HEALTHY,
       NodeEventType.NODE_HEARTBEAT_HEALTHY)
   .addTransition(NodeState.UNHEALTHY, NodeState.UNHEALTHY,
       NodeEventType.NODE_HEARTBEAT_UNHEALTHY)
   .addTransition(NodeState.UNHEALTHY, NodeState.HEARTBEAT_LOST,
       NodeEventType.NODE_HEARTBEAT_TIMED_OUT)

   .addTransition(NodeState.HEARTBEAT_LOST, NodeState.HEALTHY,
       NodeEventType.NODE_HEARTBEAT_HEALTHY)
   .addTransition(NodeState.HEARTBEAT_LOST, NodeState.UNHEALTHY,
       NodeEventType.NODE_HEARTBEAT_UNHEALTHY)
   .addTransition(NodeState.HEARTBEAT_LOST, NodeState.HEARTBEAT_LOST,
       NodeEventType.NODE_HEARTBEAT_TIMED_OUT)
   .installTopology();


  static class NodeVerifiedTransition
      implements SingleArcTransition<NodeFSMImpl, NodeEvent> {

    @Override
    public void transition(NodeFSMImpl node, NodeEvent event) {
      // TODO Auto-generated method stub
    }

  }

}
