package org.apache.ambari.server;


public interface Action {

  /**
   * Get the Action ID for the action
   * @return ActionId
   */
  public ActionId getId();

  /**
   * Get the current state of the Action
   * @return ActionState
   */
  public ActionState getState();

}
