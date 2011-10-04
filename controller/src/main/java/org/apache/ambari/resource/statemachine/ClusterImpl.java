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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
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
import org.apache.ambari.components.ClusterContext;
import org.apache.ambari.event.EventHandler;

public class ClusterImpl implements Cluster, EventHandler<ClusterEvent> {

  /* The state machine for the cluster looks like:
   * INACTIVE --START--> STARTING --START_SUCCESS from all services--> ACTIVE
   *                                --START_FAILURE from any service--> FAIL
   * ACTIVE --STOP--> STOPPING --STOP_SUCCESS from all services--> INACTIVE
   *                             --STOP_FAILURE from any service--> UNCLEAN_STOP
   * FAIL --STOP--> STOPPING --STOP_SUCCESS--> INACTIVE
   *                           --STOP_FAILURE--> UNCLEAN_STOP
   * INACTIVE --RELEASE_NODES--> ATTIC
   * ATTIC --ADD_NODES--> INACTIVE
   */

  private static final StateMachineFactory
  <ClusterImpl,ClusterState,ClusterEventType,ClusterEvent>
  stateMachineFactory = 
  new StateMachineFactory
  <ClusterImpl,ClusterState,ClusterEventType,ClusterEvent>(ClusterState.INACTIVE)
  .addTransition(ClusterState.INACTIVE, ClusterState.STARTING, 
      ClusterEventType.START, new StartClusterTransition())
  .addTransition(ClusterState.STARTING, EnumSet.of(ClusterState.ACTIVE, 
      ClusterState.STARTING), ClusterEventType.START_SUCCESS, 
      new ServiceStartedTransition())
  .addTransition(ClusterState.STARTING, ClusterState.FAIL, 
      ClusterEventType.START_FAILURE)
  .addTransition(ClusterState.ACTIVE, ClusterState.STOPPING, 
      ClusterEventType.STOP)
  .addTransition(ClusterState.STOPPING, ClusterState.INACTIVE, 
      ClusterEventType.STOP_SUCCESS)
  .addTransition(ClusterState.STOPPING, ClusterState.UNCLEAN_STOP, 
      ClusterEventType.STOP_FAILURE)
  .addTransition(ClusterState.FAIL, ClusterState.STOPPING, 
      ClusterEventType.STOP)
  .addTransition(ClusterState.STOPPING, ClusterState.INACTIVE, 
      ClusterEventType.STOP_SUCCESS)
  .addTransition(ClusterState.STOPPING, ClusterState.UNCLEAN_STOP, 
      ClusterEventType.STOP_FAILURE)
  .addTransition(ClusterState.INACTIVE, ClusterState.ATTIC, 
      ClusterEventType.RELEASE_NODES)
  .addTransition(ClusterState.ATTIC, ClusterState.INACTIVE, 
      ClusterEventType.ADD_NODES)
  .installTopology();
  
  private List<Service> services;
  private StateMachine<ClusterState, ClusterEventType, ClusterEvent> 
          stateMachine;
  private int totalEnabledServices;
  private Lock readLock;
  private Lock writeLock;
  private String clusterName;
  private Iterator<Service> iterator;
    
  public ClusterImpl(String name, List<Service> services) {
    this.clusterName = name;
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    this.readLock = readWriteLock.readLock();
    this.writeLock = readWriteLock.writeLock();
    this.stateMachine = stateMachineFactory.make(this);
    this.services = services;
  }
  
  public ClusterImpl(String name) {
    this(name, new ArrayList<Service>());
  }

  @Override
  public ClusterState getState() {
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
    //The services start in the order they appear in the list
    this.services.addAll(services);
  }
  
  private Service getFirstService() {
    //this call should reset the iterator
    iterator = services.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }
    return null;
  }
  
  private Service getNextService() {
    if (iterator.hasNext()) {
      return iterator.next();
    }
    return null;
  }
  
  private int getTotalServiceCount() {
    return totalEnabledServices;
  }
  
  static class StartClusterTransition implements 
  SingleArcTransition<ClusterImpl, ClusterEvent>  {

    @Override
    public void transition(ClusterImpl operand, ClusterEvent event) {
      Service service = operand.getFirstService();
      if (service != null) {
        StateMachineInvoker.getAMBARIEventHandler().handle(
            new ServiceEvent(ServiceEventType.START, service));
      }
    }
    
  }
  
  static class ServiceStartedTransition implements 
  MultipleArcTransition<ClusterImpl, ClusterEvent, ClusterState>  {
    @Override
    public ClusterState transition(ClusterImpl operand, ClusterEvent event) {
      //check whether all services started, and if not remain in the STARTING
      //state, else move to the ACTIVE state
      Service service = operand.getNextService();
      if (service != null) {
        StateMachineInvoker.getAMBARIEventHandler().handle(new ServiceEvent(
            ServiceEventType.START, service));
        return ClusterState.STARTING;
      }
      return ClusterState.ACTIVE;
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
        new ClusterEvent(ClusterEventType.START, this));
  }

  @Override
  public void deactivate() {
    StateMachineInvoker.getAMBARIEventHandler().handle(
        new ClusterEvent(ClusterEventType.STOP, this));
  }

  @Override
  public void terminate() {
    StateMachineInvoker.getAMBARIEventHandler().handle(
        new ClusterEvent(ClusterEventType.RELEASE_NODES, this));    
  }

  @Override
  public String getClusterName() {
    return clusterName;
  }
}
