/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.ambari.resource.statemachine;

import org.apache.ambari.common.state.SingleArcTransition;
import org.apache.ambari.common.state.StateMachine;
import org.apache.ambari.common.state.StateMachineFactory;
import org.apache.ambari.event.EventHandler;

public class RoleImpl implements RoleFSM, EventHandler<RoleEvent> {

  private RoleState myState;
  private String roleName;
  private ServiceFSM service;
  
  /* The state machine for the role looks like:
   * INACTIVE --S_START--> STARTING --S_START_SUCCESS--> ACTIVE
   *                                --S_START_FAILURE--> FAIL
   * ACTIVE --S_STOP--> STOPPING --S_STOP_SUCCESS--> INACTIVE
   *                             --S_STOP_FAILURE--> UNCLEAN_STOP
   * FAIL --S_STOP--> STOPPING --S_STOP_SUCCESS--> STOPPED
   *                           --S_STOP_FAILURE--> UNCLEAN_STOP
   */
  
  private static final StateMachineFactory 
  <RoleImpl, RoleState, RoleEventType, RoleEvent> stateMachineFactory 
         = new StateMachineFactory<RoleImpl, RoleState, RoleEventType, 
         RoleEvent>(RoleState.INACTIVE)
         
         .addTransition(RoleState.INACTIVE, RoleState.STARTING, 
             RoleEventType.START)
             
         .addTransition(RoleState.STOPPED, RoleState.STARTING, 
             RoleEventType.START)
             
         .addTransition(RoleState.STARTING, 
             RoleState.ACTIVE,
             RoleEventType.START_SUCCESS, new SuccessfulStartTransition())
         
         .addTransition(RoleState.ACTIVE, RoleState.ACTIVE,
             RoleEventType.START_SUCCESS)
             
         .addTransition(RoleState.STARTING, 
             RoleState.STARTING,
             RoleEventType.START_FAILURE)
             
         .addTransition(RoleState.FAIL, RoleState.FAIL, 
             RoleEventType.START_FAILURE)
             
         .addTransition(RoleState.ACTIVE, RoleState.STOPPING, 
             RoleEventType.STOP)
             
         .addTransition(RoleState.STOPPING, RoleState.INACTIVE,
             RoleEventType.STOP_SUCCESS, new RoleStopTransition())
             
         .addTransition(RoleState.STOPPING, RoleState.UNCLEAN_STOP,
             RoleEventType.STOP_FAILURE)
             
         .addTransition(RoleState.FAIL, RoleState.STOPPING, RoleEventType.STOP)
         
         .addTransition(RoleState.STOPPING, RoleState.STOPPED, 
             RoleEventType.STOP_SUCCESS)
             
         .addTransition(RoleState.STOPPED, RoleState.STOPPED,
             RoleEventType.STOP_SUCCESS)
             
         .addTransition(RoleState.STOPPING, RoleState.UNCLEAN_STOP, 
             RoleEventType.STOP_FAILURE)
             
         .addTransition(RoleState.UNCLEAN_STOP, RoleState.UNCLEAN_STOP,
             RoleEventType.STOP_FAILURE)
             
         .installTopology();
  
  private final StateMachine<RoleState, RoleEventType, RoleEvent>
      stateMachine;
  
  public RoleImpl(ServiceFSM service, String roleName) {
    this.roleName = roleName;
    this.service = service;
    this.myState = RoleState.INACTIVE;
    stateMachine = stateMachineFactory.make(this);
  }
  
  public StateMachine getStateMachine() {
    return stateMachine;
  }
  
  @Override
  public RoleState getRoleState() {
    return stateMachine.getCurrentState();
  }

  @Override
  public String getRoleName() {
    return roleName;
  }

  @Override
  public void handle(RoleEvent event) {
    getStateMachine().doTransition(event.getType(), event);
  }

  @Override
  public ServiceFSM getAssociatedService() {
    return service;
  }
  
  static class SuccessfulStartTransition implements 
  SingleArcTransition<RoleImpl, RoleEvent>  {

    @Override
    public void transition(RoleImpl operand, RoleEvent event) {
      ServiceFSM service = operand.getAssociatedService();
      //if one instance of the role starts up fine, we consider the service
      //as ready for the 'safe-mode' kinds of checks
      StateMachineInvoker.getAMBARIEventHandler().handle(
          new ServiceEvent(ServiceEventType.ROLE_STARTED, service, 
              operand));
    }
  }
  
  
  static class RoleStopTransition implements
  SingleArcTransition<RoleImpl, RoleEvent> {
    
    @Override
    public void transition(RoleImpl operand, RoleEvent event) {
      ServiceFSM service = operand.getAssociatedService();
      StateMachineInvoker.getAMBARIEventHandler().handle(
          new ServiceEvent(ServiceEventType.ROLE_STOPPED, service,
              operand));
    }
  }

  @Override
  public void activate() {
    //load the plugin and get the commands for starting the role
  }

  @Override
  public void deactivate() {
    
  }

  @Override
  public boolean shouldStop() {
    return myState == RoleState.STOPPING || myState == RoleState.STOPPED;
  }

  @Override
  public boolean shouldStart() {
    return myState == RoleState.STARTING || myState == RoleState.ACTIVE;
  }
}
