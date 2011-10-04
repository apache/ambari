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

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.common.state.MultipleArcTransition;
import org.apache.ambari.common.state.SingleArcTransition;
import org.apache.ambari.common.state.StateMachine;
import org.apache.ambari.common.state.StateMachineFactory;
import org.apache.ambari.components.ComponentPlugin;
import org.apache.ambari.components.impl.HDFSPluginImpl;
import org.apache.ambari.event.EventHandler;

public class ServiceImpl implements Service, EventHandler<ServiceEvent> {

  private ServiceState myState;
  private Cluster cluster;
  private ComponentPlugin plugin;
  
  /* The state machine for the service looks like:
   * INACTIVE --S_START--> STARTING --S_START_SUCCESS--> ACTIVE
   *                                --S_START_FAILURE--> FAIL
   * ACTIVE --S_STOP--> STOPPING --S_STOP_SUCCESS--> INACTIVE
   *                             --S_STOP_FAILURE--> UNCLEAN_STOP
   * FAIL --S_STOP--> STOPPING --S_STOP_SUCCESS--> INACTIVE
   *                           --S_STOP_FAILURE--> UNCLEAN_STOP
   */
  
  private static final StateMachineFactory 
  <ServiceImpl, ServiceState, ServiceEventType, ServiceEvent> stateMachineFactory 
         = new StateMachineFactory<ServiceImpl, ServiceState, ServiceEventType, 
         ServiceEvent>(ServiceState.INACTIVE)
         
         .addTransition(ServiceState.INACTIVE, ServiceState.STARTING, 
             ServiceEventType.START, new StartServiceTransition())
             
         .addTransition(ServiceState.STARTING, 
             EnumSet.of(ServiceState.ACTIVE, ServiceState.STARTING), 
             ServiceEventType.ROLE_STARTED,
             new RoleStartedTransition())
             
         .addTransition(ServiceState.STARTING, ServiceState.FAIL, 
             ServiceEventType.START_FAILURE)
             
         .addTransition(ServiceState.ACTIVE, ServiceState.ACTIVE, 
             ServiceEventType.ROLE_STARTED)
             
         .addTransition(ServiceState.ACTIVE, ServiceState.STOPPING, 
             ServiceEventType.STOP)
             
         .addTransition(ServiceState.STOPPING, 
             EnumSet.of(ServiceState.INACTIVE, ServiceState.STOPPING),
             ServiceEventType.STOP_SUCCESS, 
             new RoleStoppedTransition())
             
         .addTransition(ServiceState.STOPPING, ServiceState.UNCLEAN_STOP, 
             ServiceEventType.STOP_FAILURE)
             
         .addTransition(ServiceState.FAIL, ServiceState.STOPPING, 
             ServiceEventType.STOP)
             
         .addTransition(ServiceState.STOPPING, ServiceState.INACTIVE, 
             ServiceEventType.STOP_SUCCESS)
             
         .addTransition(ServiceState.STOPPING, ServiceState.UNCLEAN_STOP,
             ServiceEventType.STOP_FAILURE)
             
         .installTopology();
  
  private final StateMachine<ServiceState, ServiceEventType, ServiceEvent>
      stateMachine;
  private final List<Role> serviceRoles = new ArrayList<Role>();
  private final String serviceName;
  private short roleCount;
  
  public ServiceImpl(Cluster cluster, String serviceName) throws IOException {
    this.cluster = cluster;
    this.serviceName = serviceName;
    this.myState = ServiceState.INACTIVE;
    //load plugin and get the roles and create them
    this.plugin = new HDFSPluginImpl();
    String[] roles = this.plugin.getActiveRoles();
    for (String role : roles) {
      RoleImpl roleImpl = new RoleImpl(this, role);
      serviceRoles.add(roleImpl);
    }
    stateMachine = stateMachineFactory.make(this);
  }
    
  public StateMachine getStateMachine() {
    return stateMachine;
  }
  
  @Override
  public ServiceState getServiceState() {
    return stateMachine.getCurrentState();
  }

  @Override
  public void handle(ServiceEvent event) {
    getStateMachine().doTransition(event.getType(), event);
  }

  @Override
  public Cluster getAssociatedCluster() {
    return cluster;
  }
  
  @Override
  public String getServiceName() {
    return serviceName;
  }
  
  public void addRoles(List<Role> roles) {
    this.serviceRoles.addAll(roles);
  }
  
  public Role getNextRole() {
    if (++roleCount <= serviceRoles.size()) {
      return serviceRoles.get(roleCount - 1);  
    }
    return null;
  }

  static class StartServiceTransition implements 
  SingleArcTransition<ServiceImpl, ServiceEvent>  {

    @Override
    public void transition(ServiceImpl operand, ServiceEvent event) {
      Role firstRole = operand.getNextRole();
      if (firstRole != null) {
        StateMachineInvoker.getAMBARIEventHandler().handle(
                            new RoleEvent(RoleEventType.START, firstRole));
      }
    }
    
  }
  
  static class RoleStartedTransition 
  implements MultipleArcTransition<ServiceImpl, ServiceEvent, ServiceState>  {

    @Override
    public ServiceState transition(ServiceImpl operand, ServiceEvent event) {
      //check whether all roles started, and if not remain in the STARTING
      //state, else move to the ACTIVE state
      Role role = operand.getNextRole();
      if (role != null) {
        StateMachineInvoker.getAMBARIEventHandler().handle(new RoleEvent(
            RoleEventType.START, role));
        return ServiceState.STARTING;
      } else {
        return ServiceState.ACTIVE;
      }
    }
  }
  
  static class RoleStoppedTransition 
  implements MultipleArcTransition<ServiceImpl, ServiceEvent, ServiceState>  {

    @Override
    public ServiceState transition(ServiceImpl operand, ServiceEvent event) {
      //check whether all roles stopped, and if not remain in the STOPPING
      //state, else move to the ACTIVE state
      Role role = operand.getNextRole();
      if (role != null) {
        StateMachineInvoker.getAMBARIEventHandler().handle(new RoleEvent(
            RoleEventType.STOP, role));
        return ServiceState.STOPPING;
      } else {
        return ServiceState.INACTIVE;
      }
    }
    
  }

  @Override
  public void activate() {
    StateMachineInvoker.getAMBARIEventHandler().handle(
              new ServiceEvent(ServiceEventType.START, this));
  }

  @Override
  public void deactivate() {
    StateMachineInvoker.getAMBARIEventHandler().handle(
              new ServiceEvent(ServiceEventType.STOP, this));
  }

  @Override
  public boolean isActive() {
    return myState == ServiceState.ACTIVE;
  }

  @Override
  public List<Role> getRoles() {
    return serviceRoles;
  }
}
