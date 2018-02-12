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
package org.apache.ambari.server.events.listeners.upgrade;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.reset;

import java.lang.reflect.Field;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.HostComponentVersionAdvertisedEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeState;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Provider;

import junit.framework.Assert;

/**
 * StackVersionListener tests.
 */
@RunWith(EasyMockRunner.class)
public class StackVersionListenerTest extends EasyMockSupport {

  private static final String INVALID_NEW_VERSION = "1.2.3.4-5678";
  private static final String VALID_NEW_VERSION = "2.4.0.0-1000";
  private static final String SERVICE_COMPONENT_NAME = "Some component name";
  private static final String SERVICE_NAME = "Service name";
  private static final Long CLUSTER_ID = 1L;
  private static final String UNKNOWN_VERSION = "UNKNOWN";
  private static final String VALID_PREVIOUS_VERSION = "2.2.0.0";
  private static final HostVersionEntity DUMMY_HOST_VERSION_ENTITY = new HostVersionEntity();
  private static final UpgradeEntity DUMMY_UPGRADE_ENTITY = new UpgradeEntity();
  public static final String STACK_NAME = "HDP";
  public static final String STACK_VERSION = "2.4";

  private Cluster cluster;
  private ServiceComponentHost sch;
  private Service service;
  private ServiceComponent serviceComponent;
  private VersionEventPublisher publisher = new VersionEventPublisher();
  private StackId stackId = new StackId(STACK_NAME, STACK_VERSION);

  @TestSubject
  private StackVersionListener listener = new StackVersionListener(publisher);

  @Mock
  private Provider<AmbariMetaInfo> ambariMetaInfoProvider;

  @Mock
  private ComponentInfo componentInfo;

  @Mock
  private AmbariMetaInfo ambariMetaInfo;

  @Before
  public void setup() throws Exception {
    cluster = createNiceMock(Cluster.class);
    sch = createNiceMock(ServiceComponentHost.class);
    service = createNiceMock(Service.class);
    serviceComponent = createNiceMock(ServiceComponent.class);
    componentInfo = createNiceMock(ComponentInfo.class);

    expect(cluster.getClusterId()).andReturn(CLUSTER_ID);

    expect(cluster.getService(SERVICE_NAME)).andReturn(service).anyTimes();
    expect(service.getServiceComponent(SERVICE_COMPONENT_NAME)).andReturn(
        serviceComponent).anyTimes();
    expect(sch.getDesiredStackId()).andReturn(stackId).atLeastOnce();
    expect(sch.getServiceName()).andReturn(SERVICE_NAME).anyTimes();
    expect(sch.getServiceType()).andReturn(SERVICE_NAME).atLeastOnce();
    expect(sch.getServiceComponentName()).andReturn(SERVICE_COMPONENT_NAME).atLeastOnce();

    expect(ambariMetaInfoProvider.get()).andReturn(ambariMetaInfo).atLeastOnce();
    expect(ambariMetaInfo.getComponent(STACK_NAME, STACK_VERSION, SERVICE_NAME,
        SERVICE_COMPONENT_NAME)).andReturn(componentInfo).atLeastOnce();

    injectMocks(listener);
  }

