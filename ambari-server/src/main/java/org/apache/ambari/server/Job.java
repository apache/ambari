package org.apache.ambari.server;


public interface Job {

  /**
   * Get the Action ID for the action
   * @return ActionId
   */
  public JobId getId();

  /**
   * Get the current state of the Action
   * @return ActionState
   */
  public JobState getState();


  // TODO requires some form of ActionType to ensure only one running
  // action per action type
  // There may be gotchas such as decomissioning should be allowed to happen
  // on more than one node at a time
}
