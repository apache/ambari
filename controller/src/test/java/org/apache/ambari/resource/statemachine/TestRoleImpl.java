package org.apache.ambari.resource.statemachine;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.apache.ambari.common.state.InvalidStateTransitonException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;



public class TestRoleImpl {
  RoleImpl role;
  

  @BeforeMethod
  public void setup(){
    StateMachineInvoker.setAMBARIDispatcher(new NoOPDispatcher());
    ServiceFSM service = mock(ServiceFSM.class);  
    role = new RoleImpl(service, "role1");
   
  }
 
  @Test
  public void testStateTransitionsInactiveToActive() throws IOException {     
    //from inactive to active
    verifyTransitions(RoleState.INACTIVE, getEventsForActivate(), getStatesToActive());
  }

  @Test
  public void testStateTransitionsFailToActive() throws IOException {
    //from fail to active
    verifyTransitions(RoleState.FAIL, getEventsForActivate(), getStatesToActive());
  }
  
  @Test
  public void testStateTransitionsActiveToActive() throws IOException {
    //start event on active state throws exception
      verifyTransitionException(RoleState.ACTIVE, getEventsForActivate(), getStatesToActive());
  }  
  
  RoleEventType[] getEventsForActivate(){
    //events that would move role to activated
    RoleEventType[] roleEvents = {RoleEventType.START, RoleEventType.START_SUCCESS};
    return roleEvents;
  }
  
  RoleState[] getStatesToActive(){
    //states to active state
    RoleState[] roleStates = {RoleState.STARTING, RoleState.ACTIVE};
    return roleStates;
  }

  @Test
  public void testStateTransitionFailToInactive(){
    //from fail to inactive
    verifyTransitions(RoleState.FAIL, getEventsForInActivate(), getStatesToInActive());
  }
  
  @Test
  public void testStateTransitionActiveToInactive(){
    //from active to inactive
    verifyTransitions(RoleState.ACTIVE, getEventsForInActivate(), getStatesToInActive());
  }
  
  @Test
  public void testStateTransitionInactiveToInactive(){
    //inactive to inactive throw exception
    verifyTransitionException(RoleState.INACTIVE, getEventsForInActivate(), getStatesToInActive());
  } 
  
  
  RoleEventType[] getEventsForInActivate(){
    //events that would move role to activated
    RoleEventType[] roleEvents = {RoleEventType.STOP, RoleEventType.STOP_SUCCESS};
    return roleEvents;
  }
  
  RoleState[] getStatesToInActive(){
    //states to active state
    RoleState[] roleStates = {RoleState.STOPPING, RoleState.INACTIVE};
    return roleStates;
  }
  
  @Test
  public void testStateTransitionInactiveToFail(){
    RoleEventType[] startFailEvents = {RoleEventType.START, RoleEventType.START_FAILURE};
    RoleState[] roleStates = {RoleState.STARTING, RoleState.FAIL};
    //inactive to inactive throw exception
    verifyTransitions(RoleState.INACTIVE, startFailEvents, roleStates);
  } 

  @Test
  public void testStateTransitionActiveToFail(){
    RoleEventType[] startFailEvents = {RoleEventType.STOP, RoleEventType.STOP_FAILURE};
    RoleState[] roleStates = {RoleState.STOPPING, RoleState.FAIL};
    //inactive to inactive throw exception
    verifyTransitions(RoleState.ACTIVE, startFailEvents, roleStates);
  }
  
  private void verifyTransitionException(RoleState startState, RoleEventType[] roleEvents,
      RoleState[] roleStates){
    boolean foundException = false;
    try{
      verifyTransitions(startState, roleEvents, roleStates);
    }catch(InvalidStateTransitonException e){
      foundException = true;
    }
    assertTrue(foundException, "exception expected");
  }
  
  private void verifyTransitions(RoleState startState, RoleEventType[] roleEvents,
      RoleState[] roleStates) {
    role.getStateMachine().setCurrentState(startState);
    for(int i=0; i < roleEvents.length; i++){
      RoleEventType rEvent = roleEvents[i];
      role.handle(new RoleEvent(rEvent, role));
      RoleState expectedRState = roleStates[i];
      assertEquals(role.getRoleState(), expectedRState);
    }
    
  }



  

  

//  static class RoleImplTestModule extends AbstractModule{
//
//    @Override
//    protected void configure() {
//      bind(RoleFSM.class).to(RoleImpl.class);
//      bind(ServiceFSM.class).to(ServiceImpl.class);
//      bind(ClusterFSM.class).to(ClusterImpl.class);
//
//      install(new FactoryModuleBuilder()
//      .implement(Cluster.class,Cluster.class)
//      .build(ClusterFactory.class));
//
//    }
//
//  }

}
