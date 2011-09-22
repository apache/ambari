package org.apache.ambari.resource.statemachine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.common.state.MultipleArcTransition;
import org.apache.ambari.common.state.SingleArcTransition;
import org.apache.ambari.common.state.StateMachine;
import org.apache.ambari.common.state.StateMachineFactory;
import org.apache.ambari.event.EventHandler;

public class ServiceImpl implements Service, EventHandler<ServiceEvent> {

  private ServiceState myState;
  private Cluster cluster;
  
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
         .addTransition(ServiceState.INACTIVE, ServiceState.STARTING, ServiceEventType.S_START, new StartServiceTransition())
         .addTransition(ServiceState.STARTING, EnumSet.of(ServiceState.ACTIVE, ServiceState.STARTING), ServiceEventType.S_ROLE_STARTED,
             new RoleStartedTransition())
         .addTransition(ServiceState.STARTING, ServiceState.FAIL, ServiceEventType.S_START_FAILURE)
         .addTransition(ServiceState.ACTIVE, ServiceState.STOPPING, ServiceEventType.S_STOP)
         .addTransition(ServiceState.STOPPING, ServiceState.INACTIVE, ServiceEventType.S_STOP_SUCCESS)
         .addTransition(ServiceState.STOPPING, ServiceState.UNCLEAN_STOP, ServiceEventType.S_STOP_FAILURE)
         .addTransition(ServiceState.FAIL, ServiceState.STOPPING, ServiceEventType.S_STOP)
         .addTransition(ServiceState.STOPPING, ServiceState.INACTIVE, ServiceEventType.S_STOP_SUCCESS)
         .addTransition(ServiceState.STOPPING, ServiceState.UNCLEAN_STOP, ServiceEventType.S_STOP_FAILURE)
         .installTopology();
  
  private final StateMachine<ServiceState, ServiceEventType, ServiceEvent>
      stateMachine;
  private final List<Role> serviceRoles = new ArrayList<Role>();
  private final String serviceName;
  private short roleCount;
  
  public ServiceImpl(Cluster cluster, String serviceName) {
    this.cluster = cluster;
    this.serviceName = serviceName;
    this.myState = ServiceState.INACTIVE;
    stateMachine = stateMachineFactory.make(this);
    //load plugin and get the roles and create them
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
                            new RoleEvent(RoleEventType.S_START, firstRole));
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
            RoleEventType.S_START, role));
        return ServiceState.STARTING;
      } else {
        return ServiceState.ACTIVE;
      }
    }
  }

  @Override
  public void activate() {
    StateMachineInvoker.getAMBARIEventHandler().handle(
              new ServiceEvent(ServiceEventType.S_START, this));
  }

  @Override
  public void deactivate() {
    StateMachineInvoker.getAMBARIEventHandler().handle(
              new ServiceEvent(ServiceEventType.S_STOP, this));
  }
}
