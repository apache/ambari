package org.apache.ambari.server.state.live.job;

public class JobFailedEvent extends JobEvent {

  private final long completionTime;

  // TODO
  // need to add job report

  public JobFailedEvent(JobId jobId, long completionTime) {
    super(JobEventType.JOB_FAILED, jobId);
    this.completionTime = completionTime;
  }

  /**
   * @return the completionTime
   */
  public long getCompletionTime() {
    return completionTime;
  }
}
