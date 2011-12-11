package org.apache.ambari.resource.statemachine;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.Guice;

/**
 *
 */
public class TestServiceImpl {
  

  ServiceImpl service;
  ServiceEventType [] startEvents = {
      ServiceEventType.START, 
      ServiceEventType.PRESTART_SUCCESS,
      ServiceEventType.ROLE_START_SUCCESS,
      ServiceEventType.AVAILABLE_CHECK_SUCCESS
  };
  
  ServiceState [] startStates = {
      ServiceState.PRESTART,
      ServiceState.STARTING,
      ServiceState.STARTED,
      ServiceState.ACTIVE
  };
  
  @BeforeMethod
  public void setup() throws IOException{
    Guice.createInjector(new TestModule());
    String roles[] = {"role1"};
    ClusterImpl clusterImpl = mock(ClusterImpl.class);
    service = new ServiceImpl(roles, clusterImpl, "service1");  
  }
  
  /**
   * Test INACTIVE to ACTIVE transition with one role
   * @throws Exception
   */
  @Test
  public void testInactiveToActiveOneRole() throws Exception{
    verifyTransitions(ServiceState.INACTIVE, startEvents, startStates);
  }

  /**
   * Test INACTIVE to ACTIVE transition with two roles
   * @throws Exception
   */
  @Test
  public void testInactiveToActiveTwoRole() throws Exception{
    String roles[] = {"role1", "role2"};
    setRoles(roles);
    ServiceEventType [] events = {
        ServiceEventType.START, 
        ServiceEventType.PRESTART_SUCCESS,
        ServiceEventType.ROLE_START_SUCCESS, //1st role
        ServiceEventType.ROLE_START_SUCCESS, //2nd role
        ServiceEventType.AVAILABLE_CHECK_SUCCESS
    };
    ServiceState [] states = {
        ServiceState.PRESTART,
        ServiceState.STARTING,
        ServiceState.STARTING,
        ServiceState.STARTED,
        ServiceState.ACTIVE
    };
    
    verifyTransitions(ServiceState.INACTIVE, events, states);
  }
  
  /**
   * Test FAIL to ACTIVE transition with one roles
   * @throws Exception
   */
  @Test
  public void testFailToActiveOneRole() throws Exception{
    verifyTransitions(ServiceState.FAIL, startEvents, startStates);
  }
  
  /**
   * Test start failure scenario 
   * @throws Exception
   */
  @Test
  public void testInactiveToFail1() throws Exception{

    ServiceEventType[] events = truncateServiceEventArray(startEvents, 2,
        ServiceEventType.PRESTART_FAILURE);

    ServiceState[] states = getFailedStartSequence(2);
    verifyTransitions(ServiceState.INACTIVE, events, states);
    
  }
  

  private ServiceState[] getFailedStartSequence(int i) {
    return truncateServiceStateArray(startStates, i, ServiceState.FAIL);
  }

  /**
   * Test start failure scenario 
   * @throws Exception
   */
  @Test
  public void testInactiveToFail2() throws Exception{

    ServiceEventType[] events = truncateServiceEventArray(startEvents, 3,
        ServiceEventType.ROLE_START_FAILURE);
    ServiceState[] states = getFailedStartSequence(3);

    verifyTransitions(ServiceState.INACTIVE, events, states);
    
  }
  
  
  /**
   * Test start failure scenario 
   * @throws Exception
   */
  @Test
  public void testInactiveToFail3() throws Exception{

    ServiceEventType[] events = truncateServiceEventArray(startEvents, 4,
        ServiceEventType.AVAILABLE_CHECK_FAILURE);
    ServiceState[] states = getFailedStartSequence(4);

    verifyTransitions(ServiceState.INACTIVE, events, states);
    
  }
  
  
  /**
   * truncate startevents with lenght n and replace with state at position n-1 
   * with newState
   * @param startEvents
   * @param n
   * @param newState
   * @return
   */
  private ServiceEventType[] truncateServiceEventArray(
      ServiceEventType[] startEvents, int n, ServiceEventType newState) {
    return truncateArrayAndReplaceLastState(startEvents, n, newState, ServiceEventType.class);
  }

