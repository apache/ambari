package org.apache.ambari.server.fsm;

public enum JobEventType {
  /**
   * Initial state for the action when triggered.
   */
  ACTION_INIT,
  /**
   * Action still in progress.
   */
  ACTION_IN_PROGRESS,
  /**
   * Action completed successfully.
   */
  ACTION_COMPLETED,
  /**
   * Action failed to complete successfully.
   */
  ACTION_FAILED
}
