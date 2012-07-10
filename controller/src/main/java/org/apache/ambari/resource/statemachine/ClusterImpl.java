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
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.state.MultipleArcTransition;
import org.apache.ambari.common.state.SingleArcTransition;
import org.apache.ambari.common.state.StateMachine;
import org.apache.ambari.common.state.StateMachineFactory;
import org.apache.ambari.components.ComponentPlugin;
import org.apache.ambari.controller.Cluster;
import org.apache.ambari.controller.Util;
import org.apache.ambari.event.EventHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;

public class ClusterImpl implements ClusterFSM, EventHandler<ClusterEvent> {

  /* The state machine for the cluster looks like:
   * INACTIVE or FAIL --START--> STARTING --START_SUCCESS from all services--> ACTIVE
   *                                --START_FAILURE from any service--> FAIL
   * ACTIVE or FAIL --STOP--> STOPPING --STOP_SUCCESS from all services--> INACTIVE
   *                             --STOP_FAILURE from any service--> FAIL
   */

  private static final StateMachineFactory
  <ClusterImpl,ClusterStateFSM,ClusterEventType,ClusterEvent> stateMachineFactory 
          = new StateMachineFactory<ClusterImpl,ClusterStateFSM,ClusterEventType,
          ClusterEvent>(ClusterStateFSM.INACTIVE)
  
  .addTransition(ClusterStateFSM.INACTIVE, ClusterStateFSM.STARTING, 
      ClusterEventType.START, new StartClusterTransition())

  .addTransition(ClusterStateFSM.FAIL, ClusterStateFSM.STARTING, 
      ClusterEventType.START, new StartClusterTransition())
      
  .addTransition(ClusterStateFSM.STARTING, EnumSet.of(ClusterStateFSM.ACTIVE, 
      ClusterStateFSM.STARTING), ClusterEventType.START_SUCCESS, 
      new ServiceStartedTransition())
      
  .addTransition(ClusterStateFSM.STARTING, ClusterStateFSM.FAIL, 
      ClusterEventType.START_FAILURE)
      
  .addTransition(ClusterStateFSM.ACTIVE, ClusterStateFSM.STOPPING, 
      ClusterEventType.STOP, new StopClusterTransition())
      
  .addTransition(ClusterStateFSM.FAIL, ClusterStateFSM.STOPPING, 
      ClusterEventType.STOP, new StopClusterTransition())
      
  .addTransition(ClusterStateFSM.STOPPING, EnumSet.of(ClusterStateFSM.INACTIVE,
      ClusterStateFSM.STOPPING), ClusterEventType.STOP_SUCCESS,
      new ServiceStoppedTransition())
      
  .addTransition(ClusterStateFSM.STOPPING, ClusterStateFSM.FAIL, 
      ClusterEventType.STOP_FAILURE)
      
  .addTransition(ClusterStateFSM.INACTIVE, ClusterStateFSM.INACTIVE, 
      ClusterEventType.STOP_SUCCESS)
      
  .installTopology();
  
  private List<ServiceFSM> services;
  private Cluster cls;
  private StateMachine<ClusterStateFSM, ClusterEventType, ClusterEvent> 
          stateMachine;
  private Lock readLock;
  private Lock writeLock;
  private Iterator<ServiceFSM> iterator;
  private static Log LOG = LogFactory.getLog(ClusterImpl.class);
  private static StateMachineInvokerInterface stateMachineInvoker;
  @Inject
  public static void setInvoker(StateMachineInvokerInterface sm) {
    stateMachineInvoker = sm;
  }
  public ClusterImpl(Cluster cluster, int revision) throws IOException {
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    this.readLock = readWriteLock.readLock();
    this.writeLock = readWriteLock.writeLock();
    this.stateMachine = stateMachineFactory.make(this);
    List<ServiceFSM> serviceImpls = new ArrayList<ServiceFSM>();
    for (String service :
      cluster.getClusterDefinition(revision).getEnabledServices()) {
      if(hasActiveRoles(cluster, service)){
        ServiceImpl serviceImpl = new ServiceImpl(
            cluster.getComponentDefinition(service).getActiveRoles(), 
            this, 
            service);
        
        serviceImpls.add(serviceImpl);
      }
    }
    this.cls = cluster;
    this.services = serviceImpls;
  }
  
