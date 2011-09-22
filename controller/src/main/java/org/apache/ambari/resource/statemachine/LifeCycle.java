package org.apache.ambari.resource.statemachine;

/**
 * All participants have two states -
 * ACTIVE, INACTIVE
 * 
 */
public interface LifeCycle {
  public void activate();
  public void deactivate();
}
