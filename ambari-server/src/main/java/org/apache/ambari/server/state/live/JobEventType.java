package org.apache.ambari.server.state.live;

public enum JobEventType {
  /**
   * Initial state for the job when triggered.
   */
  JOB_INIT,
  /**
   * Job still in progress.
   */
  JOB_IN_PROGRESS,
  /**
   * Job completed successfully.
   */
  JOB_COMPLETED,
  /**
   * Job failed to complete successfully.
   */
  JOB_FAILED
}
