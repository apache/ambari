package org.apache.ambari.server.state.live.job;

public class JobProgressUpdateEvent extends JobEvent {

  private final long progressUpdateTime;

  public JobProgressUpdateEvent(JobId jobId, long progressUpdateTime) {
    super(JobEventType.JOB_IN_PROGRESS, jobId);
    this.progressUpdateTime = progressUpdateTime;
  }

  /**
   * @return the progressUpdateTime
   */
  public long getProgressUpdateTime() {
    return progressUpdateTime;
  }

}
