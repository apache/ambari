package org.apache.ambari.resource.statemachine;

import org.apache.ambari.event.Dispatcher;
import org.apache.ambari.event.Event;
import org.apache.ambari.event.EventHandler;

/**
 * To test objects with state in isolation, set this no-op Dispatcher
 */
class NoOPDispatcher implements Dispatcher{
  class NoOPEventHandler implements EventHandler<Event>{

    @Override
    public void handle(Event event) {
     //no-op
    }
    
  }
  EventHandler<?> ehandler = new NoOPEventHandler();
  
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