  private static boolean hasActiveRoles(Cluster cluster, String serviceName)
      throws IOException {
    ComponentPlugin plugin = cluster.getComponentDefinition(serviceName);
    String[] roles = plugin.getActiveRoles();
    return roles.length > 0;
  }
  
  public ClusterStateFSM getState() {
    return stateMachine.getCurrentState();
  }
  
  @Override
  public void handle(ClusterEvent event) {
    getStateMachine().doTransition(event.getType(), event);
  }

  @Override
  public List<ServiceFSM> getServices() {
    return services;
  }
  
  public StateMachine getStateMachine() {
    return stateMachine;
  }
  
  private ServiceFSM getFirstService() {
    //this call should reset the iterator
    iterator = services.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }
    return null;
  }
  
  private ServiceFSM getNextService() {
    if (iterator.hasNext()) {
      return iterator.next();
    }
    return null;
  }
  
  @Override
  public String getClusterState() {
    return getState().toString();
  }
  
  static class StartClusterTransition implements 
  SingleArcTransition<ClusterImpl, ClusterEvent>  {

    @Override
    public void transition(ClusterImpl operand, ClusterEvent event) {
      ServiceFSM service = operand.getFirstService();
      if (service != null) {
        stateMachineInvoker.getAMBARIEventHandler().handle(
            new ServiceEvent(ServiceEventType.START, service));
      }
    }
    
  }
  
  static class StopClusterTransition implements
  SingleArcTransition<ClusterImpl, ClusterEvent>  {
    
    @Override
    public void transition(ClusterImpl operand, ClusterEvent event) {
      //TODO: do it in the reverse order of startup
      ServiceFSM service = operand.getFirstService();
      if (service != null) {
        stateMachineInvoker.getAMBARIEventHandler().handle(
            new ServiceEvent(ServiceEventType.STOP, service));
      }
    }
  }
  
  static class ServiceStoppedTransition implements
  MultipleArcTransition<ClusterImpl, ClusterEvent, ClusterStateFSM> {

    @Override
    public ClusterStateFSM transition(ClusterImpl operand, ClusterEvent event) {
      //check whether all services stopped, and if not remain in the STOPPING
      //state, else move to the INACTIVE state
      ServiceFSM service = operand.getNextService();
      if (service != null) {
        stateMachineInvoker.getAMBARIEventHandler().handle(new ServiceEvent(
            ServiceEventType.STOP, service));
        return ClusterStateFSM.STOPPING;
      }
      operand.updateClusterState(ClusterState.CLUSTER_STATE_INACTIVE);
      return ClusterStateFSM.INACTIVE;
    }
    
  }
  
  static class ServiceStartedTransition implements 
  MultipleArcTransition<ClusterImpl, ClusterEvent, ClusterStateFSM>  {
    @Override
    public ClusterStateFSM transition(ClusterImpl operand, ClusterEvent event){
      //check whether all services started, and if not remain in the STARTING
      //state, else move to the ACTIVE state
      ServiceFSM service = operand.getNextService();
      if (service != null) {
        stateMachineInvoker.getAMBARIEventHandler().handle(new ServiceEvent(
            ServiceEventType.START, service));
        return ClusterStateFSM.STARTING;
      }
      operand.updateClusterState(ClusterState.CLUSTER_STATE_ACTIVE);
      return ClusterStateFSM.ACTIVE;
    }
    
  }

  @Override
  public Map<String, String> getServiceStates() {
    Map<String, String> serviceStateMap = new HashMap<String,String>();
    for (ServiceFSM s : services) {
      serviceStateMap.put(s.getServiceName(), s.getServiceState().toString());
    }
    return serviceStateMap;
  }

  @Override
  public void activate() {
    stateMachineInvoker.getAMBARIEventHandler().handle(
        new ClusterEvent(ClusterEventType.START, this));
  }

  @Override
  public void deactivate() {
    stateMachineInvoker.getAMBARIEventHandler().handle(
        new ClusterEvent(ClusterEventType.STOP, this));
  }
  
  private void updateClusterState (String x) {
      
      try {
        ClusterState cs = this.cls.getClusterState();
        cs.setLastUpdateTime(Util.getXMLGregorianCalendar(new Date()));
        cs.setState(x);
        this.cls.updateClusterState(cs);
      } catch (IOException e) {
        /*
         * TODO: Should we bring down the controller? 
         */
        System.out.println ("Unbale to update/persist the cluster state change. Shutting down the controller!");
        e.printStackTrace();
        System.exit(-1);  
      }
  }
}