  @Test
  public void testRecalculateHostVersionStateWhenVersionIsNullAndNewVersionIsNotBlank() throws AmbariException {
    expect(sch.getVersion()).andReturn(null);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setVersion(INVALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(null).once();

    replayAll();

    sendEventAndVerify(INVALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateHostVersionStateWhenVersionIsUnknownAndNewVersionIsNotBlank() throws AmbariException {
    expect(sch.getVersion()).andReturn(UNKNOWN_VERSION);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setVersion(INVALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(null).once();

    replayAll();

    sendEventAndVerify(INVALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateClusterVersionStateWhenVersionIsNullAndNewVersionIsValid() throws AmbariException {
    expect(sch.getVersion()).andReturn(null);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setVersion(VALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(DUMMY_HOST_VERSION_ENTITY).once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateClusterVersionStateWhenVersionIsUnknownAndNewVersionIsValid() throws AmbariException {
    expect(sch.getVersion()).andReturn(UNKNOWN_VERSION);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setVersion(VALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(DUMMY_HOST_VERSION_ENTITY).once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateHostVersionStateWhenComponentDesiredVersionIsUnknownAndNewVersionIsNotValid() throws AmbariException {
    expect(serviceComponent.getDesiredVersion()).andReturn(UNKNOWN_VERSION);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setUpgradeState(UpgradeState.NONE);
    expectLastCall().once();
    sch.setVersion(INVALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(null).once();

    replayAll();

    sendEventAndVerify(INVALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateClusterVersionStateWhenComponentDesiredVersionIsUnknownAndNewVersionIsValid() throws AmbariException {
    expect(serviceComponent.getDesiredVersion()).andReturn(UNKNOWN_VERSION);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setUpgradeState(UpgradeState.NONE);
    expectLastCall().once();
    sch.setVersion(VALID_NEW_VERSION);
    expectLastCall().once();
    expect(sch.recalculateHostVersionState()).andReturn(DUMMY_HOST_VERSION_ENTITY).once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testRecalculateClusterVersionStateWhenVersionNotAdvertised() throws AmbariException {
    expect(componentInfo.isVersionAdvertised()).andReturn(false).once();
    replayAll();
    sendEventAndVerify(VALID_NEW_VERSION);
  }


  @Test
  public void testNoActionTakenOnNullVersion() {
    reset(ambariMetaInfoProvider, ambariMetaInfo, service, serviceComponent, sch, componentInfo);
    replayAll();

    sendEventAndVerify(null);
  }

  @Test
  public void testSetUpgradeStateToCompleteWhenUpgradeIsInProgressAndNewVersionIsEqualToComponentDesiredVersion() {
    expect(cluster.getUpgradeInProgress()).andReturn(DUMMY_UPGRADE_ENTITY);

    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(sch.getUpgradeState()).andReturn(UpgradeState.IN_PROGRESS);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setUpgradeState(UpgradeState.COMPLETE);
    expectLastCall().once();

    expect(serviceComponent.getDesiredVersion()).andStubReturn(VALID_NEW_VERSION);
    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetUpgradeStateToNoneWhenNoUpgradeAndNewVersionIsEqualToComponentDesiredVersion() {
    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(sch.getUpgradeState()).andReturn(UpgradeState.IN_PROGRESS);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setUpgradeState(UpgradeState.NONE);
    expectLastCall().once();

    expect(serviceComponent.getDesiredVersion()).andStubReturn(VALID_NEW_VERSION);
    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetUpgradeStateToVersionMismatchWhenUpgradeIsInProgressAndNewVersionIsNotEqualToComponentDesiredVersion() {
    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(sch.getUpgradeState()).andReturn(UpgradeState.IN_PROGRESS);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setUpgradeState(UpgradeState.VERSION_MISMATCH);
    expectLastCall().once();

    sch.setVersion(VALID_NEW_VERSION);
    expectLastCall().once();

    expect(serviceComponent.getDesiredVersion()).andStubReturn(VALID_PREVIOUS_VERSION);
    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetUpgradeStateToCompleteWhenHostHasVersionMismatchAndNewVersionIsEqualToComponentDesiredVersionAndClusterUpgradeIsInProgress() {
    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(sch.getUpgradeState()).andReturn(UpgradeState.VERSION_MISMATCH);
    expect(cluster.getUpgradeInProgress()).andReturn(DUMMY_UPGRADE_ENTITY);
    expect(serviceComponent.getDesiredVersion()).andStubReturn(VALID_NEW_VERSION);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setUpgradeState(UpgradeState.COMPLETE);
    expectLastCall().once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetUpgradeStateToNoneWhenHostHasVersionMismatchAndNewVersionIsEqualToComponentDesiredVersion() {
    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(sch.getUpgradeState()).andReturn(UpgradeState.VERSION_MISMATCH);
    expect(serviceComponent.getDesiredVersion()).andStubReturn(VALID_NEW_VERSION);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setUpgradeState(UpgradeState.NONE);
    expectLastCall().once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetUpgradeStateToVersionMismatchByDefaultWhenHostAndNewVersionsAreValid() {
    expect(sch.getVersion()).andReturn(VALID_PREVIOUS_VERSION);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();
    sch.setUpgradeState(UpgradeState.VERSION_MISMATCH);
    expectLastCall().once();

    replayAll();

    sendEventAndVerify(VALID_NEW_VERSION);
  }

  @Test
  public void testSetRepositoryVersion() throws Exception {
    expect(sch.getVersion()).andReturn(UNKNOWN_VERSION);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();

    RepositoryVersionDAO dao = createNiceMock(RepositoryVersionDAO.class);
    RepositoryVersionEntity entity = createNiceMock(RepositoryVersionEntity.class);
    expect(entity.getVersion()).andReturn("2.4.0.0").once();

    // when the version gets reported back, we set this repo to resolved
    entity.setResolved(true);
    expectLastCall().once();

    expect(dao.findByPK(1L)).andReturn(entity).once();
    expect(dao.merge(entity)).andReturn(entity).once();

    replayAll();

    String newVersion = VALID_NEW_VERSION;

    HostComponentVersionAdvertisedEvent event = new HostComponentVersionAdvertisedEvent(cluster, sch, newVersion, 1L);
    // !!! avoid injector for test class
    Field field = StackVersionListener.class.getDeclaredField("repositoryVersionDAO");
    field.setAccessible(true);
    field.set(listener, dao);

    listener.onAmbariEvent(event);

    verifyAll();
  }

  /**
   * Tests that if a component advertises a version and the repository already
   * matches, that we ensure that it is marked as resolved.
   *
   * @throws Exception
   */
  @Test
  public void testRepositoryResolvedWhenVersionsMatch() throws Exception {
    String version = "2.4.0.0";

    expect(sch.getVersion()).andReturn(version);
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();

    RepositoryVersionDAO dao = createNiceMock(RepositoryVersionDAO.class);
    RepositoryVersionEntity entity = createNiceMock(RepositoryVersionEntity.class);
    expect(entity.getVersion()).andReturn(version).once();
    expect(entity.isResolved()).andReturn(false).once();

    // when the version gets reported back, we set this repo to resolved
    entity.setResolved(true);
    expectLastCall().once();

    expect(dao.findByPK(1L)).andReturn(entity).once();
    expect(dao.merge(entity)).andReturn(entity).once();

    replayAll();

    String newVersion = version;

    HostComponentVersionAdvertisedEvent event = new HostComponentVersionAdvertisedEvent(cluster, sch, newVersion, 1L);
    
    // !!! avoid injector for test class
    Field field = StackVersionListener.class.getDeclaredField("repositoryVersionDAO");
    field.setAccessible(true);
    field.set(listener, dao);

    listener.onAmbariEvent(event);

    verifyAll();
  }

  /**
   * Tests that the {@link RepositoryVersionEntity} is not updated if there is
   * an upgrade, even if the repo ID is passed back and the versions don't
   * match.
   *
   * @throws Exception
   */
  @Test
  public void testRepositoryVersionNotSetDuringUpgrade() throws Exception {
    expect(componentInfo.isVersionAdvertised()).andReturn(true).once();

    // this call will make it seem like there is an upgrade in progress
    expect(cluster.getUpgradeInProgress()).andReturn(createNiceMock(UpgradeEntity.class));

    // create the DAO - nothing will be called on it, so make it strict
    RepositoryVersionDAO dao = createStrictMock(RepositoryVersionDAO.class);

    replayAll();

    // !!! avoid injector for test class

    Field field = StackVersionListener.class.getDeclaredField("repositoryVersionDAO");
    field.setAccessible(true);
    field.set(listener, dao);

    HostComponentVersionAdvertisedEvent event = new HostComponentVersionAdvertisedEvent(cluster,
        sch, VALID_NEW_VERSION, 1L);

    // make sure that a repo ID will come back
    Assert.assertNotNull(event.getRepositoryVersionId());

    listener.onAmbariEvent(event);

    verifyAll();
  }

  private void sendEventAndVerify(String newVersion) {
    HostComponentVersionAdvertisedEvent event = new HostComponentVersionAdvertisedEvent(cluster, sch, newVersion);
    listener.onAmbariEvent(event);

    verifyAll();
  }
}
