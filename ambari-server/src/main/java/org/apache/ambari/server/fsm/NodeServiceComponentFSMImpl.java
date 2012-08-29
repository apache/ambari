package org.apache.ambari.server.fsm;

import org.apache.ambari.server.fsm.StateMachineFactory;

public class NodeServiceComponentFSMImpl implements NodeServiceComponentFSM {

  private static final StateMachineFactory
  <NodeServiceComponentFSMImpl, NodeServiceComponentState,
  NodeServiceComponentEventType, NodeServiceComponentEvent>
    stateMachineFactory
      = new StateMachineFactory<NodeServiceComponentFSMImpl,
          NodeServiceComponentState, NodeServiceComponentEventType,
          NodeServiceComponentEvent>
          (NodeServiceComponentState.INIT)

  // define the state machine of a NodeServiceComponent

     .addTransition(NodeServiceComponentState.INIT,
         NodeServiceComponentState.INSTALLING,
         NodeServiceComponentEventType.NODE_SVCCOMP_INSTALL)

     .addTransition(NodeServiceComponentState.INSTALLING,
         NodeServiceComponentState.INSTALLED,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_SUCCEEDED)
     .addTransition(NodeServiceComponentState.INSTALLING,
         NodeServiceComponentState.INSTALLING,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_IN_PROGRESS)
     .addTransition(NodeServiceComponentState.INSTALLING,
         NodeServiceComponentState.INSTALL_FAILED,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_FAILED)

     .addTransition(NodeServiceComponentState.INSTALL_FAILED,
         NodeServiceComponentState.INSTALLING,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_RESTART)

     .addTransition(NodeServiceComponentState.INSTALLED,
         NodeServiceComponentState.STARTING,
         NodeServiceComponentEventType.NODE_SVCCOMP_START)
     .addTransition(NodeServiceComponentState.INSTALLED,
         NodeServiceComponentState.UNINSTALLING,
         NodeServiceComponentEventType.NODE_SVCCOMP_UNINSTALL)
     .addTransition(NodeServiceComponentState.INSTALLED,
         NodeServiceComponentState.INSTALLING,
         NodeServiceComponentEventType.NODE_SVCCOMP_INSTALL)

     .addTransition(NodeServiceComponentState.STARTING,
         NodeServiceComponentState.STARTING,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_IN_PROGRESS)
     .addTransition(NodeServiceComponentState.STARTING,
         NodeServiceComponentState.STARTED,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_SUCCEEDED)
     .addTransition(NodeServiceComponentState.STARTING,
         NodeServiceComponentState.START_FAILED,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_FAILED)

     .addTransition(NodeServiceComponentState.START_FAILED,
         NodeServiceComponentState.STARTING,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_RESTART)

     .addTransition(NodeServiceComponentState.STARTED,
         NodeServiceComponentState.STOPPING,
         NodeServiceComponentEventType.NODE_SVCCOMP_STOP)

     .addTransition(NodeServiceComponentState.STOPPING,
         NodeServiceComponentState.STOPPING,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_IN_PROGRESS)
     .addTransition(NodeServiceComponentState.STOPPING,
         NodeServiceComponentState.INSTALLED,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_SUCCEEDED)
     .addTransition(NodeServiceComponentState.STOPPING,
         NodeServiceComponentState.STOP_FAILED,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_FAILED)

     .addTransition(NodeServiceComponentState.STOP_FAILED,
         NodeServiceComponentState.STOPPING,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_RESTART)

     .addTransition(NodeServiceComponentState.UNINSTALLING,
         NodeServiceComponentState.UNINSTALLING,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_IN_PROGRESS)
     .addTransition(NodeServiceComponentState.UNINSTALLING,
         NodeServiceComponentState.UNINSTALLED,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_SUCCEEDED)
     .addTransition(NodeServiceComponentState.UNINSTALLING,
         NodeServiceComponentState.UNINSTALL_FAILED,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_FAILED)

     .addTransition(NodeServiceComponentState.UNINSTALL_FAILED,
         NodeServiceComponentState.UNINSTALLING,
         NodeServiceComponentEventType.NODE_SVCCOMP_OP_RESTART)

     .addTransition(NodeServiceComponentState.UNINSTALLED,
         NodeServiceComponentState.INSTALLING,
         NodeServiceComponentEventType.NODE_SVCCOMP_INSTALL)

     .addTransition(NodeServiceComponentState.UNINSTALLED,
         NodeServiceComponentState.INIT,
         NodeServiceComponentEventType.NODE_SVCCOMP_WIPEOUT)
         
     .installTopology();
}
