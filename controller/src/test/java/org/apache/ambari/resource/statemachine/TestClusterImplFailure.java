package org.apache.ambari.resource.statemachine;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.components.ComponentPlugin;
import org.apache.ambari.configuration.Configuration;
import org.apache.ambari.controller.Cluster;
import org.apache.ambari.controller.ControllerModule;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.Guice;

/**
 *Test state machine handling of failure scenarios
 */
public class TestClusterImplFailure {
  
 
  
  private ClusterImpl clusterImpl;
  private ServiceImpl service;
  private RoleImpl role;

  @BeforeMethod
  public void setup() throws IOException{
    Guice.createInjector(new TestModule());
    ClusterDefinition clusterDef = mock(ClusterDefinition.class);
    List<String> services = new ArrayList<String>();
    services.add("service1");
    when(clusterDef.getEnabledServices()).thenReturn(services);
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterDefinition(anyInt())).thenReturn(clusterDef);

    String [] roles = {"role1"};

    ComponentPlugin compDef = mock(ComponentPlugin.class);
    when(compDef.getActiveRoles()).thenReturn(roles);
    when(cluster.getComponentDefinition(anyString())).thenReturn(compDef);
    clusterImpl = new ClusterImpl(cluster, 1);
    service = (ServiceImpl)clusterImpl.getServices().get(0);
    role = (RoleImpl)service.getRoles().get(0);
  
  }
  
  private static class TestConfiguration extends Configuration {
    TestConfiguration() {
      super(getProperties());
    }
    private static Properties getProperties() {
      Properties props = new Properties();
      props.setProperty("data.store", "test:/");
      return props;
    }
  }
  private static class TestModule extends ControllerModule {
    @Override
    protected void configure() {
      super.configure();
      bind(StateMachineInvokerInterface.class)
      .to(StateMachineInvokerSync.class);
      bind(Configuration.class).to(TestConfiguration.class);
    }
  }
  
  
  /**
   * cluster should go into fail state if PRESTART fails
   */
  @Test
  public void testPrestartFail() {
    
    checkStates(ClusterStateFSM.INACTIVE, ServiceState.INACTIVE, RoleState.INACTIVE);

    clusterImpl.activate();
    checkStates(ClusterStateFSM.STARTING, ServiceState.PRESTART, RoleState.INACTIVE);
    
    service.handle(new ServiceEvent(ServiceEventType.PRESTART_FAILURE, service));
    checkStates(ClusterStateFSM.FAIL, ServiceState.FAIL, RoleState.INACTIVE);
  }

  private void checkStates(ClusterStateFSM clusterState, ServiceState serviceState,
      RoleState roleState) {
    assertEquals(clusterImpl.getState(), clusterState);
    assertEquals(service.getServiceState(), serviceState);
    assertEquals(role.getRoleState(), roleState);    
  }

  /**
   * cluster should go into fail state if role start fails
   */
  @Test
  public void testRoleStartFail() {
    checkStates(ClusterStateFSM.INACTIVE, ServiceState.INACTIVE, RoleState.INACTIVE);
    
    clusterImpl.activate();
    checkStates(ClusterStateFSM.STARTING, ServiceState.PRESTART, RoleState.INACTIVE);
    
    service.handle(new ServiceEvent(ServiceEventType.PRESTART_SUCCESS, service));
    checkStates(ClusterStateFSM.STARTING, ServiceState.STARTING, RoleState.STARTING);

    role.handle(new RoleEvent(RoleEventType.START_FAILURE, role));
    checkStates(ClusterStateFSM.FAIL, ServiceState.FAIL, RoleState.FAIL);
    
  }
  
  
  /**
   * cluster should go into fail state if service availability check fails
   */
  @Test
  public void testServiceAvailFailure() {
    
    setStates(ClusterStateFSM.STARTING, ServiceState.STARTED, RoleState.ACTIVE);
    
    service.handle(new ServiceEvent(ServiceEventType.AVAILABLE_CHECK_FAILURE, service));
    checkStates(ClusterStateFSM.FAIL, ServiceState.FAIL, RoleState.ACTIVE);
    
  }
  
  /**
   * cluster should go into fail state if service availability check fails
   */
  @Test
  public void testRoleStopFailure() {
    
    setStates(ClusterStateFSM.STOPPING, ServiceState.STOPPING, RoleState.STOPPING);
    role.handle(new RoleEvent(RoleEventType.STOP_FAILURE, role));
    
    checkStates(ClusterStateFSM.FAIL, ServiceState.FAIL, RoleState.FAIL);
    
  }
  
  private void setStates(ClusterStateFSM clusterState, ServiceState serviceState,
      RoleState roleState) {
   clusterImpl.getStateMachine().setCurrentState(clusterState);
   service.getStateMachine().setCurrentState(serviceState);
   role.getStateMachine().setCurrentState(roleState);
    
  }

  
}
