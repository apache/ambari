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

import com.google.inject.Inject;

public class RoleImpl implements RoleFSM, EventHandler<RoleEvent> {

  private final String roleName;
  private final ServiceFSM service;
  private static StateMachineInvokerInterface stateMachineInvoker;
  @Inject
  public static void setInvoker(StateMachineInvokerInterface sm) {
    stateMachineInvoker = sm;
  }
  /* The state machine for the role looks like:
   * (INACTIVE or FAIL) --S_START--> STARTING --S_START_SUCCESS--> ACTIVE
   *                                --S_START_FAILURE--> FAIL
   * (ACTIVE or FAIL) --S_STOP--> STOPPING --S_STOP_SUCCESS--> INACTIVE
   *                             --S_STOP_FAILURE--> FAIL
   */
  
  private static final StateMachineFactory 
  <RoleImpl, RoleState, RoleEventType, RoleEvent> stateMachineFactory 
         = new StateMachineFactory<RoleImpl, RoleState, RoleEventType, 
         RoleEvent>(RoleState.INACTIVE)

         //START event transitions
         .addTransition(RoleState.INACTIVE, RoleState.STARTING, 
             RoleEventType.START)
             
         .addTransition(RoleState.FAIL, RoleState.STARTING, 
             RoleEventType.START)
             
          //START_SUCCESS event transitions   
          //if one instance of the role starts up fine, we consider the service
          //as ready for the 'safe-mode' kinds of checks
         .addTransition(RoleState.STARTING, RoleState.ACTIVE,
             RoleEventType.START_SUCCESS, new SuccessfulStartTransition())

          //TODO: add support notion of quorom of nodes that need to be up
         .addTransition(RoleState.STARTING, RoleState.FAIL,
             RoleEventType.START_FAILURE, new FailedStartTransition())

             
         .addTransition(RoleState.STARTING, RoleState.STOPPING, 
             RoleEventType.STOP)
             
         .addTransition(RoleState.ACTIVE, RoleState.ACTIVE,
             RoleEventType.START_SUCCESS)

          //required number of nodes have this role started
         .addTransition(RoleState.ACTIVE, RoleState.ACTIVE,
             RoleEventType.START_FAILURE)
             
          //STOP event transitions   
         .addTransition(RoleState.ACTIVE, RoleState.STOPPING, 
             RoleEventType.STOP)

         .addTransition(RoleState.FAIL, RoleState.STOPPING, 
             RoleEventType.STOP)

          //STOP_SUCCESS event transitions   
         .addTransition(RoleState.STOPPING, RoleState.INACTIVE,
             RoleEventType.STOP_SUCCESS, new SuccessfulStopTransition())

         .addTransition(RoleState.INACTIVE, RoleState.INACTIVE,
             RoleEventType.STOP_SUCCESS)

          //enough number of nodes have stopped already
         .addTransition(RoleState.INACTIVE, RoleState.INACTIVE,
             RoleEventType.STOP_FAILURE)
             
          //STOP_FAILURE event transitions                
         .addTransition(RoleState.STOPPING, RoleState.FAIL,
             RoleEventType.STOP_FAILURE, new FailedStopTransition())
             
         .installTopology();
  
  private final StateMachine<RoleState, RoleEventType, RoleEvent>
      stateMachine;
  
  public RoleImpl(ServiceFSM service, String roleName) {
    this.roleName = roleName;
    this.service = service;
    stateMachine = stateMachineFactory.make(this);
  }
  
  StateMachine<RoleState, RoleEventType, RoleEvent> getStateMachine() {
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
  
  
  static void sendEventToService(RoleImpl operand, RoleEvent event,
      ServiceEventType serviceEvent) {
    ServiceFSM service = operand.getAssociatedService();
    stateMachineInvoker.getAMBARIEventHandler().handle(
        new ServiceEvent(serviceEvent, service, 
            operand));
  }
  
 
  static class SuccessfulStartTransition  implements 
  SingleArcTransition<RoleImpl, RoleEvent> {

    @Override
    public void transition(RoleImpl operand, RoleEvent event) {
      //if one instance of the role starts up fine, we consider the service
      //as ready for the 'safe-mode' kinds of checks
      sendEventToService(operand, event, ServiceEventType.ROLE_START_SUCCESS);
    }
  }
  
  static class FailedStartTransition implements 
  SingleArcTransition<RoleImpl, RoleEvent>  {

    @Override
    public void transition(RoleImpl operand, RoleEvent event) {
      //TODO : add support for notion of quorum
      sendEventToService(operand, event, ServiceEventType.ROLE_START_FAILURE);
    }
  }
  
  static class SuccessfulStopTransition implements
  SingleArcTransition<RoleImpl, RoleEvent> {
    //TODO: figure out if we need notion of quorum for stop success
    @Override
    public void transition(RoleImpl operand, RoleEvent event) {
      sendEventToService(operand, event, ServiceEventType.ROLE_STOP_SUCCESS);
    }
  }
  
  static class FailedStopTransition implements
  SingleArcTransition<RoleImpl, RoleEvent> {
    //TODO: figure out if we need notion of quorum for stop success
    @Override
    public void transition(RoleImpl operand, RoleEvent event) {
      sendEventToService(operand, event, ServiceEventType.ROLE_STOP_FAILURE);
    }
  }

  @Override
  public void activate() {
    stateMachineInvoker.getAMBARIEventHandler()
       .handle(new RoleEvent(RoleEventType.START, this));
  }

  @Override
  public void deactivate() {
    stateMachineInvoker.getAMBARIEventHandler()
       .handle(new RoleEvent(RoleEventType.STOP, this));  
  }

  @Override
  public boolean shouldStop() {
    return getRoleState() == RoleState.STOPPING 
        || getRoleState() == RoleState.INACTIVE;
  }

  @Override
  public boolean shouldStart() {
    return getRoleState() == RoleState.STARTING 
        || getRoleState() == RoleState.ACTIVE;
  }

}
