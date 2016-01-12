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
package org.apache.ambari.server.controller;

import static org.mockito.Matchers.any;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.ComponentResourceProviderTest;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.ServiceResourceProviderTest;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;

@RunWith(MockitoJUnitRunner.class)
public class AmbariCustomCommandExecutionHelperTest {
  private Injector injector;
  private AmbariManagementController controller;
  private AmbariMetaInfo ambariMetaInfo;
  private Clusters clusters;
  private TopologyManager topologyManager;


  private static final String REQUEST_CONTEXT_PROPERTY = "context";

  @Captor ArgumentCaptor<Request> requestCapture;
  @Mock ActionManager am;

  @Before
  public void setup() throws Exception {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule(){
      @Override
      protected void configure() {
        super.configure();
        bind(ActionManager.class).toInstance(am);
      }
    };
    injector = Guice.createInjector(module);


    injector.getInstance(GuiceJpaInitializer.class);
    controller = injector.getInstance(AmbariManagementController.class);
    clusters = injector.getInstance(Clusters.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);
  }
  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @SuppressWarnings("serial")
  @Test
  public void testRefreshQueueCustomCommand() throws Exception {
    createClusterFixture("HDP-2.0.6");

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put(REQUEST_CONTEXT_PROPERTY, "Refresh YARN Capacity Scheduler");
        put("command", "REFRESHQUEUES");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "REFRESHQUEUES",
        new HashMap<String, String>() {
          {
            put("forceRefreshConfigTags", "capacity-scheduler");
          }
        }, false);
    actionRequest.getResourceFilters().add(
        new RequestResourceFilter("YARN", "RESOURCEMANAGER", Collections.singletonList("c6401")));

    controller.createAction(actionRequest, requestProperties);

    Mockito.verify(am, Mockito.times(1)).sendActions(requestCapture.capture(),
        any(ExecuteActionRequest.class));

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    Assert.assertEquals(1, stage.getHosts().size());

    List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c6401");
    Assert.assertEquals(1, commands.size());

    ExecutionCommand command = commands.get(0).getExecutionCommand();

