package org.apache.ambari.resource.statemachine;

public enum RoleEventType {
  
  //Producer:Client, Cluster
  S_START,

  //Producer:Client, Cluster
  S_STOP,

  //Producer: Service
  S_START_SUCCESS,
  
  //Producer: Service
  S_START_FAILURE,
  
  //Producer: Service
  S_STOP_SUCCESS,
  
  //Producer: Service
  S_STOP_FAILURE,
  
}
