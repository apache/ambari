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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.RecoveryTopologyManager;
import org.apache.ambari.server.agent.Register;
import org.apache.ambari.server.agent.RegistrationResponse;
import org.apache.ambari.server.agent.RegistrationStatus;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.encryption.AgentEncryptionCapabilities;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

public class HeartbeatControllerTest {
  private static final long HOST_ID = 17L;
  private static final String HOST_NAME = "host.example.com";
  private static final String SESSION_ID = "session-1";

  private HeartBeatHandler heartBeatHandler;
  private AgentSessionManager agentSessionManager;
  private HostLevelParamsHolder hostLevelParamsHolder;
  private RecoveryTopologyManager recoveryTopologyManager;
  private AgentEncryptionCapabilities encryptionCapabilities;
  private AgentConfigsHolder agentConfigsHolder;
  private Host host;
  private RegistrationResponse registrationResponse;
  private HeartbeatController controller;

  @Before
  public void setUp() throws Exception {
    Injector injector = mock(Injector.class);
    heartBeatHandler = mock(HeartBeatHandler.class);
    ClustersImpl clusters = mock(ClustersImpl.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    agentSessionManager = mock(AgentSessionManager.class);
    hostLevelParamsHolder = mock(HostLevelParamsHolder.class);
    recoveryTopologyManager = mock(RecoveryTopologyManager.class);
    encryptionCapabilities = mock(AgentEncryptionCapabilities.class);
    agentConfigsHolder = mock(AgentConfigsHolder.class);
    Configuration configuration = mock(Configuration.class);
    host = mock(Host.class);

    when(injector.getInstance(HeartBeatHandler.class)).thenReturn(heartBeatHandler);
    when(injector.getInstance(ClustersImpl.class)).thenReturn(clusters);
    when(injector.getInstance(UnitOfWork.class)).thenReturn(unitOfWork);
    when(injector.getInstance(AgentSessionManager.class)).thenReturn(agentSessionManager);
    when(injector.getInstance(HostLevelParamsHolder.class)).thenReturn(hostLevelParamsHolder);
    when(injector.getInstance(RecoveryTopologyManager.class)).thenReturn(recoveryTopologyManager);
    when(injector.getInstance(AgentEncryptionCapabilities.class)).thenReturn(encryptionCapabilities);
    when(injector.getInstance(AgentConfigsHolder.class)).thenReturn(agentConfigsHolder);
    when(injector.getInstance(Configuration.class)).thenReturn(configuration);
    when(configuration.getAgentsRegistrationQueueSize()).thenReturn(10);
    when(configuration.getRegistrationThreadPoolSize()).thenReturn(1);
    when(clusters.getHost(HOST_NAME)).thenReturn(host);
    when(host.getHostId()).thenReturn(HOST_ID);

    registrationResponse = new RegistrationResponse();
    when(heartBeatHandler.handleRegistration(any(Register.class))).thenReturn(registrationResponse);
    controller = new HeartbeatController(injector);
  }

  @After
  public void shutdownExecutors() throws Exception {
    shutdownExecutor("executor");
    shutdownExecutor("scheduledExecutorService");
  }

  @Test
  public void updatesRecoveryTopologyWhenAgentRegisters() throws Exception {
    Register register = register(Collections.emptyList());

    assertSame(registrationResponse, controller.register(SESSION_ID, register).get(5, TimeUnit.SECONDS));

    verify(recoveryTopologyManager).beginAgentSession(HOST_ID, SESSION_ID);
    verify(hostLevelParamsHolder).updateRecoveryTopology(HOST_NAME);
    verify(agentSessionManager).register(SESSION_ID, host);
  }

  @Test
  public void invalidatesCachedConfigsWhenEncryptionCapabilitiesChange() throws Exception {
    List<String> advertised = Collections.singletonList(AgentEncryptionCapabilities.AES256_GCM);
    Register register = register(advertised);
    when(encryptionCapabilities.update(HOST_ID, advertised)).thenReturn(true);

    assertSame(registrationResponse, controller.register(SESSION_ID, register).get(5, TimeUnit.SECONDS));

    verify(encryptionCapabilities).update(HOST_ID, advertised);
    verify(agentConfigsHolder).onEncryptionCapabilitiesChanged(HOST_ID);
  }

  @Test
  public void keepsCachedConfigsForEmptyUnknownAndUnchangedCapabilities() throws Exception {
    List<String> empty = Collections.emptyList();
    List<String> unknown = Collections.singletonList("unknown");
    List<String> unchanged = Collections.singletonList(AgentEncryptionCapabilities.AES256_GCM);
    when(encryptionCapabilities.update(HOST_ID, empty)).thenReturn(false);
    when(encryptionCapabilities.update(HOST_ID, unknown)).thenReturn(false);
    when(encryptionCapabilities.update(HOST_ID, unchanged)).thenReturn(false);

    controller.register(SESSION_ID, register(empty)).get(5, TimeUnit.SECONDS);
    controller.register(SESSION_ID, register(unknown)).get(5, TimeUnit.SECONDS);
    controller.register(SESSION_ID, register(unchanged)).get(5, TimeUnit.SECONDS);

    verify(encryptionCapabilities).update(HOST_ID, empty);
    verify(encryptionCapabilities).update(HOST_ID, unknown);
    verify(encryptionCapabilities).update(HOST_ID, unchanged);
    verifyNoInteractions(agentConfigsHolder);
  }

  @Test
  public void doesNotUpdateCapabilitiesWhenRegistrationFails() throws Exception {
    when(heartBeatHandler.handleRegistration(any(Register.class))).thenThrow(new AmbariException("registration failed"));

    RegistrationResponse response = controller.register(SESSION_ID,
        register(Collections.singletonList(AgentEncryptionCapabilities.AES256_GCM))).get(5, TimeUnit.SECONDS);

    assertEquals(-1, response.getResponseId());
    assertEquals(RegistrationStatus.FAILED, response.getResponseStatus());
    verify(agentSessionManager, never()).register(any(String.class), any(Host.class));
    verifyNoInteractions(recoveryTopologyManager, hostLevelParamsHolder, encryptionCapabilities, agentConfigsHolder);
  }

  private Register register(List<String> encryptionTypes) {
    Register register = new Register();
    register.setHostname(HOST_NAME);
    register.setEncryptionTypes(encryptionTypes);
    return register;
  }

  private void shutdownExecutor(String fieldName) throws Exception {
    Field field = HeartbeatController.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    ((ExecutorService) field.get(controller)).shutdownNow();
  }
}
