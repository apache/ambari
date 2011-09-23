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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.ambari.common.state.MultipleArcTransition;
import org.apache.ambari.common.state.SingleArcTransition;
import org.apache.ambari.common.state.StateMachine;
import org.apache.ambari.common.state.StateMachineFactory;
import org.apache.ambari.event.EventHandler;

public class ClusterImpl implements Cluster, EventHandler<ClusterEvent> {

  /* The state machine for the cluster looks like:
   * INACTIVE --S_START--> STARTING --S_START_SUCCESS from all services--> ACTIVE
   *                                --S_START_FAILURE from any service--> FAIL
   * ACTIVE --S_STOP--> STOPPING --S_STOP_SUCCESS from all services--> INACTIVE
   *                             --S_STOP_FAILURE from any service--> UNCLEAN_STOP
   * FAIL --S_STOP--> STOPPING --S_STOP_SUCCESS--> INACTIVE
   *                           --S_STOP_FAILURE--> UNCLEAN_STOP
   * INACTIVE --S_RELEASE_NODES--> ATTIC
   * ATTIC --S_ADD_NODES--> INACTIVE
   */

  private static final StateMachineFactory
  <ClusterImpl,ClusterState,ClusterEventType,ClusterEvent>
  stateMachineFactory = 
  new StateMachineFactory
  <ClusterImpl,ClusterState,ClusterEventType,ClusterEvent>(ClusterState.INACTIVE)
  .addTransition(ClusterState.INACTIVE, ClusterState.STARTING, 
      ClusterEventType.S_START, new StartClusterTransition())
  .addTransition(ClusterState.STARTING, EnumSet.of(ClusterState.ACTIVE, 
      ClusterState.STARTING), ClusterEventType.S_START_SUCCESS, 
      new ServiceStartedTransition())
  .addTransition(ClusterState.STARTING, ClusterState.FAIL, 
      ClusterEventType.S_START_FAILURE)
  .addTransition(ClusterState.ACTIVE, ClusterState.STOPPING, 
      ClusterEventType.S_STOP)
  .addTransition(ClusterState.STOPPING, ClusterState.INACTIVE, 
      ClusterEventType.S_STOP_SUCCESS)
  .addTransition(ClusterState.STOPPING, ClusterState.UNCLEAN_STOP, 
      ClusterEventType.S_STOP_FAILURE)
  .addTransition(ClusterState.FAIL, ClusterState.STOPPING, 
      ClusterEventType.S_STOP)
  .addTransition(ClusterState.STOPPING, ClusterState.INACTIVE, 
      ClusterEventType.S_STOP_SUCCESS)
  .addTransition(ClusterState.STOPPING, ClusterState.UNCLEAN_STOP, 
      ClusterEventType.S_STOP_FAILURE)
  .addTransition(ClusterState.INACTIVE, ClusterState.ATTIC, 
      ClusterEventType.S_RELEASE_NODES)
  .addTransition(ClusterState.ATTIC, ClusterState.INACTIVE, 
      ClusterEventType.S_ADD_NODES)
  .installTopology();
  
  private List<Service> services;
  private Map<String, Set<String>> roleToNodes;
  private StateMachine<ClusterState, ClusterEventType, ClusterEvent> 
          stateMachine;
  private String clusterName;
  private int numServicesStarted;
  private int totalEnabledServices;
  private Lock readLock;
  private Lock writeLock;
  private short roleCount; 
    
  public ClusterImpl(String name) {
    this.clusterName = name;
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    this.readLock = readWriteLock.readLock();
    this.writeLock = readWriteLock.writeLock();
    this.stateMachine = stateMachineFactory.make(this);
  }

  @Override
  public String getClusterName() {
    return clusterName;
  }

  @Override
  public ClusterState getClusterState() {
    return stateMachine.getCurrentState();
  }
  
  @Override
  public void handle(ClusterEvent event) {
    getStateMachine().doTransition(event.getType(), event);
  }

  @Override
  public List<Service> getServices() {
    return services;
  }
  
  public StateMachine getStateMachine() {
    return stateMachine;
  }
  
  @Override  
  public void addServices(List<Service> services) {
    this.services.addAll(services);
  }
  
  public Service getNextService() {
    if (++roleCount <= services.size()) {
      return services.get(roleCount - 1);
    }
    return null;
  }
  
  private void incrStartedServiceCount() {
    try {
      writeLock.lock();
      numServicesStarted++;
    } finally {
      writeLock.unlock();
    }
  }
  
  private int getStartedServiceCount() {
    try {
      readLock.lock();
      return numServicesStarted;
    } finally {
      readLock.unlock();
    }
  }
  
  private int getTotalServiceCount() {
    return totalEnabledServices;
  }
  
  static class StartClusterTransition implements 
  SingleArcTransition<ClusterImpl, ClusterEvent>  {

    @Override
    public void transition(ClusterImpl operand, ClusterEvent event) {
      Service service = operand.getNextService();
      if (service != null) {
              //start the first service (plugin)
        StateMachineInvoker.getAMBARIEventHandler().handle(
            new ServiceEvent(ServiceEventType.S_START, service));
      }
    }
    
  }
  
  static class ServiceStartedTransition implements 
  MultipleArcTransition<ClusterImpl, ClusterEvent, ClusterState>  {
    @Override
    public ClusterState transition(ClusterImpl operand, ClusterEvent event) {
      //check whether all services started, and if not remain in the STARTING
      //state, else move to the ACTIVE state
      if (operand.getStartedServiceCount() == operand.getTotalServiceCount()) {
        return ClusterState.ACTIVE;
      }
      operand.incrStartedServiceCount();
      //TODO: start the next service (plugin)
      return ClusterState.STARTING;
    }
    
  }

  @Override
  public Map<String, String> getServiceStates() {
    Map<String, String> serviceStateMap = new HashMap<String,String>();
    for (Service s : services) {
      serviceStateMap.put(s.getServiceName(), s.getServiceState().toString());
    }
    return serviceStateMap;
  }

  @Override
  public void activate() {
    StateMachineInvoker.getAMBARIEventHandler().handle(
        new ClusterEvent(ClusterEventType.S_START, this));
  }

  @Override
  public void deactivate() {
    StateMachineInvoker.getAMBARIEventHandler().handle(
        new ClusterEvent(ClusterEventType.S_STOP, this));
  }

  @Override
  public void terminate() {
    StateMachineInvoker.getAMBARIEventHandler().handle(
        new ClusterEvent(ClusterEventType.S_RELEASE_NODES, this));    
  }

}
