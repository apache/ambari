package org.apache.ambari.server.fsm;

import org.apache.ambari.server.ServiceComponentNodeState;;

public interface ServiceComponentNodeFSM {

  public ServiceComponentNodeState getState();

  public void setState(ServiceComponentNodeState state);

  public void handleEvent(ServiceComponentNodeEvent event)
      throws InvalidStateTransitonException;

}
