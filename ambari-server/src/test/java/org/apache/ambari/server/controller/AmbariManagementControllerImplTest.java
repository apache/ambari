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

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.captureBoolean;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.RollbackException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.rest.AgentResource;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.agent.stomp.MetadataHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.metadata.ClusterMetadataGenerator;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.LdapSyncSpecEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.registry.RegistryManager;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.security.ldap.AmbariLdapDataPopulator;
import org.apache.ambari.server.security.ldap.LdapBatchDto;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Module;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.OsSpecific;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import junit.framework.Assert;

/**
 * AmbariManagementControllerImpl unit tests
 */
public class AmbariManagementControllerImplTest {

  // Mocks
  private static final AmbariLdapDataPopulator ldapDataPopulator = createMock(AmbariLdapDataPopulator.class);
  private static final Clusters clusters = createNiceMock(Clusters.class);
  private static final ActionDBAccessorImpl actionDBAccessor = createNiceMock(ActionDBAccessorImpl.class);
  private static final AmbariMetaInfo ambariMetaInfo = createMock(AmbariMetaInfo.class);
  private static final Users users = createMock(Users.class);
  private static final AmbariSessionManager sessionManager = createNiceMock(AmbariSessionManager.class);
  private static final RegistryManager registryManager = createNiceMock(RegistryManager.class);
  private static final HostComponentStateEntity hostComponentStateEntity = createMock(HostComponentStateEntity.class);
  private static final HostComponentStateDAO hostComponentStateDAO = createMock(HostComponentStateDAO.class);
  private static final ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = createMock(ServiceComponentDesiredStateEntity.class);
  private static final ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = createMock(ServiceComponentDesiredStateDAO.class);
  private static final ClusterMetadataGenerator metadataGenerator = createNiceMock(ClusterMetadataGenerator.class);

  @BeforeClass
  public static void setupAuthentication() {
    // Set authenticated user so that authorization checks will pass
    InternalAuthenticationToken authenticationToken = new InternalAuthenticationToken("admin");
    authenticationToken.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
  }

  @Before
  public void before() {
    reset(ldapDataPopulator, clusters, actionDBAccessor, ambariMetaInfo, users, sessionManager, metadataGenerator,
            hostComponentStateEntity, hostComponentStateDAO, serviceComponentDesiredStateEntity, serviceComponentDesiredStateDAO);
    replay(metadataGenerator);
  }

  @Test
  public void testGetClusters() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();

    ClusterRequest request1 = new ClusterRequest(null, "cluster1", "1", Collections.emptySet());
    Cluster cluster = createNiceMock(Cluster.class);
    ClusterResponse response = createNiceMock(ClusterResponse.class);

