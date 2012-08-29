package org.apache.ambari.server;

public enum ActionState {
  /**
   * Initial state for the Action.
   * When a new action is triggered or set in motion.
   */
  INIT,
  /**
   * State when the action is triggered on the cluster,
   */
  IN_PROGRESS,
  /**
   * State of successful completion
   */
  COMPLETED,
  /**
   * Action failed to complete successfully
   */
  FAILED
}
