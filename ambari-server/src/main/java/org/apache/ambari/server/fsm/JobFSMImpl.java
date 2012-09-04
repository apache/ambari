package org.apache.ambari.server.fsm;

import org.apache.ambari.server.JobState;
import org.apache.ambari.server.fsm.StateMachineFactory;

public class JobFSMImpl implements JobFSM {

  private static final StateMachineFactory
    <JobFSMImpl, JobState, JobEventType, JobEvent>
      stateMachineFactory
        = new StateMachineFactory<JobFSMImpl, JobState,
          JobEventType, JobEvent>
            (JobState.INIT)

    // define the state machine of a Action

    .addTransition(JobState.INIT, JobState.IN_PROGRESS,
        JobEventType.ACTION_IN_PROGRESS)
    .addTransition(JobState.IN_PROGRESS, JobState.IN_PROGRESS,
        JobEventType.ACTION_IN_PROGRESS)
    .addTransition(JobState.IN_PROGRESS, JobState.COMPLETED,
        JobEventType.ACTION_COMPLETED)
    .addTransition(JobState.IN_PROGRESS, JobState.FAILED,
        JobEventType.ACTION_FAILED)
    .addTransition(JobState.COMPLETED, JobState.INIT,
        JobEventType.ACTION_INIT)
    .addTransition(JobState.FAILED, JobState.INIT,
        JobEventType.ACTION_INIT)
    .installTopology();

  private final StateMachine<JobState, JobEventType, JobEvent>
      stateMachine;

  public JobFSMImpl() {
    super();
    this.stateMachine = stateMachineFactory.make(this);
  }

  @Override
  public JobState getState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setState(JobState state) {
    // TODO Auto-generated method stub

  }

  @Override
  public void handleEvent(JobEvent event)
      throws InvalidStateTransitonException {
    // TODO
    stateMachine.doTransition(event.getType(), event);
  }

}
