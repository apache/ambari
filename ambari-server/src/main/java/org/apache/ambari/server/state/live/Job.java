package org.apache.ambari.server.state.live;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;

public interface Job {

  /**
   * Get the Job ID for the action
   * @return JobId
   */
  public JobId getId();

  // TODO requires some form of JobType to ensure only one running
  // action per action type
  // There may be gotchas such as de-commissioning should be allowed to happen
  // on more than one node at a time  
  
  /**
   * Get the current state of the Job
   * @return JobState
   */
  public JobState getState();

  /**
   * Set the State of the Job
   * @param state JobState
   */
  public void setState(JobState state);

  /**
   * Send a JobEvent to the Job's StateMachine
   * @param event JobEvent
   * @throws InvalidStateTransitonException
   */
  public void handleEvent(JobEvent event)
      throws InvalidStateTransitonException;
}
