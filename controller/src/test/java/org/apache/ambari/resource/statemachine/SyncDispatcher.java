package org.apache.ambari.resource.statemachine;

import org.apache.ambari.event.Dispatcher;
import org.apache.ambari.event.Event;
import org.apache.ambari.event.EventHandler;

/**
 * To test objects with state in isolation, set this no-op Dispatcher
 */
class SyncDispatcher implements Dispatcher{
  class SyncEventHandler implements EventHandler<Event>{

    @Override
    public void handle(Event event) {
      Class<?> eventClass = event.getType().getDeclaringClass();
      if(eventClass.equals(ClusterEventType.class)){
        ClusterEvent cevent = (ClusterEvent)event;
        ((EventHandler<ClusterEvent>)cevent.getCluster()).handle(cevent);
      }
      else if(eventClass.equals(ServiceEventType.class)){
        ServiceEvent sevent = (ServiceEvent)event;
        ((EventHandler<ServiceEvent>)sevent.getService()).handle(sevent);
      }
      else if(eventClass.equals(RoleEventType.class)){
        RoleEvent revent = (RoleEvent)event;
        ((EventHandler<RoleEvent>)revent.getRole()).handle(revent);
      }
      else {
        throw new UnsupportedOperationException("invalid event class: " + eventClass);
      }
    }
    
  }
  EventHandler<?> ehandler = new SyncEventHandler();
  
  @Override
  public EventHandler<?> getEventHandler() {
    return ehandler;
  }

  @Override
  public void register(Class<? extends Enum> eventType, EventHandler handler) {
    //no-op
  }

  @Override
  public void start() {
    //no-op
  }
  
}