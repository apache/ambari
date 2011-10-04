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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.common.state.MultipleArcTransition;
import org.apache.ambari.common.state.SingleArcTransition;
import org.apache.ambari.common.state.StateMachine;
import org.apache.ambari.common.state.StateMachineFactory;
import org.apache.ambari.event.EventHandler;

public class RoleImpl implements Role, EventHandler<RoleEvent> {

  private RoleState myState;
  private String roleName;
  private int totalRolesRequired;
  private int totalRolesStarted;
  private int totalRolesFailedToStart;
  private int totalRoles;
  private Service service;
  
  /* The state machine for the role looks like:
   * INACTIVE --S_START--> STARTING --S_START_SUCCESS--> ACTIVE
   *                                --S_START_FAILURE--> FAIL
   * ACTIVE --S_STOP--> STOPPING --S_STOP_SUCCESS--> INACTIVE
   *                             --S_STOP_FAILURE--> UNCLEAN_STOP
   * FAIL --S_STOP--> STOPPING --S_STOP_SUCCESS--> INACTIVE
   *                           --S_STOP_FAILURE--> UNCLEAN_STOP
   */
  
  private static final StateMachineFactory 
  <RoleImpl, RoleState, RoleEventType, RoleEvent> stateMachineFactory 
         = new StateMachineFactory<RoleImpl, RoleState, RoleEventType, 
         RoleEvent>(RoleState.INACTIVE)
         
         .addTransition(RoleState.INACTIVE, RoleState.STARTING, 
             RoleEventType.START)
             
         .addTransition(RoleState.STARTING, 
             EnumSet.of(RoleState.ACTIVE, RoleState.STARTING),
             RoleEventType.START_SUCCESS, new SuccessfulStartTransition())
         
         .addTransition(RoleState.ACTIVE, RoleState.ACTIVE,
             RoleEventType.START_SUCCESS)
             
         .addTransition(RoleState.STARTING, 
             EnumSet.of(RoleState.FAIL, RoleState.STARTING),
             RoleEventType.START_FAILURE, new FailedStartTransition())
             
         .addTransition(RoleState.FAIL, RoleState.FAIL, 
             RoleEventType.START_FAILURE)
             
         .addTransition(RoleState.ACTIVE, RoleState.STOPPING, 
             RoleEventType.STOP)
             
         .addTransition(RoleState.STOPPING, RoleState.INACTIVE,
             RoleEventType.STOP_SUCCESS)
             
         .addTransition(RoleState.STOPPING, RoleState.UNCLEAN_STOP,
             RoleEventType.STOP_FAILURE)
             
         .addTransition(RoleState.FAIL, RoleState.STOPPING, RoleEventType.STOP)
         
         .addTransition(RoleState.STOPPING, RoleState.INACTIVE, 
             RoleEventType.STOP_SUCCESS)
             
         .addTransition(RoleState.INACTIVE, RoleState.INACTIVE,
             RoleEventType.STOP_SUCCESS)
             
         .addTransition(RoleState.STOPPING, RoleState.UNCLEAN_STOP, 
             RoleEventType.STOP_FAILURE)
             
         .addTransition(RoleState.UNCLEAN_STOP, RoleState.UNCLEAN_STOP,
             RoleEventType.STOP_FAILURE)
             
         .installTopology();
  
  private final StateMachine<RoleState, RoleEventType, RoleEvent>
      stateMachine;
  
  public RoleImpl(Service service, String roleName) {
    this(service, roleName, 1, 1);
  }
  
  public RoleImpl(Service service, String roleName, int totalRoles, int totalRolesRequired) {
    this.roleName = roleName;
    this.service = service;
    this.myState = RoleState.INACTIVE;
    this.totalRolesRequired = totalRolesRequired;
    this.totalRoles = totalRoles;
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
  public Service getAssociatedService() {
    return service;
  }
  
  static class SuccessfulStartTransition implements 
  MultipleArcTransition<RoleImpl, RoleEvent, RoleState>  {

    @Override
    public RoleState transition(RoleImpl operand, RoleEvent event) {
      ServiceImpl service = (ServiceImpl)operand.getAssociatedService();
      ++operand.totalRolesStarted;
      if (operand.totalRolesRequired <= operand.totalRolesStarted) {
        StateMachineInvoker.getAMBARIEventHandler().handle(
            new ServiceEvent(ServiceEventType.ROLE_STARTED, service, 
                operand));

        return RoleState.ACTIVE;
      } else {
        return RoleState.STARTING;   
      }
    }
  }
  
  static class FailedStartTransition implements 
  MultipleArcTransition<RoleImpl, RoleEvent, RoleState>  {

    @Override
    public RoleState transition(RoleImpl operand, RoleEvent event) {
      ServiceImpl service = (ServiceImpl)operand.getAssociatedService();
      ++operand.totalRolesFailedToStart;
      //if number of remaining roles required to declare a role as 'started'
      //is more than the total number of roles that haven't reported back
      //declare the role failed to start
      if ((operand.totalRolesRequired - operand.totalRolesStarted) 
          >= (operand.totalRoles - 
              (operand.totalRolesStarted + operand.totalRolesFailedToStart))) {
        StateMachineInvoker.getAMBARIEventHandler().handle(
            new ServiceEvent(ServiceEventType.START_FAILURE, service, 
                operand));
        return RoleState.FAIL;
      } else {
        return RoleState.STARTING;   
      }
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
    return myState == RoleState.STOPPING;
  }

  @Override
  public boolean shouldStart() {
    return myState == RoleState.STARTING;
  }
}
