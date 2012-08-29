package org.apache.ambari.server.fsm;

import org.apache.ambari.server.fsm.event.AbstractEvent;

public class ActionEvent extends AbstractEvent<ActionEventType> {

  // TODO
  // this should have some action identifier
  public ActionEvent(ActionEventType type) {
    super(type);
    // TODO Auto-generated constructor stub
  }
}
