package org.apache.ambari.resource.statemachine;

import org.apache.ambari.event.AbstractEvent;


public class RoleEvent extends AbstractEvent<RoleEventType> {
  Role role;
  public RoleEvent(RoleEventType eventType, Role role) {
    super (eventType);
    this.role = role;
  }
  
  public Role getRole() {
    return role;
  }

}