    Set<ClusterRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, createNiceMock(KerberosHelper.class));

    // getCluster
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.convertToResponse()).andReturn(response);

    CredentialStoreService credentialStoreService = createNiceMock(CredentialStoreService.class);
    expect(credentialStoreService.isInitialized(anyObject(CredentialStoreType.class))).andReturn(true).anyTimes();

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // replay mocks
    replay(injector, clusters, cluster, response, credentialStoreService, hostComponentStateDAO, serviceComponentDesiredStateDAO);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);

    Field f = controller.getClass().getDeclaredField("credentialStoreService");
    f.setAccessible(true);
    f.set(controller, credentialStoreService);

    Set<ClusterResponse> setResponses = controller.getClusters(setRequests);

    // assert and verify
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(injector, clusters, cluster, response, credentialStoreService, hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  /**
   * Ensure that ClusterNotFoundException is propagated in case where there is a single request.
   */
  @Test
  public void testGetClusters___ClusterNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();

    // requests
    ClusterRequest request1 = new ClusterRequest(null, "cluster1", "1", Collections.emptySet());

    Set<ClusterRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, createNiceMock(KerberosHelper.class));

    // getCluster
    expect(clusters.getCluster("cluster1")).andThrow(new ClusterNotFoundException("cluster1"));

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // replay mocks
    replay(injector, clusters, hostComponentStateDAO, serviceComponentDesiredStateDAO);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      controller.getClusters(setRequests);
      fail("expected ClusterNotFoundException");
    } catch (ClusterNotFoundException e) {
      // expected
    }

    verify(injector, clusters, hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  /**
   * Ensure that ClusterNotFoundException is handled where there are multiple requests as would be the
   * case when an OR predicate is provided in the query.
   */
  @Test
  public void testGetClusters___OR_Predicate_ClusterNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();

    Cluster cluster = createNiceMock(Cluster.class);
    Cluster cluster2 = createNiceMock(Cluster.class);
    ClusterResponse response = createNiceMock(ClusterResponse.class);
    ClusterResponse response2 = createNiceMock(ClusterResponse.class);

    // requests
    ClusterRequest request1 = new ClusterRequest(null, "cluster1", "1", Collections.emptySet());
    ClusterRequest request2 = new ClusterRequest(null, "cluster2", "1", Collections.emptySet());
    ClusterRequest request3 = new ClusterRequest(null, "cluster3", "1", Collections.emptySet());
    ClusterRequest request4 = new ClusterRequest(null, "cluster4", "1", Collections.emptySet());

    Set<ClusterRequest> setRequests = new HashSet<>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);
    setRequests.add(request4);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, createNiceMock(KerberosHelper.class));

    // getCluster
    expect(clusters.getCluster("cluster1")).andThrow(new ClusterNotFoundException("cluster1"));
    expect(clusters.getCluster("cluster2")).andReturn(cluster);
    expect(clusters.getCluster("cluster3")).andReturn(cluster2);
    expect(clusters.getCluster("cluster4")).andThrow(new ClusterNotFoundException("cluster4"));

    expect(cluster.convertToResponse()).andReturn(response);
    expect(cluster2.convertToResponse()).andReturn(response2);

    CredentialStoreService credentialStoreService = createNiceMock(CredentialStoreService.class);
    expect(credentialStoreService.isInitialized(anyObject(CredentialStoreType.class))).andReturn(true).anyTimes();

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // replay mocks
    replay(injector, clusters, cluster, cluster2, response, response2, credentialStoreService,
            hostComponentStateDAO, serviceComponentDesiredStateDAO);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);

    Field f = controller.getClass().getDeclaredField("credentialStoreService");
    f.setAccessible(true);
    f.set(controller, credentialStoreService);

    Set<ClusterResponse> setResponses = controller.getClusters(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, cluster2, response, response2, credentialStoreService,
            hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  /**
   * Ensure that when the cluster id is provided and the given cluster name is different from the cluster's name
   * then the cluster rename logic is executed.
   */
  @Test
  public void testUpdateClusters() throws Exception {
    // member state mocks
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    ClusterRequest clusterRequest = createNiceMock(ClusterRequest.class);
    ConfigurationRequest configurationRequest = createNiceMock(ConfigurationRequest.class);

    // requests
    Set<ClusterRequest> setRequests = new HashSet<>();
    setRequests.add(clusterRequest);

    List<ConfigurationRequest> configRequests = new ArrayList<>();
    configRequests.add(configurationRequest);

    KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);
    MetadataHolder metadataHolder = createMock(MetadataHolder.class);
    AgentConfigsHolder agentConfigsHolder = createMockBuilder(AgentConfigsHolder.class)
        .addMockedMethod("updateData").createMock();
    // expectations
    constructorInit(injector, controllerCapture, null, null,
        kerberosHelper, metadataHolder, agentConfigsHolder
    );

    expect(metadataHolder.updateData(anyObject())).andReturn(true).anyTimes();

    agentConfigsHolder.updateData(anyLong(), anyObject(List.class));
    expectLastCall().anyTimes();

    expect(clusterRequest.getClusterName()).andReturn("clusterNew").times(3);
    expect(clusterRequest.getClusterId()).andReturn(1L).times(4);
    expect(clusterRequest.getDesiredConfig()).andReturn(configRequests);
    expect(configurationRequest.getVersionTag()).andReturn(null).times(1);
    expect(clusters.getClusterById(1L)).andReturn(cluster).times(1);
    expect(cluster.getClusterName()).andReturn("clusterOld").times(1);

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    cluster.setClusterName("clusterNew");
    expectLastCall();

    configurationRequest.setVersionTag(EasyMock.anyObject(String.class));
    expectLastCall();

    // replay mocks
    replay(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, configurationRequest,
            hostComponentStateDAO, serviceComponentDesiredStateDAO, metadataHolder, agentConfigsHolder);


    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(actionManager, clusters, metadataGenerator, injector);
    controller.updateClusters(setRequests, null);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, configurationRequest,
            hostComponentStateDAO, serviceComponentDesiredStateDAO,
        metadataHolder, agentConfigsHolder);
  }

  /**
   * Ensure that processing update request does not fail on configuration
   * properties with no value specified (no value = null reference value)
   * TODO disabled for now as tests nothing, check what exactly should be tested here
   * updateCluster request was noop due to equality on cluster and request configs (both contained null)
   * mocks are too limited to pass further these base checks
   */
  @Test
  @Ignore
  public void testUpdateClustersWithNullConfigPropertyValues() throws Exception {
    // member state mocks
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    ClusterRequest clusterRequest = createNiceMock(ClusterRequest.class);
    Config config = createNiceMock(Config.class);

    // requests
    Set<ClusterRequest> setRequests = Collections.singleton(clusterRequest);

    KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);
    // expectations
    constructorInit(injector, controllerCapture, kerberosHelper);

    expect(clusterRequest.getClusterName()).andReturn("clusterNew").anyTimes();
    expect(clusterRequest.getClusterId()).andReturn(1L).anyTimes();

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    ConfigurationRequest configReq = new ConfigurationRequest();
    final Map<String, String> configReqProps = Maps.newHashMap();
    configReqProps.put("p1", null);
    configReq.setProperties(configReqProps);

    expect(clusterRequest.getDesiredConfig()).andReturn(ImmutableList.of(configReq)).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster).anyTimes();
    expect(cluster.getClusterName()).andReturn("clusterOld").anyTimes();
    expect(cluster.getConfigPropertiesTypes(anyObject(String.class))).andReturn(Maps.newHashMap()).anyTimes();

    expect(config.getType()).andReturn("config-type").anyTimes();
    expect(config.getProperties()).andReturn(configReqProps).anyTimes();
    expect(config.getPropertiesAttributes()).andReturn(new HashMap<>()).anyTimes();
    expect(cluster.getDesiredConfigByType(anyObject(String.class))).andReturn(config).anyTimes();

    cluster.addSessionAttributes(EasyMock.anyObject());
    expectLastCall().once();

    cluster.setClusterName("clusterNew");
    expectLastCall();

    // replay mocks
    replay(actionManager, cluster, clusters, config, injector, clusterRequest, sessionManager,
            hostComponentStateDAO, serviceComponentDesiredStateDAO);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(actionManager, clusters, metadataGenerator, injector);
    controller.updateClusters(setRequests, null);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(actionManager, cluster, clusters, config, injector, clusterRequest, sessionManager,
            hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  /**
   * Ensure that when the cluster is updated KerberosHandler.toggleKerberos is not invoked unless
   * the security type is altered
   */
  @Test
  public void testUpdateClustersToggleKerberosNotInvoked() throws Exception {
    // member state mocks
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    ClusterRequest clusterRequest = createNiceMock(ClusterRequest.class);

    // requests
    Set<ClusterRequest> setRequests = Collections.singleton(clusterRequest);

    KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);
    MetadataHolder metadataHolder = createMock(MetadataHolder.class);
    AgentConfigsHolder agentConfigsHolder = createMockBuilder(AgentConfigsHolder.class)
        .addMockedMethod("updateData").createMock();
    // expectations
    constructorInit(injector, controllerCapture, null, null,
        kerberosHelper, metadataHolder, agentConfigsHolder
    );

    expect(metadataHolder.updateData(anyObject())).andReturn(true).anyTimes();

    agentConfigsHolder.updateData(anyLong(), anyObject(List.class));
    expectLastCall().anyTimes();

    expect(clusterRequest.getClusterId()).andReturn(1L).times(4);
    expect(clusters.getClusterById(1L)).andReturn(cluster).times(1);
    expect(cluster.getClusterName()).andReturn("cluster").times(1);

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // replay mocks
    replay(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, kerberosHelper,
            hostComponentStateDAO, serviceComponentDesiredStateDAO,
        metadataHolder, agentConfigsHolder);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(actionManager, clusters, metadataGenerator, injector);
    controller.updateClusters(setRequests, null);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, kerberosHelper,
            hostComponentStateDAO, serviceComponentDesiredStateDAO,
        metadataHolder, agentConfigsHolder);
  }

  /**
   * Ensure that when the cluster security type updated from KERBEROS to KERBEROS,
   * KerberosHandler.toggleKerberos IS NOT invoked
   */

  @Test
  public void testUpdateClustersToggleKerberosReenable() throws Exception {
    // member state mocks
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    ClusterRequest clusterRequest = createNiceMock(ClusterRequest.class);

    // requests
    Set<ClusterRequest> setRequests = Collections.singleton(clusterRequest);

    KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);
    MetadataHolder metadataHolder = createMock(MetadataHolder.class);
    AgentConfigsHolder agentConfigsHolder = createMockBuilder(AgentConfigsHolder.class)
        .addMockedMethod("updateData").createMock();
    // expectations
    constructorInit(injector, controllerCapture, null, null,
        kerberosHelper, metadataHolder, agentConfigsHolder
    );

    expect(metadataHolder.updateData(anyObject())).andReturn(true).anyTimes();

    agentConfigsHolder.updateData(anyLong(), anyObject(List.class));
    expectLastCall().anyTimes();

    expect(clusterRequest.getClusterId()).andReturn(1L).times(4);
    expect(clusterRequest.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster).times(1);
    expect(cluster.getClusterName()).andReturn("cluster").times(1);
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    expect(kerberosHelper.shouldExecuteCustomOperations(SecurityType.KERBEROS, null))
        .andReturn(false)
        .once();
    expect(kerberosHelper.getForceToggleKerberosDirective(EasyMock.anyObject()))
        .andReturn(false)
        .once();
    // Note: kerberosHelper.toggleKerberos is not called

    // replay mocks
    replay(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, kerberosHelper,
            hostComponentStateDAO, serviceComponentDesiredStateDAO,
        metadataHolder, agentConfigsHolder);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(actionManager, clusters, metadataGenerator, injector);
    controller.updateClusters(setRequests, null);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, kerberosHelper,
            hostComponentStateDAO, serviceComponentDesiredStateDAO,
        metadataHolder, agentConfigsHolder);
  }
  /**
   * Ensure that when the cluster security type updated from NONE to KERBEROS, KerberosHandler.toggleKerberos
   * IS invoked
   */
  @Test
  public void testUpdateClustersToggleKerberosEnable() throws Exception {
    // member state mocks
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    ClusterRequest clusterRequest = createNiceMock(ClusterRequest.class);

    // requests
    Set<ClusterRequest> setRequests = Collections.singleton(clusterRequest);

    KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);
    MetadataHolder metadataHolder = createMock(MetadataHolder.class);
    AgentConfigsHolder agentConfigsHolder = createMockBuilder(AgentConfigsHolder.class)
        .addMockedMethod("updateData").createMock();
    // expectations
    constructorInit(injector, controllerCapture, null, null,
        kerberosHelper, metadataHolder, agentConfigsHolder
    );

    expect(metadataHolder.updateData(anyObject())).andReturn(true).anyTimes();

    agentConfigsHolder.updateData(anyLong(), anyObject(List.class));
    expectLastCall().anyTimes();

    expect(clusterRequest.getClusterId()).andReturn(1L).times(4);
    expect(clusterRequest.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster).times(1);
    expect(cluster.getClusterName()).andReturn("cluster").times(1);
    expect(cluster.getSecurityType()).andReturn(SecurityType.NONE).anyTimes();

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    expect(kerberosHelper.shouldExecuteCustomOperations(SecurityType.KERBEROS, null))
        .andReturn(false)
        .once();
    expect(kerberosHelper.getForceToggleKerberosDirective(null))
        .andReturn(false)
        .once();
    expect(kerberosHelper.getManageIdentitiesDirective(null))
        .andReturn(null)
        .once();
    expect(kerberosHelper.toggleKerberos(anyObject(Cluster.class), anyObject(SecurityType.class), anyObject(RequestStageContainer.class), anyBoolean()))
        .andReturn(null)
        .once();

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // replay mocks
    replay(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, kerberosHelper,
            hostComponentStateDAO, serviceComponentDesiredStateDAO,
        metadataHolder, agentConfigsHolder);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(actionManager, clusters, metadataGenerator, injector);
    controller.updateClusters(setRequests, null);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, kerberosHelper,
            hostComponentStateDAO, serviceComponentDesiredStateDAO,
        metadataHolder, agentConfigsHolder);
  }

  /**
   * Ensure that when the cluster security type updated from KERBEROS to NONE, KerberosHandler.toggleKerberos
   * IS invoked
   */
  @Test
  public void testUpdateClustersToggleKerberosDisable_Default() throws Exception {
    testUpdateClustersToggleKerberosDisable(null);
  }

  /**
   * Ensure that when the cluster security type updated from KERBEROS to NONE, KerberosHandler.toggleKerberos
   * IS invoked and identities are not managed
   */
  @Test
  public void testUpdateClustersToggleKerberosDisable_NoManageIdentities() throws Exception {
    testUpdateClustersToggleKerberosDisable(Boolean.FALSE);
  }

  /**
   * Ensure that when the cluster security type updated from KERBEROS to NONE, KerberosHandler.toggleKerberos
   * IS invoked and identities are managed
   */
  @Test
  public void testUpdateClustersToggleKerberosDisable_ManageIdentities() throws Exception {
    testUpdateClustersToggleKerberosDisable(Boolean.TRUE);
  }

  /**
   * Ensure that when the cluster security type updated from KERBEROS to NONE, KerberosHandler.toggleKerberos
   * IS invoked
   */
  private void testUpdateClustersToggleKerberosDisable(Boolean manageIdentities) throws Exception {
    // member state mocks
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    ClusterRequest clusterRequest = createNiceMock(ClusterRequest.class);

    // requests
    Set<ClusterRequest> setRequests = Collections.singleton(clusterRequest);

    Capture<Boolean> manageIdentitiesCapture = EasyMock.newCapture();

    KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);
    MetadataHolder metadataHolder = createMock(MetadataHolder.class);
    AgentConfigsHolder agentConfigsHolder = createMockBuilder(AgentConfigsHolder.class)
        .addMockedMethod("updateData").createMock();
    // expectations
    constructorInit(injector, controllerCapture, null, null,
        kerberosHelper, metadataHolder, agentConfigsHolder
    );

    expect(metadataHolder.updateData(anyObject())).andReturn(true).anyTimes();

    agentConfigsHolder.updateData(anyLong(), anyObject(List.class));
    expectLastCall().anyTimes();

    expect(clusterRequest.getClusterId()).andReturn(1L).times(4);
    expect(clusterRequest.getSecurityType()).andReturn(SecurityType.NONE).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster).times(1);
    expect(cluster.getClusterName()).andReturn("cluster").times(1);
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    expect(kerberosHelper.shouldExecuteCustomOperations(SecurityType.NONE, null))
        .andReturn(false)
        .once();
    expect(kerberosHelper.getForceToggleKerberosDirective(EasyMock.anyObject()))
        .andReturn(false)
        .once();
    expect(kerberosHelper.getManageIdentitiesDirective(EasyMock.anyObject()))
        .andReturn(manageIdentities)
        .once();
    expect(kerberosHelper.toggleKerberos(anyObject(Cluster.class), anyObject(SecurityType.class), anyObject(RequestStageContainer.class), captureBoolean(manageIdentitiesCapture)))
        .andReturn(null)
        .once();

    // replay mocks
    replay(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, kerberosHelper,
        metadataHolder, agentConfigsHolder);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(actionManager, clusters, metadataGenerator, injector);
    controller.updateClusters(setRequests, null);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(manageIdentities, manageIdentitiesCapture.getValue());
    verify(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, kerberosHelper,
        metadataHolder, agentConfigsHolder);
  }

  /**
   * Ensure that when the cluster security type updated from KERBEROS to NONE, KerberosHandler.toggleKerberos
   * IS invoked
   */
  @Test
  public void testUpdateClustersToggleKerberos_Fail() throws Exception {
    // member state mocks
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createMock(Cluster.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    ClusterRequest clusterRequest = createNiceMock(ClusterRequest.class);

    // requests
    Set<ClusterRequest> setRequests = Collections.singleton(clusterRequest);

    KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);
    MetadataHolder metadataHolder = createMock(MetadataHolder.class);
    AgentConfigsHolder agentConfigsHolder = createMockBuilder(AgentConfigsHolder.class)
        .addMockedMethod("updateData").createMock();
    // expectations
    constructorInit(injector, controllerCapture, null, null,
        kerberosHelper, metadataHolder, agentConfigsHolder
    );

    expect(metadataHolder.updateData(anyObject())).andReturn(true);

    agentConfigsHolder.updateData(anyLong(), anyObject(List.class));
    expectLastCall();

    expect(clusterRequest.getClusterId()).andReturn(1L).times(4);
    expect(clusterRequest.getSecurityType()).andReturn(SecurityType.NONE).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster).times(1);
    expect(cluster.getResourceId()).andReturn(1L).times(3);
    expect(cluster.getClusterName()).andReturn("cluster").times(1);
    expect(cluster.getClusterId()).andReturn(1L).times(1);
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(null).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(null).anyTimes();

    cluster.setCurrentStackVersion(anyObject(StackId.class));
    expectLastCall().once();

    cluster.setClusterName(anyObject(String.class));
    expectLastCall().once();

    expect(kerberosHelper.shouldExecuteCustomOperations(SecurityType.NONE, null))
        .andReturn(false)
        .once();
    expect(kerberosHelper.getForceToggleKerberosDirective(EasyMock.anyObject()))
        .andReturn(false)
        .once();
    expect(kerberosHelper.getManageIdentitiesDirective(EasyMock.anyObject()))
        .andReturn(null)
        .once();
    expect(kerberosHelper.toggleKerberos(anyObject(Cluster.class), anyObject(SecurityType.class), anyObject(RequestStageContainer.class), anyBoolean()))
        .andThrow(new IllegalArgumentException("bad args!"))
        .once();

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // replay mocks
    replay(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, kerberosHelper,
            hostComponentStateDAO, serviceComponentDesiredStateDAO,
      agentConfigsHolder, metadataHolder);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(actionManager, clusters, metadataGenerator, injector);

    try {
      controller.updateClusters(setRequests, null);
      Assert.fail("IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {
      // This is expected
    }

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(actionManager, cluster, clusters, injector, clusterRequest, sessionManager, kerberosHelper,
            hostComponentStateDAO, serviceComponentDesiredStateDAO,
      agentConfigsHolder, metadataHolder);
  }

  /**
   * Ensure that RollbackException is thrown outside the updateClusters method
   * when a unique constraint violation occurs.
   */
  @Test
  public void testUpdateClusters__RollbackException() throws Exception {
    // member state mocks
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    ClusterRequest clusterRequest = createNiceMock(ClusterRequest.class);

    // requests
    Set<ClusterRequest> setRequests = Collections.singleton(clusterRequest);

    // expectations
    constructorInit(injector, controllerCapture, createNiceMock(KerberosHelper.class));

    expect(clusterRequest.getClusterName()).andReturn("clusterNew").times(3);
    expect(clusterRequest.getClusterId()).andReturn(1L).times(4);
    expect(clusters.getClusterById(1L)).andReturn(cluster).times(1);
    expect(cluster.getClusterName()).andReturn("clusterOld").times(1);
    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    cluster.setClusterName("clusterNew");
    expectLastCall().andThrow(new RollbackException());

    // replay mocks
    replay(actionManager, cluster, clusters, injector, clusterRequest, sessionManager,
            hostComponentStateDAO, serviceComponentDesiredStateDAO);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(actionManager, clusters, metadataGenerator, injector);
    try {
      controller.updateClusters(setRequests, null);
      fail("Expected RollbackException");
    } catch (RollbackException e) {
      //expected
    }
    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(actionManager, cluster, clusters, injector, clusterRequest, sessionManager,
            hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  @Test
  public void testGetHostComponents() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    final ServiceComponentHost componentHost = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response = createNiceMock(ServiceComponentHostResponse.class);

    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(componentHost)).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 1L, "component1", "component1", "host1", null);

    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    constructorInit(injector, controllerCapture, null, maintHelper,
        createNiceMock(KerberosHelper.class), null, null
    );

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(hostComponentStateDAO.findById(1L)).andReturn(hostComponentStateEntity).anyTimes();
    expect(hostComponentStateEntity.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getComponentName()).andReturn("component1").anyTimes();
    expect(hostComponentStateEntity.getComponentType()).andReturn("component1").anyTimes();

    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();
    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component1", "component1")).andReturn(serviceComponentDesiredStateEntity).anyTimes();
    expect(serviceComponentDesiredStateEntity.getId()).andReturn(1L).times(2);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
        new HashMap<String, Host>() {{
          put("host1", host);
        }}).anyTimes();

    expect(cluster.getServicesByServiceGroup("CORE")).andReturn(ImmutableList.of(service)).anyTimes();
    expect(cluster.getService("CORE", "service1")).andReturn(service).anyTimes();
    expect(cluster.getServiceByComponentName("component1")).andReturn(service).anyTimes();
    expect(service.getName()).andReturn("service1").anyTimes();
    expect(service.getServiceComponents()).andReturn(ImmutableMap.of("component1", component)).anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component).anyTimes();
    expect(component.getId()).andReturn(1L).times(2);
    expect(component.getServiceComponentHosts()).andReturn(
        new HashMap<String, ServiceComponentHost>() {{
          put("host1", componentHost);
        }});
    expect(componentHost.convertToResponse(null)).andReturn(response);
    expect(componentHost.getHostName()).andReturn("host1").anyTimes();
    expect(maintHelper.getEffectiveState(componentHost, host)).andReturn(MaintenanceState.OFF);

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, response, stack,
        ambariMetaInfo, service, component, componentHost, hostComponentStateDAO, hostComponentStateEntity,
        serviceComponentDesiredStateDAO, serviceComponentDesiredStateEntity
    );

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(injector, clusters, cluster, host, response, stack, ambariMetaInfo, service, component, componentHost,
      hostComponentStateDAO, hostComponentStateEntity, serviceComponentDesiredStateDAO,
      serviceComponentDesiredStateEntity
    );
  }

  @Test
  public void testGetHostComponents___ServiceComponentHostNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 1L, "component1", "component1", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, null, maintHelper, createNiceMock(KerberosHelper.class), null, null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));

