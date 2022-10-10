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
package org.apache.ambari.server.agent.stomp;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DATANODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCluster;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCurrentPingPort;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOsType;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.NAMENODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.SECONDARY_NAMENODE;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ActionManagerTestHelper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.agent.AgentReportsProcessor;
import org.apache.ambari.server.agent.HeartBeat;
import org.apache.ambari.server.agent.HeartBeatResponse;
import org.apache.ambari.server.agent.HeartbeatTestHelper;
import org.apache.ambari.server.agent.Register;
import org.apache.ambari.server.agent.RegistrationResponse;
import org.apache.ambari.server.agent.RegistrationStatus;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.Assert;

public class HeartbeatControllerTest {
  private static final Logger LOG = LoggerFactory.getLogger(HeartbeatControllerTest.class);
  private Injector injector;
  private Clusters clusters;
  private AgentReportsProcessor agentReportsProcessor;
  private HeartbeatController heartbeatController;

  @Inject
  private AmbariMetaInfo metaInfo;

  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private HeartbeatTestHelper heartbeatTestHelper;

  @Inject
  private ActionManagerTestHelper actionManagerTestHelper;

  @Inject
  private AuditLogger auditLogger;

  @Inject
  private OrmTestHelper helper;

  private InMemoryDefaultTestModule module;

  @Before
  public void setup() throws Exception {
    module = HeartbeatTestHelper.getTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    injector.injectMembers(this);
    heartbeatController = new HeartbeatController(injector);
    agentReportsProcessor = injector.getInstance(AgentReportsProcessor.class);
    EasyMock.replay(auditLogger, injector.getInstance(STOMPUpdatePublisher.class));

    heartbeatTestHelper.injectAgentsRegistrationQueue(heartbeatController);
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    EasyMock.reset(auditLogger);
  }

  @Test
  public void testHeartbeat() throws Exception {
    heartbeatTestHelper.registerAgent(heartbeatController, heartbeatTestHelper.createRegister());

    long registerTime = clusters.getHost(DummyHostname1).getLastHeartbeatTime();

    HeartBeat heartBeat = new HeartBeat();
    heartBeat.setResponseId(0);
    heartbeatController.heartbeat(DummyHostname1, heartBeat);

    assertTrue(registerTime <= clusters.getHost(DummyHostname1).getLastHeartbeatTime());
    assertEquals(HostState.HEALTHY, clusters.getHost(DummyHostname1).getState());
  }

  @Test
  public void testRegistration() throws Exception{
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);

    RegistrationResponse registrationResponse =
        heartbeatTestHelper.registerAgent(heartbeatController,  heartbeatTestHelper.createRegister());

    assertEquals(0, registrationResponse.getResponseId());

