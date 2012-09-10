package org.apache.ambari.server.state.live.job;

public class JobCompletedEvent extends JobEvent {

  private final long completionTime;

  public JobCompletedEvent(JobId jobId, long completionTime) {
    super(JobEventType.JOB_COMPLETED, jobId);
    this.completionTime = completionTime;
  }

  /**
   * @return the completionTime
   */
  public long getCompletionTime() {
    return completionTime;
  }
}