//    expect(cluster.getDesiredStackVersion()).andReturn(stack);
//    expect(stack.getStackName()).andReturn("stackName");
//    expect(stack.getStackVersion()).andReturn("stackVersion");
//
//    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getServicesByServiceGroup("CORE")).andReturn(ImmutableList.of(service)).anyTimes();
    expect(cluster.getService("CORE", "service1")).andReturn(service).anyTimes();
    expect(cluster.getServiceByComponentName("component1")).andReturn(service).anyTimes();
    expect(service.getServiceComponents()).andReturn(ImmutableMap.of("component1", component)).anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component).anyTimes();
    expect(component.getId()).andReturn(1L).anyTimes();
    expect(component.getServiceComponentHosts()).andReturn(null);

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(hostComponentStateDAO.findById(1L)).andReturn(hostComponentStateEntity).anyTimes();
    expect(hostComponentStateEntity.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getComponentName()).andReturn("component1").anyTimes();
    expect(hostComponentStateEntity.getComponentType()).andReturn("component1").anyTimes();

    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();
    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component1", "component1")).andReturn(serviceComponentDesiredStateEntity).anyTimes();
    expect(serviceComponentDesiredStateEntity.getId()).andReturn(1L).anyTimes();

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
        service, component, hostComponentStateDAO, hostComponentStateEntity, serviceComponentDesiredStateDAO,
            serviceComponentDesiredStateEntity);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    try {
      controller.getHostComponents(setRequests);
      fail("expected ServiceComponentHostNotFoundException");
    } catch (ServiceComponentHostNotFoundException e) {
      //expected
    }

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component,
            hostComponentStateDAO, hostComponentStateEntity, serviceComponentDesiredStateDAO,
            serviceComponentDesiredStateEntity);
  }

  @Test
  public void testGetHostComponents___ServiceComponentHostFilteredByState() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", "component1", "component1", "host1", null);
    request1.setState("INSTALLED");


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    constructorInit(injector, controllerCapture, null, maintHelper,
        createNiceMock(KerberosHelper.class), null, null);

    expect(maintHelper.getEffectiveState(
        anyObject(ServiceComponentHost.class),
        anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
        new HashMap<String, Host>() {{
          put("host1", host);
        }}).anyTimes();

//    expect(cluster.getDesiredStackVersion()).andReturn(stack);
//    expect(stack.getStackName()).andReturn("stackName");
//    expect(stack.getStackVersion()).andReturn("stackVersion");
//
//    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getClusterName()).andReturn("cl1");
    expect(cluster.getServicesByServiceGroup("CORE")).andReturn(ImmutableList.of(service)).anyTimes();
    expect(cluster.getService("CORE", "service1")).andReturn(service).anyTimes();
    expect(cluster.getServiceByComponentName("component1")).andReturn(service).anyTimes();
    expect(service.getServiceComponents()).andReturn(ImmutableMap.of("component1", component)).anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component).anyTimes();
    expect(service.getName()).andReturn("service1").anyTimes();
    expect(component.getName()).andReturn("component1").anyTimes();
    expect(component.getServiceComponentHosts()).andReturn(new HashMap<String, ServiceComponentHost>() {{
      put("host1", componentHost1);
    }});

    expect(componentHost1.getState()).andReturn(State.INSTALLED);
    expect(componentHost1.convertToResponse(null)).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
        service, component, componentHost1, response1, hostComponentStateDAO, serviceComponentDesiredStateDAO);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> responses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertTrue(responses.size() == 1);
    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component, componentHost1, response1,
            hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  @Test
  public void testGetHostComponents___ServiceComponentHostFilteredByMaintenanceState() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", "component1", "component1", "host1", null);
    request1.setMaintenanceState("ON");


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, null, maintHelper, createNiceMock(KerberosHelper.class),
        null, null);

    expect(maintHelper.getEffectiveState(
        anyObject(ServiceComponentHost.class),
        anyObject(Host.class))).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE).anyTimes();

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
        new HashMap<String, Host>() {{
          put("host1", host);
        }}).anyTimes();

    expect(cluster.getClusterName()).andReturn("cl1");
    expect(cluster.getServicesByServiceGroup("CORE")).andReturn(ImmutableList.of(service)).anyTimes();
    expect(cluster.getService("CORE", "service1")).andReturn(service).anyTimes();
    expect(cluster.getServiceByComponentName("component1")).andReturn(service).anyTimes();
    expect(service.getServiceComponents()).andReturn(ImmutableMap.of("component1", component)).anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component).anyTimes();
    expect(service.getName()).andReturn("service1").anyTimes();
    expect(component.getName()).andReturn("component1").anyTimes();
    expect(component.getServiceComponentHosts()).andReturn(new HashMap<String, ServiceComponentHost>() {{
      put("host1", componentHost1);
    }});

    expect(componentHost1.convertToResponse(null)).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
        service, component, componentHost1, response1, hostComponentStateDAO, serviceComponentDesiredStateDAO);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> responses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertTrue(responses.size() == 1);
    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component, componentHost1, response1,
            hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ServiceComponentHostNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component1 = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    final ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    MaintenanceStateHelper stateHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(stateHelper.getEffectiveState(
        anyObject(ServiceComponentHost.class),
        anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
        new HashMap<String, Host>() {{
          put("host1", host);
        }}).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 1L, "component1", "component1", "host1", null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 2L, "component2", "component2","host1", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 3L, "component3", "component3", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, null, stateHelper, createNiceMock(KerberosHelper.class),
        null, null);


    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster)).anyTimes();
    expect(cluster.getServicesByServiceGroup("CORE")).andReturn(ImmutableList.of(service)).anyTimes();
    expect(cluster.getService("CORE", "service1")).andReturn(service).anyTimes();

    expect(cluster.getServiceByComponentName("component1")).andReturn(service).anyTimes();
    expect(service.getServiceComponents()).andReturn(ImmutableMap.of("component1", component1, "component2", component2, "component3", component3)).anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component1).anyTimes();
    expect(service.getName()).andReturn("service1").anyTimes();
    expect(component1.getServiceComponentHosts()).andReturn(
        new HashMap<String, ServiceComponentHost>() {{
          put("host1", componentHost1);
        }});
    expect(componentHost1.convertToResponse(null)).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

    expect(cluster.getServiceByComponentName("component2")).andReturn(service).anyTimes();
    expect(service.getServiceComponent("component2")).andReturn(component2);
    expect(component2.getServiceComponentHosts()).andReturn(null);
    expect(componentHost2.getHostName()).andReturn("host1");

    expect(cluster.getServiceByComponentName("component3")).andReturn(service).anyTimes();
    expect(service.getServiceComponent("component3")).andReturn(component3);
    expect(component3.getServiceComponentHosts()).andReturn(
        new HashMap<String, ServiceComponentHost>() {{
          put("host1", componentHost2);
        }});
    expect(componentHost2.convertToResponse(null)).andReturn(response2);

    HostComponentStateEntity hostComponentStateEntity2 = createNiceMock(HostComponentStateEntity.class);
    HostComponentStateEntity hostComponentStateEntity3 = createNiceMock(HostComponentStateEntity.class);

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(hostComponentStateDAO.findById(1L)).andReturn(hostComponentStateEntity).anyTimes();
    expect(hostComponentStateDAO.findById(2L)).andReturn(hostComponentStateEntity2).anyTimes();
    expect(hostComponentStateDAO.findById(3L)).andReturn(hostComponentStateEntity3).anyTimes();

    expect(hostComponentStateEntity.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getComponentName()).andReturn("component1").anyTimes();
    expect(hostComponentStateEntity.getComponentType()).andReturn("component1").anyTimes();

    expect(hostComponentStateEntity2.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getComponentName()).andReturn("component2").anyTimes();
    expect(hostComponentStateEntity2.getComponentType()).andReturn("component2").anyTimes();

    expect(hostComponentStateEntity3.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getComponentName()).andReturn("component3").anyTimes();
    expect(hostComponentStateEntity3.getComponentType()).andReturn("component3").anyTimes();

    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();
    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component1", "component1")).andReturn(serviceComponentDesiredStateEntity).anyTimes();
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity2 = createMock(ServiceComponentDesiredStateEntity.class);

    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component2", "component2")).andReturn(serviceComponentDesiredStateEntity2).anyTimes();
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity3 = createMock(ServiceComponentDesiredStateEntity.class);
    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component3", "component3")).andReturn(serviceComponentDesiredStateEntity3).anyTimes();
    expect(serviceComponentDesiredStateEntity.getId()).andReturn(1L).times(2);

    // replay mocks
    replay(stateHelper, injector, clusters, cluster, host, stack,
        ambariMetaInfo, service, component1, component2, component3, componentHost1,
        componentHost2, response1, response2, hostComponentStateDAO, serviceComponentDesiredStateDAO,
            hostComponentStateEntity, hostComponentStateEntity2, hostComponentStateEntity3);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component1, component2, component3,
        componentHost1, componentHost2, response1, response2, hostComponentStateDAO, serviceComponentDesiredStateDAO,
            hostComponentStateEntity, hostComponentStateEntity2, hostComponentStateEntity3);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ServiceNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component1 = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    final ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(
        anyObject(ServiceComponentHost.class),
        anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 1L, "component1", "component1", "host1", null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service2", 2L, "component2", "component2", "host1", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 3L, "component3", "component3", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, null, maintHelper, createNiceMock(KerberosHelper.class),
        null, null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster)).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
        new HashMap<String, Host>() {{
          put("host1", host);
        }}).anyTimes();

