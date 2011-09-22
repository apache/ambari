package org.apache.ambari.resource.statemachine;

import org.apache.ambari.event.AbstractEvent;


public class ServiceEvent extends AbstractEvent<ServiceEventType> {
  private Service service;
  private Role role;
  
  public ServiceEvent(ServiceEventType eventType, Service service) {
    super (eventType);
    this.service = service;
  }
  
  public ServiceEvent(ServiceEventType eventType, Service service, Role role) {
    super (eventType);
    this.service = service;
    this.role = role;
  }
  
  public Service getService() {
    return service;
  }
  
  public Role getRole() {
    return role;
  }

}
