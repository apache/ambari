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
import java.util.Iterator;
import java.util.List;

import org.apache.ambari.common.state.MultipleArcTransition;
import org.apache.ambari.common.state.SingleArcTransition;
import org.apache.ambari.common.state.StateMachine;
import org.apache.ambari.common.state.StateMachineFactory;
import org.apache.ambari.event.EventHandler;

import com.google.inject.Inject;

public class ServiceImpl implements ServiceFSM, EventHandler<ServiceEvent> {

  private final ClusterFSM clusterFsm;
  
  /* The state machine for the service looks like:
   * INACTIVE or FAIL --S_START--> PRESTART
   * PRESTART --S_PRESTART_FAILURE--> FAIL
   * PRESTART --S_PRESTART_SUCCESS--> STARTING --S_START_SUCCESS--> STARTED
   *                                           --S_START_FAILURE--> FAIL
   * STARTED --S_AVAILABLE_CHECK_SUCCESS--> ACTIVE  (check for things like safemode here)
   * STARTED --S_AVAILABLE_CHECK_FAILURE--> FAIL
   * ACTIVE or FAIL --S_STOP--> STOPPING --S_STOP_SUCCESS--> INACTIVE
   *                             --S_STOP_FAILURE--> FAIL
   */
  private static StateMachineInvokerInterface stateMachineInvoker;
  @Inject
  public static void setInvoker(StateMachineInvokerInterface sm) {
    stateMachineInvoker = sm;
  }
  private static final StateMachineFactory 
  <ServiceImpl, ServiceState, ServiceEventType, ServiceEvent> 
  stateMachineFactory 
         = new StateMachineFactory<ServiceImpl, ServiceState, ServiceEventType,
         ServiceEvent>(ServiceState.INACTIVE)
         
          .addTransition(ServiceState.INACTIVE, ServiceState.PRESTART, 
             ServiceEventType.START)
             
         .addTransition(ServiceState.FAIL, ServiceState.PRESTART, 
             ServiceEventType.START)
             
         .addTransition(ServiceState.PRESTART, ServiceState.FAIL, 
             ServiceEventType.PRESTART_FAILURE, new StartFailTransition())  
             
         .addTransition(ServiceState.PRESTART, ServiceState.STARTING, 
             ServiceEventType.PRESTART_SUCCESS, new StartServiceTransition())    
          
         .addTransition(ServiceState.STARTING, 
             EnumSet.of(ServiceState.STARTED, ServiceState.STARTING), 
             ServiceEventType.ROLE_START_SUCCESS,
             new RoleStartedTransition())
             
         .addTransition(ServiceState.STARTING, ServiceState.FAIL, 
             ServiceEventType.ROLE_START_FAILURE, new StartFailTransition())
             
         .addTransition(ServiceState.STARTED, ServiceState.ACTIVE,
             ServiceEventType.AVAILABLE_CHECK_SUCCESS, new AvailableTransition())
         
         .addTransition(ServiceState.STARTED, ServiceState.FAIL,
             ServiceEventType.AVAILABLE_CHECK_FAILURE, new StartFailTransition())
                      
         .addTransition(ServiceState.ACTIVE, ServiceState.ACTIVE, 
             ServiceEventType.ROLE_START_SUCCESS)
             
         .addTransition(ServiceState.ACTIVE, ServiceState.STOPPING, 
             ServiceEventType.STOP, new StopServiceTransition())
             
         .addTransition(ServiceState.STOPPING, 
             EnumSet.of(ServiceState.INACTIVE, ServiceState.STOPPING),
             ServiceEventType.ROLE_STOP_SUCCESS, new RoleStoppedTransition())
             
         .addTransition(ServiceState.STOPPING, ServiceState.FAIL, 
             ServiceEventType.ROLE_STOP_FAILURE, new StopFailTransition())
             
         .addTransition(ServiceState.FAIL, ServiceState.STOPPING, 
             ServiceEventType.STOP, new StopServiceTransition())
             
         .addTransition(ServiceState.INACTIVE, ServiceState.INACTIVE, 
             ServiceEventType.ROLE_STOP_SUCCESS)
             
         .installTopology();
  
  private final StateMachine<ServiceState, ServiceEventType, ServiceEvent>
      stateMachine;
  private final List<RoleFSM> serviceRoles = new ArrayList<RoleFSM>();
  private Iterator<RoleFSM> iterator;
  private final String serviceName;

  public ServiceImpl(String[] roles, ClusterFSM clusterFsm, String serviceName)
      throws IOException {
    this.clusterFsm = clusterFsm;
    this.serviceName = serviceName;
    setRoles(roles);
    stateMachine = stateMachineFactory.make(this);
  }
    
  private void setRoles(String[] roles) {
    serviceRoles.clear();
    //get the roles for this service
    for (String role : roles) {
      RoleImpl roleImpl = new RoleImpl(this, role);
      serviceRoles.add(roleImpl);
    }    
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
  public ClusterFSM getAssociatedCluster() {
    return clusterFsm;
  }
  
  @Override
  public String getServiceName() {
    return serviceName;
  }
    
  private RoleFSM getFirstRole() {
    //this call should reset the iterator
    iterator = serviceRoles.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }
    return null;
  }
  