//    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getServicesByServiceGroup("CORE")).andReturn(ImmutableList.of(service)).anyTimes();
    expect(cluster.getService("CORE", "service1")).andReturn(service).anyTimes();
    expect(cluster.getServiceByComponentName("component1")).andReturn(service).anyTimes();
    expect(service.getServiceComponents()).andReturn(ImmutableMap.of("component1", component1, "component2", component2, "component3", component3)).anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component1).anyTimes();
    expect(component1.getServiceComponentHosts()).andReturn(ImmutableMap.of("host1", componentHost1));
    expect(componentHost1.convertToResponse(null)).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

    expect(cluster.getService("CORE", "service2")).andThrow(new ServiceNotFoundException("cluster1", "service2"));

    expect(service.getName()).andReturn("service1").anyTimes();
    expect(cluster.getServiceByComponentName("component3")).andReturn(service).anyTimes();
    expect(service.getServiceComponent("component3")).andReturn(component3).anyTimes();
    expect(component3.getServiceComponentHosts()).andReturn(ImmutableMap.of("host1", componentHost2));
    expect(componentHost2.convertToResponse(null)).andReturn(response2);
    expect(componentHost2.getHostName()).andReturn("host1");

    HostComponentStateEntity hostComponentStateEntity2 = createNiceMock(HostComponentStateEntity.class);
    HostComponentStateEntity hostComponentStateEntity3 = createNiceMock(HostComponentStateEntity.class);

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(hostComponentStateDAO.findById(1L)).andReturn(hostComponentStateEntity).anyTimes();
    expect(hostComponentStateDAO.findById(2L)).andReturn(hostComponentStateEntity2).anyTimes();
    expect(hostComponentStateDAO.findById(3L)).andReturn(hostComponentStateEntity3).anyTimes();

    expect(hostComponentStateEntity.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getComponentName()).andReturn("component1").anyTimes();
    expect(hostComponentStateEntity.getComponentType()).andReturn("component1").anyTimes();

    expect(hostComponentStateEntity2.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getServiceId()).andReturn(2L).anyTimes();
    expect(hostComponentStateEntity2.getComponentName()).andReturn("component2").anyTimes();
    expect(hostComponentStateEntity2.getComponentType()).andReturn("component2").anyTimes();

    expect(hostComponentStateEntity3.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getComponentName()).andReturn("component3").anyTimes();
    expect(hostComponentStateEntity3.getComponentType()).andReturn("component3").anyTimes();

    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();
    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component1", "component1")).andReturn(serviceComponentDesiredStateEntity).anyTimes();
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity2 = createMock(ServiceComponentDesiredStateEntity.class);

    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 2L,
            "component2", "component2")).andReturn(serviceComponentDesiredStateEntity2).anyTimes();
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity3 = createMock(ServiceComponentDesiredStateEntity.class);
    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component3", "component3")).andReturn(serviceComponentDesiredStateEntity3).anyTimes();
    expect(serviceComponentDesiredStateEntity.getId()).andReturn(1L).times(2);

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
        service, component1, component2, component3, componentHost1,
        componentHost2, response1, response2, hostComponentStateDAO, serviceComponentDesiredStateDAO,
            hostComponentStateEntity, hostComponentStateEntity2, hostComponentStateEntity3);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component1, component2, component3,
        componentHost1, componentHost2, response1, response2, hostComponentStateDAO, serviceComponentDesiredStateDAO,
            hostComponentStateEntity, hostComponentStateEntity2, hostComponentStateEntity3);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ServiceComponentNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    final ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);

    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(
        anyObject(ServiceComponentHost.class),
        anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 1L, "component1", "component1", "host1", null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service2", 2L, "component2", "component2", "host1", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 3L, "component3", "component3", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, null, maintHelper, createNiceMock(KerberosHelper.class),
        null, null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster)).anyTimes();
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(ImmutableMap.<String, Host>builder()
        .put("host1", host)
        .build()).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();


