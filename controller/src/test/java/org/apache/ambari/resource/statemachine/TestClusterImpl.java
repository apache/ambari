package org.apache.ambari.resource.statemachine;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.controller.Cluster;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.Guice;
/**
 * Test state transitions within ClusterImpl. Does not test interaction between
 * ClusterFSM and ServiceFSM
 *
 */
public class TestClusterImpl {
  

  ClusterImpl clusterImpl;
  ClusterEventType [] startEvents = {
      ClusterEventType.START, 
      ClusterEventType.START_SUCCESS,
  };
  
  ClusterStateFSM [] startStates = {
      ClusterStateFSM.STARTING,
      ClusterStateFSM.ACTIVE
  };
  
  ClusterState clsState;
  Cluster cluster;
  
  @BeforeMethod
  public void setup() throws IOException{
    Guice.createInjector(new TestModule());
    ClusterDefinition clusterDef = mock(ClusterDefinition.class);
    when(clusterDef.getEnabledServices()).thenReturn(new ArrayList<String>());
    cluster = mock(Cluster.class);
    clsState = new ClusterState();
    when(cluster.getClusterDefinition(anyInt())).thenReturn(clusterDef);
    when(cluster.getClusterState()).thenReturn(clsState);
    clusterImpl = new ClusterImpl(cluster, 1);
  }
  
  /**
   * Test INACTIVE to ACTIVE transition 
   * @throws Exception
   */
  @Test
  public void testInactiveToActive() throws Exception{
    doAnswer(new Answer<Void>(){
        public Void answer(InvocationOnMock invocation) throws Throwable {
            ClusterState cs = (ClusterState)invocation.getArguments()[0];
            assertTrue(cs.getState().equals(ClusterState.CLUSTER_STATE_ACTIVE));
            return null;
        }     
    }).when(cluster).updateClusterState(clsState);
    verifyTransitions(ClusterStateFSM.INACTIVE, startEvents, startStates);
  }

 
  /**
   * Test FAIL to ACTIVE transition
   * @throws Exception
   */
  @Test
  public void testFailToActive() throws Exception{
    doAnswer(new Answer<Void>(){
        public Void answer(InvocationOnMock invocation) throws Throwable {
            ClusterState cs = (ClusterState)invocation.getArguments()[0];
            assertTrue(cs.getState().equals(ClusterState.CLUSTER_STATE_ACTIVE));
            return null;
        }     
    }).when(cluster).updateClusterState(clsState);
    verifyTransitions(ClusterStateFSM.FAIL, startEvents, startStates);
  }
  
  ClusterEventType [] stopEvents = {
      ClusterEventType.STOP, 
      ClusterEventType.STOP_SUCCESS,
  };
  
  ClusterStateFSM [] stopStates = {
      ClusterStateFSM.STOPPING,
      ClusterStateFSM.INACTIVE
  };
  
  
  /**
   * Test ACTIVE to INACTIVE transition
   * @throws Exception
   */
  @Test
  public void testActivetoInactive() throws Exception{
    doAnswer(new Answer<Void>(){
        public Void answer(InvocationOnMock invocation) throws Throwable {
            ClusterState cs = (ClusterState)invocation.getArguments()[0];
            assertTrue(cs.getState().equals(ClusterState.CLUSTER_STATE_INACTIVE));
            return null;
        }     
    }).when(cluster).updateClusterState(clsState);
    verifyTransitions(ClusterStateFSM.ACTIVE, stopEvents, stopStates);
  }
  
  
  /**
   * Test FAIL to INACTIVE transition
   * @throws Exception
   */
  @Test
  public void testFailtoInactive() throws Exception{
    doAnswer(new Answer<Void>(){
        public Void answer(InvocationOnMock invocation) throws Throwable {
            ClusterState cs = (ClusterState)invocation.getArguments()[0];
            assertTrue(cs.getState().equals(ClusterState.CLUSTER_STATE_INACTIVE));
            return null;
        }     
    }).when(cluster).updateClusterState(clsState);
    verifyTransitions(ClusterStateFSM.FAIL, stopEvents, stopStates);
  }
  
  private void verifyTransitions(ClusterStateFSM startState, ClusterEventType[] ClusterEvents,
      ClusterStateFSM[] ClusterStateFSMs) {
    
    clusterImpl.getStateMachine().setCurrentState(startState);
    for(int i=0; i < ClusterEvents.length; i++){
      ClusterEventType rEvent = ClusterEvents[i];
      clusterImpl.handle(new ClusterEvent(rEvent, clusterImpl));
      ClusterStateFSM expectedRState = ClusterStateFSMs[i];
      assertEquals(clusterImpl.getStateMachine().getCurrentState(), expectedRState);
    }
    
  }
  
}