    Assert.assertNotNull(command.getForceRefreshConfigTags());
    Assert.assertEquals(1, command.getForceRefreshConfigTags().size());
    Assert.assertEquals("capacity-scheduler",
        command.getForceRefreshConfigTags().iterator().next());
  }

  @Test
  public void testHostsFilterHealthy() throws Exception {
    createClusterFixture("HDP-2.0.6");

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context" , "Restart all components for GANGLIA");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_name", "GANGLIA");
        put("operation_level/cluster_name", "c1");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest(
       "c1", "RESTART", null,
       Arrays.asList(
          new RequestResourceFilter("GANGLIA", "GANGLIA_SERVER", Collections.singletonList("c6401")),
          new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c6401")),
          new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c6402"))
       ),
       new RequestOperationLevel(Resource.Type.Service, "c1", "GANGLIA", null, null),
        new HashMap<String, String>() {
          {
          }
        },
       false);

    controller.createAction(actionRequest, requestProperties);

    //clusters.getHost("c6402").setState(HostState.HEARTBEAT_LOST);

    Mockito.verify(am, Mockito.times(1)).sendActions(requestCapture.capture(), any(ExecuteActionRequest.class));

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

     // Check if was generated command, one for each host
    Assert.assertEquals(2, stage.getHostRoleCommands().size());
  }

  @Test
  public void testHostsFilterUnhealthyHost() throws Exception {
    createClusterFixture("HDP-2.0.6");

    // Set custom status to host
    clusters.getHost("c6402").setState(HostState.HEARTBEAT_LOST);
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Restart all components for GANGLIA");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_name", "GANGLIA");
        put("operation_level/cluster_name", "c1");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "RESTART", null,
        Arrays.asList(
            new RequestResourceFilter("GANGLIA", "GANGLIA_SERVER",
                Collections.singletonList("c6401")),
            new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR",
                Collections.singletonList("c6401")),
            new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR",
                Collections.singletonList("c6402"))),
        new RequestOperationLevel(Resource.Type.Service, "c1", "GANGLIA", null, null),
        new HashMap<String, String>() {
          {
          }
        }, false);

    controller.createAction(actionRequest, requestProperties);

    Mockito.verify(am, Mockito.times(1)).sendActions(requestCapture.capture(),
        any(ExecuteActionRequest.class));

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    // Check if was generated command for one health host
    Assert.assertEquals(1, stage.getHostRoleCommands().size());
  }

  @Test
  public void testHostsFilterUnhealthyComponent() throws Exception {
    createClusterFixture("HDP-2.0.6");

    // Set custom status to host
    clusters.getCluster("c1").getService("GANGLIA").getServiceComponent(
        "GANGLIA_MONITOR").getServiceComponentHost("c6402").setState(State.UNKNOWN);

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Restart all components for GANGLIA");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_name", "GANGLIA");
        put("operation_level/cluster_name", "c1");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "RESTART", null,
        Arrays.asList(
            new RequestResourceFilter("GANGLIA", "GANGLIA_SERVER",
                Collections.singletonList("c6401")),
            new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR",
                Collections.singletonList("c6401")),
            new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR",
                Collections.singletonList("c6402"))),
        new RequestOperationLevel(Resource.Type.Host, "c1", "GANGLIA", null, null),
        new HashMap<String, String>() {
          {
          }
        }, false);

    controller.createAction(actionRequest, requestProperties);

    Mockito.verify(am, Mockito.times(1)).sendActions(requestCapture.capture(),
        any(ExecuteActionRequest.class));

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    // Check if was generated command for one health host
    Assert.assertEquals(1, stage.getHostRoleCommands().size());
  }

  /**
   * Tests that trying to run a service check when there are no available hosts
   * will throw an exception.
   */
  @Test(expected = AmbariException.class)
  public void testNoCandidateHostThrowsException() throws Exception {
    createClusterFixture("HDP-2.0.6");
    long clusterId = clusters.getCluster("c1").getClusterId();

    // put host into MM
    clusters.getHost("c6402").setMaintenanceState(clusterId, MaintenanceState.ON);

    // ensure that service check is added for ZOOKEEPER
    injector.getInstance(ActionMetadata.class).addServiceCheckAction("ZOOKEEPER");

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Service Check ZooKeeper");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_name", "ZOOKEEPER");
        put("operation_level/cluster_name", "c1");
      }
    };

    // create the service check on the host in MM
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
        "ZOOKEEPER_QUORUM_SERVICE_CHECK",
        null, Arrays.asList(new RequestResourceFilter("ZOOKEEPER", "ZOOKEEPER_CLIENT",
            Collections.singletonList("c6402"))),

        new RequestOperationLevel(Resource.Type.Service, "c1", "ZOOKEEPER", null, null),
        new HashMap<String, String>() {
          {
          }
        }, false);

    controller.createAction(actionRequest, requestProperties);
    Assert.fail(
        "Expected an exception since there are no hosts which can run the ZK service check");
  }

  /**
   * Tests that client-only services like TEZ are not run on hosts which are in
   * MM. The client-only service is a special path since a component is
   * typically not specified in the request.
   */
  @Test(expected = AmbariException.class)
  public void testServiceCheckMaintenanceModeWithMissingComponentName() throws Exception {
    createClusterFixture("HDP-2.0.6");
    long clusterId = clusters.getCluster("c1").getClusterId();

    // put host into MM
    clusters.getHost("c6402").setMaintenanceState(clusterId, MaintenanceState.ON);

    // ensure that service check is added for ZOOKEEPER
    injector.getInstance(ActionMetadata.class).addServiceCheckAction("ZOOKEEPER");

    // !!! use a null operation level to have us guess at the component
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Service Check ZooKeeper");
        put("operation_level/level", null);
        put("operation_level/service_name", "ZOOKEEPER");
        put("operation_level/cluster_name", "c1");
      }
    };

    // create the service check on the host in MM, passing in null for the
    // component name
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
        "ZOOKEEPER_QUORUM_SERVICE_CHECK", null, Arrays.asList(
            new RequestResourceFilter("ZOOKEEPER", null, Collections.singletonList("c6402"))),

        new RequestOperationLevel(Resource.Type.Service, "c1", "ZOOKEEPER", null, null),
        new HashMap<String, String>() {
          {
          }
        }, false);

    controller.createAction(actionRequest, requestProperties);
    Assert.fail(
        "Expected an exception since there are no hosts which can run the ZK service check");
  }

  @Test
  public void testIsTopologyRefreshRequired() throws Exception {
    AmbariCustomCommandExecutionHelper helper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    createClusterFixture("HDP-2.1.1");

    Assert.assertTrue(helper.isTopologyRefreshRequired("START", "c1", "HDFS"));
    Assert.assertTrue(helper.isTopologyRefreshRequired("RESTART", "c1", "HDFS"));
    Assert.assertFalse(helper.isTopologyRefreshRequired("STOP", "c1", "HDFS"));
  }

  private void createClusterFixture(String stackVersion) throws AmbariException {
    createCluster("c1", stackVersion);
    addHost("c6401","c1");
    addHost("c6402","c1");

    clusters.getCluster("c1");
    createService("c1", "YARN", null);
    createService("c1", "GANGLIA", null);
    createService("c1", "ZOOKEEPER", null);

    createServiceComponent("c1", "YARN","RESOURCEMANAGER", State.INIT);
    createServiceComponent("c1", "YARN", "NODEMANAGER", State.INIT);
    createServiceComponent("c1", "GANGLIA", "GANGLIA_SERVER", State.INIT);
    createServiceComponent("c1", "GANGLIA", "GANGLIA_MONITOR", State.INIT);
    createServiceComponent("c1", "ZOOKEEPER", "ZOOKEEPER_CLIENT", State.INIT);

    createServiceComponentHost("c1","YARN","RESOURCEMANAGER","c6401", null);
    createServiceComponentHost("c1","YARN","NODEMANAGER","c6401", null);
    createServiceComponentHost("c1","GANGLIA","GANGLIA_SERVER","c6401", State.INIT);
    createServiceComponentHost("c1","GANGLIA","GANGLIA_MONITOR","c6401", State.INIT);

    createServiceComponentHost("c1","YARN","NODEMANAGER","c6402", null);
    createServiceComponentHost("c1","GANGLIA","GANGLIA_MONITOR","c6402", State.INIT);
    createServiceComponentHost("c1", "ZOOKEEPER", "ZOOKEEPER_CLIENT", "c6402", State.INIT);

  }
  private void addHost(String hostname, String clusterName) throws AmbariException {
    clusters.addHost(hostname);
    setOsFamily(clusters.getHost(hostname), "redhat", "6.3");
    clusters.getHost(hostname).setState(HostState.HEALTHY);
    clusters.getHost(hostname).persist();
    if (null != clusterName) {
      clusters.mapHostToCluster(hostname, clusterName);
    }
  }
  private void setOsFamily(Host host, String osFamily, String osVersion) {
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", osFamily);
    hostAttributes.put("os_release_version", osVersion);

    host.setHostAttributes(hostAttributes);
  }

  private void createCluster(String clusterName, String stackVersion) throws AmbariException {
    ClusterRequest r = new ClusterRequest(null, clusterName, State.INSTALLED.name(),
        SecurityType.NONE, stackVersion, null);
    controller.createCluster(r);
  }

  private void createService(String clusterName,
      String serviceName, State desiredState) throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceRequest r1 = new ServiceRequest(clusterName, serviceName, dStateStr);
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r1);

    ServiceResourceProviderTest.createServices(controller, requests);
  }

  private void createServiceComponent(String clusterName,
      String serviceName, String componentName, State desiredState)
          throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentRequest r = new ServiceComponentRequest(clusterName,
        serviceName, componentName, dStateStr);
    Set<ServiceComponentRequest> requests =
        new HashSet<ServiceComponentRequest>();
    requests.add(r);
    ComponentResourceProviderTest.createComponents(controller, requests);
  }

  private void createServiceComponentHost(String clusterName, String serviceName, String componentName, String hostname, State desiredState) throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName,
        serviceName, componentName, hostname, dStateStr);
    Set<ServiceComponentHostRequest> requests =
        new HashSet<ServiceComponentHostRequest>();
    requests.add(r);
    controller.createHostComponents(requests);
  }

}
