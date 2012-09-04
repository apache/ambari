package org.apache.ambari.server.fsm;

import org.apache.ambari.server.JobState;

public interface JobFSM {

  public JobState getState();

  public void setState(JobState state);

  public void handleEvent(JobEvent event)
      throws InvalidStateTransitonException;
}
