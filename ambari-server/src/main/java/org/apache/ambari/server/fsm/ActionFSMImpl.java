package org.apache.ambari.server.fsm;

import org.apache.ambari.server.ActionState;
import org.apache.ambari.server.fsm.StateMachineFactory;

public class ActionFSMImpl implements ActionFSM {

  private static final StateMachineFactory
    <ActionFSMImpl, ActionState, ActionEventType, ActionEvent>
      stateMachineFactory
        = new StateMachineFactory<ActionFSMImpl, ActionState,
          ActionEventType, ActionEvent>
            (ActionState.INIT)

    // define the state machine of a Action

    .addTransition(ActionState.INIT, ActionState.IN_PROGRESS,
        ActionEventType.ACTION_IN_PROGRESS)
    .addTransition(ActionState.IN_PROGRESS, ActionState.IN_PROGRESS,
        ActionEventType.ACTION_IN_PROGRESS)
    .addTransition(ActionState.IN_PROGRESS, ActionState.COMPLETED,
        ActionEventType.ACTION_COMPLETED)
    .addTransition(ActionState.IN_PROGRESS, ActionState.FAILED,
        ActionEventType.ACTION_FAILED)
    .addTransition(ActionState.COMPLETED, ActionState.INIT,
        ActionEventType.ACTION_INIT)
    .addTransition(ActionState.FAILED, ActionState.INIT,
        ActionEventType.ACTION_INIT)
    .installTopology();

}
