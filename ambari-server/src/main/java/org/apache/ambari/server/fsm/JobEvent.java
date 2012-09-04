package org.apache.ambari.server.fsm;

import org.apache.ambari.server.fsm.event.AbstractEvent;

public class JobEvent extends AbstractEvent<JobEventType> {

  // TODO
  // this should have some action identifier
  public JobEvent(JobEventType type) {
    super(type);
    // TODO Auto-generated constructor stub
  }
}