//    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getServicesByServiceGroup("CORE")).andReturn(ImmutableList.of(service, service2)).anyTimes();
    expect(cluster.getService("CORE", "service1")).andReturn(service).anyTimes();
    expect(cluster.getServiceByComponentName("component1")).andReturn(service).anyTimes();
    expect(service.getServiceComponents()).andReturn(ImmutableMap.of("component1", component, "component3", component3)).anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component).anyTimes();
    expect(component.getServiceComponentHosts()).andReturn(ImmutableMap.<String, ServiceComponentHost>builder()
        .put("host1", componentHost1)
        .build());
    expect(componentHost1.convertToResponse(null)).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

//    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component2")).andReturn("service2");
    expect(cluster.getService("CORE", "service2")).andReturn(service2);
    expect(cluster.getServiceByComponentName("component2")).andReturn(service2).anyTimes();
    expect(service2.getServiceComponents()).andReturn(ImmutableMap.of()).anyTimes();
    expect(service2.getServiceComponent("component2")).
        andThrow(new ServiceComponentNotFoundException("cluster1", "service2", "service2", "CORE", "component2")).anyTimes();

//    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component3")).andReturn("service1");
    expect(cluster.getServiceByComponentName("component3")).andReturn(service).anyTimes();
    expect(service.getServiceComponent("component3")).andReturn(component3);

    expect(component3.getServiceComponentHosts()).andReturn(ImmutableMap.<String, ServiceComponentHost>builder()
        .put("host1", componentHost2)
        .build());
    expect(componentHost2.convertToResponse(null)).andReturn(response2);
    expect(componentHost2.getHostName()).andReturn("host1");

    HostComponentStateEntity hostComponentStateEntity2 = createNiceMock(HostComponentStateEntity.class);
    HostComponentStateEntity hostComponentStateEntity3 = createNiceMock(HostComponentStateEntity.class);

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(hostComponentStateDAO.findById(1L)).andReturn(hostComponentStateEntity).anyTimes();
    expect(hostComponentStateDAO.findById(2L)).andReturn(hostComponentStateEntity2).anyTimes();
    expect(hostComponentStateDAO.findById(3L)).andReturn(hostComponentStateEntity3).anyTimes();

    expect(hostComponentStateEntity.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getComponentName()).andReturn("component1").anyTimes();
    expect(hostComponentStateEntity.getComponentType()).andReturn("component1").anyTimes();

    expect(hostComponentStateEntity2.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getComponentName()).andReturn("component2").anyTimes();
    expect(hostComponentStateEntity2.getComponentType()).andReturn("component2").anyTimes();

    expect(hostComponentStateEntity3.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getComponentName()).andReturn("component3").anyTimes();
    expect(hostComponentStateEntity3.getComponentType()).andReturn("component3").anyTimes();

    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();
    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component1", "component1")).andReturn(serviceComponentDesiredStateEntity).anyTimes();
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity2 = createMock(ServiceComponentDesiredStateEntity.class);

    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component2", "component2")).andReturn(serviceComponentDesiredStateEntity2).anyTimes();
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity3 = createMock(ServiceComponentDesiredStateEntity.class);
    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component3", "component3")).andReturn(serviceComponentDesiredStateEntity3).anyTimes();
    expect(serviceComponentDesiredStateEntity.getId()).andReturn(1L).times(2);

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
        service, service2, component, component2, component3, componentHost1,
        componentHost2, response1, response2, hostComponentStateDAO, serviceComponentDesiredStateDAO,
            hostComponentStateEntity, hostComponentStateEntity2, hostComponentStateEntity3);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, service2, component, component2, component3,
        componentHost1, componentHost2, response1, response2, hostComponentStateDAO, serviceComponentDesiredStateDAO,
            hostComponentStateEntity, hostComponentStateEntity2, hostComponentStateEntity3);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_HostNotFoundException_hostProvidedInQuery() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    final ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(
        anyObject(ServiceComponentHost.class),
        anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 1L, "component1", "component1", null, null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 2L, "component2", "component2", "host2", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", 3L, "component3", "component3", null, null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, null, maintHelper, createNiceMock(KerberosHelper.class),
        null, null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
        new HashMap<String, Host>() {{
          put("host1", host);
        }}).anyTimes();

    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();

    expect(cluster.getServicesByServiceGroup("CORE")).andReturn(ImmutableList.of(service)).anyTimes();
    expect(cluster.getService("CORE", "service1")).andReturn(service).anyTimes();
    expect(cluster.getServiceByComponentName("component1")).andReturn(service).anyTimes();
    expect(service.getServiceComponents()).andReturn(ImmutableMap.of("component1", component, "component3", component3)).anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component).anyTimes();
    expect(service.getName()).andReturn("service1").anyTimes();
    expect(component.getServiceComponentHosts()).andReturn(Collections.singletonMap("foo", componentHost1));
    expect(componentHost1.convertToResponse(null)).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

    expect(clusters.getClustersForHost("host2")).andThrow(new HostNotFoundException("host2"));

    expect(cluster.getServiceByComponentName("component3")).andReturn(service).anyTimes();
    expect(service.getServiceComponent("component3")).andReturn(component3).anyTimes();
    expect(component3.getServiceComponentHosts()).andReturn(Collections.singletonMap("foo", componentHost2));
    expect(componentHost2.convertToResponse(null)).andReturn(response2);
    expect(componentHost2.getHostName()).andReturn("host1");

    HostComponentStateEntity hostComponentStateEntity2 = createNiceMock(HostComponentStateEntity.class);
    HostComponentStateEntity hostComponentStateEntity3 = createNiceMock(HostComponentStateEntity.class);

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(hostComponentStateDAO.findById(1L)).andReturn(hostComponentStateEntity).anyTimes();
    expect(hostComponentStateDAO.findById(2L)).andReturn(hostComponentStateEntity2).anyTimes();
    expect(hostComponentStateDAO.findById(3L)).andReturn(hostComponentStateEntity3).anyTimes();

    expect(hostComponentStateEntity.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity.getComponentName()).andReturn("component1").anyTimes();
    expect(hostComponentStateEntity.getComponentType()).andReturn("component1").anyTimes();

    expect(hostComponentStateEntity2.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity2.getComponentName()).andReturn("component2").anyTimes();
    expect(hostComponentStateEntity2.getComponentType()).andReturn("component2").anyTimes();

    expect(hostComponentStateEntity3.getClusterId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getServiceGroupId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getServiceId()).andReturn(1L).anyTimes();
    expect(hostComponentStateEntity3.getComponentName()).andReturn("component3").anyTimes();
    expect(hostComponentStateEntity3.getComponentType()).andReturn("component3").anyTimes();

    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();
    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component1", "component1")).andReturn(serviceComponentDesiredStateEntity).anyTimes();
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity2 = createMock(ServiceComponentDesiredStateEntity.class);

    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component2", "component2")).andReturn(serviceComponentDesiredStateEntity2).anyTimes();
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity3 = createMock(ServiceComponentDesiredStateEntity.class);
    expect(serviceComponentDesiredStateDAO.findByName(1L, 1L, 1L,
            "component3", "component3")).andReturn(serviceComponentDesiredStateEntity3).anyTimes();
    expect(serviceComponentDesiredStateEntity.getId()).andReturn(1L).times(2);

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
        service, component, component2, component3, componentHost1,
        componentHost2, response1, response2, hostComponentStateDAO, serviceComponentDesiredStateDAO,
            hostComponentStateEntity, hostComponentStateEntity2, hostComponentStateEntity3);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);
    Assert.assertNotNull(setResponses);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component, component2, component3,
        componentHost1, componentHost2, response1, response2, hostComponentStateDAO, serviceComponentDesiredStateDAO,
            hostComponentStateEntity, hostComponentStateEntity2, hostComponentStateEntity3);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_HostNotFoundException_hostProvidedInURL() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    Cluster cluster = createNiceMock(Cluster.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", "component1", "component1", "host1", null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", "component2", "component2", "host1", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", "component3", "component3", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, null, maintHelper, createNiceMock(KerberosHelper.class),
        null, null);

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andThrow(new HostNotFoundException("host1"));

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, stack, ambariMetaInfo, hostComponentStateDAO, serviceComponentDesiredStateDAO);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    try {
      controller.getHostComponents(setRequests);
      fail("expected exception");
    } catch (AmbariException e) {
      // expected
    }

    // assert and verify
    assertSame(controller, controllerCapture.getValue());

    verify(injector, clusters, cluster, stack, ambariMetaInfo, hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ClusterNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", "component1", "component1", "host1", null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", "component2", "component2", "host2", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", "component3", "component3", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, null, maintHelper, createNiceMock(KerberosHelper.class),
        null, null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andThrow(new ClusterNotFoundException("cluster1"));

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // replay mocks
    replay(maintHelper, injector, clusters, stack, ambariMetaInfo);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    try {
      controller.getHostComponents(setRequests);
      fail("expected exception");
    } catch (ParentObjectNotFoundException e) {
      //expected
    }

    // assert and verify
    assertSame(controller, controllerCapture.getValue());

    verify(injector, clusters, stack, ambariMetaInfo);
  }

  @Test
  public void testGetHostComponents___NullHostName() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(
        anyObject(ServiceComponentHost.class),
        anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", "service1", "component1", "component1", null, null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    Map<String, ServiceComponentHost> mapHostComponents = new HashMap<>();
    mapHostComponents.put("foo", componentHost1);
    mapHostComponents.put("bar", componentHost2);


    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, null, maintHelper, createNiceMock(KerberosHelper.class),
        null, null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
        new HashMap<String, Host>() {{
          put("host1", createNiceMock(Host.class));
        }}).anyTimes();

    expect(cluster.getServicesByServiceGroup("CORE")).andReturn(ImmutableList.of(service)).anyTimes();
    expect(cluster.getService("CORE", "service1")).andReturn(service).anyTimes();
    expect(service.getName()).andReturn("service1").anyTimes();
    expect(component.getName()).andReturn("component1").anyTimes();
    expect(cluster.getServiceByComponentName("component1")).andReturn(service).anyTimes();
    expect(service.getServiceComponents()).andReturn(ImmutableMap.of("component1", component)).anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component).anyTimes();
    expect(component.getServiceComponentHosts()).andReturn(mapHostComponents);
    expect(componentHost1.convertToResponse(null)).andReturn(response1);
    expect(componentHost2.convertToResponse(null)).andReturn(response2);
    expect(componentHost1.getHostName()).andReturn("host1");
    expect(componentHost2.getHostName()).andReturn("host1");

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, response1, response2,
        stack, ambariMetaInfo, service, component, componentHost1, componentHost2,
            hostComponentStateDAO, serviceComponentDesiredStateDAO);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, response1, response2, stack, ambariMetaInfo, service, component, componentHost1,
            componentHost2, hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  @Test
  public void testGetHostComponents___NullHostName_NullComponentName() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service1 = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    ServiceComponent component1 = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost3 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response3 = createNiceMock(ServiceComponentHostResponse.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(
        anyObject(ServiceComponentHost.class),
        anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", "CORE", null, null, null, null, null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    Map<String, Service> mapServices = new HashMap<>();
    mapServices.put("foo", service1);
    mapServices.put("bar", service2);

    Map<String, ServiceComponentHost> mapHostComponents = new HashMap<>();
    mapHostComponents.put("foo", componentHost1);
    mapHostComponents.put("bar", componentHost2);


    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, null, maintHelper, createNiceMock(KerberosHelper.class),
        null, null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHostsForCluster("cluster1")).andReturn(ImmutableMap.of("host1", createNiceMock(Host.class))).anyTimes();

    expect(cluster.getClusterName()).andReturn("cluster1").anyTimes();
    expect(cluster.getServicesByServiceGroup("CORE")).andReturn(ImmutableList.of(service1, service2)).anyTimes();
    expect(service1.getServiceComponents()).andReturn(Collections.singletonMap("foo", component1));
    expect(service2.getServiceComponents()).andReturn(Collections.singletonMap("bar", component2));

    expect(component1.getName()).andReturn("component1").anyTimes();
    expect(component2.getName()).andReturn("component2").anyTimes();

    expect(component1.getServiceComponentHosts()).andReturn(mapHostComponents);
    expect(componentHost1.convertToResponse(null)).andReturn(response1);
    expect(componentHost2.convertToResponse(null)).andReturn(response2);
    expect(componentHost1.getHostName()).andReturn("host1");
    expect(componentHost2.getHostName()).andReturn("host1");
    expect(componentHost3.getHostName()).andReturn("host1");

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    expect(component2.getServiceComponentHosts()).andReturn(Collections.singletonMap("foobar", componentHost3));
    expect(componentHost3.convertToResponse(null)).andReturn(response3);

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, response1, response2,
        response3, stack, ambariMetaInfo, service1, service2, component1, component2,
        componentHost1, componentHost2, componentHost3, hostComponentStateDAO, serviceComponentDesiredStateDAO);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(3, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));
    assertTrue(setResponses.contains(response3));

    verify(injector, clusters, cluster, response1, response2, response3, stack, ambariMetaInfo, service1, service2,
        component1, component2, componentHost1, componentHost2, componentHost3, hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  @Test
  public void testPopulatePackagesInfo() throws Exception {
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    StackInfo stackInfo = createNiceMock(StackInfo.class);
    Map<String, String> hostParams = new HashMap<>();
    String osFamily = "testOSFamily";

    Map<String, OsSpecific> osSpecificsService = new HashMap<>();
    Map<String, OsSpecific> osSpecificsStack = new HashMap<>();

    OsSpecific.Package package1 = new OsSpecific.Package();
    package1.setName("testrpm1");
    OsSpecific.Package package2 = new OsSpecific.Package();
    package2.setName("testrpm2");
    OsSpecific.Package package3 = new OsSpecific.Package();
    package3.setName("testrpm3");
    OsSpecific.Package packageStack = new OsSpecific.Package();
    package3.setName("testrpmStack");

    List<OsSpecific.Package> packageList1 = new ArrayList<>();
    packageList1.add(package1);
    List<OsSpecific.Package> packageList2 = new ArrayList<>();
    packageList2.add(package2);
    packageList2.add(package3);
    List<OsSpecific.Package> packageListStack = new ArrayList<>();
    packageListStack.add(packageStack);

    OsSpecific osSpecific1 = new OsSpecific("testOSFamily");
    osSpecific1.addPackages(packageList1);
    OsSpecific osSpecific2 = new OsSpecific("testOSFamily1,testOSFamily,testOSFamily2");
    osSpecific2.addPackages(packageList2);
    OsSpecific osSpecificStack = new OsSpecific("testOSFamily");
    osSpecificStack.addPackages(packageListStack);

    osSpecificsService.put("testOSFamily", osSpecific1);
    osSpecificsService.put("testOSFamily1,testOSFamily,testOSFamily2", osSpecific2);
    osSpecificsStack.put("testOSFamily", osSpecificStack);

    expect(serviceInfo.getOsSpecifics()).andReturn(osSpecificsService);
    expect(stackInfo.getOsSpecifics()).andReturn(osSpecificsStack);

    constructorInit(injector, controllerCapture, null, maintHelper, createNiceMock(KerberosHelper.class),
        null, null);

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    OsFamily osFamilyMock = createNiceMock(OsFamily.class);

    EasyMock.expect(osFamilyMock.isVersionedOsFamilyExtendedByVersionedFamily("testOSFamily", "testOSFamily")).andReturn(true).times(3);
    replay(maintHelper, injector, clusters, stackInfo, serviceInfo, osFamilyMock, hostComponentStateDAO, serviceComponentDesiredStateDAO);

    AmbariManagementControllerImplTest.NestedTestClass nestedTestClass = this.new NestedTestClass(null, clusters,
        injector, osFamilyMock);

    OsSpecific osSpecific = nestedTestClass.populatePackagesInfo(stackInfo.getOsSpecifics(), hostParams, osFamily);

    assertEquals(1, osSpecific.getPackages().size());

    osSpecific = nestedTestClass.populatePackagesInfo(serviceInfo.getOsSpecifics(), hostParams, osFamily);

    assertEquals(3, osSpecific.getPackages().size());
  }

  @Test
  public void testSynchronizeLdapUsersAndGroups() throws Exception {

    Set<String> userSet = new HashSet<>();
    userSet.add("user1");

    Set<String> groupSet = new HashSet<>();
    groupSet.add("group1");

    Injector injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule()).with(new MockModule()));

    // create mocks

    LdapBatchDto ldapBatchDto = createNiceMock(LdapBatchDto.class);


    Capture<LdapBatchDto> ldapBatchDtoCapture = EasyMock.newCapture();

    // set expectations
    expect(ldapDataPopulator.synchronizeAllLdapUsers(capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);
    expect(ldapDataPopulator.synchronizeAllLdapGroups(capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);

    expect(ldapDataPopulator.synchronizeExistingLdapUsers(capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);
    expect(ldapDataPopulator.synchronizeExistingLdapGroups(capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);

    expect(ldapDataPopulator.synchronizeLdapUsers(eq(userSet), capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);
    expect(ldapDataPopulator.synchronizeLdapGroups(eq(groupSet), capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);

    users.processLdapSync(capture(ldapBatchDtoCapture));
    expectLastCall().anyTimes();

    //replay
    replay(ldapDataPopulator, clusters, actionDBAccessor, ambariMetaInfo, users, ldapBatchDto,
            hostComponentStateDAO, serviceComponentDesiredStateDAO);

    AmbariManagementControllerImpl controller = injector.getInstance(AmbariManagementControllerImpl.class);

    LdapSyncRequest userRequest = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.ALL);
    LdapSyncRequest groupRequest = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.ALL);

    controller.synchronizeLdapUsersAndGroups(userRequest, groupRequest);

    userRequest = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.EXISTING);
    groupRequest = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.EXISTING);

    controller.synchronizeLdapUsersAndGroups(userRequest, groupRequest);

    userRequest = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.SPECIFIC, userSet);
    groupRequest = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.SPECIFIC, groupSet);

    controller.synchronizeLdapUsersAndGroups(userRequest, groupRequest);

    verify(ldapDataPopulator, clusters, users, ldapBatchDto, hostComponentStateDAO, serviceComponentDesiredStateDAO);
  }

  private void setAmbariMetaInfo(AmbariMetaInfo metaInfo, AmbariManagementController controller) throws NoSuchFieldException, IllegalAccessException {
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);
  }

  private class MockModule implements com.google.inject.Module {

    @Override
    public void configure(Binder binder) {
      binder.bind(AmbariLdapDataPopulator.class).toInstance(ldapDataPopulator);
      binder.bind(Clusters.class).toInstance(clusters);
      binder.bind(ActionDBAccessorImpl.class).toInstance(actionDBAccessor);
      binder.bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
      binder.bind(Users.class).toInstance(users);
      binder.bind(AmbariSessionManager.class).toInstance(sessionManager);
      binder.bind(RegistryManager.class).toInstance(registryManager);
      binder.bind(HostComponentStateEntity.class).toInstance(hostComponentStateEntity);
      binder.bind(HostComponentStateDAO.class).toInstance(hostComponentStateDAO);
      binder.bind(ServiceComponentDesiredStateEntity.class).toInstance(serviceComponentDesiredStateEntity);
      binder.bind(ServiceComponentDesiredStateDAO.class).toInstance(serviceComponentDesiredStateDAO);
    }
  }

  private class NestedTestClass extends AmbariManagementControllerImpl {

    public NestedTestClass(ActionManager actionManager, Clusters clusters, Injector injector, OsFamily osFamilyMock) throws Exception {
      super(actionManager, clusters, metadataGenerator, injector);
      osFamily = osFamilyMock;
    }

//    public ServiceOsSpecific testPopulateServicePackagesInfo(ServiceInfo serviceInfo, Map<String, String> hostParams,
//                                                             String osFamily) {
//      return super.populateServicePackagesInfo(serviceInfo, hostParams, osFamily);
//    }

  }

  @Test
  public void testRegisterRackChange() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = createNiceMock(ServiceComponentHost.class);
    StackId stackId = createNiceMock(StackId.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();

    // expectations
    // constructor init
    constructorInit(injector, controllerCapture, createNiceMock(KerberosHelper.class));

    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();

    RepositoryInfo dummyRepoInfo = new RepositoryInfo();
    dummyRepoInfo.setRepoName("repo_name");

    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();
    expect(service.getName()).andReturn("HDFS").anyTimes();

    Map<String, ServiceComponent> serviceComponents = new HashMap<>();
    serviceComponents.put("NAMENODE", serviceComponent);
    expect(service.getServiceComponents()).andReturn(serviceComponents).anyTimes();


    Map<String, ServiceComponentHost> schMap = new HashMap<>();
    schMap.put("host1", serviceComponentHost);
    expect(serviceComponent.getServiceComponentHosts()).andReturn(schMap).anyTimes();

    serviceComponentHost.setRestartRequired(true);

    ServiceInfo serviceInfo = new ServiceInfo();
    serviceInfo.setRestartRequiredAfterRackChange(true);
    expect(ambariMetaInfo.getService(service)).andReturn(serviceInfo);

    expect(cluster.getServices()).andReturn(ImmutableSet.of(service)).anyTimes();

    // replay mocks
    replay(injector, cluster, clusters, ambariMetaInfo, service, serviceComponent, serviceComponentHost, stackId);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    controller.registerRackChange("c1");

    verify(injector, cluster, clusters, ambariMetaInfo, service, serviceComponent, serviceComponentHost, stackId);
  }

  @Test
  public void testRegisterMpacks() throws Exception{
    MpackRequest mpackRequest = createNiceMock(MpackRequest.class);
    RequestStatusResponse response = new RequestStatusResponse(new Long(201));
    Mpack mpack = new Mpack();
    mpack.setResourceId((long)100);
    mpack.setModules(new ArrayList<Module>());
    mpack.setPrerequisites(new HashMap<String, String>());
    mpack.setRegistryId(new Long(100));
    mpack.setVersion("3.0");
    mpack.setMpackUri("abc.tar.gz");
    mpack.setDescription("Test mpack");
    mpack.setName("testMpack");
    MpackResponse mpackResponse = new MpackResponse(mpack);
    Injector injector = createNiceMock(Injector.class);
    AgentResource.init(createNiceMock(HeartBeatHandler.class));
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).atLeastOnce();
    expect(clusters.getHosts()).andReturn(Collections.emptyList()).anyTimes();
    expect(ambariMetaInfo.registerMpack(mpackRequest)).andReturn(mpackResponse);
    ambariMetaInfo.init();
    expectLastCall();
    replay(ambariMetaInfo, injector);
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);
    Assert.assertEquals(mpackResponse, controller.registerMpack(mpackRequest));
  }

  @Test
  public void testGetPacklets() throws Exception {
    Long mpackId = new Long(100);
    ArrayList<Module> packletArrayList = new ArrayList<>();
    Module samplePacklet = new Module();
    Injector injector = createNiceMock(Injector.class);
    //samplePacklet.setType(Packlet.PackletType.SERVICE_PACKLET);
    samplePacklet.setVersion("3.0.0");
    samplePacklet.setName("NIFI");
    samplePacklet.setDefinition("nifi.tar.gz");
    packletArrayList.add(samplePacklet);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).atLeastOnce();
    expect(injector.getInstance(HostComponentStateDAO.class)).andReturn(hostComponentStateDAO).anyTimes();
    expect(injector.getInstance(ServiceComponentDesiredStateDAO.class)).andReturn(serviceComponentDesiredStateDAO).anyTimes();
    expect(ambariMetaInfo.getModules(mpackId)).andReturn(packletArrayList).atLeastOnce();
    replay(ambariMetaInfo, injector);
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, metadataGenerator, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Assert.assertEquals(packletArrayList, controller.getModules(mpackId));
  }

  public static void constructorInit(Injector injector, Capture<AmbariManagementController> controllerCapture, Gson gson,
    MaintenanceStateHelper maintenanceStateHelper, KerberosHelper kerberosHelper,
    MetadataHolder metadataHolder, AgentConfigsHolder agentConfigsHolder
  ) {
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(gson);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintenanceStateHelper);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(kerberosHelper);
    expect(injector.getProvider(MetadataHolder.class)).andReturn(() -> metadataHolder);
    expect(injector.getProvider(AgentConfigsHolder.class)).andReturn(() -> agentConfigsHolder);
  }

  public static void constructorInit(Injector injector, Capture<AmbariManagementController> controllerCapture,
                               KerberosHelper kerberosHelper) {
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(kerberosHelper);
    expect(injector.getProvider(MetadataHolder.class)).andReturn(() -> null);
    expect(injector.getProvider(AgentConfigsHolder.class)).andReturn(() -> null);
  }
}
