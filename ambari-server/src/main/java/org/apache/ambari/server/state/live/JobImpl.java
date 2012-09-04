package org.apache.ambari.server.state.live;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;

public class JobImpl implements Job {

  private static final StateMachineFactory
    <JobImpl, JobState, JobEventType, JobEvent>
      stateMachineFactory
        = new StateMachineFactory<JobImpl, JobState,
          JobEventType, JobEvent>
            (JobState.INIT)

    // define the state machine of a Job

    .addTransition(JobState.INIT, JobState.IN_PROGRESS,
        JobEventType.JOB_IN_PROGRESS)
    .addTransition(JobState.IN_PROGRESS, JobState.IN_PROGRESS,
        JobEventType.JOB_IN_PROGRESS)
    .addTransition(JobState.IN_PROGRESS, JobState.COMPLETED,
        JobEventType.JOB_COMPLETED)
    .addTransition(JobState.IN_PROGRESS, JobState.FAILED,
        JobEventType.JOB_FAILED)
    .addTransition(JobState.COMPLETED, JobState.INIT,
        JobEventType.JOB_INIT)
    .addTransition(JobState.FAILED, JobState.INIT,
        JobEventType.JOB_INIT)
    .installTopology();

  private final StateMachine<JobState, JobEventType, JobEvent>
      stateMachine;

  public JobImpl() {
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

  @Override
  public JobId getId() {
    // TODO Auto-generated method stub
    return null;
  }

}