    assertEquals(hostObject.getState(), HostState.WAITING_FOR_HOST_STATUS_UPDATES);
    assertEquals(DummyOsType, hostObject.getOsType());
    assertEquals(DummyCurrentPingPort, hostObject.getCurrentPingPort());
    assertTrue(hostObject.getLastRegistrationTime() != 0);
    assertEquals(hostObject.getLastHeartbeatTime(),
        hostObject.getLastRegistrationTime());
  }

  @Test
  public void testRegistrationWithBadVersion() throws Exception {
    clusters.addHost(DummyHostname1);

    Register register = heartbeatTestHelper.createRegister();

    register.setAgentVersion("");  // Invalid agent version
    RegistrationResponse registrationResponse = heartbeatTestHelper.registerAgent(heartbeatController, register);

    assertEquals(-1, registrationResponse.getResponseId());
    assertEquals(RegistrationStatus.FAILED, registrationResponse.getResponseStatus());
    assertEquals(1, registrationResponse.getExitStatus());

    register.setAgentVersion(null); // Invalid agent version
    registrationResponse = heartbeatTestHelper.registerAgent(heartbeatController, register);

    assertEquals(-1, registrationResponse.getResponseId());
    assertEquals(RegistrationStatus.FAILED, registrationResponse.getResponseStatus());
    assertEquals(1, registrationResponse.getExitStatus());
  }

  @Test
  public void testRegistrationPublicHostname() throws Exception {
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);

    Register register = heartbeatTestHelper.createRegister();
    register.setPublicHostname(DummyHostname1 + "-public");

    heartbeatTestHelper.registerAgent(heartbeatController, register);

    assertEquals(hostObject.getState(), HostState.WAITING_FOR_HOST_STATUS_UPDATES);
    assertEquals(DummyOsType, hostObject.getOsType());
    assertTrue(hostObject.getLastRegistrationTime() != 0);
    assertEquals(hostObject.getLastHeartbeatTime(),
        hostObject.getLastRegistrationTime());

    Host verifyHost = clusters.getHost(DummyHostname1);
    assertEquals(verifyHost.getPublicHostName(), register.getPublicHostname());
  }


  @Test
  public void testInvalidOSRegistration() throws Exception {
    clusters.addHost(DummyHostname1);

    Register register = heartbeatTestHelper.createRegister();
    register.getHardwareProfile().setOS("MegaOperatingSystem");  // Invalid os

    RegistrationResponse registrationResponse = heartbeatTestHelper.registerAgent(heartbeatController, register);

    assertEquals(-1, registrationResponse.getResponseId());
    assertEquals(RegistrationStatus.FAILED, registrationResponse.getResponseStatus());
    assertEquals(1, registrationResponse.getExitStatus());
  }

  @Test
  public void testIncompatibleAgentRegistration() throws Exception {
    clusters.addHost(DummyHostname1);

    Register register = heartbeatTestHelper.createRegister();
    register.setAgentVersion("0.0.0"); // Invalid agent version

    RegistrationResponse registrationResponse = heartbeatTestHelper.registerAgent(heartbeatController, register);

    assertEquals(-1, registrationResponse.getResponseId());
    assertEquals(RegistrationStatus.FAILED, registrationResponse.getResponseStatus());
    assertEquals(1, registrationResponse.getExitStatus());
  }

  @Test
  public void testRegisterNewNode() throws Exception {
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);

    Register register = heartbeatTestHelper.createRegister();

    RegistrationResponse registrationResponse = heartbeatTestHelper.registerAgent(heartbeatController, register);

    assertEquals(hostObject.getState(), HostState.WAITING_FOR_HOST_STATUS_UPDATES);
    assertEquals("centos5", hostObject.getOsType());
    assertEquals(0, registrationResponse.getResponseId());
    assertEquals(register.getPrefix(), hostObject.getPrefix());
  }

  @Test
  public void testRequestId() throws IOException, InvalidStateTransitionException, ExecutionException,
      InterruptedException {
    clusters.addHost(DummyHostname1);

    Register register = heartbeatTestHelper.createRegister();

    RegistrationResponse registrationResponse = heartbeatTestHelper.registerAgent(heartbeatController, register);

    assertEquals("ResponseId should start from zero", 0L, registrationResponse.getResponseId());

    HeartBeat heartBeat = new HeartBeat();
    heartBeat.setResponseId(0);
    HeartBeatResponse heartBeatResponse = heartbeatController.heartbeat(DummyHostname1, heartBeat);

    assertEquals("responseId was not incremented", 1L, heartBeatResponse.getResponseId());
    assertTrue("Not cached response returned", heartBeatResponse == heartbeatController.heartbeat(DummyHostname1, heartBeat));

    heartBeat = new HeartBeat();
    heartBeat.setResponseId(1);
    heartBeatResponse = heartbeatController.heartbeat(DummyHostname1, heartBeat);
    assertEquals("responseId was not incremented", 2L, heartBeatResponse.getResponseId());
    assertTrue("Agent is flagged for restart", heartBeatResponse.isRestartAgent() == null);

    heartBeat = new HeartBeat();
    heartBeat.setResponseId(20);
    heartBeatResponse = heartbeatController.heartbeat(DummyHostname1, heartBeat);
    assertTrue("Agent is not flagged for restart", heartBeatResponse.isRestartAgent());
  }

  @Test
  public void testStatusHeartbeatWithAnnotation() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    heartbeatTestHelper.registerAgent(heartbeatController, heartbeatTestHelper.createRegister());

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);

    HeartBeat heartBeat = new HeartBeat();
    heartBeat.setResponseId(0);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }}).anyTimes();
    replay(am);

    HeartBeatResponse heartBeatResponse = heartbeatController.heartbeat(DummyHostname1, heartBeat);
    Assert.assertFalse(heartBeatResponse.hasMappedComponents());

    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INIT);

    heartBeat = new HeartBeat();
    heartBeat.setResponseId(1);
    heartBeatResponse = heartbeatController.heartbeat(DummyHostname1, heartBeat);
    Assert.assertTrue(heartBeatResponse.hasMappedComponents());
  }
}
