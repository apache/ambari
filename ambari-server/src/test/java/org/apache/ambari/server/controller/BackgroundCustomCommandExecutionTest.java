/*
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

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.ComponentResourceProviderTest;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.ServiceGroupResourceProviderTest;
import org.apache.ambari.server.controller.internal.ServiceResourceProviderTest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
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
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

@RunWith(MockitoJUnitRunner.class)
public class BackgroundCustomCommandExecutionTest {
  private Injector injector;
  private AmbariManagementController controller;
  private Clusters clusters;

  private static final String REQUEST_CONTEXT_PROPERTY = "context";

  @Captor ArgumentCaptor<Request> requestCapture;
  @Mock ActionManager am;

  private static final String STACK_VERSION = "2.0.6";
  private static final String REPO_VERSION = "2.0.6-1234";
  private static final StackId STACK_ID = new StackId("HDP", STACK_VERSION);
  private RepositoryVersionEntity m_repositoryVersion;

  @Before
  public void setup() throws Exception {
    Configuration configuration;
    TopologyManager topologyManager;

    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule(){


      @Override
      protected void configure() {
        getProperties().put(Configuration.CUSTOM_ACTION_DEFINITION.getKey(), "src/main/resources/custom_action_definitions");
        super.configure();
        bind(ActionManager.class).toInstance(am);
      }
    };
    injector = Guice.createInjector(module);


    injector.getInstance(GuiceJpaInitializer.class);
    controller = injector.getInstance(AmbariManagementController.class);
    clusters = injector.getInstance(Clusters.class);
    configuration = injector.getInstance(Configuration.class);
    topologyManager = injector.getInstance(TopologyManager.class);
    OrmTestHelper ormTestHelper = injector.getInstance(OrmTestHelper.class);

    Assert.assertEquals("src/main/resources/custom_action_definitions", configuration.getCustomActionDefinitionPath());

    StageUtils.setTopologyManager(topologyManager);
    StageUtils.setConfiguration(configuration);

    // Set the authenticated user
    // TODO: remove this or replace the authenticated user to test authorization rules
    // Set the authenticated user
    // TODO: remove this or replace the authenticated user to test authorization rules
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    m_repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(STACK_ID, REPO_VERSION);
    Assert.assertNotNull(m_repositoryVersion);
  }
  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @SuppressWarnings("serial")
  @Test
  public void testRebalanceHdfsCustomCommand() {
    try {
      createClusterFixture();

      Map<String, String> requestProperties = new HashMap<String, String>() {
        {
          put(REQUEST_CONTEXT_PROPERTY, "Refresh YARN Capacity Scheduler");
          put("command", "REBALANCEHDFS");
          put("namenode" , "{\"threshold\":13}");//case is important here
        }
      };

      ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
          "REBALANCEHDFS", new HashMap<>(), false);
      actionRequest.getResourceFilters().add(new RequestResourceFilter("CORE", "HDFS", "NAMENODE",Collections.singletonList("c6401")));

      controller.createAction(actionRequest, requestProperties);

      Mockito.verify(am, Mockito.times(1)).sendActions(requestCapture.capture(), any(ExecuteActionRequest.class));

      Request request = requestCapture.getValue();
      Assert.assertNotNull(request);
      Assert.assertNotNull(request.getStages());
      Assert.assertEquals(1, request.getStages().size());
      Stage stage = request.getStages().iterator().next();

      System.out.println(stage);

      Assert.assertEquals(1, stage.getHosts().size());

      List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c6401");
      Assert.assertEquals(1, commands.size());

      ExecutionCommand command = commands.get(0).getExecutionCommand();

      Assert.assertEquals(AgentCommandType.BACKGROUND_EXECUTION_COMMAND, command.getCommandType());
      Assert.assertEquals("{\"threshold\":13}", command.getCommandParams().get("namenode"));

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  private void createClusterFixture() throws AmbariException, AuthorizationException {
    String clusterName = "c1";
    createCluster(clusterName);
    addHost("c6401", clusterName);
    addHost("c6402", clusterName);

    clusters.getCluster(clusterName);
    String serviceGroupName = "CORE";
    ServiceGroupResourceProviderTest.createServiceGroup(controller, clusterName, serviceGroupName, STACK_ID.getStackId());
    createService(clusterName, serviceGroupName, "HDFS", null);

    createServiceComponent(clusterName, serviceGroupName, "HDFS","NAMENODE", State.INIT);

    createServiceComponentHost(clusterName, serviceGroupName, "HDFS","NAMENODE","c6401", null);
  }

  private void addHost(String hostname, String clusterName) throws AmbariException {
    clusters.addHost(hostname);
    setOsFamily(clusters.getHost(hostname), "redhat", "6.3");
    clusters.getHost(hostname).setState(HostState.HEALTHY);
    if (null != clusterName) {
      clusters.mapHostToCluster(hostname, clusterName);
    }
  }

  private void setOsFamily(Host host, String osFamily, String osVersion) {
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", osFamily);
    hostAttributes.put("os_release_version", osVersion);

    host.setHostAttributes(hostAttributes);
  }

  private void createCluster(String clusterName) throws AmbariException, AuthorizationException {
    ClusterRequest r = new ClusterRequest(null, clusterName, State.INSTALLED.name(),
        SecurityType.NONE, STACK_ID.getStackId(), null);

    controller.createCluster(r);
  }

  private void createService(String clusterName, String serviceGroupName, String serviceName, State desiredState)
      throws AmbariException, AuthorizationException {
    ServiceRequest r1 = new ServiceRequest(clusterName, serviceGroupName, serviceName, m_repositoryVersion.getId(), desiredState != null ? desiredState.toString() : null, null);
    ServiceResourceProviderTest.createServices(controller, injector.getInstance(RepositoryVersionDAO.class), Collections.singleton(r1));
  }

  private void createServiceComponent(String clusterName, String serviceGroupName,
      String serviceName, String componentName, State desiredState)
      throws AmbariException, AuthorizationException {
    ServiceComponentRequest r = new ServiceComponentRequest(clusterName, serviceGroupName, serviceName, componentName, desiredState != null ? desiredState.name() : null);
    ComponentResourceProviderTest.createComponents(controller, Collections.singleton(r));
  }

  private void createServiceComponentHost(String clusterName, String serviceGroupName, String serviceName, String componentName, String hostname, State desiredState)
      throws AmbariException, AuthorizationException {
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName, serviceGroupName, serviceName, componentName, hostname, desiredState != null ? desiredState.name() : null);
    controller.createHostComponents(Collections.singleton(r));
  }

}
