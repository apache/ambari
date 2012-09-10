package org.apache.ambari.server.state.live.job;

public class NewJobEvent extends JobEvent {

  private final long startTime;

  public NewJobEvent(JobId jobId, long creationTime) {
    super(JobEventType.JOB_INIT, jobId);
    this.startTime = creationTime;
  }

  /**
   * @return the start time of the Job
   */
  public long getStartTime() {
    return startTime;
  }

}