  private ServiceState[] truncateServiceStateArray(
      ServiceState[] startEvents2, int n, ServiceState prestartFailure) {
    return truncateArrayAndReplaceLastState(startStates, n, prestartFailure, ServiceState.class);
  }

  ServiceEventType [] stopEvents = {
      ServiceEventType.STOP, 
      ServiceEventType.ROLE_STOP_SUCCESS,
  };
  
  ServiceState [] stopStates = {
      ServiceState.STOPPING,
      ServiceState.INACTIVE
  };
   

  
  /**
   * Test active to inactive transition with one role
   * @throws Exception
   */
  @Test
  public void testActiveToInactiveOneRole() throws Exception{
    verifyTransitions(ServiceState.ACTIVE, stopEvents, stopStates);
  }
  
  /**
   * Test fail to inactive transition with one role
   * @throws Exception
   */
  @Test
  public void testFailToInactiveOneRole() throws Exception{
    verifyTransitions(ServiceState.FAIL, stopEvents, stopStates);
  }
  
  /**
   * Test failure in stop role
   * @throws Exception
   */
  @Test
  public void testActiveStopFailure() throws Exception{
    ServiceEventType [] stopEvents = {
        ServiceEventType.STOP, 
        ServiceEventType.ROLE_STOP_FAILURE,
    };
    
    ServiceState [] stopStates = {
        ServiceState.STOPPING,
        ServiceState.FAIL
    };
    verifyTransitions(ServiceState.ACTIVE, stopEvents, stopStates);
  }
  
  
  /**
   * Test active to inactive transition with two roles
   * @throws Exception
   */
  @Test
  public void testActiveToInactiveTwoRoles() throws Exception{
    String roles[] = {"role1", "role2"};
    ServiceEventType [] stopEvents = {
        ServiceEventType.STOP, 
        ServiceEventType.ROLE_STOP_SUCCESS,//1st role
        ServiceEventType.ROLE_STOP_SUCCESS,//2nd role
    };
    
    ServiceState [] stopStates = {
        ServiceState.STOPPING,
        ServiceState.STOPPING,
        ServiceState.INACTIVE
    };
    
    setRoles(roles);
    verifyTransitions(ServiceState.ACTIVE, stopEvents, stopStates);
  }
  
  
  /**
   * truncate inpArr array with length n and replace state at position n-1
   * with newState
   * @param inpArr
   * @param n
   * @param newState
   * @param tclass
   * @return
   */
  private<T>  T[] truncateArrayAndReplaceLastState(
      T[] inpArr,
      int n, T newState, Class<?> tclass) {
    T[] newEnumArr =  Arrays.copyOf(inpArr, n);
    newEnumArr[n-1] = newState;
    return newEnumArr;
  }
  
  /**
   * Call ServiceImpl.setRoles private function using reflection
   * @param roles
   * @throws Exception
   */
  private void setRoles(String[] roles) throws Exception {
    Method method = ServiceImpl.class.getDeclaredMethod("setRoles", new Class[]{String[].class});
    method.setAccessible(true);
    method.invoke(service, new Object[]{roles});

  }

  private void verifyTransitions(ServiceState startState, ServiceEventType[] serviceEvents,
      ServiceState[] serviceStates) {
    service.getStateMachine().setCurrentState(startState);
    for(int i=0; i < serviceEvents.length; i++){
      ServiceEventType rEvent = serviceEvents[i];
      service.handle(new ServiceEvent(rEvent, service));
      ServiceState expectedRState = serviceStates[i];
      assertEquals(service.getServiceState(), expectedRState);
    }
    
  }
  
}
