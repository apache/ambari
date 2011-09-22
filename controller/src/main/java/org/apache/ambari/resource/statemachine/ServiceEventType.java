package org.apache.ambari.resource.statemachine;

public enum ServiceEventType {
  
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
  
  //Producer: Role
  S_ROLE_STARTED
}
