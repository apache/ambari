package org.apache.ambari.server.state.live;

public enum JobState {
  /**
   * Initial state for the Job.
   * When a new action is triggered or set in motion.
   */
  INIT,
  /**
   * State when the job is triggered on the cluster,
   */
  IN_PROGRESS,
  /**
   * State of successful completion
   */
  COMPLETED,
  /**
   * Job failed to complete successfully
   */
  FAILED
}