  private RoleFSM getNextRole() {
    if (iterator.hasNext()) {
      return iterator.next();
    }
    return null;
  }

  static void sendEventToRole(RoleFSM role, RoleEventType roleEvent) {
    stateMachineInvoker.getAMBARIEventHandler().handle(
        new RoleEvent(roleEvent, role));
  }
  
  static class StartServiceTransition implements 
  SingleArcTransition<ServiceImpl, ServiceEvent>  {

    @Override
    public void transition(ServiceImpl operand, ServiceEvent event) {
      RoleFSM firstRole = operand.getFirstRole();
      if (firstRole != null) {
        sendEventToRole(firstRole, RoleEventType.START);
      }
    } 
  }
  
  static void sendEventToCluster(ClusterFSM cluster, ClusterEventType event){
    stateMachineInvoker.getAMBARIEventHandler().handle(
        new ClusterEvent(event, cluster));
  }
  
  static class AvailableTransition implements 
  SingleArcTransition<ServiceImpl, ServiceEvent>  {

    @Override
    public void transition(ServiceImpl operand, ServiceEvent event) {
      if (((ClusterImpl)operand.getAssociatedCluster()).getState() 
          == ClusterStateFSM.STARTING) {
        //since we support starting services explicitly (without touching the 
        //associated cluster), we need to check what the cluster state is
        //before sending it any event
        sendEventToCluster(operand.getAssociatedCluster(), ClusterEventType.START_SUCCESS);
      }
    } 
  }
  
  static class FailureTransition implements 
  SingleArcTransition<ServiceImpl, ServiceEvent>  {

    private ClusterStateFSM recievingClusterState;
    private ClusterEventType clusterEvent;
    
    protected FailureTransition(final ClusterStateFSM recievingClusterState,
        final ClusterEventType clusterEvent){
      this.recievingClusterState = recievingClusterState;
      this.clusterEvent = clusterEvent;
    }
    
    
    @Override
    public void transition(ServiceImpl operand, ServiceEvent event) {
      if (((ClusterImpl)operand.getAssociatedCluster()).getState() 
          == recievingClusterState) {
        //since we support starting/stopping services explicitly (without touching the 
        //associated cluster), we need to check what the cluster state is
        //before sending it any event
        sendEventToCluster(operand.getAssociatedCluster(), clusterEvent);
      }
    } 
  }
  
  
  static class StartFailTransition extends FailureTransition {
    protected StartFailTransition() {
      super(ClusterStateFSM.STARTING, ClusterEventType.START_FAILURE);
    }
  }
  
  static class StopFailTransition extends FailureTransition {
    protected StopFailTransition() {
      super(ClusterStateFSM.STOPPING, ClusterEventType.STOP_FAILURE);
    }
  }
  
  static class StopServiceTransition implements 
  SingleArcTransition<ServiceImpl, ServiceEvent>  {
    @Override
    public void transition(ServiceImpl operand, ServiceEvent event) {
      RoleFSM firstRole = operand.getFirstRole();
      if (firstRole != null){ 
        sendEventToRole(firstRole, RoleEventType.STOP);
      }
    }
  }
  
  static class RoleStartedTransition 
  implements MultipleArcTransition<ServiceImpl, ServiceEvent, ServiceState>  {

    @Override
    public ServiceState transition(ServiceImpl operand, ServiceEvent event) {
      //check whether all roles started, and if not remain in the STARTING
      //state, else move to the STARTED state
      RoleFSM role = operand.getNextRole();
      if (role != null) {
        sendEventToRole(role,  RoleEventType.START);
        return ServiceState.STARTING;
      } else {
        return ServiceState.STARTED;
      }
    }
  }
  
  static class RoleStoppedTransition 
  implements MultipleArcTransition<ServiceImpl, ServiceEvent, ServiceState>  {

    @Override
    public ServiceState transition(ServiceImpl operand, ServiceEvent event) {
      //check whether all roles stopped, and if not, remain in the STOPPING
      //state, else move to the INACTIVE state
      RoleFSM role = operand.getNextRole();
      if (role != null) {
        sendEventToRole(role,  RoleEventType.STOP);
        return ServiceState.STOPPING;
      } else {
        if (((ClusterImpl)operand.getAssociatedCluster()).getState() 
            == ClusterStateFSM.STOPPING) {
          //since we support stopping services explicitly (without stopping the 
          //associated cluster), we need to check what the cluster state is
          //before sending it any event
          sendEventToCluster(operand.getAssociatedCluster(), ClusterEventType.STOP_SUCCESS);
        }
        return ServiceState.INACTIVE;
      }
    }
  }

  @Override
  public void activate() {
    stateMachineInvoker.getAMBARIEventHandler().handle(
              new ServiceEvent(ServiceEventType.START, this));
  }

  @Override
  public void deactivate() {
    stateMachineInvoker.getAMBARIEventHandler().handle(
              new ServiceEvent(ServiceEventType.STOP, this));
  }

  @Override
  public boolean isActive() {
    return getServiceState() == ServiceState.ACTIVE;
  }

  @Override
  public List<RoleFSM> getRoles() {
    return serviceRoles;
  }

}
