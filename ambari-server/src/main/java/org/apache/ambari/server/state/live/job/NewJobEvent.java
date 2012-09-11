package org.apache.ambari.server.state.live.job;

public class NewJobEvent extends JobEvent {

  private final long startTime;

  public NewJobEvent(JobId jobId, long startTime) {
    super(JobEventType.JOB_INIT, jobId);
    this.startTime = startTime;
  }

  /**
   * @return the start time of the Job
   */
  public long getStartTime() {
    return startTime;
  }

}
